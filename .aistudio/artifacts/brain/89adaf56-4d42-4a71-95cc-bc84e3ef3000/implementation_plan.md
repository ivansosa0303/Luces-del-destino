# Plan de Implementación: Suite Editorial y de Historias con Estética Nocturna Esmeralda

Este plan aborda la optimización estética y funcional de la aplicación con una paleta de contraste oscuro nocturno con acentos esmeralda, junto con la creación de una **Suite Editorial Completa para Escritores y Creadores de Historias**, especializada en novelas, división y búsqueda de capítulos, análisis de estilo, ortografía, fichas narrativas (personajes y escenarios), métricas de lectura y compilación a múltiples formatos.

---

## 1. Arquitectura y Modelo de Datos

### 1.1 Esquema de Base de Datos Local (Room)
- **`StoryProjectEntity` / `ProjectEntity` Extendido**:
  - `id`: Clave primaria autoincremental.
  - `title`: Título de la obra o novela.
  - `synopsis`: Sinopsis o premisa narrativa.
  - `genre`: Género literario (Fantasía, Ciencia Ficción, Novela Negra, Romance, Histórico, etc.).
  - `targetWordCount`: Meta total de palabras del libro.
  - `currentWordCount`: Conteo acumulado de palabras.
  - `status`: Estado del proyecto (`Borrador`, `En revisión`, `Pulido`, `Listo para maquetar`).
  - `colorTag`: Código de color identificador de la novela.
  - `createdAt` / `updatedAt`: Marcas de tiempo para ordenación y sincronización.

- **`ChapterEntity` (Capítulos del Proyecto)**:
  - `id`: Clave primaria autoincremental.
  - `projectId`: Clave foránea al proyecto de la historia.
  - `chapterNumber`: Índice numérico ordenado (1, 2, 3...).
  - `title`: Título o encabezado del capítulo.
  - `content`: Texto completo del capítulo en Markdown o texto enriquecido.
  - `summary`: Resumen o notas de escena.
  - `wordCount`: Número de palabras del capítulo.
  - `status`: Estado del capítulo (`Esquema`, `Primer borrador`, `Revisado`, `Definitivo`).
  - `povCharacter`: Personaje con punto de vista (POV) si aplica.

- **`StoryBibleEntity` (Fichas Narrativas y Lore)**:
  - `id`: Clave primaria autoincremental.
  - `projectId`: Clave foránea al proyecto.
  - `type`: Categoría (`PERSONAJE`, `ESCENARIO_LUGAR`, `TRAMA_SECUNDARIA`, `OBJETO_CLAVE`).
  - `name`: Nombre del elemento (ej. "Elena Vance", "Fortaleza de Eldoria").
  - `role`: Rol o función (ej. "Protagonista", "Antagonista", "Escenario principal").
  - `description`: Descripción física, trasfondo o reglas.
  - `notes`: Notas adicionales y conexiones con capítulos.

---

## 2. Rediseño Estético: Paleta Nocturna de Alto Contraste con Esmeralda

- **Fondo y Superficies**:
  - `background`: `#090D10` (Negro abisal/obsidiana profundo para máxima comodidad de lectura nocturna).
  - `surface`: `#11181D` (Gris petróleo oscuro con contraste limpio).
  - `surfaceContainer`: `#182229` (Contenedores elevados para tarjetas y paneles).
  - `surfaceVariant`: `#212E37` (Bordes y delimitadores sutiles).
- **Acentos Esmeralda y Semánticos**:
  - `primary`: `#10B981` (Esmeralda vibrante para acciones principales, botones de guardado y progreso).
  - `primaryContainer`: `#064E3B` (Esmeralda profundo para destacar selecciones activas).
  - `secondary`: `#34D399` (Menta esmeralda claro para estados secundarios y etiquetas).
  - `tertiary`: `#38BDF8` (Azul cielo para capítulos y referencias de lectura).
  - `accentAmber`: `#F59E0B` (Ámbar para alertas de estilo, advertencias y muletillas).
- **Tipografía y Legibilidad**:
  - Tipografía serif/sans optimizada para lectura prolongada, espaciado generoso entre líneas en el editor y tarjetas con bordes redondeados (16.dp).

