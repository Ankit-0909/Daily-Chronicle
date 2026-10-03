# 📰 The Daily Chronicle — News Aggregator Platform

A full-stack news aggregation platform that ingests articles from RSS feeds, generates AI-powered summaries, and delivers a secure, personalized reading experience — built end-to-end with Spring Boot, MySQL, Redis, and vanilla JavaScript.

---

## 🔗 Live Links



- **Live Frontend:** https://ankit-0909.github.io/Daily-Chronicle/
- **Backend API:** https://daily-chronicle.onrender.com
- **Demo / Screenshots:** https://ankit-0909.github.io/Daily-Chronicle/demo.html

> ⚠️ **Note on first load:** the backend runs on Render's free tier, which sleeps after inactivity. The first request after idle time can take 30–60 seconds to respond while the service wakes up.

> All credentials and API keys are managed via environment variables — none are present in this repository or exposed through the deployed app.
---

## 🧭 Overview

The Daily Chronicle polls RSS sources, parses incoming articles, summarizes them using the **Groq LLM API**, and serves them through a searchable, filterable news feed. It includes a full authentication system (email/password with OTP verification, plus Google/GitHub OAuth), a role-gated admin dashboard, and a deployment split across four separate managed platforms.

---

## ✨ Key Features

### Reader-facing
- Email/password registration with OTP email verification, and Google/GitHub OAuth login
- Browsable, searchable news feed with category filters
- AI-generated article summaries (Groq API)
- Bookmarks — save articles to read later
- Personalization — follow specific categories/sources
- Reading history tracking

### Admin-facing
- Separate, role-gated admin login (`ROLE_ADMIN`), independent of the regular user login
- RSS source management (add/edit/delete/toggle active)
- Manual article CRUD, independent of RSS ingestion
- Feed health monitoring
- On-demand **"Fetch News Now"** trigger — in production, ingestion runs only when an admin explicitly triggers it (no background polling), to protect the Groq API quota. Locally, ingestion can also run on a timer for convenience, controlled by a single config flag.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Spring Data Redis |
| Database | MySQL — local via MySQL Workbench, production hosted on **Aiven** |
| Caching | Redis — local instance, production hosted on **Upstash** (TLS-only) |
| Auth | JWT (`jjwt`), OAuth2 (Google, GitHub) |
| Transactional Email | **Brevo REST API** in production (not SMTP — Render blocks outbound SMTP ports; local dev can use SMTP) |
| AI Summarization | Groq API |
| RSS Parsing | Java XML/DOM parsing + Jsoup (HTML cleaning) |
| Frontend | HTML, CSS, vanilla JavaScript (no framework) |
| Testing | JUnit 5, Mockito |
| Backend hosting | Render (Docker-based deployment) |
| Frontend hosting | GitHub Pages, deployed via GitHub Actions CI/CD |

---

## 🔒 Security Highlights

This project went through multiple hardening passes, not just a single security pass at the end:

- **JWT stored in memory only** (never `localStorage`) — access tokens live only in a JS variable and are gone on page close, closing the XSS-token-theft window
- **httpOnly, sliding-expiration refresh token cookie** — session extends automatically with activity, without ever exposing the refresh token to JavaScript
- **OAuth token handoff via one-time opaque code** — Google/GitHub login never exposes a raw JWT in the URL, browser history, or server logs
- **XXE-hardened XML parser** for RSS ingestion (blocks external entity resolution, protecting against local file disclosure/SSRF from a malicious feed)
- **Redis-backed rate limiting** on login, registration, OTP verification, and password reset
- **Hashed refresh tokens at rest** (SHA-256) — a database leak alone can't be used to hijack sessions
- **Bean Validation** on all auth request DTOs
- **Per-article fault isolation** in RSS ingestion — one bad/duplicate article doesn't fail an entire source's fetch cycle
- **Content-Security-Policy and other security headers** configured via Spring Security

---

## 🧪 Testing

