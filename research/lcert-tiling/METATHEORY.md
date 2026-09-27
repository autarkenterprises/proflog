# λᶜᵉʳᵗ tiling metatheory (ADR-0143)

*Paper proofs for the four steps of
[ADR-0143](../../docs/adr/ADR-0143-lcert-tiling-four-steps.md). R4 means
`nachlass/refinement/R4-metatheory.md` of `jpt4/sjas` at `f5a8490`, whose
notation this document keeps: `Vⁿₖ(A)` is the semantic set of type `A` at
budget cap `n` and footprint `k`, `‖v‖` is a certificate's number of internal
nodes, `Θₘ` is a context of `m` tokens, and Lemma 2.7 (strict overhead) says
a certificate `Check` accepts declares fewer tokens than it has nodes.*

> **Status.** Each result says how it is established:
> - **[Ansatz]** — kernel-checked, in the namespace named;
> - **[test]** — exercised by the named test namespace;
> - **[paper]** — proved here, not mechanized.
>
> The Ansatz developments embed a non-dependent fragment of the type
> language; dependent types and `T(b)` are covered on paper only (§1.2).

---

## 1. The caller-charged model, and reflect at the agent type (Step 1)

### 1.1 The change to the calculus

Two changes to λᶜᵉʳᵗ₀ as R4 fixes it.

1. **The Reflect rule's side condition.** R4 allows `reflect_D` for base data
   types `D` only. Here `reflect_A` is allowed for every closed type `A` in
   the class 𝓕 of §1.2 that contains no `reflect`. The rule is otherwise R4's:

   ```
   A ∈ 𝓕, closed, reflect-free;   Γ₁ ⊢ r :¹ R;   Γ₂ ⊢ e :¹ T(chk′ (print r) ⌜A⌝)
   ──────────────────────────────────────────────────────────────────────────
                        Γ₁ + Γ₂ ⊢ reflect_A r e :¹ A
   ```

2. **The denotation of `reflect`** (R4 §3.2) runs the decoded program at the
   *caller-charged cap*. Let `v = ⟦r⟧ⁿη`. If `‖v‖ ≤ n` and
   `Check(print v, ⌜A⌝) = tt`, decode `Θₘ ⊢ t′ :¹ A` and put

   ```
   ⟦reflect_A r e⟧ⁿη = ⟦t′⟧ⁿ′(x₁ ↦ ◇, …, xₘ ↦ ◇),   n′ = n − ‖v‖ + m;
   ```

   otherwise `dflt`. R4 used `n′ = m`.

   *Reading.* The caller's cap `n` bounds every token in play. Reflecting
   burns the certificate's `‖v‖` tokens and gives the decoded program `m`
   fresh ones, so at most `n − ‖v‖ + m` tokens exist afterwards. The decoded
   program is judged against every supply that could still exist, not only
   against its own `m` tokens. Under R4's `n′ = m` it would be judged only on
   supplies of zero footprint, which is why reflecting at a type that consumes
   certificates was unsound there (`lcert.resource-reflect-test`).

Implemented as `lcert.syntax/*reflect-class*` `:first-order` (the typing rule
and `Check`) with `lcert.eval/*charged-cap*` (the evaluator). **[test]**
`lcert.charged-reflect-test`.

**Lemma 1.1 (the charged cap descends).** If `m < ‖v‖ ≤ n` then
`n − ‖v‖ + m < n`, and `m ≤ n − ‖v‖ + m`. For a derivation-shaped code `c`
with `nodes(c) ≤ n`, `n − nodes(c) + budget(c) < n`.
*Proof:* arithmetic; the last clause by Lemma 2.7. **[Ansatz]**
`lcert.charged/charged_cap_lt`, `charged_cap_fits`, `charged_cap_code`.

So the denotation is still defined by well-founded recursion on `(n, t)`
ordered lexicographically: every `reflect` recurses to a strictly smaller cap,
and the all-token environment `(◇, …, ◇)` satisfies `Θₘ` at the new cap.

### 1.2 The classes O ⊆ D ⊆ 𝓕

All three are syntactic classes of types.

- **O, ordinary types:** no `R`, no `◇`, no `reflect` anywhere.
- **D, cap-free types:** O-types, `R`, `◇`, and `Σ(x :ρ A). B` with `A` and
  `B` in D.
- **𝓕, first-order resource types:**
  - every D-type;
  - `Π(x :ρ A). B` with `B ∈ 𝓕` and `A ∈ D` (any `A` when `ρ = 0`);
  - `Σ(x :ω A). B` with `A ∈ D`, `B ∈ 𝓕`; and `Σ(x :₀ A). B` with `B ∈ 𝓕`;
  - `Σ(x :₁ A). B` with one of `A`, `B` in D and the other in 𝓕.

Inside a type, `T(b)` is an O-type as long as `b` has no `reflect`; its
argument may mention bound variables.

**The agent types are in 𝓕.** `Agent = Π(s :₁ R). Σ(b :ω Act). Safe(b)`: its
input `R` is in D and its result is an O-type. So are the minting agent
`Π(cs :ω Syn). Agent`, the observing agent `Π(s :₁ R). Π(i :ω I). Σ(b :ω Act).
Safe(b)`, and the consumer `H = Π(s :₁ R). T(chk′ (print s) ⌜σ⌝) ⊸ σ`.
**[Ansatz]** `agent_in_F`, `minting_agent_in_F`; **[test]** `the-classes`.

