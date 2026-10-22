package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.com.project.dto.response.ApiErrorResponse;
import tech.com.project.dto.response.TechnicianResponseDto;
import tech.com.project.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Usuarios", description = "Operaciones relacionadas a usuarios y personal de soporte")
public class UserController {

    private final UserService userService;

    @GetMapping("/technicians")
    @Operation(
            summary = "Listar técnicos de soporte disponibles",
            description = "Retorna la lista de usuarios activos con rol TECHNICIAN para asignación de tickets.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de técnicos obtenido exitosamente.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TechnicianResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<List<TechnicianResponseDto>> getTechnicians() {
        return ResponseEntity.ok(userService.getAvailableTechnicians());
    }
}
