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
    `<details class="feature-card" id="feature-${escapeHTML(f.id)}" data-tier="${f.tier}">
       <summary>
         <div class="title-stack">
           <h3>
             <span class="tier-badge ${f.tier}">${f.tier}</span>${escapeHTML(f.name)}<span class="platform-chip">${escapeHTML(platformLabel(f.platform))}</span>
           </h3>
           <p class="where-line">${escapeHTML(f.where)}</p>
         </div>
         <a class="copy-link" href="#feature-${escapeHTML(f.id)}" aria-label="Copy link to ${escapeHTML(f.name)}" onclick="event.preventDefault(); copyFeatureLink(this, '${escapeHTML(f.id)}')">🔗</a>
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

// Deep-link handling — when the URL contains #feature-<id>, find the
// matching card, make sure the active filter doesn't hide it (clear
// tier filter if needed), expand it, and scroll it into view with a
// highlight pulse. Supports share links like
//   https://vanpaultek.com/guide/#feature-voice-bio
// generated by the 🔗 button on every card.
function openFeatureFromHash() {
  const hash = window.location.hash || '';
  const m = hash.match(/^#feature-([a-zA-Z0-9_-]+)$/);
  if (!m) return;
  const id = m[1];
  const feature = DATA.features.find((f) => f.id === id);
  if (!feature) return;
  // If current tier filter would hide this feature, clear it.
  if (state.tier !== 'all' && state.tier !== feature.tier) {
    state.tier = 'all';
    document.querySelectorAll('.seg.tier-filter button').forEach((b) => {
      const isAll = b.dataset.tier === 'all';
      b.classList.toggle('active', isAll);
      b.setAttribute('aria-pressed', String(isAll));
    });
    renderFeatures();
  }
  // Clear search if it hid the card.
  if (state.query) {
    const hay = [feature.name, feature.where, feature.what, feature.honest, ...(feature.how || [])].join(' ').toLowerCase();
    if (!hay.includes(state.query.trim().toLowerCase())) {
      state.query = '';
      document.getElementById('searchBox').value = '';
      renderFeatures();
    }
  }
  // Expand + scroll + highlight.
  const card = document.getElementById('feature-' + id);
  if (!card) return;
  card.open = true;
  card.scrollIntoView({ behavior: 'smooth', block: 'start' });
  card.classList.add('link-highlight');
  setTimeout(() => card.classList.remove('link-highlight'), 1800);
}

// Called from the 🔗 button inline onclick. Copies the fully-qualified
// deep-link URL to the clipboard + shows a quick toast.
window.copyFeatureLink = function(btn, id) {
  const url = window.location.origin + window.location.pathname + '#feature-' + id;
  try {
    navigator.clipboard.writeText(url);
    const original = btn.textContent;
    btn.textContent = '✓';
    btn.classList.add('copied');
    setTimeout(() => {
      btn.textContent = original;
      btn.classList.remove('copied');
    }, 1400);
  } catch (e) {
    // Fallback for older browsers / no clipboard API
    window.location.hash = 'feature-' + id;
  }
};

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
  // Open the deep-linked feature if any; handle later hash-only navs too.
  openFeatureFromHash();
  window.addEventListener('hashchange', openFeatureFromHash);
}

boot();
