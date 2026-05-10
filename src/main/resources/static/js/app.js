/* ============================================================
   SMS — app.js
   Page navigation, data loaders, form save handlers
   ============================================================ */

// ── Sidebar Toggle ────────────────────────────────────────────
function toggleSidebar() {
  const sidebar = document.getElementById('app-sidebar');
  const btn     = document.getElementById('sidebar-toggle-btn');
  const collapsed = sidebar.classList.toggle('collapsed');
  btn.setAttribute('aria-expanded', String(!collapsed));
  btn.title = collapsed ? 'Expand sidebar' : 'Collapse sidebar';
  localStorage.setItem('sidebarCollapsed', collapsed);
}

// Restore sidebar state on page load
(function() {
  if (localStorage.getItem('sidebarCollapsed') === 'true') {
    const sidebar = document.getElementById('app-sidebar');
    const btn     = document.getElementById('sidebar-toggle-btn');
    if (sidebar) sidebar.classList.add('collapsed');
    if (btn) { btn.setAttribute('aria-expanded', 'false'); btn.title = 'Expand sidebar'; }
  }
})();

// ── Tab ID Arrays (used by switchTab helper) ──────────────────
const sdTabs   = ['sd-personal','sd-family','sd-religious','sd-grades-tab','sd-hist-tab'];
const stTabs   = ['st-tab-personal','st-tab-family','st-tab-religious','st-tab-academic'];
const currTabs = ['curr-philo','curr-theo'];
let _currCourses        = {};
let _currYear           = { 'PRG-1001': 1, 'PRG-1002': 1 };
let _currSem            = { 'PRG-1001': 1, 'PRG-1002': 1 };
let _curricula          = {};   // { 'PRG-1001': [...Curriculum], 'PRG-1002': [...] }
let _selectedCurriculum = {};   // { 'PRG-1001': 'CUR-001', 'PRG-1002': 'CUR-002' }

// ── Color Palette for schedule items ─────────────────────────
const SCHED_COLORS = ['#0d1b5e','#2e4bbd','#2d7d46','#b45309','#1d4ed8','#7c3aed','#0891b2'];

// ── Validation Helper ─────────────────────────────────────────
function validateRequired(fields) {
  for (const {id, label} of fields) {
    const el = document.getElementById(id);
    if (!el || !el.value || !el.value.toString().trim()) {
      toast(`${label} is required`, 'error');
      if (el) el.focus();
      return false;
    }
  }
  return true;
}

// ── State ─────────────────────────────────────────────────────
let _instructorCache = {};
let _roomCache = {};
let _enrStudents = [];
let _allEnrSections = [];
let _currentStudentId = null;
let _scheduleCache = {};
let _currentStudent = null;
let _currentApplicantId = null;
let _currentApplicantExams = [];
let _previousApplicantStatus = null;
let _currentExamId = null;
let _currentReportType  = null;
const _courseMap = {};

// ── Page Navigation ───────────────────────────────────────────
const pageLoaders = {
  dashboard:      loadDashboard,
  submissions:    loadSubmissions,
  applicants:     loadApplicants,
  enrollment:     loadEnrollment,
  students:       loadStudents,
  alumni:         loadAlumni,
  curriculum:     () => { loadCurricula('PRG-1001'); },
  sections:       loadSections,
  schedule:       loadSchedule,
  grades:         loadGrades,
  instructors:    loadInstructors,
  rooms:          loadRooms,
  users:          loadUsers,
  'school-years': loadSchoolYears,
  backup:         loadBackup,
  'audit-logs':   loadAuditLog,
  'my-grades':    loadMyGrades,
  'my-schedule':  loadMySchedule,
  'my-profile':   loadMyProfile,
};

function gotoPage(pageId, navEl) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const pg = document.getElementById('page-' + pageId);
  if (pg) pg.classList.add('active');
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  if (navEl) navEl.classList.add('active');
  pageLoaders[pageId]?.();
}

// ── User Menu Dropdown ────────────────────────────────────────
function toggleUserMenu() {
  document.getElementById('user-dropdown').classList.toggle('open');
}
function closeUserMenu() {
  document.getElementById('user-dropdown').classList.remove('open');
}
document.addEventListener('click', function(e) {
  if (!e.target.closest('.user-menu')) closeUserMenu();
});

// ── Init ──────────────────────────────────────────────────────
async function init() {
  // Fetch current user from API
  try {
    const me = await api('/api/me');
    if (!me) { window.location.href = '/login.html'; return; }
    SMS.currentUser = me;
    SMS.role = me.role;

    // Header
    document.getElementById('hdr-name').textContent = me.username;
    document.getElementById('hdr-role').textContent = me.role;
    document.getElementById('hdr-avatar').textContent =
      me.username.substring(0,2).toUpperCase();

    // Load profile picture for header avatar (if one exists)
    try {
      const photoResp = await fetch('/api/me/photo', { credentials: 'same-origin' });
      if (photoResp.ok) {
        const blob = await photoResp.blob();
        const url  = URL.createObjectURL(blob);
        applyAvatarPhoto('hdr-avatar', url);
      }
    } catch (_) {}

    // Show correct nav
    if (me.role === 'Admin') {
      document.getElementById('nav-admin').style.display = '';
      document.getElementById('nav-registrar').style.display = '';
      document.body.classList.add('admin-view');
      loadDashboard();
      document.querySelector('#nav-admin .nav-item').classList.add('active');
    } else if (me.role === 'Registrar') {
      document.getElementById('nav-registrar').style.display = '';
      loadDashboard();
      document.querySelector('#nav-registrar .nav-item').classList.add('active');
    } else {
      document.getElementById('nav-student').style.display = '';
      loadMyGrades();
      document.querySelector('#nav-student .nav-item').classList.add('active');
      document.getElementById('page-dashboard').classList.remove('active');
      document.getElementById('page-my-grades').classList.add('active');
    }
  } catch (e) {
    window.location.href = '/login.html';
    return;
  }

  // Active semester
  try {
    const sem = await api('/api/school-years/semesters/active');
    if (sem) {
      SMS.activeSemester = sem;
      document.getElementById('hdr-sy-text').textContent = sem.semesterLabel;
      document.getElementById('dash-sem') && (document.getElementById('dash-sem').textContent = sem.semesterLabel);
      document.getElementById('enroll-sem-label') && (document.getElementById('enroll-sem-label').textContent = sem.semesterLabel);
      document.getElementById('my-sched-sub') && (document.getElementById('my-sched-sub').textContent = sem.semesterLabel);
      // Show enrollment open/closed notice on student pages
      if (SMS.role === 'Student') renderStudentEnrollmentNotice(sem);
    }
  } catch (_) {}

  // Date
  const today = new Date();
  const d = document.getElementById('dash-date');
  if (d) d.textContent = today.toLocaleDateString('en-PH',{weekday:'long',year:'numeric',month:'long',day:'numeric'});
  const g = document.getElementById('dash-greeting');
  if (g) g.textContent = `Good day, ${SMS.currentUser?.username || ''}!`;
}

// ── REGISTRAR PAGE LOADERS ────────────────────────────────────

