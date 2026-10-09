# NexoVentas Desktop

Cliente de escritorio JavaFX que consume la API pública de NexoVentas. Permite iniciar sesión con JWT, consultar productos y detectar alertas de stock mínimo.

## Ejecutar

```powershell
.\mvnw.cmd javafx:run
```

La API de producción se configura por defecto. Para usar otra URL:

```powershell
$env:NEXOVENTAS_API_URL = "https://tu-api.onrender.com"
.\mvnw.cmd javafx:run
```

Nunca incluyas contraseñas en código ni variables versionadas: ingresalas únicamente en la pantalla de inicio de sesión.
