package com.viewlift;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.sun.codemodel.JCodeModel;

public class JsonToJava {

	private static Map<String, Object> filterMap;

	public static void main(String[] args) throws IOException {
		List<String> lines = FileUtils.readLines(new File(
				"C:\\\\Users\\viewlift\\Documents\\test_final.json"));
		String serverJson = StringUtils.join(lines, "");

		List<String> lines2 = FileUtils.readLines(new File(
				"C:\\\\Users\\viewlift\\Documents\\test_client.json"));
		String clientJson = StringUtils.join(lines2, "");

		// getKeysFromMap(clientJson);
//		Map<String, Object> clientMap = new ObjectMapper().readValue(
//				clientJson, new TypeReference<Map<String, Object>>() {
//				});
//		Map<String, Object> serverMap = new ObjectMapper().readValue(
//				serverJson, new TypeReference<Map<String, Object>>() {
//				});
//		serverMap = compareAndGetFilteredMap(serverMap, clientMap);
//
//		for (Map.Entry<String, Object> ee : serverMap.entrySet()) {
//			System.out.println(ee.getKey() + "-->" + ee.getValue());
//		}
		
		String schema= new JsonSchemaGenerator(new ObjectMapper()).convert(clientJson);
		System.out.println(schema);
		String result = new GenerateSubsetJson(schema).matchMapWithSchema(serverJson);
		System.out.println(result);
	}

	void fieldsRequired(String[] fields, Map<String, Object> serverData) {
		Set<Node> serverKeys = new HashSet<>();
		Map<String, Object> cloneMap = serverData;
		for (String field : fields) {
			Node newNode = new Node(field);
			serverKeys.add(newNode);
		}
		for (Map.Entry<String, Object> entry : serverData.entrySet()) {
			Object value = entry.getValue();
			if(value instanceof Object){
				
			}
			if (!serverKeys.contains(new Node(entry.getKey())))
				cloneMap.remove(entry.getKey());
		}
	}
	
	@SuppressWarnings("unchecked")
	void getValuesFromList(Object value){
		List<Object> values = (List<Object>)value;
		for(Object cValue:values){
			
			
		}
	}

