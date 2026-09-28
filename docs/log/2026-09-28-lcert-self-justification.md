# Is λᶜᵉʳᵗ Self-Justifying, and Is Its Resource Discipline the Reason? (2026-09-28)

This note answers a question put after
[ADR-0143](../adr/ADR-0143-lcert-tiling-four-steps.md) was completed. It
builds on the
[assessment note](2026-09-26-sjas-lobian-obstacle-assessment.md) and on
[METATHEORY.md](../../research/lcert-tiling/METATHEORY.md). Working through
it exposed an error in ADR-0143's Step 4, which §6 records and corrects.

## The question

> Let us consider and restatement the basic motivation for lambda-cert, which
> is the claim that self-justification results from a certain usage
> constraint (or discipline) with regards to proof resources, such that the
> resource consumption demanded by G2 is precluded. Is this accurate or not?
> Is lambda-cert actually self-justifying (or an analogue thereof)? Convince
> yourself as to the correctness, or not, of this line of thinking, then
> report your reasoning.

**Sources.** R4 is `nachlass/refinement/R4-metatheory.md` in `jpt4/sjas`
(branch `sjas-codification`, commit `f5a8490`). "sjas LOG" is that commit's
`nachlass/LOG.md`. Willard's papers are cited from the corpus text in
`nachlass/codification/sources-text/`.

## Summary

- **Right about consistency, wrong about G2.** Stated precisely, the
  motivation correctly explains why λᶜᵉʳᵗ's self-trust is consistent. It is
  wrong to say that G2 is precluded.
- **The discipline is decisive for reflection.**
  - Make proofs reusable, as free codes or as certificates usable many
    times, and reflection at a Gödel sentence yields a closed term of `0` in
    three lines. The argument is Willard 2001 Theorem 7.2's, and it needs
    neither D2 nor D3.
  - With affine, token-built certificates, the same argument stops where it
    must use its proof twice, and T1 says no repair exists.
  - For bare consistency (`H` at `0`), the discipline is sufficient.
    Whether it is necessary is open.
- **G2 is not precluded; it is confined, and its cost is metered.**
  - P5 keeps the code-level consistency statement unprovable at every
    budget.
  - In P5's proof, `H` used at budget `n` is exactly a bounded consistency
    fact that PA proves by computation. So λᶜᵉʳᵗ proves no arithmetic
    sentence PA does not. Here "arithmetic" means a type that means the
    same at every budget; `H°` is not one (Addendum A1).
  - The cost that G2 attaches to consistency proofs is paid per use, in the
    tokens of the proof being trusted.
- **Self-justifying in Willard's sense, and as legalistic as Willard's
  systems.**
  - Both clauses of `Willard2016` Definition 3.4 hold, for the certificate
    representation.
  - The consistency constant is assumed, not derived, and `H°` is internally
    strictly weaker than code consistency.
  - In form it goes beyond Willard. Its arithmetic is PA-strength, which the
    boundary results rule out for Willard's kind of self-justification. And
    λᶜᵉʳᵗ₁ trusts itself by reflection at every closed reflect-free type;
    Willard 2001 Theorem 7.2 forbids the numeric-code form of that.
  - The best description is **metered finitistic self-trust**.

## 1. The motivation, restated

The programme's "resource form" of self-justification (sjas LOG,
2026-09-24/25) has four parts:
- **Witness (W).** A refutation of the self-consistency principle yields a
  witness.
- **Mass (M).** The witness has a mass.
- **Strict overhead.** Reflecting a witness yields a program that may spend
  strictly less than the witness cost.
- **Descent.** A least counterexample descends to a smaller one, so none
  exists.

In λᶜᵉʳᵗ these are concrete:
- The witness is a **certificate**, a derivation held as a value of type `R`.
- Each node of a certificate consumes one affine token `◇`, so a certificate
  of `n` nodes costs `n` tokens.
- The constant `H = reflect₀` consumes a certificate at usage 1.
- Strict overhead is R4's Lemma 2.7, `budget(c) < nodes(c)`. It follows from
  the encoding's rules E2 and E3: a root judgment writes out its context of
  tokens.
- T1, consistency, is proved by strong induction on the token budget.

In one sentence:

> λᶜᵉʳᵗ may contain its own soundness principle, by name, because the
> principle applies only to proofs held as affine certificates, each node of
> which costs a token, and a certificate always costs more than the program it
> certifies may spend. The diagonal arguments that make such a principle
> inconsistent must reuse or conjure a proof, and so cannot pay.

§§2–3 test this sentence, and §4 corrects its G2 clause.

