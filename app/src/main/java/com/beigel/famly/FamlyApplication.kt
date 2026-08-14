package com.beigel.famly

import android.app.Application

/**
 * Im Manifest als `android:name=".FamlyApplication"` eingetragen.
 *
 * Aktuell leer: Der Demo-Bestand liegt im Speicher und braucht keine
 * Initialisierung. Sobald Repository und Firebase zurückkommen, gehört der
 * AppContainer hier hin - dann wird daraus wieder
 * `val container: AppContainer by lazy { ... }`.
 */
class FamlyApplication : Application()
