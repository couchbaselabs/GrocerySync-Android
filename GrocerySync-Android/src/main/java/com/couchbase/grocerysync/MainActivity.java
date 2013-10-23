package com.couchbase.grocerysync;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.couchbase.cblite.CBLManager;
import com.couchbase.cblite.CBLMapEmitFunction;
import com.couchbase.cblite.CBLMapFunction;
import com.couchbase.cblite.CBLNewRevision;
import com.couchbase.cblite.CBLQuery;
import com.couchbase.cblite.CBLQueryEnumerator;
import com.couchbase.cblite.CBLQueryRow;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.CBLiteException;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, OnKeyListener {

    public static String TAG = "GrocerySync";

    //constants
    public static final String DATABASE_NAME = "grocery-sync";
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
    protected static CBLManager manager;
    private CBLDatabase db;

    // GestureDetector gestureDetector;

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


        initTouchListener();

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
            e.printStackTrace();
        }

    }

    private void initTouchListener() {
        /*TouchListener onTouchListener = new TouchListener();
        gestureDetector = new GestureDetector(this, new GestureListener());
        itemListView.setOnTouchListener(onTouchListener);*/
    }

    protected void onDestroy() {
        Log.v(TAG, "onDestroy");


        if(manager != null) {
            manager.close();
        }

        super.onDestroy();
    }

    protected void startCBLite() throws CBLiteException {
        manager = new CBLManager(getApplicationContext(), "grocery-sync");

        //install a view definition needed by the application
        db = manager.getDatabase(DATABASE_NAME);
        CBLView view = db.getView(String.format("%s/%s", dDocName, byDateViewName));
        view.setMap(new CBLMapFunction() {
            @Override
            public void map(Map<String, Object> document, CBLMapEmitFunction emitter) {
                Object createdAt = document.get("created_at");
                if(createdAt != null) {
                    emitter.emit(createdAt.toString(), document);
                }
            }
        }, "1.0");

        fillList(view);

    }

    private void fillList(CBLView view) throws CBLiteException {
        CBLQuery viewQuery = view.createQuery();

        // TODO: this isn't ideal .. it should be using a LiveQuery here
        CBLQueryEnumerator queryEnumerator = viewQuery.getRows();
        List<CBLQueryRow> rows = new ArrayList<CBLQueryRow>();
        for (Iterator<CBLQueryRow> it = queryEnumerator; it.hasNext();) {
            CBLQueryRow row = it.next();
            rows.add(row);
        }

        itemListViewAdapter = new GrocerySyncListAdapter(getApplicationContext(), R.layout.grocery_list_item, R.id.label, rows);
        itemListView.setAdapter(itemListViewAdapter);
        itemListView.setOnItemClickListener(MainActivity.this);
        itemListView.setOnItemLongClickListener(MainActivity.this);
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
                    CBLDocument groceryItemDoc = createGroceryItem(inputText);
                    itemListViewAdapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Created new grocery item!", Toast.LENGTH_LONG).show();

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        CBLQueryRow row = (CBLQueryRow) parent.getItemAtPosition(position);
        CBLDocument document = row.getDocument();
        Map<String, Object> curProperties = document.getUserProperties();
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
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        CBLQueryRow row = (CBLQueryRow) parent.getItemAtPosition(position);
        final CBLDocument clickedDocument = row.getDocument();
        String itemText = (String) clickedDocument.getCurrentRevision().getProperty("text");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog alert = builder.setTitle("Delete Item?")
                .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            clickedDocument.delete();
                            itemListViewAdapter.notifyDataSetChanged();
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

    public CBLDocument createGroceryItem(String text) throws CBLiteException {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        UUID uuid = UUID.randomUUID();
        Calendar calendar = GregorianCalendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        String id = currentTime + "-" + uuid.toString();

        CBLDocument document = db.createUntitledDocument();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("_id", id);
        properties.put("text", text);
        properties.put("check", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

        return document;
    }


    /*
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
    */

}
