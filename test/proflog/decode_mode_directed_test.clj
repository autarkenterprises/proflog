(ns proflog.decode-mode-directed-test
  "ADR-0110 (#1, ground-before-decode). The formula/term byte decoders must be
   *mode-directed*: with a ground formula they run the decode forward (the known
   structure drives the byte computation) and terminate deterministically, not
   enumerate. This is achieved purely, by conjunction ordering -- moving each
   branch's constructor `==` ahead of the recursive byte-decodes -- so the answer
   set is unchanged (round-trip agreement) while the *backward* mode (ground
   formula -> bytes), which did not terminate before, now completes.

   Before the reorder, the backward `run 1` here does not complete in 70s (a
   timed probe killed it); the forward mode is unaffected (~1 ms)."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l :refer [== run run*]]
            [clojure.test :refer [deftest is testing]]
            proflog.kernel.willard-sjas-profile))

(def ^:private decode-formula-byteso
  @#'proflog.kernel.willard-sjas-profile/decode-formula-byteso)

(def ^:private f1
  "0 = 0, encoded as bytes (eq-tag 5)(nat-tag 25, len 0 0)(nat-tag 25, len 0 0)."
  '(eq (num ()) (num ())))

(def ^:private f1-bytes '(5 25 0 0 25 0 0))

(def ^:private f2 (list 'and f1 f1))

(deftest decode-is-mode-directed
  (testing "forward (ground bytes -> formula) is unchanged"
    (is (= (list f1) (run* [f] (decode-formula-byteso '() f1-bytes '() f)))))
  (testing "backward (ground formula -> bytes) terminates deterministically"
    ;; run* must terminate (full forward computation) with the unique encoding.
    (is (= (list f1-bytes) (run* [b] (decode-formula-byteso '() b '() f1)))))
  (testing "round-trip agreement on a recursive (and ...) formula"
    (let [bytes (first (run 1 [b] (decode-formula-byteso '() b '() f2)))]
      (is (some? bytes) "backward yields an encoding")
      (is (= (list f2)
             (run* [f] (decode-formula-byteso '() (apply list bytes) '() f)))
          "forward-decoding that encoding returns the original formula"))))
