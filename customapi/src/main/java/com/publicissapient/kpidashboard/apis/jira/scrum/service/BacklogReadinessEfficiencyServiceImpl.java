package com.publicissapient.kpidashboard.apis.jira.scrum.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.common.service.impl.KpiHelperService;
import com.publicissapient.kpidashboard.apis.config.CustomApiConfig;
import com.publicissapient.kpidashboard.apis.enums.Filters;
import com.publicissapient.kpidashboard.apis.enums.JiraFeature;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.apis.enums.KPIExcelColumn;
import com.publicissapient.kpidashboard.apis.errors.ApplicationException;
import com.publicissapient.kpidashboard.apis.jira.service.JiraKPIService;
import com.publicissapient.kpidashboard.apis.jira.service.SprintVelocityServiceHelper;
import com.publicissapient.kpidashboard.apis.model.IterationKpiData;
import com.publicissapient.kpidashboard.apis.model.IterationKpiFilters;
import com.publicissapient.kpidashboard.apis.model.IterationKpiFiltersOptions;
import com.publicissapient.kpidashboard.apis.model.IterationKpiModalValue;
import com.publicissapient.kpidashboard.apis.model.IterationKpiValue;
import com.publicissapient.kpidashboard.apis.model.KpiElement;
import com.publicissapient.kpidashboard.apis.model.KpiRequest;
import com.publicissapient.kpidashboard.apis.model.Node;
import com.publicissapient.kpidashboard.apis.model.TreeAggregatorDetail;
import com.publicissapient.kpidashboard.apis.util.CommonUtils;
import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.model.application.DataCount;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.jira.IssueBacklog;
import com.publicissapient.kpidashboard.common.model.jira.IssueBacklogCustomHistory;
import com.publicissapient.kpidashboard.common.model.jira.IssueDetails;
import com.publicissapient.kpidashboard.common.model.jira.JiraHistoryChangeLog;
import com.publicissapient.kpidashboard.common.model.jira.JiraIssue;
import com.publicissapient.kpidashboard.common.model.jira.SprintDetails;
import com.publicissapient.kpidashboard.common.repository.jira.IssueBacklogCustomHistoryRepository;
import com.publicissapient.kpidashboard.common.repository.jira.IssueBacklogRepository;

/**
 * Jira service class to fetch backlog readiness kpi details
 *
 * @author dhachuda
 *
 */
@Component
public class BacklogReadinessEfficiencyServiceImpl extends JiraKPIService<Integer, List<Object>, Map<String, Object>> {

	public static final String UNCHECKED = "unchecked";
	private static final Logger LOGGER = LoggerFactory.getLogger(BacklogReadinessEfficiencyServiceImpl.class);
	private static final double DEFAULT_BACKLOG_STRENGTH = 0.0;
	private static final String DAYS = "days";
	private static final String SPRINT = "Sprint";
	private static final String SP = "SP";
	private static final String SPRINT_VELOCITY_KEY = "sprintVelocityKey";
	private static final String SPRINT_WISE_SPRINT_DETAIL_MAP = "sprintWiseSprintDetailMap";
	private static final String HISTORY = "history";
	private static final String READINESS_CYCLE_TIME = "Readiness Cycle time";
	private static final String BACKLOG_STRENGTH = "Backlog Strength";
	private static final String READY_BACKLOG = "Ready Backlog";
	private static final String SEARCH_BY_ISSUE_TYPE = "Filter by issue type";
	private static final String SEARCH_BY_PRIORITY = "Filter by priority";
	private static final String ISSUES = "issues";

	private static final String OVERALL = "Overall";

	@Autowired
	private IssueBacklogRepository issueBacklogRepository;

	@Autowired
	private IssueBacklogCustomHistoryRepository issueBacklogCustomHistoryRepository;

	@Autowired
	private ConfigHelperService configHelperService;

	@Autowired
	private KpiHelperService kpiHelperService;

	@Autowired
	private SprintVelocityServiceHelper velocityServiceHelper;

	@Autowired
	private CustomApiConfig customApiConfig;

