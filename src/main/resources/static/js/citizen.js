document.getElementById("total").textContent = "5";
document.getElementById("pending").textContent = "2";
document.getElementById("inProgress").textContent = "1";
document.getElementById("resolved").textContent = "1";
document.getElementById("rejected").textContent = "1";const form = document.getElementById("grievanceForm");

if (form) {

    form.addEventListener("submit", async function(event) {

        event.preventDefault();

        const grievance = {

            title: document.getElementById("title").value,

            categoryId:
                Number(document.getElementById("category").value),

            cityId:
                Number(document.getElementById("city").value),

            area:
                document.getElementById("area").value,

            priority:
                document.getElementById("priority").value,

            description:
                document.getElementById("description").value
        };

        try {

            const result = await apiRequest(
                "/api/citizen/grievances",
                {
                    method: "POST",
                    body: JSON.stringify(grievance)
                }
            );

            document.getElementById("message").textContent =
                "Grievance submitted. Tracking ID: " +
                result.trackingId;

        } catch (error) {

            document.getElementById("message").textContent =
                error.message;
        }

    });
}