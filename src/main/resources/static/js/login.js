const params = new URLSearchParams(window.location.search);
const role = (params.get("role") || "citizen").toLowerCase();

const titles = { citizen: "Citizen", officer: "Officer", admin: "Admin" };
const pages = {
    citizen: "citizen/dashboard.html",
    officer: "officer/dashboard.html",
    admin: "admin/dashboard.html"
};

const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const message = document.getElementById("loginMessage");
const switchRow = document.getElementById("switchRow");
const switchLink = document.getElementById("switchLink");
const title = document.getElementById("roleTitle");

let showingRegister = false;

title.textContent = (titles[role] || "Citizen") + " Login";

// Only citizens can sign up. Officers are created by an admin.
if (role === "citizen") {
    switchRow.hidden = false;
}

function showMessage(text, ok) {
    message.textContent = text;
    message.style.color = ok ? "#1e9e6a" : "#d64545";
}

// Turns the API error JSON into one readable line
function errorText(data) {
    if (data && data.fieldErrors) {
        const first = Object.entries(data.fieldErrors)[0];
        if (first) {
            return first[0] + " " + first[1];
        }
    }
    return (data && data.message) || "Something went wrong";
}

async function post(url, body) {
    const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(errorText(data));
    }
    return data;
}

switchLink.addEventListener("click", function (event) {
    event.preventDefault();
    showingRegister = !showingRegister;
    loginForm.hidden = showingRegister;
    registerForm.hidden = !showingRegister;
    title.textContent = showingRegister ? "Create Citizen Account" : "Citizen Login";
    switchLink.textContent = showingRegister ? "Already registered? Login" : "New citizen? Create an account";
    showMessage("", true);
});

loginForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    try {
        const data = await post("/api/auth/login", {
            username: document.getElementById("username").value.trim(),
            password: document.getElementById("password").value,
            role: role.toUpperCase()
        });

        sessionStorage.setItem("token", data.token);
        sessionStorage.setItem("role", data.user.role.toLowerCase());
        sessionStorage.setItem("fullName", data.user.fullName);
        sessionStorage.setItem("username", data.user.username);

        window.location.href = pages[data.user.role.toLowerCase()];

    } catch (error) {
        showMessage(error.message, false);
    }
});

registerForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    try {
        const username = document.getElementById("regUsername").value.trim();

        await post("/api/auth/register", {
            fullName: document.getElementById("regName").value.trim(),
            username: username,
            email: document.getElementById("regEmail").value.trim(),
            phone: document.getElementById("regPhone").value.trim(),
            password: document.getElementById("regPassword").value
        });

        // Back to the login form with the new username filled in
        switchLink.click();
        document.getElementById("username").value = username.toLowerCase();
        document.getElementById("password").value = "";
        showMessage("Account created. Please login.", true);

    } catch (error) {
        showMessage(error.message, false);
    }
});
