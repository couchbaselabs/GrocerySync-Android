## Prerequisites

1.  Must have Android-Couchbase imported into the workspace

## Step

1.  git clone the repo and add it to your Eclipse workspace
2.  Check that the Couchbase-Android library reference is correct (right-click on the project, click on the Android tab and look for a green check in the library section)
3.  Clean the project after importing to regenerate files

## Deviations from the iOS version

Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.

## Known Issues

1.  Artifacts from iOS were used, may need to be updated to look nice on other devices

2.  The following actions are not asynchronous and cause the app to hang
  - Initial Push/Pull syncs 
  - Add Item
  - Toggle Check Item
  - Delete Item

  (In case you're wonder what actually IS asynchronous, the table updates off of the changes feed off the main thread)

3.  Unlike the iOS version we have to handle the Sync URL changing at runtime (not currently handled, if you change it, exit the app and restart)

4.  List view is not ordered properly

5.  Completely untested on real device :)
