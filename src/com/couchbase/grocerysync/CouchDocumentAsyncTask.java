package com.couchbase.grocerysync;

import org.ektorp.CouchDbConnector;

import android.os.AsyncTask;

public class CouchDocumentAsyncTask extends AsyncTask<Object, Void, Void> {
	
	public static final int OPERATION_CREATE = 0;
	public static final int OPERATION_UPDATE = 1;
	public static final int OPERATION_DELETE = 2;
	
	private CouchDbConnector couchDbConnector;
	private int operation;
	
	public CouchDocumentAsyncTask(CouchDbConnector couchDbConnector, int operation) {
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
		couchDbConnector.delete(document);		
	}

	private void doUpdateInBackground(Object document) {
		couchDbConnector.update(document);
	}

	private void doCreateInBackground(Object document) {
		couchDbConnector.create(document);
	}

}
