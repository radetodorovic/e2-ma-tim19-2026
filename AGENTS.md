# Repository Guidelines

## Project Structure & Module Organization

This is a single-module native Android application built with Java and XML. Application code lives in `app/src/main/java/com/example/mobilnekt1/`; each screen is represented by an `*Activity.java` class. Layouts are in `app/src/main/res/layout/`, shared strings, colors, and styles are in `res/values/`, and reusable shapes or icons are in `res/drawable/`. Register new activities in `app/src/main/AndroidManifest.xml`.

The current KT1 implementation is a GUI prototype. `MockGameData` and related classes provide in-memory sample data; there is no backend, database, persistent authentication, or multiplayer service.

## Build, Test, and Development Commands

Run commands from the repository root using the checked-in Gradle wrapper:

```powershell
.\gradlew.bat assembleDebug   # Compile and create the debug APK
.\gradlew.bat installDebug    # Install on a connected emulator/device
.\gradlew.bat lint            # Run Android static analysis
.\gradlew.bat test            # Run local JVM tests when present
.\gradlew.bat connectedAndroidTest # Run device tests when present
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Use JDK 17, Android Studio, and an installed Android SDK (compile/target SDK 33).

## Coding Style & Naming Conventions

Use four-space indentation for Java and XML. Follow existing Android conventions: `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Name activities `FeatureActivity`, layouts `activity_feature.xml`, and resource IDs with descriptive `snake_case` names such as `button_login` or `input_password`.

Keep user-visible text in `res/values/strings.xml`; do not hard-code it in Java or layouts. Reuse `BaseKt1Activity` helpers for common dialogs, validation, and toast behavior. Keep each activity focused on one screen.

## Testing Guidelines

No automated tests are currently committed. Add local unit tests under `app/src/test/java/...` and instrumentation/UI tests under `app/src/androidTest/java/...`. Name test classes after the subject, for example `LoginActivityTest`, and test methods by behavior, such as `emptyPassword_showsValidationMessage`. Before submitting changes, run `assembleDebug` and `lint`, then manually verify affected navigation on an emulator.

## Commit & Pull Request Guidelines

Existing history uses short, feature-oriented messages, but capitalization is inconsistent. Prefer concise imperative commits such as `Add Skocko score validation`. Keep unrelated changes in separate commits.

Pull requests should summarize the affected screens and behavior, list verification commands, and note any remaining mock behavior. Link the relevant issue or requirement. Include screenshots or a short recording for UI changes, and confirm that new activities and resources are correctly registered and referenced.
