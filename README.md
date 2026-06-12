# Slagalica

Android native aplikacija za predmet Mobilne aplikacije, uradjena u Java + XML tehnologiji.
Registracija, prijava i igre Studenta 1 (`Korak po korak` i `Moj broj`) prosirene su za KT2.

## Preduslovi

- Android Studio
- JDK 17
- Android SDK instaliran kroz Android Studio
- Emulator ili fizicki Android uredjaj

## Pokretanje kroz Android Studio

1. Otvoriti Android Studio.
2. Izabrati `Open` i otvoriti folder `e2-ma-tim19-2026`.
3. Sacekati da Gradle zavrsi sinhronizaciju.
4. Izabrati emulator ili povezan uredjaj.
5. Kliknuti `Run`.

Pocetni ekran aplikacije je `LoginActivity`. Prijava i registracija koriste Firebase Authentication
i Cloud Firestore, ukljucujuci potvrdu email adrese i prijavu korisnickim imenom.

## Pokretanje iz terminala

Iz root foldera projekta:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK se generise u:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Za KT2 registraciju i prijavu potrebno je povezati timski Firebase projekat prema uputstvu u
[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md). Bez `app/google-services.json` aplikacija se gradi,
ali ekrani autentifikacije prikazuju jasnu konfiguracionu gresku umesto mock prijave.

Ako zelite instalaciju na povezan uredjaj ili emulator:

```powershell
.\gradlew.bat installDebug
```

## Ekrani u aplikaciji

- Prijava, registracija, potvrda registracije i reset lozinke
- Glavni meni
- Korak po korak
- Moj broj
- Profil korisnika
- Statistika igraca
- Ko zna zna
- Spojnice
- Asocijacije
- Skocko
- Notifikacije

## Trenutni opseg KT2

- Student 1: Firebase autentifikacija, email potvrda, reset/promena lozinke, `Korak po korak`
  i `Moj broj` sa tajmerima, bodovanjem i shake senzorom.
- Igre se trenutno mogu proveriti u lokalnom hot-seat rezimu za dva igraca.
- Mrezno uparivanje dva uredjaja, zajednicki tok cele partije i trajno cuvanje rezultata pripadaju
  zajednickom sistemu partije koji jos nije povezan sa pojedinacnim igrama.
