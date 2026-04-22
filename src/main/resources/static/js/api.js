// ─────────────────────────────────────────────────────────────────────────────
// LAYER 1 — FRONTEND HELPER (api.js)
// This file is the bridge between the browser UI and the Spring Boot backend.
// It is loaded by index.html and used throughout app.js.
//
// What it does:
//   - Holds shared application state (current user, role, active semester)
//     in the SMS object so all other JavaScript can access it.
//   - Provides a central apiFetch() function that all API calls go through.
//     This function automatically attaches the correct headers, handles
//     HTTP errors, and parses JSON responses.
//   - Contains utility/helper functions used across the whole frontend
//     (e.g., formatting dates, building HTML elements, showing notifications).
//
// How it connects to Layer 2:
//   Every time app.js needs data (e.g., load the student list, save a grade,
//   enroll a student), it calls a function in api.js which sends an HTTP
//   request to a Spring Boot REST controller endpoint.
//   The controller processes it and returns a JSON response, which api.js
//   passes back to app.js to display on screen.
//
// LAYER 1 → LAYER 2: Sends HTTP requests (GET/POST/PUT/PATCH/DELETE) to
//   REST API endpoints like /api/students, /api/grades, /api/enrollment, etc.
// LAYER 2 → LAYER 1: Receives JSON responses and returns them to the caller.
// ─────────────────────────────────────────────────────────────────────────────

/* ============================================================
   SMS — api.js
   API helper, utility functions, shared state
   ============================================================ */

// ── Shared State ─────────────────────────────────────────────
const SMS = {
  currentUser: null,   // set after login
  role: null,          // 'Registrar' or 'Student'
  activeSemester: null // set on init
};

// ── API Helper ───────────────────────────────────────────────
// LAYER 1 → LAYER 2: This is the single function all of app.js uses to talk to the backend.
//   Every GET/POST/PUT/PATCH/DELETE request goes through here.
//   It sends an HTTP request to a Spring Boot controller endpoint (Layer 2)
//   and returns the parsed JSON response back to whoever called it.
// LAYER 2 → LAYER 1: If the response is 401 (session expired), it redirects to login.
//   If the response is an error, it throws so the caller can handle it.
//   If the response is OK, it returns the parsed JSON data.
async function api(url, method = 'GET', body = null) {
  const headers = { 'Content-Type': 'application/json' };

  const opts = { method, headers, credentials: 'same-origin' };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(url, opts);

  // Redirect to login if session expired
  if (res.status === 401 || res.redirected && res.url.includes('/login')) {
    window.location.href = '/login.html';
    return null;
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || 'Request failed');
  }

  // Some DELETE endpoints return 200 with no body
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

// ── XSS Escape Helper ────────────────────────────────────────
// SECURITY (A03): Escape user-controlled data before inserting into innerHTML.
// Always use escHtml() on any string from the API before placing it in a template literal.
function escHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Toast ─────────────────────────────────────────────────────
function toast(msg, type = 'success') {
  let t = document.getElementById('sms-toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'sms-toast';
    t.className = 'toast';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.className = `toast show ${type}`;
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove('show'), 3500);
}

// ── Badge Helper ──────────────────────────────────────────────
function badge(text, forceType) {
  const map = {
    Active:'success', Enrolled:'success', Passed:'success', Active:'success',
    Applied:'info', Interviewed:'info', College:'info',
    Inactive:'gray', LOA:'gray', Propaedeutic:'gray', NotYetGraded:'gray', Pending:'gray',
    Failed:'danger', Rejected:'danger', Dismissed:'danger',
    Incomplete:'warn', AspiringConventionAttended:'warn', Withdrawn:'warn', Alumni:'warn',
  };
  const labels = {
    AspiringConventionAttended: 'Convention Attended',
    NotYetGraded: 'Not Yet Graded',
  };
  const cls = forceType || map[text] || 'gray';
  // SECURITY (A03): Escape badge text — badge values can come from API/user data
  return `<span class="badge badge-${cls}">${escHtml(labels[text] || text)}</span>`;
}

// ── Grade Color ───────────────────────────────────────────────
function gradeClass(v) {
  if (v === null || v === undefined) return '';
  return parseFloat(v) <= 3.0 ? 'grade-pass' : 'grade-fail';
}

// ── Filter Table ──────────────────────────────────────────────
function filterTable(tbodyId, query) {
  const rows = document.querySelectorAll(`#${tbodyId} tr`);
  const q = query.toLowerCase();
  rows.forEach(r => {
    r.style.display = r.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

// ── Sort Table ────────────────────────────────────────────────
function sortTable(tbodyId, colIndex, th) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  const isAsc = th.classList.contains('sort-asc');
  const dir = isAsc ? 'desc' : 'asc';
  th.closest('table').querySelectorAll('th.sortable').forEach(h => {
    h.classList.remove('sort-asc', 'sort-desc');
  });
  th.classList.add(dir === 'asc' ? 'sort-asc' : 'sort-desc');
  const rows = Array.from(tbody.querySelectorAll('tr'));
  rows.sort((a, b) => {
    const aText = (a.cells[colIndex]?.textContent || '').trim().toLowerCase();
    const bText = (b.cells[colIndex]?.textContent || '').trim().toLowerCase();
    const aNum = parseFloat(aText);
    const bNum = parseFloat(bText);
    if (!isNaN(aNum) && !isNaN(bNum)) return dir === 'asc' ? aNum - bNum : bNum - aNum;
    return dir === 'asc' ? aText.localeCompare(bText) : bText.localeCompare(aText);
  });
  rows.forEach(r => tbody.appendChild(r));
}

// ── Modal Helpers ──────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id).classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}
// Close on backdrop click
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('open');
  }
});

// ── Tab Helper ────────────────────────────────────────────────
function switchTab(clickedTab, showId, allIds) {
  allIds.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  });
  const target = document.getElementById(showId);
  if (target) target.style.display = '';

  const parent = clickedTab.closest('.tabs');
  if (parent) {
    parent.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  }
  clickedTab.classList.add('active');
}

// ── Format Date ───────────────────────────────────────────────
function fmtDate(str) {
  if (!str) return '—';
  const d = new Date(str);
  return d.toLocaleDateString('en-PH', { year: 'numeric', month: 'short', day: 'numeric' });
}

// ── Populate Select ───────────────────────────────────────────
function populateSelect(selectId, items, valueKey, labelFn, emptyLabel) {
  const sel = document.getElementById(selectId);
  if (!sel) return;
  sel.innerHTML = emptyLabel ? `<option value="">${emptyLabel}</option>` : '';
  items.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item[valueKey];
    opt.textContent = labelFn(item);
    sel.appendChild(opt);
  });
}
