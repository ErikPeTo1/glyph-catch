// Genera los arrays de nombres de Pokémon y tipos por idioma desde PokeAPI
const fs=require('fs');const RES='C:/Users/Paku/eclipse-workspace/GlyphCatch/app/src/main/res/';
const rd=f=>fs.readFileSync('csv/'+f+'.csv','utf8').trim().split(/\r?\n/).slice(1).map(l=>{const m=l.match(/^(\d+),(\d+),(.*?)(,|$)/);return m?[+m[1],+m[2],m[3].replace(/^"|"$/g,'')]:null;}).filter(Boolean);
const esc=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/'/g,"\'").replace(/"/g,'\\"').replace(/^\?/,'\?').replace(/^@/,'\@');
const langs={'':9,'-es':7,'-de':6,'-fr':5,'-it':8};
const species=rd('pokemon_species_names');const types=rd('type_names');
const typeIds=['normal','fighting','flying','poison','ground','rock','bug','ghost','steel','fire','water','grass','electric','psychic','ice','dragon','dark','fairy'];
for(const [suffix,lang] of Object.entries(langs)){
  const map={};species.filter(r=>r[1]===lang).forEach(r=>map[r[0]]=r[2]);
  const en={};species.filter(r=>r[1]===9).forEach(r=>en[r[0]]=r[2]);
  const items=[];for(let id=1;id<=649;id++)items.push('        <item>'+esc(map[id]||en[id])+'</item>');
  const t={};types.filter(r=>r[1]===lang).forEach(r=>t[r[0]]=r[2]);
  const typeStrings=typeIds.map((k,i)=>`    <string name="type_${k}">${esc(t[i+1])}</string>`);
  fs.mkdirSync(RES+'values'+suffix,{recursive:true});
  fs.writeFileSync(RES+'values'+suffix+'/pokemon_names.xml',`<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated from PokeAPI (tools/gen5/names.js) -->\n<resources>\n    <string-array name="pokemon_names">\n${items.join('\n')}\n    </string-array>\n${typeStrings.join('\n')}\n</resources>\n`);
  console.log(suffix||'en', Object.keys(map).filter(k=>k<=649).length, map[25], t[12]);
}
