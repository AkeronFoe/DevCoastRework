# DevCoast

Plataforma web que conecta **talento tech** (programación, diseño, datos) con **empresas**: ofertas de empleo con
*match* según el stack, portafolio de proyectos, solicitudes con seguimiento, invitaciones, mensajes, notificaciones,
analítica y planes para empresas.

Stack: Java 17 · Spring Boot 4 (MVC, Security, Thymeleaf) · MongoDB · Cloudinary (imágenes) · HTML/CSS/JS sin framework.

## Cómo correrlo

1. Copia `.env.example` como `.env` y completa `MONGODB_URI` y las credenciales de Cloudinary.
2. `./mvnw spring-boot:run` y abre <http://localhost:8080>.

Pruebas: `./mvnw verify` (necesita un MongoDB accesible en `MONGODB_URI`, como hace el CI).

## Datos de ejemplo

Con `SEED_ENABLED=true` la app crea, la primera vez que arranca, un conjunto grande y coherente de datos de ejemplo:
unos 8.000 talentos, 2.000 empresas, ~7.600 ofertas, ~11.600 proyectos, ~21.500 solicitudes con historial, chats,
notificaciones, invitaciones y cobros: más de 80.000 documentos (configurable con `SEED_TALENTOS` y `SEED_EMPRESAS`).
Las fechas son relativas a hoy y los ids llevan la fecha real de cada registro, así "nuevo esta semana" y el orden
por recientes se ven como en una plataforma en uso.

- Corre en segundo plano y por lotes: la app responde mientras se siembra (toma unos segundos).
- Es **idempotente**: deja una marca en la colección `seed_meta` y no vuelve a sembrar. Solo agrega datos nuevos;
  nunca modifica ni borra los existentes.
- No envía correos ni usa servicios externos; las direcciones, teléfonos y enlaces son ficticios.
- Si una siembra se interrumpe, no reintenta sola. Para reintentar (borra únicamente lo que esa siembra creó):
  arranca con `devcoast.seed.reintentar=true` (`DEVCOAST_SEED_REINTENTAR=true`).

**Si tu base ya tiene los datos de ejemplo del seeder anterior** (desactualizados), arranca la primera vez con
`SEED_ENABLED=true` y `SEED_LIMPIAR_ANTIGUOS=true`: antes de sembrar borra solo lo que reconoce de ese seeder
(talentos con `portfolio.<usuario>.dev`, sus empresas de descripción fija, la demo vieja y todo lo que colgaba de eso:
proyectos, ofertas, solicitudes, chats, notificaciones, invitaciones, cobros y favoritos). Cuentas creadas por personas
de verdad no se tocan. Quita `SEED_LIMPIAR_ANTIGUOS` después de esa primera vez.

Cuentas de demo (contraseña `demo1234`): `demo@devcoast.co` y `diseno@devcoast.co` (talentos),
`empresa@devcoast.co` (plan PRO) y `startup@devcoast.co` (plan gratis). El resto de cuentas usa `devcoast123`.

## Estructura

- `config/` seguridad (sesión + `RoleGuard`), índices de Mongo, interceptor de notificaciones y `DataSeeder`.
- `controller/` páginas y endpoints; las clases `*Vista` preparan lo que muestran las plantillas.
- `service/` reglas de negocio (match entre perfil y oferta, planes, limpieza en cascada).
- `templates/` Thymeleaf; `fragments/nautico.html` trae el shell (menú, barra superior) y `fragments/pase.html` las tarjetas.
- `static/CSS` y `static/JS`: el sistema visual náutico está en `nautico*.css` (diseño en `docs/diseno/`).

## Notas de rendimiento y operación

- Los estáticos se sirven con URL versionada por contenido y caché larga; con devtools no se cachean.
- Los índices de Mongo se crean al arrancar (`MongoIndexInitializer`); si alguno ya existe con otro nombre solo se avisa en el log.
- La sesión expira tras 7 días de inactividad (`server.servlet.session.timeout`).
