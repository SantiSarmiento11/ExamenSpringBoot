# **Especificación de Requisitos de Software (ERS)**

Estándar de Documentación de Ingeniería de Software

## **Control del Documento**	

| Atributo | Detalle |
| :---- | :---- |
| Proyecto | TechNova IT Helpdesk |
| Versión | 1.0.0 |
| Fecha de creación | 2026-10-07 |
| Preparado por | Santiago Andrés Sarmiento Martínez |
| Revisado por | Equipo de Arquitectura TechNova |
| Documentación de referencia | Especificaciones del Proyecto TechNova IT Helpdesk |

# **1\. Introducción**

Define el propósito y el alcance general del documento y del sistema.

* **1.1 Propósito del documento:** Definir formalmente los requisitos funcionales y no funcionales para la construcción del sistema de gestión de tickets TechNova IT Helpdesk, sirviendo como acuerdo técnico formal entre el equipo de desarrollo, la gerencia de TI y las partes interesadas.  
* **1.2 Objetivo del sistema:** Desarrollar una plataforma centralizada en la web para el soporte técnico que optimice la trazabilidad de incidentes, automatice la asignación de solicitudes, garantice los acuerdos de nivel de servicio (SLA) y mejore la satisfacción general de los usuarios en TechNova Solutions.  
* **1.3 Alcance del proyecto:** El proyecto contempla la autenticación mediante JWT, un catálogo dinámico de categorías y prioridades, la gestión de solicitudes/tickets, la asignación manual de técnicos por parte del rol ADMIN, un panel analítico para administración, notificaciones por correo electrónico y el historial completo de auditoría. Quedan fuera de esta fase las integraciones con servicios de directorio activo externos y soporte para mensajería instantánea.  
* **1.4 Descripción general del problema:** Actualmente, las solicitudes de soporte técnico en TechNova Solutions se canalizan de forma informal vía correo electrónico o mensajería, causando pérdida de incidentes, falta de priorización, desconocimiento del estado de resolución y ausencia de métricas clave sobre el desempeño del soporte informático.

# **2\. Descripción General del Sistema**

Explica el contexto operativo y tecnológico en el que funcionará la solución de software.

* **2.1 Contexto del sistema:** El sistema opera como una aplicación web de tres capas basada en backend Spring Boot (Java 21), base de datos relacional PostgreSQL y frontend responsivo desarrollado exclusivamente con HTML5, CSS3 y Vanilla JavaScript puros (sin frameworks ni librerías externas), alojado en infraestructura en la nube.  
* **2.2 Características principales:** Registro autónomo de tickets, asignación manual de técnicos por el administrador, control de SLA con temporizadores, gestión del ciclo de vida del ticket mediante máquina de estados (ABIERTA, ASIGNADA, EN\_PROCESO, RESUELTA, CERRADA), dashboard interactivo con gráficos de rendimiento y sistema automatizado de notificaciones por email.  
* **2.3 Usuarios generales:** Empleados de TechNova Solutions (usuarios finales) que solicitan asistencia, equipo de especialistas de soporte técnico (técnicos) y administradores de TI encargados de la gestión operativa y métricas.  
* **2.4 Dependencias:** Servicio SMTP externo para el envío de notificaciones por correo electrónico, servidor PostgreSQL 15+ y soporte de ejecución Java 21 Runtime Environment.  
* **2.5 Consideraciones iniciales:** Se asume una disponibilidad continua del servicio SMTP y se requiere que todo el personal cuente con navegadores web modernos con soporte para HTML5, CSS3 y Vanilla JavaScript ES6+ puro.

# **3\. Actores y Usuarios**

Define quiénes interactuarán con el sistema y qué responsabilidades y privilegios tendrá cada perfil.

