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
package net.sourceforge.subsonic.command;

import java.util.Date;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class NetworkSettingsCommand {

    private boolean portForwardingEnabled;
    private boolean urlRedirectionEnabled;
    private String urlRedirectFrom;
    private int port;
    private boolean trial;
    private Date trialExpires;
    private boolean trialExpired;

    public void setPortForwardingEnabled(boolean portForwardingEnabled) {
        this.portForwardingEnabled = portForwardingEnabled;
    }

    public boolean isPortForwardingEnabled() {
        return portForwardingEnabled;
    }

    public boolean isUrlRedirectionEnabled() {
        return urlRedirectionEnabled;
    }

    public void setUrlRedirectionEnabled(boolean urlRedirectionEnabled) {
        this.urlRedirectionEnabled = urlRedirectionEnabled;
    }

    public String getUrlRedirectFrom() {
        return urlRedirectFrom;
    }

    public void setUrlRedirectFrom(String urlRedirectFrom) {
        this.urlRedirectFrom = urlRedirectFrom;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    public boolean isTrial() {
        return trial;
    }

    public void setTrialExpires(Date trialExpires) {
        this.trialExpires = trialExpires;
    }

    public Date getTrialExpires() {
        return trialExpires;
    }

    public void setTrialExpired(boolean trialExpired) {
        this.trialExpired = trialExpired;
    }

    public boolean isTrialExpired() {
        return trialExpired;
    }
}
