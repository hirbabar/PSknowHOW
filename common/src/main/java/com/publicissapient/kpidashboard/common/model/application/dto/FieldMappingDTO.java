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

package com.publicissapient.kpidashboard.common.model.application.dto;//NOPMD

import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;

import com.publicissapient.kpidashboard.common.constant.CommonConstant;
import com.publicissapient.kpidashboard.common.model.application.AdditionalFilterConfig;
import com.publicissapient.kpidashboard.common.model.generic.BasicModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The type Field mapping. Represents Jira field mapping values
 */
@SuppressWarnings("PMD.TooManyFields")
@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FieldMappingDTO extends BasicModel {

	private ObjectId projectToolConfigId;
	private ObjectId basicProjectConfigId;
	private String projectId;
	private String atmQueryEndpoint;
	private String sprintName;
	private String epicName;
	private List<String> jiradefecttype;

	// defectPriority
	private List<String> defectPriority;
	private List<String> defectPriorityKPI135;
	private List<String> defectPriorityKPI14;
	private List<String> defectPriorityQAKPI111;
	private List<String> defectPriorityKPI82;
	private List<String> defectPriorityKPI133;

	private String[] jiraIssueTypeNames;
	private String[] jiraIssueTypeNamesAVR;
	private String storyFirstStatus;
	private String storyFirstStatusLT;
	private String[] linkDefectToStoryField;
	private String rootCause;
	private List<String> jiraStatusForDevelopment;
	private List<String> jiraStatusForDevelopmentAVR;
	private List<String> jiraStatusForDevelopmentKPI82;
	private List<String> jiraStatusForDevelopmentKPI135;
	private String jiraAtmProjectId;
	private String jiraAtmProjectKey;
	private List<String> jiraIssueEpicType;

	private List<String> jiraStatusForQa;
	private List<String> jiraStatusForQaKPI135;
	private List<String> jiraStatusForQaKPI82;
	// type of test cases
	private List<String> jiraDefectInjectionIssueType;
	private List<String> jiraDefectInjectionIssueTypeKPI14;
	private List<String> jiraDod;
	private List<String> jiraDodKPI14;
	private List<String> jiraDodQAKPI111;
	private List<String> jiraDodLT;
	private List<String> jiraDodPDA;

	private String jiraDefectCreatedStatus;
	private String jiraDefectCreatedStatusKPI14;
	private List<String> jiraTechDebtIssueType;
	private String jiraTechDebtIdentification;
	private String jiraTechDebtCustomField;
	private List<String> jiraTechDebtValue;
	private String jiraDefectRejectionStatus;
	private String jiraDefectRejectionStatusAVR;
	private String jiraDefectRejectionStatusKPI28;
	private String jiraDefectRejectionStatusKPI34;
	private String jiraDefectRejectionStatusKPI37;
	private String jiraDefectRejectionStatusKPI35;
	private String jiraDefectRejectionStatusKPI82;
	private String jiraDefectRejectionStatusKPI135;
	private String jiraDefectRejectionStatusKPI133;
	private String jiraDefectRejectionStatusRCAKPI36;
	private String jiraDefectRejectionStatusKPI14;
	private String jiraDefectRejectionStatusQAKPI111;
	private String jiraBugRaisedByIdentification;
	private List<String> jiraBugRaisedByValue;

	private List<String> jiraDefectSeepageIssueType;
	private List<String> jiraIssueTypeKPI35;
	private String jiraBugRaisedByCustomField;
	private List<String> jiraDefectRemovalStatus;
	private List<String> jiraDefectRemovalStatusKPI34;
	private List<String> jiraDefectRemovalIssueType;
	private List<String> jiraDefectRemovalIssueTypeKPI34;
	// Added for Defect Reopen Rate KPI.
	private List<String> jiraDefectClosedStatus;

	private String jiraStoryPointsCustomField;
	// parent issue type for the test
	private List<String> jiraTestAutomationIssueType;
	// value of the automated test case Eg. Yes, Cannot Automate, No

	private List<String> jiraSprintVelocityIssueType;
	private List<String> jiraSprintVelocityIssueTypeBR;

private List<String> jiraSprintCapacityIssueType;
private List<String> jiraSprintCapacityIssueTypeKpi46;

	private List<String> jiraDefectRejectionlIssueType;
	private List<String> jiraIssueTypeKPI37;

	private List<String> jiraDefectCountlIssueType; // test done
	private List<String> jiraDefectCountlIssueTypeKPI28;
	private List<String> jiraDefectCountlIssueTypeKPI36;

	private List<String> jiraIssueDeliverdStatus;
	private List<String> jiraIssueDeliverdStatusBR;
	private List<String> jiraIssueDeliverdStatusAVR;
	private List<String> jiraIssueDeliverdStatusKPI126;
	private List<String> jiraIssueDeliverdStatusKPI82;
	private String readyForDevelopmentStatus;

	private String jiraDor;
	private String jiraDorLT;

private List<String> jiraIntakeToDorIssueType;
private List<String> jiraIssueTypeLT;

private List<String> jiraStoryIdentification;
private List<String> jiraStoryIdentificationKpi40;

private String jiraLiveStatus;
private String jiraLiveStatusLT;
private String jiraLiveStatusLTK;
private String jiraLiveStatusNOPK;
private String jiraLiveStatusNOSK;
private String jiraLiveStatusNORK;
private String jiraLiveStatusOTA;//openticketaging
private String jiraLiveStatusPDA;//productionissueaging

	private List<String> ticketCountIssueType;

	private List<String> kanbanRCACountIssueType;

	private List<String> jiraTicketVelocityIssueType;

	private List<String> ticketDeliverdStatus;

	private List<String> ticketReopenStatus;

	private List<String> kanbanJiraTechDebtIssueType;

	private List<String> jiraTicketResolvedStatus;
	private List<String> jiraTicketClosedStatus;
	private List<String> kanbanCycleTimeIssueType;
	private List<String> jiraTicketTriagedStatus;
	private List<String> jiraTicketWipStatus;
	private List<String> jiraTicketRejectedStatus;

	private String jiraStatusMappingCustomField;
	private List<String> excludeStatusKpi129;

private List<String> rootCauseValue;
private List<String> excludeRCAFromFTPR; // test done
private List<String> excludeRCAFromKPI82;
private List<String> excludeRCAFromKPI135;
private List<String> excludeRCAFromKPI14;
private List<String> excludeRCAFromQAKPI111;
private List<String> excludeRCAFromKPI133;

	private Boolean pickNewATMJIRADetails;

	private List<String> jiraDorToLiveIssueType;
	private List<String> jiraProductiveStatus;

	private List<String> jiraCommitmentReliabilityIssueType;

	private List<String> resolutionTypeForRejection;
	private List<String> resolutionTypeForRejectionAVR;
	private List<String> resolutionTypeForRejectionKPI28;
	private List<String> resolutionTypeForRejectionKPI34;
	private List<String> resolutionTypeForRejectionKPI37;
	private List<String> resolutionTypeForRejectionKPI35;
	private List<String> resolutionTypeForRejectionKPI82;
	private List<String> resolutionTypeForRejectionKPI135;
	private List<String> resolutionTypeForRejectionKPI133;
	private List<String> resolutionTypeForRejectionRCAKPI36;
	private List<String> resolutionTypeForRejectionKPI14;
	private List<String> resolutionTypeForRejectionQAKPI111;

	private List<String> jiraQADefectDensityIssueType;
	private List<String> jiraQAKPI111IssueType;

	private String jiraBugRaisedByQACustomField;
	private String jiraBugRaisedByQAIdentification;
	private List<String> jiraBugRaisedByQAValue;
	private List<String> jiraDefectDroppedStatus;

	// Epic custom Field mapping
	private String epicCostOfDelay;
	private String epicRiskReduction;
	private String epicUserBusinessValue;
	private String epicWsjf;
	private String epicTimeCriticality;
	private String epicJobSize;

	private String atmSubprojectField;

	// Squad Mapping
	private String squadIdentifier;
	private List<String> squadIdentMultiValue;
	private String squadIdentSingleValue;

	// Production Defect Mapping
	private String productionDefectCustomField;
	private String productionDefectIdentifier;
	private List<String> productionDefectValue;
	private String productionDefectComponentValue;

	// issue status to exclude missing worklogs
	private List<String> issueStatusToBeExcludedFromMissingWorklogs;

// field for In Progress status
private List<String> jiraStatusForInProgress;
private List<String> jiraStatusForInProgressKPI122;
private List<String> jiraStatusForInProgressKPI145;
private List<String> jiraStatusForInProgressKPI125;
private List<String> jiraStatusForInProgressKPI128;
private List<String> jiraStatusForInProgressKPI123;
private List<String> jiraStatusForInProgressKPI119;

	@Builder.Default
	private String estimationCriteria = "Story Point";

	@Builder.Default
	private Double storyPointToHourMapping = 8D;

	@Builder.Default
	private Double workingHoursDayCPT = 6D;

	// additional filter config fields
	private List<AdditionalFilterConfig> additionalFilterConfig;

// issue status to exclude missing worklogs
private List<String> issueStatusExcluMissingWork;
private List<String> issueStatusExcluMissingWorkKPI124;

	// issue On Hold status to exclude Closure possible
	private List<String> jiraOnHoldStatus;

// field for FTPR
private List<String> jiraFTPRStoryIdentification;
private List<String> jiraKPI82StoryIdentification;
private List<String> jiraKPI135StoryIdentification;

// field for Wasting - wait status
private List<String> jiraWaitStatus;
private List<String> jiraWaitStatusKPI131;

// field for Wasting - block status
private List<String> jiraBlockedStatus;
private List<String> jiraBlockedStatusKPI131;

// field for Wasting - Include Blocked Status
private String jiraIncludeBlockedStatus; 
private String jiraIncludeBlockedStatusKPI131;

// for for JiraDueDate
@Builder.Default
private String jiraDueDateField = CommonConstant.DUE_DATE;
private String jiraDueDateCustomField;
private String jiraDevDueDateCustomField;
private List<String> jiraDevDoneStatus;
private List<String> jiraDevDoneStatusKPI119;
private List<String> jiraDevDoneStatusKPI145;
private List<String> jiraDevDoneStatusKPI128;

	// For DTS_21154 - field for Team refinement status
	private List<String> jiraRejectedInRefinement;

	// For DTS_21154 - field for Stakeholder refinement status
	private List<String> jiraAcceptedInRefinement;

	// For DTS_21154 - field for Stakeholder refinement status
	private List<String> jiraReadyForRefinement;

	private List<String> jiraFtprRejectStatus;
	private List<String> jiraFtprRejectStatusKPI135;
	private List<String> jiraFtprRejectStatusKPI82;

private List<String> jiraIterationCompletionStatusCustomField;
private List<String> jiraIterationCompletionStatusKPI135;
private List<String> jiraIterationCompletionStatusKPI122;
private List<String> jiraIterationCompletionStatusKPI75;
private List<String> jiraIterationCompletionStatusKPI145;
private List<String> jiraIterationCompletionStatusKPI140;
private List<String> jiraIterationCompletionStatusKPI132;
private List<String> jiraIterationCompletionStatusKPI136;
private List<String> jiraIterationCompletionStatusKpi72;
private List<String> jiraIterationCompletionStatusKpi39;
private List<String> jiraIterationCompletionStatusKpi5;
private List<String> jiraIterationCompletionStatusKPI124;
private List<String> jiraIterationCompletionStatusKPI123;
private List<String> jiraIterationCompletionStatusKPI125;
private List<String> jiraIterationCompletionStatusKPI120;
private List<String> jiraIterationCompletionStatusKPI128;
private List<String> jiraIterationCompletionStatusKPI134;
private List<String> jiraIterationCompletionStatusKPI133;
private List<String> jiraIterationCompletionStatusKPI119;
private List<String> jiraIterationCompletionStatusKPI131;
private List<String> jiraIterationCompletionStatusBRE;

	private List<String> jiraIterationCompletionTypeCustomField;  
	private List<String> jiraIterationIssuetypeKPI122;
	private List<String> jiraIterationIssuetypeBRE;
	private List<String> jiraIterationIssuetypeKPI131;
	private List<String> jiraIterationIssuetypeKPI128;
	private List<String> jiraIterationIssuetypeKPI134;
	private List<String> jiraIterationIssuetypeKPI145;
	private List<String> jiraIterationIssuetypeKpi72;
	private List<String> jiraIterationIssuetypeKPI119;
	private List<String> jiraIterationIssuetypeKpi5;
	private List<String> jiraIterationIssuetypeKPI75;
	private List<String> jiraIterationIssuetypeKPI123;
	private List<String> jiraIterationIssuetypeKPI125;
	private List<String> jiraIterationIssuetypeKPI120;
	private List<String> jiraIterationIssuetypeKPI124;



	/**
	 * Get jira issue type names string [ ].
	 *
	 * @return the string [ ]
	 */
	public String[] getJiraIssueTypeNames() {
		return jiraIssueTypeNames == null ? null : jiraIssueTypeNames.clone();
	}

	/**
	 * Sets jira issue type names.
	 *
	 * @param jiraIssueTypeNames
	 *            the jira issue type names
	 */
	public void setJiraIssueTypeNames(String[] jiraIssueTypeNames) {
		this.jiraIssueTypeNames = jiraIssueTypeNames == null ? null : jiraIssueTypeNames.clone();
	}

	/**
	 * Get link defect to story field string [ ].
	 *
	 * @return the string [ ]
	 */
	public String[] getLinkDefectToStoryField() {
		return linkDefectToStoryField == null ? linkDefectToStoryField : linkDefectToStoryField.clone();
	}

	/**
	 * Sets link defect to story field.
	 *
	 * @param linkDefectToStoryField
	 *            the link defect to story field
	 */
	public void setLinkDefectToStoryField(String[] linkDefectToStoryField) {
		this.linkDefectToStoryField = linkDefectToStoryField == null ? null : linkDefectToStoryField.clone();
	}

}