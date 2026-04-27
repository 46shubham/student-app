# Deploy Student API (Real server, free)

## Important

Cloud deploy cannot use your local MySQL (`127.0.0.1`).  
For a real 24/7 server, use a cloud database. The most reliable free option is **PostgreSQL** (Neon/Supabase).

## 1) Create free Postgres (Neon or Supabase)

- Neon: create a project → copy the connection string (DATABASE_URL)
- Supabase: create a project → Settings → Database → connection string

Then run the SQL from `schema_postgres.sql` to create `students` table.

## 2) Deploy backend on Render (free)

1. Push this project to GitHub.
2. In Render, create **Web Service** from repo.
3. Use:
   - Root directory: `backend`
   - Build command: `npm install`
   - Start command: `npm start`
4. Set env vars in Render dashboard:
   - `DATABASE_URL` = your Postgres connection string
   - `DB_SSL` = `true` (usually required)
   - `API_READ_TOKEN` = your private token
5. Deploy and test:
   - `/api/health`
   - `/api/students`

## 3) Android app use public API URL

In app:
1. Paste URL in API URL box, for example:
   - `https://your-service-domain/api/students`
2. Tap **Save Server URL**
3. Pull down (swipe) to refresh
