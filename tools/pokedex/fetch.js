// Builds the Pokédex entries (category, description, height, weight, gender differences) for
// the 649 species from PokeAPI. Responses are cached in tools/pokedex/cache, so it can be re-run.
//
//   node tools/pokedex/fetch.js
//
// Writes app/src/main/res/raw/pokedex_stats.json and raw[-xx]/pokedex_text.json. Portuguese has
// no Pokédex texts in PokeAPI, so it falls back to English like the Pokémon names.
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', '..');
const RES = path.join(ROOT, 'app', 'src', 'main', 'res');
const CACHE = path.join(__dirname, 'cache');
const COUNT = 649;
const LANGS = { en: 'raw', es: 'raw-es', de: 'raw-de', fr: 'raw-fr', it: 'raw-it' };
// Black and White first, then the closest games that have the text in that language
const VERSIONS = [
  'black', 'white', 'black-2', 'white-2', 'x', 'y', 'omega-ruby', 'alpha-sapphire', 'sun', 'moon',
  'ultra-sun', 'ultra-moon', 'lets-go-pikachu', 'lets-go-eevee', 'sword', 'shield', 'brilliant-diamond',
  'shining-pearl', 'legends-arceus', 'scarlet', 'violet'
];

fs.mkdirSync(CACHE, { recursive: true });

async function get(kind, id) {
  const file = path.join(CACHE, `${kind}-${id}.json`);
  if (fs.existsSync(file)) return JSON.parse(fs.readFileSync(file, 'utf8'));
  for (let attempt = 0; attempt < 4; attempt++) {
    const res = await fetch(`https://pokeapi.co/api/v2/${kind}/${id}`);
    if (res.ok) {
      const json = await res.json();
      fs.writeFileSync(file, JSON.stringify(json));
      return json;
    }
    await new Promise(r => setTimeout(r, 1000 * (attempt + 1)));
  }
  throw new Error(`${kind}/${id} failed`);
}

const clean = text => text.replace(/[\f\n\r­]+/g, ' ').replace(/\s+/g, ' ').replace(/ ([!?:;»])/g, ' $1').trim();

async function main() {
  const ids = Array.from({ length: COUNT }, (_, i) => i + 1);
  const species = {};
  const pokemon = {};
  // A few at a time, to be kind to PokeAPI
  for (let i = 0; i < ids.length; i += 8) {
    await Promise.all(ids.slice(i, i + 8).map(async id => {
      species[id] = await get('pokemon-species', id);
      pokemon[id] = await get('pokemon', id);
    }));
    process.stdout.write(`\r${Math.min(i + 8, COUNT)}/${COUNT}`);
  }
  console.log();

  const stats = {};
  for (const id of ids) {
    stats[id] = {
      h: pokemon[id].height, // decimetres
      w: pokemon[id].weight, // hectograms
      f: species[id].has_gender_differences ? 1 : 0,
      g: species[id].gender_rate // -1 genderless, 0 all male … 8 all female
    };
  }
  fs.writeFileSync(path.join(RES, 'raw', 'pokedex_stats.json'), JSON.stringify(stats));

  for (const [lang, dir] of Object.entries(LANGS)) {
    const text = {};
    let missing = 0;
    for (const id of ids) {
      const entries = species[id].flavor_text_entries.filter(f => f.language.name === lang);
      const entry = VERSIONS.map(v => entries.find(f => f.version.name === v)).find(Boolean) || entries[0];
      const genus = species[id].genera.find(g => g.language.name === lang);
      if (!entry) missing++;
      text[id] = { c: genus ? genus.genus : '', d: entry ? clean(entry.flavor_text) : '' };
    }
    fs.mkdirSync(path.join(RES, dir), { recursive: true });
    fs.writeFileSync(path.join(RES, dir, 'pokedex_text.json'), JSON.stringify(text));
    console.log(`${lang}: ${missing} without a description`);
  }
}

main().catch(e => {
  console.error(e);
  process.exit(1);
});
