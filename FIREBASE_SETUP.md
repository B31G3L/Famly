# Firebase-Setup für Famly

Diese Dateien verdrahten Famly mit einem echten Firestore-Repository +
Firebase Auth (Anonym + Google Sign-In). Damit es baut und läuft, fehlen
noch ein paar Schritte, die nur du in der Firebase Console machen kannst.

## 1. Firebase-Projekt anlegen / verbinden

1. https://console.firebase.google.com → Projekt anlegen (oder ein
   bestehendes aus Dotlist/NexTime wiederverwenden, wenn du alles in einem
   Projekt bündeln willst – spricht auch nichts dagegen, ein eigenes für
   Famly zu nehmen).
2. Android-App hinzufügen mit Package-Name **`com.beigel.famly`**.
3. Die generierte `google-services.json` herunterladen und nach
   `app/google-services.json` legen (liegt NICHT in diesem Zip, da sie
   projektspezifische Keys enthält).

## 2. Auth-Provider aktivieren

In der Firebase Console unter **Authentication → Sign-in method**:
- **Anonym** aktivieren.
- **Google** aktivieren (dafür brauchst du KEINEN eigenen OAuth-Client –
  Firebase legt automatisch einen "Web client (auto created by Google
  Service)" an). Erst danach enthält deine `google-services.json` einen
  Web-Client-Eintrag, und erst dadurch generiert das google-services-Plugin
  die Ressource `R.string.default_web_client_id`, die `MainActivity.kt`
  verwendet. **Ohne diesen Schritt schlägt der Build fehl**, weil die
  Ressource nicht existiert.
- Für Google Sign-In auf einem echten Gerät/Emulator mit Play Services
  muss außerdem der SHA-1-Fingerabdruck deines Debug- (und später
  Release-)Keystores in den Android-App-Einstellungen der Firebase Console
  hinterlegt sein (`./gradlew signingReport` zeigt ihn dir an).

## 3. Firestore anlegen

1. Firestore Database in der Console anlegen (Modus "Production").
2. Die Regeln aus `firestore.rules` (liegt im Zip) übernehmen, z. B. via
   Firebase CLI: `firebase deploy --only firestore:rules`.

## 4. Was die neue Repository-Schicht macht

- Beim App-Start meldet sich `MainActivity` automatisch anonym an
  (`signInAnonymouslyIfNeeded`) und legt – falls noch nicht vorhanden –
  über `ensureFamilyForCurrentUser()` eine Familie mit einer ersten Person
  "Ich" an. Das passiert unsichtbar im Hintergrund, ganz ohne Login-Screen.
- Im Profil-Menü gibt es jetzt einen Eintrag **"Mit Google sichern"**
  (solange der Nutzer nur anonym angemeldet ist). Das verknüpft das
  anonyme Konto mit einem Google-Konto, ohne dass die bereits angelegten
  Daten verloren gehen – Firebase behält dabei automatisch dieselbe UID.
- Alle Lesezugriffe (`familyTree`, `currentUserName`, `inviteCode`) sind
  jetzt `StateFlow`s, die von Firestore-Snapshot-Listenern gespeist werden –
  Änderungen von einem anderen Gerät erscheinen live, ohne Reload.
- `joinFamilyWithCode(code)` ist bereits implementiert, aber noch nicht an
  eine UI angebunden (dazu bräuchte es einen "Code eingeben"-Screen/Dialog,
  der aktuell nicht existiert – gerne als nächsten Schritt).

## 5. Bekannte Vereinfachungen (bewusst außerhalb des jetzigen Scopes)

- `FamilyMember.status` unterscheidet aktuell nur OWNER (die Person mit
  ID `"ich"`) und MEMBER – der ursprüngliche PENDING-Status aus dem
  Fake-Repository gibt es in der echten Version noch nicht, weil dafür ein
  eigenes Feld pro Einladung nötig wäre.
- Es gibt noch keine Push-Notifications (FCM) bei neuen Personen – kommt,
  falls gewünscht, als eigener nächster Schritt (analog zu Dotlist).
- `FakeFamilyRepository` bleibt für Compose-Previews/Tests erhalten, wird
  aber nicht mehr vom `AppContainer` verwendet.
