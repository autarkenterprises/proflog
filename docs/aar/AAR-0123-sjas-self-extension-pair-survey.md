# AAR-0123: SJAS Self-Extension Pair Survey

- Date: 2026-06-18
- ADR: [ADR-0123](../adr/ADR-0123-sjas-self-extension-pair-survey.md)
- Branch: `adr-0123-sjas-self-extension-pair-survey`

## Outcome

ADR-0123 is complete.

The self-extension workstream now has an executable first demonstration:

- the ADR-0123 survey audit selects reflected pair axioms as the first data
  encoding demo and defers recursive lists;
- public `pair-term`, `fst-term`, and `snd-term` constructors build the fresh
  pair vocabulary;
- `pair-functions` declares `pair/2`, `fst/1`, and `snd/1`;
- `pair-projection-axioms` returns the two universal projection laws as
  Pi*1-admissible reflected beta formulas;
- `pair-extension-options` and `pair-extended-system` expose the mergeable
  reflected beta fragment;
- the pair-extended Level-1 system gets two additional Group-2 beta records;
- those pair beta records are visible through decoded `axiom-member/2` and
  citeable by `tableau-proof/3` with `sjas-axiom`;
- reflected pair beta changes the encoded system source and regenerated
  Group-3/SelfCons code, while an external-only pair-like runtime clause with
  the same function signature leaves both unchanged.

This does not claim list recursion or encoded syntax manipulation. Those remain
future Workstream C obligations.

## Evidence

Initial red selectors failed as intended:

```text
self-extension-survey-selects-reflected-pair-axioms
No such var: correspondence/audit-self-extension-data-encoding-survey

sjas-pair-extension-axioms-are-level1-reflected-beta
No such var: sjas/pair-projection-axioms
```

Focused green selectors:

```text
self-extension-survey-selects-reflected-pair-axioms
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.

sjas-pair-extension-axioms-are-level1-reflected-beta
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.

sjas-pair-extended-system-changes-identity-and-selfcons
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 220 tests containing 1368 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1119 fail=0 error=0
```

## Follow-up

- Workstream C can next add list axioms on top of the reflected pair layer or
  choose another data-structure demonstration if a later survey finds a better
  target.
- Workstream B negative boundary variants remain open from ADR-0119.
