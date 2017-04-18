## Basic Instructions
Grab dependent libraries
```
git submodule init
git submodule update
```

Go to ServerProxy and build project files
```
android update project --path ./
```

Add both projects and all .jar's in the libs folder

## SDK Project Dependencies
Under sdk -> extras:<br>
android -> support -> v7 -> appcompat<br>
android -> support -> v7 -> mediarouter<br>
google -> google_play_services -> libproject -> google-play-services_lib

## SDK Library Dependencies
android -> support -> v4 -> android-support-v4.jar<br>
android -> support -> v7 -> appcompat -> libs android-support-v7-appcompat.jar<br>
android -> support -> v7 -> mediarouter -> libs -> android-support-v7-mediarouter.jar<br>
google -> google_play_services -> libproject -> google-play-services_lib -> lib -> google-play-services.jar

## Updating Icons
Media Icons are double standard size.  On https://romannurik.github.io/AndroidAssetStudio/icons-actionbar.html you can manually change this via the following js commands:
```
PARAM_RESOURCES.iconSize = {w: 64, h: 64}
PARAM_RESOURCES['targetRect-clipart'] = { x: 12, y: 12, w: 40, h: 40 }
form.fields_[0].params_.maxFinalSize = {w: 512, h: 512}
```