## 2. What the discipline blocks: the derivability conditions

Write `□A := Σ(r :₁ R). T(chk′ (print r) ⌜A⌝)`, the certificate box. G2's
standard proof, and Löb's, use the derivability conditions for the box, and
the diagonal argument below also uses contraction. R4 §4 settles all of them.

| Condition | With one budget for every `A` | Per instance | Where |
| --- | --- | --- | --- |
| D1 as a closed term (`⊢ A` gives a closed `□A`) | **no**: no closed runtime term builds a non-leaf certificate | with `‖v‖` tokens | Props 4.1, 4.2 |
| D3, `□A ⊸ □□A` | **no** | with `μ(□A) > 2μ(A)` tokens | Props 4.3–4.5 |
| boxed contraction, `□A ⊸ □A ⊗ □A` | **no** | with `2μ(A)` tokens | Props 4.4, 4.5 |
| D2, `□(A ⊸ B) ⊗ □A ⊸ □B` | conjectured **no** | with `μ(B)` tokens | Conj. 4.6 |

The first three fail for a resource reason: T3, size soundness, bounds a
program's output by the tokens it is given. D2's expected failure is not
about resources. It is the *evidence obstacle*: `chk′` reduces only on closed
canonical codes, so a term cannot show that a certificate built from variables
checks.

**Measured quotation cost.** R4 proves only `‖w‖ > 2μ(A)` for a certificate
`w` of `□A`. The implementation's encoding costs much more. Take
`A_j := 1 ⊸ ⋯ ⊸ 1` (`j` arrows), certify its proof `v`, and certify
Proposition 4.2's `(lit_v, ⋆) : □A_j` at budget `‖v‖`:

| `j` | `‖v‖` | `‖w‖` (certificate of `□A_j`) | `‖w‖ / ‖v‖` | `‖w‖ / ‖v‖²` |
| --- | --- | --- | --- | --- |
| 0 | 3 | 897 | 299.0 | 99.7 |
| 1 | 13 | 8,231 | 633.2 | 48.7 |
| 2 | 27 | 34,226 | 1,267.6 | 46.9 |
| 3 | 45 | 103,521 | 2,300.5 | 51.1 |

So one step of D3 costs roughly `50‖v‖²` tokens for `j ≥ 1`. Quadratic
growth is what E3 suggests: the term that rebuilds `v` mentions `‖v‖` tokens,
and each node of its derivation records its context. That cause is not
verified. Reproduce with `research/lcert-tiling/bin/deps` and, from
`research/lcert-tiling/lcert`,
`java -cp "$(../bin/classpath)" clojure.main quote_cost.clj`, where
`quote_cost.clj` is:

```clojure
(require '[lcert.core :as lc])
(defn arrows [j] (if (zero? j) 'Unit (list '-o 'Unit (arrows (dec j)))))
(defn prog [j] (reduce (fn [body i] (list 'fn [(symbol (str "x" i)) 1 'Unit] body))
                       'star (range j 0 -1)))
(defn box [A] (list 'Sigma '[r 1 R] (list 'T (list 'chk '(print r) (list 'code A)))))
(doseq [j [0 1 2 3]]
  (let [A (arrows j)
        v (lc/certify 0 (prog j))
        w (lc/certify (:nodes v) (list 'pair (box A) (lc/certificate-form (:code v)) 'star))]
    (println j (:nodes v) (:nodes w) (:budget w))))
```

## 3. The decisive comparison: one reflection rule, with and without the discipline

### 3.1 Without it, reflection at a Gödel sentence is inconsistent **[paper]**

Let the **free variant** replace `reflect`'s certificate by a reusable code:

    reflectω_A : Π(c :ω Syn). T(chk′ c ⌜A⌝) → A

It is available at every closed reflect-free `A`, and `chk′` checks the free
variant's own rules, by name.

**The diagonal.**
1. `recSyn` defines `diag : Syn → Syn`. It replaces the free variable of a
   code `y` by the code of the literal `y`.
2. Let `D := Π(c :ω Syn). T(chk′ c (diag x)) → 0`, a type with free
   variable `x : Syn`, and let `d := ⌜D⌝`.
3. Put `L := D[x := d]`. Then `diag d ⇝ ⌜L⌝` by closed computation, so

       L ≡ Π(c :ω Syn). T(chk′ c ⌜L⌝) → 0

   "no code proves `L`". `L` has no `R`, `◇` or `reflect`, so it is an
   O-type, in every reflect class of METATHEORY §1.2.
