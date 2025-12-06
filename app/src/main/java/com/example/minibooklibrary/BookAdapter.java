package com.example.minibooklibrary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // ✅ TAMBAHAN
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private Context context;
    private ArrayList<Book> bookList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookAdapter(Context context, ArrayList<Book> bookList, OnBookClickListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);

        holder.tvBookTitle.setText(book.getTitle());
        holder.tvAuthor.setText("by " + book.getAuthor());
        holder.tvCategory.setText(book.getCategory());
        holder.tvYear.setText(String.valueOf(book.getYear()));

        // Show read badge if book is read
        if (book.isRead()) {
            holder.tvReadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvReadBadge.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> listener.onBookClick(book));
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    // ✅ UPDATED ViewHolder
    public class BookViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookTitle, tvAuthor, tvCategory, tvYear;
        ImageView ivBookIcon; // ✅ CHANGED: TextView -> ImageView
        View tvReadBadge; // ✅ CHANGED: TextView -> View

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvYear = itemView.findViewById(R.id.tvYear);
            ivBookIcon = itemView.findViewById(R.id.ivBookIcon); // ✅ FIXED!
            tvReadBadge = itemView.findViewById(R.id.tvReadBadge);
        }
    }
}
