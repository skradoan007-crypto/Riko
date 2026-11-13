package com.rikodev.music.android.dna.core

/** Render-thread state. No demo clock, decoder, database or offline fallback. */
class LiveDotInput(private val source: BandSource) {
    init { require(source.bandCount == 40) }
    private val target = FloatArray(40)
    private val fastFollower = BandFollower(40)
    private val trailFollower = BandFollower(40)
    val fast: FloatArray get() = fastFollower.out
    val trail: FloatArray get() = trailFollower.out
    var hasSignal = false
        private set

    fun step(nowNanos: Long, dt: Float, playing: Boolean, frozen: Boolean) {
        hasSignal = playing && source.sample(nowNanos, target)
        if (!hasSignal) target.fill(0f)
        // A pause or missing PCM settles to rest even when motion is frozen.
        if (!frozen || !hasSignal) {
            fastFollower.step(target, dt, .018f, .115f)
            trailFollower.step(target, dt, .090f, .240f)
        }
    }
    fun reset() {
        target.fill(0f); fastFollower.reset(); trailFollower.reset(); hasSignal = false
    }
}
