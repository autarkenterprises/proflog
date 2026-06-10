# ADR-0073: SJAS Internalization Correspondence Program

- Status: in progress
- Date: 2026-05-25
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: pending

## Context

ADR-0072 moved the SJAS implementation away from generated formula, axiom, and
proof-target registries. Later ADR-0073/Track-1 work also replaced the
non-`sjas-axiom` `tableau-proof/3` and `subst-prf/4` shortcut through
`kernel/prove-programo` with a local proof-directed checker for the currently
generated certificate shapes. The stronger proof-machinery goal is not satisfied
by theorem-level agreement: for the literature route, the checker must become an
executable arithmetic/formal semantic-tableau predicate for the selected
`IS#_D(beta)` apparatus. A remaining bridge in that predicate is a Track 1
incompleteness, not a Track 2 escape hatch.

The refined concern is not simply that the call is host-side. The concern is
whether the call collapses distinctions that SJAS self-reference needs to
preserve. Willard's Type-A/tableau systems rely on a formalized proof predicate
for a concrete deduction apparatus. If the Proflog kernel accepts exactly the
same relevant proof objects as the SJAS-arithmetized semantic-tableau predicate,
and preserves every relevant proof-tree and size invariant, the kernel path can
be a justified implementation bridge. If it preserves only theorem-level
extension, it is not enough: theorem equivalence can erase the intensional
features that make tableau SJAS differ from Hilbert-style or list-style
systems.

The development note
[SJAS Internalization and Proflog Correspondence Program](../log/2026-05-25-sjas-internalization-correspondence-program.md)
records the exchange that split the prior "full internalization" goal into
three coordinated tracks. A later refinement added a speculative reciprocal
Track 2c: formalize the Proflog kernel itself as a candidate deductive
apparatus `D_Proflog` and determine whether Willard-style SJAS results can be
adapted to `IS#_{D_Proflog}(beta)`.

## Decision

Continue the ADR-0072 arithmeticization work, but treat it as one track of a
larger program rather than the only possible route to a justified SJAS claim.

Track 1 is direct arithmeticization of the literature predicate. The
implementation will continue replacing host-side staged readers, source
registries, decoded proof-term validation, and other predicate-application
shortcuts with object-language relations over encoded formulas, systems,
substitutions, proof trees, axiom membership, and branch closure. It also
includes the fixed-point formation of SelfCons with respect to the axiom basis
and deductive apparatus of the selected `IS#_D(beta)` instance. This remains the
strongest implementation endpoint.

Track 1 is not merely a project of assigning Godel codes to every dependency of
the proof predicate. Codes are necessary but inert unless the relations that
consume them are also represented in the SJAS object language. The direct
internalization track is therefore organized into the following logical slices:

1. **Code format:** expose formulas, systems, substitutions, and proof
   certificates as inspectable compact byte terms or U-Grounding numerals, not
   opaque host labels.
2. **Syntax:** define object-level predicates such as `wff`, formula-class
   predicates, and `neg-pair` over encoded formulas.
3. **System-code and fixed-point axiom reconstruction:** derive
   `axiom-member(system-code, formula-code)`, `AxiomConj(system-code)`, fixed
   Group-0/Group-1 axioms, reflected Group-2b clause data, and the
   profile-determined Group-3 SelfCons sentence from the encoded finite system
   descriptor and profile relations rather than from generated host registries.
   Reconstruction must preserve the public code representation selected by the
   presented `system-code` term, so a U-Grounding `s` does not accept a compact
   `code-N` variant of its fixed-point sentence merely because the byte payload
   is the same.
4. **Proof-code grammar:** encode and decode proof constructors and payload
   evidence as proof trees that can be inspected by the predicate.
5. **U-Grounding arithmetic:** evaluate the arithmetic relations needed for
   code decoding, byte/numeral interpretation, relation-backed fixed-radix
   shifts, and proof-predicate helper relations inside the selected SJAS
   profile.
6. **Substitution and fixed points:** internalize `subst-code/2`,
   `subst-prf/4`, diagonal substitution, and alpha-equivalent fixed-point
   comparison over formula codes.
7. **Tableau proof checking:** validate proof-code trees against formula-code
   trees through object-level branch expansion, quantifier, equality,
   disequality, saved-literal, and closure relations.
8. **Reflected procedure calls:** recover positive and negative procedure-call
   proof evidence from reflected clauses decoded from `system-code`, not from
   external runtime clause tables.

