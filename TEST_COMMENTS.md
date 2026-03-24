# Test Comments for PulseDesk Triage

Copy and paste these comments into the UI form at `http://localhost:8080` to test different priority and category classifications.

---

## CRITICAL Priority (Payment/System Failure)

### 1. Payment Failure
**Text:**
```
Checkout fails and users are charged twice. This is happening in production right now and we're losing revenue. Payment gateway is returning errors.
```
**Source:** `web`
**Author:** `john@customer.com`
**External Ref:** `stripe:charge_failure_123`

**Expected:** CRITICAL priority, BILLING category, creates ticket.

---

### 2. System Outage
**Text:**
```
Production database is down. Service unavailable. P0 incident. System crash. All customers affected. Cannot start the application.
```
**Source:** `ops_alert`
**Author:** `alert@monitoring.system`
**External Ref:** `pagerduty:incident_456`

**Expected:** CRITICAL priority, INCIDENT category, creates ticket.

---

### 3. SQL Injection Vulnerability
**Text:**
```
Found SQL injection vulnerability in login form. sql injection attack possible through username field. Critical security risk.
```
**Source:** `security_report`
**Author:** `security@pentest.com`
**External Ref:** `bugbounty:vuln_789`

**Expected:** CRITICAL priority, SECURITY category, creates ticket.

---

## HIGH Priority (Security/Compliance/Data Loss)

### 4. Data Loss
**Text:**
```
User reported that their account data was lost after the last update. All their files and settings disappeared. Data loss issue on our side.
```
**Source:** `support_ticket`
**Author:** `user@example.com`
**External Ref:** `zendesk:15234`

**Expected:** HIGH priority, DATA_ISSUE category, creates ticket.

---

### 5. Security Breach
**Text:**
```
We have detected a security breach. Unauthorized access to customer records. Potential data breach affecting 500+ users.
```
**Source:** `security_alert`
**Author:** `ciso@company.com`
**External Ref:** `incident_tracker:sec_001`

**Expected:** HIGH priority, SECURITY category, creates ticket.

---

### 6. GDPR Request
**Text:**
```
Customer requesting GDPR data deletion. Please delete all personal data for user id:9876. Privacy request must be handled urgently.
```
**Source:** `legal_email`
**Author:** `legal@company.com`
**External Ref:** `gdpr:request_2024_001`

**Expected:** HIGH priority, COMPLIANCE_PRIVACY category, creates ticket.

---

## MEDIUM Priority (Performance/Integration/Access)

### 7. Performance Degradation
**Text:**
```
The application is running very slowly. Response times are taking 30+ seconds. Performance issue is affecting user experience.
```
**Source:** `user_report`
**Author:** `customer@business.com`
**External Ref:** `zendesk:15289`

**Expected:** MEDIUM priority, PERFORMANCE category, creates ticket.

---

### 8. Login Issue
**Text:**
```
Users cannot login to their accounts. Getting locked out after 3 failed attempts. Password reset is not working. Account access problem.
```
**Source:** `support_chat`
**Author:** `support@company.com`
**External Ref:** `helpdesk:12445`

**Expected:** MEDIUM priority, ACCOUNT category, creates ticket.

---

### 9. API Integration Failure
**Text:**
```
Webhook integration with Slack is broken. Events are not syncing. Third-party API integration issue.
```
**Source:** `developer_report`
**Author:** `dev@company.com`
**External Ref:** `github:issue_567`

**Expected:** MEDIUM priority, INTEGRATION category, creates ticket.

---

## LOW Priority (Feature Requests/Questions)

### 10. Feature Request
**Text:**
```
Would be nice to have a dark mode option in the UI. Please add dark theme as a feature. This would improve user experience.
```
**Source:** `feature_request`
**Author:** `user@example.com`
**External Ref:** ` `

**Expected:** LOW priority, FEATURE category, creates ticket.

---

### 11. Support Question
**Text:**
```
How do I export my data in CSV format? I need to understand the export process. Can someone point me to the documentation?
```
**Source:** `support_email`
**Author:** `newuser@example.com`
**External Ref:** ` `

**Expected:** LOW priority, QUESTION_SUPPORT category, creates ticket.

---

