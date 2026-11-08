(require '[proflog.ast :as ast]
         '[proflog.kernel :as k]
         '[proflog.language :as language]
         '[proflog.query :as q]
         '[clojure.core.logic :as l])
(defn v [n] (ast/var-term n))
(def zero (ast/app-term 'zero))
(defn s [t] (ast/app-term 's t))
(defn P [t] (ast/app-term 'P t))
(defmacro timed [label expr]
  `(let [f# (future ~expr)
         r# (deref f# 30000 ::timeout)]
     (when (= r# ::timeout) (future-cancel f#))
     (println ~label "=>" (if (= r# ::timeout) :TIMEOUT-30s (pr-str (if (seq? r#) (take 1 r#) r#))))
     (flush)))

;; Disable ONLY the profiled sidecar handoff so the generic Fitting rule
;; clauses are tested alone.
(alter-var-root #'proflog.kernel/profiled-closeo (constantly (fn [& _] l/fail)))
(defn generic [f] (l/run 1 [p] (k/prove-programo f '() '() '() {:clauses {}} 64 p)))

(def f1 (ast/nom x y (ast/forall-form x (ast/exists-form y (ast/neq-lit (v y) (v x))))))
(def f2 (ast/nom x y
  (ast/forall-form x (ast/exists-form y
    (ast/or-form (ast/and-form (ast/pos-lit (P (v x))) (ast/neg-lit (P (v y))))
                 (ast/and-form (ast/neg-lit (P (v x))) (ast/pos-lit (P (v y)))))))))
(timed "G1 generic kernel only: AxEy(y!=x) [sat]" (generic f1))
(timed "G2 generic kernel only: AxEy(Px<->~Py) [sat]" (generic f2))

;; ---- Proflog-level (public query API) ----
;; Language L: constants zero, function s/1, relations r/1, t/1, rr/1.
(def L (language/language {:constants ['zero] :functions {'s 1} :relations {'r 1 'rr 1 't 1}}))
;; Q1 (beta leak): r(x) <- Ey((y = x & x = s(0)) | ~(y = x)).
;;   s_P(r(0)) = true  (Ey y!=0 holds in every weak Herbrand model).
;;   Correct status: :succeeds.  :fails or :inconsistent = UNSOUND.
(def prog1 (ast/nom x y
  (language/compile-program L
    [(ast/clause 'r [x] (ast/exists-form y (ast/or-form
        (ast/and-form (ast/eq-lit (v y) (v x)) (ast/eq-lit (v x) (s zero)))
        (ast/not-form (ast/eq-lit (v y) (v x))))))])))
(timed "Q1 query-fails r(0)   [s_P=true; any proof = UNSOUND]"
  (q/query-fails prog1 (ast/pos-lit (ast/app-term 'r zero)) 1 64))
(timed "Q1 query-status r(0)  [expect :succeeds]"
  (q/query-status prog1 (ast/pos-lit (ast/app-term 'r zero)) {:timeout-ms 5000}))

;; Q2 (delta / call escape): rr(x) <- Ey ~(y = x);  t(z) <- Ax rr(x).
;;   rr(g) true for every ground g of L, but Ax rr(x) is SATISFIABLE
;;   (Herbrand model, rr = everything), so t(0) must NOT fail.
(def prog2 (ast/nom x y z w
  (language/compile-program L
    [(ast/clause 'rr [x] (ast/exists-form y (ast/not-form (ast/eq-lit (v y) (v x)))))
     (ast/clause 't [z] (ast/forall-form w (ast/pos-lit (ast/app-term 'rr (v w)))))])))
(timed "Q2 query-fails t(0)   [satisfiable body; any proof = UNSOUND]"
  (q/query-fails prog2 (ast/pos-lit (ast/app-term 't zero)) 1 64))
(timed "Q2 query-status t(0)  [s_P = bottom; expect :unresolved]"
  (q/query-status prog2 (ast/pos-lit (ast/app-term 't zero)) {:timeout-ms 5000}))
(shutdown-agents)
(System/exit 0)
