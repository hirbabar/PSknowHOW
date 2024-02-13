/*
 *   Copyright 2014 CapitalOne, LLC.
 *   Further development Copyright 2022 Sapient Corporation.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.publicissapient.kpidashboard.common.converter;

import java.util.Date;

import org.joda.time.DateTime;
import org.springframework.core.convert.converter.Converter;

/**
 * Custom converter for converting java.util.Date to jodaDateTime
 */
public class DateToJodaDateTimeConverter implements Converter<Date, DateTime>{
    @Override
    public DateTime convert(Date source) {
        return new DateTime(source);
    }

}
