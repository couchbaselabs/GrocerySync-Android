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
import android.app.Dialog;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.libcouch.CouchbaseEmbeddedServer;
import com.couchbase.libcouch.ICouchClient;

public class AndroidGrocerySyncActivity extends Activity {
	
	//constants
	public static final String DATABASE_NAME = "grocery-sync";
	
	//splash screen
	protected Dialog splashDialog;
	protected ProgressBar splashProgressBar;
	protected TextView splashProgressMessage;
	
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
		addItemEditText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					
					String inputText = addItemEditText.getText().toString();
					if(!inputText.equals("")) {
						
						createGroceryItem(inputText);
					
						Toast.makeText(AndroidGrocerySyncActivity.this,
								inputText, Toast.LENGTH_SHORT)
								.show();
						
					}
					addItemEditText.setText("");
					return true;
				}
				return false;
			}
		});
        
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
			if(completed < (total - 1)) {
				AndroidGrocerySyncActivity.this.updateSplashScreenProgressBar(completed, total);
				AndroidGrocerySyncActivity.this.updateSplashScreenProgressMessage(getString(R.string.installing_message));
			}
			else {
				AndroidGrocerySyncActivity.this.updateSplashScreenProgressBar(completed, total);
				AndroidGrocerySyncActivity.this.updateSplashScreenProgressMessage(getString(R.string.startup_message));
			}
		}
		
		public void exit(String error) throws RemoteException {
			// TODO Auto-generated method stub
			
		}
		
		public void couchStarted(String host, int port) throws RemoteException {
			AndroidGrocerySyncActivity.this.removeSplashScreen();
			
			HttpClient httpClient = new StdHttpClient.Builder().host(host).port(port).build();
			CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
			couchDbConnector = dbInstance.createConnector(DATABASE_NAME, true);
			itemListView.setAdapter(new CouchListAdapter(AndroidGrocerySyncActivity.this, couchDbConnector));
			itemListView.setOnItemClickListener(new OnItemClickListener() {
				
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					JsonNode document = (JsonNode)parent.getItemAtPosition(position);
					toggleItemChecked(document);
				}
				
			});
			
			itemListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				
				@Override
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
				
			});
			
			
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
		CouchbaseEmbeddedServer couch = new CouchbaseEmbeddedServer(getBaseContext(), couchCallbackHandler);
		couchServiceConnection = couch.startCouchbase(); 		
	}
	
	/**
	 * Removes the Dialog that displays the splash screen
	 */
	protected void removeSplashScreen() {
	    if (splashDialog != null) {
	        splashDialog.dismiss();
	        splashDialog = null;
	        splashProgressBar = null;
	        splashProgressMessage = null;
	    }
	}
	
	/**
	 * Update the Splash Screen Progress Bar
	 */	
	protected void updateSplashScreenProgressBar(int progress, int max) {
		if(splashProgressBar != null) {
			splashProgressBar.setProgress(progress);
			splashProgressBar.setMax(max);
		}
	}
	
	/**
	 * Update the Splash Screen Progress Message
	 */
	protected void updateSplashScreenProgressMessage(String message) {
		if(splashProgressMessage != null) {
			splashProgressMessage.setText(message);
		}
	}
	 
	/**
	 * Shows the splash screen over the full Activity
	 */
	protected void showSplashScreen() {
	    splashDialog = new Dialog(this, R.style.SplashScreenStyle);
	    splashDialog.setContentView(R.layout.splashscreen);
	    splashDialog.setCancelable(false);
	    splashDialog.show();
	    
	    splashProgressBar = (ProgressBar)splashDialog.findViewById(R.id.splashProgressBar);
	    splashProgressBar.setProgress(0);
	    splashProgressBar.setMax(100);
	    
	    splashProgressMessage = (TextView)splashDialog.findViewById(R.id.splashProgressMessage);
	    splashProgressMessage.setText(getString(R.string.startup_message));
	}

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "Settings");
        return super.onCreateOptionsMenu(menu);
    }

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
    	
    	couchDbConnector.create(newItem);
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
    	
    	couchDbConnector.update(document);
    }
	
    public void deleteGroceryItem(JsonNode document) {
    	couchDbConnector.delete(document);
    }
    
}