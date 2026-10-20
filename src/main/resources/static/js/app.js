/**
 * app.js - Student Management System Frontend Logic
 * Handles all CRUD operations, search, and UI interactions
 */

const API_BASE = '/api/students';
let allStudents = [];
let deleteTargetId = null;
let studentModal = null;
let deleteModal = null;
let toastEl = null;
let searchTimeout = null;

// ============================================================
// Initialization
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    studentModal = new bootstrap.Modal(document.getElementById('studentModal'));
    deleteModal  = new bootstrap.Modal(document.getElementById('deleteModal'));
    toastEl      = document.getElementById('toastEl');

    loadStudents();
    checkHealth();
    setInterval(checkHealth, 30000); // Re-check health every 30s

    // Live search with debounce
    document.getElementById('searchInput').addEventListener('input', (e) => {
        const val = e.target.value.trim();
        document.getElementById('searchClear').style.display = val ? 'block' : 'none';
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => performSearch(val), 300);
    });

    // Delegated handler for the Edit / Delete action buttons.
    // The student id travels in data-id and the name is read from allStudents,
    // so user-supplied text is never embedded inside a JavaScript string.
    document.getElementById('studentTableBody').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;

        const id = Number(btn.dataset.id);
        const student = allStudents.find(s => s.id === id);

        if (btn.dataset.action === 'edit') {
            openEditModal(id);
        } else if (btn.dataset.action === 'delete') {
            openDeleteModal(id, student ? student.fullName : 'this student');
        }
    });
});

// ============================================================
// Health Check (Spring Boot Actuator)
// ============================================================
async function checkHealth() {
    const badge = document.getElementById('healthBadge');
    const text  = badge.querySelector('.health-text');
    try {
        const res = await fetch('/actuator/health');
        const data = await res.json();
        if (data.status === 'UP') {
            badge.className = 'health-badge up';
            text.textContent = 'Healthy';
        } else {
            badge.className = 'health-badge down';
            text.textContent = 'Degraded';
        }
    } catch {
        badge.className = 'health-badge down';
        text.textContent = 'Offline';
    }
}

// ============================================================
// Load All Students
// ============================================================
async function loadStudents() {
    showTableLoading(true);
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) throw new Error('Failed to fetch students');
        allStudents = await res.json();
        renderTable(allStudents);
        updateStats(allStudents);
    } catch (err) {
        showTableError('Could not load students. Is the server running?');
        console.error(err);
    }
}

// ============================================================
// Render Table
// ============================================================
function renderTable(students) {
    const tbody  = document.getElementById('studentTableBody');
    const empty  = document.getElementById('emptyState');
    const table  = document.getElementById('studentTable');

    showTableLoading(false);

    if (!students || students.length === 0) {
        tbody.innerHTML = '';
        table.style.display = 'none';
        empty.style.display = 'block';
        return;
    }

    table.style.display = '';
    empty.style.display = 'none';

    tbody.innerHTML = students.map((s, i) => `
        <tr data-id="${s.id}">
            <td><span class="row-num">${i + 1}</span></td>
            <td><span class="student-name">${escapeHtml(s.fullName)}</span></td>
            <td><span class="email-text">${escapeHtml(s.email)}</span></td>
            <td><span class="dept-badge">${escapeHtml(s.department)}</span></td>
            <td><span class="sem-badge">Sem ${s.semester}</span></td>
            <td class="text-center">
                <div class="d-flex gap-2 justify-content-center">
                    <button class="action-btn btn-edit" data-action="edit" data-id="${s.id}" title="Edit">
                        <i class="bi bi-pencil-fill"></i>
                    </button>
                    <button class="action-btn btn-delete" data-action="delete" data-id="${s.id}" title="Delete">
                        <i class="bi bi-trash3-fill"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// ============================================================
// Stats
// ============================================================
function updateStats(students) {
    document.getElementById('totalStudents').textContent = students.length;

    const depts = new Set(students.map(s => s.department)).size;
    document.getElementById('totalDepts').textContent = depts;

    if (students.length > 0) {
        const avg = students.reduce((sum, s) => sum + s.semester, 0) / students.length;
        document.getElementById('avgSemester').textContent = avg.toFixed(1);
        document.getElementById('latestEntry').textContent =
            students[students.length - 1].fullName.split(' ')[0];
    } else {
        document.getElementById('avgSemester').textContent = '—';
        document.getElementById('latestEntry').textContent = '—';
    }
}

// ============================================================
// Search
// ============================================================
async function performSearch(query) {
    if (!query) {
        renderTable(allStudents);
        updateStats(allStudents);
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}`);
        const results = await res.json();
        renderTable(results);
    } catch (err) {
        console.error('Search failed:', err);
    }
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('searchClear').style.display = 'none';
    renderTable(allStudents);
    updateStats(allStudents);
}

// ============================================================
// Modal: Add Student
// ============================================================
function openAddModal() {
    resetForm();
    document.getElementById('studentModalLabel').textContent = 'Add New Student';
    document.getElementById('saveBtnText').innerHTML = '<i class="bi bi-check-lg me-1"></i>Save Student';
}