### 12. Usability Question
**Text:**
```
I find the navigation menu confusing. Where is the settings page? The UI is hard to understand for new users.
```
**Source:** `feedback_form`
**Author:** `user@example.com`
**External Ref:** ` `

**Expected:** LOW priority, UX_USABILITY category, creates ticket.

---

## NO TICKET (Not a support issue)

### 13. Spam/Feedback (No ticket expected)
**Text:**
```
Your service is great! I love using it. This is just positive feedback. Keep up the good work!
```
**Source:** `social_media`
**Author:** `happy_user@example.com`
**External Ref:** `twitter:mention_123`

**Expected:** Heuristic: does not look like a support issue. NO TICKET created.

---

## Quick PowerShell Test Script

To submit multiple test comments quickly via PowerShell:

```powershell
$baseUrl = "http://localhost:8080"

# Test 1: Payment critical
Invoke-RestMethod -Method Post "$baseUrl/comments" `
  -ContentType "application/json" `
  -Body (@{
    text = "Checkout fails and users are charged twice. This is happening in production right now."
    source = "web"
    author = "john@customer.com"
    externalRef = "stripe:charge_failure_123"
  } | ConvertTo-Json)

# Test 2: System down critical
Invoke-RestMethod -Method Post "$baseUrl/comments" `
  -ContentType "application/json" `
  -Body (@{
    text = "Production database is down. Service unavailable. P0 incident. All customers affected."
    source = "ops_alert"
    author = "alert@monitoring.system"
    externalRef = "pagerduty:incident_456"
  } | ConvertTo-Json)

# Test 3: Data loss high
Invoke-RestMethod -Method Post "$baseUrl/comments" `
  -ContentType "application/json" `
  -Body (@{
    text = "User reported that their account data was lost after the last update. Data loss issue."
    source = "support_ticket"
    author = "user@example.com"
    externalRef = "zendesk:15234"
  } | ConvertTo-Json)

# Test 4: Feature low
Invoke-RestMethod -Method Post "$baseUrl/comments" `
  -ContentType "application/json" `
  -Body (@{
    text = "Would be nice to have a dark mode option in the UI. Please add dark theme as a feature."
    source = "feature_request"
    author = "user@example.com"
    externalRef = ""
  } | ConvertTo-Json)

Write-Output "All test comments submitted!"
```

---

## Manual Testing Steps

1. Start the app: `mvn spring-boot:run`
2. Open browser: `http://localhost:8080`
3. Pick a test comment above
4. Fill in the form fields
5. Click "Submit comment"
6. Watch the "Status" section update (should show `ANALYZED` or `RETRYING` after a few seconds)
7. Check the "Comments" and "Tickets" tables for results
8. Verify priority and category match expectations

---

## What to Look For

- **CRITICAL** (red): Payment failures, system crashes, SQL injection → highest priority
- **HIGH** (orange): Data loss, security issues, GDPR requests
- **MEDIUM** (yellow): Performance, account access, integrations
- **LOW** (default): Features, questions, usability feedback
- **NO TICKET**: Spam, praise, off-topic chatter

---

# Lietuviški Test Komentarai

Šie komentarai parašyti natūraliu kliento žargonu.

---

## CRITICAL Prioritetas (Kritinės problemos)

### LT-1. Pinigų problema
**Tekstas:**
```
Čia baisus reikalas! Bandau apmokėti per jūsų sistemą, bet ji iš man paima pinigus du kartus! Nespėjau nuo staigmenos. Greitai reaguokite, prašau!
```
**Šaltinis:** `web`
**Autorius:** `r.balkunas@gmail.com`
**Išorinis ref:** `payment_issue_20240324`

**Tikėtina:** CRITICAL prioritetas, BILLING kategorija, kurtas tiketą.

---

### LT-2. Sistema "negriebia"
**Tekstas:**
```
Jūsų sistema visai sugriuvo! Nuo ryto negriebus. Sėdžiu čia su komanda ir viskas sustojus. Negali darbo atlikti. Greitai fiksti!
```
**Šaltinis:** `support_chat`
**Autorius:** `vadovas@musa_firma.lt`
**Išorinis ref:** `critical_downtime_1`

**Tikėtina:** CRITICAL prioritetas, INCIDENT kategorija, kurtas tiketą.

