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

package com.publicissapient.kpidashboard.apis.auth.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import com.publicissapient.kpidashboard.apis.auth.model.ApiToken;

/**
 * Repository for api token
 */
public interface ApiTokenRepository extends CrudRepository<ApiToken, ObjectId> {
    /**
     * Find api token by user and expiration date
     * @param apiUser
     * @param expirationDt
     * @return api token
     */
    ApiToken findByApiUserAndExpirationDt(String apiUser, Long expirationDt);

    /**
     * Find api token by user and api key
     * @param apiUser
     * @param apiKey
     * @return api token
     */
    ApiToken findByApiUserAndApiKey(String apiUser, String apiKey);

    /**
     * Returns List of api tokens by user
     * @param apiUser
     * @return
     */
    List<ApiToken> findByApiUser(String apiUser);
}
