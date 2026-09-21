// Admin dashboard fake data

const adminData = {
    total: 120,
    pending: 35,
    inProgress: 28,
    resolved: 45,
    overdue: 8,
    unassigned: 4
};


function loadDashboard() {

    const total = document.getElementById("totalGrievances");

    if (total) {

        document.getElementById("totalGrievances").textContent =
            adminData.total;

        document.getElementById("pendingGrievances").textContent =
            adminData.pending;

        document.getElementById("inProgressGrievances").textContent =
            adminData.inProgress;

        document.getElementById("resolvedGrievances").textContent =
            adminData.resolved;

        document.getElementById("overdueGrievances").textContent =
            adminData.overdue;

        document.getElementById("unassignedGrievances").textContent =
            adminData.unassigned;
    }
}


function loadGrievances() {

    const table = document.getElementById("grievanceTable");

    if (!table) {
        return;
    }

    table.innerHTML = `
        <tr>
            <td>GRV-00001</td>
            <td>Road Repair</td>
            <td>Infrastructure</td>
            <td>Mumbai</td>
            <td>HIGH</td>
            <td>PENDING</td>
            <td>Officer 1</td>
            <td>
                <button>View</button>
            </td>
        </tr>

        <tr>
            <td>GRV-00002</td>
            <td>Street Light</td>
            <td>Electricity</td>
            <td>Pune</td>
            <td>MEDIUM</td>
            <td>IN_PROGRESS</td>
            <td>Officer 2</td>
            <td>
                <button>View</button>
            </td>
        </tr>
    `;
}


function loadOfficers() {

    const table = document.getElementById("officerTable");

    if (!table) {
        return;
    }

    table.innerHTML = `
        <tr>
            <td>Officer One</td>
            <td>officer1</td>
            <td>officer1@example.com</td>
            <td>Mumbai</td>
            <td>Active</td>
            <td>5</td>
            <td>20</td>
            <td>
                <button>View</button>
            </td>
        </tr>

        <tr>
            <td>Officer Two</td>
            <td>officer2</td>
            <td>officer2@example.com</td>
            <td>Pune</td>
            <td>Active</td>
            <td>3</td>
            <td>15</td>
            <td>
                <button>View</button>
            </td>
        </tr>
    `;
}


function previousPage() {
    alert("Previous page");
}


function nextPage() {
    alert("Next page");
}


// Run dashboard automatically when dashboard.html opens

loadDashboard();