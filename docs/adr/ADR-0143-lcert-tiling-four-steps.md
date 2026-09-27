# ADR-0143: λᶜᵉʳᵗ Tiling Metatheory — Charged Budgets, Lazy Supplies, Higher-Order Reflection, Changed Proof Systems

- Status: completed
- Date: 2026-09-27
- Branch: `claude/sjas-lobian-obstacle-r3csa9` (the session's designated
  branch; this research line has used it since ADR-less log work began on
  2026-09-26, so the one-branch-per-ADR convention is not applied here)
- AAR: [AAR-0143](../aar/AAR-0143-lcert-tiling-four-steps.md)

## Context

The user's claim, recorded verbatim in
[the assessment note](../log/2026-09-26-sjas-lobian-obstacle-assessment.md)
(addendum of 2026-09-27):

> The ability to specify an arbitrary token budget, produced lazily, seems to
> substantially resolve the Tiling Agents/Lobian Obstacle problem, for all
> agents feasible within the physical universe.

The assessment found that the claim resolves the telomere for tiling with an
identical system, on two conditions not yet met, and listed four steps that
would make it solid or refute it. The user then asked for exactly those four
steps, using the Ansatz Lean-in-Clojure library for the machine-checked parts:

1. Write up the model for the caller-charged budget, and prove the reflect rule
   for the agent type, with the key lemma machine-checked.
2. Add the primitive destructor, re-check the metatheory, and define what a
   lazy finite supply means.
3. Settle the higher-order case.
4. Test tiling across a changed proof system, to mark exactly where trust
   stops.

The object of study is λᶜᵉʳᵗ₀, the certificate calculus of `jpt4/sjas`
(branch `sjas-codification`, commit `f5a8490`): its implementation
`code/lcert` and its metatheory `nachlass/refinement/R4-metatheory.md`. Neither
is in this repository. The assessor's earlier experiments on it are recorded
as [a patch](../log/2026-09-26-lcert-reflect-experiments.patch) against that
commit.

What is already established, and what is not:

| Item | State before this ADR |
| --- | --- |
| T1 consistency, T3 size soundness, T4 evaluation, P5 G2-for-codes | proved on paper in R4 for `reflect` at base data types only |
| `reflect` at closed R/◇-free types | sound in R4's model; the assessor's paper argument |
| Caller-charged cap `n − ‖v‖ + m` | implemented as a demo switch; soundness argued on paper for first- and second-order types; not written up; not mechanized |
| `reflect` at the agent type `Π(s :₁ R). Σ(b :ω Act). Safe(b)` | runs in the delegation demo; not proved |
| Higher-order resource types | three shapes shown to fail in R4's fixed-cap sets; no counterexample to the calculus; open |
| Primitive destructor for R | not in the calculus; the definable `out` costs O(size) per step |
| Lazy supplies | not defined |
| Tiling across a changed proof system | argued (translation costs, G2); not tested |

Two environment facts shape the implementation: Clojars is blocked by this
environment's egress policy, so Ansatz 0.2.115 is built from its GitHub source
at the matching tag; and neither the Clojure CLI nor Leiningen is installed, so
the suites run through `java -cp` with jars fetched from Maven Central.

## Decision

Carry out the four steps in a new directory `research/lcert-tiling/`, and
decide the claim from their outcomes.

**Setup.**
- Vendor `code/lcert` at `f5a8490` unchanged into
  `research/lcert-tiling/lcert/`, with its provenance, as one commit.
- Apply the recorded experiments patch as the next commit, so every later
  change is a reviewable diff against the pristine calculus.
- Add scripts that build the classpath (Maven Central jars, Ansatz source at
  tag 0.2.115) and run the fast and extended suites without the Clojure CLI.
- Paper proofs go in `research/lcert-tiling/METATHEORY.md`; mechanized lemmas
  in Ansatz namespaces under `research/lcert-tiling/lcert/ansatz/`.

**Step 1 — the caller-charged model and the agent type.**
- Write the model: R4's semantic sets, with the denotation of `reflect`
  changed to run the decoded program at the charged cap `n − ‖v‖ + m`.
- State the class of first-order resource types for which the Reflect case
  holds, which must contain the agent types of the delegation demo, and prove
  the Reflect case for it.
- Mechanize, in Ansatz, the key lemma: the whole Reflect case at the agent
  type, from an abstract outer induction hypothesis, with the cap descent tied
  to the verified strict-overhead lemma on real code measures.

**Step 2 — the primitive destructor and lazy finite supplies.**
- Add `caseR`, a constant-time eliminator for R, to the typing rules, the
  encoding, `Check`, conversion and both evaluators.
