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
- **Decision:** Gitignored locally. CI decodes it from a base64 GitHub Actions secret
  (from M2, when Firebase is added).
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

---

## Planned decisions (from the approved plan; detailed when implemented)
- **Room is the only local cache; Firestore offline persistence is disabled** (M3/M4).
- **Outbox pattern + WorkManager for sending; client-generated message IDs make retries
  idempotent** (M4).
- **Read/delivered receipts as per-user high-water marks on the conversation, not per-message
  status writes** (M4).
- **Throttled typing indicator in Firestore; Realtime Database noted as an upgrade** (M4).
- **Growing Room query window for pagination instead of Paging 3** (M4).
- **Credential Manager for Google Sign-In** (M2).
- **Delete account keeps the other person's chat history; sent messages show as
  "Deleted user"** (M5/M6).
- **Data model is ready for groups and posts.** See `DATA_MODEL.md` § Future extensions.
