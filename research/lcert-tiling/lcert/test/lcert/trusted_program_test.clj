(ns lcert.trusted-program-test
  "Assessor's demo (not part of jpt4/sjas): what type-checks, and a
  non-trivial program that a parent trusts. These are the executable claims
  of the 2026-09-28 self-justification note, Addendum F6.

  Part A, the data layer: Ackermann's function, which is not primitive
  recursive, type-checks closed (budget 0). It needs recursion at a function
  type, and so R4's packaging of a reusable value as Σ(h :ω A). 1. Without
  the packaging it is rejected. A variant with System T's ω arrows also
  type-checks, once a number held at usage 1 is promoted by recursion.

  Part B, trust: a child certifies a controller f with a proof that f never
  outputs 0, for every time t. A parent receives the child's code as free
  data, together with a supply of tokens. It checks the code (free), mints
  the certificate from the supply (one token per node), checks the
  certificate, and reflects it. It then drives an actuator that demands
  evidence of safety, for ten steps, with no runtime checks. On any other
  input it falls back to a controller that is safe by computation.

  These tests pin existing behaviour (green when written). Mutation checks
  made them fail: see LOG.md, 2026-10-01."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [lcert.core :as lc]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.examples :as ex]))

(defn- rejects?
  "True iff checking `form` at budget n raises a type error."
  [n form]
  (try (t/check-top n (s/parse-term (s/token-scope n) form)) false
       (catch clojure.lang.ExceptionInfo e (= :lcert/type-error (:type (ex-data e))))))

;; ---------------------------------------------------------------------------
;; Part A: Ackermann's function.

(def ^:private bang
  "Σ(h :ω Nat ⊸ Nat). 1: a function packaged so that the recursion's result,
  which recN supplies at usage 1, can be unpacked at usage ω."
  '(Sigma [h w (-o Nat Nat)] Unit))

(def ^:private ack
  "ack : Π(m :ω Nat). Nat ⊸ Nat, with A(0) = succ and A(m+1)(n) =
  A(m)^(n+1)(1), by recursion on m at the packaged motive. Each level takes
  its argument at usage 1, since it uses it once, as a recursion's
  scrutinee: passing the usage-1 result g to an ω-function would scale g to
  ω, which the usage checker rejects."
  (walk/postwalk-replace
   {'BANG bang}
   '(fn [m w Nat]
      (let-pair (-o Nat Nat) [h u]
        (rec-nat [x BANG]
          (pair BANG (fn [n 1 Nat] (succ n)) star)
          [k p]
          (let-pair BANG [f u2] p
            (pair BANG (fn [n 1 Nat] (rec-nat [z Nat] (f 1) [j g] (f g) n)) star))
          m)
        h))))

(def ^:private promote
  "promote : Nat ⊸ Σ(v :ω Nat). 1. A number held at usage 1 is rebuilt by
  recursion on it from the predecessor, which recN binds at ω, and so is
  returned reusable."
  (walk/postwalk-replace
   {'BN '(Sigma [v w Nat] Unit)}
   '(fn [g 1 Nat] (rec-nat [x BN] (pair BN zero star) [k y] (pair BN (succ k) star) g))))

(def ^:private ack-omega
  "Ackermann with System T's arrows, Nat → Nat at usage ω. The usage-1
  result g of the inner recursion is promoted before f is applied to it."
  (walk/postwalk-replace
   {'BANG '(Sigma [h w (-> Nat Nat)] Unit) 'PROMOTE promote}
   '(fn [m w Nat]
      (let-pair (-> Nat Nat) [h u]
        (rec-nat [x BANG]
          (pair BANG (fn [n w Nat] (succ n)) star)
          [k p]
          (let-pair BANG [f u2] p
            (pair BANG (fn [n w Nat] (rec-nat [z Nat] (f 1)
                                              [j g] (let-pair Nat [v u3] (PROMOTE g) (f v))
                                              n))
                  star))
          m)
        h))))

(def ^:private ack-unpackaged
  "The same recursion with the plain motive Nat ⊸ Nat: its step uses the
  usage-1 result f twice, once inside a nested recursion's step."
  '(fn [m w Nat] (rec-nat [x (-o Nat Nat)] (fn [n 1 Nat] (succ n))
                          [k f] (fn [n 1 Nat] (rec-nat [z Nat] (f 1) [j g] (f g) n)) m)))

(deftest ackermann-type-checks-closed
  (testing "Ackermann's function is a closed program (budget 0)"
    (is (not (rejects? 0 ack)))
    (is (= 0 (:budget (lc/certify 0 ack)))))
  (testing "it computes A(2,3) = 9 and A(3,3) = 61"
    (is (= 9 (lc/run 0 (list ack 2 3))))
    (is (= 61 (lc/run 0 (list ack 3 3)))))
  (testing "with System T's ω arrows, after promoting the usage-1 result"
    (is (= [:pv 7 :star] (lc/run 0 (list promote 7))))
    (is (= 61 (lc/run 0 (list ack-omega 3 3)))))
  (testing "without the packaging, the recursion's result is used twice: rejected"
    (is (rejects? 0 ack-unpackaged))))

;; ---------------------------------------------------------------------------
;; Part B: a controller with a proof, trusted at runtime.

(defn- nz "The Boolean x ≠ 0." [x] (list 'rec-nat '[z Bool] 'ff '[k y] 'tt x))

(def ^:private dbl
  "dbl : Nat ⊸ Nat, doubling."
  '(fn [a 1 Nat] (rec-nat [z Nat] zero [k y] (succ (succ y)) a)))

(def ^:private pow2
  "pow2 : Nat → Nat, t ↦ 2^t. Its step doubles the recursion's result."
  (list 'fn '[t w Nat] (list 'rec-nat '[z Nat] 1 '[k y] (list dbl 'y) 't)))

(def ^:private dbl-nz
  "Lemma: Π(a :ω Nat). T(a ≠ 0) ⊸ T(dbl a ≠ 0), by cases on a. At 0 the
  hypothesis has type T(ff) ≡ 0; at a successor the goal computes to T(tt)."
  (list 'fn '[a w Nat]
        (list 'rec-nat ['x (list '-o (list 'T (nz 'x)) (list 'T (nz (list dbl 'x))))]
              (list 'fn ['e 1 (list 'T (nz 'zero))] (list 'abort (list 'T (nz (list dbl 'zero))) 'e))
              '[j y]
              (list 'fn ['e 1 (list 'T (nz '(succ j)))] 'star)
              'a)))

(def ^:private pow2-safe
  "Theorem: Π(t :ω Nat). T(2^t ≠ 0), by induction on t. 2^(k+1) is dbl 2^k,
  which no conversion evaluates for an open k, so the step needs the
  induction hypothesis and the lemma."
  (list 'fn '[t w Nat]
        (list 'rec-nat ['x (list 'T (nz (list pow2 'x)))]
              'star
              '[k y]
              (list (list dbl-nz (list pow2 'k)) 'y)
              't)))

(def ^:private controller
  "Controller = Σ(f :ω Nat → Nat). Σ(p :ω Π(t :ω Nat). T(f t ≠ 0)). 1: a
  controller with a proof that it is safe at every time. It has no R, ◇ or
  reflect, so the default reflect class admits it."
  (list 'Sigma ['f 'w '(-> Nat Nat)]
        (list 'Sigma ['p 'w (list 'Pi '[t w Nat] (list 'T (nz '(f t))))] 'Unit)))

(defn- package "The Controller term built from f and its proof." [f proof]
  (list 'pair controller f
        (list 'pair (list 'Sigma ['p 'w (list 'Pi '[t w Nat] (list 'T (nz (list f 't))))] 'Unit)
              proof 'star)))

(def ^:private child "The child: 2^t, with pow2-safe." (package pow2 pow2-safe))

(def ^:private fallback
  "The parent's own controller, t ↦ 1, safe by computation."
  (package '(fn [t w Nat] 1) '(fn [t w Nat] star)))

(def ^:private false-child
  "The identity controller with a claimed proof: false at t = 0."
  (package '(fn [t w Nat] t) '(fn [t w Nat] star)))

(def ^:private parent
  "parent : Π(c :ω Syn). R ⊸ Controller. Check the code as data, which is
  free. If it checks, mint it from the supply with the typed parser (one
  supply node per certificate node), check the certificate with inspect,
  and reflect it. Otherwise use the fallback."
  (list 'fn '[c w Syn]
        (list 'fn '[s 1 R]
              (list 'if (list 'chk 'c (list 'code controller))
                    (list 'let-pair controller '[t rest] (list ex/parse-prim-form 'c 's)
                          (list 'inspect controller 't (list 'code controller)
                                '[x e] (list 'reflect controller 'x 'e)
                                '[x e] fallback))
                    fallback))))

(def ^:private act
  "The actuator: it accepts a command only with evidence that it is safe."
  (list 'fn '[a w Nat] (list 'fn ['e 'w (list 'T (nz 'a))] 'a)))

(def ^:private plus
  "R4 §6.2's PLUS: its first argument at usage 1, since run10 passes it the
  recursion's result, which recN supplies at usage 1."
  '(fn [a 1 Nat] (fn [b w Nat] (rec-nat [z Nat] a [j y2] (succ y2) b))))

(def ^:private run10
  "Drive the actuator for times 0 … 9 and sum the commands executed. Each
  call is typed only because p supplies the evidence T(f t ≠ 0)."
  (list 'fn ['c 1 controller]
        (list 'let-pair 'Nat '[f q] 'c
              (list 'let-pair 'Nat '[p u] 'q
                    (list 'rec-nat '[x Nat] 'zero '[k y]
                          (list (list plus 'y) (list (list act '(f k)) '(p k)))
                          10)))))

(def ^:private driver
  "Π(c :ω Syn). R ⊸ Nat: the parent, then ten actuator steps."
  (list 'fn '[c w Syn] (list 'fn '[s 1 R] (list run10 (list (list parent 'c) 's)))))

(defn- drive
  "Evaluate the closed driver under cap n on a code and a lazy supply of n
  tokens. Returns the sum and the number of supply nodes materialized."
  [code n]
  (let [mat (atom 0)
        d (t/check-top 0 (s/parse-term [] driver))]
    (binding [ev/*materialized* mat]
      (let [f (#'ev/ev (ev/erase d) [] n {:erase? true})]
        {:sum ((f code) (ev/lazy-supply n)) :spent @mat}))))

(deftest the-controller-is-certified
  (let [{:keys [code budget nodes]} (lc/certify 0 child)]
    (testing "the child is closed, and Check accepts it at Controller only"
      (is (= 0 budget))
      (is (true? (lc/check code controller)))
      (is (false? (lc/check code '(-> Nat Nat))))
      (is (< 5000 nodes 20000) "a non-trivial certificate: the measured size is recorded in the note"))
    (testing "a false controller cannot be certified: T(t ≠ 0) is not 1 for an open t"
      (is (rejects? 0 false-child)))
    (testing "the parent and the driver are closed programs too"
      (is (= 0 (:budget (lc/certify 0 parent))))
      (is (= 0 (:budget (lc/certify 0 driver)))))))

(deftest a-parent-trusts-the-controller-at-runtime
  (let [{:keys [code nodes]} (lc/certify 0 child)
        tampered (walk/postwalk #(if (= % [:sl :t0]) [:sl :t1] %) code)]
    (testing "with exactly the certificate's size in tokens: minted, checked, reflected, used"
      (is (= {:sum 1023 :spent nodes} (drive code nodes))))
    (testing "one token short: the minted certificate is truncated, so inspect refuses it"
      (is (= {:sum 10 :spent (dec nodes)} (drive code (dec nodes)))))
    (testing "a tampered code fails the free check: nothing is minted"
      (is (not= tampered code))
      (is (= {:sum 10 :spent 0} (drive tampered nodes))))
    (testing "a certificate of another type (pow2 alone) is refused, also for free"
      (is (= {:sum 10 :spent 0} (drive (:code (lc/certify 0 pow2)) nodes))))))
