(require '[proflog.ast :as ast] '[proflog.kernel :as k] '[proflog.language :as language]
         '[proflog.query :as q] '[clojure.core.logic :as l])
(def zero (ast/app-term 'zero))
(defmacro timed [label expr]
  `(let [f# (future ~expr) r# (deref f# 20000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-20s (pr-str (if (seq? r#) (take 1 r#) r#)))) (flush)))
(def L (language/language {:constants ['zero] :relations {'r 1 'r2 1}}))
;; r(x) <- false   : s_P(r(0)) = false -> expect :fails
;; r2(x) <- true   : s_P(r2(0)) = true -> expect :succeeds
(def prog (ast/nom x y (language/compile-program L
  [(ast/clause 'r [x] (ast/false-form))
   (ast/clause 'r2 [y] (ast/true-form))])))
(timed "TF1 query-status r(0)  [r <- false; expect :fails]"    (q/query-status prog (ast/pos-lit (ast/app-term 'r zero)) {:timeout-ms 3000}))
(timed "TF2 query-status r2(0) [r2 <- true; expect :succeeds]" (q/query-status prog (ast/pos-lit (ast/app-term 'r2 zero)) {:timeout-ms 3000}))
(alter-var-root #'proflog.kernel/profiled-closeo (constantly (fn [& _] l/fail)))
(timed "TF3 generic kernel on (false) [should close]" (l/run 1 [p] (k/prove-programo (ast/false-form) '() '() '() {:clauses {}} 16 p)))
(shutdown-agents) (System/exit 0)
