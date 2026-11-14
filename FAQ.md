# FAQ

## Can I fail catches?

No: when you throw a Poké Ball, you have a 100% chance of catching the Pokémon. Glyph Catch is designed to be consistently rewarding and stress-free. Up to nine Pokémon can accumulate, and they stay indefinitely, even if you do unlock your phone after. The only chance element is finding Pokémon.

## What if the phone is in my pocket instead of face-down?

That's fine: as long as your phone stays locked, it counts. You won't lose your streak by turning on the screen to check the time or a notification; only unlocking the phone resets it.

## Won't I just accumulate a ton of Pokémon overnight?

Pokémon keep appearing quietly during bedtime, without animations, so you may wake up to a full queue of up to nine. You can adjust your sleep schedule in the settings menu. If you sleep at least 6 hours without unlocking your phone, you'll be well rested: a bonus chance of rare encounters the next day.

## Does this impact my battery life?

Not noticeably. And if you actually end up using your phone for just a few minutes less per day, that's almost certainly a net gain for battery life.

## Can I set a dark background for the Glyph Toy?

No - the source sprites would need manual reworking for dark backgrounds (shading, outlines), on top of adapting them to the 25x25 monochrome display. With 649 Pokémon, it's beyond the scope of this fan project.

However: if you prefer, you can dim the sprite and background slightly under Settings > Glyph Toy.

## No Pokémon are appearing for me, what's wrong?

Make sure you configured Glyph Catch as your Always-on Glyph Toy: open Android settings > Glyph Interface > Glyph Toys > ⚙️ > Always-on Glyph Toys and pick Glyph Catch. It is recommended to set the timeout to 1 minute or less.

Pokémon can start appearing after about 8 minutes without unlocking your phone, and one is guaranteed after 30.

Note that Essential Notifications may disconnect the Glyph Toy and disrupt the timer. If you are getting too many Essential Notifications, consider scheduling quiet time in apps that support it and/or narrowing the Essential Notification rules.

## Can I keep my progress from the original Glyph Catch?

Yes! Your Pokémon, Pokédex and Bag come along:

1. Update the original Glyph Catch to [v1.3.0](https://github.com/equalparts/glyph-catch/releases/tag/v1.3.0).
2. In the original app, go to Settings > Export progress and save the file.
3. Install this version (it installs alongside the original), go to Settings > Backup > Import progress and pick that file.
4. Set this version as your Always-on Glyph Toy. Once everything is there, you can uninstall the original.

Importing replaces the progress you have in this version, so do it before you start playing. From then on, you can back up your progress under Settings > Backup.

## Privacy

Glyph Catch does not collect any personal data.

When you open it, the app checks this repository's GitHub Releases to tell you when a new version is available. Only the public release information is requested; nothing about you or your game is sent.

The Pokédex shows animated sprites from Pokémon Black and White and plays each Pokémon's cry. Each sprite and cry is downloaded from PokeAPI on GitHub ([sprites](https://github.com/PokeAPI/sprites), [cries](https://github.com/PokeAPI/cries)) the first time it's needed and kept on your phone. Only the file is requested.

Live weather sync uses the popular OpenWeatherMap API with a geographical location that you configure, no live location. Refer to the [OpenWeatherMap privacy policy](https://openweather.co.uk/privacy-policy) for details.

There is a "Save logs" option (off by default) under "Debugging" for troubleshooting purposes. If enabled, these logs are saved locally on your device, not automatically sent to a server. You can inspect or delete the logs at any point.

## Building

Requires JDK 21.

```bash
./gradlew :app:assembleDebug     # Build a debug APK
./gradlew :app:installDebug      # Install on a connected phone
./gradlew :app:testDebugUnitTest # Run unit tests
./gradlew :app:ktlintCheck       # Check code style
```

Translations live in `tools/i18n` and are generated with `node tools/i18n/build.js`.
