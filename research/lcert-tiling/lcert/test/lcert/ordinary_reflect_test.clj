(ns lcert.ordinary-reflect-test
  "Assessor's experiment (not part of jpt4/sjas): reflect extended from the
  base data types to every closed type in which neither R nor ◇ occurs and
  whose T-arguments contain no reflect.  The metatheory's model gives such a
  type the same semantic set at every budget and footprint, so the Reflect
  case of Lemma 3.6 goes through unchanged.  These tests exercise the type
  checker, Check, and both evaluators (Theorems 4 and 4′) on the extension."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.encode :as e]
            [lcert.typing :as t]
            [lcert.check :as c]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.core :as lc]))

(def nonzero-form
  "λx. x ≠ 0, as a Bool-valued primitive recursion."
  '(rec-nat [z Bool] ff [k y] tt x))

(def sigma-form
  "Σ(x :ω Nat). T(x ≠ 0): an existential whose default witness 0 is wrong."
  (list 'Sigma '[x w Nat] (list 'T nonzero-form)))

(def sigma-type (s/parse-type [] sigma-form))

(def pi-form
  "Π(x :ω Nat). T(allTrue x), where allTrue = recN(tt, k y. y, x) is always tt."
  '(Pi [x w Nat] (T (rec-nat [z Bool] tt [k y] y x))))

(def pi-type (s/parse-type [] pi-form))

(defn certificate [form]
  (let [{:keys [code nodes budget]} (lc/certify 0 form)]
    {:code code :nodes nodes :budget budget :lit (c/certificate-literal code)}))

(deftest closed-trust-term-types
  (testing "λ r e. reflect A r e types at A = Σ(x :ω Nat). T(x ≠ 0)"
    (let [d (lc/check-program 0 (list 'fn '[r 1 R]
                                      (list 'fn ['e 1 (list 'T (list 'chk '(print r) (list 'code sigma-form)))]
                                            (list 'reflect sigma-form 'r 'e))))]
      (is (= :Pi (first (:type d))))
      (testing "and Check accepts its encoded derivation"
        (is (true? (c/check (e/enc-deriv d) (e/enc-exp (:type d)))))))))

(deftest reflect-returns-the-certified-witness
  (let [{:keys [code nodes lit]} (certificate (list 'pair sigma-form 1 'star))
        prog [:let [:Nat] [:reflect sigma-type lit [:star]] [:var 1]]
        d (t/check-top nodes prog)]
    (testing "the certificate checks at Σ(x :ω Nat). T(x ≠ 0)"
      (is (true? (c/check code (e/enc-exp sigma-type)))))
    (testing "the erasing evaluator returns the certified witness 1, not the default 0"
      (is (= 1 (ev/eval-deriv d nodes {:erase? true}))))
    (testing "the non-erasing evaluator agrees (Theorem 4′)"
      (is (= 1 (ev/eval-deriv d nodes {:erase? false}))))
    (testing "below the cap, reflect falls back to the default witness 0"
      (is (= 0 (ev/eval-deriv d (dec nodes) {:erase? true}))))))

(deftest reflect-at-a-function-type
  (let [{:keys [nodes lit]} (certificate (list 'fn '[x w Nat]
                                               '(rec-nat [x (T (rec-nat [z Bool] tt [k y] y x))] star [k y] y x)))
        prog [:app [:reflect pi-type lit [:star]] [:succ [:succ [:succ [:zero]]]]]
        d (t/check-top nodes prog)]
    (testing "the reflected function is applied once and yields its evidence"
      (is (= :star (ev/eval-deriv d nodes {:erase? true})))
      (is (= :star (ev/eval-deriv d nodes {:erase? false}))))))

(deftest the-extension-keeps-its-exclusions
  (testing "a type mentioning R is rejected"
    (is (thrown? Exception (lc/check-program 0 '(fn [r 1 R] (fn [e 1 (T (chk (print r) (code (-o R Nat))))]
                                                              (reflect (-o R Nat) r e)))))))
  (testing "a type mentioning ◇ is rejected"
    (is (thrown? Exception (lc/check-program 0 '(fn [r 1 R] (fn [e 1 (T (chk (print r) (code (-o Dia Nat))))]
                                                              (reflect (-o Dia Nat) r e))))))))
