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

# Optional steps

## Configuring a custom sync gateway (optional)

By default, GrocerySync is configured to sync to the Couchbase Mobile demo server.  If you want to point it to your own Sync Gateway, follow these instructions.

**Run Sync Gateway**

* Download Sync Gateway via [downloading pre-built binaries](http://www.couchbase.com/nosql-databases/downloads#Couchbase_Mobile).  Alternatively, you can [buildi it from source](https://github.com/couchbase/sync_gateway).  These instructions will assume you are using the pre-built binary.
* Untar/gz the file, you should end up with a `couchbase-sync-gateway` directory
* `cd couchbase-sync-gateway`
* `$ curl -o config.json https://raw.githubusercontent.com/couchbaselabs/GrocerySync-Android/master/docs/sync_gw_config.json` - this will download the Sync Gateway config that is suitable for Grocery Sync.
* Run Sync Gateway via `$./bin/sync_gateway config.json`

You should see the following output:

```
14:19:27.869724 Enabling logging: [CRUD+ REST+ Changes+ Attach+]
14:19:27.869785 ==== Couchbase Sync Gateway/1.0.3(81;fa9a6e7) ====
14:19:27.869800 Configured Go to use all 8 CPUs; setenv GOMAXPROCS to override this
14:19:27.869815 Opening db /grocery-sync as bucket "grocery-sync", pool "default", server <walrus:>
``` 

*If you are wondering why you don't need to download Couchbase Server -- Sync Gateway comes with an in-memory database called Walrus, which is good enough for testing.  In order to deploy it to production, you will need to use Couchbase Server as the backing store*

**Configure Grocery Sync with Sync Gateway URL**

* Configure the hardcoded `SYNC_URL` in the MainActivity.java file to the URL of your Sync Gateway instance.  
    - If you are using the Genymotion emulator: `http://10.0.3.2:4984/grocery-sync`
    - If you are using the standard android emulator: `http://10.0.2.2:4984/grocery-sync`
    - If you are running on a device: `http://<ip of sync gw>:4984/grocery-sync`

**Run Grocery Sync**

After the above steps, if you run GrocerySync your data will sync with your local Sync Gateway.  

To verify the data is being pushed from the app to Sync Gateway:

* In the Sync Gateway terminal, you should see entries like:

```
14:22:25.518106 HTTP:  #005: POST /grocery-sync/_revs_diff
14:22:25.544508 HTTP:  #006: POST /grocery-sync/_bulk_docs
```

* You can use the [Sync Gateway REST API](http://developer.couchbase.com/mobile/develop/references/sync-gateway/rest-api/index.html) to view the documents in Sync Gateway.



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
