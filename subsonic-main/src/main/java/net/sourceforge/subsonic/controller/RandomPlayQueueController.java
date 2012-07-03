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

import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.RandomSearchCriteria;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the creating a random play queue.
 *
 * @author Sindre Mehus
 */
public class RandomPlayQueueController extends ParameterizableViewController {

    private PlayerService playerService;
    private List<ReloadFrame> reloadFrames;
    private SearchService searchService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int size = ServletRequestUtils.getRequiredIntParameter(request, "size");
        String genre = request.getParameter("genre");
        if (StringUtils.equalsIgnoreCase("any", genre)) {
            genre = null;
        }

        Integer fromYear = null;
        Integer toYear = null;

        String year = request.getParameter("year");
        if (!StringUtils.equalsIgnoreCase("any", year)) {
            String[] tmp = StringUtils.split(year);
            fromYear = Integer.parseInt(tmp[0]);
            toYear = Integer.parseInt(tmp[1]);
        }

        Integer musicFolderId = ServletRequestUtils.getRequiredIntParameter(request, "musicFolderId");
        if (musicFolderId == -1) {
            musicFolderId = null;
        }

        Player player = playerService.getPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();

        RandomSearchCriteria criteria = new RandomSearchCriteria(size, genre, fromYear, toYear, musicFolderId);
        playQueue.addFiles(false, searchService.getRandomSongs(criteria));

        if (request.getParameter("autoRandom") != null) {
            playQueue.setRandomSearchCriteria(criteria);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("reloadFrames", reloadFrames);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setReloadFrames(List<ReloadFrame> reloadFrames) {
        this.reloadFrames = reloadFrames;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}