4. `q := λ(c :ω Syn). λ(e :ω T(chk′ c ⌜L⌝)). reflectω_L c e c e` has type
   `L`, and it uses `c` and `e` twice each. (With usage-1 evidence, first
   duplicate `e` by dependent elimination on the Boolean `chk′ c ⌜L⌝`.)
5. `⌜q⌝` is a closed code that checks at `⌜L⌝`: checking `q` never
   evaluates `chk′` on a closed code. So `chk′ ⌜q⌝ ⌜L⌝ ⇝δ tt`, and
   `q ⌜q⌝ ⋆ : 0`.

**What it uses:** the diagonal, δ on one closed code, and reuse of the
proof `c`. It uses no D2, no D3, and no transparency of the checker.

**It is Willard's argument.** It is Willard 2001 Theorem 7.2's proof,
transcribed:
- (66)–(67) define `Θ ⇔ ∀y ¬Prf(⌜Θ⌝, y)`;
- (68): reflection gives `α ⊢ Θ`;
- (69): Δ₀-completeness gives `α ⊢ Prf(⌜Θ⌝, m)`;
- (70): hence `α ⊢ ¬Θ`.

Willard notes that the argument assumes neither addition nor multiplication
total. Step (68) is where the hypothesis `□Θ` is used twice: from
`□Θ → (□Θ → ⊥)` to `□Θ → ⊥`.

**Mass without affinity fails the same way.** Suppose certificates cost
tokens but may be used many times once built (`r :ω R`). Then the same `q`
works: mint `q`'s certificate once at the top level, and apply `q` to it.

**At R4's original class** (data types, plus `0` through `H`), there is no
Gödel sentence to reflect at.
- Free reflection at a data type breaks certified evaluation, not
  consistency. Kleene's recursion theorem gives a certified program that
  must return its own successor, so evaluation diverges and T4 fails. A model
  can still read data reflection by defaults, as P5's proof does.
- Free reflection at `0` is `P-con`'s axiom (§3.4).

### 3.2 With it, the same argument stops at the reuse

In λᶜᵉʳᵗ₁ the rule consumes a certificate:
`reflect_A : Π(r :₁ R). T(chk′ (print r) ⌜A⌝) ⊸ A`. Take
`L_R := Π(r :₁ R). T(chk′ (print r) ⌜L_R⌝) ⊸ 0`, which is in 𝓕.

The transcribed `q` must use `r` twice, once to reflect and once as the
argument of the reflected `L_R`. The usage checker rejects it. Every repair
runs into the discipline:
- **Copying** `r` costs tokens in proportion to its size (Prop 4.4(3)).
- **Granting tokens** in `L_R`'s type does not help. The reflected `L_R`
  demands the same grant again, after the copy has spent part of it. So the
  loop is short by at least one certificate on every turn.
- **Using a free code** instead needs a parse into a certificate. That
  costs tokens, and its correctness is not available inside the calculus
  (R4 §4.7).

T1 for λᶜᵉʳᵗ₁ says no repair exists: no budget derives `0`. Its evidence is
METATHEORY §3. The Reflect case is kernel-checked over non-dependent types,
and dependent types are on paper. In Willard's terms, step (68) cannot be
taken, because `□Θ` is a resource.

### 3.3 What the comparison shows

For reflection at any class that contains O-types, such as Π₁ and Gödel
sentences, **affinity of the reflected proof is necessary**, and the
discipline as a whole is sufficient. Each ingredient does a separate job:

| Ingredient | What it does | Evidence |
| --- | --- | --- |
| Affinity: the reflected certificate is used once | Blocks the diagonal's reuse of its proof; without it, reflection is inconsistent | §3.1 **[paper]** |
| Mass: each node costs a token, and there are no token-free certificates | Copying or quoting costs tokens in proportion to size, so affinity cannot be bypassed by a copy, even one that preserves evidence | R4 Props 4.1, 4.4, 4.5; T3 |
| Strict overhead | A certificate costs more than its program may spend, so T1's descent is strict | R4 Lemma 2.7, T1 |
| Finite budgets | T1's induction is on the budget. A lazily promised 10^100 is finite; an unbounded supply is not covered | T1; METATHEORY §2.3 |
| The opaque checker (δ only on closed codes) | Not load-bearing for T1, and no defence against §3.1's diagonal: that needs only closed-code δ, which `chk′` has, and T1's model reads `chk′` as `Check`. It blocks a uniform D2 (Conj. 4.6), and may be what keeps `P-con` consistent (§3.4) | this note **[paper]** |

Two things are not settled:
- Whether affinity **without** mass would suffice. It would then lean on the
  evidence obstacle alone, since an evidence-preserving copy would reopen
  the diagonal.
