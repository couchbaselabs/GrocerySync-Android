## Grocery Sync for Couchbase Lite Android 

An example app that uses the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.

This example code corresopnds to the master branch of Couchbase Lite Android.  If you are looking for a version that corresponds with the beta2 version, you should checkout the [1.0-beta2](https://github.com/couchbaselabs/GrocerySync-Android/tree/1.0-beta2) tag.
 
![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)
 
 
## Architecture

![](http://cl.ly/image/3c1k113o182b/GrocerySync.png)

If Couchbase Lite is configured to sync it's changes with a Sync Gateway, then all changes will automatically background sync bi-directionally such that any changes on either device will propagate to the other.

## Prequisites

* Install [Android Studio](http://developer.android.com/sdk/installing/studio.html)
* (optional) Install [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) to use the sync feature.

## Android Studio Version

* If you are using the stable branch of GrocerySync, use the latest version of Android Studio from the stable channel (currently Android Studio 0.3.X)

* If you are using the master branch of GrocerySync, use the latest version of Android Studio from the canary channel (currently Android Studio 0.4.X)

## Getting the code

```
$ git clone git@github.com:couchbaselabs/GrocerySync-Android.git
```

## Import the project in Android Studio

* Choose File / Import project
* Check "Auto-import"
* Leave "Use default gradle wrapper"
* After your open the project, it should look like [this](http://cl.ly/image/2E3T1T2q261E), and the imports should be ok, as shown [here](http://cl.ly/image/2m1a1K3n0c1V)

## Enable Android Support Repository and Google Repository

Open the Android SDK from Android Studio (Tools->Android->SDK Manager) and make sure that the Android Support Repository and Google Repository items are installed.  

(This may be enabled by default, but it's good to double check since it's a required dependency in order to get the android support library: 'com.android.support:support-v4:13.0.0' )


## Configuring a remote sync gateway (optional)

GrocerySync can be configured to do a two way sync all of its data to a Sync Gateway instance, so it needs a valid URL.

* Configure the hardcoded `SYNC_URL` in the MainActivity.java file to the URL of your Sync Gateway instance.  
    - If you are using the Genymotion emulator: `http://10.0.3.2:4984/grocery-sync`
    - If you are using the standard android emulator: `http://10.0.2.2:4984/grocery-sync`
    - If you are running on a device: `http://<ip of sync gw>:4984/grocery-sync`
* Create a DB named `grocery-sync` on the Sync Gateway instance.
* Use this [config.json](https://gist.github.com/tleyden/11154924)
 

## Run the app via Android Studio

* Run it using the "play" or "debug" buttons in the UI

## Run via Gradle

* Run the android emulator
* Run `./gradlew clean && ./gradlew installDebug`
* Switch to the emulator and you should have a new app called GrocerySync-Android
* Tap it to open the app

## Change the dependency from Maven -> Direct code dependency

By default, this project depends on the Couchbase Lite maven artifacts.  However, it can also depend on the CBLite code directly, which is useful if you want to debug into the CBLIte code (or just browse the code).

See the build.gradle and settings.gradle files for instructions on how to do this.

## Where to go from here: creating your own Couchbase-Lite app

See the [Getting Started Guide](https://github.com/couchbase/couchbase-lite-android/wiki/Getting-Started).

## Deviations from the iOS version
 
Android typically uses a long-click to trigger additional action, as opposed to swipe-to-delete, so this convention was followed.
 
## Known Issues
 
We currently do not handle the Sync URL changing at runtime (if you change it you have to restart the app)

## Troubleshooting

### Configure Android SDK location

Gradle (the build system used by Studio) needs to know where your Android SDK is, otherwise it won't be able to build anything.

* First the local.properties file must be created so that Android knows where your SDK is: `$ cp local.properties.example local.properties`.
* If you are on OSX and installed Android Studio to the default location, you should be ok with the defaults in `local.properties`
* Otherwise, open `local.properties` and make sure it points to the Android SDK on your system.  Change the path as needed.

