package com.couchbase.grocerysync;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult.Row;
import org.ektorp.android.util.CouchbaseViewListAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class GrocerySyncListAdapter extends CouchbaseViewListAdapter {

	protected MainActivity parent;

	public GrocerySyncListAdapter(MainActivity parent, CouchDbConnector couchDbConnector, ViewQuery viewQuery) {
		super(couchDbConnector, viewQuery, true);
		this.parent = parent;
	}

	private static class ViewHolder {
	   ImageView icon;
	   TextView label;
	}

	@Override
	public View getView(int position, View itemView, ViewGroup parent) {
        View v = itemView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.grocery_list_item, null);
            ViewHolder vh = new ViewHolder();
            vh.label = (TextView) v.findViewById(R.id.label);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            v.setTag(vh);
        }

        TextView label = ((ViewHolder)v.getTag()).label;
        Row row = getRow(position);
        JsonNode item = row.getValueAsNode();
        JsonNode itemText = item.get("text");
        if(itemText != null) {
        	label.setText(itemText.getTextValue());
        }
        else {
        	label.setText("");
        }

        ImageView icon = ((ViewHolder)v.getTag()).icon;
        JsonNode checkNode = item.get("check");
        if(checkNode != null) {
	        if(checkNode.getBooleanValue()) {
	        	icon.setImageResource(R.drawable.list_area___checkbox___checked);
	        }
	        else {
	        	icon.setImageResource(R.drawable.list_area___checkbox___unchecked);
	        }
        }

        return v;
	}
}
