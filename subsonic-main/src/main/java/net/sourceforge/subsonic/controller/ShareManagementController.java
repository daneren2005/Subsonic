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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.*;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.Share;

/**
 * Controller for sharing music on Twitter, Facebook etc.
 *
 * @author Sindre Mehus
 */
public class ShareManagementController extends MultiActionController {

    private MediaFileService mediaFileService;
    private SettingsService settingsService;
    private ShareService shareService;
    private PlayerService playerService;
    private SecurityService securityService;

    public ModelAndView createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {

        List<MediaFile> files = getMediaFiles(request);
        MediaFile dir = null;
        if (!files.isEmpty()) {
            dir = files.get(0);
            if (!dir.isAlbum()) {
                dir = mediaFileService.getParentOf(dir);
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("urlRedirectionEnabled", settingsService.isUrlRedirectionEnabled());
        map.put("dir", dir);
        map.put("user", securityService.getCurrentUser(request));
        Share share = shareService.createShare(request, files);
        map.put("playUrl", shareService.getShareUrl(share));

        return new ModelAndView("createShare", "model", map);
    }

    private List<MediaFile> getMediaFiles(HttpServletRequest request) throws IOException {
        String dir = request.getParameter("dir");
        String playerId = request.getParameter("player");

        List<MediaFile> result = new ArrayList<MediaFile>();

        if (dir != null) {
            MediaFile album = mediaFileService.getMediaFile(dir);
            int[] indexes = ServletRequestUtils.getIntParameters(request, "i");
            if (indexes.length == 0) {
                return Arrays.asList(album);
            }
            List<MediaFile> children = mediaFileService.getChildrenOf(album, true, true, true);
            for (int index : indexes) {
                result.add(children.get(index));
            }
        } else if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            PlayQueue playQueue = player.getPlayQueue();
            List<MediaFile> result1;
            synchronized (playQueue) {
                result1 = playQueue.getFiles();
            }
            result = result1;
        }

        return result;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}