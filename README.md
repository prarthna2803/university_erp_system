# University ERP System

A full-stack Java Swing desktop application built for IIIT Delhi, supporting role-based access control across 3 user types — Student, Instructor, and Admin.

## Test Credentials

| Role | Username | Password |
|------|----------|----------|
| Instructor | inst1 | 12345 |
| Student | stu1 | stu1@12345 |
| Admin | admin1 | 12345 |

## Features

### Role-based access
- **Students** — course registration, grade viewing, timetable, hostel, fees, transcript export
- **Instructors** — manage sections, assign grades, view course stats
- **Admin** — full user management, course control, system-wide maintenance mode

### Security
- Secure authentication using bcrypt password hashing (jBCrypt)
- Dual-database architecture — `authdb` for credentials, `erpdb` for academic data
- Session management preventing unauthorized and cross-role operations

### Academic workflows
- Course enrollment with section and seat management
- Automated grade computation with configurable weighting
- CSV and PDF transcript export
- 8+ normalized database tables

### Admin dashboard
- Add/edit/delete users and courses
- Maintenance Mode — enforces read-only access system-wide during downtime
- Calendar and system stats panel

## Tech Stack

- **Java** — core application logic
- **Java Swing + FlatLaf** — desktop UI with modern theming
- **MySQL** — relational database (authdb + erpdb)
- **jBCrypt** — password hashing
- **MySQL Connector/J** — JDBC database connectivity

## Running locally

See `HowToRun.pdf` for detailed setup instructions.

Quick start:
1. Import `authdb_backup.sql` and `erpdb_backup.sql` into your MySQL server
2. Update DB credentials in `src/erp/data/DBConnection.java`
3. Open project in IntelliJ IDEA
4. Add JARs from `/lib` to your classpath
5. Run `src/erp/ui/LoginFrame.java`

## Project structure
src/
├── erp/
│   ├── data/        — DAO layer (database queries)
│   ├── domain/      — domain models (Student, Course, Grade...)
│   ├── service/     — business logic
│   └── ui/          — Swing UI (admin, instructor, student panels)
lib/                 — external JARs
authdb_backup.sql    — authentication database
erpdb_backup.sql     — ERP database
HowToRun.pdf         — setup guide
Report.pdf           — project report
Roles.pdf            — role documentation

## Documents

- `HowToRun.pdf` — setup and run instructions
- `Report.pdf` — full project report
- `Roles.pdf` — role and permission breakdown
- `Test_summary.pdf` — testing summary
- `Testing_Doc.pdf` — detailed test cases
