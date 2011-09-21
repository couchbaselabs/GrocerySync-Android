package com.couchbase.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;

import android.os.AsyncTask;
import android.util.Log;

public class CouchbaseDocumentAsyncTask extends
		AsyncTask<Object, Void, Void> {

	public static final String TAG = "CouchbaseDocumentAsyncTask";

	public static final int OPERATION_CREATE = 0;
	public static final int OPERATION_UPDATE = 1;
	public static final int OPERATION_DELETE = 2;

	private CouchDbConnector couchDbConnector;
	private int operation;

	public CouchbaseDocumentAsyncTask(CouchDbConnector couchDbConnector, int operation) {
		this.couchDbConnector = couchDbConnector;
		this.operation = operation;
	}

	@Override
	protected Void doInBackground(Object... params) {

		for (Object document : params) {
			switch(operation) {
			case OPERATION_CREATE:
				doCreateInBackground(document);
				break;
			case OPERATION_UPDATE:
				doUpdateInBackground(document);
				break;
			case OPERATION_DELETE:
				doDeleteInBackground(document);
				break;
			}
		}

		return null;
	}

	private void doDeleteInBackground(Object document) {
		try {
			couchDbConnector.delete(document);
		} catch (DbAccessException e) {
			Log.v(TAG, "Exception while deleting", e);
		}
	}

	private void doUpdateInBackground(Object document) {
		try {
			couchDbConnector.update(document);
		} catch (DbAccessException e) {
			Log.v(TAG, "Exception while updating", e);
		}
	}

	private void doCreateInBackground(Object document) {
		try {
			couchDbConnector.create(document);
		} catch (DbAccessException e) {
			Log.v(TAG, "Exception while creating", e);
		}
	}

}
