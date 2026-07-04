(ns proflog.sjas-step1-dstar-probe
  "ADR-0147 step 1 (Eq 6): D* from (A) SelfCons and (B), via the SUPPORTED
   premise-clash idiom (avoiding the unsupported exists-delta-clash), using a
   concrete instance of Willard's (B). SMALL synthetic codes isolate the tree
   STRUCTURE from the real-diagonal perf wall (a step-4-fixed-point / Codex
   perf concern, orthogonal to whether the step-1 shape closes).

     A          = forall p. not SemPrf_alpha(s,BOT,p)                [SelfCons-L0 shape]
     B-instance = SemPrfK(s,1,dk,p0,b0) -> SemPrf_alpha(s,BOT,q0)    [instance of (B)]
     premise    = SemPrfK(s,1,dk,p0,b0)
   Refute A ^ B-instance ^ premise; both beta branches close by premise-clash
   (¬SemPrfK clashes the premise; SemPrf(BOT,q0) clashed by A's gamma).
   Run: lein run -m proflog.sjas-step1-dstar-probe"
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
        s (sjas/numeral 3) bot (sjas/numeral 2) dk (sjas/numeral 5)
        p0 (sjas/numeral 7) b0 (sjas/numeral 11) q0 (sjas/numeral 13)
        p-nom (nominal/nom (l/lvar 'p))
        ;; A = forall p. not SemPrf_alpha(s, BOT, p)   [SelfCons-L0 shape, small codes]
        a (ast/forall-form p-nom (ast/neg-lit (ast/app-term 'semprf-alpha s bot (ast/var-term p-nom))))
        antecedent (ast/pos-lit (ast/app-term 'semprfk-alpha s sjas/one dk p0 b0))
        consequent (sjas/semprf-alpha s bot q0)
        b-instance (ast/implies-form antecedent consequent)
        premise antecedent
        target (ast/and-form premise (ast/and-form b-instance a))
        b-canon (tb/ast->canonical-child b-instance {})
        impl-left (second b-canon)
        impl-right (nth b-canon 2)
        a-child (tb/ast->canonical-child (tie-body (second a)) {p-nom 'v0})
        tree (tb/flex-tableau-node system target
               (tb/flex-tableau-node system premise
                 (tb/flex-tableau-node system (ast/and-form b-instance a)
                   (tb/flex-tableau-node system b-instance
                     (tb/canonical-flex-tableau-node system (list 'not impl-left)
                       (tb/canonical-flex-tableau-node system (list 'neg (second impl-left))))
                     (tb/canonical-flex-tableau-node system impl-right
                       (tb/flex-tableau-node system a
                         (tb/canonical-flex-tableau-node system a-child)))))))
        ;; soundness guard: WITHOUT A, the consequent SemPrf(BOT,q0) is open
        no-a-target (ast/and-form premise b-instance)
        no-a-tree (tb/flex-tableau-node system no-a-target
                    (tb/flex-tableau-node system premise
                      (tb/flex-tableau-node system b-instance
                        (tb/canonical-flex-tableau-node system (list 'not impl-left)
                          (tb/canonical-flex-tableau-node system (list 'neg (second impl-left))))
                        (tb/canonical-flex-tableau-node system impl-right))))]
    (println :step1-dstar
             {:closes (boolean (tb/valid-tree? system target tree 400))
              :without-A-open (not (boolean (tb/valid-tree? system no-a-target no-a-tree 400)))})
    (println "step1 D* (premise-clash, small codes) probe complete")))
