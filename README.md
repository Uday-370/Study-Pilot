# 🚀 StudyPilot

> **An elite, distraction-free focus timer and academic vault built natively for Android.**

StudyPilot is a premium productivity application designed to help students and professionals conquer distractions, build unbreakable discipline, and master their subjects. It goes beyond a simple Pomodoro timer by integrating a full-scale academic planner, a secure file vault, and a custom graphics engine for sharing achievements.

---

## ✨ Key Features

* ⏱️ **Doze-Proof Focus Sessions:** A bulletproof background timer (Pomodoro & Stopwatch) utilizing Android `Foreground Services` and `Partial WakeLocks` to guarantee accurate ticking even when the CPU enters deep sleep.
* 📊 **Mastery Hub & Analytics:** Tracks daily study hours, calculates weekly progress against custom goals, and manages an unbroken day streak.
* 🗄️ **The Study Vault:** A central repository to securely attach PDFs, videos, images, and web links directly to specific subjects using the Android `Storage Access Framework`.
* 📅 **Smart Agenda & Calendar:** A 14-day rolling calendar with an underlying algorithm that intelligently sorts tasks by completion status, priority level, and due date.
* 🎨 **Dynamic Achievement Cards:** A custom hardware-accelerated 2D graphics engine (Canvas API) that renders holographic, personalized "Elite Focus" trading cards that users can share directly to their Instagram Stories.
* 🔔 **Smart Retention Notifications:** Uses `AlarmManager` and `BroadcastReceivers` to send perfectly timed, battery-safe local notifications to warn users before they lose their streak.
* 🌊 **Liquid UI & Animations:** Built entirely with Jetpack Compose, featuring parallax scrolling, fluid gradient meshes, and a custom "Zen" Material 3 design system.

---

## 🛠️ Tech Stack & Architecture

StudyPilot is built using modern Android development standards, following the **MVVM (Model-View-ViewModel)** architectural pattern and Unidirectional Data Flow (UDF).

**Core Technologies:**
* **UI:** Jetpack Compose, Material Design 3, Compose Animations
* **Language:** Kotlin
* **Architecture:** MVVM, Clean Architecture principles
* **Dependency Injection:** Dagger-Hilt
* **Local Database:** Room Database (SQLite)
* **Asynchronous Programming:** Kotlin Coroutines & StateFlow
* **Navigation:** Compose Destinations (Type-safe routing)
* **Image Loading:** Coil
* **Background Processing:** Foreground Services, SystemClock, PowerManager (WakeLocks)

---

## 📸 Screenshots

*(Replace these placeholder links with actual screenshots of your app)*

| Dashboard | Focus Timer | The Vault | Achievement Card |
|----------|------------|-----------|------------------|
| <img src="https://github.com/user-attachments/assets/6a1893f2-baab-40ab-af84-7187d5d72e29" width="200"/> | <img src="https://github.com/user-attachments/assets/4f9a70b3-8796-4296-91f7-a8a73fde95a6" width="200"/> | <img src="https://github.com/user-attachments/assets/8ffe5377-fdb6-4ac8-84b2-d83b893ce453" width="200"/> | <img src="https://github.com/user-attachments/assets/d3401319-190c-47fb-aa80-22cc9afebcd0" width="200"/> |

---

## 🧠 Technical Highlights

### The Custom Graphics Engine
To create a viral sharing loop, I bypassed standard XML layouts and built a custom 2D drawing engine using Android's `Canvas` API. It uses `PorterDuffXfermode` blending to create iridescent glass effects and seeds a random number generator with the user's `currentStreak` to generate a unique, mathematically consistent stardust background for every user.

### Thread-Safe Background Timers
Standard Java timers drift or freeze when Android enters Doze mode. StudyPilot solves this by decoupling the UI from the timer. A `CoroutineScope` running on the `Dispatchers.IO` thread tracks time using `SystemClock.elapsedRealtime()`, while a `PARTIAL_WAKE_LOCK` ensures the CPU stays awake just enough to trigger the alarm at the exact right millisecond, all while updating Compose `StateFlow` variables safely.




## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Minimum SDK: 26 (Android 8.0)
* Target SDK: 34 (Android 14)

### Installation
1. Clone the repository:
   ```bash
   git clone [https://github.com/Uday-370/StudyPilot.git](https://github.com/Uday-370/StudyPilot)
