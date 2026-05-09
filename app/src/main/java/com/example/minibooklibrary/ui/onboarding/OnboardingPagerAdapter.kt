package com.example.minibooklibrary.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.minibooklibrary.databinding.ItemOnboardingPageBinding

/**
 * Tiny RecyclerView adapter wired into a ViewPager2. Three static pages — no diffing, no
 * payloads, just bind once and render.
 */
class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.illustration.setImageResource(page.illustrationRes)
            binding.title.setText(page.titleRes)
            binding.body.setText(page.bodyRes)
        }
    }
}
