# DISCIPLINE QUEST — FULL ANDROID SOURCE

This is the complete source project for the personal discipline RPG app requested.

## Included
- Native Android / Kotlin / Jetpack Compose
- Room local database
- Offline-first task, habit, mission and progress tracking
- Black + red gaming UI
- HP / XP / level / rank / coins
- One-time tasks
- Habits and streaks
- Missed-task penalties and recovery
- Calendar
- Daily / historical progress records
- Discipline score
- Missions
- Achievements
- Cosmetic coin shop
- Character/profile
- Export/import JSON backup
- Optional notifications
- Burmese/English setting
- AI Coach with preview/edit-before-save workflow
- Free-tier Gemini model integration
- Generated app icon included

## AI / ZERO-COST REQUIREMENT

The AI layer is optional. The core app never requires AI.

The current Gemini configuration is:
    gemini-3.8-flash

The Gemini API currently lists Gemini 3.8 Flash with a Free Tier. The app never enables billing and does not contain a paid API key.

For a personal app, put only a Free-Tier Gemini API key into:
Settings -> AI Coach -> Gemini API Key.

Do NOT enable Google Cloud billing for this app if your requirement is zero cost.

Important security note:
A client-side API key can be extracted from a compiled APK. This project is designed for personal/sideloaded use, not for distributing a public app with a secret key. For a public release, move the Gemini request to a server-side proxy.

## BUILD

1. Open this folder in Android Studio.
2. Wait for Gradle Sync.
3. Run on an Android device/emulator.
4. For an APK:
   Build -> Generate App Bundles or APKs -> Generate APKs.

No Play Store account is needed for personal sideloading.

## AI USE

The AI Coach:
1. accepts a goal,
2. generates a mission/task plan,
3. shows a PREVIEW,
4. lets you edit/delete the AI output,
5. only saves after you press ACCEPT & SAVE.

The AI never directly writes XP, HP, coins, level, streak or achievement state.

## App icon

`app/src/main/res/drawable/app_icon.png` is the generated black/red Discipline Quest icon.
