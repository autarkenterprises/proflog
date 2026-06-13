# SJAS Tableau Correspondence Proof (Track 2b, First Fragment)

Date: 2026-06-13
ADR: [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md)
Criteria: [SJAS Correspondence Proof Criteria](2026-05-26-sjas-correspondence-proof-criteria.md)

This document proves the Track 2b correspondence theorem **over the first
correspondence fragment**. It is a proof by *direct examination* (criterion 2's
permitted route over a proof assistant): Willard's deduction method `D` is stated
precisely from the source, the Proflog side is the inductive structural-checker
relation, and every checker clause is matched to a `D` rule. It is **not**
machine-checked; §10 states exactly what it rests on.

## 0. Notation and the two sides

- **Proflog side.** `Acc(P,S,F)` ≝ the relation
  `sjas-structural-proof-check-state-decodedo` (in
  `proflog.kernel.willard-sjas-profile`, reached via
  `sjas-proof-check-programo`) succeeds with decoded proof tree `P`, system code
  `S`, and target `F`. Its branch state is `(focused, agenda, lits, env, sigma,
  neqs, proof-vars, gamma-terms, fuel)`: the focused formula, the pending
  agenda, the saved branch literals `lits`, the quantifier environment `env`
  (binding-nom ↦ parameter/witness term), the equality substitution `sigma`, the
  disequality store `neqs`, and the proof tree `P` whose node shape is
  `(byte-count formula-byte… child…)` — formula bytes then children, **no
  rule-tag bytes**.
- **Willard side.** `SemPrf_D(T,S,F)` ≝ `T` is a semantic-tableaux proof of `F`
  from the axiom system encoded by `S`, in the sense of willard2001
  (§"semantic tableaux proof", rules quoted in §2). `T` is a finite tree whose
  nodes store sentences; the root stores `¬F` (prenex*); other nodes are axioms
  of the system or deductions from ancestors by the rules of §2; every
  root-to-leaf branch is closed (contains some `Υ` and `¬Υ`).

## 1. Covered domain (criterion 1)

The biconditional ranges over: profiles `:willard-sjas-tableau0` and
`:willard-sjas-level1`; compact base-64 system/theorem/proof codes; theorem
codes that are generated axioms, structurally decoded non-generated formulas,
Group-3 sentences, and Level-1 substituted fixed points; and the **first
fragment** of proof certificates only: formula-bearing structural tableau trees
(no proof-symbol tags) and the bare `sjas-axiom` citation.

Every other proof-certificate constructor is **out of domain by Track 2a
unreachability** (criterion 1 route 3): ADR-0098 proved the equality/disequality
constructors and ADR-0099 the procedure-call and quantifier constructors are
never present in an accepted first-fragment certificate (the structural checker
realizes those features formula-bearing and consumes no such tag). They are
excluded *because they cannot occur*, not by silent quantification.

Beta membership / formula-class validity is the **axiom basis** and is assumed,
exactly as Willard's `D` assumes its axiom system α (criterion 1, trust
boundary; ADR-0072).

## 2. Willard's deduction method `D` (verbatim-faithful)

From `target/sjas-pdf-text/willard2001_self_verifying_axiom_systems_author_jsl1.txt`
(lines 806-839). A Φ-Based Candidate Tree for axiom system α has root `¬Φ`
(prenex*); other nodes are axioms of α or deductions `A ⟹ B` where `A` is an
ancestor of `B`:

1. `Υ∧Γ ⟹ Υ` and `Υ∧Γ ⟹ Γ`. *(α)*
2. `¬¬Υ ⟹ Υ`; `¬(Υ∨Γ) ⟹ ¬Υ∧¬Γ`; `¬(Υ⊃Γ) ⟹ Υ∧¬Γ`; `¬(Υ∧Γ) ⟹ ¬Υ∨¬Γ`;
   `¬∃vΥ(v) ⟹ ∀v¬Υ(v)`; `¬∀vΥ(v) ⟹ ∃v¬Υ(v)`; bounded `¬∃v≤s`, `¬∀v≤s`. *(¬)*