The normative Track 1 target is specified in
[SJAS Tableau Proof Arithmeticization Specification](../SJAS_TABLEAU_ARITHMETIZATION_SPEC.md).
That artifact fixes the semantic endpoint for this track: an object-language
relation `TabPrf_beta(system-code,theorem-code,proof-code)` whose truth means
that the proof code decodes to a closed semantic tableau for the decoded finite
system axioms plus the negated theorem. Performance shortcuts and proof-directed
entry points are acceptable only insofar as they instantiate that relation over
the accepted proof-code fragment.

The Track 1 minimum viable endpoint is correspondingly concrete: for the
selected ordinary-tableau `IS#_D(beta)` instance, the public proof predicate must
accept the decoded system code `s`, the decoded code `t` of the system's own
Group-3 consistency statement, and the decoded proof code `p` of a semantic
tableau proving that statement from `AxiomConj(s)`. This acceptance must proceed
by reading `s`, `t`, and `p`, reconstructing `AxiomConj(s) /\ not(Formula(t))`,
and validating the proof tree through the arithmeticized tableau relation. The
`sjas-axiom` citation path is useful coverage for axiom membership, but by
itself it is not the MVP evidence for full proof-machinery internalization.

These slices cannot be collapsed into one monolithic implementation step
without losing diagnostic clarity. A failed monolithic proof predicate may fail
because of code injectivity, symbol-table reconstruction, syntax decoding,
arithmetic normalization, substitution, axiom membership, equality closure,
procedure-call recovery, or the tableau rule itself. Each slice must therefore
name the host shortcut being removed, provide focused red/green evidence, and
record whether the result is fully internalized or remains a runtime
tractability boundary. If a dependency cannot be implemented without modifying
the deductive apparatus, that is a Track 1 gap until the project explicitly
chooses a different apparatus under Track 2.

Track 2a is relevance analysis for modifications and alternative apparatuses.
It does not justify leaving the literature `IS#_D(beta)` proof predicate
partially implemented in Track 1. Before any Proflog-derived or otherwise
modified apparatus can be treated as an SJAS deductive apparatus in its own
right, the project must determine which intensional aspects of the
semantic-tableau apparatus are necessary for SJAS self-justification. The
working hypothesis is that the tableau-induced tree structure, rule-induced
branching, closure discipline, proof-object inspectability, and lower-bound
proof-size discipline are relevant. Rule search scheduling, implementation
caching, and host data representation are expected to be irrelevant only when a
proof shows that they do not alter the accepted proof objects or the relevant
size/tree measures. Ambiguous extensions, including equality and
procedure-call/profile rules, remain unresolved until classified.

Track 2b is Proflog or variant-apparatus correspondence. This track depends on
Track 2a. It applies when the project studies a modified deductive apparatus,
or compares Proflog acceptance with the literature tableau predicate as a
separate theorem. It cannot be used to declare the literature predicate
implemented while one of its proof-machinery dependencies is still missing. The
project must prove and test a bidirectional correspondence of the following
shape:

```text
ProflogAccepts(P, S, F)
iff
SJAS_TableauProof(code(P), code(S), code(F))
```

The proof must preserve every intensional measure classified as relevant by
Track 2a, and must prove irrelevance for every implementation detail that is
ignored. Operational tests are required in addition to the proof; they do not
substitute for it.

"Proof" in this track means a proper formal or formalizable correspondence
argument, not an informal comparison of passing examples. At minimum, the
Proflog kernel proof relation must be specified in terms compatible with the
SJAS semantic-tableau specification, so both sides can be compared for
equivalence or the absence of equivalence can be identified precisely. If direct
examination of the implementation and SJAS specification cannot support that
level of rigor, one or both sides must be encoded into a third-party formal
verification setting, or both must be lifted into a common intermediate
semantics that admits rigorous equivalence checking.

Track 2c is Proflog-as-D formalization. It is speculative and reciprocal to
Track 2b. Instead of asking whether Proflog acceptance corresponds to
Willard's selected semantic-tableau `D`, it asks whether the Proflog kernel can
itself be specified as a deductive apparatus `D_Proflog` and whether the SJAS
literature's results can be adapted to `IS#_{D_Proflog}(beta)`. This route
requires a stable mathematical semantics for the Proflog kernel, including its
proof states, branch agendas, equality/disequality machinery, procedure calls,
profile rules, proof certificates, and proof-size measure. It does not license
using "whatever the implementation currently accepts" as `D`.

## Consequences

