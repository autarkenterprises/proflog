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
