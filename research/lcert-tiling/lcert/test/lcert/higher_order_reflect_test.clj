(ns lcert.higher-order-reflect-test
  "Assessor's demo (not part of jpt4/sjas).  Reflection at higher-order types
  that consume certificates, and a self-similar delegating agent, run under
  three caps for a reflected program: its own declared budget m (the
  metatheory's model), its caller's cap n, and the charged cap n − ‖v‖ + m.

  Programs are type checked closed, at budget 0, and applied to certificate
  values built directly from codes, so that no certificate literal has to be
  type checked in a context of thousands of tokens."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.core :as lc]
            [lcert.ho-forms :as f]))

(defn code-of [form]
  (binding [s/*allow-resource-reflect* true] (:code (lc/certify 0 form))))

(defn cert-value
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

(defn certs
  "Runtime certificates for the given codes, with disjoint tokens.  Returns
  [values total-tokens]."
  [codes]
  (reduce (fn [[vs next] c] (let [[v n] (cert-value c next)] [(conj vs v) n]))
          [[] 1] codes))

(defn run-closed
  "Type check the closed program `form`, evaluate it erased under cap n, and
  apply the result to `args`."
  [mode n form & args]
  (binding [s/*allow-resource-reflect* true
            ev/*charged-cap* (= mode :charged)
            ev/*dynamic-cap* (= mode :caller)]
    (let [d (t/check-top 0 (s/parse-term [] form))
          v (#'ev/ev (ev/erase d) [] n {:erase? true})]
      (reduce (fn [acc a] (acc a)) v args))))

(defn sigma-code [x] (code-of (list 'pair f/sigma x 'star)))

(deftest third-order-argument
  (let [[[cf w] total] (certs [(code-of f/Fa) (sigma-code 1)])
        n (dec total)]
    (testing "the model's cap: the consumer built inside Fa runs under cap 0 and returns
              the default pair (0, ⋆), 'evidence' of T(0 ≠ 0)"
      (is (= [:pv 0 :star] (run-closed :fixed n f/driver-a cf w))))
    (testing "the charged cap returns the certified pair (1, ⋆)"
      (is (= [:pv 1 :star] (run-closed :charged n f/driver-a cf w))))
    (testing "so does the caller's cap"
      (is (= [:pv 1 :star] (run-closed :caller n f/driver-a cf w))))))

(deftest two-consumers-in-a-tensor
  (let [[[cf w1 w2] total] (certs [(code-of f/Fb) (sigma-code 1) (sigma-code 2)])
        n (dec total)]
    (is (= 0 (run-closed :fixed n f/driver-b cf w1 w2)))
    (is (= 3 (run-closed :charged n f/driver-b cf w1 w2)))
    (is (= 3 (run-closed :caller n f/driver-b cf w1 w2)))))

(deftest a-reusable-consumer
  (let [[[cf w1 w2] total] (certs [(code-of f/Fc) (sigma-code 1) (sigma-code 2)])
        n (dec total)]
    (is (= 0 (run-closed :fixed n f/driver-c cf w1 w2)))
    (is (= 3 (run-closed :charged n f/driver-c cf w1 w2)))
    (is (= 3 (run-closed :caller n f/driver-c cf w1 w2)))))

(defn supply-code
  "A supply for the agent: a spine of nodes whose left children are the given
  certificates' codes, ending in a leaf."
  [codes]
  (reduce (fn [rest c] [:sn :a c rest]) [:sl :a] (reverse codes)))

(defn chain
  "Run the agent on a supply holding `depth` agent certificates and then the
  leaf agent's certificate."
  [mode depth]
  (let [ca (code-of f/agent-form)
        cl (code-of f/leaf-agent)
        [[sv] total] (certs [(supply-code (concat (repeat depth ca) [cl]))])]
    (run-closed mode (dec total) f/agent-form sv)))

(deftest self-similar-delegation
  (testing "the agent certificate checks at its own type"
    (is (true? (binding [s/*allow-resource-reflect* true]
                 (lc/check (code-of f/agent-form) f/agent-type)))))
  (testing "no delegation: the top agent runs the leaf agent, which acts (2)"
    (doseq [mode [:fixed :charged]]
      (is (= [:pv 2 :star] (chain mode 0)))))
  (testing "one and two levels of delegation under the charged cap: the leaf agent acts (2)"
    (is (= [:pv 2 :star] (chain :charged 1)))
    (is (= [:pv 2 :star] (chain :charged 2))))
  (testing "under the model's cap, a trusted successor's own reflect falls back to the default:
            action 0 with 'evidence' of T(0 ≠ 0)"
    (is (= [:pv 0 :star] (chain :fixed 1)))
    (is (= [:pv 0 :star] (chain :fixed 2))))
  (testing "a supply whose certificate is not an agent's: the fallback action 1"
    (let [[[sv] total] (certs [(supply-code [(sigma-code 5)])])]
      (is (= [:pv 1 :star] (run-closed :charged (dec total) f/agent-form sv))))))

(deftest the-model-value-fails-only-on-unholdable-inputs
  ;; The charged cap gives the program decoded from Fc's certificate the cap
  ;; w′ = n − ‖v‖ + m.  Its consumer G is correct on every certificate of at
  ;; most w′ nodes, which is every certificate a run can still hold.  The
  ;; model's requirement for a reusable (ω) consumer is correctness up to the
  ;; caller's cap n.  Handing G a certificate bigger than w′ — more tokens
  ;; than remain after the reflection — shows its value falls outside that
  ;; requirement: it returns the default witness 0.
  (binding [s/*allow-resource-reflect* true
            ev/*charged-cap* true]
    (let [cf-code (code-of f/Fc)
          small (sigma-code 1)
          big (sigma-code 400)
          [[cf w1] total] (certs [cf-code small])
          n (dec total)
          wprime (- n (count (ev/tokens cf)))
          [bigv _] (cert-value big 100000)
          d (t/check-top 0 (s/parse-term [] (list 'fn '[cf 1 R]
                                                  (list 'inspect (list 'Sigma ['f 'w f/H] 'Unit) 'cf (list 'code f/Fc-type)
                                                        '[x e] (list (list 'reflect f/Fc-type 'x 'e) 'star)
                                                        '[x e] (list 'pair (list 'Sigma ['f 'w f/H] 'Unit) f/G 'star)))))
          g (let [v ((#'ev/ev (ev/erase d) [] n {:erase? true}) cf)] (second v))]
      (testing "a certificate the run can hold: the certified witness"
        (is (<= (ev/nodes w1) wprime))
        (is (= [:pv 1 :star] ((g w1) :star))))
      (testing "a certificate bigger than w′, which no run can hold here: the default witness 0"
        (is (> (ev/nodes bigv) wprime))
        (is (= [:pv 0 :star] ((g bigv) :star)))))))