	/**
	 * Methods get the data for the KPI
	 */
	@Override
	public KpiElement getKpiData(KpiRequest kpiRequest, KpiElement kpiElement,
			TreeAggregatorDetail treeAggregatorDetail) throws ApplicationException {

		LOGGER.info("Backlog readiness efficiency service {}", kpiRequest.getRequestTrackerId());

		DataCount trendValue = new DataCount();
		treeAggregatorDetail.getMapOfListOfLeafNodes().forEach((k, v) -> {
			Filters filters = Filters.getFilter(k);
			if (filters == Filters.SPRINT) {

				projectWiseLeafNodeValue(v, trendValue, kpiElement, kpiRequest);
			}
		});
		return kpiElement;
	}

	@Override
	public String getQualifierType() {
		return KPICode.BACKLOG_READINESS_EFFICIENCY.name();
	}

	@Override
	public Integer calculateKPIMetrics(Map<String, Object> subCategoryMap) {
		return null;
	}

	/**
	 * Fetches the data from the backlog where the story have completed the grooming
	 * corresponding history and previous sprint data
	 */
	@Override
	public Map<String, Object> fetchKPIDataFromDb(List<Node> leafNodeList, String startDate, String endDate,
			KpiRequest kpiRequest) {
		Map<String, Object> resultListMap = new HashMap<>();

		List<IssueBacklog> issues = getBackLogStory(leafNodeList.get(0).getProjectFilter().getBasicProjectConfigId());
		resultListMap.put(ISSUES, issues);

		List<String> issueNumbers = issues.stream().map(IssueBacklog::getNumber).collect(Collectors.toList());
		List<IssueBacklogCustomHistory> historyForIssues = issueBacklogCustomHistoryRepository
				.findByStoryIDIn(issueNumbers);
		resultListMap.put(HISTORY, historyForIssues);

		List<Node> sprintForStregthCalculation = leafNodeList.stream()
				.limit(customApiConfig.getSprintCountForBackLogStrength()).collect(Collectors.toList());

		Map<String, Object> sprintVelocityStoryMap = kpiHelperService
				.fetchSprintVelocityDataFromDb(sprintForStregthCalculation, kpiRequest);

		resultListMap.putAll(sprintVelocityStoryMap);

		return resultListMap;
	}

