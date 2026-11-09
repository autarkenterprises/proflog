(ns proflog.fitting.syntax-test
  "§2 of Fitting's paper: the language L, formulas, Def 2.1 programs, and
  queries (ground literals of L). ADR-0149 Test Obligation 5 (syntax part)."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.fitting.syntax :as syntax]))

(def L
  "The language of Programs P1 and P2: constant 0, function s, and the
  relations even, odd and win (equality is always present)."
  (syntax/language {:constants '[0]
                    :functions '{s 1}
                    :relations '{even 1 odd 1 win 1}}))

(deftest equality-is-always-a-relation-symbol
  ;; "We assume equality is always among the relation symbols." (§2)
  (is (some #{['= '(1 1)]} (:relations L))))

(deftest language-tables
  (testing "arities are unary lists so that relations can walk them"
    (is (some #{['s '(1)]} (:functions L)))
    (is (some #{['even '(1)]} (:relations L))))
  (testing "symbol distinctness is a finite table (no `!=` is needed)"
    (let [L2 (syntax/language {:constants '[a b] :functions '{f 1 g 2} :relations '{}})]
      (is (= #{['a 'b] ['b 'a]} (set (:distinct-constants L2))))
      (is (= #{['f 'g] ['g 'f]} (set (:distinct-functions L2)))))))

(deftest reserved-words-cannot-be-symbols-of-L
  (doseq [w '[and or not implies forall exists par <- =]]
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/language {:constants [w] :functions {} :relations {}}))
        (str w))))

(deftest formulas-of-L
  (testing "well-formed formulas, with variables as bare symbols"
    (is (syntax/formula? L '(forall x (or (even x) (not (even x))))))
    (is (syntax/formula? L '(exists y (and (= x (s y)) (odd y)))))
    (is (syntax/formula? L true))
    (is (syntax/formula? L '(implies false (not true)))))
  (testing "ill-formed formulas"
    (is (not (syntax/formula? L '(even 0 0))))         ; wrong arity
    (is (not (syntax/formula? L '(and (even 0)))))     ; connectives are binary
    (is (not (syntax/formula? L '(forall 0 (even 0))))) ; 0 is a constant, not a variable
    (is (not (syntax/formula? L '(even (t 0)))))       ; t is not a function symbol of L
    (is (not (syntax/formula? L '(even (par))))))      ; parameters are not symbols of L
  (testing "free variables"
    (is (= #{'x} (syntax/free-variables '(exists y (and (= x (s y)) (odd y))))))
    (is (= #{} (syntax/free-variables '(forall x (even x)))))
    (is (= #{'x} (syntax/free-variables '(and (even x) (forall x (odd x))))))))

(def p1-clauses
  '[(<- (even x) (or (= x 0) (exists y (and (= x (s y)) (odd y)))))
    (<- (odd x) (forall y (implies (even y) (not (= x y)))))])

(deftest def-2-1-programs
  (testing "Program P1 is a program"
    (let [P (syntax/program L p1-clauses)]
      (is (= 2 (count (:clauses P))))
      (is (some #{['even '(x) '(or (= x 0) (exists y (and (= x (s y)) (odd y))))]}
                (:clauses P)))))
  (testing "at most one clause for each relation symbol"
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/program L '[(<- (even x) true) (<- (even y) false)]))))
  (testing "no clause for equality"
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/program L '[(<- (= x y) true)]))))
  (testing "the head is R(x1,...,xn) with distinct variables"
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/program (syntax/language {:constants '[0] :functions {}
                                                   :relations '{r 2}})
                                 '[(<- (r x x) true)])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/program L '[(<- (even 0) true)]))))
  (testing "the body's free variables are among the head's, so that ϕ(t̄) is a sentence"
    (is (thrown? clojure.lang.ExceptionInfo
                 (syntax/program L '[(<- (even x) (odd y))])))))

(deftest queries-are-ground-literals-of-L
  ;; "a query is any ground literal of L" (§2)
  (is (syntax/query? L '(win (s (s (s 0))))))
  (is (syntax/query? L '(not (even 0))))
  (is (syntax/query? L '(= 0 (s 0))))
  (is (not (syntax/query? L '(win x))))
  (is (not (syntax/query? L '(win (par)))))
  (is (not (syntax/query? L '(not (not (even 0))))))
  (is (not (syntax/query? L '(and (even 0) (odd 0))))))

(deftest the-names-table
  ;; Substitution only asks whether a leaf is the variable being instantiated.
  ;; The answer comes from a finite table of distinct (variable, leaf) pairs,
  ;; built from the program text, the root sentence, and L's constants.
  (let [P (syntax/program L p1-clauses)]
    (is (some #{['x 'y]} (:distinct-names P)))
    (is (some #{['y '0]} (:distinct-names P)))
    (is (not (some #{['x 'x]} (:distinct-names P))))
    (let [P' (syntax/with-sentence P '(forall z (even z)))]
      (is (some #{['z 'x]} (:distinct-names P')))
      (is (some #{['x 'z]} (:distinct-names P'))))))

(deftest pretty-printing
  ;; Rules build formulas with vectors; they unify with, and are `=` to, lists.
  ;; `pretty` prints them all as lists, the paper's prefix notation.
  (is (= "(not (win (s 0)))" (pr-str (syntax/pretty '[not (win (s 0))]))))
  (is (= "[:call (not (win 0)) [:false]]"
         (pr-str (syntax/pretty [:call '[not (win 0)] [:false]])))))
