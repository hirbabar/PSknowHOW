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
package com.publicissapient.kpidashboard.common.activedirectory.modal;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author sansharm13
 *
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ADServerDetail {
	@NotBlank(message = "username may not be blank")
	private String username;
	@NotBlank(message = "password may not be blank")
	private String password;
	@NotBlank(message = "host may not be blank")
	private String host;
	@NotBlank(message = "port may not be blank")
	private int port;
	private String userDn;
	@NotBlank(message = "UserDn may not be blank")
	private String rootDn;
	@NotBlank(message = "Domain may not be blank")
	private String domain;

}
