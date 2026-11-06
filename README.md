# OLUNAA

OLUNAA is a privacy-first social messaging platform with a native Android client and a Next.js web app.

## Stack
- Next.js 16, React 19 and TypeScript
- Prisma 6 with MongoDB
- Ably for authenticated realtime messaging and presence events
- LiveKit for WebRTC voice/video rooms
- Firebase Cloud Messaging for Android push notifications
- Native Android client (Java/Kotlin), Android SDK 35, min SDK 26
- Vercel for the web app and API

## Android app
The Android application ID is `com.olunaa.app`. Firebase's client configuration belongs at `android/app/google-services.json`. The GitHub Actions workflow builds release and debug APK artifacts when Android sources change.

The APK build must not generate dummy media assets. Keep large media out of the APK and load profile images/media on demand.

## Required Vercel environment variables
- `MONGODB_URI` — MongoDB connection string used by Prisma (required for the API).
- `ABLY_API_KEY` — Ably server key for realtime events.
- `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` — LiveKit server credentials for calls.
- `FIREBASE_PROJECT_ID` and `FIREBASE_SERVICE_ACCOUNT_JSON` — server-side FCM credentials for push notifications.

See [docs/REALTIME_SETUP.md](docs/REALTIME_SETUP.md) for secure setup and two-device verification.

## Product areas
Chats, user profiles, presence, read receipts, media, stories/updates, communities, notifications, voice/video call integration, and privacy settings.