| ID Actor | Nombre del Rol | Tipo de Usuario | Permisos / Acciones Principales |
| :---- | :---- | :---- | :---- |
| **ACT-01** | ADMIN | Interno / Administración | Gestión completa de usuarios, categorías, reasignación manual de tickets, configuración de SLA y visualización del panel de métricas. |
| **ACT-02** | TECHNICIAN | Soporte Técnico | Atención de tickets asignados, cambio de estados, adición de comentarios técnicos y resolución de incidentes. |
| **ACT-03** | USER | Empleado Final | Creación de tickets de soporte, consulta del estado de sus solicitudes, agregar comentarios y cancelación de tickets propios. |

# **4\. Requisitos Funcionales**

Define detalladamente qué debe hacer el sistema mediante funcionalidades concretas, medibles y verificables.

| Código | Nombre del Requisito | Descripción Funcional | Prioridad |
| :---- | :---- | :---- | :---- |
| **RF-01** | Autenticación y Autorización | Inicio de sesión mediante credenciales seguras y generación de tokens JWT con control de acceso basado en roles (ADMIN, TECHNICIAN, USER). | Alta |
| **RF-02** | Creación de Tickets | Los usuarios registrados deben poder crear tickets especificando título, descripción, categoría y nivel de prioridad inicial. | Alta |
| **RF-03** | Asignación Manual de Tickets | Un usuario con rol ADMIN es el único autorizado para asignar de forma manual cada ticket nuevo o en cola a un técnico específico, cambiando el estado del ticket a ASIGNADA. | Alta |
| **RF-04** | Gestión de Estados | Garantizar y validar la transición estricta de 5 estados: ABIERTA → ASIGNADA → EN\_PROCESO → RESUELTA → CERRADA, implementando validaciones programáticas en backend. | Alta |
| **RF-05** | Comentarios e Historial | Permitir interacción mediante comentarios entre usuarios y técnicos dentro de cada ticket, manteniendo la trazabilidad histórica. | Media |
| **RF-06** | Notificaciones Automáticas | Envío automático de correos electrónicos informativos ante cambios de estado, asignaciones o nuevos comentarios en los tickets. | Media |
| **RF-07** | Dashboard de Administración | Interfaz centralizada para administradores con métricas de tiempos de resolución, volumen por categoría y cumplimiento de SLA. | Alta |
| **RF-08** | Filtros y Búsqueda Avanzada | Búsqueda y filtrado multicriterio de tickets por rango de fechas, prioridad, estado, técnico asignado y palabras clave. | Media |

# **5\. Requisitos No Funcionales**

Define los atributos de calidad, estándares de rendimiento y condiciones operativas del software.

* **RT-01 — Rendimiento:** El tiempo de respuesta de las peticiones a la API REST no debe superar los 200 ms para consultas estándar bajo una carga de hasta 200 usuarios concurrentes.  
* **RT-02 — Seguridad:** Almacenamiento seguro de contraseñas mediante hash bcrypt, uso de HTTPS y validación estricta de tokens JWT en cada petición protegida.  
* **RT-03 — Disponibilidad:** El sistema debe garantizar una disponibilidad mínima del 99.5% anual en horario laboral operativo.  
* **RT-04 — Escalabilidad:** Arquitectura stateless basada en Spring Boot lista para escalamiento horizontal mediante contenedores Docker.  
* **RT-05 — Usabilidad:** Diseño responsive y limpio adaptado a pantallas de escritorio y dispositivos móviles con una curva de aprendizaje menor a 15 minutos.  
* **RT-06 — Mantenibilidad:** Separación clara de capas (Controlador, Servicio, Repositorio), código limpio documentado y uso de migraciones de BD con Flyway o Liquibase.

# **6\. Reglas de Negocio**

Define las condiciones, políticas orgánicas y restricciones propias del dominio del negocio que el sistema debe validar obligatoriamente.

