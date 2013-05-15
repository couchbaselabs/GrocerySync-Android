
## Grocery Sync for Couchbase Lite Android 


![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)

This is a simple example of using the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.

The "use case" is a shared grocery list where all devices using the application would see a mirror of the grocery list.  Any changes will automatically background sync with a CouchDB or [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) running in the cloud.  (bi-directional)


## Running

* Do a `git clone` on the repository
* Open the project in Eclipse
* Right-click com.couchbase.grocerysync package, choose "Run as .." / "Android Application"

## Point it to a custom DB

In AndroidGrocerySyncActivity, change the URL from `http://mschoch.iriscouch.com/grocery-test` to the URL of your CouchDB instance (or [Sync Gateway](https://github.com/couchbaselabs/sync_gateway))

## Deviations from the iOS version

Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.

## Known Issues

We currently do not handle the Sync URL changing at runtime (if you change it you have to restart the app)

