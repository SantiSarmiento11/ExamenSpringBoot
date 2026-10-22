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
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
import tech.com.project.dto.request.TicketStatusUpdateRequestDto;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.model.Category;
import tech.com.project.model.Priority;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.CategoryRepository;
import tech.com.project.repository.PriorityRepository;
import tech.com.project.repository.UserAccountRepository;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TicketControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PriorityRepository priorityRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userToken;
    private String adminToken;
    private String techToken;

    private Integer categoryId;
    private Integer highPriorityId;
    private Integer techUserId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userToken = obtainToken("usuario@technova.com", "admin123");
        adminToken = obtainToken("admin@technova.com", "admin123");
        techToken = obtainToken("tecnico@technova.com", "admin123");

        Category category = categoryRepository.findByName("HARDWARE").orElseGet(() -> categoryRepository.findAll().get(0));
        categoryId = category.getId();

        Priority highPriority = priorityRepository.findByName("ALTA").orElseGet(() -> priorityRepository.findAll().get(0));
        highPriorityId = highPriority.getId();

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
    @DisplayName("RF-02 / RN-03: Creación de ticket por USER, genera código correlativo y dueDate SLA")
    void shouldCreateTicketSuccessfully() throws Exception {
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Falla de monitor secundario")
                .description("La pantalla parpadea constantemente al conectar HDMI.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code", startsWith("TK-")))
                .andExpect(jsonPath("$.statusName").value("ABIERTA"))
                .andExpect(jsonPath("$.categoryName").value("HARDWARE"))
                .andExpect(jsonPath("$.priorityName").value("ALTA"))
                .andExpect(jsonPath("$.requesterEmail").value("usuario@technova.com"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty());
    }

    @Test
    @DisplayName("RF-02: Rechazar creación de ticket con categoría inexistente (404 Not Found)")
    void shouldRejectCreationWithInvalidCategory() throws Exception {
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Falla de prueba")
                .description("Descripción de prueba")
                .categoryId(99999)
                .priorityId(highPriorityId)
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("RF-02: Filtro de listado por rol (USER ve propios, ADMIN ve todos)")
    void shouldFilterTicketListByRole() throws Exception {
        // 1. Crear ticket como USER
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Ticket para verificación de roles")
                .description("Verificación de visibilidad por roles.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Consulta de USER: debe listar el ticket
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 3. Consulta de ADMIN: debe listar los tickets
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("RF-03 / CA-001: Asignación manual por ADMIN cambia estado a ASIGNADA")
    void shouldAssignTicketAsAdminSuccessfully() throws Exception {
        // 1. Crear ticket
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para asignación técnica")
                .description("Pendiente de asignación a técnico.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int ticketId = rootNode.get("id").asInt();

        // 2. Asignar por ADMIN
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("ASIGNADA"))
                .andExpect(jsonPath("$.assignedTechnicianId").value(techUserId))
                .andExpect(jsonPath("$.assignedTechnicianEmail").value("tecnico@technova.com"));
    }

    @Test
    @DisplayName("CA-001: Denegar asignación manual a usuario no administrador (403 Forbidden)")
    void shouldDenyTicketAssignmentToNonAdmin() throws Exception {
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/1/assign")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-04 / RN-02: Ciclo de vida completo estricto (ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA)")
    void shouldCompleteFullTicketLifecycle() throws Exception {
        // 1. USER crea ticket -> ABIERTA
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket ciclo completo de estados")
                .description("Prueba end-to-end de la máquina de estados.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusName").value("ABIERTA"))
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // 2. ADMIN asigna técnico -> ASIGNADA
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("ASIGNADA"));

        // 3. TECHNICIAN toma ticket -> EN_PROCESO
        TicketStatusUpdateRequestDto toProcessReq = TicketStatusUpdateRequestDto.builder()
                .status("EN_PROCESO")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toProcessReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("EN_PROCESO"));

        // 4. TECHNICIAN resuelve ticket -> RESUELTA
        TicketStatusUpdateRequestDto toResolveReq = TicketStatusUpdateRequestDto.builder()
                .status("RESUELTA")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toResolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("RESUELTA"));

        // 5. USER da conformidad y cierra ticket -> CERRADA
        TicketStatusUpdateRequestDto toCloseReq = TicketStatusUpdateRequestDto.builder()
                .status("CERRADA")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCloseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("CERRADA"));
    }

    @Test
    @DisplayName("RN-02: Rechazar saltos inválidos de estado con 400 Bad Request")
    void shouldRejectInvalidStateTransitionJump() throws Exception {
        // Crear ticket -> ABIERTA
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para salto de estado inválido")
                .description("Intento de salto directo a RESUELTA.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // Intentar saltar de ABIERTA a RESUELTA
        TicketStatusUpdateRequestDto jumpReq = TicketStatusUpdateRequestDto.builder()
                .status("RESUELTA")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jumpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Transición")));
    }

    @Test
    @DisplayName("RN-01 / RN-02: Rechazar transición ejecutada por rol no autorizado con 403 Forbidden")
    void shouldRejectStateTransitionFromUnauthorizedRole() throws Exception {
        // 1. Crear ticket
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para validación de roles en transición")
                .description("USER intenta pasar a EN_PROCESO.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // 2. Asignar por ADMIN -> ASIGNADA
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // 3. USER intenta pasar de ASIGNADA a EN_PROCESO (exclusivo de TECHNICIAN)
        TicketStatusUpdateRequestDto toProcessReq = TicketStatusUpdateRequestDto.builder()
                .status("EN_PROCESO")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toProcessReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("RF-02 / Seguridad: Consulta de detalle de ticket por ID (/api/tickets/{id})")
    void shouldHandleGetTicketByIdWithSecurityChecks() throws Exception {
        // 1. USER crea ticket
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para prueba de detalle")
                .description("Detalles del incidente.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // 2. USER consulta su propio ticket -> 200 OK
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.title").value("Ticket para prueba de detalle"));

        // 3. ADMIN consulta cualquier ticket -> 200 OK
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        // 4. TECHNICIAN no asignado consulta el ticket -> 403 Forbidden
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden());

        // 5. Consulta de ID inexistente -> 404 Not Found
        mockMvc.perform(get("/api/tickets/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Catálogos REST: Consulta de categorías, prioridades y técnicos")
    void shouldRetrieveCatalogsSuccessfully() throws Exception {
        // Categorías
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").isNotEmpty());

        // Prioridades
        mockMvc.perform(get("/api/priorities")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].level").isNumber());

        // Técnicos
        mockMvc.perform(get("/api/users/technicians")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].email").value("tecnico@technova.com"));
    }

    @Test
    @DisplayName("RN-02 / CA-001: Rechazar transición directa a ASIGNADA vía /status (debe usar /assign)")
    void shouldRejectDirectTransitionToAsignadaViaStatusEndpoint() throws Exception {
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para probar rechazo de /status hacia ASIGNADA")
                .description("Debe obligar a usar el endpoint de asignación.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        TicketStatusUpdateRequestDto statusReq = TicketStatusUpdateRequestDto.builder()
                .status("ASIGNADA")
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("asignación de un técnico")));
    }

    @Test
    @DisplayName("Seguridad / ACT-02: Técnico puede consultar sus propios tickets creados aunque no estén asignados a él")
    void shouldAllowTechnicianToViewOwnRequestedTicket() throws Exception {
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Incidente reportado por un técnico para sí mismo")
                .description("El monitor del técnico no enciende.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // El técnico consulta su propio ticket en estado ABIERTA (sin técnico asignado aún)
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.requesterEmail").value("tecnico@technova.com"));
    }

    @Test
    @DisplayName("RF-03: Rechazar asignación cuando el usuario no tiene rol TECHNICIAN")
    void shouldRejectAssignmentToNonTechnician() throws Exception {
        TicketCreateRequestDto createReq = TicketCreateRequestDto.builder()
                .title("Ticket para probar asignación inválida")
                .description("Intento de asignar a un usuario normal.")
                .categoryId(categoryId)
                .priorityId(highPriorityId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        int ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        UserAccount regularUser = userAccountRepository.findByEmail("usuario@technova.com").orElseThrow();

        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(regularUser.getId())
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("no tiene el rol de TECHNICIAN")));
    }
}