// ============================================================
// Modal: Edit Student
// ============================================================
async function openEditModal(id) {
    resetForm();
    document.getElementById('studentModalLabel').textContent = 'Edit Student';
    document.getElementById('saveBtnText').innerHTML = '<i class="bi bi-pencil me-1"></i>Update Student';

    try {
        const res = await fetch(`${API_BASE}/${id}`);
        if (!res.ok) throw new Error('Student not found');
        const s = await res.json();

        document.getElementById('studentId').value = s.id;
        document.getElementById('fullName').value = s.fullName;
        document.getElementById('email').value = s.email;
        document.getElementById('department').value = s.department;
        document.getElementById('semester').value = s.semester;

        studentModal.show();
    } catch (err) {
        showToast('Could not load student data.', 'error');
        console.error(err);
    }
}

// ============================================================
// Save Student (Create or Update)
// ============================================================
async function saveStudent() {
    clearFormErrors();

    const id = document.getElementById('studentId').value;
    const fullName   = document.getElementById('fullName').value.trim();
    const email      = document.getElementById('email').value.trim();
    const department = document.getElementById('department').value;
    const semester   = document.getElementById('semester').value;

    // Client-side validation
    let valid = true;
    if (!fullName || fullName.length < 2) {
        setFieldError('fullName', 'Full name must be at least 2 characters.');
        valid = false;
    }
    if (!email || !isValidEmail(email)) {
        setFieldError('email', 'Please enter a valid email address.');
        valid = false;
    }
    if (!department) {
        setFieldError('department', 'Please select a department.');
        valid = false;
    }
    if (!semester) {
        setFieldError('semester', 'Please select a semester.');
        valid = false;
    }
    if (!valid) return;

    const payload = { fullName, email, department, semester: parseInt(semester) };
    const isEdit = !!id;
    const url = isEdit ? `${API_BASE}/${id}` : API_BASE;
    const method = isEdit ? 'PUT' : 'POST';

    setSaveLoading(true);
    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok) {
            studentModal.hide();
            showToast(isEdit ? 'Student updated successfully!' : 'Student added successfully!', 'success');
            await loadStudents();
        } else if (res.status === 400 && data.errors) {
            // Server validation errors
            Object.entries(data.errors).forEach(([field, msg]) => setFieldError(field, msg));
        } else {
            document.getElementById('formError').style.display = 'block';
            document.getElementById('formError').textContent = data.message || 'An error occurred.';
        }
    } catch (err) {
        document.getElementById('formError').style.display = 'block';
        document.getElementById('formError').textContent = 'Network error. Please try again.';
        console.error(err);
    } finally {
        setSaveLoading(false);
    }
}

// ============================================================
// Delete Student
// ============================================================
function openDeleteModal(id, name) {
    deleteTargetId = id;
    document.getElementById('deleteStudentName').textContent = name;
    deleteModal.show();
}

async function confirmDelete() {
    if (!deleteTargetId) return;
    const btn = document.getElementById('confirmDeleteBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Deleting...';

    try {
        const res = await fetch(`${API_BASE}/${deleteTargetId}`, { method: 'DELETE' });
        if (res.ok) {
            deleteModal.hide();
            showToast('Student deleted successfully.', 'success');
            await loadStudents();
        } else {
            const data = await res.json();
            showToast(data.message || 'Delete failed.', 'error');
        }
    } catch (err) {
        showToast('Network error during delete.', 'error');
        console.error(err);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-trash3-fill me-1"></i>Delete';
        deleteTargetId = null;
    }
}

// ============================================================
// Utilities
// ============================================================
function resetForm() {
    document.getElementById('studentForm').reset();
    document.getElementById('studentId').value = '';
    clearFormErrors();
    document.getElementById('formError').style.display = 'none';
}

function clearFormErrors() {
    ['fullName','email','department','semester'].forEach(f => {
        const el = document.getElementById(f);
        if (el) el.classList.remove('is-invalid');
        const err = document.getElementById(f + 'Error');
        if (err) err.textContent = '';
    });
    document.getElementById('formError').style.display = 'none';
}

function setFieldError(field, message) {
    const el = document.getElementById(field);
    if (el) el.classList.add('is-invalid');
    const err = document.getElementById(field + 'Error');
    if (err) err.textContent = message;
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
              .replace(/"/g,'&quot;').replace(/'/g,'&#039;');
}

function showTableLoading(show) {
    const loading = document.getElementById('loadingRow');
    if (loading) loading.style.display = show ? '' : 'none';
}

function showTableError(message) {
    document.getElementById('studentTableBody').innerHTML = `
        <tr><td colspan="6" class="text-center py-5 text-danger">
            <i class="bi bi-exclamation-circle fs-3 d-block mb-2"></i>${escapeHtml(message)}
        </td></tr>`;
}

function setSaveLoading(loading) {
    const btn     = document.getElementById('saveBtn');
    const text    = document.getElementById('saveBtnText');
    const spinner = document.getElementById('saveBtnSpinner');
    btn.disabled = loading;
    text.classList.toggle('d-none', loading);
    spinner.classList.toggle('d-none', !loading);
}

function showToast(message, type = 'success') {
    const toastInstance = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 3500 });
    const body = document.getElementById('toastBody');
    body.textContent = message;
    toastEl.className = `toast align-items-center border-0 text-white bg-${type === 'success' ? 'success' : 'danger'}`;
    toastInstance.show();
}
