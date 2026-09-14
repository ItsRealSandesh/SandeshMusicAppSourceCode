package com.sandeshmusic.app.player

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class AudioEffectsManager {

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0
    private var currentBoostPercent: Int = 100
    private var currentBassBoostPercent: Int = 0

    @Synchronized
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentSessionId == audioSessionId && loudnessEnhancer != null) return

        currentSessionId = audioSessionId
        releaseEffects()

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                applyBoostInternal(this, currentBoostPercent)
            }
        } catch (e: Exception) {
            Log.w("AudioEffectsManager", "Could not initialize LoudnessEnhancer: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                applyBassInternal(this, currentBassBoostPercent)
            }
        } catch (e: Exception) {
            Log.w("AudioEffectsManager", "Could not initialize BassBoost: ${e.message}")
        }
    }

    @Synchronized
    fun setBoostPercent(percent: Int) {
        currentBoostPercent = percent.coerceIn(100, 250)
        loudnessEnhancer?.let { enhancer ->
            applyBoostInternal(enhancer, currentBoostPercent)
        }
    }

    @Synchronized
    fun setBassBoostPercent(strengthPercent: Int) {
        currentBassBoostPercent = strengthPercent.coerceIn(0, 100)
        bassBoost?.let { bb ->
            applyBassInternal(bb, currentBassBoostPercent)
        }
    }

    private fun applyBoostInternal(enhancer: LoudnessEnhancer, percent: Int) {
        try {
            if (percent > 100) {
                // Map 100% - 250% into 0 - 2500 mB (where 1000 mB is +10 dB gain)
                val targetGainMb = ((percent - 100) * 16).coerceIn(0, 2800)
                enhancer.setTargetGain(targetGainMb)
                enhancer.enabled = true
            } else {
                enhancer.setTargetGain(0)
                enhancer.enabled = false
            }
        } catch (e: Exception) {
            Log.w("AudioEffectsManager", "Error configuring LoudnessEnhancer: ${e.message}")
        }
    }

    private fun applyBassInternal(bb: BassBoost, strengthPercent: Int) {
        try {
            if (strengthPercent > 0 && bb.strengthSupported) {
                val strength = (strengthPercent * 10).toShort().coerceIn(0, 1000)
                bb.setStrength(strength)
                bb.enabled = true
            } else {
                bb.enabled = false
            }
        } catch (e: Exception) {
            Log.w("AudioEffectsManager", "Error configuring BassBoost: ${e.message}")
        }
    }

    @Synchronized
    fun releaseEffects() {
        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // Ignored
        }
        loudnessEnhancer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            // Ignored
        }
        bassBoost = null
    }
}