	/**
	 * Populates KPI value to sprint leaf nodes and gives the trend analysis at
	 * sprint level.
	 *
	 * @param sprintLeafNodeList
	 * @param trendValue
	 * @param kpiElement
	 * @param kpiRequest
	 */
	@SuppressWarnings("unchecked")
	private void projectWiseLeafNodeValue(List<Node> sprintLeafNodeList, DataCount trendValue, KpiElement kpiElement,
			KpiRequest kpiRequest) {
		String requestTrackerId = getRequestTrackerId();

		sprintLeafNodeList.sort((node1, node2) -> node1.getSprintFilter().getStartDate()
				.compareTo(node2.getSprintFilter().getStartDate()));
		List<Node> latestSprintNode = new ArrayList<>();
		Node latestSprint = sprintLeafNodeList.get(0);
		Optional.ofNullable(latestSprint).ifPresent(latestSprintNode::add);
		FieldMapping fieldMapping = latestSprint != null
				? configHelperService.getFieldMappingMap()
						.get(latestSprint.getProjectFilter().getBasicProjectConfigId())
				: new FieldMapping();

		Map<String, Object> resultMap = fetchKPIDataFromDb(sprintLeafNodeList, null, null, kpiRequest);

		Double avgVelocity = getAverageSprintCapacity(sprintLeafNodeList,
				(List<SprintDetails>) resultMap.get(SPRINT_WISE_SPRINT_DETAIL_MAP),
				kpiHelperService.convertJiraIssueToBacklog((List<JiraIssue>) resultMap.get(SPRINT_VELOCITY_KEY)),
				fieldMapping);

		List<IssueBacklog> allIssues = (List<IssueBacklog>) resultMap.get(ISSUES);
		if (CollectionUtils.isNotEmpty(allIssues)) {
			LOGGER.info("Backlog items ready for development -> request id : {} total jira Issues : {}",
					requestTrackerId, allIssues.size());
			List<IssueBacklogCustomHistory> historyForIssues = (List<IssueBacklogCustomHistory>) resultMap.get(HISTORY);
			Map<String, Map<String, List<IssueBacklog>>> typeAndPriorityWiseIssues = allIssues.stream().collect(
					Collectors.groupingBy(IssueBacklog::getTypeName, Collectors.groupingBy(IssueBacklog::getPriority)));

			Set<String> issueTypes = new HashSet<>();
			Set<String> priorities = new HashSet<>();
			List<IterationKpiValue> iterationKpiValues = new ArrayList<>();
			List<Integer> overAllIssueCount = Arrays.asList(0);
			List<Double> overAllStoryPoints = Arrays.asList(0.0);
			AtomicLong overAllCycleTime = new AtomicLong(0);
			List<IterationKpiModalValue> overAllmodalValues = new ArrayList<>();

			typeAndPriorityWiseIssues.forEach((issueType, priorityWiseIssue) -> {
				priorityWiseIssue.forEach((priority, issues) -> {
					issueTypes.add(issueType);
					priorities.add(priority);
					int issueCount = 0;
					Double storyPoint = 0.0;
					long cycleTime = 0;
					List<IterationKpiModalValue> modalValues = new ArrayList<>();
					for (IssueBacklog issueBacklog : issues) {
						populateBackLogData(overAllmodalValues, modalValues, issueBacklog);
						issueCount = issueCount + 1;
						overAllIssueCount.set(0, overAllIssueCount.get(0) + 1);
						AtomicLong difference = getActivityCycleTimeForAnIssue(
								fieldMapping.getReadyForDevelopmentStatus(), historyForIssues, issueBacklog);
						cycleTime = cycleTime + difference.get();
						overAllCycleTime.set(overAllCycleTime.get() + difference.get());
						if (null != issueBacklog.getStoryPoints()) {
							storyPoint = storyPoint + issueBacklog.getStoryPoints();
							overAllStoryPoints.set(0, overAllStoryPoints.get(0) + issueBacklog.getStoryPoints());
						}
					}
					List<IterationKpiData> data = new ArrayList<>();
					IterationKpiData issuesForDevelopment = new IterationKpiData(READY_BACKLOG,
							Double.valueOf(issueCount), storyPoint, null, SP, modalValues);
					IterationKpiData backLogStrength = new IterationKpiData(BACKLOG_STRENGTH, DEFAULT_BACKLOG_STRENGTH,
							null, null, SPRINT, null);
					LOGGER.debug("Issue type: {} priority: {} Cycle time: {}", issueType, priority, cycleTime);
					IterationKpiData averageCycleTime = new IterationKpiData(READINESS_CYCLE_TIME,
							(double) Math.round(cycleTime / Double.valueOf(issueCount)), null, null, DAYS, null);
					data.add(issuesForDevelopment);
					data.add(backLogStrength);
					data.add(averageCycleTime);
					IterationKpiValue iterationKpiValue = new IterationKpiValue(issueType, priority, data);
					iterationKpiValues.add(iterationKpiValue);
				});

			});
			List<IterationKpiData> data = new ArrayList<>();

			IterationKpiData overAllIssues = new IterationKpiData(READY_BACKLOG,
					Double.valueOf(overAllIssueCount.get(0)), overAllStoryPoints.get(0), null, SP, overAllmodalValues);
			LOGGER.debug("Overall  the avg velocity of the previous sprint: {}", avgVelocity);
			double strength = (double) Math.round(overAllStoryPoints.get(0) / avgVelocity * 100) / 100;
			IterationKpiData backLogStrength = new IterationKpiData(BACKLOG_STRENGTH, strength, null, null, SPRINT,
					null);
			LOGGER.debug("Overall  the cycle time is : {} ", overAllCycleTime.get());
			IterationKpiData averageOverAllCycleTime = new IterationKpiData(READINESS_CYCLE_TIME,
					(double) Math.round(overAllCycleTime.get() / Double.valueOf(overAllIssueCount.get(0))), null, null,
					DAYS, null);
			data.add(overAllIssues);
			data.add(backLogStrength);
			data.add(averageOverAllCycleTime);
			IterationKpiValue overAllIterationKpiValue = new IterationKpiValue(OVERALL, OVERALL, data);
			iterationKpiValues.add(overAllIterationKpiValue);

			// Create kpi level filters
			IterationKpiFiltersOptions filter1 = new IterationKpiFiltersOptions(SEARCH_BY_ISSUE_TYPE, issueTypes);
			IterationKpiFiltersOptions filter2 = new IterationKpiFiltersOptions(SEARCH_BY_PRIORITY, priorities);
			IterationKpiFilters iterationKpiFilters = new IterationKpiFilters(filter1, filter2);
			// Modal Heads Options
			trendValue.setValue(iterationKpiValues);
			kpiElement.setFilters(iterationKpiFilters);
			kpiElement.setModalHeads(KPIExcelColumn.BACKLOG_READINESS_EFFICIENCY.getColumns());
			kpiElement.setTrendValueList(trendValue);
		}
	}

