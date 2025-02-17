/*******************************************************************************
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.publicissapient.kpidashboard.jira.util;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;

@RunWith(MockitoJUnitRunner.class)
public class JiraProcessorUtilTest {

	@Mock
	SprintDetails sprintDetails;

	@Test
	public void deodeUTF8StringNull() {
		Object jiraResponse = null;
		assertNotNull(JiraProcessorUtil.deodeUTF8String(jiraResponse));

	}

	@Test
	public void testDecodeUTF8String_NullInput() {
		String result = JiraProcessorUtil.deodeUTF8String(null);

		assertEquals("", result);
	}

	@Test
	public void testDecodeUTF8String_EmptyInput() {
		Object jiraResponse = ""; // or null, or any other empty string format handled in your method
		String result = JiraProcessorUtil.deodeUTF8String(jiraResponse);

		assertEquals("", result);
	}

	@Test
	public void testDecodeUTF8String_NormalInput() {
		Object jiraResponse = "Some UTF-8 String"; // Replace with your test input
		String result = JiraProcessorUtil.deodeUTF8String(jiraResponse);
		assertTrue(!result.isEmpty());

	}

	@Test
	public void getFormattedDate() {
		String date = "07-09-2021";
		assertNotNull(JiraProcessorUtil.getFormattedDate(date));

	}

	@Test
	public void getFormattedDateForSprintDetails() {
		String date = "2024-01-03T23:01:29.666+05:30";
		assertNotNull(JiraProcessorUtil.getFormattedDateForSprintDetails(date));
	}

	@Test
	public void setSprintDetailsFromString() {
		String str = "\n"
				+"\"values\": ["+"id= 31227,"+"state= closed,"
				+"name= Test|PI_5|ITR_6|9 Jun-29Jun,"
				+"startDate= 2021-06-09T08:38:00.000Z,"
				+"endDate= 2021-06-29T08:38:00.000Z,"
				+"completeDate= 2021-06-30T05:27:26.503Z,"
				+"activatedDate= 2021-06-09T08:38:16.563Z,"
				+"originBoardId= 11856,"
				+"goal=1 " + "    ]";
		assertNull(JiraProcessorUtil.setSprintDetailsFromString(str, sprintDetails));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void processSprintDetail() throws ParseException, JSONException {
		List<String> list = new ArrayList<String>();
		list.add("User1");
		list.add("User2");
		list.add("User3");
		JSONArray array = new JSONArray();
		for (int i = 0; i < list.size(); i++) {
			JSONObject d = new JSONObject();
			d.put(i, list.get(i));
			array.add(d);
		}
		Object data = array;

		assertNotNull(JiraProcessorUtil.processSprintDetail(data));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void processSprintDetailNull() throws ParseException, JSONException {
		List<String> list = new ArrayList<String>();
		list.add("User1");
		list.add("User2");
		list.add(null);
		JSONArray array = new JSONArray();
		for (int i = 0; i < list.size(); i++) {
			JSONObject d = new JSONObject();
			d.put(i, list.get(i));
			array.add(d);
		}
		Object data = array;

		assertNotNull(JiraProcessorUtil.processSprintDetail(data));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void processSprintDetail1() throws ParseException, JSONException {
		List<String> list = new ArrayList<String>();
		list.add("User1");
		list.add("User2");
		list.add("User3");
		org.codehaus.jettison.json.JSONArray array = new org.codehaus.jettison.json.JSONArray();
		for (int i = 0; i < list.size(); i++) {
			JSONObject d = new JSONObject();
			d.put(i, list.get(i));
			array.put(d);
		}
		Object data = array;

		assertNotNull(JiraProcessorUtil.processSprintDetail(data));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void processSprintDetail1Null() throws ParseException, JSONException {
		List<String> list = new ArrayList<String>();
		list.add("User1");
		list.add("User2");
		list.add(null);
		org.codehaus.jettison.json.JSONArray array = new org.codehaus.jettison.json.JSONArray();
		for (int i = 0; i < list.size(); i++) {
			JSONObject d = new JSONObject();
			d.put(i, list.get(i));
			array.put(d);
		}
		Object data = array;

		assertNotNull(JiraProcessorUtil.processSprintDetail(data));

	}

	@Test
	public void processJqlForSprintFetch() {
		// Arrange
		List<String> issueKeys = Arrays.asList("KEY-1", "KEY-2", "KEY-3");
		String expected = "issueKey in (KEY-1, KEY-2, KEY-3)";

		// Act
		String actual = JiraProcessorUtil.processJqlForSprintFetch(issueKeys);

		// Assert
		assertEquals(expected, actual);
	}
}