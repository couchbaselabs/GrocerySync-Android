## Grocery Sync for Couchbase Lite Android 
 
 
![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)
 
This is a simple example of using the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.
 
The "use case" is a shared grocery list where all devices using the application would see a mirror of the grocery list.  Any changes will automatically background sync with a CouchDB or [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) running in the cloud.  (bi-directional)
 
## Prequisites

* Install Android Studio 

## Getting the code

* Do a `git clone` on the repository URL

## Building via Android Studio
 
* Open the project in Android Studio
* Under Build menu, choose the Make Project option

## Building via Gradle

* Run `./gradlew build`

## Configuring

* Configure the DB URL's in MainActivity to the URL of your CouchDB instance (or [Sync Gateway](https://github.com/couchbaselabs/sync_gateway)).  
* Create a DB named `grocery-test` if it doesn't already exist.

## Run via Android Studio UI

* Run it using the "play" or "debug" buttons in the UI

## Run via Gradle

* Run the android emulator
* Run `./gradlew installDebug`
* Switch to the emulator and you should have a new app called GrocerySync-Android
* Tap it to open the app

## Creating your own Couchbase-Lite app

See the [Getting Started Guide](https://github.com/couchbase/couchbase-lite-android/wiki/Getting-Started).

## Deviations from the iOS version
 
Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.
 
## Known Issues
 
We currently do not handle the Sync URL changing at runtime (if you change it you have to restart the app)