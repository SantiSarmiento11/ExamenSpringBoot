package tech.com.project.service;

import tech.com.project.dto.response.TechnicianResponseDto;

import java.util.List;

public interface UserService {
    List<TechnicianResponseDto> getAvailableTechnicians();
}
