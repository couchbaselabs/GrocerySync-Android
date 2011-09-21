package com.couchbase.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;

import android.os.AsyncTask;
import android.util.Log;

public class CouchbaseViewAsyncTask extends AsyncTask<ViewQuery, Void, ViewResult> {

	public static final String TAG = "CouchbaseViewAsyncTask";

	protected CouchDbConnector couchDbConnector;

	public CouchbaseViewAsyncTask(CouchDbConnector couchDbConnector) {
		this.couchDbConnector = couchDbConnector;
	}

	@Override
	protected ViewResult doInBackground(ViewQuery... viewQuery) {
		ViewResult vr = null;
		try {
			vr = couchDbConnector.queryView(viewQuery[0]);
		} catch (DbAccessException e) {
			Log.v(TAG, "Exception while replicating", e);
			//notify of error
		}
		catch (RuntimeException re) {
			Log.v(TAG, "Some other exception happend", re);
		}
		return vr;
	}

}
