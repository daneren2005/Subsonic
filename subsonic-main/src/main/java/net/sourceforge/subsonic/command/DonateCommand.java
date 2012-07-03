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

import net.sourceforge.subsonic.controller.DonateController;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * Command used in {@link DonateController}.
 *
 * @author Sindre Mehus
 */
public class DonateCommand {

    private String path;
    private String emailAddress;
    private String license;
    private Date licenseDate;
    private boolean licenseValid;
    private String brand;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = StringUtils.trim(emailAddress);
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = StringUtils.trim(license);
    }

    public Date getLicenseDate() {
        return licenseDate;
    }

    public void setLicenseDate(Date licenseDate) {
        this.licenseDate = licenseDate;
    }

    public boolean isLicenseValid() {
        return licenseValid;
    }

    public void setLicenseValid(boolean licenseValid) {
        this.licenseValid = licenseValid;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
