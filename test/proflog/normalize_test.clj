(ns proflog.normalize-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.normalize :as normalize]))

(deftest to-nnf-eliminates-implication-and-pushes-negation-inward
  (testing "surface implication and negation compile to tagged NNF"
    (ast/nom x
      (is (= (ast/and-form
               (ast/pos-lit (ast/app-term 'warm (ast/var-term x)))
               (ast/neg-lit (ast/app-term 'cool (ast/var-term x))))
             (normalize/to-nnf
               (ast/not-form
                 (ast/implies-form
                   (ast/pos-lit (ast/app-term 'warm (ast/var-term x)))
                   (ast/pos-lit (ast/app-term 'cool (ast/var-term x)))))))))))

(deftest to-nnf-handles-quantifier-duality-and-equality-negation
  (testing "negated quantifiers and equality switch to their dual NNF forms"
    (ast/nom x
      (is (= (ast/exists-form
               x
               (ast/neq-lit (ast/var-term x) (ast/app-term 'zero)))
             (normalize/to-nnf
               (ast/not-form
                 (ast/forall-form
                   x
                   (ast/eq-lit (ast/var-term x) (ast/app-term 'zero)))))))
      (is (= (ast/eq-lit (ast/app-term 'zero) (ast/app-term 'one))
             (normalize/negate-formula
               (ast/neq-lit (ast/app-term 'zero) (ast/app-term 'one))))))))
