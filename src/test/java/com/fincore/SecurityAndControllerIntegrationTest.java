package com.fincore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to protected dashboard should redirect to login")
    void testUnauthenticatedAccess_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Customer user accessing admin portal should receive 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"CUSTOMER"})
    void testCustomerAccessToAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrator accessing admin dashboard should receive 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAccessToAdminDashboard_ReturnsOk() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @DisplayName("Customer accessing their dashboard should receive 200 OK")
    @WithMockUser(username = "john_doe", roles = {"CUSTOMER"})
    void testCustomerAccessToDashboard_ReturnsOk() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @DisplayName("REST API GET /api/v1/accounts with authentication should return 200 OK")
    @WithMockUser(username = "john_doe", roles = {"CUSTOMER"})
    void testRestApiAccounts_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
