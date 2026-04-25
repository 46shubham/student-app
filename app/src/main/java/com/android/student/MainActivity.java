package com.android.student;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_STUDENTS_API_URL = "http://192.168.0.106:3000/api/students";
    private static final String PREFS_NAME = "student_app_prefs";
    private static final String PREF_API_URL = "students_api_url";

    private final List<Student> students = new ArrayList<>();
    private StudentAdapter adapter;
    private TextView tvStatus;
    private EditText etApiUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        etApiUrl = findViewById(R.id.etApiUrl);
        RecyclerView recyclerView = findViewById(R.id.recyclerStudents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(students);
        recyclerView.setAdapter(adapter);

        etApiUrl.setText(getStudentsApiUrl());

        Button btnSaveUrl = findViewById(R.id.btnSaveUrl);
        btnSaveUrl.setOnClickListener(v -> saveApiUrl());

        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> fetchStudents());

        fetchStudents();
    }

    private void fetchStudents() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String apiUrl = getStudentsApiUrl();
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    String errorDetail = readStream(
                            connection.getErrorStream() != null
                                    ? connection.getErrorStream()
                                    : connection.getInputStream()
                    );
                    showError("HTTP " + responseCode + " " + extractMessage(errorDetail));
                    return;
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

                runOnUiThread(() -> {
                    students.clear();
                    students.addAll(loadedStudents);
                    adapter.notifyDataSetChanged();
                    if (students.isEmpty()) {
                        tvStatus.setText(R.string.students_empty);
                    } else {
                        tvStatus.setText(getString(R.string.students_loaded, students.size()));
                    }
                });
            } catch (Exception e) {
                showError(e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                executor.shutdown();
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> tvStatus.setText(getString(R.string.students_error) + ": " + message));
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