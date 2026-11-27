[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/O5O514FGIG)

# DSub2000

Connect to your Subsonic server and listen to your music wherever you go. Songs
are cached for playback to save on your mobile bandwidth and to make them
available when you have no connection at all.

Subsonic is a cross-platform FOSS media server that's capable of indexing very
large media collections. The server can transcode if necessary so that the app
can play files that your device may not normally support.

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/github.paroj.dsub2000)

## Screenshots

| Albums | Sidebar | Current Song |
| ------ | ------- | ------------ |
| ![](fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/2.png) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg) |

| Playlists | Now Playing | Details |
| --------- | ----------- | ------- |
| ![](fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/5.png) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/6.png) |
| You can make playlists available offline by selecting "cached" here. | This shows the play queue. You drag and drop songs to rearrange. Note that you can press the "cached" icon for a context menu. | You can check whether the file was transcoded here. |

## Features

* Drag and drop songs to rearrange your playlist on the Now Playing tab
* Pause playback when other apps request audio focus (navigation, etc)
* Lockscreen controls
* Gapless Playback
* Highly customizable UI
* User defined Cache Size, Network Timeout, and Buffer Length

## Permissions

This is why we need the permissions we do:

* Foreground Service - For downloading your playlists to cache
* Read & Write External Storage - Store the music cache on a sdcard
* Access Fine Location - Automatic day/ night mode and different server URL in local WiFi
* Read Phone State - Auto-pause playback when a call is received

## Updating Icons
Media Icons are double standard size.  On https://romannurik.github.io/AndroidAssetStudio/icons-actionbar.html you can manually change this via the following js commands:
```
PARAM_RESOURCES.iconSize = {w: 64, h: 64}
PARAM_RESOURCES['targetRect-clipart'] = { x: 12, y: 12, w: 40, h: 40 }
form.fields_[0].params_.maxFinalSize = {w: 512, h: 512}
```
