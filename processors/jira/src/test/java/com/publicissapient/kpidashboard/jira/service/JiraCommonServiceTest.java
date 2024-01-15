package com.publicissapient.kpidashboard.jira.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.BasicVotes;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.FieldType;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.Visibility;
import com.atlassian.jira.rest.client.api.domain.Worklog;
import com.publicissapient.kpidashboard.common.client.KerberosClient;
import com.publicissapient.kpidashboard.common.constant.ProcessorConstants;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.application.ProjectBasicConfig;
import com.publicissapient.kpidashboard.common.model.application.ProjectToolConfig;
import com.publicissapient.kpidashboard.common.model.application.ProjectVersion;
import com.publicissapient.kpidashboard.common.model.connection.Connection;
import com.publicissapient.kpidashboard.common.service.AesEncryptionService;
import com.publicissapient.kpidashboard.common.service.ToolCredentialProvider;
import com.publicissapient.kpidashboard.jira.client.CustomAsynchronousIssueRestClient;
import com.publicissapient.kpidashboard.jira.client.ProcessorJiraRestClient;
import com.publicissapient.kpidashboard.jira.config.JiraProcessorConfig;
import com.publicissapient.kpidashboard.jira.dataFactories.ConnectionsDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.FieldMappingDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.ProjectBasicConfigDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.ToolConfigDataFactory;
import com.publicissapient.kpidashboard.jira.model.JiraToolConfig;
import com.publicissapient.kpidashboard.jira.model.ProjectConfFieldMapping;

import io.atlassian.util.concurrent.Promise;

@RunWith(MockitoJUnitRunner.class)
public class JiraCommonServiceTest {

	@Mock
	private JiraProcessorConfig jiraProcessorConfig;

	@Mock
	SearchRestClient searchRestClient;

	@Mock
	CustomAsynchronousIssueRestClient customAsynchronousIssueRestClient;

	@Mock
	Promise<SearchResult> promisedRs;

	SearchResult searchResult;

	@Mock
	private ToolCredentialProvider toolCredentialProvider;

	@Mock
	private AesEncryptionService aesEncryptionService;

	@InjectMocks
	JiraCommonService jiraCommonService;

	@Mock
	ProcessorJiraRestClient jiraRestClient;

	@Mock
	KerberosClient krb5Client;

	private ProjectConfFieldMapping projectConfFieldMapping = ProjectConfFieldMapping.builder().build();
	List<ProjectBasicConfig> projectConfigsList;
	List<ProjectToolConfig> projectToolConfigsJQL;
	List<ProjectToolConfig> projectToolConfigsBoard;
	Optional<Connection> connection;
	FieldMapping fieldMapping = new FieldMapping();

