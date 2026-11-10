# RIKO Performance — Before / After

> **Plan:** `docs/superpowers/plans/2026-10-05-riko-professional-campaign.md` §20 (Wave 10)
> **Status:** structure created `[M]` · device numbers **PENDING** `[N]`
> **Rule:** Never replace a poor baseline with a new test that is easier to pass.

---

## Comparison Matrix

| Metric | Before (Wave 0) | After Wave 1 | After Wave 4 | Final | Unit |
|---|---|---|---|---|---|
| TTID (cold) | — | — | — | — | ms |
| TTID (warm) | — | — | — | — | ms |
| TTFD | — | — | — | — | ms |
| MyMusic fling (p50) | — | — | — | — | ms |
| MyMusic fling (p95) | — | — | — | — | ms |
| Favorites fling (p50) | — | — | — | — | ms |
| Home fling (p50) | — | — | — | — | ms |
| Queue scroll (p50) | — | — | — | — | ms |
| Queue reorder | — | — | — | — | frame feel |
| NP expand | — | — | — | — | ms |
| EQ band drag | — | — | — | — | ms |
| Memory (30min soak) | — | — | — | — | MB |
| Battery drain (30min) | — | — | — | — | % |
| Thermal throttle | — | — | — | — | y/n |

---

## Device / API Matrix

| Device | API | Display | Status |
|---|---|---|---|
| Device A | 35+ | high-refresh | `[N]` |
| Device B | 33+ | standard | `[N]` |
| Device C | 26–29 | fallback | `[N]` |

---

## Scenario Checklist

- [ ] My Music list fling
- [ ] Favorites fling
- [ ] Home fling
- [ ] Queue scroll
- [ ] Queue reorder
- [ ] Now Playing expand/collapse
- [ ] EQ interaction (band drag)
- [ ] Visualizer Studio open
- [ ] Home startup (cold/warm/hot)
- [ ] Search result fling
- [ ] 30-minute playback soak

---

## Notes

* Baseline: `docs/performance/PERF_BASELINE.md` (Wave 0)
* All numbers must be measured on real devices — `[M]` measured, `[R]` researched, `[V]` device-verified, `[N]` not yet verified
* Compare: `Before → Wave 1 → Wave 4 → Final`
