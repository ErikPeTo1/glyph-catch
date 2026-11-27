# Privacy

Glyph Catch doesn't need an account and keeps your game on your phone. The only thing it can
send anywhere is a small set of anonymous stats, and only if you turn them on.

## Anonymous stats

They're **off unless you turn them on**: when a call asks you, or in
**Settings > Data & privacy**. They help me see which phones and settings people use, so I know
what to improve.

**What's sent**, once a day at most, when you open the app:

- **App:** its version.
- **Phone:** maker and model (like `Nothing A024`), system build, Android version, and whether it
  has a Glyph Matrix.
- **Language** of the app.
- **Settings:** spawn pace, how many Pokémon can wait, color sprites, what the Glyph Matrix shows
  while idle, lower brightness, and how many of each widget you've placed.
- **Progress:** Pokédex entries, shinies registered, Pokémon caught and days since your first
  catch.
- **Bag:** how many Incenses, evolution stones and Linking Cords you have.

**What's never sent:** your name or the name you gave Professor Oak, nicknames, your location,
your contacts, your Google account, your phone's identifiers, or anything else on your phone.

"See what's sent" in **Settings > Data & privacy** shows exactly what goes, as it will be sent.

### How it works

- Each install gets a random id from Firebase's anonymous sign-in. It isn't linked to you, your
  account or your phone, and you never sign in to anything.
- The stats are a single document per install in Google's Firebase (Cloud Firestore), stored in
  Europe (Madrid). Each new send replaces the last one, so there's no history.
- Only I can read them. The app itself can't read anyone's stats, its own included.
- I use them only to improve Glyph Catch. They're never sold, shared or used for ads.

### Deleting them

Turn the stats off in **Settings > Data & privacy**: the app deletes its document and the random
id right away. If you turn them on again later, you start over with a new id.

Uninstalling the app doesn't delete them, so turn them off first. Since they aren't linked to
you, there's no way to tell which ones were yours afterwards.

## Everything else

- **Weather:** if you turn on live weather, the app asks [Open-Meteo](https://open-meteo.com/) for
  the weather at coarsened coordinates of the place you chose (about 11 km precision). No API key
  or account is used. Nothing else goes with the request. Weather data by Open-Meteo.com (CC BY 4.0).
- **Sprites and cries:** the animated sprites and the Pokémon cries are downloaded from PokeAPI's
  public files on GitHub the first time they're needed. Only the file is requested.
- **Updates:** the app checks GitHub Releases for a newer version. Only the public release
  information is requested.
- **Backups** are files you export yourself and keep wherever you like.

## Questions

Open an [issue](https://github.com/ErikPeTo1/glyph-catch/issues) on GitHub.
