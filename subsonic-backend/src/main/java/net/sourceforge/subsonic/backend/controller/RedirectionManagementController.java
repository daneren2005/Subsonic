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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpConnectionParams;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import net.sourceforge.subsonic.backend.dao.RedirectionDao;
import net.sourceforge.subsonic.backend.domain.Redirection;

/**
 * @author Sindre Mehus
 */
public class RedirectionManagementController extends MultiActionController {

    private static final Logger LOG = Logger.getLogger(RedirectionManagementController.class);

    public static final Map<String,String> RESERVED_REDIRECTS = new HashMap<String, String>();

    static {
        RESERVED_REDIRECTS.put("forum", "http://www.activeobjects.no/subsonic/forum/index.php");
        RESERVED_REDIRECTS.put("www", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("web", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("ftp", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("mail", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("s", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("subsonic", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("gosubsonic", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("android", "http://www.subsonic.org/pages/android.jsp");
        RESERVED_REDIRECTS.put("iphone", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("subair", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("m", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("link", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("share", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("mobile", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("mobil", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("phone", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("wap", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("db", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("shop", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("wiki", "http://www.subsonic.org/pages/index.jsp");
        RESERVED_REDIRECTS.put("test", "http://www.subsonic.org/pages/index.jsp");
    }

    private RedirectionDao redirectionDao;

    public void register(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String redirectFrom = StringUtils.lowerCase(ServletRequestUtils.getRequiredStringParameter(request, "redirectFrom"));
        String licenseHolder = ServletRequestUtils.getStringParameter(request, "licenseHolder");
        String serverId = ServletRequestUtils.getRequiredStringParameter(request, "serverId");
        int port = ServletRequestUtils.getRequiredIntParameter(request, "port");
        Integer localPort = ServletRequestUtils.getIntParameter(request, "localPort");
        String localIp = ServletRequestUtils.getStringParameter(request, "localIp");
        String contextPath = ServletRequestUtils.getRequiredStringParameter(request, "contextPath");
        boolean trial = ServletRequestUtils.getBooleanParameter(request, "trial", false);

        Date now = new Date();
        Date trialExpires = null;
        if (trial) {
            trialExpires = new Date(ServletRequestUtils.getRequiredLongParameter(request, "trialExpires"));
        }

        if (RESERVED_REDIRECTS.containsKey(redirectFrom)) {
            sendError(response, "\"" + redirectFrom + "\" is a reserved address. Please select another.");
            return;
        }

        if (!redirectFrom.matches("(\\w|\\-)+")) {
            sendError(response, "Illegal characters present in \"" + redirectFrom + "\". Please select another.");
            return;
        }

        String host = request.getRemoteAddr();
        URL url = new URL("http", host, port, "/" + contextPath);
        String redirectTo = url.toExternalForm();

        String localRedirectTo = null;
        if (localIp != null && localPort != null) {
            URL localUrl = new URL("http", localIp, localPort, "/" + contextPath);
            localRedirectTo = localUrl.toExternalForm();
        }

        Redirection redirection = redirectionDao.getRedirection(redirectFrom);
        if (redirection == null) {

            // Delete other redirects for same server ID.
            redirectionDao.deleteRedirectionsByServerId(serverId);

            redirection = new Redirection(0, licenseHolder, serverId, redirectFrom, redirectTo, localRedirectTo, trial, trialExpires, now, null, 0);
            redirectionDao.createRedirection(redirection);
            LOG.info("Created " + redirection);

        } else {

            boolean sameServerId = serverId.equals(redirection.getServerId());
            boolean sameLicenseHolder = licenseHolder != null && licenseHolder.equals(redirection.getLicenseHolder());

            // Note: A licensed user can take over any expired trial domain.
            boolean existingTrialExpired = redirection.getTrialExpires() != null && redirection.getTrialExpires().before(now);

            if (sameServerId || sameLicenseHolder || (existingTrialExpired && !trial)) {
                redirection.setLicenseHolder(licenseHolder);
                redirection.setServerId(serverId);
                redirection.setRedirectFrom(redirectFrom);
                redirection.setRedirectTo(redirectTo);
                redirection.setLocalRedirectTo(localRedirectTo);
                redirection.setTrial(trial);
                redirection.setTrialExpires(trialExpires);
                redirection.setLastUpdated(now);
                redirectionDao.updateRedirection(redirection);
                LOG.info("Updated " + redirection);
            } else {
                sendError(response, "The web address \"" + redirectFrom + "\" is already in use. Please select another.");
            }
        }
    }

    public void unregister(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String serverId = ServletRequestUtils.getStringParameter(request, "serverId");
        if (!StringUtils.isEmpty(serverId)) {
            redirectionDao.deleteRedirectionsByServerId(serverId);
        }
    }

    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String redirectFrom = StringUtils.lowerCase(ServletRequestUtils.getRequiredStringParameter(request, "redirectFrom"));

        Redirection redirection = redirectionDao.getRedirection(redirectFrom);
        if (redirection == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Web address " + redirectFrom + ".subsonic.org not registered.");
            return;
        }

        PrintWriter writer = response.getWriter();
        String url = redirection.getRedirectTo();
        if (!url.endsWith("/")) {
            url += "/";
        }
        writer.println(url);

        url = redirection.getLocalRedirectTo();
        if (!url.endsWith("/")) {
            url += "/";
        }
        writer.println(url);
    }

    public void test(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String redirectFrom = StringUtils.lowerCase(ServletRequestUtils.getRequiredStringParameter(request, "redirectFrom"));
        PrintWriter writer = response.getWriter();

        Redirection redirection = redirectionDao.getRedirection(redirectFrom);
        String webAddress = redirectFrom + ".subsonic.org";
        if (redirection == null) {
            writer.print("Web address " + webAddress + " not registered.");
            return;
        }

        if (redirection.getTrialExpires() != null && redirection.getTrialExpires().before(new Date())) {
            writer.print("Trial period expired. Please donate to activate web address.");
            return;
        }

        String url = redirection.getRedirectTo();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "icons/favicon.ico";

        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 15000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 15000);
        HttpGet method = new HttpGet(url);

        try {
            HttpResponse resp = client.execute(method);
            StatusLine status = resp.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK) {
                String msg = webAddress + " responded successfully.";
                writer.print(msg);
                LOG.info(msg);
            } else {
                String msg = webAddress + " returned HTTP error code " + status.getStatusCode() + " " + status.getReasonPhrase();
                writer.print(msg);
                LOG.info(msg);
            }
        } catch (SSLPeerUnverifiedException x) {
            String msg = webAddress + " responded successfully, but could not authenticate it.";
            writer.print(msg);
            LOG.info(msg);

        } catch (Throwable x) {
            String msg = webAddress + " is registered, but could not connect to it. (" + x.getClass().getSimpleName() + ")";
            writer.print(msg);
            LOG.info(msg);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.getWriter().print(message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    public void dump(HttpServletRequest request, HttpServletResponse response) throws Exception {

        File file = File.createTempFile("redirections", ".txt");
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        try {
            int offset = 0;
            int count = 100;
            while (true) {
                List<Redirection> redirections = redirectionDao.getAllRedirections(offset, count);
                if (redirections.isEmpty()) {
                    break;
                }
                offset += redirections.size();
                for (Redirection redirection : redirections) {
                    writer.println(redirection);
                }
            }
            LOG.info("Dumped redirections to " + file.getAbsolutePath());
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public void setRedirectionDao(RedirectionDao redirectionDao) {
        this.redirectionDao = redirectionDao;
    }
}