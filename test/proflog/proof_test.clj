(ns proflog.proof-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.proof :as proof]))

(deftest proof-terms-record-the-major-tableau-steps
  (testing "proof terms expose the structural steps used by the base kernel"
    (let [conj-proof (first
                       (kernel/prove
                         (ast/and-form
                           (ast/pos-lit (ast/app-term 'p))
                           (ast/neg-lit (ast/app-term 'p)))
                         1))
          split-proof (first
                        (kernel/prove
                          (ast/or-form
                            (ast/and-form
                              (ast/pos-lit (ast/app-term 'p))
                              (ast/neg-lit (ast/app-term 'p)))
                            (ast/and-form
                              (ast/pos-lit (ast/app-term 'q))
                              (ast/neg-lit (ast/app-term 'q))))
                          1))]
      (is (proof/contains-step? conj-proof 'conj))
      (is (proof/contains-step? conj-proof 'close))
      (is (proof/contains-step? split-proof 'split)))))

(deftest quantifier-proof-steps-are-distinguishable
  (testing "universal and existential work leave distinct proof tags"
    (ast/nom x
      (let [univ-proof (first
                         (kernel/prove
                           (ast/and-form
                             (ast/forall-form x
                                              (ast/pos-lit
                                                (ast/app-term 'value (ast/var-term x))))
                             (ast/neg-lit (ast/app-term 'value (ast/app-term 'zero))))
                           1))
            witness-proof (first
                            (kernel/prove
                              (ast/exists-form
                                x
                                (ast/and-form
                                  (ast/pos-lit (ast/app-term 'value (ast/var-term x)))
                                  (ast/neg-lit (ast/app-term 'value (ast/var-term x)))))
                              1))]
        (is (proof/contains-step? univ-proof 'univ))
        (is (proof/contains-step? witness-proof 'witness))))))
