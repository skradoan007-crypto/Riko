---
name: harness-engineering
version: 0.1.0
---

# Harness Engineering Claude Code Skill

Apply the Harness Engineering methodology to design agent-first development environments.

## Before you begin

This skill generates scaffolding, documentation, and tooling. It does **not** write application code or install dependencies.

## Install the Claude Code skill

```text
/plugin marketplace add lispking/harness-skills
/plugin install harness-skills@harness-skills
```

## What the skill helps with

- Audit projects for agent-readiness (scored 0-10)
- Generate `CLAUDE.md` files (~100 lines, map-style)
- Design progressive documentation structures (`docs/`)
- Create golden principles for codebases
- Set up mechanical enforcement (custom linters, structural tests)
- Build agent-to-agent review loops
- Design garbage collection workflows for continuous code quality
- Create execution plan templates

## Verified Principles

The skill is grounded in 10 verified principles (P1–P10):

| ID | Principle | Core Idea |
|----|-----------|-----------|
| P1 | Context is scarce | Keep agent instructions short, use maps |
| P2 | Non-guidance | Prioritize — mark only top 3-5 rules as critical |
| P3 | Documentation rots | Split into versioned, owned, validated docs |
| P4 | Progressive disclosure | Entry point → deeper docs → inline comments |
| P5 | Enforce invariants | Linters and tests, not style micromanagement |
| P6 | Repo-local knowledge | If it's not in the repo, it doesn't exist |
| P7 | Boring technology | Prefer stable, well-documented tools |
| P8 | Garbage collection | Continuous small cleanup over big bursts |
| P9 | Agent legibility | Optimize for agent reasoning, not just humans |
| P10 | Non-blocking workflows | Corrections are cheap, waiting is expensive |

## Documented capabilities

- Project audit with readiness scoring
- CLAUDE.md generation (under 100 lines)
- Progressive docs/ structure design
- Golden principles authoring
- Linter configuration for architecture constraints
- Execution plan templates
- CI workflow design for docs freshness and code GC
- Agent review protocol design

## Safety and operating rules

- Never overwrites existing CLAUDE.md without confirmation
- Never modifies production code
- Adapts to existing tech stack (no prescriptions)
- Respects existing conventions and patterns

## More examples

See [examples.md](./examples.md) for detailed worked examples covering all skill capabilities.
