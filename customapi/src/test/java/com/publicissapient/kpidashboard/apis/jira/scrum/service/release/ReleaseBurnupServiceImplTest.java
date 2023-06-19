package com.publicissapient.kpidashboard.apis.jira.scrum.service.release;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import com.publicissapient.kpidashboard.apis.data.JiraIssueDataFactory;
import com.publicissapient.kpidashboard.apis.data.JiraIssueHistoryDataFactory;
import com.publicissapient.kpidashboard.apis.data.JiraIssueReleaseStatusDataFactory;
import com.publicissapient.kpidashboard.apis.data.KpiRequestFactory;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.enums.KPISource;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.jira.service.JiraServiceR;
import com.publicissapient.kpidashboard.apis.model.AccountHierarchyData;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.apis.model.TreeAggregatorDetail;
import com.publicissapient.kpidashboard.apis.util.KPIHelperUtil;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssueCustomHistory;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssueReleaseStatus;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueRepository;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseBurnupServiceImplTest {
	@Mock
	CacheService cacheService;
	@Mock
	ConfigHelperService configHelperService;
	@InjectMocks
	private ReleaseBurnupServiceImpl releaseBurnupService;
	@Mock
	private JiraServiceR jiraService;

	@Mock
	private JiraIssueRepository jiraIssueRepository;

	private KpiRequest kpiRequest;
	private List<AccountHierarchyData> accountHierarchyDataList = new ArrayList<>();
	private List<JiraIssue> jiraIssues = new ArrayList<>();
	private List<JiraIssueCustomHistory> jiraIssuesCustomHistory = new ArrayList<>();
	private List<JiraIssueReleaseStatus> jiraIssueReleaseStatusList;

	@Before
	public void setUp() {
		KpiRequestFactory kpiRequestFactory = KpiRequestFactory.newInstance("");
		kpiRequest = kpiRequestFactory.findKpiRequest("kpi141");
		kpiRequest.setLabel("RELEASE");
		AccountHierarchyFilterDataFactory accountHierarchyFilterDataFactory = AccountHierarchyFilterDataFactory
				.newInstance("/json/default/account_hierarchy_filter_data_release.json");
		accountHierarchyDataList = accountHierarchyFilterDataFactory.getAccountHierarchyDataList();
		JiraIssueDataFactory jiraIssueDataFactory = JiraIssueDataFactory
				.newInstance("/json/default/iteration/jira_issues_new_structure.json");
		jiraIssues = jiraIssueDataFactory.getJiraIssues();
		JiraIssueHistoryDataFactory jiraIssueHistoryDataFactory = JiraIssueHistoryDataFactory
				.newInstance("/json/default/iteration/jira_issue_custom_history_new_structure.json");
		jiraIssuesCustomHistory = jiraIssueHistoryDataFactory.getJiraIssueCustomHistory();
		JiraIssueReleaseStatusDataFactory jiraIssueReleaseStatusDataFactory = JiraIssueReleaseStatusDataFactory
				.newInstance("/json/default/jira_issue_release_status.json");
		jiraIssueReleaseStatusList = jiraIssueReleaseStatusDataFactory.getJiraIssueReleaseStatusList();

	}

	@Test
	public void getQualifierType() {
		assertThat(releaseBurnupService.getQualifierType(), equalTo(KPICode.RELEASE_BURNUP.name()));
	}

	@Test
	public void getKpiData() throws ApplicationException {
		TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
				accountHierarchyDataList, new ArrayList<>(), "hierarchyLevelOne", 5);
		String kpiRequestTrackerId = "Jira-Excel-QADD-track001";
		when(cacheService.getFromApplicationCache(Constant.KPI_REQUEST_TRACKER_ID_KEY + KPISource.JIRA.name()))
				.thenReturn(kpiRequestTrackerId);
		when(jiraService.getJiraIssueReleaseForProject(anyString())).thenReturn(jiraIssueReleaseStatusList.get(0));
		when(jiraService.getJiraIssuesCustomHistoryForCurrentSprint()).thenReturn(jiraIssuesCustomHistory);
		when(jiraIssueRepository.findByNumberInAndBasicProjectConfigId(any(), any())).thenReturn(jiraIssues);
		when(jiraService.getReleaseList()).thenReturn(Arrays.asList("AP v2.0.0"));
		KpiElement kpiElement = releaseBurnupService.getKpiData(kpiRequest, kpiRequest.getKpiList().get(0),
				treeAggregatorDetail);
		assertNotNull(kpiElement.getTrendValueList());
	}

	@Test
	public void withNullStartEndDate() throws ApplicationException {
		accountHierarchyDataList = accountHierarchyDataList.stream().filter(data -> {
			data.getNode().stream().filter(node -> node.getGroupName().equalsIgnoreCase("release")).forEach(node -> {
				node.getAccountHierarchy().setBeginDate(null);
				node.getAccountHierarchy().setEndDate(null);
			});
			return true;
		}).collect(Collectors.toList());
		TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
				accountHierarchyDataList, new ArrayList<>(), "hierarchyLevelOne", 5);
		String kpiRequestTrackerId = "Jira-Excel-QADD-track001";
		when(cacheService.getFromApplicationCache(Constant.KPI_REQUEST_TRACKER_ID_KEY + KPISource.JIRA.name()))
				.thenReturn(kpiRequestTrackerId);
		when(jiraService.getJiraIssueReleaseForProject(anyString())).thenReturn(jiraIssueReleaseStatusList.get(0));
		when(jiraService.getJiraIssuesCustomHistoryForCurrentSprint()).thenReturn(jiraIssuesCustomHistory);
		when(jiraIssueRepository.findByNumberInAndBasicProjectConfigId(any(), any())).thenReturn(jiraIssues);
		when(jiraService.getReleaseList()).thenReturn(Arrays.asList("AP v2.0.0"));
		KpiElement kpiElement = releaseBurnupService.getKpiData(kpiRequest, kpiRequest.getKpiList().get(0),
				treeAggregatorDetail);
		assertNotNull(kpiElement.getTrendValueList());
	}

	@Test
	public void withJustStartDate() throws ApplicationException {
		accountHierarchyDataList = accountHierarchyDataList.stream().filter(data -> {
			data.getNode().stream().filter(node -> node.getGroupName().equalsIgnoreCase("release")).forEach(node -> {
				node.getAccountHierarchy().setBeginDate("2023-05-25T15:53:00.0000000");
				node.getAccountHierarchy().setEndDate(null);
			});
			return true;
		}).collect(Collectors.toList());
		TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
				accountHierarchyDataList, new ArrayList<>(), "hierarchyLevelOne", 5);
		String kpiRequestTrackerId = "Jira-Excel-QADD-track001";
		when(cacheService.getFromApplicationCache(Constant.KPI_REQUEST_TRACKER_ID_KEY + KPISource.JIRA.name()))
				.thenReturn(kpiRequestTrackerId);
		when(jiraService.getJiraIssueReleaseForProject(anyString())).thenReturn(jiraIssueReleaseStatusList.get(0));
		when(jiraService.getJiraIssuesCustomHistoryForCurrentSprint()).thenReturn(jiraIssuesCustomHistory);
		when(jiraIssueRepository.findByNumberInAndBasicProjectConfigId(any(), any())).thenReturn(jiraIssues);
		when(jiraService.getReleaseList()).thenReturn(Arrays.asList("AP v2.0.0"));
		KpiElement kpiElement = releaseBurnupService.getKpiData(kpiRequest, kpiRequest.getKpiList().get(0),
				treeAggregatorDetail);
		assertNotNull(kpiElement.getTrendValueList());
	}

}