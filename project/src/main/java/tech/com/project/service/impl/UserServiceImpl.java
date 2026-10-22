package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.response.TechnicianResponseDto;
import tech.com.project.repository.UserAccountRepository;
import tech.com.project.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TechnicianResponseDto> getAvailableTechnicians() {
        return userAccountRepository.findActiveUsersByRoleName("TECHNICIAN").stream()
                .map(user -> TechnicianResponseDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .isActive(user.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }
}
