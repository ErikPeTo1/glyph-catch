const fs=require('fs');
const rd=f=>{const [h,...r]=fs.readFileSync('csv/'+f+'.csv','utf8').trim().split(/\r?\n/);const H=h.split(',');return r.map(l=>{const v=l.split(',');const o={};H.forEach((k,i)=>o[k]=v[i]);return o;});};
const sp=rd('pokemon_species'),ev=rd('pokemon_evolution'),items=rd('items'),types=rd('types'),ptypes=rd('pokemon_types'),names=rd('pokemon_species_names');
const it={};items.forEach(i=>it[i.id]=i.identifier);const ty={};types.forEach(t=>ty[t.id]=t.identifier.toUpperCase());
const S={};sp.filter(s=>+s.id<=649).forEach(s=>S[s.id]=s);
const disp={};names.filter(n=>n.local_language_id=='9').forEach(n=>{if(S[n.pokemon_species_id])disp[n.pokemon_species_id]=n.name;});
const T={};ptypes.filter(p=>S[p.pokemon_id]).sort((a,b)=>a.slot-b.slot).forEach(p=>(T[p.pokemon_id]=T[p.pokemon_id]||[]).push(ty[p.type_id]));
const C=id=>S[id].identifier.toUpperCase().replace(/-/g,'_');
// overrides: target id -> requirement string
const O={25:'at 12',35:'at 12',39:'at 12',113:'at 15',122:'at 18',143:'at 25',169:'at 35',176:'at 15',183:'at 12',185:'at 17',
186:'byTrade',196:'with Item.SUN_STONE',197:'with Item.MOON_STONE',199:'byTrade',208:'byTrade',212:'byTrade',226:'at 25',230:'byTrade',233:'byTrade',
242:'at 40',315:'at 15',350:'byTrade',358:'at 15',367:'byTrade',368:'byTrade',413:'at 20',414:'at 20',416:'at 21',424:'at 32',428:'at 30',448:'at 25',
461:'with Item.DUSK_STONE',462:'with Item.THUNDER_STONE',463:'at 33',464:'byTrade',465:'at 33',466:'byTrade',467:'byTrade',469:'at 33',470:'with Item.LEAF_STONE',
471:'with Item.ICE_STONE',472:'with Item.DUSK_STONE',473:'at 34',474:'byTrade',475:'with Item.DAWN_STONE',476:'with Item.THUNDER_STONE',477:'byTrade',478:'with Item.DAWN_STONE',
528:'at 30',542:'at 30',589:'byTrade',617:'byTrade',292:null};
function req(id){if(id in O)return O[id];const e=ev.find(e=>e.evolved_species_id==id);if(!e)return null;
 if(e.evolution_trigger_id=='1'&&e.minimum_level)return 'at '+e.minimum_level;
 if(e.evolution_trigger_id=='3')return 'with Item.'+it[e.trigger_item_id].toUpperCase().replace(/-/g,'_');
 if(e.evolution_trigger_id=='2')return 'byTrade';throw new Error('unhandled '+id);}
const lines=[],late=[];
for(let id=152;id<=649;id++){if(id==225||id==234)continue;const s=S[id];const t=T[id];
 const from=s.evolves_from_species_id;let f='';const r=from?req(id):null;
 if(from&&r){if(+from<id)f=`, from = ${C(from)} ${r.startsWith('by')?'.'+r:r}`.replace(' .','.');else late.push(`evolve(${C(id)}, ${C(from)}${r.startsWith('by')?'.'+r:' '+r})`);}
 const n=JSON.stringify(disp[id]);lines.push(`    val ${C(id)} = add(${id}, ${n}, Type.${t[0]}${t[1]?', Type.'+t[1]:''}${f})`);}
// gen1 targets with gen2+ sources
for(const id of [25,35,39,113,122,124,125,126,143]){const s=S[id];late.push(`evolve(${C(id)}, ${C(s.evolves_from_species_id)} ${O[id]||req(id)})`);}
fs.writeFileSync('out_species.kt',lines.join('\n')+'\n');fs.writeFileSync('out_late.kt',late.map(l=>'        '+l).join('\n')+'\n');
// pools
const legend=id=>S[id].is_legendary=='1'||S[id].is_mythical=='1';
const fishing=[170,211,222,223,458,318,320,339,341,349,366,369,370,456,550,592,594];
const starters=[152,155,158,252,255,258,387,390,393,495,498,501];
const fossils=[345,347,408,410,564,566];
const pseudo=[246,371,374,443,633];
const night=new Set([163,198,200,302,353,355,425,527,607,570]);
const special=new Set([292]);
const pools={Common:[],Uncommon:[],Rare:[],Fishing:[]};
for(let id=152;id<=649;id++){const s=S[id];if(id==225||id==234)continue;
 const isBase=!s.evolves_from_species_id||(id in O&&O[id]===null);
 if(!isBase||legend(id)||special.has(id)||fossils.includes(id))continue;
 let pool,w='1.0f';const cr=+s.capture_rate;
 if(fishing.includes(id))pool='Fishing';else if(starters.includes(id))pool='Rare';else if(pseudo.includes(id)){pool='Rare';w='0.5f';}
 else if(s.is_baby=='1'){pool='Uncommon';w='0.5f';}else if(cr>=150)pool='Common';else if(cr>=60)pool='Uncommon';else {pool='Rare';w=cr<=30?'0.5f':'1.0f';}
 let line=`Pokemon.${C(id)} at ${w}`;if(night.has(id))line=`Pokemon.${C(id)} at 2.0f during time::night`;
 pools[pool].push('            '+line);}
for(const k in pools)fs.writeFileSync('out_pool_'+k+'.kt',pools[k].join('\n')+'\n');
console.log(lines.length,late.length,Object.entries(pools).map(([k,v])=>k+':'+v.length).join(' '));
console.log('legend',Object.keys(S).filter(id=>+id>151&&legend(id)).map(id=>id+':'+C(id)).join(' '));
