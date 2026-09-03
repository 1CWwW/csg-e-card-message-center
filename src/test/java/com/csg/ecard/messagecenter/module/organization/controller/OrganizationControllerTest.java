package com.csg.ecard.messagecenter.module.organization.controller;

import com.csg.ecard.messagecenter.module.organization.service.OrganizationService;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationLazyNodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerTest {

    private OrganizationService organizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrganizationController(organizationService)).build();
    }

    @Test
    void shouldExposeLazyChildrenEndpointAndKeepOrganizationIdAsString() throws Exception {
        OrganizationLazyNodeVO node = new OrganizationLazyNodeVO();
        node.setOrgId("100000000000000001");
        node.setOrgName("某某供电局");
        node.setState(1);
        node.setHasChildren(true);
        when(organizationService.children("100")).thenReturn(List.of(node));

        mockMvc.perform(get("/api/msg/organization/tree/children").param("parentOrgId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.result[0].orgId").value("100000000000000001"))
                .andExpect(jsonPath("$.result[0].state").value(1))
                .andExpect(jsonPath("$.result[0].hasChildren").value(true))
                .andExpect(jsonPath("$.result[0].children").isArray())
                .andExpect(jsonPath("$.result[0].children").isEmpty());
    }

    @Test
    void shouldExposeBatchResolveEndpointWithFlatNodeContract() throws Exception {
        OrganizationLazyNodeVO resolved = new OrganizationLazyNodeVO();
        resolved.setOrgId("1001");
        resolved.setHasChildren(false);
        when(organizationService.resolve(any())).thenReturn(List.of(resolved));

        mockMvc.perform(post("/api/msg/organization/tree/resolve")
                        .contentType("application/json")
                        .content("{\"orgIds\":[\"1001\",\"1002\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].orgId").value("1001"))
                .andExpect(jsonPath("$.result[0].hasChildren").value(false))
                .andExpect(jsonPath("$.result[0].children").isEmpty())
                .andExpect(jsonPath("$.result[0].ancestors").doesNotExist());
    }

    @Test
    void shouldReturnEmptySearchResultForBlankKeyword() throws Exception {
        when(organizationService.search("")).thenReturn(List.of());

        mockMvc.perform(get("/api/msg/organization/search").param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result").isEmpty());
    }
}
