package com.couchbase.grocerysync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;

import java.util.List;


public class GrocerySyncArrayAdapter extends ArrayAdapter<QueryRow> {

    private List<QueryRow> list;
    private final Context context;

    public GrocerySyncArrayAdapter(Context context, int resource, int textViewResourceId, List<QueryRow> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
    }

	private static class ViewHolder {
	   ImageView icon;
	   TextView label;
	}

	@Override
	public View getView(int position, View itemView, ViewGroup parent) {

        if (itemView == null) {
            LayoutInflater vi = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = vi.inflate(R.layout.grocery_list_item, null);
            ViewHolder vh = new ViewHolder();
            vh.label = (TextView) itemView.findViewById(R.id.label);
            vh.icon = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setTag(vh);
        }

        try {
            TextView label = ((ViewHolder)itemView.getTag()).label;
            QueryRow row = getItem(position);
            SavedRevision currentRevision = row.getDocument().getCurrentRevision();
            Object check = (Object) currentRevision.getProperty("check");
            boolean isGroceryItemChecked = false;
            if (check != null && check instanceof Boolean) {
                isGroceryItemChecked = ((Boolean)check).booleanValue();
            }
            String groceryItemText = (String) currentRevision.getProperty("text");
            label.setText(groceryItemText);

            ImageView icon = ((ViewHolder)itemView.getTag()).icon;
            if(isGroceryItemChecked) {
                icon.setImageResource(R.drawable.list_area___checkbox___checked);
            }
            else {
                icon.setImageResource(R.drawable.list_area___checkbox___unchecked);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return itemView;
	}
}
