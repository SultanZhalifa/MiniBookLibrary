package com.example.minibooklibrary;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;

public class StatisticsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

    private TextView tvTotalBooks, tvReadBooks, tvUnreadBooks;
    private TextView tvFictionCount, tvNonFictionCount, tvScienceCount;
    private TextView tvTechnologyCount, tvHistoryCount, tvBiographyCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("BookLibraryPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

        initViews();
        loadStatistics();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvTotalBooks = findViewById(R.id.tvTotalBooks);
        tvReadBooks = findViewById(R.id.tvReadBooks);
        tvUnreadBooks = findViewById(R.id.tvUnreadBooks);
        tvFictionCount = findViewById(R.id.tvFictionCount);
        tvNonFictionCount = findViewById(R.id.tvNonFictionCount);
        tvScienceCount = findViewById(R.id.tvScienceCount);
        tvTechnologyCount = findViewById(R.id.tvTechnologyCount);
        tvHistoryCount = findViewById(R.id.tvHistoryCount);
        tvBiographyCount = findViewById(R.id.tvBiographyCount);
    }

    private void loadStatistics() {
        ArrayList<Book> allBooks = dbHelper.getAllBooks(userId);

        int total = allBooks.size();
        int read = 0;
        HashMap<String, Integer> categoryCount = new HashMap<>();

        for (Book book : allBooks) {
            if (book.isRead()) read++;

            String category = book.getCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }

        int unread = total - read;

        // Update UI
        tvTotalBooks.setText(String.valueOf(total));
        tvReadBooks.setText(String.valueOf(read));
        tvUnreadBooks.setText(String.valueOf(unread));

        tvFictionCount.setText(String.valueOf(categoryCount.getOrDefault("Fiction", 0)));
        tvNonFictionCount.setText(String.valueOf(categoryCount.getOrDefault("Non-Fiction", 0)));
        tvScienceCount.setText(String.valueOf(categoryCount.getOrDefault("Science", 0)));
        tvTechnologyCount.setText(String.valueOf(categoryCount.getOrDefault("Technology", 0)));
        tvHistoryCount.setText(String.valueOf(categoryCount.getOrDefault("History", 0)));
        tvBiographyCount.setText(String.valueOf(categoryCount.getOrDefault("Biography", 0)));
    }
}
