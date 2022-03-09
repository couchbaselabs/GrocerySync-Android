⚠️ This repo is obsolete.  It was built against a version of Couchbase Lite that reached end of life years ago.

## Grocery Sync for Couchbase Lite Android 

An example app that uses the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) mobile database framework.

This example code corresopnds to the master branch of Couchbase Lite Android.  
 
![](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png)

## Architecture

![](http://cl.ly/image/3c1k113o182b/GrocerySync.png)

If Couchbase Lite is configured to sync it's changes with a Sync Gateway, then all changes will automatically background sync bi-directionally such that any changes on either device will propagate to the other.

## Install Android Studio

* [Android Studio](http://developer.android.com/sdk/installing/studio.html) -- see the [Android Studio Compatibility Table](https://github.com/couchbase/couchbase-lite-android/blob/master/README.md#building-couchbase-lite-via-android-studio) to make sure the version you are using is supported.

## Screencasts

The following screencasts walk you through getting GrocerySync up and running:

* [GrocerySync - Couchbase Lite Android demo app: Part I](https://www.youtube.com/watch?v=9rWY2CrnFHw) -- get up and running (4m 35s)
* [GrocerySync - Couchbase Lite Android demo app: Part II](https://www.youtube.com/watch?v=rX9IPMBl780) -- run a local Sync Gateway (5m 47s)

The screencasts follow the instructions in the remainder of this document.

## Getting the code

```
$ git clone https://github.com/couchbaselabs/GrocerySync-Android.git
$ git submodule update --init
```

## Import the project in Android Studio

Follow the same instructions as importing [Couchbase Lite](https://github.com/couchbase/couchbase-lite-android#importing-project-into-android-studio)

## Run the app via Android Studio

* Run it using the "play" or "debug" buttons in the UI

Congratulations!  If you got this far and see [this UI](http://cl.ly/image/1H11131G2c3d/Screen%20Shot%202013-05-14%20at%204.44.48%20PM.png), you have your first Couchbase Lite Android app up and running.

## Install Sync Gateway

1. Download and install Sync Gateway (see the [installation page](http://developer.couchbase.com/documentation/mobile/current/installation/sync-gateway/index.html) for details).
2. Start Sync Gateway with the configuration file in the root of this project.

	```bash
	~/Downloads/couchbase-sync-gateway/bin/sync_gateway sync-gateway-config.json
	```

3. Open **MainActivity.java** and update the `SYNC_URL` constant to point to your Sync Gateway instance.

	```java
	public static final String SYNC_URL = "http://10.0.2.2:4984/grocery-sync";
	```

	- If you are using the Genymotion emulator: `http://10.0.3.2:4984/grocery-sync`
	- If you are using the standard android emulator: `http://10.0.2.2:4984/grocery-sync`
	- If you are running on a device: `http://<ip of sync gw>:4984/grocery-sync`

4. Build and run the app.
5. Add items and they should be visible on the Sync Gateway Admin UI at [http://localhost:4985/_admin/]
(http://localhost:4985/_admin/).

## Change the dependency from Maven -> Direct code dependency (optional)

By default, this project depends on the Couchbase Lite maven artifacts.  However, it can also depend on the CBLite code directly, which is useful if you need to modify the Couchbase Lite code.  (Note: if you are using maven artifacts, since we ship the source code artifacts, you should already be able to browse the code and debug)

See the build.gradle and settings.gradle files for instructions on how to do this.

## Run via Gradle (optional)

If you would rather run the project via the command line, you can do the following:

* Run the android emulator
* Run `./gradlew clean && ./gradlew installDebug`
* Switch to the emulator and you should have a new app called GrocerySync-Android
* Tap it to open the app

## Compile with submodules (Couchbase Lite from source code) (optional)

If you would rather compile the project with couchbase lite source codes, you can do the following:

* Open `sttings.gradle`, remove `/*` and `*/`.
* Open `build.gradle` in `GrocerySync-Android` folder, comment out `compile 'com.couchbase.lite:couchbase-lite-android:1.1.0'` and remove `//` for `compile project(':libraries:couchbase-lite-android')`, `compile project(':libraries:couchbase-lite-java-core')`, and `compile project(':libraries:couchbase-lite-java-native:sqlite-default')`.


# Additional Information

## Where to go from here: creating your own Couchbase-Lite app

See the [Getting Started Guide](http://developer.couchbase.com/mobile/develop/training/build-first-android-app/index.html).

## Support

See [Getting Help](https://github.com/couchbase/couchbase-lite-android#getting-help)
