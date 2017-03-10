package com.viewlift;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class ResponseCreator {

	public GenericJson genericReader(String json) {
		ObjectMapper mapper = new ObjectMapper();
		GenericJson root = new GenericJson(false);
		try {

			LinkedHashMap<String, Object> map = mapper.readValue(json,
					new TypeReference<Map<String, Object>>() {
					});
			getJsonObjectsFromMap(map, root);
			GenericJson test = GenericJson.find("required", root);

			System.out.println(test.toString());
			System.out.println(test.getKeysInThatNode());

			test.removeChild(new GenericJson("generatedTrayModule", false));
			System.out.println(test.toString());
			System.out.println(test.getKeysInThatNode());

		} catch (IOException e) {
			e.printStackTrace();
		}

		return root;
	}

	@SuppressWarnings("unchecked")
	void getJsonObjectsFromMap(Map<String, Object> map, GenericJson parent) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object jsonObject = entry.getValue();
			String key = entry.getKey();
			GenericJson currentParent = null;
			if (jsonObject instanceof LinkedHashMap) {
				currentParent = new GenericJson(key, false);
				getJsonObjectsFromMap(
						(LinkedHashMap<String, Object>) jsonObject,
						currentParent);
			} else if (jsonObject instanceof ArrayList) {
				currentParent = new GenericJson(key, true);
				getJsonObjectsFromList((ArrayList<Object>) jsonObject,
						currentParent);
			} else if (jsonObject instanceof String) {
				currentParent = createViewliftJsonString(key,
						(Object) jsonObject);
			} else if (jsonObject instanceof Boolean) {
				currentParent = createViewliftJsonBoolean(key,
						(Boolean) jsonObject);
			} else if (jsonObject instanceof Number) {
				currentParent = createViewliftJsonNumber(key,
						(Number) jsonObject);
			}
			if (currentParent != null) {
				parent.addValueToParentKey(currentParent);
			}
		}
	}

	@SuppressWarnings("unchecked")
	void getJsonObjectsFromList(ArrayList<Object> jsonObjects,
			GenericJson parent) {
		boolean keyValueObjects = false;
		for (Object jsonObject : jsonObjects) {
			GenericJson currentParent = null;
			keyValueObjects = false;
			if (jsonObject instanceof LinkedHashMap) {
				currentParent = new GenericJson(false);
				getJsonObjectsFromMap(
						(LinkedHashMap<String, Object>) jsonObject,
						currentParent);
			} else if (jsonObject instanceof ArrayList) {
				currentParent = new GenericJson(true);
				getJsonObjectsFromList((ArrayList<Object>) jsonObject,
						currentParent);
			} else if (jsonObject instanceof String) {
				currentParent = new GenericJson((Object) jsonObject);
				keyValueObjects = true;
			} else if (jsonObject instanceof Boolean) {
				currentParent = new GenericJson((Object) (Boolean) jsonObject);
				keyValueObjects = true;
			} else if (jsonObject instanceof Number) {
				currentParent = new GenericJson((Object) (Number) jsonObject);
				keyValueObjects = true;
			}

			if (parent == null)
				parent = currentParent;
			else if (currentParent != null) {
				parent.addValueToParentKey(currentParent);
//				if(keyValueObjects)
//					parent.addKeysInParentList(currentParent.getKey());
			}
		}
	}

	GenericJson createViewliftJsonString(String key, Object object) {
		return new GenericJson(key, object, false);
	}

	GenericJson createViewliftJsonBoolean(String key, boolean value) {
		return new GenericJson(key, value, false);
	}

	GenericJson createViewliftJsonNumber(String key, Number value) {
		return new GenericJson(key, value, false);
	}
}

/**
 * 
 * @author chandrahas
 *
 */
class GenericJson {

	private String key;
	private Object value;
	private Object valueWithNoKey;
	private Map<String, GenericJson> valueAsMap;
	private List<Object> valueAsList;
	private boolean valueIsList, valueHasNoKey;
	private List<String> keysInParent;

