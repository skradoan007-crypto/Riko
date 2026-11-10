package com.rikodev.music.android.presentation.minicontact

/**
 * Interruption types for the Mini Player gesture system.
 *
 * When a system event (phone call, notification, split-screen) or a
 * gesture cancel occurs, the reducer consults [resolvePolicy] to decide
 * how to handle the in-progress animation.
 *
 * References: MPRS Part K (Interruption Policy).
 */
object MiniPlayerInterruption {

    /**
     * What kind of interrupt occurred.
     */
    enum class InterruptType {
        /** Phone call, notification shade, split-screen. */
        SYSTEM_INTERRUPT,
        /** Back press while morph in progress. */
        BACK_PRESS,
        /** Navigation change (e.g. deep link to Now Playing). */
        NAVIGATION,
        /** Memory pressure / config change. */
        SYSTEM_LOW,
    }

    /**
     * How the Mini Player should respond to an interruption.
     */
    enum class InterruptPolicy {
        /** Reverse the current animation (spring back to start). */
        REVERSE,
        /** Cancel instantly — reset to idle. */
        CANCEL,
        /** Cancel but accelerate toward destination (e.g. finish dismiss). */
        CANCEL_TO_DESTINATION,
        /** Keep current state frozen. */
        PRESERVE,
        /** Pause animations but don't reset. */
        PAUSE,
        /** Speed up the current animation. */
        ACCELERATE,
        /** Continue as if nothing happened. */
        CONTINUE,
    }

    /**
     * Resolve the interruption policy based on current state and interrupt type.
     *
     * @param currentAnimation The active animation name (EXPANDING, CONTRACTING,
     *   SETTLING, DISMISSING, IDLE).
     * @param interruptType What triggered the interrupt.
     * @param morphProgress Current morph progress (0 = collapsed, 1 = expanded).
     */
    fun resolvePolicy(
        currentAnimation: String,
        interruptType: InterruptType,
        morphProgress: Float,
    ): InterruptPolicy {
        return when (interruptType) {
            InterruptType.SYSTEM_INTERRUPT -> {
                when (currentAnimation) {
                    "EXPANDING", "CONTRACTING" -> InterruptPolicy.CANCEL
                    "SETTLING" -> InterruptPolicy.CONTINUE
                    "DISMISSING" -> InterruptPolicy.CANCEL_TO_DESTINATION
                    else -> InterruptPolicy.PRESERVE
                }
            }
            InterruptType.BACK_PRESS -> {
                when (currentAnimation) {
                    "EXPANDING" -> InterruptPolicy.REVERSE
                    "CONTRACTING" -> InterruptPolicy.PRESERVE
                    "SETTLING" -> InterruptPolicy.REVERSE
                    "DISMISSING" -> InterruptPolicy.CANCEL_TO_DESTINATION
                    else -> InterruptPolicy.PRESERVE
                }
            }
            InterruptType.NAVIGATION -> {
                // If navigating to Now Playing, accelerate to complete
                if (morphProgress > 0.5f) InterruptPolicy.ACCELERATE
                else InterruptPolicy.CANCEL
            }
            InterruptType.SYSTEM_LOW -> {
                InterruptPolicy.PRESERVE
            }
        }
    }
}
