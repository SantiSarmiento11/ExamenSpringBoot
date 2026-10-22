package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.com.project.dto.request.CommentCreateRequestDto;
import tech.com.project.dto.response.ApiErrorResponse;
import tech.com.project.dto.response.CommentResponseDto;
import tech.com.project.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comentarios", description = "Endpoints para registro y consulta cronológica de comentarios en tickets (RF-05)")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(
            summary = "Agregar un comentario a un ticket",
            description = "Permite registrar un comentario en el ticket. USER solo puede comentar en tickets propios, TECHNICIAN en tickets asignados o propios, y ADMIN en cualquier ticket.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Comentario registrado exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos del comentario inválidos o vacíos.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token JWT requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. No posee permisos sobre este ticket.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable Integer ticketId,
            @Valid @RequestBody CommentCreateRequestDto request
    ) {
        CommentResponseDto response = commentService.addComment(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar comentarios cronológicos de un ticket",
            description = "Retorna todos los comentarios registrados en el ticket en orden cronológico ascendente.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de comentarios obtenido exitosamente.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CommentResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token JWT requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. No posee permisos para consultar este ticket.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Integer ticketId) {
        List<CommentResponseDto> response = commentService.getCommentsByTicketId(ticketId);
        return ResponseEntity.ok(response);
    }
}
