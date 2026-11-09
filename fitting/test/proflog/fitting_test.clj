(ns proflog.fitting-test
  "The entry point, `proflog.fitting/ask`: Definition 6.1 on the paper's
  programs (ADR-0149 Test Obligation 4), and the D1–D5 cases of ADR-0149 that
  apply to the ground calculus.

  Each test states the value s_P (§3) of the query and why. By Theorem 7.2
  (t_P = s_P), a query succeeds exactly when s_P gives it true, and fails
  exactly when s_P gives it false. A ⊥ query neither succeeds nor fails. With
  no bound `ask` would search for ever, as Definition 6.1 allows, so the ⊥
  tests give a fuel bound. A finite test can confirm only that no closed
  tableau exists within the bound it allows."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.fitting :as fitting]
            [proflog.fitting.programs :as programs]
            [proflog.fitting.tableau-test :refer [win-3-fails]]))

(defn- status
  "The status `ask` gives the query A."
  [language program A & {:as opts}]
  (:status (fitting/ask language program A (or opts {}))))

(defn- unresolved?
  "No closed tableau on either side with at most `fuel` choices on each
  branch: the most a finite run can show about a ⊥ query."
  [language program A fuel]
  (= {:status :unresolved :reason :exhausted :fuel fuel}
     (fitting/ask language program A {:fuel fuel})))

;;; ---------------------------------------------------------------------
;;; The entry point

