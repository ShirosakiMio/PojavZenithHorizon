package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.setting.Settings
import com.movtery.zalithlauncher.ui.fragment.settings.ControlSettingsFragment
import com.movtery.zalithlauncher.ui.fragment.settings.ExperimentalSettingsFragment
import com.movtery.zalithlauncher.ui.fragment.settings.GameSettingsFragment
import com.movtery.zalithlauncher.ui.fragment.settings.LauncherSettingsFragment
import com.movtery.zalithlauncher.ui.fragment.settings.VideoSettingsFragment
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.databinding.FragmentSettingsBinding

class SettingsFragment : FragmentWithAnim(R.layout.fragment_settings) {
    companion object {
        const val TAG: String = "SettingsFragment"
    }

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()

        binding.settingsTab.observeIndexChange { _, toIndex, reselect, _ ->
            if (reselect) return@observeIndexChange
            binding.settingsViewpager.currentItem = toIndex
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.refreshSettings()
    }

    private fun initViewPager() {
        binding.settingsViewpager.apply {
            adapter = ViewPagerAdapter(this@SettingsFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            setPageTransformer(MarginPageTransformer(Tools.dpToPx(12F).toInt()))
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                }
            })
        }
    }

    private fun onFragmentSelect(position: Int) {
        binding.settingsTab.onPageSelected(position)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.BounceInRight))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.FadeOutLeft))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.FadeOutUp))
    }

    private class ViewPagerAdapter(val fragment: FragmentWithAnim): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 6
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ControlSettingsFragment(fragment)
                2 -> GameSettingsFragment()
                3 -> LauncherSettingsFragment(fragment)
                4 -> ExperimentalSettingsFragment()
                else -> VideoSettingsFragment()
            }
        }
    }
}