async function loadDashboard() {
  try {
    const d = await api('/api/dashboard/stats');
    document.getElementById('stat-students').textContent   = d.activeStudents  ?? '—';
    document.getElementById('stat-applicants').textContent = d.totalApplicants ?? '—';
    document.getElementById('stat-courses').textContent    = d.activeCourses   ?? '—';
    document.getElementById('stat-alumni').textContent     = d.totalAlumni     ?? '—';
    document.getElementById('dash-sem') && (document.getElementById('dash-sem').textContent = d.activeSemester || '—');

    const tbody = document.getElementById('dash-enrollments');
    tbody.innerHTML = (d.recentEnrollments || []).map(e =>
      `<tr><td>${escHtml(e.studentId)}</td><td>${escHtml(e.studentName)}</td><td>${escHtml(e.program)}</td><td>${badge('Enrolled')}</td></tr>`
    ).join('') || '<tr><td colspan="4" style="text-align:center;color:var(--gray-400)">No enrollments yet</td></tr>';

    const prog = document.getElementById('dash-programs');
    prog.innerHTML = (d.programs || []).map(p =>
      `<div style="margin-bottom:18px">
        <div style="display:flex;justify-content:space-between;font-size:.83rem;margin-bottom:6px">
          <span style="font-weight:500">${p.programName}</span>
          <span style="color:var(--gray-400)">${p.studentCount} students</span>
        </div>
        <div class="progress-wrap"><div class="progress-bar" style="width:${Math.min(100,p.studentCount*10)}%"></div></div>
      </div>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadApplicants() {
  try {
    const data = await api('/api/applicants');

    // Pipeline counts (always from full data, ignoring filter)
    const count = s => data.filter(a => (a.applicationStatus || 'Applied') === s).length;
    document.getElementById('pc-applied').textContent    = count('Applied');
    document.getElementById('pc-interviewed').textContent= count('Interviewed');
    document.getElementById('pc-convention').textContent = count('AspiringConventionAttended');
    document.getElementById('pc-admitted').textContent   = count('Admitted');
    document.getElementById('pc-enrolled').textContent   = count('Enrolled');

    const statusFilter = document.getElementById('filter-applicant-status')?.value ?? 'active';
    const ACTIVE_STATUSES = ['Applied','Interviewed','AspiringConventionAttended','Admitted'];
    const filtered = statusFilter === 'active'
      ? data.filter(a => ACTIVE_STATUSES.includes(a.applicationStatus || 'Applied'))
      : statusFilter
        ? data.filter(a => (a.applicationStatus || 'Applied') === statusFilter)
        : data;
    const tbody = document.getElementById('tbl-applicants');
    clearSortCache('tbl-applicants');
    if (!filtered.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No applicants found.</p></div></td></tr>'; return; }
    tbody.innerHTML = filtered.map(a =>
      `<tr style="cursor:pointer" onclick="viewApplicantDetail('${escHtml(a.applicantId)}')">
        <td>${escHtml(a.applicantId)}</td>
        <td>${escHtml(a.firstName)} ${escHtml(a.lastName)}</td>
        <td>${escHtml(a.seminaryLevel || '—')}</td>
        <td>${escHtml(a.appliedProgram?.programCode || '—')}</td>
        <td>${badge(a.applicationStatus || 'Applied')}</td>
      </tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadEnrollment() {
  const filterEl = document.getElementById('enroll-filter-sem');
  // Populate semester filter on first load, then auto-select active semester
  if (filterEl && filterEl.options.length === 0) {
    try {
      const sems = await api('/api/school-years/semesters');
      sems.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.semesterId;
        opt.textContent = s.semesterLabel;
        filterEl.appendChild(opt);
      });
    } catch (_) {}
    if (SMS.activeSemester) filterEl.value = SMS.activeSemester.semesterId;
  }
  // Render enrollment open/close status badge and toggle buttons
  renderEnrollmentStatusBadge(SMS.activeSemester);
  try {
    const selected = filterEl?.value;
    const url = selected ? `/api/enrollment?semester=${selected}` : '/api/enrollment';
    const data = await api(url);
    const tbody = document.getElementById('tbl-enrollment');
    clearSortCache('tbl-enrollment');
    const active = data.filter(e => e.student?.currentStatus !== 'Alumni');
    if (!active.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No enrollments found.</p></div></td></tr>'; return; }
    tbody.innerHTML = active.map(e =>
      `<tr style="cursor:pointer" onclick="viewSubjects('${escHtml(e.enrollmentId)}','${escHtml(e.student?.firstName)} ${escHtml(e.student?.lastName)}','${escHtml(e.program?.programId || '')}')">
        <td>${escHtml(e.enrollmentId)}</td>
        <td>${escHtml(e.student?.firstName)} ${escHtml(e.student?.lastName)}</td>
        <td>${escHtml(e.program?.programCode)}</td>
        <td>${escHtml(e.yearLevel)}</td>
        <td>${fmtDate(e.enrollmentDate)}</td>
        <td>${badge(e.enrollmentStatus)}</td>
      </tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

function renderEnrollmentStatusBadge(sem) {
  const badge    = document.getElementById('enroll-status-badge');
  const text     = document.getElementById('enroll-status-text');
  const btnClose = document.getElementById('btn-close-enrollment');
  const btnOpen  = document.getElementById('btn-open-enrollment');
  const btnEnroll = document.getElementById('btn-enroll-student');
  if (!badge) return;
  const isOpen = !sem || sem.enrollmentOpen !== false;
  badge.style.display = 'flex';
  badge.className = 'enroll-status-badge ' + (isOpen ? 'enroll-status-open' : 'enroll-status-closed');
  text.textContent  = isOpen ? 'Enrollment Open' : 'Enrollment Closed';
  if (btnClose)  btnClose.style.display  = isOpen  ? '' : 'none';
  if (btnOpen)   btnOpen.style.display   = !isOpen ? '' : 'none';
  if (btnEnroll) btnEnroll.style.display = isOpen  ? '' : 'none';
}

let _enrollmentToggleTarget = null;

function toggleEnrollmentStatus(open) {
  _enrollmentToggleTarget = open;
  const label = SMS.activeSemester?.semesterLabel || 'this semester';
  const title  = document.getElementById('enr-toggle-title');
  const sub    = document.getElementById('enr-toggle-sub');
  const btn    = document.getElementById('btn-enr-toggle-confirm');
  if (open) {
    title.textContent   = 'Reopen Enrollment';
    sub.textContent     = `This will allow new students to be enrolled in ${label}. You can close it again at any time.`;
    btn.textContent     = 'Reopen Enrollment';
    btn.className       = 'btn btn-primary';
  } else {
    title.textContent   = 'Close Enrollment';
    sub.textContent     = `This will prevent new students from being enrolled in ${label}. You can reopen it later for late enrollees.`;
    btn.textContent     = 'Close Enrollment';
    btn.className       = 'btn btn-danger';
  }
  openModal('modal-enrollment-toggle');
}

async function doToggleEnrollmentStatus() {
  const open  = _enrollmentToggleTarget;
  const label = SMS.activeSemester?.semesterLabel || 'this semester';
  closeModal('modal-enrollment-toggle');
  try {
    const endpoint = open ? '/api/enrollment/open-enrollment' : '/api/enrollment/close-enrollment';
    await api(endpoint, 'PUT');
    SMS.activeSemester = { ...SMS.activeSemester, enrollmentOpen: open };
    renderEnrollmentStatusBadge(SMS.activeSemester);
    toast(`Enrollment ${open ? 'reopened' : 'closed'} for ${label}.`);
  } catch (e) {
    toast(e.message || 'Failed to update enrollment status.', 'error');
  }
}

function renderStudentEnrollmentNotice(sem) {
  const ids = ['student-enroll-notice', 'student-enroll-notice-sched'];
  const isOpen = sem && sem.enrollmentOpen !== false;
  const html = isOpen
    ? `<span class="enroll-notice-dot enroll-notice-open"></span>Enrollment for <strong>${escHtml(sem.semesterLabel)}</strong> is currently <strong>open</strong>.`
    : `<span class="enroll-notice-dot enroll-notice-closed"></span>Enrollment for <strong>${escHtml(sem.semesterLabel)}</strong> is currently <strong>closed</strong>. Contact the registrar for late enrollment.`;
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerHTML = html;
    el.className = 'student-enroll-notice ' + (isOpen ? 'notice-open' : 'notice-closed');
    el.style.display = 'flex';
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 1 — FRONTEND (app.js)
// LAYER 1 → LAYER 2: This function sends an HTTP GET request to the Controller
//   at /api/students. The Controller (Layer 2) receives it and decides what to do.
// LAYER 2 → LAYER 1: The Controller sends back a JSON list of students.
//   renderStudentTable() below receives that JSON and displays it on screen.
// ─────────────────────────────────────────────────────────────────────────────
async function loadStudents() {
  try {
    const program = document.getElementById('filter-program')?.value;
    const status  = document.getElementById('filter-status')?.value;
    let url = '/api/students?';
    if (program) url += `program=${program}&`;
    if (status)  url += `status=${status}&`;
    // LAYER 1 → LAYER 2: Sends GET /api/students to StudentController.getAll()
    const data = await api(url);
    // LAYER 2 → LAYER 1: 'data' is the JSON the Controller sent back — now render it
    renderStudentTable(data);
  } catch (e) { console.error(e); }
}

async function searchStudents(q) {
  if (q.length < 2) { loadStudents(); return; }
  try {
    // LAYER 1 → LAYER 2: Sends GET /api/students?q=... to StudentController.getAll()
    const data = await api(`/api/students?q=${encodeURIComponent(q)}`);
    // LAYER 2 → LAYER 1: 'data' is the JSON list of matching students
    renderStudentTable(data);
  } catch (e) { console.error(e); }
}

// LAYER 2 → LAYER 1: This function receives the final JSON from the Controller
//   and turns it into HTML rows on screen. Each field (s.studentId, s.firstName, etc.)
//   came from the Student entity (Layer 5) all the way up through the layers.
function renderStudentTable(data) {
  const tbody = document.getElementById('tbl-students');
  clearSortCache('tbl-students');
  if (!data.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No students found.</p></div></td></tr>'; return; }
  tbody.innerHTML = data.map(s =>
    `<tr style="cursor:pointer" onclick="viewStudent('${escHtml(s.studentId)}')">
      <td>${escHtml(s.studentId)}</td>
      <td>${escHtml(s.firstName)} ${escHtml(s.lastName)}</td>
      <td>${escHtml(s.seminaryLevel)}</td>
      <td>${escHtml(s.program?.programCode || '—')}</td>
      <td>${escHtml(s.currentYearLevel)}</td>
      <td>${badge(s.currentStatus)}</td>
    </tr>`
  ).join('');
}

async function viewStudent(id) {
  try {
    const s = await api(`/api/students/${id}`);
    _currentStudentId = id;
    _currentStudent = s;
    document.getElementById('sd-title').textContent = `${s.firstName} ${s.lastName}`;
    document.getElementById('sd-sub').textContent   = `${s.studentId} · ${s.program?.programName || '—'}`;
    document.getElementById('sd-avatar').textContent = (s.firstName[0] + s.lastName[0]).toUpperCase();
    document.getElementById('sd-name').textContent   = `${s.firstName} ${s.middleName || ''} ${s.lastName}`.trim();
    document.getElementById('sd-id').textContent     = s.studentId;
    document.getElementById('sd-badges').innerHTML   = badge(s.currentStatus) + ' ' + badge(s.seminaryLevel, 'info');
    document.getElementById('sd-info').innerHTML = `
      <div class="info-item"><span class="i-label">Program</span><span class="i-val">${escHtml(s.program?.programName || '—')}</span></div>
      <div class="info-item"><span class="i-label">Year Level</span><span class="i-val">${escHtml(s.currentYearLevel)}</span></div>
      <div class="info-item"><span class="i-label">Email</span><span class="i-val" style="font-size:.75rem">${escHtml(s.email)}</span></div>
      <div class="info-item"><span class="i-label">Contact</span><span class="i-val">${escHtml(s.contactNumber || '—')}</span></div>`;
    document.getElementById('sd-personal-fields').innerHTML = readonlyField('First Name',s.firstName) + readonlyField('Last Name',s.lastName) + readonlyField('Middle Name',s.middleName) + readonlyField('Date of Birth',fmtDate(s.dateOfBirth)) + readonlyField('Nationality',s.nationality || '—') + readonlyField('Email',s.email) + readonlyField('Contact',s.contactNumber || '—') + readonlyField('Blood Type',s.bloodType || '—') + readonlyField('Medical Conditions',s.medicalConditions || 'None');
    document.getElementById('sd-family-fields').innerHTML   = readonlyField("Father's Name",s.fatherName || '—') + readonlyField("Father's Occupation",s.fatherOccupation || '—') + readonlyField("Mother's Name",s.motherName || '—') + readonlyField("Mother's Occupation",s.motherOccupation || '—') + readonlyField("Guardian",s.guardianName || '—') + readonlyField("Guardian Contact",s.guardianContact || '—');
    document.getElementById('sd-religious-fields').innerHTML = readonlyField("Religion",s.religion || '—') + readonlyField("Diocese",s.diocese || '—') + readonlyField("Parish Priest",s.parishPriest || '—') + readonlyField("Baptism Date",fmtDate(s.baptismDate)) + readonlyField("Baptism Church",s.baptismChurch || '—') + readonlyField("Confirmation Date",fmtDate(s.confirmationDate));

    const grades = await api(`/api/grades/student/${id}`);
    document.getElementById('sd-grades-body').innerHTML = grades.map(g =>
      `<tr><td>${escHtml(g.course?.courseCode)}</td><td>${escHtml(g.course?.courseName)}</td><td>${escHtml(g.course?.units)}</td>
      <td class="${gradeClass(g.midtermGrade)}">${g.midtermGrade || '—'}</td>
      <td class="${gradeClass(g.finalGrade)}">${g.finalGrade || '—'}</td>
      <td class="${gradeClass(g.finalRating)}">${g.finalRating || '—'}</td>
      <td>${badge(g.gradeStatus)}</td></tr>`
    ).join('') || '<tr><td colspan="7" style="text-align:center;color:var(--gray-400)">No grades recorded</td></tr>';

    const hist = await api(`/api/enrollment/student/${id}`);
    document.getElementById('sd-hist-body').innerHTML = hist.map(e =>
      `<tr><td>${escHtml(e.semester?.semesterLabel)}</td><td>${escHtml(e.program?.programCode)}</td><td>${escHtml(e.yearLevel)}</td><td>${badge(e.enrollmentStatus)}</td></tr>`
    ).join('') || '<tr><td colspan="4" style="text-align:center;color:var(--gray-400)">No history</td></tr>';

    const isAlumni = s.currentStatus === 'Alumni';

    document.getElementById('btn-edit-student').style.display = isAlumni ? 'none' : '';
    document.getElementById('btn-graduate').style.display    = isAlumni ? 'none' : '';

    const uploadBtn = document.querySelector('#page-student-detail .registrar-only[onclick*="modal-upload-doc"]');
    if (uploadBtn) uploadBtn.style.display = isAlumni ? 'none' : '';

    let notice = document.getElementById('sd-alumni-notice');
    if (!notice) {
      notice = document.createElement('div');
      notice.id = 'sd-alumni-notice';
      notice.className = 'alumni-notice';
      notice.textContent = 'This student has graduated. The record is view-only.';
      document.getElementById('sd-badges').insertAdjacentElement('afterend', notice);
    }
    notice.style.display = isAlumni ? '' : 'none';

    await loadStudentDocuments(id);

    gotoPage('student-detail', null);
  } catch (e) { toast('Failed to load student record', 'error'); }
}

async function loadStudentDocuments(studentId) {
  const container = document.getElementById('sd-docs');
  if (!container) return;
  const fileIcon  = `<svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style="flex-shrink:0;margin-top:1px"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm-1 7V3.5L18.5 9H13zm-2 8H7v-2h4v2zm4-4H7v-2h8v2z"/></svg>`;
  const printIcon = `<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z"/></svg>`;
  try {
    const docs = await api(`/api/documents/student/${studentId}`);
    if (!docs.length) {
      container.innerHTML = `<p style="font-size:.75rem;color:var(--gray-400);font-style:italic;margin:0">No documents on file.</p>`;
        return;
    }
    container.innerHTML = docs.map(d => `
      <div style="display:flex;align-items:center;gap:8px;padding:6px 8px;border-radius:6px;background:var(--gray-50);border:1px solid var(--gray-100)">
        <span style="color:var(--gray-400)">${fileIcon}</span>
        <a href="/api/documents/${d.index}/file" target="_blank"
           style="flex:1;font-size:.76rem;font-weight:500;color:var(--accent);text-decoration:none;overflow:hidden;text-overflow:ellipsis;white-space:nowrap"
           title="${escHtml(d.fileName)}"
           onmouseover="this.style.textDecoration='underline'" onmouseout="this.style.textDecoration='none'">
          ${escHtml(fmtDocType(d.documentType))}
        </a>
        <button onclick="printDocument(${d.index})" title="Print document"
          style="background:none;border:none;cursor:pointer;padding:2px 4px;color:var(--accent);display:flex;align-items:center;border-radius:4px;transition:opacity .15s;opacity:.75"
          onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='.75'">${printIcon}</button>
        <button class="registrar-only" onclick="deleteDocument(${d.index},'${escHtml(studentId)}','${escHtml(fmtDocType(d.documentType))}')" title="Remove document"
          style="background:none;border:none;cursor:pointer;padding:2px 4px;font-size:.8rem;font-weight:700;color:var(--danger);line-height:1;border-radius:4px;opacity:.6;transition:opacity .15s"
          onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='.6'">✕</button>
      </div>`).join('');
  } catch (_) {
    container.innerHTML = `<p style="font-size:.75rem;color:var(--gray-400);font-style:italic;margin:0">Could not load documents.</p>`;
  }
}

function printDocument(docId) {
  const win = window.open(`/api/documents/${docId}/file`, '_blank');
  if (win) win.addEventListener('load', () => { try { win.print(); } catch(_) {} });
}

function printAllDocuments() {
  const container = document.getElementById('sd-docs');
  const ids = (container?.dataset?.docIds || '').split(',').filter(Boolean);
  if (!ids.length) return;
  ids.forEach(id => window.open(`/api/documents/${id}/file`, '_blank'));
  toast(`${ids.length} document${ids.length > 1 ? 's' : ''} opened — press Ctrl+P in each tab to print`);
}

function fmtDocType(type) {
  const map = {
    BirthCertificate:'Birth Certificate', Form137:'Form 137', Diploma:'Diploma',
    BaptismalRecord:'Baptismal Record', ConfirmationRecord:'Confirmation Record',
    MarriageContractOfParents:'Marriage Contract of Parents', MedicalRecord:'Medical Record',
    DentalRecord:'Dental Record', ParishPriestRecommendation:'Parish Priest Recommendation',
    GoodMoral:'Good Moral Certificate', Other:'Other Document'
  };
  return map[type] || type;
}

let _deleteDocId = null;
let _deleteDocStudentId = null;

function deleteDocument(docId, studentId, docLabel) {
  _deleteDocId = docId;
  _deleteDocStudentId = studentId;
  document.getElementById('delete-doc-sub').textContent =
    `Are you sure you want to remove "${docLabel}" from this student's record? This cannot be undone.`;
  openModal('modal-delete-doc');
}

async function confirmDeleteDocument() {
  try {
    await api(`/api/documents/${_deleteDocId}`, 'DELETE');
    closeModal('modal-delete-doc');
    await loadStudentDocuments(_deleteDocStudentId);
    toast('Document removed');
  } catch (e) {
    closeModal('modal-delete-doc');
    toast('Failed to remove document', 'error');
  }
}

function onDocFileChange(input) {
  const file = input.files[0];
  const label = document.getElementById('doc-file-label');
  const zone  = document.getElementById('doc-file-zone');
  if (file) {
    label.textContent = file.name;
    label.style.color = 'var(--gray-800)';
    zone.style.borderColor = 'var(--accent)';
    zone.style.borderStyle = 'solid';
  } else {
    label.textContent = 'Click to choose a file or drag and drop here';
    label.style.color = 'var(--gray-600)';
    zone.style.borderColor = 'var(--gray-200)';
    zone.style.borderStyle = 'dashed';
  }
}

function handleDocFileDrop(event) {
  event.preventDefault();
  const zone = document.getElementById('doc-file-zone');
  zone.style.borderColor = 'var(--gray-200)';
  const file = event.dataTransfer.files[0];
  if (!file) return;
  const input = document.getElementById('doc-file');
  const dt = new DataTransfer();
  dt.items.add(file);
  input.files = dt.files;
  onDocFileChange(input);
}

function closeUploadDocModal() {
  closeModal('modal-upload-doc');
  document.getElementById('doc-file').value = '';
  document.getElementById('doc-remarks').value = '';
  document.getElementById('doc-type').value = '';
  document.getElementById('doc-file-label').textContent = 'Click to choose a file or drag and drop here';
  document.getElementById('doc-file-label').style.color = 'var(--gray-600)';
  const zone = document.getElementById('doc-file-zone');
  zone.style.borderColor = 'var(--gray-200)';
  zone.style.borderStyle = 'dashed';
  document.getElementById('upload-doc-error').style.display = 'none';
}

function showUploadDocError(msg) {
  const el = document.getElementById('upload-doc-error');
  el.textContent = msg;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function uploadStudentDocument() {
  document.getElementById('upload-doc-error').style.display = 'none';
  const type    = document.getElementById('doc-type').value;
  const file    = document.getElementById('doc-file').files[0];
  const remarks = document.getElementById('doc-remarks').value;

  if (!type)  { showUploadDocError('Please select a document type.'); return; }
  if (!file)  { showUploadDocError('Please select a file to upload.'); return; }

  const allowed = ['pdf', 'jpg', 'jpeg', 'png'];
  const ext = file.name.split('.').pop().toLowerCase();
  if (!allowed.includes(ext)) { showUploadDocError('File must be a PDF, JPG, or PNG.'); return; }
  if (file.size > 5 * 1024 * 1024) { showUploadDocError('File size must not exceed 5 MB.'); return; }

  // Warn if a document of the same type already exists
  try {
    const existing = await api(`/api/documents/student/${_currentStudentId}`);
    const duplicate = existing.find(d => d.documentType === type);
    if (duplicate) {
      const confirmed = confirm(`A ${fmtDocType(type)} is already on file for this student. Uploading a new one will add it alongside the existing document — it will not replace it. Continue?`);
      if (!confirmed) return;
    }
  } catch (_) {}

  const btn = document.querySelector('#modal-upload-doc .btn-primary');
  if (btn) { btn.disabled = true; btn.textContent = 'Uploading…'; }

  const formData = new FormData();
  formData.append('documentType', type);
  formData.append('file', file);
  if (remarks.trim()) formData.append('remarks', remarks.trim());
  try {
    const res = await fetch(`/api/documents/student/${_currentStudentId}`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
      body: formData
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || 'Upload failed. Please try again.');
    }
    closeUploadDocModal();
    await loadStudentDocuments(_currentStudentId);
    toast('Document uploaded successfully');
  } catch (e) { showUploadDocError(e.message); }
  finally { if (btn) { btn.disabled = false; btn.textContent = 'Upload'; } }
}

function readonlyField(label, value) {
  // SECURITY (A03): Escape value before injecting into HTML attribute
  return `<div class="field"><label>${escHtml(label)}</label><input value="${escHtml(value || '—')}" readonly/></div>`;
}

async function loadAlumni() {
  try {
    const data = await api('/api/alumni');
    const tbody = document.getElementById('tbl-alumni');
    clearSortCache('tbl-alumni');
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><p>No alumni records yet. Graduated students will appear here.</p></div></td></tr>'; return; }
    tbody.innerHTML = data.map(a =>
      `<tr><td>${escHtml(a.alumniId)}</td><td>${escHtml(a.student?.firstName)} ${escHtml(a.student?.lastName)}</td><td>${escHtml(a.program?.programCode)}</td><td>${fmtDate(a.graduationDate)}</td><td>${escHtml(a.honors || '—')}</td><td>${escHtml(a.currentMinistry || '—')}</td>
      <td style="display:flex;gap:6px">
        <button class="btn btn-outline btn-sm registrar-only">Edit</button>
        <button class="btn btn-outline btn-sm registrar-only" style="color:var(--danger);border-color:var(--danger)" onclick="unmarkAlumni('${escHtml(a.alumniId)}','${escHtml(a.student?.firstName)} ${escHtml(a.student?.lastName)}')">Unmark</button>
      </td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

let _unmarkAlumniId   = null;
let _unmarkAlumniName = null;

function unmarkAlumni(alumniId, studentName) {
  _unmarkAlumniId   = alumniId;
  _unmarkAlumniName = studentName;
  document.getElementById('unmark-alumni-name').textContent = studentName;
  openModal('modal-unmark-alumni');
}

async function confirmUnmarkAlumni() {
  try {
    await api(`/api/alumni/${_unmarkAlumniId}`, 'DELETE');
    toast(`${_unmarkAlumniName} has been reactivated as a student`);
    closeModal('modal-unmark-alumni');
    loadAlumni();
  } catch (e) { toast(e.message, 'error'); }
}

// Loads all curriculum versions for a program, auto-selects the active one, then loads its courses.
async function loadCurricula(programId) {
  try {
    const data = await api(`/api/curriculum/curricula?program=${programId}`);
    _curricula[programId] = data;
    const active = data.find(c => c.isActive);
    if (active) {
      _selectedCurriculum[programId] = active.curriculumId;
    } else if (data.length && !_selectedCurriculum[programId]) {
      _selectedCurriculum[programId] = data[0].curriculumId;
    }
    renderCurriculaSelector(programId);
    await loadCurriculum(programId);
  } catch (e) { console.error(e); }
}

// Populates the curriculum version <select> and status badge for the given program.
function renderCurriculaSelector(programId) {
  const selectId = programId === 'PRG-1001' ? 'philo-curriculum-select' : 'theo-curriculum-select';
  const badgeId  = programId === 'PRG-1001' ? 'philo-curriculum-badge'  : 'theo-curriculum-badge';
  const sel      = document.getElementById(selectId);
  const badgeEl  = document.getElementById(badgeId);
  if (!sel) return;
  sel.innerHTML = (_curricula[programId] || []).map(c =>
    `<option value="${escHtml(c.curriculumId)}">${escHtml(c.label)}${c.isActive ? ' (Active)' : ''}</option>`
  ).join('');
  const selected = _selectedCurriculum[programId];
  if (selected) sel.value = selected;
  const cur = (_curricula[programId] || []).find(c => c.curriculumId === selected);
  badgeEl.innerHTML = cur?.isActive ? badge('Active', 'success') : '';
  const activateBtnId = programId === 'PRG-1001' ? 'philo-curriculum-activate-btn' : 'theo-curriculum-activate-btn';
  const activateBtn = document.getElementById(activateBtnId);
  if (activateBtn) activateBtn.style.display = cur && !cur.isActive ? '' : 'none';
}

// Fetches courses for the currently selected curriculum version and renders the table.
async function loadCurriculum(programId) {
  try {
    const curriculumId = _selectedCurriculum[programId];
    const url = curriculumId
      ? `/api/curriculum/courses?curriculum=${curriculumId}`
      : `/api/curriculum/courses?program=${programId}`;
    const data = await api(url);
    data.forEach(c => { _courseMap[c.courseId] = c; });
    _currCourses[programId] = data;
    renderCurriculumTable(programId);
    _updateAddCourseButtonState(programId);
  } catch (e) { console.error(e); }
}

async function activateCurriculum(programId) {
  const curriculumId = _selectedCurriculum[programId];
  if (!curriculumId) return;
  try {
    await api(`/api/curriculum/curricula/${curriculumId}/activate`, 'PATCH');
    toast('Curriculum set as active');
    await loadCurricula(programId);
  } catch (e) { toast(e.message, 'error'); }
}

// Disables the "+ Add Course" button when viewing a historical (non-active) curriculum.
function _updateAddCourseButtonState(programId) {
  const cur = (_curricula[programId] || []).find(c => c.curriculumId === _selectedCurriculum[programId]);
  const btn = document.getElementById('btn-add-course');
  if (btn) btn.disabled = !cur; // disabled only if no curriculum is selected at all
}

function renderCurriculumTable(programId) {
  const tbodyId = programId === 'PRG-1001' ? 'tbl-curr-philo' : 'tbl-curr-theo';
  const tbody   = document.getElementById(tbodyId);
  const year    = _currYear[programId] || 1;
  const sem     = _currSem[programId]  || 1;
  const rows    = (_currCourses[programId] || []).filter(c => c.yearLevel === year && c.semesterNumber === sem);
  const cur     = (_curricula[programId] || []).find(c => c.curriculumId === _selectedCurriculum[programId]);
  const editable = !!cur; // editable whenever a curriculum is selected (backend enforces safety)

  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><p>No courses for this semester.</p></div></td></tr>';
    return;
  }
  tbody.innerHTML = rows.map(c => {
    const actions = editable
      ? `<button class="btn btn-outline btn-sm registrar-only" onclick="openCourseModal('${escHtml(c.courseId)}')">Edit</button>
         <button class="btn btn-danger btn-sm registrar-only" onclick="deleteCourse('${escHtml(c.courseId)}')">Delete</button>`
      : `<span style="color:var(--muted);font-size:.8rem">Read-only</span>`;
    return `<tr>
      <td>${escHtml(c.courseCode)}</td>
      <td>${escHtml(c.courseName)}</td>
      <td>${c.units}</td>
      <td>${c.prerequisites?.length ? c.prerequisites.map(p => badge(p.prerequisiteCourse?.courseCode,'warn')).join(' ') : 'None'}</td>
      <td style="white-space:nowrap">${actions}</td>
    </tr>`;
  }).join('');
}

function currYearTab(el, programId, year) {
  const tabsId = programId === 'PRG-1001' ? 'philo-year-tabs' : 'theo-year-tabs';
  document.querySelectorAll(`#${tabsId} .tab`).forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  _currYear[programId] = year;
  renderCurriculumTable(programId);
}

function currSemTab(el, programId, sem) {
  const tabsId = programId === 'PRG-1001' ? 'philo-sem-tabs' : 'theo-sem-tabs';
  document.querySelectorAll(`#${tabsId} .tab`).forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  _currSem[programId] = sem;
  renderCurriculumTable(programId);
}

function currTab(el, showId, programId) {
  switchTab(el, showId, currTabs);
  loadCurricula(programId);
}

async function onCurriculumChange(programId, curriculumId) {
  _selectedCurriculum[programId] = curriculumId;
  renderCurriculaSelector(programId);
  await loadCurriculum(programId);
}

let _secStudentsAll       = [];
let _allSections          = [];
let _allSemestersForFilter = [];
let _activeSemTab         = null;

async function loadSections() {
  try {
    [_allSections, _allSemestersForFilter] = await Promise.all([
      api('/api/sections'),
      api('/api/school-years/semesters').catch(() => []),
    ]);
    _buildSemFilter();
    _renderSectionsBySem(_activeSemTab);
    // Fetch enrolled counts in parallel and update badges
    Promise.all(_allSections.map(s =>
      api(`/api/sections/${s.sectionId}/students`)
        .then(ss => ({ id: s.sectionId, cap: s.capacity, count: ss.length }))
        .catch(() => ({ id: s.sectionId, cap: s.capacity, count: '?' }))
    )).then(counts => {
      counts.forEach(({ id, cap, count }) => {
        const badge = document.getElementById(`sec-badge-${id}`);
        if (badge) {
          badge.textContent = `${count} / ${cap}`;
          badge.classList.toggle('sec-enroll-full', typeof count === 'number' && count >= cap);
        }
      });
    });
  } catch (e) { console.error(e); }
}

function _buildSemFilter() {
  // Use full semesters list so empty semesters also appear in the filter
  const sems = _allSemestersForFilter.length ? _allSemestersForFilter : [];

  const sel = document.getElementById('sec-sem-filter');
  if (!sel) return;
  sel.innerHTML = '';

  if (!sems.length) return;

  // Default to the active semester, or the first in the list
  const activeSem = sems.find(s => s.isActive);
  if (!_activeSemTab || !sems.find(s => s.semesterId === _activeSemTab))
    _activeSemTab = activeSem?.semesterId || sems[0].semesterId;

  // Group by school year label for display (newest first — sems arrive DESC by index)
  sems.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s.semesterId;
    opt.textContent = s.semesterLabel;
    opt.selected = s.semesterId === _activeSemTab;
    sel.appendChild(opt);
  });
}

function _renderSectionsBySem(semId) {
  const wrap = document.getElementById('sec-groups-wrap');
  const filtered = semId ? _allSections.filter(s => s.semester?.semesterId === semId) : _allSections;

  if (!filtered.length) {
    wrap.innerHTML = '<div class="empty-state"><p>No sections for this semester.</p></div>';
    return;
  }

  // Group by year level
  const byYear = new Map();
  filtered.forEach(s => {
    const y = s.yearLevel ?? 0;
    if (!byYear.has(y)) byYear.set(y, []);
    byYear.get(y).push(s);
  });

  // Sort year levels ascending
  const sortedYears = [...byYear.keys()].sort((a, b) => a - b);

  wrap.innerHTML = sortedYears.map(year => {
    const sections = byYear.get(year);
    const rows = sections.map(s => `<tr>
      <td>${escHtml(s.sectionId)}</td>
      <td>${escHtml(s.sectionCode)}</td>
      <td>${escHtml(s.sectionName)}</td>
      <td>${escHtml(s.program?.programCode || '—')}</td>
      <td>
        <span id="sec-badge-${escHtml(s.sectionId)}" class="sec-enroll-badge"
          onclick="viewSectionStudents('${escHtml(s.sectionId)}','${escHtml(s.sectionName)}',${s.capacity})"
          title="Click to view enrolled students">— / ${s.capacity}</span>
      </td>
      <td style="white-space:nowrap">
        <button class="btn btn-outline btn-sm" onclick="viewSectionStudents('${escHtml(s.sectionId)}','${escHtml(s.sectionName)}',${s.capacity})">View</button>
        <button class="btn btn-outline btn-sm registrar-only" onclick='openSectionModal(${JSON.stringify(s)})'>Edit</button>
        <button class="btn btn-danger btn-sm registrar-only" onclick="deleteSection('${escHtml(s.sectionId)}')">Delete</button>
      </td>
    </tr>`).join('');

    return `
      <div class="sec-year-group">
        <div class="sec-year-label">Year ${year}</div>
        <table class="data-table" style="width:100%">
          <thead><tr>
            <th>ID</th><th>Code</th><th>Name</th><th>Program</th><th>Enrolled</th><th>Actions</th>
          </tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  }).join('');
}

async function viewSectionStudents(sectionId, sectionName, capacity) {
  document.getElementById('sec-stu-title').textContent = sectionName;
  document.getElementById('sec-stu-sub').textContent = `Section ID: ${sectionId}`;
  document.getElementById('sec-stu-count-badge').textContent = '…';
  document.getElementById('sec-stu-search').value = '';
  document.getElementById('tbl-sec-students').innerHTML =
    '<tr><td colspan="5" style="text-align:center;color:var(--gray-400);padding:16px">Loading…</td></tr>';
  openModal('modal-section-students');
  try {
    const students = await api(`/api/sections/${sectionId}/students`);
    _secStudentsAll = students;
    const badge = document.getElementById('sec-stu-count-badge');
    badge.textContent = `${students.length} / ${capacity}`;
    badge.className = 'sec-stu-count-badge' + (students.length >= capacity ? ' sec-enroll-full' : '');
    renderSecStudentRows(students);
  } catch (e) {
    document.getElementById('tbl-sec-students').innerHTML =
      `<tr><td colspan="5" style="text-align:center;color:var(--danger);padding:16px">${escHtml(e.message)}</td></tr>`;
  }
}

function renderSecStudentRows(list) {
  const tbody = document.getElementById('tbl-sec-students');
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><p>No students enrolled in this section.</p></div></td></tr>';
    return;
  }
  tbody.innerHTML = list.map(s =>
    `<tr>
      <td>${escHtml(s.studentId)}</td>
      <td>${escHtml(s.fullName)}</td>
      <td>${escHtml(s.program || '—')}</td>
      <td style="text-align:center">${s.currentYearLevel ?? '—'}</td>
      <td>${escHtml(s.dateAssigned || '—')}</td>
    </tr>`
  ).join('');
}

function filterSecStudents(query) {
  const q = query.toLowerCase();
  const filtered = _secStudentsAll.filter(s =>
    (s.fullName || '').toLowerCase().includes(q) || (s.studentId || '').toLowerCase().includes(q)
  );
  renderSecStudentRows(filtered);
}

async function loadSchedule() {
  try {
    const activeSemId = SMS.activeSemester?.semesterId || '';
    const sections = await api(`/api/sections?semester=${activeSemId}`);
    const sel = document.getElementById('sched-section-filter');
    sel.innerHTML = '<option value="">Select section…</option>' +
      sections.map(s => `<option value="${s.sectionId}">${s.sectionName}</option>`).join('');
  } catch (_) {}
  _renderScheduleTable([]);
}

function _renderScheduleTable(data) {
  const tbody = document.getElementById('tbl-schedule');
  if (!data || !data.length) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--gray-400);padding:20px">Select a section above to view its schedules</td></tr>';
    return;
  }
  _scheduleCache = {};
  data.forEach(s => { _scheduleCache[s.scheduleId] = s; });
  tbody.innerHTML = data.map(s =>
    `<tr>
      <td>${escHtml(s.section?.sectionCode)}</td>
      <td>${escHtml(s.course?.courseCode)} – ${escHtml(s.course?.courseName)}</td>
      <td>${escHtml(s.instructor?.firstName)} ${escHtml(s.instructor?.lastName)}</td>
      <td>${escHtml(s.room?.roomName)}</td>
      <td>${escHtml(s.dayOfWeek)}</td>
      <td>${_fmtTime(s.timeStart)} – ${_fmtTime(s.timeEnd)}</td>
      <td style="display:flex;gap:6px">
        <button class="btn btn-outline btn-sm registrar-only" onclick="openEditSchedModal('${escHtml(s.scheduleId)}')">Edit</button>
        <button class="btn btn-danger btn-sm registrar-only" onclick="confirmDeleteSchedule('${escHtml(s.scheduleId)}')">Delete</button>
      </td>
    </tr>`
  ).join('');
}

// ── Continuous-time calendar helpers ────────────────────────────────────────
const SCHED_PX_PER_MIN = 1.5;          // 90 px per hour
const SCHED_CAL_START  = 6  * 60;      // 6:00 AM in minutes
const SCHED_CAL_END    = 22 * 60;      // 10:00 PM in minutes
const SCHED_CAL_H      = (SCHED_CAL_END - SCHED_CAL_START) * SCHED_PX_PER_MIN; // 1440 px

function _schedMin(t) {
  if (!t) return 0;
  const [h, m] = t.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

function _fmtTime(t) {
  if (!t) return '';
  const [h, m] = t.split(':');
  return `${h}:${m}`;
}

function _initSchedCal(gutterId, colPrefix) {
  const DAYS = ['mon','tue','wed','thu','fri'];
  const gutter = document.getElementById(gutterId);
  if (!gutter) return;
  gutter.style.height = SCHED_CAL_H + 'px';
  gutter.innerHTML = '';
  DAYS.forEach(d => {
    const col = document.getElementById(`${colPrefix}-${d}`);
    if (col) { col.style.height = SCHED_CAL_H + 'px'; col.innerHTML = ''; }
  });
  for (let h = 6; h <= 21; h++) {
    const top = (h * 60 - SCHED_CAL_START) * SCHED_PX_PER_MIN;
    const lbl = document.createElement('div');
    lbl.className = 'sched-hour-label';
    lbl.style.top = top + 'px';
    lbl.textContent = h === 12 ? '12:00' : h < 12 ? `${h}:00` : `${h - 12}:00`;
    gutter.appendChild(lbl);
    DAYS.forEach(d => {
      const col = document.getElementById(`${colPrefix}-${d}`);
      if (!col) return;
      const line = document.createElement('div');
      line.className = 'sched-hour-line';
      line.style.top = top + 'px';
      col.appendChild(line);
      if (h < 21) {
        const half = document.createElement('div');
        half.className = 'sched-hour-line minor';
        half.style.top = (top + 30 * SCHED_PX_PER_MIN) + 'px';
        col.appendChild(half);
      }
    });
  }
}

function _renderSchedBlocks(colPrefix, data, editable) {
  const DAY_MAP = { Monday:'mon', Tuesday:'tue', Wednesday:'wed', Thursday:'thu', Friday:'fri' };
  const byDay   = { mon:[], tue:[], wed:[], thu:[], fri:[] };
  data.forEach(s => { const d = DAY_MAP[s.dayOfWeek]; if (d) byDay[d].push(s); });

  Object.entries(byDay).forEach(([day, scheds]) => {
    const col = document.getElementById(`${colPrefix}-${day}`);
    if (!col) return;
    scheds.sort((a, b) => (a.timeStart || '').localeCompare(b.timeStart || ''));

    // Greedy lane assignment — overlapping cards split into side-by-side lanes
    const lanes = [];
    const meta  = scheds.map(s => {
      const startMin = _schedMin(s.timeStart);
      const endMin   = _schedMin(s.timeEnd);
      let lane = lanes.findIndex(end => end <= startMin);
      if (lane === -1) { lane = lanes.length; lanes.push(endMin); }
      else lanes[lane] = endMin;
      return { s, startMin, endMin, lane };
    });
    const totalLanes = Math.max(lanes.length, 1);

    meta.forEach(({ s, startMin, endMin, lane }, i) => {
      const top    = (startMin - SCHED_CAL_START) * SCHED_PX_PER_MIN;
      const height = Math.max((endMin - startMin) * SCHED_PX_PER_MIN, 28);
      const color  = SCHED_COLORS[i % SCHED_COLORS.length];
      const instr  = [s.instructor?.firstName, s.instructor?.lastName].filter(Boolean).join(' ');
      const tooltipParts = [
        s.course?.courseCode,
        `${_fmtTime(s.timeStart)}–${_fmtTime(s.timeEnd)}`,
        s.room?.roomName,
        instr
      ].filter(Boolean);
      const block  = document.createElement('div');
      block.className = 'sched-block' + (editable ? ' editable' : '');
      block.title = tooltipParts.join(' | ');
      block.style.cssText = `top:${top}px;height:${height}px;`
        + `left:calc(${(lane / totalLanes) * 100}% + 4px);`
        + `width:calc(${100 / totalLanes}% - 8px);`
        + `background:${color};color:#fff`;
      block.innerHTML =
        `<div class="si-course">${escHtml(s.course?.courseCode || '')}</div>`
        + `<div class="si-time">${escHtml(_fmtTime(s.timeStart))}–${escHtml(_fmtTime(s.timeEnd))}</div>`
        + `<div class="si-room">${escHtml(s.room?.roomName || '')}</div>`
        + (instr ? `<div class="si-instructor">${escHtml(instr)}</div>` : '');
      if (editable) block.onclick = () => openEditSchedModal(s.scheduleId);
      col.appendChild(block);
    });
  });
}

function loadScheduleGrid() {
  _initSchedCal('gc-gutter', 'gc-col');
  const sectionId = document.getElementById('sched-section-filter')?.value;
  if (!sectionId) { _renderScheduleTable([]); return; }
  api(`/api/schedule?section=${sectionId}`)
    .then(data => { _renderSchedBlocks('gc-col', data, true); _renderScheduleTable(data); })
    .catch(console.error);
}

// Student list for grade search — loaded once, reused on every keystroke
let _gradeStudents = [];
let _selectedGradeStudentId = null;

async function loadGrades() {
  try {
    // Populate semester dropdown on first load
    const semEl = document.getElementById('grade-filter-sem');
    if (semEl && semEl.options.length === 0) {
      try {
        const [semesters, activeSem] = await Promise.all([
          api('/api/school-years/semesters'),
          SMS.activeSemester ? Promise.resolve(SMS.activeSemester) : api('/api/school-years/semesters/active').catch(() => null)
        ]);
        if (!SMS.activeSemester && activeSem) SMS.activeSemester = activeSem;
        semesters.forEach(s => {
          const opt = document.createElement('option');
          opt.value = s.semesterId;
          const activeMark = SMS.activeSemester?.semesterId === s.semesterId ? ' (Active)' : '';
          opt.textContent = s.semesterLabel + activeMark;
          semEl.appendChild(opt);
        });
      } catch (_) {}
      if (SMS.activeSemester) semEl.value = SMS.activeSemester.semesterId;
    }

    // Load student list once for the search box
    if (_gradeStudents.length === 0) {
      try { _gradeStudents = await api('/api/students'); } catch (_) {}
    }

    const tbody = document.getElementById('tbl-grades');
    if (!_selectedGradeStudentId) {
      tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><p>Search and select a student above to view their grades.</p></div></td></tr>';
      return;
    }

    // Loading state — Nielsen H1: visibility of system status
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:28px;color:var(--gray-400)">Loading grades…</td></tr>';

    // Active Semester option (empty value) falls back to the actual active semester ID
    const selectedSem = semEl?.value || SMS.activeSemester?.semesterId || '';
    const semParam = selectedSem ? `?semester=${selectedSem}` : '';
    const data = await api(`/api/grades/student/${_selectedGradeStudentId}${semParam}`);

    if (!data.length) {
      tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><p>No grades recorded for this student yet.</p></div></td></tr>';
      return;
    }

    const student = data[0].student;
    const name = escHtml(`${student?.firstName || ''} ${student?.lastName || ''}`.trim());
    const sid  = escHtml(student?.studentId || '—');

    let totalWeighted = 0, totalUnits = 0;
    data.forEach(g => {
      if (g.finalRating != null && g.course?.units) {
        totalWeighted += parseFloat(g.finalRating) * g.course.units;
        totalUnits += g.course.units;
      }
    });
    const gwa = totalUnits > 0 ? (totalWeighted / totalUnits).toFixed(2) : null;
    const gwaHtml = gwa
      ? `<span class="grade-gwa-chip ${parseFloat(gwa) <= 3.0 ? 'grade-pass' : 'grade-fail'}">GWA ${gwa}</span>`
      : `<span class="grade-gwa-chip" style="color:var(--gray-400)">No ratings yet</span>`;

    let html = `<tr class="grade-group-header">
      <td colspan="6"><div class="grade-group-meta">
        <span class="grade-group-name">${name}</span>
        <span class="grade-group-id">${sid}</span>
        ${gwaHtml}
      </div></td><td></td>
    </tr>`;

    data.forEach(g => {
      html += `<tr class="grade-detail-row">
        <td>
          <span class="grade-course-code">${escHtml(g.course?.courseCode || '—')}</span>
          <span class="grade-course-name">${escHtml(g.course?.courseName || '')}</span>
        </td>
        <td style="text-align:center">${g.course?.units ?? '—'}</td>
        <td class="${gradeClass(g.midtermGrade)}">${g.midtermGrade || '—'}</td>
        <td class="${gradeClass(g.finalGrade)}">${g.finalGrade || '—'}</td>
        <td class="${gradeClass(g.finalRating)}">${g.finalRating || '—'}</td>
        <td>${badge(g.gradeStatus)}</td>
        <td><button class="btn btn-outline btn-sm registrar-only"
          data-grade-id="${g.gradeId}"
          data-student="${escHtml((g.student?.firstName || '') + ' ' + (g.student?.lastName || ''))}"
          data-course="${escHtml(g.course?.courseCode || '')}"
          data-mt-cs="${g.midtermClassStanding || ''}"
          data-mt-exam="${g.midtermExam || ''}"
          data-fn-cs="${g.finalClassStanding || ''}"
          data-fn-exam="${g.finalExam || ''}"
          data-mt-grade="${g.midtermGrade || ''}"
          data-fn-grade="${g.finalGrade || ''}"
          data-status="${escHtml(g.gradeStatus || '')}"
          data-remarks="${escHtml(g.remarks || '')}"
          onclick="editGradeFromRow(this)">Edit</button></td>
      </tr>`;
    });

    tbody.innerHTML = html;
  } catch (e) { console.error(e); }
}

function filterGradeStudents(query) {
  const sugEl = document.getElementById('grade-suggestions');
  const q = query.trim().toLowerCase();

  // Clear selection whenever the user edits the input — mark as unconfirmed
  _selectedGradeStudentId = null;
  document.getElementById('grade-student-search')?.classList.add('unconfirmed');

  if (!q) {
    sugEl.innerHTML = '';
    sugEl.classList.remove('open');
    document.getElementById('grade-student-search')?.classList.remove('unconfirmed');
    document.getElementById('tbl-grades').innerHTML =
      '<tr><td colspan="7"><div class="empty-state"><p>Search and select a student above to view their grades.</p></div></td></tr>';
    return;
  }

  const matches = _gradeStudents
    .filter(s => `${s.firstName} ${s.lastName}`.toLowerCase().includes(q) || s.studentId.toLowerCase().includes(q))
    .slice(0, 8);

  if (!matches.length) {
    sugEl.innerHTML = '<div class="grade-sug-empty">No students found</div>';
    sugEl.classList.add('open');
    return;
  }

  // tabindex + onkeydown on each item for keyboard navigation (WCAG 2.1, ISO 9241-171)
  sugEl.innerHTML = matches.map(s =>
    `<div class="grade-sug-item" tabindex="0"
      onmousedown="selectGradeStudent('${escHtml(s.studentId)}','${escHtml(s.firstName + ' ' + s.lastName)}')"
      onkeydown="onGradeSugKeydown(event,this,'${escHtml(s.studentId)}','${escHtml(s.firstName + ' ' + s.lastName)}')">
      <span class="grade-sug-name">${escHtml(s.firstName)} ${escHtml(s.lastName)}</span>
      <span class="grade-sug-id">${escHtml(s.studentId)}</span>
    </div>`
  ).join('');
  sugEl.classList.add('open');
}

function selectGradeStudent(studentId, fullName) {
  _selectedGradeStudentId = studentId;
  const input = document.getElementById('grade-student-search');
  input.value = fullName;
  input.classList.remove('unconfirmed');
  const sugEl = document.getElementById('grade-suggestions');
  sugEl.classList.remove('open');
  sugEl.innerHTML = '';
  loadGrades();
}

// ArrowDown from input → focus first suggestion; Escape → close
function onGradeSearchKeydown(e) {
  const sugEl = document.getElementById('grade-suggestions');
  if (e.key === 'ArrowDown') {
    e.preventDefault();
    const first = sugEl.querySelector('.grade-sug-item');
    if (first) first.focus();
  } else if (e.key === 'Escape') {
    sugEl.classList.remove('open');
    sugEl.innerHTML = '';
  }
}

// Arrow keys + Enter + Escape on suggestion items
function onGradeSugKeydown(e, el, studentId, fullName) {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    selectGradeStudent(studentId, fullName);
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    const next = el.nextElementSibling;
    if (next?.classList.contains('grade-sug-item')) next.focus();
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    const prev = el.previousElementSibling;
    if (prev?.classList.contains('grade-sug-item')) prev.focus();
    else document.getElementById('grade-student-search').focus();
  } else if (e.key === 'Escape') {
    document.getElementById('grade-suggestions').classList.remove('open');
    document.getElementById('grade-suggestions').innerHTML = '';
    document.getElementById('grade-student-search').focus();
  }
}

// Close suggestions when clicking anywhere outside the search box
document.addEventListener('click', e => {
  if (!e.target.closest('.grade-search-wrap')) {
    const sugEl = document.getElementById('grade-suggestions');
    if (sugEl) { sugEl.classList.remove('open'); sugEl.innerHTML = ''; }
  }
});

async function loadInstructors() {
  try {
    const data = await api('/api/sections/instructors');
    const tbody = document.getElementById('tbl-instructors');
    clearSortCache('tbl-instructors');
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No instructors found.</p></div></td></tr>'; return; }
    _instructorCache = {};
    data.forEach(i => { _instructorCache[i.instructorId] = i; });
    tbody.innerHTML = data.map(i =>
      `<tr><td>${escHtml(i.instructorId)}</td><td>${escHtml(i.firstName)} ${escHtml(i.lastName)}</td><td>${escHtml(i.email || '—')}</td><td>${escHtml(i.specialization || '—')}</td>
      <td>${badge(i.isActive ? 'Active' : 'Inactive', i.isActive ? 'success' : 'gray')}</td>
      <td>
        <button class="btn btn-outline btn-sm registrar-only" onclick="editInstructor('${escHtml(i.instructorId)}')">Edit</button>
        <button class="btn btn-danger btn-sm registrar-only" onclick="deleteInstructor('${escHtml(i.instructorId)}')">Delete</button>
      </td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadRooms() {
  try {
    const data = await api('/api/sections/rooms');
    const tbody = document.getElementById('tbl-rooms');
    clearSortCache('tbl-rooms');
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No rooms found.</p></div></td></tr>'; return; }
    _roomCache = {};
    data.forEach(r => { _roomCache[r.roomId] = r; });
    tbody.innerHTML = data.map(r =>
      `<tr><td>${escHtml(r.roomId)}</td><td>${escHtml(r.roomName)}</td><td>${escHtml(r.building || '—')}</td><td>${r.capacity || '—'}</td>
      <td>${badge(r.isActive ? 'Active' : 'Inactive', r.isActive ? 'success' : 'gray')}</td>
      <td>
        <button class="btn btn-outline btn-sm registrar-only" onclick="editRoom('${escHtml(r.roomId)}')">Edit</button>
        <button class="btn btn-danger btn-sm registrar-only" onclick="deleteRoom('${escHtml(r.roomId)}')">Delete</button>
      </td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadUsers() {
  try {
    const data = await api('/api/users');
    const tbody = document.getElementById('tbl-users');
    clearSortCache('tbl-users');
    tbody.innerHTML = data.map(u =>
      `<tr>
        <td>${escHtml(u.userId)}</td><td>${escHtml(u.username)}</td>
        <td>${badge(u.role, u.role==='Registrar' ? 'info' : 'gray')}</td>
        <td>${badge(u.isActive ? 'Active' : 'Inactive', u.isActive ? 'success' : 'danger')}</td>
        <td>
          ${u.userId !== SMS.currentUser?.userId ? `<button class="btn ${u.isActive ? 'btn-danger' : 'btn-outline'} btn-sm" onclick="toggleUser('${escHtml(u.userId)}')">${u.isActive ? 'Disable' : 'Enable'}</button>` : ''}
          <button class="btn btn-outline btn-sm" onclick="resetPw('${escHtml(u.userId)}')">Reset PW</button>
        </td>
      </tr>`
    ).join('') || '<tr><td colspan="5" style="text-align:center;color:var(--gray-400)">No users found</td></tr>';
  } catch (e) { console.error(e); }
}

async function loadSchoolYears() {
  try {
    const data = await api('/api/school-years/semesters');
    const tbody = document.getElementById('tbl-semesters');
    if (!data.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--gray-400)">No semesters found</td></tr>';
      return;
    }

    // Group semesters by school year
    const groups = {};
    data.forEach(s => {
      const key = s.schoolYear?.yearLabel || 'Unknown';
      if (!groups[key]) groups[key] = [];
      groups[key].push(s);
    });

    tbody.innerHTML = Object.entries(groups).map(([yearLabel, sems]) => {
      const groupHeader = `<tr>
        <td colspan="5" style="background:var(--gray-50);padding:8px 14px;font-weight:700;font-size:.8rem;color:var(--navy);letter-spacing:.03em;border-bottom:1px solid var(--gray-200)">${yearLabel}</td>
      </tr>`;
      const rows = sems.map(s => {
        const sd = JSON.stringify(s).replace(/'/g, '&#39;');
        const semLabel = s.semesterNumber === 1 ? '1st Semester' : s.semesterNumber === 2 ? '2nd Semester' : 'Summer';
        const editBtn  = `<button class="btn btn-outline btn-sm" onclick='openEditSemModal(${sd})'>Edit</button>`;
        const activeBtn = !s.isActive
          ? `<button class="btn btn-success btn-sm" onclick="activateSem('${s.semesterId}')">Set Active</button>`
          : '<span style="color:var(--gray-400);font-size:.8rem">★ Current</span>';
        return `<tr>
          <td style="padding-left:24px">${semLabel}</td>
          <td>${fmtDate(s.startDate)}</td>
          <td>${fmtDate(s.endDate)}</td>
          <td>${badge(s.isActive ? 'Active' : 'Inactive', s.isActive ? 'success' : 'gray')}</td>
          <td style="display:flex;gap:6px;align-items:center">${editBtn}${activeBtn}</td>
        </tr>`;
      }).join('');
      return groupHeader + rows;
    }).join('');
  } catch (e) { console.error(e); }
}

function openEditSemModal(s) {
  document.getElementById('sem-id').value    = s.semesterId;
  document.getElementById('sy-label').value  = s.schoolYear?.yearLabel || '';
  document.getElementById('sy-label').readOnly = true;
  document.getElementById('sy-label').style.opacity = '.5';
  document.getElementById('sem-num').value   = s.semesterNumber;
  document.getElementById('sem-start').value = s.startDate;
  document.getElementById('sem-end').value   = s.endDate;
  document.getElementById('sy-modal').classList.add('editing');
  document.querySelector('#sy-modal .edit-mode-banner').style.display = '';
  document.getElementById('sy-modal-title').textContent = s.semesterLabel;
  document.getElementById('sy-modal-sub').textContent   = s.semesterId;
  document.getElementById('sy-save-btn').textContent    = 'Save Changes';
  openModal('modal-school-year');
}

function closeSemModal() {
  document.getElementById('sem-id').value    = '';
  document.getElementById('sy-label').value  = '';
  document.getElementById('sy-label').readOnly = false;
  document.getElementById('sy-label').style.opacity = '';
  document.getElementById('sem-start').value = '';
  document.getElementById('sem-end').value   = '';
  document.getElementById('sem-num').value   = '1';
  document.getElementById('sy-modal').classList.remove('editing');
  document.querySelector('#sy-modal .edit-mode-banner').style.display = 'none';
  document.getElementById('sy-modal-title').textContent = 'Add Semester';
  document.getElementById('sy-modal-sub').textContent   = 'Create a new school year and semester';
  document.getElementById('sy-save-btn').textContent    = 'Save';
  closeModal('modal-school-year');
}

async function loadBackup() {
  try {
    const data = await api('/api/backup/log');
    const tbody = document.getElementById('tbl-backup');
    if (!Array.isArray(data) || !data.length) {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--gray-400);padding:20px">No backup records yet.</td></tr>';
      return;
    }
    tbody.innerHTML = data.map(b =>
      `<tr><td>${fmtDate(b.backupDate)}</td><td>${escHtml(b.backupType)}</td><td>${escHtml(b.performedBy?.username || '—')}</td><td>${escHtml(b.notes || '—')}</td></tr>`
    ).join('');
  } catch (_) {}
}

// ── ADMIN PAGE LOADERS ────────────────────────────────────────

let _auditPage = 0;
let _auditFilter = null;

function setAuditFilter(type, btn) {
  _auditFilter = type;
  document.querySelectorAll('.audit-filter-tab').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  loadAuditLog(0);
}

async function runDocumentBackfill() {
  if (!confirm('This will copy online submission documents to student records for all existing students. Safe to run multiple times. Continue?')) return;
  try {
    const result = await api('/api/documents/backfill', 'POST');
    toast(`Backfill complete — ${result.documentsTransferred} documents transferred, ${result.studentsSkipped} students skipped`);
  } catch (e) { toast('Backfill failed: ' + e.message, 'error'); }
}

async function loadAuditLog(page) {
  _auditPage = (page !== undefined) ? page : 0;
  try {
    const typeParam = _auditFilter ? `&type=${_auditFilter}` : '';
    const data = await api(`/api/admin/audit-logs?page=${_auditPage}&size=50${typeParam}`);
    const tbody = document.getElementById('tbl-audit-log');
    clearSortCache('tbl-audit-log');
    if (!tbody) return;
    const actionColors = {
      CREATE: '#2d7d46', UPDATE: '#b45309', DELETE: '#c0392b',
      LOGIN_SUCCESS: '#1d4ed8', FAILED_LOGIN: '#7c3aed', ACCOUNT_LOCKED: '#dc2626',
      LOGOUT: '#374151', PASSWORD_CHANGED: '#0369a1', PASSWORD_RESET_REQUESTED: '#b45309',
      PASSWORD_RESET_COMPLETED: '#2d7d46', INVALID_RESET_TOKEN: '#dc2626',
      INACTIVE_LOGIN_ATTEMPT: '#7c3aed'
    };
    const typeBadge = t => t === 'SECURITY'
      ? `<span style="background:#fef2f2;color:#991b1b;border:1px solid #fca5a5;padding:2px 8px;border-radius:12px;font-size:.75rem;font-weight:600">Security</span>`
      : `<span style="background:#eff6ff;color:#1e40af;border:1px solid #bfdbfe;padding:2px 8px;border-radius:12px;font-size:.75rem;font-weight:600">Audit</span>`;
    tbody.innerHTML = (data.items || []).map(e => {
      const color = actionColors[e.action] || '#374151';
      return `<tr>
        <td style="white-space:nowrap;font-size:.8rem">${fmtDateTime(e.timestamp)}</td>
        <td>${escHtml(e.performedBy)}</td>
        <td><span class="badge">${escHtml(e.role)}</span></td>
        <td>${typeBadge(e.logType)}</td>
        <td><span style="font-weight:600;color:${color}">${escHtml(e.action)}</span></td>
        <td style="max-width:320px;font-size:.82rem">${escHtml(e.detail || '—')}</td>
        <td style="font-size:.8rem;color:var(--gray-400)">${escHtml(e.ipAddress || '—')}</td>
      </tr>`;
    }).join('') || '<tr><td colspan="7" style="text-align:center;color:var(--gray-400);padding:20px">No log entries found.</td></tr>';

    const pag = document.getElementById('audit-log-pagination');
    if (pag && data.totalPages > 1) {
      const totalPages = data.totalPages;
      let html = `<span>Page ${_auditPage + 1} of ${totalPages} &nbsp;|&nbsp; ${data.totalItems} total entries</span>&nbsp;`;
      if (_auditPage > 0) html += `<button class="btn btn-outline btn-sm" onclick="loadAuditLog(${_auditPage - 1})">← Prev</button> `;
      if (_auditPage < totalPages - 1) html += `<button class="btn btn-outline btn-sm" onclick="loadAuditLog(${_auditPage + 1})">Next →</button>`;
      pag.innerHTML = html;
    } else if (pag) {
      pag.innerHTML = data.totalItems ? `<span>${data.totalItems} entries</span>` : '';
    }
  } catch (e) { console.error(e); }
}

// ── STUDENT PAGE LOADERS ───────────────────────────────────────

let _allMyGrades = [];

async function loadMyGrades() {
  try {
    const [me, activeSem] = await Promise.all([
      api('/api/students/me'),
      SMS.activeSemester ? Promise.resolve(SMS.activeSemester) : api('/api/school-years/semesters/active')
    ]);
    if (!SMS.activeSemester && activeSem) SMS.activeSemester = activeSem;

    if (me) {
      document.getElementById('my-grades-sub').textContent =
        `${me.studentId} · ${me.firstName} ${me.lastName} · ${me.program?.programName} · Year ${me.currentYearLevel}`;
    }

    _allMyGrades = await api('/api/grades/student/me');

    // Build semester dropdown — always include the active semester even if no grades yet
    const semsMap = new Map();
    if (SMS.activeSemester?.semesterId) semsMap.set(SMS.activeSemester.semesterId, SMS.activeSemester);
    _allMyGrades.forEach(g => {
      if (g.semester?.semesterId) semsMap.set(g.semester.semesterId, g.semester);
    });
    const sems = Array.from(semsMap.values()).sort((a, b) => {
      const yl = (b.schoolYear?.yearLabel || b.semesterLabel || '').localeCompare(
                  a.schoolYear?.yearLabel || a.semesterLabel || '');
      if (yl !== 0) return yl;
      return (b.semesterNumber || 0) - (a.semesterNumber || 0);
    });

    const semSelect = document.getElementById('my-grades-sem-filter');
    semSelect.innerHTML = sems.map(s =>
      `<option value="${escHtml(s.semesterId)}">${escHtml(s.semesterLabel)}</option>`
    ).join('');

    // Default to active semester
    const activeSemId = SMS.activeSemester?.semesterId;
    if (activeSemId) {
      semSelect.value = activeSemId;
    } else if (sems.length) {
      semSelect.value = sems[0].semesterId;
    }

    renderMyGradesForSem(semSelect.value);
  } catch (e) { console.error(e); }
}

function filterMyGradesBySem() {
  const semId = document.getElementById('my-grades-sem-filter').value;
  renderMyGradesForSem(semId);
}

function renderMyGradesForSem(semId) {
  const grades = semId
    ? _allMyGrades.filter(g => g.semester?.semesterId === semId)
    : _allMyGrades;

  const semLabel = _allMyGrades.find(g => g.semester?.semesterId === semId)?.semester?.semesterLabel || 'Grades';
  document.getElementById('my-grades-sem').textContent = semLabel;

  const tbody = document.getElementById('tbl-my-grades');
  tbody.innerHTML = grades.map(g =>
    `<tr>
      <td>${g.course?.courseCode}</td><td>${g.course?.courseName}</td><td>${g.course?.units}</td>
      <td class="${gradeClass(g.midtermGrade)}">${g.midtermGrade || '—'}</td>
      <td class="${gradeClass(g.finalGrade)}">${g.finalGrade || '—'}</td>
      <td class="${gradeClass(g.finalRating)}">${g.finalRating || '—'}</td>
      <td>${badge(g.gradeStatus)}</td>
    </tr>`
  ).join('') || '<tr><td colspan="7" style="text-align:center;color:var(--gray-400)">No grades yet</td></tr>';

  // GWA for the selected semester
  const gwaEl = document.getElementById('my-gwa');
  const valid = grades.filter(g => g.finalRating != null);
  if (valid.length) {
    const totalWeighted = valid.reduce((s, g) => s + g.finalRating * (g.course?.units || 1), 0);
    const totalUnits    = valid.reduce((s, g) => s + (g.course?.units || 1), 0);
    gwaEl.textContent = (totalWeighted / totalUnits).toFixed(2);
  } else {
    gwaEl.textContent = '—';
  }
}

async function loadMySchedule() {
  _initSchedCal('ms-gutter', 'ms-col');
  try {
    const data = await api('/api/schedule/mine');
    _renderSchedBlocks('ms-col', data, false);
  } catch (e) { console.error(e); }
}

async function loadMyProfile() {
  // Load profile picture (for all users)
  try {
    const photoResp = await fetch('/api/me/photo?t=' + Date.now(), { credentials: 'same-origin' });
    if (photoResp.ok) {
      const blob = await photoResp.blob();
      const url  = URL.createObjectURL(blob);
      applyAvatarPhoto('mp-avatar', url);
      applyAvatarPhoto('hdr-avatar', url);
    }
  } catch (_) {}

  if (SMS.role === 'Student') {
    document.getElementById('mp-student-info').style.display  = '';
    document.getElementById('mp-registrar-info').style.display = 'none';
    try {
      const s = await api('/api/students/me');
      const av = document.getElementById('mp-avatar');
      if (!av.style.backgroundImage) {
        av.textContent = (s.firstName[0] + s.lastName[0]).toUpperCase();
      }
      document.getElementById('mp-name').textContent = `${s.firstName} ${s.lastName}`;
      document.getElementById('mp-id').textContent   = s.studentId;
      document.getElementById('mp-info').innerHTML = `
        <div class="info-item"><span class="i-label">Program</span><span class="i-val">${escHtml(s.program?.programName || '—')}</span></div>
        <div class="info-item"><span class="i-label">Year Level</span><span class="i-val">${escHtml(s.currentYearLevel)}</span></div>
        <div class="info-item"><span class="i-label">Seminary Level</span><span class="i-val">${escHtml(s.seminaryLevel)}</span></div>
        <div class="info-item"><span class="i-label">Status</span><span class="i-val">${escHtml(s.currentStatus)}</span></div>
        <div class="info-item"><span class="i-label">Email</span><span class="i-val" style="font-size:.75rem">${escHtml(s.email)}</span></div>`;
      document.getElementById('mp-fields').innerHTML =
        readonlyField('First Name', s.firstName) + readonlyField('Last Name', s.lastName) +
        readonlyField('Date of Birth', fmtDate(s.dateOfBirth)) +
        readonlyField('Nationality', s.nationality || '—') + readonlyField('Religion', s.religion || '—');
    } catch (e) { console.error(e); }
  } else {
    // Registrar
    document.getElementById('mp-student-info').style.display  = 'none';
    document.getElementById('mp-registrar-info').style.display = '';
    const av = document.getElementById('mp-avatar');
    if (!av.style.backgroundImage) {
      av.textContent = SMS.currentUser?.username?.substring(0, 2).toUpperCase() || '??';
    }
    document.getElementById('mp-name').textContent = SMS.currentUser?.username || '—';
    document.getElementById('mp-id').textContent   = '';
    document.getElementById('mp-info').innerHTML   = `
      <div class="info-item"><span class="i-label">Role</span><span class="i-val">Registrar</span></div>`;
  }
}

// ── Apply photo to an avatar div (uses background-image) ──────
function applyAvatarPhoto(elementId, url) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = '';
  el.style.backgroundImage    = `url('${url}')`;
  el.style.backgroundSize     = 'cover';
  el.style.backgroundPosition = 'center';
}

// ── Upload Profile Picture ────────────────────────────────────
async function uploadProfilePic(input) {
  const file = input.files[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) { toast('Only image files are allowed', 'error'); input.value = ''; return; }
  if (file.size > 2 * 1024 * 1024) { toast('Image must be smaller than 2 MB', 'error'); input.value = ''; return; }

  const formData = new FormData();
  formData.append('photo', file);
  try {
    const resp = await fetch('/api/me/photo', {
      method: 'POST',
      credentials: 'same-origin',
      body: formData,
    });
    if (!resp.ok) {
      const data = await resp.json().catch(() => ({ error: 'Upload failed' }));
      throw new Error(data.error || 'Upload failed');
    }
    const url = URL.createObjectURL(file);
    applyAvatarPhoto('mp-avatar',  url);
    applyAvatarPhoto('hdr-avatar', url);
    toast('Profile photo updated');
  } catch (e) {
    toast(e.message, 'error');
  }
  input.value = '';
}

// ── Change Username (self-service) ────────────────────────────
async function changeMyUsername() {
  const newUsername = document.getElementById('settings-username').value.trim();
  if (!newUsername) { toast('Please enter a new username', 'error'); return; }
  try {
    const result = await api('/api/me/username', 'PATCH', { username: newUsername });
    toast(result.message || 'Username updated');
    document.getElementById('settings-username').value = '';
    // Update header display; full effect requires re-login
    document.getElementById('hdr-name').textContent = newUsername;
  } catch (e) { toast(e.message, 'error'); }
}

// ── Change Password (self-service) ────────────────────────────
async function changeMyPassword() {
  const cur     = document.getElementById('settings-cur-pw').value;
  const newPw   = document.getElementById('settings-new-pw').value;
  const confirm = document.getElementById('settings-confirm-pw').value;
  if (!cur || !newPw || !confirm) { toast('All fields are required', 'error'); return; }
  if (newPw !== confirm) { toast('New passwords do not match', 'error'); return; }
  try {
    const result = await api('/api/me/password', 'PATCH', { currentPassword: cur, newPassword: newPw });
    toast(result.message || 'Password updated');
    document.getElementById('settings-cur-pw').value     = '';
    document.getElementById('settings-new-pw').value     = '';
    document.getElementById('settings-confirm-pw').value = '';
  } catch (e) { toast(e.message, 'error'); }
}

// ── SAVE ACTIONS ──────────────────────────────────────────────

async function saveApplicant() {
  if (!validateRequired([
    {id:'ap-fname', label:'First Name'},
    {id:'ap-lname', label:'Last Name'},
    {id:'ap-dob',   label:'Date of Birth'},
    {id:'ap-email', label:'Email'},
  ])) return;
  try {
    await api('/api/applicants', 'POST', {
      firstName:        document.getElementById('ap-fname').value,
      lastName:         document.getElementById('ap-lname').value,
      middleName:       document.getElementById('ap-mname').value,
      dateOfBirth:      document.getElementById('ap-dob').value,
      placeOfBirth:     document.getElementById('ap-pob').value,
      gender:           'Male',
      email:            document.getElementById('ap-email').value,
      contactNumber:    document.getElementById('ap-contact').value,
      nationality:      document.getElementById('ap-nationality').value,
      religion:         document.getElementById('ap-religion').value,
      seminaryLevel:    document.getElementById('ap-level').value,
      address:          document.getElementById('ap-address').value,
      fatherName:       document.getElementById('ap-father').value,
      fatherOccupation: document.getElementById('ap-father-occ').value,
      motherName:       document.getElementById('ap-mother').value,
      motherOccupation: document.getElementById('ap-mother-occ').value,
      guardianName:     document.getElementById('ap-guardian').value,
      guardianContact:  document.getElementById('ap-guardian-contact').value,
      lastSchoolAttended: document.getElementById('ap-school').value,
      lastSchoolYear:   document.getElementById('ap-school-year').value,
      lastYearLevel:    document.getElementById('ap-year-level').value,
      appliedProgram:   { programId: document.getElementById('ap-program').value }
    });
    toast('Applicant saved successfully');
    closeModal('modal-applicant'); loadApplicants();
  } catch (e) { toast(e.message, 'error'); }
}

function closeStudentModal() {
  document.getElementById('st-modal').classList.remove('editing');
  document.getElementById('st-modal-title').textContent = 'Student Record';
  document.getElementById('st-modal-sub').textContent   = 'Fill in the student\'s details';
  document.getElementById('st-save-btn').textContent    = 'Save Record';
  closeModal('modal-student');
}

function openAddStudentModal() {
  _currentStudent   = null;
  _currentStudentId = null;
  document.getElementById('st-modal').classList.remove('editing');
  document.getElementById('st-modal-title').textContent = 'Student Record';
  document.getElementById('st-modal-sub').textContent   = 'Fill in the student\'s details';
  document.getElementById('st-save-btn').textContent    = 'Save Record';
  openModal('modal-student');
}

function openEditStudentModal() {
  if (!_currentStudent) return;
  const s = _currentStudent;
  // Activate editing style
  document.getElementById('st-modal').classList.add('editing');
  document.getElementById('st-modal-title').textContent = `${s.firstName} ${s.lastName}`;
  document.getElementById('st-modal-sub').textContent   = `${s.studentId} · ${s.program?.programCode || '—'}`;
  document.getElementById('st-save-btn').textContent    = 'Save Changes';
  // Reset to first tab
  switchTab(document.querySelector('#modal-student .tab'), 'st-tab-personal', stTabs);
  // Personal
  document.getElementById('st-fname').value      = s.firstName || '';
  document.getElementById('st-lname').value      = s.lastName || '';
  document.getElementById('st-mname').value      = s.middleName || '';
  document.getElementById('st-dob').value        = s.dateOfBirth || '';
  document.getElementById('st-nationality').value= s.nationality || '';
  document.getElementById('st-contact').value    = s.contactNumber || '';
  document.getElementById('st-email').value      = s.email || '';
  document.getElementById('st-address').value    = s.address || '';
  document.getElementById('st-blood').value      = s.bloodType || '';
  document.getElementById('st-medical').value    = s.medicalConditions || '';
  // Family
  document.getElementById('st-father').value     = s.fatherName || '';
  document.getElementById('st-father-occ').value = s.fatherOccupation || '';
  document.getElementById('st-mother').value     = s.motherName || '';
  document.getElementById('st-mother-occ').value = s.motherOccupation || '';
  document.getElementById('st-guardian').value   = s.guardianName || '';
  document.getElementById('st-guardian-contact').value = s.guardianContact || '';
  // Religious
  document.getElementById('st-religion').value   = s.religion || 'Roman Catholic';
  document.getElementById('st-diocese').value    = s.diocese || '';
  document.getElementById('st-priest').value     = s.parishPriest || '';
  document.getElementById('st-baptism').value    = s.baptismDate || '';
  document.getElementById('st-baptism-church').value = s.baptismChurch || '';
  document.getElementById('st-confirm').value    = s.confirmationDate || '';
  // Academic
  document.getElementById('st-sem-level').value  = s.seminaryLevel || 'College';
  document.getElementById('st-program').value    = s.program?.programId || '';
  document.getElementById('st-year').value       = s.currentYearLevel || '1';
  document.getElementById('st-status').value     = s.currentStatus || 'Active';
  openModal('modal-student');
}

async function saveStudent() {
  if (!validateRequired([
    {id:'st-fname',     label:'First Name'},
    {id:'st-lname',     label:'Last Name'},
    {id:'st-dob',       label:'Date of Birth'},
    {id:'st-email',     label:'Email'},
    {id:'st-sem-level', label:'Seminary Level'},
    {id:'st-program',   label:'Program'},
  ])) return;
  const isEditing = !!_currentStudentId && !!_currentStudent;
  const payload = {
    firstName:        document.getElementById('st-fname').value,
    lastName:         document.getElementById('st-lname').value,
    middleName:       document.getElementById('st-mname').value,
    dateOfBirth:      document.getElementById('st-dob').value,
    gender:           'Male',
    nationality:      document.getElementById('st-nationality').value,
    contactNumber:    document.getElementById('st-contact').value,
    email:            document.getElementById('st-email').value,
    address:          document.getElementById('st-address').value,
    bloodType:        document.getElementById('st-blood').value,
    medicalConditions:document.getElementById('st-medical').value,
    fatherName:       document.getElementById('st-father').value,
    fatherOccupation: document.getElementById('st-father-occ').value,
    motherName:       document.getElementById('st-mother').value,
    motherOccupation: document.getElementById('st-mother-occ').value,
    guardianName:     document.getElementById('st-guardian').value,
    guardianContact:  document.getElementById('st-guardian-contact').value,
    religion:         document.getElementById('st-religion').value,
    diocese:          document.getElementById('st-diocese').value,
    parishPriest:     document.getElementById('st-priest').value,
    baptismDate:      document.getElementById('st-baptism').value || null,
    baptismChurch:    document.getElementById('st-baptism-church').value,
    confirmationDate: document.getElementById('st-confirm').value || null,
    seminaryLevel:    document.getElementById('st-sem-level').value,
    program:          { programId: document.getElementById('st-program').value },
    currentYearLevel: parseInt(document.getElementById('st-year').value),
    currentStatus:    document.getElementById('st-status').value,
  };
  try {
    if (isEditing) {
      const id = _currentStudentId;
      await api(`/api/students/${id}`, 'PUT', payload);
      toast('Student record updated');
      closeStudentModal();
      loadStudents();
      viewStudent(id);
      _gradeStudents = []; // invalidate grade search cache so updated name is picked up
    } else {
      await api('/api/students', 'POST', payload);
      toast('Student record saved');
      closeStudentModal(); loadStudents();
    }
  } catch (e) { toast(e.message, 'error'); }
}

// ── Enroll Modal — Searchable Student Dropdown ───────────────
function renderEnrStudentOptions(query) {
  const list = document.getElementById('enr-student-list');
  if (!list) return;
  const q = (query || '').toLowerCase();
  const filtered = _enrStudents.filter(s =>
    `${s.firstName} ${s.lastName} ${s.studentId}`.toLowerCase().includes(q)
  );
  list.innerHTML = '';
  if (!filtered.length) {
    list.innerHTML = '<div class="enr-student-option no-match">No students found</div>';
    return;
  }
  filtered.forEach(s => {
    const label = `${s.firstName} ${s.lastName} (${s.studentId})`;
    const item = document.createElement('div');
    item.className = 'enr-student-option';
    item.textContent = label;
    item.setAttribute('role', 'option');
    item.addEventListener('mousedown', e => {
      e.preventDefault(); // keep focus on input until selection confirmed
      document.getElementById('enr-student').value = s.studentId;
      document.getElementById('enr-student-search').value = label;
      document.getElementById('enr-program').value = s.program?.programId || '';
      document.getElementById('enr-program-display').textContent = s.program?.programName || s.program?.programId || 'Unknown program';
      closeEnrStudentDropdown();
      autoFillEnrYearLevel(s.studentId);
    });
    list.appendChild(item);
  });
}
function openEnrStudentDropdown() {
  document.getElementById('enr-student-list').classList.add('open');
}
function closeEnrStudentDropdown() {
  document.getElementById('enr-student-list').classList.remove('open');
}

async function autoFillEnrYearLevel(studentId) {
  const yearEl      = document.getElementById('enr-year');
  const hintEl      = document.getElementById('enr-year-hint');
  const confirmBtn  = document.getElementById('btn-confirm-enroll');
  if (hintEl) { hintEl.textContent = 'Detecting…'; hintEl.style.color = 'var(--gray-500,#6b7280)'; }
  if (confirmBtn) confirmBtn.disabled = false;
  clearEnrollError();
  try {
    const data = await api(`/api/enrollment/suggested-year-level?studentId=${encodeURIComponent(studentId)}`);
    if (data.alreadyEnrolled) {
      showEnrollError('This student is already enrolled in the active semester.');
      if (confirmBtn) confirmBtn.disabled = true;
      if (hintEl) hintEl.textContent = '';
      return;
    }
    yearEl.value = String(data.suggestedYearLevel);
    if (hintEl) {
      hintEl.textContent = `Auto-detected: ${data.reason}`;
      hintEl.style.color = 'var(--info,#2563eb)';
    }
    filterEnrSections('std');
  } catch (_) {
    if (hintEl) hintEl.textContent = '';
  }
}

let _enrollTab = 'applicant';

function switchEnrollTab(tab) {
  _enrollTab = tab;
  clearEnrollError();
  const hintEl = document.getElementById('enr-year-hint');
  if (hintEl) hintEl.textContent = '';
  const confirmBtn = document.getElementById('btn-confirm-enroll');
  if (confirmBtn) confirmBtn.disabled = false;
  document.getElementById('enr-panel-applicant').style.display = tab === 'applicant' ? '' : 'none';
  document.getElementById('enr-panel-student').style.display   = tab === 'student'   ? '' : 'none';
  document.getElementById('enr-tab-applicant').style.background = tab === 'applicant' ? 'var(--navy)' : 'white';
  document.getElementById('enr-tab-applicant').style.color      = tab === 'applicant' ? 'white' : 'var(--gray-600)';
  document.getElementById('enr-tab-student').style.background  = tab === 'student'   ? 'var(--navy)' : 'white';
  document.getElementById('enr-tab-student').style.color       = tab === 'student'   ? 'white' : 'var(--gray-600)';
}

function filterEnrSections(panel) {
  const yearEl    = document.getElementById(panel === 'app' ? 'enr-app-year'    : 'enr-year');
  const programEl = document.getElementById(panel === 'app' ? 'enr-app-program' : 'enr-program');
  const selectEl  = document.getElementById(panel === 'app' ? 'enr-app-section' : 'enr-section');
  if (!yearEl || !programEl || !selectEl) return;
  const year      = parseInt(yearEl.value);
  const programId = programEl.value;
  const filtered  = _allEnrSections.filter(s => s.yearLevel === year && s.program?.programId === programId);
  selectEl.innerHTML = '<option value="">No section</option>';
  filtered.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s.sectionId;
    opt.textContent = s.sectionName;
    selectEl.appendChild(opt);
  });
}

