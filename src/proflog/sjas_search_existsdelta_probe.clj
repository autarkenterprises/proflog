(ns proflog.sjas-search-existsdelta-probe
  "ADR-0147: confirm the exists-delta gap is CONFINED to construct-and-check and
   does NOT affect ordinary proof search. The ordinary kernel prover closes the
   tableau for an unsatisfiable formula via its OWN delta/gamma + clash.
     UNSAT : (exists x. P(x)) ^ (forall y. not P(y))  -> should CLOSE
     SAT   : (exists x. P(x)) ^ (forall y. not Q(y))  -> should stay OPEN
   Run: lein run -m proflog.sjas-search-existsdelta-probe"
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]))

(defn -main [& _]
  (let [P (fn [nom] (ast/pos-lit (ast/app-term 'p (ast/var-term nom))))
        nP (fn [nom] (ast/neg-lit (ast/app-term 'p (ast/var-term nom))))
        nQ (fn [nom] (ast/neg-lit (ast/app-term 'q (ast/var-term nom))))
        xn (nominal/nom (l/lvar 'x)) yn (nominal/nom (l/lvar 'y)) zn (nominal/nom (l/lvar 'z))
        ex-P   (ast/exists-form xn (P xn))
        all-nP (ast/forall-form yn (nP yn))
        all-nQ (ast/forall-form zn (nQ zn))
        unsat (ast/and-form ex-P all-nP)
        sat   (ast/and-form ex-P all-nQ)]
    (println :ordinary-kernel-UNSAT-closes
             (boolean (seq (kernel/prove unsat 1 400)))) (flush)
    (println :ordinary-kernel-SAT-stays-open
             (empty? (kernel/prove sat 1 40))) (flush)
    (println "ordinary-search exists-delta probe complete") (flush)))
