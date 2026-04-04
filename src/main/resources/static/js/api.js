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
    Applied:'info', Interviewed:'info', Confirmed:'info', College:'info',
    Inactive:'gray', LOA:'gray', Propaedeutic:'gray', NotYetGraded:'gray', Pending:'gray',
    Failed:'danger', Rejected:'danger', Dismissed:'danger',
    Incomplete:'warn', AspiringConventionAttended:'warn', Withdrawn:'warn', Alumni:'warn',
  };
  const cls = forceType || map[text] || 'gray';
  // SECURITY (A03): Escape badge text — badge values can come from API/user data
  return `<span class="badge badge-${cls}">${escHtml(text)}</span>`;
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
