# NexoVentas Desktop

Cliente de escritorio de gestión comercial construido con JavaFX. Consume la API REST pública de NexoVentas y muestra productos, inventario y alertas de stock desde una interfaz de escritorio.

## Funcionalidades

- Inicio de sesión contra la API con JWT.
- Consulta de productos en tiempo real.
- Indicadores de productos registrados y alertas de stock mínimo.
- Actualización manual de datos y manejo de errores de red.
- URL de API configurable sin modificar el código.

## Arquitectura

```text
JavaFX Desktop → HTTP Client Java → NexoVentas API (Spring Boot / Render) → PostgreSQL (Neon)
```

## Tecnologías

Java 21, JavaFX, Java HTTP Client, Jackson, Maven, Spring Boot, JWT, PostgreSQL, Neon y Render.

## Requisitos

- JDK 21.
- Acceso a la API de NexoVentas.

El proyecto incluye Maven Wrapper, así que no necesitás instalar Maven de forma global.

## Ejecutar

En PowerShell:

```powershell
.\mvnw.cmd javafx:run
```

La aplicación usa por defecto `https://nexoventas-api.onrender.com`. Ingresá con las credenciales de administrador configuradas como variables de entorno en Render.

> El primer acceso puede demorar si el servicio gratuito de Render está iniciando después de un período sin uso.

## Configurar otra API

```powershell
$env:NEXOVENTAS_API_URL = "https://tu-api.onrender.com"
.\mvnw.cmd javafx:run
```

## API relacionada

- [Swagger UI](https://nexoventas-api.onrender.com/swagger-ui.html)
- [Repositorio del backend](https://github.com/H1r0waz/nexoventas-ap)

## Seguridad

El cliente no guarda contraseñas ni tokens en archivos. Las credenciales se ingresan en la pantalla de inicio de sesión y el token JWT se mantiene únicamente en memoria durante la ejecución.
