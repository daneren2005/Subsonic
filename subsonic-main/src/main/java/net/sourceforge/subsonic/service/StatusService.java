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
package net.sourceforge.subsonic.service;

import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.TransferStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides services for maintaining the list of stream, download and upload statuses.
 * <p/>
 * Note that for stream statuses, the last inactive status is also stored.
 *
 * @author Sindre Mehus
 * @see TransferStatus
 */
public class StatusService {

    private final List<TransferStatus> streamStatuses = new ArrayList<TransferStatus>();
    private final List<TransferStatus> downloadStatuses = new ArrayList<TransferStatus>();
    private final List<TransferStatus> uploadStatuses = new ArrayList<TransferStatus>();

    // Maps from player ID to latest inactive stream status.
    private final Map<String, TransferStatus> inactiveStreamStatuses = new LinkedHashMap<String, TransferStatus>();

    public synchronized TransferStatus createStreamStatus(Player player) {
        // Reuse existing status, if possible.
        TransferStatus status = inactiveStreamStatuses.get(player.getId());
        if (status != null) {
            status.setActive(true);
        } else {
            status = createStatus(player, streamStatuses);
        }
        return status;
    }

    public synchronized void removeStreamStatus(TransferStatus status) {
        // Move it to the map of inactive statuses.
        status.setActive(false);
        inactiveStreamStatuses.put(status.getPlayer().getId(), status);
        streamStatuses.remove(status);
    }

    public synchronized List<TransferStatus> getAllStreamStatuses() {

        List<TransferStatus> result = new ArrayList<TransferStatus>(streamStatuses);

        // Add inactive status for those players that have no active status.
        Set<String> activePlayers = new HashSet<String>();
        for (TransferStatus status : streamStatuses) {
            activePlayers.add(status.getPlayer().getId());
        }

        for (Map.Entry<String, TransferStatus> entry : inactiveStreamStatuses.entrySet()) {
            if (!activePlayers.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public synchronized List<TransferStatus> getStreamStatusesForPlayer(Player player) {
        List<TransferStatus> result = new ArrayList<TransferStatus>();
        for (TransferStatus status : streamStatuses) {
            if (status.getPlayer().getId().equals(player.getId())) {
                result.add(status);
            }
        }

        // If no active statuses exists, add the inactive one.
        if (result.isEmpty()) {
            TransferStatus inactiveStatus = inactiveStreamStatuses.get(player.getId());
            if (inactiveStatus != null) {
                result.add(inactiveStatus);
            }
        }

        return result;
    }

    public synchronized TransferStatus createDownloadStatus(Player player) {
        return createStatus(player, downloadStatuses);
    }

    public synchronized void removeDownloadStatus(TransferStatus status) {
        downloadStatuses.remove(status);
    }

    public synchronized List<TransferStatus> getAllDownloadStatuses() {
        return new ArrayList<TransferStatus>(downloadStatuses);
    }

    public synchronized TransferStatus createUploadStatus(Player player) {
        return createStatus(player, uploadStatuses);
    }

    public synchronized void removeUploadStatus(TransferStatus status) {
        uploadStatuses.remove(status);
    }

    public synchronized List<TransferStatus> getAllUploadStatuses() {
        return new ArrayList<TransferStatus>(uploadStatuses);
    }

    private synchronized TransferStatus createStatus(Player player, List<TransferStatus> statusList) {
        TransferStatus status = new TransferStatus();
        status.setPlayer(player);
        statusList.add(status);
        return status;
    }

}