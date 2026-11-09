(ns proflog.fitting.query-test
  "The optional parallel runner (ADR-0149 revision 7): `queryo` with the
  status fixed to :succeeds on one thread and to :fails on another, with
  cancellation and timeouts."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.fitting.programs :as programs]
            [proflog.fitting.query :as query]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau-test :refer [win-3-fails]]))

(def P2 (syntax/program (syntax/language programs/L2) programs/P2))
(def move (syntax/program (syntax/language programs/L-move) programs/move-program))

(deftest the-first-side-to-close-answers
  (is (= {:status :fails :certificate win-3-fails}
         (query/race P2 '(win (s (s (s 0)))) {})))
  (is (= :succeeds (:status (query/race P2 '(win (s 0)) {})))))

(deftest fuel-bounds-both-sides
  (is (= {:status :unresolved :reason :exhausted :fuel 3}
         (query/race P2 '(win (s (s (s 0)))) {:fuel 3}))))

(deftest a-timeout-stops-both-sides
  ;; win(0) under the factored program is ⊥, so neither side closes.
  (is (= {:status :unresolved :reason :timeout}
         (query/race move '(win 0) {:timeout-ms 500})))
  (is (= {:status :unresolved :reason :timeout :fuel 40}
         (query/race move '(win 0) {:fuel 40 :timeout-ms 500}))))

(deftest the-both-option-searches-both-sides
  ;; Lemma 7.5 and Theorem 7.2: at most one side closes. :both waits for the
  ;; second side to end, so it needs a bound.
  (is (= :succeeds (:status (query/race P2 '(win (s 0)) {:fuel 6 :both true}))))
  (testing "and with no closed tableau on either side, the answer is still exhausted"
    (is (= {:status :unresolved :reason :exhausted :fuel 4}
           (query/race move '(win 0) {:fuel 4 :both true})))))
