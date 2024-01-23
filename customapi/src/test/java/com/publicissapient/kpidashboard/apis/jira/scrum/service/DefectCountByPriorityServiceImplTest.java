package com.publicissapient.kpidashboard.apis.jira.scrum.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.common.service.CacheService;
import com.publicissapient.kpidashboard.apis.constant.Constant;
import com.publicissapient.kpidashboard.apis.data.AccountHierarchyFilterDataFactory;
import com.publicissapient.kpidashboard.apis.data.FieldMappingDataFactory;
import com.publicissapient.kpidashboard.apis.data.JiraIssueDataFactory;
import com.publicissapient.kpidashboard.apis.data.KpiRequestFactory;
import com.publicissapient.kpidashboard.apis.data.SprintDetailsDataFactory;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.enums.KPISource;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.jira.service.JiraServiceR;
import com.publicissapient.kpidashboard.apis.model.AccountHierarchyData;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.apis.model.Node;
import com.publicissapient.kpidashboard.apis.model.TreeAggregatorDetail;
import com.publicissapient.kpidashboard.apis.util.KPIHelperUtil;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;
import com.publicissapient.kpidashboard.common.model.jira.SprintIssue;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueRepository;

@RunWith(MockitoJUnitRunner.class)
public class DefectCountByPriorityServiceImplTest {

	@InjectMocks
	DefectCountByPriorityServiceImpl defectCountByPriorityService;
	@Mock
	JiraIssueRepository jiraIssueRepository;
	@Mock
	CacheService cacheService;
	@Mock
	ConfigHelperService configHelperService;
	private KpiRequest kpiRequest;
	private SprintDetails sprintDetails;
	private List<JiraIssue> storyList = new ArrayList<>();
	private List<JiraIssue> bugList = new ArrayList<>();
	private List<AccountHierarchyData> accountHierarchyDataList = new ArrayList<>();
	private Map<ObjectId, FieldMapping> fieldMappingMap = new HashMap<>();
	@Mock
	private JiraServiceR jiraService;

	@Test
	public void testGetCalculateKPIMetrics() {
		assertThat(defectCountByPriorityService.calculateKPIMetrics(new HashMap<>()), equalTo(null));
	}

	@Test
	public void testGetQualifierType() {
		assertThat(defectCountByPriorityService.getQualifierType(),
				equalTo(KPICode.DEFECT_COUNT_BY_PRIORITY_PIE_CHART.name()));
	}

	@Before
	public void setup() {
		KpiRequestFactory kpiRequestFactory = KpiRequestFactory.newInstance("");
		kpiRequest = kpiRequestFactory.findKpiRequest("kpi113");
		kpiRequest.setLabel("PROJECT");
		AccountHierarchyFilterDataFactory accountHierarchyFilterDataFactory = AccountHierarchyFilterDataFactory
				.newInstance();
		accountHierarchyDataList = accountHierarchyFilterDataFactory.getAccountHierarchyDataList();
		SprintDetailsDataFactory sprintDetailsDataFactory = SprintDetailsDataFactory.newInstance();
		sprintDetails = sprintDetailsDataFactory.getSprintDetails().get(0);
		List<String> jiraIssueList = sprintDetails.getTotalIssues().stream().filter(Objects::nonNull)
				.map(SprintIssue::getNumber).distinct().collect(Collectors.toList());
		JiraIssueDataFactory jiraIssueDataFactory = JiraIssueDataFactory.newInstance();
		storyList = jiraIssueDataFactory.findIssueByNumberList(jiraIssueList);
		bugList = jiraIssueDataFactory.getBugs();
		FieldMappingDataFactory fieldMappingDataFactory = FieldMappingDataFactory
				.newInstance("/json/default/scrum_project_field_mappings.json");
		FieldMapping fieldMapping = fieldMappingDataFactory.getFieldMappings().get(0);
		fieldMappingMap.put(fieldMapping.getBasicProjectConfigId(), fieldMapping);
	}

	@Test
	public void testGetKpiDataProject() throws ApplicationException {

		TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
				accountHierarchyDataList, new ArrayList<>(), "hierarchyLevelOne", 5);
		try {
			String kpiRequestTrackerId = "Jira-Excel-QADD-track001";
			when(cacheService.getFromApplicationCache(Constant.KPI_REQUEST_TRACKER_ID_KEY + KPISource.JIRA.name()))
					.thenReturn(kpiRequestTrackerId);
			when(jiraService.getCurrentSprintDetails()).thenReturn(sprintDetails);
			when(configHelperService.getFieldMappingMap()).thenReturn(fieldMappingMap);
			when(jiraService.getJiraIssuesForCurrentSprint()).thenReturn(storyList);
			when(jiraIssueRepository.findLinkedDefects(anyMap(), any(), anyMap())).thenReturn(bugList);
			KpiElement kpiElement = defectCountByPriorityService.getKpiData(kpiRequest, kpiRequest.getKpiList().get(0),
					treeAggregatorDetail);
			assertNotNull(kpiElement);

		} catch (ApplicationException applicationException) {
		}
	}

	@Test
	public void testFetchKPIDataFromDbData() throws ApplicationException {
		TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
				accountHierarchyDataList, new ArrayList<>(), "hierarchyLevelOne", 5);
		List<Node> leafNodeList = new ArrayList<>();
		leafNodeList = KPIHelperUtil.getLeafNodes(treeAggregatorDetail.getRoot(), leafNodeList);
		String startDate = leafNodeList.get(0).getSprintFilter().getStartDate();
		String endDate = leafNodeList.get(leafNodeList.size() - 1).getSprintFilter().getEndDate();
		when(jiraService.getCurrentSprintDetails()).thenReturn(sprintDetails);
		when(jiraService.getJiraIssuesForCurrentSprint()).thenReturn(storyList);
		when(configHelperService.getFieldMappingMap()).thenReturn(fieldMappingMap);
		Map<String, Object> returnMap = defectCountByPriorityService.fetchKPIDataFromDb(leafNodeList, startDate,
				endDate, kpiRequest);
		assertNotNull(returnMap);
	}
}