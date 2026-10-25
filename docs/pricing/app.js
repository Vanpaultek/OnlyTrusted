const PLANS = {"Free":{"monthly":0,"yearly":0,"detail":"Everyday safety essentials, free forever.","features":["Trusted contacts allowlist","Call blocking + SMS block list","Blocking & Unknown screen","Data Removal visibility scan (50+ sites)","Unlimited local private alias generation","Breach Watch for 1 email","Contact Safety Scan (bundled scam feed)","Android scam SMS scan","App Lock, pocket-dial & screenshot controls","Panic Lock and JSON/CSV data export","Emergency contact & distress options","Honest platform-limit notices"]},"Premium":{"monthly":0.99,"yearly":4.99,"familyMonthly":1.99,"familyYearly":9.99,"detail":"A stronger privacy routine with audits and reminders.","features":["Everything in Free","Privacy Shield alerts for OnlyTrusted-owned capture","Breach Watch for up to 5 emails","Private Aliases CSV backup","Identity Protection and yearly renewal reminders","Data Removal 90-day recheck reminders","Weekly PDF digest","Quiet hours and area-code allowlist"]},"Pro":{"monthly":2.99,"yearly":19.99,"familyMonthly":4.99,"familyYearly":29.99,"detail":"Advanced protection for communication and personal safety.","features":["Everything in Premium","Duress Mode and decoy vault","SIM-Swap Alarm and Wangiri protection","App Permission Watchdog","Tracker Finder (supported BLE detection)","Smart Link Guard: SMS, QR and shared links","Safety Check-In"]},"Elite":{"monthly":3.99,"yearly":29.99,"familyMonthly":7.99,"familyYearly":49.99,"detail":"The complete OnlyTrusted personal security suite.","features":["Everything in Pro","App Shield (platform-specific restrictions)","Border Mode preset and Panic Lockdown","Hardware security key and signed audit log","Encrypted vault backup, Shamir Split, Trusted Witness","Dead-Man Wipe and geo-fence Auto-Lock","Face-Change Lock and voice biometrics","Steganographic vault, rogue-network scan and offline threat intelligence"]}};
const APPLE = {"Premium":{"individual":{"monthly":"com.onlytrusted.app.premium.monthly","yearly":"com.onlytrusted.app.premium.yearly"},"family":{"monthly":"com.onlytrusted.app.family.monthly","yearly":"com.onlytrusted.app.family.yearly"}},"Pro":{"individual":{"monthly":"com.onlytrusted.app.pro.monthly","yearly":"com.onlytrusted.app.pro.yearly"},"family":{"monthly":"com.onlytrusted.app.pro_family.monthly","yearly":"com.onlytrusted.app.pro_family.yearly"}},"Elite":{"individual":{"monthly":"com.onlytrusted.app.elite.monthly","yearly":"com.onlytrusted.app.elite.yearly"},"family":{"monthly":"com.onlytrusted.app.elite_family.monthly","yearly":"com.onlytrusted.app.elite_family.yearly"}}};
const PLAY = {"Premium":{"individual":{"monthly":["premium","premium_monthly"],"yearly":["premium","premium_yearly"]},"family":{"monthly":["premium_family","family_monthly"],"yearly":["premium_family","family_yearly"]}},"Pro":{"individual":{"monthly":["pro","pro_monthly"],"yearly":["pro","pro_yearly"]},"family":{"monthly":["pro_family","pro_family_monthly"],"yearly":["pro_family","pro_family_yearly"]}},"Elite":{"individual":{"monthly":["elite","elite_monthly"],"yearly":["elite","elite_yearly"]},"family":{"monthly":["elite_family","elite_family_monthly"],"yearly":["elite_family","elite_family_yearly"]}}};
const state={audience:'individual',cycle:'yearly'};
const tierNames=['Free','Premium','Pro','Elite'];
const matrix=[['Call blocking & emergency SOS',0],['Local private alias generation',0],['Breach Watch',0,'1 email','5 emails'],['Data removal visibility scan',0],['Privacy Shield (OnlyTrusted-owned captures)',1],['Identity reminders & PDF digest',1],['Advanced call blocking',1],['Duress mode & decoy vault',2],['SIM-swap / Wangiri protection',2],['App Permission Watchdog',2],['Tracker Finder & Smart Link Guard',2],['Safety Check-In',2],['Selected-app shield (where supported)',3],['Panic Lockdown & Border Mode',3],['Hardware key & signed logs',3],['Encrypted backup & trusted witnesses',3],['Advanced vault & biometric controls',3]];
const fmt=(n)=>'$'+Number(n).toFixed(2);
const escapeHTML=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function notify(message){const n=document.getElementById('toast');n.textContent=message;n.classList.add('show');setTimeout(()=>n.classList.remove('show'),6000)}
function detectedPlatform(){const ua=navigator.userAgent||'';return /iPhone|iPad|iPod/i.test(ua)?'ios':/Android/i.test(ua)?'android':'web'}
function purchase(tier){if(tier==='Free'){notify('Free features are available without a purchase.');return;}const productId=APPLE[tier][state.audience][state.cycle];const play=PLAY[tier][state.audience][state.cycle];const payload={tier,audience:state.audience,period:state.cycle,appleProductId:productId,googlePlayProductId:play[0],googlePlayBasePlanId:play[1]};const platform=detectedPlatform();
// iOS WKWebView: inject a message handler named onlyTrustedPurchase
if(window.webkit?.messageHandlers?.onlyTrustedPurchase){window.webkit.messageHandlers.onlyTrustedPurchase.postMessage(payload);return;}
// Android WebView: expose @JavascriptInterface purchase(json)
if(window.OnlyTrustedAndroid?.purchase){window.OnlyTrustedAndroid.purchase(JSON.stringify(payload));return;}
// Optional hosting site integration, MUST validate server-side and use legal region-specific flow.
if(typeof window.onlyTrustedPurchase==='function'){window.onlyTrustedPurchase(payload);return;}
notify('Open this page inside the OnlyTrusted app to purchase securely with '+(platform==='ios'?'Apple In-App Purchase':platform==='android'?'Google Play Billing':'the mobile app')+'. Checkout is not enabled in this website preview.');}
function updateStartingAt(){
  // The cheapest paid tier is Premium — use that as the "starting from"
  // reference. Reads the current audience + cycle from state and formats
  // "$X.XX/yr" or "$X.XX/mo" so the sticky bar always shows what the
  // user would pay to upgrade from Free given their current selections.
  // Triggers a cyan flash animation when the value actually changes so
  // the user visually catches the toggle feedback.
  const p = PLANS.Premium;
  const amount = state.audience === 'family'
    ? p[state.cycle === 'yearly' ? 'familyYearly' : 'familyMonthly']
    : p[state.cycle];
  const unit = state.cycle === 'yearly' ? '/yr' : '/mo';
  const el = document.getElementById('startingPrice');
  if (!el) return;
  const nextText = fmt(amount) + unit;
  if (el.textContent === nextText) return;  // initial render — no flash
  el.textContent = nextText;
  // Replace the node to force the animation to restart cleanly on rapid
  // repeat toggles (toggling .flash off then on in the same frame doesn't
  // retrigger a running CSS animation).
  el.classList.remove('flash');
  void el.offsetWidth;  // reflow trick so the browser notices the removal
  el.classList.add('flash');
}
function render(){updateStartingAt();for(const [id,choice,field] of [['individual','individual','audience'],['family','family','audience'],['yearly','yearly','cycle'],['monthly','monthly','cycle']]){const e=document.getElementById(id);e.classList.toggle('active',state[field]===choice);e.setAttribute('aria-pressed',String(state[field]===choice));}
const cards=document.getElementById('cards');cards.replaceChildren();tierNames.forEach((tier,index)=>{const t=PLANS[tier];const amount=tier==='Free'?0:state.audience==='family'?t[state.cycle==='yearly'?'familyYearly':'familyMonthly']:t[state.cycle];const payPeriod=state.cycle==='yearly'?'/year':'/month';const monthly=state.audience==='family'?t.familyMonthly:t.monthly;const savings=tier==='Free'||state.cycle==='monthly'?null:Math.round((1-amount/(monthly*12))*100);const seatText=state.audience==='family'&&tier!=='Free'?`Up to 5 members · ${fmt(amount/(state.cycle==='yearly'?60:5))}/member/month`:'Individual plan';const el=document.createElement('article');el.className='plan'+(tier==='Pro'?' featured':'');const description=t.detail;el.innerHTML=`${tier==='Pro'?'<span class="ribbon">Popular plan</span>':''}<span class="plan-label">${escapeHTML(tier)}</span><h3>${tier==='Free'?'Always Free':escapeHTML(tier)}</h3><p class="desc">${escapeHTML(description)}</p><div class="price"><span class="amount">${fmt(amount)}</span><span class="period">${tier==='Free'?'forever':payPeriod}</span></div><div class="subprice">${tier==='Free'?'No credit card required':escapeHTML(seatText)} ${savings?`<div class="save">Save ${savings}% vs monthly</div>`:''}</div><button class="action" data-tier="${tier}">${tier==='Free'?'Continue with Free':'Choose '+tier+' →'}</button><span class="feature-title">Included protection</span><ul class="features">${t.features.map(f=>`<li><span class="check">✓</span><span>${escapeHTML(f)}</span></li>`).join('')}</ul>`;cards.append(el);});cards.querySelectorAll('[data-tier]').forEach(b=>b.addEventListener('click',()=>purchase(b.dataset.tier)));}
function renderMatrix(){const root=document.getElementById('featureRows');root.replaceChildren();matrix.forEach(([label,minIndex,freeVariant,premiumVariant])=>{const row=document.createElement('tr');let html=`<td>${escapeHTML(label)}</td>`;for(let i=0;i<4;i++){let value=i>=minIndex?'<span class="yes">✓</span>':'<span class="no">—</span>';if(label==='Breach Watch'){value=i===0?'1 email':'Up to 5'}html+=`<td>${value}</td>`}row.innerHTML=html;root.append(row)});}

