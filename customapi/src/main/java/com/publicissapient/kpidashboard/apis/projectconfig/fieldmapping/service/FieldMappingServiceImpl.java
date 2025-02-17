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

package com.publicissapient.kpidashboard.apis.projectconfig.fieldmapping.service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.publicissapient.kpidashboard.apis.abac.UserAuthorizedProjectsService;
import com.publicissapient.kpidashboard.apis.appsetting.service.ConfigHelperService;
import com.publicissapient.kpidashboard.apis.auth.service.AuthenticationService;
import com.publicissapient.kpidashboard.apis.auth.token.TokenAuthenticationService;
import com.publicissapient.kpidashboard.apis.common.service.CacheService;
import com.publicissapient.kpidashboard.apis.common.service.impl.KpiHelperService;
import com.publicissapient.kpidashboard.apis.enums.FieldMappingEnum;
import com.publicissapient.kpidashboard.apis.enums.KPICode;
import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.constant.ProcessorConstants;
import com.publicissapient.kpidashboard.common.model.ProcessorExecutionTraceLog;
import com.publicissapient.kpidashboard.common.model.application.BaseFieldMappingStructure;
import com.publicissapient.kpidashboard.common.model.application.ConfigurationHistoryChangeLog;
import com.publicissapient.kpidashboard.common.model.application.FieldMapping;
import com.publicissapient.kpidashboard.common.model.application.FieldMappingResponse;
import com.publicissapient.kpidashboard.common.model.application.FieldMappingStructure;
import com.publicissapient.kpidashboard.common.model.application.ProjectBasicConfig;
import com.publicissapient.kpidashboard.common.model.application.ProjectToolConfig;
import com.publicissapient.kpidashboard.common.repository.application.FieldMappingRepository;
import com.publicissapient.kpidashboard.common.repository.application.ProjectBasicConfigRepository;
import com.publicissapient.kpidashboard.common.repository.application.ProjectToolConfigRepository;
import com.publicissapient.kpidashboard.common.repository.tracelog.ProcessorExecutionTraceLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anisingh4
 */
@Service
@Slf4j
public class FieldMappingServiceImpl implements FieldMappingService {

	public static final String INVALID_PROJECT_TOOL_CONFIG_ID = "Invalid projectToolConfigId";
	public static final String HISTORY = "history";
	public static final String OBJECT_ID = "org.bson.types.ObjectId";
	public static final String DOUBLE = "java.lang.Double";
	@Autowired
	private FieldMappingRepository fieldMappingRepository;

	@Autowired
	private ProjectToolConfigRepository toolConfigRepository;

	@Autowired
	private ProjectBasicConfigRepository projectBasicConfigRepository;

	@Autowired
	private ConfigHelperService configHelperService;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private TokenAuthenticationService tokenAuthenticationService;

	@Autowired
	private UserAuthorizedProjectsService authorizedProjectsService;

	@Autowired
	private ProcessorExecutionTraceLogRepository processorExecutionTraceLogRepository;

	@Autowired
	private MongoTemplate operations;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private KpiHelperService kPIHelperService;

	@Override
	public FieldMapping getFieldMapping(String projectToolConfigId) {

		if (!ObjectId.isValid(projectToolConfigId)) {
			throw new IllegalArgumentException(INVALID_PROJECT_TOOL_CONFIG_ID);
		}
		if (!authorizedProjectsService.ifSuperAdminUser() && !hasProjectAccess(projectToolConfigId)) {
			throw new AccessDeniedException("Access is denied");
		}
		return fieldMappingRepository.findByProjectToolConfigId(new ObjectId(projectToolConfigId));
	}

