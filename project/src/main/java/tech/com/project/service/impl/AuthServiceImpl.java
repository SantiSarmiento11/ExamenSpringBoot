package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.request.LoginRequestDto;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.dto.response.UserProfileDto;
import tech.com.project.security.jwt.JwtUtils;
import tech.com.project.security.services.UserDetailsImpl;
import tech.com.project.service.AuthService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final tech.com.project.repository.UserAccountRepository userAccountRepository;
    private final tech.com.project.repository.RoleRepository roleRepository;
    private final tech.com.project.repository.UserRoleRepository userRoleRepository;

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("Procesando autenticación para el usuario: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);

        log.info("Autenticación exitosa. Token generado para: {}", userDetails.getUsername());

        return LoginResponseDto.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .roles(userDetails.getRoleNames())
                .permissions(userDetails.getPermissionNames())
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("No hay un usuario autenticado en la sesión actual.");
        }

        return UserProfileDto.builder()
                .id(userDetails.getId())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .email(userDetails.getEmail())
                .isActive(userDetails.isEnabled())
                .roles(userDetails.getRoleNames())
                .permissions(userDetails.getPermissionNames())
                .build();
    }

    @Override
    @Transactional
    public UserProfileDto register(tech.com.project.dto.request.UserRegisterRequestDto request) {
        log.info("Procesando registro de nuevo usuario: {}", request.getEmail());

        String rawRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";
        if (rawRole.startsWith("ROLE_")) {
            rawRole = rawRole.substring(5);
        }

        // Restricción: Únicamente se permite crear técnicos o usuarios normales
        if (!"TECHNICIAN".equals(rawRole) && !"USER".equals(rawRole)) {
            throw new IllegalArgumentException("Solo se permite registrar usuarios con rol TECHNICIAN o USER.");
        }

        final String finalRoleName = rawRole;
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado en el sistema: " + normalizedEmail);
        }

        tech.com.project.model.Role role = roleRepository.findByName(finalRoleName)
                .orElseThrow(() -> new tech.com.project.exception.ResourceNotFoundException("El rol " + finalRoleName + " no existe en el sistema."));

        tech.com.project.model.UserAccount user = tech.com.project.model.UserAccount.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        tech.com.project.model.UserAccount savedUser = userAccountRepository.save(user);

        tech.com.project.model.UserRole userRole = tech.com.project.model.UserRole.builder()
                .id(new tech.com.project.model.UserRoleId(savedUser.getId(), role.getId()))
                .userAccount(savedUser)
                .role(role)
                .assignedAt(java.time.LocalDateTime.now())
                .build();

        userRoleRepository.save(userRole);

        log.info("Usuario [{}] registrado exitosamente con rol [{}]", savedUser.getEmail(), role.getName());

        java.util.List<String> permissions = role.getRolePermissions() != null
                ? role.getRolePermissions().stream().map(rp -> rp.getPermission().getName()).toList()
                : java.util.List.of();

        return UserProfileDto.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .isActive(savedUser.getIsActive())
                .roles(java.util.List.of(role.getName()))
                .permissions(permissions)
                .build();
    }
}
