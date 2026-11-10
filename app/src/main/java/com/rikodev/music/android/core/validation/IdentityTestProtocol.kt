/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 7 / 10 — IdentityTestProtocol.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G6 — Identity Test. THE MOST IMPORTANT GATE OF M2.9.
 *
 * Protocol:
 *   1. Mask the RIKO logo (splash hidden, app icon masked).
 *   2. Open the app on a fresh device.
 *   3. Within 5 seconds, a reviewer must recognize:
 *        "এটা RIKO"
 *
 * If yes → identity pass.
 * If no  → identity fail. Brand work required.
 *
 * WHY THIS MATTERS
 * ---------------
 * Feature copy করা যায়। Feeling copy করা কঠিন। (Rule #72.)
 *
 * Spotify has its green. Apple Music has its pink-red. RIKO must have
 * its own signature — Glass + Wave + Color + Motion combined into a
 * singular recognizable identity. If a reviewer cannot recognize
 * RIKO in 5 seconds, we have built a generic music player, not an
 * Experience OS.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                  IDENTITY TEST PROTOCOL                      │
 *   │                                                              │
 *   │  Reviewer Pool                                               │
 *   │    3+ reviewers (unanimous pass required)                    │
 *   │    Mix of: familiar / unfamiliar / first-time                │
 *   │                                                              │
 *   │  Test Sessions                                               │
 *   │    Per reviewer: 3 sessions (cold / warm / repeated)         │
 *   │                                                              │
 *   │  Per-Session Capture                                         │
 *   │    5-second screen recording                                 │
 *   │    Logo / icon / app name masked                             │
 *   │    Reviewer declares: "Yes RIKO" / "Not sure" / "No"         │
 *   │    Recognition time recorded (ms)                            │
 *   │    Recognition signals recorded                              │
 *   │                                                              │
 *   │  Pass Criteria                                               │
 *   │    3 of 3 reviewers recognize in ≤5s in 2 of 3 sessions     │
 *   │    No reviewer fails 2+ sessions                             │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * IDENTITY SIGNALS (what makes RIKO recognizable?)
 * ------------------------------------------------
 *   - Glass depth (background never flat)
 *   - Wave breathing (subtle motion at idle)
 *   - Color saturation from album art
 *   - Hero scale and presence
 *   - Mini Player positioning and behavior
 *   - Section spacing rhythm
 *   - Loading shimmer pattern
 *
 * RULES ENFORCED
 * --------------
 * #63  Background Never Demands Attention — but contributes identity
 * #65  Hero Never Competes — but contributes identity
 * #67  Dock Never Interrupts — but contributes identity
 * #72  Identity Before Feature — this is THE identity gate
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// REVIEWER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A test reviewer participating in the identity test.
 *
 * @property id            Stable reviewer ID (e.g. "R1").
 * @property displayName   Human-readable name (may be anonymized).
 * @property familiarity   How familiar the reviewer is with RIKO.
 */
data class IdentityReviewer(
    val id: String,
    val displayName: String,
    val familiarity: ReviewerFamiliarity,
)

/**
 * Familiarity levels for the reviewer pool.
 *
 * A balanced pool has at least one of each.
 */
enum class ReviewerFamiliarity(val displayName: String) {
    /** Has used RIKO extensively. Likely recognizes quickly. */
    FAMILIAR("Familiar"),

    /** Has seen RIKO a few times. Tests recognition strength. */
    CASUAL("Casual"),

    /** Never seen RIKO. Tests pure identity recognizability. */
    FIRST_TIME("First-time"),
}

// ═══════════════════════════════════════════════════════════════════════════
// SESSION TYPE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Type of identity test session.
 */
enum class SessionType(val displayName: String, val description: String) {
    /** App launched from killed state. Tests cold-start identity. */
    COLD_START("Cold Start", "App launched from killed state — tests splash + first frame identity"),

    /** App resumed from background. Tests warm identity. */
    WARM_RESUME("Warm Resume", "App resumed from background — tests retention of identity"),

    /** Repeated session within short window. Tests identity persistence. */
    REPEATED("Repeated", "Session repeated within 30 minutes — tests identity durability"),
}

// ═══════════════════════════════════════════════════════════════════════════
// IDENTITY SIGNALS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Recognizable identity signals a reviewer may cite.
 */
enum class IdentitySignal(val displayName: String, val description: String) {
    GLASS_DEPTH("Glass Depth", "Background has visible depth / blur / layering"),
    WAVE_BREATHING("Wave Breathing", "Subtle wave motion visible at idle"),
    COLOR_SATURATION("Color Saturation", "Screen colors feel derived from album art"),
    HERO_PRESENCE("Hero Presence", "Hero card has distinctive scale and breathing"),
    DOCK_STYLE("Dock Style", "Mini Player has distinctive position and behavior"),
    SECTION_RHYTHM("Section Rhythm", "Sections have distinctive spacing / stagger"),
    LOADING_SHIMMER("Loading Shimmer", "Loading state has distinctive shimmer pattern"),
    MOTION_LANGUAGE("Motion Language", "Animation has distinctive easing / spring"),
    EMPTY_STATE("Empty State", "Empty state has distinctive personality"),
    GLASS_TINT("Glass Tint", "Glass surfaces have distinctive tint color"),
}

// ═══════════════════════════════════════════════════════════════════════════
// RECOGNITION OUTCOME
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The reviewer's recognition verdict.
 */
enum class RecognitionVerdict(val displayName: String) {
    /** Reviewer declared "এটা RIKO" within 5 seconds. */
    RECOGNIZED("Recognized"),

    /** Reviewer declared "Not sure" or took >5 seconds. */
    UNCERTAIN("Uncertain"),

    /** Reviewer declared "I don't recognize this" or misidentified. */
    NOT_RECOGNIZED("Not Recognized"),

    /** Reviewer misidentified as another app. */
    MISIDENTIFIED("Misidentified"),
}

/**
 * A single session's result.
 *
 * @property sessionId           Stable session ID.
 * @property reviewerId          Reviewer who ran this session.
 * @property sessionType         Cold / warm / repeated.
 * @property recognitionMs       Time from app-open to reviewer declaration.
 * @property verdict             Reviewer's verdict.
 * @property signalsCited        Identity signals the reviewer cited.
 * @property misidentifiedAs     If [verdict] is MISIDENTIFIED, what they
 *                               thought it was.
 * @property notes               Free-form reviewer notes.
 * @property recordingPath       Path to the 5-second screen recording.
 * @property timestampMs         When the session ran.
 */
data class IdentitySession(
    val sessionId: String,
    val reviewerId: String,
    val sessionType: SessionType,
    val recognitionMs: Long,
    val verdict: RecognitionVerdict,
    val signalsCited: List<IdentitySignal>,
    val misidentifiedAs: String?,
    val notes: String?,
    val recordingPath: String?,
    val timestampMs: Long,
) {
    /** True if this session counts as a recognition pass. */
    val isRecognitionPass: Boolean
        get() = verdict == RecognitionVerdict.RECOGNIZED && recognitionMs <= 5_000

    /** True if reviewer was within time budget but uncertain. */
    val isWithinTimeBudget: Boolean get() = recognitionMs <= 5_000
}

// ═══════════════════════════════════════════════════════════════════════════
// REVIEWER SUMMARY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Aggregated result for one reviewer across their sessions.
 */
data class ReviewerSummary(
    val reviewer: IdentityReviewer,
    val sessions: List<IdentitySession>,
) {
    /** Number of sessions that passed recognition. */
    val recognitionPassCount: Int get() = sessions.count { it.isRecognitionPass }

    /** Number of sessions that failed recognition. */
    val recognitionFailCount: Int get() = sessions.count { !it.isRecognitionPass }

    /** Mean recognition time across all sessions. */
    val meanRecognitionMs: Long
        get() = if (sessions.isEmpty()) 0 else sessions.sumOf { it.recognitionMs } / sessions.size

    /** All signals cited across all sessions. */
    val allSignalsCited: List<IdentitySignal>
        get() = sessions.flatMap { it.signalsCited }.distinct()

    /** Most-cited signal across sessions. */
    val mostCitedSignal: IdentitySignal?
        get() = sessions.flatMap { it.signalsCited }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    /** True if reviewer passed in 2+ of 3 sessions. */
    val reviewerPass: Boolean get() = recognitionPassCount >= 2
}

// ═══════════════════════════════════════════════════════════════════════════
// IDENTITY TEST PROTOCOL
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Identity Test Protocol (Gate G6). ⭐
 *
 * Aggregates sessions across a reviewer pool and produces the final
 * identity verdict. This is the single most important gate of M2.9 —
 * failure here means RIKO does not have a recognizable identity.
 *
 * Usage:
 *   1. [registerReviewer] for each reviewer in the pool
 *   2. [recordSession] for each session run
 *   3. [evaluate] for the final verdict
 */
class IdentityTestProtocol(
    private val requiredReviewers: Int = 3,
    private val sessionsPerReviewer: Int = 3,
    private val recognitionTimeBudgetMs: Long = 5_000,
    private val requiredSessionsToPass: Int = 2,
) {

    private val _reviewers: MutableMap<String, IdentityReviewer> = mutableMapOf()
    private val _sessions: MutableList<IdentitySession> = mutableListOf()

    val reviewers: List<IdentityReviewer> get() = _reviewers.values.toList()
    val sessions: List<IdentitySession> get() = _sessions.toList()

    // ─── Registration ─────────────────────────────────────────────────────

    /**
     * Register a reviewer in the pool.
     */
    fun registerReviewer(reviewer: IdentityReviewer) {
        require(reviewer.id !in _reviewers) {
            "Reviewer ${reviewer.id} already registered"
        }
        _reviewers[reviewer.id] = reviewer
    }

    /**
     * Record a session's outcome.
     */
    fun recordSession(session: IdentitySession) {
        require(session.reviewerId in _reviewers) {
            "Session references unknown reviewer ${session.reviewerId}"
        }
        _sessions += session
    }

    // ─── Per-reviewer summaries ───────────────────────────────────────────

    /**
     * Compute summary for one reviewer.
     */
    fun summaryFor(reviewerId: String): ReviewerSummary? {
        val reviewer = _reviewers[reviewerId] ?: return null
        val reviewerSessions = _sessions.filter { it.reviewerId == reviewerId }
        return ReviewerSummary(reviewer = reviewer, sessions = reviewerSessions)
    }

    /**
     * Compute summaries for all reviewers.
     */
    fun allSummaries(): List<ReviewerSummary> =
        _reviewers.keys.mapNotNull { summaryFor(it) }

    // ─── Aggregate statistics ─────────────────────────────────────────────

    /**
     * Most-cited identity signal across all sessions.
     */
    fun mostCitedSignalAcrossAll(): IdentitySignal? =
        _sessions.flatMap { it.signalsCited }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    /**
     * Mean recognition time across all sessions.
     */
    fun meanRecognitionMs(): Long =
        if (_sessions.isEmpty()) 0 else _sessions.sumOf { it.recognitionMs } / _sessions.size

    /**
     * Count of sessions where reviewer misidentified the app.
     */
    fun misidentificationCount(): Int =
        _sessions.count { it.verdict == RecognitionVerdict.MISIDENTIFIED }

    // ─── Evaluation ───────────────────────────────────────────────────────

    /**
     * Final verdict for Gate G6. ⭐
     *
     * Pass requires:
     *   - At least [requiredReviewers] reviewers registered
     *   - Each reviewer ran at least [sessionsPerReviewer] sessions
     *   - Each reviewer passed recognition in [requiredSessionsToPass]+ sessions
     *   - No reviewer misidentified the app in any session
     *   - Mean recognition time across all sessions ≤ [recognitionTimeBudgetMs]
     */
    fun evaluate(): GateG6Verdict {
        if (_reviewers.size < requiredReviewers) {
            return GateG6Verdict.Incomplete(
                reviewersRegistered = _reviewers.size,
                required = requiredReviewers,
            )
        }
        val summaries = allSummaries()
        val insufficientSessions = summaries.filter { it.sessions.size < sessionsPerReviewer }
        if (insufficientSessions.isNotEmpty()) {
            return GateG6Verdict.Incomplete(
                reviewersRegistered = _reviewers.size,
                required = requiredReviewers,
                detail = "Reviewers with insufficient sessions: ${insufficientSessions.map { it.reviewer.id}}",
            )
        }
        val failedReviewers = summaries.filter { !it.reviewerPass }
        if (failedReviewers.isNotEmpty()) {
            return GateG6Verdict.Failed(
                reason = "Reviewers who failed 2+ of 3 sessions: ${failedReviewers.map { it.reviewer.id }}",
                failedReviewerIds = failedReviewers.map { it.reviewer.id },
                summaries = summaries,
            )
        }
        val misidentified = _sessions.filter { it.verdict == RecognitionVerdict.MISIDENTIFIED }
        if (misidentified.isNotEmpty()) {
            return GateG6Verdict.Failed(
                reason = "Misidentification in ${misidentified.size} session(s): " +
                    misidentified.joinToString { "${it.reviewerId}→${it.misidentifiedAs}" },
                failedReviewerIds = misidentified.map { it.reviewerId }.distinct(),
                summaries = summaries,
            )
        }
        val meanMs = meanRecognitionMs()
        if (meanMs > recognitionTimeBudgetMs) {
            return GateG6Verdict.Failed(
                reason = "Mean recognition time ${meanMs}ms exceeds budget ${recognitionTimeBudgetMs}ms",
                failedReviewerIds = emptyList(),
                summaries = summaries,
            )
        }
        return GateG6Verdict.Passed(
            reviewersPassed = summaries.size,
            meanRecognitionMs = meanMs,
            mostCitedSignal = mostCitedSignalAcrossAll(),
            summaries = summaries,
        )
    }

    /**
     * Human-readable summary.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G6 — IDENTITY TEST ⭐ ═══")
        appendLine()
        if (_reviewers.isEmpty()) {
            appendLine("No reviewers registered.")
            return@buildString
        }
        appendLine("Reviewers registered: ${_reviewers.size} / $requiredReviewers")
        appendLine("Sessions recorded:    ${_sessions.size}")
        appendLine("Mean recognition:     ${meanRecognitionMs()}ms (budget ${recognitionTimeBudgetMs}ms)")
        appendLine("Misidentifications:   ${misidentificationCount()}")
        appendLine()
        appendLine("Per-reviewer results:")
        allSummaries().forEach { summary ->
            val passFail = if (summary.reviewerPass) "PASS" else "FAIL"
            appendLine("  [${summary.reviewer.id}] ${summary.reviewer.displayName} " +
                "(${summary.reviewer.familiarity.displayName}) — $passFail")
            appendLine("      recognition pass: ${summary.recognitionPassCount}/${summary.sessions.size}")
            appendLine("      mean time:        ${summary.meanRecognitionMs}ms")
            appendLine("      most-cited signal: ${summary.mostCitedSignal?.displayName ?: "—"}")
        }
        appendLine()
        val mostCited = mostCitedSignalAcrossAll()
        appendLine("Most-cited signal across all: ${mostCited?.displayName ?: "—"}")
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G6.
 */
sealed class GateG6Verdict {
    /**
     * All reviewers recognized RIKO within budget. Identity pass. ⭐
     */
    data class Passed(
        val reviewersPassed: Int,
        val meanRecognitionMs: Long,
        val mostCitedSignal: IdentitySignal?,
        val summaries: List<ReviewerSummary>,
    ) : GateG6Verdict()

    /**
     * Not enough reviewers or sessions to evaluate.
     */
    data class Incomplete(
        val reviewersRegistered: Int,
        val required: Int,
        val detail: String? = null,
    ) : GateG6Verdict()

    /**
     * One or more reviewers failed to recognize RIKO.
     */
    data class Failed(
        val reason: String,
        val failedReviewerIds: List<String>,
        val summaries: List<ReviewerSummary>,
    ) : GateG6Verdict() {
        init {
            require(failedReviewerIds.isNotEmpty() || reason.isNotBlank()) {
                "Failed verdict must have a reason or failed reviewers"
            }
        }
    }

    val isPassed: Boolean get() = this is Passed
}
