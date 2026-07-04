(ns proflog.sjas-step4-probe
  "ADR-0147 step 4 / BOT core: the full closure reduces to D* ^ P2 -> BOT, where
   D* = forall y z. not SemPrf^k(dk,y,z) (step 1, 'no bounded proof of Dk') and
   P2 = SemPrf^k(dk,p,bound) ('a bounded proof of Dk exists'). Two forms:

     CLOSURE (P2 concrete premise): D* ^ SemPrf^k(dk,p,bound)
        -> D*'s gamma clashes the CONCRETE P2 -> premise-clash -> CLOSES.
     STEP4   (P2 from proving Dk):  D* ^ (exists y z. SemPrf^k(dk,y,z))
        -> the exists delta-expands to a par witness that D*'s gamma must clash
           -> exists-delta gap -> does NOT close (construct-and-check).

   This pins the exact boundary: the BOT contradiction is executable GIVEN the
   fixed-point proof P2 (premise-clash), while DERIVING P2 (proving Dk universally
   = step 4) is the one place that needs the exists-delta construct-and-check fix.
   Default run covers only the small concrete case. `exists` runs the known
   construct-and-check gap. `all` runs those two bounded cases. `real` runs the
   real diagonal concrete premise case and must be launched only through the
   durable Proflog runner with a heap cap."
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- tie-body [t] (.-body ^clojure.core.logic.nominal.Tie t))

(defn mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- timed-valid-tree?
  [label system target tree fuel]
  (println :starting label)
  (flush)
  (let [started (System/nanoTime)
        result (boolean (tb/valid-tree? system target tree fuel))
        elapsed-ms (quot (- (System/nanoTime) started) 1000000)]
    (println label {:closes result :elapsed-ms elapsed-ms})
    (flush)
    result))

(defn step4-cases
  "Return the concrete BOT-core and unresolved exists-P2 step-4 cases.

   The concrete case is the useful continuation point: once a public proof code
   `p` for Dk is known, D-star clashes that concrete bounded-proof premise. The
   exists case deliberately preserves the current construct-and-check gap where
   an existential delta witness does not flow into the saved-literal clash."
  []
  (let [system (mul+pow-system)
        s (sjas/numeral 3) k sjas/one dk (sjas/numeral 5)
        p (sjas/numeral 7) bound (sjas/numeral 11)
        yn (nominal/nom (l/lvar 'y)) zn (nominal/nom (l/lvar 'z))
        semk (fn [proof bd] (ast/pos-lit (ast/app-term 'semprfk-alpha s k dk proof bd)))
        nsemk (fn [proof bd] (ast/neg-lit (ast/app-term 'semprfk-alpha s k dk proof bd)))
        ;; D* = forall y z. not SemPrf^k(s,k,dk,y,z)
        dstar (ast/forall-form yn (ast/forall-form zn (nsemk (ast/var-term yn) (ast/var-term zn))))
        b1 (tie-body (second dstar)) b2 (tie-body (second b1))
        c1 (tb/ast->canonical-child b1 {yn 'v0})
        c2 (tb/ast->canonical-child b2 {yn 'v0 zn 'v1})
        ;; --- CLOSURE: D* ^ P2(concrete) ---
        p2 (semk p bound)
        tgtC (ast/and-form p2 dstar)
        treeC (tb/flex-tableau-node system tgtC
                (tb/flex-tableau-node system p2
                  (tb/flex-tableau-node system dstar
                    (tb/canonical-flex-tableau-node system c1
                      (tb/canonical-flex-tableau-node system c2)))))
        ;; --- STEP4: D* ^ (exists y z SemPrf^k) ---
        en (nominal/nom (l/lvar 'ey)) fn' (nominal/nom (l/lvar 'ez))
        exq (ast/exists-form en (ast/exists-form fn' (semk (ast/var-term en) (ast/var-term fn'))))
        eb1 (tie-body (second exq)) eb2 (tie-body (second eb1))
        e1 (tb/ast->canonical-child eb1 {en 'v0})
        e2 (tb/ast->canonical-child eb2 {en 'v0 fn' 'v1})
        tgtS (ast/and-form exq dstar)
        treeS (tb/flex-tableau-node system tgtS
                (tb/flex-tableau-node system exq
                  (tb/canonical-flex-tableau-node system e1
                    (tb/canonical-flex-tableau-node system e2
                      (tb/flex-tableau-node system dstar
                        (tb/canonical-flex-tableau-node system c1
                          (tb/canonical-flex-tableau-node system c2)))))))]
    {:concrete-p2 {:label :CLOSURE-Dstar-and-concrete-P2
                   :system system
                   :target tgtC
                   :tree treeC
                   :fuel 400}
     :exists-p2 {:label :STEP4-Dstar-and-exists-P2
                 :system system
                 :target tgtS
                 :tree treeS
                 :fuel 400}}))

(defn real-concrete-p2-case
  "Concrete BOT-core case over the real Theorem 2.3 diagonal code.

   This still treats the bounded proof premise as an assumed concrete literal;
   it does not construct the proof code for Dk. The point is narrower and
   executable: once a public proof code is present, the real D-star formula
   closes against that concrete SemPrf^k premise through ordinary tableau
   expansion and saved-literal closure."
  []
  (let [system (mul+pow-system)
        diag (sjas/theorem23-diagonal system sjas/one)
        dk-code (:diagonal-code diag)
        y (nominal/nom (l/lvar 'real-y))
        z (nominal/nom (l/lvar 'real-z))
        semk (fn [proof bound]
               (sjas/semprfk-alpha (:system-code system)
                                   sjas/one
                                   dk-code
                                   proof
                                   bound))
        dstar (ast/forall-form y
                (ast/forall-form z
                  (ast/not-form
                    (semk (ast/var-term y) (ast/var-term z)))))
        cert (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        cert-value (sjas-code/bytes->u-grounding-code-value
                     (sjas-code/u-grounding-code-term-bytes cert))
        bound (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral (inc cert-value)))
        p2 (semk cert bound)
        target (ast/and-form p2 dstar)
        b1 (tie-body (second dstar))
        b2 (tie-body (second b1))
        c1 (tb/ast->canonical-child b1 {y 'v0})
        c2 (tb/ast->canonical-child b2 {y 'v0 z 'v1})
        c3 (list 'neg (second (second c2)))
        tree (tb/flex-tableau-node system target
               (tb/flex-tableau-node system p2
                 (tb/flex-tableau-node system dstar
                   (tb/canonical-flex-tableau-node system c1
                     (tb/canonical-flex-tableau-node system c2
                       (tb/canonical-flex-tableau-node system c3))))))]
    {:label :REAL-CLOSURE-Dstar-and-concrete-P2
     :system system
     :target target
     :tree tree
     :fuel 400}))

(defn run-case! [{:keys [label system target tree fuel]}]
  (timed-valid-tree? label system target tree fuel))

(defn -main [& args]
  (let [{:keys [concrete-p2 exists-p2]} (step4-cases)
        real-concrete-p2 (delay (real-concrete-p2-case))
        requested (set args)]
    (cond
      (contains? requested "all")
      (do
        (run-case! concrete-p2)
        (run-case! exists-p2))

      (contains? requested "real")
      (do
        (println :real-case-warning
                 "Run only through proflog-test-runner with timeout and JVM heap cap.")
        (flush)
        (run-case! @real-concrete-p2))

      (contains? requested "exists")
      (run-case! exists-p2)

      :else
      (run-case! concrete-p2))
    (println "step4 / BOT-core probe complete")))
