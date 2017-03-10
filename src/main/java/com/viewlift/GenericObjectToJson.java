package com.viewlift;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GenericObjectToJson {

	final static ObjectMapper mapper = new ObjectMapper();
	final static String ObjectType = "java.lang.Object";
	final static String StringType = "java.lang.String";
	final static String IntegerType = "java.lang.Integer";
	final static String LongType = "java.lang.Long";
	final static String BooleanType = "java.lang.Boolean";
	final static String MapType = "java.util.Map";
	final static String ListType = "java.util.List";
	static final BeanUtilsBean BEAN_UTILS_BEAN = BeanUtilsBean.getInstance();

	@SuppressWarnings("unchecked")
	public static Object objectToJson(Object object) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException{
		Object obj = null;
		if (object instanceof Map) {
			obj = createJsonFromMap((Map<String, Object>) object);
			System.out.println(obj.toString());
		} else if (object instanceof List) {
			obj = createJsonFromArray((List<Object>) object);
			System.out.println(obj.toString());
		} else if (object instanceof Object) {
			obj = createJsonFromObject(object);
			System.out.println(obj.toString());
		}
		return obj;
	}

	static boolean isPrimitiveType(Object object) {
		if (object instanceof String || object instanceof Number
				|| object instanceof Boolean) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private static Object createJsonFromObject(Object object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
			{
		String className[] = object.getClass().getName().split("\\.");
		String parentKey = className[className.length - 1];
		ObjectNode topNode = mapper.createObjectNode();
		ObjectNode tempNode = mapper.createObjectNode();
//		Field[] fields = object.getClass().getDeclaredFields();
//		for (Field field : fields) {
//			Object fieldType = field.get(object);
//			String fieldPackage[] = field.toString().split(" ")[1].split("\\.");
//			String fieldName = fieldPackage[fieldPackage.length - 1];
//			if (isPrimitiveType(fieldType)) {
//				if (fieldType instanceof String)
//					tempNode.put(fieldName, (String) fieldType);
//				else if (fieldType instanceof Boolean)
//					tempNode.put(fieldName, (Boolean) fieldType);
//				else if (fieldType instanceof Integer)
//					tempNode.put(fieldName, (Integer) fieldType);
//			} else {
//				Object returnedValue = objectToJson(fieldType);
//				if(returnedValue!=null)
//					tempNode.put(fieldName, ((JsonNode) objectToJson(fieldType)));
//			}
//		}
		for (Method method : object.getClass().getDeclaredMethods()) {
		    if (Modifier.isPublic(method.getModifiers())
		        && method.getParameterTypes().length == 0
		        && method.getReturnType() != void.class
		        && (method.getName().startsWith("get") || method.getName().startsWith("is"))
		    ) {
		        Object value = method.invoke(object);
		        if (value != null) {
		            System.out.println(method.getName() + "=" + value);
		        }
		    }
		}
		topNode.set(parentKey,tempNode);
		return mapper.convertValue(topNode, JsonNode.class);
	}

	@SuppressWarnings("unchecked")
	private static Object createJsonFromArray(List<Object> node) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		ArrayNode fields = mapper.createArrayNode();
		for (Object field : node) {
			if (isPrimitiveType(field)) {
				if (field instanceof String)
					fields.add((String)field);
				else if (field instanceof Boolean)
					fields.add((Boolean)field);
				else if (field instanceof Integer)
					fields.add((Integer)field);
			}
			else if (field instanceof Map) {
				fields.add((JsonNode) createJsonFromMap((Map<String, Object>) field));
			} else if (field instanceof List) {
				fields.add((JsonNode) createJsonFromArray((List<Object>) field));
			}else{
				fields.add((JsonNode)createJsonFromObject(field));
			}
		}

		return mapper.convertValue(fields, JsonNode.class);
	}

	@SuppressWarnings("unchecked")
	private static Object createJsonFromMap(Map<String, Object> jsonNode) {

		final ObjectNode objectFields = mapper.createObjectNode();
		Set<Entry<String, Object>> elements = jsonNode.entrySet();

		for (Entry<String, Object> map : elements) {

			String key = map.getKey();
			Object nextNode = map.getValue();

			if (nextNode instanceof Number) {
				objectFields.put(key, (short) nextNode);
			} else if (nextNode instanceof String) {
				objectFields.put(key, (String) nextNode);
			} else if (nextNode instanceof Boolean) {
				objectFields.put(key, (Boolean) nextNode);
			} else if (nextNode instanceof List) {
				ArrayNode internalFields = mapper.createArrayNode();
				List<Object> innerList = (List<Object>) nextNode;

				for (Object tempNode : innerList) {
					if (tempNode instanceof Map)
						internalFields
								.add((JsonNode) createJsonFromMap((Map<String, Object>) tempNode));
				}

				objectFields.set(map.getKey(), internalFields);
			} else if (nextNode instanceof Map) {
				objectFields
						.set(map.getKey(),
								(JsonNode) createJsonFromMap((Map<String, Object>) nextNode));
			}
		}
		return mapper.convertValue(objectFields, JsonNode.class);
	}

	public static void main(String as[]) throws IOException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		User use = new User();
		objectToJson(use);
	}
}

class User {
	Object value;
	String key;
	List<Object> list;
	Map<String, Object> map;

	public User() {
		this.value = 10;
		this.key = "chandra";
		this.list = fillListData();

	}

	User(String key, Object val) {
		this.key = key;
		this.value = val;
	}

	private List<Object> fillListData() {
		List<Object> list = new ArrayList<>();
		List<Object> iList = new ArrayList<>();
		iList.add("abc");
		iList.add("abc1");
		iList.add("abc2");
		iList.add("abc3");
		list.add(new User("parent1", "children1"));
		list.add(new User("parent2", 12));
		list.add(new User("parent3", false));
		list.add(new User("parent4", "children2"));
		list.add(new User("list1", iList));
		return list;
	}
}
