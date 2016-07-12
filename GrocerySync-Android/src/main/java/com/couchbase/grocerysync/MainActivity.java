package com.couchbase.grocerysync;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements
        OnItemClickListener,
        OnItemLongClickListener,
        OnKeyListener {
    //constants
    public static final String designDocName = "grocery-local";
    public static final String byDateViewName = "byDate";

    //menu_main screen
    protected EditText addItemEditText;
    protected ListView itemListView;
    protected GrocerySyncArrayAdapter grocerySyncArrayAdapter;

    //couch internals
    private Database database;
    private LiveQuery liveQuery;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //connect items from layout
        addItemEditText = (EditText)findViewById(R.id.addItemEditText);
        itemListView = (ListView)findViewById(R.id.itemListView);

        //connect listeners
        addItemEditText.setOnKeyListener(this);

        try {
            startCBLite();
        } catch (Exception e) {
            Application application = (Application) getApplication();
            application.showMessage("Error initializing CBLite", e);
        }
    }

    protected void startCBLite() throws Exception {
        Application application = (Application) getApplication();
        database = application.getDatabase();

        com.couchbase.lite.View viewItemsByDate =
                database.getView(String.format("%s/%s", designDocName, byDateViewName));
        if (viewItemsByDate.getMap() == null) {
            viewItemsByDate.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    Object createdAt = document.get("created_at");
                    if (createdAt != null) {
                        emitter.emit(createdAt.toString(), null);
                    }
                }
            }, "1.0");
        }

        initItemListAdapter();

        startLiveQuery(viewItemsByDate);
    }

    private void startLiveQuery(com.couchbase.lite.View view) throws Exception {
        final ProgressDialog progressDialog = showLoadingSpinner();

        if (liveQuery == null) {
            liveQuery = view.createQuery().toLiveQuery();
            liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
                public void changed(final LiveQuery.ChangeEvent event) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            grocerySyncArrayAdapter.clear();
                            for (Iterator<QueryRow> it = event.getRows(); it.hasNext();) {
                                grocerySyncArrayAdapter.add(it.next());
                            }
                            grocerySyncArrayAdapter.notifyDataSetChanged();
                            progressDialog.dismiss();
                        }
                    });
                }
            });

            liveQuery.start();
        }
    }

    private void initItemListAdapter() {
        grocerySyncArrayAdapter = new GrocerySyncArrayAdapter(
                getApplicationContext(),
                R.layout.grocery_list_item,
                R.id.label,
                new ArrayList<QueryRow>()
        );
        itemListView.setAdapter(grocerySyncArrayAdapter);
        itemListView.setOnItemClickListener(MainActivity.this);
        itemListView.setOnItemLongClickListener(MainActivity.this);
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
                    if (inputText.contains(":")) {  // hack to create multiple items
                        int numCreated = createMultipleGrocerySyncItems(inputText);
                        String msg = String.format("Created %d new grocery items!", numCreated);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    } else {
                        createGroceryItem(inputText);
                        Toast.makeText(getApplicationContext(), "Created new grocery item!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error creating document, see logs for details", Toast.LENGTH_LONG).show();
                    Log.e(Application.TAG, "Error creating document.", e);
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
        QueryRow row = (QueryRow) adapterView.getItemAtPosition(position);
        Document document = row.getDocument();
        Map<String, Object> newProperties = new HashMap<String, Object>(document.getProperties());

        boolean checked = ((Boolean) newProperties.get("check")).booleanValue();
        newProperties.put("check", !checked);

        try {
            document.putProperties(newProperties);
            grocerySyncArrayAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Application application = (Application) getApplication();
            application.showMessage("Error updating database", e);
        }
    }

    /**
     * Handle long-click on item in list
     */
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        QueryRow row = (QueryRow) adapterView.getItemAtPosition(position);
        final Document clickedDocument = row.getDocument();
        String itemText = (String) clickedDocument.getCurrentRevision().getProperty("text");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog alert = builder.setTitle("Delete Item?")
                .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            clickedDocument.delete();
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Error deleting document, see logs for details", Toast.LENGTH_LONG).show();
                            Log.e(Application.TAG, "Error deleting document", e);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_main_logout:
                logout();
                return true;
        }
        return false;
    }

    private Document createGroceryItem(String text) throws Exception {
        Application application = (Application) getApplication();

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        UUID uuid = UUID.randomUUID();
        Calendar calendar = GregorianCalendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        String id = currentTime + "-" + uuid.toString();

        Document document = database.createDocument();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("_id", id);
        properties.put("text", text);
        properties.put("check", Boolean.FALSE);
        properties.put("owner", application.getUsername());
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

        Log.d(Application.TAG, "Created new grocery item with id: %s", document.getId());

        return document;
    }

    private int createMultipleGrocerySyncItems(String text) throws Exception {
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

    private void logout() {
        Application application = (Application) getApplication();
        application.logout();
    }
}
