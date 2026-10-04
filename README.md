# PassVaultSec 🔐

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-13%2B%20(API%2033--35)-green.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Crypto-AES--256--GCM%20%2B%20Keystore-red.svg?logo=security)](https://developer.android.com/training/articles/keystore)
[![Version](https://img.shields.io/badge/Release-v1.0.0%20(MVP%20Finalizado)-purple.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

> **Aplicación nativa para Android 13 y posteriores (API 33+)** que combina la simplicidad, dinamismo y fluidez de un **gestor de notas visual moderno** con una **capa de seguridad militar (cifrado AES-256-GCM + Android Keystore)**, bloqueo granular por **huella dactilar o PIN** (`BiometricPrompt`) con **rebloqueo inmediato Zero-Trust**, soporte multimedia completo (fotos, cámara, GIFs, enlaces enriquecidos con OpenGraph, emojis), y la posibilidad de **compartir notas colaborativas en tiempo real** mediante Google Sign-In (Firebase Auth + Cloud Firestore).

---

## 🚀 Características Principales (MVP v1.0.0)

### 1. Experiencia de Usuario Moderna, Dinámica y Enriquecida
- **Diseño Moderno Material You / Material Design 3**: Adaptación fluida a Modo Claro, Modo Oscuro y Tema del Sistema persistido en `SharedPreferences` con `StateFlow`.
- **Márgenes del Sistema Perfectos (`WindowInsets`)**: La barra de búsqueda flotante respeta la barra de estado de Android (reloj, batería, notch y red con `.statusBarsPadding()`), y los botones flotantes y teclado respetan las barras de navegación (`.navigationBarsPadding()` e `.imePadding()`).
- **Vista Flexible**: Alterna con un toque entre **cuadrícula escalonada (*staggered grid*)** de 2 columnas o lista vertical continua.
- **Paleta de Colores de Fondo y Contraste Automático Inteligente**:
  - Paleta de notas en tonos pastel para modo claro y pastel oscuro para modo oscuro.
  - **Cálculo de Contraste Automático WCAG (ITU-R BT.709)**: Al cambiar el color de una nota, el texto y elementos secundarios calculan su luminancia relativa en tiempo real para ser 100% legibles en cualquier tono.
  - **Selector Manual de Color de Texto**: Paleta integrada para fijar colores específicos de texto o volver al modo automático con un toque.
- **Notas de Texto y Listas de Verificación (*Checklists*)**: Casillas interactivas, tareas tachadas y alternancia dinámica entre texto libre y lista.
- **Fijado, Archivo y Búsqueda Instantánea**: Fija notas prioritarias en la parte superior, archiva sin eliminar y busca en tiempo real por título, texto o etiquetas.

---

### 2. Soporte Multimedia y Enriquecimiento de Contenido
- **Captura Directa con Cámara**: Botón de cámara integrado con `FileProvider` seguro que guarda fotos en alta resolución en el sandbox privado de la app (`context.filesDir/note_images/`).
- **Galería de Imágenes & GIFs Animados**: Integración con el Photo Picker nativo de Android 13+ y motor de renderizado acelerado por hardware **Coil 2.7.0** (`coil-compose` y `coil-gif`).
- **Barra de Emoticones Rápidos**: Inserción inmediata de emojis frecuentes (`🔒`, `🔑`, `🛡️`, `📝`, `💡`, `📌`, `⭐`, `⚠️`, `💰`, `🏠`, `💼`, `🎯`, `🚀`, etc.) con un solo toque.
- **Vistas Previas Enriquecidas de Enlaces Web (Rich Link Previews)**:
  - Extractor asíncrono OpenGraph (`UrlMetadataExtractor`) que obtiene miniatura (`og:image`), título (`og:title`) y descripción.
  - Tarjetas interactivas con dominio que abren el navegador seguro con un toque (`LocalUriHandler`).

---

### 3. Capa de Seguridad Militar y Privacidad Zero-Trust
- **Cifrado AES-256-GCM con Android Keystore**:
  - Claves criptográficas generadas y resguardadas dentro del hardware seguro (**TEE / StrongBox**) del dispositivo, garantizando que nunca queden expuestas en memoria o almacenamiento ordinario.
  - Vector de inicialización (IV) criptográfico aleatorio de 12 bytes por cada nota.
- **Bloqueo Granular por Nota (`BiometricPrompt`) con Rebloqueo Inmediato**:
  - Puedes proteger notas individuales con un candado.
  - En el tablero general, las notas bloqueadas enmascaran su título como *"Nota Protegida"* y ocultan totalmente su contenido, imágenes o enlaces.
  - **Rebloqueo Inmediato**: Cada acceso a una nota protegida exige huella o PIN. En cuanto el usuario sale o retrocede de la nota, esta se bloquea de inmediato sin retener credenciales en memoria.
- **Protección contra Capturas de Pantalla (`FLAG_SECURE`)**:
  - Al abrir y editar notas confidenciales, la ventana activa `FLAG_SECURE`, impidiendo capturas de pantalla, grabaciones o fugas en el selector de aplicaciones recientes.

---

### 4. Colaboración en Tiempo Real y Cuentas de Google
- **Google Sign-In con Credential Manager**: Integración moderna en un toque para Android 13+.
- **Sincronización Cloud Firestore**:
  - **Propietario (*Owner*)**: Control total para añadir o remover colaboradores y eliminar la nota.
  - **Editor**: Puede leer y modificar contenido en tiempo real.
  - **Lector (*Viewer*)**: Acceso de solo lectura protegido contra ediciones.
- **Reglas del Servidor (`firestore.rules`)**: Validación criptográfica de identidad y permisos en la nube.

---

## 🛡️ Blindaje de Seguridad para Repositorios Públicos (GitHub Zero-Leak Guarantee)

Este repositorio está configurado bajo la regla de oro de **cero fuga de secretos**:

| Archivo / Recurso | Tipo de Contenido | Estado en Git | Razón de Seguridad |
| :--- | :--- | :--- | :--- |
| [`app/google-services.json`](app/google-services.json) | Identificadores de Firebase y Clientes OAuth | **Ignorado en [`.gitignore`](.gitignore)** | No expone configuraciones reales del proyecto en GitHub. |
| `secrets.properties` | `WEB_CLIENT_ID` y claves de API | **Ignorado en [`.gitignore`](.gitignore)** | Aislamiento estricto de secretos locales del desarrollador. |
| `*.keystore` / `*.jks` | Certificados de firma digital | **Ignorado en [`.gitignore`](.gitignore)** | Imposibilidad de filtración de claves de firma. |
| `*.apk` | Binarios ejecutables compilados | **Ignorado en [`.gitignore`](.gitignore)** | Limpieza del repositorio e integridad de binarios. |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías |
| :--- | :--- |
| **Plataforma** | Android 13+ (`minSdk = 33`, `targetSdk = 35`, `compileSdk = 35`) |
| **Lenguaje y Runtime** | Kotlin 2.2.10 (Toolchain Java 21) + Coroutines & Flow |
| **Build System** | Gradle 9.6.0 + Android Gradle Plugin (AGP) 9.4.1 + KSP 2.3.6 |
| **UI & Diseño** | Jetpack Compose + Material Design 3 + Navigation Compose |
| **Carga Multimedia** | Coil 2.7.0 (`coil-compose`, `coil-gif`) + FileProvider nativo |
| **Criptografía** | Android Keystore + AES-256-GCM + AndroidX Biometric 1.2.0 |
| **Persistencia Local** | Room Database 2.8.5 (Schema v2) + TypeConverters Gson (Offline-First) |
| **Backend & Cloud** | Firebase Authentication + Cloud Firestore + Google Services Plugin |
| **Arquitectura** | Clean Architecture (Domain, Data, Presentation) + MVVM |

---

## 📁 Estructura del Código

```
PassVaultSec/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json.example        # Plantilla segura de ejemplo
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Permisos y FileProvider
│   │   ├── java/com/passvaultsec/app/
│   │   │   ├── PassVaultApplication.kt     # Inyección y ThemeManager
│   │   │   ├── core/
│   │   │   │   ├── auth/GoogleAuthManager.kt        # Credential Manager + Firebase
│   │   │   │   ├── security/CryptoManager.kt       # Motor AES-256 Keystore
│   │   │   │   ├── security/BiometricAuthManager.kt# Biometría Huella/PIN
│   │   │   │   ├── ui/theme/                       # ThemeManager, Color (WCAG) y Theme
│   │   │   │   └── ui/util/                        # ImageStorage, Context y UrlMetadata
│   │   │   ├── domain/model/                       # Note, ChecklistItem, UrlPreview
│   │   │   ├── data/local/                         # Room Database, DAO y Entities
│   │   │   └── presentation/                       # MainActivity, NotesScreen y NoteEditorScreen
│   │   └── res/xml/file_paths.xml                  # Rutas seguras para fotos de cámara
├── CHANGELOG.md                                    # Registro oficial de versiones
├── firestore.rules                                 # Reglas de seguridad de Firestore
├── secrets.properties.example                      # Plantilla de variables privadas
└── README.md
```

---

## ⚙️ Compilación e Instalación

### Compilación desde Terminal (CLI):
```bash
# Compilar APK de depuración
./gradlew assembleDebug

# Ejecutar suite de pruebas unitarias
./gradlew test

# Instalar por ADB (USB o Wi-Fi)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Historial de Versiones y Cierre de Etapas

El proyecto sigue una estricta política de versionado documentada en [`CHANGELOG.md`](CHANGELOG.md):

* **`v1.0.0` (Actual - MVP Finalizado)**: Lanzamiento oficial del MVP con todas las características core, diseño pulido, multimedia, enlaces y seguridad auditada.
* **`v1.1.0` (Próxima etapa)**: Sincronización en segundo plano con WorkManager y copias de seguridad locales (.pvs).
* **`v2.0.0` (MVP v2 - Opción B comprometida)**: Dashboard web en tiempo real con telemetría de eventos de seguridad y métricas de servidor / Cloud Firestore.
