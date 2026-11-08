(require '[proflog.ast :as ast] '[proflog.kernel :as k] '[proflog.language :as language])
(defn v [n] (ast/var-term n))
(defn s [t] (ast/app-term 's t))
(defn A [t] (ast/app-term 'a t))
(defmacro timed [label expr]
  `(let [f# (future ~expr) r# (deref f# 60000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-60s (pr-str (if (seq? r#) (take 1 r#) r#)))) (flush)))
(def L (language/language {:constants ['zero] :functions {'s 1} :relations {'a 1}}))
(def prog (ast/nom z (language/compile-program L [(ast/clause 'a [z] (ast/pos-lit (A (v z))))])))
(defn body [y] (ast/and-form (ast/pos-lit (A (v y))) (ast/neg-lit (A (s (v y))))))
(timed "K3 forall      y (a(y) & ~a(s y)), a defined [unsat]" (k/prove-program prog (ast/nom y (ast/forall-form y (body y))) 1 256))
(timed "K4 once-forall y (a(y) & ~a(s y)), a defined [unsat]" (k/prove-program prog (ast/nom y (ast/once-forall-form y (body y))) 1 256))
(shutdown-agents) (System/exit 0)
