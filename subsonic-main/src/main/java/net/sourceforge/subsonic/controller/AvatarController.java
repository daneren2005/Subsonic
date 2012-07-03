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

import net.sourceforge.subsonic.domain.Avatar;
import net.sourceforge.subsonic.domain.AvatarScheme;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller which produces avatar images.
 *
 * @author Sindre Mehus
 */
public class AvatarController implements Controller, LastModified {

    private SettingsService settingsService;

    public long getLastModified(HttpServletRequest request) {
        Avatar avatar = getAvatar(request);
        return avatar == null ? -1L : avatar.getCreatedDate().getTime();
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Avatar avatar = getAvatar(request);

        if (avatar == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // TODO: specify caching filter.

        response.setContentType(avatar.getMimeType());
        response.getOutputStream().write(avatar.getData());
        return null;
    }

    private Avatar getAvatar(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id != null) {
            return settingsService.getSystemAvatar(Integer.parseInt(id));
        }

        String username = request.getParameter("username");
        if (username == null) {
            return null;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);
        if (userSettings.getAvatarScheme() == AvatarScheme.SYSTEM) {
            return settingsService.getSystemAvatar(userSettings.getSystemAvatarId());
        }
        return settingsService.getCustomAvatar(username);
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}