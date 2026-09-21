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

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "Something went wrong");
    }

    return data;
}