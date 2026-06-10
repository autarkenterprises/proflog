# SJAS Compact Code Projector Removal

## Context

ADR-0073 Track 1 requires replacing host-side staged readers and predicate
shortcuts with object-language relations over encoded formulas, systems, proof
trees, and procedure-call evidence. One remaining compact-code boundary was the
public `code-N` byte reader in `proflog.kernel.willard-sjas-profile`.

The old reader used two host projectors:

- `ground-code-byte-termo`, which used `project` and `lvar?` to decide whether
  a compact byte argument was already ground before interpreting it.
- `ground-code-constructoro`, which used `project` to inspect a ground
  `code-N` constructor before falling back to the finite constructor relation.

This was not a generated formula registry, but it was still a host-side
scheduling bridge in the proof-facing code reader. For Track 1, the public code
bytes should be consumed through the object-level numeral relation and finite
byte relation rather than through host projection.

## Change

The compact-code byte reader no longer imports or uses `project` or `lvar?`.
The old `ground-code-byte-termo`, `generated-code-byte-termo`, and
`ground-code-constructoro` helpers were removed.

`compact-code-byte-bits-termo` is now a relation in both relevant modes:

- present public byte terms decode through the object-level U-grounding numeral
  reader and then through `byte-bitso`;
- generated embedded code bytes constrain the finite byte value first, generate
  the canonical numeral term, and fall back to the object-level numeral reader
  only when a byte term is already bound to a noncanonical numeral.

`code-constructoro` still uses ground-mode host branching for first-order
signature arity scheduling when the constructor symbol or byte count is already
ordinary Clojure data, but the unbound constructor/arity relation remains the
finite `code-constructor-entries` relation. The removed boundary in this slice
is the proof-reader `project` bridge, not the broader question of whether the
host language signature itself is an irrelevant implementation representation.

## Regression

The source audit now rejects:

- `defn- ground-code-byte-termo`
- `defn- ground-code-constructoro`
- `project [` in `willard_sjas_profile.clj`

The red audit failed first on all three conditions, then passed after the
reader change.

## Verification

Focused selectors passed:

- `sjas-profile-source-audit-rejects-host-proof-checker-route`: 45 assertions
- `sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically`
- `sjas-formula-codes-preserve-trailing-zero-embedded-code-payloads`
- `sjas-code-format-dispatch-does-not-read-source-registry`
- `sjas-formal-codes-are-godel-byte-terms`
- `sjas-syntax-predicates-decode-application-codes-without-symbol-registry`
- `sjas-syntax-predicates-decode-formula-godel-codes`
- `sjas-tableau-proof-checks-kernel-certificates`
- `sjas-subst-prf-checks-identity-substitution-certificates`
- `large-tableau-proof-raw-direct-evidence-materializes`
- `sjas-selfcons-demonstration-uses-substantive-proof-targets`
- `sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry`
- `sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code`
- `sjas-subst-prf-reconstructs-axiom-basis-without-system-registry`

Broad gates passed:

- `lein test-proflog-fast`: 164 tests, 653 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions

`lein test-proflog-sjas-focused` was also started. It progressed one test var
at a time with no failures through
`sjas-proof-predicates-ignore-external-runtime-clauses`, which completed in
561595.584 ms, then was stopped as
`sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry`
began. The run did not produce a final focused-suite summary. Notable timings
from that partial run include:

- `large-tableau-proof-raw-direct-evidence-materializes`: 219107.544 ms
- `sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures`:
  162490.162 ms
- `sjas-proof-check-accepts-recursive-guarded-call-sequence-from-system-code`:
  288089.533 ms
- `sjas-proof-predicates-do-not-require-source-preprocessing-registry`:
  311549.325 ms
- `sjas-proof-predicates-ignore-external-runtime-clauses`: 561595.584 ms

## Assessment

The slice advances Track 1 by removing host projection from compact public code
byte decoding in the proof-facing SJAS profile. It also exposes a significant
runtime cost: once the projector is gone, deep proof-predicate checks pay for
object-level code reading in several system-code reconstruction paths.

That cost is acceptable for the current ADR priority because the implementation
is moving toward a concrete arithmeticized proof predicate rather than toward a
host shortcut. The remaining performance issue should be treated as a
tractability boundary for later relation scheduling work, not as justification
for restoring the projector.
