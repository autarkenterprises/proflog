(ns lcert.core-test
  "End-to-end tests of the public API, and executable versions of results in
  R4-metatheory.md: Proposition 4.10 (H° from H₁ at a constant budget, from
  review R4-02), the definable destructor of §4.7 (review R4-03),
  Proposition 4.9 at depth 0 (bounded code consistency with no tokens), and
  the form of H° that Addendum D1 of the 2026-09-28 self-justification note
  relies on (one sentence, one term, at every budget)."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.core :as lc]
            [lcert.examples :as ex]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.syntax :as s]
            [lcert.typing :as t]))

(defn- rejects?
  "True iff checking `form` at budget n raises a type error."
  [n form]
  (try (t/check-top n (s/parse-term (s/token-scope n) form)) false
       (catch clojure.lang.ExceptionInfo e (= :lcert/type-error (:type (ex-data e))))))

(deftest the-api
  (testing "certify a closed program, then check its certificate"
    (let [{:keys [code type nodes budget]} (lc/certify 0 ex/not-form)]
      (is (= [:Pi :w [:Bool] [:Bool]] type))
      (is (= 35 nodes))
      (is (= 0 budget))
      (is (true? (lc/check code '(-> Bool Bool))))
      (is (false? (lc/check code '(-> Nat Nat))))))
  (testing "run a program"
    (is (= true (lc/run 0 (list ex/not-form 'ff))))))

(deftest h-from-h1-at-constant-budget
  (testing "Prop 4.10: Θ_K ⊢ λr e. H₁ r lit_v c⊥ e ⋆ : H°, with K the size of a fixed certificate"
    (let [{:keys [budget term]} (ex/h-from-h1)
          d (lc/check-term budget term)
          code (lc/encode d)]
      (is (= ex/h-type (:type d)))
      (is (true? (lc/check-code code (lc/type-code ex/h-type))))
      ;; the budget is the fixed certificate's size, whatever refutation is fed in
      (is (= budget (k/nodes (:code (lc/certify 0 ex/zero-lolli-zero))))))))

(deftest the-definable-destructor
  (testing "§4.7: roll (out r) gives back r, token for token"
    (let [r '(node $1 :a (node $2 :b (leaf :c) (leaf :c)) (leaf :c))
          v (lc/run 2 r)
          w (lc/run 2 (ex/roll-out r))]
      (is (= v w))))
  (testing "out exposes a node's token and children: take the left child"
    (let [left (lc/run 2 (ex/left-child '(node $1 :a (node $2 :b (leaf :c) (leaf :c)) (leaf :c))))]
      (is (= :b (nth left 2)))
      (is (= 1 (count (filter #(= :rn %) (flatten left)))))))
  (testing "out and roll are closed: they use no tokens of their own"
    (is (= 0 (:budget (lc/certify 0 ex/roll-form))))
    (is (= 0 (:budget (lc/certify 0 ex/out-form))))))

(deftest bounded-code-consistency-at-depth-0
  (testing "Prop 4.9 at k = 0: a closed, token-free proof over every code of depth 0"
    (let [{:keys [type budget]} (lc/certify 0 (ex/bounded-con 0))]
      (is (= 0 budget))
      (is (= (ex/bounded-con-type 0) type)))))

;; Artemov's consistency scheme, regrouped by proof size, is Pudlák's family
;; Con(n); H° at budget n has that content. What differs is form: H° is ONE
;; sentence, proved by ONE term at every budget, whose proof quantifier ranges
;; over certificates held. These assertions pin the three facts the note's
;; Addendum D1 uses: the same term and type at every budget, with only the
;; certificate growing; no free-code (usage-ω, Syn) variant; no second use of
;; one held certificate. They pin existing behaviour (green when written).
(deftest h-is-one-sentence-at-every-budget
  (let [h '(fn [r 1 R] (fn [e 1 (T (chk (print r) c-bot))] (H r e)))
        h-type (s/parse-type [] '(Pi [r 1 R] (-o (T (chk (print r) c-bot)) Void)))
        certs (for [n [0 1 5 40]] (assoc (lc/certify n h) :n n))]
    (testing "the identical term proves the identical type at budgets 0, 1, 5 and 40"
      (doseq [{:keys [n type budget]} certs]
        (is (= h-type type) (str "type at budget " n))
        (is (= n budget) (str "declared budget at budget " n))))
    (testing "only the certificate grows, since E3 writes the token context out"
      (is (apply < (map :nodes certs)))
      (is (every? #(> (:nodes %) (:n %)) certs))))
  (testing "the bound cannot be dropped: the free-code (usage-ω, Syn) variant is rejected"
    (doseq [n [0 40]]
      (is (rejects? n '(fn [c w Syn] (fn [e 1 (T (chk c c-bot))] (H c e)))))))
  (testing "one held certificate is spent once: control accepted, second use rejected"
    (is (not (rejects? 0 '(fn [r 1 R] (fn [e 1 (T (chk (print r) c-bot))]
                                        (fn [f 1 (T (chk (print r) c-bot))]
                                          (pair (Sigma [x 1 Void] Unit) (H r e) star)))))))
    (is (rejects? 0 '(fn [r 1 R] (fn [e 1 (T (chk (print r) c-bot))]
                                   (fn [f 1 (T (chk (print r) c-bot))]
                                     (pair (Sigma [x 1 Void] Void) (H r e) (H r f)))))))))

(deftest parsing-a-runtime-code-into-a-certificate
  (testing "review RR2-10: a typed parser threads a supply of tokens through a code"
    (let [supply '(node $1 :a (leaf :a) (node $2 :a (leaf :a) (node $3 :a (leaf :a) (leaf :a))))
          code [:sn :b [:sn :c [:sl :a] [:sl :a]] [:sl :a]]
          prog (fn [C body] (ex/parse-then (list 'code-literal code) supply C body))]
      (testing "the parser is closed: it costs no tokens of its own"
        (is (= 0 (:budget (lc/certify 0 ex/parse-form)))))
      (testing "with enough supply, the certificate prints back to the code"
        (is (= code (lc/run 3 (prog 'Syn '(print t))))))
      (testing "one supply node per certificate node; the rest is returned"
        (is (= 1 (count (filter #(= :rn %) (flatten (lc/run 3 (prog 'R 'rest))))))))
      (testing "with too little supply, the result is truncated, which print reveals"
        (let [short '(node $1 :a (leaf :a) (leaf :a))]
          (is (not= code (lc/run 1 (ex/parse-then (list 'code-literal code) short 'Syn '(print t))))))))))

(deftest certificate-forms
  (testing "certificate-form: the surface program building a code's certificate from $1..$k"
    (let [{:keys [code nodes]} (lc/certify 0 ex/not-form)
          v (lc/run nodes (lc/certificate-form code))]
      (is (= 35 (count (filter #(= :rn %) (flatten v)))))
      (is (= code (ev/print-value v))))))

