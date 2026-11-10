package com.rikodev.music.android.core.experience.audio

import kotlin.math.cos
import kotlin.math.sin

/**
 * Small radix-2 Float FFT implementation.
 */
class Fft(private val size: Int) {
    init {
        require(size > 0 && (size and (size - 1)) == 0) { "FFT size must be a power of 2" }
    }

    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)
    private val bitReverse = IntArray(size)

    init {
        for (i in 0 until size / 2) {
            val angle = -2.0 * Math.PI * i / size
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
        val log2Size = (31 - Integer.numberOfLeadingZeros(size))
        for (i in 0 until size) {
            var reversed = 0
            var temp = i
            for (j in 0 until log2Size) {
                reversed = (reversed shl 1) or (temp and 1)
                temp = temp shr 1
            }
            bitReverse[i] = reversed
        }
    }

    /**
     * Performs an in-place forward FFT.
     * @param real Real part of the input. Output will be in this array.
     * @param imag Imaginary part of the input. Output will be in this array.
     */
    fun forward(real: FloatArray, imag: FloatArray) {
        for (i in 0 until size) {
            val rev = bitReverse[i]
            if (i < rev) {
                val tr = real[i]
                real[i] = real[rev]
                real[rev] = tr
                val ti = imag[i]
                imag[i] = imag[rev]
                imag[rev] = ti
            }
        }

        var step = 2
        var halfStep = 1
        while (step <= size) {
            val tableStep = size / step
            for (i in 0 until size step step) {
                for (j in 0 until halfStep) {
                    val k = i + j
                    val m = k + halfStep
                    val tableIdx = j * tableStep
                    val tCos = cosTable[tableIdx]
                    val tSin = sinTable[tableIdx]

                    val rm = real[m]
                    val im = imag[m]
                    
                    val tr = tCos * rm - tSin * im
                    val ti = tCos * im + tSin * rm

                    real[m] = real[k] - tr
                    imag[m] = imag[k] - ti
                    real[k] += tr
                    imag[k] += ti
                }
            }
            halfStep = step
            step = step shl 1
        }
    }
}
