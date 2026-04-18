(ns proflog.equality
  "Constraint-style free-constructor equality helpers for the greenfield kernel.

   The kernel keeps free variables explicit as `(var nom)` terms and threads an
   equality substitution separately. Positive equality extends that
   substitution; negative equality stores symbolic disequalities and rechecks
   them after each new binding. Saved atoms close by unifying their walked
   arguments, so congruence comes from a shared branch state rather than
   object-level rewriting."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [!= == conde fresh lcons membero]]))

(defn lookupo
  "Relational lookup in an explicit substitution or disequality environment."
  [binding-nom env value]
  (fresh [rest]
    (conde
      [(== (lcons [binding-nom value] rest) env)]
      [(fresh [pair]
         (== (lcons pair rest) env)
         (lookupo binding-nom rest value))])))

(defn unboundo
  "Succeed when `binding-nom` does not appear in `env`."
  [binding-nom env]
  (conde
    [(== '() env)]
    [(fresh [other value rest]
       (== (lcons [other value] rest) env)
       (!= other binding-nom)
       (unboundo binding-nom rest))]))

(declare walko walk*o walk-term*o walk-atomo
         absent-termo absent-term*o
         absent-paro absent-par*o
         occurs-termo occurs-term*o
         same-termo same-term*o
         unify-termo unify-term*o
         eq-contradictiono eq-contradiction-term*o)

(defn walko
  "Walk the root of `term` through the explicit equality substitution `sigma`."
  [term sigma out]
  (conde
    [(fresh [binding-nom value]
       (== (list 'var binding-nom) term)
       (lookupo binding-nom sigma value)
       (walko value sigma out))]
    [(fresh [binding-nom]
       (== (list 'var binding-nom) term)
       (unboundo binding-nom sigma)
       (== term out))]
    [(fresh [binding-nom value]
       (== (list 'par binding-nom) term)
       (lookupo binding-nom sigma value)
       (walko value sigma out))]
    [(fresh [binding-nom]
       (== (list 'par binding-nom) term)
       (== term out))]
    [(fresh [head args]
       (== (lcons 'app (lcons head args)) term)
       (== term out))]))

(defn walk*o
  "Deeply walk a term through `sigma`, normalizing all reachable bindings."
  [term sigma out]
  (fresh [root]
    (walko term sigma root)
    (conde
      [(fresh [binding-nom]
         (== (list 'var binding-nom) root)
         (== root out))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) root)
         (== root out))]
      [(fresh [head args args-out]
         (== (lcons 'app (lcons head args)) root)
         (== (lcons 'app (lcons head args-out)) out)
         (walk-term*o args sigma args-out))])))

(defn walk-term*o
  "Deep walk over an argument list."
  [terms sigma out]
  (conde
    [(== '() terms) (== '() out)]
    [(fresh [head tail head-out tail-out]
       (== (lcons head tail) terms)
       (== (lcons head-out tail-out) out)
       (walk*o head sigma head-out)
       (walk-term*o tail sigma tail-out))]))

(defn walk-atomo
  "Deep walk over one atomic application."
  [atom sigma out]
  (fresh [head args args-out]
    (== (lcons 'app (lcons head args)) atom)
    (== (lcons 'app (lcons head args-out)) out)
    (walk-term*o args sigma args-out)))

(defn absent-termo
  "Succeed when `(var binding-nom)` does not occur anywhere in `term`."
  [binding-nom term sigma]
  (fresh [root]
    (walko term sigma root)
    (conde
      [(fresh [other]
         (== (list 'var other) root)
         (!= other binding-nom))]
      [(fresh [parameter-nom]
         (== (list 'par parameter-nom) root))]
      [(fresh [head args]
         (== (lcons 'app (lcons head args)) root)
         (absent-term*o binding-nom args sigma))])))

(defn absent-term*o
  "Succeed when `binding-nom` is absent from every term in `terms`."
  [binding-nom terms sigma]
  (conde
    [(== '() terms)]
    [(fresh [head tail]
       (== (lcons head tail) terms)
       (absent-termo binding-nom head sigma)
       (absent-term*o binding-nom tail sigma))]))

(defn absent-paro
  "Succeed when `(par binding-nom)` does not occur anywhere in `term`."
  [binding-nom term sigma]
  (fresh [root]
    (walko term sigma root)
    (conde
      [(fresh [other]
         (== (list 'var other) root))]
      [(fresh [other]
         (== (list 'par other) root)
         (!= other binding-nom))]
      [(fresh [head args]
         (== (lcons 'app (lcons head args)) root)
         (absent-par*o binding-nom args sigma))])))

(defn absent-par*o
  "Succeed when `(par binding-nom)` is absent from every term in `terms`."
  [binding-nom terms sigma]
  (conde
    [(== '() terms)]
    [(fresh [head tail]
       (== (lcons head tail) terms)
       (absent-paro binding-nom head sigma)
       (absent-par*o binding-nom tail sigma))]))

(defn occurs-termo
  "Succeed when `(var binding-nom)` occurs somewhere inside `term`."
  [binding-nom term sigma]
  (fresh [root]
    (walko term sigma root)
    (conde
      [(== (list 'var binding-nom) root)]
      [(fresh [head args]
         (== (lcons 'app (lcons head args)) root)
         (occurs-term*o binding-nom args sigma))])))

(defn occurs-term*o
  "Succeed when `(var binding-nom)` occurs in one of the `terms`."
  [binding-nom terms sigma]
  (fresh [head tail]
    (== (lcons head tail) terms)
    (conde
      [(occurs-termo binding-nom head sigma)]
      [(occurs-term*o binding-nom tail sigma)])))

(defn same-termo
  "Structural equality on walked terms without introducing new bindings."
  [left right sigma]
  (fresh [left-root right-root]
    (walko left sigma left-root)
    (walko right sigma right-root)
    (conde
      [(fresh [binding-nom]
         (== (list 'var binding-nom) left-root)
         (== (list 'var binding-nom) right-root))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) left-root)
         (== (list 'par binding-nom) right-root))]
      [(fresh [head left-args right-args]
         (== (lcons 'app (lcons head left-args)) left-root)
         (== (lcons 'app (lcons head right-args)) right-root)
         (same-term*o left-args right-args sigma))])))

(defn same-term*o
  "Structural equality on walked term lists."
  [left right sigma]
  (conde
    [(== '() left) (== '() right)]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (same-termo left-head right-head sigma)
       (same-term*o left-tail right-tail sigma))]))

(defn eq-contradictiono
  "Succeed with a proof tag when `left = right` is impossible under `sigma`."
  [left right sigma proof]
  (fresh [left-root right-root]
    (walko left sigma left-root)
    (walko right sigma right-root)
    (conde
      [(fresh [binding-nom]
         (== (list 'var binding-nom) left-root)
         (occurs-termo binding-nom right-root sigma)
         (== '(occurs-close) proof))]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) right-root)
         (occurs-termo binding-nom left-root sigma)
         (== '(occurs-close) proof))]
      [(fresh [left-head left-args right-head right-args]
         (== (lcons 'app (lcons left-head left-args)) left-root)
         (== (lcons 'app (lcons right-head right-args)) right-root)
         (!= left-head right-head)
         (== '(free-close) proof))]
      [(fresh [head left-args right-args subproof]
         (== (lcons 'app (lcons head left-args)) left-root)
         (== (lcons 'app (lcons head right-args)) right-root)
         (eq-contradiction-term*o left-args right-args sigma subproof)
         (== (list 'decompose subproof) proof))])))

(defn eq-contradiction-term*o
  "Find the first contradictory argument pair in two application argument lists."
  [left right sigma proof]
  (conde
    [(fresh [head tail]
       (== (lcons head tail) left)
       (== '() right)
       (== '(free-close) proof))]
    [(fresh [head tail]
       (== '() left)
       (== (lcons head tail) right)
       (== '(free-close) proof))]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (conde
         [(eq-contradictiono left-head right-head sigma proof)]
         [(same-termo left-head right-head sigma)
          (eq-contradiction-term*o left-tail right-tail sigma proof)]))]))

(defn unify-termo
  "Extend `sigma` so `left` and `right` become equal in the free term algebra."
  [left right sigma sigma-out proof]
  (fresh [left-root right-root]
    (walko left sigma left-root)
    (walko right sigma right-root)
    (conde
      [(same-termo left-root right-root sigma)
       (== sigma sigma-out)
       (== '(eq-refl) proof)]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) left-root)
         (absent-termo binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out)
         (== '(eq-bind) proof))]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) right-root)
         (absent-termo binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out)
         (== '(eq-bind) proof))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) left-root)
         (absent-paro binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out)
         (== '(par-bind) proof))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) right-root)
         (absent-paro binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out)
         (== '(par-bind) proof))]
      [(fresh [head left-args right-args subproof]
         (== (lcons 'app (lcons head left-args)) left-root)
         (== (lcons 'app (lcons head right-args)) right-root)
         (unify-term*o left-args right-args sigma sigma-out subproof)
         (== (list 'decompose subproof) proof))])))

(defn unify-term*o
  "Pairwise unification across argument lists."
  [left right sigma sigma-out proof]
  (conde
    [(== '() left)
     (== '() right)
     (== sigma sigma-out)
     (== '() proof)]
    [(fresh [left-head left-tail right-head right-tail sigma-mid head-proof tail-proof]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (unify-termo left-head right-head sigma sigma-mid head-proof)
       (unify-term*o left-tail right-tail sigma-mid sigma-out tail-proof)
       (== (list 'args head-proof tail-proof) proof))]))

(defn atom-unifyo
  "Unify two atomic applications of the same relation symbol."
  [left right sigma sigma-out proof]
  (fresh [head left-args right-args arg-proof]
    (== (lcons 'app (lcons head left-args)) left)
    (== (lcons 'app (lcons head right-args)) right)
    (unify-term*o left-args right-args sigma sigma-out arg-proof)
    (== (list 'atom-close arg-proof) proof)))

(defn neq-violatedo
  "Succeed when one saved disequality has become false under `sigma`."
  [neqs sigma proof]
  (fresh [left right rest]
    (conde
      [(== (lcons [left right] rest) neqs)
       (same-termo left right sigma)
       (== '(neq-close) proof)]
      [(== (lcons [left right] rest) neqs)
       (neq-violatedo rest sigma proof)])))

(defn contradictory-atomso
  "Succeed when saved positive and negative atoms now unify under `sigma`."
  [lits sigma proof]
  (fresh [left-atom right-atom sigma-out atom-proof]
    (conde
      [(membero (list 'pos left-atom) lits)
       (membero (list 'neg right-atom) lits)]
      [(membero (list 'neg left-atom) lits)
       (membero (list 'pos right-atom) lits)])
    (atom-unifyo left-atom right-atom sigma sigma-out atom-proof)
    (== atom-proof proof)))
