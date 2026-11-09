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

;; Whole-tableau consequence. Let
;;   S = ∀x[ (∀y R(x,y) ∧ ¬∀y R(y,y)) ∨ (∀y Q(x,y) ∧ ¬∀y Q(y,y)) ].
;; S is true in the weak Herbrand model {d,e} with 0 ↦ d and e non-standard
;; (§3 drops domain closure; Def 3.1 is vacuous for L = {0}), R false only at
;; (d,d) and Q false only at (e,e). So Fitting's calculus has no closed
;; tableau for it. After γ (v) and β, each branch "closes" if its quantified
;; pair unifies. The binding of v is global (rigid), so one binding must serve
;; both branches.
;; Expected output:
;;   named: both branches close   : ((var y))  ; escape left, capture right: S refuted
;;   nominal: both branches close : ()         ; names globally distinct: no recapture
;;   nominal: left branch alone   : (a_0)      ; but the left closure is still bogus
(println "named: both branches close   :"
  (run* [v]
    (== (list 'forall 'y (list 'R v (list 'var 'y)))
        (list 'forall 'y (list 'R (list 'var 'y) (list 'var 'y))))
    (== (list 'forall 'y (list 'Q v (list 'var 'y)))
        (list 'forall 'y (list 'Q (list 'var 'y) (list 'var 'y))))))

(println "nominal: both branches close :"
  (run* [v] (nom/fresh [a b c d]
              (== (nom/tie a (list 'R v a)) (nom/tie b (list 'R b b)))
              (== (nom/tie c (list 'Q v c)) (nom/tie d (list 'Q d d))))))

(println "nominal: left branch alone   :"
  (run* [v] (nom/fresh [a b]
              (== (nom/tie a (list 'R v a)) (nom/tie b (list 'R b b))))))
