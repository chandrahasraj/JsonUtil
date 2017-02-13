package com.viewlift;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class JsonToJava {


	public static void main(String[] args) throws IOException {
			List<String> lines=FileUtils.readLines(new File("C:\\\\Users\\viewlift\\Documents\\test_final.json"));
			String json = StringUtils.join(lines, "");
			
			List<String> lines2=FileUtils.readLines(new File("C:\\\\Users\\viewlift\\Documents\\test_client.json"));
			String json2 = StringUtils.join(lines2, "");
			
			GenericJson server = new ResponseCreator().genericReader(json);
			GenericJson client = new ResponseCreator().genericReader(json2);
			
//			new JsonComparator().compareJsons(server, client);
			
			System.out.println(server.toString());

	}

}
