;; Design probe for ADR-0149 Decision §2/§3 (added 2026-11-08): what happens
;; when unification meets a formula that still contains a binder.
;;
;; Setting: a free-variable (Fitting §8) branch holds (∀y)P(v) and ¬(∀y)P(y),
;; where v is a rigid tableau variable (a core.logic logic variable). For every
;; closed term t, {P(t), (∃y)¬P(y)} is satisfiable, so the branch must not close.
;; Closing it in one step by unifying the two quantified formulas is unsound
;; whenever the unifier binds v to the bound occurrence of y: the bound name then
;; escapes its scope.
;;
;; Expected output (stock core.logic 1.0.1):
;;   named + ==         : ((var y))   ; escape: v bound to a bound occurrence
;;   nominal, no hash   : (a_0)       ; escape: nominal unification allows it
;;   nominal + a#v, b#v : ()          ; blocked only by explicit freshness
;;   atomic control     : ((c))       ; literals contain no binder: no hazard
;;
;; Conclusion recorded in the ADR: the hazard is independent of binder
;; representation. Engines must unify only literals and terms (atomic closure).
(require '[clojure.core.logic :refer [run* ==]]
         '[clojure.core.logic.nominal :as nom])

(println "named + ==         :"
  (run* [v] (== (list 'forall 'y (list 'P v))
                (list 'forall 'y (list 'P (list 'var 'y))))))

(println "nominal, no hash   :"
  (run* [v] (nom/fresh [a b]
              (== (nom/tie a (list 'P v))
                  (nom/tie b (list 'P b))))))

(println "nominal + a#v, b#v :"
  (run* [v] (nom/fresh [a b]
              (nom/hash a v) (nom/hash b v)
              (== (nom/tie a (list 'P v))
                  (nom/tie b (list 'P b))))))

(println "atomic control     :"
  (run* [v] (== (list 'P v) (list 'P '(c)))))
