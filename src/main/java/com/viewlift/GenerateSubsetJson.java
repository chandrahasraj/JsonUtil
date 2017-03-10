package com.viewlift;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Chandrahas Raj
 *
 *This class generates subset json from the schema provided. A schema skeleton can would be as follows
 *{
 *	"$schema":"http://json-schema.org/draft-04/schema#",
 *	"type":"object", 
 *	"properties":{
 *		"key":"string",
 *		"key1":long,
 *		"key2":{
 *			"type":"array",
 *			"items":{
 *				"type":"string"
 *			}
 *		}
 *	},
 *	"required":["key","key1","key2"]
 *}
 *
 *This schema can validate the following json
 *{
 *	"key":"value",
 *	"key1":10,
 *	"key2":["d1","d2","d3"]
 *}
 */
public class GenerateSubsetJson {

	Map<String,Object> schema;
	
	/**
	 * Takes the schema provided. Which is used to create the subset json
	 * @param schemaToConvert
	 */
	public GenerateSubsetJson(String schemaToConvert){
		try {
			schema = new ObjectMapper().readValue(schemaToConvert,new TypeReference<Map<String,Object>>(){});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main Method to convert the given Json into the schema provided as schemaToConvert
	 * We take the json and create a map or list out of the json based on the top level schema.
	 * This map or list is then recursively looped through and every time is compared with current schema.
	 * The schema ensures which level we are at and what are the required keys. 
	 * We can loop through any type of json if we have the schema with us.
	 * 
	 * @param changeableJson
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public String matchMapWithSchema(String changeableJson) throws JsonParseException, JsonMappingException, IOException{
		String type = (String) schema.get("type");
		String finalMappedJson = "";
		if(type.equals("object")){
			Map<String,Object> changeableMap = new ObjectMapper().readValue(changeableJson,new TypeReference<Map<String,Object>>(){});
			if(schema.containsKey("required"))//to check if empty 
				changeableMap = matchChangeableMapWithSchemaMap(schema,changeableMap);
			finalMappedJson = new ObjectMapper().writeValueAsString(changeableMap);
		}else if(type.equals("array")){
			List<Object> changeableList = new ObjectMapper().readValue(changeableJson,new TypeReference<List<Object>>(){});
			changeableList = matchChangeableListWithSchemaList(schema, changeableList);
			finalMappedJson = new ObjectMapper().writeValueAsString(changeableList);
		}
		return finalMappedJson;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> matchChangeableMapWithSchemaMap(Map<String, Object> schemaElement, Map<String, Object> mapElement) {
		List<String> required = (List<String>) schemaElement.get("required");
		Set<String> removeableKeys = mapElement.keySet();
		mapElement = removeKeysFromChangeableMap(required, removeableKeys, mapElement);
		Map<String,Object> properties = (Map<String, Object>) schemaElement.get("properties");
		for (String key : required) {
			Map<String, Object> nextSchemaElement = (Map<String, Object>) properties.get(key);
			if (nextSchemaElement.containsKey("type")) {
				String type = (String) nextSchemaElement.get("type");
				Object nextMapElement = mapElement.get(key);
				if (type.equals("object")) {
					if(nextSchemaElement.containsKey("required")){
						Map<String, Object> resultMap = matchChangeableMapWithSchemaMap(
								(Map<String, Object>) nextSchemaElement,
								(Map<String, Object>) nextMapElement);
						mapElement.put(key, resultMap);
					}else if(!nextSchemaElement.containsKey("required") && nextSchemaElement.containsKey("properties")){
						Map<String,Object> resultMap = (Map<String, Object>) nextMapElement;
						resultMap = removeKeysFromChangeableMap(null, resultMap.keySet(), resultMap);
						mapElement.put(key, resultMap);
					}
				} else if (type.equals("array")) {
					List<Object> resultList = matchChangeableListWithSchemaList(
							(Map<String, Object>) nextSchemaElement,
							(List<Object>) nextMapElement);
					mapElement.put(key, resultList);
				}
			}
		}
		return mapElement;
	}

	@SuppressWarnings("unchecked")
	private List<Object> matchChangeableListWithSchemaList(Map<String, Object> nextSchemaElement, List<Object> nextMapElement) {
		Map<String,Object> schemaItems = (Map<String, Object>) nextSchemaElement.get("items");
		List<Object> cloneListMapElement = new ArrayList<>();
		if(schemaItems.containsKey("required")){//if array contains object. Cannot manipulate primitive types
			List<String> required = (List<String>) schemaItems.get("required");
			for(int i=0;i<nextMapElement.size();i++){
				Object object = nextMapElement.get(i);
				if(object instanceof Map){// the array can contain primitive types, array types and object types;
					Map<String,Object> mapInsideList = (Map<String,Object>)object;
					Map<String,Object> schemaListObject = (Map<String, Object>) schemaItems.get("properties");
					Set<String> removeableKeys = mapInsideList.keySet();
					mapInsideList = removeKeysFromChangeableMap(required, removeableKeys, mapInsideList);
					//The map can contain internal maps and lists and any other type
					for(String key:required){
						Object parent = mapInsideList.get(key);
						if(parent instanceof Map){
							Map<String,Object> mapInsideListOfMaps = (Map<String, Object>) mapInsideList.get(key);
							Map<String,Object> innerSchemaListObject = (Map<String, Object>) schemaListObject.get(key);
							if(innerSchemaListObject.containsKey("required"))
								mapInsideListOfMaps = matchChangeableMapWithSchemaMap(innerSchemaListObject,mapInsideListOfMaps);
							mapInsideList.put(key, mapInsideListOfMaps);
						}else if(parent instanceof List){
							List<Object> innerList = (List<Object>) mapInsideList.get(key);
							Map<String,Object> innerSchemaListObject = (Map<String, Object>) schemaListObject.get(key);
							innerList = matchChangeableListWithSchemaList(innerSchemaListObject, innerList);
							mapInsideList.put(key, innerList);
						}//no need to check for primitive types
					}
					cloneListMapElement.add(mapInsideList);
				}//cannot support list with lists
			}
		}else if(!schemaItems.containsKey("required") && schemaItems.containsKey("properties")){
			for(int i=0;i<nextMapElement.size();i++){
				Object object = nextMapElement.get(i);
				if(object instanceof Map){
					Map<String,Object> mapInsideList = (Map<String,Object>)object;
					mapInsideList = removeKeysFromChangeableMap(null, mapInsideList.keySet(), mapInsideList);
					cloneListMapElement.add(mapInsideList);
				}
			}
		}
		return cloneListMapElement;
	}

	private Map<String, Object> removeKeysFromChangeableMap(List<String> required, Set<String> removeableKeys, Map<String, Object> changeableMap) {
		Map<String,Object> result = new HashMap<>(changeableMap);
		if(removeableKeys!=null && !removeableKeys.isEmpty()){//super set cannot be null or empty. Can't proceed forward if null or empty
			if(required!=null){ // the required keys can be empty;
				removeableKeys.removeAll(required);
			}
			for (String key : removeableKeys)
				result.remove(key);
			}
		return result;
	}
}

