# Computational Self-Justification: Assessment Against The Artifact

Date: 2026-06-10

This note records the 2026-06-09/10 audit's assessment of the project's
three research questions, as they stand after ADR-0073 Track 1 completion
and the ADR-0086/0087/0089 literature-fidelity corrections, together with
commentary on the `project_landscape.txt` framing (Autarkic Systems). It is
an assessment, not a results claim: each paragraph names what the executable
artifact currently evidences and what remains conjecture.

## Question 1: Is there a direct computational equivalent of logical self-justification?

The property splits under translation.

The *self-reference* half has an exact computational equivalent: Kleene's
second recursion theorem — quining. The implementation discovered this
empirically rather than by stipulation: the hardest Track 1 slices were
precisely the machinery of a program carrying and reconstructing its own
description (embedded payload header-first dispatch, public compact byte
readers, walked system-code reconstruction, diagonal substitution,
`AxiomConj(s)` reconstruction from the descriptor `s` whose decoded basis
includes the Group-3 sentence formed over `s` itself). ADR-0089 sharpened
the point: the self-reference must be anchored to the *presented* public
code representation, not merely to an equivalent byte payload — fixed-point
identity is representation-sensitive, which is itself a finding about what
"this very system" means computationally.

The *justification* half — consistency — has no positive runtime witness.
It is a Pi-1 property; a running system manifests it only negatively, by
its self-consistency oracle never being refuted. What the artifact makes
precise is the operational content of asserting it anyway: self-justification
is O(1) axiomatic access to a global property of the system's own search
space that the system could never establish by running itself. This
asymmetry is now directly observable in the artifact: citing `SelfCons`
through `axiom-member` validates in minutes (the public Tableau-0 selector,
about 8m30s end to end), while the negative searches that would *establish*
comparable content by exhaustion exceed 40-minute and multi-CPU-hour budgets
(`TEST_RUNTIME_BASELINE.md`, post-ADR-0086 rows) — and for the real claim,
quantifying over all proof codes, they never terminate. That is the Second
Incompleteness Theorem rendered as a runtime phenomenon.

Two structural anchors situate the construction among known computational
phenomena. First, Brown-Palsberg self-interpretation for strongly
normalizing calculi: folklore held that diagonalization forbids a
self-interpreter for System F-omega; the knot was tied anyway by weakening
the representation demanded of "self-interpreter" — formally Willard's move
(weaken the arithmetic and the deduction method until the diagonal argument
starves) transposed to programming languages. Verified self-hosting chains
(Milawa, CakeML) are the *stratified* resolution of the same tension; SJAS
is the *knotted* one. Second, the project's own history: negation-as-failure
justifies `not p` by the finite failure of the system's own proof search;
`SelfCons` asserts the infinitary analogue by axiom. The working slogan:
**self-justification is to consistency what negation-as-failure is to
finite failure.**

## Question 2: What effect does self-justification have on programming in an SJAS-lang?

Three effects are already supported by the artifact's behavior; each is a
candidate worked-example or paper section.

1. **Anti-modularity of self-trust.** Consistency does not compose, and the
   builder enforces the computational image of that fact: adding or
   changing a reflected (Group-2b) clause changes the system identity and
   regenerates Group-3, while external clauses change neither (the
   Godel-code regression demonstrates both directions). SJAS-lang programs
   are therefore quine-like whole artifacts: linking is not concatenation
   but re-tying the fixed point. The cost structure of trusted
   self-modification — what must be recomputed when the reflected basis
   changes, what survives — is empirically measurable in this system.
2. **Boundedness as a type discipline.** No total multiplication appears
   computationally as no multiplicative term growth, finite-table
   scheduling, and bounded byte decoding; ADR-0087's build-time Pi*1
   validation of the reflected basis makes the formula-class discipline a
   compile-time error. The class restrictions self-justification
   presupposes function, in programming terms, as a type system.
3. **A Lobian ceiling with measurable height.** The language may trust
   itself only by axiom, never by derivation; Willard's boundary results
   (Tab-1 survives, Tab-2 and Hilbert deduction collapse) give the ceiling
   a precise location. This suggests the strength-dial experiment: add
   multiplication totality or Tab-2-style theorem reuse to a generated
   system and observe self-refutation becoming derivable — the Second
   Incompleteness Theorem as an observable phase transition in a running
   program. The audit's deepest open finding feeds the same experiment from
   below: the implemented checker's arithmeticized closure rules already
   extend Willard's complementary-pair-only closure, so the Track 2a
   relevance matrix must locate the current apparatus on the same dial.

## Question 3: If there is no direct equivalent, what is the nearest computational property?

The honest synthesis: the equivalent is *conditional and composite* —
quining plus a Pi-1 certificate that the program asserts but can never
exhibit, sound only because the language is weakened until the diagonal
argument fails. Nearest kin, ordered by structural distance:
negation-as-failure / closed-world assumption (local, finitary
self-justification of negative facts); proof-carrying code (certificate
without self-reference); verified self-hosting towers (self-reference
stratified away); Brown-Palsberg-style self-interpretation under weakened
representation (the closest structural cousin); and the tiling-agents /
Lobian-obstacle literature on self-modifying agents, where Willard's
systems were explicitly proposed as an escape hatch and where this artifact
appears to be the first executable instance anyone could run.

## Commentary On `project_landscape.txt` (Autarkic Systems Framing)

The landscape draft situates SJAS inside the Autarkic Systems programme:
what are the self-powered capabilities of a system, with Autarkic Formal
Systems as the formal arm, SJAS paradigmatic because it pairs a precise
capability (syntactic consistency) with a precise autarky (self-provability
thereof), and an applied aim of epistemic security for artificial cognitive
entities — non-underminable certainty in the *capacity* of one's thought,
as distinct from the correctness of any particular thought.

Two connections between that framing and the audit results are worth
recording:

1. The audit independently converged on Brown-Palsberg as the sharpest
   computational cousin of Willard's construction before reading the
   landscape draft, which already lists it in the AFS constellation. The
   convergence is mild evidence that the "narrow path" (tableau LP as the
   correspondence substrate) generalizes: the same weaken-until-the-diagonal-
   fails pattern recurs wherever self-representation meets normalization.
2. The epistemic-security target — provable conservation of invariants
   across self-modification — is exactly where the anti-modularity effect
   (Question 2.1) bites. The artifact already demonstrates that any change
   to the reflected basis forces re-tying the Group-3 fixed point; the
   open question it makes tractable is the *cost and ceremony* of that
   re-tying under trusted self-modification, which is the computational
   shadow of the Lobian obstacle. A worked example that modifies a
   reflected clause and tracks exactly which certificates survive would be
   the first concrete measurement in that direction.

The landscape draft's bracketed alternatives (theorem prover for SJAS, type
system rather than language) remain open; the audit adds one datum in their
favor and one against: for — the boundedness-as-type-discipline effect
suggests the type-system rendering is natural; against — the
representation-sensitivity of fixed-point identity (ADR-0089) is easiest to
study where codes are first-class runtime values, as in the present
language rendering.
