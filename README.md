# Purrgenda

A cat-themed Android reminder app I built in Kotlin. Make a reminder, pick a date and time, and the app nags you with a notification a few minutes before it's due. Your reminders are tied to your account and synced through Firebase, so they follow you across sessions and survive a reboot.

## Why I built it

I wanted a real end-to-end Android project to get past tutorial-sized apps — something with actual accounts, cloud storage, and the parts of Android that are genuinely fiddly: exact alarms, notification permissions, and rescheduling everything after the phone restarts. A reminder app is small enough to finish but touches all of those, so it was a good excuse to wire them up myself. The cat theme (hence "Purrgenda") was just to make it feel like mine.

## What it does

- Email/password sign up, login, and a forgot-password flow on Firebase Auth. You can log in with a username too — it looks up the matching email in Firestore first.
- "Remember me" so you skip the login screen on the next launch.
- Add, edit, and delete reminders. Each one has a title and a date/time picked through the standard date/time dialogs.
- Reminders live in Cloud Firestore, scoped to your user id, and the list updates live through a Firestore snapshot listener.
- Swipe a reminder left or right to delete it (with a confirm dialog), and toggle hiding completed ones.
- Local notifications fire 10 and 5 minutes before a reminder, scheduled with `AlarmManager` using exact alarms. Notifications can be turned off in settings.
- A `BootReceiver` re-reads your reminders and reschedules the alarms after a reboot, since Android drops pending alarms on restart.
- Dark mode toggle (saved in `SharedPreferences`) and a `purrgenda://add` deep link that opens straight into the add-reminder flow.

## Tech

Kotlin, native Android Views with XML layouts and ViewBinding. Firebase Auth + Cloud Firestore for accounts and storage. `AlarmManager` + `BroadcastReceiver` + `NotificationCompat` for the reminder notifications. Gson and a small `ViewModel`/`LiveData` setup are in there too. Gradle with the Kotlin DSL and a version catalog.

- `minSdk` 24, `targetSdk` / `compileSdk` 35
- Lottie for animation

## Running it

You'll need Android Studio and your own Firebase project, since the app talks to Firestore and Firebase Auth.

```bash
git clone https://github.com/MohammadAlfalah/ReminderApp.git
```

Then:

1. Create a Firebase project, add an Android app with the package `com.example.Purrgenda`, and enable **Email/Password** auth and **Cloud Firestore**.
2. Drop your own `google-services.json` into `app/`.
3. Build and run from Android Studio, or:

```bash
./gradlew installDebug
```

Grant the notification and exact-alarm permissions when the app asks — without exact alarms the reminders won't fire on time.

## Known rough edges

A few things I'd clean up if I came back to it:

- The `applicationId` is still `com.example.myfapp` while the namespace is `com.example.Purrgenda` — a leftover from renaming the project mid-way.
- Date/time is stored as a formatted string (`"d/M/yyyy at HH:mm"`) and parsed back out, which is brittle. Storing a real timestamp would be the right move.
- Notification lead times (10 and 5 minutes) are hardcoded rather than configurable.
- There's no editing of an existing reminder's alarm beyond what Firestore + reschedule-on-boot covers, and the in-memory `ReminderViewModel` is largely vestigial now that Firestore drives the list.

This is a personal learning project, not something I'm shipping to the Play Store — but it's a complete, working app, not a half-finished demo.
