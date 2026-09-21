const params = new URLSearchParams(window.location.search);

const role = params.get("role");

console.log(role);
const roleTitle = document.getElementById("roleTitle");

if (role === "citizen") {
    roleTitle.textContent = "Citizen Login";
}

if (role === "officer") {
    roleTitle.textContent = "Officer Login";
}

if (role === "admin") {
    roleTitle.textContent = "Admin Login";
}