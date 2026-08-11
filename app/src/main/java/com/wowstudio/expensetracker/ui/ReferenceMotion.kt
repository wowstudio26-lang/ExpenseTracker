package com.wowstudio.expensetracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val MotionDuration = 260
private const val EmphasisDuration = 340

/** Reference-style screen transition: short, soft, and never flashy. */
@Composable
fun <T> ReferenceAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(MotionDuration, easing = FastOutSlowInEasing)) +
                slideInHorizontally(
                    animationSpec = tween(MotionDuration, easing = FastOutSlowInEasing),
                    initialOffsetX = { it / 12 }
                )) togetherWith
                (fadeOut(tween(MotionDuration / 2)) +
                    slideOutHorizontally(
                        animationSpec = tween(MotionDuration, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it / 18 }
                    )) using SizeTransform(clip = false)
        },
        label = "reference_screen_transition",
        content = { state -> content(state) }
    )
}

/** Subtle entrance used for cards, sections, and bottom-sheet-like content. */
@Composable
fun ReferenceVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(EmphasisDuration)) + scaleIn(
            initialScale = 0.97f,
            animationSpec = tween(EmphasisDuration, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(MotionDuration)) + scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(MotionDuration, easing = FastOutSlowInEasing)
        ),
        content = { content() }
    )
}

/** Smoothly animates a selected control between inactive and active states. */
@Composable
fun ReferenceSelectionScale(selected: Boolean): Float =
    animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(MotionDuration, easing = FastOutSlowInEasing),
        label = "reference_selection_scale"
    ).value

@Composable
fun ReferenceScaleOnSelection(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = ReferenceSelectionScale(selected)
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { content() }
}
