/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.backend.controller;

import net.sourceforge.subsonic.backend.dao.RedirectionDao;
import net.sourceforge.subsonic.backend.domain.Redirection;
import static net.sourceforge.subsonic.backend.controller.RedirectionManagementController.RESERVED_REDIRECTS;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.io.UnsupportedEncodingException;

/**
 * Redirects vanity URLs (such as http://sindre.subsonic.org).
 *
 * @author Sindre Mehus
 */
public class RedirectionController implements Controller {

    private static final Logger LOG = Logger.getLogger(RedirectionController.class);
    private RedirectionDao redirectionDao;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String redirectFrom = getRedirectFrom(request);
        if (RESERVED_REDIRECTS.containsKey(redirectFrom)) {
            LOG.info("Reserved redirection: " + redirectFrom);
            return new ModelAndView(new RedirectView(RESERVED_REDIRECTS.get(redirectFrom)));
        }

        Redirection redirection = redirectFrom == null ? null : redirectionDao.getRedirection(redirectFrom);

        if (redirection == null) {
            LOG.info("No redirection found: " + redirectFrom);
            return new ModelAndView(new RedirectView("http://subsonic.org/pages"));
        }

        redirection.setLastRead(new Date());
        redirection.setReadCount(redirection.getReadCount() + 1);
        redirectionDao.updateRedirection(redirection);

        // Check for trial expiration (unless called from REST client for which the Subsonic server manages trial expiry).
        if (isTrialExpired(redirection) && !isREST(request)) {
            LOG.info("Expired redirection: " + redirectFrom);
            return new ModelAndView(new RedirectView("http://subsonic.org/pages/redirect-expired.jsp?redirectFrom=" +
                    redirectFrom + "&expired=" + redirection.getTrialExpires().getTime()));
        }

        String requestUrl = getFullRequestURL(request);
        String to = StringUtils.removeEnd(getRedirectTo(request, redirection), "/");
        String redirectTo = requestUrl.replaceFirst("http://" + redirectFrom + "\\.subsonic\\.org", to);
        LOG.info("Redirecting from " + requestUrl + " to " + redirectTo);

        return new ModelAndView(new RedirectView(redirectTo));
    }

    private String getRedirectTo(HttpServletRequest request, Redirection redirection) {

        // If the request comes from within the same LAN as the destination Subsonic
        // server, redirect using the local IP address of the server.

        String localRedirectTo = redirection.getLocalRedirectTo();
        if (localRedirectTo != null) {
            try {
                URL url = new URL(redirection.getRedirectTo());
                if (url.getHost().equals(request.getRemoteAddr())) {
                    return localRedirectTo;
                }
            } catch (Throwable x) {
                LOG.error("Malformed local redirect URL.", x);
            }
        }

        return redirection.getRedirectTo();
    }

    private boolean isTrialExpired(Redirection redirection) {
        return redirection.isTrial() && redirection.getTrialExpires() != null && redirection.getTrialExpires().before(new Date());
    }

    private boolean isREST(HttpServletRequest request) {
        return request.getParameter("c") != null;
    }

    private String getFullRequestURL(HttpServletRequest request) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder(request.getRequestURL());

        // For backwards compatibility; return query parameters in exact same sequence.
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (request.getQueryString() != null) {
                builder.append("?").append(request.getQueryString());
            }
            return builder.toString();
        }

        builder.append("?");

        Enumeration<?> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                String p = URLEncoder.encode(paramValue, "UTF-8");
                builder.append(paramName).append("=").append(p).append("&");
            }
        }

        return builder.toString();
    }

    private String getRedirectFrom(HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        String host = url.getHost();

        String redirectFrom;
        if (host.contains(".")) {
            redirectFrom = StringUtils.substringBefore(host, ".");
        } else {
            // For testing.
            redirectFrom = request.getParameter("redirectFrom");
        }

        return StringUtils.lowerCase(redirectFrom);
    }

    public void setRedirectionDao(RedirectionDao redirectionDao) {
        this.redirectionDao = redirectionDao;
    }
}
