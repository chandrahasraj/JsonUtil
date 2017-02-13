package com.viewlift;

import java.util.List;


public class JsonComparator {

	
	public void compareJsons(GenericJson server, GenericJson client){
		if(server == null || client == null)
			return;
		
		if(server.valueHasNoKey() && client.valueHasNoKey()){
		}
		
		if(server.valueHasNoKey()){
			if(client.valueHasNoKey()){
				if(server.getKey()!=null && server.getKey().equals(client.getKey()))
					return;
				else
					return;
			}else
				return;
		}
		
		if(server.valueIsList()){
			List<Object> serverObjs = server.getValueList();
			List<Object> clientObjs = server.getValueList();
			for(Object obj:serverObjs){
				if(clientObjs.contains(obj)){
					server = (GenericJson)obj;
					client = (GenericJson)clientObjs.get(clientObjs.indexOf(obj));
					compareJsons(server,client);
				}else{
					serverObjs.remove(obj);
				}
			}
		}
		
		
		List<String> clientProvidedKeys = client.getKeysInThatNode();
		List<String> serverObtainedKeys = server.getKeysInThatNode();
		for(String key:serverObtainedKeys){
			if(clientProvidedKeys.contains(key)){
				server = GenericJson.find(key, server);
				client = GenericJson.find(key, client);
				compareJsons(server,client);
			}else{
				server.removeChild(new GenericJson(key));
			}
		}
	}
}
