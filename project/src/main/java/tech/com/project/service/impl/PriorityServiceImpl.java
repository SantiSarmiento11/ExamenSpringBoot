package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.response.PriorityResponseDto;
import tech.com.project.repository.PriorityRepository;
import tech.com.project.service.PriorityService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriorityServiceImpl implements PriorityService {

    private final PriorityRepository priorityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PriorityResponseDto> getAllPriorities() {
        return priorityRepository.findAllByOrderByLevelAsc().stream()
                .map(priority -> PriorityResponseDto.builder()
                        .id(priority.getId())
                        .name(priority.getName())
                        .description(priority.getDescription())
                        .level(priority.getLevel())
                        .build())
                .collect(Collectors.toList());
    }
}
