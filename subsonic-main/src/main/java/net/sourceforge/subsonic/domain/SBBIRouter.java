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

import net.sbbi.upnp.impls.InternetGatewayDevice;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Sindre Mehus
 */
public class SBBIRouter implements Router {

    // The timeout in milliseconds for finding a router device.
    private static final int DISCOVERY_TIMEOUT = 3000;

    private final InternetGatewayDevice device;

    private SBBIRouter(InternetGatewayDevice device) {
        this.device = device;
    }

    public static SBBIRouter findRouter() throws Exception {
        InternetGatewayDevice[] devices;
        try {
            devices = InternetGatewayDevice.getDevices(DISCOVERY_TIMEOUT);
        } catch (IOException e) {
            throw new Exception("Could not find router", e);
        }

        if (devices == null || devices.length == 0) {
            return null;
        }

        return new SBBIRouter(devices[0]);
    }

    public void addPortMapping(int externalPort, int internalPort, int leaseDuration) throws Exception {
        String localIp = InetAddress.getLocalHost().getHostAddress();
        device.addPortMapping("Subsonic", null, internalPort, externalPort, localIp, leaseDuration, "TCP");
    }

    public void deletePortMapping(int externalPort, int internal) throws Exception {
        device.deletePortMapping(null, externalPort, "TCP");
    }
}