async function openEnrollModal() {
  _enrollTab = 'applicant';
  switchEnrollTab('applicant');
  clearEnrollError();

  // Reset fields
  const searchEl = document.getElementById('enr-student-search');
  const hiddenEl = document.getElementById('enr-student');
  if (searchEl) searchEl.value = '';
  if (hiddenEl) hiddenEl.value = '';

  // Reset applicant panel
  const appApplicantSel = document.getElementById('enr-applicant');
  if (appApplicantSel) appApplicantSel.value = '';
  const appProgramDisplay = document.getElementById('enr-app-program-display');
  if (appProgramDisplay) appProgramDisplay.textContent = 'Select an applicant first';
  const appProgramHidden = document.getElementById('enr-app-program');
  if (appProgramHidden) appProgramHidden.value = '';
  const appYearSel = document.getElementById('enr-app-year');
  if (appYearSel) appYearSel.value = '1';
  const appSectionSel = document.getElementById('enr-app-section');
  if (appSectionSel) appSectionSel.innerHTML = '<option value="">No section</option>';

  // Reset student panel
  const programDisplay = document.getElementById('enr-program-display');
  if (programDisplay) programDisplay.textContent = 'Select a student first';
  const programHidden = document.getElementById('enr-program');
  if (programHidden) programHidden.value = '';
  const stdYearSel = document.getElementById('enr-year');
  if (stdYearSel) stdYearSel.value = '1';
  const stdSectionSel = document.getElementById('enr-section');
  if (stdSectionSel) stdSectionSel.innerHTML = '<option value="">No section</option>';

  _enrStudents = [];

  // Load admitted applicants
  try {
    const applicants = await api('/api/enrollment/admitted-applicants');
    const sel = document.getElementById('enr-applicant');
    sel.innerHTML = '<option value="">Select admitted applicant…</option>';
    applicants.forEach(a => {
      const opt = document.createElement('option');
      opt.value = a.applicantId;
      opt.textContent = `${a.name} — ${a.programName}`;
      opt.dataset.programId   = a.programId;
      opt.dataset.programName = a.programName;
      sel.appendChild(opt);
    });
    if (applicants.length === 0) {
      sel.innerHTML = '<option value="">No admitted applicants pending enrollment</option>';
    }
  } catch (_) {}

  // Load existing students
  try {
    const students = await api('/api/students?status=Active');
    _enrStudents = students;
    renderEnrStudentOptions('');
  } catch (_) {}

  // Show active semester label (read-only)
  const semLabel = document.getElementById('enr-active-sem-label');
  if (semLabel) semLabel.textContent = SMS.activeSemester?.semesterLabel || '—';

  // Load sections for the active semester only; filtering by year level is done client-side
  try {
    const activeSemId = SMS.activeSemester?.semesterId || '';
    _allEnrSections = await api(`/api/sections?semester=${activeSemId}`);
    filterEnrSections('app');
    filterEnrSections('std');
  } catch (_) {}

  openModal('modal-enroll');
}

