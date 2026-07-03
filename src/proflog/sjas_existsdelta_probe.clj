(ns proflog.sjas-existsdelta-probe
  "ADR-0147: establish the exists-delta-clashes-gamma idiom in construct-and-check,
   isolated over subst-code (a known-clashing named relation) at small mass.
     A: (exists x. subst-code(1,x)) ^ (forall y. not subst-code(1,y))   [delta+gamma]
     B: subst-code(1,5) ^ (forall y. not subst-code(1,y))               [premise control]
   Run: lein run -m proflog.sjas-existsdelta-probe"
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]))

(defn- tie-body [t] (.-body ^clojure.core.logic.nominal.Tie t))
(defn- mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn -main [& _]
  (let [system (mul+pow-system)
        one sjas/one
        ;; forall y. not subst-code(1,y)
        yn (nominal/nom (l/lvar 'yn))
        univ (ast/forall-form yn (ast/neg-lit (ast/app-term 'subst-code one (ast/var-term yn))))
        ;; exists x. subst-code(1,x)
        xn (nominal/nom (l/lvar 'xn))
        exq (ast/exists-form xn (ast/pos-lit (ast/app-term 'subst-code one (ast/var-term xn))))
        ;; --- Test A: exists-delta then forall-gamma ---
        tgtA (ast/and-form exq univ)
        exch-A0 (tb/ast->canonical-child (tie-body (second exq)) {xn 'v0})   ; subst-code(1,v0)
        unch-A1 (tb/ast->canonical-child (tie-body (second univ)) {yn 'v1})  ; not subst-code(1,v1)
        unch-A0 (tb/ast->canonical-child (tie-body (second univ)) {yn 'v0})  ; not subst-code(1,v0)
        treeA-v1 (tb/flex-tableau-node system tgtA
                   (tb/flex-tableau-node system exq
                     (tb/canonical-flex-tableau-node system exch-A0
                       (tb/flex-tableau-node system univ
                         (tb/canonical-flex-tableau-node system unch-A1)))))
        treeA-v0 (tb/flex-tableau-node system tgtA
                   (tb/flex-tableau-node system exq
                     (tb/canonical-flex-tableau-node system exch-A0
                       (tb/flex-tableau-node system univ
                         (tb/canonical-flex-tableau-node system unch-A0)))))
        ;; --- Test B: concrete premise then forall-gamma (control) ---
        prem (ast/pos-lit (ast/app-term 'subst-code one (sjas/numeral 5)))
        tgtB (ast/and-form prem univ)
        unB (tb/ast->canonical-child (tie-body (second univ)) {yn 'v0})
        treeB (tb/flex-tableau-node system tgtB
                (tb/flex-tableau-node system prem
                  (tb/flex-tableau-node system univ
                    (tb/canonical-flex-tableau-node system unB))))]
    (println :A-exists-delta-gamma-v1 (boolean (tb/valid-tree? system tgtA treeA-v1 300)))
    (println :A-exists-delta-gamma-v0 (boolean (tb/valid-tree? system tgtA treeA-v0 300)))
    (println :B-premise-gamma-control (boolean (tb/valid-tree? system tgtB treeB 300)))
    (println "exists-delta idiom probe complete")))
