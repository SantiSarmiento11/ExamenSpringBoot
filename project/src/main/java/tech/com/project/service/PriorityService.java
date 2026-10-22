package tech.com.project.service;

import tech.com.project.dto.response.PriorityResponseDto;

import java.util.List;

public interface PriorityService {
    List<PriorityResponseDto> getAllPriorities();
}
