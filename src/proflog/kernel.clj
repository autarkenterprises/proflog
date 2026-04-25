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
            [proflog.kernel-support :as support]
            [proflog.program :as program]
            [proflog.subst :as subst]))

(declare prove-stateo saved-call-closeso)

(def l-ground-termo support/l-ground-termo)

(defn saved-call-closeso
  "Succeed when one saved atom becomes callable under the current equality
   substitution and its subsidiary tableau closes."
  [lits proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (membero (list 'pos atom) lits)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo body
                     '()
                     '()
                     call-env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     next-fuel
                     subproof))]
    [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (membero (list 'neg atom) lits)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-neg-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo negated-body
                     '()
                     '()
                     call-env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     next-fuel
                     subproof))]))

(defn prove-stateo
  "Relational tableau prover with explicit equality and disequality state.

   This is the ordinary proof kernel. It closes branches by proof search, but
   it does not export answer vars, residual deferred calls, or answer-mode
   recursive budgets; ADR-0015 moves those concerns into
   `proflog.answer-overlay`."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    [(fresh [left right prf]
       (== (list 'and left right) fml)
       (== (list 'conj prf) proof)
       (prove-stateo left
                     (lcons right unexpanded)
                     lits
                     env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     fuel
                     prf))]

    [(fresh [left right sigma-mid neqs-mid left-proof right-proof]
       (== (list 'or left right) fml)
       (== (list 'split left-proof right-proof) proof)
       (prove-stateo left
                     unexpanded
                     lits
                     env
                     proof-vars
                     sigma
                     sigma-mid
                     neqs
                     neqs-mid
                     prog
                     fuel
                     left-proof)
       (prove-stateo right
                     unexpanded
                     lits
                     env
                     proof-vars
                     sigma-mid
                     sigma-out
                     neqs-mid
                     neqs-out
                     prog
                     fuel
                     right-proof))]

    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== '() unexpanded)
           (== (list 'univ prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body-subst
                         '()
                         lits
                         (lcons [binding-nom (ast/var-term free-var-nom)] env)
                         (lcons free-var-nom proof-vars)
                         sigma
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         next-fuel
                         prf))))]
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env pending next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== (list 'univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body-subst
                         pending
                         lits
                         (lcons [binding-nom (ast/var-term free-var-nom)] env)
                         (lcons free-var-nom proof-vars)
                         sigma
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         next-fuel
                         prf))))]

    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env prf]
           (== (list 'once-forall (nominal/tie binding-nom body)) fml)
           (== (list 'once-univ prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (prove-stateo body-subst
                         unexpanded
                         lits
                         (lcons [binding-nom (ast/var-term free-var-nom)] env)
                         (lcons free-var-nom proof-vars)
                         sigma
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         fuel
                         prf))))]

    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body body-subst narrowed-env next-fuel prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body-subst
                         unexpanded
                         lits
                         (lcons [binding-nom (ast/par-term parameter-nom)] env)
                         proof-vars
                         sigma
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         next-fuel
                         prf))))]

    [(fresh [lit left right contradiction-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/eq-contradictiono left right sigma contradiction-proof)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== contradiction-proof proof))]

    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/neq-violatedo neqs sigma-mid branch-proof)
       (== sigma-mid sigma-out)
       (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/contradictory-atomso lits sigma-mid sigma-out branch-proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (saved-call-closeso lits proof-vars sigma-mid sigma-out neqs neqs-out prog fuel branch-proof)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (== (lcons next rest) unexpanded)
       (== (list 'eq-step step-proof prf) proof)
       (support/stable-neqso neqs sigma-mid)
       (prove-stateo next
                     rest
                     lits
                     env
                     proof-vars
                     sigma-mid
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     fuel
                     prf))]

    [(fresh [lit left right]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/same-termo left right sigma)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== '(refl-close) proof))]
    [(fresh [lit left right sigma-mid new-bindings binding rest step-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (appendo new-bindings sigma sigma-mid)
       (== (lcons binding rest) new-bindings)
       (support/proof-bindingso new-bindings proof-vars)
       (== sigma-mid sigma-out)
       (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
       (== (list 'neq-close step-proof) proof))]
    [(fresh [lit left right next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'neq-store prf) proof)
       (prove-stateo next
                     rest
                     lits
                     env
                     proof-vars
                     sigma
                     sigma-out
                     (lcons [left right] neqs)
                     neqs-out
                     prog
                     fuel
                     prf))]

    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (support/complementary-lito lit lits sigma sigma-out proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'pos-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo body
                     '()
                     '()
                     call-env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     next-fuel
                     subproof))]
    [(fresh [lit atom next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (prove-stateo next
                     rest
                     (lcons lit lits)
                     env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     fuel
                     prf))]

    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (support/complementary-lito lit lits sigma sigma-out proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'neg-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo negated-body
                     '()
                     '()
                     call-env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     next-fuel
                     subproof))]
    [(fresh [lit atom next rest prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (prove-stateo next
                     rest
                     (lcons lit lits)
                     env
                     proof-vars
                     sigma
                     sigma-out
                     neqs
                     neqs-out
                     prog
                     fuel
                     prf))]))

(defn proveo
  "Public five-argument pure kernel relation."
  ([fml unexpanded lits env proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil nil proof)))
  ([fml unexpanded lits env fuel proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil fuel proof))))

(defn prove-programo
  "Pure kernel relation with an explicit compiled program for procedure calls."
  ([fml unexpanded lits env prog proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out prog nil proof)))
  ([fml unexpanded lits env prog fuel proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out prog fuel proof))))

(defn prove
  "Return up to `n` proof terms closing the given greenfield formula."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
        (proveo fml '() '() '() proof)))
  ([fml n fuel]
   (run n [proof]
        (proveo fml '() '() '() fuel proof))))

(defn prove-program
  "Return up to `n` proof terms closing `fml` relative to `prog`."
  ([prog fml n]
   (run n [proof]
        (prove-programo fml '() '() '() prog proof)))
  ([prog fml n fuel]
   (run n [proof]
        (prove-programo fml '() '() '() prog fuel proof))))
