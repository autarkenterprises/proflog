(require '[proflog.ast :as ast] '[proflog.language :as language] '[proflog.query :as q])
(defn v [n] (ast/var-term n))
(def zero (ast/app-term 'zero))
(defmacro timed [label expr]
  `(let [f# (future ~expr) r# (deref f# 30000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-30s (pr-str (if (seq? r#) (take 1 r#) r#)))) (flush)))
(def L (language/language {:constants ['zero] :functions {'s 1} :relations {'p 1 'q 1 'q0 1}}))
;; p(x) <- false.   q(z) <- Ax(x = 0 | p(x)).   q0(z) <- Ax(x = 0).
;; Fitting: gamma with closed term s(0): s(0)=0 free-closes, p(s(0)) calls -> false closes.
;; So closed P-tableau for q(0) exists: expect :fails for both.
(def prog (ast/nom x y z w u (language/compile-program L
  [(ast/clause 'p [x] (ast/false-form))
   (ast/clause 'q [z] (ast/forall-form y (ast/or-form (ast/eq-lit (v y) zero) (ast/pos-lit (ast/app-term 'p (v y))))))
   (ast/clause 'q0 [w] (ast/forall-form u (ast/eq-lit (v u) zero)))])))
(timed "GM0 query-status q0(0) [Ax(x=0), call-free; expect :fails]" (q/query-status prog (ast/pos-lit (ast/app-term 'q0 zero)) {:timeout-ms 10000}))
(timed "GM1 query-status q(0)  [Ax(x=0 | p(x)); expect :fails]"     (q/query-status prog (ast/pos-lit (ast/app-term 'q zero)) {:timeout-ms 10000}))
(shutdown-agents) (System/exit 0)
