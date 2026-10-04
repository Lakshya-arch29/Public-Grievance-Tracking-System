// Admin JS – calls real backend APIs instead of using fake data

// ─── Dashboard ───

async function loadDashboard() {
    const total = document.getElementById("totalGrievances");
    if (!total) return;

    try {
        const stats = await apiRequest("/api/admin/dashboard");
        document.getElementById("totalGrievances").textContent      = stats.total;
        document.getElementById("pendingGrievances").textContent     = stats.pending;
        document.getElementById("inProgressGrievances").textContent  = stats.inProgress;
        document.getElementById("resolvedGrievances").textContent    = stats.resolved;
        document.getElementById("overdueGrievances").textContent     = stats.overdue;
        document.getElementById("unassignedGrievances").textContent  = stats.unassigned;
    } catch (e) {
        console.error("Dashboard load failed:", e);
    }
}

// ─── Grievance list (admin/grievances.html) ───

async function loadGrievances() {
    const table = document.getElementById("grievanceTable");
    if (!table) return;

    const status   = document.getElementById("statusFilter")   ? document.getElementById("statusFilter").value   : "";
    const priority = document.getElementById("priorityFilter") ? document.getElementById("priorityFilter").value : "";
    const search   = document.getElementById("searchInput")    ? document.getElementById("searchInput").value    : "";

    let url = "/api/admin/grievances?";
    if (status)   url += "status="   + encodeURIComponent(status)   + "&";
    if (priority) url += "priority=" + encodeURIComponent(priority) + "&";
    if (search)   url += "search="   + encodeURIComponent(search)   + "&";

    try {
        const list = await apiRequest(url);

        if (list.length === 0) {
            table.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--muted)">No grievances found.</td></tr>';
            return;
        }

        table.innerHTML = list.map(g => `
            <tr>
                <td><a href="#" onclick="toggleHistory('${g.trackingId}'); return false;" style="text-decoration:none; font-weight:bold; color:var(--primary)">${g.trackingId} ▾</a></td>
                <td>${g.title}</td>
                <td>${g.category}</td>
                <td>${g.city}</td>
                <td><span class="status status-${priorityClass(g.priority)}">${g.priority}</span></td>
                <td><span class="status status-${statusClass(g.status)}">${formatStatus(g.status)}</span></td>
                <td>${g.officerName}</td>
                <td>
                    ${g.officerPhone ? `<a href="tel:${g.officerPhone}" style="margin-right:8px; text-decoration:none;" title="Call ${g.officerPhone}">📞</a>` : ''}
                    ${g.officerEmail ? `<a href="mailto:${g.officerEmail}" style="text-decoration:none;" title="Email ${g.officerEmail}">✉️</a>` : (g.officerName !== 'Unassigned' ? 'N/A' : '-')}
                </td>
            </tr>
            <tr id="history-${g.trackingId}" style="display:none; background:#fafafa;">
                <td colspan="8" style="padding:15px; border-top:none;">
                    <div style="color:var(--muted); font-size:0.9em;">Loading timeline...</div>
                </td>
            </tr>
        `).join("");
    } catch (e) {
        table.innerHTML = '<tr><td colspan="8" style="color:var(--red)">Failed to load: ' + e.message + '</td></tr>';
    }
}

async function toggleHistory(trackingId) {
    const row = document.getElementById("history-" + trackingId);
    if (!row) return;

    if (row.style.display === "none") {
        row.style.display = "table-row";
        try {
            const history = await apiRequest("/api/admin/grievances/" + trackingId + "/history");
            let html = "<ul style='margin:0; padding-left:25px; font-size:0.95em; text-align:left; line-height:1.6;'>";
            history.forEach(h => {
                html += `<li style="margin-bottom:8px;">
                            <strong>${h.changedAt.replace('T', ' ').substring(0, 16)}</strong> &mdash; 
                            <span class="status status-${statusClass(h.status)}">${formatStatus(h.status)}</span> 
                            by <strong>${h.changedBy || 'System'}</strong>
                            <br><span style="color:#555; display:inline-block; margin-top:2px;">${h.remark}</span>
                         </li>`;
            });
            html += "</ul>";
            row.cells[0].innerHTML = html;
        } catch (e) {
            row.cells[0].innerHTML = '<div style="color:var(--red);">Failed to load timeline: ' + e.message + '</div>';
        }
    } else {
        row.style.display = "none";
    }
}