- Bare consistency, below.

### 3.4 Bare consistency: sufficient, necessity open

Remove the discipline from `H`, reflection at `0`, and it becomes
`Con′_ω = Π(c :ω Syn). T(chk′ c c⊥) → 0`, by name. That is exactly the axiom
`ax-con` of ADR-0143's `P-con`, which is Willard's "I am consistent" axiom
(`Willard2016` Example 3.5, ⊕) over codes.
- Theorem 7.2's diagonal does not apply: `ax-con` reflects only at `0`.
- The Gödel–Löb argument against a consistency axiom needs D2 and D3 for
  `chk′` on open codes, which the opaque checker does not supply.
- A model needs `P-con`'s own consistency, which is circular (§6).

So whether `P-con` is consistent is open. If it is, bare self-consistency
needs only the opaque checker, a Feferman-style nonstandard predicate. The
discipline would then be what makes *reflection*, and certified evaluation,
possible, rather than what makes consistency possible. Reflection is what
tiling uses (AAR-0143).

## 4. Where the motivation needs correcting

### 4.1 G2 is confined, not precluded

G2 is a theorem about every consistent, recursively axiomatized theory that
interprets enough arithmetic, applied to its standard consistency statement.
λᶜᵉʳᵗ interprets PA (R4 §6.2), and P5 confirms G2 for it: `Con′_ω` is
underivable at every budget.

What the discipline changes is the certificate box. It fails the derivability
conditions uniformly (§2), so G2 does not reach `H°`, the box's consistency
statement. This is the intensional escape Feferman identified in 1960, taken
on principle rather than by a contrived predicate. The box
- is how λᶜᵉʳᵗ programs actually hold proofs, and
- fails the conditions for a reason, resources, rather than by design.

So "the resource consumption demanded by G2" is demanded of the box and
refused there. The code box pays nothing, and gets nothing.

### 4.2 The cost is relocated, not avoided

R4's proof of P5 (Lemma 6.4) fixes the budget `n` and interprets
`reflect_0 = H` by the bounded fact

    BF₀(n)  ∀v (IsSyn(v) ∧ NODES(v) ≤ n → CHECK(v, c⊥) ≠ 1)

"no code of at most `n` nodes checks as a refutation". This is finitistic
consistency. It is true because of T1, and PA proves each instance by
computation (S2).

The proof shows more than P5 states. Lemma 6.4 holds at every node of every
derivation at budget `n`. Take an O-type: one with no `R`, `◇` or `reflect`,
whose meaning is the same at every budget (R4 §3.3). A derivation of it at
any budget gives an E-PA^ω proof, and, when it is an arithmetic sentence, a
PA proof by S3. Conversely λᶜᵉʳᵗ₀ interprets PA (§6.2). **So λᶜᵉʳᵗ's
arithmetic theorems are exactly PA's; the self-trust adds no arithmetic
strength.** This is my reading of R4 §6 and METATHEORY §1.5, on paper.
`H°` is not an O-type; Addendum A1 explains why it is no exception.

Theories must pay for bounded consistency. Pudlák (1986) showed that a
theory's own proofs of its finitistic consistency statements `Con(n)` grow
at least as `n^ε`, and can be kept polynomial for sequential theories
(Addendum A3 gives the argument and its verification status). λᶜᵉʳᵗ
exhibits both ways of paying:
- **In proof size.** R4 Proposition 4.9 gives a token-free closed term for
  code consistency up to depth `k`, of size doubly exponential in `k`.
- **In held mass.** `H°` is one constant term, but a certificate of `n`
  nodes costs `n` tokens to hold.

What λᶜᵉʳᵗ adds is **uniformity**. One rule replaces a family of growing
proofs, and the index `n` is supplied by the size of the proof held. The
lazy supply of ADR-0143 makes that index arbitrary but finite.

### 4.3 Not usage alone

"Usage constraint" names affinity. The table in §3.3 shows that mass, strict
overhead and finiteness each carry part of T1. The usage discipline covers
both of `H`'s arguments, which R4 made runtime so that an erased argument
cannot hide a refutation from the descent.

## 5. Is λᶜᵉʳᵗ self-justifying?

**By definition, yes.** `Willard2016` Definition 3.4 calls `(α, d)` self
justifying when:
- (i) one of its theorems (or axioms) states that `d`, applied to `α`,
  produces a consistent set of theorems; and
- (ii) `α` is in fact consistent.

R4's T2 gives (i): `H°` is inhabited at budget 0 by `λr e. H r e`. T1 gives
(ii). That holds for natural deduction with certificates, and for λᶜᵉʳᵗ₁
through METATHEORY §1.5.

