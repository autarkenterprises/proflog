# ADR-0119: SJAS Next Research Roadmap

- Status: accepted
- Date: 2026-06-18
- Branch: `adr-0119-sjas-next-research-roadmap`
- AAR: n/a (planning/control ADR; implementation ADRs will receive AARs)

## Context

The SJAS line now has a working ordinary-tableau substrate, a Level-1
`IS#_{D_SJAS}(beta)` implementation path, and the `D_SJAS` accounting repair
needed by ADR-0108 and ADR-0109. The next research phase should not be driven by
one-off `/goal` loops that forget the boundary between three different questions:

- adding Willard-style proof-list theorem reuse, especially Tab-1;
- deliberately constructing variants that should fail at the Goedel boundary;
- demonstrating consistency-preserving self-extension with reflected data
  encodings.

This ADR is the consolidated planning record for that phase. It does not
authorize SJAS implementation changes. Later implementation ADRs should spawn
from this roadmap with their own red tests, branches, AARs, and gate evidence.

Primary local and public anchors:

- `sjas/nachlass/papers/willard2013_significance_self_justifying_axiom_systems_arxiv_1307.0150.pdf`
  and the public arXiv record
  <https://arxiv.org/abs/1307.0150>.
- `sjas/nachlass/papers/willard2020.pdf`,
  `sjas/lit/willard_2020_how_lem_pertains_2nd_inc_thm_boundary_case_exceptions.pdf`,
  and the public arXiv PDF <https://arxiv.org/pdf/2006.01057>.
- `sjas/nachlass/papers/willard2001_self_verifying_axiom_systems_author_jsl1.pdf`
  and the public PhilPapers record <https://philpapers.org/rec/WILSAS-2>.
- [ADR-0058](ADR-0058-willard-sjas-language-profile.md),
  [ADR-0062](ADR-0062-sjas-self-justification-demonstration.md),
  [ADR-0070](ADR-0070-sjas-byte-sequence-coding-audit.md),
  [ADR-0087](ADR-0087-sjas-level1-pi-star-1-pair-restriction.md),
  [ADR-0092](ADR-0092-sjas-nnf-pi-star-1-encodability.md),
  [ADR-0104](ADR-0104-dsjas-track2c.md),
  [ADR-0108](ADR-0108-dsjas-quantitative-ea-stability.md), and
  [ADR-0109](ADR-0109-dsjas-composite-proof-object-internalization.md).
- [Willard SJAS independent review synthesis](../log/2026-05-10-willard-sjas-agent-review-synthesis.md).

## Decision

Preserve three parallel SJAS workstreams and require future work to identify
which stream it advances.

### Workstream A: Tab-k And Tab-1

Define generic `Tab-k` as a proof-list apparatus. A candidate proof list is:

```text
H = [(t1,p1), ..., (tn,pn)]
```

For each `i`, `pi` is a semantic-tableau proof of `ti` from the finite basis
`beta` plus earlier list entries `t1 ... t(i-1)`. Every intermediate theorem
`t1 ... t(n-1)` must be classified as no stronger than `Pi*_k` or `Sigma*_k`.
The last entry `tn` is the target theorem certified by the list.

`Tab-1` is the `k = 1` instance. The implementation path should reuse the
existing `Pi*_1` / `Sigma*_1` classifier machinery, but a first Tab-1 ADR must
reconcile project terminology against Willard's Rank-1* and `U1*`
presentations before changing code. The reconciliation must say which local
classifier is used at each object-language boundary and which forms are merely
host-side acceptance conveniences, such as ADR-0092's NNF encodability check.

Future Tab-1 implementation ADRs must add, in this order unless a later ADR
justifies reordering:

- proof-list syntax and public proof-object coding;
- measured-object accounting compatible with `Log_D_SJAS`;
- arithmeticized validation of list entries and earlier-theorem reuse;
- public compatibility predicates that do not break existing
  `tableau-proof/3`, `subst-prf/4`, `dsjas-tableau-proof/3`, or
  `dsjas-subst-prf/4` callers;
- generated SelfCons forms that quantify over the measured Tab-1 object when
  the selected apparatus is Tab-1 rather than the current ordinary-tableau
  `D_SJAS`.

Tab-2 and stronger proof-list variants are not positive implementation goals in
this workstream. They belong to Workstream B as boundary-failure probes unless a
new source audit and ADR explicitly promote one of them.

### Workstream B: Programmatized Goedel-Boundary Failures

Implement future negative SJAS variants that are expected to cross the boundary
where self-justification fails. The first planned variants are:

- total multiplication as an official object-language function rather than the
  current relational `mult/3` graph discipline;
- `Tab-2` or stronger theorem-reuse apparatus;
- `Xtab` or Law of Excluded Middle packaged as an axiom schema rather than
  derived inside the tableau setting.

Each negative variant must proceed in two stages:

- a reduced reflected-beta witness, small enough to inspect and debug;
- the full generated SelfCons contradiction target for that variant.

