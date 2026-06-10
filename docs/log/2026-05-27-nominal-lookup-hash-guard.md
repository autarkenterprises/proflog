# Nominal Lookup Hash Guard

Date: 2026-05-27

## Research

The core.logic changelog records `LOGIC-101`, "fix surprising behavior with
vars in nom/hash", between `0.8.0-beta5` and `0.8.0-rc1`. The project default
dependency, core.logic `1.0.1`, and the optional `1.1.1` source overlay both
carry the post-LOGIC-101 nominal implementation.

The current upstream nominal implementation still has the same `hash`, `suspc`,
and `tie` shape as the local 1.0.1/1.1.1 files inspected for this fix. Direct
regressions confirmed that `nom/hash` rejects delayed self-aliasing:

```clojure
(nominal/hash key skipped)
(== key skipped)
```

fails once `key` and `skipped` become the same nom.

The bug was therefore not a failure in core.logic's `hash` relation. It was a
failure to use `hash` in local nominal-key environment lookup recursion.

## Bug

αleanTAP-style substitution environments are finite maps from noms to tableau
instantiations. A relational lookup has two branches:

1. the head key is the searched key;
2. the head key is skipped and lookup continues in the tail.

The second branch must assert that the searched nom is fresh for the skipped
key. Without that guard, this relational mode leaks an invalid answer:

```clojure
(lookupo key
         (lcons [skipped :first]
                (lcons [wanted :second] '()))
         out)
(== key skipped)
(== skipped wanted)
```

An unguarded implementation returns both `:first` and `:second`. The second
answer comes from recursing past a binding that later becomes the same key,
creating a duplicate-key environment. A nominal finite-map lookup must return
only `:first`.

Core.logic's own nominal test-suite examples use the required shape in their
typed lambda-calculus `lookupo`:

```clojure
(nom/hash x xc)
```

before recursing past `xc`.

## Fix

Added the freshness guard to local nominal lookup recursion:

```clojure
(fresh [skipped-key skipped-value]
  (== (lcons [skipped-key skipped-value] rest) env)
  (nominal/hash binding-nom skipped-key)
  (lookupo binding-nom rest value))
```

The legacy αleanTAP namespaces use their referred `hash` symbol; greenfield
substitution, equality, and first-order code use `nominal/hash`.

## Verification

Red before implementation:

- `lein test :only cljtap.alphaleantap-e-test/test-LK01-lookupo-guards-skipped-nominal-key`
- `lein test :only cljtap.alphaleantap-ep-test/test-LK01-lookupo-guards-skipped-nominal-key`
- `lein test :only proflog.subst-test/lookupo-guards-skipped-nominal-key`
- `lein test :only proflog.equality-test/lookupo-guards-skipped-nominal-key`
- `lein test :only proflog.kernel.first-order-test/lookupo-guards-skipped-nominal-key`

Green after implementation:

- all red selectors above
- `lein test proflog.core-logic-nominal-hash-test`
- `lein with-profile +core-logic-1.1.1 test proflog.core-logic-nominal-hash-test`
- `lein with-profile +core-logic-source-overlay test proflog.core-logic-nominal-hash-test`
- `lein test :only cljtap.alphaleantap-e-test/test-I07-universal-existential-interaction`
- `lein test :only cljtap.alphaleantap-ep-test/test-FD07-warm-unique-true`
- `lein test-proflog-fast`
- `git diff --check`
