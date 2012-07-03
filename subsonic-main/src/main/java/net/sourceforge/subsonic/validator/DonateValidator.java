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
package net.sourceforge.subsonic.validator;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import net.sourceforge.subsonic.command.PasswordSettingsCommand;
import net.sourceforge.subsonic.command.DonateCommand;
import net.sourceforge.subsonic.controller.DonateController;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * Validator for {@link DonateController}.
 *
 * @author Sindre Mehus
 */
public class DonateValidator implements Validator {
    private SettingsService settingsService;

    public boolean supports(Class clazz) {
        return clazz.equals(DonateCommand.class);
    }

    public void validate(Object obj, Errors errors) {
        DonateCommand command = (DonateCommand) obj;

        if (!settingsService.isLicenseValid(command.getEmailAddress(), command.getLicense())) {
            errors.rejectValue("license", "donate.invalidlicense");
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
