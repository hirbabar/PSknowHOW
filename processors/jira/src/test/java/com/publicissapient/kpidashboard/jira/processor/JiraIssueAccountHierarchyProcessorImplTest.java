package com.publicissapient.kpidashboard.jira.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.constant.ProcessorConstants;
import com.publicissapient.kpidashboard.common.model.application.AccountHierarchy;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.application.HierarchyLevel;
import com.publicissapient.kpidashboard.common.model.application.ProjectBasicConfig;
import com.publicissapient.kpidashboard.common.model.application.ProjectToolConfig;
import com.publicissapient.kpidashboard.common.model.connection.Connection;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;
import com.publicissapient.kpidashboard.common.repository.application.AccountHierarchyRepository;
import com.publicissapient.kpidashboard.common.service.HierarchyLevelService;
import com.publicissapient.kpidashboard.jira.dataFactories.AccountHierarchiesDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.ConnectionsDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.FieldMappingDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.HierachyLevelFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.JiraIssueDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.ProjectBasicConfigDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.SprintDetailsDataFactory;
import com.publicissapient.kpidashboard.jira.dataFactories.ToolConfigDataFactory;
import com.publicissapient.kpidashboard.jira.model.JiraToolConfig;
import com.publicissapient.kpidashboard.jira.model.ProjectConfFieldMapping;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueAccountHierarchyProcessorImplTest {

	List<HierarchyLevel> hierarchyLevelList;
	List<AccountHierarchy> accountHierarchyList;
	List<AccountHierarchy> accountHierarchies;
	List<ProjectToolConfig> projectToolConfigs;
	List<FieldMapping> fieldMappingList;
	Optional<Connection> connection;
	List<JiraIssue> jiraIssues;
	List<ProjectBasicConfig> projectConfigsList;
	@Mock
	private HierarchyLevelService hierarchyLevelService;
	@Mock
	private AccountHierarchyRepository accountHierarchyRepository;
	@Mock
	private SprintDetails sprintDetails;
	@InjectMocks
	private JiraIssueAccountHierarchyProcessorImpl createAccountHierarchy;

	@Before
	public void setup() {
		hierarchyLevelList = getMockHierarchyLevel();
		AccountHierarchiesDataFactory accountHierarchiesDataFactory = AccountHierarchiesDataFactory
				.newInstance("/json/default/account_hierarchy.json");
		accountHierarchyList = accountHierarchiesDataFactory.getAccountHierarchies();
		accountHierarchies = accountHierarchiesDataFactory.findByLabelNameAndBasicProjectConfigId(
				CommonConstant.HIERARCHY_LEVEL_ID_PROJECT, "63c04dc7b7617e260763ca4e");
		projectToolConfigs = getMockProjectToolConfig();
		fieldMappingList = getMockFieldMapping();
		connection = getMockConnection();
		jiraIssues = getMockJiraIssue();
		projectConfigsList = getMockProjectConfig();
		SprintDetailsDataFactory sprintDetailsDataFactory = SprintDetailsDataFactory.newInstance();
		sprintDetails = sprintDetailsDataFactory.getSprintDetails().get(0);
	}

	@Test
	public void createAccountHierarchy() {
		when(hierarchyLevelService.getFullHierarchyLevels(false)).thenReturn(hierarchyLevelList);
		when(accountHierarchyRepository.findAll()).thenReturn(accountHierarchyList);
		when(accountHierarchyRepository.findByLabelNameAndBasicProjectConfigId(any(), any()))
				.thenReturn(accountHierarchies);
		Assert.assertEquals(2, createAccountHierarchy
				.createAccountHierarchy(jiraIssues.get(0), createProjectConfig(), getSprintDetails())
				.size());
	}

	private List<HierarchyLevel> getMockHierarchyLevel() {
		HierachyLevelFactory hierarchyLevelFactory = HierachyLevelFactory
				.newInstance("/json/default/hierarchy_levels.json");
		return hierarchyLevelFactory.getHierarchyLevels();
	}

	private Set<SprintDetails> getSprintDetails() {
		Set<SprintDetails> set = new HashSet<>();
		SprintDetails sprintDetails = new SprintDetails();
		sprintDetails.setSprintID("41409_NewJira_63c04dc7b7617e260763ca4e");
		sprintDetails.setOriginalSprintId("41409");
		sprintDetails.setState("ACTIVE");
		sprintDetails.setBasicProjectConfigId(new ObjectId("63c04dc7b7617e260763ca4e"));
		List<String> list = new ArrayList<>();
		list.add("41409");
		sprintDetails.setOriginBoardId(list);
		set.add(sprintDetails);
		return set;
	}

	private List<JiraIssue> getMockJiraIssue() {
		JiraIssueDataFactory jiraIssueDataFactory = JiraIssueDataFactory.newInstance("/json/default/jira_issues.json");
		return jiraIssueDataFactory.getJiraIssues();
	}

	private List<ProjectBasicConfig> getMockProjectConfig() {
		ProjectBasicConfigDataFactory projectConfigDataFactory = ProjectBasicConfigDataFactory
				.newInstance("/json/default/project_basic_configs.json");
		return projectConfigDataFactory.getProjectBasicConfigs();
	}

	private ProjectConfFieldMapping createProjectConfig() {
		ProjectConfFieldMapping projectConfFieldMapping = ProjectConfFieldMapping.builder().build();
		ProjectBasicConfig projectConfig = projectConfigsList.get(2);
		try {
			BeanUtils.copyProperties(projectConfFieldMapping, projectConfig);
		} catch (IllegalAccessException | InvocationTargetException e) {

		}
		projectConfFieldMapping.setProjectBasicConfig(projectConfig);
		projectConfFieldMapping.setKanban(projectConfig.getIsKanban());
		projectConfFieldMapping.setBasicProjectConfigId(projectConfig.getId());
		projectConfFieldMapping.setJira(getJiraToolConfig());
		projectConfFieldMapping.setJiraToolConfigId(projectToolConfigs.get(0).getId());
		projectConfFieldMapping.setFieldMapping(fieldMappingList.get(1));

		return projectConfFieldMapping;
	}

	private JiraToolConfig getJiraToolConfig() {
		JiraToolConfig toolObj = new JiraToolConfig();
		try {
			BeanUtils.copyProperties(toolObj, projectToolConfigs.get(0));
		} catch (IllegalAccessException | InvocationTargetException e) {

		}
		toolObj.setConnection(connection);
		return toolObj;
	}

	private List<ProjectToolConfig> getMockProjectToolConfig() {
		ToolConfigDataFactory projectToolConfigDataFactory = ToolConfigDataFactory
				.newInstance("/json/default/project_tool_configs.json");
		return projectToolConfigDataFactory.findByToolNameAndBasicProjectConfigId(ProcessorConstants.JIRA,
				"63c04dc7b7617e260763ca4e");
	}

	private Optional<Connection> getMockConnection() {
		ConnectionsDataFactory connectionDataFactory = ConnectionsDataFactory
				.newInstance("/json/default/connections.json");
		return connectionDataFactory.findConnectionById("5fd99f7bc8b51a7b55aec836");
	}

	private List<FieldMapping> getMockFieldMapping() {
		FieldMappingDataFactory fieldMappingDataFactory = FieldMappingDataFactory
				.newInstance("/json/default/field_mapping.json");
		return fieldMappingDataFactory.getFieldMappings();
	}

}