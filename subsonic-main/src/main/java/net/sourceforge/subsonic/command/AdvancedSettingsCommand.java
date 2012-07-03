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

import net.sourceforge.subsonic.controller.AdvancedSettingsController;

/**
 * Command used in {@link AdvancedSettingsController}.
 *
 * @author Sindre Mehus
 */
public class AdvancedSettingsCommand {
    private String downsampleCommand;
    private String coverArtLimit;
    private String downloadLimit;
    private String uploadLimit;
    private String streamPort;
    private boolean ldapEnabled;
    private String ldapUrl;
    private String ldapSearchFilter;
    private String ldapManagerDn;
    private String ldapManagerPassword;
    private boolean ldapAutoShadowing;
    private String brand;
    private boolean isReloadNeeded;

    public String getDownsampleCommand() {
        return downsampleCommand;
    }

    public void setDownsampleCommand(String downsampleCommand) {
        this.downsampleCommand = downsampleCommand;
    }

    public String getCoverArtLimit() {
        return coverArtLimit;
    }

    public void setCoverArtLimit(String coverArtLimit) {
        this.coverArtLimit = coverArtLimit;
    }

    public String getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(String downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    public String getUploadLimit() {
        return uploadLimit;
    }

    public String getStreamPort() {
        return streamPort;
    }

    public void setStreamPort(String streamPort) {
        this.streamPort = streamPort;
    }

    public void setUploadLimit(String uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        this.ldapEnabled = ldapEnabled;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        this.ldapSearchFilter = ldapSearchFilter;
    }

    public String getLdapManagerDn() {
        return ldapManagerDn;
    }

    public void setLdapManagerDn(String ldapManagerDn) {
        this.ldapManagerDn = ldapManagerDn;
    }

    public String getLdapManagerPassword() {
        return ldapManagerPassword;
    }

    public void setLdapManagerPassword(String ldapManagerPassword) {
        this.ldapManagerPassword = ldapManagerPassword;
    }

    public boolean isLdapAutoShadowing() {
        return ldapAutoShadowing;
    }

    public void setLdapAutoShadowing(boolean ldapAutoShadowing) {
        this.ldapAutoShadowing = ldapAutoShadowing;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setReloadNeeded(boolean reloadNeeded) {
        isReloadNeeded = reloadNeeded;
    }

    public boolean isReloadNeeded() {
        return isReloadNeeded;
    }
}
