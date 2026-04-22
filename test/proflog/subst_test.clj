(ns proflog.subst-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :refer [run* ==]]
            [proflog.ast :as ast]
            [proflog.subst :as subst]))

(deftest subst-termo-replaces-vars-and-preserves-pars
  (testing "term substitution replaces tagged vars and preserves internal parameters"
    (ast/nom x p
      (is (= (ast/app-term 'zero)
             (subst/subst-term
               (ast/var-term x)
               (list [x (ast/app-term 'zero)]))))
      (is (= [true]
             (run* [q]
               (== q true)
               (subst/subst-termo
                 (ast/var-term x)
                 (list [x (ast/app-term 'zero)])
                 (ast/app-term 'zero)))))
      (is (= (ast/par-term p)
             (subst/subst-term
               (ast/par-term p)
               (list [x (ast/app-term 'zero)]))))
      (is (= [true]
             (run* [q]
               (== q true)
               (subst/subst-termo
                 (ast/par-term p)
                 (list [x (ast/app-term 'zero)])
                 (ast/par-term p))))))))

(deftest subst-formulao-respects-binding-and-shadowing
  (testing "substitution does not replace occurrences protected by a quantifier binder"
    (ast/nom x y
      (let [formula (ast/forall-form
                      x
                      (ast/and-form
                        (ast/pos-lit (ast/app-term 'value (ast/var-term x)))
                        (ast/pos-lit (ast/app-term 'value (ast/var-term y)))))
            expected (ast/forall-form
                       x
                       (ast/and-form
                         (ast/pos-lit (ast/app-term 'value (ast/var-term x)))
                         (ast/pos-lit (ast/app-term 'value (ast/app-term 'one)))))]
        (is (= [expected]
               [(subst/subst-formula
                  formula
                  (list [x (ast/app-term 'zero)]
                        [y (ast/app-term 'one)]))]))
        (is (= [true]
               (run* [q]
                 (== q true)
                 (subst/subst-formulao
                   formula
                   (list [x (ast/app-term 'zero)]
                         [y (ast/app-term 'one)])
                   expected))))))))

(deftest subst-formulao-supports-once-forall-bodies
  (testing "forward substitution also threads through the internal single-use universal form"
    (ast/nom x y z
      (let [formula (ast/once-forall-form
                      x
                      (ast/or-form
                        (ast/neq-lit (ast/var-term y) (ast/var-term x))
                        (ast/neg-lit (ast/app-term 'p (ast/var-term z)))))
            expected (ast/once-forall-form
                       x
                       (ast/or-form
                         (ast/neq-lit (ast/app-term 'zero) (ast/var-term x))
                         (ast/neg-lit (ast/app-term 'p (ast/app-term 'one)))))]
        (is (= expected
               (subst/subst-formula
                 formula
                 (list [y (ast/app-term 'zero)]
                       [z (ast/app-term 'one)]))))
        (is (= [true]
               (run* [q]
                 (== q true)
                 (subst/subst-formulao
                   formula
                   (list [y (ast/app-term 'zero)]
                         [z (ast/app-term 'one)])
                   expected))))))))