// ─── Officer list (admin/officers.html) ───

async function loadOfficers() {
    const table = document.getElementById("officerTable");
    if (!table) return;

    const search = document.getElementById("officerSearch") ? document.getElementById("officerSearch").value : "";
    const active = document.getElementById("activeFilter")  ? document.getElementById("activeFilter").value  : "";

    let url = "/api/admin/officers?";
    if (search) url += "search=" + encodeURIComponent(search) + "&";
    if (active) url += "active=" + encodeURIComponent(active) + "&";

    try {
        const list = await apiRequest(url);

        if (list.length === 0) {
            table.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--muted)">No officers found.</td></tr>';
            return;
        }

        table.innerHTML = list.map(o => `
            <tr>
                <td>${o.fullName}</td>
                <td>${o.username}</td>
                <td>${o.email}</td>
                <td>${o.city}</td>
                <td>
                    <span class="status ${o.active ? 'status-resolved' : 'status-rejected'}">
                        ${o.active ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td>${o.pending}</td>
                <td>${o.resolved}</td>
                <td>
                    <button onclick="toggleOfficer(${o.userId}, ${!o.active})" style="font-size:.82rem">
                        ${o.active ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch (e) {
        table.innerHTML = '<tr><td colspan="8" style="color:var(--red)">Failed to load: ' + e.message + '</td></tr>';
    }
}

async function toggleOfficer(userId, active) {
    try {
        await apiRequest("/api/admin/officers/" + userId + "/active", {
            method: "PUT",
            body: JSON.stringify({ active: active })
        });
        loadOfficers();
    } catch (e) {
        alert("Toggle failed: " + e.message);
    }
}

// ─── Create Officer form ───

const officerForm = document.getElementById("officerForm");
if (officerForm) {
    // Load city dropdown
    (async function () {
        try {
            const cities = await fetch("/api/lookup/cities").then(r => r.json());

            const citySelect = document.getElementById("officerCity");

            if (citySelect) {
                citySelect.innerHTML = '<option value="">Select city</option>' +
                    cities.map(c => `<option value="${c.id}">${c.name}</option>`).join("");
            }
        } catch (e) {
            console.error("Failed to load lookups:", e);
        }
    })();

    officerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const msg = document.getElementById("officerMessage");

        try {
            await apiRequest("/api/admin/officers", {
                method: "POST",
                body: JSON.stringify({
                    fullName:   document.getElementById("officerName").value.trim(),
                    username:   document.getElementById("username").value.trim(),
                    email:      document.getElementById("email").value.trim(),
                    phone:      document.getElementById("phone").value.trim(),
                    password:   document.getElementById("password").value,
                    cityId:     parseInt(document.getElementById("officerCity").value)
                })
            });

            if (msg) {
                msg.textContent = "Officer created successfully!";
                msg.className = "msg-ok";
            }
            officerForm.reset();
            loadOfficers();
        } catch (e) {
            if (msg) {
                msg.textContent = e.message;
                msg.className = "msg-err";
            }
        }
    });
}

// ─── Helpers ───

function statusClass(s) {
    if (s === "PENDING")     return "pending";
    if (s === "IN_PROGRESS") return "progress";
    if (s === "RESOLVED")    return "resolved";
    if (s === "REJECTED")    return "rejected";
    return "";
}

function priorityClass(p) {
    if (p === "HIGH")   return "rejected";
    if (p === "MEDIUM") return "pending";
    if (p === "LOW")    return "resolved";
    return "";
}

function formatStatus(s) {
    return s.replace("_", " ");
}

function previousPage() { /* pagination placeholder */ }
function nextPage()     { /* pagination placeholder */ }

// Auto-load when pages open
loadDashboard();
loadOfficers();
loadGrievances();