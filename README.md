# Integrated Rural Healthcare System with AI Triage

**An offline-first, AI-powered healthcare ecosystem ensuring No Village is Left Behind.**

## 🚀 Problem Statement
Rural healthcare faces critical challenges:
*   **Lack of Doctors**: Specialist access is rare in remote villages.
*   **Delayed Triage**: Critical emergencies are often missed due to lack of screening.
*   **Connectivity**: Apps fail in patchy internet zones.
*   **Fragmentation**: Data is siloed between Hospitals, ASHA workers, and Patients.

## 💡 Solution
A unified, multi-role Android application connecting **Hospitals, ASHA Workers, and Patients** on a real-time sync network.

### Key Features
*   **🤖 AI Triage Engine**: Instantly assesses symptoms (Low/Medium/High Risk) using local catalog & LLM Fallback (Grok AI).
*   **📡 Offline-First & Real-Time**: Firebase Firestore synchronization ensures data continuity even without internet.
*   **👩‍⚕️ Cross-Portal Sync**:
    *   **Dashboard**: Hospital sees live triage queue.
    *   **ASHA**: Receives case assignments instantly.
    *   **Patient**: Tracks doctor availability live.
*   **💊 Pharmacy Forecasting**: AI prediction for medicine stock requirements.
*   **🗣️ Voice & Vernacular**: Voice-to-Text symptom entry and multilingual support (English/Hindi/Punjabi) for accessibility.

## 🛠️ Tech Stack
*   **Language**: Kotlin (Jetpack Compose)
*   **Architecture**: MVVM + Clean Architecture + Hilt DI
*   **Backend**: Firebase (Firestore, Auth)
*   **AI/ML**: Grok API (Triage), Custom Logic (Risk Classification)
*   **Async**: Coroutines & Flows (Real-time compatibility)

## 📲 Setup Instructions

### Prerequisites
*   Android Studio Ladybug or newer
*   JDK 17+

### 1. Clone Repository
```bash
git clone <your-repo-url>
cd healthapp
```

### 2. Configure Firebase
*   Place your `google-services.json` in the `/app` directory. This file is **excluded** from git for security.

### 3. Build & Run
*   Sync Gradle.
*   Run on Emulator or Real Device.

## ⚠️ Important Note
This project uses `google-services.json` for Firebase connectivity. For security reasons, this file is not included in the repository. Please contact the maintainer or use your own Firebase project config.

## 👥 Team
*   **Ashin Kiran Krishna** - Lead Developer
