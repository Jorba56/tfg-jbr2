# 🏎️ FastFingers

![FastFingers Banner](front/static/images/android-chrome-512x512.png) **FastFingers** es una plataforma web competitiva de mecanografía y agilidad mental diseñada bajo una arquitectura de microservicios. Combina mecánicas de *e-sports* (competición 1v1 en tiempo real), progresión de usuarios mediante economía virtual y generación de contenido dinámico a través de Inteligencia Artificial.

Este proyecto ha sido desarrollado como Trabajo de Fin de Grado (TFG) para el C.F.G.S. de Desarrollo de Aplicaciones Web en el **I.E.S. Ágora**, por **Jorge Barriga Rubio**.

---

## Funcionalidades Principales

* **Generación de Contenido con IA:** Integración directa con la API de Google Gemini para crear partidas temáticas ("Partida a Medida" y "El Rosco") de forma dinámica. Nunca jugarás dos partidas iguales.
* **Multijugador en Tiempo Real (Versus):** Sistema de salas privadas y sincronización de barras de progreso al milisegundo utilizando WebSockets (STOMP).
* **Progresión y Economía:** Los jugadores ganan créditos según su Precisión y Pulsaciones Por Minuto (PPM), que pueden invertir en una tienda virtual para adquirir *power-ups* (como congelar el tiempo) y avatares cosméticos.
* **Salón de la Fama:** Clasificación global (Rankings) persistida en base de datos.
* **Seguridad Integrada:** Autenticación fluida y segura mediante *JSON Web Tokens* (JWT).

---

##  Stack Tecnológico y Arquitectura

El proyecto está diseñado para ser escalable, resiliente y altamente interactivo, dividiendo responsabilidades entre el Frontend y un ecosistema de microservicios Backend.

###  Frontend (Cliente)
* **Vanilla JavaScript (ES6+):** Arquitectura *Single Page Application* (SPA) modular, sin recargas de página y manipulando el DOM dinámicamente.
* **CSS3 Avanzado:** Diseño estético *Cyberpunk / Racing* con variables globales, Flexbox, Grid y *Media Queries* (Mobile-First).
* **WebSockets:** Cliente STOMP y SockJS para comunicaciones Full-Duplex.

###  Backend (Microservicios)
* **Java 21 & Spring Boot 4:** Framework base para todos los microservicios.
* **Spring Cloud Gateway & Eureka:** API Gateway para enrutamiento seguro, resolución de CORS centralizada y descubrimiento de servicios.
* **Spring Security & JWT:** Protección de endpoints y validación de usuarios.
* **Programación Orientada a Aspectos (AOP):** Interceptores para auditoría global y registro de excepciones (`@AfterThrowing`) sin contaminar la lógica de negocio.
* **Spring WebSocket:** Implementación del protocolo STOMP como *Message Broker* en memoria.

### ️ Persistencia y Despliegue
* **MySQL & Spring Data JPA:** Base de datos relacional para la persistencia transaccional (usuarios, inventarios, puntuaciones).
* **Render / Railway:** Despliegue en la nube (*Cloud Hosting*) para los servicios y bases de datos.

---

##  Instalación y Despliegue en Local

Si deseas probar el proyecto en un entorno de desarrollo local, sigue estos pasos:

### Prerrequisitos
* Java 21 o superior.
* Maven.
* MySQL Server (o Docker para levantar la base de datos).
* Una API Key válida de Google Gemini.

### Configuración del Backend
1. Clona este repositorio:
   ```bash
   git clone https://github.com/Jorba56/tfg-jbr2.git