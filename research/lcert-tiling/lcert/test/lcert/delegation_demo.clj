(ns lcert.delegation-demo
  "Assessor's demo (not part of jpt4/sjas): a self-similar delegation chain,
  narrated step by step, and a supply-size scaling run.

    java ... clojure.main -m lcert.delegation-demo            both parts
    java ... clojure.main -m lcert.delegation-demo chain      the chain only
    java ... clojure.main -m lcert.delegation-demo scaling    scaling only"
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [lcert.syntax :as s]
            [lcert.encode :as e]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.core :as lc]
            [lcert.ho-forms :as f]))

(defn code-of [form]
  (binding [s/*allow-resource-reflect* true] (:code (lc/certify 0 form))))

(defn nodes-of [code]
  (if (= :sn (first code)) (+ 1 (nodes-of (nth code 2)) (nodes-of (nth code 3))) 0))

(defn cert-value
  "A runtime certificate with the shape of code, one fresh token per node."
  [code start]
  (case (first code)
    :sl [[:rl (second code)] start]
    :sn (let [[_ l a b] code
              tok (ev/token start)
              [va n1] (cert-value a (inc start))
              [vb n2] (cert-value b n1)]
          [[:rn tok l va vb] n2])))

(defn spare-tree
  "A balanced tree of k spare nodes.  Balanced, not a spine: the evaluator's
  runtime linearity check walks both subtrees at every node it builds, which is
  quadratic on a spine."
  [k]
  (if (zero? k)
    [:sl :a]
    (let [left (quot (dec k) 2)]
      [:sn :a (spare-tree left) (spare-tree (- (dec k) left))])))

(defn supply-code
  "The supply: a right spine whose left children are the given codes, ending
  in `tail`."
  ([codes] (supply-code codes [:sl :a]))
  ([codes tail] (reduce (fn [rest c] [:sn :a c rest]) tail (reverse codes))))

(defn run-agent
  "Type check the agent closed, evaluate it under cap n (the supply's size),
  and apply it to the supply.  mode is :fixed (the metatheory's cap: a
  reflected program runs under its own budget m) or :charged (n − ‖v‖ + m)."
  [mode supply-code' trace]
  (let [[sv next] (cert-value supply-code' 1)
        n (dec next)]
    (binding [s/*allow-resource-reflect* true
              ev/*charged-cap* (= mode :charged)
              ev/*trace* trace]
      (let [d (t/check-top 0 (s/parse-term [] f/agent-form))
            agent-value (#'ev/ev (ev/erase d) [] n {:erase? true})]
        {:tokens n :result (agent-value sv)}))))

(defn fmt [n] (format "%,d" n))

(defn narrate-chain []
  (let [ca (code-of f/agent-form)
        cl (code-of f/leaf-agent)
        type-names {(e/enc-exp (s/parse-type [] f/agent-type)) "Agent"
                    (e/enc-exp (s/parse-type [] f/sigma)) "Σ"}
        name-of (fn [D-or-code]
                  (or (type-names D-or-code)
                      (type-names (e/enc-exp D-or-code))
                      "?"))]
    (println "== The programs ==")
    (println "Safe action type  sigma = Σ(x :ω Nat). T(x ≠ 0)   (an action x with evidence x ≠ 0)")
    (println "Agent type        Agent = Π(s :₁ R). sigma        (given a supply, return a safe action)")
    (println "The agent, abbreviations unexpanded:")
    (pp/pprint f/agent-template)
    (println "The leaf agent:   " (pr-str (list 'fn '[s 1 R] (list 'pair 'SIGMA 2 'star))))
    (println)
    (println "Certificate of the agent:     " (fmt (nodes-of ca)) "nodes (declared budget 0: it is a closed program)")
    (println "Certificate of the leaf agent:" (fmt (nodes-of cl)) "nodes")
    (doseq [depth [0 1 2]
            mode [:fixed :charged]]
      (let [codes (concat (repeat depth ca) [cl])
            events (atom [])
            {:keys [tokens result]} (run-agent mode (supply-code codes) #(swap! events conj %))
            [_ x _] result]
        (println)
        (println (format "== %d agent level%s, then the leaf agent; cap mode %s ==" depth (if (= depth 1) "" "s") (name mode)))
        (println "Supply:" (str/join " → " (concat (map #(str "node[" (if (= % ca) "agent cert" "leaf-agent cert") "]") codes) ["leaf"]))
                 (str "(" (fmt tokens) " tokens, one per node)"))
        (doseq [{:keys [event depth nodes ok cap runs m decoded-cap type type-code within-cap]} @events]
          (let [pad (apply str (repeat (* 2 depth) " "))]
            (case event
              :inspect (println (str pad "level " depth ": inspect a " (fmt nodes) "-node certificate against " (name-of type-code)
                                     " → " (if ok "valid" "not valid")))
              :reflect (if runs
                         (println (str pad "level " depth ": reflect it at " (name-of type) " (cap here " (fmt cap)
                                       "): runs the decoded program with " m " tokens under cap " (fmt decoded-cap)
                                       "; " (fmt (- nodes m)) " tokens burnt"))
                         (println (str pad "level " depth ": reflect it at " (name-of type) " (cap here " (fmt cap) "): "
                                       (if within-cap "the check fails" (str "refused, " (fmt nodes) " nodes > cap " (fmt cap)))
                                       " → returns the default value"))))))
        (println "Result:" (pr-str result)
                 (if (pos? x) (str "— action " x ", evidence valid (" x " ≠ 0)")
                     (str "— action " x ", evidence claims T(0 ≠ 0): false")))))))

(defn scaling []
  (let [ca (code-of f/agent-form)
        cl (code-of f/leaf-agent)]
    (println)
    (println "== Supply size: the same agent program, spare tokens appended after the leaf agent's certificate ==")
    (println "The agent's certificate has" (fmt (nodes-of ca)) "nodes in every run; only the supply grows.")
    (println (format "%12s  %14s  %10s  %s" "spare tokens" "supply tokens" "seconds" "result"))
    (doseq [k [0 21135 42270 200000 1000000]]
      (System/gc)
      (let [t0 (System/nanoTime)
            {:keys [tokens result]} (run-agent :charged (supply-code [ca cl] (spare-tree k)) nil)
            secs (/ (- (System/nanoTime) t0) 1e9)]
        (println (format "%12s  %14s  %10.2f  %s" (fmt k) (fmt tokens) secs (pr-str result)))))))

(defn -main [& args]
  (let [what (set args)]
    (when (or (empty? what) (what "chain")) (narrate-chain))
    (when (or (empty? what) (what "scaling")) (scaling))
    (shutdown-agents)))