function onEnrApplicantChange() {
  const sel = document.getElementById('enr-applicant');
  const opt = sel.options[sel.selectedIndex];
  const programId   = opt?.dataset?.programId   || '';
  const programName = opt?.dataset?.programName || '';
  document.getElementById('enr-app-program').value = programId;
  document.getElementById('enr-app-program-display').textContent = programName || 'Select an applicant first';
  filterEnrSections('app');
}

async function saveEnrollment() {
  if (_enrollTab === 'applicant') {
    const applicantId = document.getElementById('enr-applicant').value;
    if (!applicantId) { toast('Please select an admitted applicant', 'error'); return; }
    if (!validateRequired([
      {id:'enr-app-year',    label:'Year Level'},
      {id:'enr-app-section', label:'Section'},
    ])) return;
  } else {
    if (!validateRequired([
      {id:'enr-student', label:'Student'},
      {id:'enr-year',    label:'Year Level'},
      {id:'enr-section', label:'Section'},
    ])) return;
  }
  if (!SMS.activeSemester) { showEnrollError('No active semester is set. Please set an active semester first.'); return; }
  const activeSemId = SMS.activeSemester.semesterId;
  const btn = document.getElementById('btn-confirm-enroll');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Enrolling…'; }
  await new Promise(r => requestAnimationFrame(r));
  try {
    if (_enrollTab === 'applicant') {
      const applicantId = document.getElementById('enr-applicant').value;
      const result = await api('/api/enrollment', 'POST', {
        applicantId,
        yearLevel:  parseInt(document.getElementById('enr-app-year').value),
        sectionId:  document.getElementById('enr-app-section').value || null,
        semesterId: activeSemId,
      });
      closeModal('modal-enroll');
      // Show credentials — only available now
      document.getElementById('cred-username').textContent = result.studentId;
      document.getElementById('cred-password').textContent = result.temporaryPassword;
      const emailNote = document.getElementById('cred-email-note');
      if (emailNote) emailNote.textContent = result.emailSent
        ? 'Credentials have also been sent to the student\'s email.'
        : 'No email was sent — email address not on file. Give these credentials to the student directly.';
      openModal('modal-credentials');
      loadEnrollment();
    } else {
      await api('/api/enrollment', 'POST', {
        studentId:  document.getElementById('enr-student').value,
        programId:  document.getElementById('enr-program').value,
        yearLevel:  parseInt(document.getElementById('enr-year').value),
        sectionId:  document.getElementById('enr-section').value || null,
        semesterId: activeSemId,
      });
      toast('Student enrolled successfully');
      closeModal('modal-enroll'); loadEnrollment();
    }
  } catch (e) { showEnrollError(e.message); }
  finally { if (btn) { btn.disabled = false; btn.textContent = 'Confirm Enrollment'; } }
}

