package com.couchbase.grocerysync;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult.Row;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.cblite.CBLDatabase;
import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.CBLViewMapBlock;
import com.couchbase.cblite.CBLViewMapEmitBlock;
import com.couchbase.cblite.ektorp.CBLiteHttpClient;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, OnKeyListener {

    public static String TAG = "GrocerySync";

    //constants
    public static final String DATABASE_NAME = "how2db";
    public static final String dDocName = "grocery-local";
    public static final String dDocId = "_design/" + dDocName;
    public static final String byDateViewName = "byDate";
    public static final String DATABASE_URL = "http://sync.couchbasecloud.com";  // 10.0.2.2 == Android Simulator equivalent of 127.0.0.1

    //splash screen
    protected SplashScreenDialog splashDialog;

    //main screen
    protected EditText addItemEditText;
    protected ListView itemListView;
    protected GrocerySyncListAdapter itemListViewAdapter;

    //couch internals
    protected static CBLServer server;
    protected static HttpClient httpClient;

    //ektorp impl
    protected CouchDbInstance dbInstance;
    protected CouchDbConnector couchDbConnector;
    protected ReplicationCommand pushReplicationCommand;
    protected ReplicationCommand pullReplicationCommand;

    GestureDetector gestureDetector;

    //static inializer to ensure that touchdb:// URLs are handled properly
    {
        CBLURLStreamHandlerFactory.registerSelfIgnoreError();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //connect items from layout
        addItemEditText = (EditText)findViewById(R.id.addItemEditText);
        itemListView = (ListView)findViewById(R.id.itemListView);
        TouchListener onTouchListener = new TouchListener();
        gestureDetector = new GestureDetector(this, new GestureListener());
        itemListView.setOnTouchListener(onTouchListener);

        //connect listeners
        addItemEditText.setOnKeyListener(this);

        //show splash and start couch
        showSplashScreen();
        removeSplashScreen();
        startCBLite();
        startEktorp();

    }

    protected void onDestroy() {
        Log.v(TAG, "onDestroy");

        //need to stop the async task thats following the changes feed
        itemListViewAdapter.cancelContinuous();

        //clean up our http client connection manager
        if(httpClient != null) {
            httpClient.shutdown();
        }

        if(server != null) {
            server.close();
        }

        super.onDestroy();
    }

    protected void startCBLite() {
        String filesDir = getFilesDir().getAbsolutePath();
        try {
            server = new CBLServer(filesDir);
        } catch (IOException e) {
            Log.e(TAG, "Error starting TDServer", e);
        }

        //install a view definition needed by the application
        CBLDatabase db = server.getDatabaseNamed(DATABASE_NAME);
        CBLView view = db.getViewNamed(String.format("%s/%s", dDocName, byDateViewName));
        view.setMapReduceBlocks(new CBLViewMapBlock() {

            @Override
            public void map(Map<String, Object> document, CBLViewMapEmitBlock emitter) {
                Object createdAt = document.get("created_at");
                if(createdAt != null) {
                    emitter.emit(createdAt.toString(), document);
                }

            }
        }, null, "1.0");
    }

    protected void startEktorp() {
        Log.v(TAG, "starting ektorp");

        if(httpClient != null) {
            httpClient.shutdown();
        }

        httpClient = new CBLiteHttpClient(server);
        dbInstance = new StdCouchDbInstance(httpClient);

        GrocerySyncEktorpAsyncTask startupTask = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                couchDbConnector = dbInstance.createConnector(DATABASE_NAME, true);
            }

            @Override
            protected void onSuccess() {
                //attach list adapter to the list and handle clicks
                ViewQuery viewQuery = new ViewQuery().designDocId(dDocId).viewName(byDateViewName).descending(true);
                itemListViewAdapter = new GrocerySyncListAdapter(MainActivity.this, couchDbConnector, viewQuery);
                itemListView.setAdapter(itemListViewAdapter);
                itemListView.setOnItemClickListener(MainActivity.this);
                itemListView.setOnItemLongClickListener(MainActivity.this);

                startReplications();
            }
        };
        startupTask.execute();
    }

    public void startReplications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String defaultDatabaseUrl = DATABASE_URL + "/" + DATABASE_NAME;

        pushReplicationCommand = new ReplicationCommand.Builder()
                .source(DATABASE_NAME)
                .target(prefs.getString("sync_url", defaultDatabaseUrl))
                .continuous(true)
                .build();

        GrocerySyncEktorpAsyncTask pushReplication = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                dbInstance.replicate(pushReplicationCommand);
            }
        };

        pushReplication.execute();

        pullReplicationCommand = new ReplicationCommand.Builder()
                .source(prefs.getString("sync_url", defaultDatabaseUrl))
                .target(DATABASE_NAME)
                .continuous(true)
                .build();

        GrocerySyncEktorpAsyncTask pullReplication = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                dbInstance.replicate(pullReplicationCommand);
            }
        };

        pullReplication.execute();
    }

    public void stopEktorp() {
    }


    /**
     * Handle typing item text
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER)) {

            String inputText = addItemEditText.getText().toString();
            if(!inputText.equals("")) {
                createGroceryItem(inputText);
            }
            addItemEditText.setText("");
            return true;
        }
        return false;
    }

    /**
     * Handle click on item in list
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Row row = (Row)parent.getItemAtPosition(position);
        JsonNode item = row.getValueAsNode();
        toggleItemChecked(item);
    }

    /**
     * Handle long-click on item in list
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Row row = (Row)parent.getItemAtPosition(position);
        final JsonNode item = row.getValueAsNode();
        JsonNode textNode = item.get("text");
        String itemText = "";
        if(textNode != null) {
            itemText = textNode.getTextValue();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog alert = builder.setTitle("Delete Item?")
                .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteGroceryItem(item);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle Cancel
                    }
                })
                .create();

        alert.show();

        return true;
    }

    /**
     * Removes the Dialog that displays the splash screen
     */
    protected void removeSplashScreen() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
        }
    }

    /**
     * Shows the splash screen over the full Activity
     */
    protected void showSplashScreen() {
        splashDialog = new SplashScreenDialog(this);
        splashDialog.show();
    }

    /**
     * Add settings item to the menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "Settings");
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Launch the settings activity
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                startActivity(new Intent(this, GrocerySyncPreferencesActivity.class));
                return true;
        }
        return false;
    }

    public void createGroceryItem(String name) {
        final JsonNode item = GroceryItemUtils.createWithText(name);
        GrocerySyncEktorpAsyncTask createItemTask = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                couchDbConnector.create(item);
            }

            @Override
            protected void onSuccess() {
                Log.d(TAG, "Document created successfully.  item: " + item.toString());
            }

            @Override
            protected void onUpdateConflict(
                    UpdateConflictException updateConflictException) {
                Log.d(TAG, "Got an update conflict for: " + item.toString());
            }
        };
        createItemTask.execute();
    }

    public void toggleItemChecked(final JsonNode item) {
        GroceryItemUtils.toggleCheck(item);

        GrocerySyncEktorpAsyncTask updateTask = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                couchDbConnector.update(item);
            }

            @Override
            protected void onSuccess() {
                Log.d(TAG, "Document updated successfully");
            }

            @Override
            protected void onUpdateConflict(
                    UpdateConflictException updateConflictException) {
                Log.d(TAG, "Got an update conflict for: " + item.toString());
            }
        };
        updateTask.execute();
    }

    public void deleteGroceryItem(final JsonNode item) {
        GrocerySyncEktorpAsyncTask deleteTask = new GrocerySyncEktorpAsyncTask() {

            @Override
            protected void doInBackground() {
                couchDbConnector.delete(item);
            }

            @Override
            protected void onSuccess() {
                Log.d(TAG, "Document deleted successfully");
            }

            @Override
            protected void onUpdateConflict(
                    UpdateConflictException updateConflictException) {
                Log.d(TAG, "Got an update conflict for: " + item.toString());
            }
        };
        deleteTask.execute();
    }


    protected class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            if (gestureDetector.onTouchEvent(e)){
                return true;
            }else{
                return false;
            }
        }

    }

    protected class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        private static final int SWIPE_MIN_DISTANCE = 150;
        private static final int SWIPE_MAX_OFF_PATH = 100;
        private static final int SWIPE_THRESHOLD_VELOCITY = 100;

        private MotionEvent mLastOnDownEvent = null;


        @Override
        public boolean onDown(MotionEvent e)
        {
            mLastOnDownEvent = e;
            return super.onDown(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if(e1 == null){
                e1 = mLastOnDownEvent;
            }
            if(e1==null || e2==null){
                return false;
            }

            float dX = e2.getX() - e1.getX();
            float dY = e1.getY() - e2.getY();

            int itemId = itemListView.pointToPosition((int) e1.getX(),(int) e1.getY());
            Row row = (Row)itemListView.getAdapter().getItem(itemId);
            final JsonNode item = row.getValueAsNode();
            JsonNode itemText = item.get("text");
            String text = (String) itemText.getTextValue();

            GrocerySyncEktorpAsyncTask createItemTask = new GrocerySyncEktorpAsyncTask() {

                @Override
                protected void doInBackground() {
                    couchDbConnector.delete(item);
                }

                @Override
                protected void onSuccess() {
                    itemListViewAdapter.notifyDataSetChanged();
                }

                @Override
                protected void onUpdateConflict(
                        UpdateConflictException updateConflictException) {
                    Log.d(TAG, "Got an update conflict for: " + item.toString());
                }
            };
            createItemTask.execute();



            if (Math.abs(dY) < SWIPE_MAX_OFF_PATH && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dX) >= SWIPE_MIN_DISTANCE ) {
                if (dX > 0) {
                    Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            else if (Math.abs(dX) < SWIPE_MAX_OFF_PATH && Math.abs(velocityY)>=SWIPE_THRESHOLD_VELOCITY && Math.abs(dY)>=SWIPE_MIN_DISTANCE ) {
                if (dY>0) {
                    Toast.makeText(getApplicationContext(), "Up Swipe", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Down Swipe", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }
    }

}
