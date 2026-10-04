// Page guard: loaded on every dashboard page.
// Sends the visitor back to the home page if they are not logged in,
// or if their role does not match the folder of the page (citizen / officer / admin).
(function () {
    const token = sessionStorage.getItem("token");
    const role = sessionStorage.getItem("role");
    const folder = window.location.pathname.split("/").slice(-2, -1)[0];

    if (!token || role !== folder) {
        sessionStorage.clear();
        window.location.href = "../index.html";
        return;
    }

    // Show who is logged in at the right of the top bar
    const bar = document.querySelector(".navbar");
    if (bar) {
        const who = document.createElement("span");
        who.className = "nav-user";
        who.textContent = sessionStorage.getItem("fullName") + " (" + role + ")";
        bar.appendChild(who);
    }
})();
