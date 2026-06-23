(ns proflog.literature-tableau-golden-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.literature-tableau-golden :as golden]))

(defn source-tests
  []
  (set (map :source-test golden/inventory-entries)))

(defn entry
  [source-test]
  (golden/entry-by-id source-test))

(defn translation
  [source-test]
  (golden/translation-for-source-test source-test))

(defn open?
  [formula]
  (= :open (golden/proflog-observation formula)))

(defn closes?
  [formula]
  (= :closes (golden/proflog-observation formula)))

(defn lit
  [name]
  (ast/pos-lit (ast/app-term (symbol name))))

(def p (lit "p"))
(def q (lit "q"))
(def r (lit "r"))
(def s (lit "s"))

(defn not*
  [formula]
  (ast/not-form formula))

(defn and*
  [& formulas]
  (reduce ast/and-form formulas))

(defn or*
  [& formulas]
  (reduce ast/or-form formulas))

(defn implies*
  [antecedent consequent]
  (ast/implies-form antecedent consequent))

(deftest upstream-suite-inventory-is-complete
  (testing "The ADR-0112 catalog keeps a row for every upstream pytest test."
    (is (= 76 (count golden/inventory-entries)))
    (is (= 76 (count (source-tests))))
    (is (every? (fn [row]
                  (and (:source-test row)
                       (:source-file row)
                       (:coverage row)
                       (:source-location row)
                       (:source-expectation row)))
                golden/inventory-entries))))

(deftest coverage-classes-are-explicit
  (testing "Every row is classified as direct, analog, performance, or unsupported."
    (is (= #{:direct :analog :performance :unsupported}
           (set (map :coverage golden/inventory-entries))))
    (is (= {:direct 30
            :analog 13
            :performance 7
            :unsupported 26}
           golden/inventory-stats))))

(deftest runnable-rows-have-an-auditable-translation-record
  (doseq [row golden/inventory-entries]
    (let [record (translation (:source-test row))]
      (if (= :unsupported (:coverage row))
        (is (nil? record) (:source-test row))
        (do
          (is record (:source-test row))
          (is (= (:source-test row) (:source-test record)) (:source-test row))
          (is (= (:source-expectation row) (:source-expectation record)) (:source-test row))
          (is (contains? record :retained-assertions) (:source-test row))
          (is (contains? record :dropped-assertions) (:source-test row))
          (is (seq (:retained-assertions record)) (:source-test row))
          (is (:proflog-mode record) (:source-test row))
          (is (:proflog-expectation record) (:source-test row)))))))

(deftest direct-rows-do-not-hide-dropped-semantic-obligations
  (doseq [row (filter #(= :direct (:coverage %)) golden/inventory-entries)]
    (let [record (translation (:source-test row))]
      (is (empty? (:dropped-assertions record))
          (str (:source-test row) " is marked direct but drops "
               (:dropped-assertions record))))))

(deftest unsupported-rows-include-specific-reconciliation-reasons
  (testing "Unsupported rows explain the missing feature instead of disappearing from the catalog."
    (is (= 26 (count golden/reconciliation-entries)))
    (doseq [{:keys [source-test status reason]} golden/reconciliation-entries]
      (is (= :unsupported status) source-test)
      (is (seq reason) source-test))))

(deftest previously-mismatched-direct-translations-are-source-faithful
  (testing "test_complex_nested_formula keeps the exact implication chain and the source model assertion."
    (is (= (and* (implies* (and* p q) r)
                 (implies* r s)
                 (and* p q))
           (golden/formula-for-key :complex-nested-formula)))
    (is (= :open (-> (translation "test_complex_nested_formula") :proflog-expectation)))
    (is (= ["formula is satisfiable"]
           (-> (translation "test_complex_nested_formula") :retained-assertions)))
    (is (= ["all extracted models set s=true"]
           (-> (translation "test_complex_nested_formula") :dropped-assertions)))
    (is (= :analog (:coverage (entry "test_complex_nested_formula")))))

  (testing "test_de_morgan_laws carries all four tautological implications as refutation checks."
    (is (= [(implies* (not* (and* p q)) (or* (not* p) (not* q)))
            (implies* (or* (not* p) (not* q)) (not* (and* p q)))
            (implies* (not* (or* p q)) (and* (not* p) (not* q)))
            (implies* (and* (not* p) (not* q)) (not* (or* p q)))]
           (map golden/formula-for-key
                (:refutation-formula-keys (translation "test_de_morgan_laws")))))
    (is (= :closes (-> (translation "test_de_morgan_laws") :proflog-expectation)))
    (is (= :direct (:coverage (entry "test_de_morgan_laws")))))

  (testing "test_multiple_formulas_consistent preserves the source list as a conjunction."
    (is (= (and* (implies* p q)
                 (implies* q r)
                 p)
           (golden/formula-for-key :multiple-formulas-consistent)))
    (is (= ["formula set is satisfiable"]
           (-> (translation "test_multiple_formulas_consistent") :retained-assertions)))
    (is (= ["all extracted models set p,q,r=true"]
           (-> (translation "test_multiple_formulas_consistent") :dropped-assertions)))
    (is (= :analog (:coverage (entry "test_multiple_formulas_consistent")))))

  (testing "test_multiple_formulas_inconsistent preserves p, p->q, and not q."
    (is (= (and* p
                 (implies* p q)
                 (not* q))
           (golden/formula-for-key :multiple-formulas-inconsistent)))
    (is (= :closes (-> (translation "test_multiple_formulas_inconsistent") :proflog-expectation)))
    (is (= :direct (:coverage (entry "test_multiple_formulas_inconsistent"))))))

(deftest literature-examples-are-translated-faithfully
  (testing "Fitting examples retain the formulas that drive branch opening and closure."
    (is (= (not* (and* p q))
           (golden/formula-for-key :fitting-basic-expansion)))
    (is (open? (golden/formula-for-key :fitting-basic-expansion)))
    (is (= (and* p (not* p) q)
           (golden/formula-for-key :fitting-closure)))
    (is (closes? (golden/formula-for-key :fitting-closure)))
    (is (= (and* (or* p q) (or* (not* p) r))
           (golden/formula-for-key :fitting-satisfiable)))
    (is (open? (golden/formula-for-key :fitting-satisfiable))))

  (testing "Smullyan and Handbook tautology checks are represented as refutations of the source formula."
    (is (closes? (golden/refutation-formula-for-key :smullyan-systematic-tautology)))
    (is (open? (golden/formula-for-key :smullyan-completeness)))
    (is (open? (golden/formula-for-key :handbook-signed-conjunction)))
    (is (closes? (golden/refutation-formula-for-key :priest-deep-tautology))))

  (testing "Performance and CLI examples keep their actual formulas and expected status."
    (is (= (and* (or* p q) r)
           (golden/formula-for-key :performance-formula-prioritization)))
    (is (open? (golden/formula-for-key :performance-formula-prioritization)))
    (is (= (and* (implies* p q) p (not* q))
           (golden/formula-for-key :formula-modus-ponens-contradiction)))
    (is (closes? (golden/formula-for-key :formula-modus-ponens-contradiction)))))

(deftest runnable-proflog-observations-match-recorded-expectations
  (doseq [record (vals golden/translation-records)]
    (testing (:source-test record)
      (let [actual (golden/proflog-observation-for-translation record)]
        (is (= (:proflog-expectation record) actual))))))
