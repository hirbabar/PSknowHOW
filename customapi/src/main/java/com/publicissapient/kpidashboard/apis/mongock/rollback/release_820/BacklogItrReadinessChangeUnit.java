/*
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.publicissapient.kpidashboard.apis.mongock.rollback.release_820;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.publicissapient.kpidashboard.apis.util.MongockUtil;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * @author shunaray
 */
@SuppressWarnings("java:S1192")
@ChangeUnit(id = "r_backlog_itr_ready_Ehc", order = "08204", author = "shunaray", systemVersion = "8.2.0")
public class BacklogItrReadinessChangeUnit {
	public static final String FIELD_MAPPING_STRUCTURE = "field_mapping_structure";

	private final MongoTemplate mongoTemplate;

	public BacklogItrReadinessChangeUnit(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Execution
	public void execution() {
		deleteFieldMapping();
		rollbackKpiFilterToRadioBtn();
	}

	public void deleteFieldMapping() {
		List<String> fieldNamesToDelete = Arrays.asList("jiraStatusForInProgressKPI161", "jiraStatusForRefinedKPI161",
				"jiraStatusForNotRefinedKPI161");
		Document filter = new Document("fieldName", new Document("$in", fieldNamesToDelete));
		mongoTemplate.getCollection(FIELD_MAPPING_STRUCTURE).deleteMany(filter);
	}

	public void rollbackKpiFilterToRadioBtn() {
		mongoTemplate.getCollection("kpi_master").updateOne(
				new Document("kpiId", new Document("$in", Collections.singletonList("kpi161"))),
				new Document("$unset", new Document("kpiFilter", "")));
	}

	@RollbackExecution
	public void rollback() {
		insertFieldMappings();
		makeKpiFilterToRadioBtn();
	}

	public void insertFieldMappings() {
		List<Document> fieldMappings = Arrays.asList(
				MongockUtil.createFieldMapping("jiraStatusForInProgressKPI161", "Status to identify In Progress issues",
						"WorkFlow Status Mapping", "workflow", "chips",
						"All statuses that issues have moved from the Created status and also has not been completed."),
				MongockUtil.createFieldMapping("jiraStatusForRefinedKPI161", "Status to identify In Refined issues",
						"WorkFlow Status Mapping", "workflow", "chips",
						"All statuses that correspond to refined status of Iteration Readiness."),
				MongockUtil.createFieldMapping("jiraStatusForNotRefinedKPI161",
						"Status to identify In Not Refined issues", "WorkFlow Status Mapping", "workflow", "chips",
						"All statuses that correspond to not refined status of Iteration Readiness."));

		mongoTemplate.getCollection(FIELD_MAPPING_STRUCTURE).insertMany(fieldMappings);
	}

	public void makeKpiFilterToRadioBtn() {
		mongoTemplate.getCollection("kpi_master").updateOne(
				new Document("kpiId", new Document("$in", Collections.singletonList("kpi161"))),
				new Document("$set", new Document("kpiFilter", "radioButton")));
	}
}