- **Unit tests** — JWT generation/validation and core authentication logic
- JUnit 5 + Mockito based, run without loading the Spring application context (no external DB/Redis dependency), so they run identically in local dev and in CI

---

## 🏗️ Deployment Architecture

This project deliberately uses **four independently managed services**, each handling one concern:

```
GitHub Pages (static frontend)
        │
        ▼
Render (Spring Boot backend, Docker)
        │
        ├──► Aiven (MySQL)
        └──► Upstash (Redis)
```

- **Frontend and backend are on different domains** (`github.io` vs `onrender.com`), which required explicit CORS configuration, environment-driven redirect URLs, and cross-site cookie handling (`SameSite=None; Secure`) for the auth flow.
- **Backend config is fully externalized** — `application-prod.properties` contains only `${ENV_VAR}` placeholders; no secrets are committed to the repository. A separate `application.example.properties` documents every required key without real values.
- **Database schema is environment-managed**, not committed as SQL dumps — tables are created via Hibernate on first deploy, then the app is switched to schema-validation-only mode for safety.

---

## 🔑 Authentication Flow

- **Access token** — short-lived JWT, returned in the login response body, held only in a JavaScript variable (never written to `localStorage`), which limits the damage window if it were ever leaked via XSS.
- **Refresh token** — longer-lived (7 days, sliding expiration — each successful refresh extends the window, so an active user is never forced to re-login), delivered only via an `httpOnly` cookie, invisible to JavaScript, and hashed (SHA-256) before being stored in the database. On access-token expiry or on any new page load, the frontend silently calls `/api/auth/refresh`, which uses the cookie to issue a new access token without requiring the user to log in again.
- **OAuth (Google/GitHub)** — after provider login, the backend issues a short-lived one-time exchange code rather than putting a raw JWT in the redirect URL, which is then exchanged for real tokens via a POST request.

**Known limitation:** because the frontend and backend are on different domains, the refresh-token cookie is a cross-site cookie. Some mobile browsers (e.g. Chrome on Android with strict third-party cookie blocking enabled) can block this cookie, which breaks silent session restoration on those devices. This is a known architectural trade-off of the current split-domain deployment; moving frontend and backend to subdomains of one custom domain would resolve it. See [Future Improvements](#️-possible-future-improvements).

---

## 👑 Admin Access

There is no public admin sign-up path. Admin access is granted by manually updating a user's role directly in the production database after they've registered a normal account:

```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin-email@example.com';
```

The user must log out and log back in afterward, since the role is encoded into the JWT at login time.

---

## 🛠️ Getting Started (Local Setup)

### Prerequisites
- Java 17+
- MySQL
- Redis
- Maven

### Backend
```bash
git clone <your-repo-url>
cd News-Backend
mvn clean install
mvn spring-boot:run
```

### Frontend
Open `News-Frontend/index.html` with a local static server (e.g., VS Code Live Server) — no build step required. `config.js` automatically switches between local and production API URLs based on hostname.

### Required Configuration (see `application.example.properties` for the full list)
```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.data.redis.host=
spring.data.redis.port=
spring.data.redis.password=
newsapp.jwt.secret=
groq.api.key=
spring.mail.username=
brevo.api.key=
spring.security.oauth2.client.registration.google.client-id=
spring.security.oauth2.client.registration.google.client-secret=
spring.security.oauth2.client.registration.github.client-id=
spring.security.oauth2.client.registration.github.client-secret=
```

None of these have real values committed anywhere in this repository.

---

## 🗺️ Possible Future Improvements

- Move frontend and backend to subdomains of a single custom domain to fully resolve the mobile cross-site cookie limitation
- Automated test coverage beyond the current authentication unit tests
- CI step to run tests on every push, before deployment
- Push notifications for followed categories/sources
- Full article reading view (beyond the AI-summary modal)
- User profile/settings page

---

## 👤 Author

**Ankit Kumar**
Full Stack Developer (Spring Boot, Java, MySQL, REST APIs)

---

*Built as a hands-on project to practice production-grade deployment across multiple managed services, cross-domain authentication, and real-world infrastructure debugging.*