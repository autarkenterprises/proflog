(ns proflog.sjas-semprfk-tree-closure-test
  "ADR-0142 Phase 3 (pow vocabulary): a bounded `SemPrf^k_alpha` atom can appear as
   a DECODED interior node in a construct-and-check tableau tree.

   Step 5 of the Theorem 2.3 closure (`not Dk`) closes its hard branch on
   `not SemPrf^k_alpha(code(Dk), p, 2^(p+1))`, which is an interior node decoded
   from proof-code bytes -- not the literal root target. Two things must hold for
   the ordinary structural checker to close it in construct-and-check mode:

     1. `semprfk-alpha` and `pow` must DECODE BY NAME, so the decoded node formula
        matches the literal branch formula (`sjas-proof-node-formula-matcho`).
        Before the pow-vocabulary coding change they decode to `(sym n)` (the
        `dsjas-tab2-proof` compaction gap shifts the boundary cluster, and `pow`
        was not reserved at all), so the node never matches.
     2. The structural tree validator must have a `SemPrf^k` closure rule. The
        V-route bound interpretation previously existed only in the kernel SEARCH
        hook; `sjas-semprfk-alpha-structural-closeo` exposes it to
        `structural-proof-valid?`.

   These tests pin both: the same symbolic-tower witness the query-path test
   accepts now closes a decoded `not SemPrf^k` node, while a too-small tower bound
   does not (the bound stays genuinely checked, not assumed)."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- mul+pow-system
  "The generated multiplication-total system with `pow` declared as a language
   function (a 2-place symbolic power used only inside the `SemPrf^k` Log bound)."
  []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- tower-witness
  "Return `[system theorem-code proof-cert proof-value]` for a real, checker-valid
   bounded proof: a Group-2 route axiom proved by its `sjas-axiom` certificate."
  []
  (let [system (mul+pow-system)
        route (first (filter #(= :group-two (:group %)) (:axioms system)))
        cert (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        proof-value (sjas-code/bytes->u-grounding-code-value
                      (sjas-code/u-grounding-code-term-bytes cert))]
    [system (:code route) cert proof-value]))

(defn- pow-bound [exp] (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral exp)))

(defn- semprfk-neg-literal
  "The NNF negated bounded-proof literal `(neg (app semprfk-alpha ...))`."
  [system theorem-code proof-cert bound]
  (ast/neg-lit
    (second (sjas/semprfk-alpha (:system-code system) sjas/one
                                theorem-code proof-cert bound))))

(deftest decoded-semprfk-node-decodes-by-name
  (testing "after the pow-vocabulary coding change, semprfk-alpha and pow decode to their names inside an encoded formula"
    (let [[system theorem-code cert proof-value] (tower-witness)
          neg-lit (semprfk-neg-literal system theorem-code cert (pow-bound (inc proof-value)))
          ;; round-trip the literal back through the proof-facing decoder
          decoded (tb/decode-proof-facing system neg-lit)
          heads (->> decoded (tree-seq seqable? seq)
                     (filter #(and (seq? %) (= 'app (first %))))
                     (map second) set)]
      (is (contains? heads 'semprfk-alpha)
          "the bounded-proof relation head decodes to the name semprfk-alpha, not (sym n)")
      (is (contains? heads 'pow)
          "the symbolic Log bound's pow decodes to the name pow, not (sym n)"))))

(deftest decoded-semprfk-leaf-closes-with-symbolic-tower-bound
  (testing "a DECODED (neg SemPrf^k) node with the symbolic 2^(p+1) bound closes via the structural V-route"
    (let [[system theorem-code cert proof-value] (tower-witness)
          neg-lit (semprfk-neg-literal system theorem-code cert (pow-bound (inc proof-value)))]
      (is (tb/valid-tree? system neg-lit (tb/flex-tableau-node system neg-lit) 80)
          "the constructed cut-free single-node tree for (neg SemPrf^k) is checker-accepted"))))

(deftest too-small-tower-bound-does-not-close
  (testing "the bound is genuinely checked: 2^p gives Log(2^p,1)=p, so proof<p fails and (neg SemPrf^k) does NOT close"
    (let [[system theorem-code cert proof-value] (tower-witness)
          neg-lit (semprfk-neg-literal system theorem-code cert (pow-bound proof-value))]
      (is (not (tb/valid-tree? system neg-lit (tb/flex-tableau-node system neg-lit) 80))
          "a too-small symbolic bound leaves SemPrf^k false, so its negation cannot close"))))
