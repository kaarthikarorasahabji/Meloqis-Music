export const DASHBOARD_HTML = String.raw`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="robots" content="noindex,nofollow">
  <meta name="color-scheme" content="dark">
  <title>Meloqis Insights</title>
  <style>
    :root{--bg:#09080d;--panel:#111018;--line:#282431;--ink:#f7f4ff;--muted:#aaa4b5;--accent:#a67cff;--good:#62d7a8;--bad:#ff7c91}
    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:14px/1.45 Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
    button,input,select{font:inherit}.hidden{display:none!important}
    .login{min-height:100svh;display:grid;place-items:center;padding:24px;background:radial-gradient(circle at 50% 10%,#241a35 0,transparent 36%)}
    .login-panel{width:min(420px,100%);animation:rise .5s cubic-bezier(.2,.8,.2,1)}
    .mark{width:42px;height:42px;margin-bottom:36px;border:1px solid #6f52a6;border-radius:14px;display:grid;place-items:center;color:var(--accent);font-size:20px}
    .login h1{font-size:38px;letter-spacing:-.055em;margin:0 0 8px}.login p{color:var(--muted);margin:0 0 34px}
    label{display:block;color:#d7d1df;font-size:12px;margin:18px 0 7px}.field{width:100%;border:1px solid var(--line);border-radius:12px;background:#0d0c12;color:var(--ink);padding:14px 15px;outline:none}.field:focus{border-color:var(--accent);box-shadow:0 0 0 3px #a67cff1c}.owner{display:flex;align-items:center;justify-content:space-between;border-block:1px solid var(--line);padding:14px 0;margin:26px 0 4px}.owner span{color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.12em}.owner strong{font-size:14px}
    .primary{width:100%;margin-top:24px;border:0;border-radius:12px;padding:14px;background:var(--accent);color:#120d1d;font-weight:800;cursor:pointer}.primary:hover{filter:brightness(1.08)}
    .error{min-height:20px;color:var(--bad);margin-top:12px}
    .app{min-height:100svh}.topbar{height:70px;border-bottom:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;padding:0 clamp(20px,4vw,56px);position:sticky;top:0;background:#09080de8;backdrop-filter:blur(18px);z-index:5}
    .brand{font-size:18px;font-weight:800;letter-spacing:-.035em}.brand span{color:var(--accent)}
    .top-actions{display:flex;align-items:center;gap:12px}.select,.ghost{border:1px solid var(--line);background:#0e0d13;color:var(--ink);border-radius:10px;padding:9px 12px}.ghost{cursor:pointer}
    main{padding:42px clamp(20px,4vw,56px) 70px;max-width:1500px;margin:auto}
    .heading{display:flex;justify-content:space-between;gap:24px;align-items:end;margin-bottom:36px}.heading h1{font-size:clamp(34px,5vw,64px);letter-spacing:-.06em;margin:0}.heading p{color:var(--muted);margin:8px 0 0}.freshness{color:var(--muted);font-size:12px}
    .kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));border-block:1px solid var(--line);margin-bottom:48px}
    .kpi{padding:24px 22px 24px 0}.kpi+.kpi{border-left:1px solid var(--line);padding-left:22px}.kpi strong{display:block;font-size:clamp(25px,3vw,42px);letter-spacing:-.055em}.kpi span{color:var(--muted);font-size:12px}
    .workspace{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(300px,.75fr);gap:48px}
    section{min-width:0}.section-head{display:flex;align-items:baseline;justify-content:space-between;border-bottom:1px solid var(--line);padding-bottom:13px;margin-bottom:20px}.section-head h2{font-size:17px;margin:0}.section-head span{color:var(--muted);font-size:12px}
    .chart{height:255px;display:flex;align-items:end;gap:6px;padding-top:24px}.bar-wrap{height:100%;flex:1;display:flex;align-items:end;position:relative}.bar{width:100%;min-height:2px;border-radius:5px 5px 1px 1px;background:linear-gradient(180deg,var(--accent),#64469e);transform-origin:bottom;animation:grow .55s cubic-bezier(.2,.8,.2,1)}.bar-wrap:hover:after{content:attr(data-tip);position:absolute;left:50%;bottom:calc(var(--h) + 8px);transform:translateX(-50%);white-space:nowrap;background:#1b1723;border:1px solid var(--line);padding:5px 7px;border-radius:7px;font-size:11px;z-index:3}
    .axis{display:flex;justify-content:space-between;color:var(--muted);font-size:10px;margin-top:9px}
    .funnel{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;background:var(--line);margin:42px 0}.funnel div{background:var(--bg);padding:19px}.funnel strong{display:block;font-size:24px}.funnel span{color:var(--muted);font-size:11px}
    .version-row{display:grid;grid-template-columns:80px 1fr 48px;gap:12px;align-items:center;margin:16px 0}.meter{height:7px;background:#1d1a23;border-radius:99px;overflow:hidden}.meter i{display:block;height:100%;background:var(--accent);border-radius:inherit}.version-row small{color:var(--muted);text-align:right}
    .reliability{display:grid;grid-template-columns:1fr 1fr;border-block:1px solid var(--line);margin-top:40px}.reliability div{padding:20px 0}.reliability div+div{border-left:1px solid var(--line);padding-left:20px}.reliability strong{font-size:30px;display:block}.reliability span{font-size:11px;color:var(--muted)}
    table{width:100%;border-collapse:collapse;margin-top:12px}th,td{text-align:left;border-bottom:1px solid #201d26;padding:12px 8px;font-size:12px}th{color:var(--muted);font-weight:600}td:last-child{text-align:right}.status-bad{color:var(--bad)}
    .privacy-note{margin-top:32px;padding-top:20px;border-top:1px solid var(--line);color:var(--muted);font-size:12px}
    @keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}@keyframes grow{from{transform:scaleY(0)}to{transform:scaleY(1)}}
    @media(max-width:1000px){.kpis{grid-template-columns:repeat(2,1fr)}.kpi:nth-child(odd){border-left:0;padding-left:0}.workspace{grid-template-columns:1fr}.funnel{grid-template-columns:1fr 1fr}}
    @media(max-width:600px){.topbar{height:62px}.top-actions .ghost{font-size:0}.top-actions .ghost:after{content:"Out";font-size:12px}.heading{align-items:start;flex-direction:column}.kpis{grid-template-columns:1fr 1fr}.kpi{padding:18px 10px 18px 0}.kpi+.kpi{padding-left:14px}.funnel{grid-template-columns:1fr}.chart{height:190px}}
    @media(prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}
  </style>
</head>
<body>
  <div id="login" class="login">
    <form id="login-form" class="login-panel">
      <div class="mark">M</div>
      <h1>Meloqis Insights</h1>
      <p>Private installation and reliability telemetry.</p>
      <div class="owner"><span>Administrator</span><strong>Kaarthik Dass Arora</strong></div>
      <label for="password">Password</label>
      <input id="password" class="field" type="password" autocomplete="current-password" required>
      <button class="primary" type="submit">Open dashboard</button>
      <div id="login-error" class="error" role="alert"></div>
    </form>
  </div>
  <div id="app" class="app hidden">
    <header class="topbar">
      <div class="brand">Meloqis <span>Insights</span></div>
      <div class="top-actions">
        <select id="range" class="select" aria-label="Reporting range"><option value="7">7 days</option><option value="30" selected>30 days</option><option value="90">90 days</option></select>
        <a class="ghost" href="/api/admin-artifact">Admin APK</a>
        <button id="logout" class="ghost">Sign out</button>
      </div>
    </header>
    <main>
      <div class="heading">
        <div><h1>Android health</h1><p>Anonymous, consent-aware signals from Meloqis releases.</p></div>
        <div id="freshness" class="freshness"></div>
      </div>
      <div class="kpis">
        <div class="kpi"><strong id="total-installs">0</strong><span>First opens</span></div>
        <div class="kpi"><strong id="active-7">0</strong><span>Active in 7 days</span></div>
        <div class="kpi"><strong id="active-30">0</strong><span>Active in 30 days</span></div>
        <div class="kpi"><strong id="downloads">0</strong><span>Download starts</span></div>
        <div class="kpi"><strong id="adoption">0%</strong><span>Latest version adoption</span></div>
      </div>
      <div class="workspace">
        <section>
          <div class="section-head"><h2>Daily active installations</h2><span id="chart-label">Selected range</span></div>
          <div id="chart" class="chart"></div><div id="axis" class="axis"></div>
          <div class="funnel">
            <div><strong id="f-downloads">0</strong><span>Website download starts</span></div>
            <div><strong id="f-first">0</strong><span>First opens</span></div>
            <div><strong id="f-update-downloads">0</strong><span>In-app update downloads</span></div>
            <div><strong id="f-update-success">0</strong><span>Confirmed updates</span></div>
          </div>
          <div class="section-head"><h2>Recent reliability signals</h2><span>No song names or personal data</span></div>
          <table><thead><tr><th>Time</th><th>Signal</th><th>Version</th><th>Code</th></tr></thead><tbody id="events"></tbody></table>
        </section>
        <aside>
          <div class="section-head"><h2>Version distribution</h2><span id="device-count">0 active</span></div>
          <div id="versions"></div>
          <div class="reliability">
            <div><strong id="crash-rate">0</strong><span>Crashes / 100 active installs</span></div>
            <div><strong id="playback-rate">0</strong><span>Playback failures / 100 active installs</span></div>
          </div>
          <p class="privacy-note">Installation identifiers are random on-device UUIDs and are SHA-256 hashed before storage. Meloqis does not send IMEI, phone number, advertising ID, song titles, account data, or IP addresses to this dataset. Counts include only installations with anonymous insights enabled.</p>
        </aside>
      </div>
    </main>
  </div>
  <script>
    const login = document.querySelector("#login"), app = document.querySelector("#app");
    const number = new Intl.NumberFormat();
    function text(id,value){document.querySelector(id).textContent=value}
    async function api(path,options={}){const response=await fetch(path,{...options,headers:{"content-type":"application/json",...(options.headers||{})}});if(response.status===401)throw new Error("UNAUTHORIZED");if(!response.ok)throw new Error(await response.text());return response.status===204?null:response.json()}
    function showLogin(){login.classList.remove("hidden");app.classList.add("hidden")}
    function showApp(){login.classList.add("hidden");app.classList.remove("hidden")}
    function render(data){
      showApp();const k=data.kpis;
      text("#total-installs",number.format(k.totalFirstOpens));text("#active-7",number.format(k.active7));text("#active-30",number.format(k.active30));text("#downloads",number.format(k.downloads));text("#adoption",k.latestAdoption+"%");
      text("#f-downloads",number.format(k.downloads));text("#f-first",number.format(k.totalFirstOpens));text("#f-update-downloads",number.format(k.updateDownloads));text("#f-update-success",number.format(k.updateSuccess));
      text("#crash-rate",data.reliability.crashesPer100);text("#playback-rate",data.reliability.playbackFailuresPer100);text("#freshness","Updated "+new Date(data.generatedAt).toLocaleString());text("#device-count",number.format(k.active30)+" active");
      const peak=Math.max(1,...data.activity.map(x=>x.active));const chart=document.querySelector("#chart");chart.innerHTML=data.activity.map(x=>{const h=Math.max(2,Math.round(x.active/peak*100));return '<div class="bar-wrap" style="--h:'+h+'%" data-tip="'+x.day+' · '+x.active+'"><i class="bar" style="height:'+h+'%"></i></div>'}).join("");
      const axis=document.querySelector("#axis");axis.innerHTML=data.activity.length?'<span>'+data.activity[0].day.slice(5)+'</span><span>'+data.activity.at(-1).day.slice(5)+'</span>':"";
      const total=Math.max(1,...data.versions.map(x=>x.devices));document.querySelector("#versions").innerHTML=data.versions.map(x=>'<div class="version-row"><b>'+x.version+'</b><div class="meter"><i style="width:'+Math.round(x.devices/total*100)+'%"></i></div><small>'+x.devices+'</small></div>').join("")||'<p class="privacy-note">No installation data yet.</p>';
      document.querySelector("#events").innerHTML=data.recentFailures.map(x=>'<tr><td>'+new Date(x.createdAt).toLocaleString()+'</td><td class="status-bad">'+x.name.replaceAll("_"," ")+'</td><td>'+x.appVersion+'</td><td>'+escapeHtml(x.code||"—")+'</td></tr>').join("")||'<tr><td colspan="4">No failures in this range.</td></tr>';
    }
    function escapeHtml(value){const div=document.createElement("div");div.textContent=value;return div.innerHTML}
    async function load(){try{render(await api("/api/dashboard?days="+document.querySelector("#range").value))}catch(error){if(error.message==="UNAUTHORIZED")showLogin();else alert("Dashboard could not load.")}}
    document.querySelector("#login-form").addEventListener("submit",async event=>{event.preventDefault();text("#login-error","");try{await api("/api/login",{method:"POST",body:JSON.stringify({password:document.querySelector("#password").value})});document.querySelector("#password").value="";await load()}catch(error){text("#login-error",error.message==="UNAUTHORIZED"?"Password is incorrect.":"Login is temporarily unavailable.")}});
    document.querySelector("#range").addEventListener("change",load);
    document.querySelector("#logout").addEventListener("click",async()=>{await api("/api/logout",{method:"POST"});showLogin()});
    load();
  </script>
</body>
</html>`;
