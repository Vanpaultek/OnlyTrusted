// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// OnlyTrusted User Guide — client-side rendering + filter/search.
// Loads content.json once + populates feature cards, platform-diff
// table, honest-limits list, and FAQ. Tier filter + search box run
// locally; no network traffic after the initial content.json fetch.

const state = { tier: 'all', query: '' };
let DATA = null;

const escapeHTML = (s) => String(s).replace(/[&<>"']/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const platformLabel = (p) => ({
  both: 'iOS + Android',
  android: 'Android only',
  ios: 'iOS only'
}[p] || p);

function renderSetup() {
  const root = document.getElementById('setupSteps');
  const steps = DATA.sections.find((s) => s.id === 'onboarding').steps;
  root.innerHTML = steps.map((s) =>
    `<li><h3>${escapeHTML(s.name)}</h3><p>${escapeHTML(s.detail)}</p></li>`
  ).join('');
}

function renderFeatures() {
  const grid = document.getElementById('featuresGrid');
  const emptyMsg = document.getElementById('emptyMsg');
  const q = state.query.trim().toLowerCase();
  const filtered = DATA.features.filter((f) => {
    if (state.tier !== 'all' && f.tier !== state.tier) return false;
    if (q) {
      const hay = [f.name, f.where, f.what, f.honest, ...(f.how || [])].join(' ').toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });

  grid.innerHTML = filtered.map((f) =>
    `<details class="feature-card" data-tier="${f.tier}">
       <summary>
         <div class="title-stack">
           <h3>
             <span class="tier-badge ${f.tier}">${f.tier}</span>${escapeHTML(f.name)}<span class="platform-chip">${escapeHTML(platformLabel(f.platform))}</span>
           </h3>
           <p class="where-line">${escapeHTML(f.where)}</p>
         </div>
       </summary>
       <div class="detail">
         <div class="detail-group">
           <span class="detail-label">What it does</span>
           <p class="detail-text">${escapeHTML(f.what)}</p>
         </div>
         <div class="detail-group">
           <span class="detail-label">How to use</span>
           <ul class="detail-list">${(f.how || []).map((h) => `<li>${escapeHTML(h)}</li>`).join('')}</ul>
         </div>
         <div class="detail-group">
           <span class="detail-label">Honest claim</span>
           <p class="detail-text">${escapeHTML(f.honest)}</p>
         </div>
       </div>
     </details>`
  ).join('');

  emptyMsg.hidden = filtered.length > 0;

  const countEl = document.getElementById('resultCount');
  const nextCount = String(filtered.length);
  if (countEl.textContent !== nextCount) {
    countEl.textContent = nextCount;
    countEl.classList.remove('flash');
    void countEl.offsetWidth;
    countEl.classList.add('flash');
  }
}

function renderDiffs() {
  const rows = document.getElementById('diffsRows');
  rows.innerHTML = DATA.platformDiffs.map((d) =>
    `<tr><td>${escapeHTML(d.feature)}</td><td>${escapeHTML(d.ios)}</td><td>${escapeHTML(d.android)}</td><td>${escapeHTML(d.note)}</td></tr>`
  ).join('');
}

function renderLimits() {
  const root = document.getElementById('limitsList');
  root.innerHTML = DATA.honestLimits.map((l) => `<li>${escapeHTML(l)}</li>`).join('');
}

function renderFaq() {
  const root = document.getElementById('faqList');
  root.innerHTML = DATA.faq.map((f) =>
    `<details><summary>${escapeHTML(f.q)}</summary><p>${escapeHTML(f.a)}</p></details>`
  ).join('');
}

function setupFilters() {
  document.querySelectorAll('.seg.tier-filter button').forEach((b) => {
    b.addEventListener('click', () => {
      state.tier = b.dataset.tier;
      document.querySelectorAll('.seg.tier-filter button').forEach((x) => {
        x.classList.toggle('active', x === b);
        x.setAttribute('aria-pressed', String(x === b));
      });
      renderFeatures();
    });
  });
  const searchBox = document.getElementById('searchBox');
  let searchTimer = null;
  searchBox.addEventListener('input', (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      state.query = e.target.value;
      renderFeatures();
    }, 120);
  });
}

function setupCta() {
  const cta = document.getElementById('getCta');
  if (!cta) return;
  const ua = navigator.userAgent || '';
  const platform = /iPhone|iPad|iPod/i.test(ua) ? 'ios'
                 : /Android/i.test(ua) ? 'android' : 'web';
  const PLAY_URL  = 'https://play.google.com/store/apps/details?id=com.onlytrusted.app';
  const APPLE_URL = 'https://apps.apple.com/us/app/onlytrusted';
  const label = cta.querySelector('.cta-text');
  if (platform === 'ios') {
    cta.href = APPLE_URL;
    if (label) label.textContent = 'Get on App Store';
  } else if (platform === 'android') {
    cta.href = PLAY_URL;
    if (label) label.textContent = 'Get on Google Play';
  }
}

async function boot() {
  try {
    const res = await fetch('content.json');
    DATA = await res.json();
  } catch (err) {
    document.getElementById('featuresGrid').innerHTML =
      '<p class="empty-msg">Couldn\'t load content.json. Serve this folder over a local HTTP server (<code>python3 -m http.server 8787 --directory docs/guide</code>) instead of opening the file directly.</p>';
    return;
  }
  renderSetup();
  renderDiffs();
  renderLimits();
  renderFaq();
  setupFilters();
  renderFeatures();
  setupCta();
  document.getElementById('year').textContent = new Date().getFullYear();
}

boot();
