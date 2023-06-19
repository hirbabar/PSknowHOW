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

package com.publicissapient.kpidashboard.apis.jira.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;

import com.publicissapient.kpidashboard.apis.abac.UserAuthorizedProjectsService;
import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.common.service.CacheService;
import com.publicissapient.kpidashboard.apis.common.service.impl.KpiHelperService;
import com.publicissapient.kpidashboard.apis.enums.Filters;
import com.publicissapient.kpidashboard.apis.enums.JiraFeature;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.enums.KPISource;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.errors.EntityNotFoundException;
import com.publicissapient.kpidashboard.apis.filter.service.FilterHelperService;
import com.publicissapient.kpidashboard.apis.jira.factory.JiraKPIServiceFactory;
import com.publicissapient.kpidashboard.apis.model.AccountHierarchyData;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.apis.model.Node;
import com.publicissapient.kpidashboard.apis.model.TreeAggregatorDetail;
import com.publicissapient.kpidashboard.apis.util.KPIHelperUtil;
import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssueCustomHistory;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssueReleaseStatus;
import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;
import com.publicissapient.kpidashboard.common.model.jira.SprintIssue;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueCustomHistoryRepository;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueReleaseStatusRepository;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueRepository;
import com.publicissapient.kpidashboard.common.repository.jira.SprintRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * This class handle all Scrum JIRA based KPI request and call each KPIs service
 * in thread. It is responsible for cache of KPI data at different level.
 * 
 * @author tauakram
 *
 */
@Service
@Slf4j
public class JiraServiceR {

	@Autowired
	private KpiHelperService kpiHelperService;

	@Autowired
	private FilterHelperService filterHelperService;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private UserAuthorizedProjectsService authorizedProjectsService;

	@Autowired
	private SprintRepository sprintRepository;
	private List<SprintDetails> sprintDetails;
	@Autowired
	private JiraIssueRepository jiraIssueRepository;
	@Autowired
	private JiraIssueCustomHistoryRepository jiraIssueCustomHistoryRepository;
	@Autowired
	private JiraIssueReleaseStatusRepository jiraIssueReleaseStatusRepository;
	@Autowired
	private ConfigHelperService configHelperService;
	private List<JiraIssue> jiraIssueList;
	private List<JiraIssueCustomHistory> jiraIssueCustomHistoryList;
	private List<String> releaseList;

	/**
	 * This method process scrum JIRA based kpi request, cache data and call service
	 * in multiple thread.
	 *
	 * @param kpiRequest
	 *            JIRA KPI request true if flow for precalculated, false for direct
	 *            flow.
	 * @return List of KPI data
	 * @throws EntityNotFoundException
	 */
	@SuppressWarnings({ "PMD.AvoidCatchingGenericException", "unchecked" })
	public List<KpiElement> process(KpiRequest kpiRequest) throws EntityNotFoundException {

		log.info("Processing KPI calculation for data {}", kpiRequest.getKpiList());
		List<KpiElement> origRequestedKpis = kpiRequest.getKpiList().stream().map(KpiElement::new)
				.collect(Collectors.toList());
		List<KpiElement> responseList = new ArrayList<>();
		String[] projectKeyCache = null;
		try {
			Integer groupId = kpiRequest.getKpiList().get(0).getGroupId();
			String groupName = filterHelperService.getHierarachyLevelId(kpiRequest.getLevel(), kpiRequest.getLabel(),
					false);
			if (null != groupName) {
				kpiRequest.setLabel(groupName.toUpperCase());
			} else {
				log.error("label name for selected hierarchy not found");
			}
			List<AccountHierarchyData> filteredAccountDataList = filterHelperService.getFilteredBuilds(kpiRequest,
					groupName);
			if (!CollectionUtils.isEmpty(filteredAccountDataList)) {
				projectKeyCache = getProjectKeyCache(kpiRequest, filteredAccountDataList);

				filteredAccountDataList = getAuthorizedFilteredList(kpiRequest, filteredAccountDataList);
				if (filteredAccountDataList.isEmpty()) {
					return responseList;
				}

				Object cachedData = cacheService.getFromApplicationCache(projectKeyCache, KPISource.JIRA.name(),
						groupId, kpiRequest.getSprintIncluded());
				if (!kpiRequest.getRequestTrackerId().toLowerCase().contains(KPISource.EXCEL.name().toLowerCase())
						&& null != cachedData) {
					log.info("Fetching value from cache for {}", Arrays.toString(kpiRequest.getIds()));
					return (List<KpiElement>) cachedData;
				}

				TreeAggregatorDetail treeAggregatorDetail = KPIHelperUtil.getTreeLeafNodesGroupedByFilter(kpiRequest,
						filteredAccountDataList, null, filterHelperService.getFirstHierarachyLevel(),
						filterHelperService.getHierarchyIdLevelMap(false)
								.getOrDefault(CommonConstant.HIERARCHY_LEVEL_ID_SPRINT, 0));

				if (!CollectionUtils.isEmpty(origRequestedKpis)
						&& StringUtils.isNotEmpty(origRequestedKpis.get(0).getKpiCategory())) {
					updateJiraIssueList(kpiRequest, origRequestedKpis, filteredAccountDataList, treeAggregatorDetail);
				}

				// set filter value to show on trend line. If sub-projects are
				// in
				// selection then show sub-projects on trend line else show
				// projects
				kpiRequest.setFilterToShowOnTrend(groupName);

				List<ParallelJiraServices> listOfTask = new ArrayList<>();
				for (KpiElement kpiEle : kpiRequest.getKpiList()) {

					listOfTask.add(new ParallelJiraServices(kpiRequest, responseList, kpiEle, treeAggregatorDetail));
				}

				ForkJoinTask.invokeAll(listOfTask);
				List<KpiElement> missingKpis = origRequestedKpis.stream()
						.filter(reqKpi -> responseList.stream()
								.noneMatch(responseKpi -> reqKpi.getKpiId().equals(responseKpi.getKpiId())))
						.collect(Collectors.toList());
				responseList.addAll(missingKpis);

				setIntoApplicationCache(kpiRequest, responseList, groupId, projectKeyCache);
			} else {
				responseList.addAll(origRequestedKpis);
			}
			sprintDetails = null;
			jiraIssueList = null;
			jiraIssueCustomHistoryList = null;
		} catch (Exception e) {
			log.error("Error while KPI calculation for data {} {}", kpiRequest.getKpiList(), e);
			throw new HttpMessageNotWritableException(e.getMessage(), e);
		}

		return responseList;
	}

