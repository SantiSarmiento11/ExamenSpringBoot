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
import tech.com.project.dto.request.UserRegisterRequestDto;
import tech.com.project.dto.response.LoginResponseDto;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserRegisterTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Obtener token de ADMIN
        LoginRequestDto adminLogin = LoginRequestDto.builder()
                .email("admin@technova.com")
                .password("admin123")
                .build();

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto adminResp = objectMapper.readValue(adminResult.getResponse().getContentAsString(), LoginResponseDto.class);
        this.adminToken = adminResp.getToken();

        // Obtener token de USER regular
        LoginRequestDto userLogin = LoginRequestDto.builder()
                .email("usuario@technova.com")
                .password("admin123")
                .build();

        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto userResp = objectMapper.readValue(userResult.getResponse().getContentAsString(), LoginResponseDto.class);
        this.userToken = userResp.getToken();
    }

    @Test
    @DisplayName("ADMIN puede registrar exitosamente un técnico en /register")
    void adminCanRegisterTechnician() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Marcos")
                .lastName("Técnico")
                .email("marcos.tech." + System.currentTimeMillis() + "@technova.com")
                .password("clave123")
                .role("TECHNICIAN")
                .build();

        mockMvc.perform(post("/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.roles", hasItem("TECHNICIAN")));
    }

    @Test
    @DisplayName("ADMIN puede registrar exitosamente un usuario regular en /api/auth/register")
    void adminCanRegisterNormalUser() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Lucia")
                .lastName("Cliente")
                .email("lucia.user." + System.currentTimeMillis() + "@technova.com")
                .password("clave123")
                .role("USER")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.roles", hasItem("USER")));
    }

    @Test
    @DisplayName("Debe rechazar la creación de usuarios con rol ADMIN")
    void shouldRejectCreationOfAdminRole() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Hack")
                .lastName("Admin")
                .email("hack.admin." + System.currentTimeMillis() + "@technova.com")
                .password("clave123")
                .role("ADMIN")
                .build();

        mockMvc.perform(post("/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Solo se permite registrar usuarios con rol TECHNICIAN o USER")));
    }

    @Test
    @DisplayName("Debe rechazar con 403 Forbidden si un usuario no ADMIN intenta registrar")
    void shouldDenyNonAdminUser() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Intruso")
                .lastName("Test")
                .email("intruso." + System.currentTimeMillis() + "@technova.com")
                .password("clave123")
                .role("USER")
                .build();

        mockMvc.perform(post("/register")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe rechazar con 401 Unauthorized si la petición no está autenticada")
    void shouldDenyUnauthenticated() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Anon")
                .lastName("Test")
                .email("anon." + System.currentTimeMillis() + "@technova.com")
                .password("clave123")
                .role("USER")
                .build();

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe rechazar el registro si el correo ya existe")
    void shouldRejectDuplicateEmail() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .firstName("Duplicado")
                .lastName("Test")
                .email("admin@technova.com") // Ya existe
                .password("clave123")
                .role("USER")
                .build();

        mockMvc.perform(post("/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ya se encuentra registrado")));
    }
}
