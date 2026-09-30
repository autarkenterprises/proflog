# AAR-0143: λᶜᵉʳᵗ Tiling Metatheory — The Four Steps

- Date: 2026-09-27
- ADR: [ADR-0143](../adr/ADR-0143-lcert-tiling-four-steps.md)
- Branch: `claude/sjas-lobian-obstacle-r3csa9`
- Work: [research/lcert-tiling](../../research/lcert-tiling/README.md);
  proofs in [METATHEORY.md](../../research/lcert-tiling/METATHEORY.md)

## Outcome

The claim under test:

> The ability to specify an arbitrary token budget, produced lazily, seems to
> substantially resolve the Tiling Agents/Lobian Obstacle problem, for all
> agents feasible within the physical universe.

**Verdict: solidified for successors that keep their parent's proof system,
and refuted for successors that strengthen it.**

- **Solidified.** In λᶜᵉʳᵗ₁ — λᶜᵉʳᵗ₀ with the primitive destructor `caseR`,
  with `reflect` at every closed type that contains no `reflect`, and with
  the caller-charged cap as the meaning of `reflect` — an agent can trust
  successors that use the same calculus, generation after generation.
  - The rule and the trust term are the same at every generation. Only
    tokens fall, and a budget promised at any finite size, 10^100 included,
    costs nothing until it is spent.
  - The key cases are machine-checked.
  - A lineage on a supply promised at 10^100 tokens runs, and materializes
    exactly the certificates it mints.
- **Refuted in its full scope.** "All agents feasible within the physical
  universe" includes agents whose successors improve their own reasoning.
  - A parent trusts exactly the successor programs that have derivations in
    its own rules. A successor with a new axiom, whatever its truth, or a
    stronger reflection principle, even a sound one, is refused, however
    many tokens come with it. (*Revisited 2026-09-28:* the axiom tested,
    `ax-con`, was called true; its soundness is in fact open. See below.)
  - Adopting the stronger system needs justification from outside the
    system, and again at the next strengthening. The tower of strengthening
    theories that the Löbian obstacle produces returns there.
  - The budget moves none of this.
