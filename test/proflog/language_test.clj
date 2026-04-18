(ns proflog.language-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.language :as language]))

(def simple-language
  (language/language
    {:constants ['zero 'one]
     :functions {'succ 1}
     :relations {'even 1
                 'odd 1
                 'value 1}}))

(deftest language-rejects-undeclared-and-mismatched-symbols
  (testing "queries using undeclared relations, undeclared functions, or wrong arities are rejected"
    (ast/nom x
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Undeclared relation symbol: mystery"
            (language/validate-query
              simple-language
              (ast/pos-lit (ast/app-term 'mystery (ast/var-term x))))))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Undeclared function symbol: weird"
            (language/validate-query
              simple-language
              (ast/pos-lit (ast/app-term 'even (ast/app-term 'weird))))))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Arity mismatch for relation symbol even"
            (language/validate-query
              simple-language
              (ast/pos-lit (ast/app-term 'even (ast/var-term x) (ast/app-term 'zero)))))))))

(deftest compile-program-desugars-multiple-clauses-into-one-core-clause
  (testing "multiple surface clauses for the same relation become one compiled clause with an OR body"
    (ast/nom x y
      (let [program (language/compile-program
                      simple-language
                      [(ast/clause 'value [x]
                                   (ast/eq-lit (ast/var-term x) (ast/app-term 'zero)))
                       (ast/clause 'value [y]
                                   (ast/eq-lit (ast/var-term y) (ast/app-term 'one)))])
            compiled (get-in program [:clauses 'value])]
        (is (= 'value (:relation compiled)))
        (is (= 1 (count (:params compiled))))
        (is (= (ast/or-form
                 (ast/eq-lit (ast/var-term (first (:params compiled)))
                             (ast/app-term 'zero))
                 (ast/eq-lit (ast/var-term (first (:params compiled)))
                             (ast/app-term 'one)))
               (:body compiled)))))))

(deftest compile-program-rejects-par-in-surface-programs
  (testing "internal parameters are not admissible in user programs"
    (ast/nom x p
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Internal parameter terms are not admissible in surface programs"
            (language/compile-program
              simple-language
              [(ast/clause 'value [x]
                           (ast/eq-lit (ast/var-term x) (ast/par-term p)))]))))))
