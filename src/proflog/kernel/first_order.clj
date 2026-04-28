(ns proflog.kernel.first-order
  "Equality-free first-order tableau component for profile-dispatched proofs.

   This layer narrows theorem proving to the branch state actually needed for
   equality-free first-order NNF: current formula, pending work, saved branch
   literals, substitution environment, optional fuel, and proof term. Equality,
   disequality maintenance, generated closed terms, and program-call machinery
   stay in the full Proflog kernel."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fresh lcons run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.kernel-support :as support]))

(declare prove-stateo subst-termo subst-term*o subst-lito)

(defn- appendo
  [left right out]
  (conde
    [(== '() left)
     (== right out)]
    [(fresh [head tail rest]
       (== (lcons head tail) left)
       (== (lcons head rest) out)
       (appendo tail right rest))]))

(defn- membero
  [x xs]
  (fresh [head tail]
    (== (lcons head tail) xs)
    (conde
      [(== head x)]
      [(membero x tail)])))

(defn- lookupo
  [binding-nom env value]
  (fresh [rest]
    (conde
      [(== (lcons [binding-nom value] rest) env)]
      [(fresh [pair]
         (== (lcons pair rest) env)
         (lookupo binding-nom rest value))])))

(defn- subst-termo
  [term env out]
  (conde
    [(fresh [binding-nom]
       (== (list 'var binding-nom) term)
       (lookupo binding-nom env out))]
    [(fresh [binding-nom]
       (== (list 'par binding-nom) term)
       (== term out))]
    [(fresh [head args args-out]
       (== (lcons 'app (lcons head args)) term)
       (== (lcons 'app (lcons head args-out)) out)
       (subst-term*o args env args-out))]))

(defn- subst-term*o
  [terms env out]
  (conde
    [(== '() terms)
     (== '() out)]
    [(fresh [head tail head-out tail-out]
       (== (lcons head tail) terms)
       (== (lcons head-out tail-out) out)
       (subst-termo head env head-out)
       (subst-term*o tail env tail-out))]))

(defn- subst-lito
  [fml env out]
  (conde
    [(fresh [term term-out]
       (== (list 'pos term) fml)
       (== (list 'pos term-out) out)
       (subst-termo term env term-out))]
    [(fresh [term term-out]
       (== (list 'neg term) fml)
       (== (list 'neg term-out) out)
       (subst-termo term env term-out))]))

(defn- close-lito
  [lit lits proof]
  (conde
    [(fresh [atom]
       (== (list 'pos atom) lit)
       (membero (list 'neg atom) lits)
       (== '(close) proof))]
    [(fresh [atom]
       (== (list 'neg atom) lit)
       (membero (list 'pos atom) lits)
       (== '(close) proof))]))

(defn- save-lito
  [lit unexpanded lits env fuel proof]
  (fresh [next rest next-fuel prf]
    (== (lcons next rest) unexpanded)
    (== (list 'savefml prf) proof)
    (support/step-fuelo fuel next-fuel)
    (prove-stateo next rest (lcons lit lits) env next-fuel prf)))

(defn prove-stateo
  "Relational equality-free first-order branch-closing tableau."
  [fml unexpanded lits env fuel proof]
  (conde
    ;; Alpha rule: both conjuncts stay on the same branch.
    [(fresh [left right next-fuel prf]
       (== (list 'and left right) fml)
       (== (list 'conj prf) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo left (lcons right unexpanded) lits env next-fuel prf))]

    ;; Beta rule: both disjunctive branches must close.
    [(fresh [left right next-fuel left-proof right-proof]
       (== (list 'or left right) fml)
       (== (list 'split left-proof right-proof) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo left unexpanded lits env next-fuel left-proof)
       (prove-stateo right unexpanded lits env next-fuel right-proof))]

    ;; False closes a branch immediately.
    [(== (list 'false) fml)
     (== '(false-close) proof)]

    ;; True contributes nothing; continue only when more branch work remains.
    [(fresh [next rest next-fuel prf]
       (== (list 'true) fml)
       (== (lcons next rest) unexpanded)
       (== (list 'skip-true prf) proof)
       (support/step-fuelo fuel next-fuel)
       (prove-stateo next rest lits env next-fuel prf))]

    ;; Universal quantifier: instantiate with a fresh proof variable and
    ;; re-enqueue the quantified formula at the end of the pending stack.
    [(nominal/fresh [binding-nom]
       (fresh [free-var body pending next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== (list 'univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body
                         pending
                         lits
                         (lcons [binding-nom free-var] env)
                         next-fuel
                         prf)))]

    ;; In the call-free theorem component, a negated existential is just a
    ;; classical universal. Re-enqueue it here; the full program kernel keeps
    ;; the single-use operational interpretation for procedure-call bodies.
    [(nominal/fresh [binding-nom]
       (fresh [free-var body pending next-fuel prf]
           (== (list 'once-forall (nominal/tie binding-nom body)) fml)
           (== (list 'once-univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body
                         pending
                         lits
                         (lcons [binding-nom free-var] env)
                         next-fuel
                         prf)))]

    ;; Existential quantifier: introduce a fresh rigid branch parameter.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body next-fuel prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (support/step-fuelo fuel next-fuel)
           (prove-stateo body
                         unexpanded
                         lits
                         (lcons [binding-nom (ast/par-term parameter-nom)] env)
                         next-fuel
                         prf))))]

    ;; Literals close directly against complementary saved literals after
    ;; environment substitution. Otherwise they are saved and branch work
    ;; continues.
    [(fresh [lit]
       (subst-lito fml env lit)
       (conde
         [(close-lito lit lits proof)]
         [(save-lito lit unexpanded lits env fuel proof)]))]))

(defn proveo
  "Public equality-free first-order proof relation."
  ([fml unexpanded lits env proof]
   (prove-stateo fml unexpanded lits env nil proof))
  ([fml unexpanded lits env fuel proof]
   (prove-stateo fml unexpanded lits env fuel proof)))

(defn prove
  "Return up to `n` equality-free first-order proof terms for `fml`."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
     (proveo fml '() '() '() proof)))
  ([fml n fuel]
   (run n [proof]
     (proveo fml '() '() '() fuel proof))))