**The three higher-order shapes are not in 𝓕.** (a) `(H ⊸ σ) ⊸ σ` has an
input that is not in D; (b) `1 ⊸ H ⊗ H` has a tensor with no D side; (c)
`1 ⊸ Σ(f :ω H). 1` has a reusable component not in D. **[Ansatz]**
`shape_a_not_F`, `shape_b_not_F`, `shape_c_not_F`. They are §3's subject.

**The embedding.** `lcert.charged` defines an inductive `STy` of
non-dependent types — ordinary data `tdat`, the agent's result `tgood`, `R`,
`◇`, `Π₁`, `Π_ω`, `⊗` and `Σ_ω` — with a carrier `SCar : STy → Type` and R4
§3.3's sets `VF t n k x` clause for clause, and the classes as predicates
`IsO`, `IsD`, `IsF`. Dependency adds nothing to the arguments below: every
clause is read in an environment `(η, x ↦ a)`, and O-types, including `T(b)`,
have sets independent of cap and footprint (R4 §3.3).

### 1.3 The inclusions

**Lemma 1.2 (footprint and cap).**
1. `Vⁿₖ(A) ⊆ Vⁿₖ′(A)` for `k ≤ k′ ≤ n` (R4 Lemma 3.3).
2. For `A ∈ O`, `Vⁿₖ(A) = Vⁿ′ₖ′(A)` whenever `k ≤ n` and `k′ ≤ n′`.
3. For `A ∈ D`, `Vⁿₖ(A) = Vⁿ′ₖ(A)` whenever `k ≤ n` and `k ≤ n′`.
4. `O ⊆ D ⊆ 𝓕`.

*Proof.* Induction on `A`.
1. Raising `k` shrinks the range `j ≤ n − k` of a `Π₁` clause and enlarges
   every target.
2. For a `Π₁` whose parts are O-types, the range `j ≤ n − k` is never empty
   when `k ≤ n`, and the argument's set is the same for every `j`, so the
   clause says `∀a ∈ V(A). f(a) ∈ V(B)` at every cap. Pairs split at `0`.
3. The sets of `R` and `◇` never mention the cap; O-types by 2; pairs
   componentwise.
4. By the definitions. ∎

**[Ansatz]** `VF_foot_mono`, `oindep`, `capfree`, `o_sub_d`, `d_sub_f`.

**Lemma 1.3 (Q, the charged inclusion).** For `A ∈ 𝓕`, and caps and
footprints with `n′ ≤ n`, `m ≤ k ≤ n`, `m ≤ n′` and `n − n′ ≤ k − m`:

```
Vⁿ′ₘ(A) ⊆ Vⁿₖ(A).
```

*Proof.* Induction on `A`; each case keeps the gap `n − n′ ≤ k − m`.
- **D-types.** `Vⁿ′ₘ = Vⁿₘ ⊆ Vⁿₖ` by Lemma 1.2 (3, then 1).
- **`Π(x :₁ A′). B`, `A′ ∈ D`.** Let `f ∈ Vⁿ′ₘ`, `j ≤ n − k` and
  `a ∈ Vⁿⱼ(A′)`. Then `j ≤ n − k ≤ n′ − m`, so `a ∈ Vⁿ′ⱼ(A′)` (Lemma 1.2.3)
  is an argument `f` accepts, and `f(a) ∈ Vⁿ′ₘ₊ⱼ(B)`. The hypothesis at
  `(n′, m + j, n, k + j)`, whose gap is again `k − m`, gives
  `f(a) ∈ Vⁿₖ₊ⱼ(B)`.
- **`Π(x :ω A′). B`, `Π(x :₀ A′). B`.** The argument is at footprint `0` (or
  unconstrained); the hypothesis on `B` at the same indices.
- **`Σ(x :ω A′). B`, `A′ ∈ D`.** The first component by Lemma 1.2.3, the
  second by hypothesis.
- **`Σ(x :₁ A′). B`, `A′ ∈ D`, `B ∈ 𝓕`.** Keep the split `j`; the second
  component by hypothesis at `(n′, m − j, n, k − j)`.
- **`Σ(x :₁ A′). B`, `A′ ∈ 𝓕`, `B ∈ D`.** Move the split to
  `j + (k − m)`: the first component by hypothesis at
  `(n′, j, n, j + (k − m))`, the second by Lemma 1.2.3 and then 1, since
  `m − j = k − (j + (k − m))`. ∎

**[Ansatz]** `q_f` (every `IsF` type), from the case lemmas `q_lol`, `q_fun`,
`q_bang`, `q_ten_l`, `q_ten_r`.

*Why the class ends where it does.* The tensor case can give the gap to one
component only: two consumers side by side would each need it (shape b). An
`ω` component has footprint `0` at both caps and gets no gap (shape c). A
function argument that itself consumes certificates would need the inclusion
in the opposite direction, which fails (shape a). §3 removes all three limits
with a different model.

### 1.4 The Reflect case, and the fundamental lemma

**Theorem 1.4 (soundness at budget `n`).** R4's Lemma 3.6 holds for λᶜᵉʳᵗ₀
with the Reflect rule of §1.1, under the charged denotation: if
`Γ ⊢ t :¹ A` is derivable and `η ⊨ⁿₖ Γ`, then `⟦t⟧ⁿη ∈ Vⁿₖ(A)η`.

*Proof.* R4's proof, by strong induction on `n` and then on the derivation.
Only the Reflect case changes; the others never consult the denotation of
`reflect`, and the H₁ case still descends to budget `m₁ + m₂ < n`.

