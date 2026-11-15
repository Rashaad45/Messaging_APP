# Decision log

Each entry: **decision**, **alternatives considered**, **why**. Newest at the bottom.
Status values: *Accepted*, *Superseded by D-xxx*.

---

## D-001 · Single `:app` module, layered by package
- **Decision:** One Gradle module with `ui/`, `domain/`, `data/`, `core/` packages.
- **Alternatives:** Multi-module (`:core:data`, `:feature:chat`, …) as in Google's Now in Android.
- **Why:** Multi-module mainly pays off with large teams or long build times. For a single
  developer it adds Gradle boilerplate without much benefit. Package boundaries give the same
  layering, and the packages can be split into modules later without rewriting code.
- **Status:** Accepted (M1)

## D-002 · minSdk 26 · targetSdk 36 · compileSdk 37
- **Decision:** minSdk 26 (Android 8.0), targetSdk 36 (Android 16), compileSdk 37.
- **Alternatives:** targetSdk 35 (original brief); targetSdk 37 (latest).
- **Why:**
  - *minSdk* decides which devices can install the app. 26 covers the vast majority of active devices and
    gives adaptive icons and `java.time` without desugaring.
  - *targetSdk* opts the app into each Android version's runtime behaviour changes. Google Play
    has required 36 for new apps since Aug 2026, and the test phone (Galaxy A34) runs Android 16,
    so 36 is the newest level I can test on real hardware. The lint `OldTargetApi` check is
    disabled with a comment pointing here.
  - *compileSdk* only controls which APIs the compiler can see. Current AndroidX libraries
    (Compose BOM 2026.09, Navigation 2.10, Hilt 1.4) refuse to build below 37.
- **Status:** Accepted (M1)

## D-003 · AGP 9 built-in Kotlin, Gradle version catalog
- **Decision:** AGP 9.5 with its built-in Kotlin support (no `org.jetbrains.kotlin.android`
  plugin). All versions live in `gradle/libs.versions.toml`.
- **Alternatives:** Hard-coded versions in each build file; `buildSrc` convention plugins.
- **Why:** The version catalog keeps every version in one place and is what the Android docs
  use. Convention plugins only help once there are several modules (see D-001).
- **Status:** Accepted (M1)

## D-004 · KSP (not kapt) for annotation processing
- **Decision:** Hilt (and Room in M3) use KSP.
- **Alternatives:** kapt.
- **Why:** kapt generates Java stubs and is in maintenance mode. KSP is roughly 2× faster and
  is the supported path for Kotlin 2.x.
- **Status:** Accepted (M1)

## D-005 · Static analysis: ktlint + detekt + Android Lint
- **Decision:** ktlint (formatting, `android_studio` style with trailing commas allowed),
  detekt (code smells/complexity), Android Lint (Android-specific bugs). All run in CI.
- **Alternatives:** Only detekt with its ktlint-wrapper `formatting` rule set; only Android Lint.
- **Why:** Each tool covers a different area. The separate ktlint plugin tracks new ktlint
  releases faster than detekt's wrapper.
- **Known issue:** detekt 1.23.8 and ktlint-gradle 14.2 use Gradle APIs that are deprecated
  for Gradle 10 (`ReportingExtension.file`, `Configuration.setVisible`). These warnings come
  from the plugins, not from our build scripts. Revisit when detekt 2.0 is stable.
- **Status:** Accepted (M1)

## D-006 · Compose UI tests run on the JVM with Robolectric
- **Decision:** Compose UI tests live in `src/test` and run with Robolectric.
- **Alternatives:** Instrumented tests in `src/androidTest` on an emulator.
- **Why:** CI can run them on every push without starting an Android emulator, which is slow
  and flaky on GitHub runners. The same test code can move to `androidTest` unchanged if
  device-only behaviour needs testing. Uses the v2 `createComposeRule`
  (StandardTestDispatcher), which replaced the deprecated v1 rule.
- **Status:** Accepted (M1)

