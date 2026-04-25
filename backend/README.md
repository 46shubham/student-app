# Student API

This API serves student records from MySQL for the Android app.

## 1) Create database and table

Run this SQL in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS student_app_db;

USE student_app_db;

CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 2) Configure environment

Copy `.env.example` to `.env` and update credentials.

## 3) Install and run

```bash
npm install
npm start
```

Server starts on `http://localhost:3000`.

## 4) API endpoints

- `GET /api/health`
- `GET /api/students`
