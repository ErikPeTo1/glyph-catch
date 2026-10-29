// Genera img_toy_preview.png (icono del Glyph Toy) a partir de un sprite 25x25: node toy_preview.js 258  (o un png 25x25: node toy_preview.js mudkip_icon_25.png)
const {PNG}=require('pngjs');const fs=require('fs');
const RES=__dirname+'/../../app/src/main/res/drawable/';
const arg=process.argv[2]||'258';
const sp=PNG.sync.read(fs.readFileSync(/.png$/.test(arg)?arg:RES+'sprite_'+arg.padStart(4,'0')+'.png'));
const W=512,out=new PNG({width:W,height:W});
for(let y=0;y<W;y++)for(let x=0;x<W;x++){const i=(y*W+x)*4;const d=Math.hypot(x+0.5-256,y+0.5-256);
  out.data[i]=out.data[i+1]=out.data[i+2]=0;out.data[i+3]=d<=256?255:0;}
for(let cy=0;cy<25;cy++)for(let cx=0;cx<25;cx++){const j=(cy*25+cx)*4;if(!sp.data[j+3])continue;const v=sp.data[j];
  const x0=Math.round(36+cx*17.79),y0=Math.round(36+cy*17.79);
  for(let y=y0;y<y0+13;y++)for(let x=x0;x<x0+13;x++){const i=(y*W+x)*4;out.data[i]=out.data[i+1]=out.data[i+2]=v;}}
fs.writeFileSync(RES+'img_toy_preview.png',PNG.sync.write(out));console.log('ok',arg);