3. siblings `Υ` and `Γ` when their ancestor is `Υ∨Γ`. *(β)*
4. siblings `¬Υ` and `Γ` when their ancestor is `Υ⊃Γ`. *(⊃)*
5. `∃vΥ(v) ⟹ Υ(u)`, `u` a newly introduced parameter symbol. *(δ)*
6. `∃v≤s Υ(v) ⟹ u≤s ∧ Υ(u)`, `u` fresh, `s` a parameter term. *(bounded δ)*
7. `∀vΥ(v) ⟹ Υ(t)`, `t` a parameter term over previously introduced
   parameters. *(γ)*
8. `∀v≤s Υ(v) ⟹ t≤s ⊃ Υ(t)`, `s,t` parameter terms. *(bounded γ)*

Closure: a root-to-leaf branch is closed iff it contains some `Υ` and `¬Υ`. A
proof of `Φ` is a candidate tree with root `¬Φ` (prenex*) all of whose branches
are closed. (Willard notes the theorems hold without prenex*; it is a
normalization convenience — see §8, normalization irrelevance.)

**Encoding requirement (Conventional Tableaux Encoding Requirement).** A proof
with `J` function-symbol occurrences must have Gödel number ≥ `2^{5J}` (≥ 5J
bits) — willard2011 lines 2020-2042; the anti-compression discipline (§7).

## 3. Per-rule correspondence

Each Proflog checker clause (line numbers in `willard_sjas_profile.clj`) is
matched to a `D` rule. Proflog formulas are kept in negation normal form, so the
`D` `¬`-rules appear both as NNF pre-normalization and as the explicit `(not …)`
clauses below.

| `D` rule | Proflog checker clause | Match |
|---|---|---|
| 1 (α) | `(and l r)` decomposition, both conjuncts to the branch (6220) | conjunct extraction; the saved-`lits` set holds both, as `A⟹Υ`, `A⟹Γ`. |
| 2 (¬¬) | `(not (not b)) ⟹ b` (6395) | identical. |
| 2 (¬∧) | `(not (and l r)) ⟹ (not l) ∣ (not r)` siblings (6486-6503) | de Morgan + β. |
| 2 (¬∨) | `(not (or l r)) ⟹ (not l) ∧ (not r)` (6520-6527) | de Morgan. |
| 2 (¬⊃) | `(not (implies l r)) ⟹ (not r) ∧ l` (6577-6578) | identical. |
| 2 (¬∃/¬∀) | `(not (quant (tie n b)))` quantifier negation (6604-6632) | `¬∃⟹∀¬`, `¬∀⟹∃¬`. |
| 2 (¬ over =) | `(not (eq l r))⟹(neq l r)`, `(not (neq l r))⟹(eq l r)` (6449-6470) | literal-level `¬`. |
| 3 (β) | `(or l r)` two-child split | sibling nodes from `Υ∨Γ`. |
| 4 (⊃) | `(implies l r) ⟹ (not l) ∣ r` siblings (6543-6546) | identical to rule 4. |
| 5 (δ) | `(exists (tie n b)) ⟹ b[n:=(par-term p)]`, `p` fresh into `env` (6317/6340) | fresh parameter symbol; freshness is `env` extension with a new par-nom. |
| 6 (bounded δ) | bounded-existential expansion to `u≤s ∧ Υ(u)` (6649-6660 guard form) | matches rule 6. |
| 7 (γ) | `(forall (tie n b)) ⟹ b[n:=t]`, `t` a parameter/gamma term (6602) | parameter term from `env`/`gamma-terms`; repeatable. |
| 8 (bounded γ) | bounded-universal expansion to `t≤s ⊃ Υ(t)` | matches rule 8. |
| axiom node | `sjas-axiom-member-structural-closeo` / reflected-call expansion (6758, 6816-6867) | "nodes are axioms of α": Group-0/1/2(β)/reflected axioms cited from `S`, or a reflected clause instantiated as a deduction. |
| closure | `sjas-complementary-lit-close-coreo` (5950): branch `lits` contains `(pos a)` and `(neg a)` | "branch contains `Υ` and `¬Υ`". |
| closure (=) | reflexive `(neq t t)` via `same-termo` (6707); positive `(eq …)` contradiction / stored-neq violation (6784-6805) | equality closure; admissible in `D` over the Q-equality axioms (willard2000/2002), and formula-bearing here. |

