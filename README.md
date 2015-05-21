##Basic Instructions
Run these commands to grab dependent libraries:
git submodule init
git submodule update

Go to DragSortListView/library, and ServerProxy and build project files with:
android update project --path ./

Add both projects and all .jar's in the libs folder

###SDK Project Dependencies (under sdk -> extras):
android -> support -> v7 -> appcompat
android -> support -> v7 -> mediarouter
google -> google_play_services -> libproject -> google-play-services_lib

###SDK Library Dependencies:
android -> support -> v4 -> android-support-v4.jar
android -> support -> v7 -> appcompat -> libs android-support-v7-appcompat.jar
android -> support -> v7 -> mediarouter -> libs -> android-support-v7-mediarouter.jar
google -> google_play_services -> libproject -> google-play-services_lib -> lib -> google-play-services.jar
