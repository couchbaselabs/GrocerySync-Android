## Deviations from the iOS version

Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.

## Known Issues

1.  Artifacts from iOS were used, may need to be updated to look nice on other devices

2.  The intial triggering of push/pull sync is not asynchronous (unclear if this is a problem as it seems to run in the background)

3.  Unlike the iOS version we have to handle the Sync URL changing at runtime (not currently handled, if you change it, exit the app and restart)

4.  List view is not ordered properly

