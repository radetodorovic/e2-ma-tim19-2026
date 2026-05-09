# Slagalica

Android native aplikacija za predmet Mobilne aplikacije. Projekat je KT1 GUI prototip uradjen u Java + XML tehnologiji.

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

Pocetni ekran aplikacije je `LoginActivity`. Za KT1 login je mock, pa unos vodi ka glavnom meniju bez backend provere.

## Pokretanje iz terminala

Iz root foldera projekta:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK se generise u:

```text
app/build/outputs/apk/debug/app-debug.apk
```

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

## Napomena za KT1

Aplikacija koristi mock podatke. Nisu implementirani backend, baza, Firebase, pravi multiplayer, trajno cuvanje rezultata niti realna autentifikacija.
