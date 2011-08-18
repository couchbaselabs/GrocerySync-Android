package com.couchbase.grocerysync;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbInfo;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CouchListAdapter extends BaseAdapter {
	
	protected Context context;
	//protected List<Row> rows;
	public HashMap<String, JsonNode> rowMap;
	
	public CouchListAdapter(Context context, CouchDbConnector couchDbConnector) {
		this.context = context;
		
		rowMap = new HashMap<String, JsonNode>();
		
		DbInfo dbInfo = couchDbConnector.getDbInfo();
		int lastUpdateSeq = dbInfo.getUpdateSeq();
		
		ViewResult vr = couchDbConnector.queryView(new ViewQuery().allDocs().includeDocs(true));
//		rows = vr.getRows();
		Iterator<Row> rowIterator = vr.iterator();
		while(rowIterator.hasNext()) {
			Row row = rowIterator.next();
			rowMap.put(row.getId(), row.getDocAsNode());
		}
		
		//create an ansyc task to get updates
		CouchChangesAsyncTask couchChangesAsyncTask = new CouchChangesAsyncTask(this, couchDbConnector, lastUpdateSeq);
		couchChangesAsyncTask.execute((Integer[])null);
	}

	@Override
	public int getCount() {
//		return rows.size();
		return rowMap.size();
	}

	@Override
	public Object getItem(int position) {
//		return rows.get(position);
		String key = (String)rowMap.keySet().toArray()[position];
		return rowMap.get(key);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View itemView, ViewGroup parent) {
        View v = itemView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.grocery_list_item, null);
        }
        
//        Row row = (Row)rows.get(position);
        TextView label = (TextView) v.findViewById(R.id.label);
//        JsonNode document = row.getDocAsNode();
        JsonNode document = (JsonNode)getItem(position);
        JsonNode textNode = document.get("text");
        label.setText(textNode.getTextValue());
        
        JsonNode checkNode = document.get("check");
        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        if(checkNode.getBooleanValue()) {
        	icon.setImageResource(R.drawable.list_area___checkbox___checked);
        }
        else {
        	icon.setImageResource(R.drawable.list_area___checkbox___unchecked);
        }
        
        
        return v;
	}

}
