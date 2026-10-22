package tech.com.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tech.com.project.dto.request.LoginRequestDto;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminDashboardControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private String adminToken;
    private String techToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminToken = obtainToken("admin@technova.com", "admin123");
        techToken = obtainToken("tecnico@technova.com", "admin123");
        userToken = obtainToken("usuario@technova.com", "admin123");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("token").asText();
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - Exitoso para rol ADMIN con métricas consolidadas (RF-07)")
    void getDashboardMetrics_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets", notNullValue()))
                .andExpect(jsonPath("$.openTickets", notNullValue()))
                .andExpect(jsonPath("$.assignedTickets", notNullValue()))
                .andExpect(jsonPath("$.inProgressTickets", notNullValue()))
                .andExpect(jsonPath("$.resolvedTickets", notNullValue()))
                .andExpect(jsonPath("$.closedTickets", notNullValue()))
                .andExpect(jsonPath("$.slaCompliancePercentage", notNullValue()))
                .andExpect(jsonPath("$.averageResolutionTimeHours", notNullValue()))
                .andExpect(jsonPath("$.categoryDistribution", isA(java.util.List.class)))
                .andExpect(jsonPath("$.priorityDistribution", isA(java.util.List.class)))
                .andExpect(jsonPath("$.statusDistribution", isA(java.util.List.class)))
                .andExpect(jsonPath("$.statusDistribution[0].statusId", notNullValue()))
                .andExpect(jsonPath("$.technicianWorkloads", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - 403 Forbidden para rol TECHNICIAN")
    void getDashboardMetrics_TechnicianForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - 403 Forbidden para rol USER")
    void getDashboardMetrics_UserForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - 401 Unauthorized sin token JWT")
    void getDashboardMetrics_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