**Reflect at `A ∈ 𝓕`.** The inner hypothesis gives `v = ⟦r⟧ⁿη` with
`‖v‖ ≤ k₁`, and `⟦e⟧` in `V(T(chk′ (print r) ⌜A⌝))`, so the check computes to
`tt`, with `k₁ + k₂ ≤ k ≤ n`.
1. By R4 Lemmas 2.6–2.8, `print v` encodes `Θₘ ⊢ t′ :¹ A`, and `m < ‖v‖`.
2. The cap test `‖v‖ ≤ n` holds, so the value is `⟦t′⟧ⁿ′(◇, …, ◇)` with
   `n′ = n − ‖v‖ + m < n` (Lemma 1.1).
3. The all-token environment satisfies `Θₘ` at cap `n′` and footprint `m`,
   since `m ≤ n′`. The **outer** hypothesis at `n′` gives
   `⟦t′⟧ⁿ′ ∈ Vⁿ′ₘ(A)`.
4. The gap is `n − n′ = ‖v‖ − m ≤ k − m`, and `m < ‖v‖ ≤ k`. Lemma 1.3
   gives `⟦t′⟧ⁿ′ ∈ Vⁿₖ(A)`. ∎

**[Ansatz]** `lcert.charged/reflect_case_charged` is exactly this case, for
every `IsF` type of the embedding. It quantifies over an arbitrary
denotation `den : ℕ → CT → SCar t` and derivability predicate, assumes only
the outer hypothesis at smaller caps (step 3), and takes the certificate as a
derivation-shaped code `cell l true j p` so that strict overhead (step 1) and
the cap arithmetic (step 2) are the verified ones of `lcert.verified`.

### 1.5 What else holds

- **T1 (consistency)** and **T3 (size soundness)** follow from Theorem 1.4 as
  in R4 §§3.7–3.8. Corollary 3.7 (no code checks as a refutation or as a
  contradictory pair) holds for the extended `Check`. **[paper]**
- **T4 and T4′ (evaluation).** R4's proofs relate the evaluator to the model
  by strong induction on the cap. Their Reflect cases call the evaluator on
  the decoded program at the cap the model uses; with the charged cap that is
  `n′ < n`, again covered by the outer induction. Neither relation depends on
  the cap. The evaluator's closures capture the cap at which they were
  created, as the model's `⟦λx. t⟧ⁿ` does (`lcert.eval`, `:lam`). **[paper]**;
  the evaluator's charged caps are traced in **[test]**
  `charged-delegation-under-the-first-order-rule`.
- **P5 (the second incompleteness theorem for codes).** R4 §6.3 interprets
  `reflect` by defaults, which fails at a type whose default is not in its
  set (such as `Σ(x :ω Nat). T(x ≠ 0)`). Interpret instead `reflect_A r e` by
  `⟦t*⟧`, where `t*` is the program of a smallest certificate `c*` of `A`, of
  size `μ(A)`.
  - If some certificate of `A` has at most `n` nodes, E-PA^ω proves
    `V_{m*}(A)(⟦t*⟧)` by Lemma 6.4 applied to the derivation of `c*`, at the
    same `n`, and then `V_k(A)(⟦t*⟧)` by footprint monotonicity, since
    `m* < μ(A) ≤ ‖r‖ ≤ k`. That `‖r‖ ≥ μ(A)` when `r` checks at `A` is a
    bounded fact, true by the definition of `μ` and proved in PA by S2.
  - Otherwise "no code of at most `n` nodes checks at `A`" is a true bounded
    fact, and the case is vacuous, as BF₀ makes `H`'s.
  - Inside `t*`, a `reflect_B` either has `μ(B) ≤ m* < μ(A)` or is vacuous by
    the bounded fact at `m*`. So Lemma 6.4 is proved by an outer induction on
    `μ`, at the meta level, with finitely many cases.

  No cap arithmetic enters: the interpretation never evaluates `r`. The
  bounded facts are true because of T1 for the extended calculus, above.
  **[paper]**

---

## 2. The primitive destructor, and lazy finite supplies (Step 2)

### 2.1 `caseR`

**Why it is needed.** R4 has one eliminator for certificates, `itR`, which
iterates over the whole tree; the destructor `out : R ⊸ V` of R4 §4.7 is
defined from it, so taking one node off a supply of `N` nodes costs `O(N)`
and rebuilds every subtree. A parser that mints a certificate of `k` nodes
from a supply of `N` therefore costs `O(k · N)` and touches all `N` supply
nodes. A supply promised at 10^100 could not be used at all.

**The rule** (a *rule extension*: `lcert.syntax/*extensions*` contains
`:caseR` by default):

```
Γ₁ ⊢ r :¹ R      Γ ⊢ X type      Γ₂, a :ω Lbl ⊢ t_l :¹ X
Γ₂, d :₁ ◇, a :ω Lbl, r₁ :₁ R, r₂ :₁ R ⊢ t_n :¹ X
───────────────────────────────────────────────────────────
Γ₁ + Γ₂ ⊢ caseR_X(r, a. t_l, d a r₁ r₂. t_n) :¹ X
```

- The motive `X` does not depend on `r`, as `itR`'s does not.
- One branch runs, so the branches share `Γ₂`, as `if`'s do.
- **Conversion** gains two ι-rules:
  `caseR_X(leaf a′, …) ⇝ t_l[a′/a]` and
  `caseR_X(node d′ a′ r₁′ r₂′, …) ⇝ t_n[d′, a′, r₁′, r₂′ / d, a, r₁, r₂]`.
