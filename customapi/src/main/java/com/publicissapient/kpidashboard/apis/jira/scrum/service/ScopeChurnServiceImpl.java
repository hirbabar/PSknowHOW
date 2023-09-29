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

package com.publicissapient.kpidashboard.apis.jira.scrum.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.config.CustomApiConfig;
import com.publicissapient.kpidashboard.apis.constant.Constant;
import com.publicissapient.kpidashboard.apis.enums.Filters;
import com.publicissapient.kpidashboard.apis.enums.JiraFeature;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.enums.KPIExcelColumn;
import com.publicissapient.kpidashboard.apis.enums.KPISource;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.filter.service.FilterHelperService;
import com.publicissapient.kpidashboard.apis.jira.service.JiraKPIService;
import com.publicissapient.kpidashboard.apis.model.KPIExcelData;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.apis.model.Node;
import com.publicissapient.kpidashboard.apis.model.TreeAggregatorDetail;
import com.publicissapient.kpidashboard.apis.util.KPIExcelUtility;
import com.publicissapient.kpidashboard.apis.util.KpiDataHelper;
import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.model.application.DataCount;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.jira.JiraHistoryChangeLog;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssueCustomHistory;
import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueCustomHistoryRepository;
import com.publicissapient.kpidashboard.common.repository.jira.JiraIssueRepository;
import com.publicissapient.kpidashboard.common.repository.jira.SprintRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * This class fetches the Scope Churn KPI along with trend analysis. Trend
 * analysis for Scope Churn KPI has percentage at y-axis and sprint id at
 * x-axis. {@link JiraKPIService}
 *
 * @author Shubh
 *
 */
@Component
@Slf4j
public class ScopeChurnServiceImpl extends JiraKPIService<Double, List<Object>, Map<String, Object>> {

	private static final String DEV = "DeveloperKpi";
	public static final String TOTAL_ISSUE = "totalIssue";
	public static final String SCOPE_CHANGE = "Scope Change";
	public static final String SPRINT_DETAILS = "sprintDetails";
	public static final String INITIAL_SCOPE = "Initial Commitment";
	public static final String SCOPE_CHANGE_ISSUE_HISTORY = "scopeChangeIssuesHistories";
	public static final String SEPARATOR_ASTERISK = "*************************************";
	@Autowired
	private SprintRepository sprintRepository;
	@Autowired
	private JiraIssueRepository jiraIssueRepository;
	@Autowired
	private ConfigHelperService configHelperService;
	@Autowired
	private FilterHelperService filterHelperService;
	@Autowired
	private CustomApiConfig customApiConfig;
	@Autowired
	private JiraIssueCustomHistoryRepository jiraIssueCustomHistoryRepository;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public KpiElement getKpiData(KpiRequest kpiRequest, KpiElement kpiElement,
			TreeAggregatorDetail treeAggregatorDetail) throws ApplicationException {

		List<DataCount> trendValueList = new ArrayList<>();
		Node root = treeAggregatorDetail.getRoot();
		Map<String, Node> mapTmp = treeAggregatorDetail.getMapTmp();

		treeAggregatorDetail.getMapOfListOfLeafNodes().forEach((k, v) -> {
			if (Filters.getFilter(k) == Filters.SPRINT) {
				sprintWiseLeafNodeValue(mapTmp, v, trendValueList, kpiElement, kpiRequest);
			}
		});

		log.debug("[SCOPE-CHURN-LEAF-NODE-VALUE][{}]. Aggregated Value at each level in the tree {}",
				kpiRequest.getRequestTrackerId(), root);

		Map<Pair<String, String>, Node> nodeWiseKPIValue = new HashMap<>();
		calculateAggregatedValue(root, nodeWiseKPIValue, KPICode.SCOPE_CHURN);

		List<DataCount> trendValues = getTrendValues(kpiRequest, nodeWiseKPIValue, KPICode.SCOPE_CHURN);
		kpiElement.setTrendValueList(trendValues);
		return kpiElement;
	}

