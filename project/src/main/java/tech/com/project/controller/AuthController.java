package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.com.project.dto.request.LoginRequestDto;
import tech.com.project.dto.response.ApiErrorResponse;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.dto.response.UserProfileDto;
import tech.com.project.service.AuthService;

@RestController
@RequestMapping({"/api/auth", ""})
@RequiredArgsConstructor
@Tag(name = "1. Autenticación y Autorización (RF-01)", description = "Operaciones de inicio de sesión con JWT y consulta de identidad del usuario")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión y obtener token JWT",
            description = "Valida las credenciales de un usuario registrado (ADMIN, TECHNICIAN o USER) y emite un token JWT con vigencia de 24 horas y roles asignados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa. Retorna el token JWT y los datos del perfil.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o campos incompletos.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas (correo o contraseña incorrectos).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Obtener perfil del usuario autenticado",
            description = "Retorna la información personal, roles y permisos del usuario actualmente autenticado mediante el token JWT enviado en el header Authorization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil obtenido correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado. Token no proporcionado, inválido o expirado (CA-002).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        UserProfileDto profile = authService.getCurrentUser();
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/register")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Registrar nuevo técnico o usuario normal (solo ADMIN)",
            description = "Permite a un usuario con rol ADMIN crear técnicos (TECHNICIAN) y usuarios regulares (USER).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos, correo ya registrado o rol no permitido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. Solo administradores pueden registrar usuarios.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<UserProfileDto> register(@Valid @RequestBody tech.com.project.dto.request.UserRegisterRequestDto request) {
        UserProfileDto profile = authService.register(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(profile);
    }
}
