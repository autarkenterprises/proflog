# ADR-0141: SJAS Boundary Completion

- Status: accepted
- Date: 2026-06-20
- Branch: `adr-0141-sjas-boundary-completion`
- AAR: pending

## Context

ADR-0119 requires programmatized failures at three Goedel boundaries: total
multiplication, Tab-2-or-stronger deduction, and Xtab/LEM-as-axiom. ADR-0124
through ADR-0139 created useful source, target, validation, and probe surfaces,
but ADR-0140 found that their certificates proved the positive Group-3
SelfCons axiom instead of supplying a counterexample to its proof predicate.

A source audit found a second, more fundamental gap. The current boundary
profiles do not implement the hypotheses of the cited negative results:

- `mul/2` has only the equations `mul(x,0)=0` and `mul(x,1)=x`, rather than
  total natural-number multiplication with the arithmetic basis used by the
  total-multiplication boundary argument;
- the Xtab profile has one reflected excluded-middle formula, rather than an
  apparatus that admits every instance `phi or not(phi)`;
- `dsjas-tab2-proof/3` occurs in generated SelfCons syntax but has no
  arithmeticized checker, and the target beta lacks the required arithmetic
  basis.

Consequently, none of the current Workstream B systems is entitled to the
negative theorem it names. Constructing contradictory beta axioms or relabeling
ordinary Group-3 proofs would make the tests pass without demonstrating the
boundary. This ADR prohibits both shortcuts.

The source anchors are Willard's 2002 multiplication argument (Lemmas 4.7 and
4.8), the Tab-2 boundary results summarized in the 2013 paper
<https://arxiv.org/abs/1307.0150>, and the Xtab boundary analysis in the 2020
paper <https://arxiv.org/pdf/2006.01057>.

## Decision

Complete ADR-0119 Workstream B in this branch. Do not spawn another
implementation ADR merely to record an open obligation.

### Executable apparatus

- Add an Xtab proof profile whose arithmeticized structural checker accepts a
  formula-independent LEM injection exactly when the injected formula decodes
  as `phi or not(phi)`. The profile's generated SelfCons must quantify over the
  measured Xtab proof relation actually executed by the kernel.
- Implement measured Tab-2 proof-list objects and `dsjas-tab2-proof/3` by
  generalizing the existing Tab-1 entry/reuse checker. Every non-final theorem
  must satisfy the implemented Rank-2 boundary classifier; earlier theorems
  must be usable by later tableau entries.
- Replace the multiplication seed with a genuine reflected total-multiplication
  basis. Ground multiplication facts and the arithmetic laws needed by the
  selected finite boundary witness must be kernel-checkable consequences of
  that basis, not host-calculated claims or uninterpreted labels.
- Encode and check the arithmetic assumptions used by each negative argument.
  A generated target may not be labeled theorem-eligible until those checks
  pass.

### Conclusive evidence

For each variant, provide both:

1. an explicit constructed tuple `(x,y,p,q)` accepted by the exact positive
   body of that system's generated `not(SelfCons(S))`; and
2. an independently synthesized tuple found by running the same object-level
   predicates with fresh tuple variables.

The theorem and complement proof objects must decode, bind to the exact system
and fixed-point code, and contain formula-bearing nodes that execute the
variant's reduced witness. Group-3 citation, a proof of positive SelfCons,
candidate metadata, an inconsistent beta pair, and host-side acceptance are
all ineligible.

Every synthesis run must write its command, output, elapsed time, and result to
`test-runs/`. A replayable checked-in manifest may summarize those ignored run
files, but may not replace them.

### Closure

ADR-0119 is complete only when Workstreams A, B, and C are all closed. Existing
Tab-1 and pair/list work closes A and C only if their current regression tests
remain green. This ADR closes B only when all six evidence obligations pass the
ADR-0140 verifier. An AAR must list the exact tuple codes and durable synthesis
logs.

## Test Obligations

Red before implementation:

- Xtab rejects a non-LEM injected node and accepts an arbitrary well-formed LEM
  node through its own measured proof relation;
- Tab-2 accepts a proof list with a genuine Rank-2 intermediate, rejects the
  same object under Tab-1, and rejects an intermediate above Rank 2;
- total multiplication proves representative non-seed products and rejects an
  incorrect product through the object-language basis;
- all profiles reject theorem-eligibility when a required arithmetic axiom or
  apparatus component is missing;
- all three constructed counterexample validators return true only for exact
  generated-system tuples;
- all three fresh-variable synthesis queries recover the expected tuples and
  reject supplied tuple metadata as a substitute;
- the Workstream B ledger reports six of six obligations complete only after
  full validation.

Run focused red/green selectors first, then `lein test-proflog-fast` and
`lein test-proflog-extended` in parallel, followed by focused SJAS progression
and `lein test-proflog-sjas`.

## Exit Criteria

- The three negative systems satisfy the encoded hypotheses they claim.
- Xtab and Tab-2 have executable arithmeticized proof relations.
- All three explicit counterexample tuples pass exact generated-SelfCons and
  proof-route validation.
- All three independently synthesized tuples pass the same validation and have
  durable logs.
- The evidence ledger reports all six obligations complete.
- ADR-0119 records Workstream B and the consolidated roadmap complete.
- AAR-0141 records implementation, red/green evidence, tuple artifacts,
  synthesis logs, coverage, and final gates.
