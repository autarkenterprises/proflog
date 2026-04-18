(ns proflog.equality-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.proof :as proof]))

(defn provable?
  "True when the greenfield kernel finds at least one closed tableau."
  [formula]
  (seq (kernel/prove formula 1)))

(defn not-provable?
  "True when the greenfield kernel finds no proof within the given bound."
  ([formula] (not-provable? formula 1))
  ([formula n]
   (empty? (kernel/prove formula n))))

(deftest free-constructor-clash-closes-equality
  (testing "distinct constructors cannot be equal in the free-constructor theory"
    (is (provable?
          (ast/eq-lit (ast/app-term 'zero)
                      (ast/app-term 'one))))
    (is (provable?
          (ast/eq-lit (ast/app-term 'zero)
                      (ast/app-term 'succ (ast/app-term 'zero)))))))

(deftest injectivity-and-eq-neq-closure-work-together
  (testing "same-head equalities bind inner free variables and can violate disequalities"
    (ast/nom x
      (is (provable?
            (ast/and-form
              (ast/eq-lit (ast/app-term 'succ (ast/var-term x))
                          (ast/app-term 'succ (ast/app-term 'a)))
              (ast/neq-lit (ast/var-term x) (ast/app-term 'a)))))
      (is (provable?
            (ast/and-form
              (ast/neq-lit (ast/var-term x) (ast/app-term 'a))
              (ast/eq-lit (ast/app-term 'succ (ast/var-term x))
                          (ast/app-term 'succ (ast/app-term 'a)))))))))

(deftest equality-supports-atom-congruence-on-the-branch
  (testing "equality bindings propagate into later atom closure checks"
    (ast/nom x
      (is (provable?
            (ast/and-form
              (ast/eq-lit (ast/var-term x) (ast/app-term 'a))
              (ast/and-form
                (ast/pos-lit (ast/app-term 'color (ast/var-term x)))
                (ast/neg-lit (ast/app-term 'color (ast/app-term 'a))))))))))

(deftest disequality-stays-open-until-it-is-violated
  (testing "symbolic disequalities remain open until later equalities force them false"
    (ast/nom x
      (is (not-provable?
            (ast/neq-lit (ast/app-term 'succ (ast/var-term x))
                         (ast/app-term 'succ (ast/app-term 'a)))))
      (is (provable?
            (ast/and-form
              (ast/neq-lit (ast/var-term x) (ast/app-term 'a))
              (ast/eq-lit (ast/var-term x) (ast/app-term 'a))))))))

(deftest ground-occurs-check-shape-closes-by-constructor-clash
  (testing "the ground shape a = f(a) is unsatisfiable in the free-constructor theory"
    (is (provable?
          (ast/eq-lit (ast/app-term 'a)
                      (ast/app-term 'f (ast/app-term 'a)))))))

(deftest cyclic-open-equality-fails-by-occurs-check
  (testing "a free variable cannot unify with a term that contains it"
    (ast/nom x
      (is (provable?
            (ast/eq-lit (ast/var-term x)
                        (ast/app-term 'f (ast/var-term x))))))))

(deftest unresolved-parameters-do-not-close-by-constructor-clash-alone
  (testing "an unresolved internal parameter stays open until some equality constrains it"
    (ast/nom p q
      (is (not-provable?
            (ast/eq-lit (ast/par-term p)
                        (ast/app-term 'zero))))
      (is (not-provable?
            (ast/eq-lit (ast/par-term p)
                        (ast/par-term q))))
      (is (provable?
            (ast/and-form
              (ast/eq-lit (ast/par-term p)
                          (ast/app-term 'zero))
              (ast/neq-lit (ast/par-term p)
                           (ast/app-term 'zero))))))))

(deftest equality-proof-tags-remain-inspectable
  (testing "equality closure leaves explicit proof tags for debugging"
    (ast/nom x
      (let [clash-proof (first
                          (kernel/prove
                            (ast/eq-lit (ast/app-term 'zero)
                                        (ast/app-term 'one))
                            1))
            atom-proof (first
                         (kernel/prove
                           (ast/and-form
                             (ast/eq-lit (ast/var-term x) (ast/app-term 'a))
                             (ast/and-form
                               (ast/pos-lit (ast/app-term 'color (ast/var-term x)))
                               (ast/neg-lit (ast/app-term 'color (ast/app-term 'a)))))
                           1))
            occurs-proof (first
                           (kernel/prove
                             (ast/eq-lit (ast/var-term x)
                                         (ast/app-term 'f (ast/var-term x)))
                             1))]
      (is (proof/contains-step? clash-proof 'free-close))
      (is (proof/contains-step? atom-proof 'eq-bind))
      (is (proof/contains-step? atom-proof 'close))
      (is (proof/contains-step? occurs-proof 'occurs-close))))))
