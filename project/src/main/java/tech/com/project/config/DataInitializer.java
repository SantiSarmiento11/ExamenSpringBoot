package tech.com.project.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.model.*;
import tech.com.project.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final TicketStatusRepository ticketStatusRepository;
    private final StatusTransitionRepository statusTransitionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("==> Iniciando siembra inicial de datos para TechNova IT Helpdesk...");

        initCategories();
        initPriorities();
        initTicketStatuses();
        initStatusTransitions();
        initRolesAndPermissions();
        initDefaultUsers();

        log.info("==> Siembra inicial de datos completada exitosamente.");
    }

    private void initCategories() {
        createCategoryIfNotExists("HARDWARE", "Incidentes relacionados con equipos físicos, periféricos y componentes de hardware.");
        createCategoryIfNotExists("SOFTWARE", "Problemas con aplicaciones, sistemas operativos o licencias de software.");
        createCategoryIfNotExists("REDES", "Problemas de conectividad, Wi-Fi, VPN, switches o acceso a red local/internet.");
        createCategoryIfNotExists("ACCESOS", "Gestión de credenciales, permisos, desbloqueo de cuentas y autorizaciones.");
        createCategoryIfNotExists("OTROS", "Otras solicitudes o incidencias de soporte no categorizadas.");
    }

    private void createCategoryIfNotExists(String name, String description) {
        if (!categoryRepository.existsByName(name)) {
            Category category = Category.builder()
                    .name(name)
                    .description(description)
                    .isActive(true)
                    .build();
            categoryRepository.save(category);
            log.info("Categoría creada: {}", name);
        }
    }

    private void initPriorities() {
        createPriorityIfNotExists("BAJA", "Incidencias menores con impacto mínimo en operaciones (SLA: 24 horas)", 1);
        createPriorityIfNotExists("MEDIA", "Incidencias estándar que afectan parcialmente operaciones (SLA: 8 horas)", 2);
        createPriorityIfNotExists("ALTA", "Incidencias graves que detienen labores críticas (SLA: 2 horas)", 3);
        createPriorityIfNotExists("CRITICA", "Fallas catastróficas que impactan a toda la organización o servicios core (SLA: 1 hora)", 4);
    }

    private void createPriorityIfNotExists(String name, String description, int level) {
        if (!priorityRepository.existsByName(name)) {
            Priority priority = Priority.builder()
                    .name(name)
                    .description(description)
                    .level(level)
                    .build();
            priorityRepository.save(priority);
            log.info("Prioridad creada: {}", name);
        }
    }

    private void initTicketStatuses() {
        createStatusIfNotExists("ABIERTA", "Ticket registrado y pendiente de asignación", false);
        createStatusIfNotExists("ASIGNADA", "Ticket asignado formalmente a un técnico de soporte", false);
        createStatusIfNotExists("EN_PROCESO", "Técnico trabajando activamente en la resolución del ticket", false);
        createStatusIfNotExists("RESUELTA", "Solución implementada por el técnico, pendiente de confirmación", false);
        createStatusIfNotExists("CERRADA", "Ticket formalmente cerrado tras conformidad o inactividad", true);
    }

    private void createStatusIfNotExists(String name, String description, boolean isFinal) {
        if (!ticketStatusRepository.existsByName(name)) {
            TicketStatus status = TicketStatus.builder()
                    .name(name)
                    .description(description)
                    .isFinal(isFinal)
                    .build();
            ticketStatusRepository.save(status);
            log.info("Estado de ticket creado: {}", name);
        }
    }

    private void initStatusTransitions() {
        createTransitionIfNotExists("ABIERTA", "ASIGNADA");
        createTransitionIfNotExists("ASIGNADA", "EN_PROCESO");
        createTransitionIfNotExists("EN_PROCESO", "RESUELTA");
        createTransitionIfNotExists("RESUELTA", "CERRADA");
    }

    private void createTransitionIfNotExists(String fromName, String toName) {
        TicketStatus fromStatus = ticketStatusRepository.findByName(fromName).orElse(null);
        TicketStatus toStatus = ticketStatusRepository.findByName(toName).orElse(null);

        if (fromStatus != null && toStatus != null) {
            if (!statusTransitionRepository.existsByFromStatusAndToStatus(fromStatus, toStatus)) {
                StatusTransition transition = StatusTransition.builder()
                        .fromStatus(fromStatus)
                        .toStatus(toStatus)
                        .isActive(true)
                        .build();
                statusTransitionRepository.save(transition);
                log.info("Transición registrada: {} -> {}", fromName, toName);
            }
        }
    }

    private void initRolesAndPermissions() {
        Role adminRole = createRoleIfNotExists("ADMIN", "Administrador del sistema con privilegios completos");
        Role techRole = createRoleIfNotExists("TECHNICIAN", "Especialista de soporte técnico para atención de tickets");
        Role userRole = createRoleIfNotExists("USER", "Empleado solicitante de tickets de soporte");

        // Limpiar asignaciones previas para reflejar exactamente la nueva matriz de permisos
        rolePermissionRepository.deleteAll();

        // --- Permisos Administrador (ADMIN) ---
        Permission pAdmin1 = createPermissionIfNotExists("TICKET_VIEW_ALL", "Consultar todos los tickets registrados.");
        Permission pAdmin2 = createPermissionIfNotExists("TECHNICIAN_VIEW_AVAILABLE", "Visualizar los técnicos disponibles.");
        Permission pAdmin3 = createPermissionIfNotExists("TICKET_ASSIGN", "Asignar y reasignar técnicos a los tickets.");
        Permission pAdmin4 = createPermissionIfNotExists("USER_VIEW_ALL", "Consultar los usuarios registrados.");
        Permission pAdmin5 = createPermissionIfNotExists("STATISTICS_VIEW", "Consultar estadísticas generales de las incidencias.");
        Permission pAdmin6 = createPermissionIfNotExists("TICKET_HISTORY_VIEW_ALL", "Visualizar el historial de cualquier ticket.");

        assignPermissionToRole(adminRole, pAdmin1);
        assignPermissionToRole(adminRole, pAdmin2);
        assignPermissionToRole(adminRole, pAdmin3);
        assignPermissionToRole(adminRole, pAdmin4);
        assignPermissionToRole(adminRole, pAdmin5);
        assignPermissionToRole(adminRole, pAdmin6);

        // --- Permisos Técnico (TECHNICIAN) ---
        Permission pTech1 = createPermissionIfNotExists("TICKET_VIEW_ASSIGNED", "Consultar los tickets asignados a su cuenta.");
        Permission pTech2 = createPermissionIfNotExists("TICKET_DETAILS_VIEW_ASSIGNED", "Visualizar los detalles de las incidencias asignadas.");
        Permission pTech3 = createPermissionIfNotExists("TICKET_STATUS_UPDATE", "Cambiar el estado de los tickets según las reglas establecidas.");
        Permission pTech4 = createPermissionIfNotExists("TICKET_COMMENT_TECHNICIAN", "Agregar comentarios sobre el proceso de atención.");
        Permission pTech5 = createPermissionIfNotExists("TICKET_HISTORY_VIEW_ASSIGNED", "Consultar el historial de las solicitudes asignadas.");

        assignPermissionToRole(techRole, pTech1);
        assignPermissionToRole(techRole, pTech2);
        assignPermissionToRole(techRole, pTech3);
        assignPermissionToRole(techRole, pTech4);
        assignPermissionToRole(techRole, pTech5);

        // --- Permisos Usuario (USER) ---
        Permission pUser1 = createPermissionIfNotExists("AUTH_REGISTER_LOGIN", "Registrarse e iniciar sesión.");
        Permission pUser2 = createPermissionIfNotExists("TICKET_CREATE", "Crear solicitudes de soporte tecnológico.");
        Permission pUser3 = createPermissionIfNotExists("TICKET_VIEW_OWN", "Consultar únicamente sus propias solicitudes.");
        Permission pUser4 = createPermissionIfNotExists("TICKET_EDIT_OPEN_UNASSIGNED", "Modificar una solicitud mientras permanezca abierta y sin asignar.");
        Permission pUser5 = createPermissionIfNotExists("TICKET_COMMENT_OWN", "Agregar comentarios a sus solicitudes activas.");
        Permission pUser6 = createPermissionIfNotExists("TICKET_HISTORY_VIEW_OWN", "Consultar el estado y el historial de sus tickets.");
        Permission pUser7 = createPermissionIfNotExists("TICKET_CLOSE_RESOLVED", "Cerrar un ticket cuando haya sido resuelto.");

        assignPermissionToRole(userRole, pUser1);
        assignPermissionToRole(userRole, pUser2);
        assignPermissionToRole(userRole, pUser3);
        assignPermissionToRole(userRole, pUser4);
        assignPermissionToRole(userRole, pUser5);
        assignPermissionToRole(userRole, pUser6);
        assignPermissionToRole(userRole, pUser7);
    }

    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            Role saved = roleRepository.save(role);
            log.info("Rol creado: {}", name);
            return saved;
        });
    }

    private Permission createPermissionIfNotExists(String name, String description) {
        return permissionRepository.findByName(name).map(p -> {
            p.setDescription(description);
            return permissionRepository.save(p);
        }).orElseGet(() -> {
            Permission permission = Permission.builder()
                    .name(name)
                    .description(description)
                    .build();
            Permission saved = permissionRepository.save(permission);
            log.info("Permiso creado: {}", name);
            return saved;
        });
    }

    private void assignPermissionToRole(Role role, Permission permission) {
        RolePermissionId id = new RolePermissionId(role.getId(), permission.getId());
        if (!rolePermissionRepository.existsById(id)) {
            RolePermission rp = RolePermission.builder()
                    .id(id)
                    .role(role)
                    .permission(permission)
                    .assignedAt(LocalDateTime.now())
                    .build();
            rolePermissionRepository.save(rp);
            log.info("Permiso {} asignado al rol {}", permission.getName(), role.getName());
        }
    }

    private void initDefaultUsers() {
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        Role techRole = roleRepository.findByName("TECHNICIAN").orElse(null);
        Role userRole = roleRepository.findByName("USER").orElse(null);

        String defaultHashedPassword = passwordEncoder.encode("admin123");

        if (adminRole != null) {
            createUserWithRole("Admin", "TechNova", "admin@technova.com", defaultHashedPassword, adminRole);
        }
        if (techRole != null) {
            createUserWithRole("Carlos", "Técnico", "tecnico@technova.com", defaultHashedPassword, techRole);
        }
        if (userRole != null) {
            createUserWithRole("Santiago", "Usuario", "usuario@technova.com", defaultHashedPassword, userRole);
        }
    }

    private void createUserWithRole(String firstName, String lastName, String email, String passwordHash, Role role) {
        if (!userAccountRepository.existsByEmail(email)) {
            LocalDateTime now = LocalDateTime.now();
            UserAccount account = UserAccount.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .passwordHash(passwordHash)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            UserAccount savedAccount = userAccountRepository.save(account);

            UserRoleId userRoleId = new UserRoleId(savedAccount.getId(), role.getId());
            UserRole userRole = UserRole.builder()
                    .id(userRoleId)
                    .userAccount(savedAccount)
                    .role(role)
                    .assignedAt(now)
                    .build();
            userRoleRepository.save(userRole);

            log.info("Usuario inicial creado: {} ({}) con rol {}", email, firstName, role.getName());
        }
    }
}
