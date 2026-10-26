// Keep the active editor inside the resized WebView/visual viewport.
const editable=el=>el?.matches?.('textarea,input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit]):not([type=hidden])');
let nativeOpen=false,baseline=innerHeight,frame=0;
function keepVisible(){
 const el=document.activeElement,v=window.visualViewport;
 const height=v?.height||innerHeight,top=v?.offsetTop||0;
 const open=nativeOpen||editable(el)&&baseline-height>120;
 document.body.classList.toggle('keyboard-open',!!open);
 if(!editable(el)||!el.isConnected)return;
 const r=el.getBoundingClientRect(),bottom=top+height-24;
 if(r.bottom>bottom||r.top<top+16){
  el.scrollIntoView({block:'nearest',inline:'nearest',behavior:'instant'});
  const next=el.getBoundingClientRect();
  if(next.bottom>bottom)window.scrollBy(0,next.bottom-bottom);
  else if(next.top<top+16)window.scrollBy(0,next.top-top-16);
 }
}
function schedule(){cancelAnimationFrame(frame);frame=requestAnimationFrame(keepVisible);}
document.addEventListener('focusin',()=>{schedule();setTimeout(schedule,300);});
document.addEventListener('focusout',()=>setTimeout(schedule,0));
window.addEventListener('resize',()=>{if(!editable(document.activeElement))baseline=Math.max(baseline,innerHeight);schedule();});
window.visualViewport?.addEventListener('resize',schedule);
window.visualViewport?.addEventListener('scroll',schedule);
window.addEventListener('keyboard-visibility',e=>{nativeOpen=!!e.detail?.visible;schedule();});
