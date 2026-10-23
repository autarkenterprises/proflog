(ns lcert.token-provisioning-test
  "Assessor's demo (not part of jpt4/sjas): where λᶜᵉʳᵗ's tokens come from,
  and how much of their provisioning λᶜᵉʳᵗ does itself. These are the
  executable claims of the 2026-09-28 self-justification note, Addendum F7.

  Everything after the root grant is internal:
    - a parent runs a child on the parent's own tokens, by application,
      once the child's token context is abstracted (Lam at usage 1);
    - reflect runs a decoded program on m of the certificate's own tokens,
      the first m in preorder, and burns the rest;
    - a lineage passes its supply down, and mints from it.

  The grant itself is not internal, and cannot be. It is the measure that
  every run's termination, and the induction behind T1, stand on.
    - A self-reproducing agent, given its own code as data and a supply,
      mints itself, checks itself and reflects itself. It goes exactly
      ⌊N / ‖c‖⌋ levels deep on a supply of N.
    - On a supply promised at 10^100, nothing inside the language stops it.
      Only the evaluator's materialization guard, which is outside the
      semantics (METATHEORY §2.3), does.
    - Tokens the cap does not account for make reflect's refusal reachable,
      and the refusal returns a default value: an action 0 with \"evidence\"
      that 0 ≠ 0. So the supply and the cap must be one number.

  These tests pin existing behaviour (green when written). Mutation checks
  made each of them fail: see LOG.md, 2026-10-22 and 2026-10-23."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [lcert.core :as lc]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.examples :as ex]
            [lcert.ho-forms :as f]))

;; ---------------------------------------------------------------------------
;; The runner's role, played inside the language.