	private void updateJiraIssueList(KpiRequest kpiRequest, List<KpiElement> origRequestedKpis,
			List<AccountHierarchyData> filteredAccountDataList, TreeAggregatorDetail treeAggregatorDetail) {
		if (origRequestedKpis.get(0).getKpiCategory().equalsIgnoreCase(CommonConstant.ITERATION)) {
			fetchSprintDetails(kpiRequest.getIds());
			List<String> sprintIssuesList = createIssuesList(
					filteredAccountDataList.get(0).getBasicProjectConfigId().toString());
			fetchJiraIssues(filteredAccountDataList.get(0).getBasicProjectConfigId().toString(), sprintIssuesList,
					CommonConstant.ITERATION);
			fetchJiraIssuesCustomHistory(filteredAccountDataList.get(0).getBasicProjectConfigId().toString(),
					sprintIssuesList, CommonConstant.ITERATION);
		} else if (origRequestedKpis.get(0).getKpiCategory().equalsIgnoreCase(CommonConstant.RELEASE)) {
			releaseList = getReleaseList(treeAggregatorDetail);
			fetchJiraIssues(filteredAccountDataList.get(0).getBasicProjectConfigId().toString(), releaseList,
					CommonConstant.RELEASE);
			fetchJiraIssuesCustomHistory(filteredAccountDataList.get(0).getBasicProjectConfigId().toString(),
					releaseList, CommonConstant.RELEASE);
		}
	}

