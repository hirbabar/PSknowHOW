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
package com.publicissapient.kpidashboard.apis.mongock.ddl;

import com.publicissapient.kpidashboard.apis.data.*;
import com.publicissapient.kpidashboard.apis.util.MongockUtil;
import com.publicissapient.kpidashboard.common.model.application.*;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;

@Slf4j
@ChangeUnit(id = "ddl5", order = "005", author = "hargupta15", runAlways = true)
public class KpiDefinition {
	private final MongoTemplate mongoTemplate;
	private final String KPI_MASTER_COLLECTION = "kpi_master";
	private final String KPI_CATEGORY_COLLECTION = "kpi_category";

	private final String KPI_CATEGORY_MAPPING_COLLECTION = "kpi_category_mapping";

	private final String KPI_COLUMN_CONFIGS_COLLECTION = "kpi_column_configs";

	private final String FIELD_MAPPING_STRUCTURE_COLLECTION = "field_mapping_structure";

	List<KpiMaster> kpiList;
	List<KpiColumnConfig> kpiColumnConfigs;
	List<KpiCategoryMapping> kpiCategoryMappingList;
	List<KpiCategory> kpiCategoryList;
	List<BaseFieldMappingStructure> fieldMappingStructureList;

	public KpiDefinition(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;

		KpiDefinationDataFactory kpiDefinationDataFactory = KpiDefinationDataFactory.newInstance();
		KpiColumnConfigDataFactory kpiColumnConfigDataFactory = KpiColumnConfigDataFactory.newInstance();
		KpiCategoryMappingDataFactory kpiCategoryMappingDataFactory = KpiCategoryMappingDataFactory.newInstance();
		KpiCategoryDataFactory kpiCategoryDataFactory = KpiCategoryDataFactory.newInstance();
		FieldMappingStructureDataFactory fieldMappingStructureDataFactory = FieldMappingStructureDataFactory
				.newInstance();

		kpiList = kpiDefinationDataFactory.getKpiList();
		kpiColumnConfigs = kpiColumnConfigDataFactory.getKpiColumnConfigs();
		kpiCategoryMappingList = kpiCategoryMappingDataFactory.getKpiCategoryMappingList();
		kpiCategoryList = kpiCategoryDataFactory.getKpiCategoryList();
		fieldMappingStructureList = fieldMappingStructureDataFactory.getFieldMappingStructureList();
	}

	@Execution
	public void changeSet() {
		MongockUtil.saveListToDB(kpiList, KPI_MASTER_COLLECTION, mongoTemplate);
		MongockUtil.saveListToDB(kpiColumnConfigs, KPI_COLUMN_CONFIGS_COLLECTION, mongoTemplate);
		MongockUtil.saveListToDB(kpiCategoryMappingList, KPI_CATEGORY_MAPPING_COLLECTION, mongoTemplate);
		MongockUtil.saveListToDB(kpiCategoryList, KPI_CATEGORY_COLLECTION, mongoTemplate);
		MongockUtil.saveListToDB(fieldMappingStructureList, FIELD_MAPPING_STRUCTURE_COLLECTION, mongoTemplate);
	}

	@RollbackExecution
	public void rollback() {

	}
}
