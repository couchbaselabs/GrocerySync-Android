
## Grocery Sync for Couchbase Lite Android 


![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)

This is a simple example of using the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.

The "use case" is a shared grocery list where all devices using the application would see a mirror of the grocery list.  Any changes will automatically background sync with a CouchDB or [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) running in the cloud.  (bi-directional)

## Prerequisites

* You must have the Android SDK installed (which bundles Eclipse Juno).  Here is the page to [Download the Android SDK](http://developer.android.com/sdk/index.html)

## Running

* Do a `git clone` on this repository
* Open the Android SDK (Eclipse)
* Choose the File/Import menu
* Choose Android/Existing Android Code into Workspace
* Hit Browse.. button, and navigate to GrocerySync-Android directory cloned in earlier step.
* Hit "Finish"  
* In Package Explorer, navigate to AndroidGrocerySync/src
* Right-click com.couchbase.grocerysync package, choose "Run as .." / "Android Application"
* If the emulator shows a lock icon, hit fn + uparrow to unlock it.  

This should start an Android device emulator and start the GrocerySync app.

## Point it to a custom DB

By default, it will point to a shared database that has a lot of junk data in it.  Here are the instructions to point it to your own DB.

In AndroidGrocerySyncActivity, change all instances of the URL from `http://mschoch.iriscouch.com/grocery-test` to one of the following:

* The URL of your CouchDB instance 
* The URL of your [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) instance  

In either case, you will need to make sure there is a database named "grocery-test" on the server.


## Deviations from the iOS version

Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.

## Known Issues

We currently do not handle the Sync URL changing at runtime (if you change it you have to restart the app)