	@Override
	public FieldMapping addFieldMapping(String projectToolConfigId, FieldMapping fieldMapping, ObjectId basicProjectConfigId) {

		if (!ObjectId.isValid(projectToolConfigId)) {
			throw new IllegalArgumentException(INVALID_PROJECT_TOOL_CONFIG_ID);
		}

		if (fieldMapping == null) {
			throw new IllegalArgumentException("Field mapping can't be null");
		}

		fieldMapping.setProjectToolConfigId(new ObjectId(projectToolConfigId));
		fieldMapping.setBasicProjectConfigId(basicProjectConfigId);

		FieldMapping existingFieldMapping = fieldMappingRepository
				.findByProjectToolConfigId(new ObjectId(projectToolConfigId));
		if (existingFieldMapping != null) {
			fieldMapping.setId(existingFieldMapping.getId());
			updateJiraData(existingFieldMapping.getBasicProjectConfigId(), fieldMapping, existingFieldMapping);
		} else {
			// when no fieldmapping is present in the db, all the history should get
			// maintained
			updateJiraData(fieldMapping.getBasicProjectConfigId(), fieldMapping, new FieldMapping());
		}

		FieldMapping mapping = fieldMappingRepository.save(fieldMapping);
		cacheService.clearCache(CommonConstant.CACHE_FIELD_MAPPING_MAP);
		clearCache();
		return mapping;
	}

	/**
	 * Checks if fields are updated and then unset changeDate in jira collections.
	 *
	 * @param basicProjectConfigId
	 *            basicProjectConfigId
	 * @param fieldMapping
	 *            fieldMapping
	 * @param existingFieldMapping
	 *            existingFieldMapping
	 */
	private void updateJiraData(ObjectId basicProjectConfigId, FieldMapping fieldMapping,
			FieldMapping existingFieldMapping) {
		Optional<ProjectBasicConfig> projectBasicConfigOpt = projectBasicConfigRepository
				.findById(basicProjectConfigId);
		if (projectBasicConfigOpt.isPresent()) {
			ProjectBasicConfig projectBasicConfig = projectBasicConfigOpt.get();
			Optional<ProjectToolConfig> projectToolConfigOpt = toolConfigRepository
					.findById(fieldMapping.getProjectToolConfigId());
			updateFields(fieldMapping, existingFieldMapping, projectBasicConfig, projectToolConfigOpt);
			removeTraceLog(basicProjectConfigId);
			projectToolConfigOpt
					.ifPresent(projectToolConfig -> saveTemplateCode(projectBasicConfig, projectToolConfig));

		}

	}

	/**
	 * if jiraIterationCompletionStatusCustomField field mapping changes then put
	 * identifier to change in sprint report issues based on status for azure board
	 *
	 * @param fieldMapping
	 *            fieldMapping
	 * @param existingFieldMapping
	 *            existingFieldMapping
	 * @param projectBasicConfig
	 *            projectBasicConfig
	 * @param projectToolConfigOpt
	 *            projectToolConfigOpt
	 */
	private void updateFields(FieldMapping fieldMapping, FieldMapping existingFieldMapping,
			ProjectBasicConfig projectBasicConfig, Optional<ProjectToolConfig> projectToolConfigOpt) {
		// azureSprintReportStatusUpdateBasedOnFieldChange
		if (projectToolConfigOpt.isPresent()
				&& projectToolConfigOpt.get().getToolName().equals(ProcessorConstants.AZURE)
				&& !projectBasicConfig.getIsKanban() && checkFieldsForUpdation(fieldMapping, existingFieldMapping,
						Collections.singletonList("jiraIterationCompletionStatusCustomField"))) {
			ProjectToolConfig projectToolConfig = projectToolConfigOpt.get();
			projectToolConfig.setAzureIterationStatusFieldUpdate(true);
			toolConfigRepository.save(projectToolConfig);
		} else {
			// update all fields
			checkAllFieldsForUpdation(fieldMapping, existingFieldMapping);
		}
	}

	@Override
	public boolean hasProjectAccess(String projectToolConfigId) {
		Optional<ProjectBasicConfig> projectBasicConfig;
		ProjectToolConfig projectToolConfig = toolConfigRepository.findById(projectToolConfigId);
		if (null != projectToolConfig && null != projectToolConfig.getBasicProjectConfigId()) {
			projectBasicConfig = projectBasicConfigRepository.findById(projectToolConfig.getBasicProjectConfigId());

			Set<String> configIds = tokenAuthenticationService.getUserProjects();
			return projectBasicConfig.isPresent() && configIds.contains(projectBasicConfig.get().getId().toString());

		}
		return false;
	}

