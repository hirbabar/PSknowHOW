package com.publicissapient.kpidashboard.jira.parser;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.FieldType;
import com.atlassian.jira.rest.client.internal.json.ChangelogItemJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class CustomChangelogJsonParserTest {

	@Mock
	private JSONObject mockJsonObject;

	@Mock
	private ChangelogItemJsonParser mockChangelogItemJsonParser;

	@InjectMocks
	CustomChangelogJsonParser customChangelogJsonParser;

	ChangelogItem changelogItem;



	JSONArray mockData = new JSONArray();

	@Before
	public void setup() throws JSONException {
		changelogItem = mockChangeLogData();
		//sampleJson = mockJSONData();
		mockData =  mockJSONArray();
	}

	@Test
	public void testParse() throws JSONException {
		MockitoAnnotations.openMocks(this);

		// Create a sample JSONObject for testing
		JSONObject sampleJson = new JSONObject();
		JSONObject changelogGroup1 = mockData.getJSONObject(0);
		sampleJson.put("created", "2022-01-11T12:34:56.555Z");
		sampleJson.put("author", changelogGroup1.getJSONObject("author"));
		sampleJson.put("items", changelogGroup1.getJSONArray("items"));

		// Mock behavior for the dependencies
		Mockito.when(mockJsonObject.getJSONObject("author")).thenReturn(changelogGroup1.getJSONObject("author"));
		Mockito.when(mockJsonObject.getJSONArray("items")).thenReturn(changelogGroup1.getJSONArray("items"));
		Mockito.when(mockChangelogItemJsonParser.parse(Mockito.any(JSONObject.class))).thenReturn(changelogItem);

		try {
			ChangelogGroup changelogGroup = customChangelogJsonParser.parse(sampleJson);

			// Add assertions based on your expected results
			Assert.assertNotNull(changelogGroup);
			// Add more assertions as needed

		} catch (JSONException e) {
			Assert.fail("Unexpected JSONException: " + e.getMessage());
		}
	}

	private ChangelogItem mockChangeLogData() {
		return new ChangelogItem(FieldType.JIRA, "field", "from", "fromString", "to", "toString");
	}

	private JSONArray mockJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		// Creating a sample JSONObject for the first changelog group
		JSONObject changelogGroup1 = new JSONObject();
		changelogGroup1.put("created", "2022-01-11T12:34:56");

		// Creating a sample author JSONObject
		JSONObject author1 = new JSONObject();
		author1.put("name", "John Doe");
		author1.put("key", "johndoe");
		author1.put("self", "http://example.com/rest/api/2/user/101");
		// Add more author details as needed

		changelogGroup1.put("author", author1);

		// Creating a sample items JSONArray for the first changelog group
		JSONArray items1 = new JSONArray();

		// Creating a sample changelog item
		JSONObject changelogItem1 = new JSONObject();
		changelogItem1.put("field", "status");
		changelogItem1.put("fieldtype", "jira");
		changelogItem1.put("fieldId", "status");
		changelogItem1.put("from", "Open");
		changelogItem1.put("fromString", "Open");
		changelogItem1.put("to", "In Progress");
		changelogItem1.put("toString", "In Progress");

		// Add more changelog items as needed

		items1.put(changelogItem1);

		changelogGroup1.put("items", items1);

		// Add the first changelog group to the JSONArray
		jsonArray.put(changelogGroup1);

		return jsonArray;
	}
}