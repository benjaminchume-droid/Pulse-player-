# 🎧 Pulse Player - automated CI/CD Engine Setup Directory

This document provides a comprehensive step-by-step manual for configuring the enterprise-ready GitHub Actions CI/CD workflows for the **Pulse Player** Flutter music application. This setup includes automated tests, build diagnostics, static analyses, and secured code signing pipeline that produces production-ready `.apk` and Google Play Store `.aab` (App Bundle) binaries.

---

## 🛠 CI/CD Pipeline Architecture Overview

The system utilizes three separate workflow blueprints aligned with top-tier DevOps maturity standards:

```
                      [ Developer Branch Workflows ]
                        
     Feature Branch PRs               develop Pushes                  v* Git Release Tag
            │                               │                               │
            ▼                               ▼                               ▼
 ┌──────────────────────┐        ┌──────────────────────┐        ┌──────────────────────┐
 │ pr_verification.yml  │        │ development_build.yml│        │production_release.yml│
 ├──────────────────────┤        ├──────────────────────┤        ├──────────────────────┤
 │ ▸ lint & code format │        │ ▸ flutter pub get    │        │ ▸ lint, format, test │
 │ ▸ static analysis    │        │ ▸ lint, format, test │        │ ▸ load signing keys  │
 │ ▸ full unit testing  │        │ ▸ compile unsigned    │        │ ▸ build signed AAB   │
 │ ▸ coverage reports   │        │   release split-APK  │        │ ▸ build signed APK   │
 │                      │        │ ▸ upload artifacts   │        │ ▸ create gh-release  │
 │                      │        │                      │        │ ▸ attach release assets  │
 └──────────────────────┘        └──────────────────────┘        └──────────────────────┘
```

1. **Pull Request Verification Workflow (`pr_verification.yml`)**
   - **Target**: Triggered on pull requests to `main` or `develop`.
   - **Responsibility**: Gatekeeper verification of all code. Runs static analysis (`flutter analyze`), formatting validations, and unit test suites on Ubuntu runtimes with parallel workers.

2. **Development Build Workflow (`development_build.yml`)**
   - **Target**: On pushes directly to `develop` or manual run dispatching.
   - **Responsibility**: Builds debuggable, unsigned split files per ABI (`armeabi-v7a`, `arm64-v8a`, `x86_64`) for QA manual installs. Retains artifacts for 14 days.

3. **Production Release Pipeline (`production_release.yml`)**
   - **Target**: Pushed tags initiating on semantic tagging patterns matching `v*` (e.g. `v1.0.4+25`).
   - **Responsibility**: Performs deep build tests, decodes production signing credentials, compiles production-ready `.aab` (Android App Bundle) and `.apk`, automatically generates Release changelogs by tracking differences since prior Git tag, registers a release under the tag name on GitHub, and uploads binaries directly onto the Release page.

---

## 🔐 Android App Signing Setup (Play Store and Secure Sideloading)

To build production-grade binaries in a headless environment, Gradle must sign the applications using a release keystore without hardcoding any password passwords.

### Step 1: Generate your Custom Production Keystore
Create a secure custom Keystore signature file if you haven't already.

```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias pulse-release-key
```

Keep variables secure when prompted:
- **Keystore Store Password**: (e.g., `MyStorePassword123`)
- **Key Alias**: `pulse-release-key`
- **Key Password**: (e.g., `MyKeyPassword123`)

### Step 2: Encode Keystore File into Base64 for GitHub Secrets
To securely inject the binary `release-key.jks` file into the GitHub runner workspace, we convert the raw binary to an ASCII Base64 string.

**On macOS / Linux:**
```bash
base64 -i release-key.jks -o keystore_base64.txt
# Copy the string in keystore_base64.txt to your clipboard
```

