const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");
require("dotenv").config();

const app = express();
app.use(cors());
app.use(express.json());

const port = Number(process.env.PORT || process.env.API_PORT || 3000);
// If env is missing/misloaded, fallback keeps app working.
const apiReadToken = (process.env.API_READ_TOKEN || "student_read_only_2026_secure").trim();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: false } : undefined,
  max: Number(process.env.DB_POOL_MAX || 10),
});

function isAuthorizedRequest(req) {
  if (!apiReadToken) return true;

  const authHeader = req.get("authorization") || "";
  const apiKeyHeader = req.get("x-api-key") || "";

  const bearer = authHeader.startsWith("Bearer ")
    ? authHeader.slice("Bearer ".length).trim()
    : "";

  return bearer.trim() === apiReadToken || apiKeyHeader.trim() === apiReadToken;
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
  if (!isAuthorizedRequest(_req)) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  try {
    const { rows } = await pool.query(
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
