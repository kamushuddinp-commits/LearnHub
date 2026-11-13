# LearnHub — Student Course Enrollment Portal

A full-stack demonstration project for the problem statement:

**Online Portal Mapping:** Build a Java class structure utilizing dynamic array storage and reference passing to model many-to-many (M:N) ER relationships between students and online courses.

## Included
- Responsive HTML/CSS/JavaScript frontend served by Spring Boot
- Java 25 + Spring Boot + Maven REST backend
- MySQL database with JPA/Hibernate
- `Student`, `Course`, and `Enrollment` entities
- Enrollment junction table with a unique student/course pair
- Student registration and login (passwords hashed with BCrypt)
- Course catalogue, enrollment, unenrollment and student dashboard
- Six sample courses inserted automatically on first run

## Requirements
1. Java JDK 25 (LTS)
2. Maven 3.6.3 or later
3. MySQL 8.x
4. VS Code (Java Extension Pack recommended)

## 1. Create the database
Open MySQL Workbench or the MySQL command line and run:

```sql
CREATE DATABASE IF NOT EXISTS learnhub;
```

The application uses these default connection values:
- Host: `localhost`
- Port: `3306`
- Database: `learnhub`
- Username: `root`
- Password: blank

If your MySQL username/password differs, edit:
`src/main/resources/application.properties`

Or set environment variables before running:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Example (Mac terminal):
```bash
export DB_USERNAME=root
export DB_PASSWORD='your_mysql_password'
```

## 2. Open in VS Code
1. Extract `LearnHub.zip`.
2. Open VS Code.
3. Choose **File → Open Folder**.
4. Select the extracted `LearnHub` folder (the folder containing `pom.xml`).
5. Open **Terminal → New Terminal**.

## 3. Run the project
In the VS Code terminal, run:

```bash
mvn spring-boot:run
```

Wait until the terminal shows that Spring Boot has started on port 8080.

Open this address in your browser:

**http://localhost:8080**

The frontend and backend are served from the same Spring Boot application. Do not use Live Server for this project.

## 4. Presentation demo
1. Open `http://localhost:8080`.
2. Select **Get started** and register a student account.
3. Open **Explore courses** and enroll in one or more courses.
4. Open **Dashboard** to show the student's enrolled courses.
5. Register a second student and enroll them in the same course to demonstrate the M:N relationship.

## 5. API endpoints
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/students/register` | Register student |
| POST | `/api/students/login` | Login |
| GET | `/api/courses` | List courses |
| POST | `/api/students/{studentId}/enrollments/{courseId}` | Enroll |
| GET | `/api/students/{studentId}/enrollments` | View student's courses |
| DELETE | `/api/students/{studentId}/enrollments/{courseId}` | Unenroll |

## 6. M:N database explanation
The relationship is implemented through three entities:

- `Student`: one row for each student.
- `Course`: one row for each course.
- `Enrollment`: junction entity containing `student_id` and `course_id`.

The database relationship is:

`Student 1 ─── many Enrollment many ─── 1 Course`

This represents a many-to-many relationship between Student and Course. The `Enrollment` entity stores the two foreign keys and enrollment date. A unique constraint on `(student_id, course_id)` prevents the same student from enrolling in the same course twice.

## Notes
- This is a local academic demonstration, not a production-ready authentication system. Login returns a student profile and the browser stores it locally; production deployment needs proper server-side sessions or token authentication, authorization, HTTPS and additional security controls.
- MySQL must be running before starting the application.
- The first Maven run needs an internet connection to download dependencies.
- If `mvn` is not found, install Maven or use the Maven support available in your VS Code environment.

## Updates: persistent login and AI Study Lab

### Login behavior
- LearnHub reuses the student profile saved in browser `localStorage` across pages and refreshes.
- If an already signed-in student opens `login.html` or `register.html`, LearnHub redirects them to the dashboard.
- Public navigation changes from **Log in / Get started** to **Dashboard** while signed in.
- The saved login is cleared only when the student clicks **Log out**.
- Use the same browser and exact site address (for example, always `http://localhost:8080`, not sometimes `127.0.0.1`), because browsers keep separate local storage for different origins.

### AI Study Lab setup
The dashboard includes Explain a topic, Summarize notes, Generate a quiz, Create flashcards, and Build a 7-day study plan. These tools use Google's Gemini API. The API key stays on the Spring Boot server; do not put it in frontend JavaScript or commit it to Git.

1. Create a Gemini API key in Google AI Studio.
2. Set it as an environment variable before starting the app.

macOS / Linux:
```bash
export GEMINI_API_KEY="your_api_key_here"
mvn spring-boot:run
```

Windows PowerShell:
```powershell
$env:GEMINI_API_KEY="your_api_key_here"
mvn spring-boot:run
```

Restart the app after setting the variable. If no key is configured, the rest of LearnHub still works and the AI panel displays a setup message.
