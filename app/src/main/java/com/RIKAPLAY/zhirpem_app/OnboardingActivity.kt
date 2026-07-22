package com.RIKAPLAY.zhirpem_app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

data class OnboardingPage(val imageResId: Int, val title: String, val description: String)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!sharedPrefs.getBoolean("is_first_launch", true)) {
            // Убеждаемся, что MainActivity включена (могла быть выключена при смене иконки)
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)

        val pages = listOf(
            OnboardingPage(
                R.drawable.onboarding4,
                "Удобная лента и посты",
                "Читайте публикации, следите за обновлениями друзей и будьте в курсе всех событий в единой интерактивной ленте."
            ),
            OnboardingPage(
                R.drawable.onboarding3,
                "Личный профиль",
                "Настраивайте свой профиль, набирайте читателей и делитесь важными моментами своей жизни."
            ),
            OnboardingPage(
                R.drawable.onboarding2,
                "Делитесь контентом",
                "Публикуйте мысли, прикрепляйте фото, видео, GIF-анимации и создавайте опросы в пару кликов."
            ),
            OnboardingPage(
                R.drawable.onboarding1,
                "Личные чаты",
                "Общайтесь с друзьями тет-а-тет, отправляйте стикеры, медиафайлы и голосовые сообщения."
            ),
            OnboardingPage(
                R.drawable.onboarding,
                "Полная кастомизация",
                "Гибко настраивайте внешний вид: выбирайте акцентные цвета, темы оформления и эффект жидкого стекла под себя."
            )
        )

        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == pages.size - 1) {
                    btnNext.text = "Начать"
                } else {
                    btnNext.text = "Далее"
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
        
        // Убеждаемся, что MainActivity включена перед переходом
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivOnboarding)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.ivImage.setImageResource(page.imageResId)
        holder.tvTitle.text = page.title
        holder.tvDescription.text = page.description
    }

    override fun getItemCount(): Int = pages.size
}
