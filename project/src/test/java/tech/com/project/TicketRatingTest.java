package tech.com.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tech.com.project.dto.request.*;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.dto.response.TicketResponseDto;
import tech.com.project.model.*;
import tech.com.project.repository.*;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TicketRatingTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PriorityRepository priorityRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private String userToken;
    private String user2Token;
    private String adminToken;
    private String techToken;

    private Integer categoryId;
    private Integer priorityId;
    private Integer techUserId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Asegurar la existencia de un segundo usuario solicitante para pruebas de permisos
        createSecondUserIfNotExists();

        userToken = obtainToken("usuario@technova.com", "admin123");
        user2Token = obtainToken("usuario2@technova.com", "admin123");
        adminToken = obtainToken("admin@technova.com", "admin123");
        techToken = obtainToken("tecnico@technova.com", "admin123");

        Category category = categoryRepository.findByName("HARDWARE")
                .orElseGet(() -> categoryRepository.findAll().get(0));
        categoryId = category.getId();

        Priority priority = priorityRepository.findByName("MEDIA")
                .orElseGet(() -> priorityRepository.findAll().get(0));
        priorityId = priority.getId();

        UserAccount techUser = userAccountRepository.findByEmail("tecnico@technova.com").orElseThrow();
        techUserId = techUser.getId();
    }

    private void createSecondUserIfNotExists() {
        if (!userAccountRepository.existsByEmail("usuario2@technova.com")) {
            Role userRole = roleRepository.findByName("USER").orElseThrow();
            UserAccount u2 = UserAccount.builder()
                    .firstName("Maria")
                    .lastName("Perez")
                    .email("usuario2@technova.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            UserAccount saved = userAccountRepository.save(u2);
            UserRole ur = UserRole.builder()
                    .id(new UserRoleId(saved.getId(), userRole.getId()))
                    .userAccount(saved)
                    .role(userRole)
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(ur);
        }
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

    private Integer createTicketAsUser() throws Exception {
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Problema con periférico CSAT")
                .description("Mouse no funciona correctamente en estación de trabajo.")
                .categoryId(categoryId)
                .priorityId(priorityId)
                .build();

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(content, "$.id");
    }

    private Integer createAndCloseTicket() throws Exception {
        Integer ticketId = createTicketAsUser();

        // Asignar
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder().technicianId(techUserId).build();
        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // EN_PROCESO
        TicketStatusUpdateRequestDto procReq = new TicketStatusUpdateRequestDto("EN_PROCESO");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procReq)))
                .andExpect(status().isOk());

        // RESUELTA
        TicketStatusUpdateRequestDto resReq = new TicketStatusUpdateRequestDto("RESUELTA");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isOk());

        // CERRADA
        TicketStatusUpdateRequestDto closeReq = new TicketStatusUpdateRequestDto("CERRADA");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk());

        return ticketId;
    }

    @Test
    @DisplayName("CSAT: Calificación exitosa (201 Created) de ticket en estado CERRADA por su solicitante y consulta (200 OK)")
    void shouldRateClosedTicketSuccessfully() throws Exception {
        Integer ticketId = createAndCloseTicket();

        TicketRatingRequestDto request = TicketRatingRequestDto.builder()
                .puntuacion(5)
                .comentario("Excelente atención y resolución rápida.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.puntuacion").value(5))
                .andExpect(jsonPath("$.comentario").value("Excelente atención y resolución rápida."))
                .andExpect(jsonPath("$.fechaCalificacion").isNotEmpty())
                .andExpect(jsonPath("$.requesterName").isNotEmpty())
                .andExpect(jsonPath("$.technicianId").value(techUserId))
                .andExpect(jsonPath("$.technicianName").isNotEmpty());

        // Consultar calificación
        mockMvc.perform(get("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.puntuacion").value(5));
    }

    @Test
    @DisplayName("CSAT: Rechazo (400 Bad Request) si el ticket no está en estado CERRADA (ABIERTA)")
    void shouldRejectRatingWhenTicketIsOpen() throws Exception {
        Integer ticketId = createTicketAsUser();

        TicketRatingRequestDto request = TicketRatingRequestDto.builder()
                .puntuacion(4)
                .comentario("Aún está abierto.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Solo se pueden calificar tickets en estado CERRADA")));
    }

    @Test
    @DisplayName("CSAT: Rechazo (400 Bad Request) si el ticket no está en estado CERRADA (RESUELTA)")
    void shouldRejectRatingWhenTicketIsResolved() throws Exception {
        Integer ticketId = createTicketAsUser();

        // Asignar
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder().technicianId(techUserId).build();
        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // EN_PROCESO
        TicketStatusUpdateRequestDto procReq = new TicketStatusUpdateRequestDto("EN_PROCESO");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procReq)))
                .andExpect(status().isOk());

        // RESUELTA
        TicketStatusUpdateRequestDto resReq = new TicketStatusUpdateRequestDto("RESUELTA");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isOk());

        TicketRatingRequestDto request = TicketRatingRequestDto.builder()
                .puntuacion(4)
                .comentario("Aún no está cerrado formalmente.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Solo se pueden calificar tickets en estado CERRADA")));
    }

    @Test
    @DisplayName("CSAT: Rechazo (403 Forbidden) si califica un usuario que no es el solicitante")
    void shouldRejectRatingByNonRequester() throws Exception {
        Integer ticketId = createAndCloseTicket();

        TicketRatingRequestDto request = TicketRatingRequestDto.builder()
                .puntuacion(3)
                .comentario("Intento de calificar ticket ajeno.")
                .build();

        // Intento por usuario2 (otro USER)
        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Intento por técnico
        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSAT: Rechazo (400 Bad Request) si se intenta calificar por segunda vez el mismo ticket")
    void shouldRejectDuplicateRating() throws Exception {
        Integer ticketId = createAndCloseTicket();

        TicketRatingRequestDto request = TicketRatingRequestDto.builder()
                .puntuacion(5)
                .comentario("Primera calificación.")
                .build();

        // Primera calificación exitosa
        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segunda calificación rechazada
        TicketRatingRequestDto requestDuplicado = TicketRatingRequestDto.builder()
                .puntuacion(4)
                .comentario("Segunda calificación no permitida.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDuplicado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("El ticket ya cuenta con una calificación registrada.")));
    }

    @Test
    @DisplayName("CSAT: Rechazo (400 Bad Request) si puntuacion < 1 o > 5")
    void shouldRejectRatingWithInvalidScore() throws Exception {
        Integer ticketId = createAndCloseTicket();

        // Puntuación 0 (< 1)
        TicketRatingRequestDto reqMenor = TicketRatingRequestDto.builder()
                .puntuacion(0)
                .comentario("Puntuación muy baja no válida")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMenor)))
                .andExpect(status().isBadRequest());

        // Puntuación 6 (> 5)
        TicketRatingRequestDto reqMayor = TicketRatingRequestDto.builder()
                .puntuacion(6)
                .comentario("Puntuación sobrepasada no válida")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/calificacion")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMayor)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CSAT: Consulta exitosa de GET /api/admin/metricas-csat para ADMIN y 403 para usuarios no ADMIN")
    void shouldAllowAdminToViewCsatMetricsAndRejectNonAdmin() throws Exception {
        // ADMIN consulta exitosamente
        mockMvc.perform(get("/api/admin/metricas-csat")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedioGeneral").isNumber())
                .andExpect(jsonPath("$.totalCalificaciones").isNumber())
                .andExpect(jsonPath("$.desglosePorTecnico").isArray());

        // USER consulta y es rechazado con 403
        mockMvc.perform(get("/api/admin/metricas-csat")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // TECHNICIAN consulta y es rechazado con 403
        mockMvc.perform(get("/api/admin/metricas-csat")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSAT: Retorna 404 Not Found si se consulta calificación inexistente")
    void shouldReturn404WhenRatingNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/999999/calificacion")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}
