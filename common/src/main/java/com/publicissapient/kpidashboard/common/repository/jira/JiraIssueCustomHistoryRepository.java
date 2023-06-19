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

package com.publicissapient.kpidashboard.common.repository.jira;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.publicissapient.kpidashboard.common.model.jira.JiraIssueCustomHistory;

/**
 * Repository for FeatureCollector.
 */
@Repository
public interface JiraIssueCustomHistoryRepository extends CrudRepository<JiraIssueCustomHistory, String>,
		QuerydslPredicateExecutor<JiraIssueCustomHistory>, JiraIssueHistoryCustomQueryRepository {

	/**
	 * Find by story id list.
	 *
	 * @param storyID
	 *            the story id
	 * @param basicProjectConfigId
	 *            basicProjectConfigId
	 * @return the list
	 */
	List<JiraIssueCustomHistory> findByStoryIDAndBasicProjectConfigId(String storyID, String basicProjectConfigId);

	/**
	 * Find by story id in list.
	 *
	 * @param storyList
	 *            the story list
	 * @return the list
	 */
	@Query(value = "{ 'storyID' : { $in: ?0 } }", fields = "{ 'storyID' : 1, 'storySprintDetails' : 1}")
	List<JiraIssueCustomHistory> findByStoryIDIn(List<String> storyList);

	/**
	 * Deletes all documents that matches with given projectID.
	 * 
	 * @param projectID
	 *            String projectID
	 */
	void deleteByBasicProjectConfigId(String projectID);

	/**
	 * Find by story id list.
	 *
	 * @param storyID
	 *            the story id
	 * @param basicProjectConfigId
	 *            basicProjectConfigId
	 * @return the list
	 */
	List<JiraIssueCustomHistory> findByStoryIDInAndBasicProjectConfigIdIn(List<String> storyID,
			List<String> basicProjectConfigId);

}