package com.publicissapient.kpidashboard.apis.mongock.ddl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeUnit(id = "DDL3", order = "003", author = "PSKnowHow")
public class GlobalConfigChangeLog {

	private final MongoTemplate mongoTemplate;
	private static final String CLASS_KEY = "_class";
	private static final String BUILD = "BUILD";

	public GlobalConfigChangeLog(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Execution
	public void executeGlobalConfig() {
		insertGlobalConfigData();
		insertProcessorData();
	}

	public void insertGlobalConfigData() {
		Document existingConfig = mongoTemplate.getCollection("global_config").find(new Document("env", "production"))
				.first();

		if (existingConfig == null) {
			Document globalConfig = new Document().append("env", "production")
					.append("authTypeStatus", new Document().append("standardLogin", true).append("adLogin", false))
					.append("emailServerDetail",
							new Document().append("emailHost", "mail.example.com").append("emailPort", 25)
									.append("fromEmail", "no-reply@example.com")
									.append("feedbackEmailIds", Collections.singletonList("sampleemail@example.com")))
					.append("zephyrCloudBaseUrl", "https://api.zephyrscale.smartbear.com/v2/");

			mongoTemplate.getCollection("global_config").insertOne(globalConfig);
		}
	}

	public void insertProcessorData() {
		if (mongoTemplate.getCollection("processor").countDocuments() == 0) {
			List<Document> processorData = Arrays.asList(
					createProcessor("Jira", "AGILE_TOOL", "com.publicissapient.kpidashboard.jira.model.JiraProcessor"),
					createSonarProcessor(),
					createProcessor("Zephyr", "TESTING_TOOLS",
							"com.publicissapient.kpidashboard.zephyr.model.ZephyrProcessor"),
					createProcessor("GitHub", "SCM", "com.publicissapient.kpidashboard.github.model.GitHubProcessor"),
					createProcessor("Teamcity", BUILD,
							"com.publicissapient.kpidashboard.teamcity.model.TeamcityProcessor"),
					createProcessor("Bitbucket", "SCM",
							"com.publicissapient.kpidashboard.bitbucket.model.BitbucketProcessor"),
					createProcessor("GitLab", "SCM", "com.publicissapient.kpidashboard.gitlab.model.GitLabProcessor"),
					createProcessor("Jenkins", BUILD,
							"com.publicissapient.kpidashboard.jenkins.model.JenkinsProcessor"),
					createProcessor("Bamboo", BUILD, "com.publicissapient.kpidashboard.bamboo.model.BambooProcessor"),
					createProcessor("Azure", "AGILE_TOOL",
							"com.publicissapient.kpidashboard.azure.model.AzureProcessor"),
					createProcessor("AzureRepository", "SCM",
							"com.publicissapient.kpidashboard.azurerepo.model.AzureRepoProcessor"),
					createProcessor("AzurePipeline", BUILD,
							"com.publicissapient.kpidashboard.azurepipeline.model.AzurePipelineProcessor"),
					createProcessor("JiraTest", "TESTING_TOOLS",
							"com.publicissapient.kpidashboard.jiratest.model.JiraTestProcessor"),
					createProcessor("GitHubAction", BUILD,
							"com.publicissapient.kpidashboard.githubaction.model.GitHubActionProcessor"),
					createProcessor("RepoTool", "SCM",
							"com.publicissapient.kpidashboard.repodb.model.RepoDbProcessor"));

			mongoTemplate.getCollection("processor").insertMany(processorData);
		}
	}

	private Document createProcessor(String processorName, String processorType, String className) {
		return new Document().append("processorName", processorName).append("processorType", processorType)
				.append("isActive", true).append("isOnline", true).append("errors", Collections.emptyList())
				.append("isLastSuccess", false).append(CLASS_KEY, className);
	}

	private Document createSonarProcessor() {
		return new Document().append("processorName", "Sonar").append("processorType", "SONAR_ANALYSIS")
				.append("isActive", true).append("isOnline", true).append("errors", Collections.emptyList())
				.append("isLastSuccess", false)
				.append(CLASS_KEY, "com.publicissapient.kpidashboard.sonar.model.SonarProcessor")
				.append("sonarKpiMetrics", createSonarKpiMetrics());
	}

	private List<String> createSonarKpiMetrics() {
		return Arrays.asList("lines", "ncloc", "violations", "new_vulnerabilities", "critical_violations",
				"major_violations", "blocker_violations", "minor_violations", "info_violations", "tests",
				"test_success_density", "test_errors", "test_failures", "coverage", "line_coverage", "sqale_index",
				"alert_status", "quality_gate_details", "sqale_rating");
	}

	@RollbackExecution
	public void rollback() {
		// We are inserting the documents through DDL, no rollback to any collections.
	}

}
