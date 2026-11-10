/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 1 / 10 — ValidationRegistry.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The single source of truth for every validation gate in M2.9. It
 * declares which gates exist, their dependencies, their exit criteria,
 * and their order. Other validation files register themselves here;
 * the analysis phase reads this registry to know what was verified.
 *
 * This file is observation-only — it never executes checks itself.
 * Rule #76 (Performance Guards Never Optimize) extends to all M2.9
 * files: observe, report, never auto-fix.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                   VALIDATION REGISTRY                        │
 *   │                                                              │
 *   │  Gate G1 — Library Reality Test (10K/20K/50K/100K)           │
 *   │  Gate G2 — Memory Audit (leak/bitmap/coroutine/flow/snap)    │
 *   │  Gate G3 — Frame Timing (30/60/90/120/144 Hz)                │
 *   │  Gate G4 — Animation Continuity (100 morph cycles)           │
 *   │  Gate G5 — Stress Test (interleaved actions)                 │
 *   │  Gate G6 — Identity Test (5-second blind recognition)        │
 *   │                                                              │
 *   │  Exit Criteria — All 6 gates pass                            │
 *   │  Rule References — #53, #72, #76, #83                        │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * GATE DEPENDENCY GRAPH
 * ---------------------
 *
 *   G1 (Library Reality) ─┐
 *                         ├─→ G5 (Stress) ─→ G6 (Identity)
 *   G2 (Memory)          ─┤                  ↑
 *   G3 (Frame)           ─┤                  │
 *   G4 (Animation)       ─┘                  │
 *                                            │
 *   Exit: G1 ∧ G2 ∧ G3 ∧ G4 ∧ G5 ∧ G6  ──────┘
 *
 * RULES ENFORCED
 * --------------
 * #53  Diagnostics Never Affects Engine — registry is observation-only
 * #72  Identity Before Feature — G6 is the most important gate
 * #76  Performance Guards Never Optimize — report, never auto-fix
 * #83  Coordinator Executes — registry verifies Rule #83 enforcement
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// GATE IDENTIFIERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Stable identifier for each M2.9 validation gate.
 *
 * Stable IDs matter because the analysis phase (Phase 2) reads these
 * from exported reports — renaming breaks historical comparison.
 */
enum class ValidationGateId(val code: String) {
    /** Gate G1 — Library reality test across scale tiers. */
    LIBRARY_REALITY("G1"),

    /** Gate G2 — Memory audit across screen transitions. */
    MEMORY_AUDIT("G2"),

    /** Gate G3 — Frame timing audit across refresh rates. */
    FRAME_TIMING("G3"),

    /** Gate G4 — Animation continuity across 100 morph cycles. */
    ANIMATION_CONTINUITY("G4"),

    /** Gate G5 — Stress test with interleaved actions. */
    STRESS_TEST("G5"),

    /** Gate G6 — Identity test, 5-second blind recognition. ⭐ */
    IDENTITY_TEST("G6");

