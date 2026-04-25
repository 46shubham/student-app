# Deploy Student API (Render or Railway)

## Important

Cloud deploy cannot use your local MySQL (`127.0.0.1`).  
Use a cloud MySQL database and set its credentials in environment variables.

## 1) Railway (recommended easiest)

1. Push this project to GitHub.
2. In Railway, create a new project.
3. Add **MySQL** service in Railway.
4. Add a new service from GitHub repo and set **Root Directory** as `backend`.
5. In service variables set:
   - `DB_HOST` = Railway MySQL host
   - `DB_PORT` = Railway MySQL port
   - `DB_USER` = Railway MySQL user
   - `DB_PASSWORD` = Railway MySQL password
   - `DB_NAME` = `student_app_db`
6. Deploy.
7. Open Railway generated URL and test:
   - `/api/health`
   - `/api/students`

## 2) Render

1. Push this project to GitHub.
2. In Render, create **Web Service** from repo.
3. Use:
   - Root directory: `backend`
   - Build command: `npm install`
   - Start command: `npm start`
4. Set env vars in Render dashboard:
   - `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`
5. Deploy and test:
   - `/api/health`
   - `/api/students`

## 3) Android app use public API URL

In app:
1. Paste URL in API URL box, for example:
   - `https://your-service-domain/api/students`
2. Tap **Save Server URL**
3. Tap **Refresh**