- **Untouched**, as before: quantified self-trust (P5: no budget derives
  `Con′`), and naturalistic trust (the safety predicate describes a model of
  the world, and the checker's implementation is trusted from outside).

The claim therefore resolves the resource side of the obstacle, the telomere
of identical-system tiling. Fallenstein's Tκ had resolved it in principle;
λᶜᵉʳᵗ₁ does it with evidence that is not relativized to a step count, and
now for higher-order agent types too. It does not resolve the logical side:
trusting stronger reasoning, which is where the second incompleteness
theorem binds.

## Against the desiderata

| Yudkowsky–Herreshoff desideratum | Outcome | Evidence |
| --- | --- | --- |
| Indefinite tiling with an identical system | **Established** for λᶜᵉʳᵗ₁ agents, to any finite depth the budget pays for; the budget is arbitrary and lazily produced | Steps 1–3; minting run at 10^100 |
| No loss of strength per generation | **Established:** same rules, same trust term at every generation; the cost is tokens | Steps 1, 3 |
| Reflectively coherent quantified self-trust | **Not established:** P5 holds for λᶜᵉʳᵗ₁; the axiom that would supply it (`Con′_ω`) is exactly what a parent cannot accept | METATHEORY §§1.5, 3.5, 4 |
| Successors with changed proof systems | **Refuted beyond derivable changes:** re-encodings and definitional extensions are trusted after translation (at up to ×2.3 certificate size, and with laziness lost for `caseR`); new axioms and stronger reflection are not | Step 4 |
| Naturalistic trust | **Unaffected** | — |

## The steps against their exit criteria

| Step | Exit criterion (ADR-0143) | Result |
| --- | --- | --- |
| 1 | Reflect case at the agent type proved for a class containing the demo's agent types; key lemma kernel-checked | **Met.** The class 𝓕 of first-order resource types contains every agent type of the demos and excludes the three higher-order shapes. `lcert.charged` (49 declarations) checks Q for every 𝓕-type of an embedded language and the whole Reflect case (`reflect_case_charged`) from an abstract outer hypothesis, with the cap descent from the verified strict-overhead lemma. |
| 2 | `caseR` added, T1/T3/T4/T4′/P5 re-checked; lazy finite supply defined, equal to its eager tree on runs; 10^100 supply runs in time proportional to use | **Met.** `caseR` is in every layer as a rule extension. The re-check is on paper, and the node split is kernel-checked (`caser_node_env`). A lazy supply is a promise N denoting the spine of N; observational equality is on paper and tested. `parse_spine` proves in the kernel that minting a code of k nodes consumes exactly k supply nodes. A minting lineage on 10^100 materializes exactly its 37,919 certificate nodes. |
| 3 | A model validating reflect at every closed reflect-free type, or a counterexample to the calculus | **Met, positively.** R4's sets provably fail at shape (c) (`fixed_rejects_c`). A world-indexed Kripke model validates the Reflect case at every type, with no class hypothesis (`reflect_case_world`, `lcert.kripke`, 32 declarations). The fundamental lemma's other cases are on paper; monotonicity and the application and pair cases are kernel-checked. |
| 4 | Each case tested; the table of where trust stops filled from results | **Met.** Seven child systems, one test each; the table is METATHEORY §4.2. |

## Evidence

- **Suites at the end:** fast 74 tests / 599 assertions; extended 102 tests
  / 2,034 assertions, including every Ansatz lemma re-verified by the kernel
  and its statement pinned. All green.
- **Red first.** Every new mechanism was red before it existed: the class
  predicates (`No such var: s/ordinary-type?`), `lcert.charged`, `caseR`
  (`s/*extensions*`), lazy supplies, `lcert.supply`, `lcert.kripke`, and the
  axioms (`unbound variable ax-con`). The minting, higher-order-shape and
  closure-cap tests pinned behaviour that already existed, and were green
  first, which their commits say.
- **What rests on paper.** In §3 the model's definition is general, but only
  the Reflect, application and pair cases and monotonicity are
  kernel-checked, over a non-dependent type fragment. The other cases of the
  fundamental lemma are on paper, as are dependent types, T4/T4′ with the
  charged cap, and P5 with the fixed-witness interpretation. P5 still cites
  S1–S3, as R4 does. None of this has been independently reviewed.
- **What the kernel checks.** Ansatz's Java kernel, built from the 0.2.115
  source tag, checks every lemma. Tactic bugs cannot produce a false
  theorem, since each proof term is re-checked. They cost time instead (see
  below).

## What went wrong, and what was learnt

- **Ansatz as a proof assistant.** Type-valued and Prop-valued definitions
  need the explicit recursor, and cannot be compiled to Clojure. `try` does
  not catch every elaboration error. `exfalso` drops introduced hypotheses.
  `rewrite` rotates the goal list. `omega` builds ill-typed proof terms, or
  exhausts the heap, on nested truncated subtraction and on some goals whose
  atoms are function applications. Each was worked around (LESSONS.md
  2026-09-27).
- **Process slips.** A `pkill -f` pattern killed its own shell and left a
  stale REPL script, so later probes ran old code until noticed.
  Test-fixture errors cost a few runs: `:d` is not a label, and a Σ at ω
  cannot hold a linear component. A usage-limit pause lost the REPL's
  in-memory kernel state; the development was replayed from scratch files.
- **Environment.** Clojars is blocked here; Ansatz was built from GitHub.
  One Maven Central fetch was throttled (HTTP 429), and the jars already
  downloaded were reused.

## Follow-ups

- Mechanize the remaining cases of §3's fundamental lemma over a dependent
  term language, or port the development to Lean 4, which the Ansatz kernel
  is compatible with.
- Offer λᶜᵉʳᵗ₁ (`caseR`, `:all`, charged cap) and the three Ansatz
  namespaces upstream to `jpt4/sjas`; nothing was pushed there.
- Reusable reflected evidence (`reflect-w`, specified in the 2026-09-26
  note) remains unimplemented.
- The physical bound is certificate size and checking time, not tokens: a
  λᶜᵉʳᵗ₁ delegating agent's certificate has 9,181 nodes, a minting agent's
  18,831. A more compact encoding that keeps strict overhead is still
  open (R4).

## Revisited 2026-09-28

Examining the motivation of λᶜᵉʳᵗ, that its self-justification comes from a
resource discipline
([note](../log/2026-09-28-lcert-self-justification.md)), exposed an error in
Step 4.

- **The error.** METATHEORY §4.1 called `P-con`'s axiom
  `ax-con : Con′_ω` sound, "since every set `V(T(chk′ c c⊥))` is empty by
  Corollary 3.7". But `chk′` names the running system's `Check`, and
  `P-con`'s `Check` accepts `ax-con`. So the axiom asserts `P-con`'s own
  consistency, and Corollary 3.7 for `P-con`'s `Check` *is* that
  consistency. The argument was circular. T1's induction on budgets does not
  reach the axiom either, since the axiom quantifies over codes of every
  size.
- **The correct status.** Whether `P-con` is consistent is open. No
  derivation of `0` is known: the Gödel–Löb arguments against a consistency
  axiom need D2 and D3 for the opaque `chk′`, which R4 §4.5 expects not to
  hold. It is a Feferman-style question. A true axiom of the kind intended
  would fix the checker to `λᶜᵉʳᵗ₁`'s rules; the implementation has none.
- **What changes.** The labels in METATHEORY §4.1–4.3, `MEMORY.md`, the
  axiom's docstring, and the name of the test, now
  `a-self-referential-consistency-axiom`. The test's assertions are
  unchanged and still hold.
- **What does not.** The verdict. A parent refuses a rule that is not its
  own whatever the rule's truth. The refusal of a *sound* strengthening
  rests on the stronger-reflection row, whose soundness §3 proves.
- **Why it matters beyond Step 4.** `ax-con` is exactly λᶜᵉʳᵗ's
  consistency constant with the resource discipline taken away. Its open
  status is part of the answer to whether the discipline is what makes
  λᶜᵉʳᵗ self-justifying (the note, §4).
- **2026-09-29: an audit of the runtime's token accounting.** The addendum
  "Pudlák Read In Full, Pudlák And Willard, And A Machine Model" (§C4)
  re-ran Step 2's minting lineage with every token creation recorded.
  - All 37,919 tokens came from the supply, and `reflect` created none.
  - But the evaluator does not itself check the reflect handoff: `m < ‖v‖`
    and the exact token count are assumed from `Check`'s encoding rule.
  - Step 2's materialization result stands. What stands on the checker
    rather than on the runtime is now stated. The hardening is proposed as
    [ADR-0144](../adr/ADR-0144-lcert-resource-machine.md).
- **2026-09-30: λᶜᵉʳᵗ's standing as self-justification.** The note's
  Addendum E2 concludes that λᶜᵉʳᵗ is not an analogue of Willard's
  self-justifying systems in substance, but their trade inverted.
  - Willard proves an unrestricted self-consistency statement in a weakened
    system. λᶜᵉʳᵗ proves a restricted one and keeps PA.
  - This verdict does not depend on that. Identical-system tiling rests on
    the form of `H°`: one sentence, usable at a runtime certificate, with
    its bound fixed by tokens (Addendum D1). That form is unchanged.