- ADR-0072 remains useful and active: each removed host shortcut reduces the
  proof burden of any eventual correspondence theorem.
- A kernel call is not accepted merely because Proflog is sound for the same
  formulas. It is accepted only if the relevant proof-object correspondence is
  established.
- A theorem-level "Proflog proves exactly what SJAS proves" result is
  insufficient if it loses proof-tree, closure, encoding, or proof-size facts
  relevant to SJAS self-reference.
- Some variant work should wait for the relevance matrix. Code that would
  preserve irrelevant details only for their own sake is not required for a
  modified apparatus, while code that hides structure relevant to the literature
  predicate must be replaced before Track 1 is complete.
- The correspondence work may identify Proflog implementation details that need
  explicit proof-term instrumentation or tests even when no user-visible theorem
  result changes.
- Track 2c, if pursued, has its own burden: define `D_Proflog` as a formal
  calculus and adapt the relevant Willard proof obligations to that calculus.
  It is not a weaker version of Track 2b.

## Test Obligations

Tests must be red before implementation and then pass for any code slice.
Documentation-only slices must still pass `git diff --check` before commit.

- Track 1 tests must expose the specific host shortcut being removed and show
  that the relevant predicate still succeeds or fails through object-level
  relations after the shortcut is gone.
- Track 1 implementation slices must identify which proof-predicate dependency
  is being internalized: code format, syntax, system-code and fixed-point
  `AxiomConj` reconstruction, proof-code grammar, U-Grounding arithmetic,
  substitution/fixed points, tableau proof checking, or reflected procedure-call
  recovery. Each slice must leave behind a focused regression or source audit
  that would fail if the prior host shortcut or nominal registry path were
  reintroduced.
- Track 2a must maintain a relevance matrix that names each intensional aspect,
  classifies it as relevant, irrelevant, or unresolved, cites the project or
  Willard source for that classification when available, and records the proof
  obligation created by the classification.
- Track 2b must include both a written proof artifact and operational tests.
  The tests must exercise representative proof objects in both directions of
  the correspondence and must include negative cases for features classified as
  relevant.
- The Track 2b proof artifact must state the formal semantics used for the
  Proflog kernel side and for the SJAS tableau side. It must justify why those
  semantics are compatible enough for equivalence checking, or document the
  selected third-party formalization/common intermediate language used to make
  the comparison rigorous.
- Track 2c artifacts must state the formal semantics of `D_Proflog`, the proof
  certificate grammar, the proof-size/lower-bound measure, and which
  Proflog-specific rules are primitives, bounded macros, irrelevant runtime
  details, or excluded features.
- For semantic implementation commits, use the focused SJAS practice recorded
  in `AGENTS.md`: exact selectors first, then `lein test-vars <namespace>` or
  `lein test-proflog-sjas-focused` for expensive SJAS namespaces. Broad gates
  remain `lein test-proflog-fast` and `lein test-proflog-extended`; use the
  opaque `lein test-proflog-sjas` gate only when it adds signal or as a final
  full namespace confirmation if its runtime is acceptable.

## Exit Criteria

- The remaining ADR-0072 host paths in the literature predicate are audited and
  internalized. Paths that require a modified deductive apparatus are moved out
  of Track 1 and recorded as Track 2 variant work.
- The Track 2a relevance matrix is complete enough to classify every proof
  object, rule, encoding, closure, search, and size feature relied on by the
  current Proflog SJAS implementation.
- A written correspondence proof is supplied for the Proflog kernel and the
  SJAS-specified semantic-tableau predicate, preserving all relevant measures
  and proving irrelevance for ignored implementation details.
- The proof is based on compatible formal specifications of both sides, or on a
  third-party/common-intermediate formalization capable of checking the claimed
  equivalence.
- Operational tests demonstrate the correspondence over the implemented proof
  machinery, including representative positive and negative cases.
- The implementation passes `lein test-proflog-fast`,
  `lein test-proflog-extended`, and `lein test-proflog-sjas`; any deliberately
  deferred slow probes are recorded under `docs/log/` with rationale.
- After the Track 1 proof machinery is fully internalized, the project prints
  the numerical Godel code of the self-consistency statement for the concrete
  `IS#_D(beta)` instance. This must be computed from the fully internalized
  system/formula coding path, not from a transitional host-side proof-predicate
  shortcut.
- The AAR records which route justified each implementation boundary: direct
  arithmeticization for the literature predicate, or explicit relocation into a
  modified Track 2 apparatus approved by the user.
