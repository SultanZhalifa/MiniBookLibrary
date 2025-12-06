package com.example.minibooklibrary;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddBookActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etAuthor, etYear;
    private Spinner spinnerCategory;
    private CheckBox cbIsRead;
    private MaterialButton btnSave, btnDelete;
    private ImageButton btnBack;
    private TextView tvTitle;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;
    private boolean isEditMode = false;
    private int bookId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("BookLibraryPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

        initViews();
        setupSpinner();
        checkEditMode();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etYear = findViewById(R.id.etYear);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        cbIsRead = findViewById(R.id.cbIsRead);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("isEdit") && getIntent().getBooleanExtra("isEdit", false)) {
            isEditMode = true;
            bookId = getIntent().getIntExtra("bookId", -1);

            tvTitle.setText("Edit Book");
            btnSave.setText("Update Book");
            btnDelete.setVisibility(View.VISIBLE);

            // Fill data
            etTitle.setText(getIntent().getStringExtra("title"));
            etAuthor.setText(getIntent().getStringExtra("author"));
            etYear.setText(String.valueOf(getIntent().getIntExtra("year", 0)));
            cbIsRead.setChecked(getIntent().getBooleanExtra("isRead", false));

            String category = getIntent().getStringExtra("category");
            ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
            int position = adapter.getPosition(category);
            spinnerCategory.setSelection(position);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                updateBook();
            } else {
                saveBook();
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    private void saveBook() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String yearStr = etYear.getText().toString().trim();
        boolean isRead = cbIsRead.isChecked();

        if (!validateInput(title, author, yearStr)) {
            return;
        }
        int year = Integer.parseInt(yearStr);

        Book book = new Book(0, title, author, category, year, isRead);
        boolean success = dbHelper.addBook(book, userId);

        if (success) {
            Toast.makeText(this, "Book added successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add book", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBook() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String yearStr = etYear.getText().toString().trim();
        boolean isRead = cbIsRead.isChecked();

        if (!validateInput(title, author, yearStr)) {
            return;
        }
        int year = Integer.parseInt(yearStr);

        Book book = new Book(bookId, title, author, category, year, isRead);
        boolean success = dbHelper.updateBook(book);

        if (success) {
            Toast.makeText(this, "Book updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update book", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String title, String author, String year) {
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title required");
            return false;
        }

        if (TextUtils.isEmpty(author)) {
            etAuthor.setError("Author required");
            return false;
        }

        if (TextUtils.isEmpty(year)) {
            etYear.setError("Year required");
            return false;
        }

        if (year.length() != 4) {
            etYear.setError("Invalid year format");
            return false;
        }

        try {
            int yearInt = Integer.parseInt(year);
            if (yearInt < 1000 || yearInt > 2100) {
                etYear.setError("Year must be between 1000-2100");
                return false;
            }
        } catch (NumberFormatException e) {
            etYear.setError("Invalid year");
            return false;
        }

        return true;
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete this book?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBook() {
        boolean success = dbHelper.deleteBook(bookId);

        if (success) {
            Toast.makeText(this, "Book deleted successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to delete book", Toast.LENGTH_SHORT).show();
        }
    }
}
