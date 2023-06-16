package com.publicissapient.kpidashboard.common.model.comments;

import com.publicissapient.kpidashboard.common.model.generic.BasicModel;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "kpi_comments")
public class KPIComments extends BasicModel {

	private String node;
	private String level;
	private String sprintId;
	private String kpiId;
	private List<CommentsInfo> commentsInfo;

}