The map is **total** on the first fragment: every clause that can fire on a
tag-free node is in the table, and (Track 2a) no other clause can fire because
its tag is unreachable.

## 4. Translation / reconstruction (criterion 3)

`decode(P)` is the formula-bearing tree obtained by reading each node's
`byte-count`+formula bytes into its sentence and recursing on the children. This
is the `sjas-formal-code`/structural decoder, **not** a call back into
`kernel/prove-programo`. For the first fragment the reconstruction is direct: a
node *is* its sentence plus its children. For `δ`/`γ` the witness/parameter term
is recovered from the child node's instantiated formula together with the `env`
threaded by the checker (the introduced par-nom for `δ`, the parameter term for
`γ`); no separate `witness`/`univ` tag is needed or present (ADR-0099). Hence
`decode` loses nothing the SJAS tree needs.

## 5. Soundness: `Acc(P,S,F) ⟹ SemPrf_D(decode(P),S,F)`

By structural induction on the checker derivation. The checker threads the
branch and, at each node, fires exactly one clause of §3. For each clause, the
matched `D` rule justifies the same parent→child(ren) sentence step:

- α/¬/β/⊃ clauses (6220, 6395-6632, `(or)`, 6543): the child sentences the
  checker pushes are exactly the `D`-deductions of rules 1-4 from the node's
  sentence; the saved `lits` realize the ancestor relation `A ⟹ B`.
- δ (6317/6340): the checker introduces a fresh par-nom into `env` and continues
  with the instantiated body — rule 5's `Υ(u)`, `u` fresh; freshness holds
  because `env` never reuses a par-nom.
- γ (6602): the checker continues with `Υ(t)` for a term `t` drawn from
  `env`/`gamma-terms`, all built from previously introduced parameters — rule 7's
  parameter-term condition.
- bounded δ/γ: the guard child `u≤s ∧ Υ(u)` / `t≤s ⊃ Υ(t)` matches rules 6/8.
- closure (5950, 6707-6805): the checker closes a branch only when `lits` holds
  complementary `(pos a)`/`(neg a)`, or a reflexive/contradictory equality —
  i.e. the branch contains `Υ` and `¬Υ` (the equality closures are `D`-closures
  over the equality axioms).
- axiom node (6758, 6816): the checker admits a node only if its sentence is an
  axiom of `S` (Group-0/1/2(β)/reflected), or a reflected clause instantiated —
  Willard "axioms of α or deductions from higher nodes".

The checker succeeds only when every branch closes, so `decode(P)` is a
candidate tree all of whose root-to-leaf branches are closed: a `D`-proof of `F`.
∎ (modulo §10)

## 6. Completeness: `SemPrf_D(T,S,F) ⟹ ∃P. Acc(P,S,F) ∧ decode(P)=T` (up to §8)

Conversely, every `D`-rule application has a matching checker clause (the §3 map
is onto the rules usable in the fragment). Given a `D`-proof `T`, encode each
node's sentence into the formula-bearing node shape and keep `T`'s child
structure: the resulting `P` is a first-fragment certificate, and replaying the
checker on `P` fires, at each node, the §3 clause for the `D` rule `T` used —
the α/β/¬/⊃ decompositions, the δ fresh-parameter and γ parameter-term steps
(the checker accepts any parameter term `env`/`gamma-terms` admit, which includes
`T`'s), and the closures (`T`'s `Υ`/`¬Υ` pair is in `lits`). No `D`-rule used in
the fragment lacks a clause, so the checker accepts `P`, and `decode(P)=T` up to
the irrelevances of §8. ∎ (modulo §10)

## 7. Anti-compression lemma (criterion 7)