	/**
	 * Populates KPI value to sprint leaf nodes and gives the trend analysis at
	 * sprint wise.
	 * 
	 * @param mapTmp
	 * @param sprintLeafNodeList
	 * @param trendValueList
	 * @param kpiElement
	 * @param kpiRequest
	 */
	@SuppressWarnings("unchecked")
	private void sprintWiseLeafNodeValue(Map<String, Node> mapTmp, List<Node> sprintLeafNodeList,
			List<DataCount> trendValueList, KpiElement kpiElement, KpiRequest kpiRequest) {
		String requestTrackerId = getRequestTrackerId();

		Map<String, Object> storyChurnFetchDetails = fetchKPIDataFromDb(sprintLeafNodeList, null, null, kpiRequest);

		List<SprintDetails> sprintDetails = (List<SprintDetails>) storyChurnFetchDetails.get(SPRINT_DETAILS);
		List<JiraIssue> fetchedIssue = (List<JiraIssue>) storyChurnFetchDetails.get(TOTAL_ISSUE);
		List<JiraIssueCustomHistory> fetchedIssueHistory = (List<JiraIssueCustomHistory>) storyChurnFetchDetails
				.getOrDefault(SCOPE_CHANGE_ISSUE_HISTORY, new ArrayList<>());
		Map<String, List<JiraHistoryChangeLog>> issueKeyWiseHistoryMap = fetchedIssueHistory.stream()
				.collect(Collectors.toMap(JiraIssueCustomHistory::getStoryID,
						JiraIssueCustomHistory::getSprintUpdationLog, (existingValue, newValue) -> newValue,
						LinkedHashMap::new));

		Map<Pair<String, String>, Double> sprintWiseStoryChurnDataMap = new HashMap<>();
		Map<Pair<String, String>, Map<String, Object>> sprintWiseHowerMap = new HashMap<>();
		Map<Pair<String, String>, List<JiraIssue>> sprintWiseAddedListMap = new HashMap<>();
		Map<Pair<String, String>, List<JiraIssue>> sprintWiseRemovedListMap = new HashMap<>();
		Map<Pair<String, String>, String> sprintNameMap = new HashMap<>();
		List<KPIExcelData> excelData = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(sprintDetails)) {

			sprintDetails.forEach(sd -> {
				List<JiraIssue> sprintWiseAddedList;
				List<JiraIssue> sprintWiseRemovedList;
				List<JiraIssue> sprintWiseInitialComitList;

				Map<String, Object> currentSprintLeafNodeDataMap = new HashMap<>();

				List<String> completedIssues = new ArrayList<>(
						KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(sd, CommonConstant.COMPLETED_ISSUES));
				List<String> notCompletedIssues = new ArrayList<>(KpiDataHelper
						.getIssuesIdListBasedOnTypeFromSprintDetails(sd, CommonConstant.NOT_COMPLETED_ISSUES));
				List<String> removedIssues = KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(sd,
						CommonConstant.PUNTED_ISSUES);
				List<String> addedIssues = KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(sd,
						CommonConstant.ADDED_ISSUES);
				// For Initial Commitment Issue : completed + notCompleted + removed - added
				List<String> initialCommitIssue = new ArrayList<>();
				initialCommitIssue.addAll(completedIssues);
				initialCommitIssue.addAll(notCompletedIssues);
				initialCommitIssue.addAll(removedIssues);
				initialCommitIssue.removeAll(addedIssues);
				sprintWiseAddedList = fetchedIssue.stream().filter(f -> addedIssues.contains(f.getNumber()))
						.collect(Collectors.toList());
				sprintWiseRemovedList = fetchedIssue.stream().filter(f -> removedIssues.contains(f.getNumber()))
						.collect(Collectors.toList());
				sprintWiseInitialComitList = fetchedIssue.stream()
						.filter(f -> initialCommitIssue.contains(f.getNumber())).collect(Collectors.toList());
				// For Scope Change : Added + Removed Issue
				Set<JiraIssue> sprintWiseScopeChangeList = new HashSet<>(sprintWiseAddedList);
				sprintWiseScopeChangeList.addAll(sprintWiseRemovedList);
				double storyChurnForCurrLeaf = 0.0d;
				currentSprintLeafNodeDataMap.put(SCOPE_CHANGE, sprintWiseScopeChangeList);
				currentSprintLeafNodeDataMap.put(INITIAL_SCOPE, sprintWiseInitialComitList);
				if (CollectionUtils.isNotEmpty(sprintWiseScopeChangeList)
						&& CollectionUtils.isNotEmpty(sprintWiseInitialComitList)) {

					storyChurnForCurrLeaf = calculateKPIMetrics(currentSprintLeafNodeDataMap);
				}

				Pair<String, String> sprint = Pair.of(sd.getBasicProjectConfigId().toString(), sd.getSprintID());
				sprintWiseAddedListMap.put(sprint, sprintWiseAddedList);
				sprintWiseRemovedListMap.put(sprint, sprintWiseRemovedList);
				sprintWiseStoryChurnDataMap.put(sprint, storyChurnForCurrLeaf);
				sprintNameMap.put(sprint, sd.getSprintName());
				setHoverMap(sprintWiseHowerMap, sprint, sprintWiseScopeChangeList, sprintWiseInitialComitList);
				setSprintWiseLogger(sprint, sprintWiseAddedList, sprintWiseRemovedList, sprintWiseInitialComitList);
			});
		}

