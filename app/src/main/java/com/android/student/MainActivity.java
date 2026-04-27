package com.android.student;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_STUDENTS_API_URL = "https://student-api-sig1.onrender.com/api/students";
    private static final String API_READ_TOKEN = "student_read_only_2026_secure";
    // Fallback for same-WiFi cases where trycloudflare is blocked by router/DNS.
    private static final String LOCAL_FALLBACK_API_URL = "http://192.168.0.106:3000/api/students";
    private static final String PREFS_NAME = "student_app_prefs";
    private static final String PREF_API_URL = "students_api_url";

    private final List<Student> students = new ArrayList<>();
    private StudentAdapter adapter;
    private TextView tvStatus;
    private EditText etApiUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isFetching = false;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        etApiUrl = findViewById(R.id.etApiUrl);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        RecyclerView recyclerView = findViewById(R.id.recyclerStudents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(students);
        recyclerView.setAdapter(adapter);

        etApiUrl.setText(getStudentsApiUrl());

        android.widget.Button btnSaveUrl = findViewById(R.id.btnSaveUrl);
        btnSaveUrl.setOnClickListener(v -> saveApiUrl());

        swipeRefreshLayout.setOnRefreshListener(this::fetchStudents);

        fetchStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchStudents();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void fetchStudents() {
        if (isFetching) {
            return;
        }
        isFetching = true;

        runOnUiThread(() -> {
            tvStatus.setText(R.string.loading_students);
            swipeRefreshLayout.setRefreshing(true);
        });

        executor.execute(() -> {
            try {
                List<Student> loadedStudents = fetchStudentsFromUrl(getStudentsApiUrl(), true);

                runOnUiThread(() -> {
                    students.clear();
                    students.addAll(loadedStudents);
                    adapter.notifyDataSetChanged();
                    if (students.isEmpty()) {
                        tvStatus.setText(R.string.students_empty);
                    } else {
                        tvStatus.setText(getString(R.string.students_loaded, students.size()));
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (Exception e) {
                showError(e.getMessage());
            } finally {
                isFetching = false;
            }
        });
    }

    private List<Student> fetchStudentsFromUrl(String apiUrl, boolean allowFallback) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + API_READ_TOKEN);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorDetail = readStream(
                        connection.getErrorStream() != null
                                ? connection.getErrorStream()
                                : connection.getInputStream()
                );

                // If trycloudflare is blocked on the current Wi‑Fi, retry via LAN.
                if (allowFallback && shouldFallback(apiUrl, responseCode, errorDetail)) {
                    return fetchStudentsFromUrl(LOCAL_FALLBACK_API_URL, false);
                }

                throw new Exception("HTTP " + responseCode + " " + extractMessage(errorDetail));
            }

            String response = readStream(connection.getInputStream());
            JSONArray jsonArray = new JSONArray(response);
            List<Student> loadedStudents = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                loadedStudents.add(new Student(
                        item.getInt("id"),
                        item.getString("name"),
                        item.getString("email"),
                        item.getString("phone"),
                        item.getString("address")
                ));
            }
            return loadedStudents;
        } catch (SocketTimeoutException e) {
            if (allowFallback && apiUrl.contains("trycloudflare.com")) {
                return fetchStudentsFromUrl(LOCAL_FALLBACK_API_URL, false);
            }
            throw e;
        } catch (UnknownHostException | ConnectException e) {
            if (allowFallback && apiUrl.contains("trycloudflare.com")) {
                return fetchStudentsFromUrl(LOCAL_FALLBACK_API_URL, false);
            }
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean shouldFallback(String apiUrl, int responseCode, String errorBody) {
        if (!apiUrl.contains("trycloudflare.com")) return false;
        if (responseCode == 502 || responseCode == 503 || responseCode == 504) return true;
        String msg = (errorBody == null ? "" : errorBody).toLowerCase();
        return msg.contains("bad gateway") || msg.contains("unable to reach the origin");
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            tvStatus.setText(getString(R.string.students_error) + ": " + message);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private String readStream(java.io.InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private String extractMessage(String body) {
        try {
            JSONObject obj = new JSONObject(body);
            if (obj.has("message")) {
                return obj.getString("message");
            }
            if (obj.has("error")) {
                return obj.getString("error");
            }
        } catch (Exception ignored) {
            // Keep raw body when not JSON.
        }
        return body;
    }

    private String getStudentsApiUrl() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_API_URL, DEFAULT_STUDENTS_API_URL);
    }

    private void saveApiUrl() {
        String enteredUrl = etApiUrl.getText().toString().trim();
        if (!(enteredUrl.startsWith("http://") || enteredUrl.startsWith("https://"))) {
            Toast.makeText(this, R.string.invalid_server_url, Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_API_URL, enteredUrl)
                .apply();

        Toast.makeText(this, R.string.server_url_saved, Toast.LENGTH_SHORT).show();
        fetchStudents();
    }
}