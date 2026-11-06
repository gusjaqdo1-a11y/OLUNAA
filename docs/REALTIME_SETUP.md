# OLUNAA Realtime, Calls and Push Setup

The repository contains integrations for Ably Realtime, LiveKit WebRTC, and Firebase Cloud Messaging. A feature is only live after its credentials are configured and the corresponding two-device tests pass.

## 1. Database (required for the API)

The Prisma schema uses MongoDB. Add this Vercel environment variable to **Production, Preview, and Development** as appropriate:

- `MONGODB_URI` — MongoDB Atlas connection string, including a database name such as `olunaa`.

Do not use `DATABASE_URL`; the application reads `MONGODB_URI`.

## 2. Ably Realtime

Create an Ably app and an API key. Add this Vercel environment variable:

- `ABLY_API_KEY` — full Ably key string. Keep it server-side only.

The backend issues short-lived, user-scoped TokenRequests. Conversation channels are named `conversation:<conversationId>`; private call signaling channels are named `user:<userId>`. Never put the Ably API key in the Android APK or browser bundle.

## 3. LiveKit WebRTC calls

Create a LiveKit Cloud project (or use a self-hosted LiveKit server) and add these Vercel environment variables:

- `LIVEKIT_URL` — the project's `wss://...` endpoint.
- `LIVEKIT_API_KEY` — LiveKit API key.
- `LIVEKIT_API_SECRET` — LiveKit API secret.

The API secret must never be shipped in the Android app. OLUNAA's authenticated `/api/calls/token` endpoint verifies conversation membership and creates a short-lived room token. Calls use the room `olunaa-<conversationId>`.

## 4. Firebase Cloud Messaging

The Android Firebase client file has been added at:

`android/app/google-services.json`

It is registered for package `com.olunaa.app` in Firebase project `olunaa-52ff0`. The Google Services Gradle plugin is applied when that file is present.

**Important: this file is not the server credential.** To send push notifications from Vercel, create/download a Firebase **service-account JSON** from Firebase Console / Google Cloud IAM, then add these Vercel environment variables:

- `FIREBASE_PROJECT_ID=olunaa-52ff0`
- `FIREBASE_SERVICE_ACCOUNT_JSON` — the complete service-account JSON as a protected/secret environment variable.

Enable the Firebase Cloud Messaging API and grant the service account permission to send FCM messages. Never commit the service-account private key or place it in the Android app.

The native app stores its FCM token and registers it against the authenticated account when the session cookie is saved/restored. The backend stores tokens at `/api/devices/token` and sends message/incoming-call alerts.

## 5. Android APK size and build

The previous Gradle build generated a 48 MB random media file that was not used by the app. That generation task has been removed so it no longer inflates every APK.

The GitHub Actions workflow builds the Android APK when files under `android/**` change. A successful GitHub Actions build confirms compilation only; it does not prove FCM delivery, network reachability, microphone/camera permissions, or real call quality.

## 6. Deploy and verify

After adding or changing Vercel environment variables, trigger a new Production deployment. Environment changes do not affect deployments that already exist.

Test with two different OLUNAA accounts/devices:

1. Sign in on both devices and confirm each FCM token is registered.
2. Open the same conversation on both devices and confirm a new message appears without waiting for the fallback refresh.
3. Start a voice call, accept it on the second device, then test mute and hang-up.
4. Repeat with video and camera permissions enabled.
5. Background or close the receiver app and verify FCM message and incoming-call notifications.
6. Tap a notification and verify that it opens the correct conversation/call.

Do not announce voice/video calling or push notifications as production-ready until these device-to-device tests pass.
