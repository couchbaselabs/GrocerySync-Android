package com.couchbase.grocerysync;

import org.ektorp.CouchDbConnector;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;

import android.os.AsyncTask;

public class CouchChangesAsyncTask extends AsyncTask<Integer, DocumentChange, Void> {
	
	private CouchListAdapter parent;
	private CouchDbConnector couchDbConnector;
	private Long since;
	private ChangesFeed feed;
	
	public CouchChangesAsyncTask(CouchListAdapter parent, CouchDbConnector couchDbConnector, Long since) {
		this.parent = parent;
		this.couchDbConnector = couchDbConnector;
		this.since = since;
	}
	
	protected Void doInBackground(Integer... unused) {
		
		ChangesCommand cmd = new ChangesCommand.Builder().since(since)
				.includeDocs(true)
				.continuous(true)
				.heartbeat(5000)
				.build();

		feed = couchDbConnector.changesFeed(cmd);

		while (feed.isAlive()) {
		    try {
				DocumentChange change = feed.next();
				publishProgress(change);
			} catch (InterruptedException e) {
				cancel(true);
			}
		    
		}
		
		
		return null;
	};
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	protected void onProgressUpdate(DocumentChange... values) {
		for (DocumentChange documentChange : values) {
			String id = documentChange.getId();
			if(documentChange.isDeleted()) {
				parent.rowMap.remove(id);
			}
			else {
				parent.rowMap.put(id, documentChange.getDocAsNode());
			}
		}
		parent.notifyDataSetChanged();
	};
	
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
	};
	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		
		if(feed != null) {
			feed.cancel();
		}
	}

}
