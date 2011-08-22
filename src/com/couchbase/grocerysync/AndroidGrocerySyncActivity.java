package com.couchbase.grocerysync;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
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

import com.couchbase.libcouch.CouchbaseMobile;
import com.couchbase.libcouch.ICouchClient;

public class AndroidGrocerySyncActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, OnKeyListener {
	
	//constants
	public static final String DATABASE_NAME = "grocery-sync";
	
	//splash screen
	protected SplashScreenDialog splashDialog;
	
	//main screen
	protected EditText addItemEditText;
	protected ListView itemListView;
	
	//couch internals
	protected ServiceConnection couchServiceConnection;
	
	//ektorp impl
	protected CouchDbConnector couchDbConnector;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        showSplashScreen();
        startCouch();
        
        setContentView(R.layout.main);
               
        //connect items from layout
        addItemEditText = (EditText)findViewById(R.id.addItemEditText);
        itemListView = (ListView)findViewById(R.id.itemListView);        
        
        //connect listeners
		addItemEditText.setOnKeyListener(this);
        
    }
    
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(couchServiceConnection);
		} catch (IllegalArgumentException e) {
		}
	}    
    
    protected ICouchClient couchCallbackHandler = new ICouchClient.Stub() {
		
		public void installing(int completed, int total) throws RemoteException {
			AndroidGrocerySyncActivity.this.splashDialog.updateSplashScreenProgress(completed,  total);
		}
		
		public void exit(String error) throws RemoteException {
			AlertDialog.Builder builder = new AlertDialog.Builder(AndroidGrocerySyncActivity.this);
			builder.setMessage(error)
			       .setCancelable(false)
			       .setPositiveButton(R.string.error_dialog_button, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   AndroidGrocerySyncActivity.this.finish();
			           }
			       })
			       .setTitle(R.string.error_dialog_title);
			AlertDialog alert = builder.create();			
			alert.show();
		}		
		
		public void couchStarted(String host, int port) throws RemoteException {
			AndroidGrocerySyncActivity.this.removeSplashScreen();
			
			HttpClient httpClient = new StdHttpClient.Builder().host(host).port(port).build();
			CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
			couchDbConnector = dbInstance.createConnector(DATABASE_NAME, true);
			
			//attach list adapter to the list and handle clicks
			itemListView.setAdapter(new CouchListAdapter(AndroidGrocerySyncActivity.this, couchDbConnector));
			itemListView.setOnItemClickListener(AndroidGrocerySyncActivity.this);
			itemListView.setOnItemLongClickListener(AndroidGrocerySyncActivity.this);
			
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			
			ReplicationCommand pushReplication = new ReplicationCommand.Builder()
				.source(DATABASE_NAME)
				.target(prefs.getString("sync_url", "http://couchbase.iriscouch.com/grocery-sync"))
				.continuous(true)
				.build();
			
			dbInstance.replicate(pushReplication);
			
			ReplicationCommand pullReplication = new ReplicationCommand.Builder()
				.source(prefs.getString("sync_url", "http://couchbase.iriscouch.com/grocery-sync"))
				.target(DATABASE_NAME)
				.continuous(true)
				.build();
			
			dbInstance.replicate(pullReplication);
		}
	};
	
	protected void startCouch() {
		CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), couchCallbackHandler);
		couchServiceConnection = couch.startCouchbase(); 		
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
		JsonNode document = (JsonNode)parent.getItemAtPosition(position);
		toggleItemChecked(document);
	}
	
	/**
	 * Handle long-click on item in list
	 */
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

		final JsonNode document = (JsonNode)parent.getItemAtPosition(position);
        JsonNode textNode = document.get("text");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(AndroidGrocerySyncActivity.this);
		AlertDialog alert = builder.setTitle("Delete Item?")
			   .setMessage("Are you sure you want to delete " + textNode.getValueAsText() + "?")
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   deleteGroceryItem(document);
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
    	UUID uuid = UUID.randomUUID();
    	Calendar calendar = GregorianCalendar.getInstance();
    	long currentTime = calendar.getTimeInMillis();
    	String currentTimeString = DateFormat.format("EEEE-MM-dd'T'HH:mm:ss.SSS'Z'", calendar).toString();
    	
    	String id = currentTime + "-" + uuid.toString();
    	
    	Map<String, String> newItem = new HashMap<String, String>();
    	newItem.put("_id", id);
    	newItem.put("text", name);
    	newItem.put("check", Boolean.FALSE.toString());
    	newItem.put("created_at", currentTimeString);
    	
    	CouchDocumentAsyncTask createTask = new CouchDocumentAsyncTask(couchDbConnector, CouchDocumentAsyncTask.OPERATION_CREATE);
    	createTask.execute(newItem);
    }
    
    public void toggleItemChecked(JsonNode document) {
    	JsonNode check = document.get("check");
    	if(check.getBooleanValue()) {
    		ObjectNode documentObject = (ObjectNode)document;
    		documentObject.put("check", false);
    	}
    	else {
    		ObjectNode documentObject = (ObjectNode)document;
    		documentObject.put("check", true);    		
    	}
    	
    	CouchDocumentAsyncTask updateTask = new CouchDocumentAsyncTask(couchDbConnector, CouchDocumentAsyncTask.OPERATION_UPDATE);
    	updateTask.execute(document);
    }
	
    public void deleteGroceryItem(JsonNode document) {
    	CouchDocumentAsyncTask deleteTask = new CouchDocumentAsyncTask(couchDbConnector, CouchDocumentAsyncTask.OPERATION_DELETE);
    	deleteTask.execute(document);
    }
    
}