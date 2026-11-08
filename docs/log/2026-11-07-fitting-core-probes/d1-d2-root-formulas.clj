(require '[proflog.ast :as ast]
         '[proflog.kernel :as k]
         '[proflog.language :as language]
         '[proflog.query :as q])
(defn v [n] (ast/var-term n))
(def zero (ast/app-term 'zero))
(defn s [t] (ast/app-term 's t))
(defn P [t] (ast/app-term 'P t))
(defmacro timed [label expr]
  `(let [f# (future ~expr)
         r# (deref f# 20000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-20s (pr-str (if (seq? r#) (take 1 r#) r#))))
     (flush)))

;; PROBE 1 (delta freshness): forall x exists y. y != x  is SATISFIABLE.
;; A sound tableau must NOT close it.
(def f1 (ast/nom x y
  (ast/forall-form x (ast/exists-form y (ast/neq-lit (v y) (v x))))))
(timed "P1 kernel/prove-program  AxEy(y!=x)  [satisfiable; closing = UNSOUND]"
  (k/prove-program {:clauses {}} f1 1 64))

;; PROBE 2 (delta freshness, equality-free): forall x exists y. (P(x) & ~P(y)) | (~P(x) & P(y))
;; satisfiable (domain {a,b}, P={a}); must NOT close.
(def f2 (ast/nom x y
  (ast/forall-form x (ast/exists-form y
    (ast/or-form (ast/and-form (ast/pos-lit (P (v x))) (ast/neg-lit (P (v y))))
                 (ast/and-form (ast/neg-lit (P (v x))) (ast/pos-lit (P (v y)))))))))
(timed "P2 kernel/prove-program  AxEy(Px<->~Py)  [satisfiable; closing = UNSOUND]"
  (k/prove-program {:clauses {}} f2 1 64))
(timed "P2' kernel/prove (profiled FO layer)  [satisfiable; closing = UNSOUND]"
  (k/prove f2 1 64))

;; PROBE 3 (beta sigma threading of branch-local param equality):
;; exists y. (y = 0 & 0 = s(0)) | y != 0   is SATISFIABLE (y := s(0)); must NOT close.
(def f3 (ast/nom y
  (ast/exists-form y
    (ast/or-form (ast/and-form (ast/eq-lit (v y) zero) (ast/eq-lit zero (s zero)))
                 (ast/neq-lit (v y) zero)))))
(timed "P3 kernel/prove-program  Ey((y=0 & 0=s0) | y!=0)  [satisfiable; closing = UNSOUND]"
  (k/prove-program {:clauses {}} f3 1 64))

;; CONTROL: exists x. x != x  is unsatisfiable -> SHOULD close.
(def c1 (ast/nom x (ast/exists-form x (ast/neq-lit (v x) (v x)))))
(timed "CONTROL  Ex(x!=x)  [unsat; should close]" (k/prove-program {:clauses {}} c1 1 64))
(shutdown-agents)
(System/exit 0)