For a first-fragment certificate, each tableau node carries its sentence's
base-64 bytes explicitly (node shape `(byte-count formula-byte… child…)`), and
no tag stands in for a subtree (Track 2a: equality/call/quantifier expansions
are formula-bearing children, ADR-0098/0099; ADR-0097 audits node/leaf/depth and
formula-byte totals). Therefore the certificate's byte length is at least the
sum of its nodes' formula-byte lengths, and each function-symbol occurrence
contributes ≥ 1 base-64 byte = 6 ≥ 5 bits. Hence a certificate proving a tree
with `J` function-symbol occurrences has ≥ `5J` bits — Willard's Conventional
Tableaux Encoding Requirement. A fixed-size or skeletal certificate cannot
validate an arbitrarily large formula-bearing tree: the checker demands the
formula bytes at every node, so the missing content cannot be smuggled through
the `(S,F)` arguments (operational regression in the test suite). ∎

## 8. Irrelevance lemmas (criterion 8)

- **Fuel.** `Acc` is stated existentially over sufficient `fuel`; `fuel` bounds
  search depth, not the accepted tree (the proof tree is supplied). A larger
  fuel accepts the same trees.
- **Scheduling / agenda order.** The checker validates a *supplied* tree; agenda
  order changes which clause is tried first, not which trees are accepted.
- **Caching / host data structures.** Below the proof-object level; absent from
  `decode(P)`.
- **Compact byte layout.** The base-64 encoding is injective and satisfies §7;
  any other natural injective layout meeting §7 gives the same `SemPrf_D` tree
  (Willard permits any natural encoding meeting the requirement).
- **Outer profile wrapper.** `(profiled willard-sjas-tableau0 …)` is erased by
  public certificate construction; profile selection is recovered from `S`
  (ADR-0089). First-fragment certificates carry no profile tag.
- **prenex\* vs NNF root.** Willard's `D` holds with or without prenex* (§2);
  Proflog normalizes the target to NNF. The `¬`-rules of rule 2 are exactly the
  NNF transformation, applied by Proflog up front and by the explicit `(not …)`
  clauses; both reach the same closed formula-bearing tree.

## 9. Relevant-invariant preservation (criterion 6)

Finite tree shape, root target + axiom/deduction ancestry, α/β/γ/δ parent-child
structure, branch closure, code inspectability, byte-arity non-lossiness,
quantifier witness/parameter freshness and γ-repeatability, and the
`subst-prf/4` substitution vocabulary for Level-1 fixed points are all preserved
— each is the §3 match or the §7 size fact, and is audited operationally
(ADR-0096/0097 and the §11 tests).

## 10. What this proof rests on (honesty)

This is a proof by direct examination, not machine-checked. Its validity depends
on:

1. **Faithful `D`.** §2 is transcribed from the willard2001 source; the
   correspondence is only as good as that transcription. `D` for Level-1 also
   draws on the Q-equality / bounded-quantifier treatment of
   willard2000/2002/2005; those rules are matched at the same granularity.
2. **The per-rule matching (§3).** Each match is a structural claim about a
   specific checker clause; the source line numbers let a reviewer re-check each.
3. **Beta trust boundary.** Axiom-basis validity is assumed (§1), as in `D`.
4. **Bounded fragment.** The theorem is the first-fragment biconditional. The
   unbounded-domain theorem (admitting every constructor as a primitive or
   bounded macro) is *not* claimed; Track 2a discharges those constructors by
   unreachability instead.

## 11. Operational corroboration (criterion 9)

The per-rule correspondence witnesses (each `D` rule exercised by an accepted
formula-bearing, in-fragment certificate, audited tag-free), the existing
acceptance selectors (`sjas-proof-check-accepts-formula-bearing-*`), the
rejection selectors (wrong theorem/system/substitution/proof), and the
proof-size regression together corroborate the proof. They are evidence, not a
substitute for §§5-7.

## 12. Conclusion

Over the first correspondence fragment, `Acc(P,S,F) ⟺ SemPrf_D(decode(P),S,F)`
with the §7 size lower bound: the Proflog SJAS tableau deduction method
satisfies the same requirements as Willard's `D` on this fragment. Track 2b is
**complete over the first fragment**; the unbounded-domain extension, the
U-Grounding format, and a machine-checked mechanization are recorded as
follow-up.
