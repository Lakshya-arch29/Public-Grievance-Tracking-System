async function logout() {
    const token = sessionStorage.getItem("token");

    // Tell the server to delete the token; leave anyway if this fails
    try {
        await fetch("/api/auth/logout", {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });
    } catch (error) {
        // ignore
    }

    sessionStorage.clear();
    window.location.href = "../index.html";
}