- **The model:** `⟦caseR_X(r, …)⟧ⁿη` is `⟦t_l⟧ⁿ(η, a ↦ ℓ)` when `⟦r⟧ⁿη` is
  `leaf ℓ`, and `⟦t_n⟧ⁿ(η, d ↦ ◇, a ↦ ℓ, r₁ ↦ v₁, r₂ ↦ v₂)` when it is
  `node ◇ ℓ v₁ v₂`.
- **Encoding and `Check`:** two new labels, `:caseR` (the term) and `:CaseR`
  (the rule); the term is encoded like `inspect`'s. `Check`'s new local
  condition is structural, like `If`'s and `Inspect`'s.
- `out-prim-form` rebuilds `out` on `caseR` with the same type and the same
  views; `parse-prim-form` rebuilds the parser on it.

**[test]** `lcert.caser-test`: typing and usages (a child or the token used
twice is rejected), `Check` accepts `caseR` derivations and rejects a
tampered one, conversion and both evaluators compute the ι-rules, the two
`out`s agree on every tree tried, and so do the two parsers.

### 2.2 The metatheory, re-checked

Each result of R4, and of §1, with the new rule. **[paper]** unless marked.

- **Syntactic lemmas (R4 §2).** Weakening, renaming, strengthening and
  composition gain one case each, handled like `Inspect`'s. Skeletons: the
  rule's premises are simply typed at `skel X`, and each ι-step preserves
  skeletons.
- **Strict overhead (Lemma 2.7)** is a property of every derivation-shaped
  code, whatever its rule label: unchanged. **[Ansatz]**
  `lcert.verified/strict_overhead` quantifies over the rule label.
- **Denotation and conversion (Lemmas 3.1–3.2).** The new clause is
  compositional; the ι-rules preserve denotations by the clause itself.
- **The fundamental lemma, `caseR` case**, in R4's sets and in §1's.
  The inner hypothesis gives `v = ⟦r⟧` with `‖v‖ ≤ k₁` and `k₁ + k₂ ≤ k`.
  - On a leaf, the branch's environment is `Γ₂` plus a label: footprint `k₂`.
  - On a node `node ◇ ℓ v₁ v₂`, the token lies in `V₁(◇)`, the children in
    `V_{‖v₁‖}(R)` and `V_{‖v₂‖}(R)`, and the branch's footprint is
    `k₂ + 1 + ‖v₁‖ + ‖v₂‖ = k₂ + ‖v‖ ≤ k`.

  So the inner hypothesis applies to the branch. No cap enters. **[Ansatz]**
  `lcert.supply/caser_node_env` is the node split.
- **T1, T3** follow as before.
- **T4 and T4′.** The evaluator's `caseR` takes one node apart: it
  terminates, and since the runtime tree equals the carrier tree, both sides
  take the same branch with related bindings. Erasure does not touch `caseR`
  (no usage-0 position).
- **P5.** *Step 1* (PA into λᶜᵉʳᵗ): translated PA proofs use no `caseR`, and
  the extended `Check` accepts every derivation the old one did (its rule
  table only grows); the PA-translation tests pass unchanged. *Step 2*
  (λᶜᵉʳᵗ into E-PA^ω): `caseR` becomes definition by cases on tree codes,
  primitive recursive; its ι-rules are provable equations (Lemma 6.2.3), and
  Lemma 6.4's new case is the fundamental lemma's, in E-PA^ω.
- **What changes in principle:** nothing new is inhabited that `out` did not
  already give. `caseR` changes costs, not strength. But it does change the
  proof system: `Check` now accepts derivations that use the `CaseR` rule,
  and a checker without the extension rejects them (**[test]**
  `the-extension-is-a-change-of-proof-system`). Step 4 uses exactly this.

### 2.3 Lazy finite supplies

**Definition.** A *lazy finite supply* is a pair `(N, σ)` of a natural number
`N`, its *promise*, and a name `σ`. It denotes one finite certificate, the
*spine of N*:

```
spine_σ(N) = node(τ_σ,0, a, leaf a, node(τ_σ,1, a, leaf a, … node(τ_σ,N−1, a, leaf a, leaf a)))
```

whose i-th token `τ_σ,i` belongs to `σ` alone. The evaluator represents the
not-yet-taken suffix from index `i` as a thunk carrying `N − i`.

- **Materialization.** Destructing a thunk yields `node(τ_σ,i, a, leaf a,
  thunk(i+1))`, or `leaf a` at the end. Every operation that looks inside a
  certificate destructs as it goes; `nodes` alone answers `N − i` from the
  promise without materializing. `reflect` materializes a certificate within
  its cap before checking it.
- **The guard.** No run may materialize a node at index `L` or beyond of any
  supply (`*materialize-limit*`, 10⁷ by default). It is not part of the
  semantics: without it a walk over a 10^100 supply would never return.

**Observational equality.** For every program `p` and every run, `p` on the
lazy supply `(N, σ)` returns the value `p` returns on the tree
`spine_σ(N)` — up to how much of the leftover supply has been materialized —
provided the run does not reach the guard.

