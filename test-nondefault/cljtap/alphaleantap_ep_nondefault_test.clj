;; ============================================================================
;; αleanTAP-EP Non-Default Test Suite
;; ============================================================================
;;
;; These tests are intentionally excluded from routine `lein test` because they
;; are the kind of larger finite-domain proofs that stress the current generic
;; prover hard enough to dominate normal edit/verify loops.
;;
;; Run them explicitly with:
;;   lein test-nondefault
;;
;; The goal of perf-lab-review is to make these kinds of tests pass as genuine
;; tests rather than leaving them only as ad hoc benchmark scripts.

(ns cljtap.alphaleantap-ep-nondefault-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :refer :all :rename {is l-is, appendo logic-appendo, membero logic-membero}]
            [clojure.core.logic.nominal :refer [tie hash]]
            [cljtap.alphaleantap-ep :refer :all]
            [cljtap.gv-assoc :as gv]))

;; ---------------------------------------------------------------------------
;; Section GV (Non-Default): Z4 Group Axioms
;; ---------------------------------------------------------------------------
;;
;; Z4 is the smallest domain on this branch where the finite-table GV encodings
;; stop being lightweight smoke tests and start exercising the prover as a real
;; performance workload.  Keeping them in a separate suite preserves routine
;; feedback time without losing the higher-bar coverage entirely.

(deftest test-GV17-z4-assoc-precomputed-neg-call
  (testing "GV17: Z4 is associative under the precomputed 3-universal encoding.
            This preserves the historical Z4 stress case referenced in the
            performance review notes."
    (is (seq
          (run 1 [proof]
            (nom x y z
              (let [prog (gv/gv-assoc-precomputed-program gv/gv-z4 x y z)]
                (proveo ['neg ['app 'gv_assoc_pre]]
                        '() '() '() prog proof))))))))

(deftest test-GV21-z4-identity
  (testing "GV21: Z4 has identity element 0."
    (is (seq
          (run 1 [proof]
            (nom x
              (let [prog (gv/gv-identity-program gv/gv-z4 x)]
                (proveo ['neg ['app 'gv_identity]]
                        '() '() '() prog proof))))))))

(deftest test-GV22-z4-closure
  (testing "GV22: Z4 is closed under its operation."
    (is (seq
          (run 1 [proof]
            (nom x y z
              (let [prog (gv/gv-closure-program gv/gv-z4 x y z)]
                (proveo ['neg ['app 'gv_closure]]
                        '() '() '() prog proof))))))))

(deftest test-GV23-z4-inverses
  (testing "GV23: Every element of Z4 has an inverse."
    (is (seq
          (run 1 [proof]
            (nom x y
              (let [prog (gv/gv-inverses-program gv/gv-z4 x y)]
                (proveo ['neg ['app 'gv_inverses]]
                        '() '() '() prog proof))))))))
