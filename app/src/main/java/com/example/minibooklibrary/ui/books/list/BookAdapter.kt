package com.example.minibooklibrary.ui.books.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.databinding.ItemBookHorizontalBinding
import com.example.minibooklibrary.databinding.ItemBookVerticalBinding
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.util.show
import com.example.minibooklibrary.util.showIf
import java.io.File

/**
 * Generic book adapter that renders either a vertical card (library list) or horizontal
 * tile (dashboard rail). Layout choice is fixed at construction; we don't switch via
 * `getItemViewType` because the consumer always knows which variant they want.
 */
class BookAdapter(
    private val variant: LayoutVariant,
    private val onClick: (BookEntity) -> Unit
) : ListAdapter<BookEntity, RecyclerView.ViewHolder>(DIFF) {

    enum class LayoutVariant { VERTICAL, HORIZONTAL }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (variant) {
            LayoutVariant.VERTICAL -> VerticalHolder(
                ItemBookVerticalBinding.inflate(inflater, parent, false)
            )
            LayoutVariant.HORIZONTAL -> HorizontalHolder(
                ItemBookHorizontalBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is VerticalHolder -> holder.bind(item, onClick)
            is HorizontalHolder -> holder.bind(item, onClick)
        }
    }

    /** Vertical card used in the main library list. */
    class VerticalHolder(
        private val binding: ItemBookVerticalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookEntity, onClick: (BookEntity) -> Unit) {
            binding.title.text = book.title
            binding.author.text = "by ${book.author}"
            binding.category.text = book.category
            binding.year.text = if (book.year > 0) book.year.toString() else "—"

            val status = ReadingStatus.fromStorageValue(book.readingStatus)
            binding.statusChip.text = status.displayLabel
            binding.statusChip.chipBackgroundColor = androidx.core.content.ContextCompat
                .getColorStateList(itemView.context, statusBackgroundColor(status))

            binding.rating.showIf(book.rating > 0f)
            binding.ratingText.text = String.format("%.1f", book.rating)

            // Cover: file URI when present, gradient placeholder otherwise.
            val cover = book.coverImageUri
            if (!cover.isNullOrBlank()) {
                binding.cover.load(File(cover)) {
                    crossfade(true)
                    placeholder(R.drawable.bg_cover_placeholder)
                    error(R.drawable.bg_cover_placeholder)
                }
                binding.cover.background = null
                binding.coverIcon.visibility = android.view.View.GONE
            } else {
                binding.cover.setImageDrawable(null)
                binding.cover.setBackgroundResource(R.drawable.bg_cover_placeholder)
                binding.coverIcon.show()
            }

            binding.root.setOnClickListener { onClick(book) }
        }
    }

    /** Compact tile used in the dashboard "recently added" rail. */
    class HorizontalHolder(
        private val binding: ItemBookHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookEntity, onClick: (BookEntity) -> Unit) {
            binding.title.text = book.title
            binding.author.text = book.author
            val cover = book.coverImageUri
            if (!cover.isNullOrBlank()) {
                binding.cover.load(File(cover)) {
                    crossfade(true)
                    placeholder(R.drawable.bg_cover_placeholder)
                    error(R.drawable.bg_cover_placeholder)
                }
                binding.cover.background = null
                binding.coverIcon.visibility = android.view.View.GONE
            } else {
                binding.cover.setImageDrawable(null)
                binding.cover.setBackgroundResource(R.drawable.bg_cover_placeholder)
                binding.coverIcon.show()
            }
            binding.root.setOnClickListener { onClick(book) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BookEntity>() {
            override fun areItemsTheSame(oldItem: BookEntity, newItem: BookEntity) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BookEntity, newItem: BookEntity) =
                oldItem == newItem
        }

        @androidx.annotation.ColorRes
        fun statusBackgroundColor(status: ReadingStatus): Int = when (status) {
            ReadingStatus.WANT_TO_READ -> R.color.status_want_bg
            ReadingStatus.CURRENTLY_READING -> R.color.status_reading_bg
            ReadingStatus.FINISHED -> R.color.status_finished_bg
        }
    }
}
