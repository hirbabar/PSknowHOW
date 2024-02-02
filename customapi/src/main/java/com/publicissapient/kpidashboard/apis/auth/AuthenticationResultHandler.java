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

package com.publicissapient.kpidashboard.apis.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.publicissapient.kpidashboard.apis.auth.service.AuthenticationService;
import com.publicissapient.kpidashboard.apis.auth.token.CookieUtil;
import com.publicissapient.kpidashboard.apis.common.service.CustomAnalyticsService;

@Component
public class AuthenticationResultHandler implements AuthenticationSuccessHandler {

	@Autowired
	private AuthenticationResponseService authenticationResponseService;

	@Autowired
	private CustomAnalyticsService customAnalyticsService;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private CookieUtil cookieUtil;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		authenticationResponseService.handle(response, authentication);
		// sgu106: Google Analytics data population starts
		String username = authenticationService.getUsername(authentication);
		Cookie authCookie = cookieUtil.getAuthCookie(request);
		String token = authCookie.getValue();
		Map<String, Object> userMap = customAnalyticsService.addAnalyticsData(response, username, token);
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(userMap);
		// sgu106: Google Analytics data population ends

	}

}