	/**
	 * Used to create a GenericJson object for the following type of string
	 * object({
	 * 	"key":value
	 * })
	 * object([
	 * 	{"key":"value"}
	 * ])
	 * @param valueIsList
	 */
	GenericJson(boolean valueIsList) {
		this.valueIsList = valueIsList;
		this.valueHasNoKey = false;
		this.valueWithNoKey = null;
		this.key = null;
		this.value = null;
		createChildren(valueIsList);
	}

	/**
	 * Used to create a GenericJson object for the following type of string
	 * json which has values inside of an array. These values can be either string, number or boolean
	 * [
	 * 	object("string"),object(Number),object(Boolean)
	 * ]
	 * @param valueWithNoKey
	 */
	GenericJson(Object valueWithNoKey) {
		this.valueWithNoKey = valueWithNoKey;
		this.valueHasNoKey = true;
		this.valueIsList = false;
		this.key = null;
		this.value = null;
	}

	/**
	 * Used to create a GenericJson object for the following type of string
	 * json which has a key and can hold value as arraylist or hashmap
	 * object("key":{
	 * 	{},[]
	 * })
	 * 
	 * @param key
	 * @param valueIsList
	 */
	GenericJson(String key, boolean valueIsList) {
		this.key = key;
		this.valueHasNoKey = false;
		this.valueIsList = valueIsList;
		this.valueWithNoKey = null;
		this.value = null;
		createChildren(valueIsList);
	}

	/**
	 * Used to create a GenericJson object for the following type of string
	 * json which has key and value
	 * 
	 * {"key":"value"} or {"key":1} or {"key":false}
	 * 
	 * @param key
	 * @param value
	 * @param createChildren
	 */
	GenericJson(String key, Object value, boolean createChildren) {
		this.key = key;
		this.value = value;
		this.valueHasNoKey = true;
		this.valueIsList = createChildren;
		this.valueWithNoKey = value;
	}

	
	private void createChildren(boolean valueIsList) {
		if (valueIsList) {
			this.valueAsList = new ArrayList<>();
			this.valueAsMap = null;
		} else {
			this.valueAsMap = new LinkedHashMap<>();
			this.valueAsList = null;
			this.keysInParent = new ArrayList<>();
		}
	}

	/**
	 * add value to parent generic json object, value can be list or map or primitive json value
	 * 
	 * @param value
	 */
	public void addValueToParentKey(GenericJson value) {
		if (valueIsList())
			valueAsList.add(value);
		else {
			valueAsMap.put(value.getKey(), value);
			keysInParent.add(value.getKey());
		}
	}

	/**
	 * remove value from parent
	 * @param value
	 */
	public void removeChild(GenericJson value) {
		String keyToRemove = value.getKey();
		if (valueAsMap.containsKey(keyToRemove)) {
			valueAsMap.remove(keyToRemove);
			keysInParent.remove(keysInParent.indexOf(keyToRemove));
		}
	}

	/**
	 * checks if the value has key or not
	 * true for the following json
	 * ["val"] or [{}]
	 * 
	 * @return
	 */
	public boolean valueHasNoKey() {
		return valueHasNoKey;
	}

	/**
	 * set/replace the value for the parent key
	 * @param valueHasNoKey
	 */
	public void setValueWithNoKey(boolean valueHasNoKey) {
		this.valueHasNoKey = valueHasNoKey;
	}
	
	/**
	 * get the list value if the parent has the value as list
	 * @return
	 */
	public List<Object> getValueList() {
		return valueAsList;
	}

	/**
	 * set/replace the value as list 
	 * @param valueAsList
	 */
	public void setValueList(List<Object> valueAsList) {
		this.valueAsList = valueAsList;
	}

	/**
	 * get the list as value if the parent has list
	 * @return
	 */
	public boolean valueIsList() {
		return valueIsList;
	}

	/**
	 * get the primitive values in a list
	 * @return
	 */
	public Object getValueWithNoKey() {
		return valueWithNoKey;
	}

