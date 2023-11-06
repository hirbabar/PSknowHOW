package com.publicissapient.kpidashboard.apis.jira.service.releasedashboard;

import com.publicissapient.kpidashboard.apis.abac.UserAuthorizedProjectsService;
import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.common.service.CacheService;
import com.publicissapient.kpidashboard.apis.common.service.impl.KpiHelperService;
import com.publicissapient.kpidashboard.apis.data.AccountHierarchyFilterDataFactory;
import com.publicissapient.kpidashboard.apis.data.FieldMappingDataFactory;
import com.publicissapient.kpidashboard.apis.data.HierachyLevelFactory;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.filter.service.FilterHelperService;
import com.publicissapient.kpidashboard.apis.jira.factory.JiraNonTrendKPIServiceFactory;
import com.publicissapient.kpidashboard.apis.jira.scrum.service.release.ReleaseBurnUpServiceImpl;
import com.publicissapient.kpidashboard.apis.model.AccountHierarchyData;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.application.HierarchyLevel;
import com.publicissapient.kpidashboard.common.model.application.ProjectBasicConfig;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueCustomHistoryRepository;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueReleaseStatusRepository;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueRepository;
import com.publicissapient.kpidashboard.common.repository.jira.SprintRepository;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JiraReleaseServiceRTest {


    private static String GROUP_PROJECT = "project";
    public Map<String, ProjectBasicConfig> projectConfigMap = new HashMap<>();
    public Map<ObjectId, FieldMapping> fieldMappingMap = new HashMap<>();
    @Mock
    KpiHelperService kpiHelperService;
    @Mock
    FilterHelperService filterHelperService;
    List<KpiElement> mockKpiElementList = new ArrayList<>();
    @Mock
    SprintRepository sprintRepository;
    @Mock
    JiraIssueRepository jiraIssueRepository;
    @Mock
    JiraIssueCustomHistoryRepository jiraIssueCustomHistoryRepository;
    @Mock
    ConfigHelperService configHelperService;
    @InjectMocks
    @Spy
    private JiraReleaseServiceR jiraServiceR;
    @Mock
    private CacheService cacheService;
    @Mock
    private ReleaseBurnUpServiceImpl releaseBurnupService;
    @SuppressWarnings("rawtypes")
    @Mock
    private List<JiraReleaseKPIService> services;
    private List<AccountHierarchyData> accountHierarchyDataList = new ArrayList<>();
    private String[] projectKey;
    private List<HierarchyLevel> hierarchyLevels = new ArrayList<>();
    private KpiElement ibKpiElement;
    private Map<String, JiraReleaseKPIService> jiraServiceCache = new HashMap<>();
    @Mock
    private JiraNonTrendKPIServiceFactory jiraKPIServiceFactory;
    @Mock
    private UserAuthorizedProjectsService authorizedProjectsService;
    @Mock
    private JiraIssueReleaseStatusRepository jiraIssueReleaseStatusRepository;

    @Before
    public void setup() {
        mockKpiElementList.add(ibKpiElement);

        when(cacheService.getFromApplicationCache(any(), Mockito.anyString(), any(), ArgumentMatchers.anyList()))
                .thenReturn(mockKpiElementList);

        AccountHierarchyFilterDataFactory accountHierarchyFilterDataFactory = AccountHierarchyFilterDataFactory
                .newInstance("/json/default/account_hierarchy_filter_data_release.json");
        accountHierarchyDataList = accountHierarchyFilterDataFactory.getAccountHierarchyDataList();
        HierachyLevelFactory hierachyLevelFactory = HierachyLevelFactory.newInstance();
        hierarchyLevels = hierachyLevelFactory.getHierarchyLevels();

        ProjectBasicConfig projectConfig = new ProjectBasicConfig();
        projectConfig.setId(new ObjectId("6335363749794a18e8a4479b"));
        projectConfig.setProjectName("Scrum Project");
        projectConfigMap.put(projectConfig.getProjectName(), projectConfig);

        FieldMappingDataFactory fieldMappingDataFactory = FieldMappingDataFactory
                .newInstance("/json/default/scrum_project_field_mappings.json");
        FieldMapping fieldMapping = fieldMappingDataFactory.getFieldMappings().get(0);
        fieldMappingMap.put(fieldMapping.getBasicProjectConfigId(), fieldMapping);

        when(filterHelperService.getHierarachyLevelId(5, "release", false)).thenReturn("release");

        setIbKpiElement();

    }

    private void setIbKpiElement() {

        ibKpiElement = setKpiElement("kpi36", "RELEASE_BURNUP");

        ibKpiElement.setValue(null);
    }

    private KpiElement setKpiElement(String kpiId, String kpiName) {

        KpiElement kpiElement = new KpiElement();
        kpiElement.setKpiId(kpiId);
        kpiElement.setKpiName(kpiName);

        return kpiElement;
    }

    @After
    public void cleanup() {

    }

    @org.junit.Test(expected = Exception.class)
    public void testProcessException() throws Exception {

        KpiRequest kpiRequest = createKpiRequest(6);

        when(cacheService.cacheAccountHierarchyData()).thenThrow(ApplicationException.class);

        jiraServiceR.process(kpiRequest);

    }

    @org.junit.Test
    public void TestProcess_pickFromCache() throws Exception {

        KpiRequest kpiRequest = createKpiRequest(5);

        // checking only for RCA
        mockKpiElementList.add(ibKpiElement);

        when(cacheService.getFromApplicationCache(any(), Mockito.anyString(), any(), ArgumentMatchers.anyList()))
                .thenReturn(mockKpiElementList);
        when(authorizedProjectsService.ifSuperAdminUser()).thenReturn(true);

        List<KpiElement> resultList = jiraServiceR.process(kpiRequest);

        assertThat("Kpi Name :", resultList.get(0).getKpiName(), equalTo("RELEASE_BURNUP"));

    }

    @SuppressWarnings("unchecked")
    @org.junit.Test
    public void TestProcess() throws Exception {

        KpiRequest kpiRequest = createKpiRequest(5);

        @SuppressWarnings("rawtypes")
        JiraReleaseKPIService mcokAbstract = releaseBurnupService;
        jiraServiceCache.put(KPICode.RELEASE_BURNUP.name(), mcokAbstract);

        try (MockedStatic<JiraNonTrendKPIServiceFactory> utilities = Mockito.mockStatic(JiraNonTrendKPIServiceFactory.class)) {
            utilities.when((MockedStatic.Verification) JiraNonTrendKPIServiceFactory.getJiraKPIService(any()))
                    .thenReturn(mcokAbstract);
        }

        Map<String, Integer> map = new HashMap<>();
        Map<String, HierarchyLevel> hierarchyMap = hierarchyLevels.stream()
                .collect(Collectors.toMap(HierarchyLevel::getHierarchyLevelId, x -> x));
        hierarchyMap.entrySet().stream().forEach(k -> map.put(k.getKey(), k.getValue().getLevel()));
        when(filterHelperService.getHierarchyIdLevelMap(false)).thenReturn(map);
        when(cacheService.getFromApplicationCache(any(), any(), any(), any())).thenReturn(null);
        when(cacheService.cacheAccountHierarchyData()).thenReturn(accountHierarchyDataList);
        when(authorizedProjectsService.getProjectKey(accountHierarchyDataList, kpiRequest)).thenReturn(projectKey);
        when(authorizedProjectsService.filterProjects(any())).thenReturn(accountHierarchyDataList.stream().filter(s -> s.getLeafNodeId().equalsIgnoreCase("38296_Scrum Project_6335363749794a18e8a4479b")).collect(Collectors.toList()));
        when(filterHelperService.getFirstHierarachyLevel()).thenReturn("hierarchyLevelOne");
        when(cacheService.cacheFieldMappingMapData()).thenReturn(fieldMappingMap);
        List<KpiElement> resultList = jiraServiceR.process(kpiRequest);

        resultList.forEach(k -> {

            KPICode kpi = KPICode.getKPI(k.getKpiId());

            switch (kpi) {

                case RELEASE_BURNUP:
                    assertThat("Kpi Name :", k.getKpiName(), equalTo("RELEASE_BURNUP"));
                    break;

                default:
                    break;
            }

        });

    }

    private KpiRequest createKpiRequest(int level) {
        KpiRequest kpiRequest = new KpiRequest();
        List<KpiElement> kpiList = new ArrayList<>();

        addKpiElement(kpiList, KPICode.RELEASE_BURNUP.getKpiId(), KPICode.RELEASE_BURNUP.name(),
                "Release", "");
        kpiRequest.setLevel(level);
        kpiRequest.setIds(new String[]{"38296_Scrum Project_6335363749794a18e8a4479b"});
        kpiRequest.setKpiList(kpiList);
        kpiRequest.setRequestTrackerId();
        kpiRequest.setLabel("release");
        Map<String, List<String>> s = new HashMap<>();
        s.put("Release", Arrays.asList("38296_Scrum Project_6335363749794a18e8a4479b"));
        kpiRequest.setSelectedMap(s);
        return kpiRequest;
    }

    private void addKpiElement(List<KpiElement> kpiList, String kpiId, String kpiName, String category,
                               String kpiUnit) {
        KpiElement kpiElement = new KpiElement();
        kpiElement.setKpiId(kpiId);
        kpiElement.setKpiName(kpiName);
        kpiElement.setKpiCategory(category);
        kpiElement.setKpiUnit(kpiUnit);
        kpiElement.setKpiSource("Jira");

        kpiElement.setMaxValue("500");
        kpiElement.setChartType("gaugeChart");
        kpiList.add(kpiElement);
    }


}