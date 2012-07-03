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
package net.sourceforge.subsonic.service;

/**
 * Provides services for generating ads.
 *
 * @author Sindre Mehus
 */
public class AdService {

    private final String[] ads = {

            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=40&l=ur1&category=computers_accesories&banner=1CH7VNNWF908JYQPHX82&f=ifr' width='120' height='60' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=21&l=ur1&category=game_downloads&banner=13PTQH69Q2290VF8SR82&f=ifr' width='125' height='125' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='ad/omakasa.html' width='120' height='240' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=29&l=ur1&category=homeaudiohometheater&banner=0T4YJ6YBNCMJM9GGAK02&f=ifr' width='120' height='600' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=21&l=ur1&category=50mp3albums5each&banner=19QT8FZHDHFZDN87C482&f=ifr' width='125' height='125' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=21&l=ur1&category=computers_accesories&banner=0Q1FJ9TBD13SA09DSMR2&f=ifr' width='125' height='125' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=40&l=ur1&category=mp3&banner=0TBQHNYNA4B47J02NFG2&f=ifr' width='120' height='60' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<OBJECT classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' codebase='http://fpdownload.macromedia.com/get/flashplayer/current/swflash.cab' id='Player_3d36fdd7-b2fa-4dfd-b517-c5efe035a14d'  WIDTH='120px' HEIGHT='500px'> <PARAM NAME='movie' VALUE='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8010%2F3d36fdd7-b2fa-4dfd-b517-c5efe035a14d&Operation=GetDisplayTemplate'><PARAM NAME='quality' VALUE='high'><PARAM NAME='bgcolor' VALUE='#FFFFFF'><PARAM NAME='allowscriptaccess' VALUE='always'><embed src='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8010%2F3d36fdd7-b2fa-4dfd-b517-c5efe035a14d&Operation=GetDisplayTemplate' id='Player_3d36fdd7-b2fa-4dfd-b517-c5efe035a14d' quality='high' bgcolor='#ffffff' name='Player_3d36fdd7-b2fa-4dfd-b517-c5efe035a14d' allowscriptaccess='always'  type='application/x-shockwave-flash' align='middle' height='500px' width='120px'/> </OBJECT> <NOSCRIPT><A HREF='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8010%2F3d36fdd7-b2fa-4dfd-b517-c5efe035a14d&Operation=NoScript'>Amazon.com Widgets</A></NOSCRIPT>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=40&l=ur1&category=kindle&banner=19NTJJCKSX6TY1C567G2&f=ifr' width='120' height='60' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='ad/omakasa.html' width='120' height='240' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=14&l=ur1&category=electronicsrot&f=ifr' width='160' height='600' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<OBJECT classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' codebase='http://fpdownload.macromedia.com/get/flashplayer/current/swflash.cab' id='Player_3fde1609-804d-46de-8802-2a16321cf533'  WIDTH='160px' HEIGHT='400px'> <PARAM NAME='movie' VALUE='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8009%2F3fde1609-804d-46de-8802-2a16321cf533&Operation=GetDisplayTemplate'><PARAM NAME='quality' VALUE='high'><PARAM NAME='bgcolor' VALUE='#FFFFFF'><PARAM NAME='allowscriptaccess' VALUE='always'><embed src='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8009%2F3fde1609-804d-46de-8802-2a16321cf533&Operation=GetDisplayTemplate' id='Player_3fde1609-804d-46de-8802-2a16321cf533' quality='high' bgcolor='#ffffff' name='Player_3fde1609-804d-46de-8802-2a16321cf533' allowscriptaccess='always'  type='application/x-shockwave-flash' align='middle' height='400px' width='160px'/> </OBJECT> <NOSCRIPT><A HREF='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8009%2F3fde1609-804d-46de-8802-2a16321cf533&Operation=NoScript'>Amazon.com Widgets</A></NOSCRIPT>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=40&l=ur1&category=unboxdigital&banner=10NVPFMW8ACPNX4T4E82&f=ifr' width='120' height='60' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<OBJECT classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' codebase='http://fpdownload.macromedia.com/get/flashplayer/current/swflash.cab' id='Player_2e2141ca-ec13-4dc9-88f2-08be95e47e6d'  WIDTH='160px' HEIGHT='300px'> <PARAM NAME='movie' VALUE='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8014%2F2e2141ca-ec13-4dc9-88f2-08be95e47e6d&Operation=GetDisplayTemplate'><PARAM NAME='quality' VALUE='high'><PARAM NAME='bgcolor' VALUE='#FFFFFF'><PARAM NAME='allowscriptaccess' VALUE='always'><embed src='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8014%2F2e2141ca-ec13-4dc9-88f2-08be95e47e6d&Operation=GetDisplayTemplate' id='Player_2e2141ca-ec13-4dc9-88f2-08be95e47e6d' quality='high' bgcolor='#ffffff' name='Player_2e2141ca-ec13-4dc9-88f2-08be95e47e6d' allowscriptaccess='always'  type='application/x-shockwave-flash' align='middle' height='300px' width='160px'></embed></OBJECT> <NOSCRIPT><A HREF='http://ws.amazon.com/widgets/q?ServiceVersion=20070822&MarketPlace=US&ID=V20070822%2FUS%2Fsubsonic-20%2F8014%2F2e2141ca-ec13-4dc9-88f2-08be95e47e6d&Operation=NoScript'>Amazon.com Widgets</A></NOSCRIPT>",
            "<iframe src='http://rcm.amazon.com/e/cm?t=subsonic-20&o=1&p=14&l=ur1&category=musicandentertainmentrot&f=ifr' width='160' height='600' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://www.subsonic.org/pages/subsonic-ad.jsp' width='180' height='400' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>",
            "<iframe src='http://www.subsonic.org/pages/zazeen-ad.jsp' width='120' height='600' scrolling='no' border='0' marginwidth='0' style='border:none;' frameborder='0'></iframe>"
    };
    private int adInterval;
    private int pageCount;
    private int adIndex;

    /**
     * Returns an ad or <code>null</code> if no ad should be displayed.
     */
    public String getAd() {
        if (pageCount++ % adInterval == 0) {

            adIndex = (adIndex + 1) % ads.length;
            return ads[adIndex];
        }

        return null;
    }

    /**
     * Set by Spring.
     */
    public void setAdInterval(int adInterval) {
        this.adInterval = adInterval;
    }
}