function showEnrollError(msg) {
  const banner = document.getElementById('enroll-error-banner');
  document.getElementById('enroll-error-text').textContent = msg;
  banner.style.display = 'flex';
  banner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function clearEnrollError() {
  const banner = document.getElementById('enroll-error-banner');
  banner.style.display = 'none';
  document.getElementById('enroll-error-text').textContent = '';
}

function editGrade(id, student, course, mtCS, mtExam, fnCS, fnExam, mtGrade, fnGrade, status, remarks) {
  document.getElementById('grade-id').value    = id;
  document.getElementById('gr-student').value  = student;
  document.getElementById('gr-course').value   = course;
  document.getElementById('gr-mt-cs').value    = mtCS   || '';
  document.getElementById('gr-mt-exam').value  = mtExam || '';
  document.getElementById('gr-fn-cs').value    = fnCS   || '';
  document.getElementById('gr-fn-exam').value  = fnExam || '';
  document.getElementById('gr-mt-grade').textContent = mtGrade || '—';
  document.getElementById('gr-fn-grade').textContent = fnGrade || '—';
  document.getElementById('gr-status').value   = status;
  document.getElementById('gr-remarks').value  = remarks;
  _updateGradeStatusBadge(status);
  _setGradeInputsDisabled(status === 'Incomplete' || status === 'Dropped');
  recomputeGradeModal();
  openModal('modal-grade');
}

function toggleGradeOverride(newStatus) {
  const statusEl = document.getElementById('gr-status');
  const current  = statusEl.value;
  // Toggle off if already active
  const resolved = current === newStatus ? 'NotYetGraded' : newStatus;
  statusEl.value = resolved;
  _updateGradeStatusBadge(resolved);
  _setGradeInputsDisabled(resolved === 'Incomplete' || resolved === 'Dropped');
  if (resolved === 'Incomplete' || resolved === 'Dropped') {
    ['gr-mt-cs','gr-mt-exam','gr-fn-cs','gr-fn-exam'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('gr-mt-grade').textContent = '—';
    document.getElementById('gr-fn-grade').textContent = '—';
    document.getElementById('gr-final-rating').textContent = '—';
    document.getElementById('gr-final-rating').className = 'grade-final-val';
  } else {
    recomputeGradeModal();
  }
}

function _setGradeInputsDisabled(disabled) {
  ['gr-mt-cs','gr-mt-exam','gr-fn-cs','gr-fn-exam'].forEach(id => {
    document.getElementById(id).disabled = disabled;
  });
  document.getElementById('btn-mark-incomplete').classList.toggle('btn-warning', document.getElementById('gr-status').value === 'Incomplete');
  document.getElementById('btn-mark-dropped').classList.toggle('btn-danger', document.getElementById('gr-status').value === 'Dropped');
}

function _updateGradeStatusBadge(status) {
  const badge = document.getElementById('gr-status-badge');
  if (!badge) return;
  const map = {
    Passed: ['Passed', 'success'], Failed: ['Failed', 'danger'],
    Incomplete: ['Incomplete', 'warn'], Dropped: ['Dropped', 'danger'],
    NotYetGraded: ['Not Yet Graded', 'gray']
  };
  const [label, type] = map[status] || ['Unknown', 'gray'];
  const colors = { success:'#16a34a', danger:'#dc2626', warn:'#d97706', gray:'#6b7280' };
  badge.textContent = label;
  badge.style.color = colors[type] || colors.gray;
}

// SECURITY (A03): Reads grade data from data-* attributes instead of inline onclick strings.
// This prevents XSS via student names or remarks embedded in event handler strings.
function editGradeFromRow(btn) {
  editGrade(
    btn.dataset.gradeId,
    btn.dataset.student,
    btn.dataset.course,
    btn.dataset.mtCs    || '',
    btn.dataset.mtExam  || '',
    btn.dataset.fnCs    || '',
    btn.dataset.fnExam  || '',
    btn.dataset.mtGrade || '',
    btn.dataset.fnGrade || '',
    btn.dataset.status,
    btn.dataset.remarks
  );
}

async function saveGrade() {
  const id = document.getElementById('grade-id').value;
  try {
    await api(`/api/grades/${id}`, 'PUT', {
      midtermClassStanding: document.getElementById('gr-mt-cs').value   || null,
      midtermExam:          document.getElementById('gr-mt-exam').value || null,
      finalClassStanding:   document.getElementById('gr-fn-cs').value   || null,
      finalExam:            document.getElementById('gr-fn-exam').value || null,
      gradeStatus:          document.getElementById('gr-status').value,
      remarks:              document.getElementById('gr-remarks').value,
    });
    toast('Grade saved'); closeModal('modal-grade'); loadGrades();
  } catch (e) { toast(e.message, 'error'); }
}

// Live computation — mirrors the Java formula so the registrar sees the result before saving
function recomputeGradeModal() {
  const mtCS   = parseFloat(document.getElementById('gr-mt-cs').value);
  const mtExam = parseFloat(document.getElementById('gr-mt-exam').value);
  const fnCS   = parseFloat(document.getElementById('gr-fn-cs').value);
  const fnExam = parseFloat(document.getElementById('gr-fn-exam').value);

  const mtGrade = (!isNaN(mtCS) && !isNaN(mtExam)) ? (mtCS * 0.60 + mtExam * 0.40) : null;
  const fnGrade = (!isNaN(fnCS) && !isNaN(fnExam)) ? (fnCS * 0.60 + fnExam * 0.40) : null;
  const rating  = (mtGrade !== null && fnGrade !== null) ? ((mtGrade + fnGrade) / 2) : null;

  const fmt = v => v !== null ? v.toFixed(2) : '—';
  document.getElementById('gr-mt-grade').textContent  = fmt(mtGrade);
  document.getElementById('gr-fn-grade').textContent  = fmt(fnGrade);

  const ratingEl = document.getElementById('gr-final-rating');
  ratingEl.textContent = fmt(rating);
  ratingEl.className = 'grade-final-val' + (rating !== null ? (rating <= 3.0 ? ' grade-pass' : ' grade-fail') : '');

  // Auto-set status based on computed rating (unless manually overridden)
  const statusEl = document.getElementById('gr-status');
  if (statusEl.value !== 'Incomplete' && statusEl.value !== 'Dropped') {
    const computed = rating !== null ? (rating <= 3.0 ? 'Passed' : 'Failed') : 'NotYetGraded';
    statusEl.value = computed;
    _updateGradeStatusBadge(computed);
  }
}

function openCourseModal(courseIdOrNull) {
  const c         = courseIdOrNull ? _courseMap[courseIdOrNull] : null;
  const programId = c?.program?.programId ||
    (document.getElementById('curr-theo')?.style.display !== 'none' ? 'PRG-1002' : 'PRG-1001');

  document.getElementById('co-id').value      = c?.courseId  || '';
  document.getElementById('co-code').value    = c?.courseCode || '';
  document.getElementById('co-name').value    = c?.courseName || '';
  document.getElementById('co-units').value   = c?.units      || '';
  document.getElementById('co-program').value = programId;
  document.getElementById('co-year').value    = c?.yearLevel      || '1';
  document.getElementById('co-sem').value     = c?.semesterNumber || '1';
  document.getElementById('co-modal-title').textContent = c ? 'Edit Course' : 'Add Course';

  // Populate the prerequisite dropdown with all other courses in this curriculum
  const allCourses   = _currCourses[programId] || [];
  const prereqSelect = document.getElementById('co-prereq');
  prereqSelect.innerHTML = '<option value="">None</option>' +
    allCourses
      .filter(x => x.courseId !== courseIdOrNull)
      .map(x => `<option value="${escHtml(x.courseId)}">${escHtml(x.courseCode)} — ${escHtml(x.courseName)}</option>`)
      .join('');

  // Re-select the existing prerequisite when editing
  const existingPrereq = c?.prerequisites?.[0]?.prerequisiteCourse?.courseId || '';
  prereqSelect.value = existingPrereq;

  openModal('modal-course');
}

async function saveCourse() {
  if (!validateRequired([
    {id:'co-code',    label:'Course Code'},
    {id:'co-name',    label:'Course Name'},
    {id:'co-units',   label:'Units'},
    {id:'co-program', label:'Program'},
    {id:'co-year',    label:'Year Level'},
    {id:'co-sem',     label:'Semester Number'},
  ])) return;

  const id         = document.getElementById('co-id').value;
  const programId  = document.getElementById('co-program').value;
  const courseCode = document.getElementById('co-code').value.trim();
  const unitsRaw   = document.getElementById('co-units').value;

  // Gap 2: course code must be ≤30 chars and contain no leading/trailing spaces
  if (courseCode.length > 30) {
    toast('Course Code must be 30 characters or fewer', 'error');
    document.getElementById('co-code').focus();
    return;
  }

  // Gap 1: units must be a whole positive number
  const units = Number(unitsRaw);
  if (!Number.isInteger(units) || units < 1) {
    toast('Units must be a whole number of at least 1', 'error');
    document.getElementById('co-units').focus();
    return;
  }

  // Gap 4: duplicate course code check within the same curriculum (only on Add)
  if (!id) {
    const curriculumId = _selectedCurriculum[programId];
    const existing = (_currCourses[programId] || []).find(
      x => x.courseCode.trim().toLowerCase() === courseCode.toLowerCase()
    );
    if (existing) {
      toast(`Course code "${courseCode}" already exists in this curriculum`, 'error');
      document.getElementById('co-code').focus();
      return;
    }
  }

  const curriculumId = _selectedCurriculum[programId];
  const payload = {
    courseCode,
    courseName:     document.getElementById('co-name').value.trim(),
    units,
    program:        { programId },
    curriculum:     curriculumId ? { curriculumId } : null,
    yearLevel:      parseInt(document.getElementById('co-year').value),
    semesterNumber: parseInt(document.getElementById('co-sem').value),
    isActive:       true,
  };
  const prereqCourseId = document.getElementById('co-prereq').value;
  try {
    if (id) {
      await api(`/api/curriculum/courses/${id}`, 'PUT', payload);
      // Delete all existing prereqs for this course, then add the new one if selected
      const existingPrereqs = _courseMap[id]?.prerequisites || [];
      for (const p of existingPrereqs) {
        await api(`/api/curriculum/prerequisites/${p.index}`, 'DELETE');
      }
      if (prereqCourseId) {
        await api('/api/curriculum/prerequisites', 'POST', {
          course:              { courseId: id },
          prerequisiteCourse:  { courseId: prereqCourseId },
        });
      }
      toast('Course updated');
    } else {
      const saved = await api('/api/curriculum/courses', 'POST', payload);
      if (prereqCourseId) {
        await api('/api/curriculum/prerequisites', 'POST', {
          course:              { courseId: saved.courseId },
          prerequisiteCourse:  { courseId: prereqCourseId },
        });
      }
      toast('Course added');
    }
    closeModal('modal-course');
    loadCurriculum(programId);
  } catch (e) { toast(e.message, 'error'); }
}

function deleteCourse(courseId) {
  document.getElementById('del-course-id').value = courseId;
  openModal('modal-course-delete');
}

async function confirmDeleteCourse() {
  const id = document.getElementById('del-course-id').value;
  const programId = _courseMap[id]?.program?.programId;
  try {
    await api(`/api/curriculum/courses/${id}`, 'DELETE');
    toast('Course deleted');
    closeModal('modal-course-delete');
    await loadCurriculum(programId || 'PRG-1001');
  } catch (e) { toast(e.message, 'error'); }
}

function openNewCurriculumModal() {
  // Pre-select whichever program tab is currently visible
  const activeProgramId = document.getElementById('curr-philo').style.display !== 'none'
    ? 'PRG-1001' : 'PRG-1002';
  const ncProgram = document.getElementById('nc-program');
  ncProgram.value = activeProgramId;
  _populateCopyFromDropdown(activeProgramId);
  ncProgram.onchange = function () { _populateCopyFromDropdown(this.value); };
  document.getElementById('nc-label').value = '';
  openModal('modal-new-curriculum');
}

function _populateCopyFromDropdown(programId) {
  const sel = document.getElementById('nc-copy-from');
  sel.innerHTML = '<option value="">Start empty</option>' +
    (_curricula[programId] || []).map(c =>
      `<option value="${escHtml(c.curriculumId)}">${escHtml(c.label)}</option>`
    ).join('');
}

async function saveNewCurriculum() {
  const programId  = document.getElementById('nc-program').value;
  const label      = document.getElementById('nc-label').value.trim();
  const copyFromId = document.getElementById('nc-copy-from').value;
  if (!label) { toast('Curriculum label is required', 'error'); return; }
  try {
    await api('/api/curriculum/curricula', 'POST', {
      programId,
      label,
      copyFromCurriculumId: copyFromId || null,
    });
    toast('Curriculum created as draft. Add courses then click "Set Active".');
    closeModal('modal-new-curriculum');
    await loadCurricula(programId);
  } catch (e) { toast(e.message, 'error'); }
}

async function openSectionModal(section) {
  try {
    const sems = await api('/api/school-years/semesters');
    populateSelect('sec-semester', sems, 'semesterId', s => s.semesterLabel, '');
  } catch (_) {}
  document.getElementById('sec-id').value       = section?.sectionId  || '';
  document.getElementById('sec-code').value     = section?.sectionCode || '';
  document.getElementById('sec-name').value     = section?.sectionName || '';
  document.getElementById('sec-program').value  = section?.program?.programId || 'PRG-1001';
  document.getElementById('sec-year').value     = section?.yearLevel  || '1';
  document.getElementById('sec-capacity').value = section?.capacity   || '40';
  if (section?.semester?.semesterId) document.getElementById('sec-semester').value = section.semester.semesterId;
  document.getElementById('sec-modal-title').textContent = section ? 'Edit Section' : 'Add Section';
  openModal('modal-section');
}

async function saveSection() {
  if (!validateRequired([
    {id:'sec-code',     label:'Section Code'},
    {id:'sec-name',     label:'Section Name'},
    {id:'sec-program',  label:'Program'},
    {id:'sec-year',     label:'Year Level'},
    {id:'sec-semester', label:'Semester'},
  ])) return;
  const id = document.getElementById('sec-id').value;
  const payload = {
    sectionCode: document.getElementById('sec-code').value,
    sectionName: document.getElementById('sec-name').value,
    program:     { programId: document.getElementById('sec-program').value },
    yearLevel:   parseInt(document.getElementById('sec-year').value),
    semester:    { semesterId: document.getElementById('sec-semester').value },
    capacity:    parseInt(document.getElementById('sec-capacity').value),
  };
  try {
    if (id) {
      await api(`/api/sections/${id}`, 'PUT', payload);
      toast('Section updated');
    } else {
      await api('/api/sections', 'POST', payload);
      toast('Section added');
    }
    closeModal('modal-section'); loadSections();
  } catch (e) { toast(e.message, 'error'); }
}

function deleteSection(sectionId) {
  document.getElementById('del-section-id').value = sectionId;
  openModal('modal-section-delete');
}

async function confirmDeleteSection() {
  const id = document.getElementById('del-section-id').value;
  try {
    await api(`/api/sections/${id}`, 'DELETE');
    toast('Section deleted');
    closeModal('modal-section-delete');
    loadSections();
  } catch (e) { toast(e.message, 'error'); }
}

function closeSchedModal() {
  document.getElementById('sch-modal').classList.remove('editing');
  document.getElementById('sch-modal-title').textContent = 'Add Schedule';
  document.getElementById('sch-save-btn').textContent    = 'Save Schedule';
  document.getElementById('sch-id').value                = '';
  document.getElementById('sched-conflict-alert').style.display = 'none';
  closeModal('modal-schedule');
}

async function _populateSchedSelects() {
  const activeSemId = SMS.activeSemester?.semesterId || '';
  const [sections, courses, instructors, rooms] = await Promise.all([
    api(`/api/sections?semester=${activeSemId}`), api('/api/curriculum/courses'),
    api('/api/sections/instructors'), api('/api/sections/rooms')
  ]);
  populateSelect('sch-section',    sections,    'sectionId',    s => s.sectionName, 'Select…');
  populateSelect('sch-course',     courses,     'courseId',     c => `${c.courseCode} – ${c.courseName}`, 'Select…');
  populateSelect('sch-instructor', instructors, 'instructorId', i => `${i.firstName} ${i.lastName}`, 'Select…');
  populateSelect('sch-room',       rooms,       'roomId',       r => r.roomName, 'Select…');
}

async function openSchedModal() {
  document.getElementById('sch-modal').classList.remove('editing');
  document.getElementById('sch-modal-title').textContent = 'Add Schedule';
  document.getElementById('sch-save-btn').textContent    = 'Save Schedule';
  document.getElementById('sch-id').value                = '';
  try { await _populateSchedSelects(); } catch (_) {}
  openModal('modal-schedule');
}

async function openEditSchedModal(scheduleId) {
  const s = _scheduleCache[scheduleId];
  if (!s) { toast('Schedule data not found', 'error'); return; }
  try { await _populateSchedSelects(); } catch (_) {}
  document.getElementById('sch-modal').classList.add('editing');
  document.getElementById('sch-modal-title').textContent = `${s.course?.courseCode} – ${s.section?.sectionCode}`;
  document.getElementById('sch-save-btn').textContent    = 'Save Changes';
  document.getElementById('sch-id').value                = s.scheduleId;
  document.getElementById('sch-section').value           = s.section?.sectionId       || '';
  document.getElementById('sch-course').value            = s.course?.courseId         || '';
  document.getElementById('sch-instructor').value        = s.instructor?.instructorId || '';
  document.getElementById('sch-room').value              = s.room?.roomId             || '';
  document.getElementById('sch-day').value               = s.dayOfWeek                || '';
  document.getElementById('sch-start').value             = s.timeStart                || '';
  document.getElementById('sch-end').value               = s.timeEnd                  || '';
  document.getElementById('sched-conflict-alert').style.display = 'none';
  openModal('modal-schedule');
}

async function saveSchedule() {
  if (!validateRequired([
    {id:'sch-section', label:'Section'},
    {id:'sch-course',  label:'Course'},
    {id:'sch-day',     label:'Day of Week'},
    {id:'sch-start',   label:'Start Time'},
    {id:'sch-end',     label:'End Time'},
  ])) return;
  const alertEl = document.getElementById('sched-conflict-alert');
  const schedId = document.getElementById('sch-id').value;
  const payload = {
    sectionId:    document.getElementById('sch-section').value,
    courseId:     document.getElementById('sch-course').value,
    instructorId: document.getElementById('sch-instructor').value,
    roomId:       document.getElementById('sch-room').value,
    dayOfWeek:    document.getElementById('sch-day').value,
    timeStart:    document.getElementById('sch-start').value,
    timeEnd:      document.getElementById('sch-end').value,
  };
  try {
    if (schedId) {
      await api(`/api/schedule/${schedId}`, 'PUT', payload);
      toast('Schedule updated');
    } else {
      await api('/api/schedule', 'POST', payload);
      toast('Schedule saved');
    }
    alertEl.style.display = 'none';
    closeSchedModal(); loadScheduleGrid();
  } catch (e) {
    alertEl.textContent = e.message; alertEl.style.display = 'block';
  }
}

let _deleteSchedId = null;
function confirmDeleteSchedule(id) {
  _deleteSchedId = id;
  openModal('modal-sched-delete');
}

async function doDeleteSchedule() {
  try {
    await api(`/api/schedule/${_deleteSchedId}`, 'DELETE');
    toast('Schedule deleted');
    closeModal('modal-sched-delete'); loadScheduleGrid();
  } catch (e) { toast(e.message, 'error'); }
}

function clearInstructorForm() {
  ['ins-id','ins-fname','ins-lname','ins-mname','ins-email','ins-contact','ins-spec'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('ins-modal').classList.remove('editing');
  document.getElementById('ins-modal-title').textContent = 'Add Instructor';
}

function editInstructor(instructorId) {
  const i = _instructorCache[instructorId];
  if (!i) return;
  document.getElementById('ins-id').value      = i.instructorId;
  document.getElementById('ins-fname').value   = i.firstName   || '';
  document.getElementById('ins-lname').value   = i.lastName    || '';
  document.getElementById('ins-mname').value   = i.middleName  || '';
  document.getElementById('ins-email').value   = i.email       || '';
  document.getElementById('ins-contact').value = i.contactNumber || '';
  document.getElementById('ins-spec').value    = i.specialization || '';
  document.getElementById('ins-modal').classList.add('editing');
  document.getElementById('ins-modal-title').textContent = 'Edit Instructor';
  openModal('modal-instructor');
}

async function saveInstructor() {
  if (!validateRequired([
    {id:'ins-fname', label:'First Name'},
    {id:'ins-lname', label:'Last Name'},
  ])) return;
  const existingId = document.getElementById('ins-id').value;
  const payload = {
    firstName:       document.getElementById('ins-fname').value,
    lastName:        document.getElementById('ins-lname').value,
    middleName:      document.getElementById('ins-mname').value,
    email:           document.getElementById('ins-email').value,
    contactNumber:   document.getElementById('ins-contact').value,
    specialization:  document.getElementById('ins-spec').value,
  };
  try {
    if (existingId) {
      await api(`/api/sections/instructors/${existingId}`, 'PUT', payload);
      toast('Instructor updated');
    } else {
      await api('/api/sections/instructors', 'POST', payload);
      toast('Instructor saved');
    }
    closeModal('modal-instructor'); loadInstructors();
  } catch (e) { toast(e.message, 'error'); }
}

function clearRoomForm() {
  ['rm-id','rm-name','rm-building','rm-capacity'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('rm-modal-title').textContent = 'Add Room';
}

function editRoom(roomId) {
  const r = _roomCache[roomId];
  if (!r) return;
  document.getElementById('rm-id').value       = r.roomId;
  document.getElementById('rm-name').value     = r.roomName    || '';
  document.getElementById('rm-building').value = r.building    || '';
  document.getElementById('rm-capacity').value = r.capacity    || '';
  document.getElementById('rm-modal-title').textContent = 'Edit Room';
  openModal('modal-room');
}

function deleteInstructor(instructorId) {
  document.getElementById('del-instructor-id').value = instructorId;
  openModal('modal-instructor-delete');
}

async function confirmDeleteInstructor() {
  const id = document.getElementById('del-instructor-id').value;
  try {
    await api(`/api/sections/instructors/${id}`, 'DELETE');
    toast('Instructor deleted');
    closeModal('modal-instructor-delete');
    loadInstructors();
  } catch (e) { toast(e.message, 'error'); }
}

function deleteRoom(roomId) {
  document.getElementById('del-room-id').value = roomId;
  openModal('modal-room-delete');
}

async function confirmDeleteRoom() {
  const id = document.getElementById('del-room-id').value;
  try {
    await api(`/api/sections/rooms/${id}`, 'DELETE');
    toast('Room deleted');
    closeModal('modal-room-delete');
    loadRooms();
  } catch (e) { toast(e.message, 'error'); }
}

async function saveRoom() {
  if (!validateRequired([
    {id:'rm-name', label:'Room Name'},
  ])) return;
  const existingId = document.getElementById('rm-id').value;
  const payload = {
    roomName:  document.getElementById('rm-name').value,
    building:  document.getElementById('rm-building').value,
    capacity:  parseInt(document.getElementById('rm-capacity').value) || null,
  };
  try {
    if (existingId) {
      await api(`/api/sections/rooms/${existingId}`, 'PUT', payload);
      toast('Room updated');
    } else {
      await api('/api/sections/rooms', 'POST', payload);
      toast('Room saved');
    }
    closeModal('modal-room'); loadRooms();
  } catch (e) { toast(e.message, 'error'); }
}

async function saveUser() {
  if (!validateRequired([
    {id:'usr-username',  label:'Username'},
    {id:'usr-password',  label:'Password'},
    {id:'usr-password2', label:'Confirm Password'},
  ])) return;
  const pw  = document.getElementById('usr-password').value;
  const pw2 = document.getElementById('usr-password2').value;
  if (pw !== pw2) { toast('Passwords do not match', 'error'); return; }
  try {
    const email = document.getElementById('usr-email').value.trim();
    await api('/api/users', 'POST', {
      username: document.getElementById('usr-username').value,
      password: pw,
      role:     document.getElementById('usr-role').value,
      email:    email || null,
    });
    toast('User account created');
    ['usr-username','usr-email','usr-password','usr-password2'].forEach(id => document.getElementById(id).value = '');
    closeModal('modal-user'); loadUsers();
  } catch (e) { toast(e.message, 'error'); }
}

async function toggleUser(id) {
  try {
    await api(`/api/users/${id}/toggle`, 'PATCH');
    toast('User status updated'); loadUsers();
  } catch (e) { toast(e.message, 'error'); }
}

function resetPw(id) {
  document.getElementById('reset-pw-user-id').value    = id;
  document.getElementById('reset-pw-generated').textContent = '—';
  document.getElementById('reset-pw-step1').style.display   = '';
  document.getElementById('reset-pw-step2').style.display   = 'none';
  openModal('modal-reset-pw');
}

async function submitResetPw() {
  const id = document.getElementById('reset-pw-user-id').value;
  try {
    const result = await api(`/api/users/${id}/generate-temp-password`, 'PATCH');
    document.getElementById('reset-pw-generated').textContent = result.temporaryPassword;
    document.getElementById('reset-pw-step1').style.display   = 'none';
    document.getElementById('reset-pw-step2').style.display   = '';
  } catch (e) { toast(e.message, 'error'); }
}

async function activateSem(id) {
  try {
    await api(`/api/school-years/semesters/${id}/activate`, 'PATCH');
    toast('Semester activated'); loadSchoolYears();
    // refresh active sem info
    const sem = await api('/api/school-years/semesters/active');
    SMS.activeSemester = sem;
    document.getElementById('hdr-sy-text').textContent = sem.semesterLabel;
    // reset enrollment filter so next visit defaults to the new active semester
    const enrollFilter = document.getElementById('enroll-filter-sem');
    if (enrollFilter) { enrollFilter.innerHTML = ''; }
  } catch (e) { toast(e.message, 'error'); }
}

async function saveSchoolYear() {
  if (!validateRequired([
    {id:'sy-label',  label:'Year Label'},
    {id:'sem-start', label:'Start Date'},
    {id:'sem-end',   label:'End Date'},
  ])) return;
  const yearLabel  = document.getElementById('sy-label').value.trim();
  const semNum     = document.getElementById('sem-num').value;
  const existingId = document.getElementById('sem-id').value.trim();
  const startDate  = document.getElementById('sem-start').value;
  const endDate    = document.getElementById('sem-end').value;

  if (startDate >= endDate) { toast('End date must be after start date', 'error'); return; }

  const parts = yearLabel.match(/^(\d{4})-(\d{4})$/);
  if (!parts) { toast('Year Label must be in format e.g. 2026-2027', 'error'); return; }
  const syStartYear = parseInt(parts[1]);
  const syEndYear   = parseInt(parts[2]);
  const startYear   = new Date(startDate).getFullYear();
  const endYear     = new Date(endDate).getFullYear();
  if (startYear < syStartYear || startYear > syEndYear)
    { toast(`Start date must fall within the school year ${yearLabel}`, 'error'); return; }
  if (endYear < syStartYear || endYear > syEndYear)
    { toast(`End date must fall within the school year ${yearLabel}`, 'error'); return; }

  const diffDays = (new Date(endDate) - new Date(startDate)) / 86400000;
  if (diffDays < 30) { toast('Semester must span at least 30 days', 'error'); return; }

  // Auto-generate semester label from year + semester number
  const semNumNames = { '1': 'First', '2': 'Second', '3': 'Summer' };
  const semLabel = semNum === '3'
    ? `Summer ${yearLabel}`
    : `${semNumNames[semNum]} Semester ${yearLabel}`;

  try {
    if (existingId) {
      await api(`/api/school-years/semesters/${existingId}`, 'PUT', {
        semesterLabel: semLabel, semesterNumber: parseInt(semNum), startDate, endDate,
      });
      toast('Semester updated');
    } else {
      const syId  = 'SY-' + parts[1].slice(2) + parts[2].slice(2);
      try { await api('/api/school-years', 'POST', { schoolYearId: syId, yearLabel }); } catch (_) {}
      const semId = 'SEM-' + syId.replace(/^SY-/i, '') + '-' + semNum;
      await api('/api/school-years/semesters', 'POST', {
        semesterId: semId, schoolYearId: syId,
        semesterNumber: parseInt(semNum), semesterLabel: semLabel, startDate, endDate,
      });
      toast('Semester added');
    }
    closeSemModal();
    loadSchoolYears();
  } catch (e) { toast(e.message, 'error'); }
}

async function openGraduateModal() {
  try {
    const result = await api(`/api/alumni/eligibility/${_currentStudentId}`);
    if (!result.eligible) {
      const semLabel = n => n === 1 ? '1st Semester' : n === 2 ? '2nd Semester' : `Semester ${n}`;
      const groups = {};
      result.incomplete.forEach(c => {
        const key = `${c.yearLevel}-${c.semesterNumber}`;
        if (!groups[key]) groups[key] = { yearLevel: c.yearLevel, semesterNumber: c.semesterNumber, courses: [] };
        groups[key].courses.push(c.courseName);
      });
      const container = document.getElementById('grad-inelig-list');
      container.innerHTML = Object.values(groups).map(g =>
        `<div class="grad-inelig-card" role="listitem">
          <div class="grad-inelig-card-header">Year ${g.yearLevel} — ${semLabel(g.semesterNumber)}</div>
          <ul class="grad-inelig-card-courses">
            ${g.courses.map(name => `<li>${escHtml(name)}</li>`).join('')}
          </ul>
        </div>`
      ).join('');
      openModal('modal-grad-ineligible');
      return;
    }
    openModal('modal-graduate');
  } catch (e) { toast(e.message, 'error'); }
}

async function graduateStudent() {
  if (!validateRequired([
    {id:'grad-date', label:'Graduation Date'},
  ])) return;
  try {
    await api(`/api/alumni/graduate/${_currentStudentId}`, 'POST', {
      graduationDate:  document.getElementById('grad-date').value,
      honors:          document.getElementById('grad-honors').value,
      currentMinistry: document.getElementById('grad-ministry').value,
    });
    toast('Student graduated and moved to Alumni');
    closeModal('modal-graduate'); gotoPage('alumni', null);
  } catch (e) { toast(e.message, 'error'); }
}

function updateExamResult() {
  const scoreVal  = document.getElementById('ex-score').value;
  const maxScore  = parseFloat(document.getElementById('ex-max').value) || 100;
  const score     = parseFloat(scoreVal);
  const display   = document.getElementById('ex-result-display');
  const hidden    = document.getElementById('ex-result');

  if (scoreVal === '' || isNaN(score)) {
    display.textContent  = 'Pending';
    display.style.color  = 'var(--gray-400)';
    hidden.value         = 'Pending';
  } else if (score >= maxScore * 0.60) {
    display.textContent  = 'Passed';
    display.style.color  = 'var(--success,#16a34a)';
    hidden.value         = 'Passed';
  } else {
    display.textContent  = 'Failed';
    display.style.color  = 'var(--danger,#dc2626)';
    hidden.value         = 'Failed';
  }
}

async function refreshApplicantExams() {
  try {
    const exams = await api(`/api/applicants/${_currentApplicantId}/exams`);
    _currentApplicantExams = exams || [];
    document.getElementById('apd-exams-body').innerHTML = exams.length
      ? exams.map(e => `<tr>
          <td>${escHtml(e.examDate || '—')}</td>
          <td>${escHtml(e.score ?? '—')}</td>
          <td>${escHtml(e.maxScore ?? 100)}</td>
          <td>${badge(e.result)}</td>
          <td>${escHtml(e.remarks || '—')}</td>
          <td style="white-space:nowrap;text-align:right">
            <button class="btn btn-outline btn-sm registrar-only" onclick="openEditExamModal('${escHtml(e.examId)}')" style="padding:3px 10px;font-size:.72rem;margin-right:4px">Edit</button>
            <button class="registrar-only" onclick="deleteExam('${escHtml(e.examId)}')" style="padding:3px 10px;font-size:.72rem;font-weight:600;font-family:inherit;background:var(--danger,#dc2626);color:#fff;border:none;border-radius:6px;cursor:pointer">Delete</button>
          </td>
        </tr>`).join('')
      : '<tr><td colspan="6" style="text-align:center;color:var(--gray-400,#9ca3af);padding:12px">No exams recorded</td></tr>';

    // Re-evaluate any visible warnings now that exam data has changed
    const latestExam = _currentApplicantExams
      .slice()
      .sort((a, b) => new Date(b.examDate) - new Date(a.examDate))[0];
    const latestIsFailed = latestExam && latestExam.result === 'Failed';

    // Status dropdown warning (shown when user picks Admitted from the dropdown)
    const statusWarn = document.getElementById('apd-status-warn');
    if (statusWarn && statusWarn.style.display !== 'none' && !latestIsFailed) {
      statusWarn.style.display = 'none';
      _previousApplicantStatus = null;
    }

    // Admit modal warning (shown inside the Admit confirmation modal)
    const admitWarn = document.getElementById('admit-exam-warning');
    if (admitWarn && admitWarn.style.display !== 'none' && !latestIsFailed) {
      admitWarn.style.display = 'none';
    }
  } catch (_) {}
}

function openExamModal(applicantId, name) {
  _currentApplicantId = applicantId;
  _currentExamId = null;
  document.getElementById('exam-modal-title').textContent = 'Record Entrance Exam';
  document.getElementById('exam-modal-sub').textContent = `Recording exam for ${name}`;
  document.getElementById('ex-date').value    = '';
  document.getElementById('ex-score').value   = '';
  document.getElementById('ex-max').value     = '100';
  document.getElementById('ex-remarks').value = '';
  updateExamResult();
  openModal('modal-exam');
}

function openEditExamModal(examId) {
  const exam = _currentApplicantExams.find(e => e.examId === examId);
  if (!exam) return;
  _currentExamId = examId;
  document.getElementById('exam-modal-title').textContent = 'Edit Exam Record';
  document.getElementById('exam-modal-sub').textContent = `Editing exam from ${exam.examDate}`;
  document.getElementById('ex-date').value    = exam.examDate || '';
  document.getElementById('ex-score').value   = exam.score ?? '';
  document.getElementById('ex-max').value     = exam.maxScore ?? 100;
  document.getElementById('ex-remarks').value = exam.remarks || '';
  updateExamResult();
  openModal('modal-exam');
}

async function deleteExam(examId) {
  if (!confirm('Delete this exam record? This cannot be undone.')) return;
  try {
    await api(`/api/applicants/exams/${examId}`, 'DELETE');
    toast('Exam record deleted');
    await refreshApplicantExams();
  } catch (e) { toast(e.message, 'error'); }
}

async function saveExam() {
  if (!validateRequired([{id:'ex-date', label:'Exam Date'}])) return;
  const payload = {
    examDate: document.getElementById('ex-date').value,
    score:    parseFloat(document.getElementById('ex-score').value) || null,
    maxScore: parseFloat(document.getElementById('ex-max').value) || 100,
    result:   document.getElementById('ex-result').value,
    remarks:  document.getElementById('ex-remarks').value,
  };
  try {
    if (_currentExamId) {
      await api(`/api/applicants/exams/${_currentExamId}`, 'PUT', payload);
      toast('Exam updated');
    } else {
      await api(`/api/applicants/${_currentApplicantId}/exams`, 'POST', payload);
      toast('Exam recorded');
    }
    closeModal('modal-exam');
    await refreshApplicantExams();
  } catch (e) { toast(e.message, 'error'); }
}

let _rptStudents = [];

function renderRptStudentOptions(query) {
  const list = document.getElementById('gen-report-student-list');
  if (!list) return;
  const q = (query || '').toLowerCase();
  const filtered = _rptStudents.filter(s =>
    `${s.firstName} ${s.lastName} ${s.studentId}`.toLowerCase().includes(q)
  );
  list.innerHTML = '';
  if (!filtered.length) {
    list.innerHTML = '<div class="enr-student-option no-match">No students found</div>';
    return;
  }
  filtered.forEach(s => {
    const label = `${s.firstName} ${s.lastName} (${s.studentId})`;
    const item = document.createElement('div');
    item.className = 'enr-student-option';
    item.textContent = label;
    item.setAttribute('role', 'option');
    item.addEventListener('mousedown', e => {
      e.preventDefault();
      document.getElementById('gen-report-student').value = s.studentId;
      document.getElementById('gen-report-student-search').value = label;
      closeRptStudentDropdown();
    });
    list.appendChild(item);
  });
}
function openRptStudentDropdown()  { document.getElementById('gen-report-student-list').classList.add('open'); }
function closeRptStudentDropdown() { document.getElementById('gen-report-student-list').classList.remove('open'); }

function openReportModal(type) {
  _currentReportType = type;

  const titles = {
    GradeCard:           'Grade Card',
    TranscriptOfRecords: 'Transcript of Records',
    CHEDReport:          'CHED Report',
  };
  const subs = {
    GradeCard:           'Select a student and semester to generate the grade card.',
    TranscriptOfRecords: 'Select a student to generate their full transcript.',
    CHEDReport:          'Select a semester to generate the CHED compliance report.',
  };
  document.getElementById('gen-report-title').textContent = titles[type] || type;
  document.getElementById('gen-report-sub').textContent   = subs[type]   || '';

  const showStudent  = type === 'GradeCard' || type === 'TranscriptOfRecords';
  const showSemester = type === 'GradeCard' || type === 'CHEDReport';

  document.getElementById('gen-report-student-wrap').style.display = showStudent  ? '' : 'none';
  document.getElementById('gen-report-sem-wrap').style.display     = showSemester ? '' : 'none';

  // Reset student search field
  document.getElementById('gen-report-student-search').value = '';
  document.getElementById('gen-report-student').value = '';

  if (showStudent) {
    _rptStudents = [];
    api('/api/students').then(students => {
      _rptStudents = students;
      renderRptStudentOptions('');
    }).catch(_=>{});
  }
  if (showSemester) {
    api('/api/school-years/semesters').then(sems => {
      populateSelect('gen-report-sem', sems, 'semesterId', s => s.semesterLabel, '— Select Semester —');
    }).catch(_=>{});
  }
  openModal('modal-gen-report');
}

let _reportBlobUrl = null;
let _reportFilename = 'report.pdf';

async function generateReport() {
  const type       = _currentReportType;
  const studentId  = document.getElementById('gen-report-student').value;
  const semesterId = document.getElementById('gen-report-sem').value;

  let url;
  if (type === 'GradeCard') {
    if (!studentId || !semesterId) { toast('Please select a student and a semester.', 'error'); return; }
    url = `/api/reports/grade-card?studentId=${encodeURIComponent(studentId)}&semesterId=${encodeURIComponent(semesterId)}`;
    _reportFilename = `grade-card-${studentId}.pdf`;
  } else if (type === 'TranscriptOfRecords') {
    if (!studentId) { toast('Please select a student.', 'error'); return; }
    url = `/api/reports/transcript?studentId=${encodeURIComponent(studentId)}`;
    _reportFilename = `transcript-${studentId}.pdf`;
  } else if (type === 'CHEDReport') {
    if (!semesterId) { toast('Please select a semester.', 'error'); return; }
    url = `/api/reports/ched-report?semesterId=${encodeURIComponent(semesterId)}`;
    _reportFilename = `ched-report-${semesterId}.pdf`;
  } else {
    toast('Unknown report type.', 'error'); return;
  }

  closeModal('modal-gen-report');

  // Show preview modal in loading state
  const titles = { GradeCard: 'Grade Card', TranscriptOfRecords: 'Transcript of Records', CHEDReport: 'CHED Compliance Report' };
  document.getElementById('report-preview-title').textContent = titles[type] || type;
  document.getElementById('report-preview-sub').textContent = 'Generating PDF — please wait…';
  document.getElementById('report-preview-loading').style.display = 'flex';
  document.getElementById('report-preview-iframe').style.display = 'none';
  document.getElementById('btn-report-download').disabled = true;
  if (_reportBlobUrl) { URL.revokeObjectURL(_reportBlobUrl); _reportBlobUrl = null; }
  openModal('modal-report-preview');

  try {
    const resp = await fetch(url, { credentials: 'include' });
    if (!resp.ok) {
      const msg = await resp.text().catch(() => 'Unknown error');
      throw new Error(msg || `HTTP ${resp.status}`);
    }
    const blob = await resp.blob();
    _reportBlobUrl = URL.createObjectURL(blob);

    // #navpanes=0 hides the thumbnail sidebar in Chrome's PDF viewer, giving full width to content
    document.getElementById('report-preview-iframe').src = _reportBlobUrl + '#navpanes=0';
    document.getElementById('report-preview-loading').style.display = 'none';
    document.getElementById('report-preview-iframe').style.display = 'block';
    document.getElementById('report-preview-sub').textContent = 'Review the report below. Click Download PDF to save it.';
    document.getElementById('btn-report-download').disabled = false;
  } catch (e) {
    closeModal('modal-report-preview');
    toast('Failed to generate report: ' + e.message, 'error');
  }
}

function downloadReport() {
  if (!_reportBlobUrl) return;
  const a = document.createElement('a');
  a.href = _reportBlobUrl;
  a.download = _reportFilename;
  a.click();
}

function closeReportPreview() {
  closeModal('modal-report-preview');
  if (_reportBlobUrl) { URL.revokeObjectURL(_reportBlobUrl); _reportBlobUrl = null; }
  document.getElementById('report-preview-iframe').src = 'about:blank';
}

async function triggerBackup() {
  const btn = document.getElementById('btn-download-backup');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Creating backup…'; }
  try {
    const resp = await fetch('/api/backup/create', { method: 'POST' });
    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      toast(err.error || 'Backup failed.', 'error'); return;
    }
    const blob = await resp.blob();
    const disposition = resp.headers.get('Content-Disposition') || '';
    const match = disposition.match(/filename="([^"]+)"/);
    const filename = match ? match[1] : 'sms_backup.sql';
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
    toast('Backup downloaded: ' + filename, 'success');
    loadBackup();
  } catch (e) {
    toast('Backup failed: ' + e.message, 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '⬇ Download Backup'; }
  }
}

function triggerRestore() {
  document.getElementById('restore-file-input').click();
}

async function doRestore(input) {
  const file = input.files[0];
  input.value = '';
  if (!file) return;
  if (!confirm(`Restoring will OVERWRITE the entire database with "${file.name}".\n\nThis cannot be undone. Are you sure?`)) return;
  const btn = document.getElementById('btn-restore');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Restoring…'; }
  try {
    const form = new FormData();
    form.append('file', file);
    const resp = await fetch('/api/backup/restore', { method: 'POST', body: form });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok) { toast(data.error || 'Restore failed.', 'error'); return; }
    toast('Database restored successfully. Refresh the page to see updated data.', 'success');
    loadBackup();
  } catch (e) {
    toast('Restore failed: ' + e.message, 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '⬆ Upload & Restore'; }
  }
}

async function viewApplicantDetail(id) {
  try {
    const a = await api(`/api/applicants/${id}`);
    _currentApplicantId = id;
    _currentApplicantExams = [];

    document.getElementById('apd-title').textContent = `${a.firstName} ${a.lastName}`;
    document.getElementById('apd-sub').textContent   = `${a.applicantId} · ${a.appliedProgram?.programCode || '—'}`;

    // Populate personal fields
    document.getElementById('apd-fname').value    = a.firstName || '';
    document.getElementById('apd-lname').value    = a.lastName  || '';
    document.getElementById('apd-mname').value    = a.middleName || '';
    document.getElementById('apd-dob').value      = a.dateOfBirth || '';
    document.getElementById('apd-email').value    = a.email || '';
    document.getElementById('apd-contact').value  = a.contactNumber || '';
    document.getElementById('apd-level').value    = a.seminaryLevel || 'College';
    document.getElementById('apd-program').value  = a.appliedProgram?.programId || 'PRG-1001';
    document.getElementById('apd-address').value  = a.address || '';
    document.getElementById('apd-school').value   = a.lastSchoolAttended || '';
    document.getElementById('apd-schoolyr').value = a.lastSchoolYear || '';
    document.getElementById('apd-schoollvl').value= a.lastYearLevel || '';

    // Populate family fields
    document.getElementById('apd-father').value          = a.fatherName || '';
    document.getElementById('apd-father-occ').value      = a.fatherOccupation || '';
    document.getElementById('apd-mother').value          = a.motherName || '';
    document.getElementById('apd-mother-occ').value      = a.motherOccupation || '';
    document.getElementById('apd-guardian').value        = a.guardianName || '';
    document.getElementById('apd-guardian-contact').value= a.guardianContact || '';
    document.getElementById('apd-nationality').value     = a.nationality || '';
    document.getElementById('apd-religion').value        = a.religion || 'Roman Catholic';

    // Load application status
    let currentStatus = 'Applied';
    try {
      const app = await api(`/api/applicants/${id}/application`);
      currentStatus = app?.applicationStatus || 'Applied';
    } catch (_) {}
    const statusEl = document.getElementById('apd-status');
    statusEl.value = currentStatus;
    statusEl.dataset.savedValue = currentStatus;
    document.getElementById('apd-status-warn').style.display = 'none';

    // Show Admit button only when convention is done and not yet admitted
    document.getElementById('apd-admit-btn').style.display =
      currentStatus === 'AspiringConventionAttended' ? '' : 'none';

    await refreshApplicantExams();

    // Reset to first tab and view mode
    switchTab(document.querySelector('#modal-applicant-detail .tab'), 'apd-tab-personal', ['apd-tab-personal','apd-tab-family','apd-tab-exams']);
    applicantEditMode(false);
    openModal('modal-applicant-detail');
  } catch (e) { toast('Failed to load applicant', 'error'); }
}


function openAdmitModal() {
  const name = document.getElementById('apd-title').textContent;
  document.getElementById('admit-sub').textContent = `Admitting: ${name}`;
  const warningEl = document.getElementById('admit-exam-warning');
  const latest = _currentApplicantExams
    .slice()
    .sort((a, b) => new Date(b.examDate) - new Date(a.examDate))[0];
  warningEl.style.display = (latest && latest.result === 'Failed') ? 'block' : 'none';
  openModal('modal-admit');
}

async function confirmAdmit() {
  try {
    await api(`/api/applicants/${_currentApplicantId}/admit`, 'POST');
    closeModal('modal-admit');
    closeModal('modal-applicant-detail');
    toast('Applicant admitted. Go to Enrollment to enroll them and create their account.');
    loadApplicants();
  } catch (e) { toast(e.message, 'error'); }
}

function copyCredField(elementId) {
  const text = document.getElementById(elementId).textContent;
  navigator.clipboard.writeText(text).then(() => toast('Copied to clipboard'));
}

function applicantEditMode(on) {
  const inputs = ['apd-fname','apd-lname','apd-mname','apd-dob','apd-email','apd-contact','apd-address','apd-school','apd-schoolyr','apd-schoollvl','apd-father','apd-father-occ','apd-mother','apd-mother-occ','apd-guardian','apd-guardian-contact','apd-nationality'];
  inputs.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.readOnly = !on;
  });
  document.getElementById('apd-level').disabled   = !on;
  document.getElementById('apd-program').disabled = !on;
  document.getElementById('apd-edit-btn').style.display  = on ? 'none' : '';
  document.getElementById('apd-admit-btn').style.display =
    on ? 'none' : (document.getElementById('apd-status').value === 'AspiringConventionAttended' ? '' : 'none');
  document.getElementById('apd-actions').style.display     = on ? '' : 'none';
  document.getElementById('apd-modal').classList.toggle('editing', on);
}

