# Changelog

## 5.8.1 (18/11/2026)

- Fix APKs failing to install
- New overview screen with a tile grid, an Albums/Songs switch and a folder picker; Videos moved to the side menu
- Add song lists on servers with the songListsNCM extension (recently played, most played, highest rated, by year, alphabetical); capabilities are detected and refreshed when the server software changes
- Show station logos in the internet radio list
- Show artist credits as provided by the server, including multi-artist credits
- Show messages as snackbars instead of system toasts
- Fix the restore play queue prompt reappearing and queue changes being lost when playback stops
- Fix "No network available" warning in offline mode
- Fix TalkBack crash when drawer entries are hidden
- Fix the gap below the Now Playing title with three-button navigation
- Fix Chromecast, DLNA and Jukebox route selection when not scanning
- Fix the Authorization header being sent when disabled, breaking servers such as Nextcloud Music
- Fix crashes from duplicate entries in the play queue
- Fix launch crashes caused by a foreground service timeout
- Speed up queueing large playlists
