const {PNG}=require('pngjs');const fs=require('fs');
const RES='C:/Users/Paku/eclipse-workspace/GlyphCatch/app/src/main/res/drawable/';
const mask=PNG.sync.read(fs.readFileSync(RES+'sprite_0025.png'));
const N=25;
function lum(r,g,b){return 0.299*r+0.587*g+0.114*b;}
function convert(file,opt={}){
  const S=opt.size||24, wMin=opt.wMin??0.5, gamma=opt.gamma??1.3;
  const p=PNG.sync.read(fs.readFileSync(file));
  let x0=1e9,y0=1e9,x1=-1,y1=-1;
  for(let y=0;y<p.height;y++)for(let x=0;x<p.width;x++)if(p.data[(y*p.width+x)*4+3]>40){x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);}
  const bw=x1-x0+1,bh=y1-y0+1;const sc=Math.min(S/bw,S/bh,opt.maxScale||1);
  const tw=bw*sc,th=bh*sc;const ox=(N-tw)/2,oy=(N-th)/2+ (opt.yShift||0);
  const out=new Float32Array(N*N).fill(255);const covA=new Float32Array(N*N);const edgeSet=new Set();
  // edge detection in source: pixel opaque adjacent to transparent -> outline (already dark in BW sprites)
  for(let ty=0;ty<N;ty++)for(let tx=0;tx<N;tx++){
    const sx0=x0+(tx-ox)/sc,sx1=x0+(tx+1-ox)/sc,sy0=y0+(ty-oy)/sc,sy1=y0+(ty+1-oy)/sc;
    let sum=0,wsum=0,mn=255,cov=0;
    for(let sy=Math.floor(sy0);sy<Math.ceil(sy1);sy++)for(let sx=Math.floor(sx0);sx<Math.ceil(sx1);sx++){
      const w=(Math.min(sx+1,sx1)-Math.max(sx,sx0))*(Math.min(sy+1,sy1)-Math.max(sy,sy0));if(w<=0)continue;
      let v=255,a=0;
      if(sx>=0&&sy>=0&&sx<p.width&&sy<p.height){const i=(sy*p.width+sx)*4;a=p.data[i+3]/255;v=lum(p.data[i],p.data[i+1],p.data[i+2]);}
      if(a>0.15)mn=Math.min(mn,v);
      sum+=w*(a*v+(1-a)*255);wsum+=w;cov+=w*a;}
    if(!wsum)continue;let avg=sum/wsum;cov/=wsum;covA[ty*N+tx]=cov;
    let v=avg; if(cov>0.2) v=(1-wMin)*avg+wMin*mn*Math.min(1,cov*1.5)+wMin*avg*(1-Math.min(1,cov*1.5));
    out[ty*N+tx]=v;}
  if(opt.outline!==false){const o=out.slice();for(let y=0;y<N;y++)for(let x=0;x<N;x++){const k=y*N+x;if(covA[k]<0.35)continue;let edge=false;for(const[dx,dy]of[[1,0],[-1,0],[0,1],[0,-1]]){const X=x+dx,Y=y+dy;if(X<0||Y<0||X>=N||Y>=N||covA[Y*N+X]<0.35)edge=true;}if(edge){o[k]=Math.min(out[k],opt.edgeV??40);edgeSet.add(k);}}out.set(o);}
  // contrast stretch for interior (non-white) pixels
  let vals=[];out.forEach((v,k)=>{if(v<250&&!edgeSet.has(k))vals.push(v);});vals.sort((a,b)=>a-b);
  const lo=vals[Math.floor(vals.length*0.03)]||0, hi=vals[Math.floor(vals.length*0.97)]||255;
  // adaptive gamma: bring the median interior tone to mid-grey so dark Pokémon keep their details
  const med=Math.max(0.02,Math.min(0.98,((vals[Math.floor(vals.length/2)]||128)-lo)/Math.max(1,hi-lo)));
  const g=Math.max(0.45,Math.min(1.3,Math.log(opt.target??0.5)/Math.log(med)));
  const png=new PNG({width:N,height:N});
  for(let i=0;i<N*N;i++){let v=out[i];
    if(v<250){v=Math.max(0,Math.min(1,(v-lo)/Math.max(1,(hi-lo))));v=edgeSet.has(i)?Math.min(v,0.15)*(opt.top??250):Math.pow(v,g)*(opt.top??250);}
    v=Math.round(v);const a=mask.data[i*4+3];png.data[i*4]=png.data[i*4+1]=png.data[i*4+2]=a?v:0;png.data[i*4+3]=a;}
  return png;}
function preview(sp){const W=300;const png=new PNG({width:W,height:W});
  for(let y=0;y<W;y++)for(let x=0;x<W;x++){const cx=Math.floor(x/12),cy=Math.floor(y/12),j=(cy*N+cx)*4,i=(y*W+x)*4;
    const a=sp.data[j+3];const inner=(x%12)!=0&&(x%12)!=11&&(y%12)!=0&&(y%12)!=11;
    png.data[i]=png.data[i+1]=png.data[i+2]=a?sp.data[j]:0;png.data[i+3]=a?(inner?255:102):0;}
  return png;}
module.exports={convert,preview,PNG};
if(require.main===module){
  // contact sheet: original vs converted for given ids
  const ids=process.argv.slice(2).map(Number);const C=26*4;const sheet=new PNG({width:ids.length*C,height:C*2});sheet.data.fill(40);
  ids.forEach((id,k)=>{
    const conv=convert(`bw/${id}.png`);
    let orig=null;try{orig=PNG.sync.read(fs.readFileSync(RES+'sprite_'+String(id).padStart(4,'0')+'.png'));}catch{}
    [orig,conv].forEach((sp,row)=>{if(!sp)return;for(let y=0;y<N*4;y++)for(let x=0;x<N*4;x++){const j=(Math.floor(y/4)*N+Math.floor(x/4))*4;const i=((row*C+y)*sheet.width+k*C+x)*4;
      if(sp.data[j+3]){sheet.data[i]=sheet.data[i+1]=sheet.data[i+2]=sp.data[j];}sheet.data[i+3]=255;}});
    for(let y=0;y<C*2;y++)for(let x=0;x<C;x++){const i=(y*sheet.width+k*C+x)*4;sheet.data[i+3]=255;}
  });
  fs.writeFileSync('sheet.png',PNG.sync.write(sheet));}
