# 🚀 FastFingers: Plataforma de Mecanografía Gamificada y Competitiva

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-Cloud-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JavaScript](https://img.shields.io/badge/Vanilla_JS-ES6+-yellow.svg)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**FastFingers** es una plataforma web interactiva Full-Stack orientada al aprendizaje y perfeccionamiento de la mecanografía mediante dinámicas de juego competitivas (*gamification*). Este proyecto constituye el Trabajo de Fin de Grado (TFG) del **Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Web (DAW)**.

---

## 🏗️ Arquitectura del Sistema

El proyecto abandona las estructuras monolíticas tradicionales en favor de una **Arquitectura de Microservicios** distribuida y de alta disponibilidad:

- **API Gateway:** Punto único de entrada que centraliza el enrutamiento, gestiona las políticas de seguridad perimetral y resuelve de forma unificada el intercambio de recursos de origen cruzado (CORS).
- **Servicio de Descubrimiento (Eureka):** Permite el registro dinámico y la comunicación fluida entre los distintos microservicios.
- **Backend (Spring Boot / Cloud):** Estructura modular basada en controladores, servicios e interfaces de persistencia mediante Spring Data JPA. Incorpora seguridad robusta con **Spring Security y JSON Web Tokens (JWT)**.
- **Frontend (Single Page Application):** Desarrollado íntegramente en **Vanilla JavaScript (ES6+)**, prescindiendo de frameworks pesados para garantizar un control granular sobre el DOM, un rendimiento óptimo y una gestión asíncrona fluida.
- **Base de Datos (MySQL):** Modelo relacional optimizado para la persistencia de usuarios, roles (RBAC), telemetría de partidas, inventarios y economía virtual.

---

## ⚙️ Características Técnicas Destacadas

1. **Comunicaciones Full-Duplex en Tiempo Real (WebSockets)**
   - El "Modo Versus" (multijugador 1v1) prescinde del tradicional polling HTTP mediante la implementación del protocolo **STOMP sobre WebSockets**.
   - El servidor actúa como un *Message Broker* para sincronizar cuentas atrás, eventos de partida y barras de progreso cruzadas con una latencia de milisegundos. Cuenta con gestión de desconexiones para proteger la sesión del jugador activo ante caídas de red.

2. **Integración Resiliente de Inteligencia Artificial (Google Gemini)**
   - El modo "Partida a Medida" consume la API de **Google Gemini** mediante flujos transaccionales reales.
   - Se ha implementado un estricto control de *Prompt Engineering* junto con un sistema de **Fallback**: si la API externa sufre latencia o agota su cuota, el servidor inyecta de forma transparente un conjunto de frases locales predefinidas, garantizando una disponibilidad del 100%.

3. **Auditoría Transversal mediante Programación Orientada a Aspectos (AOP)**
   - El rastreo de excepciones y la auditoría de negocio operan de forma invisible. Mediante la definición de *Pointcuts* y `@AfterThrowing`, cualquier error en la capa de servicios es interceptado y persistido automáticamente en la base de datos sin interrumpir la experiencia del usuario.

---

## 📦 Estructura del Repositorio

```text
tfg-jbr2/
├── back/
│   ├── api-gateway/         # Enrutamiento y seguridad perimetral
│   ├── eureka-server/       # Servidor de descubrimiento
│   ├── incidencias-service/ # Microservicio de control de errores y logs (AOP)
│   └── usuarios-service/    # Lógica de negocio, autenticación JWT y juego
├── front/
│   ├── html/                # Vistas estructuradas para la SPA
│   ├── js/                  # Módulos de lógica de cliente, API y WebSockets
│   └── static/               # Recursos visuales, estilos CSS y avatares
└── README.md
```

---

## 🛠️ Guía de Puesta en Marcha (Desarrollo Local)

### Prerrequisitos

- Java JDK 17 o superior
- Apache Maven
- Servidor MySQL

### 1. Configuración de la Base de Datos

Crea una base de datos en tu servidor MySQL local y configura las credenciales de conexión en los archivos `application.properties` correspondientes dentro de cada microservicio del directorio `back/`.

### 2. Ejecución del Backend

Levanta los servicios en el siguiente orden estricto para garantizar el correcto direccionamiento:

1. **Eureka Server** (Servidor de descubrimiento)
2. **API Gateway** (Punto de entrada)
3. **Microservicios** (`usuarios-service` e `incidencias-service`)

Puedes arrancarlos mediante Maven utilizando la terminal:

```bash
mvn spring-boot:run
```

### 3. Ejecución del Frontend

Al tratarse de una aplicación estática basada en una SPA (Single Page Application), puedes servir el directorio `front/html/index.html` utilizando cualquier servidor local (como la extensión Live Server de VS Code o un servidor estático de Node.js).

---

## 📊 Diagrama Entidad-Relación (Resumen)

- **usuarios**: Núcleo del sistema (credenciales, telemetría y configuración).
- **partidas**: Almacena el histórico de puntuaciones, precisión y créditos obtenidos.
- **items / cosmeticos**: Soporte para la economía del juego, power-ups y personalización de interfaz.
- **incidencias**: Registro automático de excepciones del sistema capturadas mediante AOP.

---

Desarrollado por **Jorge Barriga Rubio** como parte del proyecto de titulación en el **I.E.S. Ágora (Cáceres)**.
