package com.couchbase.grocerysync;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.cblite.CBLDocument;
import com.couchbase.cblite.CBLQueryRow;

import java.util.List;


public class GrocerySyncListAdapter extends ArrayAdapter<CBLQueryRow> {

    private List<CBLQueryRow> list;
    private final Context context;

    public GrocerySyncListAdapter(Context context, int resource, int textViewResourceId, List<CBLQueryRow> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.list = objects;
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
        CBLQueryRow row = list.get(position);
        CBLDocument document = row.getDocument();
        boolean checked = false;
        try {
            label.setText((String)document.getCurrentRevision().getProperty("text"));
            checked = ((Boolean) document.getCurrentRevision().getProperty("check")).booleanValue();
        } catch (Exception e) {
            label.setText("Error");
            Log.e(MainActivity.TAG, "Error Displaying document", e);
        }
        ImageView icon = ((ViewHolder)v.getTag()).icon;
        if(checked) {
            icon.setImageResource(R.drawable.list_area___checkbox___checked);
        }
        else {
            icon.setImageResource(R.drawable.list_area___checkbox___unchecked);
        }

        return v;
	}
}
