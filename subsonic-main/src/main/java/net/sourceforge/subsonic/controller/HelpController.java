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
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.*;
import net.sourceforge.subsonic.service.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.*;

import javax.servlet.http.*;
import java.util.*;

/**
 * Controller for the help page.
 *
 * @author Sindre Mehus
 */
public class HelpController extends ParameterizableViewController {

    private VersionService versionService;
    private SettingsService settingsService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        if (versionService.isNewFinalVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestFinalVersion());
        } else if (versionService.isNewBetaVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestBetaVersion());
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        String serverInfo = request.getSession().getServletContext().getServerInfo() +
                            ", java " + System.getProperty("java.version") +
                            ", " + System.getProperty("os.name");

        map.put("brand", settingsService.getBrand());
        map.put("localVersion", versionService.getLocalVersion());
        map.put("buildDate", versionService.getLocalBuildDate());
        map.put("buildNumber", versionService.getLocalBuildNumber());
        map.put("serverInfo", serverInfo);
        map.put("usedMemory", totalMemory - freeMemory);
        map.put("totalMemory", totalMemory);
        map.put("logEntries", Logger.getLatestLogEntries());
        map.put("logFile", Logger.getLogFile());

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
