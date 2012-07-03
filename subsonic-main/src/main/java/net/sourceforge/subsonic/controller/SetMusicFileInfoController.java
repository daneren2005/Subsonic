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

import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.service.*;
import net.sourceforge.subsonic.util.*;
import net.sourceforge.subsonic.filter.ParameterDecodingFilter;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.view.*;
import org.springframework.web.servlet.mvc.*;

import javax.servlet.http.*;

/**
 * Controller for updating music file metadata.
 *
 * @author Sindre Mehus
 */
public class SetMusicFileInfoController extends AbstractController {

    private MediaFileService mediaFileService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getParameter("path");
        String action = request.getParameter("action");

        MediaFile mediaFile = mediaFileService.getMediaFile(path);

        if ("comment".equals(action)) {
            mediaFile.setComment(StringUtil.toHtml(request.getParameter("comment")));
            mediaFileService.updateMediaFile(mediaFile);
        }

        String url = "main.view?path" + ParameterDecodingFilter.PARAM_SUFFIX  + "=" + StringUtil.utf8HexEncode(path);
        return new ModelAndView(new RedirectView(url));
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
