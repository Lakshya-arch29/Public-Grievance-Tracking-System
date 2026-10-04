// Citizen pages: dashboard, new grievance form, grievance detail.
// User text is always inserted with textContent (never innerHTML).

const STATUS_CLASS = {
    PENDING: "status-pending",
    IN_PROGRESS: "status-progress",
    RESOLVED: "status-resolved",
    REJECTED: "status-rejected"
};

const STATUS_TEXT = {
    PENDING: "Pending",
    IN_PROGRESS: "In Progress",
    RESOLVED: "Resolved",
    REJECTED: "Rejected"
};

const byId = (id) => document.getElementById(id);

function makeBadge(status) {
    const badge = document.createElement("span");
    badge.className = "status " + (STATUS_CLASS[status] || "");
    badge.textContent = STATUS_TEXT[status] || status;
    return badge;
}

function formatDate(iso) {
    return iso ? new Date(iso).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "-";
}

function addCell(row, content) {
    const td = document.createElement("td");
    if (content instanceof Node) {
        td.appendChild(content);
    } else {
        td.textContent = content;
    }
    row.appendChild(td);
}

// ---------------- Dashboard ----------------

async function loadSummary() {
    const s = await apiRequest("/api/citizen/summary");
    if (!s) return;
    byId("total").textContent = s.total;
    byId("pending").textContent = s.pending;
    byId("inProgress").textContent = s.inProgress;
    byId("resolved").textContent = s.resolved;
    byId("rejected").textContent = s.rejected;
}

async function loadList() {
    const box = byId("grievanceList");
    const status = byId("statusFilter").value;
    const rows = await apiRequest("/api/citizen/grievances" + (status ? "?status=" + status : ""));
    if (!rows) return;

    box.replaceChildren();
    if (rows.length === 0) {
        box.textContent = "No grievances found.";
        return;
    }

    const wrap = document.createElement("div");
    wrap.className = "table-wrap";
    const table = document.createElement("table");

    const head = table.insertRow();
    ["Tracking ID", "Title", "Category", "City", "Priority", "Status", "Filed on"].forEach((name) => {
        const th = document.createElement("th");
        th.textContent = name;
        head.appendChild(th);
    });

    rows.forEach((g) => {
        const row = table.insertRow();
        const link = document.createElement("a");
        link.href = "grievance.html?id=" + encodeURIComponent(g.trackingId);
        link.textContent = g.trackingId;
        addCell(row, link);
        addCell(row, g.title);
        addCell(row, g.category);
        addCell(row, g.city);
        addCell(row, g.priority);
        addCell(row, makeBadge(g.status));
        addCell(row, formatDate(g.createdAt));
    });

    wrap.appendChild(table);
    box.appendChild(wrap);
}

async function startDashboard() {
    try {
        await loadSummary();
        await loadList();
        byId("statusFilter").addEventListener("change", () => loadList().catch(showListError));
    } catch (error) {
        showListError(error);
    }
}

function showListError(error) {
    byId("grievanceList").textContent = error.message;
}

// ---------------- New grievance form ----------------

function fillSelect(select, items, placeholder) {
    select.replaceChildren(new Option(placeholder, ""));
    items.forEach((item) => select.appendChild(new Option(item.name, item.id)));
}

async function startForm() {
    const message = byId("message");

    try {
        const [categories, cities] = await Promise.all([
            apiRequest("/api/lookup/categories"),
            apiRequest("/api/lookup/cities")
        ]);
        fillSelect(byId("category"), categories, "Select category");
        fillSelect(byId("city"), cities, "Select city");
    } catch (error) {
        message.textContent = error.message;
        message.className = "msg-err";
    }

    byId("grievanceForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        message.className = "";
        message.textContent = "Submitting...";

        try {
            const result = await apiRequest("/api/citizen/grievances", {
                method: "POST",
                body: JSON.stringify({
                    title: byId("title").value,
                    categoryId: Number(byId("category").value),
                    cityId: Number(byId("city").value),
                    area: byId("area").value,
                    priority: byId("priority").value,
                    description: byId("description").value
                })
            });
            if (!result) return;

            const officer = result.assignedOfficer
                ? "Assigned to " + result.assignedOfficer.fullName + "."
                : "Awaiting assignment.";

            message.className = "msg-ok";
            message.textContent = "Grievance submitted. Tracking ID: " + result.trackingId + ". " + officer + " ";

            const link = document.createElement("a");
            link.href = "grievance.html?id=" + encodeURIComponent(result.trackingId);
            link.textContent = "View it";
            message.appendChild(link);

            byId("grievanceForm").reset();

        } catch (error) {
            message.className = "msg-err";
            message.textContent = error.message;
        }
    });
}

// ---------------- Grievance detail ----------------

function addField(parent, label, value) {
    const box = document.createElement("div");
    const small = document.createElement("small");
    small.textContent = label;
    const strong = document.createElement("strong");
    strong.textContent = value;
    box.append(small, strong);
    parent.appendChild(box);
}

async function startDetail() {
    const box = byId("detail");
    const id = new URLSearchParams(window.location.search).get("id");

    try {
        const g = await apiRequest("/api/citizen/grievances/" + encodeURIComponent(id));
        if (!g) return;

        box.replaceChildren();

        const title = document.createElement("h1");
        title.textContent = g.title;
        const sub = document.createElement("p");
        sub.className = "muted";
        sub.append(g.trackingId + "  ", makeBadge(g.status));
        box.append(title, sub);

        const grid = document.createElement("div");
        grid.className = "meta-grid";
        addField(grid, "Category", g.category);
        addField(grid, "City", g.city);
        addField(grid, "Area", g.area);
        addField(grid, "Priority", g.priority);
        addField(grid, "Filed on", formatDate(g.createdAt));
        addField(grid, "Officer", g.assignedOfficer
            ? g.assignedOfficer.fullName + (g.assignedOfficer.phone ? " (" + g.assignedOfficer.phone + ")" : "")
            : "Awaiting assignment");
        box.appendChild(grid);

        const desc = document.createElement("p");
        desc.className = "description";
        desc.textContent = g.description;
        box.appendChild(desc);

        const heading = document.createElement("h2");
        heading.textContent = "Progress";
        box.appendChild(heading);

        const timeline = document.createElement("ul");
        timeline.className = "timeline";
        g.history.forEach((h) => {
            const item = document.createElement("li");
            const top = document.createElement("div");
            top.appendChild(makeBadge(h.status));
            const remark = document.createElement("p");
            remark.textContent = h.remark;
            const who = document.createElement("small");
            who.textContent = h.changedBy + " \u00B7 " + formatDate(h.changedAt);
            item.append(top, remark, who);
            timeline.appendChild(item);
        });
        box.appendChild(timeline);

    } catch (error) {
        box.textContent = error.message;
    }
}

// Run the right part for the page that loaded this file
if (byId("grievanceList")) startDashboard();
if (byId("grievanceForm")) startForm();
if (byId("detail")) startDetail();