* **RN-01:** Únicamente los usuarios con el rol ADMIN o TECHNICIAN pueden modificar el estado de un ticket de soporte.  
* **RN-02:** Ciclo de Vida y Máquina de Estados: El backend debe validar programáticamente las transiciones permitidas: ABIERTA → ASIGNADA (realizada exclusivamente por ADMIN al asignar un técnico), ASIGNADA → EN\_PROCESO (realizada por el TECHNICIAN al iniciar la atención), EN\_PROCESO → RESUELTA (realizada por el TECHNICIAN al dar solución) y RESUELTA → CERRADA (realizada exclusivamente por el USER solicitante al dar conformidad o por ADMIN por inactividad). Ninguna otra transición o salto de estado está permitido.  
* **RN-03:** La prioridad de un ticket determina el SLA máximo de respuesta: ALTA (2 horas), MEDIA (8 horas), BAJA (24 horas).

# **7\. Datos e Información**

Define la arquitectura conceptual de los datos que maneja el sistema, sus flujos y persistencia.

* **7.1 Entidades principales:** Usuario (User), Ticket, Categoria (Category), Comentario (Comment) y RegistroAuditoria (AuditLog).  
* **7.2 Datos requeridos:** Usuario (ID, nombre, correo, contraseña\_hash, rol); Ticket (ID, título, descripción, estado, prioridad, id\_usuario, id\_tecnico, id\_categoria, fecha\_creacion, fecha\_actualizacion).  
* **7.3 Relaciones entre entidades:** Un Usuario crea N Tickets (1:N); un Técnico atiende N Tickets (1:N); un Ticket pertenece a 1 Categoría (N:1); un Ticket tiene N Comentarios (1:N).  
* **7.4 Información almacenada:** Persistencia relacional completa en base de datos PostgreSQL con auditoría de cambios de estado y marcas temporales.  
* **7.5 Información consultada o modificada:** Consultas optimizadas de tickets por estado/usuario e historial de modificaciones en el panel de soporte.

# **8\. Interfaces**

Define cómo se comunica e integra el sistema con usuarios finales, infraestructura y servicios externos.

* **8.1 Interfaz de usuario (UI):** Interfaz web SPA desarrollada estrictamente con HTML5, CSS3 y Vanilla JavaScript puro ES6+, sin frameworks (React, Angular, Vue) ni librerías CSS/JS (Tailwind CSS, Bootstrap, jQuery).  
* **8.2 API REST / Web Services:** Endpoints documentados con Swagger/OpenAPI: POST /api/auth/login, GET/POST /api/tickets, PUT /api/tickets/{id}/assign, PUT /api/tickets/{id}/status, POST /api/tickets/{id}/comments, GET /api/admin/dashboard.  
* **8.3 Base de datos:** Conexión con PostgreSQL mediante Spring Data JPA / Hibernate con pool de conexiones HikariCP.  
* **8.4 Servicios externos / Sistemas de terceros:** Integración con servicio SMTP mediante JavaMailSender para el envío automatizado de correos de notificación.

# **9\. Casos de Uso y Flujos**

Describe detalladamente las interacciones paso a paso entre los actores y el sistema.

* ## **CU-001 — Creación y Asignación Automática de Ticket**

* **Actor principal:** Empleado Final (USER)  
* **Precondiciones:** El usuario se encuentra autenticado en el sistema con un token JWT válido.  
* **Flujo principal:**  
  1. 1\. El usuario completa el formulario de solicitud seleccionando categoría, prioridad e ingresando título y descripción.  
  2. 2\. El sistema valida los datos de entrada y registra el ticket en estado ABIERTO.  
  3. 3\. El administrador (ADMIN) revisa la lista de tickets en estado ABIERTA y asigna manualmente el ticket a un técnico.  
  4. 4\. El sistema envía una notificación por correo electrónico al técnico asignado y una confirmación al usuario.  
* **Flujos alternativos / Excepciones:**  
  * **3a. Ticket pendiente de asignación:** El ticket permanece en estado ABIERTA hasta que el ADMIN realice la asignación manual correspondiente.  
* **Resultado esperado:** Ticket registrado correctamente, asignado a un técnico y con notificaciones enviadas.

