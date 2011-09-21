package com.couchbase.ektorp;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbInfo;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.DocumentChange;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CouchbaseViewListAdapter extends BaseAdapter {

	public static final String TAG = "CouchbaseViewListAdapter";

	protected CouchDbConnector couchDbConnector;
	protected ViewQuery viewQuery;
	protected boolean followChanges;
	protected List<Row> listRows;

	private CouchbaseListStructureAsyncTask updateListItemsTask;
	private CouchbaseListChangesAsyncTask couchChangesAsyncTask;

	public CouchbaseViewListAdapter(CouchDbConnector couchDbConnector, ViewQuery viewQuery, boolean followChanges) {
		this.couchDbConnector = couchDbConnector;
		this.viewQuery = viewQuery;
		this.followChanges = followChanges;

		listRows = new ArrayList<Row>();

		DbInfo dbInfo = couchDbConnector.getDbInfo();
		long lastUpdateSeq = dbInfo.getUpdateSeq();

		//trigger initial update
		updateListItems(false);

		if(followChanges) {
			//create an ansyc task to get updates
			ChangesCommand changesCmd = new ChangesCommand.Builder().since(lastUpdateSeq)
					.includeDocs(false)
					.continuous(true)
					.heartbeat(5000)
					.build();

			couchChangesAsyncTask = new CouchbaseListChangesAsyncTask(couchDbConnector);
			couchChangesAsyncTask.execute(changesCmd);
		}
	}

	@Override
	public int getCount() {
		return listRows.size();
	}

	@Override
	public Object getItem(int position) {
		return listRows.get(position);
	}

	public Row getRow(int position) {
		return (Row)getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		throw new UnsupportedOperationException("You must implement getView() yourself.");
	}

	public void updateListItems(boolean includeDocs) {
		if(updateListItemsTask == null) {
			updateListItemsTask = new CouchbaseListStructureAsyncTask(couchDbConnector);
			updateListItemsTask.execute(viewQuery.includeDocs(includeDocs));
		}
	}

	private class CouchbaseListStructureAsyncTask extends CouchbaseViewAsyncTask {

		public CouchbaseListStructureAsyncTask(CouchDbConnector couchDbConnector) {
			super(couchDbConnector);
		}

		@Override
		protected void onPostExecute(ViewResult result) {
			if(result != null) {
				listRows = result.getRows();
				notifyDataSetChanged();
			}
			updateListItemsTask = null;
		}

	}

	private class CouchbaseListChangesAsyncTask extends CouchbaseChangesAsyncTask {

		public CouchbaseListChangesAsyncTask(CouchDbConnector couchDbConnector) {
			super(couchDbConnector);
		}

		@Override
		protected void onProgressUpdate(DocumentChange... values) {
			updateListItems(false);
		}

	}

	public void cancelContinuous() {
		couchChangesAsyncTask.cancel(true);
	}

}
