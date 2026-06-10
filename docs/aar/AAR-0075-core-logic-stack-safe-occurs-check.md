# AAR-0075: Core.logic Stack-Safe Occurs Check

- Date: 2026-06-08
- ADR: [ADR-0075](../adr/ADR-0075-core-logic-stack-safe-occurs-check.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Implemented a project-local core.logic 1.0.1 source overlay and made
`clojure.core.logic/occurs-check` stack-safe for deeply nested acyclic terms.
The patch replaces recursive first-child descent with an explicit worklist and
preserves occurs-check rejection for direct self-reference. The same small
worklist patch was mirrored into the existing 1.1.1 source-overlay profile so
that profile does not retain the known failure.

The default runtime now loads:

```text
vendor/core.logic-1.0.1/src/clojure/core/logic.clj
```

with marker:

```text
vendor/core.logic-1.0.1/src stack-safe-occurs-check
```

## Evidence

The focused regression failed red under the unpatched Maven dependency with
`StackOverflowError` at `clojure.core.logic`'s persistent-collection
`occurs-check-term` first-child recursion:

```text
lein test :only proflog.core-logic-occurs-check-test/occurs-check-is-stack-safe-for-deep-acyclic-ground-terms
Ran 1 tests containing 2 assertions.
0 failures, 1 errors.
elapsed 0:14.07 maxrss 207972KB
```

After the overlay patch, the same selector passed:

```text
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:11.33 maxrss 203500KB
```

The existing 1.1.1 source-overlay profile also passed the focused selector:

```text
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:17.18 maxrss 207256KB
```

The host audit passed and reported the default local source overlay. The final
post-ADR-0076 broad gates also passed with this core.logic overlay active:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:22.51 maxrss 444196KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 5:49.71 maxrss 570300KB
```

## Follow-up

The core.logic occurs-check stack failure is closed for the observed deep
acyclic U-Grounding proof terms. Subsequent Track 1 work exposed a separate
Proflog-side `validate-term` recursion, handled by ADR-0076.
