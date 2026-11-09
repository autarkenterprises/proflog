# Proflog, as Fitting defined it

This subproject implements the logic programming language of Melvin
Fitting, *Tableaus for Logic Programming* (1993; `../LPTableaus.pdf`). It
covers §2–§7, and nothing else:

- the language L and its programs (§2, Definition 2.1);
- the tableau rules (§4, Tables 1 and 2);
- the equality rules (§5);
- the Procedure Call Rule and queries (§6, Definition 6.1);
- the systematic construction that §7's completeness proof uses.

The semantics of §3 is the specification of every expected value in the
tests. The decision record is
[ADR-0149](../docs/adr/ADR-0149-fitting-core-extraction.md).

## Quick start

```clojure
(require '[proflog.fitting.programs :refer [P1 P2]]
         '[proflog.fitting.query :as q])

(q/query-status P2 '(win (s (s (s 0)))) {})
;; => {:status :fails, :certificate [:call (win (s (s (s 0)))) [:delta ...]]}

(q/query-status P1 '(odd (s 0)) {})
;; => {:status :succeeds, :certificate [:call (not (odd (s 0))) ...]}
```

A query is a ground literal of L. It **succeeds** if there is a closed
P-tableau for ¬A, and **fails** if there is a closed P-tableau for A
(Definition 6.1). Every answer carries its certificate, the closed tableau,
and the relation that found it has re-checked it. `:unresolved` means that
no closed tableau was found within the fuel or time allowed. By Theorem 7.2
this is the only answer a ⊥ query can get.

## Writing programs

```clojure
(require '[proflog.fitting.syntax :as syntax])

(def L (syntax/language {:constants '[0] :functions '{s 1} :relations '{win 1}}))

(def P2
  (syntax/program L
    '[(<- (win x) (exists y (and (or (= x (s y)) (= x (s (s y)))) (not (win y)))))]))
```

| Paper | Here |
|---|---|
| constant `0`, function `s(t)` | `0`, `(s t)` |
| variable `x` | `x`, any symbol that is not a symbol of L |
| parameter (§4) | `(par)`, `(par 1)`, `(par 1 1)`, … (unary) |
| `R(t1,…,tn)`, `t = u` | `(R t1 … tn)`, `(= t u)` |
| `¬X`, `X ∧ Y`, `X ∨ Y`, `X ⊃ Y` | `(not X)`, `(and X Y)`, `(or X Y)`, `(implies X Y)` |
| `(∀x)Φ`, `(∃x)Φ`, true, false | `(forall x Φ)`, `(exists x Φ)`, `true`, `false` |
| `R(x1,…,xn) ← ϕ` | `(<- (R x1 … xn) ϕ)` |

`syntax/program` enforces Definition 2.1:
- at most one clause for each relation;
- distinct head variables;
- every free variable of a body occurs in its head.

The language is fixed first and a program is written in it, as in §2: "A
program, written in a language L, can only answer questions about things
that have names in L."

## Certificates

A certificate is the closed tableau, written as nested vectors. This one is
§6's worked example, exactly as `query-status` finds it. Comments give the
paper's line numbers.

```clojure
[:call (win (s (s (s 0))))                          ; Procedure Call Rule
 [:delta                                            ; (2) new parameter c = (par)
  [:alpha                                           ; (3), (4)
   [:beta                                           ; (5) | (8)
    [:one-one (= (s (s (s 0))) (s (par)))           ; (6)
     [:substitutivity (= (s (s 0)) (par)) (not (win (par))) (not (win (s (s 0))))  ; (7)
      [:call (not (win (s (s 0))))
       [:gamma 0 [:beta [:alpha [:reflexivity (s (s 0))]]
                        [:not-not [:call (win 0) ...]]]]]]]
    ...]]]]
```

A second example: why `odd(s(0))` succeeds under P1. The query succeeds
because the tableau for ¬odd(s(0)) closes:

```clojure
[:call (not (odd (s 0)))           ; ¬odd(s(0)) calls ¬(∀y)[even(y) ⊃ ¬(s(0) = y)]
 [:delta                           ; ¬[even(c) ⊃ ¬(s(0) = c)] for a new c
  [:alpha                          ; even(c), ¬¬(s(0) = c)
   [:not-not                       ; s(0) = c
    [:substitutivity (= (s 0) (par)) (even (par)) (even (s 0))
     [:call (even (s 0))           ; refute even(s(0)):
      [:beta [:free-closure (= (s 0) 0)]           ;   s(0) = 0 is impossible
             [:delta [:alpha                       ;   s(0) = s(d) ∧ odd(d)
              [:one-one (= (s 0) (s (par)))        ;   0 = d
               [:substitutivity (= 0 (par)) (odd (par)) (odd 0)
                [:call (odd 0)                     ;   refute odd(0):
                 [:gamma 0 [:beta [:call (not (even 0)) [:alpha [:reflexivity 0]]]
                                  [:reflexivity 0]]]]]]]]]]]]]]]
```

Rule tags:

| Tag | Rule |
|---|---|
| `:not-not`, `:not-true`, `:not-false`, `:alpha`, `:beta`, `:delta` | Table 2 |
| `:gamma t` | Table 2, γ with the term `t` |
| `:false`, `:close A` | §4 closure |
| `:reflexivity t`, `:free-closure e` | §5 closure |
| `:one-one e`, `:substitutivity e X X'` | §5 extension |
| `:call A` | §6 |

Certificates are plain EDN. `(q/certificate? P X cert)` checks one with
the same relation that found it.

## How it works

`proflog.fitting.tableau/tableauo` is a single relation in core.logic's pure
subset: `==`, `conde`, `fresh`, and relations built from them. There is no
`!=`, nominal logic, `fd`, `conda`/`condu` or `project`.
- **Prove mode.** With the certificate fresh, the relation searches for a
  closed tableau.
- **Check mode.** With the certificate ground, it checks it without
  searching.

Every side condition that the paper states negatively is a lookup in a
finite table built from L and the program text. These cover distinct
symbols, an occurrence of the variable being substituted, and a new
parameter (a per-branch counter).

The relation is §7's systematic construction:
- Each branch expands its sentences in order.
- When only γ sentences are left, the branch is *saturated*. It then either
  closes or applies γ again.
- The certificate records the choices: which closure, which γ term, and
  which equality steps.
- Restrictions that lose nothing keep the search small:
  - each γ term is used once per γ sentence;
  - no literal is added twice;
  - every closure uses a literal that is new since the previous saturation
    point.

Fuel bounds the steps on each branch. The query API searches the success and
failure sides in parallel at one fuel bound, and cancels the slower search.

This is the ground calculus of §4–§6, which Fitting says "is not suitable
for implementation" because of how γ chooses its terms. §8's remedy, free
variables and unification, is an implementation strategy rather than part of
the language. It would be a follow-up ADR.

## Tests

```sh
lein test         # default suite
lein test :slow   # deeper P1 queries (seconds to a minute each)
lein test :all
```
