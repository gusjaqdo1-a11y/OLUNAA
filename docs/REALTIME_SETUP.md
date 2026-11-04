# OLUNAA Realtime, Calls and Push Setup

The application now contains the integration points for Ably Realtime, LiveKit WebRTC, and Firebase Cloud Messaging. These features remain disabled until the matching service credentials and Android Firebase configuration are supplied.

## 1. Ably Realtime

Create an Ably app and an API key. Add this Vercel environment variable:

- `ABLY_API_KEY` — full Ably key string. Keep it server-side only.

The backend issues short-lived, user-scoped TokenRequests. Conversation channels are named `conversation:<conversationId>`; private call signaling channels are named `user:<userId>`. Do not put the Ably API key in the Android APK or browser bundle.

## 2. LiveKit WebRTC calls

Create a LiveKit Cloud project (or use a self-hosted LiveKit server) and add these Vercel environment variables:

- `LIVEKIT_URL` — the LiveKit WebSocket URL, for example the project's `wss://...` endpoint.
- `LIVEKIT_API_KEY` — LiveKit API key.
- `LIVEKIT_API_SECRET` — LiveKit API secret.

The API secret must never be shipped in the Android app. OLUNAA's authenticated `/api/calls/token` endpoint verifies conversation membership and creates a short-lived room token. Calls use the room `olunaa-<conversationId>`.

## 3. Firebase Cloud Messaging

Register an Android app in Firebase with the exact package name:

`com.olunaa.app`

Download its `google-services.json` and place it at:

`android/app/google-services.json`

The Android build applies the Google Services plugin only when this file exists. Then add these Vercel environment variables:

- `FIREBASE_SERVICE_ACCOUNT_JSON` — the full Firebase service-account JSON, stored as a secret.
- `FIREBASE_PROJECT_ID` — Firebase project ID (optional if it is present in the service-account JSON).

Enable the Firebase Cloud Messaging API for that Google Cloud project. The service account must be permitted to send FCM messages. Never commit the service-account private key or add it to the APK.

The app registers each FCM device token at `/api/devices/token`; the backend sends high-priority message and incoming-call alerts.

## 4. Deploy and verify

After setting the Vercel variables, redeploy Production. After adding `google-services.json`, push the file and wait for the Android GitHub Actions release build.

Test with two different OLUNAA accounts/devices:
1. Open the same conversation on both devices and confirm a new message appears without waiting for the fallback refresh.
2. Start a voice call, accept it on the second device, then test mute and hang-up.
3. Repeat with video and camera permissions enabled.
4. Close or background the receiver app and verify FCM message and incoming-call notifications.

A green build alone does not prove service credentials, network reachability, microphone/camera permissions, or real device-to-device call quality. Complete the two-device tests before announcing the features as live.
