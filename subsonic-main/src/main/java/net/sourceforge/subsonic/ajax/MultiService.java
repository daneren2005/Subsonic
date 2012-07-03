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
package net.sourceforge.subsonic.ajax;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.service.NetworkService;

/**
 * Provides miscellaneous AJAX-enabled services.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class MultiService {

    private static final Logger LOG = Logger.getLogger(MultiService.class);
    private NetworkService networkService;

    /**
     * Returns status for port forwarding and URL redirection.
     */
    public NetworkStatus getNetworkStatus() {
        NetworkService.Status portForwardingStatus = networkService.getPortForwardingStatus();
        NetworkService.Status urlRedirectionStatus = networkService.getURLRedirecionStatus();
        return new NetworkStatus(portForwardingStatus.getText(),
                                 portForwardingStatus.getDate(),
                                 urlRedirectionStatus.getText(),
                                 urlRedirectionStatus.getDate());
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }
}