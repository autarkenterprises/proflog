(ns proflog.sjas-not-dk-probe
  "ADR-0147 detached probe: check the real-diagonal step-5 not-Dk tree.

   The tree is structurally validated by the fast synthetic depth-3 pin
   (`depth`-parameterized implies+clash certificates close in seconds); at the
   real diagonal's argument mass the checker run is a long single slice, so
   per the long-evidence practice (AGENTS.md practice 17; the optimization
   doctrine's 'very long-running tests are acceptable') this probe runs the
   real check detached and records the envelope, rather than pinning an
   interactive-budget test to a wall-clock guess.

   Run: lein probe-proflog-not-dk  (writes progress to stdout; exit 0 iff the
   tree is checker-accepted)."
  (:require [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- tie-nom [tie] (.-binding_nom ^clojure.core.logic.nominal.Tie tie))
(defn- tie-body [tie] (.-body ^clojure.core.logic.nominal.Tie tie))

(defn real-not-dk-case
  "Build [system target tree] for the real-diagonal step-5 not-Dk check."
  []
  (let [system (sjas/system {:profile :willard-sjas-total-multiplication
                             :functions (assoc sjas/total-multiplication-functions 'pow 2)
                             :relations sjas/total-multiplication-willard-relations
                             :beta [(ast/eq-lit sjas/one sjas/one)]
                             :code-format :u-grounding})
        diag (sjas/theorem23-diagonal system sjas/one)
        nbar (:skeleton-code diag)
        dk-code (:diagonal-code diag)
        dk (:diagonal diag)
        t1 (second dk)   h-nom (tie-nom t1) b1 (tie-body t1)
        t2 (second b1)   y-nom (tie-nom t2) b2 (tie-body t2)
        t3 (second b2)   z-nom (tie-nom t3) b3 (tie-body t3)
        c1 (tb/ast->canonical-child b1 {h-nom 'v0})
        c2 (tb/ast->canonical-child b2 {h-nom 'v0 y-nom 'v1})
        c3 (tb/ast->canonical-child b3 {h-nom 'v0 y-nom 'v1 z-nom 'v2})
        impl-left (second c3)
        impl-right (nth c3 2)
        cert (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        cert-value (sjas-code/bytes->u-grounding-code-value
                     (sjas-code/u-grounding-code-term-bytes cert))
        bound (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral (inc cert-value)))
        p1 (sjas/subst-code nbar dk-code)
        p2 (ast/pos-lit (second (sjas/semprfk-alpha (:system-code system) sjas/one
                                                    dk-code cert bound)))
        target (ast/and-form p1 (ast/and-form p2 dk))
        tree (tb/flex-tableau-node system target
               (tb/flex-tableau-node system p1
                 (tb/flex-tableau-node system (ast/and-form p2 dk)
                   (tb/flex-tableau-node system p2
                     (tb/flex-tableau-node system dk
                       (tb/canonical-flex-tableau-node system c1
                         (tb/canonical-flex-tableau-node system c2
                           (tb/canonical-flex-tableau-node system c3
                             (tb/canonical-flex-tableau-node system (list 'not impl-left)
                               (tb/canonical-flex-tableau-node system
                                 (list 'neg (second impl-left))))
                             (tb/canonical-flex-tableau-node system impl-right
                               (tb/canonical-flex-tableau-node system
                                 (list 'neg (second (second impl-right)))))))))))))]
    [system target tree]))

(defn -main [& _]
  (println ":PROBE real-diagonal-not-dk start" (java.util.Date.))
  (let [[system target tree] (real-not-dk-case)
        t0 (System/nanoTime)
        accepted? (tb/valid-tree? system target tree 120)
        sec (/ (Math/round (/ (- (System/nanoTime) t0) 1e7)) 100.0)]
    (println ":PROBE result" {:accepted accepted? :sec sec})
    (System/exit (if accepted? 0 1))))
