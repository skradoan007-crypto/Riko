# Harness Engineering — Worked Examples

## What is verified

All examples below follow the 10 verified principles (P1–P10) derived from the OpenAI Harness Engineering article.

---

## Example 1: Audit a project for agent-readiness

**Input**: "Audit my project for agent-readiness"

**Output format**:

```
## Agent-Readiness Audit

### Project: [name]
### Type: [language/framework]
### Architecture: [monolith/microservices/etc.]

| Dimension              | Score (0-10) | Status         |
|------------------------|-------------|----------------|
| Context Management     | 3/10        | Needs work     |
| Documentation Structure| 5/10        | Partial        |
| Mechanical Enforcement | 2/10        | Critical gap   |
| Agent Legibility       | 4/10        | Needs work     |
| Workflow Design        | 6/10        | Good           |

**Overall: 4.0/10**

### Top 3 Gaps
1. No CLAUDE.md — agents have no entry point (P1, P4)
2. Documentation is monolithic README — rots instantly (P3)
3. No custom linters — architecture not mechanically enforced (P5)

### Quick Wins
1. Generate CLAUDE.md (~100 lines) → immediate context improvement
2. Split README into docs/ structure → enables progressive disclosure
3. Add structural test for import layering → prevents architectural drift
```

---

## Example 2: Generate CLAUDE.md

**Input**: "Generate an CLAUDE.md for my TypeScript monorepo"

**Output** (kept under ~100 lines):

```markdown
# Agent Instructions

## Project Overview
TypeScript monorepo with 3 packages: `@core/api`, `@core/web`, `@core/shared`.
Built with pnpm workspaces. Tests via Vitest.

## Architecture
See [docs/architecture.md](docs/architecture.md) for the full map.

**Rule**: Dependencies flow one direction:
  shared → api → web
  Never import from a consumer back into a dependency.

## Critical Rules (must follow)
1. **Boundary validation** — parse all external data at API boundaries with Zod schemas (see [docs/principles.md](docs/principles.md#boundary-validation))
2. **Structured logging** — use `@core/shared/logger`, never `console.log` directly
3. **No YOLO data access** — always validate shapes at boundaries, never trust raw input

## Where to Find Things
- [Architecture map](docs/architecture.md) — package layering and domain boundaries
- [Golden principles](docs/principles.md) — opinionated rules and rationale
- [Quality grades](docs/quality.md) — per-domain quality status
- [Active plans](docs/plans/) — in-progress work with progress logs
- [Decisions](docs/decisions/) — architecture decision records

## Testing
- Unit tests: `vitest` — colocated with source (`*.test.ts`)
- Integration tests: `vitest` — in `tests/integration/`
- Run all: `pnpm test`
- Run one package: `pnpm --filter @core/api test`

## CI
- PR checks: lint, type-check, test, docs-freshness
- Merge: squash-merge after agent review passes

## Style Notes
- Prefer `async/await` over `.then()` chains
- Use `import type` for type-only imports
- Files stay under 300 lines — split if larger
```

**Principles applied**: P1 (scarce context), P2 (non-guidance), P4 (progressive disclosure)

---

## Example 3: Design progressive documentation structure

**Input**: "Design docs/ structure for my Python FastAPI project"

**Output**:

```
docs/
├── README.md                  # Index: lists all docs with verification status
├── architecture.md            # Top-level map of domains and layers
├── principles.md              # Golden principles with rationale
├── quality.md                 # Quality grades per domain (A-F scale)
├── onboarding.md              # Agent onboarding guide (what to read first)
├── plans/
│   ├── README.md              # Active plans index
│   ├── 001-user-auth.md       # Execution plan with progress log
│   └── completed/             # Merged/closed plans
│       └── 000-project-setup.md
├── decisions/
│   ├── README.md              # Decision index
│   └── 001-db-choice.md       # Architecture decision record
└── domains/
    ├── README.md              # Domain index
    ├── auth.md                # Auth domain docs
    ├── payments.md            # Payments domain docs
    └── notifications.md       # Notifications domain docs
```