**How legalistic.** Willard himself grants that his systems verify their
consistency "only in a technically purely legalistic sense" (`Willard2016`
§8). The sjas LOG (Q6) splits that into two parts, and both transfer:
1. **Assumed, not derived.** `H` is a primitive constant, and its proof of
   `H°` is one line.
2. **Representation-relative.** Externally, `H°` is equivalent to
   consistency. Internally, `Con′ ⊸ H°` (Prop. 4.7), but no budget derives
   `H° → Con′_ω` (Prop. 4.8).

A third part is new here: **arithmetic conservativity** (§4.2).

**Beyond Willard, in form.** Neither result below contradicts Willard. At
budget `n`, `H°` is `BF₀(n)`, not a Π₁ statement about all proofs. And
λᶜᵉʳᵗ₁'s reflection is not the canonical principle, because it quantifies
over held certificates.
- **Arithmetic strength.** The corpus's boundary table (codified-sjas §8.0)
  is negative for Type-M arithmetics, where multiplication is total, under
  both Hilbert and tableaux deduction, with the proof statuses recorded
  there. λᶜᵉʳᵗ interprets PA and is self-justifying in the sense above.
- **Reflection.** Willard 2001 Theorem 7.2 denies canonical self-reflection
  for all Π₁ sentences to every consistent system with a Δ₀ proof predicate
  that proves PAX's Π₁ theorems. λᶜᵉʳᵗ₁ reflects at every closed
  reflect-free type, Gödel sentences included, and is consistent (§3.2).

**The mechanism is a firewall between two sorts.**
- **Proofs as data** (`Syn`) are free, fully arithmetical, and nameable, and
  are never reflected.
- **Proofs as evidence** (`R`) are affine and massive, and are the only
  thing reflected.
- A closed `Syn ⊸ R` map returns only leaves (Prop. 4.1). So growth in the
  arithmetic cannot leak into trusted evidence.

Willard's proofs are numbers, so he must weaken the numbers. λᶜᵉʳᵗ weakens
only the evidence.

The sjas analogies hold as analogies:
- *certificates : codes :: cut-free tableaux : Hilbert proofs*, and
- C5's "Willard's growth restriction is the Gödel-numbered shadow of an
  affine discipline".

In both systems the gap between the self-justified representation and the
standard one is a transformation the system cannot afford uniformly: cut
elimination needs superexponential growth, and parsing needs tokens. It is
not an identity, though. λᶜᵉʳᵗ's arithmetic has full growth; its restriction
lives on a separate sort. It also blocks reuse (contraction), which has no
counterpart for numbers.

**What it amounts to: metered finitistic self-trust.**
- λᶜᵉʳᵗ trusts every proof it holds, soundly, by one constant rule.
- It cannot trust proofs it does not hold. It may find or receive any proof
  freely; holding one means paying to mint it (Addendum A2).
- It never learns an arithmetic truth that PA does not know.

That is exactly what identical-system succession needs (AAR-0143). It offers
nothing toward Hilbert's second problem, and no escape from the tower that
strengthening successors face.

## 6. Correction: ADR-0143 Step 4's "true, unprovable axiom"

METATHEORY §4.1 called `P-con`'s `ax-con : Con′_ω` sound, "since every set
`V(T(chk′ c c⊥))` is empty by Corollary 3.7". That argument is circular.
- `chk′` names the running system's `Check` (METATHEORY §4.1;
  `lcert.check`), and `P-con`'s `Check` accepts `ax-con`. So `ax-con` says
  "`P-con` has no refutation".
- Corollary 3.7 for `P-con`'s `Check` *is* that statement. It would follow
  from T1 for `P-con`, which is what the argument set out to establish.
- T1's budget induction does not close either. Validating `ax-con` at budget
  `n` needs every code, of any size, to fail as a refutation.

**Status.** Whether `P-con` is consistent is open (§3.4).

**Fixed.** The wording in these places:
- METATHEORY §4.1–4.3;
- AAR-0143 (in place, and a "Revisited" section);
- `MEMORY.md`;
- the axiom's docstring in `lcert.syntax`;
- the test's name, now `a-self-referential-consistency-axiom`;
- inline correction notes in `LOG.md` and the 2026-09-26 assessment note.

The test's assertions are unchanged.

**The verdict of AAR-0143 stands:**
- A parent refuses a rule that is not its own, whatever the rule's truth.
- The refusal of a *sound* strengthening rests on the stronger-reflection
  row, whose soundness METATHEORY §3 proves.