*Proof.* The only operations that look into a certificate are `caseR`,
`itR`, `print`, `inspect`, `reflect` and the evaluator's linearity check.
Each destructs top-down, and destructing a thunk yields exactly the eager
tree's node at that position, token included; `nodes` of a thunk equals the
eager subtree's node count. The linearity check skips thunks; their tokens
are fresh by construction, so no duplicate can involve them unless the
thunk itself is duplicated, which affine typing excludes. ∎ **[paper]**;
**[test]** `programs-cannot-tell-lazy-from-eager` (a count by `itR`, `print`,
a two-level `caseR`, and the parser; sizes 0, 1, 2, 17 and 9).

**The materialization bound.** Minting a well-formed code `c` from the spine
of `N ≥ nodes(c)` with the parser on `caseR` returns `c` itself and the spine
of `N − nodes(c)`; so it materializes exactly `nodes(c)` supply nodes.
**[Ansatz]** `lcert.supply/parse_spine`, over a CT model of the parser whose
compiled function agrees with lcert's program on 40 random codes
(`the-compiled-parser-agrees-with-lcert's`); `spine_nodes` shows the promise
is the real size. **[test]** `destructing-materializes-only-what-it-takes`:
a 4-node code minted from a supply promised at 10^100 materializes 4 nodes.

**Soundness with a lazy supply.** The model sees `spine_σ(N)`, a finite tree.
A run on it is a run in `Θ_N`-worth of world, covered by T1 and Theorem 1.4
at cap `n ≥ N`, for every finite `N`, 10^100 included: nothing in the
metatheory depends on the size of `N`, and the implementation's caps are
arbitrary-precision.

**Why "finite" is essential.**
- An endless supply is not an element of `C(R)`, which holds finite trees, so
  the model does not interpret it, and T1 says nothing about a run on it.
- The charged cap `n − ‖v‖ + m` needs a finite `n`; its strict descent is
  what makes the model's recursion well founded.
- T4 fails: `itR` over an endless supply does not terminate.
- A chain that keeps finding its own certificate in an endless supply
  delegates for ever and never acts: safe, but never done. With a finite
  promise it acts after at most `N / min ‖v‖` delegations.

  A promise of 10^100 costs nothing until it is spent, so finiteness is no
  practical limit.

### 2.4 Minting on a supply promised at 10^100

`MintAgent = Π(cs :ω Syn). Π(s :₁ R). Σ(b :ω Nat). T(b ≠ 0)`, a first-order
type (§1.2). The agent is handed a list of codes and a raw supply. It mints
the first code into a certificate from the supply, checks it at `MintAgent`,
reflects it under the charged cap, and runs it on the rest of the list and
the rest of the supply. On an empty list, or a code that does not check, it
takes the known-safe action 1. Its certificate has 18,831 nodes and declares
no tokens; `Check` accepts it in about 0.1 s.

**[test]** `lcert.minting-agent-test`, on a lazy supply promised at 10^100,
with the chain *agent, agent, leaf agent* (the leaf takes action 2):
- the result is `(2, ⋆)`, after three reflections;
- exactly 37,919 = 2 × 18,831 + 257 supply nodes are materialized: the
  certificates minted, and nothing else;
- the charged caps run 10^100 → 10^100 − 18,831 → 10^100 − 37,662 →
  10^100 − 37,919;
- the same agent built on the definable `out` walks the whole supply at its
  first step, and the guard stops it.

The run takes a few seconds, dominated by minting and checking the three
certificates.

---

## 3. The higher-order case, settled (Step 3)

**Result.** Under the caller-charged cap, `reflect` is sound at **every**
closed type that contains no `reflect`, higher-order resource types
included. R4's semantic sets cannot show this; a world-indexed Kripke model
does. The three higher-order shapes are sound, and so is every other closed
`reflect`-free type.

### 3.1 Why R4's sets fail, and why the failure is harmless

Take shape (c), `A = 1 ⊸ Σ(f :ω H). 1` with `H = R ⊸ Σ(b :ω Act). Safe(b)`:
a program that hands out a reusable certificate consumer. Let a certificate
of `‖v‖ = 1` node declaring `m = 0` tokens be reflected at `A` by a caller of
footprint `k = 1` at cap `n = 1`, so the charged cap is `n′ = 1 − 1 + 0 = 0`.
The decoded program runs at cap 0, where its consumer's own `reflect`
refuses every certificate with nodes and returns the default. Its value `g`
is therefore right on certificates with no nodes and wrong on the rest.

- `g ∈ V⁰₀(A)`: at cap 0 the consumer is only asked about certificates with
  no nodes. **[Ansatz]** `lcert.kripke/fixed_accepts_c`.
- `g ∉ V¹₁(A)`: at cap 1 a reusable (`ω`) consumer, of footprint 0, must be
  right on every certificate of up to 1 node. **[Ansatz]** `fixed_rejects_c`.

So Q of §1.3 fails at shape (c), although the gap condition
`n − n′ ≤ k − m` holds: no Reflect case can be proved at this type in R4's
sets, with the charged cap or with R4's own cap `m` (which gives the same
`g` here). Running the program at the caller's own cap `n` would put its
value in `V¹₁`, but then the model's recursion does not descend, so it
defines nothing.

The failure is harmless. A certificate of 1 node could only reach the
consumer after the reflection; but the reflection burnt the caller's only
token, so no such certificate can exist then. **[test]**
`the-model-value-fails-only-on-unholdable-inputs` shows the same thing on a
real run: the consumer is wrong exactly on certificates bigger than the
charged cap, which no run can hold after the reflection. R4's sets judge
every value against every supply that fits the cap *ever*; what matters is
the supplies that can still exist *when the value is used*.

