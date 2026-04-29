# Proflog

This repository is the greenfield implementation track for Melvin Fitting's Proflog in Clojure/core.logic.

The existing `cljtap.*` namespaces and tests are reference material and experimental prior art. They are useful for pressure-testing ideas, but they are not the new implementation authority. The greenfield implementation must justify each convergent design independently against Fitting, αleanTAP, miniKanren/core.logic, and the local research reports.

## Mission

See [MISSION.md](MISSION.md).

## Execution Docs

- [docs/EXECUTION_PLAN.md](docs/EXECUTION_PLAN.md)
- [LOG.md](LOG.md)
- [docs/TEST_MATRIX.md](docs/TEST_MATRIX.md)
- [docs/SEMANTIC_VARIANTS.md](docs/SEMANTIC_VARIANTS.md)
- [docs/adr/README.md](docs/adr/README.md)
- [docs/aar/README.md](docs/aar/README.md)

## Local Source Stack

- [development-practices.md](development-practices.md)
- [deep-research-report-summary.md](deep-research-report-summary.md)
- [deep-research-report.md](deep-research-report.md)
- [deep-research-report2.md](deep-research-report2.md)
- [DESIGN.md](DESIGN.md)
- [LPTableaux.pdf](LPTableaux.pdf)

## External Primary Sources Reviewed

- αleanTAP paper: <https://people.csail.mit.edu/jnear/papers/alphatap.pdf>
- core.logic repository: <https://github.com/clojure/core.logic>
- core.logic API reference: <https://clojure.github.io/core.logic/>
- Byrd dissertation: <https://hdl.handle.net/2022/8777>
- Fitting 1994 bibliographic record: <https://dblp.org/rec/journals/jar/Fitting94>

## Working Agreement

- `greenfield` is the integration branch for the new implementation.
- New feature work should follow ADR-specific branches and merge into `greenfield` before any promotion to `master`.
- `greenfield` is also a fresh sandbox: existing code in `src/`, `test/`, or elsewhere may be removed, rewritten, or refactored when that better serves the active ADR and test plan.
- New implementation code is planned under `src/proflog/` and `test/proflog/`.
- `src/cljtap/` and `test/cljtap/` remain reference and regression material unless a later ADR explicitly retires them.
