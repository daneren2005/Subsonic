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

import java.awt.Dimension;

import junit.framework.TestCase;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class StreamControllerTestCase extends TestCase {

    public void testGetRequestedVideoSize() {
        StreamController controller = new StreamController();

        // Valid spec.
        assertEquals("Wrong size.", new Dimension(123, 456), controller.getRequestedVideoSize("123x456"));
        assertEquals("Wrong size.", new Dimension(456, 123), controller.getRequestedVideoSize("456x123"));
        assertEquals("Wrong size.", new Dimension(1, 1), controller.getRequestedVideoSize("1x1"));

        // Missing spec.
        assertNull("Wrong size.", controller.getRequestedVideoSize(null));

        // Invalid spec.
        assertNull("Wrong size.", controller.getRequestedVideoSize("123"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("123x"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("x123"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("x"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("foo123x456bar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("foo123x456"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("123x456bar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("fooxbar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("-1x1"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("1x-1"));

        // Too large.
        assertNull("Wrong size.", controller.getRequestedVideoSize("3000x100"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("100x3000"));
    }

    public void testGetSuitableVideoSize() {

        // 4:3 aspect rate
        doTestGetSuitableVideoSize(720, 540, 200, 320, 240);
        doTestGetSuitableVideoSize(720, 540, 300, 320, 240);
        doTestGetSuitableVideoSize(720, 540, 400, 320, 240);
        doTestGetSuitableVideoSize(720, 540, 500, 320, 240);
        doTestGetSuitableVideoSize(720, 540, 600, 320, 240);

        doTestGetSuitableVideoSize(720, 540, 700, 480, 360);
        doTestGetSuitableVideoSize(720, 540, 800, 480, 360);
        doTestGetSuitableVideoSize(720, 540, 900, 480, 360);
        doTestGetSuitableVideoSize(720, 540, 1000, 480, 360);

        doTestGetSuitableVideoSize(720, 540, 1100, 640, 480);
        doTestGetSuitableVideoSize(720, 540, 1200, 640, 480);
        doTestGetSuitableVideoSize(720, 540, 1500, 640, 480);
        doTestGetSuitableVideoSize(720, 540, 2000, 640, 480);

        // 16:9 aspect rate
        doTestGetSuitableVideoSize(960, 540, 200, 428, 240);
        doTestGetSuitableVideoSize(960, 540, 300, 428, 240);
        doTestGetSuitableVideoSize(960, 540, 400, 428, 240);
        doTestGetSuitableVideoSize(960, 540, 500, 428, 240);
        doTestGetSuitableVideoSize(960, 540, 600, 428, 240);

        doTestGetSuitableVideoSize(960, 540, 700, 640, 360);
        doTestGetSuitableVideoSize(960, 540, 800, 640, 360);
        doTestGetSuitableVideoSize(960, 540, 900, 640, 360);
        doTestGetSuitableVideoSize(960, 540, 1000, 640, 360);

        doTestGetSuitableVideoSize(960, 540, 1100, 854, 480);
        doTestGetSuitableVideoSize(960, 540, 1200, 854, 480);
        doTestGetSuitableVideoSize(960, 540, 1500, 854, 480);
        doTestGetSuitableVideoSize(960, 540, 2000, 854, 480);

        // Small original size.
        doTestGetSuitableVideoSize(100, 100, 1000, 100, 100);
        doTestGetSuitableVideoSize(100, 1000, 1000, 100, 1000);
        doTestGetSuitableVideoSize(1000, 100, 100, 1000, 100);

        // Unknown original size.
        doTestGetSuitableVideoSize(720, null, 200, 320, 240);
        doTestGetSuitableVideoSize(null, 540, 300, 320, 240);
        doTestGetSuitableVideoSize(null, null, 400, 320, 240);
        doTestGetSuitableVideoSize(720, null, 500, 320, 240);
        doTestGetSuitableVideoSize(null, 540, 600, 320, 240);
        doTestGetSuitableVideoSize(null, null, 700, 480, 360);
        doTestGetSuitableVideoSize(720, null, 1200, 640, 480);
        doTestGetSuitableVideoSize(null, 540, 1500, 640, 480);
        doTestGetSuitableVideoSize(null, null, 2000, 640, 480);

        // Odd original size.
        doTestGetSuitableVideoSize(853, 464, 1500, 854, 464);
        doTestGetSuitableVideoSize(464, 853, 1500, 464, 854);
    }

    private void doTestGetSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate, int expectedWidth, int expectedHeight) {
        StreamController controller = new StreamController();
        Dimension dimension = controller.getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        assertEquals("Wrong width.", expectedWidth, dimension.width);
        assertEquals("Wrong height.", expectedHeight, dimension.height);
    }
}