		sprintLeafNodeList.forEach(node -> {

			String trendLineName = node.getProjectFilter().getName();

			FieldMapping fieldMapping = configHelperService.getFieldMappingMap()
					.get(node.getProjectFilter().getBasicProjectConfigId());

			Pair<String, String> currentNodeIdentifier = Pair
					.of(node.getProjectFilter().getBasicProjectConfigId().toString(), node.getSprintFilter().getId());

			double dreForCurrentLeaf;

			if (sprintWiseStoryChurnDataMap.containsKey(currentNodeIdentifier)) {
				dreForCurrentLeaf = sprintWiseStoryChurnDataMap.get(currentNodeIdentifier);
				List<JiraIssue> sprintWiseAddedList = sprintWiseAddedListMap.get(currentNodeIdentifier);
				List<JiraIssue> spirntWiseRemovedList = sprintWiseRemovedListMap.get(currentNodeIdentifier);
				String sprintName = sprintNameMap.get(currentNodeIdentifier);
				populateExcelDataObject(node.getSprintFilter().getName(), excelData, sprintWiseAddedList,
						spirntWiseRemovedList, issueKeyWiseHistoryMap, fieldMapping, sprintName);

			} else {
				dreForCurrentLeaf = 0.0d;
			}
			log.debug("[SCOPE-CHURN-SPRINT-WISE][{}]. STORY-CHURN for sprint {}  is {}", requestTrackerId,
					node.getSprintFilter().getName(), dreForCurrentLeaf);

			DataCount dataCount = new DataCount();
			dataCount.setData(String.valueOf(Math.round(dreForCurrentLeaf)));
			dataCount.setSProjectName(trendLineName);
			dataCount.setSSprintID(node.getSprintFilter().getId());
			dataCount.setSSprintName(node.getSprintFilter().getName());
			dataCount.setSprintIds(new ArrayList<>(Arrays.asList(node.getSprintFilter().getId())));
			dataCount.setSprintNames(new ArrayList<>(Arrays.asList(node.getSprintFilter().getName())));
			dataCount.setValue(dreForCurrentLeaf);
			dataCount.setHoverValue(sprintWiseHowerMap.get(currentNodeIdentifier));
			mapTmp.get(node.getId()).setValue(new ArrayList<DataCount>(Arrays.asList(dataCount)));
			trendValueList.add(dataCount);

		});
		kpiElement.setExcelData(excelData);
		kpiElement.setExcelColumns(KPIExcelColumn.SCOPE_CHURN.getColumns());

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> fetchKPIDataFromDb(List<Node> leafNodeList, String startDate, String endDate,
			KpiRequest kpiRequest) {
		Map<String, List<String>> mapOfFilters = new LinkedHashMap<>();
		Map<String, Object> resultListMap = new HashMap<>();
		List<String> sprintList = new ArrayList<>();
		List<String> basicProjectConfigIds = new ArrayList<>();
		Map<String, Map<String, Object>> uniqueProjectMap = new HashMap<>();
		String requestTrackerId = getRequestTrackerId();
		leafNodeList.forEach(leaf -> {
			Map<String, Object> mapOfProjectFilters = new LinkedHashMap<>();

			ObjectId basicProjectConfigId = leaf.getProjectFilter().getBasicProjectConfigId();
			sprintList.add(leaf.getSprintFilter().getId());
			basicProjectConfigIds.add(basicProjectConfigId.toString());
			FieldMapping fieldMapping = configHelperService.getFieldMappingMap().get(basicProjectConfigId);
			if (CollectionUtils.isNotEmpty(fieldMapping.getJiraStoryIdentificationKPI164())) {
				KpiDataHelper.prepareFieldMappingDefectTypeTransformation(mapOfProjectFilters,
						fieldMapping.getJiradefecttype(), fieldMapping.getJiraStoryIdentificationKPI164(),
						JiraFeature.ISSUE_TYPE.getFieldValueInFeature());
				uniqueProjectMap.put(basicProjectConfigId.toString(), mapOfProjectFilters);
			} else {
				// In Case of no issue type fetching all the issueType for that proj
				uniqueProjectMap.put(basicProjectConfigId.toString(), new HashMap<>());
			}
		});

		List<SprintDetails> sprintDetails = new ArrayList<>(sprintRepository.findBySprintIDIn(sprintList));

		Set<String> totalIssue = new HashSet<>();
		Set<String> scopeChangeIssue = new HashSet<>();
		sprintDetails.forEach(dbSprintDetail -> {
			if (CollectionUtils.isNotEmpty(dbSprintDetail.getCompletedIssues())) {
				totalIssue.addAll(KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(dbSprintDetail,
						CommonConstant.COMPLETED_ISSUES));
			}
			if (CollectionUtils.isNotEmpty(dbSprintDetail.getNotCompletedIssues())) {
				totalIssue.addAll(KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(dbSprintDetail,
						CommonConstant.NOT_COMPLETED_ISSUES));
			}
			if (CollectionUtils.isNotEmpty(dbSprintDetail.getPuntedIssues())) {
				List<String> removedIssues = KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(dbSprintDetail,
						CommonConstant.PUNTED_ISSUES);
				totalIssue.addAll(removedIssues);
				scopeChangeIssue.addAll(removedIssues);
			}
			if (CollectionUtils.isNotEmpty(dbSprintDetail.getAddedIssues())) {
				List<String> addedIssues = KpiDataHelper.getIssuesIdListBasedOnTypeFromSprintDetails(dbSprintDetail,
						CommonConstant.ADDED_ISSUES);
				totalIssue.addAll(addedIssues);
				scopeChangeIssue.addAll(addedIssues);
			}
		});

		/** additional filter **/
		KpiDataHelper.createAdditionalFilterMap(kpiRequest, mapOfFilters, Constant.SCRUM, DEV, filterHelperService);

		mapOfFilters.put(JiraFeature.BASIC_PROJECT_CONFIG_ID.getFieldValueInFeature(),
				basicProjectConfigIds.stream().distinct().collect(Collectors.toList()));

		if (CollectionUtils.isNotEmpty(totalIssue)) {
			List<JiraIssueCustomHistory> scopeChangeIssueHistories = new ArrayList<>();
			List<JiraIssue> totalJiraIssue = jiraIssueRepository.findIssueByNumber(mapOfFilters, totalIssue,
					uniqueProjectMap);
			resultListMap.put(SPRINT_DETAILS, sprintDetails);
			resultListMap.put(TOTAL_ISSUE, totalJiraIssue);
			if (requestTrackerId.toLowerCase().contains(KPISource.EXCEL.name().toLowerCase())) {
				// Fetching history only for change/removed issue date on Excel req
				scopeChangeIssueHistories = jiraIssueCustomHistoryRepository.findByStoryIDInAndBasicProjectConfigIdIn(
						new ArrayList<>(scopeChangeIssue),
						basicProjectConfigIds.stream().distinct().collect(Collectors.toList()));
				resultListMap.put(SCOPE_CHANGE_ISSUE_HISTORY, scopeChangeIssueHistories);
			}
			setDbQueryLogger(sprintDetails, totalJiraIssue, scopeChangeIssueHistories);
		}
		return resultListMap;
	}