## D-007 · `google-services.json` is not committed
- **Decision:** Gitignored locally. CI writes it from the `GOOGLE_SERVICES_JSON` GitHub
  Actions secret (the raw file contents). If the secret is missing, CI uses the placeholder
  `ci/google-services.ci.json`, so builds and unit tests never depend on secrets (see D-016).
- **Alternatives:** Commit it, which is common and acceptable for a private repo.
- **Why:** The file holds the project ID, app ID and a Firebase API key. That key is **not a
  secret**: it ships inside every APK, and data access is controlled by Firestore Security
  Rules and Auth. However, the repo is planned to go public for the portfolio (M6), and bots
  scrape public repos for keys, which leads to quota abuse and noisy security alerts. Keeping
  it out of git from day one avoids having to rewrite history later. The API key will also be
  restricted to this app's package name + SHA-1 in Google Cloud Console.
- **Status:** Accepted (M1)

## D-008 · Gradle wrapper pinned by SHA-256
- **Decision:** `gradle-wrapper.properties` sets `distributionSha256Sum`. CI's
  `setup-gradle` action also validates the wrapper jar.
- **Why:** Protects against a tampered Gradle download or wrapper jar (a supply-chain attack
  on the build).
- **Status:** Accepted (M1)

## D-009 · Backups disabled
- **Decision:** `allowBackup="false"` plus `dataExtractionRules` excluding every domain.
- **Why:** The local cache (Room) and Firebase Auth session belong to one device. Restoring
  them onto a new phone would show stale messages or an invalid session. All real data lives
  in Firestore and re-syncs after sign-in.
- **Status:** Accepted (M1)

## D-010 · Session state, not the back stack, gates sign-in
- **Decision:** `MessengerApp` observes a `SessionState` (Loading / SignedOut / NeedsProfile /
  SignedIn) and shows one of three UIs. Navigation Compose is only used *inside* the
  signed-in part.
- **Alternatives:** Sign-in and onboarding as NavHost destinations, navigating with
  `popUpTo` after success.
- **Why:** With destinations, every sign-in or sign-out needs careful back stack clearing.
  Otherwise Back can return to the login screen, and a session that expires mid-use goes
  unnoticed. Deriving the UI from state makes those bugs impossible. The state comes from
  `ObserveSessionUseCase`, which combines the auth and profile repositories. That's a use
  case because it spans two repositories.
- **Status:** Accepted (M2)

## D-011 · Username uniqueness: lock documents + transaction + rules
- **Decision:** `usernames/{name}` documents act as locks. The client claims a name in a
  Firestore **transaction** that reads the lock, then writes `users/{uid}` and the lock.
  Security rules check, using `getAfter()`, that both documents are written in the same
  commit and agree with each other.
- **Alternatives:** Query `users where username == x` before writing, which has a race:
  two people can both see "free". Or a Cloud Function to claim names, which costs money
  and adds latency.
- **Why:** A Firestore `create` fails if the document already exists, so the lock is atomic
  server-side. The transaction gives a precise "username taken" message. The rules make the
  guarantee hold even against a modified client. Claiming a username needs to be online:
  transactions don't run offline, and the app shows a specific offline message for this.
- **Known limitation:** rules `lower()` and Kotlin `lowercase()` might disagree on rare
  Unicode characters (e.g. Turkish İ), which would reject that display name. Acceptable for
  v1, because `displayNameLower` is only used for future search.
- **Status:** Accepted (M2)

## D-012 · Robolectric runs in pure-Java mode on Windows
- **Decision:** On Windows, unit tests set `robolectric.graphicsMode=LEGACY`,
  `sqliteMode=LEGACY` and `conscryptMode=OFF`. CI (Linux) uses the defaults.
- **Why:** Windows 11 **Smart App Control** blocks Robolectric's unsigned native DLLs
  (`libandroid_runtime.dll`, Conscrypt) with "An Application Control policy has blocked
  this file". Turning Smart App Control off would make tests pass, but it lowers the
  machine's security and can't be turned back on without reinstalling Windows. The
  pure-Java modes are slightly less realistic. CI still runs the native mode, so nothing
  goes unverified.