---

### LT-3. Saugumo grėsmė
**Tekstas:**
```
Kažkas iš gal SQL injection? Bandžiau įvesti tą "' or 1=1 --" ir programa manęs iš tos pusės atmetė, bet įvertinti ar saugus iš tikro. Tai nėra gerai.
```
**Šaltinis:** `security_report`
**Autorius:** `it_admin@company.lt`
**Išorinis ref:** `sec_vuln_101`

**Tikėtina:** CRITICAL prioritetas, SECURITY kategorija, kurtas tiketą.

---

## HIGH Prioritetas (Svarbi problema)

### LT-4. Duomenys dingo
**Tekstas:**
```
Šiandien buvau darbą atlikęs, išsaugojau viską, šiandien grįžau ir viskas pryšo! Visa man svarbi informacija negrįžtu. Kas darėsi?
```
**Šaltinis:** `user_report`
**Autorius:** `ignas.k@mail.lt`
**Išorinis ref:** `zendesk:15234`

**Tikėtina:** HIGH prioritetas, DATA_ISSUE kategorija, kurtas tiketą.

---

### LT-5. Privatumo reikalavimas
**Tekstas:**
```
Norėčiau, kad visos mano asmeninės duomenys būtų ištrinti. Nebenoriu jūsų paslaugos. Pagal BDAR turite tai padaryti. Kai greičiau!
```
**Šaltinis:** `legal_email`
**Autorius:** `jonas@gmail.com`
**Išorinis ref:** `gdpr_delete_request`

**Tikėtina:** HIGH prioritetas, COMPLIANCE_PRIVACY kategorija, kurtas tiketą.

---

## MEDIUM Prioritetas (Vidutinės problemos)

### LT-6. Programėlė lėta
**Tekstas:**
```
Jūsų programa pas mane labai lėta. Kol naudžiaus, užsidegina komp. Gal kur nors memory leak'as? Nedaro gero vaizdžio.
```
**Šaltinis:** `feedback_form`
**Autorius:** `user@example.lt`
**Išorinis ref:** ` `

**Tikėtina:** MEDIUM prioritetas, PERFORMANCE kategorija, kurtas tiketą.

---

### LT-7. Negaliu prisijungti
**Tekstas:**
```
Vėl senis pamiršau slaptažodį ir dabar negali prisijungti. Bandžiau reset'ą, bet el. paštą negaunu. Nepaprasta! Pagelbėkit!
```
**Šaltinis:** `support_email`
**Autorius:** `vytautas.65@yahoo.com`
**Išorinis ref:** `help_login_001`

**Tikėtina:** MEDIUM prioritetas, ACCOUNT kategorija, kurtas tiketą.

---

## LOW Prioritetas (Mažos bėdos, pasiūlymai)

### LT-8. Pasiūlymas
**Tekstas:**
```
Gal norėtumėte pridėti jūsų programą į App Store? Aš norėčiau jį poblogioje naudoti. Būtų labai patogu iš telefono.
```
**Šaltinis:** `user_suggestion`
**Autorius:** `andrius@gmail.com`
**Išorinis ref:** ` `

**Tikėtina:** LOW prioritetas, FEATURE kategorija, kurtas tiketą.

---

### LT-9. Klausimas
**Tekstas:**
```
Sveiki! Ar galiu iš jūsų ištraukti ataskaitą formatą Excel? Man reikia ta duomenimis padirbėti biure. Padėkit!
```
**Šaltinis:** `support_chat`
**Autorius:** `laima_v@bank.lt`
**Išorinis ref:** ` `

**Tikėtina:** LOW prioritetas, QUESTION_SUPPORT kategorija, kurtas tiketą.

---

## NO TICKET (Ne problema)

### LT-10. Gera programa!
**Tekstas:**
```
Jūsų programa labai gera! Naudojuosi dabar jau metus. Nors kartais būna keistenybių, bet bendrai super! Rekomenduoju draugams. Ačiū!
```
**Šaltinis:** `social_media`
**Autorius:** `zulike.lt@gmail.com`
**Išorinis ref:** ` `

**Tikėtina:** NO TICKET. Tai just positive feedback, sistema nesukurs tiketą.
