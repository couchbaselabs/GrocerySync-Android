package com.couchbase.grocerysync;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import android.text.format.DateFormat;

public class GroceryItemUtils {

	public static JsonNode toggleCheck(JsonNode item) {
		ObjectNode itemObject = (ObjectNode)item;
		JsonNode checkNode = item.get("check");
		if(checkNode != null) {
			if(checkNode.getBooleanValue()) {
				itemObject.put("check", false);
			}
			else {
				itemObject.put("check", true);
			}
		}
		else {
			itemObject.put("check", true);
		}

		return item;
	}

	public static JsonNode createWithText(String text) {
    	UUID uuid = UUID.randomUUID();
    	Calendar calendar = GregorianCalendar.getInstance();
    	long currentTime = calendar.getTimeInMillis();
    	String currentTimeString = DateFormat.format("EEEE-MM-dd'T'HH:mm:ss.SSS'Z'", calendar).toString();

    	String id = currentTime + "-" + uuid.toString();

    	ObjectNode item = JsonNodeFactory.instance.objectNode();

    	item.put("_id", id);
    	item.put("text", text);
    	item.put("check", Boolean.FALSE);
    	item.put("created_at", currentTimeString);

    	return item;
	}

}
