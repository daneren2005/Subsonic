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

import net.sourceforge.subsonic.domain.PodcastEpisode;
import net.sourceforge.subsonic.domain.PodcastStatus;
import net.sourceforge.subsonic.service.PodcastService;
import net.sourceforge.subsonic.util.StringUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;

/**
 * Controller for the "Podcast receiver" page.
 *
 * @author Sindre Mehus
 */
public class PodcastReceiverAdminController extends AbstractController {

    private PodcastService podcastService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        handleParameters(request);
        return new ModelAndView(new RedirectView("podcastReceiver.view?expandedChannels=" + request.getParameter("expandedChannels")));
    }

    private void handleParameters(HttpServletRequest request) {
        if (request.getParameter("add") != null) {
            String url = request.getParameter("add");
            podcastService.createChannel(url);
        }
        if (request.getParameter("downloadChannel") != null ||
            request.getParameter("downloadEpisode") != null) {
            download(StringUtil.parseInts(request.getParameter("downloadChannel")),
                     StringUtil.parseInts(request.getParameter("downloadEpisode")));
        }
        if (request.getParameter("deleteChannel") != null) {
            for (int channelId : StringUtil.parseInts(request.getParameter("deleteChannel"))) {
                podcastService.deleteChannel(channelId);
            }
        }
        if (request.getParameter("deleteEpisode") != null) {
            for (int episodeId : StringUtil.parseInts(request.getParameter("deleteEpisode"))) {
                podcastService.deleteEpisode(episodeId, true);
            }
        }
        if (request.getParameter("refresh") != null) {
            podcastService.refreshAllChannels(true);
        }
    }

    private void download(int[] channelIds, int[] episodeIds) {
        SortedSet<Integer> uniqueEpisodeIds = new TreeSet<Integer>();
        for (int episodeId : episodeIds) {
            uniqueEpisodeIds.add(episodeId);
        }
        for (int channelId : channelIds) {
            List<PodcastEpisode> episodes = podcastService.getEpisodes(channelId, false);
            for (PodcastEpisode episode : episodes) {
                uniqueEpisodeIds.add(episode.getId());
            }
        }

        for (Integer episodeId : uniqueEpisodeIds) {
            PodcastEpisode episode = podcastService.getEpisode(episodeId, false);
            if (episode != null && episode.getUrl() != null &&
                (episode.getStatus() == PodcastStatus.NEW ||
                 episode.getStatus() == PodcastStatus.ERROR ||
                 episode.getStatus() == PodcastStatus.SKIPPED)) {

                podcastService.downloadEpisode(episode);
            }
        }
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }
}