A true axiom of the kind intended, `Con(λᶜᵉʳᵗ₁)` with the checker fixed to
`λᶜᵉʳᵗ₁`'s rules, is sound by T1. The implementation has no such checker, so
it is not tested.

## 7. Evidence levels

- **Kernel-checked** (ADR-0143, Ansatz): T1's Reflect case for λᶜᵉʳᵗ₁'s
  charged and world models, over non-dependent types.
- **Measured here:** the quotation costs of §2.
- **Tested** (ADR-0143 Step 4): refusal of `ax-con` and `ax-bot` by a
  λᶜᵉʳᵗ₁ parent.
- **Paper, R4:** T1, T2, T3, P5, and Propositions 4.1–4.9. P5 cites S1–S3.
- **Paper, this note:**
  - the free diagonal (§3.1, Willard 2001 Theorem 7.2's argument);
  - the reading of Lemma 6.4 as arithmetic conservativity (§4.2);
  - the roles of the ingredients (§3.3);
  - the circularity of §6.
- **Pudlák 1986's bounds:** the statements are corroborated by web-search
  summaries. The primary text could not be opened (egress policy). The
  proof sketches are reconstructions (Addendum A3).
- **Open:**
  - `P-con`'s consistency;
  - whether affinity without mass suffices;
  - the cause of the quadratic quotation cost.

## 8. Follow-ups

- **Machine-check §3.1.** Add a free-reflect rule extension, build `diag`
  with `recSyn`, and assert two things: `Check` accepts `q ⌜q⌝ ⋆ : 0` in
  the free variant, and λᶜᵉʳᵗ₁ rejects `q` for its usage.
- **A fixed-checker `Con(λᶜᵉʳᵗ₁)` axiom**, so that Step 4 has a tested true,
  unprovable axiom.
- **`P-con`.** Attempt a consistency proof by induction on the length of
  `Check`'s computation, or a refutation.

## Addendum: Three Follow-Up Questions (2026-09-28)

> λᶜᵉʳᵗ proves no arithmetic sentence PA doesn't. The self-trust adds no
> strength <- Is the SelfCons sentence (which lambda-cert can prove
> trivially, as an axiom, but PA cannot) not an "arithmetic" sentence?
>
> It can't trust proofs it doesn't hold <- How does it gain proofs in the
> first place?
>
> Elaborate on Pudlák's argument.

### A1. Is `H°` an arithmetic sentence?

No, and that is the point of the design.

**Which sentences are arithmetic.** A λᶜᵉʳᵗ type means the same at every
budget exactly when it contains no `R`, `◇` or `reflect`: the O-types (R4
§3.3; METATHEORY §1.2). Those are its arithmetic sentences, and §4.2's
conservativity is about them.

**`H°` is budget-indexed.** `H° = Π(r :₁ R). T(chk′ (print r) c⊥) ⊸ 0`
mentions `R`. In R4's model the usage-1 `Π` ranges only over arguments that
fit in the budget:

    Vₖ(Π(x :₁ A). B) = { f : ∀j ≤ n−k. ∀a ∈ Vⱼ(A). f(a) ∈ Vₖ₊ⱼ(B) }

So at budget `n`, `H°` means `BF₀(n)`: no code of at most `n` nodes is a
refutation. PA proves each `BF₀(n)` by computation (R4 Lemma 6.3).

**λᶜᵉʳᵗ cannot state the all-budgets reading.** "At every budget" would be
`Con_λ`. But:
- the budget is the length of the judgment's token context, not a term, so
  λᶜᵉʳᵗ cannot quantify over it;
- a usage-ω quantifier over `R` ranges over `V₀(R)`, which contains only
  leaves.

The only λᶜᵉʳᵗ sentence that means `Con_λ` is `Con′_ω`, an O-type, and P5
makes it underivable.

**There are three sentences, not one.**

| Sentence | What it says | λᶜᵉʳᵗ | PA |
| --- | --- | --- | --- |
| `H°` | at budget `n`: `BF₀(n)` | proves it (T2) | proves every instance `BF₀(n)` |
| `Con′_ω` | `Con_λ`, at every budget | cannot prove it (P5) | — |
| `Con_λ` | no code checks as a refutation | — | cannot prove it (it implies `Con_PA`) |

No arithmetic sentence separates λᶜᵉʳᵗ from PA.

**`H°` does carry `Con_λ` externally.** The metatheorem that `H°` holds at
every budget (T1 with T2) is equivalent to `Con_λ`. It is established in the
metatheory, which is stronger than PA, and λᶜᵉʳᵗ cannot cash it into any
arithmetic conclusion (Prop 4.8). This is the representation-relative half of
Willard's "legalistic" objection, in its sharpest form.

**A better description of `H°`.** It is the finitistic consistency schema
`{Con_λ(n)}`, with its index moved out of the formula and into the context:
- PA's `Con_PA(n̄)` carries `n` in a numeral of `O(log n)` symbols;
- `H°` carries it in `Θₙ`, which has `n` entries.

A3 shows why that placement matters.

**Where the intuition is right: Willard, and `P-con`.**
- **Willard's systems do prove arithmetic sentences beyond PA.** `IS(A)`'s
  Group-2 axioms (Willard 2001, eq. (7)) are
  `∀y {TransProof_A(⌜Φ⌝, y) ⊃ Φ}` for every `Π₁⁻` sentence `Φ`. At
  `Φ = (0 = 1)` that is `Con(A)`, so `IS(PA)` proves `Con(PA)`. Its SelfCons
  is a `Π₁` sentence as well. It pays by not proving multiplication total
  (the 2001 systems do not prove even addition total).
- **The λᶜᵉʳᵗ variant with an arithmetic SelfCons axiom exists.** It is Step
  4's `P-con`, whose `ax-con` is `Con′_ω`, by name. If `P-con` is consistent,
  it proves an arithmetic sentence PA cannot. That is the open question of
  §3.4 and §6.
- **λᶜᵉʳᵗ trades the other way.** It keeps all of PA, and its self-trust adds
  no arithmetic theorem.

### A2. How does λᶜᵉʳᵗ gain proofs?

**As data, freely.** Codes (`Syn`) cost no tokens to search for, compute,
receive, store or check. `chk′` and `inspect` cost computation only. An
agent can find proofs by search, build them, or be sent them.

**As evidence, by paying.** To act on what a code proves, a program:
1. mints the code into a certificate with the typed parser, spending one
   supply token per node (R4 §4.7; ADR-0143 `parse_spine`);
2. checks the certificate with `inspect`;
3. reflects it.

The minting agent of METATHEORY §2.4 does exactly this. It is handed a list of
codes and a raw supply. It mints the first code, checks it at its own type,
reflects it, and runs it on the rest. On a code that does not check, it takes
its safe action.

**By receipt.** A certificate can arrive already minted, as an argument. The
delegating agent reads one from the front of its supply.

**By being written in.** A proof in the program's own text is a term. Its
conclusion holds by typing and costs nothing. Only proofs found or received
at runtime pay.

**Where tokens come from.** Only from the caller. No program creates tokens
(T3).
- The root program's budget, a token context or a lazily promised supply, is
  set by whoever runs it.
- Each agent passes the rest of its supply down.
- A lazy supply promised at `N` costs nothing until it is minted. The
  three-generation lineage on `10^100` materialized 37,919 nodes, exactly its
  certificates.

**What "hold" rules out.** It does not stop an agent finding or receiving
proofs. It stops the agent trusting, in advance, proofs not yet minted and
checked. That advance, universal trust would be `Con′_ω` (P5).

### A3. Pudlák's argument

**Verification status.** The primary text could not be opened:
`users.math.cas.cz` and `arxiv.org` are blocked by this environment's egress
policy. The primary text is Pudlák, "On the length of proofs of finitistic
consistency statements in first order theories", *Logic Colloquium '84*,
North-Holland 1986, pp. 165–196. His survey "Incompleteness in the finite
domain" (*BSL*, 2017) was also unreachable. Web-search summaries corroborate
the two statements below. The proof sketches are my reconstructions of the
standard argument.

**Statements.** `Con_T(n)` says "no T-proof of `0 = 1` has at most `n`
symbols". Written with a binary numeral it has `O(log n)` symbols.
- **Lower bound** (Pudlák; also credited to Friedman). Let `T` be
  consistent, extend a weak bounded arithmetic such as `S¹₂`, and have
  polynomial-time recognizable axioms. Then there is `ε > 0` such that, for
  large `n`, every T-proof of `Con_T(n̄)` has at least `n^ε` symbols.
- **Upper bound** (Pudlák). For sequential `T`, suitably axiomatized (for
  instance finitely), `T` proves `Con_T(n̄)` with proofs of size polynomial
  in `n`.

So, inside `T`, the price of consistency up to size `n` lies between `n^ε`
and `n^c`. That is exponential in the length of the statement.

**The lower bound is G2 with the sizes counted.**
1. **A sentence that says it has no short proof.** By the diagonal lemma,
   `δ(x) ↔` "`δ(ẋ)` has no T-proof with at most `x` symbols".
2. **Each `δ(n̄)` is true, and has no proof of at most `n` symbols.**
   Suppose `p` proves `δ(n̄)` with `|p| ≤ n`. Then "`δ(n̄)` has a proof of at
   most `n` symbols" is a true bounded `Σ₁` sentence. `T` proves such
   sentences, so it would prove `¬δ(n̄)` as well as `δ(n̄)`.
3. **`T` knows step 2, efficiently.** The key lemma is formalized
   `Σ₁`-completeness with a polynomial bound: `T` proves that a true bounded
   `Σ₁` statement with a witness of size `s` has a T-proof of size `q(s)`,
   for a fixed polynomial `q`. `S¹₂` proves this, which is why it is the
   base. So `T` proves once, with a fixed proof `π₀`,
   `∀x (Con_T(m(x)) → δ(x))`, where `m(x) = x + q(x) + c`. From a short proof
   of `δ(x)` it would build a proof of `0 = 1` of at most `m(x)` symbols.
4. **Instantiate.** For each `n`, `Con_T(m̄) → δ(n̄)` has a T-proof of size
   `|π₀| + poly(log n)`, with `m = m(n)`.
5. **Conclude.** A T-proof of `Con_T(m̄)` of size `L` gives a proof of
   `δ(n̄)` of size `L + |π₀| + poly(log n)`. Step 2 says that exceeds `n`, so
   `L > n − poly(log n) − |π₀|`. Since `m ≤ n^k`, this gives `L ≥ m^ε` for
   any `ε < 1/k`.

Step 3 is D3, formalized `Σ₁`-completeness, with its cost stated. G2 is the
limit `n → ∞`: consistency for all proofs has no finite price.

**The upper bound uses partial truth.**
- In a sequential theory, satisfaction for formulas of at most `n` symbols is
  definable by a formula of polynomial size in `n`. Its Tarski clauses have
  proofs of polynomial size.
- Every axiom is true, every rule preserves truth, and `0 = 1` is false.
- The induction along a proof of at most `n` lines may be beyond a weak
  theory's induction. It is carried out on a definable cut that provably
  contains `n̄` (Pudlák's shortening of cuts).

**What it says about resources.** G2 does demand resource consumption, in a
precise sense. To be sure that no proof of up to `n` symbols is a refutation,
a theory must spend at least `n^ε` symbols of proof. Nothing precludes that
consumption: the upper bound says the theory can pay it. What cannot exist is
one price for every `n`.

**What it means for λᶜᵉʳᵗ.**
- **Where the price is paid.** At budget `n`, `H°` means `Con_λ(n)`, with
  size counted in certificate nodes (A1). λᶜᵉʳᵗ proves `H°` with one constant
  term. That does not contradict the lower bound:
  - `H°` is not the sentence `Con_λ(n̄)`. Getting that sentence from `H°`
    needs the parser's correctness inside the calculus, which is not
    established (R4 §4.7).
  - Its content at size `n` is exercised only in two ways, and both cost at
    least `n`. One is a derivation at budget `n`, which has more than `n`
    nodes (R4 Lemma 2.7: E3 writes the token context out). The other is a run
    on a supply of `n` nodes, which materializes and checks every certificate
    it trusts.
- **The other currency.** Prop 4.9 pays in proof size: a token-free term for
  depth `≤ k`, doubly exponential in `k`. The upper bound suggests a
  polynomial one exists: T1's model up to budget `n`, formalized inside as a
  partial truth definition. None has been built.
- **A design constraint, from a second direction.** Suppose a variant wrote
  budgets compactly (`O(log n)` nodes for `n` tokens) and verified parsing
  inside the calculus.
  - Then `λc h e. H (parse c s) (transport e)` would derive `Con(n̄)` in
    `O(log n)` nodes, below `n^ε`.
  - If the variant meets the theorem's hypotheses, it would be inconsistent.
    It extends PA; the efficiency conditions are not checked here.
  - So Pudlák's bound independently requires what T1's descent requires:
    granting `n` tokens must cost proof size that grows with `n`. T1's proof
    uses linear growth, which is E3 and strict overhead.
- **The motivation, in these terms.** G2's demand for resources is not
  precluded. λᶜᵉʳᵗ builds it into its types and charges it per trusted proof.
  What λᶜᵉʳᵗ precludes is trust without payment, which is exactly what G2
  forbids.

**Further (from memory).** Krajíček and Pudlák (*JSL* 1989) related a
question to the existence of optimal propositional proof systems, which is
open. The question: does some single theory prove every theory's `Con(n̄)`
with proofs of polynomial size?
