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
package net.sourceforge.subsonic.theme;

import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Theme source implementation which uses two resource bundles: the
 * theme specific (e.g., barents.properties), and the default (default.properties).
 *
 * @author Sindre Mehus
 */
public class SubsonicThemeSource extends ResourceBundleThemeSource {

    private String defaultResourceBundle;

    @Override
    protected MessageSource createMessageSource(String basename) {
        ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) super.createMessageSource(basename);

        ResourceBundleMessageSource parentMessageSource = new ResourceBundleMessageSource();
        parentMessageSource.setBasename(defaultResourceBundle);
        messageSource.setParentMessageSource(parentMessageSource);

        return messageSource;
    }

    public void setDefaultResourceBundle(String defaultResourceBundle) {
        this.defaultResourceBundle = defaultResourceBundle;
    }
}
