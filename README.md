# Grand Beauty

The appointment book for Grand Beauty salon — clients, bookings, debt,
reminders, and check-ins. Offline-first, zero dependencies, pure platform
Android (API 24+, targets 36). No Gradle; the whole build peaks ~600MB RAM.

## Features
- Onboarding (name → personalized greetings), light/dark elegant
  pink + gold + charcoal palette
- Bookings: day-grouped list, "Needs attention" section for unmarked past
  bookings, 60-day History, quick mark-done, search (name/service/phone)
- Clients: initials avatar, call/SMS/WhatsApp, stats (visits/total/balance),
  one-tap debt settle, per-client booking history
- Debt tracking: charge vs paid per booking, gold "Owes" badges, salon-wide
  outstanding total
- Reminders: exact alarms (fallback window), lead time 15min–1day
  (default 2h), rescheduled on boot/update/time-change
- Morning summary 8:00 (bookings, expected take, owed, needs-attention count)
- Regular check-ins: gentle nudge when a client is silent 5+ weeks
- CSV backup to Downloads/Bookings, share next-7-days via any app
- Edge cases: past-date booking allowed (lands in history with hint),
  double-booking warning, empty-state CTAs

## Data safety
- Package `com.grandbeauty`, SQLite v2 with forward migration (v1→v2 adds
  `paid`, backfills from `amount` — no data loss)
- Updates install over the old version; data survives (verified 3× on device)
- Keystore: `~/.config/grandbeauty/keystore.jks` (600)
- Password: `~/.config/grandbeauty/signing` (600) — plaintext secret file,
  outside the repo, needed to sign updates. Same cert required forever.

## Build / Install
```
./gen_icons.sh   # only after changing icons
./build.sh       # aapt2 → javac → d8 → zipalign → apksigner
adb install --no-incremental -r GrandBeauty.apk
```
Output: `GrandBeauty.apk` (v2+v3 signed). Bump `--version-code` per release.

## Device-verified (Infinix X6816, Android 11)
- Launch, onboarding, greeting personalization
- Client create → appointment create → debt math ("2,000 owed")
- Settle flow clears balance
- Context menu (call/edit/done/cancel/delete)
- Reminder alarm registered as exact RTC_WAKEUP at lead-time
- DailyReceiver posts summary (verified via exported test build)
- CSV export lands in Downloads/Bookings with paid/balance columns
- Light + dark themes, update-over-data ×3

## Not verified
- 8:00 am firing in the wild (logic verified via explicit broadcast)
- Regular check-in nudge with a real 5-week-lapsed client (query verified,
  trigger path same as daily summary)