(deftest ask-takes-the-language-the-program-and-the-query
  ;; §2 and §6: the language is fixed first, the program is written in it,
  ;; and a query is a ground literal of it.
  (let [L '{:constants [0] :functions {s 1} :relations {win 1}}
        P '[(<- (win x) (exists y (and (or (= x (s y)) (= x (s (s y))))
                                       (not (win y)))))]]
    (is (= :fails (:status (fitting/ask L P '(win 0)))))
    (is (= :succeeds (:status (fitting/ask L P '(win (s 0))))))))

(deftest a-query-is-a-ground-literal-of-L
  (doseq [A '[(win x)                  ; a variable, not ground
              (win (par))              ; a parameter belongs to Lpar, not L
              (and (win 0) (win 0))    ; not a literal
              (win (f 0))]]            ; f is not a function symbol of L
    (is (thrown? clojure.lang.ExceptionInfo (fitting/ask programs/L2 programs/P2 A))
        (pr-str A))))

(deftest the-program-is-checked-against-definition-2-1
  (testing "at most one clause for each relation symbol"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fitting/ask programs/L2 '[(<- (win x) true) (<- (win y) false)] '(win 0)))))
  (testing "a body's free variables are among its head's"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fitting/ask programs/L2 '[(<- (win x) (win y))] '(win 0))))))

(deftest the-language-is-checked
  (is (thrown? clojure.lang.ExceptionInfo
               (fitting/ask '{:constants [0] :functions {s 0} :relations {win 1}}
                            programs/P2 '(win 0)))))

(deftest answers-carry-certificates-that-the-relation-accepts
  (let [{:keys [status certificate]} (fitting/ask programs/L2 programs/P2 '(win (s 0)))]
    (is (= :succeeds status))
    ;; A success is a closed P-tableau for ¬A, and not one for A.
    (is (fitting/certificate? programs/L2 programs/P2 '(not (win (s 0))) certificate))
    (is (not (fitting/certificate? programs/L2 programs/P2 '(win (s 0)) certificate)))
    (is (= :call (first certificate)))))

(deftest succeeds-and-fails-are-definition-6-1
  ;; "A query A succeeds with program P if there is a closed P-tableau for
  ;; ¬A. A query A fails with program P if there is a closed P-tableau for A."
  (is (= :succeeds (status programs/L2 programs/P2 '(win (s 0)))))
  (is (= :fails (status programs/L2 programs/P2 '(win 0))))
  (testing "a negated query is a literal too"
    (is (= :fails (status programs/L2 programs/P2 '(not (win (s 0))))))
    (is (= :succeeds (status programs/L2 programs/P2 '(not (win 0)))))))

(deftest fuel-bounds-the-search
  ;; With a bound, a search that finds nothing ends, and says so.
  (is (= {:status :unresolved :reason :exhausted :fuel 3}
         (fitting/ask programs/L2 programs/P2 '(win (s (s (s 0)))) {:fuel 3})))
  (is (= :succeeds (:status (fitting/ask programs/L2 programs/P2 '(win (s 0)) {:fuel 6})))))

(deftest an-unbounded-search-ends-when-nothing-is-left-to-try
  ;; r has no clause, so s_P(r(0)) = ⊥: the smallest supervaluation model
  ;; (Def 3.9) leaves an atom ⊥ unless a clause fixes it. The tableaus for
  ;; r(0) and ¬r(0) have no rule left to apply after their roots (no
  ;; clause to call, no γ sentence, no closure), so even with no bound the
  ;; search ends, and it has shown that neither side closes.
  (is (= {:status :unresolved :reason :exhausted}
         (fitting/ask '{:constants [0] :functions {s 1} :relations {r 1}} '[] '(r 0)))))

(deftest the-parallel-runner-gives-the-same-answers
  ;; Each side's first closed tableau does not depend on whether the other
  ;; side is interleaved with it or run on another thread.
  (doseq [A '[(win 0) (win (s 0)) (win (s (s 0)))]]
    (is (= (fitting/ask programs/L2 programs/P2 A)
           (fitting/ask programs/L2 programs/P2 A {:parallel true})))))

(deftest a-timeout-is-reported-as-such
  ;; win(0) under the factored program is ⊥, so neither search ends; the
  ;; runner stops them and says so.
  (is (= {:status :unresolved :reason :timeout}
         (fitting/ask programs/L-move programs/move-program '(win 0) {:timeout-ms 500}))))

(deftest certificates-print-and-read-back
  (doseq [A '[(win 0) (win (s 0)) (win (s (s 0)))]]
    (let [{:keys [certificate]} (fitting/ask programs/L2 programs/P2 A)]
      (is (= certificate (read-string (pr-str certificate)))))))

;;; ---------------------------------------------------------------------
;;; Program P2 (nim): win(n) is true when n mod 3 ≠ 0.
;;;
;;; s_P: win(0) is false, since (∃y)[(0 = s(y) ∨ 0 = s(s(y))) ∧ ¬win(y)] is
;;; false in every weak Herbrand model (0 is not in the range of s). The
;;; other values follow by induction on n through Φ_P, as in §6's example.

(deftest program-p2
  (is (= :fails (status programs/L2 programs/P2 '(win 0))))
  (is (= :succeeds (status programs/L2 programs/P2 '(win (s 0)))))
  (is (= :succeeds (status programs/L2 programs/P2 '(win (s (s 0))))))
  (testing "§6's worked example: prove mode finds the paper's own tableau"
    (is (= {:status :fails :certificate win-3-fails}
           (fitting/ask programs/L2 programs/P2 '(win (s (s (s 0)))))))))

(deftest ^:slow program-p2-deeper
  ;; win(s⁴(0)) succeeds by moving to s³(0), which needs γ with s(s(s(0)))
  ;; and then the whole §6 proof that win(s³(0)) fails.
  (is (= :succeeds (status programs/L2 programs/P2 '(win (s (s (s (s 0)))))))))

;;; ---------------------------------------------------------------------
;;; Program P1: "even(t) is true just of those closed terms of the form
;;; s^n(0), where n is even. Likewise odd(t) is true just of closed terms
;;; designating odd numbers." (§3)

(deftest program-p1
  (is (= :succeeds (status programs/L1 programs/P1 '(even 0))))
  (is (= :fails (status programs/L1 programs/P1 '(even (s 0)))))
  (is (= :fails (status programs/L1 programs/P1 '(odd 0))))
  (is (= :succeeds (status programs/L1 programs/P1 '(odd (s 0))))))

(deftest ^:slow program-p1-deeper
  ;; These need deep tableaus: even(s(s(0))) calls through odd(s(0)),
  ;; even(s(0)) and odd(0), and odd(s(s(0))) needs γ with s(s(0)) and then
  ;; all of even(s(s(0))).
  (is (= :succeeds (status programs/L1 programs/P1 '(even (s (s 0))))))
  (is (= :fails (status programs/L1 programs/P1 '(odd (s (s 0)))))))

;;; ---------------------------------------------------------------------
;;; §8's factored version of P2. "It is not hard to check that this does
;;; not work as expected": move is not fixed in weak Herbrand models, so a
;;; non-standard y with move(0, y) and ¬win(y) is possible, and win(0)
;;; is ⊥ rather than false.

(deftest factored-move-does-not-work-as-expected
  (testing "move itself is fine on terms of L"
    (is (= :succeeds (status programs/L-move programs/move-program '(move (s 0) 0))))
    (is (= :fails (status programs/L-move programs/move-program '(move 0 0)))))
  (testing "win(0) is ⊥: neither succeeds nor fails"
    (is (unresolved? programs/L-move programs/move-program '(win 0) 6))))

;;; ---------------------------------------------------------------------
;;; ADR-0149's kernel defects D1–D5, as they apply to the ground calculus.

(def L-d
  '{:constants [0] :functions {s 1}
    :relations {rr 1 t 1 r 1 r3 1 a 1 q0 1 r2 1}})

(deftest d1-delta-parameters-are-new
  ;; rr(x) ← (∃y)¬(y = x); t(z) ← (∀w)rr(w). For each w, y = s(w) is a
  ;; witness, so rr is true on L and t(0) has a model. But a non-standard w
  ;; need not satisfy rr (clauses constrain L only), so s_P(t(0)) = ⊥.
  ;; It must not fail.
  (let [P '[(<- (rr x) (exists y (not (= y x))))
            (<- (t z) (forall w (rr w)))]]
    (is (unresolved? L-d P '(t 0) 6))
    (is (= :succeeds (status L-d P '(rr 0))))))

(deftest d2-equality-facts-stay-on-their-branch
  ;; r(x) ← (∃y)((y = x ∧ x = s(0)) ∨ ¬(y = x)). At x = 0 the right
  ;; disjunct holds for y = s(0) in every model, so r(0) is true.
  (let [P '[(<- (r x) (exists y (or (and (= y x) (= x (s 0)))
                                    (not (= y x)))))]]
    (is (= :succeeds (status L-d P '(r 0))))))

(deftest d3-gamma-reuse-under-negative-calls
  ;; a(z) ← a(z) leaves every a-atom ⊥ (the fixed point never moves).
  ;; (∀y)(a(y) ∧ ¬a(s(y))) is false in every model: y = 0 needs a(s(0))
  ;; false, and y = s(0) needs it true. So r3(x) ← (∀y)(a(y) ∧ ¬a(s(y)))
  ;; makes r3(0) false, and r(x) ← (∃y)(¬a(y) ∨ a(s(y))), the negation of
  ;; that body, makes r(0) true. Both need γ applied twice to the same
  ;; sentence; for r(0) that sentence comes from a negative call, where the
  ;; root kernel's `once-forall` allowed only one instance.
  (let [P '[(<- (a z) (a z))
            (<- (r x) (exists y (or (not (a y)) (a (s y)))))
            (<- (r3 x) (forall y (and (a y) (not (a (s y))))))]]
    (is (= :succeeds (status L-d P '(r 0))))
    (is (= :fails (status L-d P '(r3 0))))))

(deftest d4-true-and-false
  (let [P '[(<- (r x) false) (<- (r2 x) true)]]
    (is (= :fails (status L-d P '(r 0))))
    (is (= :succeeds (status L-d P '(r2 0))))))

(deftest d5-closed-term-gamma-and-free-closure
  ;; q0(w) ← (∀u)(u = 0). With s in L, u = s(0) falsifies the body in
  ;; every weak Herbrand model, so q0(0) fails.
  (is (= :fails (status L-d '[(<- (q0 w) (forall u (= u 0)))] '(q0 0))))
  (testing "unrelated clauses do not change the answer"
    (is (= :fails (status L-d '[(<- (q0 w) (forall u (= u 0)))
                                (<- (r x) false)
                                (<- (t z) (r z))]
                          '(q0 0)))))
  (testing "falsifier: with L = {0} the one-element model satisfies the body,
            and a model with one non-standard element (Def 3.1 permits it)
            falsifies it, so q0(0) is ⊥ and must not fail"
    (is (unresolved? '{:constants [0] :functions {} :relations {q0 1}}
                     '[(<- (q0 w) (forall u (= u 0)))]
                     '(q0 0) 6))))

(deftest binder-capture-falsifier
  ;; ADR-0149, Decision §2. With L = {0}, no function symbols, and no
  ;; clauses for R or Q, p(w) ← S for
  ;;   S = (∀x)[((∀y)R(x,y) ∧ ¬(∀y)R(y,y)) ∨ ((∀y)Q(x,y) ∧ ¬(∀y)Q(y,y))].
  ;; S is false in the weak Herbrand model {0}. It is true in the weak
  ;; Herbrand model {d, e} with 0 = d and e non-standard (§3 drops domain
  ;; closure; Def 3.1 is vacuous here), R false only at (d,d) and Q false
  ;; only at (e,e). R and Q atoms are ⊥ in s_P (Def 3.7), so both models
  ;; extend it, and s_P(p(0)) = ⊥ (Def 3.4).
  (let [L0 '{:constants [0] :functions {} :relations {p 1 c 1 R 2 Q 2}}
        P '[(<- (p w) (forall x (or (and (forall y (R x y)) (not (forall y (R y y))))
                                    (and (forall y (Q x y)) (not (forall y (Q y y)))))))
            ;; Control: (∀x)R(x,x) ∧ ¬R(0,0) is false in every model.
            (<- (c w) (and (forall x (R x x)) (not (R 0 0))))]]
    (is (unresolved? L0 P '(p 0) 6))
    (testing "control: the same machinery refutes a contradictory body"
      (is (= :fails (status L0 P '(c 0)))))))

;;; ---------------------------------------------------------------------
;;; Review findings (2026-11-09 independent fidelity review)

(deftest function-free-programs
  ;; p(z) ← (∀x)(∀y)r(x,y) ∧ ¬r(a,a) with L = {a}: the body is false in every
  ;; weak Herbrand model, so s_P(p(a)) is false.
  (is (= :fails (status '{:constants [a] :functions {} :relations {p 1 r 2}}
                        '[(<- (p z) (and (forall x (forall y (r x y))) (not (r a a))))]
                        '(p a))))
  (is (= :fails (status '{:constants [a b] :functions {} :relations {q 1 r3 3}}
                        '[(<- (q w) (and (forall x (forall y (forall z (r3 x y z))))
                                         (not (r3 a a a))))]
                        '(q a)))))

(deftest a-call-substitutes-free-occurrences-only
  ;; q(x) ← (∃x)¬(x = 0). The body has no free x, so ϕ(0) is the same
  ;; sentence, true with x = s(0): q(0) succeeds. Substituting under the
  ;; binder would give (∃x)¬(0 = 0), and q(0) would fail.
  (is (= :succeeds (status '{:constants [0] :functions {s 1} :relations {q 1}}
                           '[(<- (q x) (exists x (not (= x 0))))]
                           '(q 0)))))

(deftest zero-place-relations-and-equality-queries
  (let [Lp '{:constants [0] :functions {s 1} :relations {p 0 r 0}}
        P '[(<- (p) true) (<- (r) (not (p)))]]
    (is (= :succeeds (status Lp P '(p))))
    (is (= :fails (status Lp P '(r))))
    ;; Equality has no clause; its literals are settled by §5 alone.
    (is (= :fails (status Lp P '(= 0 (s 0)))))
    (is (= :succeeds (status Lp P '(= (s 0) (s 0)))))))
