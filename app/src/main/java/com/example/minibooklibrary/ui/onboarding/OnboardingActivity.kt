package com.example.minibooklibrary.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.ActivityOnboardingBinding
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.auth.AuthActivity

/**
 * 3-slide onboarding shown only on the first launch. The "completed" flag is flipped
 * the moment the user moves past this screen — see [finishOnboarding].
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            bodyRes = R.string.onboarding_body_1,
            illustrationRes = R.drawable.illustration_onboarding_1
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            bodyRes = R.string.onboarding_body_2,
            illustrationRes = R.drawable.illustration_onboarding_2
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            bodyRes = R.string.onboarding_body_3,
            illustrationRes = R.drawable.illustration_onboarding_3
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingPagerAdapter(pages)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderState(position)
        })

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current >= pages.lastIndex) finishOnboarding()
            else binding.viewPager.currentItem = current + 1
        }

        renderState(0)
    }

    private fun renderState(position: Int) {
        renderDots(position)
        val isLast = position == pages.lastIndex
        binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        binding.btnNext.text = getString(
            if (isLast) R.string.onboarding_get_started else R.string.onboarding_next
        )
    }

    private fun renderDots(activePosition: Int) {
        binding.dotIndicator.removeAllViews()
        val inflater = LayoutInflater.from(this)
        repeat(pages.size) { index ->
            val dot = inflater.inflate(R.layout.view_indicator_dot, binding.dotIndicator, false) as ImageView
            dot.setImageResource(
                if (index == activePosition) R.drawable.bg_dot_indicator_active
                else R.drawable.bg_dot_indicator_inactive
            )
            (dot.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = if (index == 0) 0 else dpToPx(6)
            }
            binding.dotIndicator.addView(dot)
        }
    }

    private fun finishOnboarding() {
        ServiceLocator.providePreferences(this).onboardingCompleted = true
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
