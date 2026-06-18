# ADR-0116: The Core Procedure Call Rule is Fitting's §8 Free-Variable Extension, not §6

- Status: accepted
- Date: 2026-06-17
- Branch: `fitting-fidelity-audit`
- AAR: [AAR-0116](../aar/AAR-0116-fitting-free-variable-procedure-call.md)
- Audit: [docs/FITTING_FIDELITY_AUDIT.md](../FITTING_FIDELITY_AUDIT.md) (§6 row)

## Context

The Fitting-fidelity audit anchors the greenfield core to Melvin Fitting's
*Tableaus for Logic Programming* (`LPTableaus.pdf`). Fitting's **Procedure Call
Rule** (Definition 6.1, §6) is stated only for a **ground atom of `L`**: a branch
closes if it contains a ground atom `R(t̄)` (resp. `¬R(t̄)`) with a clause
`R(x̄) ← φ(x̄)` and a closed tableau for `φ(t̄)` (resp. `¬φ(t̄)`). Fitting is
explicit that "the ground atom `R(t̄)` … must be of `L`, not of `L^par`."

In §8 ("Implementation and Other Issues") Fitting separately discusses *relaxing*
this to **free variables**: "The introduction of free variables into the tableau
mechanism is useful … it makes it possible to ask queries with free variables in
familiar Prolog style. But this carries a cost." A closed tableau for `R(x)` that
instantiates `x` to `t` during the proof is sound "only provided that `t` is a
term of `L`, and not of the enlargement of `L` by Skolem functions. A mechanism
for ensuring this must be introduced, and it could prove to be a serious
complication."

## The finding (mechanism)

The greenfield core implements the §8 **free-variable** form, not the §6
ground-only form. The admissibility guard for a procedure call is
`kernel-support/l-ground-termo` (`kernel_support.clj:313`):

```clojure
(defn l-ground-termo [term]
  (conde
   ;; An object-language variable is always admissible.
   [(fresh [binding-nom] (== (list 'var binding-nom) term))]
   ;; Constructor term: admissible only if every argument is also in L.
   [(fresh [head args]
      (== (lcons 'app (lcons head args)) term)
      (l-ground-term*o args))]))
```

It **admits `(var …)`** proof variables and **rejects only `(par …)`** delta
parameters. The procedure-call rules gate on it — positive call
`kernel.clj:1209`, negative-call variants `:1266 / :1290 / :1313` — so the core
fires calls on atoms that are *L-ground* (parameter-free) but **not ground**
(they may carry free proof variables). That is exactly Fitting's §8 free-variable
call, and `l-ground-termo` is exactly his "the unifier must be a term of `L`, not
the Skolem enlargement" mechanism, realized structurally by rejecting `par`
(the witnesses of `L^par`).

This **corrects an earlier hypothesis** that the ground-only §6 rule lived in the
kernel with free-variable calls confined to `answer_overlay`. The extension is in
the **core** itself; the overlay adds residuals / call-depth / answer-export on
top of an already-free-variable call rule.

## Decision (classification)

Classify the core Procedure Call Rule as **extension-beyond-Fitting**, landing
squarely in Fitting's own §8 design space (free-variable, Prolog-style calls).
This is a *characterization*, not a defect: it is the intended, useful relaxation
Fitting names. The audit records it as `➕ extension` rather than `faithful`
because the firing condition is strictly broader than §6 as written.

Soundness rests on two guards, which together implement Fitting's §8 "keep the
unifier in `L`" requirement:

- `l-ground-termo` rejects any `(par …)`, so a Skolem/`L^par` witness can never be
  the subject of a call (no leak of the enlargement into an `L`-call);
- `proof-bindingso` (`kernel_support.clj:430`) confines disequality-closure
  bindings to γ-introduced proof variables, and answer export re-filters through
  `l-ground`, so a free variable closed during a call denotes an `L`-term.

The **completeness envelope** is Fitting's §8 disunification caveat: free-variable
calls bring "the systematic generation of all disunifiers … may turn out to be
the most intractable implementation issue." The greenfield uses a symbolic `neqs`
store with delayed checking and bounded fuel; it is therefore *incomplete* for
free-variable answers in general, by design and in line with §8's "incomplete but
efficient mechanism … provided the sources of the incompleteness are sufficiently
well understood."

## Test obligations (TDD)

- `proflog.fitting-fidelity-test/sec6-l-ground-guard-admits-variables-but-rejects-parameters`
  pins the guard: `(var x)` and `s(x)` are L-ground (call admitted); `(par p)`
  and `s(par p)` are not (call rejected). **Green.**
- `…/sec3-p1-even-or-odd-is-undefined` provides indirect behavioural evidence: a
  call whose argument is a delta parameter does not fire, leaving
  `(∀x)(even(x) ∨ odd(x))` at `:unresolved` (Fitting's ⊥, p.6). **Green.**

## Honest scope

This ADR **classifies and pins** the firing condition; it does not *prove*
soundness of the free-variable call rule against the supervaluation semantics
(Fitting's Lemma 7.4 is stated for ground instances). A mechanized
soundness argument for the free-variable extension — and any decision to tighten
toward §6 ground-only calls for a "strict-Fitting" mode — is deferred; this pass
documents, it does not remediate.

## Reconciliation note (parallel work)

This branch stays independent of `origin/main`, which has advanced with related
Proflog work: **ADR-0114 (open-branch witness extraction)** is adjacent to the
free-variable / open-branch behaviour characterized here, and **ADR-0112
(literature tableau golden suite)** may add corpus-level coverage of the same
boundary. Reconcile at audit close (numbering already avoids the taken 0111–0115).

## Exit criteria

- §6 firing condition classified (`➕ extension`), evidenced by a green
  interrogation test, and recorded in the audit matrix with this ADR linked.
- Soundness guards (`l-ground-termo` + `proof-bindingso`) and the completeness
  envelope (§8 disunification) named explicitly.
- Remediation / soundness proof / strict-Fitting mode tracked as deferred.
