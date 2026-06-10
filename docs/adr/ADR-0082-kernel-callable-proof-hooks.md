# ADR-0082: Kernel Callable Proof Hooks

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0082](../aar/AAR-0082-kernel-callable-proof-hooks.md)

## Context

ADR-0081 removed committed-choice dispatch from proof-facing kernel and SJAS
profile paths. The follow-up audit still found optional hook dispatch encoded
as host-side nil selection:

- `recursive-prove-stateo` selected between `*recursive-prove-stateo*` and
  `prove-stateo` with Clojure `or`;
- `theory-profile-closeo` selected between `*theory-profile-closeo*` and
  `fail` with Clojure `if-let`;
- `close-agendao` still guarded on `*theory-profile-closeo*` being nil or
  non-nil.

These hooks are scheduling infrastructure, not SJAS proof rules. Track 1 is
better served if proof-facing hooks are always callable relations whose default
behavior is itself relational: ordinary recursion delegates to `prove-stateo`,
and the absent theory profile is represented by a failing relation.

## Decision

Replace optional nil-dispatch hooks with callable default relations:

- default recursive proof dispatch calls `prove-stateo`;
- default theory-profile dispatch is `fail`;
- `recursive-prove-stateo` and `theory-profile-closeo` simply apply the current
  relation value;
- `close-agendao` tries the theory-profile relation and ordinary kernel closure
  as ordinary `conde` alternatives, without dynamic-var nil tests.

## Consequences

- `*recursive-prove-stateo*` and `*theory-profile-closeo*` are expected to be
  callable relations when rebound.
- Existing tabling and SJAS/Robinson-Q bindings continue to work because they
  already bind relation functions.
- The ordinary kernel path remains available through default relations rather
  than host nil selection.

## Test Obligations

- Add a red source audit rejecting host optional dispatch for the proof hooks.
- Keep the existing no-committed-choice audit green.
- Run focused kernel/tabled/profile selectors before broad gates.
