
## Grocery Sync for [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android)

![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)

## Running

* Open the project in Eclipse
* Right-click com.couchbase.grocerysync package, choose "Run as .." / "Android Application"

## Point it to a custom DB

In AndroidGrocerySyncActivity, change the URL from `http://mschoch.iriscouch.com/grocery-test` to the URL of your CouchDB (or Sync Gateway)

## Deviations from the iOS version

Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.

## Known Issues

We currently do not handle the Sync URL changing at runtime (if you change it you have to restart the app)

