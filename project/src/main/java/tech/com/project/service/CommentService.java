package tech.com.project.service;

import tech.com.project.dto.request.CommentCreateRequestDto;
import tech.com.project.dto.response.CommentResponseDto;

import java.util.List;

public interface CommentService {

    CommentResponseDto addComment(Integer ticketId, CommentCreateRequestDto request);

    List<CommentResponseDto> getCommentsByTicketId(Integer ticketId);
}