	@Override
	public void deleteByBasicProjectConfigId(ObjectId basicProjectConfigId) {
		log.info("deleting field mapping for {}" + basicProjectConfigId.toHexString());
		fieldMappingRepository.deleteByBasicProjectConfigId(basicProjectConfigId);
	}

	@Override
	public ProjectBasicConfig getBasicProjectConfigById(ObjectId basicProjectConfigId) {
		Optional<ProjectBasicConfig> projectBasicConfig = Optional.empty();
		ProjectBasicConfig projectBasicConfigObj = null;
		if (null != basicProjectConfigId) {
			projectBasicConfig = projectBasicConfigRepository.findById(basicProjectConfigId);
		}
		if (projectBasicConfig.isPresent()) {
			projectBasicConfigObj = projectBasicConfig.get();
		}
		return projectBasicConfigObj;
	}

	/*
	 * for all the fields present in the FieldMapping Enum, get its name, the
	 * original value and its history upto 5 limit in reverse order i.e., latest
	 * first from fieldmapping table
	 */
	@Override
	public List<FieldMappingResponse> getKpiSpecificFieldsAndHistory(KPICode kpi, String projectToolConfigId)
			throws NoSuchFieldException, IllegalAccessException {
		FieldMappingEnum fieldMappingEnum = FieldMappingEnum.valueOf(kpi.getKpiId().toUpperCase());
		List<String> fields = fieldMappingEnum.getFields();
		FieldMapping fieldMapping = getFieldMapping(projectToolConfigId);
		List<FieldMappingResponse> fieldMappingResponses = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(fields) && fieldMapping != null) {
			Class<FieldMapping> fieldMappingClass = FieldMapping.class;
			for (String field : fields) {
				FieldMappingResponse mappingResponse = new FieldMappingResponse();
				Object value = getFieldMappingField(fieldMapping, fieldMappingClass, field);
				mappingResponse.setFieldName(field);
				mappingResponse.setOriginalValue(value);
				List<ConfigurationHistoryChangeLog> changeLogs = getAccessibleFieldHistory(fieldMapping, field);
				if (CollectionUtils.isNotEmpty(changeLogs)) {
					mappingResponse.setHistory(changeLogs.stream()
							.sorted(Comparator.comparing(ConfigurationHistoryChangeLog::getUpdatedOn).reversed())
							.limit(5).toList());
				}
				fieldMappingResponses.add(mappingResponse);
			}
		}
		return fieldMappingResponses;
	}

	/**
	 * for each changed field present in the fieldMappingResponseList-- the normal
	 * value and its history implementation has to be updated by bulkupdate
	 * operation
	 * 
	 * if there is diversion from the default configuration field then the prompt
	 * should appear only once
	 * 
	 * if some processor level field is changed, then only the tracelog to be
	 * deleted if the field matches the azure level field the tool repo is to be
	 * updated
	 * 
	 * @param kpi
	 *            kpiCode
	 * @param projectToolConfig
	 *            projectToolConfig
	 * @param originalFieldMappingResponseList
	 *            originalFieldMappingResponseList
	 */
	@Override
	public void updateSpecificFieldsAndHistory(KPICode kpi, ProjectToolConfig projectToolConfig,
			List<FieldMappingResponse> originalFieldMappingResponseList)
			throws NoSuchFieldException, IllegalAccessException {
		List<FieldMappingStructure> fieldMappingStructureList = (List<FieldMappingStructure>) configHelperService
				.loadFieldMappingStructure();
		if (projectToolConfig != null && CollectionUtils.isNotEmpty(originalFieldMappingResponseList)
				&& CollectionUtils.isNotEmpty(fieldMappingStructureList)) {
			FieldMappingEnum fieldMappingEnum = FieldMappingEnum.valueOf(kpi.getKpiId().toUpperCase());
			List<String> fieldList = fieldMappingEnum.getFields();
			List<FieldMappingStructure> fieldMappingStructure = kPIHelperService
					.getFieldMappingStructure(fieldMappingStructureList, fieldList);
			Map<String, FieldMappingStructure> fieldMappingStructureMap = fieldMappingStructure.stream()
					.collect(Collectors.toMap(FieldMappingStructure::getFieldName, Function.identity()));

			ObjectId projectToolConfigId = projectToolConfig.getId();
			ProjectBasicConfig projectBasicConfig = ((Map<String, ProjectBasicConfig>) cacheService
					.cacheProjectConfigMapData()).get(projectToolConfig.getBasicProjectConfigId().toString());

			Query query = new Query(Criteria.where("projectToolConfigId").is(projectToolConfigId));
			Update update = new Update();
			final String loggedInUser = authenticationService.getLoggedInUser();

			// Map to store fieldName and corresponding object
			Map<String, FieldMappingResponse> responseHashMap = new HashMap<>();
			// Iterate over responses
			removeDuplicateFieldsFromResponse(originalFieldMappingResponseList, responseHashMap);
			List<FieldMappingResponse> fieldMappingResponseList = new ArrayList<>(responseHashMap.values());
			boolean cleanTraceLog = false;
			for (FieldMappingResponse fieldMappingResponse : fieldMappingResponseList) {
				update.set(fieldMappingResponse.getFieldName(), fieldMappingResponse.getOriginalValue());
				FieldMappingStructure mappingStructure = fieldMappingStructureMap
						.get(fieldMappingResponse.getFieldName());
				if (null != mappingStructure) {
					// for nested fields
					FieldMapping fieldMapping = configHelperService.getFieldMappingMap()
							.get(projectToolConfig.getBasicProjectConfigId());
					generateHistoryForNestedFields(fieldMappingResponseList, fieldMappingResponse, mappingStructure,
							fieldMapping);
					// for additionalfilters
					setMappingResponseWithGeneratedField(fieldMappingResponse, fieldMapping);

					if (!cleanTraceLog) {
						cleanTraceLog = mappingStructure.isProcessorCommon();
					}
				}
				updateFields(fieldMappingResponse.getFieldName(), projectToolConfig, projectBasicConfig);
				ConfigurationHistoryChangeLog configurationHistoryChangeLog = new ConfigurationHistoryChangeLog();
				configurationHistoryChangeLog.setChangedTo(fieldMappingResponse.getOriginalValue());
				configurationHistoryChangeLog.setChangedFrom(fieldMappingResponse.getPreviousValue());
				configurationHistoryChangeLog.setChangedBy(loggedInUser);
				configurationHistoryChangeLog.setUpdatedOn(LocalDateTime.now().toString());
				update.addToSet(HISTORY + fieldMappingResponse.getFieldName(), configurationHistoryChangeLog);
			}
			operations.updateFirst(query, update, "field_mapping");
			saveTemplateCode(projectBasicConfig, projectToolConfig);
			if (cleanTraceLog)
				removeTraceLog(projectBasicConfig.getId());
			cacheService.clearCache(CommonConstant.CACHE_FIELD_MAPPING_MAP);
			clearCache();
		}
	}

	private void removeDuplicateFieldsFromResponse(List<FieldMappingResponse> originalFieldMappingResponseList, Map<String, FieldMappingResponse> responseHashMap) {
		for (FieldMappingResponse response : originalFieldMappingResponseList) {
			responseHashMap.computeIfPresent(response.getFieldName(),
					(k, existingResponse) -> (existingResponse.getPreviousValue() == null
							&& response.getPreviousValue() != null) ? response : existingResponse);
			responseHashMap.putIfAbsent(response.getFieldName(), response);
		}
	}

	/**
	 * the history of all the nested fields should appear on the first identifier.
	 * 
	 * @param fieldMappingResponseList
	 *            fieldMappingResponseList
	 * @param fieldMappingResponse
	 *            fieldMappingResponse
	 * @param mappingStructure
	 *            mappingStructure
	 * @param fieldMapping
	 *            fieldMapping
	 * @throws NoSuchFieldException
	 *             NoSuchFieldException
	 * @throws IllegalAccessException
	 *             IllegalAccessException
	 */
	private void generateHistoryForNestedFields(List<FieldMappingResponse> fieldMappingResponseList,
			FieldMappingResponse fieldMappingResponse, FieldMappingStructure mappingStructure,
			FieldMapping fieldMapping) throws NoSuchFieldException, IllegalAccessException {
		if (CollectionUtils.isNotEmpty(mappingStructure.getNestedFields())) {

			StringBuilder originalValue = new StringBuilder(fieldMappingResponse.getOriginalValue() + "-");
			// check the fields in nestedfield section of fieldmapping structure and find if
			// those fields are present in the fieldmapping response
			for (BaseFieldMappingStructure nestedField : mappingStructure.getNestedFields()) {
				Optional<FieldMappingResponse> mappingResponse = fieldMappingResponseList.stream()
						.filter(response -> response.getFieldName().equalsIgnoreCase(nestedField.getFieldName())
								&& nestedField.getFilterGroup()
										.contains(fieldMappingResponse.getOriginalValue().toString()))
						.findFirst();
				mappingResponse.ifPresent(response -> originalValue.append(response.getOriginalValue()).append(":"));
			}
			setFieldMappingResponse(fieldMappingResponse, fieldMapping, originalValue);
		}

	}

	private Object getNestedField(FieldMapping newMapping, Class<FieldMapping> fieldMappingClass, Object newValue,
			FieldMappingStructure mappingStructure) throws NoSuchFieldException, IllegalAccessException {
		if (null != mappingStructure && CollectionUtils.isNotEmpty(mappingStructure.getNestedFields())) {
			StringBuilder originalValue = new StringBuilder(newValue + "-");
			// for nested fields
			for (BaseFieldMappingStructure nestedField : mappingStructure.getNestedFields()) {
				if (nestedField.getFilterGroup().contains(newValue)) {
					Object fieldMappingField = getFieldMappingField(newMapping, fieldMappingClass,
							nestedField.getFieldName());
					if (fieldMappingField != null) {
						originalValue.append(fieldMappingField).append(":");
					}
				}
			}
			return originalValue.deleteCharAt(originalValue.length() - 1).toString();
		}
		return newValue;
	}

	private void setFieldMappingResponse(FieldMappingResponse fieldMappingResponse, FieldMapping fieldMapping,
			StringBuilder originalValue) throws NoSuchFieldException, IllegalAccessException {
		List<ConfigurationHistoryChangeLog> changeLogs = getAccessibleFieldHistory(fieldMapping,
				fieldMappingResponse.getFieldName());
		String previousValue = "";
		if (CollectionUtils.isNotEmpty(changeLogs)) {
			ConfigurationHistoryChangeLog configurationHistoryChangeLog = changeLogs.get(changeLogs.size() - 1);
			previousValue = String.valueOf(configurationHistoryChangeLog.getChangedTo());

		}
		if(ObjectUtils.isNotEmpty(originalValue)) {
			fieldMappingResponse.setOriginalValue(originalValue.deleteCharAt(originalValue.length() - 1).toString());
		}
		fieldMappingResponse.setPreviousValue(previousValue);
	}

	private Object generateAdditionalFilters(Object newValue, String fieldName) {
		if (fieldName.equalsIgnoreCase("additionalFilterConfig")) {
			List<LinkedHashMap<String,Object>> additonalValue = (List<LinkedHashMap<String, Object>>) newValue;
			StringBuilder originalValue = new StringBuilder();
			for (Map<String, Object> value : additonalValue) {

				String identificationButton = (String) value.get("identifyFrom");
				String identificationValue;
				if (identificationButton.equalsIgnoreCase("customfield")) {
					identificationValue = (String) value.get("identificationField");
				} else {
					identificationValue = value.get("values").toString();
				}
				originalValue.append(value.get("filterId")).append("-").append(identificationButton).append(":").append(identificationValue).append(" ,");
			}
			return originalValue.toString();
		}
		return null;
	}

	private void setMappingResponseWithGeneratedField(FieldMappingResponse fieldMappingResponse,
			FieldMapping fieldMapping) throws NoSuchFieldException, IllegalAccessException {
		Object additonalFilter = generateAdditionalFilters(fieldMappingResponse.getOriginalValue(),
				fieldMappingResponse.getFieldName());
		if (additonalFilter != null) {
			setFieldMappingResponse(fieldMappingResponse, fieldMapping, new StringBuilder((String) additonalFilter));
		}
	}

	/**
	 * 
	 * @param fieldMapping
	 *            fieldMapping
	 * @param fieldName
	 *            fieldName
	 * @return list of history logs
	 * @throws NoSuchFieldException
	 *             no field exception
	 * @throws IllegalAccessException
	 *             accessibility
	 */
	private List<ConfigurationHistoryChangeLog> getAccessibleFieldHistory(FieldMapping fieldMapping, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		return (List<ConfigurationHistoryChangeLog>) getFieldMappingField(fieldMapping,
				FieldMapping.class.getSuperclass(), HISTORY + fieldName);
	}

	/**
	 * if jiraIterationCompletionStatusCustomField field mapping changes then put
	 * identifier to change in sprint report issues based on status for azure board
	 * 
	 * @param fieldName
	 *            fieldName
	 * @param projectToolConfig
	 *            projectToolConfig
	 * @param projectBasicConfig
	 *            projectBasicConfig
	 */
	private void updateFields(String fieldName, ProjectToolConfig projectToolConfig,
			ProjectBasicConfig projectBasicConfig) {
		if (fieldName.equalsIgnoreCase("jiraIterationCompletionStatusCustomField")
				&& projectToolConfig.getToolName().equals(ProcessorConstants.AZURE)
				&& !projectBasicConfig.getIsKanban()) {
			projectToolConfig.setAzureIterationStatusFieldUpdate(true);
			toolConfigRepository.save(projectToolConfig);
		}
	}

	private Object getFieldMappingField(FieldMapping fieldMapping, Class<?> fieldMapping1, String field)
			throws NoSuchFieldException, IllegalAccessException {
		Field declaredField = fieldMapping1.getDeclaredField(field);
		setAccessible(declaredField);
		return declaredField.get(fieldMapping);
	}

	private void clearCache() {
		cacheService.clearCache(CommonConstant.JIRAKANBAN_KPI_CACHE);
		cacheService.clearCache(CommonConstant.JIRA_KPI_CACHE);
		cacheService.clearCache(CommonConstant.BITBUCKET_KPI_CACHE);
		cacheService.clearCache(CommonConstant.SONAR_KPI_CACHE);
		cacheService.clearCache(CommonConstant.TESTING_KPI_CACHE);
		cacheService.clearCache(CommonConstant.JENKINS_KPI_CACHE);
	}

	/**
	 * @param unsaved
	 *            new field mapping
	 * @param saved
	 *            fieldmapping already in db
	 * @param fieldNameList
	 *            input fieldNameList
	 * @return updated or not
	 */
	private boolean checkFieldsForUpdation(FieldMapping unsaved, FieldMapping saved, List<String> fieldNameList) {
		boolean isUpdated = false;
		if (CollectionUtils.isNotEmpty(fieldNameList)) {
			for (String fName : fieldNameList) {
				try {
					Field field = FieldMapping.class.getDeclaredField(fName);
					setAccessible(field);
					isUpdated = isValueUpdated(field.get(unsaved), field.get(saved));

				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					isUpdated = false;
				}

				if (isUpdated) {
					break;
				}
			}

		} else {
			checkAllFieldsForUpdation(unsaved, saved);
			isUpdated = true;
		}
		return isUpdated;
	}

	/**
	 * checking each Field which are changed, and set the History of each
	 * fieldmapping
	 * 
	 * @param newMapping
	 *            unsaved FieldMapping
	 * @param oldMapping
	 *            saved FieldMapping
	 */
	private void checkAllFieldsForUpdation(FieldMapping newMapping, FieldMapping oldMapping) {
		Class<FieldMapping> fieldMappingClass = FieldMapping.class;
		Field[] fields = fieldMappingClass.getDeclaredFields();
		Class<? super FieldMapping> historyClass = fieldMappingClass.getSuperclass();
		Map<String, FieldMappingStructure> fieldMappingStructureMap = ((List<FieldMappingStructure>) configHelperService
				.loadFieldMappingStructure()).stream()
						.collect(Collectors.toMap(FieldMappingStructure::getFieldName, Function.identity()));

		for (Field field : fields) {
			setAccessible(field);
			try {
				Object oldValue = field.get(oldMapping);
				Object newValue = field.get(newMapping);
				String fieldName = field.getName();
				String setterName = "setHistory" + fieldName;
				FieldMappingStructure mappingStructure = fieldMappingStructureMap.get(fieldName);
				if(mappingStructure!=null) {
					List<ConfigurationHistoryChangeLog> changeLogs = getAccessibleFieldHistory(oldMapping, field.getName());
					Method setter = historyClass.getMethod(setterName, List.class);
					if (isValueUpdated(oldValue, newValue)) {
						newValue = getNestedField(newMapping, fieldMappingClass, newValue, mappingStructure);
						newValue = generateAdditionalFilters(newValue, fieldName);
						String loggedInUser = authenticationService.getLoggedInUser();
						String localDateTime = LocalDateTime.now().toString();
						if (CollectionUtils.isNotEmpty(changeLogs)) {
							// if change log is already present then we will be adding the new log
							changeLogs.add(
									new ConfigurationHistoryChangeLog(changeLogs.get(changeLogs.size() - 1).getChangedTo(),
											newValue, loggedInUser, localDateTime));
						} else {
							// if change log is absent then we will be creating the new log
							changeLogs = new ArrayList<>();
							changeLogs.add(new ConfigurationHistoryChangeLog("", newValue, loggedInUser, localDateTime));
						}
						setter.invoke(newMapping, changeLogs);
					}
				}
			} catch (IllegalAccessException | NoSuchFieldException | InvocationTargetException
					| NoSuchMethodException e) {
				log.debug("No Such Method Found" + e);
			}
		}
	}

	private void setAccessible(Field field) {
		field.setAccessible(true); // NOSONAR
	}

	/**
	 * compares field values for saved and unsaved data.
	 *
	 * @param value
	 *            unsaved value
	 * @param value1
	 *            existing value
	 * @return is value updated
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean isValueUpdated(Object value, Object value1) {
		if (ObjectUtils.isEmpty(value) && ObjectUtils.isEmpty(value1)) {
			return false;

		} else {
			if (value instanceof List) {
				return !(value1 instanceof List && (((List) value).size() == ((List) value1).size())
						&& ((List) value).containsAll((List) value1));

			} else if (value instanceof String[]) {
				return !(value1 instanceof String[] && Arrays.equals((String[]) value, (String[]) value1));

			} else {
				if (value1 != null) {
					return !value1.equals(value);
				} else {
					return true;
				}

			}
		}
	}

	private void saveTemplateCode(ProjectBasicConfig projectBasicConfig, ProjectToolConfig projectToolConfig) {
		if (projectToolConfig.getToolName().equalsIgnoreCase(ProcessorConstants.JIRA)|| projectToolConfig.getToolName().equalsIgnoreCase(ProcessorConstants.AZURE)) {
			if(projectToolConfig.getMetadataTemplateCode()!=null) {
				if (projectBasicConfig.getIsKanban() && !projectToolConfig.getMetadataTemplateCode()
						.equalsIgnoreCase(CommonConstant.CUSTOM_TEMPLATE_CODE_KANBAN)) {
					projectToolConfig.setMetadataTemplateCode(CommonConstant.CUSTOM_TEMPLATE_CODE_KANBAN);
					toolConfigRepository.save(projectToolConfig);
					cacheService.clearCache(CommonConstant.CACHE_PROJECT_TOOL_CONFIG);
				} else if (!projectBasicConfig.getIsKanban() && !projectToolConfig.getMetadataTemplateCode()
						.equalsIgnoreCase(CommonConstant.CUSTOM_TEMPLATE_CODE_SCRUM)) {
					projectToolConfig.setMetadataTemplateCode(CommonConstant.CUSTOM_TEMPLATE_CODE_SCRUM);
					toolConfigRepository.save(projectToolConfig);
					cacheService.clearCache(CommonConstant.CACHE_PROJECT_TOOL_CONFIG);
				}
			} else {
				projectToolConfig.setMetadataTemplateCode(CommonConstant.CUSTOM_TEMPLATE_CODE_SCRUM);
				toolConfigRepository.save(projectToolConfig);
				cacheService.clearCache(CommonConstant.CACHE_PROJECT_TOOL_CONFIG);
			}

		}

	}

	/**
	 * remove trace log, when u directly import some fieldmapping or when some
	 * processor common field is get update
	 * 
	 * @param basicProjectConfigId
	 *            project Basic Config id
	 */
	private void removeTraceLog(ObjectId basicProjectConfigId) {
		List<ProcessorExecutionTraceLog> traceLogs = processorExecutionTraceLogRepository
				.findByProcessorNameAndBasicProjectConfigIdIn(ProcessorConstants.JIRA,
						Collections.singletonList(basicProjectConfigId.toHexString()));

		if (!traceLogs.isEmpty()) {
			for (ProcessorExecutionTraceLog traceLog : traceLogs) {
				if (traceLog != null) {
					traceLog.setLastSuccessfulRun(null);
					traceLog.setLastSavedEntryUpdatedDateByType(new HashMap<>());
				}
			}
			processorExecutionTraceLogRepository.saveAll(traceLogs);
		}
	}

	@Override
	public boolean convertToFieldMappingAndCheckIsFieldPresent(List<FieldMappingResponse> fieldMappingResponseList, FieldMapping fieldMapping) throws IllegalAccessException {
		// this function checks if Fields are Present in FieldMapping or not and converts fieldMappingResponseList to FieldMapping.
		boolean allfieldFound=false;
		for (FieldMappingResponse response : fieldMappingResponseList) {
			String fieldName = response.getFieldName();

			if (fieldName.contains(HISTORY) || fieldName.equalsIgnoreCase("id")){
				continue;
			}
			if (isFieldPresent(fieldMapping.getClass(), fieldName)) {
				Object originalValue = response.getOriginalValue();
				setFieldValue(fieldMapping, fieldName, originalValue);
			} else if (!allfieldFound) {
				allfieldFound=true;
			}
		}

		return allfieldFound;
	}

	private boolean isFieldPresent(Class<?> clazz, String fieldName) {
		try {
			clazz.getDeclaredField(fieldName);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private void setFieldValue(FieldMapping object, String fieldName, Object value) throws IllegalAccessException {
		try {
			Field field = FieldMapping.class.getDeclaredField(fieldName);
			setAccessible(field);
			Object v=convertToSameType(field,value);
			field.set(object, v);
		} catch (NoSuchFieldException e) {
			log.warn("Field not found");
		}
	}

	private Object convertToSameType(Field field, Object value1) {
		Class<?> fieldType = field.getType();
		if (fieldType.isArray() && fieldType.getComponentType() == String.class) {
			List<?> list = (List<?>) value1;
			return list.toArray(new String[0]);
		} else if (fieldType.getName().equalsIgnoreCase(OBJECT_ID)) {
			return new ObjectId((String) value1);
		} else if (fieldType.getName().equalsIgnoreCase(DOUBLE)) {
			return ((Integer) value1).doubleValue();
		} else {
			// Unsupported type, return null
			return value1;
		}
	}
}
