# GSD Operating Mode

GSD Core is the default development methodology for this project.

For every development request:

1. First inspect the current project state and `.planning/`.
2. Follow the GSD workflow whenever the task is more than a trivial change:
   Discuss → Plan → Execute → Verify → Ship.
3. Do not jump directly into implementation when planning or architectural decisions are required.
4. Reuse existing GSD planning artifacts instead of creating parallel plans.
5. Keep `.planning/STATE.md` and other GSD artifacts current.
6. Break large work into small, verifiable phases.
7. Use GSD agents/subagents when appropriate.
8. Run relevant tests, linting, builds, and verification before declaring work complete.
9. If something fails, diagnose and fix it rather than merely reporting the failure.
10. For trivial requests such as a typo, one-line change, or simple explanation, do not unnecessarily invoke the full GSD cycle.
11. When unsure about the next GSD action, use `/gsd-progress`.

The goal is not to blindly use GSD commands for every message. The goal is to apply GSD's methodology automatically and proportionally.
