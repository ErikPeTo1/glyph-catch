// Genera los sprites de formas: matriz (sprite_/matrix_) y app (color_/shiny_)
const {convert,preview,PNG}=require('./conv.js');const fs=require('fs');
const RES='C:/Users/Paku/eclipse-workspace/GlyphCatch/app/src/main/res/';
function crop(file){const p=PNG.sync.read(fs.readFileSync(file));let x0=1e9,y0=1e9,x1=-1,y1=-1;
  for(let y=0;y<p.height;y++)for(let x=0;x<p.width;x++)if(p.data[(y*p.width+x)*4+3]>0){x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);}
  const w=x1-x0+1,h=y1-y0+1,S=Math.max(w,h)+2;const o=new PNG({width:S,height:S});const ox=Math.floor((S-w)/2),oy=Math.floor((S-h)/2);
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){const i=((y0+y)*p.width+x0+x)*4,j=((oy+y)*S+ox+x)*4;for(let k=0;k<4;k++)o.data[j+k]=p.data[i+k];}return PNG.sync.write(o);}
let n=0;
for(const f of fs.readdirSync('forms')){if(f.startsWith('shiny_'))continue;const key=f.replace('.png','');
  const sp=convert('forms/'+f);fs.writeFileSync(RES+'drawable/sprite_'+key+'.png',PNG.sync.write(sp));fs.writeFileSync(RES+'drawable/matrix_'+key+'.png',PNG.sync.write(preview(sp)));
  fs.writeFileSync(RES+'drawable-nodpi/color_'+key+'.png',crop('forms/'+f));fs.writeFileSync(RES+'drawable-nodpi/shiny_'+key+'.png',crop('forms/shiny_'+f));n++;}
console.log(n);
