package com.movtery.anim.animations.slide

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import com.movtery.anim.animations.BaseAnimator

class SlideOutLeftAnimator: BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(target, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(target, "translationX", 0f, -target.right.toFloat()))
    }
}