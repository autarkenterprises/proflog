(ns proflog.fitting.tableau-test
  "The tableau relation, rule by rule: §2 substitution, Table 1, and check
  mode on hand-written certificates (ADR-0149 Test Obligations 2, 3 and 5).

  Every certificate below is checked by the same relation that proves:
  `tableauo` with a ground certificate. A test that expects rejection says
  which rule of the paper the certificate breaks."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [run run*]]
            [clojure.test :refer [deftest is testing]]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau :as t]))

;;; ---------------------------------------------------------------------
;;; A language and programs for the tests

(def L
  (syntax/language {:constants '[0]
                    :functions '{s 1}
                    :relations '{win 1 P 1 Q 1 R 2}}))

(def P2
  "Program P2 of §2: nim with moves of 1 or 2."
  (syntax/program
   L
   '[(<- (win x) (exists y (and (or (= x (s y)) (= x (s (s y)))) (not (win y)))))]))

(def empty-program (syntax/program L []))

(defn- checks?
  "Does tableauo accept cert as a closed P-tableau for X?"
  [P X cert]
  (boolean (seq (run 1 [q] (t/tableauo (syntax/with-sentence P X) X cert)))))

;;; ---------------------------------------------------------------------
;;; §2: ϕ(t), substitution of t for the free occurrences of x

(defn- subst [P phi x term]
  (run* [out] (t/substo P phi x term out)))

(deftest substitution-replaces-free-occurrences
  (let [P (syntax/with-sentence empty-program '(forall x (R x (s x))))]
    (is (= '[(R 0 (s 0))] (map syntax/pretty (subst P '(R x (s x)) 'x 0))))))

(deftest substitution-stops-at-an-inner-binder-of-the-same-name
  ;; Occurrences of x under an inner (∀x) are not free (§2).
  (let [phi '(and (P x) (forall x (Q x)))
        P (syntax/with-sentence empty-program (list 'forall 'x phi))]
    (is (= '[(and (P (s 0)) (forall x (Q x)))]
           (map syntax/pretty (subst P phi 'x '(s 0)))))))

(deftest substitution-leaves-other-variables-and-parameters
  (let [phi '(exists y (R x y))
        P (syntax/with-sentence empty-program (list 'forall 'x phi))]
    (is (= '[(exists y (R (par 1) y))]
           (map syntax/pretty (subst P phi 'x '(par 1)))))))

;;; ---------------------------------------------------------------------
;;; Table 1: sentence categories

(deftest table-1-alpha
  (is (= '[[(P 0) (Q 0)]] (run* [a b] (t/alphao '(and (P 0) (Q 0)) a b))))
  (is (= '[[(not (P 0)) (not (Q 0))]]
         (map syntax/pretty (run* [a b] (t/alphao '(not (or (P 0) (Q 0))) a b)))))
  (is (= '[[(P 0) (not (Q 0))]]
         (map syntax/pretty (run* [a b] (t/alphao '(not (implies (P 0) (Q 0))) a b)))))
  (is (empty? (run* [a b] (t/alphao '(or (P 0) (Q 0)) a b)))))

(deftest table-1-beta
  (is (= '[[(not (P 0)) (not (Q 0))]]
         (map syntax/pretty (run* [a b] (t/betao '(not (and (P 0) (Q 0))) a b)))))
  (is (= '[[(P 0) (Q 0)]] (run* [a b] (t/betao '(or (P 0) (Q 0)) a b))))
  (is (= '[[(not (P 0)) (Q 0)]]
         (map syntax/pretty (run* [a b] (t/betao '(implies (P 0) (Q 0)) a b)))))
  (is (empty? (run* [a b] (t/betao '(and (P 0) (Q 0)) a b)))))

(deftest table-1-gamma-and-delta
  ;; γ(t) is Φ(t) or ¬Φ(t); the relations give x and the matrix Φ or ¬Φ.
  (is (= '[[x (P x)]] (run* [x m] (t/gammao '(forall x (P x)) x m))))
  (is (= '[[x (not (P x))]]
         (map syntax/pretty (run* [x m] (t/gammao '(not (exists x (P x))) x m)))))
  (is (= '[[x (P x)]] (run* [x m] (t/deltao '(exists x (P x)) x m))))
  (is (= '[[x (not (P x))]]
         (map syntax/pretty (run* [x m] (t/deltao '(not (forall x (P x))) x m)))))
  (is (empty? (run* [x m] (t/gammao '(exists x (P x)) x m))))
  (is (empty? (run* [x m] (t/deltao '(forall x (P x)) x m)))))

;;; ---------------------------------------------------------------------
;;; Check mode: the paper's own tableau (§6)

;; §6's worked example, as certificates. The paper writes the δ parameters
;; c and d; here each is (par), the first parameter of its own tableau. The
;; numbers in comments are the paper's line numbers.

(def win-0-fails
  "The paper's third tableau: a closed P2-tableau for win(0)."
  '[:call (win 0)
    ;; (1) (∃y)[(0 = s(y) ∨ 0 = s(s(y))) ∧ ¬win(y)]
    [:delta                                   ; (2), new parameter d
     [:alpha                                  ; (3), (4)
      [:beta                                  ; (5) | (6)
       [:free-closure (= 0 (s (par)))]
       [:free-closure (= 0 (s (s (par))))]]]]])

(defn- calling-not-win
  "A call on ¬win(n), for n = s(s(0)) or s(0), closed as the paper closes
  its line (7): γ with the term 0, then Reflexivity on one side of β and a
  call on win(0) on the other."
  [n]
  [:call (list 'not (list 'win n))
   ;; (1) ¬(∃y)[(n = s(y) ∨ n = s(s(y))) ∧ ¬win(y)]
   [:gamma 0                                  ; (2), with the term 0
    [:beta                                    ; (3) | (7)
     [:alpha                                  ; (4), (5)
      [:reflexivity n]]                       ; (6) n = n
     [:not-not                                ; (8) win(0)
      win-0-fails]]]])

(def win-3-fails
  "§6's worked example: a closed P2-tableau for win(s(s(s(0)))), so
  win(s(s(s(0)))) fails. The right-hand branch, which the paper leaves to
  the reader, is closed in the same way as the left."
  [:call '(win (s (s (s 0))))
   ;; (1) (∃y)[(s³0 = s(y) ∨ s³0 = s(s(y))) ∧ ¬win(y)]
   [:delta                                    ; (2), new parameter c
    [:alpha                                   ; (3), (4)
     [:beta                                   ; (5) | (8)
      [:one-one '(= (s (s (s 0))) (s (par))) '(= (s (s 0)) (par))  ; (6)
       [:substitutivity '(= (s (s 0)) (par)) '(not (win (par))) '(not (win (s (s 0))))  ; (7)
        (calling-not-win '(s (s 0)))]]
      [:one-one '(= (s (s (s 0))) (s (s (par)))) '(= (s (s 0)) (s (par)))  ; (9)
       [:one-one '(= (s (s 0)) (s (par))) '(= (s 0) (par))                ; (10)
        [:substitutivity '(= (s 0) (par)) '(not (win (par))) '(not (win (s 0)))  ; (11)
         (calling-not-win '(s 0))]]]]]]])

(deftest the-papers-tableau-is-accepted
  (is (checks? P2 '(win (s (s (s 0)))) win-3-fails)))

(deftest a-check-is-deterministic
  ;; With a ground certificate every step is fixed, so the check has a
  ;; single answer.
  (let [X '(win (s (s (s 0))))]
    (is (= 1 (count (run* [q] (t/tableauo (syntax/with-sentence P2 X) X win-3-fails)))))))

(deftest certificates-print-and-read-back
  (is (= win-3-fails (read-string (pr-str (syntax/pretty win-3-fails))))))

;;; ---------------------------------------------------------------------
;;; Check mode: falsifiers. Each certificate breaks one rule of the paper.

(deftest falsifier-gamma-with-a-parameter-not-yet-on-the-branch
  ;; (∀x)(∃y)¬(y = x) is true in every weak Herbrand model of this L
  ;; (y = s(x) is a witness), so no tableau for it closes. The certificate
  ;; instantiates γ with (par) before δ introduces (par), so δ's parameter
  ;; is no longer new (§4) and the branch would close by Reflexivity.
  (is (not (checks? empty-program '(forall x (exists y (not (= y x))))
                    '[:gamma (par) [:delta [:reflexivity (par)]]]))))

(deftest falsifier-gamma-term-not-closed-or-outside-Lpar
  ;; γ needs a closed term of Lpar (Table 2). ¬(∃x)(x = x) closes with any
  ;; closed term, so only the term itself is at issue.
  (let [X '(not (exists x (= x x)))]
    (testing "x is a variable, not a closed term"
      (is (not (checks? empty-program X '[:gamma x [:reflexivity x]]))))
    (testing "f is not a function symbol of L"
      (is (not (checks? empty-program X '[:gamma (f 0) [:reflexivity (f 0)]]))))
    (testing "s(0) is a closed term of L"
      (is (checks? empty-program X '[:gamma (s 0) [:reflexivity (s 0)]])))))

(deftest falsifier-closure-on-alpha-variants
  ;; Closure needs "both A and ¬A for some sentence A" (§4): syntactic
  ;; identity. (∀x)P(x) and ¬(∀y)P(y) are different sentences.
  (let [X '(and (forall x (P x)) (not (forall y (P y))))]
    (is (not (checks? empty-program X '[:alpha [:close (forall x (P x))]])))
    (testing "the paper's closure goes through δ, γ and an atomic closure"
      (is (checks? empty-program X '[:alpha [:delta [:gamma (par) [:close (P (par))]]]])))))

(deftest falsifier-call-on-an-atom-with-a-parameter
  ;; The Procedure Call Rule applies to atoms of L, not of Lpar (§6). With
  ;; P(x) ← true, every P(t) with t in L is true, but a non-standard element
  ;; need not satisfy P, so (∃y)¬P(y) has value ⊥ and no tableau for it may
  ;; close. Calling on ¬P(c) for the δ parameter c would close it.
  (let [P-true (syntax/program L '[(<- (P x) true)])]
    (is (not (checks? P-true '(exists y (not (P y)))
                      '[:delta [:call (not (P (par))) [:not-true [:false]]]])))
    (testing "the same call on an atom of L is allowed"
      (is (checks? P-true '(not (P 0)) '[:call (not (P 0)) [:not-true [:false]]])))))

(deftest falsifier-open-leaf
  (is (not (checks? empty-program '(and (P 0) (not (Q 0))) '[:alpha [:close (P 0)]])))
  (is (not (checks? empty-program '(P 0) '[:false]))))

(deftest falsifier-wrong-rule-for-the-sentence
  ;; P(0) ∧ ¬P(0) is α, not β.
  (is (not (checks? empty-program '(and (P 0) (not (P 0)))
                    '[:beta [:close (P 0)] [:close (P 0)]])))
  (is (checks? empty-program '(and (P 0) (not (P 0))) '[:alpha [:close (P 0)]])))

(deftest falsifier-free-closure-on-a-parameter
  ;; Free Closure concerns symbols of L; (par) is a parameter, so
  ;; (par) = 0 does not close a branch (§5).
  (is (not (checks? empty-program '(exists y (= y 0)) '[:delta [:free-closure (= (par) 0)]])))
  (testing "it does close on s(0) = 0"
    (is (checks? empty-program '(= (s 0) 0) '[:free-closure (= (s 0) 0)]))))

(deftest falsifier-substitutivity-must-replace-one-occurrence
  (let [X '(and (= 0 (s 0)) (and (P 0) (not (P (s (s 0))))))]
    ;; One replacement of 0 by s(0) in P(0) gives P(s(0)), not P(s(s(0))).
    (is (not (checks? empty-program X
                      '[:alpha [:alpha [:substitutivity (= 0 (s 0)) (P 0) (P (s (s 0)))
                                        [:close (P (s (s 0)))]]]])))))

(deftest falsifier-reflexivity-needs-identical-sides
  (is (not (checks? empty-program '(not (= 0 (s 0))) '[:reflexivity 0])))
  (is (checks? empty-program '(not (= (s 0) (s 0))) '[:reflexivity (s 0)])))

;;; ---------------------------------------------------------------------
;;; Table 2's propositional rules and constants

(deftest table-2-negation-rules
  (is (checks? empty-program false '[:false]))
  (is (checks? empty-program '(not true) '[:not-true [:false]]))
  (is (not (checks? empty-program '(not false) '[:not-false [:false]])))
  (is (checks? empty-program '(not (not (and (P 0) (not (P 0)))))
               '[:not-not [:alpha [:close (P 0)]]])))

;;; ---------------------------------------------------------------------
;;; Review findings (2026-11-09 independent fidelity review)

(defn- proves?
  "Does prove mode find a closed P-tableau for X with at most n choices on
  each branch?"
  [P X n]
  (boolean (seq (run 1 [c] (t/fueled-tableauo (syntax/with-sentence P X) X n c)))))

(deftest gamma-is-never-stuck-in-a-language-without-function-symbols
  ;; With finitely many closed terms of L, the γ sentence at the head of
  ;; the queue can use up its terms. γ may then use a new parameter ("any
  ;; closed term of Lpar"), so the γ sentences behind it are still reached.
  (testing "L = {a}: (∀x)(∀y)r(x,y) ∧ ¬r(a,a) is unsatisfiable"
    (let [La (syntax/language {:constants '[a] :functions {} :relations '{r 2}})]
      (is (proves? (syntax/program La []) '(and (forall x (forall y (r x y))) (not (r a a))) 8))))
  (testing "no constants: (∀x)(∀y)r(x,y) ∧ ¬(∀z)r(z,z) is unsatisfiable"
    (let [L0 (syntax/language {:constants [] :functions {} :relations '{r 2}})]
      (is (proves? (syntax/program L0 []) '(and (forall x (forall y (r x y))) (not (forall z (r z z)))) 8))))
  (testing "a new parameter for γ is a closed term of Lpar, so the certificate checks"
    (let [La (syntax/language {:constants '[a] :functions {} :relations '{r 2}})]
      (is (checks? (syntax/program La []) '(and (forall x (forall y (r x y))) (not (r a a)))
                   '[:alpha [:gamma a [:gamma (par) [:gamma a [:close (r a a)]]]]])))))

(deftest falsifier-delta-parameter-must-be-new
  ;; ∃x p(x) ∧ ∃y ¬p(y) is satisfiable. Each δ takes the next parameter, so
  ;; the two witnesses are (par) and (par 1), and the branch stays open.
  (let [X '(and (exists x (P x)) (exists y (not (P y))))]
    (is (not (checks? empty-program X '[:alpha [:delta [:delta [:close (P (par))]]]])))
    (is (not (checks? empty-program X '[:alpha [:delta [:delta [:close (P (par 1))]]]])))))

(deftest falsifier-call-polarity-and-clause
  (let [P-true (syntax/program L '[(<- (P x) true)])]
    (testing "a positive call needs a closed tableau for ϕ(t̄), here `true`"
      (is (not (checks? P-true '(P 0) '[:call (P 0) [:not-true [:false]]]))))
    (testing "a relation with no clause cannot be called"
      (is (not (checks? P-true '(not (Q 0)) '[:call (not (Q 0)) [:not-true [:false]]]))))))

(deftest falsifier-one-one-needs-one-function-symbol-of-L
  ;; (par) is a parameter, not f(t1,...,tn) for a function symbol f of L,
  ;; so One-One gives nothing from (par) = s(0). The branch would close by
  ;; Reflexivity anyway; the One-One step is the only fault.
  (is (not (checks? empty-program '(exists y (and (= y (s 0)) (not (= 0 0))))
                    '[:delta [:alpha [:one-one (= (par) (s 0)) (= 0 0) [:reflexivity 0]]]])))
  (testing "One-One on s(s(0)) = s(0) gives s(0) = 0, which Free Closure closes"
    (is (checks? empty-program '(= (s (s 0)) (s 0))
                 '[:one-one (= (s (s 0)) (s 0)) (= (s 0) 0) [:free-closure (= (s 0) 0)]]))))

(deftest falsifier-substitutivity-needs-the-equation-on-the-branch
  (is (not (checks? empty-program '(and (P 0) (not (P (s 0))))
                    '[:alpha [:substitutivity (= 0 (s 0)) (P 0) (P (s 0)) [:close (P (s 0))]]]))))

(deftest falsifier-free-closure-needs-distinct-symbols
  (is (not (checks? empty-program '(= 0 0) '[:free-closure (= 0 0)])))
  (is (not (checks? empty-program '(= (s 0) (s (s 0))) '[:free-closure (= (s 0) (s (s 0)))]))))

;;; ---------------------------------------------------------------------
;;; The systematic construction does not repeat itself (its normal form)

(deftest normal-form-gamma-term-once-per-sentence
  ;; The checker accepts the systematic construction's normal form only. A
  ;; second instance of a γ sentence with the same term adds nothing, so it
  ;; is not a step of that construction. (Revision 7's version of this test
  ;; repeated s(0), whose instance P(s(0)) cannot close against ¬P(0); the
  ;; certificate was rejected for that reason too.)
  (let [X '(and (forall x (P x)) (not (P 0)))]
    (is (not (checks? empty-program X '[:alpha [:gamma 0 [:gamma 0 [:close (P 0)]]]])))
    (is (checks? empty-program X '[:alpha [:gamma 0 [:close (P 0)]]]))))

(deftest normal-form-closure-uses-a-new-literal
  ;; P(0) and ¬P(0) are both on the branch before γ is applied. A closure
  ;; on them after γ was already possible before it, so the systematic
  ;; construction closes there.
  (let [X '(and (and (P 0) (not (P 0))) (forall x (Q x)))]
    (is (not (checks? empty-program X '[:alpha [:alpha [:gamma 0 [:close (P 0)]]]])))
    (is (checks? empty-program X '[:alpha [:alpha [:close (P 0)]]]))))

(deftest normal-form-equality-steps-add-new-literals
  ;; Substitutivity on 0 = s(0) turns P(0) into P(s(0)), which is already on
  ;; the branch, so the construction does not add it; it closes at once.
  (let [X '(and (= 0 (s 0)) (and (P 0) (and (P (s 0)) (not (P (s 0))))))]
    (is (not (checks? empty-program X
                      '[:alpha [:alpha [:alpha [:substitutivity (= 0 (s 0)) (P 0) (P (s 0))
                                                [:close (P (s 0))]]]]])))
    (is (checks? empty-program X '[:alpha [:alpha [:alpha [:close (P (s 0))]]]]))))

;;; ---------------------------------------------------------------------
;;; One-One as the paper states it (ADR-0149 revision 8)

(def Lf
  "A language with a two-place function symbol, for One-One."
  (syntax/language {:constants '[0] :functions '{s 1 f 2} :relations {}}))

(deftest one-one-adds-one-equation
  ;; One-One: a branch containing f(t1,...,tn) = f(u1,...,un) "may be
  ;; extended by adding ti = ui to the end, for any i = 1,...,n" (§5). The
  ;; certificate names the equation it adds.
  (let [P (syntax/program Lf [])
        e '(= (f (s (s 0)) (s 0)) (f (s 0) 0))
        X e]
    (testing "i = 2 gives s(0) = 0, which Free Closure closes"
      (is (checks? P X [:one-one e '(= (s 0) 0) '[:free-closure (= (s 0) 0)]])))
    (testing "the equation is ti = ui, in that order"
      (is (not (checks? P X [:one-one e '(= 0 (s 0)) '[:free-closure (= 0 (s 0))]]))))
    (testing "one application adds one equation: after i = 1, s(0) = 0 is not yet on the branch"
      (is (not (checks? P X [:one-one e '(= (s (s 0)) (s 0)) '[:free-closure (= (s 0) 0)]])))
      (is (checks? P X [:one-one e '(= (s (s 0)) (s 0))
                        [:one-one e '(= (s 0) 0) '[:free-closure (= (s 0) 0)]]])))))

;;; ---------------------------------------------------------------------
;;; No bound in the relation; fuel as a wrapper (ADR-0149 revision 7)

(deftest the-relation-has-no-bound
  ;; Fitting's rules have no step bound, and neither has tableauo. Unbounded
  ;; prove mode finds the paper's own tableau for win(s(s(s(0)))). That
  ;; tableau makes 7 choices on its longest branch, and every closed tableau
  ;; for this root makes at least 4 (a call, One-One, Substitutivity and a
  ;; second call), so a bound of 3 finds nothing.
  (let [X '(win (s (s (s 0))))
        P (syntax/with-sentence P2 X)]
    (is (= [win-3-fails] (map syntax/pretty (run 1 [c] (t/tableauo P X c)))))
    (is (empty? (run 1 [c] (t/fueled-tableauo P X 3 c))))))

(deftest fuel-counts-the-choices-on-each-branch
  ;; fueled-tableauo with n accepts the tableaus with at most n choices on
  ;; each branch: γ instances, procedure calls, One-One and Substitutivity
  ;; steps. The paper's tableau makes 7 on its right-hand branch: the call
  ;; on win(s(s(s(0)))), two One-One steps, Substitutivity, the call on
  ;; ¬win(s(0)), γ with 0, and the call on win(0).
  (let [X '(win (s (s (s 0))))
        P (syntax/with-sentence P2 X)]
    (is (empty? (run 1 [q] (t/fueled-tableauo P X 6 win-3-fails))))
    (is (seq (run 1 [q] (t/fueled-tableauo P X 7 win-3-fails))))))

(deftest forced-steps-and-closure-need-no-fuel
  ;; α and closure on A and ¬A are not choices, so they run with fuel 0.
  (let [X '(and (P 0) (not (P 0)))]
    (is (= '[[:alpha [:close (P 0)]]]
           (map syntax/pretty
                (run* [c] (t/fueled-tableauo (syntax/with-sentence empty-program X) X 0 c))))))
  (testing "γ is a choice"
    (let [X '(and (forall x (P x)) (not (P 0)))
          P (syntax/with-sentence empty-program X)]
      (is (empty? (run* [c] (t/fueled-tableauo P X 0 c))))
      (is (= '[[:alpha [:gamma 0 [:close (P 0)]]]]
             (map syntax/pretty (run* [c] (t/fueled-tableauo P X 1 c))))))))

(deftest gamma-terms-are-no-deeper-than-the-fuel-left
  ;; The bound also limits the terms γ may choose, so that each bounded
  ;; search is finite: a γ term is no deeper than the choices left after
  ;; its γ instance. Closing (∀x)P(x) ∧ ¬P(s(s(0))) needs γ with s(s(0)),
  ;; of depth 2, so one choice for γ and two more for the term.
  (let [X '(and (forall x (P x)) (not (P (s (s 0)))))
        P (syntax/with-sentence empty-program X)]
    (is (empty? (run* [c] (t/fueled-tableauo P X 2 c))))
    (is (= '[[:alpha [:gamma (s (s 0)) [:close (P (s (s 0)))]]]]
           (map syntax/pretty (run* [c] (t/fueled-tableauo P X 3 c)))))))

(deftest a-fueled-search-ends
  ;; (∀x)(∃y)¬(y = x) is true in every weak Herbrand model (y = s(x)), so
  ;; no tableau for it closes. Unbounded, the search would never end. With
  ;; a bound it is finite: run* returns, with nothing.
  (let [X '(forall x (exists y (not (= y x))))]
    (is (empty? (run* [c] (t/fueled-tableauo (syntax/with-sentence empty-program X) X 3 c))))))

(deftest every-fueled-certificate-is-a-certificate
  ;; fueled-tableauo ties the same rules as tableauo through n stages, so
  ;; whatever it finds, tableauo accepts.
  (doseq [[P X] [[P2 '(win 0)]
                 [P2 '(not (win (s 0)))]
                 [empty-program '(and (forall x (P x)) (not (P (s (s 0)))))]]]
    (let [certs (run 3 [c] (t/fueled-tableauo (syntax/with-sentence P X) X 6 c))]
      (is (seq certs))
      (doseq [c certs]
        (is (checks? P X c))))))

;;; ---------------------------------------------------------------------
;;; Definition 6.1 as a relation (ADR-0149 revision 7)

(deftest definition-6-1-is-a-relation
  ;; "A query A succeeds with program P if there is a closed P-tableau for
  ;; ¬A. A query A fails with program P if there is a closed P-tableau for
  ;; A."
  (let [A '(win (s (s (s 0))))]
    (testing "with the status fresh, the two sides race and the first closed tableau answers"
      (is (= [[:fails win-3-fails]] (map syntax/pretty (run 1 [s c] (t/queryo P2 A s c))))))
    (testing "with the certificate ground, it is checked: the paper's tableau shows that A fails"
      (is (= [:fails] (run* [s] (t/queryo P2 A s win-3-fails)))))
    (testing "with the status ground, one side: no closed tableau for ¬A within the bound"
      (is (empty? (run 1 [c] (t/fueled-queryo P2 A 4 :succeeds c)))))))