	/**
	 * 
	 * @param serverData
	 *            :map which contains data fom client which is to be obtained
	 * @param clientData
	 *            : map which contains all the data. get only required data
	 *            using filterFrom
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> compareAndGetFilteredMap( Map<String, Object> serverData, Map<String, Object> clientData) {
		Map<String, Object> tempMap = new HashMap<>(serverData);
		//client must always be a subset of server data 
		if (getDifference(clientData.keySet(), tempMap.keySet()).isEmpty()) {
			SetView<String> commonKeys = getIntersection(tempMap.keySet(),
					clientData.keySet());
			SetView<String> differenceKeys = getDifference(tempMap.keySet(),
					clientData.keySet());
			for (String toRemove : differenceKeys) {
				serverData.remove(toRemove);
			}
			for (String key : commonKeys) {
				Object o1 = tempMap.get(key);
				Object o2 = clientData.get(key);
				if (o1 instanceof Map && o2 instanceof Map) {
					o1 = compareAndGetFilteredMap((Map<String, Object>) o1,
							(Map<String, Object>) o2);
					serverData.put(key, o1);
				} else if (o1 instanceof List && o2 instanceof List) {
					List<Object> filterFromNested = (List<Object>) o1;
					List<Object> toBeFilteredNest = (List<Object>) o2;
					List<Object> resultData = compareLists(new HashSet<>(filterFromNested),
							new HashSet<>(toBeFilteredNest));
					serverData.put(key, resultData);
				}
			}
		}
		return serverData;
	}

	static <T> SetView<T> getDifference(Set<T> set1, Set<T> set2) {
		return Sets.difference(set1, set2);
	}

	static <T> SetView<T> getIntersection(Set<T> set1, Set<T> set2) {
		return Sets.intersection(set1, set2);
	}
	
	//only single nested list is supported currently
	@SuppressWarnings("unchecked")
	public static List<Object> compareLists(Set<Object> serverData,
			Set<Object> clientData) {
		List<Object> serverNestedData = new ArrayList<>(serverData);
		List<Object> clientNestedData = new ArrayList<>(clientData);
		SetView<Object> commonKeysNested = getIntersection(new HashSet<>(
				serverNestedData), new HashSet<>(clientNestedData));
		SetView<Object> differenceKeysNested = getDifference(new HashSet<>(
				serverNestedData), new HashSet<>(clientNestedData));

		for (Object obj : differenceKeysNested) {
			serverNestedData.remove(obj);
		}
		for (Object obj : commonKeysNested) {
			if (obj instanceof Map) {
				Map<String,Object> resultMap = compareAndGetFilteredMap(
						(Map<String, Object>) (serverNestedData.get(serverNestedData
								.indexOf(obj))),
						(Map<String, Object>) (clientNestedData
								.get(clientNestedData.indexOf(obj))));
				serverNestedData.add(resultMap);
			}else if(obj instanceof List){
				Set<Object> set1 = new HashSet<>((List<Object>)serverNestedData.remove(serverNestedData.indexOf(obj)));
				Set<Object> set2 = new HashSet<>((List<Object>)clientNestedData.get(clientNestedData.indexOf(obj)));
				List<Object> resultList = compareLists(set1,set2);
				serverNestedData.add(resultList);
			}
		}
		return serverNestedData;
	}

	public static void compareMaps(Map<String, Object> m1,
			Map<String, Object> m2) {
		MapDifference<String, Object> diff = Maps.difference(m1, m2);
		System.out.println(diff.entriesDiffering());
		System.out.println(diff.entriesOnlyOnRight());
		System.out.println(diff.entriesOnlyOnLeft());
		Map<String, ValueDifference<Object>> obj = diff.entriesDiffering();
		for (Entry<String, ValueDifference<Object>> entry : obj.entrySet()) {
			System.out.println("key-->" + entry.getKey());
			System.out.println(entry.getValue().leftValue());
			System.out.println(entry.getValue().rightValue());
		}
		Map<String, Object> entriesInm2 = diff.entriesOnlyOnRight();
		for (Map.Entry<String, Object> ee : entriesInm2.entrySet()) {
			System.out.println(ee.getKey() + "-->" + ee.getValue());
		}
	}

	private static void getKeysFromMap(String json) {
		JsonToJava.filterMap = new ConcurrentHashMap<>();
		if (json != null && !json.isEmpty()) {
			try {
				JsonNode tree = new ObjectMapper().readTree(json);
				final Iterator<Map.Entry<String, JsonNode>> elements = tree
						.fields();
				Map.Entry<String, JsonNode> map;
				while (elements.hasNext()) {
					map = elements.next();
					String key = map.getKey();
					JsonNode value = map.getValue();
					if (value.isArray()) {
						List<String> filterValues = new ArrayList<>();
						for (JsonNode node : value) {
							if (node.isTextual()) {
								filterValues.add(node.asText());
							} else if (node.isObject()) {
								getNestedValues(node);
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<String, Object> getNestedValues(JsonNode node) {
		final Iterator<Map.Entry<String, JsonNode>> elements = node.fields();
		Map.Entry<String, JsonNode> map;
		while (elements.hasNext()) {
			map = elements.next();
			String key = map.getKey();
			JsonNode value = map.getValue();
			List<String> filterValues = new ArrayList<>();
			Map<String, Object> temp = new ConcurrentHashMap<>();
			if (value.isArray()) {
				ArrayNode arrayNode = (ArrayNode) value;
				for (JsonNode val : arrayNode) {
					if (val.isTextual()) {
						filterValues.add(val.asText());
					} else if (val.isObject()) {
						temp.put(key, (Object) getNestedValues(val));
						return temp;
					}
				}
				if (!filterValues.isEmpty()) {
					temp.put(key, filterValues);
					return temp;
				}
			} else if (value.isObject()) {
				Map<String, Object> temp2 = new ConcurrentHashMap<>();
				temp2.put(key, getNestedValues(value));
				return temp2;
			}
		}
		return new ConcurrentHashMap<>();
	}

	public void generateClassFromSchema(String jsonSchema) throws IOException {
		JCodeModel codeModel = new JCodeModel();

		// URL source = new URL(jsonSchema);

		GenerationConfig config = new DefaultGenerationConfig() {
			@Override
			public boolean isGenerateBuilders() { // set config option by
													// overriding method
				return true;
			}
		};

		SchemaMapper mapper = new SchemaMapper(new RuleFactory(config,
				new Jackson2Annotator(config), new SchemaStore()),
				new SchemaGenerator());
		mapper.generate(codeModel, "ClassName", "com.example", jsonSchema);

		File outputDirectory = new File("output");
		if (outputDirectory.exists()) {
			outputDirectory.delete();
			if (outputDirectory.mkdirs())
				System.out.println("created");
		}
		if (outputDirectory.exists())
			codeModel.build(outputDirectory);
	}

}

class Node {

	boolean hasChildren;
	String childName;
	Node next;

	Node(String childName) {
		if (childName.contains("\\.")) {
			String children[] = childName.split("\\.");
			this.hasChildren = true;
			this.childName = children[0];
			StringBuilder temp = new StringBuilder();
			for (int i = 1; i < children.length; i++)
				temp.append(children[i]);
			this.next = new Node(temp.toString());
		} else {
			this.hasChildren = false;
			this.next = null;
			this.childName = childName;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((childName == null) ? 0 : childName.hashCode());
		result = prime * result + (hasChildren ? 1231 : 1237);
		result = prime * result + ((next == null) ? 0 : next.hashCode());
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
		Node other = (Node) obj;
		if (childName == null) {
			if (other.childName != null)
				return false;
		} else if (!childName.equals(other.childName))
			return false;
		if (hasChildren != other.hasChildren)
			return false;
		if (next == null) {
			if (other.next != null)
				return false;
		} else if (!next.equals(other.next))
			return false;
		return true;
	}
	
	
}