# SJAS Track 2a Relevance Matrix — Completion

Date: 2026-06-13
ADRs: [ADR-0098](../adr/ADR-0098-sjas-equality-fragment-reachability.md),
[ADR-0099](../adr/ADR-0099-sjas-track2a-completion.md)
(building on Track 2b ADR-0096/0097)

This note closes Track 2a of ADR-0073. The
[relevance matrix](2026-05-25-sjas-tableau-relevance-matrix.md) opened with six
relevant rows, three "probably irrelevant" rows, and several "unresolved"
rows — three of them *high risk*. Every row now has a recorded disposition and
no proof symbol remains `:unresolved` in the executable Track 2a classifier
(`track-2a-relevance-matrix-has-no-unresolved-symbols`).

## Final disposition of every matrix row

### Relevant — preserved, with executable mechanisms in place

| Row | Mechanism |
|---|---|
| Proof object is a finite tree, not a Hilbert list | Formula-bearing structural certificate; ADR-0097 tree/shape audit. |
| Branching by tableau decomposition | Structural checker expands each connective into formula-bearing children. |
| Root / axiom / deduction ancestry | Structural checker threads the branch; `sjas-axiom`/axiom-member closure. |
| Branch closure by formula and negation | Formula-bearing complementary-literal and `false`/arithmetic closure. |
| Inspectable formula/proof/system byte encoding | ADR-0063–0071 base-64 / U-Grounding codes; ADR-0096 fragment boundary. |
| Lower-bound proof-size discipline | ADR-0097 node/leaf/depth/byte metrics over the explicit formula-bearing tree. |

### Probably irrelevant — classified, residual obligation handed to Track 2b

| Row | Disposition |
|---|---|
| Exact historical byte layout | Probably irrelevant; a bounded-translation argument to Willard's notation is a Track 2b step, not a Track 2a blocker. |
| Rule-selection order and search scheduling | Probably irrelevant; the accepted proof-tree codes are scheduler-independent (the checker validates a supplied tree). |
| Runtime caching / tabling / agenda / host data structures | Probably irrelevant; below the proof-object level, not present in decoded certificates. |

### Resolved high-risk rows (this completion)

| Row | Resolution |
|---|---|
| Proof term layout inside Proflog certificates | **Resolved**: ADR-0096 admits formula-bearing structural nodes + bare `sjas-axiom`; ADR-0097 audits their tree/size. |
| Equality and disequality profile rules | **Resolved via unreachability** (ADR-0098): the checker closes all eq/neq cases formula-bearing; the equality/disequality tags are unreachable in first-fragment certificates. |
| Procedure-call and profile-interleaved theory rules | **Resolved via unreachability** (ADR-0099): reflected calls expand to formula-bearing clause-body children; `pos-call`/`neg-call`/`alt`/guarded tags are unreachable. The expansion is the explicit subtree, so its size is accounted (ADR-0097). |
| Quantifier instantiation and witness policy | **Resolved via unreachability** (ADR-0099): quantifier nodes introduce a `par-term` parameter / gamma witness and continue with the instantiated body as a formula-bearing child; `univ`/`once-univ`/`witness` tags are unreachable, with the instantiation carried explicitly and size-accounted. |

### Conditional and meta rows

| Row | Disposition |
|---|---|
| Mechanism of applying a rule once selected | **Discharged**: the reachability probes show each rule application (equality, call, quantifier) produces the same relevant formula-bearing child structure, with no compacting tag — exactly the condition the matrix required. |
| Beta truth and formula-class validation | Deferred trust boundary (ADR-0072): correspondence proceeds with beta membership as an assumption; full soundness needs a later validation or explicit trust statement. |
| Failure to close self-referential proof trees | Relevant *outcome*; the `^:slow` SelfCons negating-witness / subst-prf probes supply the operational negative evidence, paired with the Track 2b proof. |
| Formal specification medium for the correspondence proof | Track 2b **meta-obligation**: choose direct structural semantics over both proof relations or a shared/third-party formalization before claiming Track 2b complete. |

## What Track 2a establishes

The intensional features the matrix flagged as possibly relevant or risky are
either preserved with executable audits (tree, branching, ancestry, closure,
encoding, size) or shown to be **absorbed into the formula-bearing fragment**
(equality, procedure-call, quantifier) so they add no out-of-fragment rule
power and hide no subtree behind a compact tag. The user's original suspicion —
that tableau tree structure, rule-induced branching, branch closure, and
proof-size/encoding discipline are the relevant intensional measures, while
search scheduling and host mechanics are not — now holds with the high-risk
exceptions resolved rather than left open.

## Residual (Track 2b)

Track 2a is the classification; Track 2b is the proof. For each resolved
high-risk row the residual obligation is a correspondence theorem over the
formula-bearing tree: that the formula-bearing equality/call/quantifier closures
correspond to the selected SJAS deduction method `D` (or a specified
free-constructor / reflected-axiom / parameter theory) and preserve the
proof-size lower bound. The proof medium for that theorem is itself the open
meta-obligation above.
