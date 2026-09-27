(ns lcert.changed-system-test
  "Step 4 of proflog ADR-0143: tiling across a changed proof system — where
  trust stops (METATHEORY.md §4).  (Not part of jpt4/sjas.)

  A proof system here is a set of rule extensions (lcert.syntax/*extensions*)
  and a reflect class (*reflect-class*).  `chk′` names the Check of the
  system a program runs in, so a parent run in system P checks its
  successors' certificates with P's rules, whatever system produced them.

  Parent and children are delegating agents, Π(s :₁ R). σ: the parent reads
  a certificate from the front of its supply, checks it at its own type,
  trusts it (reflect, charged cap) and runs it on the rest; otherwise it
  takes the safe action 1.  Each test runs one child system against one
  parent system and asserts where trust stops."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.encode :as e]
            [lcert.check :as c]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.core :as lc]
            [lcert.examples :as ex]
            [lcert.ho-forms :as f]))

;; ---------------------------------------------------------------------------
;; Proof systems.

(def P0
  "λᶜᵉʳᵗ₀ with the charged, first-order reflect rule of Step 1, and no caseR."
  {:extensions #{} :reflect-class :first-order})

(def P1
  "P0 with the primitive destructor caseR (Step 2)."
  {:extensions #{:caseR} :reflect-class :first-order})

(def P1-all
  "λᶜᵉʳᵗ₁: P1 with reflect at every closed reflect-free type (Step 3)."
  {:extensions #{:caseR} :reflect-class :all})

(def P-con
  "λᶜᵉʳᵗ₁ with an axiom asserting its own code-level consistency, Con′_ω:
  true (by T1), and not derivable (by P5)."
  {:extensions #{:caseR :ax-con} :reflect-class :all})

(def P-bot
  "λᶜᵉʳᵗ₁ with an unsound axiom of the empty type."
  {:extensions #{:caseR :ax-bot} :reflect-class :all})

(defmacro in-system [sys & body]
  `(binding [s/*extensions* (:extensions ~sys)
             s/*reflect-class* (:reflect-class ~sys)]
     ~@body))

(defn- certify-in [sys form] (in-system sys (lc/certify 0 form)))

(defn- checks-in? [sys code type-form] (in-system sys (lc/check code type-form)))

;; ---------------------------------------------------------------------------
;; Agents.

(defn- agent-on
  "The delegating agent of lcert.ho-forms, with its supply destructor `out`."
  [out]
  (walk/postwalk-replace
   {'K ex/K-type 'SIGMA f/sigma 'AGENT f/agent-type 'FALLBACK f/fallback 'OUT out}
   f/agent-template))

(def agent-def "The agent on the definable out (itR): a P0 program." (agent-on ex/out-form))
(def agent-prim "The agent on the primitive out (caseR): a P1 program." (agent-on ex/out-prim-form))

(def agent-ho
  "A child that reflects at a higher-order type, (c) = 1 ⊸ Σ(f :ω H). 1, which
  only the :all class admits: it trusts a certificate of shape (c) from its
  supply, and hands the reusable consumer it gets the next certificate."
  (walk/postwalk-replace
   {'SIGMA f/sigma 'FC f/Fc-type 'FALLBACK f/fallback}
   '(fn [s 1 R]
      (case-r SIGMA s [a] FALLBACK
        [d a2 l r]
        (inspect SIGMA l (code FC)
          [x e] (let-pair SIGMA [g u] ((reflect FC x e) star)
                  (inspect SIGMA r (code SIGMA)
                    [y e2] ((g y) e2)
                    [y e2] FALLBACK))
          [x e] FALLBACK)))))

(def con-type
  "Con′_ω = Π(c :ω Syn). T(chk′ c c⊥) → 0: no code checks as a refutation."
  '(Pi [c w Syn] (-> (T (chk c c-bot)) Void)))

(def agent-con
  "A child whose certificate uses the axiom Con′_ω (it holds it, unused, and
  acts 3)."
  (list 'fn '[s 1 R] (list 'let ['k 'w con-type 'ax-con] (list 'pair f/sigma 3 'star))))

(def agent-bot
  "A child in the unsound system: it returns anything at all, by aborting on
  the axiom of the empty type."
  (list 'fn '[s 1 R] (list 'abort f/sigma 'ax-bot)))

;; ---------------------------------------------------------------------------
;; Runs.

(defn- cert-value
  "A runtime certificate with the shape of `code`, spending fresh tokens
  numbered from `start` in preorder.  Returns [value next]."
  [code start]
  (case (first code)
    :sl [[:rl (second code)] start]
    :sn (let [[_ l a b] code
              tok (ev/token start)
              [va n1] (cert-value a (inc start))
              [vb n2] (cert-value b n1)]
          [[:rn tok l va vb] n2])))

(defn- supply
  "The parent's supply: the child's certificate, then the child's own supply."
  [child-code rest-code]
  (let [[v next] (cert-value [:sn :a child-code rest-code] 1)] [v (dec next)]))

(defn- run-parent
  "Run the parent program `parent-form` in system `sys` on `supply-value` of
  `n` tokens, with the charged cap.  Returns the action pair and whether the
  parent's check accepted the child."
  [sys parent-form [supply-value n]]
  (let [events (atom [])]
    (in-system sys
      (binding [ev/*charged-cap* true
                ev/*trace* #(swap! events conj %)]
        (let [agent (#'ev/ev (ev/erase (t/check-top 0 (s/parse-term [] parent-form))) [] n {:erase? true})
              result (agent supply-value)
              first-inspect (first (filter #(= :inspect (:event %)) @events))]
          {:result result :accepted (:ok first-inspect)})))))

(def ^:private leaf-code (delay (:code (certify-in P0 f/leaf-agent))))
(def ^:private leaf-rest [:sl :a])

;; ---------------------------------------------------------------------------
;; The cases.

(deftest identical-system
  (testing "P0 trusts a P0 child: the delegation reaches the leaf agent's action 2"
    (let [child (:code (certify-in P0 agent-def))
          r (run-parent P0 agent-def (supply child (vec [:sn :a @leaf-code leaf-rest])))]
      (is (:accepted r))
      (is (= [:pv 2 :star] (:result r))))))

(deftest weaker-child
  (testing "P1 trusts a P0 child: its rules are a subset, so P1's Check accepts it"
    (let [child (:code (certify-in P0 agent-def))]
      (is (checks-in? P1 child f/agent-type))
      (let [r (run-parent P1 agent-prim (supply child [:sn :a @leaf-code leaf-rest]))]
        (is (:accepted r))
        (is (= [:pv 2 :star] (:result r)))))))

(deftest re-encoded-child
  (testing "a child whose codes use other labels is rejected; relabelling it back is
            free, and the parent re-checks the result, so the relabeller need not be trusted"
    (let [child (:code (certify-in P0 agent-def))
          swap {:Lam :App, :App :Lam}
          relabel (fn relabel [c] (case (first c)
                                    :sl [:sl (get swap (second c) (second c))]
                                    :sn [:sn (get swap (second c) (second c)) (relabel (nth c 2)) (relabel (nth c 3))]))
          foreign (relabel child)]
      (is (not (checks-in? P0 foreign f/agent-type)))
      (is (= :rejected (if (:accepted (run-parent P0 agent-def (supply foreign [:sn :a @leaf-code leaf-rest]))) :accepted :rejected)))
      (is (= child (relabel foreign)))
      (is (= (k/nodes child) (k/nodes (relabel foreign))) "no change of size")
      (is (checks-in? P0 (relabel foreign) f/agent-type)))))

(deftest definitional-extension
  (let [child (certify-in P1 agent-prim)
        translated (certify-in P0 agent-def)]
    (testing "a P1 child uses caseR, which P0 lacks: P0 rejects it and takes the safe action"
      (is (not (checks-in? P0 (:code child) f/agent-type)))
      (let [r (run-parent P0 agent-def (supply (:code child) [:sn :a @leaf-code leaf-rest]))]
        (is (false? (:accepted r)))
        (is (= [:pv 1 :star] (:result r)))))
    (testing "caseR is definable from itR, so the child can be translated into P0 — by
              replacing out on caseR with the definable out — and P0 trusts the translation"
      (is (checks-in? P0 (:code translated) f/agent-type))
      (let [r (run-parent P0 agent-def (supply (:code translated) [:sn :a @leaf-code leaf-rest]))]
        (is (:accepted r))
        (is (= [:pv 2 :star] (:result r)))))
    (testing "the translation costs tokens: the translated certificate is larger"
      (is (> (:nodes translated) (:nodes child))))))

(deftest stronger-reflection-rule
  (let [child (certify-in P1-all agent-ho)
        fc (:code (certify-in P1-all f/Fc))
        sig (:code (certify-in P1-all (list 'pair f/sigma 5 'star)))
        sup (supply (:code child) [:sn :a fc sig])]
    (testing "the child reflects at shape (c), sound by Step 3 but outside the first-order
              class: a first-order parent rejects it"
      (is (not (checks-in? P1 (:code child) f/agent-type)))
      (let [r (run-parent P1 agent-prim sup)]
        (is (false? (:accepted r)))
        (is (= [:pv 1 :star] (:result r)))))
    (testing "a parent in λᶜᵉʳᵗ₁ (:all) trusts it, and the child's consumer certifies 5"
      (let [r (run-parent P1-all agent-prim sup)]
        (is (:accepted r))
        (is (= [:pv 5 :star] (:result r)))))))

(deftest a-true-but-unprovable-axiom
  (testing "Con′_ω, the calculus's own code-level consistency, as an axiom: certified at
            budget 0 in P-con, where Check accepts it"
    (let [{:keys [code type]} (certify-in P-con 'ax-con)]
      (is (= (s/parse-type [] con-type) type))
      (is (checks-in? P-con code con-type))
      (is (not (checks-in? P1-all code con-type)) "λᶜᵉʳᵗ₁'s Check does not know the axiom")))
  (testing "a child that uses it is rejected by a λᶜᵉʳᵗ₁ parent, and trusted by a P-con one"
    (let [child (:code (certify-in P-con agent-con))
          sup (supply child leaf-rest)]
      (is (false? (:accepted (run-parent P1-all agent-prim sup))))
      (is (= [:pv 1 :star] (:result (run-parent P1-all agent-prim sup))))
      (is (= [:pv 3 :star] (:result (run-parent P-con agent-prim sup)))))))

(deftest an-unsound-axiom
  (testing "with an axiom of the empty type, Check accepts a refutation: T1 fails"
    (let [{:keys [code]} (certify-in P-bot 'ax-bot)]
      (is (in-system P-bot (c/check code e/c-bot)))
      (is (not (in-system P1-all (c/check code e/c-bot))))))
  (testing "a λᶜᵉʳᵗ₁ parent rejects the unsound child; a parent that adopted the axiom
            would trust it, and its 'certified' action is 0, with evidence of T(0 ≠ 0)"
    (let [child (:code (certify-in P-bot agent-bot))
          sup (supply child leaf-rest)]
      (is (false? (:accepted (run-parent P1-all agent-prim sup))))
      (is (= [:pv 1 :star] (:result (run-parent P1-all agent-prim sup))))
      (is (= [:pv 0 :star] (:result (run-parent P-bot agent-prim sup)))))))
