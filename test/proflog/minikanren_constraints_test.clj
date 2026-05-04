(ns proflog.minikanren-constraints-test
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== fresh lcons run run*]]
            [clojure.test :refer [deftest is testing]]
            [proflog.minikanren-constraints :as mkc]))

(defn- residual-answer?
  [answer]
  (and (seq? answer)
       (some #{':-} answer)))

(deftest symbolo-accepts-symbols-and-rejects-non-symbols
  (testing "ground symbols pass"
    (is (= '(token)
           (run* [q]
             (mkc/symbolo q)
             (== q 'token)))))
  (testing "non-symbols fail"
    (is (= '()
           (run* [q]
             (mkc/symbolo q)
             (== q 12)))))
  (testing "open variables retain a delayed symbolic constraint"
    (let [answers (run 1 [q]
                    (mkc/symbolo q))]
      (is (= 1 (count answers)))
      (is (residual-answer? (first answers)))
      (is (some #{'symbolo} (flatten answers))))))

(deftest numbero-accepts-numbers-and-rejects-non-numbers
  (testing "ground numbers pass"
    (is (= '(12)
           (run* [q]
             (mkc/numbero q)
             (== q 12)))))
  (testing "non-numbers fail"
    (is (= '()
           (run* [q]
             (mkc/numbero q)
             (== q 'token)))))
  (testing "open variables retain a delayed numeric constraint"
    (let [answers (run 1 [q]
                    (mkc/numbero q))]
      (is (= 1 (count answers)))
      (is (residual-answer? (first answers)))
      (is (some #{'numbero} (flatten answers))))))

(deftest absento-accepts-terms-without-target-and-rejects-present-targets
  (testing "ground terms without the target pass"
    (is (= '(:ok)
           (run* [q]
             (mkc/absento 'intval '(call arg))
             (== q :ok)))))
  (testing "ground terms containing the target fail"
    (is (= '()
           (run* [q]
             (mkc/absento 'intval '(call intval))
             (== q :ok)))))
  (testing "the target may appear neither at the root nor in a discovered child"
    (is (= '()
           (run* [q]
             (mkc/absento 'intval q)
             (== q '(intval)))))
    (is (= '()
           (run* [q]
             (mkc/absento 'intval q)
             (== q '(call (intval))))))))

(deftest absento-delays-over-open-terms
  (testing "an entirely open term leaves a residual tree constraint"
    (let [answers (run 1 [q]
                    (mkc/absento 'intval q))]
      (is (= 1 (count answers)))
      (is (residual-answer? (first answers)))))
  (testing "an open tail keeps the absence check delayed after known safe structure"
    (let [answers (run 1 [q]
                    (fresh [tail]
                      (mkc/absento 'intval q)
                      (== (lcons 'call tail) q)))]
      (is (= 1 (count answers)))
      (is (residual-answer? (first answers)))))
  (testing "later discovery of a forbidden value fails"
    (is (= '()
           (run* [q]
             (fresh [tail]
               (mkc/absento 'intval q)
               (== (lcons 'call tail) q)
               (== (lcons 'intval '()) tail)))))))

(deftest absento-pushes-down-across-upstream-orderings
  (doseq [[label goal]
          [["push-down problems 2"
            (fn []
              (fresh [x a d]
                (mkc/absento 'intval x)
                (== 'intval a)
                (== (lcons a d) x)))]
           ["push-down problems 3"
            (fn []
              (fresh [x a d]
                (== (lcons a d) x)
                (mkc/absento 'intval x)
                (== 'intval a)))]
           ["push-down problems 4"
            (fn []
              (fresh [x a d]
                (== (lcons a d) x)
                (== 'intval a)
                (mkc/absento 'intval x)))]
           ["push-down problems 6"
            (fn []
              (fresh [x a d]
                (== 'intval a)
                (== (lcons a d) x)
                (mkc/absento 'intval x)))]
           ["push-down problems 1"
            (fn []
              (fresh [x a d]
                (mkc/absento 'intval x)
                (== (lcons a d) x)
                (== 'intval a)))]
           ["push-down problems 5"
            (fn []
              (fresh [x a d]
                (== 'intval a)
                (mkc/absento 'intval x)
                (== (lcons a d) x)))]]]
    (testing label
      (is (= '()
             (run* [q]
               (goal)))))))