(def ^:private child
  "A budget-2 program: a two-node certificate built from $1 and $2."
  '(node $1 :a (node $2 :b (leaf :c) (leaf :c)) (leaf :c)))

(def ^:private abstracted-child
  "The same program with its token context abstracted: ◇ ⊸ ◇ ⊸ R, closed."
  '(fn [d1 1 Dia] (fn [d2 1 Dia] (node d1 :a (node d2 :b (leaf :c) (leaf :c)) (leaf :c)))))

(deftest a-parent-runs-a-child-on-its-own-tokens
  (testing "abstracting the token context makes the child a closed function"
    (is (= 0 (:budget (lc/certify 0 abstracted-child)))))
  (testing "a parent with two tokens runs it by application, as the runner would"
    (let [direct (lc/run 2 child)
          hosted (lc/run 2 (list abstracted-child '$1 '$2))]
      (is (= direct hosted))
      ;; Both runs take their tokens from the same runner, so a fault there
      ;; changes both alike and the equality above still holds. This
      ;; absolute check is what catches it (mutation m6, LOG.md 2026-10-23).
      (is (= [(ev/token 1) (ev/token 2)] (ev/tokens hosted))))))

(deftest reflect-runs-the-decoded-program-on-the-certificates-own-tokens
  (binding [s/*reflect-class* :first-order]
    (let [{:keys [code nodes budget]} (lc/certify 2 child)
          ;; the parent mints the certificate from its own tokens $1 … $nodes,
          ;; in preorder, checks it at R, and reflects it
          parent (list 'inspect 'R (lc/certificate-form code) '(code R)
                       '[x e] '(reflect R x e)
                       '[x e] '(leaf :a))
          result (lc/run nodes parent)]
      (testing "the child's certificate declares its two tokens and has many more nodes"
        (is (= 2 budget))
        (is (< 2 nodes)))
      (testing "the decoded child runs on the certificate's first two tokens; the rest are burned"
        (is (= [(ev/token 1) (ev/token 2)] (ev/tokens result)))
        (is (= (ev/print-value (lc/run 2 child)) (ev/print-value result)))))))

;; ---------------------------------------------------------------------------
;; A lineage that provisions itself from one grant.

(defn- self-agent-with
  "Π(c :ω Syn). Π(s :₁ R). σ. Given its own code c, as data, and a supply s:
  mint c from s with the typed parser, check the certificate at its own
  type, reflect it, and run the result on c and the rest of the supply. If
  the supply is too short to mint c, the check fails and the agent takes the
  known-safe action 1. `finish` turns the child's σ-pair into this level's."
  [finish]
  (walk/postwalk-replace
   {'SIGMA f/sigma 'SELF f/mint-agent-type 'PARSE ex/parse-prim-form
    'FALLBACK f/fallback 'FINISH finish}
   '(fn [c w Syn]
      (fn [s 1 R]
        (let-pair SIGMA [t rest] (PARSE c s)
          (inspect SIGMA t (code SELF)
            [x e] (FINISH (((reflect SELF x e) c) rest))
            [x e] FALLBACK))))))

(def ^:private counting-agent
  "Each level adds one to its child's action, so the root's action is the
  number of levels below it plus one."
  (self-agent-with (list 'fn ['p 1 f/sigma]
                         (list 'let-pair f/sigma '[a ev] 'p
                               (list 'pair f/sigma '(succ a) 'star)))))

(def ^:private passing-agent
  "Each level returns its child's σ-pair unchanged, so the root returns what
  the deepest level returned."
  (self-agent-with (list 'fn ['p 1 f/sigma] 'p)))

(defn- certify-fo [form] (binding [s/*reflect-class* :first-order] (lc/certify 0 form)))

(defn- run-lineage
  "Run the closed agent `form` on its own `code` and a lazy supply promising
  `supply-n` nodes, under the evaluator cap `cap`, with the first-order
  reflect class and the charged cap. Returns the root's σ-pair (or the error
  type), the supply nodes materialized, and the reflections that ran and
  that were refused."
  [form code supply-n cap]
  (let [events (atom []) mat (atom 0)
        outcome (binding [s/*reflect-class* :first-order
                          ev/*charged-cap* true
                          ev/*trace* #(swap! events conj %)
                          ev/*materialized* mat]
                  (try
                    (let [agent (#'ev/ev (ev/erase (t/check-top 0 (s/parse-term [] form)))
                                         [] cap {:erase? true})]
                      {:result ((agent code) (ev/lazy-supply supply-n))})
                    (catch clojure.lang.ExceptionInfo e
                      {:error (:type (ex-data e))})))
        reflects (filter #(= :reflect (:event %)) @events)]
    (assoc outcome
           :spent @mat
           :ran (count (filter :runs reflects))
           :refused (count (remove :runs reflects)))))

(deftest a-lineage-goes-exactly-as-deep-as-its-supply-pays-for
  (let [{:keys [code nodes budget]} (certify-fo counting-agent)]
    (testing "the agent is closed, and certifies at its own type"
      (is (= 0 budget))
      (is (binding [s/*reflect-class* :first-order] (lc/check code f/mint-agent-type))))
    (testing "a supply of three times its size: three generations, all minted from that supply"
      (is (= {:result [:pv 4 :star] :spent (* 3 nodes) :ran 3 :refused 0}
             (run-lineage counting-agent code (* 3 nodes) (* 3 nodes)))))
    (testing "one token fewer: two generations; the third mint is truncated and refused by inspect"
      (is (= {:result [:pv 3 :star] :spent (dec (* 3 nodes)) :ran 2 :refused 0}
             (run-lineage counting-agent code (dec (* 3 nodes)) (dec (* 3 nodes))))))))

(deftest on-an-unbounded-promise-only-an-outside-guard-stops-it
  (let [{:keys [code nodes]} (certify-fo counting-agent)
        huge (.pow (biginteger 10) 100)]
    (testing "promised 10^100, the lineage runs on until the materialization guard throws"
      (binding [ev/*materialize-limit* (* 3 nodes)]
        (is (= {:error :lcert/materialization-limit :spent (* 3 nodes) :ran 3 :refused 0}
               (run-lineage counting-agent code huge huge)))))))

(deftest tokens-beyond-the-cap-fabricate-evidence
  (let [{:keys [code nodes]} (certify-fo passing-agent)]
    (testing "an honest grant, supply and cap equal: the lineage ends in the safe action 1"
      (is (= {:result [:pv 1 :star] :spent (* 2 nodes) :ran 2 :refused 0}
             (run-lineage passing-agent code (* 2 nodes) (* 2 nodes)))))
    (testing "twice the tokens the cap allows: a refused reflection returns the default, action 0"
      (let [{:keys [result ran refused]} (run-lineage passing-agent code (* 4 nodes) (* 2 nodes))]
        (is (= [:pv 0 :star] result) "σ claims 0 ≠ 0: false evidence")
        (is (= 2 ran))
        (is (= 1 refused))))))
