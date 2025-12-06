package com.example.minibooklibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BookLibrary.db";
    private static final int DATABASE_VERSION = 2;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    // Books table
    private static final String TABLE_BOOKS = "books";
    private static final String COL_BOOK_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_AUTHOR = "author";
    private static final String COL_CATEGORY = "category";
    private static final String COL_YEAR = "year";
    private static final String COL_IS_READ = "is_read";
    private static final String COL_USER_ID_FK = "user_id";
    private static final String COL_RATING = "rating";
    private static final String COL_COVER_IMAGE = "cover_image_path";
    private static final String COL_CURRENT_PAGE = "current_page";
    private static final String COL_TOTAL_PAGES = "total_pages";
    private static final String COL_NOTES = "notes";
    private static final String COL_DATE_ADDED = "date_added";
    private static final String COL_LAST_MODIFIED = "last_modified";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create books table with new columns
        String createBooksTable = "CREATE TABLE " + TABLE_BOOKS + " (" +
                COL_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_AUTHOR + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                COL_YEAR + " INTEGER, " + // Seharusnya INTEGER untuk tahun
                COL_IS_READ + " INTEGER DEFAULT 0, " +
                COL_USER_ID_FK + " INTEGER, " +
                COL_RATING + " REAL DEFAULT 0.0, " +
                COL_COVER_IMAGE + " TEXT, " +
                COL_CURRENT_PAGE + " INTEGER DEFAULT 0, " +
                COL_TOTAL_PAGES + " INTEGER DEFAULT 0, " +
                COL_NOTES + " TEXT, " +
                COL_DATE_ADDED + " INTEGER, " +
                COL_LAST_MODIFIED + " INTEGER, " +
                "FOREIGN KEY(" + COL_USER_ID_FK + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createBooksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_RATING + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_COVER_IMAGE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_CURRENT_PAGE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_TOTAL_PAGES + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_NOTES + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_DATE_ADDED + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COL_LAST_MODIFIED + " INTEGER");
        }
    }

    // ========== USER METHODS ==========

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password); // Note: For a real app, hash the password!
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public int loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);

        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
            cursor.close();
        }
        db.close();
        return userId;
    }

    // ========== BOOK METHODS ==========

    public boolean addBook(Book book, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, book.getTitle());
        values.put(COL_AUTHOR, book.getAuthor());
        values.put(COL_CATEGORY, book.getCategory());
        values.put(COL_YEAR, book.getYear());
        values.put(COL_IS_READ, book.isRead() ? 1 : 0);
        values.put(COL_USER_ID_FK, userId);
        values.put(COL_RATING, book.getRating());
        values.put(COL_COVER_IMAGE, book.getCoverImagePath());
        values.put(COL_CURRENT_PAGE, book.getCurrentPage());
        values.put(COL_TOTAL_PAGES, book.getTotalPages());
        values.put(COL_NOTES, book.getNotes());
        values.put(COL_DATE_ADDED, System.currentTimeMillis());
        values.put(COL_LAST_MODIFIED, System.currentTimeMillis());

        long result = db.insert(TABLE_BOOKS, null, values);
        db.close();
        return result != -1;
    }

    public ArrayList<Book> getAllBooks(int userId) {
        ArrayList<Book> bookList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BOOKS, null,
                COL_USER_ID_FK + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COL_LAST_MODIFIED + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Book book = new Book(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOK_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_AUTHOR)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_READ)) == 1,
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_RATING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_COVER_IMAGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CURRENT_PAGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_PAGES)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_ADDED)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_MODIFIED))
                );
                bookList.add(book);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return bookList;
    }

    public boolean updateBook(Book book) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, book.getTitle());
        values.put(COL_AUTHOR, book.getAuthor());
        values.put(COL_CATEGORY, book.getCategory());
        values.put(COL_YEAR, book.getYear());
        values.put(COL_IS_READ, book.isRead() ? 1 : 0);
        values.put(COL_RATING, book.getRating());
        values.put(COL_COVER_IMAGE, book.getCoverImagePath());
        values.put(COL_CURRENT_PAGE, book.getCurrentPage());
        values.put(COL_TOTAL_PAGES, book.getTotalPages());
        values.put(COL_NOTES, book.getNotes());
        values.put(COL_LAST_MODIFIED, System.currentTimeMillis());

        int result = db.update(TABLE_BOOKS, values,
                COL_BOOK_ID + "=?",
                new String[]{String.valueOf(book.getId())});
        db.close();
        return result > 0;
    }

    public boolean deleteBook(int bookId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_BOOKS,
                COL_BOOK_ID + "=?",
                new String[]{String.valueOf(bookId)});
        db.close();
        return result > 0;
    }

    // ✅ INI METODE BARU YANG ANDA BUTUHKAN
    public ArrayList<Book> searchBooks(String query, int userId) {
        ArrayList<Book> bookList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COL_USER_ID_FK + " = ? AND (" +
                COL_TITLE + " LIKE ? OR " +
                COL_AUTHOR + " LIKE ?)";
        String[] selectionArgs = {
                String.valueOf(userId),
                "%" + query + "%",
                "%" + query + "%"
        };

        Cursor cursor = db.query(
                TABLE_BOOKS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COL_LAST_MODIFIED + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Book book = new Book(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOK_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_AUTHOR)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_YEAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_READ)) == 1,
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_RATING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_COVER_IMAGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CURRENT_PAGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_PAGES)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_ADDED)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_MODIFIED))
                );
                bookList.add(book);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return bookList;
    }

    // ========== NEW: Get statistics ==========

    public int getTotalBooks(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_BOOKS +
                        " WHERE " + COL_USER_ID_FK + "=?",
                new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getReadBooks(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_BOOKS +
                        " WHERE " + COL_USER_ID_FK + "=? AND " + COL_IS_READ + "=1",
                new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public float getAverageRating(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + COL_RATING + ") FROM " + TABLE_BOOKS +
                        " WHERE " + COL_USER_ID_FK + "=? AND " + COL_RATING + ">0",
                new String[]{String.valueOf(userId)});

        float avg = 0.0f;
        if (cursor.moveToFirst()) {
            avg = cursor.getFloat(0);
        }
        cursor.close();
        db.close();
        return avg;
    }
}