function onApplicantStatusChange() {
  const select   = document.getElementById('apd-status');
  const newStatus = select.value;
  if (newStatus === 'Admitted') {
    const latest = _currentApplicantExams
      .slice()
      .sort((a, b) => new Date(b.examDate) - new Date(a.examDate))[0];
    if (latest && latest.result === 'Failed') {
      _previousApplicantStatus = select.dataset.savedValue || 'Applied';
      document.getElementById('apd-status-warn').style.display = 'block';
      return;
    }
  }
  document.getElementById('apd-status-warn').style.display = 'none';
  _doSaveApplicantStatus(newStatus);
}

async function confirmApplicantStatusAdmit() {
  document.getElementById('apd-status-warn').style.display = 'none';
  await _doSaveApplicantStatus('Admitted');
}

function cancelApplicantStatusAdmit() {
  document.getElementById('apd-status-warn').style.display = 'none';
  const select = document.getElementById('apd-status');
  select.value = _previousApplicantStatus || 'Applied';
  _previousApplicantStatus = null;
}

async function _doSaveApplicantStatus(status) {
  try {
    const app = await api(`/api/applicants/${_currentApplicantId}/application`);
    if (!app) return;
    await api(`/api/applicants/applications/${app.applicationId}/status?status=${status}`, 'PATCH');
    document.getElementById('apd-status').dataset.savedValue = status;
    document.getElementById('apd-admit-btn').style.display =
      status === 'AspiringConventionAttended' ? '' : 'none';
    loadApplicants();
    toast('Status updated');
  } catch (e) { toast(e.message, 'error'); }
}

