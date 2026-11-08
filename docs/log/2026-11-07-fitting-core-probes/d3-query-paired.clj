;; Paired D3 probe: the SAME subsidiary formula Ay(a(y) & ~a(s y)) reached
;; (r) through a negative call (normalizer emits once-forall), and
;; (r3) through a positive call (normalizer emits ordinary forall).
;; P: a(z) <- a(z);  r(x) <- Ey(~a(y) | a(s y));  r3(x) <- Ay(a(y) & ~a(s y))
;; s_P(r(0)) = true, s_P(r3(0)) = false; both need the same two gamma instances.
(require '[proflog.ast :as ast] '[proflog.language :as language] '[proflog.query :as q])
(defn v [n] (ast/var-term n))
(def zero (ast/app-term 'zero))
(defn s [t] (ast/app-term 's t))
(defn A [t] (ast/app-term 'a t))
(defmacro timed [label expr]
  `(let [f# (future ~expr) r# (deref f# 60000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-60s (pr-str (if (seq? r#) (take 1 r#) r#)))) (flush)))
(def L (language/language {:constants ['zero] :functions {'s 1} :relations {'a 1 'r 1 'r3 1}}))
(def prog (ast/nom x y z x3 y3
  (language/compile-program L
    [(ast/clause 'a [z] (ast/pos-lit (A (v z))))
     (ast/clause 'r [x] (ast/exists-form y (ast/or-form (ast/not-form (ast/pos-lit (A (v y))))
                                                        (ast/pos-lit (A (s (v y)))))))
     (ast/clause 'r3 [x3] (ast/forall-form y3 (ast/and-form (ast/pos-lit (A (v y3)))
                                                            (ast/neg-lit (A (s (v y3)))))))])))
(timed "r  (negative call -> once-forall) query-status r(0)  [s_P=true;  expect :succeeds]"
  (q/query-status prog (ast/pos-lit (ast/app-term 'r zero)) {:timeout-ms 20000}))
(timed "r3 (positive call -> forall)      query-status r3(0) [s_P=false; expect :fails]"
  (q/query-status prog (ast/pos-lit (ast/app-term 'r3 zero)) {:timeout-ms 20000}))
(shutdown-agents) (System/exit 0)
