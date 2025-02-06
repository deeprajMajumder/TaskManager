# 📱 Task Manager

A modern Android app built with **Kotlin** and **Jetpack Compose**, following clean architecture principles with **MVVM** and **Hilt Dependency Injection**. The app integrates **Room Database, Firebase Analytics, and Crashlytics** for a seamless user experience.

---

## 🚀 Features

- ✅ Jetpack Compose UI for a modern UI experience.
- ✅ MVVM architecture for better separation of concerns.
- ✅ Hilt for Dependency Injection.
- ✅ Room Database for local task storage.
- ✅ Retrofit for API call.
- ✅ API integration to fetch tasks from a remote server.
- ✅ Firebase Analytics & Crashlytics integration.

---

## 🛠️ Setup & Run Instructions

### Prerequisites

- 📌 Android Studio **latest version**
- 📌 JDK **17+**
- 📌 An **active Firebase project** (for Analytics & Crashlytics)
- 📌 Internet connection for API calls

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/deeprajMajumder/TaskManager
   cd TaskManager
   ```
2. **Open in Android Studio** and sync Gradle. Required AGP = 8.8.0
3. **Set up Firebase**:
   - Add your `google-services.json` file to the `app/` directory.
   - Enable Firebase Analytics & Crashlytics in the Firebase console.
4. Add **baseUrl="https://jsonplaceholder.typicode.com/"** in gradle.properties
5. Add **keystore.properties** for running release variant with parameters are below
   - storePassword=myStorePassword
   - keyPassword=mykeyPassword
   - keyAlias=myKeyAlias
   - storeFile=myStoreFileLocation
6. **Run the app** on an emulator or physical device.
7. **Check logs** for Firebase event tracking:
   ```bash
   adb shell setprop log.tag.FirebaseAnalytics DEBUG
   adb logcat -s FirebaseAnalytics
   ```

---

## 📦 Third-Party Libraries Used

| Library | Purpose |
|---------|---------|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | UI framework |
| [Hilt](https://dagger.dev/hilt/) | Dependency Injection |
| [Retrofit](https://square.github.io/retrofit/) | API calls |
| [Room](https://developer.android.com/jetpack/androidx/releases/room) | Local database |
| [Firebase Analytics](https://firebase.google.com/docs/analytics) | Event tracking |
| [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics) | Crash reporting |

---

## 🏗️ Design Decisions

- **MVVM Architecture**: Keeps UI logic separate from business logic.
- **Hilt for Dependency Injection**: Ensures better code maintainability.
- **Singleton for Firebase Analytics**: Prevents multiple instances and ensures efficient logging.
- **Repository Pattern**: Ensures data sources are well-structured and easily testable.
- **Separation of Concerns**: Each class follows the Single Responsibility Principle.

I chose to implement Clean Architecture and the MVVM design pattern in the Task Manager app to ensure scalability, maintainability, and separation of concerns. Clean Architecture helps organize the app into layers, making it easier to manage and test. By separating business logic from UI, the app becomes more modular and adaptable to future changes.

Using MVVM ensures a clear separation between the View (UI) and the ViewModel (business logic), allowing for better testability and easier updates to the UI without affecting the core logic.

Material Design principles were followed to provide a consistent, intuitive, and visually appealing user experience. It ensures that users have clear feedback for different actions and operations within the app, improving overall usability.

---

## 📊 Firebase Analytics Events

Below are screenshots of key **Firebase Analytics** events:

🖼 Firebase Analytics Dashboard
![Firebase Analytics Dashboard](appScreenshotsAndRecordings/AnalyticsDashboard.png)

🖼 Task Added
![Task Added](appScreenshotsAndRecordings/TaskAdded.png)

🖼 Task Completed
![Task Completed](appScreenshotsAndRecordings/TaskCompleted.png)

🖼 Events Dashboard
![Events Dashboard](appScreenshotsAndRecordings/EventsDashboard.png)

---

## 🔥 Crash Reports & Debugging

A recording of the app crashing and **Crashlytics logs**:

📹 **Screen recording of the crash**  

   - Forced Crash
     🎬   [Forced Crash](https://drive.google.com/file/d/1lWwM3tK4YOmv0_iW2fdPC7J9QIHIGKHz/view?usp=sharing)

   - Forced DataBase Crash
   🎬   [Forced DataBase Crash](https://drive.google.com/file/d/10L_xctxYUyLpTACq51xwMztcUwCx9e1-/view?usp=sharing)

📸 **Firebase Console Crash Logs**  

   - Forced Crash 
   🖼 ![Forced Crash](appScreenshotsAndRecordings/ForcedCrash.png)
   - Forced DataBase Crash
   🖼 ![Forced DataBase Crash](appScreenshotsAndRecordings/DatabaseCrash.png)
---

## 🌐 Network Performance

Below is an analysis of **network request performance**:

🖼 Network Performance
![Network Performance](appScreenshotsAndRecordings/NetworkPerformance.png)

🖼 Network Call Success Rate
![Network Call Success Rate](appScreenshotsAndRecordings/NetworkCallSuccessRate.png)

---

## 📜 License

```
MIT License
© 2025 Deepraj Majumdar
```

---

💡 **Contributions are welcome!**

