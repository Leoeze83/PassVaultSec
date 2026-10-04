# 📜 Registro Oficial de Versiones (Changelog) - PassVaultSec

Este documento mantiene el historial cronológico y formal de todas las versiones, hitos y etapas de desarrollo de **PassVaultSec**.
A partir de la finalización del MVP, cada cierre de etapa ("terminamos una etapa") incrementa automáticamente la versión bajo el estándar [Semantic Versioning (SemVer 2.0.0)](https://semver.org/).

---

## [1.0.0] - MVP Finalizado y Blindado (Cierre de Etapa 1) 🚀
**Fecha:** 4 de Octubre de 2026  
**Estado:** Estable / Producción MVP  
**Tag Git:** `v1.0.0`  
**Paquete:** `com.passvaultsec.app` (`PassVaultSec-debug.apk`)

### ✨ Novedades y Funcionalidades Incorporadas:
1. **Contraste Automático Dinámico de Texto (Fórmula WCAG / ITU-R BT.709)**:
   - Al cambiar el color de fondo de las notas, el texto y elementos secundarios calculan automáticamente su luminancia relativa en tiempo real, garantizando máxima legibilidad sin textos ilegibles o invisibles.
2. **Selector de Color de Texto Personalizado**:
   - Paleta integrada en el editor (`NoteTextPaletteColors`) para fijar colores específicos de texto (Negro, Blanco, Grafito, Azul vibrante, Rojo, Verde, Púrpura, Ámbar) o alternar al modo automático con un toque.
3. **Soporte Multimedia Completo (Cámara, Galería y GIFs Animados)**:
   - **Cámara Directa**: Captura de fotografías en alta resolución mediante `FileProvider` nativo en sandbox seguro (`context.filesDir/note_images/`).
   - **Galería de Imágenes & GIFs**: Integración con el Photo Picker de Android 13+ (`ActivityResultContracts.PickVisualMedia`) y renderizado acelerado por hardware con **Coil 2.7.0** (`coil-gif`).
   - Miniaturas y banners dinámicos tanto en el editor como en las tarjetas del tablero general.
4. **Soporte para Emoticones y Teclado Enriquecido**:
   - Barra flotante de emojis frecuentes (`🔒`, `🔑`, `🛡️`, `📝`, `💡`, `📌`, `⭐`, `⚠️`, `💰`, `🏠`, `💼`, `🎯`, `🚀`, etc.) para inserción rápida con un solo toque.
5. **Previsualización Enriquecida de Enlaces Web (Rich Link Previews - OpenGraph)**:
   - Motor asíncrono `UrlMetadataExtractor` que analiza etiquetas OpenGraph (`og:title`, `og:description`, `og:image`) y genera tarjetas visuales con miniatura, dominio y enlace directo al navegador mediante `LocalUriHandler`.
6. **Soporte Completo de Modo Claro / Modo Oscuro / Sistema**:
   - `ThemeManager` reactivo persistido en `SharedPreferences` con `StateFlow`.
   - Paleta adaptativa en modo oscuro (tonos pastel oscuros con alto contraste).
   - Acceso rápido en la barra de búsqueda y selector triple en el diálogo de cuenta.
7. **Corrección de Window Insets (Status Bar & Navigation Bar)**:
   - Barra de búsqueda ajustada con `.statusBarsPadding()` para respetar notch, reloj, batería y red.
   - Botones flotantes (FAB) y teclado virtual con `.navigationBarsPadding()` e `.imePadding()`.
8. **Autenticación con Google Funcional (Credential Manager & Firebase)**:
   - Desacople de `ContextThemeWrapper` y llamada directa a `FragmentActivity`.
   - Registro de huellas digitales **SHA-1** y **SHA-256** en Firebase Console y creación de clientes OAuth 2.0.
9. **Seguridad Zero-Trust: Rebloqueo Inmediato de Notas**:
   - Eliminación de persistencia de desbloqueo en memoria (`unlockedNoteIds`).
   - Cada nota con candado exige obligatoriamente huella/PIN cada vez que se abre.
   - Al salir o retroceder, la nota vuelve inmediatamente a estar enmascarada y bloqueada en el tablero.

---

## [0.9.0-rc] - Candidato a Release (Optimizaciones y Bugfixes UI/Auth)
**Fecha:** 4 de Octubre de 2026  
**Estado:** Superado  
- Ajuste de márgenes superiores e inferiores de la ventana en Compose.
- Depuración de Credential Manager contra errores de configuración en Google Play Services.
- Creación de extensiones seguras de contexto (`findActivity()`, `findFragmentActivity()`).

---

## [0.5.0-beta] - Capa Criptográfica y Firebase Backend
**Fecha:** 3 de Octubre de 2026  
**Estado:** Superado  
- Integración de motor criptográfico **AES-256-GCM** respaldado en hardware seguro con **Android Keystore**.
- Bloqueo biométrico individual por nota mediante `androidx.biometric:biometric:1.2.0-alpha05`.
- Base de datos local offline-first con **Room Database 2.8.5** y KSP.
- Backend en la nube con **Cloud Firestore** y reglas de seguridad `firestore.rules`.
- Diálogo de colaboración multiusuario y roles (Owner, Editor, Viewer).

---

## [0.1.0-alpha] - Prototipo Inicial (Arquitectura Base)
**Fecha:** 2 de Octubre de 2026  
**Estado:** Superado  
- Estructura base en Clean Architecture + MVVM.
- UI moderna en Jetpack Compose con tablero interactivo, tarjetas dinámicas y checklists.
- Configuración de dependencias en `libs.versions.toml`.

---

## 🔮 Hoja de Ruta para Versiones Futuras

### [1.1.0] - Próxima Etapa Planificada
- Sincronización offline en segundo plano con WorkManager.
- Compartir notas mediante enlaces cifrados temporales.
- Exportación e importación segura de copias de respaldo locales cifradas (.pvs).

### [2.0.0] - Versión 2 del MVP (Opción B: Dashboard Web Real-Time)
- **Dashboard web en tiempo real** para monitoreo de telemetría de eventos de seguridad.
- Métricas de operaciones de base de datos en Cloud Firestore y ejecuciones en Google Cloud / servidor.
- Control de tráfico y usabilidad con múltiples usuarios de prueba.
