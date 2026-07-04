(ns proflog.sjas-clash-probe
  "ADR-0147: isolate the semprf-alpha syntactic-clash mechanism vs subst-code.
   Run: lein run -m proflog.sjas-clash-probe"
  (:require [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]))

(defn- mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- pos-leaf-closes?
  "Does a bare positive atom close on its own (i.e. the relation is standard-false)?"
  [system atom]
  (tb/valid-tree? system atom (tb/flex-tableau-node system atom) 200))

(defn- clash-closes?
  "Does `pos A ∧ neg A` (identical concrete args) close by complementary clash?"
  [system atom]
  (let [negative (ast/neg-lit (second atom))
        target (ast/and-form atom negative)
        proof (tb/flex-tableau-node system target
                (tb/flex-tableau-node system atom
                  (tb/flex-tableau-node system negative)))]
    (tb/valid-tree? system target proof 200)))

(defn -main [& _]
  (let [system (mul+pow-system)
        a (sjas/numeral 1) b (sjas/numeral 2) c (sjas/numeral 3)
        semprf (ast/pos-lit (ast/app-term 'semprf-alpha a b c))
        subst  (sjas/subst-code a b)]
    (println :semprf-alpha
             {:pos-leaf-closes (boolean (pos-leaf-closes? system semprf))
              :clash-closes (boolean (clash-closes? system semprf))})
    (println :subst-code
             {:pos-leaf-closes (boolean (pos-leaf-closes? system subst))
              :clash-closes (boolean (clash-closes? system subst))})
    (println "clash-mechanism probe complete")))