	/**
	 * Sets DB Query log
	 * 
	 * @param sprintDetails
	 * @param totalJiraIssue
	 * @param scopeChangeIssueHistories
	 */
	private void setDbQueryLogger(List<SprintDetails> sprintDetails, List<JiraIssue> totalJiraIssue,
			List<JiraIssueCustomHistory> scopeChangeIssueHistories) {
		if (customApiConfig.getApplicationDetailedLogger().equalsIgnoreCase("on")) {
			log.info(SEPARATOR_ASTERISK);
			log.info("************* SCOPE CHURN (dB) *******************");
			log.info("SprintDetails[{}]: {}", sprintDetails.size(), sprintDetails);
			log.info("TotalJiraIssue[{}]: {}", totalJiraIssue.size(), totalJiraIssue);
			log.info("ScopeChangeJiraIssueHistory[{}]: {}", scopeChangeIssueHistories.size(),
					scopeChangeIssueHistories);
			log.info(SEPARATOR_ASTERISK);
			log.info("******************X----X*******************");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Double calculateKPIMetrics(Map<String, Object> scopeChangeAndInitialScopeMap) {
		int scopeChange = ((Set<JiraIssue>) scopeChangeAndInitialScopeMap.get(SCOPE_CHANGE)).size();
		int initialScope = ((List<JiraIssue>) scopeChangeAndInitialScopeMap.get(INITIAL_SCOPE)).size();
		return (double) Math.round((100.0 * scopeChange) / (initialScope));
	}

	/**
	 * Sets map to show on hover of sprint node.
	 *
	 * @param sprintWiseHowerMap
	 * @param sprint
	 * @param scopeChange
	 * @param initialIssue
	 */
	private void setHoverMap(Map<Pair<String, String>, Map<String, Object>> sprintWiseHowerMap,
			Pair<String, String> sprint, Set<JiraIssue> scopeChange, List<JiraIssue> initialIssue) {
		Map<String, Object> howerMap = new LinkedHashMap<>();
		if (CollectionUtils.isNotEmpty(scopeChange)) {
			howerMap.put(SCOPE_CHANGE, scopeChange.size());
		} else {
			howerMap.put(SCOPE_CHANGE, 0);
		}
		if (CollectionUtils.isNotEmpty(initialIssue)) {
			howerMap.put(INITIAL_SCOPE, initialIssue.size());
		} else {
			howerMap.put(INITIAL_SCOPE, 0);
		}
		sprintWiseHowerMap.put(sprint, howerMap);
	}

	/**
	 * Sets logger for sprint level KPI data.
	 * 
	 * @param sprint
	 * @param sprintWiseAddedList
	 * @param sprintWiseRemovedList
	 * @param sprintWiseInitialComitList
	 */
	private void setSprintWiseLogger(Pair<String, String> sprint, List<JiraIssue> sprintWiseAddedList,
			List<JiraIssue> sprintWiseRemovedList, List<JiraIssue> sprintWiseInitialComitList) {
		if (customApiConfig.getApplicationDetailedLogger().equalsIgnoreCase("on")) {
			log.debug(SEPARATOR_ASTERISK);
			log.debug("************* SPRINT WISE SCOPE CHURN *******************");
			log.debug("Sprint: {}", sprint.getValue());
			log.debug("SprintWiseAddedList[{}]: {}", sprintWiseAddedList.size(),
					sprintWiseAddedList.stream().map(JiraIssue::getNumber).collect(Collectors.toList()));
			log.debug("SprintWiseRemovedList[{}]: {}", sprintWiseRemovedList.size(),
					sprintWiseAddedList.stream().map(JiraIssue::getNumber).collect(Collectors.toList()));
			log.debug("SprintWiseInitialCommitList[{}]: {}", sprintWiseInitialComitList.size(),
					sprintWiseAddedList.stream().map(JiraIssue::getNumber).collect(Collectors.toList()));
			log.debug(SEPARATOR_ASTERISK);
		}
	}

	/**
	 * Method to populate the Excel
	 * 
	 * @param sprintName
	 * @param excelData
	 * @param sprintWiseAddedList
	 * @param sprintWiseRemovedList
	 * @param issueWiseHistoryMap
	 * @param fieldMapping
	 * @param curSprintName
	 */
	private void populateExcelDataObject(String sprintName, List<KPIExcelData> excelData,
			List<JiraIssue> sprintWiseAddedList, List<JiraIssue> sprintWiseRemovedList,
			Map<String, List<JiraHistoryChangeLog>> issueWiseHistoryMap, FieldMapping fieldMapping,
			String curSprintName) {
		String requestTrackerId = getRequestTrackerId();

		if (requestTrackerId.toLowerCase().contains(KPISource.EXCEL.name().toLowerCase())) {
			Map<String, String> addedIssueDateMap = KpiDataHelper.processSprintIssues(sprintWiseAddedList,
					curSprintName, issueWiseHistoryMap, CommonConstant.ADDED);
			Map<String, String> removedIssueDateMap = KpiDataHelper.processSprintIssues(sprintWiseRemovedList,
					curSprintName, issueWiseHistoryMap, CommonConstant.REMOVED);

			if (CollectionUtils.isNotEmpty(sprintWiseRemovedList)) {
				Map<String, JiraIssue> totalSprintStoryMap = new HashMap<>();
				sprintWiseAddedList.forEach(issue -> totalSprintStoryMap.putIfAbsent(issue.getNumber(), issue));
				sprintWiseRemovedList.forEach(issue -> totalSprintStoryMap.putIfAbsent(issue.getNumber(), issue));
				KPIExcelUtility.populateStoryChunk(sprintName, totalSprintStoryMap, addedIssueDateMap,
						removedIssueDateMap, excelData, fieldMapping);

			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double calculateKpiValue(List<Double> valueList, String kpiName) {
		return calculateKpiValueForDouble(valueList, kpiName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getQualifierType() {
		return KPICode.SCOPE_CHURN.name();
	}
}