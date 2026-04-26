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
let _currCourses = {};
let _currYear    = { 'PRG-1001': 1, 'PRG-1002': 1 };
let _currSem     = { 'PRG-1001': 1, 'PRG-1002': 1 };

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
let _enrStudents = [];
let _currentStudentId = null;
let _scheduleCache = {};
let _currentStudent = null;
let _currentApplicantId = null;
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
  curriculum:     () => { loadCurriculum('PRG-1001'); },
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
      document.getElementById('hdr-sy').textContent = sem.semesterLabel;
      document.getElementById('dash-sem') && (document.getElementById('dash-sem').textContent = sem.semesterLabel);
      document.getElementById('enroll-sem-label') && (document.getElementById('enroll-sem-label').textContent = sem.semesterLabel);
      document.getElementById('my-sched-sub') && (document.getElementById('my-sched-sub').textContent = sem.semesterLabel);
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
  try {
    const selected = filterEl?.value;
    const url = selected ? `/api/enrollment?semester=${selected}` : '/api/enrollment';
    const data = await api(url);
    const tbody = document.getElementById('tbl-enrollment');
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
    document.getElementById('sd-personal-fields').innerHTML = readonlyField('First Name',s.firstName) + readonlyField('Last Name',s.lastName) + readonlyField('Middle Name',s.middleName) + readonlyField('Date of Birth',fmtDate(s.dateOfBirth)) + readonlyField('Gender',s.gender) + readonlyField('Nationality',s.nationality || '—') + readonlyField('Email',s.email) + readonlyField('Contact',s.contactNumber || '—') + readonlyField('Blood Type',s.bloodType || '—') + readonlyField('Medical Conditions',s.medicalConditions || 'None');
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

    gotoPage('student-detail', null);
  } catch (e) { toast('Failed to load student record', 'error'); }
}

function readonlyField(label, value) {
  // SECURITY (A03): Escape value before injecting into HTML attribute
  return `<div class="field"><label>${escHtml(label)}</label><input value="${escHtml(value || '—')}" readonly/></div>`;
}

