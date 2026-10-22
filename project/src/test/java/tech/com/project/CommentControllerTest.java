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
import tech.com.project.dto.request.CommentCreateRequestDto;
import tech.com.project.dto.request.LoginRequestDto;
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
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
class CommentControllerTest {

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

    private Integer categoryId;
    private Integer priorityId;
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

        Priority priority = priorityRepository.findByName("MEDIA").orElseGet(() -> priorityRepository.findAll().get(0));
        priorityId = priority.getId();

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

    private Integer createTicketAsUser() throws Exception {
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Problema con periférico")
                .description("El teclado no responde adecuadamente.")
                .categoryId(categoryId)
                .priorityId(priorityId)
                .build();

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return rootNode.get("id").asInt();
    }

    @Test
    @DisplayName("RF-05: El solicitante (USER) puede agregar un comentario en su propio ticket")
    void shouldAllowRequesterToAddComment() throws Exception {
        Integer ticketId = createTicketAsUser();

        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("Adjunto información adicional: el problema ocurre en puertos USB 3.0.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.content").value("Adjunto información adicional: el problema ocurre en puertos USB 3.0."))
                .andExpect(jsonPath("$.authorEmail").value("usuario@technova.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("RF-05: Un ADMIN puede agregar comentarios en cualquier ticket")
    void shouldAllowAdminToAddComment() throws Exception {
        Integer ticketId = createTicketAsUser();

        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("Nota administrativa: ticket revisado por el coordinador.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorEmail").value("admin@technova.com"));
    }

    @Test
    @DisplayName("RF-05: Un TECHNICIAN asignado puede agregar comentarios")
    void shouldAllowAssignedTechnicianToAddComment() throws Exception {
        Integer ticketId = createTicketAsUser();

        // Asignar técnico por ADMIN
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder()
                .technicianId(techUserId)
                .build();

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // Comentar por el técnico asignado
        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("He recibido la solicitud y comenzaré con las pruebas de hardware.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorEmail").value("tecnico@technova.com"));
    }

    @Test
    @DisplayName("RF-05: Un TECHNICIAN no asignado no puede comentar en un ticket que no es suyo (403 Forbidden)")
    void shouldRejectUnassignedTechnicianFromCommenting() throws Exception {
        Integer ticketId = createTicketAsUser();

        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("Intento de comentario no autorizado por técnico sin asignación.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-05: Validación de contenido vacío rechaza con 400 Bad Request")
    void shouldRejectEmptyCommentContent() throws Exception {
        Integer ticketId = createTicketAsUser();

        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("   ")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("RF-05: Listado cronológico de comentarios")
    void shouldListCommentsInChronologicalOrder() throws Exception {
        Integer ticketId = createTicketAsUser();

        CommentCreateRequestDto c1 = CommentCreateRequestDto.builder().content("Primer comentario").build();
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1)))
                .andExpect(status().isCreated());

        CommentCreateRequestDto c2 = CommentCreateRequestDto.builder().content("Segundo comentario").build();
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("Primer comentario"))
                .andExpect(jsonPath("$[1].content").value("Segundo comentario"));
    }

    @Test
    @DisplayName("RN-02 / RF-05: Rechazar comentarios cuando el ticket se encuentra en estado CERRADA (400 Bad Request)")
    void shouldRejectCommentWhenTicketIsClosed() throws Exception {
        Integer ticketId = createTicketAsUser();

        // Asignar
        TicketAssignRequestDto assignReq = TicketAssignRequestDto.builder().technicianId(techUserId).build();
        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // EN_PROCESO
        tech.com.project.dto.request.TicketStatusUpdateRequestDto procReq = new tech.com.project.dto.request.TicketStatusUpdateRequestDto("EN_PROCESO");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procReq)))
                .andExpect(status().isOk());

        // RESUELTA
        tech.com.project.dto.request.TicketStatusUpdateRequestDto resReq = new tech.com.project.dto.request.TicketStatusUpdateRequestDto("RESUELTA");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isOk());

        // CERRADA
        tech.com.project.dto.request.TicketStatusUpdateRequestDto closeReq = new tech.com.project.dto.request.TicketStatusUpdateRequestDto("CERRADA");
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk());

        // Intento de comentar sobre ticket CERRADO
        CommentCreateRequestDto commentReq = CommentCreateRequestDto.builder()
                .content("Intento de reapertura con comentario no permitido.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CERRADA")));
    }
}
