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
import tech.com.project.dto.response.LoginResponseDto;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Debe autenticar exitosamente a un administrador y retornar token JWT")
    void shouldLoginSuccessfullyAsAdmin() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("admin@technova.com")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("admin@technova.com"))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")));
    }

    @Test
    @DisplayName("Debe rechazar credenciales incorrectas con 401 Unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("admin@technova.com")
                .password("contrasena_incorrecta")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("CA-002: Debe rechazar peticiones protegidas sin token con 401 Unauthorized")
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Debe permitir acceso a perfil (/api/auth/me) con token JWT válido")
    void shouldAccessProfileWithValidToken() throws Exception {
        // 1. Login previo para obtener token
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("usuario@technova.com")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponseDto.class
        );

        // 2. Consulta de /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@technova.com"))
                .andExpect(jsonPath("$.roles", hasItem("USER")));
    }

    @Test
    @DisplayName("Debe denegar acceso (403 Forbidden) a un USER en endpoints exclusivos de ADMIN")
    void shouldDenyAccessToUserOnAdminEndpoint() throws Exception {
        // 1. Login como USER
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("usuario@technova.com")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponseDto.class
        );

        // 2. Intentar ingresar a endpoint de ADMIN
        mockMvc.perform(get("/api/auth/demo/admin")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Debe permitir acceso (200 OK) a un ADMIN en endpoints exclusivos de ADMIN")
    void shouldAllowAccessToAdminOnAdminEndpoint() throws Exception {
        // 1. Login como ADMIN
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("admin@technova.com")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponseDto.class
        );

        // 2. Ingresar a endpoint de ADMIN
        mockMvc.perform(get("/api/auth/demo/admin")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