Final success for a boundary ADR requires both explicit constructed certificates
and proof-search synthesis evidence. A hand-constructed certificate is useful
intermediate evidence, but it is not completion by itself. The expected witness
is closure of the appropriate contradiction proof against the variant's own
generated SelfCons statement, not merely a contradiction in some neighboring or
externally strengthened theory.

Long-running synthesis probes in this stream must follow the durable
`test-runs/` logging discipline from the project instructions, including PID
capture and `/usr/bin/time` output.

ADR-0140 tightens the meaning of this evidence after the first implementation
audit. A proof of the positive Group-3 SelfCons axiom is ordinary
self-justification, even when represented by a structural tableau. Final
boundary evidence must instead supply the concrete tuple quantified by the
positive body of `not(SelfCons(S))`, validate every class/complement/proof
predicate against the exact generated system, and derive reduced-witness use
from formula-bearing nodes in the kernel-accepted proof objects. Candidate
metadata is not proof-route evidence. The Tab-2 variant additionally requires
an implemented arithmeticized `dsjas-tab2-proof/3` relation before either final
evidence obligation can close.

### Workstream C: Self-Interpretation / Self-Extension

The default positive implementation path is reflected pair axioms first and
list axioms second, with pairs treated as the intermediate representation layer
for lists. This gives the system a small, inspectable data-structure extension
before it attempts list recursion or richer encoded syntax manipulation.

Before implementation, the first self-extension ADR must survey
beta-axiomatizable data encodings and operations under SJAS constraints. The
survey must compare pairs, lists, and any simpler candidate representation
against:

- ability to state the encoding through finite reflected beta axioms;
- ability to keep the added axioms within the Level-1 classifier discipline;
- impact on generated system identity, Group-3 reconstruction, and SelfCons
  content;
- proof-search tractability in the focused SJAS suite.

If the survey finds another data-structure demonstration that is easier or more
compelling than pairs-first, this ADR permits replacing the default first demo.
The replacement must still be a reflected extension of the baseline system, not
an external helper library.

The minimum final direction is to show a consistency-preserving reflected
extension of the baseline system with data-structure beta axioms incorporated
into the generated self-consistency statement.

## Consequences

- ADR-0119 is a control ADR. It should remain stable while smaller
  implementation ADRs are spawned beneath it.
- Claims about the current implementation remain labeled as ordinary-tableau
  and Level-1 `D_SJAS` unless a later ADR implements and tests Tab-1.
- `D_SJAS` composite proof-object accounting from ADR-0108 and ADR-0109 remains
  the baseline measure for any new proof predicate unless a later ADR explicitly
  replaces it.
- Boundary-failure work must demonstrate failure against the variant's own
  generated SelfCons target; generic inconsistency or constructed-only evidence
  is insufficient.
- Self-extension work must prove that reflected beta changes alter system
  identity and regenerated Group-3/SelfCons content.

## Test Obligations

ADR-0119 itself has only documentation checks:

- the public links in this ADR resolve or have an explicit local witness;
- [ADR README](README.md) indexes ADR-0119;
- [LOG.md](../../LOG.md) records ADR-0119 as a planning/control ADR for future
  `/goal` work;
- no implementation files are changed in this branch.

Future Tab-k ADRs must start red with missing proof-list predicates/builders,
classifier restrictions, and SelfCons relation-symbol changes.

Future boundary ADRs must include reduced and full SelfCons negative witnesses,
with durable logs for any synthesis probe expected to run for hours.

Future self-extension ADRs must include tests proving that reflected beta changes
alter system identity and regenerated Group-3/SelfCons content.

## Exit Criteria

- ADR-0119 exists with the three workstreams and spawn points above.
- The ADR index and development log link to it.
- Documentation checks confirm that only ADR-0119, the ADR index, and `LOG.md`
  changed for this branch.
- No AAR is added unless implementation begins on this branch.

## Roadmap Completion (2026-06-22)

All three workstreams spawned from this control ADR are now closed, so the
consolidated SJAS research roadmap is complete:

- **Workstream A (Tab-k / Tab-1):** implemented by ADR-0120 (proof-list
  surface), ADR-0121 (entry validation), and ADR-0122 (theorem reuse). Its
  regression tests are green on the current branch.
- **Workstream B (programmatized Goedel-boundary failures):** closed by
  [ADR-0141](ADR-0141-sjas-boundary-completion.md). The three negative variants
  (total multiplication, Xtab/LEM-as-axiom, Tab-2-or-stronger) implement the
  hypotheses they name and pass all six final-evidence obligations — a
  constructed and an independently synthesized `not(SelfCons)` counterexample
  tuple per variant — through the ADR-0140 verifier, tallied by the new
  Workstream B evidence ledger. See
  [AAR-0141](../aar/AAR-0141-sjas-boundary-completion.md).
- **Workstream C (self-interpretation / self-extension):** the pair-first survey
  (ADR-0123) and reflected list self-extension (ADR-0128) provide the
  consistency-preserving reflected data-structure extension; their regression
  tests are green.

This section records completion only; the three-workstream contract above
remains the stable planning reference.
