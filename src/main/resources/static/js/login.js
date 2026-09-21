
const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("loginMessage");

loginForm.addEventListener("submit", function(event) {

    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    // Fake login accounts
    const users = {
        admin: {
            password: "12345678",
            role: "admin",
            page: "admin/dashboard.html"
        },

        officer: {
            password: "12345678",
            role: "officer",
            page: "officer/dashboard.html"
        },

        citizen: {
            password: "12345678",
            role: "citizen",
            page: "citizen/dashboard.html"
        }
    };

    // Check if username exists and password is correct
    if (users[username] && users[username].password === password) {

        // Save fake login information
        sessionStorage.setItem("loggedIn", "true");
        sessionStorage.setItem("username", username);
        sessionStorage.setItem("role", users[username].role);

        // Open the correct dashboard
        window.location.href = users[username].page;

    } else {

        // Show error
        loginMessage.textContent = "Invalid username or password.";
        loginMessage.style.color = "#ff6b6b";
    }

});