**Each document includes**:
- Verification status (last verified date, verified by agent/human)
- Owner (which team/agent maintains it)
- Cross-links to related docs

**Principles applied**: P3 (freshness), P4 (progressive disclosure), P6 (repo-local knowledge)

---

## Example 4: Create golden principles

**Input**: "Create golden principles for my Rust project"

**Output**:

```markdown
# Golden Principles

## 1. Shared Utilities Over Hand-Rolled Helpers
**Rule**: Use `crate::utils::*` instead of writing inline helpers.
**Why**: Keeps invariants centralized. Agents that copy-paste helpers
       create drift. (P8 — garbage collection)
**Enforcement**: Custom Clippy lint `disallowed_inline_helpers`

## 2. Boundary Validation, Not YOLO Access
**Rule**: All data crossing API boundaries must be validated with
       typed schemas (serde + validator).
**Why**: Agents can't distinguish "this looks right" from "this is
       guaranteed correct". (P5 — enforce invariants)
**Enforcement**: Structural test — every HTTP handler must use a
       validated input type.

## 3. Files Stay Under 400 Lines
**Rule**: No source file exceeds 400 lines. Split modules if larger.
**Why**: Long files exceed agent context windows and reduce legibility.
       (P1 — scarce context)
**Enforcement**: CI check `max_file_lines: 400`

## 4. Structured Logging Only
**Rule**: Use `tracing::info!` / `tracing::error!` with structured fields.
       Never use `println!` or `dbg!` in production code.
**Why**: Agents need parseable logs for debugging. Unstructured output
       is invisible to observability tooling. (P9 — agent legibility)
**Enforcement**: Custom Clippy lint `disallowed_println`

## 5. Prefer Boring Dependencies
**Rule**: Before adding a new crate, check if the standard library or
       an existing dependency can do the job. If adding, prefer crates
       with >1000 GitHub stars and stable APIs.
**Why**: Boring tech is well-represented in training data and easier
       for agents to model. (P7 — boring technology)
**Enforcement**: Review checklist item in PR template
```

**Principles applied**: P5 (invariants), P8 (garbage collection), P9 (agent legibility)

---

## Example 5: Set up mechanical enforcement

**Input**: "Create linters for my TypeScript project's architecture constraints"

**Output — Layer dependency linter**:

```typescript
// .linters/layer-dependencies.ts
// Enforces: shared → api → web dependency direction
// Principle: P5 — enforce invariants, not implementations

const LAYER_ORDER = ["shared", "api", "web"];
const FORBIDDEN_IMPORTS: Record<string, string[]> = {
  shared: [],           // shared imports nothing
  api: ["web"],         // api must not import web
  web: [],              // web can import anything above
};

function checkImport(sourceLayer: string, importPath: string): void {
  const targetLayer = detectLayer(importPath);
  const forbidden = FORBIDDEN_IMPORTS[sourceLayer] || [];
  if (forbidden.includes(targetLayer)) {
    throw new Error(
      `Layer violation: ${sourceLayer} imports ${targetLayer}.\n` +
      `Rule: Dependencies must flow forward: ${LAYER_ORDER.join(" → ")}\n` +
      `Fix: Move shared logic into a lower layer.`
    );
  }
}
```

**Output — CI docs-freshness job**:

```yaml
# .github/workflows/docs-freshness.yml
# Principle: P3 — documentation rots instantly
name: Docs Freshness Check
on: [pull_request]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Verify docs cross-links
        run: |
          # Check all markdown links are valid
          npx markdown-link-check docs/**/*.md
      - name: Verify verification dates
        run: |
          # Flag docs not verified in 30 days
          node .linters/check-doc-freshness.mjs
```

**Principles applied**: P5 (invariants), P3 (freshness), P9 (agent legibility)

---

## Example 6: Execution plan template

**Input**: "Create an execution plan template"

**Output**:

```markdown
# Plan: [Title]

## Status: [Draft | In Progress | Review | Complete]

## Objective
[1-2 sentence description of what this plan accomplishes]

## Context
- Related docs: [links]
- Related decisions: [links]
- Dependencies: [what must be true before starting]

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Steps
1. [Step description] — Status: [ ] Pending / [x] Done
2. [Step description] — Status: [ ] Pending / [x] Done
3. ...

## Progress Log
| Date | Agent/Human | Action | Notes |
|------|------------|--------|-------|
|      |            |        |       |

## Decisions Made
| # | Decision | Rationale | Date |
|---|----------|-----------|------|
|   |          |           |      |

## Verification
- [ ] All acceptance criteria met
- [ ] Tests pass
- [ ] Agent review approved
- [ ] Documentation updated
```

**Principles applied**: P4 (progressive disclosure), P6 (repo-local knowledge)

---

## Example 7: Garbage collection workflow

**Input**: "Set up recurring code cleanup for agent-generated code"

**Output**:

```yaml
# .github/workflows/code-gc.yml
# Runs daily to scan for deviations from golden principles
# Principle: P8 — garbage collection for code

name: Code Garbage Collection
on:
  schedule:
    - cron: "0 3 * * 1-5"  # Weekdays at 3am
  workflow_dispatch:

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Check file sizes
        run: |
          # Flag files exceeding 400 lines
          find src -name "*.ts" -exec wc -l {} \; |
            awk '$1 > 400 { print "OVERSIZE:", $2, $1, "lines" }'

      - name: Check for inline helpers
        run: |
          # Flag duplicate utility patterns
          node .linters/detect-duplicate-utils.mjs

      - name: Check boundary validation
        run: |
          # Verify all API handlers use validated input types
          node .linters/check-boundary-validation.mjs

      - name: Update quality grades
        run: |
          # Auto-update docs/quality.md with scan results
          node .linters/update-quality-grades.mjs

      - name: Open refactoring PR if needed
        run: |
          if git diff --quiet; then
            echo "No issues found"
          else
            git checkout -b gc/code-quality-$(date +%Y%m%d)
            git add .
            git commit -m "chore: automated code quality fixes"
            gh pr create \
              --title "GC: Code quality fixes $(date +%Y-%m-%d)" \
              --body "Automated garbage collection scan results." \
              --label "automated,gc"
          fi
```

**Principles applied**: P8 (garbage collection), P5 (mechanical enforcement)

---

## Example 8: Agent review loop configuration

**Input**: "Design agent-to-agent review workflow"

**Output**:

```markdown
## Agent Review Protocol

### Step 1: Self-Review
Before opening a PR, the coding agent must:
- Run `pnpm lint && pnpm test && pnpm type-check`
- Review its own diff with `git diff HEAD~1`
- Fix any issues found

### Step 2: Agent Reviewers
Open PR with label `needs-agent-review`.

Required reviewers (2):
1. **Architecture reviewer** — checks layer dependencies, naming conventions
2. **Quality reviewer** — checks test coverage, golden principles compliance

### Step 3: Feedback Loop
For each reviewer comment:
1. Agent reads the comment
2. Agent applies the fix
3. Agent responds inline with what changed
4. Reviewer re-checks

### Step 4: Merge Criteria
- [ ] All CI checks pass
- [ ] Both agent reviewers approve
- [ ] No unresolved comments
- [ ] Agent self-review complete

### Human Escalation
Only escalate to humans when:
- Ambiguous product requirements
- Security-sensitive changes
- Cross-team coordination needed
```

**Principles applied**: P10 (non-blocking), P5 (invariants)

---

## Boundaries

This skill does **not**:

1. Write application code — only scaffolding, documentation, and tooling
2. Replace project-specific domain knowledge — you must provide context
3. Guarantee agent performance — it optimizes the environment, not the model
4. Prescribe a specific tech stack — adapts to whatever you already use
5. Replace human judgment on product decisions — humans steer, agents execute
