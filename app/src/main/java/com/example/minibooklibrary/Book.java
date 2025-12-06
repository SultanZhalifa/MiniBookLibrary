package com.example.minibooklibrary;

public class Book {
    private int id;
    private String title;
    private String author;
    private String category;
    private int year;
    private boolean isRead;

    // ✅ NEW FIELDS
    private float rating; // 0.0 - 5.0
    private String coverImagePath; // Path to cover image
    private int currentPage; // Current reading page
    private int totalPages; // Total pages in book
    private String notes; // Personal notes
    private long dateAdded; // Timestamp when added
    private long lastModified; // Last update timestamp

    // Empty constructor
    public Book() {}

    // Original constructor
    public Book(int id, String title, String author, String category, int year, boolean isRead) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.year = year;
        this.isRead = isRead;
        this.rating = 0.0f;
        this.coverImagePath = "";
        this.currentPage = 0;
        this.totalPages = 0;
        this.notes = "";
        this.dateAdded = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    // Full constructor with new fields
    public Book(int id, String title, String author, String category, int year, boolean isRead,
                float rating, String coverImagePath, int currentPage, int totalPages,
                String notes, long dateAdded, long lastModified) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.year = year;
        this.isRead = isRead;
        this.rating = rating;
        this.coverImagePath = coverImagePath;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.notes = notes;
        this.dateAdded = dateAdded;
        this.lastModified = lastModified;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    // ✅ NEW GETTERS & SETTERS
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    // ✅ HELPER METHODS
    public int getReadingProgress() {
        if (totalPages == 0) return 0;
        return (int) ((currentPage * 100.0) / totalPages);
    }

    public boolean hasRating() {
        return rating > 0;
    }

    public boolean hasCoverImage() {
        return coverImagePath != null && !coverImagePath.isEmpty();
    }
}