- **Status:** Accepted (M2)

## D-013 · Typed errors: `Outcome<T, E>` + domain error enums
- **Decision:** Repositories return `Outcome.Success`/`Outcome.Failure(AuthError | ProfileError)`.
  Firebase exceptions are mapped in the data layer (`FirebaseAuthErrorMapper`). The UI maps
  error values to string resources (`ErrorMessages.kt`).
- **Alternatives:** Throw exceptions to the ViewModel; `kotlin.Result<T>` (untyped Throwable).
- **Why:** The compiler forces every error case to be handled (exhaustive `when`).
  ViewModels stay free of Firebase and Android types, so they're tested with plain JUnit.
  Every failure gets a specific message, which covers the "handle errors visibly"
  requirement. `CancellationException` is always rethrown so coroutine cancellation still works.
- **Status:** Accepted (M2)

## D-014 · Google Sign-In via Credential Manager, called from the UI layer
- **Decision:** `GetSignInWithGoogleOption` (the "Sign in with Google" button flow) through
  `androidx.credentials`. The ID token is exchanged for a Firebase session in
  `AuthRepository`. The Credential Manager call itself lives in `ui/auth/GoogleSignIn.kt`.
- **Alternatives:** The legacy `GoogleSignInClient` (deprecated); `GetGoogleIdOption` (meant
  for automatic one-tap prompts, not for a button).
- **Why:** Credential Manager is Google's supported API. It needs an **Activity** context to
  show its UI, and a ViewModel must never hold an Activity (the ViewModel outlives it, which
  would leak memory). So the screen gets the token and hands it to the ViewModel. A user
  cancelling the picker is not treated as an error. Sign-out also clears the credential
  state, so the account picker shows again next time.
- **Status:** Accepted (M2)

## D-015 · Rules tests: Node's built-in test runner + emulator, firebase-tools pinned
- **Decision:** `firestore-tests/` uses `@firebase/rules-unit-testing` with `node --test`.
  `firebase-tools` is a pinned devDependency, and `npm test` runs `emulators:exec`.
- **Alternatives:** Jest or Vitest; a globally installed Firebase CLI.
- **Why:** Using Node's built-in runner means no test framework to configure. Pinning the
  CLI makes CI and local runs use the same emulator version. Note: the emulator needs
  **Java 21+**, while the Android build uses JDK 17.
- **Status:** Accepted (M2). These tests caught a real bug in the first draft of the rules:
  `$(path)` with a slash builds a single path segment.

## D-016 · CI builds without secrets (placeholder Firebase config)
- **Decision:** If the `GOOGLE_SERVICES_JSON` secret isn't set (e.g. pull requests from
  forks, or a new clone), CI copies `ci/google-services.ci.json`, which has fake values.
- **Why:** Unit tests never talk to Firebase, so a fake config is enough for building and
  testing. A missing secret then can't turn CI red. The real config is only needed for an
  APK that should actually work on a phone.
- **Status:** Accepted (M2)

---

## Planned decisions (from the approved plan; detailed when implemented)
- **Room is the only local cache; Firestore offline persistence is disabled** (M3/M4).
  Until then (M2) the Firestore SDK's default cache keeps the user's profile available
  offline, so opening the app without a network doesn't send existing users to onboarding.
- **Outbox pattern + WorkManager for sending; client-generated message IDs make retries
  idempotent** (M4).
- **Read/delivered receipts as per-user high-water marks on the conversation, not per-message
  status writes** (M4).
- **Throttled typing indicator in Firestore; Realtime Database noted as an upgrade** (M4).
- **Growing Room query window for pagination instead of Paging 3** (M4).
- **Delete account keeps the other person's chat history; sent messages show as
  "Deleted user"** (M5/M6).
- **Data model is ready for groups and posts.** See `DATA_MODEL.md` § Future extensions.
