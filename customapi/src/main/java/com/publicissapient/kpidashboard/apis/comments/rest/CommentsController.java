package com.publicissapient.kpidashboard.apis.comments.rest;

import com.publicissapient.kpidashboard.apis.comments.service.CommentsService;
import com.publicissapient.kpidashboard.common.model.comments.CommentRequestDTO;
import com.publicissapient.kpidashboard.common.model.comments.CommentSubmitDTO;
import com.publicissapient.kpidashboard.apis.model.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;


/**
 * @author Mahesh
 *
 */
@RestController
@RequestMapping("/comments")
@Slf4j
public class CommentsController {

	@Autowired
	private CommentsService commentsService;

	/**
	 * This method will get the comments data based on the selected project from the organization level. This feature will work for both, Scrum and Kanban KPIs.
	 *
	 * @param commentRequestDTO
	 * @return
	 */
	@PostMapping("/getCommentsByKpiId")
	public ResponseEntity<ServiceResponse> getCommentsByKPI(@RequestBody CommentRequestDTO commentRequestDTO) {

		final Map<String, Object> mappedCommentInfo = commentsService.findCommentByKPIId(commentRequestDTO.getNode(),
				commentRequestDTO.getLevel(), commentRequestDTO.getSprintId(), commentRequestDTO.getKpiId());
		if (MapUtils.isEmpty(mappedCommentInfo)) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ServiceResponse(false, "Comment not found", mappedCommentInfo));
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(new ServiceResponse(true, "Found comments", mappedCommentInfo));

	}

	/**
	 * This method will save the comment for a selected project from the organization level. Only one comment can submit at a time for the project & selected KPI.
	 * 
	 * @param comment
	 * @return
	 */
	@PostMapping("/submitComments")
	public ResponseEntity<ServiceResponse> submitComments(@Valid @RequestBody CommentSubmitDTO comment) {

		boolean responseStatus = commentsService.submitComment(comment);
		if (responseStatus) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ServiceResponse(responseStatus, "Your comment is submitted successfully.", comment));
		} else {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ServiceResponse(responseStatus, "Issue occurred while saving the comment.", comment));
		}
	}
}