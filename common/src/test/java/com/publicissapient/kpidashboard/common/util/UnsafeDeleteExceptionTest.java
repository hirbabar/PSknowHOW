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


package com.publicissapient.kpidashboard.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnsafeDeleteExceptionTest {

    @Test
    public void testConstructorWithoutArguments() {
        UnsafeDeleteException exception = new UnsafeDeleteException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessage() {
        String message = "Test Message";
        UnsafeDeleteException exception = new UnsafeDeleteException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndThrowable() {
        String message = "Test Message";
        Throwable cause = new RuntimeException("Test Cause");
        UnsafeDeleteException exception = new UnsafeDeleteException(message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithThrowable() {
        Throwable cause = new RuntimeException("Test Cause");
        UnsafeDeleteException exception = new UnsafeDeleteException(cause);
        assertEquals(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Test Cause", exception.getMessage());
    }
}
