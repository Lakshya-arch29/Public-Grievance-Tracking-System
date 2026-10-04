# Frontend Guide

*Screens, navigation, JavaScript files and UI conventions*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

## 1. Overview

The frontend is plain HTML5, CSS3 and vanilla JavaScript using the fetch API. There is no Node.js, no build step and no CORS setup: every file sits in Spring Boot's `static` folder, so one server on one port (8080) serves both the pages and the API.

The UI developer builds against the API contract in the Backend Guide and can start with fake responses before the backend exists.

### Folder layout

```text
src/main/resources/static/
|-- index.html            role picker (landing page)
|-- login.html            login and sign up, role passed in the query string
|-- citizen/              dashboard.html, new-grievance.html, grievance.html
|-- officer/              dashboard.html
|-- admin/                dashboard.html, grievances.html, officers.html
|-- css/style.css
`-- js/                   api.js, auth.js, ui.js, citizen.js, officer.js, admin.js
```

### JavaScript files and their jobs

| File | Responsibility |
|---|---|
| api.js | The only place that calls fetch. Adds the Authorization header, turns error responses into a readable message, and on HTTP 401 clears the token and redirects to index.html. |
| auth.js | Loaded by every page except index.html and login.html. Redirects to index.html when there is no valid token, or when the logged-in role does not match the page's folder. |
| ui.js | Shared helpers: status and priority badges, loading, empty and error states, date formatting, safe text insertion. |
| citizen.js | Citizen dashboard, new grievance form and grievance timeline. |
| officer.js | Officer dashboard, filters, detail panel and status update. |
| admin.js | Admin dashboard, grievance table with pagination, reassignment, officer directory and officer forms. |

## 2. Screens and navigation

| Screen | File | Role | API calls |
|---|---|---|---|
| Role picker (landing) | index.html | Public | none |
| Login and Sign up | login.html?role=citizen\|officer\|admin | Public | POST /api/auth/login, POST /api/auth/register |
| Citizen dashboard | citizen/dashboard.html | Citizen | GET /api/citizen/summary, GET /api/citizen/grievances |
| New grievance | citizen/new-grievance.html | Citizen | GET /api/lookup/categories, GET /api/lookup/cities, POST /api/citizen/grievances |
| Grievance detail and timeline | citizen/grievance.html?id=GRV-00037 | Citizen | GET /api/citizen/grievances/{trackingId} |
| Officer dashboard | officer/dashboard.html | Officer | GET /api/officer/summary, GET /api/officer/grievances, GET /api/officer/grievances/{id}, PUT /api/officer/grievances/{id}/status |
| Admin dashboard | admin/dashboard.html | Admin | GET /api/admin/summary, GET /api/admin/stats/cities |
| Admin grievances | admin/grievances.html | Admin | GET /api/admin/grievances, GET /api/admin/grievances/{id}, PUT /api/admin/grievances/{id}/assign, GET /api/admin/officers |
| Admin officers | admin/officers.html | Admin | GET /api/admin/officers, GET /api/admin/officers/{id}, POST /api/admin/officers, PATCH /api/admin/officers/{id}/active, GET /api/admin/stats/cities |

### Public pages

- The landing page shows three role cards: Citizen, Officer and Admin. No statistics are shown to visitors.
- The Citizen card leads to Login and Sign up tabs. Officer and Admin cards lead to Login only.
- The role card is a convenience. The server checks the account's real role, and a mismatch shows the message returned by the API (for example 'This account is not an Admin account').

### Citizen screens

- Dashboard: counts (total, pending, in progress, resolved, rejected) and a filterable list of the citizen's own grievances.
- New grievance form: title, category (12 options from the lookup API), city (dropdown from the lookup API), area or ward, priority and description. After submit, show the tracking ID and the assigned officer's name, or the note 'awaiting assignment' when the API returns assignedOfficer as null.
- Grievance detail: description, current status and a timeline of every history entry with its remark, who changed it and when.

### Officer screen

- Summary cards: assigned, pending, in progress, resolved and overdue.
- Table of assigned grievances with filters for status and priority, plus a text search box (searches title and area).
- Opening a grievance shows the description and the citizen's contact details.
- A status form offers In Progress, Resolved or Rejected with a mandatory remark. Hide or disable options that the status flow does not allow.

### Admin screens

- Dashboard: totals per status, overdue count, unassigned count, grievances per category, and cities ranked by pending cases.
- All grievances: table with filters (status, category, city, unassigned only), search and pagination. A reassign control lists only active officers.
- Officers: the directory described below, plus an Add officer form and an enable or disable control.

## 3. Admin Officers screen design

```text
+- Admin - Officers ----------------------------------------------------------+
| City [All v]  Category [All v]  Active [All v]  Sort [Most pending v]        |
| Search [name / username________]                                             |
|                                                                              |
| Top cities by pending:  Pune 42 ####  Nagpur 31 ###  Thane 18 ##             |
|                                                                              |
| Officer    | City  | Category | Pend | InPr | Res | Over | Avg days | Contact |
| Mr Sharma  | Pune  | Roads    |  12  |   5  | 40  |  3 ! |   2.8    | tel mail|
| Ms Patil   | Thane | Water    |   4  |   6  | 55  |  0   |   1.9    | tel mail|
|                                                                              |
| Click a row -> officer card: contact info and their open grievances          |
+------------------------------------------------------------------------------+
```

- Filters: city, category, active or disabled, name search. Sort: most pending, most overdue, most resolved, name.
- The 'Top cities by pending' strip is also a filter: clicking a city applies it as the city filter.
- The phone icon is a `tel:` link and the mail icon is a `mailto:` link, so they use the admin's own phone or mail app. The server does not send email or SMS in version 1.
- Rows with overdue above zero get a warning marker.
- Disabled officers are shown greyed out with a 'Disabled' label.

## 4. Frontend conventions

- **Token storage:** the token and basic user info live in sessionStorage, which is cleared when the tab closes.
- **No direct fetch:** every page calls `api.js`. If a page needs a new call, add a function there.
- **Cross-site scripting:** any text that came from a user (title, description, remarks, names) is inserted with `textContent`, never `innerHTML`.
- **Status colours:** PENDING amber, IN_PROGRESS blue, RESOLVED green, REJECTED red.
- **Priority colours:** HIGH red, MEDIUM amber, LOW grey.
- **Every list page** shows a loading state, an empty state ('No grievances yet') and an error message.
- **Responsive:** the layout must work on a phone-sized screen. Tables scroll horizontally inside their own container.
- **Errors:** show the `message` from the API error body, and show `fieldErrors` next to the matching form field.
- **Dates:** the API sends ISO-8601 local time without zone. Format it for display, do not convert time zones.

### Redirect rules in auth.js

| Situation | Action |
|---|---|
| No token in sessionStorage | Redirect to index.html |
| API answers 401 (expired or invalid token) | Clear sessionStorage, redirect to index.html |
| Logged in as citizen but opens an admin or officer page | Redirect to the citizen dashboard (or to index.html) |
| Logout button pressed | Call POST /api/auth/logout, clear sessionStorage, redirect to index.html |

### Working before the backend exists

Build each page against the example responses in the Backend Guide. A simple approach is a `USE_FAKE` flag in `api.js` that returns the example JSON. Switch it off at the first integration checkpoint. A half-working login connected to the real backend is worth more than a perfect page that has never talked to it.

### Checklist before calling a page done

- It works in the browser against the real API, not only with fake data.
- Loading, empty and error states are all visible when triggered.
- User-supplied text is inserted with textContent.
- It looks usable on a phone-sized window.
- Another team member has looked at it and it is merged to main.
