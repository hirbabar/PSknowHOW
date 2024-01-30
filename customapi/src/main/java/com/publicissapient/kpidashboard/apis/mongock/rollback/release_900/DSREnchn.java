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

package com.publicissapient.kpidashboard.apis.mongock.rollback.release_900;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoCollection;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * @author shi6
 */
@ChangeUnit(id = "r_dsr_ehnc", order = "09001", author = "shi6", systemVersion = "9.0.0")
public class DSREnchn {

	public static final String FIELD_MAPPING_STRUCTURE = "field_mapping_structure";
	public static final String FIELD_NAME = "fieldName";
	public static final String LABEL = "label";
	public static final String VALUE = "value";

	private final MongoTemplate mongoTemplate;

	public DSREnchn(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Execution
	public void execution() {
		MongoCollection<Document> fieldMappingStruture = mongoTemplate.getCollection(FIELD_MAPPING_STRUCTURE);
		Document filter = new Document(FIELD_NAME, "jiraBugRaisedByIdentification");

		// Specify the rollback operation
		Document rollback = new Document("$unset", new Document("processorCommon", ""));

		// Perform the rollback
		fieldMappingStruture.updateOne(filter, rollback);

		List<String> fieldNamesToDelete = Arrays.asList("includeRCAForKPI35", "defectPriorityKPI135",
				"useUnLinkedDefect");
		// Delete documents that match the filter
		fieldMappingStruture.deleteMany(new Document(FIELD_NAME, new Document("$in", fieldNamesToDelete)));


		mongoTemplate.getCollection("kpi_master").updateOne(new Document("kpiId", "kpi35"),
				new Document("$set", new Document("kpiFilter" , "")));

	}

	@RollbackExecution
	public void rollback() {
		MongoCollection<Document> fieldMappingStructure = mongoTemplate.getCollection(FIELD_MAPPING_STRUCTURE);
		Document filter = new Document(FIELD_NAME, "jiraBugRaisedByIdentification");

		// Specify the update operation
		Document update = new Document("$set", new Document("processorCommon", true));
		fieldMappingStructure.updateOne(filter, update);

		List<Document> documents = Arrays.asList(
				new Document(FIELD_NAME, "includeRCAForKPI35").append("fieldLabel", "Root cause values to be included")
						.append("fieldType", "chips").append("section", "Defects Mapping")
						.append("tooltip", new Document("definition",
								"Root cause reasons for defects which are to be included in 'DSR' calculation")),
				new Document(FIELD_NAME, "defectPriorityKPI35").append("fieldLabel", "Priority to be excluded")
						.append("fieldType", "multiselect").append("section", "Defects Mapping")
						.append("tooltip",
								new Document("definition",
										"Priority values of defects which are to be excluded in 'DSR' calculation"))
						.append("options",
								Arrays.asList(new Document(LABEL, "p1").append(VALUE, "p1"),
										new Document(LABEL, "p2").append(VALUE, "p2"),
										new Document(LABEL, "p3").append(VALUE, "p3"),
										new Document(LABEL, "p4").append(VALUE, "p4"),
										new Document(LABEL, "p5").append(VALUE, "p5"))),

				new Document().append(FIELD_NAME, "excludeUnlinkedDefects").append("fieldLabel", "Exclude Unlinked Defects")
						.append("fieldType", "toggle").append("section", "WorkFlow Status Mapping")
						.append("processorCommon", false).append("tooltip",
								new Document("definition", "Disable Toggle to see calculations on unlinked defects too.")));

		fieldMappingStructure.insertMany(documents);
		fieldMappingStructure.updateOne(new Document(FIELD_NAME, "jiraBugRaisedByIdentification"),
				new Document("$set", new Document("fieldLabel", "Escaped defects identification (Processor Run)")));

		mongoTemplate.getCollection("kpi_master").updateOne(new Document("kpiId", "kpi35"),
				new Document("$set", new Document("kpiFilter" , "multiSelectDropDown")));

	}

}