	/**
	 * set the primitive value in the list
	 * @param valueWithNoKey
	 */
	public void setValueWithNoKey(Object valueWithNoKey) {
		this.valueWithNoKey = valueWithNoKey;
	}

	/**
	 * get the value of the generic json object
	 * @return
	 */
	public Object getvalue() {
		return value;
	}

	/**
	 * set/replace the value of the json object
	 * @param value
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * get the key of the current json
	 * @return
	 */
	public String getKey() {
		return key;
	}

	/**
	 * set/replace the key of the current object
	 * @param key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * get the value as a map if the json has one
	 * @return
	 */
	public Map<String, GenericJson> getValueAsMap() {
		return valueAsMap;
	}

	/**
	 * set the map value of the current object
	 * @param valueAsMap
	 */
	public void setValueAsMap(Map<String, GenericJson> valueAsMap) {
		this.valueAsMap = valueAsMap;
	}

	/**
	 * get all the keys in current object
	 * @return
	 */
	public List<String> getKeysInThatNode() {
		return keysInParent;
	}

	/**
	 * set all the keys in current object
	 * @param keysInParent
	 */
	public void setKeysInParent(List<String> keysInParent) {
		this.keysInParent = keysInParent;
	}
	
	/**
	 * adding keys from {key:value,key2:value} to the parent's keys list
	 * @param key
	 */
	public void addKeysInParentList(String key){
		keysInParent.add(key);
	}

	/**
	 * generate json for this object
	 */
	public String toString() {
		return generateJson(this);
	}

	private String generateJson(GenericJson node) {
		StringBuilder builder = new StringBuilder();
		if (node.valueHasNoKey()) {
			if (node.getKey() != null)
				return "\"" + node.getvalue() + "\"";
			else
				return "\"" + node.getValueWithNoKey() + "\"";
		} else if (node.valueIsList) {

			builder.append("[");
			int size = node.valueAsList.size();
			for (Object obj : node.valueAsList) {
				GenericJson curObj = (GenericJson) obj;
				builder.append(generateJson(curObj));
				size--;
				if (size > 0)
					builder.append(",");
			}
			builder.append("]");
		} else {
			builder.append("{");
			int size = node.valueAsMap.size();
			for (Entry<String, GenericJson> entry : node.valueAsMap.entrySet()) {
				GenericJson is = (GenericJson) entry.getValue();
				builder.append("\"").append(entry.getKey()).append("\":")
						.append(generateJson(is));
				size--;
				if (size > 0)
					builder.append(",");
			}
			builder.append("}");
		}

		return builder.toString();
	}

	/**
	 * find a node with this key in the calling object
	 * @param key
	 * @param searchNode
	 * @return
	 */
	public static GenericJson find(String key, GenericJson searchNode) {
		if (searchNode != null && searchNode.getKey() != null) {
			if (searchNode.getKey().equals(key))
				return searchNode;
		} else if (searchNode != null && searchNode.valueHasNoKey())
			return null;

		if (searchNode.valueHasNoKey) {
			if (searchNode.getValueWithNoKey() != null
					|| searchNode.getKey() != null) {
				if (searchNode.getValueWithNoKey().equals(key)
						|| searchNode.getKey().equals(key))
					return searchNode;
			}
			return null;
		}
		if (searchNode.valueIsList()) {
			List<Object> list = searchNode.getValueList();
			if (list.contains(key))
				return searchNode;
			else {
				GenericJson found = null;
				for (Object param : list) {
					found = find(key, (GenericJson) param);
					if (found != null)
						return found;
				}
				return null;
			}
		} else {
			Map<String, GenericJson> map = searchNode.getValueAsMap();
			if (map.containsKey(key))
				return map.get(key);
			else {
				GenericJson found = null;
				for (Map.Entry<String, GenericJson> entry : map.entrySet()) {
					found = find(key, entry.getValue());
					if (found != null)
						return found;
				}
				return null;
			}
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericJson other = (GenericJson) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
