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
import tech.com.project.dto.response.PriorityResponseDto;
import tech.com.project.service.PriorityService;

import java.util.List;

@RestController
@RequestMapping("/api/priorities")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Prioridades", description = "Operaciones de consulta del catálogo de prioridades y SLAs")
public class PriorityController {

    private final PriorityService priorityService;

    @GetMapping
    @Operation(
            summary = "Listar prioridades y acuerdos de nivel de servicio (SLA)",
            description = "Retorna la lista de prioridades con su nivel numérico y tiempo máximo de resolución SLA (RN-03).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo de prioridades obtenido exitosamente.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PriorityResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<List<PriorityResponseDto>> getAllPriorities() {
        return ResponseEntity.ok(priorityService.getAllPriorities());
    }
}
