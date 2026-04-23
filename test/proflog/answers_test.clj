(ns proflog.answers-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.list-programs-test :as lp]
            [proflog.recursive-synthesis-test :as rst]))

(def answer-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'p 1
                 'dup 1
                 'even 1
                 'odd 1
                 'win 1}}))

(defn numeral
  [n]
  (if (zero? n)
    (ast/app-term 'zero)
    (ast/app-term 's (numeral (dec n)))))

(defn simple-program
  []
  (ast/nom x
    (language/compile-program
      answer-language
      [(ast/clause 'p [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'zero)))])))

(defn nim-program
  []
  (ast/nom x y
    (language/compile-program
      answer-language
      [(ast/clause 'win [x]
                   (ast/exists-form y
                                    (ast/and-form
                                      (ast/or-form
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's (ast/var-term y)))
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's
                                                                  (ast/app-term 's
                                                                                (ast/var-term y)))))
                                      (ast/neg-lit (ast/app-term 'win (ast/var-term y))))))])))

(defn duplicate-answer-program
  []
  (ast/nom x
    (language/compile-program
      answer-language
      [(ast/clause 'dup [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'zero)))
       (ast/clause 'dup [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'zero)))
       (ast/clause 'dup [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 's (ast/app-term 'zero))))])))

(defn answer-terms
  [records]
  (mapv (fn [record]
          (-> record :bindings first second))
        records))

(defn self-contradictory-neq?
  [formula]
  (and (= 'neq (ast/tag-of formula))
       (= (second formula) (nth formula 2))))

(deftest bounded-ground-enumerator-follows-constructor-depth
  (testing "ground term generation stays inside the declared language and depth bound"
    (is (= [(numeral 0) (numeral 1) (numeral 2) (numeral 3)]
           (answers/ground-terms-up-to-depth answer-language 3)))))

(deftest generic-query-answers-export-symbolic-bindings
  (testing "the generic answer API exports direct substitutions without bounded term enumeration"
    (ast/nom x
      (let [records (answers/query-answers
                      (simple-program)
                      (ast/pos-lit (ast/app-term 'p (ast/var-term x)))
                      [x]
                      {:proof-limit 4})]
        (is (= [(numeral 0)]
               (answer-terms records)))
        (is (= [] (:residuals (first records))))))))

(deftest query-answers-collect-unique-answers-beyond-duplicate-proof-paths
  (testing "duplicate proofs for one answer do not starve later distinct answers"
    (ast/nom x
      (let [records (answers/query-answers
                      (duplicate-answer-program)
                      (ast/pos-lit (ast/app-term 'dup (ast/var-term x)))
                      [x]
                      {:proof-limit 2})
            answer-terms (answer-terms records)]
        (is (= [(numeral 0) (numeral 1)]
               answer-terms))
        (is (not-any? self-contradictory-neq?
                      (mapcat :residuals records)))
        (is (= []
               (:residuals (first records))))
        (is (every? (fn [residual]
                      (= (ast/neq-lit (numeral 1) (numeral 0))
                         residual))
                    (:residuals (second records))))))))

(deftest query-answer-diagnostics-reports-raw-vs-unique-growth
  (testing "diagnostics show duplicate raw proof paths before later unique answers surface"
    (ast/nom x
      (let [snapshots (answers/query-answer-diagnostics
                        (duplicate-answer-program)
                        (ast/pos-lit (ast/app-term 'dup (ast/var-term x)))
                        [x]
                        {:raw-limits [1 2 4]
                         :sample-limit 2})
            first-snapshot (first snapshots)
            last-snapshot (last snapshots)]
        (is (= [1 2 3]
               (mapv :raw-count snapshots)))
        (is (= 1
               (:unique-count first-snapshot)))
        (is (= 2
               (:unique-count last-snapshot)))
        (is (= [(numeral 0) (numeral 1)]
               (answer-terms (:sample-records last-snapshot))))))))

(deftest query-stage-diagnostics-summarize-proof-families
  (testing "stage diagnostics expose duplicate exported answers and proof-family summaries"
    (ast/nom x
      (let [stages (answers/query-stage-diagnostics
                     (duplicate-answer-program)
                     (ast/pos-lit (ast/app-term 'dup (ast/var-term x)))
                     [x]
                     {:call-depth 1
                      :raw-limits [4]
                      :sample-limit 2
                      :proof-sample-limit 2
                      :proof-step-limit 6})
            stage (second stages)
            snapshot (-> stage :snapshots first)]
        (is (= 1 (:stage stage)))
        (is (:productive? stage))
        (is (= 2 (:best-unique-count stage)))
        (is (= 1 (:duplicate-exported-count snapshot)))
        (is (= (:raw-count snapshot)
               (+ (:distinct-proof-signature-count snapshot)
                  (:duplicate-proof-signature-count snapshot))))
        (is (seq (:common-proof-signatures snapshot)))))))

(deftest generic-formula-answers-preserve-residual-disequalities
  (testing "symbolic answer export keeps residual neq constraints when the proof closes elsewhere"
    (ast/nom x
      (let [records (answers/formula-answers
                      answer-language
                      (ast/and-form
                        (ast/neq-lit (ast/var-term x) (numeral 1))
                        (ast/eq-lit (numeral 0) (numeral 1)))
                      [x]
                      {:proof-limit 4})
            residual-record (some (fn [record]
                                    (when (= [(ast/neq-lit (ast/var-term x) (numeral 1))]
                                             (:residuals record))
                                      record))
                                  records)]
        (is residual-record)
        (is (= (ast/var-term x)
               (answers/binding-term residual-record x)))))))

(deftest generic-query-answers-handle-a-recursive-open-query
  (testing "the symbolic answer path returns both a direct witness and a residual recursive obligation"
    (ast/nom x
      (let [records (answers/query-answers
                      (rst/recursive-parity-program)
                      (ast/pos-lit (ast/app-term 'even (ast/var-term x)))
                      [x]
                      {:proof-limit 2
                       :fuel 8
                       :call-depth 1})
            zero-record (some (fn [record]
                                (when (= (numeral 0)
                                         (answers/binding-term record x))
                                  record))
                              records)
            symbolic-record (some (fn [record]
                                    (let [term (answers/binding-term record x)
                                          residuals (:residuals record)]
                                      (when (and (= 'app (ast/tag-of term))
                                                 (= 's (second term))
                                                 (some (fn [residual]
                                                         (and (= 'neg (ast/tag-of residual))
                                                              (= 'odd (second (second residual)))))
                                                       residuals))
                                        record)))
                                  records)]
        (is zero-record)
        (is symbolic-record)))))

(deftest generic-query-answers-preserve-nim-call-obligations
  (testing "open win(x) exports a symbolic predecessor constraint plus a residual win obligation"
    (ast/nom x
      (let [records (answers/query-answers
                      (nim-program)
                      (ast/pos-lit (ast/app-term 'win (ast/var-term x)))
                      [x]
                      {:proof-limit 2
                       :fuel 8
                       :call-depth 1})
            record (first records)
            x-term (answers/binding-term record x)
            residual (first (:residuals record))]
        (is (= 'app (ast/tag-of x-term)))
        (is (= 's (second x-term)))
        (is (ast/var-term? (nth x-term 2)))
        (is (= [(ast/pos-lit (ast/app-term 'win (nth x-term 2)))]
               (:residuals record)))))))

(deftest query-answer-diagnostics-can-explain-a-recursive-symbolic-frontier
  (testing "diagnostics expose the first exported reverse frontier before deeper unfolding is attempted"
    (ast/nom r
      (let [input (lp/list-term (ast/app-term 'a)
                                (ast/app-term 'b))
            snapshots (answers/query-answer-diagnostics
                        (lp/list-program)
                        (ast/pos-lit (ast/app-term 'reverse input (ast/var-term r)))
                        [r]
                        {:raw-limits [1]
                         :fuel 32
                         :call-depth 1
                         :sample-limit 1})
            snapshot (first snapshots)
            record (first (:sample-records snapshot))]
        (is (= 1 (:raw-count snapshot)))
        (is (= 1 (:unique-count snapshot)))
        (is (= (lp/list-term)
               (answers/binding-term record r)))
        (is (= 3 (count (:residuals record))))))))

(deftest query-stage-diagnostics-distinguish-productive-and-dry-reverse-stages
  (testing "stage diagnostics show that reverse stays productive at depth 1 but goes dry at depth 2 for this fuel slice"
    (ast/nom r
      (let [input (lp/list-term (ast/app-term 'a)
                                (ast/app-term 'b))
            stages (answers/query-stage-diagnostics
                     (lp/list-program)
                     (ast/pos-lit (ast/app-term 'reverse input (ast/var-term r)))
                     [r]
                     {:call-depth 2
                      :raw-limits [1]
                      :fuel 32
                      :sample-limit 1
                      :proof-sample-limit 1
                      :proof-step-limit 8})
            stage-1 (nth stages 1)
            stage-2 (nth stages 2)
            snapshot-1 (-> stage-1 :snapshots first)
            snapshot-2 (-> stage-2 :snapshots first)]
        (is (:productive? stage-1))
        (is (= 1 (:best-unique-count stage-1)))
        (is (= 1 (:first-productive-raw-limit stage-1)))
        (is (not (:productive? stage-2)))
        (is (zero? (:best-unique-count stage-2)))
        (is (nil? (:first-productive-raw-limit stage-2)))
        (is (= 1 (:raw-count snapshot-1)))
        (is (zero? (:raw-count snapshot-2)))))))

(deftest query-answers-fall-back-to-the-last-productive-stage
  (testing "when a deeper unfolded stage goes dry, query-answers keeps the last productive frontier"
    (let [program (lp/list-program)
          input (lp/list-term (ast/app-term 'a)
                              (ast/app-term 'b))]
      (ast/nom r
        (let [depth-1 (answers/query-answers
                        program
                        (ast/pos-lit (ast/app-term 'reverse input (ast/var-term r)))
                        [r]
                        {:proof-limit 1
                         :max-raw-proof-limit 16
                         :fuel 32
                         :call-depth 1})
              depth-2 (answers/query-answers
                        program
                        (ast/pos-lit (ast/app-term 'reverse input (ast/var-term r)))
                        [r]
                        {:proof-limit 1
                         :max-raw-proof-limit 16
                         :fuel 32
                         :call-depth 2})]
          (is (= (mapv :bindings depth-1)
                 (mapv :bindings depth-2)))
          (is (= (mapv :residuals depth-1)
                 (mapv :residuals depth-2))))))))

(deftest query-answers-use-a-deeper-productive-stage-for-inverse-append
  (testing "inverse append reaches the first recursive split family at call-depth 2"
    (let [program (lp/list-program)
          abc (lp/list-term (ast/app-term 'a)
                            (ast/app-term 'b)
                            (ast/app-term 'c))]
      (ast/nom left right
        (let [records (answers/query-answers
                        program
                        (ast/pos-lit (ast/app-term 'append
                                                   (ast/var-term left)
                                                   (ast/var-term right)
                                                   abc))
                        [left right]
                        {:proof-limit 2
                         :max-raw-proof-limit 64
                         :fuel 16
                         :call-depth 2})]
          (is (= [[left (lp/list-term)]
                   [right abc]]
                 (:bindings (first records))))
          (is (= [[left (lp/list-term (ast/app-term 'a))]
                   [right (lp/list-term (ast/app-term 'b)
                                        (ast/app-term 'c))]]
                 (:bindings (second records)))))))))

(deftest bounded-open-query-generation-finds-first-small-nim-winner
  (testing "the non-generic bounded materializer still recovers the first winning Nim position"
    (ast/nom x
      (let [records (answers/query-ground-answers
                      (nim-program)
                      (ast/pos-lit (ast/app-term 'win (ast/var-term x)))
                      [x]
                      {:max-depth 4
                       :limit 1
                       :failure-timeout-ms 2000
                       :fuel 8})]
        (is (= [(numeral 1)]
               (answer-terms records)))))))
