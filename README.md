# PaltoScan 🥑🔍

**PaltoScan** es una aplicación móvil desarrollada en **Android (Kotlin)** diseñada para el diagnóstico y detección inteligente de enfermedades en hojas del cultivo de **palta (aguacate)** utilizando visión artificial y Machine Learning directamente en el dispositivo (Edge AI).

---

## 🌟 Características Principales

- **Detección en tiempo real**: Captura y análisis de hojas de palto mediante **CameraX**.
- **Inferencia en el dispositivo**: Modelos optimizados con **TensorFlow Lite (TFLite)** para funcionamiento offline.
- **Explicabilidad con Grad-CAM**: Generación de mapas de calor para visualizar las zonas críticas identificadas por la red neuronal.
- **Historial de diagnósticos**: Almacenamiento local mediante **Room Database**.
- **Arquitectura Limpia**: Separación modular en capas (`:app` y `:core-ml`) con **MVVM** e inyección de dependencias con **Hilt**.
- **Monitoreo & Estabilidad**: Integración con **Firebase Crashlytics** y Analytics.

---

## 🏗️ Arquitectura y Tecnologías

- **Lenguaje**: Kotlin
- **JDK Target**: 17
- **Inyección de Dependencias**: Hilt / Dagger
- **Base de Datos Local**: Room
- **Cámara & Visión**: CameraX, TensorFlow Lite
- **UI / UX**: Material Design 3, Lottie Animations, ViewBinding
- **Navegación**: Jetpack Navigation Component (SafeArgs)

---

## 🚀 Comenzando

### Prerrequisitos
1. Android Studio Ladybug o superior.
2. JDK 17 configurado en el entorno de desarrollo.
3. Dispositivo o emulador con Android 7.0 (API nivel 24) o superior.

### Instalación
```bash
git clone https://github.com/Percy-30/PaltoScan.git
```
Abre el proyecto en Android Studio y sincroniza con Gradle.
