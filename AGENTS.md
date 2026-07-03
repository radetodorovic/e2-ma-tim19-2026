# Repository Guidelines

## Project Structure & Module Organization

This repository contains a single Android application module, `app`, built with Java 17 and XML layouts. Production code lives under `app/src/main/java/com/example/mobilnekt1`; group new features into the existing `data`, `domain`, and `presentation` packages where appropriate. Android resources are in `app/src/main/res`, with screen layouts named `activity_<screen>.xml`. Local unit tests mirror production packages under `app/src/test/java`. Root-level Firebase configuration includes `firebase.json` and `firestore.rules`; setup instructions are in `FIREBASE_SETUP.md`.

## Build, Test, and Development Commands

Run commands from the repository root. On Windows, use the checked-in Gradle wrapper:

- `.\gradlew.bat assembleDebug` builds the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `.\gradlew.bat test` runs all local JUnit tests.
- `.\gradlew.bat testDebugUnitTest` runs the debug variant's unit tests only.
- `.\gradlew.bat installDebug` installs the debug build on a connected emulator or device.
- `.\gradlew.bat lintDebug` runs Android lint checks.

Use `./gradlew` for the equivalent commands on macOS or Linux. Android Studio is recommended for Gradle sync, emulator management, and interactive debugging.

## Coding Style & Naming Conventions

Follow existing Java style: four-space indentation, braces on the same line, and one public top-level class per file. Use `PascalCase` for classes, `camelCase` for methods and variables, and `UPPER_SNAKE_CASE` for constants. Keep package names lowercase. Name activities `<Feature>Activity`, view models `<Feature>ViewModel`, repositories `<Feature>Repository`, and tests `<ClassUnderTest>Test`. Use lowercase snake_case for resource names, such as `activity_match_lobby.xml`. No formatter is configured, so use Android Studio's standard Java/XML formatting and keep imports organized.

## Testing Guidelines

Tests use JUnit 4. Add deterministic unit tests for domain rules, scoring, validation, and game engines. Mirror the source package and use descriptive method names such as `malformedEmail_isRejected`. Run `.\gradlew.bat test` before opening a pull request. The project has no enforced coverage threshold; prioritize edge cases and regressions in changed logic.

## Commit & Pull Request Guidelines

History uses short, feature-focused commit subjects, often in Serbian (for example, `Mobilne prijatelji`). Prefer an imperative, specific subject and keep each commit scoped to one concern. Pull requests should explain the behavior change, list verification commands, link the relevant task or issue, and include screenshots or recordings for UI changes. Call out Firebase or Firestore rule changes explicitly.

## Security & Configuration

Do not commit `app/google-services.json`, `local.properties`, credentials, or generated APKs; these are ignored intentionally. Follow `FIREBASE_SETUP.md` for local Firebase configuration and review `firestore.rules` carefully when changing persisted data access.
