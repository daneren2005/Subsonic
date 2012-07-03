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
package net.sourceforge.subsonic.domain;

import org.wetorrent.upnp.GatewayDevice;
import org.wetorrent.upnp.GatewayDiscover;

import java.net.InetAddress;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class WeUPnPRouter implements Router {
    private final GatewayDevice device;

    private WeUPnPRouter(GatewayDevice device) {
        this.device = device;
    }

    public static WeUPnPRouter findRouter() throws Exception {
        GatewayDiscover discover = new GatewayDiscover();
        discover.discover();
        GatewayDevice device = discover.getValidGateway();
        if (device == null) {
            return null;
        }

        return new WeUPnPRouter(device);
    }

    public void addPortMapping(int externalPort, int internalPort, int leaseDuration) throws Exception {
        String localIp = InetAddress.getLocalHost().getHostAddress();
        device.addPortMapping(externalPort, internalPort, localIp, "TCP", "Subsonic");
    }

    public void deletePortMapping(int externalPort, int internalPort) throws Exception {
        device.deletePortMapping(externalPort, "TCP");
    }
}