### 3.2 The world model

**Worlds.** A *world* `w` bounds the number of tokens alive in a run: those
of the value in question, of its caller, and of everything else the program
holds. Tokens are never created. `reflect` burns a certificate's `‖v‖`
tokens and hands its program `m < ‖v‖` fresh ones, so it lowers the world
by `‖v‖ − m ≥ 1`; nothing raises it. Later worlds are smaller.

**The sets.** `V_w^k(A)η ⊆ C(skel A)`, for `k ≤ w ≤ n`, reads: values of `A`
accounting for `k` of the `w` live tokens. The cap `n` enters only through
`T(b)`, via `⟦b⟧ⁿη`.

```
V_w^k(0) = ∅    V_w^k(1), V_w^k(Bool), …, V_w^k(Syn) as in R4
V_w^k(◇) = {◇} if k ≥ 1, else ∅          V_w^k(R) = { v : ‖v‖ ≤ k }
V_w^k(T(b))η = {⋆} if ⟦b⟧ⁿη = tt, else ∅
V_w^k(Π(x :₁ A). B)η = { f : ∀w′ ≤ w. ∀j. k + j ≤ w′ → ∀a ∈ V_{w′}^j(A)η.
                              ∃b ≤ k + j. f(a) ∈ V_{w′−b}^{k+j−b}(B)(η, x↦a) }
V_w^k(Π(x :ω A). B)η = { f : ∀w′ ≤ w. k ≤ w′ → ∀a ∈ V_{w′}^0(A)η.
                              ∃b ≤ k. f(a) ∈ V_{w′−b}^{k−b}(B)(η, x↦a) }
V_w^k(Π(x :₀ A). B)η = { f : ∀w′ ≤ w. k ≤ w′ → ∀a ∈ C(skel A).
                              ∃b ≤ k. f(a) ∈ V_{w′−b}^{k−b}(B)(η, x↦a) }
V_w^k(Σ(x :₁ A). B)η = { (a, c) : ∃j ≤ k. a ∈ V_w^j(A)η, c ∈ V_w^{k−j}(B)(η, x↦a) }
V_w^k(Σ(x :ω A). B)η = { (a, c) : a ∈ V_w^0(A)η, c ∈ V_w^k(B)(η, x↦a) }
V_w^k(Σ(x :₀ A). B)η = { (a, c) : a ∈ C(skel A), c ∈ V_w^k(B)(η, x↦a) }
```

Two features do the work.
- **A function is judged at every later world.** A consumer created in the
  small world after a reflection needs to be right only on the certificates
  that can exist there, and it stays right as the world shrinks further.
- **A result may live in a smaller world than its call began in,** by the
  *burn* `b` of tokens the call destroys. A call that reflects burns
  certificate tokens, and its result is judged in the world after that.

`η ⊨_w^k Γ` is R4's §3.4 with sets at world `w`: the usage-1 entries account
for footprints summing to at most `k ≤ w`, the usage-ω ones for footprint 0.

