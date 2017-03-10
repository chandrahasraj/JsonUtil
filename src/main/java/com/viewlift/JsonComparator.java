package com.viewlift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public class JsonComparator {

	private static final String REQUIRED = "required";
	private static final String PROPERTIES = "properties";
	
	public void compareJsons(String schema,Map<String,Object> map){
		JsonElement element = new JsonParser().parse(schema);
		System.out.println(element.getAsJsonObject().get(PROPERTIES).getAsJsonObject().get(REQUIRED));
		
		for(Map.Entry<String, Object> entry:map.entrySet()){
			
		}
	}
	
	List<String> getUnavilableEntries(JsonArray array,List<String> keys){
		List<String> unavailable = new ArrayList<>();
		for(JsonElement element:array){
			
		}
		return unavailable;
	}
	
	void compareSchemaWithMap(JsonElement element, Map<String,Object> map){
		if(element.isJsonObject()){
			JsonArray requiredElements = getRequired(element);
			if(requiredElements==null){
				requiredElements = getElements(element);
			}
		}
	}

	JsonObject getProperties(JsonElement element){
		return element.getAsJsonObject();
	}

	JsonArray getRequired(JsonElement element){
		return element.getAsJsonObject().getAsJsonArray();
	}
	
	JsonArray getElements(JsonElement element){
		return getRequired((JsonElement)getProperties(element));
	}
}


