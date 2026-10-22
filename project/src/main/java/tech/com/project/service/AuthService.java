package tech.com.project.service;

import tech.com.project.dto.request.LoginRequestDto;
import tech.com.project.dto.response.LoginResponseDto;
import tech.com.project.dto.response.UserProfileDto;

public interface AuthService {

    /**
     * Autentica las credenciales del usuario y genera un token JWT.
     *
     * @param request DTO con el correo y la contraseña.
     * @return DTO con el token JWT emitido y la información de identidad del usuario.
     */
    LoginResponseDto login(LoginRequestDto request);

    /**
     * Obtiene el perfil del usuario actualmente autenticado en la sesión.
     *
     * @return DTO con los datos del perfil y roles.
     */
    UserProfileDto getCurrentUser();

    /**
     * Registra un nuevo usuario con rol TECHNICIAN o USER (solo ADMIN).
     *
     * @param request Datos del nuevo usuario a registrar.
     * @return Perfil del usuario creado.
     */
    UserProfileDto register(tech.com.project.dto.request.UserRegisterRequestDto request);
}

