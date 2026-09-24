# Lupa Digital y Amplificador Inteligente (Android)

Una aplicación moderna desarrollada en Kotlin y Jetpack Compose diseñada para transformar tu teléfono en una **lupa digital de alta precisión**. Ideal para leer letras pequeñas, examinar detalles, etiquetas de medicamentos, menús o pequeños objetos con la máxima nitidez.

---

## 🌟 Características Principales

1. **Control de Zoom Avanzado**:
   - Barra deslizante precisa y botones de ajuste rápido (`1x`, `2x`, `3x`, `5x`, `10x`).
   - Gestos táctiles de pellizcar con dos dedos (*pinch-to-zoom*) para acercar o alejar fluidamente.
   - Doble toque para alternar rápidamente entre la vista normal y aumento de 3x.

2. **Selección de Cámara Inteligente**:
   - Menú desplegable integrado en la barra superior junto al título.
   - Detecta automáticamente todas las lentes disponibles en el dispositivo (Cámara principal, Gran Angular, Macro y Frontal).
   - Prioriza por defecto la cámara trasera principal con flash para garantizar la mejor calidad y luminosidad.

3. **Linterna y Flash Trasero**:
   - Botón dedicado de linterna en la barra de acciones inferior.
   - Enciende de forma robusta el flash LED trasero utilizando la API de CameraX y el servicio del sistema (`CameraManager`).
   - Incluye modo de respaldo de iluminación por bordes de pantalla para dispositivos o emuladores sin flash físico.

4. **Congelamiento de Imagen (Captura y Pausa)**:
   - Botón central de disparo para pausar el fotograma actual y leer cómodamente sin fatiga ni temblores en las manos.
   - Permite hacer zoom y desplazar la imagen congelada.

5. **Guías de Lectura y Enfoque**:
   - Enfoque táctil interactivo con retícula animada tocando cualquier punto de la pantalla.
   - Guías de cruce y líneas horizontales de apoyo para alinear y leer texto continuo sin desviarse.

---

## 🛠️ Tecnologías y Arquitectura

- **UI**: Jetpack Compose y Material Design 3 (M3) con diseño oscuro de alto contraste adaptado para accesibilidad visual.
- **Cámara**: Android CameraX (`Preview`, `CameraControl`, `CameraInfo`, `ProcessCameraProvider`) y Camera2 Interop.
- **Arquitectura**: MVVM (Model-View-ViewModel) con flujos reactivos (`StateFlow`).
- **Lenguaje**: Kotlin al 100% con corrutinas.

---

## 📱 Cómo Ejecutar / Instalar

1. Clona o abre este proyecto en **Google AI Studio**.
2. Sincroniza y compila el proyecto mediante Gradle (`compile_applet` o en Android Studio).
3. Conecta un dispositivo Android físico o inicia el emulador integrado para disfrutar de la experiencia de lupa en tiempo real.