	/**
	 * Calculates the activity cycle time based on the history data for an issue
	 *
	 * @param status
	 * @param historyForIssues
	 * @param issueBacklog
	 * @return
	 */
	private AtomicLong getActivityCycleTimeForAnIssue(String status, List<IssueBacklogCustomHistory> historyForIssues,
			IssueBacklog issueBacklog) {
		Optional<IssueBacklogCustomHistory> jiraCustomHistory = historyForIssues.stream()
				.filter(history -> history.getStoryID().equals(issueBacklog.getNumber())).findAny();
		AtomicLong difference = new AtomicLong(0);
		if (jiraCustomHistory.isPresent()) {

			Optional<JiraHistoryChangeLog> sprint = jiraCustomHistory.get().getStatusUpdationLog().stream()
					.filter(sprintDetails -> sprintDetails.getChangedTo().equalsIgnoreCase(status))
					.sorted(Comparator.comparing(JiraHistoryChangeLog::getUpdatedOn).reversed()).findFirst();
			if (sprint.isPresent()) {
				DateTime createdDate = new DateTime(jiraCustomHistory.get().getCreatedDate(), DateTimeZone.UTC);
				DateTime changedDate = new DateTime(sprint.get().getUpdatedOn().toString(), DateTimeZone.UTC);
				difference.set(difference.get() + new Duration(createdDate, changedDate).getStandardDays());
			}
		}
		LOGGER.debug("cycle time for the issue {} is {}", issueBacklog.getNumber(), difference.get());
		return difference;
	}

	/**
	 * Gets the sprint velocity of n number of previous sprint
	 *
	 * @param leafNodeList
	 * @param sprintDetails
	 * @param allIssueBacklog
	 * @param fieldMapping
	 * @return
	 */
	private Double getAverageSprintCapacity(List<Node> leafNodeList, List<SprintDetails> sprintDetails,
			List<IssueBacklog> allIssueBacklog, FieldMapping fieldMapping) {
		int sprintCountForBackLogStrength = customApiConfig.getSprintCountForBackLogStrength();
		List<Node> inputNodes = new ArrayList<>(leafNodeList);
		Collections.reverse(inputNodes);

		List<Node> sprintForStregthCalculation = inputNodes.stream()
				.filter(node -> null != node.getSprintFilter().getEndDate()
						&& DateTime.parse(node.getSprintFilter().getEndDate()).isBefore(DateTime.now()))
				.limit(sprintCountForBackLogStrength).collect(Collectors.toList());

		Map<Pair<String, String>, List<IssueBacklog>> sprintWiseIssues = new HashMap<>();
		Map<Pair<String, String>, Set<IssueDetails>> currentSprintLeafVelocityMap = new HashMap<>();
		getSprintForProject(allIssueBacklog, sprintWiseIssues, sprintDetails, currentSprintLeafVelocityMap);
		AtomicDouble storyPoint = new AtomicDouble();
		sprintForStregthCalculation.forEach(node -> {
			Pair<String, String> currentNodeIdentifier = Pair
					.of(node.getProjectFilter().getBasicProjectConfigId().toString(), node.getSprintFilter().getId());
			double sprintVelocityForCurrentLeaf = calculateSprintVelocityValue(currentSprintLeafVelocityMap,
					currentNodeIdentifier, sprintWiseIssues, fieldMapping);
			storyPoint.set(storyPoint.doubleValue() + sprintVelocityForCurrentLeaf);
		});
		LOGGER.debug("Velocity for {} sprints is {}", sprintCountForBackLogStrength, storyPoint.get());
		return Double.valueOf(storyPoint.get() / sprintCountForBackLogStrength);
	}