**On Windows (Powershell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-key.jks")) > keystore_base64.txt
# Copy the resulting text
```

### Step 3: Configure GitHub Secrets
Navigate to your GitHub Repository: `Settings` ➔ `Secrets and variables` ➔ `Actions` ➔ `New repository secret` and add the following 4 secrets:

| Secret Key Identifier | Purpose / Value Description | Example Value |
| :--- | :--- | :--- |
| **`SIGNING_KEY_STORE_BASE64`** | The complete output text paste of your base64 encoded keystore | `MIIDvQYJKoZIhvcNAQcCoIDr...` |
| **`PULSE_KEY_ALIAS`** | Name of the primary key alias generated in keytool | `pulse-release-key` |
| **`PULSE_KEY_PASSWORD`** | Password specified when designing key | `MyKeyPassword123` |
| **`PULSE_STORE_PASSWORD`**| Password created protecting Keystore container | `MyStorePassword123` |

---

## 📂 Codebase Configuration Updates

To integrate cleanly with the automated engine, update these configuration locations in your Flutter application directory:

### 📱 1. Android Gradle Settings Setup (`android/app/build.gradle`)
Replace or expand your `android/app/build.gradle` file utilizing the pattern generated in `android/app/build.gradle.example`. This tells the build logic to fetch credentials from Gradle environment proxies:

```groovy
def getSigningProperty(String envVarName, String propName) {
    if (System.getenv(envVarName) != null) {
        return System.getenv(envVarName)
    }
    if (project.hasProperty(propName)) {
        return project.property(propName)
    }
    return null
}

android {
    ...
    signingConfigs {
        release {
            def keystoreFile = file("keystores/release-key.jks")
            if (keystoreFile.exists()) {
                storeFile keystoreFile
                storePassword getSigningProperty("PULSE_STORE_PASSWORD", "pulseStorePassword")
                keyAlias getSigningProperty("PULSE_KEY_ALIAS", "pulseKeyAlias")
                keyPassword getSigningProperty("PULSE_KEY_PASSWORD", "pulseKeyPassword")
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 🏷️ 2. Dynamic Versioning Mechanism
The Production Release workflow completely automates the release versioning process. When building the application, the command:
```bash
flutter build appbundle --release \
  --build-name="${{ steps.pre_version.outputs.version_name }}" \
  --build-number="${{ steps.pre_version.outputs.build_number }}"
```
overrides the dynamic variables.
- **Git tag `v1.2.5`** sets the app's internal text representation version name to `1.2.5`.
- **`github.run_number` (e.g., `42`)** sets the Google Play compatible incremental sequential integer version code, ensuring update compliance on the platform without manual increments.

---

## 🚀 Speeding Up Builds (Caching Policies)

To achieve lightning-fast setup (reducing execution duration from over 8 minutes to under 3 minutes per run), the workflows leverage dynamic, multi-tier caches:

1. **Flutter SDK Cache**: Pre-built using the optimized `subosito/flutter-action@v2` with direct path cache configurations.
2. **Dart Pub Cache**: Standardized cross-build package managers are restored and updated via semantic hash indicators.
3. **Gradle Native Caching Manager**: Handles underlying Java-based build modules using `gradle/actions/setup-gradle@v4`, preventing rebuilds of unchanged libraries and native dependencies.

---

## 🛑 Failure Alerts Setup & Integration

All workflows output clear metrics on complete failure. You can optionally attach instant Slack notifications to the bottom of critical files (e.g. `production_release.yml`) by appending a standardized webhook executor:

```yaml
      - name: Send Slack Notification on Failure
        if: failure()
        uses: rtCamp/action-slack-notify@v2
        env:
          SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_COLOR: '#EF4444'
          SLACK_TITLE: "🚨 Pulse Player Build Failed!"
          SLACK_MESSAGE: "Production build workflow failed under commit user @${{ github.actor }} on branch: ${{ github.ref_name }}. Critical inspection is requested."
```

---

## 🎖️ GitHub Readme Badges Template

Decorate your project's internal `README.md` using status indicator endpoints for live developer monitoring:

```markdown
# 🎧 Pulse Player

Pulse Player is a premium, immersive Android music player.

| Main Stack Workflow | Build Status Badge |
| :--- | :--- |
| **Pull Request Integrity Gate** | [![PR Verification](https://github.com/OWNER/REPO/actions/workflows/pr_verification.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/pr_verification.yml) |
| **QA / Develop Deployments** | [![Develop Build Status](https://github.com/OWNER/REPO/actions/workflows/development_build.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/development_build.yml) |
| **Production Store Release** | [![Prod Release Status](https://github.com/OWNER/REPO/actions/workflows/production_release.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/production_release.yml) |
```
*(Remember to swap `OWNER/REPO` with your repository paths when configuring details!)*
