// Officer JS – calls real backend APIs for officer dashboard

// ─── Dashboard stats ───

async function loadOfficerDashboard() {
    const assigned = document.getElementById("assignedCount");
    if (!assigned) return;

    try {
        const stats = await apiRequest("/api/officer/dashboard");
        document.getElementById("assignedCount").textContent   = stats.assigned;
        document.getElementById("pendingCount").textContent     = stats.pending;
        document.getElementById("inProgressCount").textContent  = stats.inProgress;
        document.getElementById("resolvedCount").textContent    = stats.resolved;
        document.getElementById("rejectedCount").textContent    = stats.rejected;
    } catch (e) {
        console.error("Dashboard load failed:", e);
    }
}

// ─── Grievance list ───

async function loadOfficerGrievances() {
    const container = document.getElementById("grievanceList");
    if (!container) return;

    const statusEl   = document.getElementById("statusFilter");
    const priorityEl = document.getElementById("priorityFilter");
    const searchEl   = document.getElementById("searchInput");

    const status   = statusEl   ? statusEl.value   : "";
    const priority = priorityEl ? priorityEl.value : "";
    const search   = searchEl   ? searchEl.value   : "";

    let url = "/api/officer/grievances?";
    if (status)   url += "status="   + encodeURIComponent(status)   + "&";
    if (priority) url += "priority=" + encodeURIComponent(priority) + "&";
    if (search)   url += "search="   + encodeURIComponent(search)   + "&";

    try {
        const list = await apiRequest(url);

        if (list.length === 0) {
            container.innerHTML = '<p style="color:var(--muted)">No grievances assigned to you.</p>';
            return;
        }

        container.innerHTML = `
            <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Tracking ID</th>
                        <th>Title</th>
                        <th>Category</th>
                        <th>City</th>
                        <th>Priority</th>
                        <th>Status</th>
                        <th>Filed By</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${list.map(g => `
                        <tr>
                            <td>${g.trackingId}</td>
                            <td>${g.title}</td>
                            <td>${g.category}</td>
                            <td>${g.city}</td>
                            <td><span class="status status-${priorityClass(g.priority)}">${g.priority}</span></td>
                            <td><span class="status status-${statusClass(g.status)}">${formatStatus(g.status)}</span></td>
                            <td>${g.citizenName}</td>
                            <td>
                                ${canUpdate(g.status) ? `
                                <select onchange="updateOfficerGrievance('${g.trackingId}', this.value)">
                                    <option value="">Update…</option>
                                    <option value="IN_PROGRESS">In Progress</option>
                                    <option value="RESOLVED">Resolved</option>
                                    <option value="REJECTED">Rejected</option>
                                </select>` : '<span class="muted">Closed</span>'}
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
            </div>
        `;
    } catch (e) {
        container.innerHTML = '<p style="color:var(--red)">Failed to load: ' + e.message + '</p>';
    }
}

async function updateOfficerGrievance(trackingId, status) {
    if (!status) return;
    const remark = prompt("Add a remark (required, min 5 chars):", "");
    if (!remark || remark.trim().length < 5) {
        alert("Remark must be at least 5 characters.");
        return;
    }
    try {
        await apiRequest("/api/officer/grievances/" + trackingId + "/status", {
            method: "PUT",
            body: JSON.stringify({ status: status, remark: remark.trim() })
        });
        loadOfficerDashboard();
        loadOfficerGrievances();
    } catch (e) {
        alert("Update failed: " + e.message);
    }
}

function applyOfficerFilters() {
    loadOfficerGrievances();
}

// ─── Helpers ───

function canUpdate(status) {
    return status === "PENDING" || status === "IN_PROGRESS";
}

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

// Auto-load
loadOfficerDashboard();
loadOfficerGrievances();
