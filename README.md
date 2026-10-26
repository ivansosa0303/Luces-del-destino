# Luces del Destino

Aplicación Android offline para escribir y estructurar novelas por capítulos.
Todo el texto vive en el dispositivo (Room/SQLite). Firebase es opcional: sin
`google-services.json` la app funciona igual, sin sincronización en la nube.

## Qué incluye

- **Editor de capítulos** con autoguardado, deshacer/rehacer, buscar y
  reemplazar y modo enfoque.
- **Suite editorial**: repeticiones, muletillas, proporción de diálogo,
  legibilidad y correcciones de formato en un toque.
- **Biblia de la novela**: personajes, escenarios y tramas secundarias
  editables.
- **Progreso y metas diarias** calculados con un único conteo de palabras.
- **Exportación de manuscrito**: PDF maquetado (portada, índice, un capítulo
  por página y numeración) y DOCX para Word, además de Markdown, TXT y JSON.
- **Respaldo y restauración local en JSON** con capítulos, fichas y metas.

## Compilar

Requisitos: JDK 21, Android SDK con `platforms;android-36.1` y
`build-tools;36.1.0`.

```bash
export JAVA_HOME=/ruta/al/jdk-21
export ANDROID_HOME=/ruta/al/android-sdk
./gradlew :app:assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Los tests de lógica (motor editorial y métricas de texto) corren en cualquier
equipo. Las clases Robolectric (UI y conversores) necesitan el runtime nativo de
Robolectric, que no se publica para Linux/aarch64 (Termux, proot). En ese
entorno Gradle las omite automáticamente para no reportar fallos de plataforma;
en x86_64 se ejecutan normalmente. Para forzarlas:
`./gradlew :app:testDebugUnitTest -PforceRobolectric=true`.

### Notas para ARM64 (Termux/proot)

- El `aapt2` oficial de `build-tools` es x86_64. En un host aarch64 hay que
  usar un binario `aapt2` de ARM64 y pasarlo con
  `-Pandroid.aapt2FromMavenOverride=/ruta/a/aapt2`.
- Usa un JDK **glibc** (por ejemplo el de Ubuntu). El JDK de Termux (bionic) no
  puede cargar la SQLite nativa que Room usa durante KSP.

## Respaldo

En la pantalla de novelas:

- **Respaldar** genera un `.json` con todas las novelas y abre el menú de
  compartir (Drive, correo, WhatsApp, etc.).
- **Restaurar** importa ese `.json` y crea copias con identificadores nuevos,
  sin sobrescribir lo que ya existe.

Haz un respaldo antes de desinstalar la app: los datos locales se pierden al
borrar la aplicación.

## Instalar el APK

El APK de depuración se firma con el `debug.keystore` incluido en el
repositorio, de modo que las actualizaciones se instalan encima de la versión
anterior sin desinstalar ni perder datos.

Si la app que tienes instalada se firmó con otra clave, Android rechazará la
actualización (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). En ese caso hay que
desinstalar, y desinstalar borra la base de datos local: exporta antes un
respaldo desde la propia app o copia el archivo de datos si tienes acceso root.