/* Mobile Compare — per-tier accordion stack. Hidden on desktop by CSS,
   visible at <=620px where the horizontal matrix would require horizontal
   scrolling to see all four tier columns. Each card shows the tier's
   included features (checkmark) + excluded ones (dash). Premium opens
   by default, Pro gets the "Popular" badge mirroring the pricing card. */
function renderTierStack(){
  const root = document.getElementById('tierStack');
  if (!root) return;
  root.replaceChildren();
  const tiers = [
    {name:'Free',    idx:0, open:false, badge:null},
    {name:'Premium', idx:1, open:true,  badge:null},
    {name:'Pro',     idx:2, open:false, badge:'Popular'},
    {name:'Elite',   idx:3, open:false, badge:null}
  ];
  tiers.forEach(({name, idx, open, badge}) => {
    const card = document.createElement('details');
    card.className = 'tier-card' + (badge ? ' featured' : '');
    if (open) card.open = true;
    const items = matrix.map(([label, minIndex]) => {
      const included = idx >= minIndex;
      let mark = included ? '✓' : '—';
      if (label === 'Breach Watch') mark = idx === 0 ? '1 email' : 'Up to 5';
      const cls = included ? '' : 'excluded';
      return `<li class="${cls}"><span class="included">${escapeHTML(mark)}</span><span>${escapeHTML(label)}</span></li>`;
    }).join('');
    card.innerHTML =
      `<summary><span><span class="tier-name">${escapeHTML(name)}</span>` +
      (badge ? `<span class="tier-badge">${escapeHTML(badge)}</span>` : '') +
      `</span></summary><ul class="tier-features">${items}</ul>`;
    root.append(card);
  });
}

function setup(){['individual','family'].forEach(x=>document.getElementById(x).addEventListener('click',()=>{state.audience=x;render()}));['yearly','monthly'].forEach(x=>document.getElementById(x).addEventListener('click',()=>{state.cycle=x;render()}));if(detectedPlatform()==='ios'){const url='https://apps.apple.com/account/subscriptions';for(const id of ['manage','footerManage'])document.getElementById(id).href=url;}
document.getElementById('year').textContent=new Date().getFullYear();render();renderMatrix();renderTierStack();}setup();