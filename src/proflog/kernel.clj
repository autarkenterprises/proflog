(ns proflog.kernel
  "Greenfield tableau kernel with explicit equality state.

   The branch state now carries an explicit free-variable substitution and a
   symbolic disequality store. Quantifier instantiation introduces tagged
   `(var nom)` terms, positive equality extends the substitution, and saved
   atoms or disequalities are rechecked after each new equality binding."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fresh lcons membero run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.subst :as subst]))

(defn complementary-lito
  "Succeed when `lit` closes directly against a saved complementary atom."
  [lit lits sigma proof]
  (conde
    [(fresh [atom opposite sigma-out atom-proof]
       (== (list 'pos atom) lit)
       (membero (list 'neg opposite) lits)
       (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
       (== '(close) proof))]
    [(fresh [atom opposite sigma-out atom-proof]
       (== (list 'neg atom) lit)
       (membero (list 'pos opposite) lits)
       (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
       (== '(close) proof))]))

(declare prove-stateo)

(defn prove-stateo
  "Relational tableau prover with explicit equality and disequality state.

   Arguments:
   - `fml`: current formula to process
   - `unexpanded`: remaining formulas on the current branch
   - `lits`: saved positive and negative atoms on the branch
   - `env`: nominal substitution for lexical binders
   - `sigma`: explicit substitution for free proof variables `(var nom)`
   - `neqs`: saved symbolic disequalities
   - `proof`: proof term describing the closure"
  [fml unexpanded lits env sigma neqs proof]
  (conde
    ;; α-rule: both conjuncts must close on the same branch, so the second
    ;; conjunct is pushed onto the branch work stack.
    [(fresh [left right prf]
       (== (list 'and left right) fml)
       (== (list 'conj prf) proof)
       (prove-stateo left (lcons right unexpanded) lits env sigma neqs prf))]

    ;; β-rule: both branches of the disjunction must close independently.
    [(fresh [left right left-proof right-proof]
       (== (list 'or left right) fml)
       (== (list 'split left-proof right-proof) proof)
       (prove-stateo left unexpanded lits env sigma neqs left-proof)
       (prove-stateo right unexpanded lits env sigma neqs right-proof))]

    ;; γ-rule: instantiate a universal with an explicit free variable term and
    ;; re-enqueue the original universal so later instantiations remain
    ;; available on the branch.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body pending prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== (list 'univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (prove-stateo body
                         pending
                         lits
                         (lcons [binding-nom (ast/var-term free-var-nom)] env)
                         sigma
                         neqs
                         prf))))]

    ;; δ-rule: instantiate an existential exactly once with a rigid internal
    ;; parameter. The original existential is not re-enqueued.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (prove-stateo body
                         unexpanded
                         lits
                         (lcons [binding-nom (ast/par-term parameter-nom)] env)
                         sigma
                         neqs
                         prf))))]

    ;; Positive equality closes immediately when the two terms cannot denote
    ;; the same free-constructor object.
    [(fresh [lit left right contradiction-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/eq-contradictiono left right sigma contradiction-proof)
       (== contradiction-proof proof))]

    ;; Otherwise positive equality extends the branch substitution. That new
    ;; information can close the branch either by violating a saved disequality
    ;; or by making two saved complementary atoms unify.
    [(fresh [lit left right sigma-out step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-out step-proof)
       (equality/neq-violatedo neqs sigma-out branch-proof)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-out step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-out step-proof)
       (equality/contradictory-atomso lits sigma-out branch-proof)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-out step-proof next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-out step-proof)
       (== (lcons next rest) unexpanded)
       (== (list 'eq-step step-proof prf) proof)
       (prove-stateo next rest lits env sigma-out neqs prf))]

    ;; Negative equality closes only once its two walked sides are forced equal.
    ;; Otherwise it is stored symbolically and rechecked after later bindings.
    [(fresh [lit left right]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/same-termo left right sigma)
       (== '(refl-close) proof))]
    [(fresh [lit left right next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'neq-store prf) proof)
       (prove-stateo next rest lits env sigma (lcons [left right] neqs) prf))]

    ;; Positive and negative atoms close against a saved complementary atom if
    ;; their walked arguments can be unified. Otherwise they are saved.
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (complementary-lito lit lits sigma proof))]
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (complementary-lito lit lits sigma proof))]
    [(fresh [lit atom next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (prove-stateo next rest (lcons lit lits) env sigma neqs prf))]
    [(fresh [lit atom next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (prove-stateo next rest (lcons lit lits) env sigma neqs prf))]))

(defn proveo
  "Public five-argument kernel relation.

   Existing callers see the same surface signature, but each branch now starts
   with an empty equality substitution and empty disequality store."
  [fml unexpanded lits env proof]
  (prove-stateo fml unexpanded lits env '() '() proof))

(defn prove
  "Return up to `n` proof terms closing the given greenfield formula."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
     (proveo fml '() '() '() proof))))
