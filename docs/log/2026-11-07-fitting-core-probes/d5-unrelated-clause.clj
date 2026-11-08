;; D5 locality check: is q0(0)'s answer changed by an UNRELATED clause?
;; L: zero, s/1. q0(w) <- Au(u = 0). s_P(q0(0)) = false (u := s(0) gives s(0)=0).
(require '[proflog.ast :as ast] '[proflog.language :as language] '[proflog.query :as q])
(defn v [n] (ast/var-term n))
(def zero (ast/app-term 'zero))
(defn run [label prog]
  (let [f (future (q/query-status prog (ast/pos-lit (ast/app-term 'q0 zero)) {:timeout-ms 10000}))
        r (deref f 30000 ::timeout)]
    (println label "=>" r) (flush)))
(def L (language/language {:constants ['zero] :functions {'s 1} :relations {'q0 1 'p 1 'q 1}}))
(def alone (ast/nom w u (language/compile-program L [(ast/clause 'q0 [w] (ast/forall-form u (ast/eq-lit (v u) zero)))])))
(def with-unrelated (ast/nom w u x z
  (language/compile-program L
    [(ast/clause 'q0 [w] (ast/forall-form u (ast/eq-lit (v u) zero)))
     (ast/clause 'p [x] (ast/false-form))
     (ast/clause 'q [z] (ast/pos-lit (ast/app-term 'p (v z))))])))
(run "q0(0), program = {q0}                [expect :fails]" alone)
(run "q0(0), program = {q0, p<-false, q<-p} [expect :fails]" with-unrelated)
(shutdown-agents) (System/exit 0)
