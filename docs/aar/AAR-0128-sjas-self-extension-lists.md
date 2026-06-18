# AAR-0128: SJAS Self-Extension Lists From Pairs

- Date: 2026-06-18
- ADR: [ADR-0128](../adr/ADR-0128-sjas-self-extension-lists.md)
- Branch: `adr-0128-sjas-self-extension-lists`

## Outcome

ADR-0128 is complete as the Workstream C pair-backed list representation slice.

The implementation adds:

- `list-constants`, with the reflected `list-nil` constant;
- `list-functions`, declaring `list-cons/2`, `list-head/1`, and
  `list-tail/1`;
- source term helpers for list nil, cons, head, and tail;
- `list-constructor-axioms`, the finite beta laws tying `list-cons` to the
  ADR-0123 pair representation and exposing head/tail projections;
- `list-extension-options`, combining pair and list reflected beta records;
- `list-extended-system`, building a Level-1 system whose generated source and
  SelfCons statement include the pair-backed list layer;
- a Workstream C audit update marking `:lists-from-pairs` implemented while
  keeping list recursion and encoded syntax manipulation deferred.

This does not implement recursive list processing or full self-interpretation.
It gives later ADRs a reflected list representation to build on.

## Evidence

Initial red selectors failed as intended:

```text
self-extension-survey-selects-reflected-pair-axioms
expected :implemented, actual :second-stage
expected implemented demos, actual nil

sjas-list-extension-is-reflected-through-pair-layer
No such var: sjas/list-constants
```

Focused green selectors:

```text
self-extension-survey-selects-reflected-pair-axioms
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

sjas-list-extension-is-reflected-through-pair-layer
Ran 1 tests containing 19 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 223 tests containing 1394 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1173 fail=0 error=0
```

## Follow-up

- Add recursive list predicates once their reflected beta/profile boundary is
  specified.
- Add encoded syntax manipulation over the pair-backed list representation.
- Workstream B still needs constructed certificates and proof-search synthesis
  evidence for negative variants.
