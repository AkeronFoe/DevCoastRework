/* DevCoast · landing: scroll como historia (docs/diseno/DEVCOAST-LANDING-MOTION.md). Solo transform y opacity; vanilla. */
(function(){var $=function(s){return document.querySelector(s)}, $$=function(s){return Array.prototype.slice.call(document.querySelectorAll(s))};
  var clamp=function(v,a,b){a=a==null?0:a;b=b==null?1:b;return v<a?a:(v>b?b:v)};
  var lerp=function(a,b,t){return a+(b-a)*t};
  var ease=function(t){return t<.5?2*t*t:1-Math.pow(-2*t+2,2)/2};
  var root=document.documentElement, body=document.body;
  var rmq=window.matchMedia('(prefers-reduced-motion: reduce)');
  var rm=false, simple=false, H=window.innerHeight, S=0, max=1, force=true, ticking=false;
  var ids=['hero','ruta','pases','gaviota','muelles','crear'].filter(function(id){return document.getElementById(id)}), M={}, last={};

  /* escribe solo si el valor cambió */
  function sty(el,k,v){var c=el.__c||(el.__c={}); if(c[k]===v) return; c[k]=v; el.style[k]=v;}
  function sv(el,k,v){var c=el.__c||(el.__c={}); var key='--'+k; if(c[key]===v) return; c[key]=v; el.style.setProperty(key,v);}
  function changed(id,p){ if(force||last[id]===undefined||Math.abs(last[id]-p)>0.0002){last[id]=p;return true;} return false; }

  function setMode(){
    rm=rmq.matches; simple=rm||window.innerWidth<820;
    root.classList.toggle('rm',rm); root.classList.toggle('simple',simple);
  }
  /* se mide una sola vez (y al cambiar el tamaño), no en cada scroll */
  function measure(){
    H=window.innerHeight; S=window.pageYOffset;
    ids.forEach(function(id){var el=document.getElementById(id), r=el.getBoundingClientRect(); M[id]={top:r.top+S,h:el.offsetHeight};});
    max=Math.max(1,root.scrollHeight-H);
    var bh=$('#hudBar').clientHeight; if(bh) root.style.setProperty('--bh',bh+'px');
    force=true;
  }
  function prog(id){
    if((simple&&id!=='hero')||!M[id]) return 1;
    var m=M[id], total=m.h-H; return total>0?clamp((S-m.top)/total):1;
  }

  /* piezas clonadas: barco de la ruta y gaviota del recuadro (misma geometría que el barco principal) */
  var boat=$('#boatWrap .boat'), gullSrc=boat.querySelector('.gull');
  var mini=boat.cloneNode(true);
  mini.removeAttribute('id'); mini.removeAttribute('class'); mini.removeAttribute('role'); mini.removeAttribute('aria-label'); mini.setAttribute('aria-hidden','true');
  Array.prototype.forEach.call(mini.querySelectorAll('[id]'),function(n){n.removeAttribute('id')});
  var miniWrap=$('#miniWrap'); miniWrap.appendChild(mini);
  var tileG=gullSrc.cloneNode(true); tileG.removeAttribute('id'); $('#tileSvg').appendChild(tileG);
  var looks=$$('.gl-look');

  /* la gaviota mira hacia el cursor */
  var mx=window.innerWidth*0.5, my=window.innerHeight*0.5, lookPend=false;
  function lookAt(){
    lookPend=false; if(rm) return;
    looks.forEach(function(g){
      if(g.closest('.off')) return;
      var b=g.getBoundingClientRect(); if(!b.width) return;
      var cy=b.top+b.height/2, a=clamp((cy-my)/(window.innerHeight*0.5),-1,1)*8;
      sv(g,'look',a.toFixed(1));
    });
  }
  window.addEventListener('pointermove',function(e){mx=e.clientX;my=e.clientY;if(!lookPend){lookPend=true;requestAnimationFrame(lookAt);}},{passive:true});
  function hop(g){ if(rm) return; g.classList.remove('hop'); void g.getBoundingClientRect(); g.classList.add('hop'); setTimeout(function(){g.classList.remove('hop')},950); }

  /* se pausan las animaciones de lo que no se ve */
  if('IntersectionObserver' in window){
    var io=new IntersectionObserver(function(es){es.forEach(function(e){e.target.classList.toggle('off',!e.isIntersecting)})});
    ['hero','ruta','gaviota'].forEach(function(id){var el=document.getElementById(id); if(el) io.observe(el)});
  }

  /* ruta */
  var base=$('#routeBase'), fill=$('#routeFill'), len=base.getTotalLength();
  fill.style.strokeDasharray=len; fill.style.strokeDashoffset=len;
  var portsF=[0.17,0.5,0.84];
  function pt(f){return base.getPointAtLength(len*clamp(f))}
  var nodes=portsF.map(function(f,i){var p=pt(f), n=$('#n'+i); n.style.left=(p.x/12)+'%'; n.style.top=((p.y-90)/2.5)+'%'; return n;});
  var cards=[$('#pc0'),$('#pc1'),$('#pc2')];

  var heroEl=$('#hero'), sea=$('#sea'), boatWrap=$('#boatWrap'), heroText=$('#heroText'), hud=$('#hud'), bub=$('#bubbles');
  var hopped=false, quoteHop=false, lastBg='';
  var words=(function(){var q=$('#quote'), t=q.textContent.trim().split(/\s+/); q.innerHTML=t.map(function(w){return '<span class="qw">'+w+'</span>'}).join(' '); return $$('.qw');})();
  var tks=$$('.tk'), stubs=tks.map(function(t){return t.querySelector('.stub')});
  var ranges=[[0,.3],[.18,.48],[.36,.66]];
  var c0=[14,42,45], c1=[6,17,19];

  function update(){
    ticking=false; S=window.pageYOffset;
    var g=clamp(S/max), heroEnd=M.hero.h-H;

    /* fondo (se cambia solo cuando el color cambia de verdad) y profundidad */
    var bg=clamp((S-heroEnd)/Math.max(1,max-heroEnd));
    var bgs='rgb('+c0.map(function(v,i){return Math.round(lerp(v,c1[i],bg))}).join(',')+')';
    if(bgs!==lastBg){lastBg=bgs; body.style.background=bgs; root.style.setProperty('--bgnow',bgs);}
    sty(hud,'opacity',(rm?1:clamp((S-H*0.25)/(H*0.5))).toFixed(2));
    var d=Math.round(g*40); if(hud.__d!==d){hud.__d=d; $('#hudN').textContent=d;}
    sv(root,'g',g.toFixed(4));
    sty(bub,'opacity',(rm?0:clamp((S-heroEnd*0.7)/(H*0.5))*0.9).toFixed(2));

    /* HERO: el mar sube y te sumerge */
    var p=rm?0:prog('hero');
    if(changed('hero',p)){
      var e=ease(clamp(p/0.88));
      sty(sea,'transform','translateY('+lerp(H-150,-175,e).toFixed(1)+'px)');
      sty(boatWrap,'transform','translateY('+lerp(0,-H*0.04,e).toFixed(1)+'px) rotate('+lerp(0,-7,e).toFixed(2)+'deg)');
      sty(heroText,'opacity',(1-clamp(p/0.42)).toFixed(3));
      sty(heroText,'transform','translateY('+(-p*150).toFixed(1)+'px)');
      if(p>0.05&&!hopped){hopped=true;hop(gullSrc);} if(p<0.01) hopped=false;
    }

    /* RUTA */
    var pr=prog('ruta');
    if(changed('ruta',pr)){
      var f=simple?0.98:lerp(0.02,0.98,clamp(pr/0.92));
      fill.style.strokeDashoffset=len*(1-f);
      var a=pt(f), b1=pt(f-0.012), b2=pt(f+0.012), ang=clamp(Math.atan2(b2.y-b1.y,b2.x-b1.x)*180/Math.PI,-16,16);
      sty(miniWrap,'left',(a.x/12).toFixed(2)+'%'); sty(miniWrap,'top',((a.y-90)/2.5).toFixed(2)+'%');
      sty(miniWrap,'transform','translate(-50%,-90%) rotate('+ang.toFixed(1)+'deg)');
      portsF.forEach(function(pf,i){
        var t=simple?1:clamp((f-(pf-0.07))/0.07), lit=simple||f>=pf-0.01;
        sty(cards[i],'opacity',t.toFixed(3)); sv(cards[i],'ty',((1-t)*20).toFixed(1)+'px');
        if(nodes[i].__l!==lit){nodes[i].__l=lit; nodes[i].classList.toggle('lit',lit);}
      });
      var cr=$('#compassRing'); cr.style.transformBox='view-box'; cr.style.transformOrigin='80px 80px'; cr.style.transform='rotate('+(pr*540).toFixed(1)+'deg)';
    }

    /* PASES */
    var pp=prog('pases');
    if(changed('pases',pp)&&!simple){
      tks.forEach(function(el,i){
        var t=ease(clamp((pp-ranges[i][0])/(ranges[i][1]-ranges[i][0]))), rot=+el.dataset.rot, fy=+el.dataset.y;
        sty(el,'transform','translate('+((1-t)*window.innerWidth*0.85).toFixed(0)+'px,'+lerp(-140,fy,t).toFixed(1)+'px) rotate('+lerp(18,rot,t).toFixed(2)+'deg)');
        sty(el,'opacity',clamp(t*4).toFixed(2));
        var u=clamp((pp-0.72-i*0.05)/0.2);
        sty(stubs[i],'transform','translate('+(u*16).toFixed(1)+'px,'+(u*46).toFixed(1)+'px) rotate('+(u*8).toFixed(1)+'deg)');
        sty(stubs[i],'opacity',(1-clamp((u-0.55)/0.45)).toFixed(2));
      });
    }

    /* GAVIOTA */
    var gp=prog('gaviota');
    if(changed('gaviota',gp)){
      var k=clamp(gp/0.75)*words.length;
      words.forEach(function(w,i){sty(w,'opacity',simple?'1':(0.14+0.86*clamp(k-i)).toFixed(2))});
      if(gp>0.8&&!quoteHop&&!simple){quoteHop=true;hop(tileG);} if(gp<0.5) quoteHop=false;
    }

    /* MUELLES */
    var mp=prog('muelles');
    if(changed('muelles',mp)&&!simple){
      var t2=ease(clamp(mp/0.6));
      sty($('#dockL'),'transform','translateX('+(-(1-t2)*window.innerWidth*0.6).toFixed(0)+'px)');
      sty($('#dockR'),'transform','translateX('+((1-t2)*window.innerWidth*0.6).toFixed(0)+'px)');
      sty($('#muellesH'),'opacity',clamp(mp/0.15).toFixed(2));
    }

    /* FINAL */
    var q=simple?1:clamp((S+H-M.crear.top)/(H*0.8));
    if(changed('crear',q)){
      sty($('#xA'),'transform','rotate(45deg) scaleY('+clamp(q/0.5).toFixed(3)+')');
      sty($('#xB'),'transform','rotate(-45deg) scaleY('+clamp((q-0.25)/0.5).toFixed(3)+')');
      sty($('#xRing'),'opacity',clamp(q*2).toFixed(2));
      sty($('#xTitle'),'opacity',simple?'1':clamp((q-0.3)/0.35).toFixed(2));
      sty($('#xCta'),'opacity',simple?'1':clamp((q-0.6)/0.25).toFixed(2));
    }
    force=false;
  }
  function onScroll(){ if(!ticking){ticking=true;requestAnimationFrame(update);} }
  function relayout(){ setMode(); measure(); onScroll(); if(!lookPend){lookPend=true;requestAnimationFrame(lookAt);} }
  window.addEventListener('scroll',onScroll,{passive:true});
  window.addEventListener('resize',relayout);
  window.addEventListener('load',relayout);
  if(rmq.addEventListener) rmq.addEventListener('change',relayout);
  if(document.fonts&&document.fonts.ready) document.fonts.ready.then(relayout);
  relayout();
})();
