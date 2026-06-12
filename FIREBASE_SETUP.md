# Firebase Setup

The authentication implementation requires a Firebase project owned by the team.

1. In Firebase Console, create/register the Android app with package name
   `com.example.mobilnekt1`.
2. Download `google-services.json` and place it in `app/google-services.json`. The file is
   intentionally ignored by Git so each environment can select its Firebase project.
3. In **Authentication > Sign-in method**, enable **Email/Password**.
4. Create a Cloud Firestore database. Do not leave it in unrestricted test mode.
   The app uses `users/{uid}` for account data and `matches/{matchId}` for the shared
   two-device lobby and current match state.
5. Install Firebase CLI, authenticate, and select the project:

   ```powershell
   npm install -g firebase-tools
   firebase login
   firebase use --add
   ```

6. Deploy the Firestore rules:

   ```powershell
   firebase deploy --only firestore:rules
   ```

7. Build and run the app:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

Email and username login work through Firebase Authentication and Firestore. This setup works
on the free Spark plan. For production, move username lookup behind trusted backend code,
enable Firebase App Check, and configure authorized email action domains.

## Two-device lobby check

1. Sign in with two verified Firebase accounts on separate devices or emulators.
2. On device A, open **Partija preko dva telefona** and create a match.
3. Enter the displayed six-character code on device B.
4. Confirm that both devices show both usernames and the active status.
5. Start either game and confirm that both devices open it with the same `matchId`.
