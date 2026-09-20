# User Guide

*How citizens, officers and admins use the JanSunwai portal*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

JanSunwai lets citizens report civic problems and follow them until they are resolved. This guide explains what each kind of user can do. Open the portal in a web browser (on a laptop or a phone) at the address your team gives you, for local use `http://localhost:8080`.

## 1. Getting started

The first page shows three cards: **Citizen**, **Officer** and **Admin**. Click the card that matches your role.

| If you are | Click | You can |
|---|---|---|
| A member of the public | Citizen | Sign up, log in and report problems |
| A municipal officer | Officer | Log in only (an admin creates your account) |
| The administrator | Admin | Log in only |

> If you click the wrong card, the portal tells you, for example 'This account is not an Admin account'. Go back and pick the right card. Your password is not the problem.

## 2. For citizens

### Create an account

1. Click the Citizen card, then the Sign up tab.
2. Enter your full name, a username (4 to 30 letters, digits or underscores), your email, a 10-digit phone number and a password of at least 8 characters.
3. Press Sign up. If the username or email is already used, choose another.
4. Switch to the Login tab and log in.

### Report a problem

1. On your dashboard, choose New grievance.
2. Fill in a short title (5 to 150 characters), the category, your city, the area or ward, the priority and a description (10 to 2000 characters).
3. Press Submit. You receive a tracking ID such as **GRV-00042** and the name of the officer assigned to your case.
4. If no officer is available yet, you see 'awaiting assignment'. The administrator will assign one. Nothing more is needed from you.

Tip: be specific. 'Large pothole near the bus stop on MG Road' is more useful to the officer than 'road problem'.

### Follow your grievance

- Your dashboard shows how many grievances you have in total, and how many are pending, in progress, resolved or rejected.
- Use the status filter to narrow the list.
- Open any grievance to see its details and a timeline. Every change appears with the officer's remark, who made it and when.
- You can only see your own grievances.

### What the statuses mean

| Status | Meaning |
|---|---|
| Pending | Received and waiting for action. |
| In progress | The officer has started working on it. |
| Resolved | The problem has been fixed. In this version a resolved grievance cannot be re-opened. |
| Rejected | The officer could not act on it. The remark explains why. |

To follow up with an officer, use the phone number shown on your grievance page.

## 3. For officers

Your account is created by the administrator. You receive a username and a temporary password from them. Click the Officer card and log in.

### Your dashboard

- Summary cards show how many grievances are assigned to you, and how many are pending, in progress, resolved or overdue.
- Overdue means still open and more than 7 days old. Work on those first.
- The table lists your grievances. Filter by status or priority, or search by title or area.

### Update a grievance

1. Open a grievance to read the description and the citizen's contact details (phone and email).
2. Choose the new status: In Progress, Resolved or Rejected.
3. Write a remark (1 to 500 characters). The remark is required and the citizen sees it in the timeline.
4. Press Update. The change is saved in the history.

Resolved and rejected are final in this version. Check before you press the button.

You only see grievances assigned to you. If you think a grievance belongs to someone else, ask the administrator to reassign it.

## 4. For administrators

### Dashboard

- Totals per status, the number of overdue grievances and the number of unassigned grievances.
- Grievances per category, and cities ranked by pending cases.

### All grievances

- Filter by status, category or city, show unassigned ones only, or search by text. The list is paginated.
- Open a grievance to see the citizen, the officer and the full history.
- To reassign, choose an active officer and optionally add a remark. The move is recorded in the history.

### Officers

- The directory shows each officer's city, category, phone, email, active flag and statistics: pending, in progress, resolved, overdue and average resolution days.
- Filter by city, category and active or disabled. Search by name. Sort by most pending, most overdue, most resolved or name.
- The strip of top cities shows where most cases are pending. Click a city to filter the table to it.
- Press the phone icon to call, or the mail icon to write an email, using your own phone or mail app.
- Click an officer to see the contact card and their open grievances.

### Add an officer

1. Choose Add officer and enter name, username, email, 10-digit phone, a temporary password, city and category.
2. Give the username and temporary password to the officer. Ask them to keep the password private.
3. The city and category decide which grievances are assigned to this officer automatically.

### Disable or enable an officer

- Disabling blocks the officer's login and moves their open grievances to Unassigned. Reassign them from the grievances list.
- Resolved and rejected grievances stay with the officer for reporting.
- Enabling an officer again lets them log in, but does not give their old grievances back.

## 5. How grievances are assigned

When a citizen files a grievance the system looks for an officer automatically:

1. First: an active officer in the same city who handles the same category. If there are several, the one with the fewest open grievances is chosen.
2. Second: if there is none, an active officer in the same city for any category, again the least busy.
3. Third: if there is still none, the grievance stays unassigned until the administrator assigns it.

## 6. Common questions

| Problem | What to do |
|---|---|
| I forgot my password | Password reset is not available in this version. An administrator can create a new officer account. Citizens should create a new account. |
| It says 'Account is disabled' | The administrator has disabled your officer account. Contact the administrator. |
| It says my session expired or I am sent to the first page | You were logged out after 8 hours, or your tab was closed. Log in again. |
| My grievance shows 'awaiting assignment' | No officer covers your city yet. The administrator will assign one. |
| I cannot see a grievance that I know exists | You only see your own grievances (citizens) or those assigned to you (officers). |
| The page looks old after an update | Refresh with Ctrl+Shift+R. |
| Can I attach a photo? | Not in this version. Describe the location clearly in the area and description fields. |
