/*******************************************************************************
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package com.publicissapient.kpidashboard.apis.jira.service.backlogdashboard;

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
import com.publicissapient.kpidashboard.apis.jira.scrum.service.BacklogReadinessEfficiencyServiceImpl;
import com.publicissapient.kpidashboard.apis.jira.scrum.service.FlowLoadServiceImpl;
import com.publicissapient.kpidashboard.apis.jira.service.backlogdashboard.JiraBacklogKPIService;
import com.publicissapient.kpidashboard.apis.jira.service.backlogdashboard.JiraBacklogServiceR;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JiraBacklogServiceRTest {


    private static String GROUP_PROJECT = "project";
    public Map<String, ProjectBasicConfig> projectConfigMap = new HashMap<>();
    public Map<ObjectId, FieldMapping> fieldMappingMap = new HashMap<>();
    @Mock
    KpiHelperService kpiHelperService;
    @Mock
    FilterHelperService filterHelperService;
    List<KpiElement> mockKpiElementList = new ArrayList<>();
    @InjectMocks
    @Spy
    private JiraBacklogServiceR jiraServiceR;
    @Mock
    private CacheService cacheService;
    @Mock
    private FlowLoadServiceImpl releaseBurnupService;
    @SuppressWarnings("rawtypes")
    @Mock
    private List<JiraBacklogKPIService> services;
    private List<AccountHierarchyData> accountHierarchyDataList = new ArrayList<>();
    private String[] projectKey;
    private List<HierarchyLevel> hierarchyLevels = new ArrayList<>();
    private KpiElement ibKpiElement;
    private Map<String, JiraBacklogKPIService> jiraServiceCache = new HashMap<>();
    @Mock
    private JiraNonTrendKPIServiceFactory jiraKPIServiceFactory;
    @Mock
    private UserAuthorizedProjectsService authorizedProjectsService;
    @Mock
    SprintRepository sprintRepository;
    @Mock
    JiraIssueRepository jiraIssueRepository;
    @Mock
    JiraIssueCustomHistoryRepository jiraIssueCustomHistoryRepository;
    @Mock
    ConfigHelperService configHelperService;
    @Mock
    private JiraIssueReleaseStatusRepository jiraIssueReleaseStatusRepository;

    @Before
    public void setup() {
        mockKpiElementList.add(ibKpiElement);

        when(cacheService.getFromApplicationCache(any(), Mockito.anyString(), any(), ArgumentMatchers.anyList()))
                .thenReturn(mockKpiElementList);

        AccountHierarchyFilterDataFactory accountHierarchyFilterDataFactory = AccountHierarchyFilterDataFactory
                .newInstance("/json/default/account_hierarchy_filter_data.json");
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

        when(filterHelperService.getHierarachyLevelId(4, "project", false)).thenReturn("project");

        setIbKpiElement();

    }

    private void setIbKpiElement() {

        ibKpiElement = setKpiElement("kpi138", "FLOW_LOAD");

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

        when(filterHelperService.getFilteredBuilds(any(),any())).thenThrow(ApplicationException.class);

        jiraServiceR.process(kpiRequest);

    }

    @org.junit.Test
    public void TestProcess_pickFromCache() throws Exception {

        KpiRequest kpiRequest = createKpiRequest(5);

        mockKpiElementList.add(ibKpiElement);

        when(cacheService.getFromApplicationCache(any(), Mockito.anyString(), any(), ArgumentMatchers.anyList()))
                .thenReturn(mockKpiElementList);
        when(authorizedProjectsService.ifSuperAdminUser()).thenReturn(true);

        List<KpiElement> resultList = jiraServiceR.process(kpiRequest);

        assertThat("Kpi Name :", resultList.get(0).getKpiName(), equalTo("FLOW_LOAD"));

    }

    @SuppressWarnings("unchecked")
    @org.junit.Test
    public void TestProcess() throws Exception {

        KpiRequest kpiRequest = createKpiRequest(4);

        @SuppressWarnings("rawtypes")
        JiraBacklogKPIService jiraKPIService = Mockito.mock(JiraBacklogKPIService.class);
        jiraServiceCache.put(KPICode.FLOW_LOAD.name(), releaseBurnupService);

        try (MockedStatic<JiraNonTrendKPIServiceFactory> utilities = Mockito.mockStatic(JiraNonTrendKPIServiceFactory.class)) {
            utilities.when((MockedStatic.Verification) JiraNonTrendKPIServiceFactory.getJiraKPIService(any()))
                    .thenReturn(jiraKPIService);
        }

        Map<String, Integer> map = new HashMap<>();
        Map<String, HierarchyLevel> hierarchyMap = hierarchyLevels.stream()
                .collect(Collectors.toMap(HierarchyLevel::getHierarchyLevelId, x -> x));
        hierarchyMap.entrySet().stream().forEach(k -> map.put(k.getKey(), k.getValue().getLevel()));
        when(filterHelperService.getHierarchyIdLevelMap(false)).thenReturn(map);
        when(cacheService.getFromApplicationCache(any(),any(),any(),any())).thenReturn(null);
        when(filterHelperService.getFilteredBuilds(any(),any())).thenReturn(accountHierarchyDataList);
        when(cacheService.cacheAccountHierarchyData()).thenReturn(accountHierarchyDataList);
        when(authorizedProjectsService.getProjectKey(accountHierarchyDataList, kpiRequest)).thenReturn(projectKey);
        when(authorizedProjectsService.filterProjects(any())).thenReturn(accountHierarchyDataList.stream().filter(s->s.getLeafNodeId().equalsIgnoreCase("Scrum Project_6335363749794a18e8a4479b")).collect(Collectors.toList()));
        when(filterHelperService.getFirstHierarachyLevel()).thenReturn("hierarchyLevelOne");
        when(cacheService.cacheFieldMappingMapData()).thenReturn(fieldMappingMap);
        List<KpiElement> resultList = jiraServiceR.process(kpiRequest);

        resultList.forEach(k -> {

            KPICode kpi = KPICode.getKPI(k.getKpiId());

            switch (kpi) {

                case FLOW_LOAD:
                    assertThat("Kpi Name :", k.getKpiName(), equalTo("FLOW_LOAD"));
                    break;

                default:
                    break;
            }

        });

    }

    private KpiRequest createKpiRequest(int level) {
        KpiRequest kpiRequest = new KpiRequest();
        List<KpiElement> kpiList = new ArrayList<>();

        addKpiElement(kpiList, KPICode.FLOW_LOAD.getKpiId(), KPICode.FLOW_LOAD.name(),
                "Backlog", "");
        kpiRequest.setLevel(level);
        kpiRequest.setIds(new String[]{"Scrum Project_6335363749794a18e8a4479b"});
        kpiRequest.setKpiList(kpiList);
        kpiRequest.setRequestTrackerId();
        kpiRequest.setLabel("project");
        Map<String,List<String>> s=new HashMap<>();
        s.put("project", Arrays.asList("Scrum Project_6335363749794a18e8a4479b"));
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