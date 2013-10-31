package com.couchbase.grocerysync;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.cblite.CBLDatabase;
import com.couchbase.cblite.CBLDocument;
import com.couchbase.cblite.CBLLiveQuery;
import com.couchbase.cblite.CBLLiveQueryChangedFunction;
import com.couchbase.cblite.CBLManager;
import com.couchbase.cblite.CBLMapEmitFunction;
import com.couchbase.cblite.CBLMapFunction;
import com.couchbase.cblite.CBLQuery;
import com.couchbase.cblite.CBLQueryEnumerator;
import com.couchbase.cblite.CBLQueryRow;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.CBLiteException;
import com.couchbase.cblite.replicator.CBLPuller;
import com.couchbase.cblite.replicator.CBLReplicator;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, OnKeyListener {

    public static String TAG = "GrocerySync";

    //constants
    public static final String DATABASE_NAME = "grocery-sync";
    public static final String designDocName = "grocery-local";
    public static final String byDateViewName = "byDate";
    public static final String SYNC_URL = "http://10.0.2.2:4984/grocery-sync";  // 10.0.2.2 == Android Simulator equivalent of 127.0.0.1

    //splash screen
    protected SplashScreenDialog splashDialog;

    //main screen
    protected EditText addItemEditText;
    protected ListView itemListView;
    protected GrocerySyncListAdapter itemListViewAdapter;

    //couch internals
    protected static CBLManager manager;
    private CBLDatabase database;
    private CBLLiveQuery liveQuery;


    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //connect items from layout
        addItemEditText = (EditText)findViewById(R.id.addItemEditText);
        itemListView = (ListView)findViewById(R.id.itemListView);

        //connect listeners
        addItemEditText.setOnKeyListener(this);

        //show splash and start couch
        showSplashScreen();
        removeSplashScreen();

        try {
            startCBLite();
        } catch (CBLiteException e) {
            Toast.makeText(getApplicationContext(), "Error Initializing CBLIte, see logs for details", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error initializing CBLite", e);
        }

    }


    protected void onDestroy() {
        Log.v(TAG, "onDestroy");

        if(manager != null) {
            manager.close();
        }

        super.onDestroy();
    }

    protected void startCBLite() throws CBLiteException {

        manager = new CBLManager(getApplicationContext().getFilesDir());

        //install a view definition needed by the application
        database = manager.getDatabase(DATABASE_NAME);
        CBLView viewItemsByDate = database.getView(String.format("%s/%s", designDocName, byDateViewName));
        viewItemsByDate.setMap(new CBLMapFunction() {
            @Override
            public void map(Map<String, Object> document, CBLMapEmitFunction emitter) {
                Object createdAt = document.get("created_at");
                if (createdAt != null) {
                    emitter.emit(createdAt.toString(), null);
                }
            }
        }, "1.0");

        startLiveQuery(viewItemsByDate);

        startSync();



    }

    private void startSync() {

        URL syncUrl;
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        CBLReplicator pullReplication = database.getPullReplication(syncUrl);
        pullReplication.setContinuous(true);

        CBLReplicator pushReplication = database.getPushReplication(syncUrl);
        pushReplication.setContinuous(true);

        pullReplication.start();
        pushReplication.start();


    }

    private void startLiveQuery(CBLView view) throws CBLiteException {

        final ProgressDialog progressDialog = showLoadingSpinner();

        if (liveQuery == null) {

            liveQuery = view.createQuery().toLiveQuery();

            liveQuery.addChangeListener(new CBLLiveQueryChangedFunction() {

                @Override
                public void onLiveQueryChanged(CBLQueryEnumerator queryEnumerator) {

                    final List<CBLQueryRow> rows = getRowsFromQueryEnumerator(queryEnumerator);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            itemListViewAdapter = new GrocerySyncListAdapter(
                                    getApplicationContext(),
                                    R.layout.grocery_list_item,
                                    R.id.label,
                                    rows
                            );
                            itemListView.setAdapter(itemListViewAdapter);
                            itemListView.setOnItemClickListener(MainActivity.this);
                            itemListView.setOnItemLongClickListener(MainActivity.this);

                            progressDialog.dismiss();

                        }
                    });

                }

                @Override
                public void onFailureLiveQueryChanged(CBLiteException exception) {
                    Toast.makeText(getApplicationContext(), "An internal error occurred running query, see logs for details", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error running query", exception);
                }
            });

            liveQuery.start();

        }


    }

    private List<CBLQueryRow> getRowsFromQueryEnumerator(CBLQueryEnumerator queryEnumerator) {
        List<CBLQueryRow> rows = new ArrayList<CBLQueryRow>();
        for (Iterator<CBLQueryRow> it = queryEnumerator; it.hasNext();) {
            CBLQueryRow row = it.next();
            rows.add(row);
        }
        return rows;
    }

    private ProgressDialog showLoadingSpinner() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.show();
        return progress;
    }


    /**
     * Handle typing item text
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER)) {

            String inputText = addItemEditText.getText().toString();
            if(!inputText.equals("")) {
                try {

                    if (inputText.contains(":")) {
                        int numCreated = createMultipleGrocerySyncItems(inputText);
                        String msg = String.format("Created %d new grocery items!", numCreated);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    }
                    else {
                        createGroceryItem(inputText);
                        Toast.makeText(getApplicationContext(), "Created new grocery item!", Toast.LENGTH_LONG).show();
                    }

                } catch (CBLiteException e) {
                    Toast.makeText(getApplicationContext(), "Error creating document, see logs for details", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error creating document", e);
                }
            }
            addItemEditText.setText("");
            return true;
        }
        return false;
    }


    /**
     * Handle click on item in list
     */
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        CBLQueryRow row = (CBLQueryRow) adapterView.getItemAtPosition(position);
        CBLDocument document = row.getDocument();
        Map<String, Object> curProperties = document.getProperties();
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.putAll(curProperties);

        boolean checked = ((Boolean) newProperties.get("check")).booleanValue();
        newProperties.put("check", !checked);

        try {
            document.putProperties(newProperties);
            itemListViewAdapter.notifyDataSetChanged();
        } catch (CBLiteException e) {
            Toast.makeText(getApplicationContext(), "Error updating database, see logs for details", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error updating database", e);
        }

    }

    /**
     * Handle long-click on item in list
     */
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        CBLQueryRow row = (CBLQueryRow) adapterView.getItemAtPosition(position);
        final CBLDocument clickedDocument = row.getDocument();
        String itemText = (String) clickedDocument.getCurrentRevision().getProperty("text");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog alert = builder.setTitle("Delete Item?")
                .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            clickedDocument.delete();
                        } catch (CBLiteException e) {
                            Toast.makeText(getApplicationContext(), "Error deleting document, see logs for details", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error deleting document", e);
                        }
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
    private void showSplashScreen() {
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

    private CBLDocument createGroceryItem(String text) throws CBLiteException {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        UUID uuid = UUID.randomUUID();
        Calendar calendar = GregorianCalendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        String id = currentTime + "-" + uuid.toString();

        CBLDocument document = database.createDocument();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("_id", id);
        properties.put("text", text);
        properties.put("check", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

        return document;
    }

    private int createMultipleGrocerySyncItems(String text) throws CBLiteException {
        StringTokenizer st = new StringTokenizer(text, ":");
        String itemText = st.nextToken();
        String numItemsString = st.nextToken();
        int numItems = Integer.parseInt(numItemsString);
        for (int i = 0; i < numItems; i++) {
            String curItemText = String.format("%s-%d", itemText, i);
            createGroceryItem(curItemText);
        }
        return numItems;
    }


}
