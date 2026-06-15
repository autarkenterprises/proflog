(ns proflog.dsjas-arithmetic-soundness-test
  "Executable corroboration of the standard-model soundness proof for the D_SJAS
   U-Grounding arithmetic primitives
   (docs/log/2026-06-14-dsjas-standard-model-soundness-proof.md).

   It checks the bit-level KBFS relations and the SJAS totalization wrappers
   against reference Clojure arithmetic over a range of values. This does not
   replace the proof (which cites KBFS for the bit-level core and argues the
   wrappers/bridge directly) -- it is a regression guard that would fail loudly
   if a primitive ever stopped computing the standard (totalized) value."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== fresh run run*]]
            [clojure.test :refer [deftest is testing]]
            [proflog.relational-arithmetic :as arith]
            proflog.kernel.willard-sjas-profile))

(defn- b [n] (arith/build-num n))

;; private SJAS wrappers under test
(def ^:private monuso @#'proflog.kernel.willard-sjas-profile/sjas-monuso)
(def ^:private divo*  @#'proflog.kernel.willard-sjas-profile/sjas-divo)
(def ^:private maxo   @#'proflog.kernel.willard-sjas-profile/sjas-maxo)
(def ^:private logo*  @#'proflog.kernel.willard-sjas-profile/sjas-logo)
(def ^:private powo   @#'proflog.kernel.willard-sjas-profile/sjas-powo)
(def ^:private rooto  @#'proflog.kernel.willard-sjas-profile/sjas-rooto)
(def ^:private counto @#'proflog.kernel.willard-sjas-profile/sjas-counto)

;; reference standard (totalized) functions
(defn- r-monus [a c] (max 0 (- a c)))
(defn- r-idiv  [a c] (if (zero? c) a (quot a c)))          ; Willard div0 = numerator
(defn- r-log2  [a]   (if (<= a 1) 0 (loop [n a e 0] (if (< n 2) e (recur (quot n 2) (inc e))))))
(defn- r-pow   [base e] (reduce * 1 (repeat e base)))
(defn- r-root  [x y] (cond (zero? y) x (zero? x) 0 :else (loop [o 1] (if (<= x (r-pow o y)) o (recur (inc o)))))) ; ceil(x^(1/y))
(defn- r-count [a w] (count (filter #(= 1 %) (take w (concat (arith/build-num a) (repeat 0))))))

(defn- holds? [g] (boolean (seq (run 1 [q] (fresh [] g (== q true))))))

(def ^:private vals [0 1 2 3 4 5 7 8 15 16 31])
(def ^:private small [0 1 2 3 4 5 8])

;; ---- 2. bit-level KBFS relations vs standard arithmetic ----

(deftest plus-mult-are-standard
  (doseq [a vals b' vals]
    (is (= (list (b (+ a b'))) (run* [k] (arith/pluso (b a) (b b') k)))
        (str "pluso " a "+" b'))
    (is (= (list (b (* a b'))) (run* [p] (arith/*o (b a) (b b') p)))
        (str "*o " a "*" b'))))

(deftest order-relations-are-standard
  (doseq [a vals b' vals]
    (is (= (< a b') (holds? (arith/<o (b a) (b b')))) (str "<o " a " " b'))
    (is (= (<= a b') (holds? (arith/<=o (b a) (b b')))) (str "<=o " a " " b'))))

(deftest minus-and-div-bit-level
  (doseq [a vals c vals]
    (testing "minuso is partial (defined iff a>=c) and standard"
      (is (= (if (>= a c) (list (b (- a c))) '())
             (run* [k] (arith/minuso (b a) (b c) k)))
          (str "minuso " a "-" c)))
    (testing "KBFS divo is euclidean for c>0"
      (when (pos? c)
        (is (= (list (b (quot a c)))
               (run* [q] (fresh [rem] (arith/divo (b a) (b c) q rem))))
            (str "divo " a "/" c))))))

;; ---- 3. SJAS totalization wrappers vs standard totalized functions ----

(deftest monus-wrapper-is-total-monus
  (doseq [a vals c vals]
    (is (= (list (b (r-monus a c))) (run* [o] (monuso (b a) (b c) o)))
        (str "monus " a " " c))))

(deftest div-wrapper-totalizes-div-by-zero
  (doseq [a vals c vals]
    (is (= (list (b (r-idiv a c))) (run* [o] (divo* (b a) (b c) o)))
        (str "div " a " " c " (div0=numerator)"))))

(deftest max-wrapper-is-standard-max
  (doseq [a vals c vals]
    (is (= (list (b (max a c))) (run* [o] (maxo (b a) (b c) o)))
        (str "max " a " " c))))

(deftest log-wrapper-is-floor-log2-totalized
  (doseq [a vals]
    (is (= (list (b (r-log2 a))) (run* [o] (logo* (b a) o)))
        (str "log2 " a))))

(deftest pow-wrapper-is-standard-power
  (doseq [base small e small]
    (is (= (list (b (r-pow base e))) (run* [o] (powo (b base) (b e) o)))
        (str base "^" e))))

(deftest root-wrapper-is-ceil-root-totalized
  ;; `rooto` generates its output by an unbounded `pluso`, so `run*` would chase
  ;; an infinite failing tail past the answer; the checker uses first-answer
  ;; semantics, so `run 1` is the faithful corroboration (the proof argues
  ;; uniqueness separately).
  (doseq [x [0 1 2 3 4 5 8 9 16 25 26] y [0 1 2 3]]
    (is (= (list (b (r-root x y))) (run 1 [o] (rooto (b x) (b y) o)))
        (str "root " x " " y))))

(deftest count-wrapper-is-bounded-popcount
  (doseq [a [0 1 2 3 5 7 11 21 31] w [0 1 2 3 4 5]]
    (is (= (list (b (r-count a w))) (run* [o] (counto (b a) (b w) o)))
        (str "count(" a ", width " w ")"))))