async function loadAlumni() {
  try {
    const data = await api('/api/alumni');
    const tbody = document.getElementById('tbl-alumni');
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

async function loadCurriculum(programId) {
  try {
    const data = await api(`/api/curriculum/courses?program=${programId}`);
    data.forEach(c => { _courseMap[c.courseId] = c; });
    _currCourses[programId] = data;
    renderCurriculumTable(programId);
  } catch (e) { console.error(e); }
}

function renderCurriculumTable(programId) {
  const tbodyId = programId === 'PRG-1001' ? 'tbl-curr-philo' : 'tbl-curr-theo';
  const tbody = document.getElementById(tbodyId);
  const year  = _currYear[programId] || 1;
  const sem   = _currSem[programId]  || 1;
  const rows  = (_currCourses[programId] || []).filter(c => c.yearLevel === year && c.semesterNumber === sem);
  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><p>No courses for this semester.</p></div></td></tr>';
    return;
  }
  tbody.innerHTML = rows.map(c =>
    `<tr>
      <td>${escHtml(c.courseCode)}</td>
      <td>${escHtml(c.courseName)}</td>
      <td>${c.units}</td>
      <td>${c.prerequisites?.length ? c.prerequisites.map(p => badge(p.prerequisiteCourse?.courseCode,'warn')).join(' ') : 'None'}</td>
      <td style="white-space:nowrap">
        <button class="btn btn-outline btn-sm registrar-only" onclick="openCourseModal('${escHtml(c.courseId)}')">Edit</button>
        <button class="btn btn-danger btn-sm registrar-only" onclick="deleteCourse('${escHtml(c.courseId)}')">Delete</button>
      </td>
    </tr>`
  ).join('');
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
  loadCurriculum(programId);
}

async function loadSections() {
  try {
    const data = await api('/api/sections');
    const tbody = document.getElementById('tbl-sections');
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="8"><div class="empty-state"><p>No sections found.</p></div></td></tr>'; return; }
    tbody.innerHTML = data.map(s =>
      `<tr>
        <td>${escHtml(s.sectionId)}</td><td>${escHtml(s.sectionCode)}</td><td>${escHtml(s.sectionName)}</td>
        <td>${escHtml(s.program?.programCode)}</td><td>${s.yearLevel}</td>
        <td>${escHtml(s.semester?.semesterLabel || '—')}</td><td>${s.capacity}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-outline btn-sm registrar-only" onclick='openSectionModal(${JSON.stringify(s)})'>Edit</button>
          <button class="btn btn-danger btn-sm registrar-only" onclick="deleteSection('${escHtml(s.sectionId)}')">Delete</button>
        </td>
      </tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadSchedule() {
  try {
    const sections = await api('/api/sections');
    const sel = document.getElementById('sched-section-filter');
    sel.innerHTML = '<option value="">Select section…</option>' +
      sections.map(s => `<option value="${s.sectionId}">${s.sectionName}</option>`).join('');
  } catch (_) {}
  try {
    const data = await api('/api/schedule');
    _scheduleCache = {};
    data.forEach(s => { _scheduleCache[s.scheduleId] = s; });
    const tbody = document.getElementById('tbl-schedule');
    tbody.innerHTML = data.map(s =>
      `<tr>
        <td>${escHtml(s.section?.sectionCode)}</td>
        <td>${escHtml(s.course?.courseCode)} – ${escHtml(s.course?.courseName)}</td>
        <td>${escHtml(s.instructor?.firstName)} ${escHtml(s.instructor?.lastName)}</td>
        <td>${escHtml(s.room?.roomName)}</td>
        <td>${escHtml(s.dayOfWeek)}</td>
        <td>${escHtml(s.timeStart)} – ${escHtml(s.timeEnd)}</td>
        <td style="display:flex;gap:6px">
          <button class="btn btn-outline btn-sm registrar-only" onclick="openEditSchedModal('${escHtml(s.scheduleId)}')">Edit</button>
          <button class="btn btn-danger btn-sm registrar-only" onclick="confirmDeleteSchedule('${escHtml(s.scheduleId)}')">Delete</button>
        </td>
      </tr>`
    ).join('') || '<tr><td colspan="7" style="text-align:center;color:var(--gray-400)">No schedules yet</td></tr>';
  } catch (e) { console.error(e); }
}

function loadScheduleGrid() {
  const sectionId = document.getElementById('sched-section-filter')?.value;
  if (!sectionId) return;
  // Clear cells
  document.querySelectorAll('[id^="gc-"]').forEach(c => c.innerHTML = '');
  api(`/api/schedule?section=${sectionId}`).then(data => {
    data.forEach((s, i) => {
      const hour = parseInt(s.timeStart?.split(':')[0] || '7');
      const dayMap = { Monday:'mon', Tuesday:'tue', Wednesday:'wed', Thursday:'thu', Friday:'fri' };
      const day = dayMap[s.dayOfWeek];
      if (!day) return;
      const slots = [7,9,11,13,15];
      const slot = slots.reduce((a,b) => Math.abs(b-hour) < Math.abs(a-hour) ? b : a);
      const cell = document.getElementById(`gc-${day}-${slot}`);
      if (cell) {
        cell.innerHTML = `<div class="sched-item" style="background:${SCHED_COLORS[i%SCHED_COLORS.length]};color:white">
          <div class="si-course">${s.course?.courseCode}</div>
          <div class="si-room">${s.room?.roomName}</div>
        </div>`;
      }
    });
  }).catch(console.error);
}

// Student list for grade search — loaded once, reused on every keystroke
let _gradeStudents = [];
let _selectedGradeStudentId = null;

async function loadGrades() {
  try {
    // Populate semester dropdown on first load
    const semEl = document.getElementById('grade-filter-sem');
    if (semEl && semEl.options.length <= 1) {
      try {
        const semesters = await api('/api/school-years/semesters');
        semesters.forEach(s => {
          const opt = document.createElement('option');
          opt.value = s.semesterId;
          opt.textContent = s.semesterLabel;
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
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>No instructors found.</p></div></td></tr>'; return; }
    _instructorCache = {};
    data.forEach(i => { _instructorCache[i.instructorId] = i; });
    tbody.innerHTML = data.map(i =>
      `<tr><td>${escHtml(i.instructorId)}</td><td>${escHtml(i.firstName)} ${escHtml(i.lastName)}</td><td>${escHtml(i.email || '—')}</td><td>${escHtml(i.specialization || '—')}</td>
      <td>${badge(i.isActive ? 'Active' : 'Inactive', i.isActive ? 'success' : 'gray')}</td>
      <td><button class="btn btn-outline btn-sm registrar-only" onclick="editInstructor('${escHtml(i.instructorId)}')">Edit</button></td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadRooms() {
  try {
    const data = await api('/api/sections/rooms');
    const tbody = document.getElementById('tbl-rooms');
    if (!data.length) { tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><p>No rooms found.</p></div></td></tr>'; return; }
    tbody.innerHTML = data.map(r =>
      `<tr><td>${r.roomId}</td><td>${r.roomName}</td><td>${r.building || '—'}</td><td>${r.capacity || '—'}</td>
      <td>${badge(r.isActive ? 'Active' : 'Inactive', r.isActive ? 'success' : 'gray')}</td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function loadUsers() {
  try {
    const data = await api('/api/users');
    const tbody = document.getElementById('tbl-users');
    tbody.innerHTML = data.map(u =>
      `<tr>
        <td>${escHtml(u.userId)}</td><td>${escHtml(u.username)}</td>
        <td>${badge(u.role, u.role==='Registrar' ? 'info' : 'gray')}</td>
        <td>${badge(u.isActive ? 'Active' : 'Inactive', u.isActive ? 'success' : 'danger')}</td>
        <td>
          <button class="btn btn-outline btn-sm" onclick="toggleUser('${escHtml(u.userId)}')">Toggle</button>
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
    tbody.innerHTML = data.map(s =>
      `<tr>
        <td>${s.semesterId}</td><td>${s.semesterLabel}</td>
        <td>${fmtDate(s.startDate)}</td><td>${fmtDate(s.endDate)}</td>
        <td>${badge(s.isActive ? 'Active' : 'Inactive', s.isActive ? 'success' : 'gray')}</td>
        <td>${!s.isActive ? `<button class="btn btn-success btn-sm" onclick="activateSem('${s.semesterId}')">Set Active</button>` : '<span style="color:var(--gray-400);font-size:.8rem">Current</span>'}</td>
      </tr>`
    ).join('') || '<tr><td colspan="6" style="text-align:center;color:var(--gray-400)">No semesters found</td></tr>';
  } catch (e) { console.error(e); }
}

async function loadBackup() {
  try {
    const data = await api('/api/backup/log');
    if (!Array.isArray(data) || !data.length) return;
    const tbody = document.getElementById('tbl-backup');
    tbody.innerHTML = data.map(b =>
      `<tr><td>${fmtDate(b.backupDate)}</td><td>${b.backupType}</td><td>${b.performedBy?.username || '—'}</td><td>${b.notes || '—'}</td></tr>`
    ).join('');
  } catch (_) {}
}

// ── ADMIN PAGE LOADERS ────────────────────────────────────────

let _auditPage = 0;

async function loadAuditLog(page) {
  _auditPage = (page !== undefined) ? page : 0;
  try {
    const data = await api(`/api/admin/audit-logs?page=${_auditPage}&size=50`);
    const tbody = document.getElementById('tbl-audit-log');
    if (!tbody) return;
    const actionColors = { CREATE: '#2d7d46', UPDATE: '#b45309', DELETE: '#c0392b', LOGIN: '#1d4ed8', LOGIN_FAILED: '#7c3aed' };
    tbody.innerHTML = (data.items || []).map(e => {
      const color = actionColors[e.action] || '#374151';
      return `<tr>
        <td style="white-space:nowrap;font-size:.8rem">${fmtDate(e.timestamp)}</td>
        <td>${escHtml(e.performedBy)}</td>
        <td><span class="badge">${escHtml(e.role)}</span></td>
        <td><span style="font-weight:600;color:${color}">${escHtml(e.action)}</span></td>
        <td>${escHtml(e.entityType)}</td>
        <td style="max-width:300px;font-size:.82rem">${escHtml(e.detail || '—')}</td>
        <td style="font-size:.8rem;color:var(--gray-400)">${escHtml(e.ipAddress || '—')}</td>
      </tr>`;
    }).join('') || '<tr><td colspan="7" style="text-align:center;color:var(--gray-400);padding:20px">No audit log entries yet.</td></tr>';

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

async function loadMyGrades() {
  try {
    const me = await api('/api/students/me');
    if (me) {
      document.getElementById('my-grades-sub').textContent =
        `${me.studentId} · ${me.firstName} ${me.lastName} · ${me.program?.programName} · Year ${me.currentYearLevel}`;
    }
    const grades = await api('/api/grades/student/me');
    document.getElementById('my-grades-sem').textContent = SMS.activeSemester?.semesterLabel || 'Grades';
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
    // GWA
    const gwaEl = document.getElementById('my-gwa');
    const validGrades = grades.filter(g => g.finalRating != null);
    if (validGrades.length) {
      const gwa = (validGrades.reduce((sum,g) => sum + g.finalRating, 0) / validGrades.length).toFixed(2);
      gwaEl.textContent = gwa;
    }
  } catch (e) { console.error(e); }
}

async function loadMySchedule() {
  try {
    const data = await api('/api/schedule/mine');
    // Clear
    document.querySelectorAll('[id^="ms-"]').forEach(c => c.innerHTML = '');
    data.forEach((s, i) => {
      const hour = parseInt(s.timeStart?.split(':')[0] || '7');
      const dayMap = { Monday:'mon', Tuesday:'tue', Wednesday:'wed', Thursday:'thu', Friday:'fri' };
      const day = dayMap[s.dayOfWeek];
      if (!day) return;
      const slots = [7,9,11,13];
      const slot = slots.reduce((a,b) => Math.abs(b-hour) < Math.abs(a-hour) ? b : a);
      const cell = document.getElementById(`ms-${day}-${slot}`);
      if (cell) {
        cell.innerHTML = `<div class="sched-item" style="background:${SCHED_COLORS[i%SCHED_COLORS.length]};color:white">
          <div class="si-course">${s.course?.courseCode}</div>
          <div class="si-room">${s.room?.roomName}</div>
        </div>`;
      }
    });
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
        readonlyField('Date of Birth', fmtDate(s.dateOfBirth)) + readonlyField('Gender', s.gender) +
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
      gender:           document.getElementById('ap-gender').value || null,
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
  document.getElementById('st-gender').value     = s.gender || '';
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
  document.getElementById('st-religion').value   = s.religion || '';
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
    gender:           document.getElementById('st-gender').value,
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
      viewStudent(id);
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
      closeEnrStudentDropdown();
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

let _enrollTab = 'applicant';

function switchEnrollTab(tab) {
  _enrollTab = tab;
  document.getElementById('enr-panel-applicant').style.display = tab === 'applicant' ? '' : 'none';
  document.getElementById('enr-panel-student').style.display   = tab === 'student'   ? '' : 'none';
  document.getElementById('enr-tab-applicant').style.background = tab === 'applicant' ? 'var(--navy)' : 'white';
  document.getElementById('enr-tab-applicant').style.color      = tab === 'applicant' ? 'white' : 'var(--gray-600)';
  document.getElementById('enr-tab-student').style.background  = tab === 'student'   ? 'var(--navy)' : 'white';
  document.getElementById('enr-tab-student').style.color       = tab === 'student'   ? 'white' : 'var(--gray-600)';
}

async function openEnrollModal() {
  _enrollTab = 'applicant';
  switchEnrollTab('applicant');

  // Reset fields
  const searchEl = document.getElementById('enr-student-search');
  const hiddenEl = document.getElementById('enr-student');
  if (searchEl) searchEl.value = '';
  if (hiddenEl) hiddenEl.value = '';
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
      opt.dataset.programId = a.programId;
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

  // Shared: semesters and sections
  try {
    const sems = await api('/api/school-years/semesters');
    populateSelect('enr-semester', sems, 'semesterId', s => s.semesterLabel, '');
    populateSelect('enr-app-semester', sems, 'semesterId', s => s.semesterLabel, '');
    if (SMS.activeSemester) {
      document.getElementById('enr-semester').value     = SMS.activeSemester.semesterId;
      document.getElementById('enr-app-semester').value = SMS.activeSemester.semesterId;
    }
  } catch (_) {}
  try {
    const sections = await api('/api/sections');
    populateSelect('enr-section',     sections, 'sectionId', s => s.sectionName, 'No section');
    populateSelect('enr-app-section', sections, 'sectionId', s => s.sectionName, 'No section');
  } catch (_) {}

  openModal('modal-enroll');
}

async function saveEnrollment() {
  if (_enrollTab === 'applicant') {
    const applicantId = document.getElementById('enr-applicant').value;
    if (!applicantId) { toast('Please select an admitted applicant', 'error'); return; }
    if (!validateRequired([
      {id:'enr-app-program',  label:'Program'},
      {id:'enr-app-semester', label:'Semester'},
      {id:'enr-app-year',     label:'Year Level'},
    ])) return;
  } else {
    if (!validateRequired([
      {id:'enr-student',  label:'Student'},
      {id:'enr-program',  label:'Program'},
      {id:'enr-semester', label:'Semester'},
      {id:'enr-year',     label:'Year Level'},
    ])) return;
  }
  const btn = document.getElementById('btn-confirm-enroll');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Enrolling…'; }
  await new Promise(r => requestAnimationFrame(r));
  try {
    if (_enrollTab === 'applicant') {
      const applicantId = document.getElementById('enr-applicant').value;
      const result = await api('/api/enrollment', 'POST', {
        applicantId,
        programId:  document.getElementById('enr-app-program').value,
        yearLevel:  parseInt(document.getElementById('enr-app-year').value),
        sectionId:  document.getElementById('enr-app-section').value || null,
        semesterId: document.getElementById('enr-app-semester').value,
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
        semesterId: document.getElementById('enr-semester').value,
      });
      toast('Student enrolled successfully');
      closeModal('modal-enroll'); loadEnrollment();
    }
  } catch (e) { toast(e.message, 'error'); }
  finally { if (btn) { btn.disabled = false; btn.textContent = 'Confirm Enrollment'; } }
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
  recomputeGradeModal();
  openModal('modal-grade');
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

  // Auto-set status when both term grades are complete
  if (rating !== null) {
    const statusEl = document.getElementById('gr-status');
    if (statusEl.value !== 'Incomplete' && statusEl.value !== 'Dropped')
      statusEl.value = rating <= 3.0 ? 'Passed' : 'Failed';
  }
}

function openCourseModal(courseIdOrNull) {
  const c = courseIdOrNull ? _courseMap[courseIdOrNull] : null;
  document.getElementById('co-id').value        = c?.courseId       || '';
  document.getElementById('co-code').value      = c?.courseCode     || '';
  document.getElementById('co-name').value      = c?.courseName     || '';
  document.getElementById('co-units').value     = c?.units          || '';
  document.getElementById('co-program').value   = c?.program?.programId || 'PRG-1001';
  document.getElementById('co-year').value      = c?.yearLevel      || '1';
  document.getElementById('co-sem').value       = c?.semesterNumber || '1';
  document.getElementById('co-modal-title').textContent = c ? 'Edit Course' : 'Add Course';
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
  const id      = document.getElementById('co-id').value;
  const programId = document.getElementById('co-program').value;
  const payload = {
    courseCode:     document.getElementById('co-code').value,
    courseName:     document.getElementById('co-name').value,
    units:          parseInt(document.getElementById('co-units').value),
    program:        { programId },
    yearLevel:      parseInt(document.getElementById('co-year').value),
    semesterNumber: parseInt(document.getElementById('co-sem').value),
    isActive:       true,
  };
  try {
    if (id) {
      await api(`/api/curriculum/courses/${id}`, 'PUT', payload);
      toast('Course updated');
    } else {
      await api('/api/curriculum/courses', 'POST', payload);
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
  try {
    await api(`/api/curriculum/courses/${id}`, 'DELETE');
    toast('Course deleted');
    closeModal('modal-course-delete');
    loadCurriculum('PRG-1001');
    loadCurriculum('PRG-1002');
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
  const [sections, courses, instructors, rooms] = await Promise.all([
    api('/api/sections'), api('/api/curriculum/courses'),
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
    closeSchedModal(); loadSchedule();
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
    closeModal('modal-sched-delete'); loadSchedule();
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

async function saveRoom() {
  if (!validateRequired([
    {id:'rm-name', label:'Room Name'},
  ])) return;
  try {
    await api('/api/sections/rooms', 'POST', {
      roomId:    document.getElementById('rm-id').value,
      roomName:  document.getElementById('rm-name').value,
      building:  document.getElementById('rm-building').value,
      capacity:  parseInt(document.getElementById('rm-capacity').value) || null,
    });
    toast('Room saved'); closeModal('modal-room'); loadRooms();
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
    await api('/api/users', 'POST', {
      username: document.getElementById('usr-username').value,
      password: pw,
      role:     document.getElementById('usr-role').value,
    });
    toast('User account created'); closeModal('modal-user'); loadUsers();
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
    document.getElementById('hdr-sy').textContent = sem.semesterLabel;
    // reset enrollment filter so next visit defaults to the new active semester
    const enrollFilter = document.getElementById('enroll-filter-sem');
    if (enrollFilter) { enrollFilter.innerHTML = ''; }
  } catch (e) { toast(e.message, 'error'); }
}

async function saveSchoolYear() {
  if (!validateRequired([
    {id:'sy-label', label:'Year Label'},
  ])) return;
  try {
    await api('/api/school-years', 'POST', {
      schoolYearId: document.getElementById('sy-id').value,
      yearLabel:    document.getElementById('sy-label').value,
    });
    toast('School year saved'); closeModal('modal-school-year'); loadSchoolYears();
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

function openExamModal(applicantId, name) {
  _currentApplicantId = applicantId;
  document.getElementById('exam-modal-sub').textContent = `Recording exam for ${name}`;
  openModal('modal-exam');
}

async function saveExam() {
  if (!validateRequired([
    {id:'ex-date',   label:'Exam Date'},
    {id:'ex-result', label:'Result'},
  ])) return;
  try {
    await api(`/api/applicants/${_currentApplicantId}/exams`, 'POST', {
      examDate: document.getElementById('ex-date').value,
      score:    parseFloat(document.getElementById('ex-score').value) || null,
      maxScore: parseFloat(document.getElementById('ex-max').value) || 100,
      result:   document.getElementById('ex-result').value,
      remarks:  document.getElementById('ex-remarks').value,
    });
    toast('Exam recorded'); closeModal('modal-exam');
  } catch (e) { toast(e.message, 'error'); }
}

function openReportModal(type) {
  _currentReportType = type;
  document.getElementById('gen-report-title').textContent = type.replace(/([A-Z])/g, ' $1').trim();
  api('/api/students').then(students => {
    populateSelect('gen-report-student', students, 'studentId',
      s => `${s.firstName} ${s.lastName} (${s.studentId})`, 'All Students');
  }).catch(_=>{});
  api('/api/school-years/semesters').then(sems => {
    populateSelect('gen-report-sem', sems, 'semesterId', s => s.semesterLabel, '');
  }).catch(_=>{});
  openModal('modal-gen-report');
}

function generateReport() {
  toast('Report generation requires backend PDF/XLSX implementation. API endpoint ready at /api/reports.', 'info');
  closeModal('modal-gen-report');
}

function triggerBackup() {
  toast('Backup requires backend implementation via mysqldump. API endpoint: /api/backup/create', 'info');
}

async function viewApplicantDetail(id) {
  try {
    const a = await api(`/api/applicants/${id}`);
    _currentApplicantId = id;

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
    document.getElementById('apd-religion').value        = a.religion || '';

    // Load application status
    let currentStatus = 'Applied';
    try {
      const app = await api(`/api/applicants/${id}/application`);
      currentStatus = app?.applicationStatus || 'Applied';
      document.getElementById('apd-status').value = currentStatus;
      const label = document.querySelector(`#apd-status option[value="${currentStatus}"]`)?.textContent || currentStatus;
      document.getElementById('apd-status-view').textContent = label;
    } catch (_) {
      document.getElementById('apd-status').value = 'Applied';
      document.getElementById('apd-status-view').textContent = 'Applied';
    }

    // Show Admit button only when convention is done and not yet admitted
    document.getElementById('apd-admit-btn').style.display =
      currentStatus === 'AspiringConventionAttended' ? '' : 'none';

    // Load exams
    try {
      const exams = await api(`/api/applicants/${id}/exams`);
      document.getElementById('apd-exams-body').innerHTML = exams.length
        ? exams.map(e => `<tr><td>${escHtml(e.examDate || '—')}</td><td>${escHtml(e.score ?? '—')}</td><td>${escHtml(e.maxScore ?? 100)}</td><td>${badge(e.result)}</td><td>${escHtml(e.remarks || '—')}</td></tr>`).join('')
        : '<tr><td colspan="5" style="text-align:center;color:var(--gray-400,#9ca3af);padding:12px">No exams recorded</td></tr>';
    } catch (_) {}

    // Reset to first tab and view mode
    switchTab(document.querySelector('#modal-applicant-detail .tab'), 'apd-tab-personal', ['apd-tab-personal','apd-tab-family','apd-tab-exams']);
    applicantEditMode(false);
    openModal('modal-applicant-detail');
  } catch (e) { toast('Failed to load applicant', 'error'); }
}


function openAdmitModal() {
  const name = document.getElementById('apd-title').textContent;
  document.getElementById('admit-sub').textContent = `Admitting: ${name}`;
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
  const inputs = ['apd-fname','apd-lname','apd-mname','apd-dob','apd-email','apd-contact','apd-address','apd-school','apd-schoolyr','apd-schoollvl','apd-father','apd-father-occ','apd-mother','apd-mother-occ','apd-guardian','apd-guardian-contact','apd-nationality','apd-religion'];
  inputs.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.readOnly = !on;
  });
  document.getElementById('apd-level').disabled   = !on;
  document.getElementById('apd-program').disabled = !on;
  document.getElementById('apd-status-view').style.display = on ? 'none' : '';
  document.getElementById('apd-status').style.display      = on ? '' : 'none';
  document.getElementById('apd-edit-btn').style.display    = on ? 'none' : '';
  document.getElementById('apd-admit-btn').style.display   = on ? 'none' : (document.getElementById('apd-status').value === 'AspiringConventionAttended' ? '' : 'none');
  document.getElementById('apd-actions').style.display     = on ? '' : 'none';
  document.getElementById('apd-modal').classList.toggle('editing', on);
}

async function saveApplicantEdit() {
  if (!validateRequired([
    {id:'apd-fname', label:'First Name'},
    {id:'apd-lname', label:'Last Name'},
  ])) return;
  try {
    // Save status change if there's an application record
    try {
      const app = await api(`/api/applicants/${_currentApplicantId}/application`);
      if (app) {
        const status = document.getElementById('apd-status').value;
        await api(`/api/applicants/applications/${app.applicationId}/status?status=${status}`, 'PATCH');
      }
    } catch (_) {}

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
    // Update the status label in view mode
    const status = document.getElementById('apd-status').value;
    const label = document.querySelector(`#apd-status option[value="${status}"]`)?.textContent || status;
    document.getElementById('apd-status-view').textContent = label;
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
          <td>${escHtml(s.course?.courseName || '—')}</td>
          <td style="text-align:center">${escHtml(String(s.course?.units ?? '—'))}</td>
          <td>${badge(s.status)}</td>
        </tr>`).join('')
      : '<tr><td colspan="4" style="text-align:center;color:var(--gray-400);padding:20px">No subjects enrolled yet</td></tr>';
  } catch (_) { toast('Failed to load subjects', 'error'); }
}

async function toggleAddSubjectForm() {
  const form = document.getElementById('enrs-add-form');
  const isHidden = form.style.display === 'none';
  if (isHidden) {
    // Populate course select filtered by program
    try {
      const url = _currentEnrollmentPgm
        ? `/api/curriculum/courses?program=${_currentEnrollmentPgm}`
        : '/api/curriculum/courses';
      const courses = await api(url);
      const select = document.getElementById('enrs-course-select');
      select.innerHTML = '<option value="">Select course…</option>' +
        courses.map(c => `<option value="${escHtml(c.courseId)}">${escHtml(c.courseCode)} — ${escHtml(c.courseName)}</option>`).join('');
    } catch (_) { toast('Failed to load courses', 'error'); return; }
    form.style.display = 'block';
  } else {
    form.style.display = 'none';
  }
}

async function addEnrollmentSubject() {
  const courseId = document.getElementById('enrs-course-select').value;
  if (!courseId) { toast('Please select a course', 'error'); return; }
  try {
    await api(`/api/enrollment/${_currentEnrollmentId}/subjects`, 'POST', { courseId });
    toast('Subject added successfully');
    document.getElementById('enrs-add-form').style.display = 'none';
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
    sv('sv-gender',        s.gender || '—');
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
