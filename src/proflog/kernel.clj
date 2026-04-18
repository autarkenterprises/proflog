(ns proflog.kernel
  "Greenfield base tableau kernel.

   This ADR deliberately implements only the baseline first-order tableau
   fragment: conjunction, disjunction, universal quantification, existential
   quantification, and complementary literal closure. Equality and procedure
   calls are left for later ADRs so the kernel can be validated in isolation."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fresh lcons membero run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.subst :as subst]))

(defn complementary-lito
  "Succeed when `lit` has a complementary literal in `lits`.

   Saved literals are already substituted through the current environment, so
   complementary closure can work directly over the branch literal store."
  [lit lits]
  (fresh [atom opposite]
    (conde
      [(== (list 'pos atom) lit)
       (== (list 'neg atom) opposite)]
      [(== (list 'neg atom) lit)
       (== (list 'pos atom) opposite)])
    (membero opposite lits)))

(declare proveo)

(defn proveo
  "Relational tableau prover for the greenfield base kernel.

   Arguments:
   - `fml`: current formula to process
   - `unexpanded`: remaining formulas on the current branch
   - `lits`: substituted literals already saved on the branch
   - `env`: nominal substitution environment for quantifier instantiations
   - `proof`: proof term describing the closure"
  [fml unexpanded lits env proof]
  (conde
    ;; α-rule: both conjuncts must close on the same branch, so the second
    ;; conjunct is pushed onto the branch work stack.
    [(fresh [left right prf]
       (== (list 'and left right) fml)
       (== (list 'conj prf) proof)
       (proveo left (lcons right unexpanded) lits env prf))]

    ;; β-rule: both branches of the disjunction must close independently.
    [(fresh [left right left-proof right-proof]
       (== (list 'or left right) fml)
       (== (list 'split left-proof right-proof) proof)
       (proveo left unexpanded lits env left-proof)
       (proveo right unexpanded lits env right-proof))]

    ;; γ-rule: instantiate a universal with a fresh logic variable and
    ;; re-enqueue the original universal so additional instantiations remain
    ;; available later in the branch.
    [(nominal/fresh [binding-nom]
       (fresh [logic-var body pending prf]
         (== (list 'forall (nominal/tie binding-nom body)) fml)
         (== (list 'univ prf) proof)
         (appendo unexpanded (list fml) pending)
         (proveo body pending lits (lcons [binding-nom logic-var] env) prf)))]

    ;; δ-rule: instantiate an existential exactly once with a rigid internal
    ;; parameter. The original existential is not re-enqueued.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (proveo body
                   unexpanded
                   lits
                   (lcons [binding-nom (list 'par parameter-nom)] env)
                   prf))))]

    ;; Literal handling: substitute the current environment into the literal,
    ;; then either close against an existing complementary literal or save the
    ;; literal and continue with the next pending formula.
    [(fresh [lit]
       (subst/subst-formulao fml env lit)
       (conde
         [(complementary-lito lit lits)
          (== '(close) proof)]
         [(fresh [next rest prf]
            (== (lcons next rest) unexpanded)
            (== (list 'savefml prf) proof)
            (proveo next rest (lcons lit lits) env prf))]))]))

(defn prove
  "Return up to `n` proof terms closing the given greenfield formula."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
     (proveo fml '() '() '() proof))))
