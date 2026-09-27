(ns lcert.supply-test
  "Tests of the Ansatz-checked lemmas of Step 2 (proflog ADR-0143,
  ansatz/lcert/supply.clj): the spine supply's size, the parser's exact
  consumption, and the caseR case of the fundamental lemma.

  Besides presence, kernel re-verification and pinned statements, the
  compiled functions are run against lcert's own parser on the primitive
  destructor, so the mechanized parser and the program it models are seen to
  agree on data."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lcert.supply :as sp]
            [lcert.verified :as v]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.examples :as ex]))

(deftest the-lemmas-are-in-the-kernel
  (doseq [nm sp/theorem-names]
    (is (sp/declared? nm) nm)
    (is (sp/reverifies? nm) (str "the kernel re-checks " nm))))

(deftest the-key-statements
  (is (str/includes? (sp/statement "spine_nodes") "(nodes (spine #0))"))
  (is (= "(∀ : CT, (∀ : (wfc #0), (ParseOK #1)))" (sp/statement "parse_spine")))
  (testing "ParseOK c says: from any spine of N ≥ nodes(c), parse returns c and the spine of N − nodes(c)"
    (let [d (sp/definition "ParseOK")]
      (doseq [piece ["parse" "spine" "Nat.sub" "nodes" "Prod.mk"]]
        (is (str/includes? d piece) piece))))
  (is (str/includes? (sp/statement "caser_node_env") "VF")))

(defn- random-code [^java.util.Random rng d]
  (if (or (zero? d) (< (.nextDouble rng) 0.35))
    [:sl (rand-nth [:a :b :c])]
    [:sn (rand-nth [:a :b :c]) (random-code rng (dec d)) (random-code rng (dec d))]))

(deftest the-compiled-parser-agrees-with-lcert's
  (let [rng (java.util.Random. 7)
        parse (#'ev/ev (ev/erase (t/check-top 0 (s/parse-term [] ex/parse-prim-form))) [] 1000 {:erase? true})]
    (doseq [code (repeatedly 40 #(random-code rng 5))]
      (let [n (+ (k/nodes code) 3)
            ct (v/code->ct code)
            [ct-cert ct-rest] ((sp/parse ct) (sp/spine n))
            [_ cert rest] ((parse code) (ev/lazy-supply n))]
        (testing (pr-str code)
          (is (= ct ct-cert) "the mechanized parser returns the code itself")
          (is (= (- n (k/nodes code)) (v/nodes ct-rest) (ev/nodes rest))
              "both leave exactly N − nodes(c) supply nodes")
          (is (= code (ev/print-value cert)) "lcert's certificate prints back to the code"))))))
