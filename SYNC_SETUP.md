# Expense Tracker — Live Sync Setup

The Android app now supports shared cloud sync using Firebase Realtime Database REST APIs.

## What it does

- Same expense data can be used on two phones.
- Add/edit/delete changes are merged using `updatedAt` timestamps.
- Deletes are stored as tombstones so they do not reappear on the other phone.
- When the app is open, it checks for remote changes every 5 seconds.
- The existing local data remains available when sync is not configured.

## 1. Create a Firebase project

Create a Firebase project and add a Realtime Database.

Enable **Authentication → Sign-in method → Anonymous**.

## 2. Realtime Database rules

Use authenticated-only rules. Do not make the database publicly readable/writable.

```json
{
  "rules": {
    "expenseRooms": {
      "$room": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

The app uses Firebase Anonymous Authentication before reading or writing the room.

## 3. Get the two values needed by the app

You need:

- **Realtime Database URL** — for example `https://YOUR_PROJECT-default-rtdb.firebaseio.com`
- **Web API key** — from Firebase project settings → General → Your apps / Web API key

The API key is not a database password. Database access is protected by the authenticated database rules.

## 4. Pair both phones

Open the app → Dashboard → cloud/settings icon.

Enter the same:

1. Firebase Database URL
2. Web API key
3. Shared room code

Use a long random room code, for example:

`JO_EXP_2026_8F4K9Q2M`

Both phones must use exactly the same room code.

## Important

The current implementation is **near-real-time while the app is open**: approximately a 5-second polling interval. It is not a push notification service and does not continuously synchronize a completely closed app.

For production use, the next upgrade should replace polling with Firebase Realtime Database streaming and add per-user authentication/room membership instead of relying on a shared room code.
