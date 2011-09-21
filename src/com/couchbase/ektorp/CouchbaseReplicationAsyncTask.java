package com.couchbase.ektorp;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;

import android.os.AsyncTask;
import android.util.Log;

public class CouchbaseReplicationAsyncTask extends
		AsyncTask<ReplicationCommand, Void, ReplicationStatus> {

	public static final String TAG = "CouchbaseReplicationAsyncTask";

	private CouchDbInstance couchDbInstance;

	public CouchbaseReplicationAsyncTask(CouchDbInstance couchDbInstance) {
		this.couchDbInstance = couchDbInstance;
	}

	@Override
	protected ReplicationStatus doInBackground(ReplicationCommand... params) {
		ReplicationStatus result = null;
		try {
			result = couchDbInstance.replicate(params[0]);
		} catch (DbAccessException e) {
			Log.v(TAG, "Exception while replicating", e);
			//notify of error
		}
		catch (RuntimeException re) {
			Log.v(TAG, "Some other exception happend", re);
		}
		return result;
	}

}