	/**
	 * creating relase List on the basis of releaseId
	 * 
	 * @param treeAggregatorDetail
	 * @return
	 */
	private List<String> getReleaseList(TreeAggregatorDetail treeAggregatorDetail) {
		List<Node> nodes = treeAggregatorDetail.getMapOfListOfLeafNodes().get(Filters.RELEASE.toString().toLowerCase());
		List<String> processedList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(nodes)) {
			nodes.forEach(relaseNode -> processedList.add(relaseNode.getReleaseFilter().getName().split("_")[0]));
		}
		return processedList;
	}

	/**
	 * @param kpiRequest
	 * @param filteredAccountDataList
	 * @return
	 */
	private List<AccountHierarchyData> getAuthorizedFilteredList(KpiRequest kpiRequest,
			List<AccountHierarchyData> filteredAccountDataList) {
		kpiHelperService.kpiResolution(kpiRequest.getKpiList());
		if (!authorizedProjectsService.ifSuperAdminUser()) {
			filteredAccountDataList = authorizedProjectsService.filterProjects(filteredAccountDataList);
		}

		return filteredAccountDataList;
	}

	/**
	 * @param kpiRequest
	 * @param filteredAccountDataList
	 */
	private String[] getProjectKeyCache(KpiRequest kpiRequest, List<AccountHierarchyData> filteredAccountDataList) {
		String[] projectKeyCache;
		if (!authorizedProjectsService.ifSuperAdminUser()) {
			projectKeyCache = authorizedProjectsService.getProjectKey(filteredAccountDataList, kpiRequest);
		} else {
			projectKeyCache = kpiRequest.getIds();
		}

		return projectKeyCache;
	}

	/**
	 * 
	 * @param kpiRequest
	 * @param responseList
	 * @param groupId
	 */
	private void setIntoApplicationCache(KpiRequest kpiRequest, List<KpiElement> responseList, Integer groupId,
			String[] projects) {
		Integer sprintLevel = filterHelperService.getHierarchyIdLevelMap(false)
				.get(CommonConstant.HIERARCHY_LEVEL_ID_SPRINT);

		if (!kpiRequest.getRequestTrackerId().toLowerCase().contains(KPISource.EXCEL.name().toLowerCase())
				&& sprintLevel >= kpiRequest.getLevel()) {
			cacheService.setIntoApplicationCache(projects, responseList, KPISource.JIRA.name(), groupId,
					kpiRequest.getSprintIncluded());
		}

	}

	public void fetchSprintDetails(String[] sprintId) {
		sprintDetails = sprintRepository.findBySprintIDIn(Arrays.stream(sprintId).collect(Collectors.toList()));
		processSprintBasedOnFieldMapping(sprintDetails, configHelperService);
	}

	public SprintDetails getCurrentSprintDetails() {
		return sprintDetails.stream().findFirst().orElse(null);
	}

	public void setSprintDetails(List<SprintDetails> modifieldSprintDetails) {
		sprintDetails = modifieldSprintDetails;
	}

	public void fetchJiraIssues(String basicProjectConfigId, List<String> sprintIssuesList, String board) {
		if (board.equalsIgnoreCase(CommonConstant.ITERATION)) {
			jiraIssueList = jiraIssueRepository.findByNumberInAndBasicProjectConfigId(sprintIssuesList,
					basicProjectConfigId);
		} else if (board.equalsIgnoreCase(CommonConstant.RELEASE)) {
			Map<String, List<String>> mapOfFilters = new LinkedHashMap<>();
			mapOfFilters.put(JiraFeature.BASIC_PROJECT_CONFIG_ID.getFieldValueInFeature(),
					Collections.singletonList(basicProjectConfigId));
			Map<String, Map<String, Object>> uniqueProjectMap = new HashMap<>();
			Map<String, Object> mapOfProjectFilters = new LinkedHashMap<>();
			mapOfProjectFilters.put(CommonConstant.RELEASE, sprintIssuesList);
			uniqueProjectMap.put(basicProjectConfigId, mapOfProjectFilters);
			jiraIssueList = jiraIssueRepository.findByRelease(mapOfFilters, uniqueProjectMap);
		}
	}

	private List<String> createIssuesList(String basicProjectConfigId) {
		List<String> totalIssuesList = new ArrayList<>();
		sprintDetails.stream().filter(sd -> sd.getBasicProjectConfigId().toString().equals(basicProjectConfigId))
				.forEach(sprintDetails1 -> {
					if (!CollectionUtils.isEmpty(sprintDetails1.getCompletedIssues())) {
						totalIssuesList.addAll(sprintDetails1.getCompletedIssues().stream().map(SprintIssue::getNumber)
								.collect(Collectors.toList()));
					}
					if (!CollectionUtils.isEmpty(sprintDetails1.getNotCompletedIssues())) {
						totalIssuesList.addAll(sprintDetails1.getNotCompletedIssues().stream()
								.map(SprintIssue::getNumber).collect(Collectors.toList()));
					}
					if (!CollectionUtils.isEmpty(sprintDetails1.getPuntedIssues())) {
						totalIssuesList.addAll(sprintDetails1.getPuntedIssues().stream().map(SprintIssue::getNumber)
								.collect(Collectors.toList()));
					}
					if (!CollectionUtils.isEmpty(sprintDetails1.getCompletedIssuesAnotherSprint())) {
						totalIssuesList.addAll(sprintDetails1.getCompletedIssuesAnotherSprint().stream()
								.map(SprintIssue::getNumber).collect(Collectors.toList()));
					}
					if (!CollectionUtils.isEmpty(sprintDetails1.getAddedIssues())) {
						totalIssuesList.addAll(sprintDetails1.getAddedIssues());
					}
				});
		return totalIssuesList;
	}

	public List<JiraIssue> getJiraIssuesForCurrentSprint() {
		return jiraIssueList;
	}

	public void fetchJiraIssuesCustomHistory(String basicProjectConfigId, List<String> sprintIssuesList, String board) {
		if (board.equalsIgnoreCase(CommonConstant.ITERATION)) {
			jiraIssueCustomHistoryList = jiraIssueCustomHistoryRepository.findByStoryIDInAndBasicProjectConfigIdIn(
					sprintIssuesList, Collections.singletonList(basicProjectConfigId));
		} else {
			jiraIssueCustomHistoryList = jiraIssueCustomHistoryRepository
					.findByFilterAndFromReleaseMap(Collections.singletonList(basicProjectConfigId), releaseList);
		}
	}

	public List<JiraIssueCustomHistory> getJiraIssuesCustomHistoryForCurrentSprint() {
		return jiraIssueCustomHistoryList;
	}

	public JiraIssueReleaseStatus getJiraIssueReleaseForProject(String basicProjectConfigId) {
		return jiraIssueReleaseStatusRepository.findByBasicProjectConfigId(basicProjectConfigId);
	}

	public void processSprintBasedOnFieldMapping(List<SprintDetails> dbSprintDetails,
			ConfigHelperService configHelperService) {
		if (CollectionUtils.isNotEmpty(dbSprintDetails) && null != configHelperService) {
			dbSprintDetails.forEach(dbSprintDetail -> {
				FieldMapping fieldMapping = configHelperService.getFieldMappingMap()
						.get(dbSprintDetail.getBasicProjectConfigId());
				if (null != fieldMapping && (CollectionUtils
						.isNotEmpty(fieldMapping.getJiraIterationCompletionTypeCustomField())
						|| CollectionUtils.isNotEmpty(fieldMapping.getJiraIterationCompletionStatusCustomField()))) {
					Set<SprintIssue> newCompletedSet = filteringByFieldMapping(dbSprintDetail, fieldMapping);
					dbSprintDetail.setCompletedIssues(newCompletedSet);
					dbSprintDetail.getNotCompletedIssues().removeAll(newCompletedSet);
					Set<SprintIssue> totalIssue = new HashSet<>();
					totalIssue.addAll(dbSprintDetail.getCompletedIssues());
					totalIssue.addAll(dbSprintDetail.getNotCompletedIssues());
					dbSprintDetail.setTotalIssues(totalIssue);
				}
			});
		}
	}

	private Set<SprintIssue> getCombinationalCompletedSet(Set<SprintIssue> typeWiseIssues,
			Set<SprintIssue> statusWiseIssues) {
		Set<SprintIssue> newCompletedSet;
		if (CollectionUtils.isNotEmpty(typeWiseIssues) && CollectionUtils.isNotEmpty(statusWiseIssues)) {
			newCompletedSet = new HashSet<>(CollectionUtils.intersection(typeWiseIssues, statusWiseIssues));
		} else if (CollectionUtils.isNotEmpty(typeWiseIssues)) {
			newCompletedSet = typeWiseIssues;
		} else {
			newCompletedSet = statusWiseIssues;
		}
		return newCompletedSet;
	}

	private Set<SprintIssue> filteringByFieldMapping(SprintDetails dbSprintDetail, FieldMapping fieldMapping) {
		Set<SprintIssue> typeWiseIssues = new HashSet<>();
		Set<SprintIssue> statusWiseIssues = new HashSet<>();
		if (CollectionUtils.isNotEmpty(fieldMapping.getJiraIterationCompletionStatusCustomField())
				&& CollectionUtils.isNotEmpty(fieldMapping.getJiraIterationCompletionTypeCustomField())) {
			List<String> jiraIterationCompletionStatusCustomField = fieldMapping
					.getJiraIterationCompletionStatusCustomField();
			List<String> jiraIterationCompletionTypeCustomField = fieldMapping
					.getJiraIterationCompletionTypeCustomField();
			statusWiseIssues.addAll(dbSprintDetail.getCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionStatusCustomField.contains(issue.getStatus()))
					.collect(Collectors.toSet()));
			statusWiseIssues.addAll(dbSprintDetail.getNotCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionStatusCustomField.contains(issue.getStatus()))
					.collect(Collectors.toSet()));
			typeWiseIssues.addAll(dbSprintDetail.getCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionTypeCustomField.contains(issue.getTypeName()))
					.collect(Collectors.toSet()));
			typeWiseIssues.addAll(dbSprintDetail.getNotCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionTypeCustomField.contains(issue.getTypeName()))
					.collect(Collectors.toSet()));
		} else if (CollectionUtils.isNotEmpty(fieldMapping.getJiraIterationCompletionStatusCustomField())) {
			List<String> jiraIterationCompletionStatusCustomField = fieldMapping
					.getJiraIterationCompletionStatusCustomField();
			statusWiseIssues.addAll(dbSprintDetail.getCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionStatusCustomField.contains(issue.getStatus()))
					.collect(Collectors.toSet()));
			statusWiseIssues.addAll(dbSprintDetail.getNotCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionStatusCustomField.contains(issue.getStatus()))
					.collect(Collectors.toSet()));
		} else if (CollectionUtils.isNotEmpty(fieldMapping.getJiraIterationCompletionTypeCustomField())) {
			List<String> jiraIterationCompletionTypeCustomField = fieldMapping
					.getJiraIterationCompletionTypeCustomField();
			typeWiseIssues.addAll(dbSprintDetail.getCompletedIssues().stream()
					.filter(issue -> jiraIterationCompletionTypeCustomField.contains(issue.getTypeName()))
					.collect(Collectors.toSet()));
		}
		return getCombinationalCompletedSet(typeWiseIssues, statusWiseIssues);
	}

	public List<String> getReleaseList() {
		return releaseList;
	}

	/**
	 * This class is used to call JIRA based KPIs service in parallel.
	 *
	 * @author pankumar8
	 */
	public class ParallelJiraServices extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		private final KpiRequest kpiRequest;
		private final transient List<KpiElement> responseList;
		private final transient KpiElement kpiEle;
		private final TreeAggregatorDetail treeAggregatorDetail;

		/*
		 * @param kpiRequest
		 *
		 * @param responseList
		 *
		 * @param kpiEle
		 *
		 * @param treeAggregatorDetail
		 */
		public ParallelJiraServices(KpiRequest kpiRequest, List<KpiElement> responseList, KpiElement kpiEle,
				TreeAggregatorDetail treeAggregatorDetail) {
			super();
			this.kpiRequest = kpiRequest;
			this.responseList = responseList;
			this.kpiEle = kpiEle;
			this.treeAggregatorDetail = treeAggregatorDetail;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("PMD.AvoidCatchingGenericException")
		@Override
		public void compute() {
			try {
				calculateAllKPIAggregatedMetrics(kpiRequest, responseList, kpiEle, treeAggregatorDetail);
			} catch (Exception e) {
				log.error("[PARALLEL_JIRA_SERVICE].Exception occured {}", e);
			}
		}

		/**
		 * This method call by multiple thread, take object of specific KPI and call
		 * method of these KPIs
		 *
		 * @param kpiRequest
		 *            JIRA KPI request
		 * @param responseList
		 *            List of KpiElement having data of each KPIs
		 * @param kpiElement
		 * @param treeAggregatorDetail
		 *            filter tree object
		 * @throws ApplicationException
		 * @throws EntityNotFoundException
		 */
		private void calculateAllKPIAggregatedMetrics(KpiRequest kpiRequest, List<KpiElement> responseList,
				KpiElement kpiElement, TreeAggregatorDetail treeAggregatorDetail) throws ApplicationException {

			JiraKPIService<?, ?, ?> jiraKPIService = null;

			KPICode kpi = KPICode.getKPI(kpiElement.getKpiId());

			jiraKPIService = JiraKPIServiceFactory.getJiraKPIService(kpi.name());

			long startTime = System.currentTimeMillis();

			if (KPICode.THROUGHPUT.equals(kpi)) {
				log.info("No need to fetch Throughput KPI data");
			} else {
				TreeAggregatorDetail treeAggregatorDetailClone = (TreeAggregatorDetail) SerializationUtils
						.clone(treeAggregatorDetail);
				responseList.add(jiraKPIService.getKpiData(kpiRequest, kpiElement, treeAggregatorDetailClone));

				long processTime = System.currentTimeMillis() - startTime;
				log.info("[JIRA-{}-TIME][{}]. KPI took {} ms", kpi.name(), kpiRequest.getRequestTrackerId(),
						processTime);
			}
		}

	}

}