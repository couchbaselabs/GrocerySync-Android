package com.couchbase.grocerysync;

import org.ektorp.DbAccessException;
import org.ektorp.android.util.EktorpAsyncTask;

import android.util.Log;

public abstract class GrocerySyncEktorpAsyncTask extends EktorpAsyncTask {

	@Override
	protected void onDbAccessException(DbAccessException dbAccessException) {
		Log.e(MainActivity.TAG, "DbAccessException in background", dbAccessException);
	}

}
