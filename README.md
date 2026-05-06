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

Proflog programs should first be readable as Fitting-style logic programs. The
hand-written notation uses Prolog-like clauses, with one added distinction:
`:=` marks a definitional helper that the frontend may inline, while `:-` marks
a real Fitting-style procedure-call relation. The planned ADR-0010 frontend
should accept the same source either through a parser or through a Clojure macro
using prefix clause operators. In prefix form, `(:= head body)` is a
definitional helper and `(|- head body)` is a real relation clause.
Language declarations remain reusable frontend values, so multiple programs can
compile against the same language.

Start with a minimal relation: `p(x)` succeeds exactly when `x = a`.

```prolog
p(x) :- x = a.
```

The planned frontend keeps that program visible inside a thin parser/macro
wrapper:

```clojure
(require '[proflog.frontend :as pf]
         '[proflog.query :as query])

(def p-language
  (pf/language
    (constants a b)
    (relations (p 1))))

(def p-program
  (pf/proflog p-language
    (|- (p x)
        (= x a))))

(query/query-status p-program (pf/q (p a)))
;; => :succeeds

(query/query-status p-program (pf/q (p b)))
;; => :fails
```

That frontend form descends to the source clause shape accepted by the current
language layer:

```clojure
(require '[proflog.ast :as ast]
         '[proflog.language :as language]
         '[proflog.query :as query])

(def p-lang
  (language/language
    {:constants ['a 'b]
     :relations {'p 1}}))

(def p-program
  (ast/nom x
    (language/compile-program
      p-lang
      [(ast/clause
         'p
         [x]
         (ast/eq-lit (ast/var-term x)
                     (ast/app-term 'a)))])))

(query/query-status
  p-program
  (ast/pos-lit (ast/app-term 'p (ast/app-term 'a))))
;; => :succeeds

(query/query-status
  p-program
  (ast/pos-lit (ast/app-term 'p (ast/app-term 'b))))
;; => :fails
```

Schematic tagged AST for the `p/1` clause body:

```clojure
(eq (var x) (app a))
```

A quantified example shows why ADR-0010 also needs definitional helpers and
inlining:

```prolog
only-zero(x) := forall y. (x != y or y = zero).

zero-only(x) :- only-zero(x).
```

In the prefix frontend, `:=` introduces a source-level formula abbreviation;
`|-` introduces a kernel-visible relation.

```clojure
(def peano-language
  (pf/language
    (constants zero)
    (functions (s 1))
    (relations (zero-only 1))))

(def zero-only-program
  (pf/proflog peano-language
    (:= (only-zero x)
      (forall [y]
        (or (!= x y)
            (= y zero))))

    (|- (zero-only x)
        (only-zero x))))

(query/query-status zero-only-program
                    (pf/q (zero-only zero))
                    {:timeout-ms 2000})
;; => :succeeds

(query/query-status zero-only-program
                    (pf/q (zero-only (s zero)))
                    {:timeout-ms 2000})
;; => :fails
```

After frontend inlining, the quantified program descends to the same current
source clause shape:

```clojure
(def zero-only-lang
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'zero-only 1}}))

(def zero-only-program
  (ast/nom x y
    (language/compile-program
      zero-only-lang
      [(ast/clause
         'zero-only
         [x]
         (ast/forall-form
           y
           (ast/or-form
             (ast/neq-lit (ast/var-term x) (ast/var-term y))
             (ast/eq-lit (ast/var-term y) (ast/app-term 'zero)))))])))

(query/query-status
  zero-only-program
  (ast/pos-lit (ast/app-term 'zero-only (ast/app-term 'zero)))
  {:timeout-ms 2000})
;; => :succeeds

(query/query-status
  zero-only-program
  (ast/pos-lit
    (ast/app-term 'zero-only
                  (ast/app-term 's (ast/app-term 'zero))))
  {:timeout-ms 2000})
;; => :fails
```

Schematic tagged AST for the inlined clause body:

```clojure
(forall
  (tie y
    (or
      (neq (var x) (var y))
      (eq (var y) (app zero)))))
```

The prefix DSL is not implemented yet. Until ADR-0010 lands, use the public
`proflog.ast` constructors and `proflog.language/compile-program` API shown
above and in the worked examples.

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
