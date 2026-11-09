(ns proflog.fitting.query-test
  "Definition 6.1 on the paper's programs (ADR-0149 Test Obligation 4), and
  the D1–D5 cases of ADR-0149 that apply to the ground calculus.

  Each test states the value s_P (§3) of the query and why. By Theorem 7.2
  (t_P = s_P), a query succeeds exactly when s_P gives it true, and fails
  exactly when s_P gives it false. A ⊥ query neither succeeds nor fails, so
  the API reports :unresolved for it at any fuel. A finite test can confirm
  only that no certificate exists within the fuel it allows."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.fitting.programs :as programs]
            [proflog.fitting.query :as query]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau-test :refer [win-3-fails]]))

(defn- status [P A & {:as opts}]
  (:status (query/query-status P A (merge {:max-fuel 12} opts))))

(defn- unresolved?
  "No closed tableau on either side within the fuel: the most a finite run
  can show about a ⊥ query."
  [P A max-fuel]
  (= {:status :unresolved :reason :exhausted :max-fuel max-fuel}
     (query/query-status P A {:max-fuel max-fuel})))

;;; ---------------------------------------------------------------------
;;; The API

(deftest a-query-is-a-ground-literal-of-L
  (is (thrown? clojure.lang.ExceptionInfo (query/query-status programs/P2 '(win x) {})))
  (is (thrown? clojure.lang.ExceptionInfo (query/query-status programs/P2 '(win (par)) {})))
  (is (thrown? clojure.lang.ExceptionInfo
               (query/query-status programs/P2 '(and (win 0) (win 0)) {}))))

(deftest answers-carry-certificates-that-the-relation-accepts
  (let [{:keys [status certificate]} (query/query-status programs/P2 '(win (s 0)) {:max-fuel 6})]
    (is (= :succeeds status))
    ;; A success is a closed P-tableau for ¬A.
    (is (query/certificate? programs/P2 '(not (win (s 0))) certificate))
    (is (= :call (first certificate)))))

(deftest succeeds-and-fails-are-definition-6-1
  ;; "A query A succeeds with program P if there is a closed P-tableau for
  ;; ¬A. A query A fails with program P if there is a closed P-tableau for A."
  (is (some? (query/succeeds programs/P2 '(win (s 0)) {:max-fuel 6})))
  (is (nil? (query/fails programs/P2 '(win (s 0)) {:max-fuel 6})))
  (is (some? (query/fails programs/P2 '(win 0) {:max-fuel 6})))
  (testing "a negated query is a literal too"
    (is (= :fails (status programs/P2 '(not (win (s 0))))))
    (is (= :succeeds (status programs/P2 '(not (win 0)))))))

;;; ---------------------------------------------------------------------
;;; Program P2 (nim): win(n) is true when n mod 3 ≠ 0.
;;;
;;; s_P: win(0) is false, since (∃y)[(0 = s(y) ∨ 0 = s(s(y))) ∧ ¬win(y)] is
;;; false in every weak Herbrand model (0 is not in the range of s). The
;;; other values follow by induction on n through Φ_P, as in §6's example.

(deftest program-p2
  (is (= :fails (status programs/P2 '(win 0))))
  (is (= :succeeds (status programs/P2 '(win (s 0)))))
  (is (= :succeeds (status programs/P2 '(win (s (s 0))))))
  (testing "§6's worked example: prove mode finds the paper's own tableau"
    (is (= {:status :fails :certificate win-3-fails}
           (query/query-status programs/P2 '(win (s (s (s 0)))) {:max-fuel 12}))))
  (testing "a definite answer at a fuel bound far above what the proof needs"
    (is (= :succeeds (status programs/P2 '(win (s (s (s (s 0))))) :max-fuel 20)))))

(deftest certificates-print-and-read-back
  (doseq [A '[(win 0) (win (s 0)) (win (s (s 0)))]]
    (let [{:keys [certificate]} (query/query-status programs/P2 A {:max-fuel 8})]
      (is (= certificate (read-string (pr-str certificate)))))))

(deftest a-timeout-is-reported-as-such
  ;; win(0) under the factored program is ⊥, so at a large fuel neither
  ;; search ends quickly; the API stops them and says so.
  (is (= {:status :unresolved :reason :timeout :max-fuel 40}
         (query/query-status programs/move-program '(win 0) {:max-fuel 40 :timeout-ms 500}))))

;;; ---------------------------------------------------------------------
;;; Program P1: "even(t) is true just of those closed terms of the form
;;; s^n(0), where n is even. Likewise odd(t) is true just of closed terms
;;; designating odd numbers." (§3)

(deftest program-p1
  (is (= :succeeds (status programs/P1 '(even 0))))
  (is (= :fails (status programs/P1 '(even (s 0)))))
  (is (= :fails (status programs/P1 '(odd 0))))
  (is (= :succeeds (status programs/P1 '(odd (s 0))))))

(deftest ^:slow program-p1-deeper
  ;; These need deep tableaus: even(s(s(0))) calls through odd(s(0)),
  ;; even(s(0)) and odd(0), and odd(s(s(0))) needs γ with s(s(0)) and then
  ;; all of even(s(s(0))). Seconds to a minute each.
  (is (= :succeeds (status programs/P1 '(even (s (s 0))) :max-fuel 16)))
  (is (= :fails (status programs/P1 '(odd (s (s 0))) :max-fuel 16))))

;;; ---------------------------------------------------------------------
;;; §8's factored version of P2. "It is not hard to check that this does
;;; not work as expected": move is not fixed in weak Herbrand models, so a
;;; non-standard y with move(0, y) and ¬win(y) is possible, and win(0)
;;; is ⊥ rather than false.

(deftest factored-move-does-not-work-as-expected
  (testing "move itself is fine on terms of L"
    (is (= :succeeds (status programs/move-program '(move (s 0) 0) :max-fuel 4)))
    (is (= :fails (status programs/move-program '(move 0 0) :max-fuel 4))))
  (testing "win(0) is ⊥: neither succeeds nor fails"
    (is (unresolved? programs/move-program '(win 0) 6))))

;;; ---------------------------------------------------------------------
;;; ADR-0149's kernel defects D1–D5, as they apply to the ground calculus.

(def L-d
  (syntax/language {:constants '[0] :functions '{s 1}
                    :relations '{rr 1 t 1 r 1 r3 1 a 1 q0 1 r2 1}}))

(deftest d1-delta-parameters-are-new
  ;; rr(x) ← (∃y)¬(y = x); t(z) ← (∀w)rr(w). For each w, y = s(w) is a
  ;; witness, so rr is true on L and t(0) has a model. But a non-standard w
  ;; need not satisfy rr (clauses constrain L only), so s_P(t(0)) = ⊥.
  ;; It must not fail.
  (let [P (syntax/program L-d '[(<- (rr x) (exists y (not (= y x))))
                                (<- (t z) (forall w (rr w)))])]
    (is (unresolved? P '(t 0) 6))
    (is (= :succeeds (status P '(rr 0) :max-fuel 6)))))

(deftest d2-equality-facts-stay-on-their-branch
  ;; r(x) ← (∃y)((y = x ∧ x = s(0)) ∨ ¬(y = x)). At x = 0 the right
  ;; disjunct holds for y = s(0) in every model, so r(0) is true.
  (let [P (syntax/program L-d '[(<- (r x) (exists y (or (and (= y x) (= x (s 0)))
                                                         (not (= y x)))))])]
    (is (= :succeeds (status P '(r 0) :max-fuel 6)))))

(deftest d3-gamma-reuse-under-negative-calls
  ;; a(z) ← a(z) leaves every a-atom ⊥ (the fixed point never moves).
  ;; (∀y)(a(y) ∧ ¬a(s(y))) is false in every model: y = 0 needs a(s(0))
  ;; false, and y = s(0) needs it true. So r3(x) ← (∀y)(a(y) ∧ ¬a(s(y)))
  ;; makes r3(0) false, and r(x) ← (∃y)(¬a(y) ∨ a(s(y))), the negation of
  ;; that body, makes r(0) true. Both need γ applied twice to the same
  ;; sentence; for r(0) that sentence comes from a negative call, where the
  ;; root kernel's `once-forall` allowed only one instance.
  (let [P (syntax/program L-d '[(<- (a z) (a z))
                                (<- (r x) (exists y (or (not (a y)) (a (s y)))))
                                (<- (r3 x) (forall y (and (a y) (not (a (s y))))))])]
    (is (= :succeeds (status P '(r 0) :max-fuel 8)))
    (is (= :fails (status P '(r3 0) :max-fuel 8)))))

(deftest d4-true-and-false
  (let [P (syntax/program L-d '[(<- (r x) false) (<- (r2 x) true)])]
    (is (= :fails (status P '(r 0) :max-fuel 3)))
    (is (= :succeeds (status P '(r2 0) :max-fuel 3)))))

(deftest d5-closed-term-gamma-and-free-closure
  ;; q0(w) ← (∀u)(u = 0). With s in L, u = s(0) falsifies the body in
  ;; every weak Herbrand model, so q0(0) fails.
  (let [P (syntax/program L-d '[(<- (q0 w) (forall u (= u 0)))])]
    (is (= :fails (status P '(q0 0) :max-fuel 4))))
  (testing "unrelated clauses do not change the answer"
    (let [P (syntax/program L-d '[(<- (q0 w) (forall u (= u 0)))
                                  (<- (r x) false)
                                  (<- (t z) (r z))])]
      (is (= :fails (status P '(q0 0) :max-fuel 4)))))
  (testing "falsifier: with L = {0} the one-element model satisfies the body,
            and a model with one non-standard element (Def 3.1 permits it)
            falsifies it, so q0(0) is ⊥ and must not fail"
    (let [L0 (syntax/language {:constants '[0] :functions {} :relations '{q0 1}})
          P (syntax/program L0 '[(<- (q0 w) (forall u (= u 0)))])]
      (is (unresolved? P '(q0 0) 6)))))

(deftest binder-capture-falsifier
  ;; ADR-0149, Decision §2. With L = {0}, no function symbols, and no
  ;; clauses for R or Q, p(w) ← S for
  ;;   S = (∀x)[((∀y)R(x,y) ∧ ¬(∀y)R(y,y)) ∨ ((∀y)Q(x,y) ∧ ¬(∀y)Q(y,y))].
  ;; S is false in the weak Herbrand model {0}. It is true in the weak
  ;; Herbrand model {d, e} with 0 = d and e non-standard (§3 drops domain
  ;; closure; Def 3.1 is vacuous here), R false only at (d,d) and Q false
  ;; only at (e,e). R and Q atoms are ⊥ in s_P (Def 3.7), so both models
  ;; extend it, and s_P(p(0)) = ⊥ (Def 3.4).
  (let [L0 (syntax/language {:constants '[0] :functions {} :relations '{p 1 c 1 R 2 Q 2}})
        P (syntax/program
           L0
           '[(<- (p w) (forall x (or (and (forall y (R x y)) (not (forall y (R y y))))
                                     (and (forall y (Q x y)) (not (forall y (Q y y)))))))
             ;; Control: (∀x)R(x,x) ∧ ¬R(0,0) is false in every model.
             (<- (c w) (and (forall x (R x x)) (not (R 0 0))))])]
    (is (unresolved? P '(p 0) 6))
    (testing "control: the same machinery refutes a contradictory body"
      (is (= :fails (status P '(c 0) :max-fuel 6))))))
