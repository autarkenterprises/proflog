# Proflog

Experiments into the implementation of Melvin Fitting's Proflog, a
tableau-based logic programming language.

This repository is the greenfield implementation track for Proflog in
Clojure/core.logic.

The existing `cljtap.*` namespaces and tests are reference material and
experimental prior art. They are useful for pressure-testing ideas, but they are
not the new implementation authority. The greenfield implementation must
justify each convergent design independently against Fitting, alphaleanTAP,
miniKanren/core.logic, and the local research reports.

## Mission

See [MISSION.md](MISSION.md).

## Quickstart

Prerequisite: install Leiningen with a working JDK.

Run the fast greenfield regression suite:

```text
lein test-proflog-fast
```

Start a REPL:

```text
lein repl
```

Define and query a first Proflog program:

```clojure
(require '[proflog.ast :as ast]
         '[proflog.language :as language]
         '[proflog.query :as query])

(def lang
  (language/language
    {:constants ['a 'b]
     :relations {'p 1}}))

(def program
  (ast/nom x
    (language/compile-program
      lang
      [(ast/clause 'p [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'a)))])))

(query/query-status program
                    (ast/pos-lit (ast/app-term 'p (ast/app-term 'a))))
;; => :succeeds

(query/query-status program
                    (ast/pos-lit (ast/app-term 'p (ast/app-term 'b))))
;; => :fails
```

For non-trivial examples, run the focused Fitting and legacy-subsumption gates:

```text
lein test-proflog-fitting-programs
lein test-proflog-legacy-subsumption
```

The worked examples in [worked-examples/](worked-examples/README.md) show how
programs are defined, translated, queried, and evaluated.

## Execution Docs

- [docs/EXECUTION_PLAN.md](docs/EXECUTION_PLAN.md)
- [docs/GREENFIELD_IMPLEMENTATION_TUTORIAL.md](docs/GREENFIELD_IMPLEMENTATION_TUTORIAL.md)
- [docs/GREENFIELD_SOURCE_MAP.md](docs/GREENFIELD_SOURCE_MAP.md)
- [worked-examples/README.md](worked-examples/README.md)
- [LOG.md](LOG.md)
- [docs/TEST_MATRIX.md](docs/TEST_MATRIX.md)
- [docs/TEST_RUNTIME_BASELINE.md](docs/TEST_RUNTIME_BASELINE.md)
- [docs/SEMANTIC_VARIANTS.md](docs/SEMANTIC_VARIANTS.md)
- [docs/adr/README.md](docs/adr/README.md)
- [docs/aar/README.md](docs/aar/README.md)

## Local Source Stack

- [development-practices.md](development-practices.md)
- [DESIGN.md](DESIGN.md)
- [LESSONS.md](LESSONS.md)
- [MEMORY.md](MEMORY.md)
- [LPTableaus.pdf](LPTableaus.pdf)

## External Primary Sources Reviewed

- alphaleanTAP paper: <https://people.csail.mit.edu/jnear/papers/alphatap.pdf>
- core.logic repository: <https://github.com/clojure/core.logic>
- core.logic API reference: <https://clojure.github.io/core.logic/>
- Byrd dissertation: <https://hdl.handle.net/2022/8777>
- Fitting 1994 bibliographic record: <https://dblp.org/rec/journals/jar/Fitting94>

## Working Agreement

- `main` is the integration branch for the implementation.
- New feature work should follow ADR-specific branches and merge into `main`
  once closed or deliberately carried forward.
- New implementation code lives under `src/proflog/` and `test/proflog/`.
- `src/cljtap/` and `test/cljtap/` remain reference and regression material
  unless a later ADR explicitly retires them.
