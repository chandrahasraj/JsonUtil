package com.viewlift;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonSchemaGenerator {

	private static final String SCHEMA = "$schema";
	private static final String TYPE = "type";
	private static final String ARRAY = "array";
	private static final String ITEMS = "items";
	private static final String STRING = "string";
	private static final String BOOLEAN = "boolean";
	private static final String OBJECT = "object";
	private static final String REQUIRED = "required";
	private static final String PROPERTIES = "properties";

	private final ObjectMapper mapper;

	public JsonSchemaGenerator(final ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public String convert(final String json) throws IOException {
		final JsonNode jsonNode = mapper.readTree(json);
		final ObjectNode finalSchema = mapper.createObjectNode();
		final ArrayNode requiredFields = mapper.createArrayNode();
		finalSchema.put(SCHEMA, "http://json-schema.org/draft-04/schema#");
		if (jsonNode.isArray()) {
			int size = jsonNode.size();
			if (size > 0) {
				ObjectNode node = getObjectProperties(jsonNode.get(size - 1), requiredFields);
				finalSchema.put(TYPE, ARRAY);
				ObjectNode properties = mapper.createObjectNode();
				properties.put(TYPE,OBJECT);
				properties.set(REQUIRED, requiredFields);
				finalSchema.set(ITEMS, properties.set(PROPERTIES, node));
			}else{
				finalSchema.put(TYPE, ARRAY);
				finalSchema.set(ITEMS, mapper.createObjectNode());
			}
		} else if(jsonNode.isObject()){
			finalSchema.put(TYPE, OBJECT);
			finalSchema.set(
					PROPERTIES,
					getObjectProperties(jsonNode, requiredFields).set(REQUIRED,
							requiredFields));
		}
		
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
				finalSchema);
	}
	
	List<String> getRequiredFieldsAsList(ArrayNode requiredFields){
		List<String> list = new ArrayList<>();
		for(JsonNode node:requiredFields){
			list.add(node.textValue());
		}
		return list;
	}

	private ObjectNode getObjectProperties(final JsonNode jsonNode,
			ArrayNode requiredFields) {

		if (jsonNode.isTextual())
			return mapper.createObjectNode().put(TYPE, STRING);
		else if (jsonNode.isBoolean())
			return mapper.createObjectNode().put(TYPE, BOOLEAN);
		else if (jsonNode.isNumber())
			return mapper.createObjectNode().put(TYPE, jsonNode.isLong() ? "long" : jsonNode.isInt() ? "integer": "double");

		final ObjectNode objectFields = mapper.createObjectNode();
		final Iterator<Map.Entry<String, JsonNode>> elements = jsonNode.fields();

		Map.Entry<String, JsonNode> map;
		while (elements.hasNext()) {
			map = elements.next();
			final JsonNode nextNode = map.getValue();

			switch (nextNode.getNodeType()) {
			case NUMBER:
				requiredFields.add(map.getKey());
				objectFields.set(map.getKey(), mapper.createObjectNode().put(TYPE, (nextNode.isLong() ? "long" : "double")));
				break;

			case STRING:
				requiredFields.add(map.getKey());
				objectFields.set(map.getKey(), mapper.createObjectNode().put(TYPE, STRING));
				break;

			case BOOLEAN:
				requiredFields.add(map.getKey());
				objectFields.set(map.getKey(), mapper.createObjectNode().put(TYPE, BOOLEAN));
				break;

			case ARRAY:
				requiredFields.add(map.getKey());
				ArrayNode internalRequiredFields = mapper.createArrayNode();
				ObjectNode internalNodeArray = mapper.createObjectNode();
				if (nextNode.size() > 0) {
					if (nextNode.get(nextNode.size() - 1).isArray()
							|| nextNode.get(nextNode.size() - 1).isObject()) {
						ObjectNode internalNodeObject = mapper
								.createObjectNode();
						internalNodeObject.set(
								PROPERTIES,
								getObjectProperties(
										nextNode.get(nextNode.size() - 1),
										internalRequiredFields));
						if (internalRequiredFields.size() > 0) {
							internalNodeObject.put(TYPE, OBJECT);
							internalNodeObject.set(REQUIRED,
									internalRequiredFields);
						}
						objectFields.set(
								map.getKey(),
								internalNodeArray.put(TYPE, ARRAY).set(ITEMS,
										internalNodeObject));
					} else {
						objectFields.set(
								map.getKey(),
								internalNodeArray.put(TYPE, ARRAY).set(
										ITEMS,
										getObjectProperties(nextNode
												.get(nextNode.size() - 1),
												internalRequiredFields)));
					}
				}else{
					objectFields.set(map.getKey(), internalNodeArray.put(TYPE, ARRAY).set(ITEMS,mapper.createObjectNode()));
				}
				break;

			case OBJECT:
				ArrayNode internalRequiredFieldsObject = mapper.createArrayNode();
				ObjectNode internalNodeObject = mapper.createObjectNode();
				requiredFields.add(map.getKey());
				ObjectNode node = objectFields;
				node.set(map.getKey(), internalNodeObject.put(TYPE, OBJECT).set(PROPERTIES, getObjectProperties(nextNode, internalRequiredFieldsObject)));
				if (internalRequiredFieldsObject.size() > 0){
					internalNodeObject.set(REQUIRED, internalRequiredFieldsObject);
				}
				break;

			default:
				throw new RuntimeException(
						"Unable to determine action for ndoetype "
								+ nextNode.getNodeType()
								+ "; Allowed types are ARRAY,BOOLEAN, STRING, NUMBER, OBJECT");
			}
		}
		return objectFields;
	}
}
