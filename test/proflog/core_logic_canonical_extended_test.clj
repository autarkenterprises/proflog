(ns proflog.core-logic-canonical-extended-test
  "ADR-0093 extended miniKanren/core.logic conformance tests.

   These tests carry the literature-derived programs that are valuable for
   implementation assessment but too slow or too puzzle-like for the fast gate:
   quines/twines through a relational interpreter, CLP(FD) cryptarithmetic and
   8-queens, and pure miniKanren binary arithmetic run backward."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l
             :refer [!= == everyg fresh run run*]]
            [clojure.core.logic.fd :as fd]
            [clojure.test :refer [deftest is testing]]
            [proflog.core-logic-canonical-test :as canonical]
            [proflog.relational-arithmetic :as arith]))

(defn- paper-twine-programs
  "The first twine shape reported in Byrd/Holk/Friedman's
   \"miniKanren, Live and Untagged\", translated to the tiny Clojure-data
   language used by `canonical/tiny-evalo`.

   `p` is a quoted program that evaluates to `q`; `q` evaluates back to `p`.
   The binder is a concrete symbol here because this extended regression checks
   the executable pearl, while the fast suite checks residual binder synthesis."
  [binder]
  (let [body (list 'list
                   (list 'quote 'quote)
                   (list 'list binder
                         (list 'list (list 'quote 'quote) binder)))
        fn-expr (list 'lambda (list binder) body)
        q (list fn-expr (list 'quote fn-expr))
        p (list 'quote q)]
    [p q]))

(defn- paper-twineo
  [binder p q]
  (fresh [body fn-expr]
    (!= binder 'closure)
    (!= binder 'lambda)
    (!= binder 'list)
    (!= binder 'quote)
    (== (list 'list
              (list 'quote 'quote)
              (list 'list binder
                    (list 'list (list 'quote 'quote) binder)))
        body)
    (== (list 'lambda (list binder) body) fn-expr)
    (== (list fn-expr (list 'quote fn-expr)) q)
    (== (list 'quote q) p)))

(defn- send-more-moneyo
  [digits]
  (fresh [s e n d m o r y]
    (== [s e n d m o r y] digits)
    (fd/in s e n d m o r y (fd/interval 0 9))
    (fd/distinct digits)
    (fd/!= s 0)
    (fd/!= m 0)
    (fd/eq
      (= (+ (* 1000 s) (* 100 e) (* 10 n) d
            (* 1000 m) (* 100 o) (* 10 r) e)
         (+ (* 10000 m) (* 1000 o) (* 100 n) (* 10 e) y)))))

(defn- queen-safe-pairo
  "Queens are represented as one column per row. Pair safety is encoded with
   finite-domain arithmetic instead of host projection: columns differ, and the
   two diagonal sums differ."
  [left-row left-col right-row right-col]
  (fresh [left-major right-major left-minor right-minor]
    (fd/+ left-col left-row left-major)
    (fd/+ right-col right-row right-major)
    (fd/!= left-major right-major)
    (fd/+ left-col right-row left-minor)
    (fd/+ right-col left-row right-minor)
    (fd/!= left-minor right-minor)))

(defn- all-queen-pairs-safeo
  [indexed-cols]
  (if (empty? indexed-cols)
    l/s#
    (let [[[row col] & rest-cols] indexed-cols]
      (l/all
        (everyg (fn [[other-row other-col]]
                  (queen-safe-pairo row col other-row other-col))
                rest-cols)
        (all-queen-pairs-safeo rest-cols)))))

(defn- eight-queenso
  [cols]
  (fresh [a b c d e f g h]
    (== [a b c d e f g h] cols)
    (fd/in a b c d e f g h (fd/interval 1 8))
    (fd/distinct cols)
    (all-queen-pairs-safeo [[1 a] [2 b] [3 c] [4 d]
                            [5 e] [6 f] [7 g] [8 h]])))

(defn- valid-eight-queens?
  [cols]
  (and (= 8 (count cols))
       (= (set (range 1 9)) (set cols))
       (every?
         (fn [[i left]]
           (every?
             (fn [[j right]]
               (or (= i j)
                   (not= (abs (- left right))
                         (abs (- i j)))))
             (map-indexed vector cols)))
         (map-indexed vector cols))))

(deftest ^{:slow true
           :expected-duration-ms 5000}
  live-untagged-quine-and-twine-pearls
  (testing "the documented quine shape evaluates to itself"
    (let [program canonical/quine-program]
      (is (= (list program)
             (run 1 [q]
               (== q program)
               (canonical/tiny-evalo q '() q))))))
  (testing "the first documented twine pair evaluates in both directions"
    (let [[p q] (paper-twine-programs 'a)]
      (is (not= p q))
      (is (= (list q)
             (run 1 [out]
               (canonical/tiny-evalo p '() out))))
      (is (= (list p)
             (run 1 [out]
               (canonical/tiny-evalo q '() out))))))
  (testing "the twine shape can still be checked relationally"
    (let [[answer] (run 1 [out]
                     (fresh [binder p q]
                       (paper-twineo binder p q)
                       (canonical/tiny-evalo p '() q)
                       (canonical/tiny-evalo q '() p)
                       (== [p q] out)))]
      (is (seq answer))
      (is (not= (first answer) (second answer))))))

(deftest ^{:slow true
           :expected-duration-ms 5000}
  send-more-money-cryptarithmetic
  (testing "the classic CLP(FD) SEND + MORE = MONEY puzzle has its unique solution"
    (is (= '([9 5 6 7 1 0 8 2])
           (run* [q]
             (send-more-moneyo q))))))

(deftest ^{:slow true
           :expected-duration-ms 60000}
  eight-queens-fd-counts-all-solutions
  (testing "the classic 8-queens CLP(FD) puzzle enumerates all 92 solutions"
    (let [answers (doall
                    (run* [q]
                      (eight-queenso q)))]
      (is (= 92 (count answers)))
      (is (every? valid-eight-queens? answers)))))

(deftest ^{:slow true
           :expected-duration-ms 5000}
  pure-relational-binary-arithmetic-factorization
  (testing "miniKanren binary multiplication can run backward to factor 30"
    (let [n30 (arith/build-num 30)
          expected #{[(arith/build-num 1) n30]
                     [n30 (arith/build-num 1)]
                     [(arith/build-num 2) (arith/build-num 15)]
                     [(arith/build-num 15) (arith/build-num 2)]
                     [(arith/build-num 3) (arith/build-num 10)]
                     [(arith/build-num 10) (arith/build-num 3)]
                     [(arith/build-num 5) (arith/build-num 6)]
                     [(arith/build-num 6) (arith/build-num 5)]}]
      (is (= expected
             (set
               (run* [q]
                 (fresh [x y]
                   (arith/*o x y n30)
                   (== [x y] q)))))))))
