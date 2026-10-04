# Testing Guide

*What to test, how to test it, and the demo script*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

## 1. Testing approach

The time available favours a small set of automated tests plus a careful manual checklist. Testing is done at three levels:

| Level | What | Who |
|---|---|---|
| Automated (JUnit 5) | Assignment rules, status flow rules, repository queries against a test database | Data and API developers |
| API checks (curl or Postman) | Endpoints, status codes, role checks, error format | API developer, reviewed by UI developer |
| Manual browser checklist | Whole flows through the UI, on a clean database | Everyone, before the demo |

A feature is done only when it works through the browser, matches the API contract, shows readable errors and has been looked at by another team member.

## 2. Automated tests to write

| Test class | Cases |
|---|---|
| AssignmentServiceTest | Same city and category picks that officer. No match on category falls back to same city. No officer in the city leaves the grievance unassigned. With two candidates, the one with fewer open grievances wins. Ties go to the lowest user id. Disabled officers are never chosen. |
| GrievanceServiceTest (status flow) | PENDING to IN_PROGRESS, RESOLVED and REJECTED are allowed. RESOLVED or REJECTED to anything is refused with a 400. A missing remark is refused. resolved_at is set only when RESOLVED. An officer cannot update a grievance assigned to someone else (404). |
| GrievanceRepositoryTest | Insert returns a tracking ID in the GRV-00042 format. Officer statistics counts match inserted rows. The city filter returns only that city's officers. City ranking orders by pending. |
| AuthServiceTest | Wrong password returns the generic message. Right password with the wrong role is refused. Disabled account is refused. Token expiry is respected. |

Run all tests with `./mvnw test`.

## 3. API checks with curl

Run these against a running app with the seed data loaded. Replace the token with the value returned by the login call.

### Log in and save the token

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"rahul","password":"Citizen@123","role":"CITIZEN"}'
```

### Call a protected endpoint with the token

```bash
curl -s http://localhost:8080/api/citizen/summary \
  -H "Authorization: Bearer PASTE_TOKEN_HERE"
```

### Expected results

| Call | Expected |
|---|---|
| Any /api/citizen/... call with no Authorization header | 401 |
| Login with a wrong password | 401 'Invalid username or password' |
| Login with a citizen account and role ADMIN | 403 'This account is not an Admin account' |
| GET /api/admin/officers with a citizen token | 403 |
| GET /api/citizen/grievances/{someone else's ID} | 404 |
| POST /api/auth/register with an existing username | 409 |
| POST /api/auth/register with a 5-character password | 400 with fieldErrors |
| POST /api/auth/register with "role":"ADMIN" in the body | 201 and the account is a CITIZEN (role is ignored) |
| Any response body | Never contains password_hash |
| GET /api/lookup/cities with no token | 200 (public) |

## 4. Manual test checklist

Run on a clean database (reset it first, see the Local Setup Guide).

| Area | Test | Expected |
|---|---|---|
| Sign up | Register with an existing username | 409, message shown on the form |
| Sign up | Password shorter than 8 characters | Validation message, no account created |
| Sign up | Phone with fewer than 10 digits | Validation message next to the phone field |
| Login | Wrong password | 'Invalid username or password' |
| Login | Citizen credentials on the Admin card | 'This account is not an Admin account' |
| Login | Disabled officer logs in | 'Account is disabled' |
| Access | Open officer/dashboard.html while logged in as a citizen | Redirected away |
| Access | Call GET /api/admin/officers with a citizen token (curl) | 403 |
| Access | Citizen A requests citizen B's tracking ID | 404 |
| Access | Open a protected page after logout | Redirected to the landing page |
| File grievance | Roads & Potholes in Pune | Assigned to the Pune roads officer |
| File grievance | A category and city with no matching officer | Unassigned, citizen sees 'awaiting assignment' |
| File grievance | City has an officer but not for this category | Assigned to a same-city officer (fallback) |
| Assignment | File two grievances that match the same two officers | The second goes to the less-loaded officer |
| Status | Resolve a grievance, then try to set it to PENDING | 400 with a clear message |
| Status | Update without a remark | Validation error |
| Status | Officer opens a grievance assigned to someone else | 404 |
| Timeline | Citizen opens an updated grievance | Every change appears in order with remarks |
| Counts | Officer resolves a grievance | Citizen dashboard counts change |
| Admin dashboard | Compare totals with the grievance list | Numbers agree |
| Admin officers | Filter by city, sort by most pending | List and city strip agree with the data |
| Admin officers | Click a city in the strip | It is applied as the city filter |
| Admin officers | Click the phone and mail icons | tel: and mailto: links open |
| Admin officers | Disable an officer with open grievances | They become Unassigned. The officer cannot log in. |
| Admin officers | Enable the officer again | Login works, old assignments are not restored |
| Admin officers | Add an officer with an existing username | 409, message shown |
| Admin | Reassign an unassigned grievance | History row added, the officer sees it |
| Admin | Reassign to a disabled officer (API) | 400 'Officer is not active' |
| Restart | Stop and start the app twice | No duplicate seed data, no errors |
| Security | Put &lt;b&gt;test&lt;/b&gt; in a grievance title | Shown as plain text, not bold |
| Layout | Resize the browser to phone width | Tables scroll inside their container, nothing breaks |

## 5. Demo script (about 6 minutes)

1. Open localhost:8080. Show the three role cards.
2. Citizen card: sign up as a new user, then log in.
3. File a grievance: Roads & Potholes, Pune, Ward 4, High priority. Point out the tracking ID and the assigned officer.
4. Log out. Officer card: log in as sharma. Show the new grievance and the overdue flag on older ones. Set it to In Progress with a remark.
5. Log back in as the citizen. Open the grievance and show the timeline with the remark.
6. As sharma, mark it Resolved. Show that the citizen's dashboard counts change.
7. Admin card: log in. Show the dashboard, cities with most pending cases and grievances per category.
8. Open Officers. Filter by a city, sort by most pending, click the phone and mail icons, open an officer's card.
9. Disable an officer, show their open grievances become Unassigned, and reassign one.
10. Finish with security: try citizen credentials on the Admin card and show the rejection.

> Rehearse the demo twice on a clean database before presenting. Reset the database first so the sample data matches the script.
