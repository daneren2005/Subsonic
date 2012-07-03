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

import net.sourceforge.subsonic.command.DonateCommand;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * Controller for the donation page.
 *
 * @author Sindre Mehus
 */
public class DonateController extends SimpleFormController {

    private SettingsService settingsService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        DonateCommand command = new DonateCommand();
        command.setPath(request.getParameter("path"));

        command.setEmailAddress(settingsService.getLicenseEmail());
        command.setLicenseDate(settingsService.getLicenseDate());
        command.setLicenseValid(settingsService.isLicenseValid());
        command.setLicense(settingsService.getLicenseCode());
        command.setBrand(settingsService.getBrand());

        return command;
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object com, BindException errors)
            throws Exception {
        DonateCommand command = (DonateCommand) com;
        Date now = new Date();

        settingsService.setLicenseCode(command.getLicense());
        settingsService.setLicenseEmail(command.getEmailAddress());
        settingsService.setLicenseDate(now);
        settingsService.save();
        settingsService.validateLicenseAsync();

        // Reflect changes in view. The validator has already validated the license.
        command.setLicenseValid(true);
        command.setLicenseDate(now);

        return new ModelAndView(getSuccessView(), errors.getModel());
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}