    companion object {
        /** All gates in canonical execution order. */
        val canonicalOrder: List<ValidationGateId> =
            listOf(LIBRARY_REALITY, MEMORY_AUDIT, FRAME_TIMING, ANIMATION_CONTINUITY, STRESS_TEST, IDENTITY_TEST)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GATE DECLARATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Immutable declaration of a single validation gate.
 *
 * @property id              Stable gate ID (G1–G6).
 * @property title           Human-readable name.
 * @property purpose         One-paragraph description of what this gate proves.
 * @property exitCriteria    Bullet list of conditions that must all hold for pass.
 * @property ruleReferences  Rule numbers this gate enforces (e.g. "#72", "#76").
 * @property dependsOn       Other gates that must run before this one.
 * @property estimatedDurationMinutes  Approximate runtime, for scheduling.
 * @property isBlocking      If true, failure blocks Milestone 2 LOCK.
 */
data class ValidationGateDeclaration(
    val id: ValidationGateId,
    val title: String,
    val purpose: String,
    val exitCriteria: List<String>,
    val ruleReferences: List<String>,
    val dependsOn: List<ValidationGateId>,
    val estimatedDurationMinutes: Int,
    val isBlocking: Boolean,
) {
    init {
        require(exitCriteria.isNotEmpty()) {
            "Gate ${id.code} must declare at least one exit criterion"
        }
        require(ruleReferences.isNotEmpty()) {
            "Gate ${id.code} must reference at least one rule"
        }
        require(estimatedDurationMinutes > 0) {
            "Gate ${id.code} duration must be positive"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GATE STATUS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Lifecycle status of a gate execution.
 *
 * Order: NOT_STARTED → SCHEDULED → RUNNING → COMPLETED → REPORTED
 * Any state may transition to BLOCKED (dependency failed) or SKIPPED
 * (explicitly waived by Product Director).
 */
enum class GateStatus {
    NOT_STARTED,
    SCHEDULED,
    RUNNING,
    COMPLETED,
    REPORTED,
    BLOCKED,
    SKIPPED,
    FAILED;

    /** True if this status represents a terminal state. */
    val isTerminal: Boolean
        get() = this in setOf(COMPLETED, REPORTED, BLOCKED, SKIPPED, FAILED)

    /** True if this status counts as a pass for exit-criteria evaluation. */
    val isPass: Boolean
        get() = this in setOf(COMPLETED, REPORTED)
}

/**
 * Mutable runtime status of a single gate.
 *
 * Wraps the immutable [ValidationGateDeclaration] with execution state.
 * Updates are made through [ValidationRegistry] methods only —
 * external mutation is forbidden.
 */
class GateRuntimeStatus(
    val declaration: ValidationGateDeclaration,
) {
    private var _status: GateStatus = GateStatus.NOT_STARTED
    private var _startedAtMs: Long? = null
    private var _completedAtMs: Long? = null
    private var _failureReason: String? = null
    private var _reportReference: String? = null

    val status: GateStatus get() = _status
    val startedAtMs: Long? get() = _startedAtMs
    val completedAtMs: Long? get() = _completedAtMs
    val failureReason: String? get() = _failureReason
    val reportReference: String? get() = _reportReference

    val elapsedMs: Long?
        get() {
            val start = _startedAtMs ?: return null
            val end = _completedAtMs ?: System.currentTimeMillis()
            return end - start
        }

    internal fun markScheduled() {
        require(_status == GateStatus.NOT_STARTED) {
            "Gate ${declaration.id.code} cannot be scheduled from $_status"
        }
        _status = GateStatus.SCHEDULED
    }

    internal fun markRunning() {
        require(_status in setOf(GateStatus.NOT_STARTED, GateStatus.SCHEDULED)) {
            "Gate ${declaration.id.code} cannot start from $_status"
        }
        _status = GateStatus.RUNNING
        _startedAtMs = System.currentTimeMillis()
    }

    internal fun markCompleted(reportReference: String) {
        require(_status == GateStatus.RUNNING) {
            "Gate ${declaration.id.code} cannot complete from $_status"
        }
        _status = GateStatus.COMPLETED
        _completedAtMs = System.currentTimeMillis()
        _reportReference = reportReference
    }

    internal fun markReported() {
        require(_status == GateStatus.COMPLETED) {
            "Gate ${declaration.id.code} cannot be reported from $_status"
        }
        _status = GateStatus.REPORTED
    }

    internal fun markBlocked(reason: String) {
        _status = GateStatus.BLOCKED
        _failureReason = reason
    }

    internal fun markFailed(reason: String) {
        _status = GateStatus.FAILED
        _failureReason = reason
        _completedAtMs = System.currentTimeMillis()
    }

    internal fun markSkipped(reason: String) {
        _status = GateStatus.SKIPPED
        _failureReason = reason
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REGISTRY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The registry of all M2.9 validation gates.
 *
 * Created once per validation run. Holds immutable declarations and
 * mutable runtime statuses. Provides dependency resolution and
 * exit-criteria evaluation.
 *
 * Thread-safety: instances are intended for single-threaded use within
 * a validation orchestrator. Concurrent mutation is the caller's
 * responsibility.
 */
class ValidationRegistry {

    private val _declarations: MutableMap<ValidationGateId, ValidationGateDeclaration> = linkedMapOf()
    private val _statuses: MutableMap<ValidationGateId, GateRuntimeStatus> = linkedMapOf()

    init {
        registerAllDefaults()
    }

    // ─── Registration ───────────────────────────────────────────────────────

    /**
     * Register a custom gate declaration. Useful when the Product Director
     * inserts an additional gate without modifying this file.
     */
    fun register(declaration: ValidationGateDeclaration) {
        require(declaration.id !in _declarations) {
            "Gate ${declaration.id.code} is already registered"
        }
        validateDependenciesAcyclic(declaration)
        _declarations[declaration.id] = declaration
        _statuses[declaration.id] = GateRuntimeStatus(declaration)
    }

    /**
     * Replace an existing declaration. Allowed only if the gate has not
     * started running — once started, declarations are frozen.
     */
    fun replace(declaration: ValidationGateDeclaration) {
        val existing = _statuses[declaration.id]
        require(existing == null || existing.status == GateStatus.NOT_STARTED) {
            "Gate ${declaration.id.code} cannot be replaced once started"
        }
        _declarations[declaration.id] = declaration
        _statuses[declaration.id] = GateRuntimeStatus(declaration)
    }

    // ─── Default declarations ──────────────────────────────────────────────

    private fun registerAllDefaults() {
        register(
            ValidationGateDeclaration(
                id = ValidationGateId.LIBRARY_REALITY,
                title = "Library Reality Test",
                purpose = "Verify the Library renders, scrolls, and ranks acceptably across " +
                    "10K, 20K, 50K, and 100K track scales. Proves Collection Engine scales.",
                exitCriteria = listOf(
                    "10K tracks — scroll frame time within budget",
                    "20K tracks — scroll frame time within budget",
                    "50K tracks — scroll frame time within budget, ranking <500ms",
                    "100K tracks — scroll frame time within budget, ranking <1s",
                    "No OOM during initial load at any scale",
                    "No jank during rapid scroll at any scale",
                ),
                ruleReferences = listOf("#53", "#76"),
                dependsOn = emptyList(),
                estimatedDurationMinutes = 30,
                isBlocking = true,
            )
        )

        register(
            ValidationGateDeclaration(
                id = ValidationGateId.MEMORY_AUDIT,
                title = "Memory Audit",
                purpose = "Detect leaks in Bitmap, Coroutine, Flow, and Snapshot across all " +
                    "major navigation cycles. Proves no resource is retained after navigation.",
                exitCriteria = listOf(
                    "Library → Now Playing → Library — zero retained Bitmaps",
                    "Library → Search → Library — zero retained Coroutine jobs",
                    "Now Playing → Queue → Now Playing — zero retained Flow collectors",
                    "Mini Player morph cycle — zero retained Snapshot states",
                    "LeakCanary heap dump analysis clean for 5 consecutive cycles",
                    "Native memory growth <2MB after 10-cycle churn",
                ),
                ruleReferences = listOf("#53", "#76"),
                dependsOn = emptyList(),
                estimatedDurationMinutes = 45,
                isBlocking = true,
            )
        )

        register(
            ValidationGateDeclaration(
                id = ValidationGateId.FRAME_TIMING,
                title = "Frame Timing Audit",
                purpose = "Verify frame budget is honored at 30, 60, 90, 120, and 144 Hz. " +
                    "Proves Wave Engine and Glass composition scale across refresh rates.",
                exitCriteria = listOf(
                    "30Hz — frame time <33ms, no dropped frames",
                    "60Hz — frame time <16ms, dropped frames <1%",
                    "90Hz — frame time <11ms, dropped frames <2%",
                    "120Hz — frame time <8ms, dropped frames <3%",
                    "144Hz — frame time <6.9ms, dropped frames <5%",
                    "Wave Engine renders within budget at all rates",
                    "Glass blur composition within budget at all rates",
                ),
                ruleReferences = listOf("#63", "#64", "#74", "#76"),
                dependsOn = emptyList(),
                estimatedDurationMinutes = 60,
                isBlocking = true,
            )
        )

        register(
            ValidationGateDeclaration(
                id = ValidationGateId.ANIMATION_CONTINUITY,
                title = "Animation Continuity Audit",
                purpose = "Verify Mini Player ↔ Library ↔ Now Playing ↔ Back ↔ Search morph " +
                    "continuity holds for 100 consecutive cycles. Proves shared element " +
                    "transitions are stable.",
                exitCriteria = listOf(
                    "100 cycles completed without crash",
                    "No morph glitch observed in any cycle",
                    "No shared-element position drift after 100 cycles",
                    "Mini Player state restored exactly after each back navigation",
                    "No recomposition storm during morph (count logged)",
                    "Memory stable after 100 cycles (no growth)",
                ),
                ruleReferences = listOf("#67", "#73", "#74", "#76"),
                dependsOn = emptyList(),
                estimatedDurationMinutes = 90,
                isBlocking = true,
            )
        )

        register(
            ValidationGateDeclaration(
                id = ValidationGateId.STRESS_TEST,
                title = "Stress Test",
                purpose = "Interleave scroll, play, queue, favorite, search, rotate, background, " +
                    "foreground, Bluetooth, headset, and notification events. Proves the " +
                    "system remains coherent under chaotic load.",
                exitCriteria = listOf(
                    "10-minute interleaved session — no crash",
                    "Audio focus correctly handled through call simulation",
                    "Background → foreground transitions preserve playback state",
                    "Bluetooth connect/disconnect does not cause audio routing glitch",
                    "Headset plug/unplug correctly pauses/resumes",
                    "Notification controls remain responsive throughout",
                    "No ANR during interleaved actions",
                ),
                ruleReferences = listOf("#53", "#72", "#83"),
                dependsOn = listOf(
                    ValidationGateId.LIBRARY_REALITY,
                    ValidationGateId.MEMORY_AUDIT,
                    ValidationGateId.FRAME_TIMING,
                    ValidationGateId.ANIMATION_CONTINUITY,
                ),
                estimatedDurationMinutes = 120,
                isBlocking = true,
            )
        )

        register(
            ValidationGateDeclaration(
                id = ValidationGateId.IDENTITY_TEST,
                title = "Identity Test ⭐",
                purpose = "Cover the RIKO logo. Open the app. Within 5 seconds, can a reviewer " +
                    "recognize 'this is RIKO'? If yes — identity pass. This is the most " +
                    "important gate of M2.9.",
                exitCriteria = listOf(
                    "Logo covered (splash hidden, app icon masked)",
                    "Reviewer recognizes RIKO within 5 seconds",
                    "Glass + Wave + Color + Motion all identifiable",
                    "Reviewer cannot confuse RIKO with another music app",
                    "3 of 3 reviewers pass — unanimous identity recognition",
                ),
                ruleReferences = listOf("#72"),
                dependsOn = listOf(ValidationGateId.STRESS_TEST),
                estimatedDurationMinutes = 30,
                isBlocking = true,
            )
        )
    }

    // ─── Lookup ────────────────────────────────────────────────────────────

    fun declaration(id: ValidationGateId): ValidationGateDeclaration =
        _declarations[id] ?: error("Gate ${id.code} not registered")

    fun status(id: ValidationGateId): GateRuntimeStatus =
        _statuses[id] ?: error("Gate ${id.code} not registered")

    fun allDeclarations(): List<ValidationGateDeclaration> =
        ValidationGateId.canonicalOrder.map { _declarations[it]!! }

    fun allStatuses(): List<GateRuntimeStatus> =
        ValidationGateId.canonicalOrder.map { _statuses[it]!! }

    // ─── Dependency resolution ─────────────────────────────────────────────

    /**
     * Compute the set of gates that must run before [id] can start.
     * Includes transitive dependencies.
     */
    fun transitiveDependencies(id: ValidationGateId): Set<ValidationGateId> {
        val visited = mutableSetOf<ValidationGateId>()
        val stack = ArrayDeque<ValidationGateId>()
        stack.addAll(declaration(id).dependsOn)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current in visited) continue
            visited += current
            stack.addAll(declaration(current).dependsOn)
        }
        return visited
    }

    /**
     * Returns the next gate that can be started (not yet started, all
     * dependencies satisfied). Null if no gate can start.
     */
    fun nextRunnable(): ValidationGateId? {
        return ValidationGateId.canonicalOrder.firstOrNull { id ->
            val status = status(id)
            if (status.status != GateStatus.NOT_STARTED) return@firstOrNull false
            transitiveDependencies(id).all { dep ->
                status(dep).status.isPass
            }
        }
    }

    // ─── Exit criteria evaluation ──────────────────────────────────────────

    /**
     * Evaluate whether Milestone 2 can be LOCKED.
     *
     * Returns [ExitDecision] which either approves LOCK or lists the
     * blocking gates with their failure reasons.
     */
    fun evaluateExitCriteria(): ExitDecision {
        val blocking = allStatuses().filter { it.declaration.isBlocking }
        val failed = blocking.filter { !it.status.isPass }
        return if (failed.isEmpty()) {
            ExitDecision.Approved(
                gatesPassed = blocking.size,
                totalGates = allDeclarations().size,
            )
        } else {
            ExitDecision.Blocked(
                blockingGates = failed.map {
                    BlockingGate(
                        id = it.declaration.id,
                        title = it.declaration.title,
                        reason = it.failureReason ?: "Status: ${it.status}",
                    )
                }
            )
        }
    }

    // ─── Mutation (internal) ───────────────────────────────────────────────

    internal fun markScheduled(id: ValidationGateId) = status(id).markScheduled()
    internal fun markRunning(id: ValidationGateId) = status(id).markRunning()
    internal fun markCompleted(id: ValidationGateId, reportReference: String) =
        status(id).markCompleted(reportReference)
    internal fun markReported(id: ValidationGateId) = status(id).markReported()
    internal fun markBlocked(id: ValidationGateId, reason: String) = status(id).markBlocked(reason)
    internal fun markFailed(id: ValidationGateId, reason: String) = status(id).markFailed(reason)
    internal fun markSkipped(id: ValidationGateId, reason: String) = status(id).markSkipped(reason)

    // ─── Validation ────────────────────────────────────────────────────────

    private fun validateDependenciesAcyclic(declaration: ValidationGateDeclaration) {
        val visited = mutableSetOf<ValidationGateId>()
        val stack = ArrayDeque<ValidationGateId>()
        stack.addAll(declaration.dependsOn)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current == declaration.id) {
                error("Gate ${declaration.id.code} has a cyclic dependency on itself")
            }
            if (current in visited) continue
            visited += current
            val dep = _declarations[current]
                ?: error("Gate ${declaration.id.code} depends on unregistered gate ${current.code}")
            stack.addAll(dep.dependsOn)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EXIT DECISION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The verdict on whether Milestone 2 can be LOCKED.
 *
 * Sealed — either [Approved] or [Blocked]. No third state.
 */
sealed class ExitDecision {

    /**
     * All blocking gates passed. Milestone 2 may be LOCKED.
     */
    data class Approved(
        val gatesPassed: Int,
        val totalGates: Int,
    ) : ExitDecision() {
        init {
            require(gatesPassed <= totalGates) {
                "Passed gates ($gatesPassed) cannot exceed total ($totalGates)"
            }
        }
    }

    /**
     * One or more blocking gates failed. Milestone 2 cannot be LOCKED
     * until each blocking gate is resolved.
     */
    data class Blocked(
        val blockingGates: List<BlockingGate>,
    ) : ExitDecision() {
        init {
            require(blockingGates.isNotEmpty()) {
                "Blocked decision must list at least one blocking gate"
            }
        }
    }

    /**
     * True if this decision approves the LOCK.
     */
    val isApproved: Boolean get() = this is Approved

    /**
     * True if this decision blocks the LOCK.
     */
    val isBlocked: Boolean get() = this is Blocked
}

/**
 * A single gate that is blocking the LOCK decision.
 */
data class BlockingGate(
    val id: ValidationGateId,
    val title: String,
    val reason: String,
)

// ═══════════════════════════════════════════════════════════════════════════
// REGISTRY SNAPSHOT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Immutable snapshot of the registry at a point in time.
 *
 * Used for exporting the registry state to a report without exposing
 * the mutable [GateRuntimeStatus] objects.
 */
data class ValidationRegistrySnapshot(
    val timestampMs: Long,
    val gates: List<GateSnapshot>,
    val exitDecision: ExitDecision,
)

/**
 * Immutable snapshot of a single gate.
 */
data class GateSnapshot(
    val id: ValidationGateId,
    val title: String,
    val status: GateStatus,
    val startedAtMs: Long?,
    val completedAtMs: Long?,
    val elapsedMs: Long?,
    val failureReason: String?,
    val reportReference: String?,
    val exitCriteria: List<String>,
    val ruleReferences: List<String>,
    val dependsOn: List<ValidationGateId>,
    val isBlocking: Boolean,
)

/**
 * Take an immutable snapshot of the entire registry.
 */
fun ValidationRegistry.snapshot(): ValidationRegistrySnapshot {
    return ValidationRegistrySnapshot(
        timestampMs = System.currentTimeMillis(),
        gates = allStatuses().map { status ->
            GateSnapshot(
                id = status.declaration.id,
                title = status.declaration.title,
                status = status.status,
                startedAtMs = status.startedAtMs,
                completedAtMs = status.completedAtMs,
                elapsedMs = status.elapsedMs,
                failureReason = status.failureReason,
                reportReference = status.reportReference,
                exitCriteria = status.declaration.exitCriteria,
                ruleReferences = status.declaration.ruleReferences,
                dependsOn = status.declaration.dependsOn,
                isBlocking = status.declaration.isBlocking,
            )
        },
        exitDecision = evaluateExitCriteria(),
    )
}
