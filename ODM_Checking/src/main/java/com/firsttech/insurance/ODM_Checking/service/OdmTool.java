package com.firsttech.insurance.ODM_Checking.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OdmTool {

	private enum odmCase {
		NB, TA, ETS
	}
	
	public String setODM(String v) {
		switch (v.toLowerCase()) {
		case "nb":
			return odmCase.NB.name().toLowerCase();
		case "ta":
			return odmCase.TA.name().toLowerCase();
		case "ets":
			return odmCase.ETS.name().toLowerCase();
		}
		return null;
	}

	public JsonNode callODM(String API, String input) {
		ObjectMapper mapper = new ObjectMapper();
		DefaultHttpClient httpClient = new DefaultHttpClient();

		try {
			HttpPost requestPost = new HttpPost(API);
			requestPost.setHeader("Accept", "application/json");
			requestPost.setHeader("Content-type", "application/json");
			requestPost.setEntity(new StringEntity(input, "UTF-8"));
			HttpResponse httpResponse = httpClient.execute(requestPost);
			int statusCode = httpResponse.getStatusLine().getStatusCode();

			if (statusCode >= 200 && statusCode < 300) {
				JsonNode output = mapper.readTree(EntityUtils.toString(httpResponse.getEntity(), "UTF-8"));
				List<String> noteCode = output.path("outParam").path("resultItem").findValuesAsText("noteCode");
				return output;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return null;
	}

	public List<String> getNBNodeCode(JsonNode json) {
		return json.path("outParam").path("resultItem").findValuesAsText("noteCode");
	}

	public List<String> getTANodeCode(JsonNode json) {
		return json.path("VerifyResult").path("resultItem").findValuesAsText("noteCode");
	}

	public List<String> getComparison(List<String> oldCode, List<String> newCode) {
		Set<String> oldSet = new HashSet<>(oldCode);
		Set<String> newSet = new HashSet<>(newCode);
		Set<String> added = new HashSet<>(newSet);
		added.removeAll(oldCode);
		Set<String> removed = new HashSet<>(oldSet);
		removed.removeAll(newCode);
		List<String> diff = new ArrayList<>();
		for (String i : added) {
			diff.add("+" + i);
		}
		for (String i : removed) {
			diff.add("-" + i);
		}
		return diff;
	}

	public Boolean isEqual(List<String> nodeCode1, List<String> nodeCode2) {
		if (nodeCode1.equals(nodeCode2))
			return true;
		return false;
	}
}