async function saveApplicantEdit() {
  if (!validateRequired([
    {id:'apd-fname', label:'First Name'},
    {id:'apd-lname', label:'Last Name'},
  ])) return;
  try {
    await api(`/api/applicants/${_currentApplicantId}`, 'PUT', {
      firstName:          document.getElementById('apd-fname').value,
      lastName:           document.getElementById('apd-lname').value,
      middleName:         document.getElementById('apd-mname').value,
      dateOfBirth:        document.getElementById('apd-dob').value,
      email:              document.getElementById('apd-email').value,
      contactNumber:      document.getElementById('apd-contact').value,
      seminaryLevel:      document.getElementById('apd-level').value,
      address:            document.getElementById('apd-address').value,
      lastSchoolAttended: document.getElementById('apd-school').value,
      lastSchoolYear:     document.getElementById('apd-schoolyr').value,
      lastYearLevel:      document.getElementById('apd-schoollvl').value,
      fatherName:         document.getElementById('apd-father').value,
      fatherOccupation:   document.getElementById('apd-father-occ').value,
      motherName:         document.getElementById('apd-mother').value,
      motherOccupation:   document.getElementById('apd-mother-occ').value,
      guardianName:       document.getElementById('apd-guardian').value,
      guardianContact:    document.getElementById('apd-guardian-contact').value,
      nationality:        document.getElementById('apd-nationality').value,
      religion:           document.getElementById('apd-religion').value,
      appliedProgram:     { programId: document.getElementById('apd-program').value },
    });
    toast('Applicant updated successfully');
    applicantEditMode(false);
    loadApplicants();
  } catch (e) { toast(e.message, 'error'); }
}

