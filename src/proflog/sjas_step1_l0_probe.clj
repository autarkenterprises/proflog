(ns proflog.sjas-step1-l0-probe
  "ADR-0147 step-1 research probe: does the generated condition-(A) SelfCons-L0
   companion CLOSE against a positive V5-consequent-shaped `SemPrf_alpha(sys,BOT,q)`
   witness, and is that closure boundary-blind?

   This is leaf (iii) of the stage-2 leaf inventory (the L0 gamma clash against
   V5's consequent witness). It is the semprf-alpha clash the falsifier guardrail
   flags as boundary-blind, so it must be MEASURED, not assumed. Run:
     lein run -m proflog.sjas-step1-l0-probe"
  (:require [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- l0-record [system]
  (first (filter #(= :group-three-l0 (:group %)) (:axioms system))))

(defn- close-case
  "Build [target proof] for: positive SemPrf_alpha(sys,BOT,W) premise +
   SelfCons-L0 companion, expecting the L0 gamma child to clash the premise."
  [system]
  (let [l0 (:formula (l0-record system))
        tie (second l0)
        p-nom (.-binding_nom ^clojure.core.logic.nominal.Tie tie)
        body (.-body ^clojure.core.logic.nominal.Tie tie) ; (neg (app semprf-alpha sys bot (var p)))
        app (second body)
        sys-code (nth app 2)
        bot-code (nth app 3)
        witness (sjas/numeral 5)
        premise (sjas/semprf-alpha sys-code bot-code witness)
        target (ast/and-form premise l0)
        child (tb/ast->canonical-child body {p-nom 'v0})
        proof (tb/flex-tableau-node system target
                (tb/flex-tableau-node system premise
                  (tb/flex-tableau-node system l0
                    (tb/canonical-flex-tableau-node system child))))
        no-premise-proof (tb/flex-tableau-node system l0
                           (tb/canonical-flex-tableau-node system child))]
    {:with-premise [target proof]
     :without-premise [l0 no-premise-proof]}))

(defn- mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- add-only-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (merge {'pow 2} sjas/total-multiplication-functions)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- subst-control
  "Same tree SHAPE as the L0 case but over `subst-code` (a named atom the not-Dk
   tests prove DOES clash). Isolates whether a `false` L0 result is a
   semprf-alpha-specific exclusion vs a structural bug in the tree shape."
  [system]
  (let [h (clojure.core.logic.nominal/nom (clojure.core.logic/lvar 'ctrl-h))
        univ (ast/forall-form h (ast/neg-lit (ast/app-term 'subst-code
                                               (sjas/numeral 3) (ast/var-term h))))
        premise (sjas/subst-code (sjas/numeral 3) (sjas/numeral 5))
        target (ast/and-form premise univ)
        body (.-body ^clojure.core.logic.nominal.Tie (second univ))
        child (tb/ast->canonical-child body {h 'v0})
        proof (tb/flex-tableau-node system target
                (tb/flex-tableau-node system premise
                  (tb/flex-tableau-node system univ
                    (tb/canonical-flex-tableau-node system child))))]
    (tb/valid-tree? system target proof 200)))

(defn- interpreted-route-confirm
  "Close `neg SemPrf_alpha(sys, code(eq(1,1)), sjas-axiom-cite)` via the
   interpreted V-route: eq(1,1) is an axiom-member and the sjas-axiom cite proves
   it, so `sjas-semprf-alpha-closeo` (NON-k) should validate the leaf. This is
   the genuine-proof analogue of the fake-witness leaf-iii case."
  [system]
  (let [beta-thm (ast/eq-lit sjas/one sjas/one)
        thm-code (sjas/formula-code system beta-thm)
        cite (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        leaf (ast/neg-lit (ast/app-term 'semprf-alpha (:system-code system)
                                        thm-code cite))]
    (tb/valid-tree? system leaf (tb/flex-tableau-node system leaf) 300)))

(defn- k-route-confirm
  "Close `neg SemPrf^k(sys, 1, code(eq(1,1)), sjas-axiom-cite, 2^(cite+1))` via the
   K-version interpreted route (the ADR-0146 mechanism), in THIS harness. If this
   closes but the non-k `interpreted-route-confirm` does not, the gap is non-k-specific."
  [system]
  (let [beta-thm (ast/eq-lit sjas/one sjas/one)
        thm-code (sjas/formula-code system beta-thm)
        cite (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        cite-value (sjas-code/bytes->u-grounding-code-value
                     (sjas-code/u-grounding-code-term-bytes cite))
        bound (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral (inc cite-value)))
        leaf (ast/neg-lit (second (sjas/semprfk-alpha (:system-code system) sjas/one
                                                      thm-code cite bound)))]
    (tb/valid-tree? system leaf (tb/flex-tableau-node system leaf) 300)))

(defn -main [& _]
  (doseq [[label system] [[:mul (mul+pow-system)] [:add-only (add-only-system)]]]
    (let [{:keys [with-premise without-premise]} (close-case system)
          [t1 p1] with-premise
          [t2 p2] without-premise
          closes (tb/valid-tree? system t1 p1 200)
          soundness (tb/valid-tree? system t2 p2 200)
          control (subst-control system)
          interp (interpreted-route-confirm system)
          kroute (k-route-confirm system)]
      (println label
               {:l0-fake-witness-syntactic-clash closes
                :l0-alone-closes (boolean soundness)
                :subst-control-closes (boolean control)
                :nonk-interpreted-route-axiom-proof (boolean interp)
                :k-interpreted-route-axiom-proof (boolean kroute)})))
  (println "leaf-iii mechanic probe complete"))
