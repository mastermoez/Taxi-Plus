# FAST TAXI — Android starter prototype

This is a starter Android Studio project for a Driver/Admin taxi app.

## Included in this prototype
- Driver/Admin screen switch
- Runtime location permission request
- GPS distance measurement while a trip is active (Google Fused Location Provider)
- Basic fare calculation: base fare + (distance in km × per-km rate) + demand
- Admin-editable sample fare values
- Recent trip list and basic income summary (in-memory for this prototype)

## Sample fare values
- Base fare: 5,000 MMK
- Per km: 1,000 MMK
- Demand: 0 MMK

These are placeholders only. Change them in the Admin tab to match your actual taxi pricing rules.

## Open and run
1. Install Android Studio (recent stable version).
2. Open this `FastTaxiAndroid` folder.
3. Let Gradle sync and install the Android SDK 35 platform if prompted.
4. Run on a real Android phone. Allow precise location for best distance accuracy.
5. Choose Driver, start a trip, drive, then end the trip.

## Important limitations before real-world launch
- This is an MVP starter, not a production-ready dispatch platform.
- Trip history and rates are currently held in app memory and are lost when the app process is closed.
- Driver/Admin roles are a local screen switch, not secure accounts.
- Admin cannot yet see other phones' trips. A backend database/API and authentication are required.
- The map is currently a GPS status panel. Google Maps display, route polylines, pickup/drop-off pins and API key setup are the next integration.
- Background tracking, foreground service notification, offline recovery, tamper checks, privacy policy and Play Store compliance must be added before commercial deployment.
- Fare calculation uses straight GPS distance accumulated between accepted location updates; it is not road-route distance. GPS drift can affect distance.