# **10\. Restricciones**

Define las limitaciones técnicas, legales, normativas u operativas impuestas al proyecto.

* **Tecnologías del Frontend:** Desarrollo exclusivo mediante HTML5, CSS3 y Vanilla JavaScript (ES6+). Queda estrictamente prohibido el uso de frameworks (React, Angular, Vue, Svelte) o librerías CSS/JS adicionales (Tailwind CSS, Bootstrap, jQuery).  
* **Motor de base de datos:** PostgreSQL 15 o superior como único motor de almacenamiento relacional.  
* **Arquitectura y Estructura Backend:** Backend desacoplado en Java 21 con Spring Boot 4.x organizado estrictamente en arquitectura por capas: Controller, Service, Repository, Entity, DTO, Security y Exception (Manejo global de excepciones con ControllerAdvice y respuestas en códigos HTTP estándar).  
* **Normativas y regulación:** Cumplimiento estricto de las políticas internas de seguridad de la información de TechNova Solutions y Habeas Data.  
* **Infraestructura y limitaciones:** Despliegue mediante contenedores Docker con restricciones de memoria de hasta 2GB RAM por instancia.  
* **Seguridad y Documentación:** Autenticación stateless mediante Spring Security y JWT con hash BCrypt para el almacenamiento seguro de contraseñas, y documentación interactiva de la API utilizando OpenAPI / Swagger UI.

# **11\. Criterios de Aceptación**

Define condiciones claras, verificables y medibles requeridas para la aprobación y entrega de cada requisito.

* **CA-001:** La asignación de un técnico a un ticket sólo puede ejecutarse por un usuario con rol ADMIN y debe actualizar automáticamente el estado del ticket de ABIERTA a ASIGNADA.  
* **CA-002:** Las peticiones REST sin un encabezado 'Authorization: Bearer \<token\>' válido deben ser rechazadas con un código de estado HTTP 401 Unauthorized.  
* **CA-003:** El cambio de estado de un ticket debe activar el envío de un correo electrónico al solicitante con el nuevo estado en un lapso no mayor a 5 segundos.

# **12\. Entregables del Proyecto**

Define los artefactos y componentes obligatorios que deben ser entregados para dar por finalizado el proyecto TechNova IT Helpdesk.

* **12.1 Repositorio de Código Fuente:** Repositorio Git estructurado con carpetas independientes para el backend (Spring Boot Java 21 en capas) y el frontend (HTML5/CSS3/Vanilla JS sin dependencias externas).  
* **12.2 Script de Base de Datos SQL:** Scripts DDL/DML para PostgreSQL que incluyan la creación de tablas, restricciones, relaciones, índices y datos iniciales de prueba (usuarios ADMIN, TECHNICIAN, USER y categorías de ejemplo).  
* **12.3 Documentación de Configuración (README.md):** Guía detallada con requisitos del entorno, instrucciones paso a paso para la compilación y ejecución del backend, despliegue del frontend, configuración de variables de entorno (JWT secret, conexión PostgreSQL, SMTP) y acceso a Swagger UI.  
* **12.4 Evidencias y Presentación Funcional:** Capturas de pantalla o video demostrativo del ciclo de vida completo de un ticket, funcionamiento de la máquina de estados, asignación manual por ADMIN y presentación ejecutiva de la solución.

# **13\. Anexos y Glosario**

* **13.1 Glosario de términos y abreviaturas:**  
  * **ERS:** Especificación de Requisitos de Software.  
  * **API:** Interfaz de Programación de Aplicaciones (Application Programming Interface).  
  * **UI / UX:** Interfaz de Usuario / Experiencia de Usuario.  
* **13.2 Diagramas y referencias:** \[Enlaces a diagramas UML, diagramas Entidad-Relación o maquetas en Figma\]  
* **13.3 Documentación adicional y consideraciones complementarias:** \[Notas de versiones previas, referencias a minutas de reunión o acuerdos de nivel de servicio (SLA)\]

