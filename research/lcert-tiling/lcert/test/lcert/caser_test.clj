(ns lcert.caser-test
  "Step 2 of proflog ADR-0143: caseR, the primitive, constant-time
  eliminator for certificates (METATHEORY.md §2.1).  (Not part of jpt4/sjas.)

    Γ₁ ⊢ r :¹ R    Γ ⊢ X type    Γ₂, a :ω Lbl ⊢ t_l :¹ X
    Γ₂, d :₁ ◇, a :ω Lbl, r₁ :₁ R, r₂ :₁ R ⊢ t_n :¹ X
    ───────────────────────────────────────────────────────
    Γ₁ + Γ₂ ⊢ caseR_X(r, a. t_l, d a r₁ r₂. t_n) :¹ X

  Surface form: (case-r X r [a] t_l [d a r1 r2] t_n).  It takes one node
  apart and hands its token and children to the node branch; the definable
  destructor `out` iterates over the whole tree instead."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.encode :as e]
            [lcert.typing :as t]
            [lcert.check :as c]
            [lcert.reduce :as r]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.core :as lc]
            [lcert.examples :as ex]))

(defn- run [n form] (ev/run n (s/parse-term (s/token-scope n) form)))

(defn- run-both
  "Evaluate `form` in Θₙ with the erasing and the non-erasing evaluator."
  [n form]
  (let [d (lc/check-program n form)]
    [(ev/eval-deriv d n {:erase? true}) (ev/eval-deriv d n {:erase? false})]))

