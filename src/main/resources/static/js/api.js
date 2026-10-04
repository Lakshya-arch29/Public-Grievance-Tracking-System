// The only place that calls fetch for logged-in pages.
// Adds the token, turns errors into readable messages, and sends you to
// the home page when the session has expired (HTTP 401).

function errorText(data) {
    if (data && data.fieldErrors) {
        const first = Object.entries(data.fieldErrors)[0];
        if (first) {
            return first[0] + " " + first[1];
        }
    }
    return (data && data.message) || "Something went wrong";
}

async function apiRequest(url, options = {}) {

    const token = sessionStorage.getItem("token");

    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token,
            ...options.headers
        }
    });

    if (response.status === 401) {
        sessionStorage.clear();
        window.location.href = "../index.html";
        return;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
        throw new Error(errorText(data));
    }

    return data;
}
