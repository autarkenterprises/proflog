# ADR-0075: Core.logic Stack-Safe Occurs Check

- Status: completed
- Date: 2026-06-08
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0075](../aar/AAR-0075-core-logic-stack-safe-occurs-check.md)

## Context

ADR-0073 Track 1 arithmeticizes SJAS proof certificates as U-Grounding numerals.
The resulting proof and formula codes are deeply nested but acyclic ground terms.
Core.logic 1.0.1 enables the occurs check in ordinary `run` queries, so binding a
fresh logic variable to one of these numerals first traverses the whole term to
verify that the variable does not occur inside it.

The current core.logic occurs check is partially iterative. It loops across
siblings in persistent collections and logic lists, but it checks the first child
with a nested call to `occurs-check`. A sufficiently deep unary or near-unary
term therefore consumes one JVM stack frame per level even though the check is
logically a simple reachability scan. This can raise `StackOverflowError` before
Proflog's own proof machinery has a chance to accept or reject the certificate.

Stack overflow is not a heap exhaustion condition. Raising the JVM heap limit
does not make recursive Clojure calls stack-safe, and `core.logic` cannot use
`recur` across the current mutually recursive `occurs-check` /
`occurs-check-term` protocol boundary.

## Decision

Vendor the current `org.clojure/core.logic` 1.0.1 source under
`vendor/core.logic-1.0.1/src` and place that source path before `src` in the
project classpath. Patch only the occurs-check traversal needed for this ADR.

Replace the nested recursive descent with an explicit worklist that preserves
the existing occurs-check semantics:

- unbound logic variables are compared with the variable being extended;
- logic lists scan `lfirst` and `lnext` without consuming the host stack;
- persistent collections scan their children without recursive first-child
  calls;
- non-collection ground terms continue to be accepted immediately.

This is a correctness patch, not a performance optimization. Any performance
effect is secondary to making large acyclic SJAS numerals admissible under the
standard occurs check.

## Consequences

- Proflog will load a project-local core.logic source overlay by default, so the
  host audit must continue to report the active implementation.
- Deep acyclic proof numerals should no longer fail because of JVM stack depth.
- Cyclic bindings must remain rejected by the occurs check.
- The patch affects all core.logic use in this repository, not only the SJAS
  profile. The regression suite must therefore cover ordinary fast and extended
  Proflog behavior, not just the focused SJAS selector that exposed the problem.

## Test Obligations

- Add a focused red regression showing that binding a fresh logic variable to a
  deeply nested acyclic ground term fails under the unpatched dependency.
- Add a companion assertion that the occurs check still rejects direct
  self-reference.
- Rerun the focused regression after the overlay patch.
- Rerun the core.logic host audit.
- Rerun the normal `lein test-proflog-fast` and `lein test-proflog-extended`
  gates before considering the core.logic patch complete.
