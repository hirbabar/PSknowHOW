package com.publicissapient.kpidashboard.common.context;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExecutionLogContextTest {

	private ExecutionLogContext executionLogContext;

	@Before
	public void setUp() {
		executionLogContext = new ExecutionLogContext();
	}

	@After
	public void tearDown() {
		executionLogContext.destroy();
	}

	@Test
	public void testContextInitialization() {
		assertNull(ExecutionLogContext.getContext().getRequestId());
		assertNull(ExecutionLogContext.getContext().getEnvironment());
		assertNull(ExecutionLogContext.getContext().getProjectName());
		assertNull(ExecutionLogContext.getContext().getProjectBasicConfgId());
		assertNull(ExecutionLogContext.getContext().getIsCron());
	}

	@Test
	public void testUpdateContextValues() {
		executionLogContext.setRequestId("initialRequestId");
		executionLogContext.setEnvironment("initialEnvironment");
		executionLogContext.setProjectName("initialProjectName");
		executionLogContext.setProjectBasicConfgId("initialProjectConfigId");
		executionLogContext.setIsCron("initialIsCron");

		ExecutionLogContext updateContext = new ExecutionLogContext();
		updateContext.setRequestId("updatedRequestId");
		updateContext.setEnvironment("updatedEnvironment");
		updateContext.setProjectName("updatedProjectName");
		updateContext.setProjectBasicConfgId("updatedProjectConfigId");
		updateContext.setIsCron("updatedIsCron");

		ExecutionLogContext updatedContext = ExecutionLogContext.updateContext(updateContext);

		assertEquals("updatedRequestId", updatedContext.getRequestId());
		assertEquals("updatedEnvironment", updatedContext.getEnvironment());
		assertEquals("updatedProjectName", updatedContext.getProjectName());
		assertEquals("updatedProjectConfigId", updatedContext.getProjectBasicConfgId());
		assertEquals("updatedIsCron", updatedContext.getIsCron());
	}

	@Test
	public void testThreadIdAssignment() {
		assertEquals(1, ExecutionLogContext.getContext().getThreadId());
	}
}