## Grocery Sync for Couchbase Lite Android 
 
![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)
 
This is a simple example of using the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.
 
The "use case" is a shared grocery list where all devices using the application would see a mirror of the grocery list.  Any changes will automatically background sync with a Sync Gateway running in the cloud.  (bi-directional)
 
## Prequisites

* Install [Android Studio](http://developer.android.com/sdk/installing/studio.html) version 0.1.3 or later with Android Support Repository and Google Repository.
* (optional) Install [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) to use the sync feature.

## Getting the code

```
$ git clone git@github.com:couchbaselabs/GrocerySync-Android.git
```

_Note_: it is important to leave the directory name as the default, otherwise it could confuse the Android Studio IDE

## Configure Android SDK location

Gradle (the build system used by Studio) needs to know where your Android SDK is, otherwise it won't be able to build anything.

* First the local.properties file must be created so that Android knows where your SDK is: `$ cp local.properties.example local.properties`. 
* If you are on OSX and installed Android Studio to the default location, you should be ok with the defaults in `local.properties`
* Otherwise, open `local.properties` and make sure it points to the Android SDK on your system.  Change the path as needed.


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

* Configure the hardcoded DATABASE_URL in the MainActivity.java file to the URL of your Sync Gateway instance.  (there is also a value in the Settings that can be used, which will override the hardcoded default)  
* Create a DB named `grocery-test` on the Sync Gateway instance.

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
