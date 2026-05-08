(ns proflog.kernel.robinson-q-profile
  "Kernel-interleaved Robinson-Q theory rules.

   The `:robinson-q` proof profile binds `robinson-q-theory-closeo` into the
   ordinary tableau kernel. The rules here are miniKanren relations over the
   current branch state: they close disequalities modulo Q conversion and close
   Q3's predecessor-or-zero branch only after ordinary tableau steps have
   exposed the relevant disequalities."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [!= == conde fresh lcons membero run]]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel :as kernel]
            [proflog.kernel-support :as support]
            [proflog.subst :as subst]))

(declare q-normal-termo)

(defn- q-normal-argso
  "Relationally normalize an argument list under Robinson-Q conversion."
  [terms out proof]
  (conde
    [(== '() terms)
     (== '() out)
     (== '(q-normal-args-done) proof)]
    [(fresh [head tail head-out tail-out head-proof tail-proof]
       (== (lcons head tail) terms)
       (== (lcons head-out tail-out) out)
       (q-normal-termo head head-out head-proof)
       (q-normal-argso tail tail-out tail-proof)
       (== (list 'q-normal-arg head-proof tail-proof) proof))]))

(defn- q-normal-generic-appo
  "Normalize a non-arithmetic application structurally."
  [term out proof]
  (fresh [head args args-out args-proof]
    (== (lcons 'app (lcons head args)) term)
    (!= head 'zero)
    (!= head 's)
    (!= head 'add)
    (!= head 'mul)
    (q-normal-argso args args-out args-proof)
    (== (lcons 'app (lcons head args-out)) out)
    (== (list 'q-normal-app args-proof) proof)))

(defn- q-neutral-right-termo
  "Recognize a normalized term that is not a Q zero or successor root.

   Q's recursive arithmetic equations inspect the right argument. Once that
   argument is normalized, only `zero` and `s(_)` can trigger another root
   rewrite; variables, parameters, and other applications must remain symbolic.
   This positive recognizer avoids trying to encode \"not any successor\" with a
   disequality against a fresh predecessor, which core.logic quite reasonably
   treats as a satisfiable constraint.
   "
  [term]
  (conde
    [(fresh [nom]
       (== (list 'var nom) term))]
    [(fresh [nom]
       (== (list 'par nom) term))]
    [(fresh [head args]
       (== (lcons 'app (lcons head args)) term)
       (!= head 'zero)
       (!= head 's))]))

(defn q-normal-termo
  "Relate a known Q term to its normal form and conversion proof.

   This relation is directional in practice: the kernel supplies `term`, and the
   relation produces `out`. It is still a miniKanren relation, so Q conversion is
   now part of proof search rather than a host-side formula preprocessing pass."
  [term out proof]
  (conde
    [(fresh [nom]
       (== (list 'var nom) term)
       (== term out)
       (== '(q-normal-var) proof))]
    [(fresh [nom]
       (== (list 'par nom) term)
       (== term out)
       (== '(q-normal-par) proof))]
    [(== (list 'app 'zero) term)
     (== term out)
     (== '(q-normal-zero) proof)]
    [(fresh [arg arg-out arg-proof]
       (== (list 'app 's arg) term)
       (q-normal-termo arg arg-out arg-proof)
       (== (list 'app 's arg-out) out)
       (== (list 'q-normal-s arg-proof) proof))]

    ;; Q4 / add-zero conversion.
    [(fresh [x y x-out y-out x-proof y-proof]
       (== (list 'app 'add x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (== (list 'app 'zero) y-out)
       (== x-out out)
       (== (list 'q-normal-add
                 x-proof
                 y-proof
                 (list 'q-rewrite
                       :add-zero
                       (list 'app 'add x-out y-out)
                       x-out))
           proof))]
    ;; Q5 / add-successor conversion.
    [(fresh [x y x-out y-out predecessor rewritten rewritten-proof
             x-proof y-proof]
       (== (list 'app 'add x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (== (list 'app 's predecessor) y-out)
       (== (list 'app 's (list 'app 'add x-out predecessor)) rewritten)
       (q-normal-termo rewritten out rewritten-proof)
       (== (list 'q-normal-add
                 x-proof
                 y-proof
                 (list 'q-rewrite
                       :add-succ
                       (list 'app 'add x-out y-out)
                       rewritten)
                 rewritten-proof)
           proof))]
    ;; Unreduced addition with symbolic right argument.
    [(fresh [x y x-out y-out x-proof y-proof]
       (== (list 'app 'add x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (q-neutral-right-termo y-out)
       (== (list 'app 'add x-out y-out) out)
       (== (list 'q-normal-add-neutral x-proof y-proof) proof))]

    ;; Q6 / mul-zero conversion.
    [(fresh [x y x-out y-out x-proof y-proof]
       (== (list 'app 'mul x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (== (list 'app 'zero) y-out)
       (== (list 'app 'zero) out)
       (== (list 'q-normal-mul
                 x-proof
                 y-proof
                 (list 'q-rewrite
                       :mul-zero
                       (list 'app 'mul x-out y-out)
                       (list 'app 'zero)))
           proof))]
    ;; Q7 / mul-successor conversion.
    [(fresh [x y x-out y-out predecessor rewritten rewritten-proof
             x-proof y-proof]
       (== (list 'app 'mul x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (== (list 'app 's predecessor) y-out)
       (== (list 'app 'add (list 'app 'mul x-out predecessor) x-out) rewritten)
       (q-normal-termo rewritten out rewritten-proof)
       (== (list 'q-normal-mul
                 x-proof
                 y-proof
                 (list 'q-rewrite
                       :mul-succ
                       (list 'app 'mul x-out y-out)
                       rewritten)
                 rewritten-proof)
           proof))]
    ;; Unreduced multiplication with symbolic right argument.
    [(fresh [x y x-out y-out x-proof y-proof]
       (== (list 'app 'mul x y) term)
       (q-normal-termo x x-out x-proof)
       (q-normal-termo y y-out y-proof)
       (q-neutral-right-termo y-out)
       (== (list 'app 'mul x-out y-out) out)
       (== (list 'q-normal-mul-neutral x-proof y-proof) proof))]

    [(q-normal-generic-appo term out proof)]))

(defn- q-convert-neq-closeo
  "Close a disequality whose sides are equal modulo Robinson-Q conversion."
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [lit left right walked-left walked-right
          normal-left normal-right left-proof right-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (equality/walk*o left sigma walked-left)
    (equality/walk*o right sigma walked-right)
    (q-normal-termo walked-left normal-left left-proof)
    (q-normal-termo walked-right normal-right right-proof)
    (== normal-left normal-right)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled
              'robinson-q
              (list 'q-convert-close left-proof right-proof))
        proof)))

(defn- q-zero-disequalityo
  "Find a saved `x != zero` obligation in the branch disequality store."
  [term neqs sigma]
  (fresh [left right walked-left walked-right]
    (membero [left right] neqs)
    (equality/walk*o left sigma walked-left)
    (equality/walk*o right sigma walked-right)
    (conde
      [(== term walked-left)
       (== (list 'app 'zero) walked-right)]
      [(== (list 'app 'zero) walked-left)
       (== term walked-right)])))

(defn- q-successor-disequalityo
  "Recognize `x != s(y)` after branch substitution and equality walking."
  [fml env sigma x predecessor]
  (fresh [lit left right walked-left walked-right]
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (equality/walk*o left sigma walked-left)
    (equality/walk*o right sigma walked-right)
    (conde
      [(== x walked-left)
       (== (list 'app 's predecessor) walked-right)]
      [(== (list 'app 's predecessor) walked-left)
       (== x walked-right)])))

(defn- q-proof-var-successoro
  "Recognize a successor whose predecessor is a proof-local universal variable.

   A full Q3 branch use is sound only when the active universal variable can be
   chosen as the predecessor supplied by Q3. A fixed parameter or compound term
   would incorrectly claim that Q3 gives a specific predecessor, so the rule is
   deliberately limited to a single proof-local `(var ...)`.
   "
  [term proof-vars predecessor]
  (fresh [predecessor-nom]
    (== (list 'app 's (list 'var predecessor-nom)) term)
    (membero predecessor-nom proof-vars)
    (== (list 'var predecessor-nom) predecessor)))

(defn- q-zero-disequality-formulao
  "Recognize `x != zero` after branch substitution and equality walking."
  [fml env sigma x]
  (fresh [lit left right walked-left walked-right]
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (equality/walk*o left sigma walked-left)
    (equality/walk*o right sigma walked-right)
    (conde
      [(== x walked-left)
       (== (list 'app 'zero) walked-right)]
      [(== (list 'app 'zero) walked-left)
       (== x walked-right)])))

(defn- q3-zero-storeo
  "Keep Q3's `x != zero` premise available for the later successor case.

   The ordinary free-constructor kernel may discharge `par != zero` as rigidly
   true and forget it. Q3 needs that premise when the branch later reaches
   `x != s(y)`, so the Robinson-Q profile stores this one theory-relevant
   disequality and continues through the ordinary kernel.
   "
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog gamma-terms fuel proof]
  (fresh [x next rest next-fuel subproof]
    (q-zero-disequality-formulao fml env sigma x)
    (== (lcons next rest) unexpanded)
    (support/step-fuelo fuel next-fuel)
    (== (list 'neq-store subproof) proof)
    (kernel/prove-stateo next
                         rest
                         lits
                         env
                         proof-vars
                         sigma
                         sigma-out
                         (lcons [x (ast/app-term 'zero)] neqs)
                         neqs-out
                         prog
                         gamma-terms
                         next-fuel
                         subproof)))

(defn- q3-case-splito
  "Close Q3's predecessor-or-zero branch when both premises are exposed."
  [fml env proof-vars sigma sigma-out neqs neqs-out proof]
  (fresh [x predecessor]
    (q-successor-disequalityo fml env sigma x predecessor)
    (q-proof-var-successoro (list 'app 's predecessor) proof-vars predecessor)
    (q-zero-disequalityo x neqs sigma)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled
              'robinson-q
              (list 'q3-case-split
                    'predecessor-or-zero
                    x
                    predecessor))
        proof)))

(defn- q3-predecessor-intro-closeo
  "Use Q3 after Q conversion to close a downstream universal disequality.

   This is the full-Q3 case absent from ADR-0050. When a branch already contains
   `x != zero`, Q3 permits choosing the active proof-local universal variable as
   a predecessor of `x`. If the current disequality normalizes to
   `x != s(v)` or `s(v) != x`, the branch closes. This covers formulas such as
   `add(v, s(zero)) != x`, whose successor shape is visible only after Q5/Q4
   conversion.
   "
  [fml env proof-vars sigma sigma-out neqs neqs-out proof]
  (fresh [lit left right walked-left walked-right
          normal-left normal-right left-proof right-proof
          nonzero predecessor]
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (equality/walk*o left sigma walked-left)
    (equality/walk*o right sigma walked-right)
    (q-normal-termo walked-left normal-left left-proof)
    (q-normal-termo walked-right normal-right right-proof)
    (conde
      [(== nonzero normal-left)
       (q-zero-disequalityo nonzero neqs sigma)
       (q-proof-var-successoro normal-right proof-vars predecessor)]
      [(== nonzero normal-right)
       (q-zero-disequalityo nonzero neqs sigma)
       (q-proof-var-successoro normal-left proof-vars predecessor)])
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled
              'robinson-q
              (list 'q3-predecessor-intro
                    'predecessor-or-zero
                    nonzero
                    predecessor
                    (list 'q-convert-close left-proof right-proof)))
        proof)))

(defn robinson-q-theory-closeo
  "Robinson-Q theory branch rule bound into the ordinary kernel."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog gamma-terms fuel proof]
  (conde
    [(q-convert-neq-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(q3-case-splito fml env proof-vars sigma sigma-out neqs neqs-out proof)]
    [(q3-zero-storeo fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog gamma-terms fuel proof)]
    [(q3-predecessor-intro-closeo fml env proof-vars sigma sigma-out neqs neqs-out proof)]))

(defn prove-program
  "Prove with Robinson-Q theory rules interleaved into the core kernel."
  [program formula proof-limit fuel]
  (binding [kernel/*theory-profile-closeo* robinson-q-theory-closeo]
    ;; `run` returns a lazy sequence. Realize it before leaving the dynamic
    ;; binding, or the kernel will resume proof search after the theory hook is
    ;; unbound and the profile will appear to prove nothing.
    (doall
      (if (nil? fuel)
        (run proof-limit [proof]
          (kernel/prove-programo formula '() '() '() program proof))
        (run proof-limit [proof]
          (kernel/prove-programo formula '() '() '() program fuel proof))))))