// ── Enrollment Subjects ───────────────────────────────────────
let _currentEnrollmentId  = null;
let _currentEnrollmentPgm = null;

async function viewSubjects(enrollmentId, studentName, programId) {
  _currentEnrollmentId  = enrollmentId;
  _currentEnrollmentPgm = programId;
  document.getElementById('enrs-title').textContent = 'Enrolled Subjects';
  document.getElementById('enrs-sub').textContent   = `${studentName} · ${enrollmentId}`;
  document.getElementById('enrs-add-form').style.display = 'none';
  await refreshSubjectsTable();
  openModal('modal-enr-subjects');
}

async function refreshSubjectsTable() {
  try {
    const data = await api(`/api/enrollment/${_currentEnrollmentId}/subjects`);
    document.getElementById('enrs-body').innerHTML = data.length
      ? data.map(s => `<tr>
          <td>${escHtml(s.course?.courseCode || '—')}</td>
          <td>${escHtml(s.course?.courseName || '—')}${s.overrideReason
            ? `<span title="Override: ${escHtml(s.overrideReason)}" style="margin-left:6px;font-size:.7rem;font-weight:700;background:#fef3c7;color:#b45309;border-radius:4px;padding:1px 5px;cursor:default">OVR</span>`
            : ''}</td>
          <td style="text-align:center">${escHtml(String(s.course?.units ?? '—'))}</td>
          <td>${badge(s.status)}</td>
        </tr>`).join('')
      : '<tr><td colspan="4" style="text-align:center;color:var(--gray-400);padding:20px">No subjects enrolled yet</td></tr>';
  } catch (_) { toast('Failed to load subjects', 'error'); }
}

async function toggleCourseChecklist() {
  const form = document.getElementById('enrs-add-form');
  if (form.style.display !== 'none') { form.style.display = 'none'; return; }

  const checklist = document.getElementById('enrs-checklist');
  checklist.innerHTML = '<p style="color:var(--gray-400);font-size:.85rem;text-align:center;padding:12px 0">Loading available subjects…</p>';
  form.style.display = 'block';
  document.getElementById('enrs-check-all').checked = false;

  try {
    const rawCourses = await api(`/api/enrollment/${_currentEnrollmentId}/available-courses`);
    if (!rawCourses.length) {
      checklist.innerHTML = '<p style="color:var(--gray-400);font-size:.85rem;text-align:center;padding:12px 0">All subjects for this year and semester are already enrolled.</p>';
      return;
    }
    // Sort: regular → retake → makeup → blocked
    const typeOrder = { regular: 0, retake: 1, makeup: 2, blocked: 3 };
    const courses = [...rawCourses].sort((a, b) => (typeOrder[a.type] ?? 0) - (typeOrder[b.type] ?? 0));

    const hasBlocked  = courses.some(c => c.type === 'blocked');
    const hasEnrollab = courses.some(c => c.type !== 'blocked');
    const hasMakeup   = courses.some(c => c.type === 'makeup');

    checklist.innerHTML = courses.map((c, i) => {
      const id = escHtml(c.courseId);
      const units = `${escHtml(String(c.units))} unit${c.units !== 1 ? 's' : ''}`;

      const prevType = i > 0 ? courses[i - 1].type : null;

      const makeupSep = (c.type === 'makeup' && prevType !== 'makeup')
        ? `<div style="font-size:.72rem;font-weight:600;color:#2563eb;letter-spacing:.04em;padding:6px 2px 2px;text-transform:uppercase">Makeup — subjects from a previous semester</div>`
        : '';

      const blockedSep = (c.type === 'blocked' && prevType !== 'blocked' && hasEnrollab)
        ? `<div style="font-size:.72rem;font-weight:600;color:var(--gray-400);letter-spacing:.04em;padding:6px 2px 2px;text-transform:uppercase">Cannot enroll — prerequisite not met</div>`
        : '';

      const separator = makeupSep + blockedSep;

      if (c.type === 'blocked') {
        // Blocked: greyed out — checkbox disabled until registrar fills override reason
        return separator + `
        <div style="border:1.5px solid var(--gray-200);border-radius:7px;overflow:hidden;opacity:.75">
          <div style="display:flex;align-items:center;gap:10px;padding:8px 10px;background:var(--gray-50);font-size:.875rem;color:var(--gray-500)">
            <input type="checkbox" class="enrs-course-cb" value="${id}" disabled
              style="width:15px;height:15px;flex-shrink:0">
            <span style="font-size:.7rem;font-weight:700;background:var(--gray-200);color:var(--gray-600);border-radius:4px;padding:2px 6px;white-space:nowrap">BLOCKED</span>
            <span style="font-weight:600;min-width:52px">${escHtml(c.courseCode)}</span>
            <span style="flex:1">${escHtml(c.courseName)}</span>
            <span style="font-size:.8rem;white-space:nowrap">${units}</span>
            <button type="button" onclick="toggleOverrideField('ovr-${id}')"
              style="font-size:.75rem;color:var(--primary);background:none;border:none;cursor:pointer;white-space:nowrap;text-decoration:underline">Override?</button>
          </div>
          <div style="font-size:.75rem;color:var(--gray-500);padding:2px 10px 4px 44px">${escHtml(c.blockedReason)}</div>
          <div id="ovr-${id}" style="display:none;padding:6px 10px;border-top:1px solid var(--gray-200);background:white">
            <input type="text" placeholder="Enter override reason (e.g. Dean's approval)…"
              data-override-for="${id}"
              oninput="handleOverrideInput(this)"
              style="width:100%;padding:7px 10px;border:1.5px solid var(--warning,#f59e0b);border-radius:6px;font-size:.8rem;font-family:inherit;outline:none;box-sizing:border-box">
          </div>
        </div>`;
      }

      if (c.type === 'makeup') {
        // Makeup: blue highlight, checked by default — course from a prior semester never taken
        return separator + `
        <label style="display:flex;align-items:center;gap:10px;padding:8px 10px;background:#eff6ff;border:1.5px solid #3b82f6;border-radius:7px;cursor:pointer;font-size:.875rem;color:var(--gray-800)">
          <input type="checkbox" class="enrs-course-cb" value="${id}" checked
            style="width:15px;height:15px;accent-color:#2563eb;flex-shrink:0">
          <span style="font-size:.7rem;font-weight:700;background:#dbeafe;color:#1d4ed8;border-radius:4px;padding:2px 6px;white-space:nowrap">MAKEUP</span>
          <span style="font-weight:600;color:#1d4ed8;min-width:52px">${escHtml(c.courseCode)}</span>
          <span style="flex:1">${escHtml(c.courseName)}</span>
          <span style="color:var(--gray-400);font-size:.8rem;white-space:nowrap">${units}</span>
        </label>`;
      }

      if (c.type === 'retake') {
        // Retake: amber highlight, checked by default — course the student previously failed
        return `
        <label style="display:flex;align-items:center;gap:10px;padding:8px 10px;background:#fffbeb;border:1.5px solid #f59e0b;border-radius:7px;cursor:pointer;font-size:.875rem;color:var(--gray-800)">
          <input type="checkbox" class="enrs-course-cb" value="${id}" checked
            style="width:15px;height:15px;accent-color:#f59e0b;flex-shrink:0">
          <span style="font-size:.7rem;font-weight:700;background:#fef3c7;color:#b45309;border-radius:4px;padding:2px 6px;white-space:nowrap">RETAKE</span>
          <span style="font-weight:600;color:#b45309;min-width:52px">${escHtml(c.courseCode)}</span>
          <span style="flex:1">${escHtml(c.courseName)}</span>
          <span style="color:var(--gray-400);font-size:.8rem;white-space:nowrap">${units}</span>
        </label>`;
      }

      // Regular course
      return `
      <label style="display:flex;align-items:center;gap:10px;padding:8px 10px;background:white;border:1.5px solid var(--gray-200);border-radius:7px;cursor:pointer;font-size:.875rem;color:var(--gray-800)">
        <input type="checkbox" class="enrs-course-cb" value="${id}"
          style="width:15px;height:15px;accent-color:var(--primary);flex-shrink:0">
        <span style="font-weight:600;color:var(--primary);min-width:52px">${escHtml(c.courseCode)}</span>
        <span style="flex:1">${escHtml(c.courseName)}</span>
        <span style="color:var(--gray-400);font-size:.8rem;white-space:nowrap">${units}</span>
      </label>`;
    }).join('');
  } catch (e) { toast('Failed to load available subjects', 'error'); form.style.display = 'none'; }
}

function toggleOverrideField(divId) {
  const div = document.getElementById(divId);
  if (!div) return;
  const isHidden = div.style.display === 'none';
  div.style.display = isHidden ? 'block' : 'none';
  if (!isHidden) {
    // Collapsing: clear input and re-disable checkbox
    const input = div.querySelector('input[data-override-for]');
    if (input) { input.value = ''; handleOverrideInput(input); }
  }
}

function handleOverrideInput(input) {
  const courseId = input.dataset.overrideFor;
  const cb = document.querySelector(`.enrs-course-cb[value="${courseId}"]`);
  if (!cb) return;
  const hasReason = input.value.trim().length > 0;
  cb.disabled = !hasReason;
  cb.checked  = hasReason;
}

function toggleAllCourseChecks(checked) {
  // Only toggle non-blocked (enabled) checkboxes
  document.querySelectorAll('.enrs-course-cb:not([disabled])').forEach(cb => cb.checked = checked);
}

async function enrollCheckedCourses() {
  const courseIds = [...document.querySelectorAll('.enrs-course-cb:checked:not([disabled])')].map(cb => cb.value);
  if (!courseIds.length) { toast('Please select at least one subject', 'error'); return; }

  // Collect override reasons for blocked courses that were unlocked (Option B)
  const overrides = {};
  document.querySelectorAll('input[data-override-for]').forEach(input => {
    const reason = input.value.trim();
    if (reason && courseIds.includes(input.dataset.overrideFor))
      overrides[input.dataset.overrideFor] = reason;
  });

  try {
    await api(`/api/enrollment/${_currentEnrollmentId}/subjects/bulk`, 'POST', { courseIds, overrides });
    const overrideCount = Object.keys(overrides).length;
    toast(`${courseIds.length} subject${courseIds.length > 1 ? 's' : ''} enrolled`
      + (overrideCount ? ` (${overrideCount} with override)` : ''));
    document.getElementById('enrs-add-form').style.display = 'none';
    document.getElementById('enrs-check-all').checked = false;
    await refreshSubjectsTable();
  } catch (e) { toast(e.message, 'error'); }
}

// ══════════════════════════════════════════════════════════════
// SUBMISSIONS MODULE
// Handles the Online Submissions screen for the registrar.
// Students submit via /apply.html → stored as OnlineSubmission (Pending).
// Registrar reviews here, then accepts (→ creates Applicant) or rejects.
// ══════════════════════════════════════════════════════════════

let _currentSubmissionId = null;   // submissionId of the modal currently open
let _submissionStatusFilter = '';  // current active tab filter

/** Loads all submissions for the given status filter and renders the table. */
async function loadSubmissions(statusFilter) {
  if (statusFilter !== undefined) _submissionStatusFilter = statusFilter;
  const url = _submissionStatusFilter
    ? `/api/submissions?status=${encodeURIComponent(_submissionStatusFilter)}`
    : '/api/submissions';
  try {
    const data = await api(url);
    renderSubmissionsTable(data);
    updateSubCounts(data, _submissionStatusFilter);
  } catch (e) {
    document.getElementById('tbl-submissions').innerHTML =
      `<tr><td colspan="7" style="text-align:center;color:var(--danger);padding:20px">${e.message}</td></tr>`;
  }
}

/** Switches the active tab and reloads the table. */
function switchSubTab(btn, status) {
  document.querySelectorAll('#sub-tabs .tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  loadSubmissions(status);
}

/** Renders submission rows into the table. */
function renderSubmissionsTable(rows) {
  const tbody = document.getElementById('tbl-submissions');
  clearSortCache('tbl-submissions');
  if (!rows || rows.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--gray-400);padding:24px">No submissions found.</td></tr>';
    return;
  }
  tbody.innerHTML = rows.map(s => {
    const name   = [s.lastName, s.firstName, s.middleName].filter(Boolean).join(', ');
    const prog   = s.appliedProgram?.programName || '—';
    const date   = s.submittedAt ? s.submittedAt.substring(0,10) : '—';
    const badge  = subStatusBadge(s.status);
    const canAct = s.status === 'Pending';
    return `<tr>
      <td>${s.submissionId}</td>
      <td>${escHtml(name)}</td>
      <td>${s.seminaryLevel || '—'}</td>
      <td>${escHtml(prog)}</td>
      <td>${date}</td>
      <td>${badge}</td>
      <td><button class="btn btn-outline" style="font-size:0.78rem;padding:4px 12px"
           onclick="openSubmissionDetail('${s.submissionId}')">Review</button></td>
    </tr>`;
  }).join('');
}

/** Updates the count badges on each tab after a full load. */
function updateSubCounts(allLoaded, currentFilter) {
  // Only update all counts when not filtered (too expensive to do 4 calls)
  // For a filtered view just update the current tab
  if (!currentFilter) {
    let pending=0, accepted=0, rejected=0;
    allLoaded.forEach(s => {
      if (s.status==='Pending')  pending++;
      if (s.status==='Accepted') accepted++;
      if (s.status==='Rejected') rejected++;
    });
    const total = allLoaded.length;
    document.getElementById('sub-count-all').textContent      = total;
    document.getElementById('sub-count-pending').textContent  = pending;
    document.getElementById('sub-count-accepted').textContent = accepted;
    document.getElementById('sub-count-rejected').textContent = rejected;
  }
}

/** Returns a colored badge span for a submission status. */
function subStatusBadge(status) {
  const map = {
    Pending:  'badge-warn',
    Accepted: 'badge-success',
    Rejected: 'badge-danger',
  };
  return `<span class="badge ${map[status] || ''}">${status}</span>`;
}

/** Opens the Submission Review modal and populates all fields. */
async function openSubmissionDetail(id) {
  _currentSubmissionId = id;
  // Reset state
  document.getElementById('sv-reject-input-wrap').style.display = 'none';
  document.getElementById('sv-rejection-wrap').style.display    = 'none';
  document.getElementById('sv-reject-reason-input').value = '';

  try {
    const s = await api(`/api/submissions/${id}`);

    // Header
    document.getElementById('sub-modal-title').textContent = `${s.lastName}, ${s.firstName}${s.middleName ? ' ' + s.middleName : ''}`;
    document.getElementById('sub-modal-subid').textContent = s.submissionId;
    const _badge = document.getElementById('sub-modal-badge');
    _badge.className = 'badge ' + (s.status==='Pending'?'badge-warn':s.status==='Accepted'?'badge-success':'badge-danger');
    _badge.textContent = s.status;

    // Fill all read-only fields
    sv('sv-fname',         s.firstName || '—');
    sv('sv-lname',         s.lastName  || '—');
    sv('sv-mname',         s.middleName || '—');
    sv('sv-dob',           s.dateOfBirth || '—');
    sv('sv-pob',           s.placeOfBirth || '—');
    sv('sv-email',         s.email || '—');
    sv('sv-contact',       s.contactNumber || '—');
    sv('sv-nationality',   s.nationality || '—');
    sv('sv-religion',      s.religion || '—');
    sv('sv-address',       s.address || '—');
    sv('sv-level',         s.seminaryLevel || '—');
    sv('sv-program',       s.appliedProgram?.programName || '—');
    sv('sv-father',        s.fatherName || '—');
    sv('sv-father-occ',    s.fatherOccupation || '—');
    sv('sv-mother',        s.motherName || '—');
    sv('sv-mother-occ',    s.motherOccupation || '—');
    sv('sv-guardian',      s.guardianName || '—');
    sv('sv-guardian-contact', s.guardianContact || '—');
    sv('sv-school',        s.lastSchoolAttended || '—');
    sv('sv-school-year',   s.lastSchoolYear || '—');
    sv('sv-year-level',    s.lastYearLevel || '—');

    // Documents
    const docMap = [
      { elId: 'sv-doc-birth',         path: s.birthCertificate,         label: 'Birth Certificate' },
      { elId: 'sv-doc-baptismal',      path: s.baptismalCertificate,     label: 'Baptismal Certificate' },
      { elId: 'sv-doc-confirmation',   path: s.confirmationCertificate,  label: 'Confirmation Certificate' },
      { elId: 'sv-doc-reportcard',     path: s.reportCard,               label: 'Report Card' },
      { elId: 'sv-doc-goodmoral',      path: s.goodMoral,                label: 'Good Moral Certificate' },
    ];
    docMap.forEach(({ elId, path, label }) => {
      const el = document.getElementById(elId);
      if (!el) return;
      if (path) {
        const parts   = path.split('/');
        const subId   = parts[0];
        const fname   = parts[1];
        el.innerHTML  = `<a href="/api/submissions/${subId}/files/${fname}" target="_blank" rel="noopener">
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          View ${label}</a>`;
      } else {
        el.innerHTML = '<span style="color:var(--gray-400);font-size:0.82rem">Not uploaded</span>';
      }
    });

    // Show/hide action buttons based on status
    const pendingActions = document.getElementById('sv-pending-actions');
    if (s.status === 'Pending') {
      pendingActions.style.display = 'flex';
    } else {
      pendingActions.style.display = 'none';
    }

    // Show rejection reason if rejected
    if (s.status === 'Rejected' && s.rejectionReason) {
      document.getElementById('sv-rejection-wrap').style.display = '';
      document.getElementById('sv-rejection-reason').textContent = s.rejectionReason;
    }

    openModal('modal-submission-detail');
  } catch (e) {
    toast('Failed to load submission: ' + e.message, 'error');
  }
}

/** Sets value of a read-only input inside the modal. */
function sv(id, val) {
  const el = document.getElementById(id);
  if (el) el.value = val || '—';
}

/** Shows the inline reject reason input. */
function showRejectInput() {
  document.getElementById('sv-reject-input-wrap').style.display = '';
  document.getElementById('sv-reject-reason-input').focus();
}

/** Sends the Accept request for the currently-open submission. */
async function acceptCurrentSubmission() {
  if (!_currentSubmissionId) return;
  const btn = document.getElementById('sv-accept-btn');
  btn.disabled = true;
  btn.textContent = 'Processing…';
  try {
    const result = await api(`/api/submissions/${_currentSubmissionId}/accept`, 'POST', {});
    closeModal('modal-submission-detail');
    toast(`Accepted! New applicant ID: ${result.applicantId}`, 'success');
    loadSubmissions();
  } catch (e) {
    toast(e.message || 'Failed to accept submission', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Accept as Applicant';
  }
}

/** Sends the Reject request using the reason entered in the inline input. */
async function confirmRejectSubmission() {
  if (!_currentSubmissionId) return;
  const reason = document.getElementById('sv-reject-reason-input').value.trim();
  try {
    await api(`/api/submissions/${_currentSubmissionId}/reject`, 'POST', { reason });
    closeModal('modal-submission-detail');
    toast('Submission rejected.', 'success');
    loadSubmissions();
  } catch (e) {
    toast(e.message || 'Failed to reject submission', 'error');
  }
}

// ── INIT ──────────────────────────────────────────────────────
init();
