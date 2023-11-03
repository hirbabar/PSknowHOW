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

package com.publicissapient.kpidashboard.apis.mongock.rollback.release_810;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * @author shunaray
 */
@ChangeUnit(id = "r_release_burnUp_changes", order = "08102", author = "shunaray", systemVersion = "8.1.0")
public class ReleaseBurnUpChangeUnit {

    private final MongoTemplate mongoTemplate;

    public ReleaseBurnUpChangeUnit(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void execution() {
        releaseBurnUpDefaultOrderRollback();
        releaseBurnUpFieldMappingRollback();
    }

    public void releaseBurnUpDefaultOrderUpdate() {
        Bson filter = Filters.in("kpiId", "kpi150");
        Bson update = Updates.set("defaultOrder", 1);
        mongoTemplate.getCollection("kpi_master").updateMany(filter, update);
    }

    public void releaseBurnUpFieldMappingInsert() {

        Document document = new Document("fieldName", "startDateCountKPI150")
                .append("fieldLabel",
                        "Count of days from the release start date to calculate closure rate for prediction")
                .append("fieldType", "number").append("section", "Issue Types Mapping")
                .append("tooltip", new Document("definition",
                        "If this field is kept blank, then daily closure rate of issues is calculated based on the number of working days between today and the release start date or date when the first issue was added. This configuration allows you to decide from which date the closure rate should be calculated."));

        mongoTemplate.insert(document, "field_mapping_structure");

    }

    public void releaseBurnUpDefaultOrderRollback() {

        Bson filter = Filters.in("kpiId", "kpi150");
        Bson rollbackUpdate = Updates.set("defaultOrder", 6);
        mongoTemplate.getCollection("kpi_master").updateMany(filter, rollbackUpdate);

    }

    public void releaseBurnUpFieldMappingRollback() {

        // Define a filter to remove the inserted document
        Bson filter = Filters.eq("fieldName", "startDateCountKPI150");
        // Remove the document from the collection
        mongoTemplate.getCollection("field_mapping_structure").deleteOne(filter);

    }

    @RollbackExecution
    public void rollback() {
        releaseBurnUpDefaultOrderUpdate();
        releaseBurnUpFieldMappingInsert();
    }

}
