# ItemManager — Inventory Management System

A full-stack Java EE web application for managing inventory items with user authentication, email verification, and one-to-one item details.

Built with **Java Servlets + JSP + JDBC + Oracle Database**, styled with **Tailwind CSS v4** and **Poppins** fonts.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [Setup Guide](#setup-guide)
- [Screenshots](#screenshots)
- [Validation Strategy](#validation-strategy)
- [Security](#security)
- [Known Issues](#known-issues)

---

## Features

### User Management
- **Registration** — Create an account with full name, username, email, and password
- **OTP Email Verification** — 6-digit code sent via SMTP (Gmail) or printed to console for development
- **Login** — Authenticate with username OR email + password
- **Remember Me** — Persistent cookie-based auto-login (1 hour)
- **Profile Management** — Update name/email, change password (with current password verification)
- **Account Deletion** — Permanently remove account with styled modal confirmation

### Inventory Management
- **Dashboard** — Responsive card grid of all inventory items
- **CRUD Operations** — Create, Read, Update, Delete items
- **Item Details** — One-to-one detail record per item (descriptive text with timestamps)
- **Cascading Delete** — Deleting an item automatically removes its detail record
- **Styled Confirmation** — Delete uses a styled modal, not the browser's native `confirm()`

### Session & Cookie Management
- **Login Session:** On successful login, an HTTP session is created with `IsLoggedIn=true` and the full `User` object stored as `session.setAttribute("user", user)`. All protected actions check for this session before proceeding.
- **Remember Me Cookie:** A cookie named `rememberUserId` is set at login (1-hour expiry, `HttpOnly=true`, path `/`). If a user visits without an active session, `UserController.doGet()` checks for this cookie and triggers an auto-login via the `selectUser` action, which looks up the user by ID and recreates the session.
- **Logout:** Invalidates the session (`session.invalidate()`) and clears the cookie (`maxAge=0`), then redirects to the login page.
- **Profile Refresh:** The `selectUser` action is also used after profile/password updates to reload the `User` object into the session with the latest data, then redirects back to the profile page.
- **No Session in JSP:** The cookie check happens in the controller `doGet()`, not in JSP scriptlets, to avoid `IllegalStateException` from trying to modify a committed response.

### Validation & Error Handling
- **Two-Phase Validation** — Client-side format checks BEFORE DB call, business rules merged with SQL errors on failure
- **Field-Level Errors** — Red borders, SVG icons, fade-in animations next to each invalid field
- **Global Error Filter** — Catches all unhandled exceptions and shows a styled error page
- **Human-Readable SQL Errors** — Oracle error codes (ORA-00001, ORA-02290, ORA-01400, etc.) mapped to plain-language messages

### UI/UX
- **Tailwind CSS v4** — Modern utility-first styling via CDN (no build step)
- **Poppins Font** — Clean, modern typography via Google Fonts
- **Gradient Theme** — Indigo/purple/pink gradients throughout
- **Animations** — Card entrance slide-up, fade-in errors, hover transitions
- **Responsive Design** — Adapts from mobile to desktop
- **Post/Redirect/Get (PRG)** — Prevents duplicate form submissions on page refresh

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | JSP, HTML5, CSS3, Tailwind CSS v4 (CDN), Google Fonts (Poppins) |
| **Backend** | Java EE (Servlet 4.0), JSP |
| **Database** | Oracle 12c+ (with pluggable database) |
| **Connection Pool** | Tomcat JNDI DataSource |
| **Password Hashing** | SHA-256 with per-user random salt (16 bytes) |
| **Email** | JavaMail (javax.mail 1.6.2) + Gmail SMTP |
| **Server** | Apache Tomcat 9+ |
| **JDBC Driver** | Oracle JDBC (ojdbc8) |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/items/
│   │   ├── controller/          # Servlets — handle HTTP requests
│   │   │   ├── ItemController.java     # All item CRUD + details
│   │   │   └── UserController.java     # Login, signup, OTP, profile
│   │   ├── filter/
│   │   │   └── ErrorFilter.java        # Global exception handler
│   │   ├── model/               # POJOs matching DB tables
│   │   │   ├── Item.java               # ITEMS table
│   │   │   ├── ItemDetails.java        # ITEM_DETAILS table
│   │   │   └── User.java               # USERS table
│   │   ├── service/             # Service interfaces
│   │   │   ├── ItemService.java
│   │   │   ├── ItemDetailsService.java
│   │   │   └── UserService.java
│   │   ├── service/impl/        # JDBC implementations
│   │   │   ├── ItemServiceImpl.java
│   │   │   ├── ItemDetailsServiceImpl.java
│   │   │   └── UserServiceImpl.java
│   │   └── util/                # Utilities
│   │       ├── EmailUtil.java           # SMTP email + console OTP
│   │       ├── PasswordUtil.java        # SHA-256 + salt hashing
│   │       ├── ItemValidator.java       # Item form validation
│   │       ├── ItemDetailsValidator.java# ItemDetail form validation
│   │       └── UserValidator.java       # User form validation
│   └── webapp/
│       ├── inc/                 # JSP includes
│       │   ├── head.jsp                 # <head> with Tailwind CDN + fonts
│       │   ├── nav_open.jsp             # Navbar start
│       │   └── user_section.jsp         # Login/logout/profile dropdown
│       ├── WEB-INF/
│       │   ├── web.xml                  # Deployment descriptor
│       │   ├── classes/
│       │   │   └── mail.properties      # SMTP credentials
│       │   └── lib/                     # JAR dependencies
│       │       ├── activation-1.1.1.jar
│       │       ├── javax.mail-1.6.2.jar
│       │       └── ojdbc8.jar
│       ├── META-INF/
│       │   └── context.xml              # Tomcat JNDI DataSource
│       ├── css/                         # Legacy stylesheets
│       ├── addItem.jsp
│       ├── addItemDetail.jsp
│       ├── error.jsp
│       ├── itemDetails.jsp
│       ├── items.jsp                    # Dashboard
│       ├── login.jsp
│       ├── profile.jsp
│       ├── register.jsp
│       ├── updateItem.jsp
│       ├── updateItemDetail.jsp
│       └── verifyOtp.jsp
```

---

## Architecture

The application follows a **layered architecture**:

```
HTTP Request
    │
    ▼
┌─────────────────────┐
│    ErrorFilter      │  ← Catches all unhandled exceptions
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Controller/Servlet │  ← Routes actions, validates input, manages session
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Service Interface  │  ← Business logic contract
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Service Impl (JDBC)│  ← Data access, SQL, exception wrapping
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   Oracle Database   │  ← ITEMS, ITEM_DETAILS, USERS tables
└─────────────────────┘
```

**Key design decisions:**
- **Post/Redirect/Get (PRG):** Form submissions always redirect, never forward directly to a result page. This prevents duplicate submissions on refresh.
- **Two-Phase Validation:** Format checks (empty, number parsing) run BEFORE the DB call. Business rules (negative/zero) are checked AFTER a SQLException and merged with constraint messages, so all errors appear simultaneously on the form.
- **SQLException Wrapping:** All JDBC operations catch SQLException and wrap it in RuntimeException. The controller or ErrorFilter then unwraps the cause chain to extract the SQL message.
- **Backward-Compatible OTP:** OTP columns (`is_verified`, `otp_code`, `otp_expires_at`) were added via ALTER TABLE. If they don't exist, the application catches the SQLException and treats the user as verified.

---

## Database Schema

### USERS Table
```sql
CREATE TABLE USERS (
    id            NUMBER(10) PRIMARY KEY,
    full_name     VARCHAR2(100) NOT NULL,
    username      VARCHAR2(30)  NOT NULL UNIQUE,
    email         VARCHAR2(255) NOT NULL UNIQUE,
    password_hash VARCHAR2(255) NOT NULL,
    salt          VARCHAR2(255) NOT NULL,
    is_active     NUMBER(1) DEFAULT 1 NOT NULL,
    is_verified   NUMBER(1) DEFAULT 0 NOT NULL,
    otp_code      VARCHAR2(6),
    otp_expires_at TIMESTAMP,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    -- Constraints:
    CONSTRAINT CHK_USERS_FULL_NAME_LENGTH CHECK (LENGTH(full_name) BETWEEN 3 AND 100),
    CONSTRAINT CHK_USERS_USERNAME_LENGTH  CHECK (LENGTH(username) BETWEEN 3 AND 30),
    CONSTRAINT CHK_USERS_USERNAME_FORMAT  CHECK (REGEXP_LIKE(username, '^[A-Za-z][A-Za-z0-9_.]*$')),
    CONSTRAINT CHK_USERS_EMAIL_LENGTH     CHECK (LENGTH(email) BETWEEN 5 AND 255),
    CONSTRAINT CHK_USERS_EMAIL_FORMAT     CHECK (REGEXP_LIKE(email, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')),
    CONSTRAINT CHK_USERS_IS_VERIFIED      CHECK (is_verified IN (0, 1))
);
```

### ITEMS Table
```sql
CREATE TABLE ITEMS (
    ID           NUMBER(10) PRIMARY KEY,
    NAME         VARCHAR2(255) NOT NULL UNIQUE,
    PRICE        NUMBER(10,2) NOT NULL,
    TOTAL_NUMBER NUMBER(10) NOT NULL,
    -- Constraints:
    CONSTRAINT UQ_ITEMS_NAME           UNIQUE (NAME),
    CONSTRAINT CHK_ITEMS_PRICE         CHECK (PRICE > 0),
    CONSTRAINT CHK_ITEMS_TOTAL_NUMBER  CHECK (TOTAL_NUMBER > 0),
    CONSTRAINT CHK_ITEMS_NAME_LENGTH   CHECK (LENGTH(NAME) >= 1)
);
```

### ITEM_DETAILS Table
```sql
CREATE TABLE ITEM_DETAILS (
    ID          NUMBER(10) PRIMARY KEY,
    ITEM_ID     NUMBER(10) NOT NULL,
    DESCRIPTION VARCHAR2(500) NOT NULL,
    CREATED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT  TIMESTAMP,
    -- Constraints:
    CONSTRAINT FK_ITEM_DETAILS_ITEMS FOREIGN KEY (ITEM_ID) REFERENCES ITEMS(ID),
    CONSTRAINT CHK_ITEM_DETAILS_DESCRIPTION_LENGTH CHECK (LENGTH(DESCRIPTION) >= 3)
);
```

---

## Setup Guide

### Prerequisites
- Java 8+ (JDK)
- Apache Tomcat 9+
- Oracle Database 12c+ (with a pluggable database)
- Eclipse IDE for Enterprise Java (or any Java EE IDE)

### Step 1: Database Setup
1. Create an Oracle user/schema (use your own secure password):
   ```sql
   CREATE USER item IDENTIFIED BY your_secure_password;
   GRANT CONNECT, RESOURCE TO item;
   ```
2. Create the tables using the schemas above (run in the `item` schema)
3. Create a sequence and trigger for auto-increment IDs (if not using identity columns)

### Step 2: Configure Tomcat DataSource
Add the following to Tomcat's `context.xml` or create your own `META-INF/context.xml` (this file is in `.gitignore` — never commit credentials):
```xml
<Resource name="jdbc/items/connection"
          type="javax.sql.DataSource"
          username="item"
          password="your_secure_password"
          driverClassName="oracle.jdbc.OracleDriver"
          url="jdbc:oracle:thin:@//localhost:1521/orclpdb" />
```

### Step 3: Deploy
1. Import the project into Eclipse as a Dynamic Web Project
2. Ensure the following JARs are in `WEB-INF/lib`:
   - `ojdbc8.jar` (Oracle JDBC driver)
   - `javax.mail-1.6.2.jar` (JavaMail API)
   - `activation-1.1.1.jar` (JavaBeans Activation Framework implementation)
3. Configure Tomcat server in Eclipse
4. Right-click project → Run As → Run on Server

### Step 4: Email Setup (Optional)
Create `WEB-INF/classes/mail.properties` with your SMTP credentials (this file is in `.gitignore` — never commit real credentials):
```properties
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=your-email@gmail.com
EMAIL_PASS=your-app-password
EMAIL_FROM=your-email@gmail.com
EMAIL_FROM_NAME=ItemManager
```
For Gmail, generate an **App Password** (not your regular password) from Google Account → Security → 2-Step Verification → App Passwords.

If SMTP is not configured, OTP codes are printed to the Eclipse/Tomcat console.

---

## Validation Strategy

The application uses a **two-phase validation** approach:

### Phase 1: Pre-DB Validation (Format + Required)
Runs BEFORE any database call. Checks:
- Fields are not empty/null
- Numeric fields parse as valid numbers
- Email format, username format, password length
- Description minimum length (3 chars for ItemDetails)

If this phase fails → re-display form with field-level errors, no DB call is made.

### Phase 2: Post-DB Validation (Business Rules + Constraints)
Runs INSIDE the `catch` block after a SQLException. The controller:
1. Extracts the SQL error message from the exception cause chain
2. Calls `Validator.parseSqlError()` to map Oracle error codes to field-level messages
3. Merges with business rule checks (negative/zero values)
4. Re-displays the form with ALL errors visible simultaneously

This ensures users see both format errors AND business rule errors at the same time, even though some could only be detected by the database.

---

## Security

- **Password Hashing:** SHA-256 with a unique 16-byte random salt per user. Salt and hash are stored separately.
- **HTTP-Only Cookies:** The `rememberUserId` cookie has `HttpOnly=true` to prevent XSS access.
- **Prepared Statements:** All SQL queries use parameterized `PreparedStatement` to prevent SQL injection.
- **HTML Escaping:** User-provided names are HTML-escaped in email templates (`escapeHtml()` in EmailUtil).
- **Session Management:** Session attributes are checked before any protected operation.
- **OTP Verification:** New accounts are blocked from logging in until email verification is complete.
- **No Password in Logs:** The `toString()` method of User explicitly excludes `passwordHash`, `salt`, and `otpCode`.

---

## Known Issues

1. **Activation JAR on Java 22:** The `activation-1.1.1.jar` is required at runtime for `javax.mail` to function. On Java 9+ (module system), you may need to add the `--add-modules java.activation` JVM argument, or switch to Jakarta EE alternatives.
2. **Hardcoded Path:** `updateItem.jsp` line 34 uses a hardcoded `/test911/` context path instead of `request.getContextPath()`. Fix: replace with `<%= request.getContextPath() %>`.
3. **OTP Console-Only:** If SMTP is not configured or fails, the OTP is only visible in the server console (Eclipse Console or Tomcat logs). This is intentional for development.

---

## License

This project is for educational purposes.
