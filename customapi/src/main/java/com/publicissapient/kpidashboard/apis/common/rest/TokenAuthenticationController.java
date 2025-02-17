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

package com.publicissapient.kpidashboard.apis.common.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.publicissapient.kpidashboard.apis.auth.AuthProperties;
import com.publicissapient.kpidashboard.apis.auth.token.CookieUtil;
import com.publicissapient.kpidashboard.apis.auth.token.TokenAuthenticationService;
import com.publicissapient.kpidashboard.apis.common.UserTokenAuthenticationDTO;
import com.publicissapient.kpidashboard.apis.common.service.CustomAnalyticsService;
import com.publicissapient.kpidashboard.apis.model.ServiceResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * aksshriv1
 */
@RestController
public class TokenAuthenticationController {

	public static final String AUTH_DETAILS_UPDATED_FLAG = "auth-details-updated";

	@Autowired
	private TokenAuthenticationService tokenAuthenticationService;

	@Autowired
	private CustomAnalyticsService customAnalyticsService;
	@Autowired
	private AuthProperties authProperties;
	@Autowired
	private CookieUtil cookieUtil;

	@PostMapping(value = "/validateToken")
	public ResponseEntity<ServiceResponse> validateToken(@Valid @RequestBody UserTokenAuthenticationDTO userData,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		String authToken = tokenAuthenticationService.getAuthToken(userData, httpServletRequest);
		ServiceResponse serviceResponse;
		if (null != authToken) {
			boolean expiredToken = tokenAuthenticationService.isJWTTokenExpired(authToken);
			String userName = tokenAuthenticationService.getUserNameFromToken(authToken);
			if (expiredToken) {
				Map<String, Object> userMap = new HashMap<>();
				userMap.put("user_name", userName);
				userMap.put("resourceTokenValid", false);
				serviceResponse = new ServiceResponse(false, "token is expired", userMap);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(serviceResponse);
			} else {
				Map<String, Object> userMap = customAnalyticsService.addAnalyticsDataAndSaveCentralUser(httpServletResponse, userName,
						authToken);
				userMap.put("resourceTokenValid", true);
				serviceResponse = new ServiceResponse(true, "success_valid_token", userMap);
				return ResponseEntity.status(HttpStatus.OK).body(serviceResponse);
			}
		} else {
			serviceResponse = new ServiceResponse(false, "Unauthorized", null);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(serviceResponse);
		}
	}

	// TODO delete code
	@PostMapping(value = "/validateResource")
	public ResponseEntity<ServiceResponse> validateResource(@Valid @RequestBody UserTokenAuthenticationDTO userData,
			HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = tokenAuthenticationService.validateAuthentication(request, response);
		ServiceResponse serviceResponse;
		if (null != authentication) {
			Collection<String> authDetails = response.getHeaders(AUTH_DETAILS_UPDATED_FLAG);
			boolean value = authDetails != null && authDetails.stream().anyMatch("true"::equals);
			if (value) {
				Cookie authCookie = cookieUtil.getAuthCookie(request);
				String token = authCookie.getValue();
				UserTokenAuthenticationDTO data = new UserTokenAuthenticationDTO();
				data.setResource(userData.getResource());
				data.setAuthToken(token);
				if (!userData.getResource().equalsIgnoreCase(authProperties.getResourceName())) {
					data.setResourceTokenValid(false);
					serviceResponse = new ServiceResponse(true, "Invalid resource", data);
				} else {
					data.setResourceTokenValid(true);
					serviceResponse = new ServiceResponse(true, "Valid resource", data);
				}
				return ResponseEntity.status(HttpStatus.OK).body(serviceResponse);

			} else {
				serviceResponse = new ServiceResponse(false, "token is expired", null);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(serviceResponse);
			}

		} else {
			serviceResponse = new ServiceResponse(false, "Unauthorized", null);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(serviceResponse);
		}
	}
}
