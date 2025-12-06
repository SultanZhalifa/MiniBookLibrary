package com.example.minibooklibrary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    // ✅ Views utama
    private RecyclerView recyclerViewBooks;
    private FloatingActionButton fabAddBook;
    private EditText etSearch;
    private TextView tvWelcome, tvBookCount;
    private ImageButton btnLogout;
    private LinearLayout emptyState;

    private DatabaseHelper dbHelper;
    private BookAdapter bookAdapter;
    private ArrayList<Book> bookList;
    private SharedPreferences sharedPreferences;
    private int userId;
    private String username;

    // ✅ Button untuk fitur baru
    private ImageButton btnSettings, btnStats, btnSort, btnFilter;

    // ✅ Variables untuk Sort & Filter
    private String currentSortOrder = "newest";
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Apply dark mode FIRST
        sharedPreferences = getSharedPreferences("BookLibraryPrefs", MODE_PRIVATE);
        applyDarkMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Get user data
        userId = sharedPreferences.getInt("userId", -1);
        username = sharedPreferences.getString("username", "User");

        if (userId == -1) {
            navigateToLogin();
            return;
        }

        initViews();
        setupRecyclerView();
        loadBooks();
        setupListeners();
    }

    // ✅ Apply dark mode
    private void applyDarkMode() {
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initViews() {
        // ✅ View utama
        recyclerViewBooks = findViewById(R.id.recyclerViewBooks);
        fabAddBook = findViewById(R.id.fabAddBook);
        etSearch = findViewById(R.id.etSearch);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvBookCount = findViewById(R.id.tvBookCount);
        btnLogout = findViewById(R.id.btnLogout);
        emptyState = findViewById(R.id.emptyState);

        // ✅ Init button baru (toolbar modern)
        btnSettings = findViewById(R.id.btnSettings);
        btnStats = findViewById(R.id.btnStats);
        btnSort = findViewById(R.id.btnSort);
        btnFilter = findViewById(R.id.btnFilter);

        // ✅ Header modern
        TextView tvGreeting = findViewById(R.id.tvGreeting); // dari layout toolbar baru
        // Kalau mau diubah dinamis bisa: tvGreeting.setText("Good evening 🌙");

        // ✅ UPDATE: Set username saja (tanpa "Welcome,")
        tvWelcome.setText(username);
    }

    private void setupRecyclerView() {
        // ✅ Setup RecyclerView
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(this, bookList, this);
        recyclerViewBooks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBooks.setAdapter(bookAdapter);

        // ✅ SWIPE TO DELETE FEATURE
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Book deletedBook = bookList.get(position);

                        // Remove from list
                        bookList.remove(position);
                        bookAdapter.notifyItemRemoved(position);
                        updateUI();

                        // Show Snackbar with UNDO option
                        Snackbar.make(recyclerViewBooks,
                                        deletedBook.getTitle() + " deleted",
                                        Snackbar.LENGTH_LONG)
                                .setAction("UNDO", v -> {
                                    bookList.add(position, deletedBook);
                                    bookAdapter.notifyItemInserted(position);
                                    updateUI();
                                })
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        if (event != DISMISS_EVENT_ACTION) {
                                            // Actually delete from database
                                            dbHelper.deleteBook(deletedBook.getId());
                                            Toast.makeText(MainActivity.this,
                                                    "Book permanently deleted",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .show();
                    }
                });

        itemTouchHelper.attachToRecyclerView(recyclerViewBooks);
    }

    // ✅ loadBooks dengan Sort & Filter
    private void loadBooks() {
        bookList.clear();
        ArrayList<Book> allBooks = dbHelper.getAllBooks(userId);

        // ✅ APPLY FILTER
        for (Book book : allBooks) {
            if (currentFilter.equals("all")) {
                bookList.add(book);
            } else if (currentFilter.equals("read") && book.isRead()) {
                bookList.add(book);
            } else if (currentFilter.equals("unread") && !book.isRead()) {
                bookList.add(book);
            }
        }

        // ✅ APPLY SORTING
        switch (currentSortOrder) {
            case "newest":
                // Already sorted by ID DESC from database
                break;
            case "oldest":
                Collections.reverse(bookList);
                break;
            case "a-z":
                Collections.sort(bookList,
                        (b1, b2) -> b1.getTitle().compareToIgnoreCase(b2.getTitle()));
                break;
            case "z-a":
                Collections.sort(bookList,
                        (b1, b2) -> b2.getTitle().compareToIgnoreCase(b1.getTitle()));
                break;
        }

        bookAdapter.notifyDataSetChanged();
        updateUI();
    }

    // ✅ Update UI (book count + empty state)
    private void updateUI() {
        tvBookCount.setText("Total Books: " + bookList.size());

        if (bookList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewBooks.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewBooks.setVisibility(View.VISIBLE);
        }
    }

    // ✅ setupListeners dengan fitur baru
    private void setupListeners() {
        // Add book
        fabAddBook.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddBookActivity.class);
            startActivity(intent);
        });

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Settings (Dark Mode)
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        // Statistics
        btnStats.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        });

        // Sort
        btnSort.setOnClickListener(v -> showSortMenu());

        // Filter
        btnFilter.setOnClickListener(v -> showFilterMenu());

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                searchBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ✅ Sort Menu
    private void showSortMenu() {
        String[] options = {"Newest First", "Oldest First", "A-Z", "Z-A"};
        int checkedItem = -1;

        switch (currentSortOrder) {
            case "newest":
                checkedItem = 0;
                break;
            case "oldest":
                checkedItem = 1;
                break;
            case "a-z":
                checkedItem = 2;
                break;
            case "z-a":
                checkedItem = 3;
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("Sort By")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortOrder = "newest";
                            break;
                        case 1:
                            currentSortOrder = "oldest";
                            break;
                        case 2:
                            currentSortOrder = "a-z";
                            break;
                        case 3:
                            currentSortOrder = "z-a";
                            break;
                    }
                    loadBooks();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Filter Menu
    private void showFilterMenu() {
        String[] options = {"All Books", "Read Only", "Unread Only"};
        int checkedItem = -1;

        switch (currentFilter) {
            case "all":
                checkedItem = 0;
                break;
            case "read":
                checkedItem = 1;
                break;
            case "unread":
                checkedItem = 2;
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("Filter By")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentFilter = "all";
                            break;
                        case 1:
                            currentFilter = "read";
                            break;
                        case 2:
                            currentFilter = "unread";
                            break;
                    }
                    loadBooks();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Search function
    private void searchBooks(String query) {
        if (query.trim().isEmpty()) {
            loadBooks();
        } else {
            bookList.clear();
            bookList.addAll(dbHelper.searchBooks(query, userId));
            bookAdapter.notifyDataSetChanged();
            updateUI();
        }
    }

    // ✅ Logout Dialog
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Logout logic
    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    // ✅ Navigate to Login
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ✅ Klik item buku
    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(MainActivity.this, AddBookActivity.class);
        intent.putExtra("bookId", book.getId());
        intent.putExtra("title", book.getTitle());
        intent.putExtra("author", book.getAuthor());
        intent.putExtra("category", book.getCategory());
        intent.putExtra("year", book.getYear());
        intent.putExtra("isRead", book.isRead());
        intent.putExtra("isEdit", true);
        startActivity(intent);
    }

    // ✅ Refresh data ketika kembali ke activity ini
    @Override
    protected void onResume() {
        super.onResume();
        loadBooks(); // Refresh data when returning from AddBookActivity
    }
}
