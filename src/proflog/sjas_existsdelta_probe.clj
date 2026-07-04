(ns proflog.sjas-existsdelta-probe
  "ADR-0147: exists-delta / forall-gamma clash idiom in construct-and-check,
   with soundness falsifiers.
     VALID   : (exists x. subst-code(1,x)) ^ (forall y. not subst-code(1,y)) -> should CLOSE (currently does NOT)
     INVALID1: (exists x. subst-code(1,x)) alone                             -> must NOT close
     INVALID2: (exists x. subst-code(2,x)) ^ (forall y. not subst-code(1,y)) -> must NOT close (args differ)
     CONTROL : subst-code(1,5) ^ (forall y. not subst-code(1,y))             -> closes
   Run: lein run -m proflog.sjas-existsdelta-probe

   FINDING (checker-fix attempt): the delta rule substitutes the binder to a rigid
   `(par vK)` witness; the canonical child arrives as `(var vK)`. The gap has TWO
   layers, only the first of which is a one-goal fix:
     1. CHILD VALIDATION (solved in a prototype): a matcher arm that, under
        ::ground-check, normalizes `(par X)`->`(var X)` and compares reconciles
        the same-nom var child against the par witness (VALIDATION-ONLY, branch
        par untouched). Verified firing/matching by diagnostic; invalids still
        rejected, control still closes.
     2. WITNESS-TO-CLASH FLOW (unresolved): even with (1), a diagnostic on the
        complementary-clash showed the par witness NEVER appears in the lit list
        at any clash point, so the forall-gamma never clashes it. The exists-delta
        arm's continuation does not surface the par witness where the subsequent
        universal's gamma-var can bind it. This is the deeper blocker; the
        prototype checker changes were REVERTED to keep the checker sound and
        clean until (2) is understood."
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]))

(defn- tie-body [t] (.-body ^clojure.core.logic.nominal.Tie t))
(defn- mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- univ [arg]
  (let [yn (nominal/nom (l/lvar 'yn))]
    [yn (ast/forall-form yn (ast/neg-lit (ast/app-term 'subst-code arg (ast/var-term yn))))]))
(defn- exq [arg]
  (let [xn (nominal/nom (l/lvar 'xn))]
    [xn (ast/exists-form xn (ast/pos-lit (ast/app-term 'subst-code arg (ast/var-term xn))))]))

(defn -main [& _]
  (let [system (mul+pow-system)
        one sjas/one two (sjas/numeral 2)
        [un1 u1] (univ one) [xn1 e1] (exq one) [xn2 e2] (exq two)
        eb (fn [xn e name] (tb/ast->canonical-child (tie-body (second e)) {xn name}))
        ub (fn [un u name] (tb/ast->canonical-child (tie-body (second u)) {un name}))
        ;; VALID: exists(1) delta v0, forall(1) gamma v1 -> clash
        tgtV (ast/and-form e1 u1)
        treeV (tb/flex-tableau-node system tgtV
                (tb/flex-tableau-node system e1
                  (tb/canonical-flex-tableau-node system (eb xn1 e1 'v0)
                    (tb/flex-tableau-node system u1
                      (tb/canonical-flex-tableau-node system (ub un1 u1 'v1))))))
        ;; INVALID1: exists(1) alone
        treeI1 (tb/flex-tableau-node system e1
                 (tb/canonical-flex-tableau-node system (eb xn1 e1 'v0)))
        ;; INVALID2: exists(2) delta, forall(1) gamma -> args differ, no clash
        tgtI2 (ast/and-form e2 u1)
        treeI2 (tb/flex-tableau-node system tgtI2
                 (tb/flex-tableau-node system e2
                   (tb/canonical-flex-tableau-node system (eb xn2 e2 'v0)
                     (tb/flex-tableau-node system u1
                       (tb/canonical-flex-tableau-node system (ub un1 u1 'v1))))))
        ;; CONTROL: concrete premise
        prem (ast/pos-lit (ast/app-term 'subst-code one (sjas/numeral 5)))
        tgtC (ast/and-form prem u1)
        treeC (tb/flex-tableau-node system tgtC
                (tb/flex-tableau-node system prem
                  (tb/flex-tableau-node system u1
                    (tb/canonical-flex-tableau-node system (ub un1 u1 'v0)))))]
    (println :VALID-closes (boolean (tb/valid-tree? system tgtV treeV 300)))
    (println :INVALID1-exists-alone-rejected (not (boolean (tb/valid-tree? system e1 treeI1 300))))
    (println :INVALID2-arg-mismatch-rejected (not (boolean (tb/valid-tree? system tgtI2 treeI2 300))))
    (println :CONTROL-premise-closes (boolean (tb/valid-tree? system tgtC treeC 300)))
    (println "exists-delta reconciliation probe complete")))