	List<Issue> issues = new ArrayList<>();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		projectConfigsList = getMockProjectConfig();
		connection = getMockConnection();
		fieldMapping = getMockFieldMapping();
		createIssue();
		//when(jiraProcessorConfig.getAesEncryptionKey()).thenReturn("AesEncryptionKey");
		//when(aesEncryptionService.decrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
		//		.thenReturn("PLAIN_TEXT_PASSWORD");
	}

	@Test
	public void fetchIssuesBasedOnJqlTest() throws InterruptedException {
		projectToolConfigsJQL = getMockProjectToolConfig("63c04dc7b7617e260763ca4e");
		createProjectConfigMap(true);
		Mockito.when(jiraProcessorConfig.getPageSize()).thenReturn(50);
		when(jiraRestClient.getProcessorSearchClient()).thenReturn(searchRestClient);
		when(searchRestClient.searchJql(anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anySet()))
				.thenReturn(promisedRs);
		when(promisedRs.claim()).thenReturn(searchResult);
		String deltaDate = "2023-10-20 08:22";
		List<Issue> issues = jiraCommonService.fetchIssuesBasedOnJql(projectConfFieldMapping, jiraRestClient, 50,
				deltaDate);
		Assert.assertEquals(2, issues.size());
	}

	@Test
	public void fetchIssueBasedOnBoardTest() throws InterruptedException, IOException {
		projectToolConfigsBoard = getMockProjectToolConfig("63bfa0d5b7617e260763ca21");
		createProjectConfigMap(false);
		Mockito.when(jiraProcessorConfig.getPageSize()).thenReturn(50);
		when(jiraRestClient.getCustomIssueClient()).thenReturn(customAsynchronousIssueRestClient);
		when(customAsynchronousIssueRestClient.searchBoardIssue(anyString(), anyString(), Mockito.anyInt(),
				Mockito.anyInt(), Mockito.anySet())).thenReturn(promisedRs);
		when(promisedRs.claim()).thenReturn(searchResult);
		String deltaDate = "2023-10-20 08:22";
		List<Issue> issues = jiraCommonService.fetchIssueBasedOnBoard(projectConfFieldMapping, jiraRestClient, 50,
				"1111", deltaDate);
		Assert.assertEquals(2, issues.size());
	}

	// @Test
	public void getVersionTest() throws IOException, ParseException {
		projectToolConfigsBoard = getMockProjectToolConfig("63bfa0d5b7617e260763ca21");
		createProjectConfigMap(false);
		Mockito.when(jiraProcessorConfig.getPageSize()).thenReturn(50);
		when(jiraProcessorConfig.getJiraVersionApi()).thenReturn("rest/api/2/project/{projectKey}/versions");
		URL mockedUrl = new URL("https://tools.publicis.sapient.com/rest/api/2/project/DTS/versions");
		HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
		HttpURLConnection request = (HttpURLConnection) mockedUrl.openConnection();
		Optional<Connection> mockedConnectionOptional = Optional.of(Mockito.mock(Connection.class));

		// Sample response data
		String responseData = "Sample response data";
		InputStream inputStream = new ByteArrayInputStream(responseData.getBytes(StandardCharsets.UTF_8));

		// Mock behavior for URL and HttpURLConnection
		// when(mockedUrl.openConnection()).thenReturn(mockedConnection);
		// when(request.getContent()).thenReturn(inputStream);

		// Create an instance of your class
		// YourClass yourClass = new YourClass();
		//
		// // Call the method to be tested
		// String result = yourClass.getDataFromServer(mockedUrl,
		// mockedConnectionOptional);
		//
		// // Verify the result
		// assertEquals(responseData, result);

		// Verify that disconnect() is called on the HttpURLConnection
		// Mockito.verify(mockedConnection).disconnect();
		List<ProjectVersion> versions = jiraCommonService.getVersion(projectConfFieldMapping, krb5Client);
		Assert.assertEquals(2, versions.size());
	}

	@Test
	public void testGetApiHost() {
		when(jiraProcessorConfig.getUiHost()).thenReturn("localhost");
		try {
			jiraCommonService.getApiHost();
		} catch (UnknownHostException e) {

		}

	}

	private void createIssue() throws URISyntaxException {
		BasicProject basicProj = new BasicProject(new URI("self"), "proj1", 1l, "project1");
		IssueType issueType1 = new IssueType(new URI("self"), 1l, "Story", false, "desc", new URI("iconURI"));
		IssueType issueType2 = new IssueType(new URI("self"), 2l, "Defect", false, "desc", new URI("iconURI"));
		Status status1 = new Status(new URI("self"), 1l, "Ready for Sprint Planning", "desc", new URI("iconURI"),
				new StatusCategory(new URI("self"), "name", 1l, "key", "colorname"));
		BasicPriority basicPriority = new BasicPriority(new URI("self"), 1l, "priority1");
		Resolution resolution = new Resolution(new URI("self"), 1l, "resolution", "resolution");
		Map<String, URI> avatarMap = new HashMap<>();
		avatarMap.put("48x48", new URI("value"));
		User user1 = new User(new URI("self"), "user1", "user1", "userAccount", "user1@xyz.com", true, null, avatarMap,
				null);
		Map<String, String> map = new HashMap<>();
		map.put("customfield_12121", "Client Testing (UAT)");
		map.put("self", "https://jiradomain.com/jira/rest/api/2/customFieldOption/20810");
		map.put("value", "Component");
		map.put("id", "20810");
		JSONObject value = new JSONObject(map);
		IssueField issueField = new IssueField("20810", "Component", null, value);
		List<IssueField> issueFields = Arrays.asList(issueField);
		Comment comment = new Comment(new URI("self"), "body", null, null, DateTime.now(), DateTime.now(),
				new Visibility(Visibility.Type.ROLE, "abc"), 1l);
		List<Comment> comments = Arrays.asList(comment);
		BasicVotes basicVotes = new BasicVotes(new URI("self"), 1, true);
		BasicUser basicUser = new BasicUser(new URI("self"), "basicuser", "basicuser", "accountId");
		Worklog worklog = new Worklog(new URI("self"), new URI("self"), basicUser, basicUser, null, DateTime.now(),
				DateTime.now(), DateTime.now(), 60, null);
		List<Worklog> workLogs = Arrays.asList(worklog);
		ChangelogItem changelogItem = new ChangelogItem(FieldType.JIRA, "field1", "from", "fromString", "to",
				"toString");
		ChangelogGroup changelogGroup = new ChangelogGroup(basicUser, DateTime.now(), Arrays.asList(changelogItem));

		Issue issue = new Issue("summary1", new URI("self"), "key1", 1l, basicProj, issueType1, status1, "story",
				basicPriority, resolution, new ArrayList<>(), user1, user1, DateTime.now(), DateTime.now(),
				DateTime.now(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, issueFields, comments,
				null, createIssueLinkData(), basicVotes, workLogs, null, Arrays.asList("expandos"), null,
				Arrays.asList(changelogGroup), null, new HashSet<>(Arrays.asList("label1")));
		Issue issue1 = new Issue("summary1", new URI("self"), "key1", 1l, basicProj, issueType2, status1, "Defect",
				basicPriority, resolution, new ArrayList<>(), user1, user1, DateTime.now(), DateTime.now(),
				DateTime.now(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, issueFields, comments,
				null, createIssueLinkData(), basicVotes, workLogs, null, Arrays.asList("expandos"), null,
				Arrays.asList(changelogGroup), null, new HashSet<>(Arrays.asList("label1")));
		issues.add(issue);
		issues.add(issue1);

		searchResult = new SearchResult(0, 10, 2, issues);

	}

	private List<IssueLink> createIssueLinkData() throws URISyntaxException {
		List<IssueLink> issueLinkList = new ArrayList<>();
		URI uri = new URI("https://testDomain.com/jira/rest/api/2/issue/12344");
		IssueLinkType linkType = new IssueLinkType("Blocks", "blocks", IssueLinkType.Direction.OUTBOUND);
		IssueLink issueLink = new IssueLink("IssueKey", uri, linkType);
		issueLinkList.add(issueLink);

		return issueLinkList;
	}

	private void createProjectConfigMap(boolean jql) {
		ProjectBasicConfig projectConfig = projectConfigsList.get(1);
		try {
			BeanUtils.copyProperties(projectConfFieldMapping, projectConfig);
		} catch (IllegalAccessException | InvocationTargetException e) {
		}
		projectConfFieldMapping.setProjectBasicConfig(projectConfig);
		projectConfFieldMapping.setKanban(projectConfig.getIsKanban());
		projectConfFieldMapping.setBasicProjectConfigId(projectConfig.getId());
		if (jql) {
			projectConfFieldMapping.setJira(getJiraToolConfig(true));
			projectConfFieldMapping.setJiraToolConfigId(projectToolConfigsJQL.get(0).getId());
			projectConfFieldMapping.setProjectToolConfig(projectToolConfigsJQL.get(0));
		} else {
			projectConfFieldMapping.setJira(getJiraToolConfig(false));
			projectConfFieldMapping.setJiraToolConfigId(projectToolConfigsBoard.get(0).getId());
			projectConfFieldMapping.setProjectToolConfig(projectToolConfigsBoard.get(0));
		}
		projectConfFieldMapping.setFieldMapping(fieldMapping);

	}

	private JiraToolConfig getJiraToolConfig(boolean jql) {
		JiraToolConfig toolObj = new JiraToolConfig();
		try {
			if (jql) {
				BeanUtils.copyProperties(toolObj, projectToolConfigsJQL.get(0));
			} else {
				BeanUtils.copyProperties(toolObj, projectToolConfigsBoard.get(0));
			}
		} catch (IllegalAccessException | InvocationTargetException e) {

		}
		toolObj.setConnection(connection);
		return toolObj;
	}

	private Optional<Connection> getMockConnection() {
		ConnectionsDataFactory connectionDataFactory = ConnectionsDataFactory
				.newInstance("/json/default/connections.json");
		return connectionDataFactory.findConnectionById("5fd99f7bc8b51a7b55aec836");
	}

	private List<ProjectBasicConfig> getMockProjectConfig() {
		ProjectBasicConfigDataFactory projectConfigDataFactory = ProjectBasicConfigDataFactory
				.newInstance("/json/default/project_basic_configs.json");
		return projectConfigDataFactory.getProjectBasicConfigs();
	}

	private List<ProjectToolConfig> getMockProjectToolConfig(String basicConfigId) {
		ToolConfigDataFactory projectToolConfigDataFactory = ToolConfigDataFactory
				.newInstance("/json/default/project_tool_configs.json");
		return projectToolConfigDataFactory.findByToolNameAndBasicProjectConfigId(ProcessorConstants.JIRA,
				basicConfigId);
	}

	private FieldMapping getMockFieldMapping() {
		FieldMappingDataFactory fieldMappingDataFactory = FieldMappingDataFactory
				.newInstance("/json/default/field_mapping.json");
		return fieldMappingDataFactory.findByBasicProjectConfigId("63c04dc7b7617e260763ca4e");
	}

}