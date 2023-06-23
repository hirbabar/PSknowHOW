package com.publicissapient.kpidashboard.apis.kpis;

import com.publicissapient.kpidashboard.apis.common.service.impl.KpiHelperService;
import com.publicissapient.kpidashboard.common.model.application.FieldMappingStructure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class FieldMappingStructureControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KpiHelperService kpiHelperService;

    @InjectMocks
    private FieldMappingStructureController fieldMappingStructureController;

    @Mock
    private FieldMappingStructure fieldMappingStructure= new FieldMappingStructure();

    private List<FieldMappingStructure> fieldMappingStructureList = new ArrayList<>();

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(fieldMappingStructureController).build();
        fieldMappingStructureList.add(fieldMappingStructure);
    }

    @After
    public void after() {
        mockMvc = null;
    }

    @Test
    public void fetchFieldMappingStructureByKpiFieldMappingData() throws Exception {
        when(kpiHelperService.fetchFieldMappingStructureByKpiFieldMappingData("kpi0")).thenReturn(fieldMappingStructureList);
        mockMvc.perform(get("/kpiFieldMapping/kpi0")).andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"));
    }
}