- Re-check T1, T3, T4/T4′ and P5 with it, on paper, and mechanize the parts
  that are about sizes.
- Define a lazy finite supply: a promised size N and a deterministic
  generator, denoting one finite tree of exactly N nodes, materialized only
  where a program destructs it. State and test observational equality with
  the eager tree, and a materialization bound.
- Demonstrate a minting agent that parses its successors' certificates from a
  supply promised at 10^100 tokens.

**Step 3 — the higher-order case.**
- Replace R4's fixed-cap sets by a Kripke relation whose worlds are bounds on
  live tokens, with existential burns in function results, and prove the
  fundamental lemma for `reflect` at every closed `reflect`-free type.
- Mechanize, over an embedded type language with a type-indexed carrier:
  world and footprint monotonicity, the Reflect case for all types, the
  application case, and a machine-checked counterexample showing that R4's
  fixed-cap sets fail at a higher-order shape.
- Make sure the evaluator's closures capture their creation cap, as the model
  requires, and test the three shapes that failed before.

**Step 4 — tiling across a changed proof system.**
- Parameterize the typing rules and `Check` by a calculus version, so that
  parent and child can run different rule sets in one evaluation.
- Test the cases: identical system; a weaker child; a child with a new
  primitive that is definable in the parent (caseR), with and without
  translation; a child with a stronger reflection rule; a child with a new
  axiom; an unsound child.
- Tabulate exactly where trust stops, and what each crossing costs.

**Verdict.** Record in the AAR, the log note and `MEMORY.md` whether the claim
is solidified, refuted, or solidified in part, with the evidence for each
part.

## Consequences

- The repository gains a research directory with a vendored copy of another
  project's code. Its provenance is recorded, it is not on proflog's source
  paths, and proflog's own suites do not run it.
- Every claim of the verdict rests on one of: a mechanized lemma, a passing
  test, or a paper proof in `METATHEORY.md`. Which one is stated each time.
- Changes to λᶜᵉʳᵗ made here are proposals for `jpt4/sjas`; nothing is pushed
  there.

## Test Obligations

Red first, then green, for every obligation.

- **Step 1.** An extended-suite test requiring the Ansatz namespace of the
  charged-cap lemmas and checking that each named theorem is in the kernel
  environment. It fails before the namespace exists. The Reflect-case lemma
  must quantify over an arbitrary denotation and outer hypothesis, so that it
  is the case of the fundamental lemma, not an instance of it.
- **Step 2.** Tests that `caseR` type-checks with the stated usages; that
  `Check` accepts its derivations and rejects malformed ones; that conversion
  and both evaluators compute its ι-rules; that `out` built on `caseR` agrees
  with the definable `out`; that the parser consumes exactly `nodes(c)` supply
  nodes; that a lazy supply is observationally equal to its eager tree on a
  run; that a run on a supply promised at 10^100 materializes only what it
  destructs. The size lemmas are mechanized.
- **Step 3.** Kernel-checked monotonicity, Reflect-case, application-case and
  counterexample lemmas; tests that closures keep their creation cap; the
  three higher-order shapes return certified results under the charged cap.
- **Step 4.** Tests for each case of the Decision's list, asserting accept or
  reject, and the measured token cost of each translation.

The fast and extended suites must stay green at every commit.

## Exit Criteria

For each step, success and failure are fixed in advance.

| Step | Succeeds if | Fails (and then refutes or narrows the claim) if |
| --- | --- | --- |
| 1 | The Reflect case at the agent type is proved on paper for a stated class containing the demo's agent types, and its key lemma is kernel-checked | A counterexample to the Reflect case at an agent type is found, or the class cannot contain the agent types |
| 2 | `caseR` is added with T1, T3, T4/T4′ and P5 re-checked; a lazy finite supply is defined, shown equal to its eager tree on runs, and a 10^100-token supply runs in time proportional to use | Any of T1, T3, T4, P5 fails with `caseR`; or laziness cannot be made observationally transparent; or a run must materialize a supply in proportion to its promised size |
| 3 | A model validates `reflect` at every closed `reflect`-free type, or a counterexample to the calculus is exhibited | Neither: the case stays open (then the claim is limited to the class of Step 1) |
| 4 | Each case is tested and the table of where trust stops is filled from test results | A case cannot be constructed; it is then argued, and marked untested |

**The claim** is judged against the five desiderata of Yudkowsky and
Herreshoff's problem, as the assessment note does: indefinite tiling with an
identical system; no loss of strength per generation; quantified self-trust;
successors with changed proof systems; naturalistic trust. The AAR states, for
each, whether the four steps establish it, refute it, or leave it unaffected.