	public double calculateSprintVelocityValue(
			Map<Pair<String, String>, Set<IssueDetails>> currentSprintLeafVelocityMap,
			Pair<String, String> currentNodeIdentifier, Map<Pair<String, String>, List<IssueBacklog>> sprintIssues,
			FieldMapping fieldMapping) {
		double sprintVelocityForCurrentLeaf = 0.0d;
		if (CollectionUtils.isNotEmpty(sprintIssues.get(currentNodeIdentifier))) {
			LOGGER.debug("Current Node identifier is present in sprintjirsissues map {} ", currentNodeIdentifier);
			List<IssueBacklog> issueBacklogList = sprintIssues.get(currentNodeIdentifier);
			if (StringUtils.isNotEmpty(fieldMapping.getEstimationCriteria())
					&& fieldMapping.getEstimationCriteria().equalsIgnoreCase(CommonConstant.STORY_POINT)) {
				sprintVelocityForCurrentLeaf = issueBacklogList.stream()
						.mapToDouble(ji -> Double.valueOf(ji.getEstimate())).sum();
			} else {
				double totalOriginalEstimate = issueBacklogList.stream()
						.filter(issueBacklog -> Objects.nonNull(issueBacklog.getOriginalEstimateMinutes()))
						.mapToDouble(IssueBacklog::getOriginalEstimateMinutes).sum();
				double totalOriginalEstimateInHours = totalOriginalEstimate / 60;
				sprintVelocityForCurrentLeaf = totalOriginalEstimateInHours / 60;
			}
		} else {
			if (Objects.nonNull(currentSprintLeafVelocityMap.get(currentNodeIdentifier))) {
				LOGGER.debug("Current Node identifier is present in currentSprintLeafVelocityMap map {} ",
						currentNodeIdentifier);
				Set<IssueDetails> issueDetailsSet = currentSprintLeafVelocityMap.get(currentNodeIdentifier);
				if (StringUtils.isNotEmpty(fieldMapping.getEstimationCriteria())
						&& fieldMapping.getEstimationCriteria().equalsIgnoreCase(CommonConstant.STORY_POINT)) {
					sprintVelocityForCurrentLeaf = issueDetailsSet.stream()
							.filter(issueDetails -> Objects.nonNull(issueDetails.getSprintIssue().getStoryPoints()))
							.mapToDouble(issueDetails -> issueDetails.getSprintIssue().getStoryPoints()).sum();
				} else {
					double totalOriginalEstimate = issueDetailsSet.stream().filter(
							issueDetails -> Objects.nonNull(issueDetails.getSprintIssue().getOriginalEstimate()))
							.mapToDouble(issueDetails -> issueDetails.getSprintIssue().getOriginalEstimate()).sum();
					double totalOriginalEstimateInHours = totalOriginalEstimate / 60;
					sprintVelocityForCurrentLeaf = totalOriginalEstimateInHours / 60;
				}
			}
		}
		LOGGER.debug("Sprint velocity for the sprint {} is {}", currentNodeIdentifier.getValue(),
				sprintVelocityForCurrentLeaf);
		return sprintVelocityForCurrentLeaf;
	}

