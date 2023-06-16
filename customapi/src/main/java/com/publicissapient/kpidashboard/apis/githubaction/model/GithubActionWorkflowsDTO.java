package com.publicissapient.kpidashboard.apis.githubaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GithubActionWorkflowsDTO {

	private String workflowName;
	private String workflowID;
}
