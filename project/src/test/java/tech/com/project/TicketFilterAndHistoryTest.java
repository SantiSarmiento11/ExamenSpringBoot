package tech.com.project;

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
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
import tech.com.project.dto.request.TicketStatusUpdateRequestDto;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.dto.response.TicketResponseDto;
import tech.com.project.model.Category;
import tech.com.project.model.Priority;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.CategoryRepository;
import tech.com.project.repository.PriorityRepository;
import tech.com.project.repository.UserAccountRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TicketFilterAndHistoryTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PriorityRepository priorityRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private String userToken;
    private String adminToken;
    private String techToken;

    private Integer hardwareCatId;
    private Integer softwareCatId;
    private Integer highPriorityId;
    private Integer lowPriorityId;
    private Integer techUserId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userToken = obtainToken("usuario@technova.com", "admin123");
        adminToken = obtainToken("admin@technova.com", "admin123");
        techToken = obtainToken("tecnico@technova.com", "admin123");

        Category hardware = categoryRepository.findByName("HARDWARE").orElseGet(() -> categoryRepository.findAll().get(0));
        hardwareCatId = hardware.getId();

        Category software = categoryRepository.findByName("SOFTWARE").orElseGet(() -> categoryRepository.findAll().get(0));
        softwareCatId = software.getId();

        Priority high = priorityRepository.findByName("ALTA").orElseGet(() -> priorityRepository.findAll().get(0));
        highPriorityId = high.getId();

        Priority low = priorityRepository.findByName("BAJA").orElseGet(() -> priorityRepository.findAll().get(0));
        lowPriorityId = low.getId();

        UserAccount techUser = userAccountRepository.findByEmail("tecnico@technova.com").orElseThrow();
        techUserId = techUser.getId();
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponseDto.class);
        return response.getToken();
    }

    @Test
    @DisplayName("RF-08: Búsqueda multicriterio por palabra clave y categoría")
    void shouldFilterTicketsByKeywordAndCategory() throws Exception {
        TicketCreateRequestDto t1 = TicketCreateRequestDto.builder()
                .title("Falla de tarjeta de video Nvidia")
                .description("Artefactos visuales en pantalla.")
                .categoryId(hardwareCatId)
                .priorityId(highPriorityId)
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isCreated());

        // Búsqueda por query "Nvidia"
        mockMvc.perform(get("/api/tickets?query=Nvidia")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title", containsString("Nvidia")));
    }

    @Test
    @DisplayName("RT-01 / RF-08: Consulta paginada con metadata completa")
    void shouldReturnPaginatedTicketsWithMetadata() throws Exception {
        mockMvc.perform(get("/api/tickets/paged?page=0&size=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("RF-05: Trazabilidad completa de auditoría e historial de cambios de estado")
    void shouldTrackCompleteTicketHistory() throws Exception {
        // 1. Crear ticket
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para auditoría de historial")
                .description("Seguimiento paso a paso del ciclo de vida.")
                .categoryId(hardwareCatId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Integer ticketId = rootNode.get("id").asInt();

        // 2. Asignar por ADMIN (ABIERTA -> ASIGNADA)
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // 3. Pasar a EN_PROCESO por TECHNICIAN
        TicketStatusUpdateRequestDto procReq = TicketStatusUpdateRequestDto.builder()
                .status("EN_PROCESO")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procReq)))
                .andExpect(status().isOk());

        // 4. Consultar historial de auditoría
        mockMvc.perform(get("/api/tickets/" + ticketId + "/history")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                // Registro inicial
                .andExpect(jsonPath("$[0].newStatusName").value("ABIERTA"))
                .andExpect(jsonPath("$[0].previousStatusName").isEmpty())
                // Asignación
                .andExpect(jsonPath("$[1].previousStatusName").value("ABIERTA"))
                .andExpect(jsonPath("$[1].newStatusName").value("ASIGNADA"))
                .andExpect(jsonPath("$[1].changedByEmail").value("admin@technova.com"))
                // En Proceso
                .andExpect(jsonPath("$[2].previousStatusName").value("ASIGNADA"))
                .andExpect(jsonPath("$[2].newStatusName").value("EN_PROCESO"))
                .andExpect(jsonPath("$[2].changedByEmail").value("tecnico@technova.com"));
    }

    @Test
    @DisplayName("SEC-01 / RF-08: Prevenir IDOR - Técnico no puede filtrar tickets de otros técnicos (403 Forbidden)")
    void shouldRejectIdorTechnicianFilterAttempt() throws Exception {
        // Técnico intenta filtrar con technicianId = 99999 (otro técnico)
        mockMvc.perform(get("/api/tickets?technicianId=99999")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("EX-01: Formato inválido en parámetro de consulta retorna 400 Bad Request en lugar de 500")
    void shouldRejectInvalidParameterTypeWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets?categoryId=textoInvalido")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("categoryId")));
    }

    @Test
    @DisplayName("RF-05 / RT-02: Usuario ajeno no puede consultar historial de ticket ajeno (403 Forbidden)")
    void shouldRejectUnauthorizedHistoryAccess() throws Exception {
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket privado de usuario")
                .description("Historial confidencial.")
                .categoryId(hardwareCatId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Integer ticketId = rootNode.get("id").asInt();

        // Técnico sin asignación intenta consultar historial
        mockMvc.perform(get("/api/tickets/" + ticketId + "/history")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