---

## 3. Módulos y Funcionalidades de la Suite Editorial

### 3.1 Gestor de Historias y Navegación de Capítulos
- **Vista de Novela y Capítulos**:
  - Listado ordenable de capítulos con su número, título, conteo de palabras y etiqueta de estado.
  - Búsqueda en tiempo real por título de capítulo, palabras clave dentro del contenido o estado.
  - Creación rápida de capítulos y reordenamiento de escenas.
  - Barra de progreso general de la novela (palabras escritas vs. meta objetivo).

### 3.2 Editor y Suite de Pulido de Texto
- **Corrector de Ortografía, Puntuación y Formato**:
  - Detección de errores típicos de maquetación en narrativa española: uso de guiones cortos en vez de guión largo o raya de diálogo (`—`), puntuación incorrecta en incisos de diálogo, comillas inglesas vs. latinas (« »).
  - Limpieza de dobles espacios, saltos de línea huérfanos y sangrías desalineadas con 1 solo toque.
- **Detector de Palabras Repetidas y Estilo**:
  - Detección de palabras repetidas en párrafos cercanos (eco léxico / rimas internas involuntarias).
  - Identificación de muletillas habituales (ej. *obviamente*, *de repente*, *muy*, *un poco*).
  - Indicador de longitud de oraciones para prevenir frases excesivamente subordinadas o farragosas.
- **Métricas de Lectura y Ritmo**:
  - Tiempo estimado de lectura por capítulo y total de la novela (calculado a ~200 palabras/minuto).
  - Balance diálogo vs. narración (porcentaje de texto en diálogos).
  - Registro de palabras diarias escritas para mantener el hábito de escritura.

### 3.3 Fichas Narrativas (Biblia del Proyecto)
- Pestaña para gestionar fichas de personajes principales/secundarios, escenarios del mundo y líneas argumentales secundarias.
- Buscador rápido dentro del editor para consultar detalles de personajes sin salir del capítulo.

### 3.4 Compilador y Exportador de la Novela
- Herramienta para compilar todos los capítulos en orden hacia un único documento libro:
  - Formato **Markdown** con títulos de capítulos y jerarquía estandarizada.
  - Formato **TXT** maquetado para manuscrito limpio.
  - Formato **JSON** para respaldo o importación.
  - Formato **PDF** con diseño de portada, índice de capítulos y numeración de páginas.

---

## 4. Fases de Ejecución

1. **Fase 1: Capa de Datos y Modelos**:
   - Agregar las entidades `StoryProjectEntity`, `ChapterEntity` y `StoryBibleEntity` al `AppDatabase` Room con sus DAOs correspondientes.
   - Proveer repositorios con soporte de Flow y métodos de análisis léxico y búsqueda.

2. **Fase 2: Motor de Corrección y Análisis de Estilo**:
   - Crear `StoryEditorialEngine.kt`: algoritmos en Kotlin puro para detección de repeticiones, verificación de rayas de diálogo, balance narrativo y cálculo de legibilidad.

3. **Fase 3: Actualización del Tema y Paleta Esmeralda Nocturna**:
   - Actualizar `Theme.kt` con la paleta de contraste oscuro nocturno y acentos esmeralda.
   - Refinar `AppBaseScaffold` y barras de navegación para reflejar los tonos esmeralda y el estado de la historia activa.

4. **Fase 4: Pantalla de Proyectos de Historias y Capítulos**:
   - Desarrollar la pantalla `StoryProjectDetailScreen.kt` con lista de capítulos, barra de búsqueda, métricas de novela y acceso a fichas narrativas.
   - Desarrollar `ChapterEditorScreen.kt` con panel lateral/inferior de revisión de estilo y ortografía en tiempo real.

5. **Fase 5: Pruebas y Verificación**:
   - Compilación con `compile_applet`.
   - Pruebas unitarias/Robolectric que verifiquen el análisis de estilo, guardado de capítulos y búsqueda.

---

## 5. Próximo Paso
- Haz clic en **Proceed** para comenzar la ejecución de las fases planificadas.
