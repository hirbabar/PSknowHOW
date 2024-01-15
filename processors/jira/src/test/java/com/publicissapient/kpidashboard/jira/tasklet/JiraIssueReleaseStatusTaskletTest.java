package com.publicissapient.kpidashboard.jira.tasklet;

import com.publicissapient.kpidashboard.jira.client.JiraClient;
import com.publicissapient.kpidashboard.jira.client.ProcessorJiraRestClient;
import com.publicissapient.kpidashboard.jira.config.FetchProjectConfiguration;
import com.publicissapient.kpidashboard.jira.config.JiraProcessorConfig;
import com.publicissapient.kpidashboard.jira.model.ProjectConfFieldMapping;
import com.publicissapient.kpidashboard.jira.service.CreateJiraIssueReleaseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueReleaseStatusTaskletTest {

    @Mock
    private FetchProjectConfiguration fetchProjectConfiguration;

    @Mock
    private JiraClient jiraClient;

    @Mock
    private CreateJiraIssueReleaseStatus createJiraIssueReleaseStatus;

    @Mock
    private JiraProcessorConfig jiraProcessorConfig;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @InjectMocks
    private JiraIssueReleaseStatusTasklet jiraIssueReleaseStatusTasklet;

    @Before
    public void setUp() {
        // Mock any setup or common behavior needed before each test
    }

    @Test
    public void testExecute() throws Exception {
        // Arrange
        String projectId = "5fd99f7bc8b51a7b55aec836";
        ProjectConfFieldMapping projectConfFieldMapping= ProjectConfFieldMapping.builder().projectName("KnowHow").build();

        when(fetchProjectConfiguration.fetchConfiguration(null)).thenReturn(projectConfFieldMapping);

        ProcessorJiraRestClient client = mock(ProcessorJiraRestClient.class);
            when(jiraClient.getClient(projectConfFieldMapping, null)).thenReturn(client);

            // Act
            RepeatStatus result = jiraIssueReleaseStatusTasklet.execute(stepContribution, chunkContext);

            // Assert
            verify(createJiraIssueReleaseStatus, times(1)).processAndSaveProjectStatusCategory(client, null);
            assertEquals(RepeatStatus.FINISHED, result);
        }

}