# Generador de Exámenes - Backend API (Arquitectura de Microservicios)

Esta es la API RESTful del backend para el proyecto "Generador de Exámenes", desarrollada con Java y Spring Boot.

Este repositorio contiene la evolución del proyecto hacia una arquitectura basada en **Microservicios**. Tras asentar unas bases sólidas de CRUD y seguridad en las primeras fases, el objetivo ha sido dividir el dominio (Usuarios e Incidencias), orquestar el tráfico mediante un API Gateway, contenerizar toda la infraestructura (incluyendo la base de datos) y aplicar prácticas de Integración y Entrega Continua (CI/CD).

## Características destacadas

* **Arquitectura Distribuida y API Gateway:** División del sistema en microservicios independientes comunicados por red interna. Un **API Gateway** reactivo actúa como único punto de entrada público, centralizando el enrutamiento y la configuración de CORS y Swagger.
* **Contenedorización y Calidad de Código:** Uso de **Docker y Docker Compose** para desplegar de forma predecible toda la infraestructura. Integración con **Jenkins** para automatización de *builds* y **SonarQube** para análisis estático y garantía de código limpio.
* **Seguridad integral con JWT (Spring Security):** Implementación de un sistema de autenticación sin estado (stateless). Las rutas están fuertemente protegidas mediante Control de Acceso Basado en Roles (RBAC) y validaciones de negocio estrictas (un usuario normal solo puede editar su perfil, un admin no puede ver contraseñas en claro).
* **Subsistema Automático de Incidencias:** Un interceptor global (`@RestControllerAdvice`) captura cualquier excepción del sistema o error de negocio, devuelve un JSON estandarizado al cliente y guarda de forma silenciosa un registro detallado en su propio microservicio dedicado (Clase, Método, Stacktrace parcial y Usuario responsable).
* **Logs Centralizados no intrusivos:** Uso de Programación Orientada a Aspectos (Spring AOP) para interceptar las llamadas a los servicios y registrar los tiempos de ejecución en milisegundos mediante la API nativa `java.util.logging`, escupiendo los datos rotativamente a un archivo `.txt` local.
* **Gestión de Usuarios y Roles:** CRUD completo con borrado lógico (soft delete) para mantener el histórico de datos intacto, usando DTOs y MapStruct para aislar las entidades de la base de datos de la vista pública. Configuración estricta en `snake_case` para el intercambio de JSON.
* **Alta cobertura de tests:** Pruebas unitarias avanzadas con JUnit y Mockito, mockeando incluso el contexto de seguridad (`Authentication`) para validar las reglas de negocio.

## Arquitectura

El ecosistema sigue una estructura de microservicios contenerizados para mantener el código desacoplado y altamente escalable:
1. **API Gateway:** Único punto de acceso público (`localhost:8080`). Recibe las peticiones HTTP y las enruta a los microservicios correspondientes.
2. **Microservicio Usuarios:** Centraliza la lógica de identidades, autenticación, tokens JWT, roles y validaciones de acceso.
3. **Microservicio Incidencias:** Actúa como auditor, centralizando la persistencia del historial de errores.
4. **Base de Datos:** Instancia de MySQL dockerizada e independiente.

*(A nivel interno, cada microservicio mantiene la estructura multicapa limpia: Controllers, Services, Repositories y Security/AOP).*

---

## Endpoints de la API

> **Nota:** Todas las peticiones deben realizarse a través del puerto del API Gateway (`http://localhost:8080`).

### Autenticación (/auth) - *Públicos*
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| POST | `/auth/login` | Valida credenciales y devuelve un Token JWT de sesión. |
| POST | `/auth/register` | Registra un nuevo usuario en el sistema (Rol ALUMNO por defecto). |

### Usuarios (/usuarios) - *Protegidos*
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| GET | `/usuarios` | Obtiene la lista de todos los usuarios activos. |
| GET | `/usuarios/{id}` | Busca un usuario concreto por su ID. |
| POST | `/usuarios` | Crea un nuevo usuario. |
| PUT | `/usuarios/{id}` | Modifica los datos del usuario (Aplica reglas ABAC según el Token). |
| DELETE | `/usuarios/{id}` | Realiza un borrado lógico del usuario. |

### Roles (/roles) - *Solo Administradores*
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| GET | `/roles` | Obtiene todos los roles activos del sistema. |
| GET | `/roles/{id}` | Busca un rol por su ID. |
| POST | `/roles` | Crea un nuevo rol. |
| PUT | `/roles/{id}` | Modifica el nombre o estado de un rol. |
| DELETE | `/roles/{id}` | Realiza un borrado lógico del rol. |

### Relaciones Usuario-Rol - *Solo Administradores*
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| POST | `/usuarios/{id}/roles` | Asigna un rol específico a un usuario. |
| DELETE | `/usuarios/{idUsuario}/roles/{idRol}` | Le quita un rol a un usuario. |
| GET | `/usuarios/{id}/roles` | Consulta todos los roles que tiene asignados un usuario. |
| GET | `/roles/{idRol}/usuarios` | Consulta qué usuarios poseen un rol concreto. |
| GET | `/usuarios_roles` | Obtiene una lista plana optimizada con todas las asignaciones. |

### Incidencias (/incidencias) - *Solo Administradores*
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| GET | `/incidencias` | Obtiene el historial completo de errores de la API. |
| GET | `/incidencias/{id}` | Busca una incidencia por ID (Protegido con Circuit Breaker). |
| GET | `/incidencias/usuario/{idUsuario}` | Historial de errores provocados por un usuario específico. |
| GET | `/incidencias/clase/{clase}` | Filtra incidencias originadas en una clase concreta. |
| GET | `/incidencias/metodo/{metodo}` | Filtra incidencias originadas en un método concreto. |

---

## Stack Tecnológico

* Java 21+
* Spring Boot 4.0.2
* **Spring Cloud Gateway (WebFlux)**
* **Docker y Docker Compose**
* **Jenkins y SonarQube**
* **Spring Security + JWT (io.jsonwebtoken)**
* **Spring AOP (AspectJ)**
* Spring Data JPA / MySQL
* MapStruct
* JUnit 6 y Mockito
* Maven

---

## Cómo ejecutar el proyecto en local

1. Clona este repositorio en tu equipo:
   ```bash
   git clone [https://github.com/Jorba56/generador-examenes-back.git](https://github.com/Jorba56/generador-examenes-back.git)

2. Compila el API Gateway y los microservicios desde la raíz:
   ```bash
   ./mvnw clean package -DskipTests

3. Levanta la infraestructura completa mediante Docker Compose:
   ```bash
   docker-compose up -d --build
   
4. Accede a la documentación centralizada de Swagger a través del Gateway en: 
   ```bash
   http://localhost:8080/swagger-ui.html