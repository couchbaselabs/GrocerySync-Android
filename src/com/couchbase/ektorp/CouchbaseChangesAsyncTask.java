package com.couchbase.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;

import android.os.AsyncTask;
import android.util.Log;

public class CouchbaseChangesAsyncTask extends
		AsyncTask<ChangesCommand, DocumentChange, Void> {

	public static final String TAG = "CouchbaseChangesAsyncTask";

	protected CouchDbConnector couchDbConnector;
	protected ChangesFeed feed;

	public CouchbaseChangesAsyncTask(CouchDbConnector couchDbConnector) {
		this.couchDbConnector = couchDbConnector;
	}

	@Override
	protected Void doInBackground(ChangesCommand... cmd) {
		feed = couchDbConnector.changesFeed(cmd[0]);

		while (!isCancelled() && feed.isAlive()) {
		    try {
				DocumentChange change = feed.next();
				publishProgress(change);
			} catch (InterruptedException e) {
				cancel(true);
			} catch (DbAccessException e) {
				Log.v(TAG, "Exception while replicating", e);
				//notify of error
			}
			catch (RuntimeException re) {
				Log.v(TAG, "Some other exception happend", re);
			}

		}

		return null;
	}

	@Override
	protected void onCancelled() {
		if(feed != null) {
			feed.cancel();
		}

		super.onCancelled();
	}

}