(def count-top
  "λ(r :₁ R). caseR: 0 on a leaf, 1 on a node (its token discarded)."
  '(fn [r 1 R] (case-r Nat r [a] zero [d a2 r1 r2] (succ zero))))

(def swap-children
  "λ(r :₁ R). rebuild the top node with its children swapped, reusing its token."
  '(fn [r 1 R] (case-r R r [a] (leaf a) [d a2 r1 r2] (node d a2 r2 r1))))

(deftest typing-and-usages
  (testing "caseR types at its motive"
    (is (= [:Pi 1 [:R] [:Nat]] (:type (lc/check-program 0 count-top))))
    (is (= [:Pi 1 [:R] [:R]] (:type (lc/check-program 0 swap-children)))))
  (testing "the node's token and children are usage 1: using one twice is a type error"
    (is (thrown? clojure.lang.ExceptionInfo
                 (lc/check-program 0 '(fn [r 1 R] (case-r R r [a] (leaf a) [d a2 r1 r2] (node d a2 r1 r1))))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (lc/check-program 0 '(fn [r 1 R] (case-r R r [a] (leaf a)
                                                    [d a2 r1 r2] (node d a2 (node d a2 r1 r2) (leaf a2))))))))
  (testing "the label is usage ω in both branches"
    (is (lc/check-program 0 '(fn [r 1 R] (case-r (prod Lbl Lbl) r [a] (pair (prod Lbl Lbl) a a)
                                             [d a2 r1 r2] (pair (prod Lbl Lbl) a2 a2))))))
  (testing "the scrutinee is consumed: it cannot be used again"
    (is (thrown? clojure.lang.ExceptionInfo
                 (lc/check-program 0 '(fn [r 1 R] (case-r R r [a] r [d a2 r1 r2] r))))))
  (testing "the branches share their outer context, as if's do"
    (is (lc/check-program 0 '(fn [q 1 R] (fn [r 1 R] (case-r R r [a] q [d a2 r1 r2] (node d a2 q r2))))))))

(deftest check-accepts-caser-derivations
  (doseq [form [count-top swap-children]]
    (let [{:keys [code type]} (lc/certify 0 form)]
      (is (c/check code (e/enc-exp type)) (pr-str form))
      (testing "and strict overhead holds for them"
        (is (< (k/budget code) (k/nodes code))))))
  (testing "a derivation whose node-branch context is tampered with is rejected"
    (let [d (lc/check-program 0 count-top)
          ;; the Lam's body is the CaseR node; drop the node branch's last binder
          caser (get-in d [:prems 1])
          bad (update-in d [:prems 1 :prems 3 :ctx] pop)]
      (is (= :CaseR (:rule caser)))
      (is (not (c/check (e/enc-deriv bad) (e/enc-exp (:type d))))))))

(deftest the-extension-is-a-change-of-proof-system
  (let [{:keys [code type]} (lc/certify 0 count-top)]
    (testing "without the caseR extension, Check rejects the same certificate"
      (binding [s/*extensions* #{}]
        (is (not (c/check code (e/enc-exp type))))))
    (testing "and the type checker refuses the term"
      (binding [s/*extensions* #{}]
        (is (thrown? clojure.lang.ExceptionInfo (lc/check-program 0 count-top)))))
    (testing "certificates that do not use caseR are accepted either way"
      (let [{nc :code nt :type} (lc/certify 0 ex/not-form)]
        (is (c/check nc (e/enc-exp nt)))
        (binding [s/*extensions* #{}] (is (c/check nc (e/enc-exp nt))))))))

(deftest conversion-computes-caser
  (testing "ι-rules on constructor forms"
    (is (= [:succ [:zero]]
           (r/nf [:caseR [:Nat] [:node [:var 0] [:lbl :a] [:leaf [:lbl :b]] [:leaf [:lbl :c]]]
                  [:zero] [:succ [:zero]]])))
    (is (= [:lbl :b]
           (r/nf [:caseR [:Lbl] [:leaf [:lbl :b]] [:var 0] [:var 2]]))))
  (testing "a type that needs the ι-rule to check"
    (is (lc/check-program 0 '(the (T (case-r Bool (leaf :a) [a] tt [d a2 r1 r2] ff)) star))))
  (testing "no ι-rule without the extension"
    (binding [s/*extensions* #{}]
      (is (nil? (r/contract [:caseR [:Lbl] [:leaf [:lbl :b]] [:var 0] [:var 2]]))))))

(deftest both-evaluators-compute-caser
  (is (= [0 0] (run-both 0 (list count-top '(leaf :a)))))
  (is (= [1 1] (run-both 1 (list count-top '(node $1 :a (leaf :b) (leaf :c))))))
  (testing "the node branch receives the node's own token: swapping children keeps the tokens"
    (let [v (run 2 (list swap-children '(node $1 :a (node $2 :b (leaf :c) (leaf :c)) (leaf :b))))]
      (is (= [:sn :a [:sl :b] [:sn :b [:sl :c] [:sl :c]]] (ev/print-value v)))
      (is (= 2 (ev/nodes v)))
      (is (apply distinct? (ev/tokens v))))))

(defn- trees
  "Some certificate forms of up to three nodes, built from $1 .. $3."
  []
  ['(leaf :a)
   '(node $1 :a (leaf :b) (leaf :c))
   '(node $1 :a (node $2 :b (leaf :c) (leaf :c)) (leaf :b))
   '(node $1 :a (leaf :b) (node $2 :c (leaf :b) (node $3 :a (leaf :b) (leaf :c))))])

(deftest primitive-out-agrees-with-the-definable-one
  (doseq [tr (trees)]
    (testing (pr-str tr)
      (is (= (ev/print-value (run 3 (list ex/roll-form (list ex/out-form tr))))
             (ev/print-value (run 3 (list ex/roll-form (list ex/out-prim-form tr))))
             (ev/print-value (run 3 tr))))
      (testing "the views agree on the flag and the label"
        (let [view (fn [out] (run 3 (list 'let-pair '(prod Bool Lbl) '[b q] (list out tr)
                                          '(let-pair (prod Bool Lbl) [a f] q (pair (prod Bool Lbl) b a)))))]
          (is (= (view ex/out-form) (view ex/out-prim-form))))))))

(defn- spine-form
  "A supply: a right spine of n nodes with leaf left children, spending
  $1 .. $n."
  [n]
  (reduce (fn [rest i] (list 'node (symbol (str "$" i)) :a '(leaf :a) rest))
          '(leaf :a) (range n 0 -1)))

(deftest the-parser-on-caser
  (let [code [:sn :a [:sn :b [:sl :c] [:sl :b]] [:sn :c [:sl :a] [:sl :b]]]  ; 3 nodes
        n 7
        prog (fn [parse] (list 'let-pair '(tensor Syn Syn) '[t rest]
                               (list parse (list 'code-literal code) (spine-form n))
                               '(pair (tensor Syn Syn) (print t) (print rest))))]
    (testing "the primitive parser builds the certificate of the code and returns the unused supply"
      (let [[_ printed rest] (run n (prog ex/parse-prim-form))]
        (is (= code printed))
        (is (= (- n (k/nodes code)) (k/nodes rest)))))
    (testing "it agrees with the parser built on the definable out"
      (is (= (run n (prog ex/parse-form)) (run n (prog ex/parse-prim-form)))))))
