const express = require("express");
const cors = require("cors");
const mysql = require("mysql2/promise");
require("dotenv").config();

const app = express();
app.use(cors());
app.use(express.json());

const port = Number(process.env.PORT || process.env.API_PORT || 3000);

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "student_app_db",
  waitForConnections: true,
  connectionLimit: 10,
});

if (!process.env.DB_PASSWORD) {
  console.warn(
    "Warning: DB_PASSWORD is empty. If MySQL root has a password, set it in backend/.env"
  );
}

app.get("/api/health", async (_req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ status: "ok", message: "Database connected" });
  } catch (error) {
    res.status(500).json({ status: "error", message: error.message });
  }
});

app.get("/api/students", async (_req, res) => {
  try {
    const [rows] = await pool.query(
      "SELECT id, name, email, phone, address, created_at FROM students ORDER BY id DESC"
    );
    res.json(rows);
  } catch (error) {
    res.status(500).json({ error: "Failed to fetch students", message: error.message });
  }
});

app.listen(port, "0.0.0.0", () => {
  console.log(`Student API running on http://0.0.0.0:${port}`);
});