	public void getSprintForProject(List<IssueBacklog> allIssueBacklog,
			Map<Pair<String, String>, List<IssueBacklog>> sprintWiseIssues, List<SprintDetails> sprintDetails,
			Map<Pair<String, String>, Set<IssueDetails>> currentSprintLeafVelocityMap) {
		if (CollectionUtils.isNotEmpty(sprintDetails)) {
			sprintDetails.forEach(sd -> {
				Set<IssueDetails> filterIssueDetailsSet = new HashSet<>();
				if (CollectionUtils.isNotEmpty(sd.getCompletedIssues())) {
					sd.getCompletedIssues().stream().forEach(sprintIssue -> {
						allIssueBacklog.stream().forEach(issueBacklog -> {
							if (sprintIssue.getNumber().equals(issueBacklog.getNumber())) {
								IssueDetails issueDetails = new IssueDetails();
								issueDetails.setSprintIssue(sprintIssue);
								issueDetails.setUrl(issueBacklog.getUrl());
								issueDetails.setDesc(issueBacklog.getName());
								filterIssueDetailsSet.add(issueDetails);
							}
						});
						Pair<String, String> currentNodeIdentifier = Pair.of(sd.getBasicProjectConfigId().toString(),
								sd.getSprintID());
						LOGGER.debug("Issue count for the sprint {} is {}", sd.getSprintID(),
								filterIssueDetailsSet.size());
						currentSprintLeafVelocityMap.put(currentNodeIdentifier, filterIssueDetailsSet);
					});
				}
			});
		} else {
			if (CollectionUtils.isNotEmpty(allIssueBacklog)) {
				// start : for azure board sprint details collections empty so
				// that we have to
				// prepare data from jira issue
				Map<String, List<IssueBacklog>> projectWiseIssueBacklogs = allIssueBacklog.stream()
						.collect(Collectors.groupingBy(IssueBacklog::getBasicProjectConfigId));
				projectWiseIssueBacklogs.forEach((basicProjectConfigId, projectWiseIssuesList) -> {
					Map<String, List<IssueBacklog>> sprintWiseIssueBacklogs = projectWiseIssuesList.stream()
							.filter(issueBacklog -> Objects.nonNull(issueBacklog.getSprintID()))
							.collect(Collectors.groupingBy(IssueBacklog::getSprintID));
					sprintWiseIssueBacklogs.forEach((sprintId, sprintWiseIssuesList) -> sprintWiseIssues
							.put(Pair.of(basicProjectConfigId, sprintId), sprintWiseIssuesList));
				});
			}
			// end : for azure board sprint details collections empty so that we
			// have to
			// prepare data from jira issue.
		}
	}

	public List<IssueBacklog> getBackLogStory(ObjectId basicProjectId) {
		Map<String, List<String>> mapOfFilters = new LinkedHashMap<>();
		Map<String, Object> mapOfProjectFilters = new LinkedHashMap<>();
		Map<String, Map<String, Object>> uniqueProjectMap = new HashMap<>();

		List<String> basicProjectConfigIds = new ArrayList<>();
		basicProjectConfigIds.add(basicProjectId.toString());

		FieldMapping fieldMapping = configHelperService.getFieldMappingMap().get(basicProjectId);

		List<String> statusList = new ArrayList<>();
		if (Optional.ofNullable(fieldMapping.getReadyForDevelopmentStatus()).isPresent()) {
			statusList.add(fieldMapping.getReadyForDevelopmentStatus());
		}
		mapOfProjectFilters.put(JiraFeature.JIRA_ISSUE_STATUS.getFieldValueInFeature(),
				CommonUtils.convertToPatternList(statusList));
		mapOfFilters.put(JiraFeature.BASIC_PROJECT_CONFIG_ID.getFieldValueInFeature(),
				basicProjectConfigIds.stream().distinct().collect(Collectors.toList()));

		mapOfFilters.put(JiraFeature.SPRINT_ID.getFieldValueInFeature(), Lists.newArrayList("", null));

		uniqueProjectMap.put(basicProjectId.toString(), mapOfProjectFilters);
		return issueBacklogRepository.findIssuesBySprintAndType(mapOfFilters, uniqueProjectMap);
	}

}