**[Ansatz]** `lcert.kripke/VW` embeds these sets for the non-dependent
fragment of `lcert.charged` (`Π₁`, `Π_ω`, `⊗`, `Σ_ω`, `R`, `◇`, data, and
the agent's result).

### 3.3 Lemmas

**Lemma 3.1 (monotonicity).**
1. `V_w^k(A) ⊆ V_{w′}^k(A)` for `k ≤ w′ ≤ w`: a function clause quantifies
   over all worlds below `w`, which include all worlds below `w′`.
2. `V_w^k(A) ⊆ V_w^{k′}(A)` for `k ≤ k′ ≤ w`: keep each burn; the targets'
   footprints grow.
3. Hence `η ⊨_w^k Γ` implies `η ⊨_{w′}^k Γ` for `k ≤ w′ ≤ w`.

**[Ansatz]** `vw_world_mono`, `vw_foot_mono`, for every embedded type.

**Lemma 3.2 (the cap does not matter for closed reflect-free types).** If
`A` is closed and contains no `reflect`, its sets are the same at every cap
`n`: the only clause that consults `n` is `T(b)`'s, through `⟦b⟧ⁿ`, and the
denotation of a `reflect`-free term never consults the cap. Closures in
environments do not change this: applying one never reads the caller's cap.
**[paper]**

### 3.4 The fundamental lemma

> **Theorem 3.3.** Let the Reflect rule allow every closed type without
> `reflect`, and let `reflect` denote at the charged cap (§1.1). If
> `Γ ⊢ t :¹ A` is derivable and `η ⊨_w^k Γ` with `w ≤ n`, then some burn
> `b ≤ k` has `⟦t⟧ⁿη ∈ V_{w−b}^{k−b}(A)η`.

*Proof.* By strong induction on `n`, then on the derivation, for all `w ≤ n`
at once. Contexts split as in R4 Lemma 3.5; `k₁ + k₂ ≤ k ≤ w`.

- **Var, constants, `Leaf`, `Lbl`, …** Burn 0, by footprint monotonicity.
- **Lam.** `⟦λx. t⟧ⁿη = a ↦ ⟦t⟧ⁿ(η, x ↦ a)`, burn 0. For `w′ ≤ w`, `j` with
  `k + j ≤ w′` and `a ∈ V_{w′}^j(A)`, the environment `(η, x ↦ a)` satisfies
  the extended context at world `w′` and footprint `k + j` (Lemma 3.1.3),
  and the inner hypothesis *at world `w′`* gives the clause. Usages ω and 0
  alike. This is why the lemma is stated for every world at once.
- **App.** `f` burns `b₁` and lands in world `w₁ = w − b₁` with footprint
  `k₁ − b₁`; `u`, evaluated next, burns `b₂` and lands in `w₂ = w₁ − b₂`;
  `k₁ − b₁ + k₂ − b₂ ≤ w₂`, so `f`'s clause applies at the later world `w₂`
  and burns `b₃`; the total `b₁ + b₂ + b₃ ≤ k`, and footprint monotonicity
  finishes. At usage ω or 0 the argument's context has footprint 0, so it
  burns nothing. **[Ansatz]** `app_case_world`.
- **Pair, Let.** The same bookkeeping: the first component moves to the
  later world by Lemma 3.1.1, and the split of the footprint is kept.
  **[Ansatz]** `pair_case_world` (tensor introduction).
- **If, ElimBool, CaseLbl, Inspect, caseR.** The scrutinee burns `b₁`; the
  branch runs in world `w − b₁` with the scrutinee's parts added to its
  environment (for `caseR`, token and children: §2.2).
- **RecN, RecSyn, ItR.** By induction on the scrutinee's value; each step is
  an application of an ω-scaled method, whose context has footprint 0, to
  the recursive results, so it is the App case again, from the world the
  previous step left.
- **SNode, Chk, Print, Succ, Node.** Data; `Node` adds its token's
  footprint 1.
- **Abort.** `V(0) = ∅`: vacuous.
- **Conv.** R4 Lemma 3.3, at the fixed cap `n`.
- **H₁.** As in R4: the two certificates' programs compose into a
  refutation at budget `m₁ + m₂ < ‖v₁‖ + ‖v₂‖ ≤ n`, and the **outer**
  hypothesis at that cap, world and footprint gives an element of `∅`.
- **Reflect at a closed `reflect`-free `A`.** `r` burns `b₁` and leaves `v`
  with `‖v‖ ≤ k₁ − b₁`; `e` burns `b₂`; the check succeeds, so `print v`
  encodes `Θₘ ⊢ t′ :¹ A` with `m < ‖v‖` (strict overhead). The reflection
  burns `‖v‖` and gives `m`, leaving the world
  `w₃ = w − (b₁ + b₂ + ‖v‖ − m)`. Then
  - `w₃ ≤ n − ‖v‖ + m = n′ < n`, and `m ≤ w₃`: the program's tokens fit;
  - the **outer** hypothesis at cap `n′`, world `w₃`, footprint `m` gives a
    burn `b₄ ≤ m` with `⟦t′⟧ⁿ′ ∈ V_{w₃−b₄}^{m−b₄}(A)`; by Lemma 3.2 these
    are the same sets at cap `n`;
  - with `b = b₁ + b₂ + (‖v‖ − m) + b₄ ≤ k`, the caller's world `w − b` is
    exactly `w₃ − b₄`, and `m − b₄ ≤ k − b`, so footprint monotonicity puts
    the value in `V_{w−b}^{k−b}(A)`. ∎

  No property of `A` is used except Lemma 3.2 and footprint monotonicity,
  which every type has. **[Ansatz]** `reflect_case_world`: the case for
  every embedded type, with no class hypothesis, from an abstract
  denotation, derivability predicate and outer hypothesis, with the cap
  descent from the verified strict-overhead lemma.

**Shape (c) again.** The Reflect case judges the program's value in the
world `w₃ = 0` the reflection leaves, and there it holds. **[Ansatz]**
`world_accepts_c`, and `shape_c_world_ok`: the conclusion of the Reflect
case for the scenario of §3.1.

### 3.5 Consequences

- **T1 and T3** follow as before: with `Θₙ ⊢ t :¹ 0`, take `w = k = n`; some
  burn puts `⟦t⟧ⁿ` in `V(0) = ∅`. Corollary 3.7 holds for the extended
  `Check`. **[paper]**
- **T4 and T4′.** Unchanged in form (§1.5); the evaluator must run a
  closure at the cap it was created under, which it does. **[test]**
  `closures-keep-their-creation-cap`: in shape (c) the consumer reflects at
  the charged cap it closed over, one reflection deep, although the driver
  that calls it runs at the full cap.
- **P5.** §1.5's fixed-witness interpretation never evaluates a
  certificate, so it needs no class restriction; the bounded facts it uses
  are true by T1 above. **[paper]**
- **The three shapes run certified.** **[test]**
  `the-shapes-return-certified-results`: (1, ⋆) for (a), and 3 for (b) and
  (c), under the `:all` policy with the charged cap.

**The calculus the four steps propose,** λᶜᵉʳᵗ₁: λᶜᵉʳᵗ₀ with `caseR`, with
`reflect` at every closed type that contains no `reflect`, and with the
caller-charged cap as the meaning of `reflect`. In the implementation:
`*extensions*` `#{:caseR}`, `*reflect-class*` `:all`, `*charged-cap*`
`true`.

**What stays excluded.** A type whose `T`-arguments contain `reflect`: its
meaning depends on the cap (Lemma 3.2 fails for it). An agent therefore
states its safety as evidence it returns, not as a proposition about its own
run. Soundness of more `reflect` instances does not add quantified
self-trust: no budget derives `Con′` (P5).

---

## 4. Tiling across a changed proof system (Step 4)

### 4.1 Set-up

A *proof system* here is a set of rule extensions (`*extensions*`) with a
reflect class (`*reflect-class*`). `chk′` names the `Check` of the system a
program runs in, so a parent running in system `S` judges every successor's
certificate by `S`'s rules, whoever produced it.

The protocol is the delegation of §1: the parent reads a certificate from
the front of its supply, checks it at its own type `Agent`, reflects it
under the charged cap and runs it on the rest; otherwise it takes the safe
action 1. A successor further down acts 2 (or another certified value).

| System | Rules |
| --- | --- |
| `P0` | λᶜᵉʳᵗ₀ with the first-order charged reflect rule (§1); no `caseR` |
| `P1` | `P0` + `caseR` (§2) |
| `λᶜᵉʳᵗ₁` | `P1` with reflect at every closed `reflect`-free type (§3) |
| `P-con` | `λᶜᵉʳᵗ₁` + an axiom `ax-con : Con′_ω` |
| `P-bot` | `λᶜᵉʳᵗ₁` + an axiom `ax-bot : 0` |

`Con′_ω = Π(c :ω Syn). T(chk′ c c⊥) → 0` says that no code checks as a
refutation. As an axiom of `P-con` it is sound: every set
`V(T(chk′ c c⊥))` is empty by Corollary 3.7, so any function inhabits
`V(Con′_ω)`, and T1 survives. It is not derivable in `λᶜᵉʳᵗ₁` at any
budget (P5). `ax-bot` is unsound.

### 4.2 Results

**[test]** `lcert.changed-system-test`, one case per row. Sizes are
certificate nodes, measured.

| Child's system, relative to the parent's | The parent's `Check` on the child's certificate | Translation into the parent's rules | Cost | Trust |
| --- | --- | --- | --- | --- |
| identical (`P0` → `P0`) | accepts | none needed | the certificate: 21,135 | **holds** (action 2) |
| weaker (`P0` child, `P1` parent) | accepts: `P1`'s rules include `P0`'s | none needed | the certificate | **holds** (action 2) |
| re-encoded (two rule labels swapped) | rejects | relabelling, a free code map; the parent re-checks its output, so the relabeller need not be trusted | none: same size | **holds after translation** |
| definitional extension (`P1` child uses `caseR`; `P0` parent) | rejects; safe action 1 | replace `out` on `caseR` by the definable `out` and re-certify; re-checked by the parent | 9,181 → 21,135 nodes (×2.30); a minting agent 18,831 → 37,389 (×1.99); and the translated agent's destructor walks its supply, so it cannot use a lazy one (§2.4) | **holds after translation, at a cost** |
| stronger reflection (`λᶜᵉʳᵗ₁` child reflects at shape (c); `P1` parent) | rejects; safe action 1 | none known: a translation would derive the stronger reflection inside the weaker system | — | **stops**; a `λᶜᵉʳᵗ₁` parent trusts it (certified action 5) |
| true, unprovable axiom (`P-con` child; `λᶜᵉʳᵗ₁` parent) | rejects; safe action 1 | none: `Con′_ω` has a 10-node certificate in `P-con` and none at any budget in `λᶜᵉʳᵗ₁` (P5) | — | **stops**; a `P-con` parent trusts it (action 3) |
| unsound axiom (`P-bot` child; `λᶜᵉʳᵗ₁` parent) | rejects; safe action 1 | none, and none may exist: `P-bot` has a 3-node refutation certificate | — | **stops, correctly**; a `P-bot` parent "certifies" action 0, with evidence of `T(0 ≠ 0)` |

### 4.3 Where trust stops, exactly

**A parent trusts a successor's certificate if and only if it is a valid
derivation in the parent's own rules.** Trust in the rules themselves is the
parent's metatheory (T1 for its system), established once, from outside. So:

1. **A successor in the same or a weaker system** is trusted outright,
   paying for its certificate in tokens.
2. **A successor whose system adds only derivable rules** — macros, derived
   rules, definitional extensions, a re-encoding — is trusted once its
   certificate is translated into the parent's rules. The parent re-checks
   the translated certificate with its own `Check`, so no translator needs
   to be trusted. The price is the size of the translated certificate, and
   whatever the derived rule was for: translated, `caseR` becomes a walk
   over the supply again, and laziness is lost.
3. **A successor whose system adds rules the parent cannot derive** — a new
   axiom, even a true one such as the calculus's own consistency, or a
   stronger reflection principle, even a sound one — is not trusted. No
   translation exists (for `Con′_ω` by P5). Nor can the parent prove the
   stronger system consistent: every derivation of the parent's is one of the
   stronger system's, so a proof that the stronger system has no refutation
   would give the parent its own `Con′`, which P5 forbids.
   - It can adopt the stronger system, but only on grounds from outside, as
     §3 justified `:all`. After adopting `λᶜᵉʳᵗ₁ + Con′`, it faces the same
     question about `λᶜᵉʳᵗ₁ + Con′ + Con(λᶜᵉʳᵗ₁ + Con′)`, and so on: Yudkowsky
     and Herreshoff's tower of strengthening theories reappears, for
     successors that improve their proof system.
4. **An unsound successor** is rejected, as it must be. A parent that
   adopted its rules would certify an unsafe action; the metatheory, not the
   runtime, is what rules that out.

**The token budget does not move this boundary.** Every row of the table
behaves the same with a budget of 10^100 as with the few thousand tokens
used here: the budget pays for certificates the parent can check, and a
certificate the parent cannot check is refused however many tokens come
with it.
