// Recorta los sprites de Negro/Blanco a su contenido y los guarda cuadrados como color_XXXX.png
const SRC=process.argv[2]||'bw/';const PREFIX=process.argv[3]||'color_';
const {PNG}=require('pngjs');const fs=require('fs');
const OUT='C:/Users/Paku/eclipse-workspace/GlyphCatch/app/src/main/res/drawable-nodpi/';
let total=0;
for(let id=1;id<=649;id++){
  const p=PNG.sync.read(fs.readFileSync(SRC+id+'.png'));
  let x0=1e9,y0=1e9,x1=-1,y1=-1;
  for(let y=0;y<p.height;y++)for(let x=0;x<p.width;x++)if(p.data[(y*p.width+x)*4+3]>0){x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);}
  const w=x1-x0+1,h=y1-y0+1,S=Math.max(w,h)+2;const o=new PNG({width:S,height:S});
  const ox=Math.floor((S-w)/2),oy=Math.floor((S-h)/2);
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){const i=((y0+y)*p.width+x0+x)*4,j=((oy+y)*S+ox+x)*4;for(let k=0;k<4;k++)o.data[j+k]=p.data[i+k];}
  const buf=PNG.sync.write(o);total+=buf.length;fs.writeFileSync(OUT+PREFIX+String(id).padStart(4,'0')+'.png',buf);
}
console.log('KB',Math.round(total/1024));
