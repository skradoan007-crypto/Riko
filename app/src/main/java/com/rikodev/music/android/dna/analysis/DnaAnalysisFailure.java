package com.rikodev.music.android.dna.analysis;

/** Public, path-free diagnostics. Never display a decoder/provider exception message. */
public enum DnaAnalysisFailure {
    ACCESS("Audio access denied. Check RIKO's music permission."),
    MISSING_FILE("Audio file could not be opened. Check that it is still on this device."),
    NO_AUDIO("No audio track was found in this file."),
    FORMAT("The offline analyzer cannot read this PCM format."),
    TIMESTAMP("Decoded audio timestamps are discontinuous. DNA was not saved."),
    INCOMPLETE("Decoding ended before the declared duration. DNA was not saved."),
    EMPTY("The decoder returned no usable audio."),
    STALLED("The offline decoder stopped responding. Try again."),
    LIMIT("This audio exceeds the analyzer's supported duration or timestamp range."),
    DECODE("Offline decoding failed on this device."),
    ANALYSIS("Audio feature analysis failed."),
    STORAGE("The DNA profile or timeline could not be read or saved. Check free storage.");

    private final String description;
    DnaAnalysisFailure(String description) { this.description = description; }
    public String getDescription() { return description; }

    public static DnaAnalysisFailure classify(Exception error) {
        if (error instanceof SecurityException) return ACCESS;
        if (error instanceof java.io.FileNotFoundException) return MISSING_FILE;
        // Only exact messages owned by our shared decoder/analyzer are recognized.
        String message = error.getMessage();
        if (message == null) message = "";
        switch (message) {
            case "No audio track": return NO_AUDIO;
            case "Unsupported PCM format":
            case "Invalid PCM bounds/alignment":
            case "Midstream PCM format change": return FORMAT;
            case "Missing initial audio":
            case "Decoder timestamp discontinuity": return TIMESTAMP;
            case "Decode ended before declared duration": return INCOMPLETE;
            case "Empty decode":
            case "Empty decoded audio":
            case "No non-priming audio": return EMPTY;
            case "Decoder stalled": return STALLED;
            case "Invalid/over-limit PCM timestamp":
            case "Analysis duration limit exceeded": return LIMIT;
            default:
                return error instanceof java.io.IOException || error instanceof IllegalStateException
                        ? DECODE : ANALYSIS;
        }
    }
}
