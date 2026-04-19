(ns proflog.kernel
  "Greenfield tableau kernel with explicit equality state.

   The branch state now carries an explicit free-variable substitution and a
   symbolic disequality store. Quantifier instantiation introduces tagged
   `(var nom)` terms, positive equality extends the substitution, and saved
   atoms or disequalities are rechecked after each new equality binding."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fail fresh lcons membero project run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.program :as program]
            [proflog.subst :as subst]))

(defn complementary-lito
  "Succeed when `lit` closes directly against a saved complementary atom.

   The resulting bindings are returned through `sigma-out`, so later proof
   obligations on the same branch can observe them."
  [lit lits sigma sigma-out proof]
  (conde
    [(fresh [atom opposite atom-proof]
       (== (list 'pos atom) lit)
       (membero (list 'neg opposite) lits)
       (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
       (== '(close) proof))]
    [(fresh [atom opposite atom-proof]
       (== (list 'neg atom) lit)
       (membero (list 'pos opposite) lits)
       (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
       (== '(close) proof))]))

(declare l-ground-term*o)

(defn l-ground-termo
  "Succeed when `term` is in the object language `L`, i.e. contains no `par`.

   This relation is structural rather than projected: explicit object-language
   variables are admissible, constructor terms recurse through their arguments,
   and any unresolved `(par ...)` term causes failure."
  [term]
  (conde
    [(fresh [binding-nom]
       (== (list 'var binding-nom) term))]
    [(fresh [head args]
       (== (lcons 'app (lcons head args)) term)
       (l-ground-term*o args))]))

(defn- l-ground-term*o
  "Succeed when every term in `terms` stays inside the object language `L`."
  [terms]
  (conde
    [(== '() terms)]
    [(fresh [head tail]
       (== (lcons head tail) terms)
       (l-ground-termo head)
       (l-ground-term*o tail))]))

(defn- step-fuelo
  "Consume one unit of bounded search control.

   The budget is reserved for the potentially unbounded expansion points:
   quantifier instantiation and recursive procedure calls. Structural branch
   processing (α/β, equality propagation, literal saving) remains unrestricted
   under one budget slice because those steps are finite once the current
   branch formulas are fixed.

   `nil` means unbounded search. A budget of `0` blocks any further bounded
   expansions while still allowing direct closure on the current branch."
  [fuel next-fuel]
  (project [fuel]
    (cond
      (nil? fuel) (== next-fuel nil)
      (> fuel 0) (== next-fuel (dec fuel))
      :else fail)))

(declare prove-stateo saved-call-closeso)

(defn- proof-bindingso
  "Succeed when every binding added during proof search targets a γ-introduced
   proof variable rather than an explicit user-level `(var ...)`."
  [bindings proof-vars]
  (conde
    [(== '() bindings)]
    [(fresh [binding-nom value rest]
       (== (lcons [binding-nom value] rest) bindings)
       (membero binding-nom proof-vars)
       (proof-bindingso rest proof-vars))]))

(defn saved-call-closeso
  "Succeed when one saved atom becomes callable under the current equality
   substitution and its subsidiary tableau closes.

   This makes procedure-call completeness depend on the branch state, not on
   whether the enabling equality literal happened to be expanded before or
   after the atom was saved."
  [lits proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (membero (list 'pos atom) lits)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-call subproof) proof)
       (step-fuelo fuel next-fuel)
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
       (l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-neg-call subproof) proof)
       (step-fuelo fuel next-fuel)
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

   Arguments:
   - `fml`: current formula to process
   - `unexpanded`: remaining formulas on the current branch
   - `lits`: saved positive and negative atoms on the branch
   - `env`: nominal substitution for lexical binders
   - `sigma`: input substitution for free proof variables `(var nom)`
   - `sigma-out`: output substitution after the branch closes
   - `neqs`: input symbolic disequality store
   - `neqs-out`: output symbolic disequality store
   - `prog`: compiled Proflog program, or nil for theorem-proving mode
   - `proof`: proof term describing the closure"
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    ;; α-rule: both conjuncts must close on the same branch, so the sibling
    ;; conjunct is pushed onto the branch work stack. Equality-triggered saved
    ;; literal closure handles the important order-insensitive case where a
    ;; later equality unlocks an earlier saved atom.
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

    ;; β-rule: both branches must close under one compatible proof state. The
    ;; resulting substitution threads from the first sibling into the second.
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

    ;; γ-rule: instantiate a universal with an explicit free variable term and
    ;; re-enqueue the original universal so later instantiations remain
    ;; available on the branch.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== '() unexpanded)
           (== (list 'univ prf) proof)
           (step-fuelo fuel next-fuel)
           (prove-stateo body
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
         (fresh [body pending next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== (list 'univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (step-fuelo fuel next-fuel)
           (prove-stateo body
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

    ;; δ-rule: instantiate an existential exactly once with a rigid internal
    ;; parameter. The original existential is not re-enqueued.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body next-fuel prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (step-fuelo fuel next-fuel)
           (prove-stateo body
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

    ;; Positive equality closes immediately when the two terms cannot denote
    ;; the same free-constructor object.
    [(fresh [lit left right contradiction-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/eq-contradictiono left right sigma contradiction-proof)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== contradiction-proof proof))]

    ;; Otherwise positive equality extends the branch substitution. That new
    ;; information can close the branch either by violating a saved disequality
    ;; or by making two saved complementary atoms unify.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/neq-violatedo neqs sigma-mid branch-proof)
       (== sigma-mid sigma-out)
       (== neqs neqs-out)
       (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/contradictory-atomso lits sigma-mid sigma-out branch-proof)
       (== neqs neqs-out)
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

    ;; Negative equality closes only once its two walked sides are forced equal.
    ;; Otherwise it is stored symbolically and rechecked after later bindings.
    [(fresh [lit left right]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/same-termo left right sigma)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== '(refl-close) proof))]
    [(fresh [lit left right sigma-mid new-bindings binding step-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (appendo new-bindings sigma sigma-mid)
       (== (lcons binding '()) new-bindings)
       (proof-bindingso new-bindings proof-vars)
       (== sigma-mid sigma-out)
       (== neqs neqs-out)
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

    ;; Positive and negative atoms close against a saved complementary atom if
    ;; their walked arguments can be unified. If no direct complement exists,
    ;; the Procedure Call Rule may close the branch through a fresh subsidiary
    ;; tableau over the compiled clause body.
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (complementary-lito lit lits sigma sigma-out proof)
       (== neqs neqs-out))]
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'pos-call subproof) proof)
       (step-fuelo fuel next-fuel)
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
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (complementary-lito lit lits sigma sigma-out proof)
       (== neqs neqs-out))]
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'neg-call subproof) proof)
       (step-fuelo fuel next-fuel)
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
  "Public five-argument kernel relation.

   Existing callers see the same surface signature, but each branch now starts
   with an empty equality substitution and empty disequality store."
  ([fml unexpanded lits env proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil nil proof)))
  ([fml unexpanded lits env fuel proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil fuel proof))))

(defn prove-programo
  "Kernel relation with an explicit compiled program for procedure calls."
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
