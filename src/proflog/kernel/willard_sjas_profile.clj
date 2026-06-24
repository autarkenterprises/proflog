(ns proflog.kernel.willard-sjas-profile
  "Kernel-interleaved Willard SJAS profile.

   ADR-0061 promotes the ADR-0060 scaffold in two ways:

   - U-grounding arithmetic is interpreted as relations over binary numerals
     whose object-language constants are `0` and `1`;
   - `tableau-proof/3` checks structural proof certificates through an
     SJAS-side proof-check relation over decoded proof constructors;
   - `subst-prf/4` exposes the Level-1 substitution-proof vocabulary by
     decoding formula codes and checking diagonal substitution structurally.

   The profile therefore remains a tableau extension, not a host-side evaluator:
   arithmetic constraints and proof checking are both miniKanren goals
  interleaved at the branch rule boundary."
  (:refer-clojure :exclude [== < <=])
  (:require [clojure.core.logic :as logic
             :refer [!= == appendo conde fail fresh lcons membero or* run
                     succeed]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel :as kernel]
            [proflog.kernel-support :as support]
            [proflog.relational-arithmetic :as arith]
            [proflog.sjas-boundary-axioms :as boundary-axioms]
            [proflog.subst :as subst]
            [proflog.willard-sjas-code :as sjas-code]))

(def ^:private zero-symbol (symbol "0"))
(def ^:private one-symbol (symbol "1"))
(def ^:private zero-term (ast/app-term zero-symbol))
(def ^:private one-term (ast/app-term one-symbol))
(def ^:private one-bits (arith/build-num 1))
(def ^:private two-bits (arith/build-num 2))

(defn strip-profile-wrapper
  [proof]
  (if (and (seq? proof)
           (= 'profiled (first proof))
           (contains? '#{willard-sjas-tableau0
                         willard-sjas-level1
                         willard-sjas-tab1
                         willard-sjas-tab2
                         willard-sjas-total-multiplication
                         willard-sjas-xtab}
                      (second proof))
           (= 3 (count proof)))
    (nth proof 2)
    proof))

;; -----------------------------------------------------------------------------
;; Binary SJAS arithmetic
;; -----------------------------------------------------------------------------

(declare sjas-num-inputo)

(defn- bits->canonical-termo
  "Relate a little-endian binary numeral to its canonical SJAS term.

   Canonical terms use only `0`, `1`, `dbl`, and `add(_,1)`. This is the point
   where answer-mode arithmetic results become public object-language terms."
  [bits term proof]
  (conde
    [(== '() bits)
     (== zero-term term)
     (== '(sjas-num-zero) proof)]
    [(== one-bits bits)
     (== one-term term)
     (== '(sjas-num-one) proof)]
    [(fresh [tail tail-term tail-proof]
       (== (lcons 0 tail) bits)
       (arith/poso tail)
       (== (list 'app 'dbl tail-term) term)
       (bits->canonical-termo tail tail-term tail-proof)
       (== (list 'sjas-num-dbl tail-proof) proof))]
    [(fresh [tail tail-term tail-proof]
       (== (lcons 1 tail) bits)
       (arith/poso tail)
       (== (list 'app 'add (list 'app 'dbl tail-term) one-term) term)
       (bits->canonical-termo tail tail-term tail-proof)
       (== (list 'sjas-num-add-one tail-proof) proof))]))

(defn- bits->internal-canonical-termo
  "Relate a binary numeral to the internal syntax decoder's term shape.

   Formula-code decoding uses an internal representation where application
   arguments are stored as one list. This mirrors `bits->canonical-termo`, but
   it produces that internal shape directly so compact numeric term payloads can
   decode without rebuilding a public AST first."
  [bits term]
  (conde
    [(== '() bits)
     (== (list 'app zero-symbol '()) term)]
    [(== one-bits bits)
     (== (list 'app one-symbol '()) term)]
    [(fresh [tail tail-term]
       (== (lcons 0 tail) bits)
       (arith/poso tail)
       (bits->internal-canonical-termo tail tail-term)
       (== (list 'app 'dbl (list tail-term)) term))]
    [(fresh [tail tail-term doubled one-internal]
       (== (lcons 1 tail) bits)
       (arith/poso tail)
       (bits->internal-canonical-termo tail tail-term)
       (== (list 'app 'dbl (list tail-term)) doubled)
       (== (list 'app one-symbol '()) one-internal)
       (== (list 'app 'add (list doubled one-internal)) term))]))

(defn- sjas-monuso
  "Willard subtraction as total monus: `x - y` is zero when `x <= y`."
  [x y out]
  (conde
    [(arith/<=o x y)
     (== '() out)]
    [(arith/<o y x)
     (arith/minuso x y out)]))

(defn- sjas-divo
  "Willard division: division by zero returns the numerator."
  [x y out]
  (conde
    [(arith/zeroo y)
     (== x out)]
    [(arith/poso y)
     (fresh [remainder]
       (arith/divo x y out remainder))]))

(defn- sjas-maxo
  [x y out]
  (conde
    [(arith/<=o x y)
     (== y out)]
    [(arith/<o y x)
     (== x out)]))

(defn- sjas-logo
  "Later Type-A SJAS logarithm: floor(log2(x)) for x >= 2, else zero."
  [x out]
  (conde
    [(arith/zeroo x)
     (== '() out)]
    [(== one-bits x)
     (== '() out)]
    [(arith/>1o x)
     (fresh [remainder]
       (arith/logo x two-bits out remainder))]))

(declare sjas-iterated-logo)

(defn- sjas-iterated-logo
  "Willard 2002 JSL2 Definition 2.1: `Log(x,k)` = the k-fold iterated base-2
   floor logarithm of `x`, with the conventions `Log(x,0)=x` and `Log(0)=0`
   (the latter inherited from `sjas-logo`).

   `k` is the operational iteration count from `SemPrf^k_alpha`'s superscript;
   it is consumed here rather than ignored, so changing `k` changes the bound.
   Recursion mirrors `sjas-powo`: descend on `k` to `0`, then re-apply
   `sjas-logo` once per iteration on the way back up."
  [x k out]
  (conde
    [(arith/zeroo k)
     (== x out)]
    [(arith/poso k)
     (fresh [predecessor partial]
       (arith/pluso predecessor one-bits k)
       (sjas-iterated-logo x predecessor partial)
       (sjas-logo partial out))]))

(defn- sjas-log-of-power-of-twoo
  "Willard 2002 JSL2 Lemma 3.2 applied to a symbolic power of two:
   `Log(2^m, k) = Log(m, k-1)` for `k >= 1`, computed WITHOUT materializing
   `2^m`.

   This is the linchpin for the `SemPrf^k` bound `proof < Log(z,K)` when `z` is
   the tower-sized witness the diagonal argument (Theorem 2.3, Eq 11) requires:
   taking `z = 2^(proof+1)` gives `Log(z,1) = proof+1 > proof`, yet `z` is never
   built as a bit-list (for a real proof code `proof ~ 10^7`, `z` would have
   ~10^7 bits). The first logarithm peels the base-2 exponent (`Log(2^m,1)=m`);
   the remaining `k-1` iterations run on the small exponent `m` via the ordinary
   `sjas-iterated-logo`. On materializable inputs it agrees with the bit-level
   `Log(2^m,k)` by construction."
  [m k out]
  (fresh [k-minus-1]
    (arith/pluso k-minus-1 one-bits k)
    (sjas-iterated-logo m k-minus-1 out)))

(declare sjas-powo)

(defn- sjas-powo
  "Relational exponentiation over binary numerals."
  [base exponent out]
  (conde
    [(arith/zeroo exponent)
     (== one-bits out)]
    [(arith/poso exponent)
     (fresh [predecessor partial]
       (arith/pluso predecessor one-bits exponent)
       (sjas-powo base predecessor partial)
       (arith/*o partial base out))]))

(defn- sjas-rooto
  "Willard root: `ceil(x^(1/y))`, with the zero-divisor convention `root(x,0)=x`."
  [x y out]
  (conde
    [(arith/zeroo y)
     (== x out)]
    [(arith/poso y)
     (arith/zeroo x)
     (== '() out)]
    [(arith/poso y)
     (arith/poso x)
     (fresh [lower out-power lower-power]
       (arith/pluso lower one-bits out)
       (sjas-powo out y out-power)
       (arith/<=o x out-power)
       (sjas-powo lower y lower-power)
       (arith/<o lower-power x))]))

(declare sjas-counto)

(defn- sjas-counto
  "Count `1` bits among the rightmost `width` bits of `bits`."
  [bits width out]
  (conde
    [(arith/zeroo width)
     (== '() out)]
    [(fresh [width-tail]
       (arith/pluso width-tail one-bits width)
       (conde
         [(== '() bits)
          (sjas-counto '() width-tail out)]
         [(fresh [tail]
            (== (lcons 0 tail) bits)
            (sjas-counto tail width-tail out))]
         [(fresh [tail subtotal]
            (== (lcons 1 tail) bits)
            (sjas-counto tail width-tail subtotal)
            (arith/pluso subtotal one-bits out))]))]))

(defn- sjas-pending-bindso
  "Bind deferred object variables to canonical numeral terms after arithmetic.

   `sjas-num-inputo` does not eagerly enumerate public terms for open variables.
   It records `[term bits]` pairs instead. Once the surrounding arithmetic
   relation has constrained the bit-list, this relation turns the bit-list back
   into the public SJAS term and unifies the original variable with it."
  [pending sigma sigma-out proof]
  (conde
    [(== '() pending)
     (== sigma sigma-out)
     (== '(sjas-bind-done) proof)]
    [(fresh [term bits rest canonical num-proof sigma-mid step-proof tail-proof]
       (== (lcons [term bits] rest) pending)
       (bits->canonical-termo bits canonical num-proof)
       (equality/unify-termo term canonical sigma sigma-mid step-proof)
       (sjas-pending-bindso rest sigma-mid sigma-out tail-proof)
       (== (list 'sjas-bind-num num-proof step-proof tail-proof) proof))]))

(defn- sjas-num-appo
  [walked bits sigma sigma-out pending pending-out proof]
  (conde
    [(== zero-term walked)
     (== '() bits)
     (== sigma sigma-out)
     (== pending pending-out)
     (== '(sjas-read-zero) proof)]
    [(== one-term walked)
     (== one-bits bits)
     (== sigma sigma-out)
     (== pending pending-out)
     (== '(sjas-read-one) proof)]
    [(fresh [arg arg-bits arg-proof]
       (== (list 'app 'dbl arg) walked)
       (sjas-num-inputo arg arg-bits sigma sigma-out pending pending-out arg-proof)
       (arith/pluso arg-bits arg-bits bits)
       (== (list 'sjas-read-dbl arg-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'add left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (arith/pluso left-bits right-bits bits)
       (== (list 'sjas-read-add left-proof right-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'mul left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (arith/*o left-bits right-bits bits)
       (== (list 'sjas-read-mul left-proof right-proof) proof))]
    [(fresh [arg arg-bits arg-proof]
       (== (list 'app 'pred arg) walked)
       (sjas-num-inputo arg arg-bits sigma sigma-out pending pending-out arg-proof)
       (sjas-monuso arg-bits one-bits bits)
       (== (list 'sjas-read-pred arg-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'sub left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (sjas-monuso left-bits right-bits bits)
       (== (list 'sjas-read-sub left-proof right-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'div left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (sjas-divo left-bits right-bits bits)
       (== (list 'sjas-read-div left-proof right-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'max left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (sjas-maxo left-bits right-bits bits)
       (== (list 'sjas-read-max left-proof right-proof) proof))]
    [(fresh [arg arg-bits arg-proof]
       (== (list 'app 'log arg) walked)
       (sjas-num-inputo arg arg-bits sigma sigma-out pending pending-out arg-proof)
       (sjas-logo arg-bits bits)
       (== (list 'sjas-read-log arg-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'root left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (sjas-rooto left-bits right-bits bits)
       (== (list 'sjas-read-root left-proof right-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid left-proof right-proof]
       (== (list 'app 'count left right) walked)
       (sjas-num-inputo left left-bits sigma sigma-mid pending pending-mid left-proof)
       (sjas-num-inputo right right-bits sigma-mid sigma-out pending-mid pending-out right-proof)
       (sjas-counto left-bits right-bits bits)
       (== (list 'sjas-read-count left-proof right-proof) proof))]))

(defn- sjas-num-inputo
  [term bits sigma sigma-out pending pending-out proof]
  (fresh [walked]
    (equality/walk*o term sigma walked)
    (conde
      [(fresh [nom]
         (== (list 'var nom) walked)
         (== sigma sigma-out)
         (== (lcons [walked bits] pending) pending-out)
         (== (list 'sjas-read-var walked) proof))]
      [(sjas-num-appo walked bits sigma sigma-out pending pending-out proof)])))

(declare sjas-canonical-num-bits-termo)

(defn- sjas-canonical-num-bits-termo
  "Read a canonical public binary numeral term into bits.

   This specialized reader is used only for formal U-Grounding syntax/proof
   codes emitted by `proflog.willard-sjas-code`. It avoids running the general
   arithmetic interpreter merely to recover the bits of an already-canonical
   numeral, while keeping the object representation in the U-Grounding
   vocabulary."
  [term bits sigma sigma-out]
  (fresh [walked]
    (equality/walko term sigma walked)
    (conde
      [(== zero-term walked)
       (== '() bits)
       (== sigma sigma-out)]
      [(== one-term walked)
       (== one-bits bits)
       (== sigma sigma-out)]
      [(fresh [arg arg-bits]
         (== (list 'app 'dbl arg) walked)
         (sjas-canonical-num-bits-termo arg arg-bits sigma sigma-out)
         (== (lcons 0 arg-bits) bits))]
      [(fresh [arg arg-bits doubled]
         (== (list 'app 'add doubled one-term) walked)
         (== (list 'app 'dbl arg) doubled)
         (sjas-canonical-num-bits-termo arg arg-bits sigma sigma-out)
         (== (lcons 1 arg-bits) bits))])))

(defn- sjas-canonical-num-termo
  [term bits sigma sigma-out proof]
  (fresh []
    (sjas-canonical-num-bits-termo term bits sigma sigma-out)
    (== '(sjas-read-canonical-num) proof)))

(defn- sjas-normal-equalo
  [left right sigma sigma-out proof]
  (fresh [left-bits right-bits sigma-left sigma-read
          pending-left pending-all left-proof right-proof bind-proof]
    (sjas-num-inputo left left-bits sigma sigma-left '() pending-left left-proof)
    (sjas-num-inputo right right-bits sigma-left sigma-read pending-left pending-all right-proof)
    (== left-bits right-bits)
    (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
    (== (list 'sjas-equal left-proof right-proof bind-proof) proof)))

(defn- sjas-relation-holdso
  [relation args sigma sigma-out proof]
  (conde
    [(fresh [left right product left-bits right-bits product-bits
             sigma-left sigma-right sigma-read pending-left pending-right pending-all
             left-proof right-proof product-proof bind-proof]
       (== 'mult relation)
       (== (lcons left (lcons right (lcons product '()))) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() pending-left left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-right pending-left pending-right right-proof)
       (sjas-num-inputo product product-bits sigma-right sigma-read pending-right pending-all product-proof)
       (arith/*o left-bits right-bits product-bits)
       (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
       (== (list 'sjas-mult left-proof right-proof product-proof bind-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             pending-left pending-all left-proof right-proof bind-proof]
       (== 'leq relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() pending-left left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-read pending-left pending-all right-proof)
       (arith/<=o left-bits right-bits)
       (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
       (== (list 'sjas-leq left-proof right-proof bind-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             pending-left pending-all left-proof right-proof bind-proof]
       (== 'lt relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() pending-left left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-read pending-left pending-all right-proof)
       (arith/<o left-bits right-bits)
       (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
       (== (list 'sjas-lt left-proof right-proof bind-proof) proof))]))

(defn- sjas-semprfk-bound-holdso
  "Willard 2002 JSL2 Definition 2.1 bound: `proof-code < Log(bound-code, k-code)`.

   This is the genuine `SemPrf^k_alpha` side condition (the half beyond the
   validated `SemPrf_alpha` proof). The `k` superscript is operational: a larger
   `k` shrinks `Log(bound,k)` and tightens the bound. The bound has two
   representations:

   1. A symbolic power-of-two bound `(pow 2 exp)`. `Log((pow 2 exp), k)` is
      computed algebraically by `sjas-log-of-power-of-twoo` (Lemma 3.2) WITHOUT
      materializing `2^exp`. This is the tower-sized witness the diagonal
      argument needs (Theorem 2.3, Eq 11): `bound = 2^(proof+1)` gives
      `Log(bound,1) = proof+1 > proof`, yet the ~`2^proof`-bit numeral is never
      built. (`pow` is recognized only here, never materialized by the general
      term interpreter.)
   2. A materialized numeral bound, read to bits and iterated-logged directly
      (the ordinary path; unchanged).

   The two branches are mutually exclusive: branch 2's `sjas-num-inputo` has no
   `pow` case, so it cannot read a `(pow ...)` bound, and branch 1 requires the
   `(app pow 2 exp)` shape."
  [proof-code bound-code k-code sigma sigma-out proof]
  (conde
    [(fresh [walked-bound base exp base-bits exp-bits proof-bits k-bits log-bits
             sigma-proof sigma-base sigma-exp sigma-read
             pending-proof pending-base pending-exp pending-all
             proof-num-proof base-num-proof exp-num-proof k-num-proof bind-proof]
       (equality/walk*o bound-code sigma walked-bound)
       (== (list 'app 'pow base exp) walked-bound)
       (sjas-num-inputo proof-code proof-bits sigma sigma-proof
                        '() pending-proof proof-num-proof)
       (sjas-num-inputo base base-bits sigma-proof sigma-base
                        pending-proof pending-base base-num-proof)
       (== two-bits base-bits)
       (sjas-num-inputo exp exp-bits sigma-base sigma-exp
                        pending-base pending-exp exp-num-proof)
       (sjas-num-inputo k-code k-bits sigma-exp sigma-read
                        pending-exp pending-all k-num-proof)
       (sjas-log-of-power-of-twoo exp-bits k-bits log-bits)
       (arith/<o proof-bits log-bits)
       (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
       (== (list 'sjas-semprfk-pow-bound
                 proof-num-proof exp-num-proof k-num-proof bind-proof)
           proof))]
    [(fresh [proof-bits bound-bits k-bits log-bits
             sigma-proof sigma-bound sigma-read
             pending-proof pending-bound pending-all
             proof-num-proof bound-num-proof k-num-proof bind-proof]
       (sjas-num-inputo proof-code proof-bits sigma sigma-proof
                        '() pending-proof proof-num-proof)
       (sjas-num-inputo bound-code bound-bits sigma-proof sigma-bound
                        pending-proof pending-bound bound-num-proof)
       (sjas-num-inputo k-code k-bits sigma-bound sigma-read
                        pending-bound pending-all k-num-proof)
       (sjas-iterated-logo bound-bits k-bits log-bits)
       (arith/<o proof-bits log-bits)
       (sjas-pending-bindso pending-all sigma-read sigma-out bind-proof)
       (== (list 'sjas-semprfk-bound
                 proof-num-proof bound-num-proof k-num-proof bind-proof)
           proof))]))

(defn- distinct-num-bitso
  "Relate two canonical binary numerals that denote different numbers."
  [left right]
  (conde
    [(arith/<o left right)]
    [(arith/<o right left)]))

(defn- sjas-relation-failso
  "Proof-producing evaluator for false SJAS arithmetic relation atoms.

   Tableau closure is two-sided for interpreted arithmetic atoms: `not R(args)`
   closes when `R(args)` is true, and `R(args)` closes when the interpreted
   relation is false. This relation is deliberately ground after equality
   walking: all `sjas-num-inputo` calls require an empty pending-bind output, so
   the checker cannot make a false atom close by assigning an open proof
   variable."
  [relation args sigma sigma-out proof]
  (conde
    [(fresh [left right product left-bits right-bits product-bits expected-bits
             sigma-left sigma-right sigma-read left-proof right-proof
             product-proof]
       (== 'mult relation)
       (== (lcons left (lcons right (lcons product '()))) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() '() left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-right '() '() right-proof)
       (sjas-num-inputo product product-bits sigma-right sigma-read '() '() product-proof)
       (arith/*o left-bits right-bits expected-bits)
       (distinct-num-bitso expected-bits product-bits)
       (== sigma-read sigma-out)
       (== (list 'sjas-not-mult left-proof right-proof product-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             left-proof right-proof]
       (== 'leq relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() '() left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-read '() '() right-proof)
       (arith/<o right-bits left-bits)
       (== sigma-read sigma-out)
       (== (list 'sjas-not-leq left-proof right-proof) proof))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             left-proof right-proof]
       (== 'lt relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-inputo left left-bits sigma sigma-left '() '() left-proof)
       (sjas-num-inputo right right-bits sigma-left sigma-read '() '() right-proof)
       (arith/<=o right-bits left-bits)
       (== sigma-read sigma-out)
       (== (list 'sjas-not-lt left-proof right-proof) proof))]))

(declare sjas-num-input-coreo
         sjas-unify-termo-coreo)

(defn- bits->canonical-termo-coreo
  "Proof-free version of `bits->canonical-termo`.

   Structural tableau arithmetic needs the canonical numeral term only to bind
   pending object variables. The proof trace explaining that numeral read is
   not part of the formula-bearing tableau tree."
  [bits term]
  (conde
    [(== '() bits)
     (== zero-term term)]
    [(== one-bits bits)
     (== one-term term)]
    [(fresh [tail tail-term]
       (== (lcons 0 tail) bits)
       (arith/poso tail)
       (== (list 'app 'dbl tail-term) term)
       (bits->canonical-termo-coreo tail tail-term))]
    [(fresh [tail tail-term]
       (== (lcons 1 tail) bits)
       (arith/poso tail)
       (== (list 'app 'add (list 'app 'dbl tail-term) one-term) term)
       (bits->canonical-termo-coreo tail tail-term))]))

(defn- sjas-pending-binds-coreo
  "Bind delayed arithmetic variables without constructing read proofs."
  [pending sigma sigma-out]
  (conde
    [(== '() pending)
     (== sigma sigma-out)]
    [(fresh [term bits rest canonical sigma-mid]
       (== (lcons [term bits] rest) pending)
       (bits->canonical-termo-coreo bits canonical)
       (sjas-unify-termo-coreo term canonical sigma sigma-mid)
       (sjas-pending-binds-coreo rest sigma-mid sigma-out))]))

(defn- sjas-num-app-coreo
  "Proof-free reader for public SJAS arithmetic application terms."
  [walked bits sigma sigma-out pending pending-out]
  (conde
    [(== zero-term walked)
     (== '() bits)
     (== sigma sigma-out)
     (== pending pending-out)]
    [(== one-term walked)
     (== one-bits bits)
     (== sigma sigma-out)
     (== pending pending-out)]
    [(fresh [arg arg-bits]
       (== (list 'app 'dbl arg) walked)
       (sjas-num-input-coreo arg arg-bits sigma sigma-out pending pending-out)
       (arith/pluso arg-bits arg-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'add left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (arith/pluso left-bits right-bits bits))]
    [(fresh [arg arg-bits]
       (== (list 'app 'pred arg) walked)
       (sjas-num-input-coreo arg arg-bits sigma sigma-out pending pending-out)
       (sjas-monuso arg-bits one-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'sub left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (sjas-monuso left-bits right-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'div left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (sjas-divo left-bits right-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'max left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (sjas-maxo left-bits right-bits bits))]
    [(fresh [arg arg-bits]
       (== (list 'app 'log arg) walked)
       (sjas-num-input-coreo arg arg-bits sigma sigma-out pending pending-out)
       (sjas-logo arg-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'root left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (sjas-rooto left-bits right-bits bits))]
    [(fresh [left right left-bits right-bits sigma-mid pending-mid]
       (== (list 'app 'count left right) walked)
       (sjas-num-input-coreo left left-bits sigma sigma-mid pending pending-mid)
       (sjas-num-input-coreo right right-bits sigma-mid sigma-out pending-mid pending-out)
       (sjas-counto left-bits right-bits bits))]))

(defn- sjas-num-input-coreo
  "Read an SJAS arithmetic term to bits without returning proof evidence."
  [term bits sigma sigma-out pending pending-out]
  (fresh [walked]
    (equality/walk*o term sigma walked)
    (conde
      [(fresh [nom]
         (== (list 'var nom) walked)
         (== sigma sigma-out)
         (== (lcons [walked bits] pending) pending-out))]
      [(sjas-num-app-coreo walked bits sigma sigma-out pending pending-out)])))

(defn- sjas-normal-equal-coreo
  "Proof-free arithmetic equality over SJAS numeral terms."
  [left right sigma sigma-out]
  (fresh [left-bits right-bits sigma-left sigma-read pending-left pending-all]
    (sjas-num-input-coreo left left-bits sigma sigma-left '() pending-left)
    (sjas-num-input-coreo right right-bits sigma-left sigma-read pending-left pending-all)
    (== left-bits right-bits)
    (sjas-pending-binds-coreo pending-all sigma-read sigma-out)))

(defn- sjas-relation-holds-coreo
  "Proof-free evaluator for SJAS arithmetic relation leaves."
  [relation args sigma sigma-out]
  (conde
    [(fresh [left right product left-bits right-bits product-bits
             sigma-left sigma-right sigma-read pending-left pending-right pending-all]
       (== 'mult relation)
       (== (lcons left (lcons right (lcons product '()))) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() pending-left)
       (sjas-num-input-coreo right right-bits sigma-left sigma-right pending-left pending-right)
       (sjas-num-input-coreo product product-bits sigma-right sigma-read pending-right pending-all)
       (arith/*o left-bits right-bits product-bits)
       (sjas-pending-binds-coreo pending-all sigma-read sigma-out))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             pending-left pending-all]
       (== 'leq relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() pending-left)
       (sjas-num-input-coreo right right-bits sigma-left sigma-read pending-left pending-all)
       (arith/<=o left-bits right-bits)
       (sjas-pending-binds-coreo pending-all sigma-read sigma-out))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read
             pending-left pending-all]
       (== 'lt relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() pending-left)
       (sjas-num-input-coreo right right-bits sigma-left sigma-read pending-left pending-all)
       (arith/<o left-bits right-bits)
       (sjas-pending-binds-coreo pending-all sigma-read sigma-out))]))

(defn- sjas-relation-fails-coreo
  "Proof-free evaluator for false SJAS arithmetic relation leaves.

   The empty pending-bind arguments make this a ground-after-walk closure
   relation, matching `sjas-relation-failso` without constructing proof traces."
  [relation args sigma sigma-out]
  (conde
    [(fresh [left right product left-bits right-bits product-bits expected-bits
             sigma-left sigma-right sigma-read]
       (== 'mult relation)
       (== (lcons left (lcons right (lcons product '()))) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() '())
       (sjas-num-input-coreo right right-bits sigma-left sigma-right '() '())
       (sjas-num-input-coreo product product-bits sigma-right sigma-read '() '())
       (arith/*o left-bits right-bits expected-bits)
       (distinct-num-bitso expected-bits product-bits)
       (== sigma-read sigma-out))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read]
       (== 'leq relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() '())
       (sjas-num-input-coreo right right-bits sigma-left sigma-read '() '())
       (arith/<o right-bits left-bits)
       (== sigma-read sigma-out))]
    [(fresh [left right left-bits right-bits sigma-left sigma-read]
       (== 'lt relation)
       (== (lcons left (lcons right '())) args)
       (sjas-num-input-coreo left left-bits sigma sigma-left '() '())
       (sjas-num-input-coreo right right-bits sigma-left sigma-read '() '())
       (arith/<=o right-bits left-bits)
       (== sigma-read sigma-out))]))

;; -----------------------------------------------------------------------------
;; Arithmetic code decoding
;; -----------------------------------------------------------------------------

(def ^:private byte-base-bits (arith/build-num sjas-code/byte-base))

(def ^:private byte-bit-entries
  (apply list
         (map (fn [byte]
                [(arith/build-num byte) byte])
              (range sjas-code/byte-base))))

(def ^:private byte-six-bit-entries
  (apply list
         (map (fn [byte]
                [(apply list
                        (map (fn [idx]
                               (if (bit-test byte idx) 1 0))
                             (range 6)))
                 byte])
              (range sjas-code/byte-base))))

(defn- compact-code-byte-term
  "Host-build the finite source expansion for one compact byte numeral.

   This is used to generate `byte-term-entries`, a 64-clause object relation
   for the fixed base-64 digit language. It is not a semantic registry lookup:
   every entry is just the canonical U-grounding numeral term that the recursive
   `compact-code-byte-bits-termo` relation defines for the same byte."
  [byte]
  (letfn [(build [bits]
            (cond
              (empty? bits) zero-term
              (= one-bits bits) one-term
              (zero? (first bits)) (ast/app-term 'dbl (build (rest bits)))
              :else (ast/app-term 'add
                                   (ast/app-term 'dbl (build (rest bits)))
                                   one-term)))]
    (build (arith/build-num byte))))

(def ^:private byte-term-entries
  (apply list
         (map (fn [byte]
                [(compact-code-byte-term byte) byte])
              (range sjas-code/byte-base))))

(def ^:private canonical-byte-term-entries
  (apply list
         (map compact-code-byte-term
              (range sjas-code/byte-base))))

(def ^:private code-constructor-entries
  (apply list
         (map (fn [[constructor byte-count]]
                [constructor byte-count])
              sjas-code/code-functions)))

(def ^:private proof-symbol-index-entries
  (apply list
         (map (fn [[idx sym]]
                [idx sym])
              sjas-code/index->proof-symbol)))

(def ^:private proof-symbol-wide-index-entries
  (apply list
         (map (fn [[idx sym]]
                [(quot idx sjas-code/byte-base)
                 (mod idx sjas-code/byte-base)
                 sym])
              sjas-code/index->proof-symbol)))

(def ^:private proof-byte-entries
  (apply list (range sjas-code/byte-base)))

(def ^:private proof-byte-decrement-entries
  (apply list
         (map (fn [byte] [byte (dec byte)])
              (range 1 sjas-code/byte-base))))

(def ^:private app-arity-counts
  (range (dec sjas-code/byte-base)))

(declare sjas-acyclic-unifyo)

(defn- static-table-entryo
  "Relate `entry` to one element of a fixed finite SJAS metadata table.

   These tables are generated from host constants at load time. Expanding them
   as explicit alternatives avoids recursive `membero` scheduling and uses the
   local acyclic unifier because the candidate entries themselves cannot contain
   cyclic logic structure."
  [entry entries]
  (or*
    (map (fn [candidate]
           (fresh []
             (sjas-acyclic-unifyo entry candidate)))
         entries)))

(defn- static-table-nonentryo
  "Constrain `entry` to be distinct from every element of a fixed table."
  [entry entries]
  (if (empty? entries)
    (== true true)
    (fresh []
      (!= entry (first entries))
      (static-table-nonentryo entry (rest entries)))))

(defn- proof-byte-decremento
  [byte predecessor]
  (static-table-entryo [byte predecessor] proof-byte-decrement-entries))

(defn- byte-bitso
  [bits byte]
  (static-table-entryo [bits byte] byte-bit-entries))

(defn- byte-six-bitso
  [bits byte]
  (static-table-entryo [bits byte] byte-six-bit-entries))

(defn- bit-prefixo
  [prefix bits rest]
  (conde
    [(== '() prefix)
     (== bits rest)]
    [(fresh [head prefix-tail bits-tail]
       (== (lcons head prefix-tail) prefix)
       (== (lcons head bits-tail) bits)
       (bit-prefixo prefix-tail bits-tail rest))]))

(defn- bitso-byteso
  "Relate a little-endian binary natural to its base-64 byte digits."
  [bits bytes]
  (conde
    [(== '() bits)
     (== '() bytes)]
    [(fresh [quotient remainder byte tail]
       (arith/divo bits byte-base-bits quotient remainder)
	       (byte-bitso remainder byte)
	       (== (lcons byte tail) bytes)
	       (bitso-byteso quotient tail))]))

(defn- mul-byte-baseo
  "Relate `tail` to `64 * tail` in little-endian bit-list form.

   This is the U-Grounding byte-base multiplication relation used by the
   arithmetized code decoder. It is intentionally specialized to the fixed
   radix of the code representation: multiplying by 64 is exactly shifting a
   positive numeral by six low zero bits. That keeps the syntax-code path
   dependent on multiplication as a relation without invoking the fully general
   multiplication search at every byte of a large formula code."
  [tail scaled proof]
  (conde
    [(== '() tail)
     (== '() scaled)
     (== '(sjas-ug-code-mul64-zero) proof)]
    [(arith/poso tail)
     (== (lcons 0
                 (lcons 0
                        (lcons 0
                               (lcons 0
                                      (lcons 0
                                             (lcons 0 tail))))))
         scaled)
     (== '(sjas-ug-code-mul64-shift) proof)]))

(defn- byte-cons-equationo
  "Check the byte-cons equation `bits = byte + 64 * tail`.

   `byte-bits` is the padded six-bit digit for the low byte. The multiplication
   relation produces the shifted tail, and the final relation overlays the
   byte's six low bits on that shifted tail. Because the shifted tail has six
   low zeros, this is the constant-radix addition case of the U-Grounding code
   equation."
  [byte-bits tail bits proof]
  (fresh [scaled mul-proof]
    (mul-byte-baseo tail scaled mul-proof)
    (bit-prefixo byte-bits bits tail)
    (== (list 'sjas-ug-code-byte-cons mul-proof) proof)))

(declare byte-list-bitso)

(defn- byte-list-bitso
  "Relate a ground byte list to the corresponding little-endian bit numeral."
  [bytes bits]
  (conde
    [(== '() bytes)
     (== '() bits)]
    [(fresh [byte tail byte-bits tail-bits scaled]
       (== (lcons byte tail) bytes)
       (byte-bitso byte-bits byte)
       (byte-list-bitso tail tail-bits)
       (arith/*o byte-base-bits tail-bits scaled)
       (arith/pluso scaled byte-bits bits))]))

(defn- sjas-ug-code-bytes-bitso
  "Decode a U-Grounding code numeral and retain byte-cons proof evidence.

   This is the relational fallback used when a public code arrives through a
   logic binding rather than as an already-ground argument. Each recursive step
   explicitly proves the radix equation `n = byte + 64 * tail`, with the
   fixed-radix multiplication proof nested under `sjas-ug-code-byte-cons`."
  [bits bytes proof]
  (fresh [byte-bits byte tail-bits cons-proof]
    (byte-six-bitso byte-bits byte)
    (byte-cons-equationo byte-bits tail-bits bits cons-proof)
    (conde
      [(== sjas-code/u-grounding-sentinel-byte byte)
       (== '() tail-bits)
       (== '() bytes)
       (== (list 'sjas-ug-code-end cons-proof) proof)]
      [(fresh [tail-bytes tail-proof]
         (sjas-ug-code-bytes-bitso tail-bits tail-bytes tail-proof)
         (== (lcons byte tail-bytes) bytes)
         (== (list 'sjas-ug-code-cons cons-proof tail-proof) proof))])))

(defn- sjas-acyclic-unifyo
  "Unify acyclic SJAS constructor terms without a recursive occurs check.

   U-Grounding public codes are deeply nested first-order numerals. Ordinary
   core.logic unification checks whether a fresh tail variable occurs anywhere
   in the remaining numeral, which can overflow the host stack for substantive
   proof-code numerals before the arithmeticized reader has peeled the next
   byte. This helper is intentionally limited to constructor decomposition in
   the code reader: it does not project bytes in host Clojure, and it restores
   the caller's occurs-check setting after the local unifier step."
  [left right]
  (fn [state]
    (let [had-constraints? (pos? (count (:cs state)))
          prepared-state (cond-> state
                           had-constraints? (assoc :vs [])
                           true (assoc :oc false))
          unified-state (logic/unify prepared-state left right)]
      (when unified-state
        (let [restored-state (assoc unified-state :oc (:oc state))
              changed-vars (when had-constraints? (:vs restored-state))]
          (if (and had-constraints? (pos? (count changed-vars)))
            ((logic/run-constraints* changed-vars
                                     (:cs restored-state)
                                     ::subst)
             (assoc restored-state :vs nil))
            restored-state))))))

(defn- sjas-local-acyclic-unifyo
  "Unify local constructor structure without occurs-check or constraint replay.

   This helper is intentionally narrower than `sjas-acyclic-unifyo`: callers use
   it only to peel fresh list variables from already-selected compact-code
   argument streams. No semantic constraint is attached to those fresh local
   `arg/rest` or `byte/rest` variables, and replaying the global constraint
   store at every byte dominates measured proof-object validation."
  [left right]
  (fn [state]
    (when-let [unified-state (logic/unify (assoc state :oc false) left right)]
      (assoc unified-state :oc (:oc state)))))

(defn- sjas-code-walko
  "Walk a public code term through equality state without deep ground occurs checks."
  [term sigma walked]
  (conde
    [(== '() sigma)
     (sjas-acyclic-unifyo term walked)]
    [(fresh []
       (!= '() sigma)
       (equality/walko term sigma walked))]))

(defn- sjas-walk-termo
  "Walk a first-order SJAS term through equality state.

   When there is no equality substitution, walking is the identity relation. The
   identity case uses the same shallow acyclic binding as the public code reader
   so a formula-bearing proof certificate can be carried as object data without
   a host-stack-sized occurs check."
  [term sigma walked]
  (sjas-code-walko term sigma walked))

(defn- sjas-canonical-nonzero-termo
  "Succeed when a canonical public U-Grounding numeral is structurally nonzero."
  [term sigma]
  (fresh [walked]
    (sjas-code-walko term sigma walked)
    (conde
      [(fresh [arg]
         (sjas-acyclic-unifyo (list 'app 'dbl arg) walked))]
      [(fresh [arg]
         (sjas-acyclic-unifyo (list 'app 'add arg one-term) walked))]
      [(sjas-acyclic-unifyo one-term walked)])))

(defn- sjas-subst-formulao
  "Substitute formula variables without walking ground proof-code numerals.

   Public SJAS proof predicates are normally entered with an empty environment.
   In that case substitution is the identity relation, so using ordinary
   `subst-formulao` would only recurse through large arithmeticized code terms
   before returning the same formula. Nonempty environments still delegate to the
   shared substitution relation."
  [formula env out]
  (conde
    [(== '() env)
     (sjas-acyclic-unifyo formula out)]
    [(fresh []
       (!= '() env)
       (subst/subst-formulao formula env out))]))

(defn- sjas-walk-atomo
  "Walk an atom through equality state without deep-scanning empty sigma cases."
  [atom sigma out]
  (conde
    [(== '() sigma)
     (sjas-acyclic-unifyo atom out)]
    [(fresh []
       (!= '() sigma)
       (equality/walk-atomo atom sigma out))]))

(defn- canonical-bit-termo
  "Peel one low bit from a canonical public U-Grounding numeral term."
  [term bit tail sigma sigma-out proof]
  (fresh [walked]
    (sjas-code-walko term sigma walked)
    (conde
      [(fresh [arg doubled]
         (sjas-acyclic-unifyo (list 'app 'add doubled one-term) walked)
         (sjas-acyclic-unifyo (list 'app 'dbl arg) doubled)
         (== 1 bit)
         (sjas-acyclic-unifyo arg tail)
         (== sigma sigma-out)
         (== '(sjas-ug-code-bit-add-one) proof))]
      [(fresh [arg]
         (sjas-acyclic-unifyo (list 'app 'dbl arg) walked)
         (== 0 bit)
         (sjas-acyclic-unifyo arg tail)
         (== sigma sigma-out)
         (== '(sjas-ug-code-bit-dbl) proof))]
      [(sjas-acyclic-unifyo zero-term walked)
       (== 0 bit)
       (sjas-acyclic-unifyo zero-term tail)
       (== sigma sigma-out)
       (== '(sjas-ug-code-bit-zero) proof)]
      [(sjas-acyclic-unifyo one-term walked)
       (== 1 bit)
       (sjas-acyclic-unifyo zero-term tail)
       (== sigma sigma-out)
       (== '(sjas-ug-code-bit-one) proof)])))

(defn- canonical-byte-cons-proofo
  "Record the fixed-radix byte equation proven by six canonical bit peels."
  [tail proof]
  (conde
    [(sjas-canonical-nonzero-termo tail '())
     (== (list 'sjas-ug-code-byte-cons
               '(sjas-ug-code-mul64-shift)
               '(sjas-ug-code-canonical-byte))
         proof)]
    [(sjas-acyclic-unifyo zero-term tail)
     (== (list 'sjas-ug-code-byte-cons
               '(sjas-ug-code-mul64-zero)
               '(sjas-ug-code-canonical-byte))
         proof)]))

(defn- canonical-byte-termo
  "Peel one base-64 byte from a canonical U-Grounding numeral term.

   This is the bounded object-level decoder used for already-ground public
   system and theorem codes. It decomposes the numeral through its public
   `0`/`1`/`dbl`/`add(_,1)` constructors instead of computing bytes in host
   Clojure."
  [term byte tail sigma sigma-out proof]
  (fresh [b0 b1 b2 b3 b4 b5
          t1 t2 t3 t4 t5 t6
          s1 s2 s3 s4 s5
          p0 p1 p2 p3 p4 p5 cons-proof]
    (canonical-bit-termo term b0 t1 sigma s1 p0)
    (canonical-bit-termo t1 b1 t2 s1 s2 p1)
    (canonical-bit-termo t2 b2 t3 s2 s3 p2)
    (canonical-bit-termo t3 b3 t4 s3 s4 p3)
    (canonical-bit-termo t4 b4 t5 s4 s5 p4)
    (canonical-bit-termo t5 b5 t6 s5 sigma-out p5)
    (byte-six-bitso (list b0 b1 b2 b3 b4 b5) byte)
    (sjas-acyclic-unifyo t6 tail)
    (canonical-byte-cons-proofo tail cons-proof)
    (== (list 'sjas-ug-code-canonical-byte
              byte
              cons-proof)
        proof)))

(defn- sjas-ug-code-bytes-termo
  [remaining term bytes sigma sigma-out proof]
  (if (neg? remaining)
    fail
    (fresh [byte tail byte-proof sigma-after]
      (canonical-byte-termo term byte tail sigma sigma-after byte-proof)
      (conde
        [(fresh [tail-bytes tail-proof]
           (sjas-canonical-nonzero-termo tail sigma-after)
           (sjas-ug-code-bytes-termo (dec remaining)
                                     tail
                                     tail-bytes
                                     sigma-after
                                     sigma-out
                                     tail-proof)
           (sjas-acyclic-unifyo (lcons byte tail-bytes) bytes)
           (== (list 'sjas-ug-code-cons byte-proof) proof))]
        [(sjas-acyclic-unifyo zero-term tail)
         (== sjas-code/u-grounding-sentinel-byte byte)
         (== '() bytes)
         (== sigma-after sigma-out)
         (== (list 'sjas-ug-code-end byte-proof) proof)]))))

(declare compact-code-byte-bits-termo
         canonical-byte-term-coreo
         code-byte-build-termo)

(defn- compact-code-byte-bits-termo
  "Read a compact-code byte argument through its U-Grounding numeral shape.

   The parser is relational in the two modes used by the proof predicate:
   ground public byte terms decode to byte values, and finite byte values can
   rebuild public numeral terms for embedded code payloads. The byte value is
   still interpreted arithmetically rather than compared against a generated
   table of 64 canonical byte terms."
  [term bits]
  (conde
    [(== '() bits)
     (== zero-term term)]
    [(== one-bits bits)
     (== one-term term)]
    [(fresh [arg arg-bits]
       (== (lcons 0 arg-bits) bits)
       (== (list 'app 'dbl arg) term)
       (compact-code-byte-bits-termo arg arg-bits))]
    [(fresh [arg doubled arg-bits]
       (== (lcons 1 arg-bits) bits)
       (== (list 'app 'add doubled one-term) term)
       (== (list 'app 'dbl arg) doubled)
       (compact-code-byte-bits-termo arg arg-bits))]))

(defn- code-byte-termo
  "Relate a compact-code byte argument to its U-Grounding numeral value.

   Source-generated compact codes always use one of the 64 canonical byte
   numerals. Recognizing those finite terms first keeps large system/formula
   codes from paying a six-bit arithmetic decode at every byte. The fallback
   keeps ADR-0095's arbitrary-presented-numeral mode: noncanonical public terms
   derive their bits through the recursive numeral reader before the finite byte
   relation is consulted. Decoded-byte reconstruction uses
   `code-byte-build-termo`; neither mode projects a ground byte term through a
   host decoder."
  [term byte]
  (conde
    [(code-byte-build-termo term byte)]
    [(fresh [tail]
       (static-table-nonentryo term canonical-byte-term-entries)
       (canonical-byte-term-coreo term byte tail '() '())
       (sjas-acyclic-unifyo zero-term tail))]
    [(fresh [bits]
       (static-table-nonentryo term canonical-byte-term-entries)
       (compact-code-byte-bits-termo term bits)
       (byte-bitso bits byte))]))

(defn- code-byte-build-termo
  "Build a compact-code public byte numeral from a decoded byte value.

  This is the byte-first companion to `code-byte-termo`. It uses the finite
   macro expansion of the 64 canonical byte numerals because rebuilding each
   digit through the fully recursive arithmetic reader dominates fixed
   formula-bearing proof checks. The relation remains first-order and
  structural: it relates a byte to the same U-grounding numeral term accepted
  by `compact-code-byte-bits-termo`."
  [term byte]
  (static-table-entryo [term byte] byte-term-entries))

(defn- code-constructoro
  [constructor byte-count]
  (static-table-entryo [constructor byte-count] code-constructor-entries))

(defn- code-constructor-buildo
  "Byte-count-first constructor relation for rebuilding compact code terms."
  [constructor byte-count]
  (or*
    (map (fn [[entry-constructor entry-byte-count]]
           (fresh []
             (== entry-byte-count byte-count)
             (== entry-constructor constructor)))
         code-constructor-entries)))

(defn- code-argso
  "Read compact-code byte arguments while tying the argument count to the
   declared constructor `byte-count` (ADR-0095).

   The running `count` is threaded through the single decoding walk, so a
   `code-N` term whose argument list is not exactly N bytes long is rejected in
   the forward direction without a second traversal of large code payloads."
  [args bytes byte-count count proof]
  (conde
    [(== '() args)
     (== '() bytes)
     (== count byte-count)
     (== '(sjas-code-args-end) proof)]
    [(fresh [arg rest byte byte-rest rest-proof]
       (sjas-acyclic-unifyo (lcons arg rest) args)
       (code-byte-termo arg byte)
       (sjas-acyclic-unifyo (lcons byte byte-rest) bytes)
       (code-argso rest byte-rest byte-count (inc count) rest-proof)
       (== (list 'sjas-code-arg byte rest-proof) proof))]))

(defn- code-args-buildo
  [args bytes proof]
  (conde
    [(== '() args)
     (== '() bytes)
     (== '(sjas-code-args-end) proof)]
    [(fresh [arg rest byte byte-rest rest-proof]
       (== (lcons byte byte-rest) bytes)
       (code-byte-build-termo arg byte)
       (== (lcons arg rest) args)
       (code-args-buildo rest byte-rest rest-proof)
       (== (list 'sjas-code-arg byte rest-proof) proof))]))

(defn- sjas-code-byteso
  "Decode an object-language SJAS code term into base-64 bytes.

   Codes are first-order terms of the shape `(code-N b0 ... bN-1)`, where each
   byte is itself a small public binary numeral. This keeps Godel codes visible
   to the object language without forcing proof search to walk a huge nested
   binary numeral for every sentence and proof certificate."
  [term bytes sigma sigma-out proof]
  (fresh [walked constructor args byte-count args-proof]
    (sjas-code-walko term sigma walked)
    (sjas-acyclic-unifyo (lcons 'app (lcons constructor args)) walked)
    (code-constructoro constructor byte-count)
    (code-argso args bytes byte-count 0 args-proof)
    (== sigma sigma-out)
    (== (list 'sjas-code-bytes args-proof) proof)))

(defn- sjas-ug-code-byteso
  "Decode an object-language U-Grounding numeral code into base-64 bytes.

   The decoder peels six canonical constructor bits per byte, proving the fixed
   radix byte equation at each step. This deliberately avoids the earlier
   deterministic host shortcut during predicate application without forcing
   proof search to materialize and re-walk the complete bit list for large
   system codes."
  [term bytes sigma sigma-out proof]
  (fresh [decode-proof]
    (sjas-ug-code-bytes-termo sjas-code/max-code-bytes
                              term
                              bytes
                              sigma
                              sigma-out
                              decode-proof)
    (== (list 'sjas-ug-code-bytes decode-proof) proof)))

(defn- sjas-formal-code-byteso
  "Decode either supported public SJAS code representation.

   `kind` is `:compact` for `code-N` terms and `:u-grounding` for the ADR-0071
  binary numeral representation. Callers that need to know how a code should
  be quoted during diagonal substitution inspect this value."
  [term bytes sigma sigma-out kind proof]
  (conde
    [(sjas-code-byteso term bytes sigma sigma-out proof)
     (== :compact kind)]
    [(sjas-ug-code-byteso term bytes sigma sigma-out proof)
     (== :u-grounding kind)]))

(defn- mul-byte-base-coreo
  "Proof-free fixed-radix companion to `mul-byte-baseo`."
  [tail scaled]
  (conde
    [(== '() tail)
     (== '() scaled)]
    [(arith/poso tail)
     (== (lcons 0
                 (lcons 0
                        (lcons 0
                               (lcons 0
                                      (lcons 0
                                             (lcons 0 tail))))))
         scaled)]))

(defn- byte-cons-equation-coreo
  "Proof-free byte-cons equation `bits = byte + 64 * tail`."
  [byte-bits tail bits]
  (fresh [scaled]
    (mul-byte-base-coreo tail scaled)
    (bit-prefixo byte-bits bits tail)))

(defn- canonical-bit-term-coreo
  "Proof-free companion to `canonical-bit-termo`."
  [term bit tail sigma sigma-out]
  (fresh [walked]
    (sjas-code-walko term sigma walked)
    (conde
      [(fresh [arg doubled]
         (sjas-acyclic-unifyo (list 'app 'add doubled one-term) walked)
         (sjas-acyclic-unifyo (list 'app 'dbl arg) doubled)
         (== 1 bit)
         (sjas-acyclic-unifyo arg tail)
         (== sigma sigma-out))]
      [(fresh [arg]
         (sjas-acyclic-unifyo (list 'app 'dbl arg) walked)
         (== 0 bit)
         (sjas-acyclic-unifyo arg tail)
         (== sigma sigma-out))]
      [(sjas-acyclic-unifyo zero-term walked)
       (== 0 bit)
       (sjas-acyclic-unifyo zero-term tail)
       (== sigma sigma-out)]
      [(sjas-acyclic-unifyo one-term walked)
       (== 1 bit)
       (sjas-acyclic-unifyo zero-term tail)
       (== sigma sigma-out)])))

(defn- canonical-byte-term-coreo
  "Proof-free companion to `canonical-byte-termo`."
  [term byte tail sigma sigma-out]
  (fresh [b0 b1 b2 b3 b4 b5
          t1 t2 t3 t4 t5 t6
          s1 s2 s3 s4 s5]
    (canonical-bit-term-coreo term b0 t1 sigma s1)
    (canonical-bit-term-coreo t1 b1 t2 s1 s2)
    (canonical-bit-term-coreo t2 b2 t3 s2 s3)
    (canonical-bit-term-coreo t3 b3 t4 s3 s4)
    (canonical-bit-term-coreo t4 b4 t5 s4 s5)
    (canonical-bit-term-coreo t5 b5 t6 s5 sigma-out)
    (byte-six-bitso (list b0 b1 b2 b3 b4 b5) byte)
    (sjas-acyclic-unifyo t6 tail)))

(defn- sjas-ug-code-bytes-term-coreo
  "Proof-free U-Grounding public-code byte reader."
  [remaining term bytes sigma sigma-out]
  (if (neg? remaining)
    fail
    (fresh [byte tail sigma-after]
      (canonical-byte-term-coreo term byte tail sigma sigma-after)
      (conde
        [(fresh [tail-bytes]
           (sjas-canonical-nonzero-termo tail sigma-after)
           (sjas-ug-code-bytes-term-coreo (dec remaining)
                                          tail
                                          tail-bytes
                                          sigma-after
                                          sigma-out)
           (sjas-acyclic-unifyo (lcons byte tail-bytes) bytes))]
        [(sjas-acyclic-unifyo zero-term tail)
         (== sjas-code/u-grounding-sentinel-byte byte)
         (== '() bytes)
         (== sigma-after sigma-out)]))))

(defn- code-args-coreo
  "Proof-free compact-code argument byte reader.

   Public compact `code-N` arguments are presented byte numerals, so this parses
   each argument through the same numeral reader as the proof-producing path,
   tying the argument count to the declared constructor `byte-count` (ADR-0095).
   Byte-first reconstruction remains isolated to `code-args-buildo`."
  [args bytes byte-count count]
  (conde
    [(== '() args)
     (== '() bytes)
     (== count byte-count)]
    [(fresh [arg rest byte byte-rest]
       (sjas-local-acyclic-unifyo (lcons arg rest) args)
       (code-byte-termo arg byte)
       (sjas-local-acyclic-unifyo (lcons byte byte-rest) bytes)
       (code-args-coreo rest byte-rest byte-count (inc count)))]))

(defn- sjas-code-bytes-coreo
  "Proof-free compact public-code byte reader."
  [term bytes sigma sigma-out]
  (fresh [walked constructor args byte-count]
    (sjas-code-walko term sigma walked)
    (sjas-acyclic-unifyo (lcons 'app (lcons constructor args)) walked)
    (code-constructoro constructor byte-count)
    (code-args-coreo args bytes byte-count 0)
    (== sigma sigma-out)))

(defn- sjas-ug-code-bytes-coreo
  "Proof-free U-Grounding public-code byte reader."
  [term bytes sigma sigma-out]
  (sjas-ug-code-bytes-term-coreo sjas-code/max-code-bytes
                                 term
                                 bytes
                                 sigma
                                 sigma-out))

(defn- sjas-ground-compact-code-bytes-coreo
  "Correctness-preserving byte reader for an already-ground compact code term.

   A ground compact public code term denotes exactly one byte string, which
   `sjas-code/code-term-bytes` computes by checking every constructor arity and
   canonical numeral. Those bytes still flow through the same pure arithmetic,
   formula, system, and proof relations, so this is a read-time optimization,
   not a host proof checker: it cannot accept a proof the relational reader would
   reject. It is the sanctioned correctness-preserving optimization (terms that
   contain logic variables return nil and fall through to the fully relational
   reader below) that keeps the boundary proof validation tractable, whose
   SelfCons skeleton and measured proof objects embed the whole reflected system
   and are read many times per check."
  [term bytes sigma sigma-out]
  (logic/project [term]
    (if-let [ground-bytes (sjas-code/code-term-bytes term)]
      (fresh []
        (== '() sigma)
        (== (apply list ground-bytes) bytes)
        (== sigma sigma-out))
      fail)))

(defn- sjas-formal-code-bytes-coreo
  "Decode either supported public SJAS code representation without building
   auxiliary proof-trace evidence.

   Semantic checking stays fully relational. The only host step is reading the
   byte string of an *already-ground* compact code term via
   `sjas-ground-compact-code-bytes-coreo`; non-ground terms use the relational
   compact/U-grounding readers."
  [term bytes sigma sigma-out kind]
  (logic/project [term]
    (if (sjas-code/code-term-bytes term)
      (fresh []
        (sjas-ground-compact-code-bytes-coreo term bytes sigma sigma-out)
        (== :compact kind))
      (conde
        [(sjas-code-bytes-coreo term bytes sigma sigma-out)
         (== :compact kind)]
        [(sjas-ug-code-bytes-coreo term bytes sigma sigma-out)
         (== :u-grounding kind)]))))

;; -----------------------------------------------------------------------------
;; Formula-code byte decoding
;; -----------------------------------------------------------------------------

(def ^:private formula-true-tag 1)
(def ^:private formula-false-tag 2)
(def ^:private formula-pos-tag 3)
(def ^:private formula-neg-tag 4)
(def ^:private formula-eq-tag 5)
(def ^:private formula-neq-tag 6)
(def ^:private formula-and-tag 7)
(def ^:private formula-or-tag 8)
(def ^:private formula-not-tag 9)
(def ^:private formula-implies-tag 10)
(def ^:private formula-forall-tag 11)
(def ^:private formula-once-forall-tag 12)
(def ^:private formula-exists-tag 13)
(def ^:private formula-bounded-forall-tag 14)
(def ^:private formula-bounded-exists-tag 15)

(def ^:private term-var-tag 21)
(def ^:private term-par-tag 22)
(def ^:private term-app-tag 23)
(def ^:private term-code-tag 24)
(def ^:private term-natural-tag 25)
(def ^:private system-code-tag 31)
(def ^:private system-profile-tableau0-tag 32)
(def ^:private system-profile-level1-tag 33)
(def ^:private system-profile-tab1-tag 35)
(def ^:private system-profile-tab2-boundary-tag 36)
(def ^:private system-profile-total-multiplication-tag 37)
(def ^:private system-profile-xtab-tag 38)
(def ^:private system-reflected-clause-tag 34)

(def ^:private internal-zero-num (list 'num '()))
(def ^:private internal-one-num (list 'num (list 1)))
(def ^:private internal-two-num (list 'num (list 2)))

(def ^:private tableau0-contradiction-formula-bytes
  "Canonical formula-code bytes for Willard's minimal Tableau-0 target `0 = 1`."
  (list formula-eq-tag
        term-natural-tag 0 0
        term-natural-tag 1 0 1))

(def ^:private reverse-contradiction-formula-bytes
  "Canonical bytes for the equality-symmetric contradiction `1 = 0`."
  (list formula-eq-tag
        term-natural-tag 1 0 1
        term-natural-tag 0 0))

(def ^:private reverse-contradiction-complement-formula-bytes
  "Canonical bytes for the fixed Group-0 axiom `1 != 0`."
  (list formula-neq-tag
        term-natural-tag 1 0 1
        term-natural-tag 0 0))

(def ^:private tableau0-contradiction-u-grounding-bytes
  (apply list
         (concat tableau0-contradiction-formula-bytes
                 (list sjas-code/u-grounding-sentinel-byte))))

(def ^:private group-zero-internal-formulas
  [(list 'neq internal-one-num internal-zero-num)
   (list 'neq internal-two-num internal-zero-num)])

(def ^:private group-one-internal-formulas
  [(list 'eq internal-zero-num internal-zero-num)
   (list 'eq internal-two-num internal-two-num)
   (list 'eq
         (list 'app 'sub (list internal-two-num internal-one-num))
         internal-one-num)])

(def ^:private fixed-axiom-formula-byte-vectors
  [[6 25 1 0 1 25 0 0]
   [6 25 1 0 2 25 0 0]
   [5 25 0 0 25 0 0]
   [5 25 1 0 2 25 1 0 2]
   [5 23 19 3 25 1 0 2 25 1 0 1 25 1 0 1]])

(def ^:private fixed-axiom-formula-byte-entries
  "Canonical formula-code byte strings for fixed Group-0 and Group-1 axioms.

   The proof-free axiom-citation checker can recognize these fixed formulas by
   bytes directly. This preserves object-code checking while avoiding a full
   formula decode and alpha-equivalence pass for the small axioms present in
   every finite SJAS system."
  (apply list
         (map (fn [bytes] (apply list bytes))
              fixed-axiom-formula-byte-vectors)))

(def ^:private fixed-axiom-formula-compact-code-entries
  "Canonical compact public code terms for fixed Group-0 and Group-1 axioms."
  (apply list (map sjas-code/bytes->code-term
                   fixed-axiom-formula-byte-vectors)))

(def ^:private positive-byte-entries
  (apply list (range 1 sjas-code/byte-base)))

(def ^:private positive-byte-except-one-entries
  (apply list (range 2 sjas-code/byte-base)))

(def ^:private positive-byte-neq-entries
  (apply list
         (for [left (range 1 sjas-code/byte-base)
               right (range 1 sjas-code/byte-base)
               :when (not= left right)]
           [left right])))

(def ^:private byte-neq-entries
  (apply list
         (for [left (range sjas-code/byte-base)
               right (range sjas-code/byte-base)
               :when (not= left right)]
           [left right])))

(def ^:private code-nom-entries
  "Shared code-level noms used when decoded formula-code variables become ASTs."
  sjas-code/code-nom-entries)

(def ^:private profile-local-reserved-symbols
  "Symbols reserved for a specific generated profile, not for every SJAS system.

   `dsjas-tab2-proof` is present in the encoder's reserved order so the
   target-only Tab-2 boundary profile has a stable symbol index. It is not a
   globally semantic symbol, because ordinary systems can still use that same
   index for their first user relation.

   The total-multiplication / Xtab boundary vocabulary (`mul`, `finax4`,
   `willard-map`, `semprfk-alpha`, `semprf-alpha`) is treated the same way. These
   symbols occur only in the boundary variants' axioms and metatheorem queries;
   the proof checker matches them against the query atom or compares their
   encoded axiom bytes, never by decoding the symbol back from a presented
   system. Excluding them from the global reserved/user index partition keeps an
   ordinary system's user-relation indexes stable, so adding the boundary
   vocabulary does not shift `multi-demo`-style indexes into a reserved slot."
  '#{dsjas-tab2-proof
     mul
     finax4
     willard-map
     semprfk-alpha
     semprf-alpha})

(def ^:private reserved-symbol-index-entries
  "Fixed SJAS vocabulary entries recoverable without a generated codebook."
  (apply list
         (keep-indexed (fn [idx sym]
                         (when-not (contains? profile-local-reserved-symbols
                                              sym)
                           [(inc idx) sym]))
                       sjas-code/reserved-coding-symbols)))

(def ^:private user-symbol-index-entries
  "Formula-code symbol indexes not reserved by the fixed SJAS codebook."
  (let [reserved-indexes (set (map first reserved-symbol-index-entries))]
    (apply list
           (remove reserved-indexes
                   (range 1 sjas-code/byte-base)))))

(defn- positive-byteo
  [byte]
  (static-table-entryo byte positive-byte-entries))

(defn- formula-bytes-forall-rooto
  "Require encoded formula bytes to have a universal-quantifier root.

   All generated SJAS Group-3/SelfCons axioms use a top-level `forall`. The
   guard lets negative axiom-citation checks reject ordinary formulas before
   entering fixed-point reconstruction or nested skeleton decoding."
  [formula-bytes]
  (fresh [idx body]
    (== (lcons formula-forall-tag
                (lcons idx body))
        formula-bytes)
    (positive-byteo idx)))

(defn- positive-byte-except-oneo
  [byte]
  (static-table-entryo byte positive-byte-except-one-entries))

(defn- proof-byteo
  [byte]
  (static-table-entryo byte proof-byte-entries))

(defn- positive-byte-neqo
  [left right]
  (static-table-entryo [left right] positive-byte-neq-entries))

(defn- byte-neqo
  [left right]
  (static-table-entryo [left right] byte-neq-entries))

(defn- sjas-reserved-symbol-indexo
  [idx sym]
  (static-table-entryo [idx sym] reserved-symbol-index-entries))

(defn- sjas-user-symbol-indexo
  [idx]
  (static-table-entryo idx user-symbol-index-entries))

(defn- sjas-symbol-indexo
  "Relate a formula-code symbol index to a declared object-language symbol.

   Only the fixed U-Grounding arithmetic and SJAS profile symbols have semantic
   names at proof-checking time. User symbols are handled by syntax decoders as
   structural `(sym n)` ids unless reflected system-code records provide a
   structural comparison target."
  [_prog idx sym]
  (sjas-reserved-symbol-indexo idx sym))

(defn- sjas-object-symbol-indexo
  "Decode a symbol index without consulting the source-language codebook.

   Fixed SJAS vocabulary entries keep their semantic names because arithmetic
   and proof-profile predicates must dispatch on those symbols. User symbols are
   preserved as structural `(sym n)` identifiers, which is enough for reflected
   call matching and alpha comparison while remaining independent of host names."
  [idx sym]
  (conde
    [(sjas-reserved-symbol-indexo idx sym)]
    [(sjas-user-symbol-indexo idx)
     (== (list 'sym idx) sym)]))


(declare decode-formula-byteso decode-term-byteso)

(defn- parse-term-list-byteso
  [prog remaining bytes rest terms]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() terms)])
    (fresh [head tail after-head]
      ;; ADR-0110 (#1): bind the output cons before recurring, so a ground
      ;; `terms` drives the byte decode forward instead of enumerating.
      (== (lcons head tail) terms)
      (decode-term-byteso prog bytes after-head head)
      (parse-term-list-byteso prog (dec remaining) after-head rest tail))))

(defn- parse-code-payload-byteso
  [remaining bytes rest payload]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() payload)])
    (fresh [byte tail after-byte]
      ;; ADR-0110 (#1): bind the output cons first, so a ground `payload` fails a
      ;; wrong byte-count arm in O(1) (head/length mismatch) rather than after
      ;; building a depth-`remaining` structure.
      (== (lcons byte tail) payload)
      (== (lcons byte after-byte) bytes)
      (parse-code-payload-byteso (dec remaining) after-byte rest tail))))

(defn- append-sentinel-byteo
  [bytes encoded]
  (conde
    [(== '() bytes)
     (== (lcons sjas-code/u-grounding-sentinel-byte '()) encoded)]
    [(fresh [head tail encoded-tail]
       (== (lcons head tail) bytes)
       (== (lcons head encoded-tail) encoded)
       (append-sentinel-byteo tail encoded-tail))]))

(defn- decode-embedded-code-bodyo
  "Decode the payload of an embedded code term after its length header matched.

   The length bytes are destructured before the bounded enumeration starts.
   This matters for large U-Grounding-coded Group-3 formulas: wrong candidate
   lengths should fail against the two header bytes, not by repeatedly
   re-walking the entire remaining byte stream."
  [low high payload-bytes rest term]
  (or*
    (map (fn [byte-count]
           (let [expected-low (mod byte-count sjas-code/byte-base)
                 expected-high (quot byte-count sjas-code/byte-base)]
             (fresh []
               (== expected-low low)
               (== expected-high high)
               (fresh [payload]
                 ;; ADR-0110 (#1): bind the payload from a ground `term` before
                 ;; parsing, so a wrong byte-count arm fails fast in the
                 ;; backward (encode) direction. The header checks still run
                 ;; first, preserving the forward-mode length rejection.
                 (== (list 'code payload) term)
                 (parse-code-payload-byteso byte-count payload-bytes rest payload)))))
         (range (inc sjas-code/max-code-bytes)))))

(defn- decode-embedded-code-termo
  "Decode a code term embedded inside a formula-code byte stream.

   The length header uses two base-64 bytes. The expensive length enumeration
   happens only after the term-code tag has matched, so ordinary numeral and
   relation atoms fail this branch with one unification."
  [bytes rest term]
  (fresh [low high payload-bytes]
    (== (lcons term-code-tag
                (lcons low
                       (lcons high payload-bytes)))
        bytes)
    (decode-embedded-code-bodyo low high payload-bytes rest term)))

(defn- decode-natural-bodyo
  "Decode a compact numeric term payload into an internal U-Grounding numeral."
  [low high payload-bytes rest term]
  (or*
    (map (fn [byte-count]
           (let [expected-low (mod byte-count sjas-code/byte-base)
                 expected-high (quot byte-count sjas-code/byte-base)]
             (fresh []
               (== expected-low low)
               (== expected-high high)
               (fresh [payload]
                 ;; ADR-0110 (#1): bind the payload from a ground `term` before
                 ;; parsing (backward/encode fast-fail); header checks still
                 ;; run first, preserving the forward-mode length rejection.
                 (== (list 'num payload) term)
                 (parse-code-payload-byteso byte-count payload-bytes rest payload)))))
         (range (inc sjas-code/max-code-bytes)))))

(defn- decode-natural-termo
  "Decode a numeral term in the formula-code byte stream.

   This is a coding shortcut, not a new object-language constructor. The
   decoded term is the same canonical `0`/`1`/`dbl`/`add` tree that would have
   been obtained by recursively coding every constructor node."
  [bytes rest term]
  (fresh [low high payload-bytes]
    (== (lcons term-natural-tag
                (lcons low
                       (lcons high payload-bytes)))
        bytes)
    (decode-natural-bodyo low high payload-bytes rest term)))

(defn- decode-app-arityo
  "Decode an application payload after the relation symbol has been read."
  [prog after-symbol rest args]
  (fresh [arity-byte arg-bytes]
    (sjas-acyclic-unifyo (lcons arity-byte arg-bytes) after-symbol)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (parse-term-list-byteso prog arity arg-bytes rest args)))
           app-arity-counts))))

(defn- decode-app-termo
  [prog bytes rest term]
  (fresh [symbol-index after-symbol sym args]
    (== (list 'app sym args) term)
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (sjas-object-symbol-indexo symbol-index sym)
    (decode-app-arityo prog after-symbol rest args)))

(defn- decode-term-byteso
  "Parse one canonical SJAS term from a flat formula-code byte stream.

   The decoded term is an internal syntax tree used by the SJAS code predicates,
   not the public Proflog AST. Keeping this layer separate lets syntax
   recognition avoid inventing host noms merely to decide that a code is a
   well-formed formula."
  [prog bytes rest term]
  (conde
    [(fresh [idx after-var]
       (== (list 'var idx) term)
       (== (lcons term-var-tag (lcons idx after-var)) bytes)
       (positive-byteo idx)
       (== after-var rest))]
    [(fresh [idx after-par]
       (== (list 'par idx) term)
       (== (lcons term-par-tag (lcons idx after-par)) bytes)
       (positive-byteo idx)
       (== after-par rest))]
    [(decode-app-termo prog bytes rest term)]
    [(decode-natural-termo bytes rest term)]
    [(decode-embedded-code-termo bytes rest term)]))

(defn- decode-formula-byteso
  "Parse one canonical formula from a flat SJAS formula-code byte stream."
  [prog bytes rest formula]
  (conde
    [(fresh [after]
       (== (list 'true) formula)
       (== (lcons formula-true-tag after) bytes)
       (== after rest))]
    [(fresh [after]
       (== (list 'false) formula)
       (== (lcons formula-false-tag after) bytes)
       (== after rest))]
    [(fresh [term after-tag]
       (== (list 'pos term) formula)
       (== (lcons formula-pos-tag after-tag) bytes)
       (decode-term-byteso prog after-tag rest term))]
    [(fresh [term after-tag]
       (== (list 'neg term) formula)
       (== (lcons formula-neg-tag after-tag) bytes)
       (decode-term-byteso prog after-tag rest term))]
    [(fresh [left right after-tag after-left]
       (== (list 'eq left right) formula)
       (== (lcons formula-eq-tag after-tag) bytes)
       (decode-term-byteso prog after-tag after-left left)
       (decode-term-byteso prog after-left rest right))]
    [(fresh [left right after-tag after-left]
       (== (list 'neq left right) formula)
       (== (lcons formula-neq-tag after-tag) bytes)
       (decode-term-byteso prog after-tag after-left left)
       (decode-term-byteso prog after-left rest right))]
    [(fresh [left right after-tag after-left]
       (== (list 'and left right) formula)
       (== (lcons formula-and-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right))]
    [(fresh [left right after-tag after-left]
       (== (list 'or left right) formula)
       (== (lcons formula-or-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right))]
    [(fresh [body after-tag]
       (== (list 'not body) formula)
       (== (lcons formula-not-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag rest body))]
    [(fresh [left right after-tag after-left]
       (== (list 'implies left right) formula)
       (== (lcons formula-implies-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right))]
    [(fresh [idx body after-idx]
       (== (list 'forall idx body) formula)
       (== (lcons formula-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body))]
    [(fresh [idx body after-idx]
       (== (list 'once-forall idx body) formula)
       (== (lcons formula-once-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body))]
    [(fresh [idx body after-idx]
       (== (list 'exists idx body) formula)
       (== (lcons formula-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body))]
    [(fresh [idx bound body after-idx after-bound]
       (== (list 'bounded-forall idx bound body) formula)
       (== (lcons formula-bounded-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-term-byteso prog after-idx after-bound bound)
       (decode-formula-byteso prog after-bound rest body))]
    [(fresh [idx bound body after-idx after-bound]
       (== (list 'bounded-exists idx bound body) formula)
       (== (lcons formula-bounded-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-term-byteso prog after-idx after-bound bound)
       (decode-formula-byteso prog after-bound rest body))]))

(declare decode-syntax-formula-byteso decode-syntax-term-byteso)

(defn- parse-syntax-term-list-byteso
  [remaining bytes rest terms]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() terms)])
    (fresh [head tail after-head]
      (decode-syntax-term-byteso bytes after-head head)
      (parse-syntax-term-list-byteso (dec remaining) after-head rest tail)
      (== (lcons head tail) terms))))

(defn- decode-syntax-app-arityo
  "Decode an application payload for syntax predicates without symbol lookup."
  [after-symbol rest args]
  (fresh [arity-byte arg-bytes]
    (sjas-acyclic-unifyo (lcons arity-byte arg-bytes) after-symbol)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (parse-syntax-term-list-byteso arity arg-bytes rest args)))
           app-arity-counts))))

(defn- decode-syntax-app-termo
  "Decode an app term as structure only: application tag, symbol id, arity, args.

   Syntax predicates such as `wff` and the formula-class checks do not need the
   host symbol named by a formula-code symbol index. Keeping the head as a
   structural `(sym idx)` term removes the finite source-time codebook from the
   syntax slice while still preserving symbol identity for structural
   complement and alpha checks."
  [bytes rest term]
  (fresh [symbol-index after-symbol sym args]
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (positive-byteo symbol-index)
    (== (list 'sym symbol-index) sym)
    (== (list 'app sym args) term)
    (decode-syntax-app-arityo after-symbol rest args)))

(defn- decode-syntax-term-byteso
  "Parse a syntax-check term without projecting symbol indexes to host names."
  [bytes rest term]
  (conde
    [(fresh [idx after-var]
       (== (lcons term-var-tag (lcons idx after-var)) bytes)
       (positive-byteo idx)
       (== after-var rest)
       (== (list 'var idx) term))]
    [(fresh [idx after-par]
       (== (lcons term-par-tag (lcons idx after-par)) bytes)
       (positive-byteo idx)
       (== after-par rest)
       (== (list 'par idx) term))]
    [(decode-syntax-app-termo bytes rest term)]
    [(decode-natural-termo bytes rest term)]
    [(decode-embedded-code-termo bytes rest term)]))

(defn- decode-syntax-formula-byteso
  "Parse formula syntax from bytes using structural numeric symbol ids.

   This relation is intentionally narrower than proof-facing formula decoding:
   it proves that the code is a well-formed formula tree and preserves enough
   structure for formula-class and neg-pair checks, but it does not recover
   source-language host symbols."
  [bytes rest formula]
  (conde
    [(fresh [after]
       (== (lcons formula-true-tag after) bytes)
       (== after rest)
       (== (list 'true) formula))]
    [(fresh [after]
       (== (lcons formula-false-tag after) bytes)
       (== after rest)
       (== (list 'false) formula))]
    [(fresh [term after-tag]
       (== (lcons formula-pos-tag after-tag) bytes)
       (decode-syntax-term-byteso after-tag rest term)
       (== (list 'pos term) formula))]
    [(fresh [term after-tag]
       (== (lcons formula-neg-tag after-tag) bytes)
       (decode-syntax-term-byteso after-tag rest term)
       (== (list 'neg term) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-eq-tag after-tag) bytes)
       (decode-syntax-term-byteso after-tag after-left left)
       (decode-syntax-term-byteso after-left rest right)
       (== (list 'eq left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-neq-tag after-tag) bytes)
       (decode-syntax-term-byteso after-tag after-left left)
       (decode-syntax-term-byteso after-left rest right)
       (== (list 'neq left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-and-tag after-tag) bytes)
       (decode-syntax-formula-byteso after-tag after-left left)
       (decode-syntax-formula-byteso after-left rest right)
       (== (list 'and left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-or-tag after-tag) bytes)
       (decode-syntax-formula-byteso after-tag after-left left)
       (decode-syntax-formula-byteso after-left rest right)
       (== (list 'or left right) formula))]
    [(fresh [body after-tag]
       (== (lcons formula-not-tag after-tag) bytes)
       (decode-syntax-formula-byteso after-tag rest body)
       (== (list 'not body) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-implies-tag after-tag) bytes)
       (decode-syntax-formula-byteso after-tag after-left left)
       (decode-syntax-formula-byteso after-left rest right)
       (== (list 'implies left right) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-syntax-formula-byteso after-idx rest body)
       (== (list 'forall idx body) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-once-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-syntax-formula-byteso after-idx rest body)
       (== (list 'once-forall idx body) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-syntax-formula-byteso after-idx rest body)
       (== (list 'exists idx body) formula))]
    [(fresh [idx bound body after-idx after-bound]
       (== (lcons formula-bounded-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-syntax-term-byteso after-idx after-bound bound)
       (decode-syntax-formula-byteso after-bound rest body)
       (== (list 'bounded-forall idx bound body) formula))]
    [(fresh [idx bound body after-idx after-bound]
       (== (lcons formula-bounded-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-syntax-term-byteso after-idx after-bound bound)
       (decode-syntax-formula-byteso after-bound rest body)
       (== (list 'bounded-exists idx bound body) formula))]))

(declare skip-syntax-formula-byteso skip-syntax-term-byteso)

(defn- skip-code-payload-byteso
  [remaining bytes rest]
  (if (zero? remaining)
    (== bytes rest)
    (fresh [byte after-byte]
      (== (lcons byte after-byte) bytes)
      (skip-code-payload-byteso (dec remaining) after-byte rest))))

(defn- skip-length-prefixed-payloado
  [low high payload-bytes rest]
  (or*
    (map (fn [byte-count]
           (let [expected-low (mod byte-count sjas-code/byte-base)
                 expected-high (quot byte-count sjas-code/byte-base)]
             (fresh []
               (== expected-low low)
               (== expected-high high)
               (skip-code-payload-byteso byte-count payload-bytes rest))))
         (range (inc sjas-code/max-code-bytes)))))

(defn- skip-syntax-term-list-byteso
  [remaining bytes rest]
  (if (zero? remaining)
    (== bytes rest)
    (fresh [after-head]
      (skip-syntax-term-byteso bytes after-head)
      (skip-syntax-term-list-byteso (dec remaining) after-head rest))))

(defn- skip-syntax-app-arityo
  [after-symbol rest]
  (fresh [arity-byte arg-bytes]
    (sjas-acyclic-unifyo (lcons arity-byte arg-bytes) after-symbol)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (skip-syntax-term-list-byteso arity arg-bytes rest)))
           app-arity-counts))))

(defn- skip-syntax-app-termo
  [bytes rest]
  (fresh [symbol-index after-symbol]
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (positive-byteo symbol-index)
    (skip-syntax-app-arityo after-symbol rest)))

(defn- skip-syntax-length-prefixed-termo
  [tag bytes rest]
  (fresh [low high payload-bytes]
    (== (lcons tag
                (lcons low
                       (lcons high payload-bytes)))
        bytes)
    (skip-length-prefixed-payloado low high payload-bytes rest)))

(defn- skip-syntax-term-byteso
  "Advance over one structurally well-formed encoded syntax term.

   This is the non-reifying counterpart to `decode-syntax-term-byteso`. It is
   used when axiom membership only needs to move across system-code records,
   not materialize their decoded syntax trees."
  [bytes rest]
  (conde
    [(fresh [idx after-var]
       (== (lcons term-var-tag (lcons idx after-var)) bytes)
       (positive-byteo idx)
       (== after-var rest))]
    [(fresh [idx after-par]
       (== (lcons term-par-tag (lcons idx after-par)) bytes)
       (positive-byteo idx)
       (== after-par rest))]
    [(skip-syntax-app-termo bytes rest)]
    [(skip-syntax-length-prefixed-termo term-natural-tag bytes rest)]
    [(skip-syntax-length-prefixed-termo term-code-tag bytes rest)]))

(defn- skip-syntax-formula-byteso
  "Advance over one structurally well-formed encoded syntax formula.

   Unlike `decode-syntax-formula-byteso`, this relation does not build the
   decoded formula tree. It preserves the same byte grammar, so beta axiom
   scans can remain object-level while failing quickly on nonmatching records."
  [bytes rest]
  (conde
    [(fresh [after]
       (== (lcons formula-true-tag after) bytes)
       (== after rest))]
    [(fresh [after]
       (== (lcons formula-false-tag after) bytes)
       (== after rest))]
    [(fresh [after-tag]
       (== (lcons formula-pos-tag after-tag) bytes)
       (skip-syntax-term-byteso after-tag rest))]
    [(fresh [after-tag]
       (== (lcons formula-neg-tag after-tag) bytes)
       (skip-syntax-term-byteso after-tag rest))]
    [(fresh [after-tag after-left]
       (== (lcons formula-eq-tag after-tag) bytes)
       (skip-syntax-term-byteso after-tag after-left)
       (skip-syntax-term-byteso after-left rest))]
    [(fresh [after-tag after-left]
       (== (lcons formula-neq-tag after-tag) bytes)
       (skip-syntax-term-byteso after-tag after-left)
       (skip-syntax-term-byteso after-left rest))]
    [(fresh [after-tag after-left]
       (== (lcons formula-and-tag after-tag) bytes)
       (skip-syntax-formula-byteso after-tag after-left)
       (skip-syntax-formula-byteso after-left rest))]
    [(fresh [after-tag after-left]
       (== (lcons formula-or-tag after-tag) bytes)
       (skip-syntax-formula-byteso after-tag after-left)
       (skip-syntax-formula-byteso after-left rest))]
    [(fresh [after-tag]
       (== (lcons formula-not-tag after-tag) bytes)
       (skip-syntax-formula-byteso after-tag rest))]
    [(fresh [after-tag after-left]
       (== (lcons formula-implies-tag after-tag) bytes)
       (skip-syntax-formula-byteso after-tag after-left)
       (skip-syntax-formula-byteso after-left rest))]
    [(fresh [idx after-idx]
       (== (lcons formula-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (skip-syntax-formula-byteso after-idx rest))]
    [(fresh [idx after-idx]
       (== (lcons formula-once-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (skip-syntax-formula-byteso after-idx rest))]
    [(fresh [idx after-idx]
       (== (lcons formula-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (skip-syntax-formula-byteso after-idx rest))]
    [(fresh [idx after-idx after-bound]
       (== (lcons formula-bounded-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (skip-syntax-term-byteso after-idx after-bound)
       (skip-syntax-formula-byteso after-bound rest))]
    [(fresh [idx after-idx after-bound]
       (== (lcons formula-bounded-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (skip-syntax-term-byteso after-idx after-bound)
       (skip-syntax-formula-byteso after-bound rest))]))

(defn- sjas-decode-syntax-formula-code-proofo
  [code sigma sigma-out formula read-proof]
  (fresh [bytes rest kind]
    (sjas-formal-code-byteso code bytes sigma sigma-out kind read-proof)
    (decode-syntax-formula-byteso bytes rest formula)
    (== '() rest)))

(defn- sjas-decode-syntax-formula-code-coreo
  "Proof-free syntax formula-code decoder."
  [code sigma sigma-out formula]
  (fresh [bytes rest kind]
    (sjas-formal-code-bytes-coreo code bytes sigma sigma-out kind)
    (decode-syntax-formula-byteso bytes rest formula)
    (== '() rest)))

(defn- sjas-decode-formula-code-proofo
  [prog code sigma sigma-out formula read-proof]
  (fresh [bytes rest kind]
    (sjas-formal-code-byteso code bytes sigma sigma-out kind read-proof)
    (decode-formula-byteso prog bytes rest formula)
    (== '() rest)))

(defn- sjas-decode-formula-codeo
  [prog code sigma sigma-out formula]
  (fresh [read-proof]
    (sjas-decode-formula-code-proofo prog code sigma sigma-out formula read-proof)))

(defn- decode-proof-formula-byteso
  "Decode a proof-predicate formula from object code.

   Fixed SJAS vocabulary symbols decode to their semantic names. User symbols
   decode to structural `(sym n)` ids, so proof checking can compare reflected
   procedure calls by code identity without reconstructing a host source
   signature."
  [prog bytes rest formula]
  (decode-formula-byteso prog bytes rest formula))

(defn- sjas-decode-proof-formula-code-proofo
  [prog code sigma sigma-out formula read-proof]
  (fresh [bytes rest kind]
    (sjas-formal-code-byteso code bytes sigma sigma-out kind read-proof)
    (decode-proof-formula-byteso prog bytes rest formula)
    (== '() rest)))

(defn- sjas-decode-proof-formula-code-coreo
  "Proof-free theorem/formula-code decoder for proof-predicate checking."
  [prog code sigma sigma-out formula]
  (fresh [bytes rest kind]
    (sjas-formal-code-bytes-coreo code bytes sigma sigma-out kind)
    (decode-proof-formula-byteso prog bytes rest formula)
    (== '() rest)))

(declare sjas-delta-star-0-formulao
         sjas-pi-star-1-formulao
         sjas-sigma-star-1-formulao
         sjas-formula-complemento
         sjas-to-nnfo)

(defn- leq-guard-formula
  [idx bound polarity]
  (list polarity
        (list 'app 'leq
              (list (list 'var idx) bound))))

(defn- sjas-delta-star-0-formulao
  [formula]
  (conde
    [(== (list 'true) formula)]
    [(== (list 'false) formula)]
    [(fresh [term] (== (list 'pos term) formula))]
    [(fresh [term] (== (list 'neg term) formula))]
    [(fresh [left right] (== (list 'eq left right) formula))]
    [(fresh [left right] (== (list 'neq left right) formula))]
    [(fresh [left right]
       (== (list 'and left right) formula)
       (sjas-delta-star-0-formulao left)
       (sjas-delta-star-0-formulao right))]
    [(fresh [left right]
       (== (list 'or left right) formula)
       (sjas-delta-star-0-formulao left)
       (sjas-delta-star-0-formulao right))]
    ;; Delta-star-0 closure under the propositional connectives mirrors the
    ;; host classifier and the formula-code grammar tags (ADR-0087).
    [(fresh [body]
       (== (list 'not body) formula)
       (sjas-delta-star-0-formulao body))]
    [(fresh [left right]
       (== (list 'implies left right) formula)
       (sjas-delta-star-0-formulao left)
       (sjas-delta-star-0-formulao right))]
    [(fresh [idx bound body]
       (== (list 'bounded-forall idx bound body) formula)
       (sjas-delta-star-0-formulao body))]
    [(fresh [idx bound body]
       (== (list 'bounded-exists idx bound body) formula)
       (sjas-delta-star-0-formulao body))]))

(defn- sjas-pi-star-1-formulao
  [formula]
  (conde
    [(sjas-delta-star-0-formulao formula)]
    [(fresh [idx body]
       (== (list 'forall idx body) formula)
       (conde
         [(sjas-delta-star-0-formulao body)]
         [(sjas-pi-star-1-formulao body)]))]))

(defn- sjas-sigma-star-1-formulao
  [formula]
  (fresh [idx body]
    (== (list 'exists idx body) formula)
    (conde
      [(sjas-delta-star-0-formulao body)]
      [(sjas-sigma-star-1-formulao body)])))

(defn- sjas-to-nnfo
  [formula nnf]
  (conde
    [(== (list 'true) formula) (== formula nnf)]
    [(== (list 'false) formula) (== formula nnf)]
    [(fresh [term] (== (list 'pos term) formula) (== formula nnf))]
    [(fresh [term] (== (list 'neg term) formula) (== formula nnf))]
    [(fresh [left right] (== (list 'eq left right) formula) (== formula nnf))]
    [(fresh [left right] (== (list 'neq left right) formula) (== formula nnf))]
    [(fresh [left right left-nnf right-nnf]
       (== (list 'and left right) formula)
       (== (list 'and left-nnf right-nnf) nnf)
       (sjas-to-nnfo left left-nnf)
       (sjas-to-nnfo right right-nnf))]
    [(fresh [left right left-nnf right-nnf]
       (== (list 'or left right) formula)
       (== (list 'or left-nnf right-nnf) nnf)
       (sjas-to-nnfo left left-nnf)
       (sjas-to-nnfo right right-nnf))]
    [(fresh [body body-complement]
       (== (list 'not body) formula)
       (sjas-formula-complemento body body-complement)
       (sjas-to-nnfo body-complement nnf))]
    [(fresh [left right left-complement left-nnf right-nnf]
       (== (list 'implies left right) formula)
       (== (list 'or left-nnf right-nnf) nnf)
       (sjas-formula-complemento left left-complement)
       (sjas-to-nnfo left-complement left-nnf)
       (sjas-to-nnfo right right-nnf))]
    [(fresh [idx body body-nnf]
       (== (list 'forall idx body) formula)
       (== (list 'forall idx body-nnf) nnf)
       (sjas-to-nnfo body body-nnf))]
    [(fresh [idx body body-nnf]
       (== (list 'once-forall idx body) formula)
       (== (list 'once-forall idx body-nnf) nnf)
       (sjas-to-nnfo body body-nnf))]
    [(fresh [idx body body-nnf]
       (== (list 'exists idx body) formula)
       (== (list 'exists idx body-nnf) nnf)
       (sjas-to-nnfo body body-nnf))]
    [(fresh [idx bound body body-nnf]
       (== (list 'bounded-forall idx bound body) formula)
       (== (list 'forall idx
                 (list 'or
                       (leq-guard-formula idx bound 'neg)
                       body-nnf))
           nnf)
       (sjas-to-nnfo body body-nnf))]
    [(fresh [idx bound body body-nnf]
       (== (list 'bounded-exists idx bound body) formula)
       (== (list 'exists idx
                 (list 'and
                       (leq-guard-formula idx bound 'pos)
                       body-nnf))
           nnf)
       (sjas-to-nnfo body body-nnf))]))

(defn- sjas-formula-complemento
  [formula complement]
  (conde
    [(== (list 'true) formula)
     (== (list 'false) complement)]
    [(== (list 'false) formula)
     (== (list 'true) complement)]
    [(fresh [term]
       (== (list 'pos term) formula)
       (== (list 'neg term) complement))]
    [(fresh [term]
       (== (list 'neg term) formula)
       (== (list 'pos term) complement))]
    [(fresh [left right]
       (== (list 'eq left right) formula)
       (== (list 'neq left right) complement))]
    [(fresh [left right]
       (== (list 'neq left right) formula)
       (== (list 'eq left right) complement))]
    [(fresh [left right left-complement right-complement]
       (== (list 'and left right) formula)
       (== (list 'or left-complement right-complement) complement)
       (sjas-formula-complemento left left-complement)
       (sjas-formula-complemento right right-complement))]
    [(fresh [left right left-complement right-complement]
       (== (list 'or left right) formula)
       (== (list 'and left-complement right-complement) complement)
       (sjas-formula-complemento left left-complement)
       (sjas-formula-complemento right right-complement))]
    [(fresh [body body-nnf]
       (== (list 'not body) formula)
       (sjas-to-nnfo body body-nnf)
       (== body-nnf complement))]
    [(fresh [left right left-nnf right-complement]
       (== (list 'implies left right) formula)
       (== (list 'and left-nnf right-complement) complement)
       (sjas-to-nnfo left left-nnf)
       (sjas-formula-complemento right right-complement))]
    [(fresh [idx body body-complement]
       (== (list 'forall idx body) formula)
       (== (list 'exists idx body-complement) complement)
       (sjas-formula-complemento body body-complement))]
    [(fresh [idx body body-complement]
       (== (list 'once-forall idx body) formula)
       (== (list 'exists idx body-complement) complement)
       (sjas-formula-complemento body body-complement))]
    [(fresh [idx body body-complement]
       (== (list 'exists idx body) formula)
       (== (list 'once-forall idx body-complement) complement)
       (sjas-formula-complemento body body-complement))]
    [(fresh [idx bound body body-complement]
       (== (list 'bounded-forall idx bound body) formula)
       (== (list 'exists idx
                 (list 'and
                       (leq-guard-formula idx bound 'pos)
                       body-complement))
           complement)
       (sjas-formula-complemento body body-complement))]
    [(fresh [idx bound body body-complement]
       (== (list 'bounded-exists idx bound body) formula)
       (== (list 'once-forall idx
                 (list 'or
                       (leq-guard-formula idx bound 'neg)
                       body-complement))
           complement)
       (sjas-formula-complemento body body-complement))]))

(defn- sjas-structural-formula-classo
  [relation formula]
  (conde
    [(== 'delta-star-0-code relation)
     (sjas-delta-star-0-formulao formula)]
    [(== 'pi-star-1-code relation)
     (sjas-pi-star-1-formulao formula)]
    [(== 'sigma-star-1-code relation)
     (sjas-sigma-star-1-formulao formula)]))

(declare sjas-subst-term-var-oneo
         sjas-subst-term-list-var-oneo
         sjas-subst-formula-var-oneo
         sjas-byte-list-equalo)

(defn- sjas-subst-term-list-var-oneo
  "Substitute in each term of an internal formula-code term list.

   Formula codes store function/relation arguments as ordinary proper lists in
   the structural decoder. This helper keeps the recursive substitution rule
   local to that internal syntax layer, before any conversion to kernel AST
   noms occurs."
  [terms replacement substituted-terms]
  (conde
    [(== '() terms)
     (== '() substituted-terms)]
    [(fresh [head tail substituted-head substituted-tail]
       (== (lcons head tail) terms)
       (== (lcons substituted-head substituted-tail) substituted-terms)
       (sjas-subst-term-var-oneo head replacement substituted-head)
       (sjas-subst-term-list-var-oneo tail replacement substituted-tail))]))

(defn- sjas-subst-term-var-oneo
  "Relate an internal term to its diagonal substitution result.

   `Subst` for `IS#_D(beta)` replaces free variable index 1, the canonical
   representation of the source-level variable `v0`, with the code term for the
   source formula itself. Embedded `(code bytes)` terms are quoted syntax and
   are therefore left opaque rather than recursively decoded."
  [term replacement substituted]
  (conde
    [(fresh [bytes substituted-bytes]
       (== (list 'var 1) term)
       (== (list 'num bytes) replacement)
       (== (list 'num substituted-bytes) substituted)
       (sjas-byte-list-equalo bytes substituted-bytes))]
    [(fresh [bytes substituted-bytes]
       (== (list 'var 1) term)
       (== (list 'code bytes) replacement)
       (== (list 'code substituted-bytes) substituted)
       (sjas-byte-list-equalo bytes substituted-bytes))]
    [(== (list 'var 1) term)
     (== replacement substituted)]
    [(fresh [idx]
       (== (list 'var idx) term)
       (positive-byte-except-oneo idx)
       (== term substituted))]
    [(fresh [idx]
       (== (list 'par idx) term)
       (== term substituted))]
    [(fresh [sym args substituted-args]
       (== (list 'app sym args) term)
       (== (list 'app sym substituted-args) substituted)
       (sjas-subst-term-list-var-oneo args replacement substituted-args))]
    [(fresh [bytes substituted-bytes]
       (== (list 'num bytes) term)
       (== (list 'num substituted-bytes) substituted)
       (sjas-byte-list-equalo bytes substituted-bytes))]
    [(fresh [bytes substituted-bytes]
       (== (list 'code bytes) term)
       (== (list 'code substituted-bytes) substituted)
       (sjas-byte-list-equalo bytes substituted-bytes))]))

(defn- sjas-subst-formula-var-oneo
  "Relate a decoded formula-code tree to its diagonal substitution result.

   The relation is deliberately syntactic. It preserves formula constructors,
   substitutes through terms, respects quantifier shadowing for variable index
   1, and substitutes inside bounded-quantifier bounds because those bounds are
   outside the newly bound variable's body scope."
  [formula replacement substituted]
  (conde
    [(== (list 'true) formula)
     (== formula substituted)]
    [(== (list 'false) formula)
     (== formula substituted)]
    [(fresh [term substituted-term]
       (== (list 'pos term) formula)
       (== (list 'pos substituted-term) substituted)
       (sjas-subst-term-var-oneo term replacement substituted-term))]
    [(fresh [term substituted-term]
       (== (list 'neg term) formula)
       (== (list 'neg substituted-term) substituted)
       (sjas-subst-term-var-oneo term replacement substituted-term))]
    [(fresh [left right substituted-left substituted-right]
       (== (list 'eq left right) formula)
       (== (list 'eq substituted-left substituted-right) substituted)
       (sjas-subst-term-var-oneo left replacement substituted-left)
       (sjas-subst-term-var-oneo right replacement substituted-right))]
    [(fresh [left right substituted-left substituted-right]
       (== (list 'neq left right) formula)
       (== (list 'neq substituted-left substituted-right) substituted)
       (sjas-subst-term-var-oneo left replacement substituted-left)
       (sjas-subst-term-var-oneo right replacement substituted-right))]
    [(fresh [left right substituted-left substituted-right]
       (== (list 'and left right) formula)
       (== (list 'and substituted-left substituted-right) substituted)
       (sjas-subst-formula-var-oneo left replacement substituted-left)
       (sjas-subst-formula-var-oneo right replacement substituted-right))]
    [(fresh [left right substituted-left substituted-right]
       (== (list 'or left right) formula)
       (== (list 'or substituted-left substituted-right) substituted)
       (sjas-subst-formula-var-oneo left replacement substituted-left)
       (sjas-subst-formula-var-oneo right replacement substituted-right))]
    [(fresh [body substituted-body]
       (== (list 'not body) formula)
       (== (list 'not substituted-body) substituted)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]
    [(fresh [left right substituted-left substituted-right]
       (== (list 'implies left right) formula)
       (== (list 'implies substituted-left substituted-right) substituted)
       (sjas-subst-formula-var-oneo left replacement substituted-left)
       (sjas-subst-formula-var-oneo right replacement substituted-right))]
    [(fresh [body]
       (== (list 'forall 1 body) formula)
       (== formula substituted))]
    [(fresh [idx body substituted-body]
       (== (list 'forall idx body) formula)
       (positive-byte-except-oneo idx)
       (== (list 'forall idx substituted-body) substituted)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]
    [(fresh [body]
       (== (list 'once-forall 1 body) formula)
       (== formula substituted))]
    [(fresh [idx body substituted-body]
       (== (list 'once-forall idx body) formula)
       (positive-byte-except-oneo idx)
       (== (list 'once-forall idx substituted-body) substituted)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]
    [(fresh [body]
       (== (list 'exists 1 body) formula)
       (== formula substituted))]
    [(fresh [idx body substituted-body]
       (== (list 'exists idx body) formula)
       (positive-byte-except-oneo idx)
       (== (list 'exists idx substituted-body) substituted)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]
    [(fresh [bound body substituted-bound]
       (== (list 'bounded-forall 1 bound body) formula)
       (== (list 'bounded-forall 1 substituted-bound body) substituted)
       (sjas-subst-term-var-oneo bound replacement substituted-bound))]
    [(fresh [idx bound body substituted-bound substituted-body]
       (== (list 'bounded-forall idx bound body) formula)
       (positive-byte-except-oneo idx)
       (== (list 'bounded-forall idx substituted-bound substituted-body) substituted)
       (sjas-subst-term-var-oneo bound replacement substituted-bound)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]
    [(fresh [bound body substituted-bound]
       (== (list 'bounded-exists 1 bound body) formula)
       (== (list 'bounded-exists 1 substituted-bound body) substituted)
       (sjas-subst-term-var-oneo bound replacement substituted-bound))]
    [(fresh [idx bound body substituted-bound substituted-body]
       (== (list 'bounded-exists idx bound body) formula)
       (positive-byte-except-oneo idx)
       (== (list 'bounded-exists idx substituted-bound substituted-body) substituted)
       (sjas-subst-term-var-oneo bound replacement substituted-bound)
       (sjas-subst-formula-var-oneo body replacement substituted-body))]))

(declare sjas-alpha-term-equivo
         sjas-alpha-term-list-equivo
         sjas-alpha-formula-equivo)

(defn- alpha-unmapped-sourceo
  "Succeed when `idx` is not a source-side bound variable in `env`.

   Alpha-equivalence uses `env` as pairs of decoded binder indexes
   `[source-index target-index]`. Free variables are compared literally, so this
   guard prevents a bound source variable from also taking the literal-free
   branch when its numeric index happens to equal the target index."
  [idx env]
  (conde
    [(== '() env)]
    [(fresh [head tail source target]
       (== (lcons head tail) env)
       (== [source target] head)
       (!= idx source)
       (alpha-unmapped-sourceo idx tail))]))

(defn- alpha-bound-varo
  [source target env]
  (fresh [entry]
    (membero entry env)
    (== [source target] entry)))

(defn- sjas-byte-list-equalo
  "Compare decoded code-byte payloads structurally.

   Large Level-1 self-reference formulas embed whole public code byte strings
   inside `num` terms. Equating those lists through one shared logic variable
   can overflow core.logic's occurs check; walking the lists byte by byte keeps
   the comparison in the object relation."
  [left right]
  (conde
    [(== '() left)
     (== '() right)]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (== left-head right-head)
       (sjas-byte-list-equalo left-tail right-tail))]))

(defn- sjas-alpha-term-list-equivo
  [left right env]
  (conde
    [(== '() left)
     (== '() right)]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (sjas-alpha-term-equivo left-head right-head env)
       (sjas-alpha-term-list-equivo left-tail right-tail env))]))

(defn- sjas-alpha-term-equivo
  "Compare decoded internal terms modulo formula-binder renaming.

   Bound object variables may be renamed by quantifier traversal. Free
   variables, parameters, function symbols, and embedded code bytes remain
   literal; this keeps `Subst` from accepting a different free-variable answer
   merely because it is alpha-equivalent under some unrelated binder."
  [left right env]
  (conde
    [(fresh [left-idx right-idx]
       (== (list 'var left-idx) left)
       (== (list 'var right-idx) right)
       (alpha-bound-varo left-idx right-idx env))]
    [(fresh [idx]
       (== (list 'var idx) left)
       (== (list 'var idx) right)
       (alpha-unmapped-sourceo idx env))]
    [(fresh [idx]
       (== (list 'par idx) left)
       (== (list 'par idx) right))]
    [(fresh [sym left-args right-args]
       (== (list 'app sym left-args) left)
       (== (list 'app sym right-args) right)
       (sjas-alpha-term-list-equivo left-args right-args env))]
    [(fresh [left-bytes right-bytes]
       (== (list 'num left-bytes) left)
       (== (list 'num right-bytes) right)
       (sjas-byte-list-equalo left-bytes right-bytes))]
    [(fresh [left-bytes right-bytes]
       (== (list 'code left-bytes) left)
       (== (list 'code right-bytes) right)
       (sjas-byte-list-equalo left-bytes right-bytes))]))

(defn- sjas-alpha-formula-equivo
  "Compare decoded formula-code trees modulo bound-variable alpha-renaming."
  [left right env]
  (conde
    [(== (list 'true) left)
     (== (list 'true) right)]
    [(== (list 'false) left)
     (== (list 'false) right)]
    [(fresh [left-term right-term]
       (== (list 'pos left-term) left)
       (== (list 'pos right-term) right)
       (sjas-alpha-term-equivo left-term right-term env))]
    [(fresh [left-term right-term]
       (== (list 'neg left-term) left)
       (== (list 'neg right-term) right)
       (sjas-alpha-term-equivo left-term right-term env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'eq left-a left-b) left)
       (== (list 'eq right-a right-b) right)
       (sjas-alpha-term-equivo left-a right-a env)
       (sjas-alpha-term-equivo left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'neq left-a left-b) left)
       (== (list 'neq right-a right-b) right)
       (sjas-alpha-term-equivo left-a right-a env)
       (sjas-alpha-term-equivo left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'and left-a left-b) left)
       (== (list 'and right-a right-b) right)
       (sjas-alpha-formula-equivo left-a right-a env)
       (sjas-alpha-formula-equivo left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'or left-a left-b) left)
       (== (list 'or right-a right-b) right)
       (sjas-alpha-formula-equivo left-a right-a env)
       (sjas-alpha-formula-equivo left-b right-b env))]
    [(fresh [left-body right-body]
       (== (list 'not left-body) left)
       (== (list 'not right-body) right)
       (sjas-alpha-formula-equivo left-body right-body env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'implies left-a left-b) left)
       (== (list 'implies right-a right-b) right)
       (sjas-alpha-formula-equivo left-a right-a env)
       (sjas-alpha-formula-equivo left-b right-b env))]
    [(fresh [left-idx right-idx left-body right-body]
       (== (list 'forall left-idx left-body) left)
       (== (list 'forall right-idx right-body) right)
       (sjas-alpha-formula-equivo left-body
                                  right-body
                                  (lcons [left-idx right-idx] env)))]
    [(fresh [left-idx right-idx left-body right-body]
       (== (list 'once-forall left-idx left-body) left)
       (== (list 'once-forall right-idx right-body) right)
       (sjas-alpha-formula-equivo left-body
                                  right-body
                                  (lcons [left-idx right-idx] env)))]
    [(fresh [left-idx right-idx left-body right-body]
       (== (list 'exists left-idx left-body) left)
       (== (list 'exists right-idx right-body) right)
       (sjas-alpha-formula-equivo left-body
                                  right-body
                                  (lcons [left-idx right-idx] env)))]
    [(fresh [left-idx right-idx left-bound right-bound left-body right-body]
       (== (list 'bounded-forall left-idx left-bound left-body) left)
       (== (list 'bounded-forall right-idx right-bound right-body) right)
       (sjas-alpha-term-equivo left-bound right-bound env)
       (sjas-alpha-formula-equivo left-body
                                  right-body
                                  (lcons [left-idx right-idx] env)))]
    [(fresh [left-idx right-idx left-bound right-bound left-body right-body]
       (== (list 'bounded-exists left-idx left-bound left-body) left)
       (== (list 'bounded-exists right-idx right-bound right-body) right)
       (sjas-alpha-term-equivo left-bound right-bound env)
       (sjas-alpha-formula-equivo left-body
                                  right-body
                                  (lcons [left-idx right-idx] env)))]))

(declare sjas-subst-alpha-term-equivo
         sjas-subst-alpha-term-list-equivo
         sjas-subst-alpha-formula-equivo)

(defn- sjas-subst-alpha-term-list-equivo
  [source-terms replacement target-terms env]
  (conde
    [(== '() source-terms)
     (== '() target-terms)]
    [(fresh [source-head source-tail target-head target-tail]
       (== (lcons source-head source-tail) source-terms)
       (== (lcons target-head target-tail) target-terms)
       (sjas-subst-alpha-term-equivo source-head replacement target-head env)
       (sjas-subst-alpha-term-list-equivo source-tail
                                          replacement
                                          target-tail
                                          env))]))

(defn- sjas-subst-alpha-term-equivo
  "Compare a target term with the source term after diagonal substitution.

   This fuses `sjas-subst-term-var-oneo` with alpha-equivalence so the Level-1
   fixed-point check does not have to materialize a large intermediate formula
   containing repeated quoted code payloads."
  [source replacement target env]
  (conde
    [(== (list 'var 1) source)
     (sjas-alpha-term-equivo replacement target env)]
    [(fresh [idx]
       (== (list 'var idx) source)
       (positive-byte-except-oneo idx)
       (sjas-alpha-term-equivo source target env))]
    [(fresh [idx]
       (== (list 'par idx) source)
       (== (list 'par idx) target))]
    [(fresh [sym source-args target-args]
       (== (list 'app sym source-args) source)
       (== (list 'app sym target-args) target)
       (sjas-subst-alpha-term-list-equivo source-args
                                          replacement
                                          target-args
                                          env))]
    [(fresh [source-bytes target-bytes]
       (== (list 'num source-bytes) source)
       (== (list 'num target-bytes) target)
       (sjas-byte-list-equalo source-bytes target-bytes))]
    [(fresh [source-bytes target-bytes]
       (== (list 'code source-bytes) source)
       (== (list 'code target-bytes) target)
       (sjas-byte-list-equalo source-bytes target-bytes))]))

(defn- sjas-subst-alpha-formula-equivo
  "Compare a target formula with the source formula after diagonal `v0` Subst."
  [source replacement target env]
  (conde
    [(== (list 'true) source)
     (== (list 'true) target)]
    [(== (list 'false) source)
     (== (list 'false) target)]
    [(fresh [source-term target-term]
       (== (list 'pos source-term) source)
       (== (list 'pos target-term) target)
       (sjas-subst-alpha-term-equivo source-term replacement target-term env))]
    [(fresh [source-term target-term]
       (== (list 'neg source-term) source)
       (== (list 'neg target-term) target)
       (sjas-subst-alpha-term-equivo source-term replacement target-term env))]
    [(fresh [source-a source-b target-a target-b]
       (== (list 'eq source-a source-b) source)
       (== (list 'eq target-a target-b) target)
       (sjas-subst-alpha-term-equivo source-a replacement target-a env)
       (sjas-subst-alpha-term-equivo source-b replacement target-b env))]
    [(fresh [source-a source-b target-a target-b]
       (== (list 'neq source-a source-b) source)
       (== (list 'neq target-a target-b) target)
       (sjas-subst-alpha-term-equivo source-a replacement target-a env)
       (sjas-subst-alpha-term-equivo source-b replacement target-b env))]
    [(fresh [source-a source-b target-a target-b]
       (== (list 'and source-a source-b) source)
       (== (list 'and target-a target-b) target)
       (sjas-subst-alpha-formula-equivo source-a replacement target-a env)
       (sjas-subst-alpha-formula-equivo source-b replacement target-b env))]
    [(fresh [source-a source-b target-a target-b]
       (== (list 'or source-a source-b) source)
       (== (list 'or target-a target-b) target)
       (sjas-subst-alpha-formula-equivo source-a replacement target-a env)
       (sjas-subst-alpha-formula-equivo source-b replacement target-b env))]
    [(fresh [source-body target-body]
       (== (list 'not source-body) source)
       (== (list 'not target-body) target)
       (sjas-subst-alpha-formula-equivo source-body
                                        replacement
                                        target-body
                                        env))]
    [(fresh [source-a source-b target-a target-b]
       (== (list 'implies source-a source-b) source)
       (== (list 'implies target-a target-b) target)
       (sjas-subst-alpha-formula-equivo source-a replacement target-a env)
       (sjas-subst-alpha-formula-equivo source-b replacement target-b env))]
    [(fresh [source-body target-idx target-body]
       (== (list 'forall 1 source-body) source)
       (== (list 'forall target-idx target-body) target)
       (sjas-alpha-formula-equivo source-body
                                  target-body
                                  (lcons [1 target-idx] env)))]
    [(fresh [source-idx source-body target-idx target-body]
       (== (list 'forall source-idx source-body) source)
       (positive-byte-except-oneo source-idx)
       (== (list 'forall target-idx target-body) target)
       (sjas-subst-alpha-formula-equivo
         source-body
         replacement
         target-body
         (lcons [source-idx target-idx] env)))]
    [(fresh [source-body target-idx target-body]
       (== (list 'once-forall 1 source-body) source)
       (== (list 'once-forall target-idx target-body) target)
       (sjas-alpha-formula-equivo source-body
                                  target-body
                                  (lcons [1 target-idx] env)))]
    [(fresh [source-idx source-body target-idx target-body]
       (== (list 'once-forall source-idx source-body) source)
       (positive-byte-except-oneo source-idx)
       (== (list 'once-forall target-idx target-body) target)
       (sjas-subst-alpha-formula-equivo
         source-body
         replacement
         target-body
         (lcons [source-idx target-idx] env)))]
    [(fresh [source-body target-idx target-body]
       (== (list 'exists 1 source-body) source)
       (== (list 'exists target-idx target-body) target)
       (sjas-alpha-formula-equivo source-body
                                  target-body
                                  (lcons [1 target-idx] env)))]
    [(fresh [source-idx source-body target-idx target-body]
       (== (list 'exists source-idx source-body) source)
       (positive-byte-except-oneo source-idx)
       (== (list 'exists target-idx target-body) target)
       (sjas-subst-alpha-formula-equivo
         source-body
         replacement
         target-body
         (lcons [source-idx target-idx] env)))]
    [(fresh [source-bound source-body target-idx target-bound target-body]
       (== (list 'bounded-forall 1 source-bound source-body) source)
       (== (list 'bounded-forall target-idx target-bound target-body) target)
       (sjas-subst-alpha-term-equivo source-bound replacement target-bound env)
       (sjas-alpha-formula-equivo source-body
                                  target-body
                                  (lcons [1 target-idx] env)))]
    [(fresh [source-idx source-bound source-body
             target-idx target-bound target-body]
       (== (list 'bounded-forall source-idx source-bound source-body) source)
       (positive-byte-except-oneo source-idx)
       (== (list 'bounded-forall target-idx target-bound target-body) target)
       (sjas-subst-alpha-term-equivo source-bound replacement target-bound env)
       (sjas-subst-alpha-formula-equivo
         source-body
         replacement
         target-body
         (lcons [source-idx target-idx] env)))]
    [(fresh [source-bound source-body target-idx target-bound target-body]
       (== (list 'bounded-exists 1 source-bound source-body) source)
       (== (list 'bounded-exists target-idx target-bound target-body) target)
       (sjas-subst-alpha-term-equivo source-bound replacement target-bound env)
       (sjas-alpha-formula-equivo source-body
                                  target-body
                                  (lcons [1 target-idx] env)))]
    [(fresh [source-idx source-bound source-body
             target-idx target-bound target-body]
       (== (list 'bounded-exists source-idx source-bound source-body) source)
       (positive-byte-except-oneo source-idx)
       (== (list 'bounded-exists target-idx target-bound target-body) target)
       (sjas-subst-alpha-term-equivo source-bound replacement target-bound env)
       (sjas-subst-alpha-formula-equivo
         source-body
         replacement
         target-body
         (lcons [source-idx target-idx] env)))]))

(declare sjas-internal-term-asto sjas-internal-formula-asto)

(defn- byte-list-counto
  [remaining bytes]
  (if (zero? remaining)
    (== '() bytes)
    (fresh [byte rest]
      (== (lcons byte rest) bytes)
      (byte-list-counto (dec remaining) rest))))

(defn- code-args-build-counto
  "Build compact code arguments while exposing the decoded byte count.

   Embedded code payloads are already decoded as byte lists. The older
   reconstruction path first searched for a byte-count whose list-length proof
   fit those bytes, then walked the same bytes again to build public code
   arguments. Walking once avoids repeated occurs checks over large quoted code
   payloads while preserving the fixed maximum compact-code arity."
  [bytes args byte-count count]
  (if (> count sjas-code/max-code-bytes)
    fail
    (conde
      [(== '() bytes)
       (== '() args)
       (== count byte-count)]
      [(fresh [byte byte-rest arg arg-rest]
         (== (lcons byte byte-rest) bytes)
         (code-byte-build-termo arg byte)
         (== (lcons arg arg-rest) args)
         (code-args-build-counto byte-rest
                                 arg-rest
                                 byte-count
                                 (inc count)))])))

(defn- sjas-internal-code-termo
  "Convert a decoded embedded code payload back to its public AST term."
  [bytes term]
  (fresh [constructor args byte-count]
    (code-args-build-counto bytes args byte-count 0)
    (code-constructor-buildo constructor byte-count)
    (== (lcons 'app (lcons constructor args)) term)))

(defn- sjas-internal-term-list-asto
  [terms ast-terms]
  (conde
    [(== '() terms)
     (== '() ast-terms)]
    [(fresh [head tail head-ast tail-ast]
       (== (lcons head tail) terms)
       (== (lcons head-ast tail-ast) ast-terms)
       (sjas-internal-term-asto head head-ast)
       (sjas-internal-term-list-asto tail tail-ast))]))

(defn- sjas-internal-nom-termo
  "Translate an internal variable/parameter index to an AST term.

   `nominal/tie` and AST constructors must receive concrete nominal values,
   not logic variables that will only later be constrained by another goal.
   Enumerating the fixed code-nom table builds each branch with an actual nom
   constant while remaining a relation over the byte index."
  [internal-tag ast-tag term ast-term]
  (or*
    (map (fn [[idx nom]]
           (fresh []
             (== (list internal-tag idx) term)
             (== (list ast-tag nom) ast-term)))
         code-nom-entries)))

(defn- sjas-internal-quantifier-asto
  [internal-tag ast-tag formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [body body-ast]
             (== (list internal-tag idx body) formula)
             (== (list ast-tag (nominal/tie nom body-ast)) ast-formula)
             (sjas-internal-formula-asto body body-ast)))
         code-nom-entries)))

(defn- sjas-internal-bounded-quantifier-asto
  [internal-tag ast-tag formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [bound body bound-ast body-ast]
             (== (list internal-tag idx bound body) formula)
             (== (list ast-tag
                       (nominal/tie nom {:bound bound-ast
                                         :body body-ast}))
                 ast-formula)
             (sjas-internal-term-asto bound bound-ast)
             (sjas-internal-formula-asto body body-ast)))
         code-nom-entries)))

(defn- sjas-internal-term-asto
  "Translate the structural decoder's internal term tree into a kernel AST term."
  [term ast-term]
  (conde
    [(sjas-internal-nom-termo 'var 'var term ast-term)]
    [(sjas-internal-nom-termo 'par 'par term ast-term)]
    [(fresh [sym args ast-args]
       (== (list 'app sym args) term)
       (== (lcons 'app (lcons sym ast-args)) ast-term)
       (sjas-internal-term-list-asto args ast-args))]
    [(fresh [bytes bits num-proof]
       (== (list 'num bytes) term)
       (byte-list-bitso bytes bits)
       (bits->canonical-termo bits ast-term num-proof))]
    [(fresh [bytes]
       (== (list 'code bytes) term)
       (sjas-internal-code-termo bytes ast-term))]))

(defn- sjas-internal-formula-asto
  "Translate a decoded formula-code tree into the ordinary Proflog kernel AST."
  [formula ast-formula]
  (conde
    [(== (list 'true) formula)
     (== (list 'true) ast-formula)]
    [(== (list 'false) formula)
     (== (list 'false) ast-formula)]
    [(fresh [term ast-term]
       (== (list 'pos term) formula)
       (== (list 'pos ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [term ast-term]
       (== (list 'neg term) formula)
       (== (list 'neg ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [left right left-ast right-ast]
       (== (list 'eq left right) formula)
       (== (list 'eq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'neq left right) formula)
       (== (list 'neq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'and left right) formula)
       (== (list 'and left-ast right-ast) ast-formula)
       (sjas-internal-formula-asto left left-ast)
       (sjas-internal-formula-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'or left right) formula)
       (== (list 'or left-ast right-ast) ast-formula)
       (sjas-internal-formula-asto left left-ast)
       (sjas-internal-formula-asto right right-ast))]
    [(fresh [body body-ast]
       (== (list 'not body) formula)
       (== (list 'not body-ast) ast-formula)
       (sjas-internal-formula-asto body body-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'implies left right) formula)
       (== (list 'implies left-ast right-ast) ast-formula)
       (sjas-internal-formula-asto left left-ast)
       (sjas-internal-formula-asto right right-ast))]
    [(sjas-internal-quantifier-asto 'forall 'forall formula ast-formula)]
    [(sjas-internal-quantifier-asto 'once-forall 'once-forall formula ast-formula)]
    [(sjas-internal-quantifier-asto 'exists 'exists formula ast-formula)]
    [(sjas-internal-bounded-quantifier-asto 'bounded-forall
                                            'bounded-forall
                                            formula
                                            ast-formula)]
    [(sjas-internal-bounded-quantifier-asto 'bounded-exists
                                            'bounded-exists
                                            formula
                                            ast-formula)]))

(defn- sjas-structural-negated-theorem-proofo
  "Decode a theorem code and build the negated target formula for proof checking.

   This relation is deliberately used even for generated axiom codes. The proof
   predicate must not recover theorem targets from a host-side finite registry
   merely because a code was generated by the current system builder. The
   formula code is read through the same object byte relation used by syntax
   predicates, so proof evidence exposes compact constructor bytes instead of a
   host-projected byte vector."
  [prog theorem-code sigma sigma-out neg-theorem theorem-read-proof]
  (fresh [formula complement read-proof]
    (sjas-decode-proof-formula-code-proofo prog
                                           theorem-code
                                           sigma
                                           sigma-out
                                           formula
                                           read-proof)
    (sjas-formula-complemento formula complement)
    (sjas-internal-formula-asto complement neg-theorem)
    (== (list 'willard-sjas-theorem-code read-proof) theorem-read-proof)))

(defn- sjas-structural-negated-theorem-coreo
  "Proof-free theorem-code companion for structural proof checking."
  [prog theorem-code sigma sigma-out neg-theorem]
  (fresh [formula complement]
    (sjas-decode-proof-formula-code-coreo prog
                                          theorem-code
                                          sigma
                                          sigma-out
                                          formula)
    (sjas-formula-complemento formula complement)
    (sjas-internal-formula-asto complement neg-theorem)))

(defn- proof-symbol-indexo
  [idx sym]
  (static-table-entryo [idx sym] proof-symbol-index-entries))

(defn- proof-symbol-wide-indexo
  [high low sym]
  (static-table-entryo [high low sym] proof-symbol-wide-index-entries))

(declare decode-proof-byteso decode-structural-proof-byteso)

(defn- parse-proof-items
  [remaining bytes rest proof]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() proof)])
    (fresh [head tail after-head]
      (decode-proof-byteso bytes after-head head)
      (parse-proof-items (dec remaining) after-head rest tail)
      (== (lcons head tail) proof))))

(defn- positive-wide-proof-counto
  [high low]
  (conde
    [(positive-byteo high)
     (proof-byteo low)]
    [(== 0 high)
     (positive-byteo low)]))

(defn- decrement-wide-proof-counto
  [high low high-out low-out]
  (conde
    [(positive-byteo low)
     (== high high-out)
     (proof-byte-decremento low low-out)]
    [(positive-byteo high)
     (== 0 low)
     (proof-byte-decremento high high-out)
     (== (dec sjas-code/byte-base) low-out)]))

(defn- parse-proof-items-wide
  [high low bytes rest proof]
  (conde
    [(== 0 high)
     (== 0 low)
     (== bytes rest)
     (== '() proof)]
    [(fresh [next-high next-low head tail after-head]
       (decrement-wide-proof-counto high low next-high next-low)
       (decode-proof-byteso bytes after-head head)
       (parse-proof-items-wide next-high next-low after-head rest tail)
       (== (lcons head tail) proof))]))

(defn- parse-structural-proof-items
  [remaining bytes rest proof]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() proof)])
    (fresh [head tail after-head]
      (decode-structural-proof-byteso bytes after-head head)
      (parse-structural-proof-items (dec remaining) after-head rest tail)
      (== (lcons head tail) proof))))

(defn- parse-structural-proof-items-wide
  [high low bytes rest proof]
  (conde
    [(== 0 high)
     (== 0 low)
     (== bytes rest)
     (== '() proof)]
    [(fresh [next-high next-low head tail after-head]
       (decrement-wide-proof-counto high low next-high next-low)
       (decode-structural-proof-byteso bytes after-head head)
       (parse-structural-proof-items-wide next-high
                                          next-low
                                          after-head
                                          rest
                                          tail)
       (== (lcons head tail) proof))]))

(defn- decode-proof-list-with-counto
  [bytes rest proof]
  (conde
    [(fresh [high low after-count]
       (== (lcons sjas-code/proof-wide-list-tag
                   (lcons high (lcons low after-count)))
           bytes)
       (positive-wide-proof-counto high low)
       (parse-proof-items-wide high low after-count rest proof))]
    [(or*
       (map (fn [count]
              (fresh [after-count]
                (== (lcons sjas-code/proof-list-tag
                            (lcons (inc count) after-count))
                    bytes)
                (parse-proof-items count after-count rest proof)))
            (range 1 63)))]))

(defn- decode-structural-proof-list-with-counto
  [bytes rest proof]
  (conde
    [(fresh [high low after-count]
       (== (lcons sjas-code/proof-wide-list-tag
                   (lcons high (lcons low after-count)))
           bytes)
       (positive-wide-proof-counto high low)
       (parse-structural-proof-items-wide high low after-count rest proof))]
    [(or*
       (map (fn [count]
              (fresh [after-count]
                (== (lcons sjas-code/proof-list-tag
                            (lcons (inc count) after-count))
                    bytes)
                (parse-structural-proof-items count after-count rest proof)))
            (range 1 63)))]))

(defn- decode-proof-byteso
  "Relate a base-64 proof-code byte stream to a Proflog kernel proof term."
  [bytes rest proof]
  (conde
    [(== (lcons sjas-code/proof-empty-list-tag rest) bytes)
     (== '() proof)]
    [(fresh [idx after-symbol]
       (== (lcons sjas-code/proof-symbol-tag (lcons idx after-symbol)) bytes)
       (proof-symbol-indexo idx proof)
       (== after-symbol rest))]
    [(fresh [high low after-symbol]
       (== (lcons sjas-code/proof-wide-symbol-tag
                   (lcons high (lcons low after-symbol)))
           bytes)
       (proof-symbol-wide-indexo high low proof)
       (== after-symbol rest))]
    [(fresh [byte after-byte]
       (== (lcons sjas-code/proof-byte-tag (lcons byte after-byte)) bytes)
       (== byte proof)
       (== after-byte rest))]
    [(decode-proof-list-with-counto bytes rest proof)]))

(defn- decode-structural-proof-byteso
  "Decode proof-code bytes for SJAS formula-bearing tableau certificates.

   The proof-predicate non-axiom branch consumes tableau trees whose nodes are
   lists of formula bytes and child nodes. Symbolic Proflog proof-rule tags are
   deliberately excluded here; the checker infers rules from formulas and
   branch state instead of trusting encoded kernel trace constructors."
  [bytes rest proof]
  (conde
    [(== (lcons sjas-code/proof-empty-list-tag rest) bytes)
     (== '() proof)]
    [(fresh [byte after-byte]
       (== (lcons sjas-code/proof-byte-tag (lcons byte after-byte)) bytes)
       (== byte proof)
       (== after-byte rest))]
    [(decode-structural-proof-list-with-counto bytes rest proof)]))

(defn- decode-proof-codeo
  [code sigma sigma-out proof-bytes proof proof-read-proof]
  (fresh [rest kind]
    (sjas-formal-code-byteso code proof-bytes sigma sigma-out kind proof-read-proof)
    (decode-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(def ^:private sjas-axiom-proof-bytes
  (apply list (sjas-code/proof-code-bytes 'sjas-axiom)))

(def ^:private sjas-axiom-proof-compact-code-term
  (sjas-code/bytes->code-term sjas-axiom-proof-bytes))

(defn- decode-sjas-axiom-proof-codeo
  "Decode a public proof code as the distinguished `sjas-axiom` certificate.

   The special axiom-citation certificate is a fixed proof-grammar symbol, so
   exact proof-byte equality is enough to classify it. This keeps proof-code
   classification inside the encoded byte relation while avoiding a
   committed-choice split in `tableau-proof/3` or `subst-prf/4`."
  [code sigma sigma-out proof-bytes proof-read-proof]
  (fresh [kind]
    (== sjas-axiom-proof-bytes proof-bytes)
    (sjas-formal-code-byteso code proof-bytes sigma sigma-out kind proof-read-proof)))

(defn- decode-sjas-axiom-proof-code-coreo
  "Proof-free companion for the distinguished `sjas-axiom` certificate."
  [code sigma sigma-out proof-bytes]
  (fresh [kind]
    (== sjas-axiom-proof-bytes proof-bytes)
    (sjas-formal-code-bytes-coreo code proof-bytes sigma sigma-out kind)))

(defn- decode-non-sjas-axiom-proof-codeo
  "Decode a public proof code as a substantive proof tree.

   Substantive tableau certificates are list-root formula-bearing proof nodes.
   The one bare proof-symbol certificate accepted by these proof predicates is
   the special `sjas-axiom` citation handled by
   `decode-sjas-axiom-proof-codeo`. Non-axiom decoding uses the structural
   proof grammar, so legacy kernel proof-rule traces such as `(false-close)`
   are rejected before proof checking."
  [code sigma sigma-out proof-bytes proof proof-read-proof]
  (fresh [kind rest walked-code]
    (sjas-code-walko code sigma walked-code)
    (!= walked-code sjas-axiom-proof-compact-code-term)
    (sjas-formal-code-byteso code proof-bytes sigma sigma-out kind proof-read-proof)
    (conde
      [(fresh [item-count after-count]
         (== (lcons sjas-code/proof-list-tag (lcons item-count after-count))
             proof-bytes))]
      [(fresh [high low after-count]
         (== (lcons sjas-code/proof-wide-list-tag
                     (lcons high (lcons low after-count)))
             proof-bytes))])
    (decode-structural-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- decode-non-sjas-axiom-proof-code-coreo
  "Decode a substantive structural proof tree without code-reader proof traces."
  [code sigma sigma-out proof-bytes proof]
  (fresh [kind rest walked-code]
    (sjas-code-walko code sigma walked-code)
    (!= walked-code sjas-axiom-proof-compact-code-term)
    (sjas-formal-code-bytes-coreo code proof-bytes sigma sigma-out kind)
    (conde
      [(fresh [item-count after-count]
         (== (lcons sjas-code/proof-list-tag (lcons item-count after-count))
             proof-bytes))]
      [(fresh [high low after-count]
         (== (lcons sjas-code/proof-wide-list-tag
                     (lcons high (lcons low after-count)))
             proof-bytes))])
    (decode-structural-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- structural-proof-list-rooto
  "Require a proof-byte stream to start with a substantive proof-list tag."
  [proof-bytes]
  (conde
    [(fresh [item-count after-count]
       (== (lcons sjas-code/proof-list-tag (lcons item-count after-count))
           proof-bytes))]
    [(fresh [high low after-count]
       (== (lcons sjas-code/proof-wide-list-tag
                   (lcons high (lcons low after-count)))
           proof-bytes))]))

(defn- decode-structural-proof-bytes-coreo
  "Decode an already-read structural proof-byte stream without proof traces."
  [proof-bytes proof]
  (fresh [rest]
    (structural-proof-list-rooto proof-bytes)
    (decode-structural-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- sjas-system-profile-tago
  [profile-tag]
  (conde
    [(== system-profile-tableau0-tag profile-tag)]
    [(== system-profile-level1-tag profile-tag)]
    [(== system-profile-tab1-tag profile-tag)]
    [(== system-profile-tab2-boundary-tag profile-tag)]
    [(== system-profile-total-multiplication-tag profile-tag)]
    [(== system-profile-xtab-tag profile-tag)]))

(defn- sjas-presented-system-profile-header-coreo
  "Read only the profile header of a presented system code.

   Compact codes expose their first two byte arguments directly. Full system
   validity is established by the enclosing proof predicate; avoiding a second
   traversal of the entire reflected source at every Xtab node keeps the new
   logical rule mode-directed. U-Grounding codes use the general reader because
   their byte sequence is represented as one numeral."
  [system-code expected-profile-tag]
  (conde
    [(fresh [walked constructor args byte-count system-tag-term
             profile-tag-term rest]
       (sjas-code-walko system-code '() walked)
       (sjas-acyclic-unifyo (lcons 'app (lcons constructor args)) walked)
       (code-constructoro constructor byte-count)
       (sjas-acyclic-unifyo
         (lcons system-tag-term (lcons profile-tag-term rest))
         args)
       (code-byte-termo system-tag-term system-code-tag)
       (code-byte-termo profile-tag-term expected-profile-tag))]
    [(fresh [system-bytes kind]
       (sjas-formal-code-bytes-coreo system-code
                                      system-bytes
                                      '()
                                      '()
                                      kind)
       (== :u-grounding kind)
       (fresh [rest]
         (== (lcons system-code-tag
                    (lcons expected-profile-tag rest))
             system-bytes)))]))

(defn- sjas-level1-family-profile-tago
  "Recognize profiles whose Group-3 sentence is the Level-1 fixed point."
  [profile-tag]
  (conde
    [(== system-profile-level1-tag profile-tag)]
    [(== system-profile-total-multiplication-tag profile-tag)]
    [(== system-profile-xtab-tag profile-tag)]))

(defn- sjas-public-code-byteso
  "Expose public code bytes through the SJAS object-language code relation.

   Both compact `code-N` terms and U-Grounding numeral terms are read by
   `sjas-formal-code-byteso`. This keeps system, formula, proof, and
   substitution code reads inspectable in proof evidence instead of projecting
   already-ground Clojure terms to byte vectors outside the object relation."
  ([code bytes proof]
   (fresh [kind]
     (sjas-public-code-byteso code bytes kind proof)))
  ([code bytes kind proof]
   (fresh [read-proof sigma-out]
     (sjas-formal-code-byteso code bytes '() sigma-out kind read-proof)
     (== '() sigma-out)
     (== (list 'sjas-system-code-bytes read-proof) proof))))

(defn- sjas-public-code-bytes-coreo
  "Expose public code bytes without constructing auxiliary proof evidence."
  ([code bytes]
   (fresh [kind]
     (sjas-public-code-bytes-coreo code bytes kind)))
  ([code bytes kind]
   (fresh [sigma-out]
     (sjas-formal-code-bytes-coreo code bytes '() sigma-out kind)
     (== '() sigma-out))))

(defn- sjas-system-code-headero
  "Recognize the common header of an encoded finite SJAS system.

   The fixed Group-0 and Group-1 axioms are available for every SJAS profile, so
   citation checking only needs to know that the supplied system code has the
   system tag and one of the known profile tags. The beta and reflected tails
   are deliberately left opaque here; later axiom-group relations inspect them
   when those groups are relevant."
  [system-bytes proof]
  (fresh [profile-tag rest]
    (== (lcons system-code-tag (lcons profile-tag rest)) system-bytes)
    (sjas-system-profile-tago profile-tag)
    (== '(sjas-system-code-header) proof)))

(defn- fixed-axiom-formulao
  "Compare a decoded theorem formula with one fixed SJAS axiom group."
  [formulas proof-step formula proof]
  (or*
    (map (fn [expected]
           (fresh []
             (sjas-alpha-formula-equivo expected formula '())
             (== (list proof-step) proof)))
         formulas)))

(defn- sjas-fixed-axiom-formulao
  "Recognize fixed Group-0 and Group-1 axiom formulas after formula decoding.

   Formula codes canonicalize object numerals into compact `num` payloads, so
   the first two Group-1 equations decode as numeric identities rather than as
   literal `add`/`dbl` application terms. This relation matches the actual code
   representation that generated axiom records use."
  [formula proof]
  (conde
    [(fixed-axiom-formulao group-zero-internal-formulas
                           'sjas-system-group-zero-axiom
                           formula
                           proof)]
    [(fixed-axiom-formulao group-one-internal-formulas
                           'sjas-system-group-one-axiom
                           formula
                           proof)]))

(defn- sjas-fixed-axiom-membero
  "Cite fixed SJAS axioms from the decoded system profile, not host facts."
  [prog system-code formula-code proof]
  (fresh [system-bytes formula-bytes system-read-proof formula-read-proof
          header-proof formula fixed-proof]
    (sjas-public-code-byteso system-code system-bytes system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (sjas-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (sjas-fixed-axiom-formulao formula fixed-proof)
    (== (list 'sjas-system-fixed-axiom
              system-read-proof
              formula-read-proof
              header-proof
              fixed-proof)
        proof)))

(defn- sjas-tableau0-system-code-headero
  "Recognize the header of a Tableau-0 SJAS system code."
  [system-bytes proof]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-tableau0-tag rest))
        system-bytes)
    (== '(sjas-system-tableau0-profile) proof)))

(defn- tableau0-group-three-code-termso
  "Build the embedded system/contradiction code terms used by Group-3.

   Compact public codes embed as `(code bytes)`. U-Grounding public codes embed
   as numerals whose payload is the byte string followed by the code sentinel.
   Both representations denote the same object-level code bytes; this relation
   accepts whichever representation the compiled system selected."
  [system-bytes system-term contradiction-term]
  (conde
    [(== (list 'code system-bytes) system-term)
     (== (list 'code tableau0-contradiction-formula-bytes)
         contradiction-term)]
    [(fresh [encoded-system]
       (append-sentinel-byteo system-bytes encoded-system)
       (== (list 'num encoded-system) system-term)
       (== (list 'num tableau0-contradiction-u-grounding-bytes)
           contradiction-term))]))

(defn- code-kind-internal-termo
  "Relate a public code representation kind to its decoded formula-term shape.

   Group-3 is a fixed-point axiom over the public system code `s`, not merely
   over the byte string denoted by `s`. Carrying the code-reader's `kind` into
   formula reconstruction prevents a U-Grounding system from accepting a
   compact-code variant of its self-consistency sentence, and conversely."
  [kind bytes term]
  (conde
    [(== :compact kind)
     (== (list 'code bytes) term)]
    [(fresh [encoded]
       (== :u-grounding kind)
       (append-sentinel-byteo bytes encoded)
       (== (list 'num encoded) term))]))

(defn- tableau0-group-three-code-terms-for-kindo
  "Build Tableau-0 Group-3 code arguments in the presented public code format."
  [kind system-bytes system-term contradiction-term]
  (fresh []
    (code-kind-internal-termo kind system-bytes system-term)
    (code-kind-internal-termo kind
                              tableau0-contradiction-formula-bytes
                              contradiction-term)))

(defn- tableau0-group-three-formulao
  "Reconstruct the Tableau-0 self-consistency axiom from a system-code byte list."
  [system-bytes formula proof]
  (fresh [system-term contradiction-term]
    (tableau0-group-three-code-termso system-bytes
                                      system-term
                                      contradiction-term)
    (== (list 'forall
              1
              (list 'neg
                    (list 'app
                          'dsjas-tableau-proof
                          (list system-term
                                contradiction-term
                                (list 'var 1)))))
        formula)
    (== '(sjas-system-tableau0-group-three-axiom) proof)))

(defn- tableau0-group-three-formula-for-kindo
  "Reconstruct the Tableau-0 Group-3 axiom in the presented code format."
  [kind system-bytes formula proof]
  (fresh [system-term contradiction-term]
    (tableau0-group-three-code-terms-for-kindo kind
                                               system-bytes
                                               system-term
                                               contradiction-term)
    (== (list 'forall
              1
              (list 'neg
                    (list 'app
                          'dsjas-tableau-proof
                          (list system-term
                                contradiction-term
                                (list 'var 1)))))
        formula)
    (== '(sjas-system-tableau0-group-three-axiom) proof)))

(defn- sjas-tableau0-group-three-axiom-membero
  "Cite the Tableau-0 Group-3 axiom from system-code, not generated facts."
  [prog system-code formula-code proof]
  (fresh [system-bytes system-kind formula-bytes system-read-proof formula-read-proof
          header-proof formula group-three-proof]
    (sjas-public-code-byteso system-code
                             system-bytes
                             system-kind
                             system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-tableau0-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tableau0-group-three-formula-for-kindo system-kind
                                            system-bytes
                                            formula
                                            group-three-proof)
    (== (list 'sjas-system-group-three-axiom
              system-read-proof
              formula-read-proof
              header-proof
              group-three-proof)
        proof)))

(defn- sjas-level1-system-code-headero
  "Recognize the header of a Level-1-family SJAS system code."
  [system-bytes proof]
  (fresh [profile-tag rest]
    (== (lcons system-code-tag
                (lcons profile-tag rest))
        system-bytes)
    (sjas-level1-family-profile-tago profile-tag)
    (== '(sjas-system-level1-profile) proof)))

(defn- system-code-internal-termo
  "Relate a system-code byte string to its embedded formula-code term shape."
  [system-bytes term]
  (conde
    [(== (list 'code system-bytes) term)]
    [(fresh [encoded-system]
       (append-sentinel-byteo system-bytes encoded-system)
       (== (list 'num encoded-system) term))]))

(defn- internal-code-term-byteso
  "Expose the formula-code bytes denoted by a decoded internal code term.

   This is used inside the Level-1 Group-3 check. The final self-consistency
   formula contains the code of its own skeleton. Rather than consulting the
   source builder's stored skeleton code, the profile reads that embedded code
   term and decodes the referenced formula bytes during predicate application."
  [term bytes]
  (conde
    [(== (list 'code bytes) term)]
    [(fresh [encoded]
       (== (list 'num encoded) term)
       (append-sentinel-byteo bytes encoded))]))

(defn- level1-selfcons-internal-formula
  "Build the decoded internal form of Willard's Level-1 self-consistency axiom.

   `x`, `y`, `p`, and `q` are formula-code binder indexes. The final Group-3
   axiom uses indexes 1-4. The skeleton code is generated with free `v0` already
   present, so its binders start at indexes 2-5 and its substitution argument is
   `(var 1)`."
  [system-term substitution-term x y p q]
  (let [x-term (list 'var x)
        y-term (list 'var y)
        p-term (list 'var p)
        q-term (list 'var q)
        ;; Willard 2013 (7): Pair(x,y) requires `x` to code a Pi-star-1
        ;; sentence; the pair restriction is encoded as the pi-star-1-code
        ;; conjunct ahead of complement pairing (ADR-0087).
        pi-restriction (list 'neg
                             (list 'app 'pi-star-1-code (list x-term)))
        neg-pair (list 'neg
                       (list 'app 'neg-pair (list x-term y-term)))
        left-subst (list 'neg
                         (list 'app
                               'dsjas-subst-prf
                               (list system-term
                                     substitution-term
                                     x-term
                                     p-term)))
        right-subst (list 'neg
                          (list 'app
                                'dsjas-subst-prf
                                (list system-term
                                      substitution-term
                                      y-term
                                      q-term)))]
    (list 'forall
          x
          (list 'forall
                y
                (list 'forall
                      p
                      (list 'forall
                            q
                            (list 'or
                                  pi-restriction
                                  (list 'or
                                        neg-pair
                                        (list 'or left-subst right-subst)))))))))

(defn- level1-group-three-formulao
  "Validate the Level-1 fixed-point axiom and its embedded skeleton code."
  [prog system-bytes formula proof]
  (fresh [system-term substitution-term skeleton-bytes skeleton-formula]
    (system-code-internal-termo system-bytes system-term)
    (== (level1-selfcons-internal-formula system-term
                                          substitution-term
                                          1
                                          2
                                          3
                                          4)
        formula)
    (internal-code-term-byteso substitution-term skeleton-bytes)
    (decode-formula-byteso prog skeleton-bytes '() skeleton-formula)
    (== (level1-selfcons-internal-formula system-term
                                          (list 'var 1)
                                          2
                                          3
                                          4
                                          5)
        skeleton-formula)
    (== '(sjas-system-level1-group-three-axiom) proof)))

(defn- level1-group-three-formula-for-kindo
  "Validate Level-1 Group-3 using the public representation selected by `s`."
  [prog kind system-bytes formula proof]
  (fresh [system-term substitution-term skeleton-bytes skeleton-formula]
    (code-kind-internal-termo kind system-bytes system-term)
    (== (level1-selfcons-internal-formula system-term
                                          substitution-term
                                          1
                                          2
                                          3
                                          4)
        formula)
    (internal-code-term-byteso substitution-term skeleton-bytes)
    (decode-formula-byteso prog skeleton-bytes '() skeleton-formula)
    (== (level1-selfcons-internal-formula system-term
                                          (list 'var 1)
                                          2
                                          3
                                          4
                                          5)
        skeleton-formula)
    (== '(sjas-system-level1-group-three-axiom) proof)))

(defn- level1-selfcons-skeleton-code-coreo
  "Validate the exact fixed-point skeleton code used by Level-1 SelfCons.

   Fully relational: the presented system bytes are decoded to an internal term
   and the presented substitution code to its skeleton formula, then the
   skeleton is checked to be exactly the open `Gamma_1(g)` self-consistency
   schema for that system (the same construction the Group-3 axiom citation
   uses). No host `project`/byte shortcut rebuilds the skeleton outside the
   relation; the ground system and substitution codes drive the decode by goal
   order. The system bytes are already tied to `system-code` by the caller."
  [prog _system-code system-bytes substitution-code sigma sigma-out]
  (fresh [system-term substitution-bytes substitution-kind skeleton-formula]
    (system-code-internal-termo system-bytes system-term)
    (sjas-formal-code-bytes-coreo substitution-code
                                  substitution-bytes
                                  sigma
                                  sigma-out
                                  substitution-kind)
    (decode-formula-byteso prog substitution-bytes '() skeleton-formula)
    (== (level1-selfcons-internal-formula system-term
                                          (list 'var 1)
                                          2
                                          3
                                          4
                                          5)
        skeleton-formula)))

(defn- sjas-level1-group-three-axiom-membero
  "Cite the Level-1 Group-3 axiom by checking its fixed-point skeleton."
  [prog system-code formula-code proof]
  (fresh [system-bytes system-kind formula-bytes system-read-proof formula-read-proof
          header-proof formula group-three-proof]
    (sjas-public-code-byteso system-code
                             system-bytes
                             system-kind
                             system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-level1-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (level1-group-three-formula-for-kindo prog
                                          system-kind
                                          system-bytes
                                          formula
                                          group-three-proof)
    (== (list 'sjas-system-group-three-axiom
              system-read-proof
              formula-read-proof
              header-proof
              group-three-proof)
        proof)))

(def ^:private tab2-boundary-proof-symbol
  "Internal formula-code head for the target-only Tab-2 proof predicate.

   The encoder gives `dsjas-tab2-proof` a stable profile-local index, while the
   ordinary decoder intentionally leaves that index structural so existing user
   relations remain decodable without a source symbol registry."
  (list 'sym (sjas-code/reserved-symbol->index 'dsjas-tab2-proof)))

(defn- tab2-boundary-selfcons-internal-formula
  "Decoded internal Tab-2 boundary SelfCons formula for a system code term."
  [system-term x y p q witness]
  (let [x-term (list 'var x)
        y-term (list 'var y)
        p-term (list 'var p)
        q-term (list 'var q)
        neg-pair (list 'neg
                       (list 'app 'neg-pair (list x-term y-term)))
        left-proof (list 'neg
                         (list 'app
                               tab2-boundary-proof-symbol
                               (list system-term x-term p-term)))
        right-proof (list 'neg
                          (list 'app
                                tab2-boundary-proof-symbol
                                (list system-term y-term q-term)))]
    (list 'forall
          x
          (list 'forall
                y
                (list 'forall
                      p
                      (list 'forall
                            q
                            (list 'exists
                                  witness
                                  (list 'or
                                        neg-pair
                                        (list 'or left-proof right-proof)))))))))

(defn- sjas-tab2-boundary-system-code-headero
  "Recognize the header of a target-only Tab-2 boundary SJAS system code."
  [system-bytes proof]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-tab2-boundary-tag rest))
        system-bytes)
    (== '(sjas-system-tab2-boundary-profile) proof)))

(defn- tab2-boundary-group-three-formula-for-kindo
  "Reconstruct target-only Tab-2 Group-3 in the presented code format."
  [kind system-bytes formula proof]
  (fresh [system-term]
    (code-kind-internal-termo kind system-bytes system-term)
    (== (tab2-boundary-selfcons-internal-formula system-term 1 2 3 4 5)
        formula)
    (== '(sjas-system-tab2-boundary-group-three-axiom) proof)))

(defn- sjas-tab2-boundary-group-three-axiom-membero
  "Cite the target-only Tab-2 boundary Group-3 axiom from system-code."
  [prog system-code formula-code proof]
  (fresh [system-bytes system-kind formula-bytes system-read-proof formula-read-proof
          header-proof formula group-three-proof]
    (sjas-public-code-byteso system-code
                             system-bytes
                             system-kind
                             system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-tab2-boundary-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tab2-boundary-group-three-formula-for-kindo system-kind
                                                 system-bytes
                                                 formula
                                                 group-three-proof)
    (== (list 'sjas-system-group-three-axiom
              system-read-proof
              formula-read-proof
              header-proof
              group-three-proof)
        proof)))

(defn- byte-prefixo
  [prefix bytes rest]
  (conde
    [(== '() prefix)
     (== bytes rest)]
    [(fresh [head prefix-tail bytes-tail]
       (== (lcons head prefix-tail) prefix)
       (== (lcons head bytes-tail) bytes)
       (byte-prefixo prefix-tail bytes-tail rest))]))

(declare byte-list-neqo)

(defn- sjas-beta-member-in-formula-byteso
  [_prog remaining bytes formula-bytes proof]
  (if (zero? remaining)
    fail
    (fresh [after-current]
      (conde
        ;; Formula-code byte streams are prefix-delimited by their root tags and
        ;; arities. Try the exact candidate prefix before parsing the current
        ;; beta formula; ground axiom citations should not pay a full syntax
        ;; walk merely to discover that the first beta record matches.
        [(byte-prefixo formula-bytes bytes after-current)
         (== '(sjas-system-beta-axiom) proof)]
        [(fresh [current-bytes]
           (skip-syntax-formula-byteso bytes after-current)
           (byte-prefixo current-bytes bytes after-current)
           (byte-list-neqo formula-bytes current-bytes)
           (sjas-beta-member-in-formula-byteso _prog
                                               (dec remaining)
                                               after-current
                                               formula-bytes
                                               proof))]))))

(defn- sjas-system-beta-formula-byteso
  [prog system-bytes formula-bytes proof]
  (fresh [profile-tag beta-count beta-bytes]
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (sjas-beta-member-in-formula-byteso prog
                                                   beta-total
                                                   beta-bytes
                                                   formula-bytes
                                                   proof)))
           (range sjas-code/byte-base)))))

(defn- sjas-beta-axiom-membero
  [prog system-code formula-code proof]
  (fresh [system-bytes formula-bytes system-read-proof formula-read-proof beta-proof]
    (sjas-public-code-byteso system-code system-bytes system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (sjas-system-beta-formula-byteso prog system-bytes formula-bytes beta-proof)
    (== (list 'sjas-system-beta-axiom
              system-read-proof
              formula-read-proof
              beta-proof)
        proof)))

(defn- skip-formula-byteso
  "Advance over `remaining` encoded formulas in a system-code byte tail.

   System codes store the finite Group-2 beta block first, followed by the
   reflected Group-2b clause block. Reflected-clause lookup therefore has to
   consume the beta formulas structurally before it can inspect the reflected
   section. The decoded formulas are intentionally discarded here; this relation
   only proves that the bytes form well-encoded formulas and exposes the later
   tail."
  [_prog remaining bytes rest]
  (if (zero? remaining)
    (== bytes rest)
    (fresh [after-formula]
      (skip-syntax-formula-byteso bytes after-formula)
      (skip-formula-byteso _prog (dec remaining) after-formula rest))))

(defn- skip-reflected-clause-byteso
  "Advance over one encoded reflected-clause source record.

   The source record layout is `[34 relation-index arity+1 body...]`. `FinAx4`
   needs to know that the finite source is well shaped, not to reconstruct the
   reflected axiom formula or recover source relation names."
  [bytes rest]
  (fresh [relation-index arity-byte body-bytes]
    (== (lcons system-reflected-clause-tag
                (lcons relation-index
                       (lcons arity-byte body-bytes)))
        bytes)
    (positive-byteo relation-index)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (skip-syntax-formula-byteso body-bytes rest)))
           (range sjas-code/byte-base)))))

(defn- skip-reflected-clause-bytes*o
  "Advance over `remaining` reflected-clause source records."
  [remaining bytes rest]
  (if (zero? remaining)
    (== bytes rest)
    (fresh [after-record]
      (skip-reflected-clause-byteso bytes after-record)
      (skip-reflected-clause-bytes*o (dec remaining) after-record rest))))

(defn- sjas-system-source-valid-coreo
  "Proof-free recognizer for the finite source part of an SJAS system code.

   This checks exactly the source encoding consumed by `FinAx4`: system tag,
   supported profile tag, finite beta formula block, finite reflected-clause
   block, and no trailing bytes. It intentionally does not reconstruct Group-0,
   Group-1, or Group-3 theorem antecedents."
  [prog system-bytes]
  (fresh [profile-tag beta-count beta-bytes after-betas
          reflected-count reflected-bytes rest]
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (skip-reflected-clause-bytes*o reflected-total
                                                        reflected-bytes
                                                        rest)
                          (== '() rest)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- reflected-head-argso
  "Build the canonical head argument list for an encoded reflected clause.

   A reflected clause record stores only relation name, arity, and body. Its
   axiom formula is reconstructed as `forall x1 ... forall xn. body -> R(x1,
   ..., xn)`, using the same one-based variable indexes that the formula-code
   encoder writes into object codes."
  [idx arity args]
  (if (> idx arity)
    (== '() args)
    (fresh [tail]
      (== (lcons (list 'var idx) tail) args)
      (reflected-head-argso (inc idx) arity tail))))

(defn- reflected-forall-wrapo
  "Wrap a reconstructed reflected-clause implication in its universal binders."
  [idx arity body formula]
  (if (> idx arity)
    (== body formula)
    (fresh [inner]
      (== (list 'forall idx inner) formula)
      (reflected-forall-wrapo (inc idx) arity body inner))))

(defn- reflected-clause-formulao
  "Relate an encoded reflected clause's relation/arity/body to its axiom text."
  [arity relation body formula]
  (fresh [args head implication]
    (reflected-head-argso 1 arity args)
    (== (list 'pos (list 'app relation args)) head)
    (== (list 'implies body head) implication)
    (reflected-forall-wrapo 1 arity implication formula)))

(defn- decode-reflected-clause-formulao
  "Decode one reflected-clause record from the reflected section of system-code.

   The record layout is `[34 relation-index arity+1 body-formula-bytes...]`.
   Fixed SJAS vocabulary indexes decode to their semantic symbols, while user
   relation indexes stay structural. No source-language symbol table is needed
   to reconstruct the axiom tree."
  [prog bytes rest formula]
  (fresh [relation-index arity-byte body-bytes relation body]
    (== (lcons system-reflected-clause-tag
                (lcons relation-index
                       (lcons arity-byte body-bytes)))
        bytes)
    (sjas-object-symbol-indexo relation-index relation)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (decode-formula-byteso prog body-bytes rest body)
               (reflected-clause-formulao arity relation body formula)))
           (range sjas-code/byte-base)))))

(defn- decode-reflected-clause-syntax-formulao
  "Decode a reflected-clause axiom without resolving source symbol names.

   Axiom membership only needs the formula tree induced by the reflected record.
   It does not need the host relation symbol used later by procedure-call proof
   reconstruction, so this decoder keeps the reflected relation as `(sym idx)`
   and decodes the body through the syntax-only formula decoder."
  [bytes rest formula]
  (fresh [relation-index arity-byte body-bytes relation body]
    (== (lcons system-reflected-clause-tag
                (lcons relation-index
                       (lcons arity-byte body-bytes)))
        bytes)
    (positive-byteo relation-index)
    (== (list 'sym relation-index) relation)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (decode-syntax-formula-byteso body-bytes rest body)
               (reflected-clause-formulao arity relation body formula)))
           (range sjas-code/byte-base)))))

(declare sjas-negated-formula-asto
         formula-list-appendo)

(defn- reflected-call-env-argso
  "Bind canonical reflected-clause parameters to actual call arguments.

   Reflected clause records use one-based formula-code variable indexes for
   their formal parameters. The proof checker decodes those indexes to the same
   fixed `sjas-vN` noms used by formula-code theorem decoding, then builds the
   ordinary branch environment expected by `subst/subst-formulao`."
  [idx arity args env]
  (if (> idx arity)
    (fresh []
      (== '() args)
      (== '() env))
    (fresh [arg arg-rest env-rest nom]
      (== (lcons arg arg-rest) args)
      (membero [idx nom] code-nom-entries)
      (== (lcons [nom arg] env-rest) env)
      (reflected-call-env-argso (inc idx) arity arg-rest env-rest))))

(defn- term-list-exact-lengtho
  [remaining terms]
  (if (zero? remaining)
    (== '() terms)
    (fresh [head tail]
      (== (lcons head tail) terms)
      (term-list-exact-lengtho (dec remaining) tail))))

(defn- reflected-atom-relation-indexo
  "Relate a decoded call atom relation to the reflected system-code symbol id.

   Fixed SJAS vocabulary has reserved semantic symbols. User relations decoded
   from formula codes keep the structural `(sym idx)` form. This relation
   recovers the same finite byte index in either case without consulting the
   source-language symbol table."
  [relation relation-index]
  (conde
    [(sjas-reserved-symbol-indexo relation-index relation)]
    [(positive-byteo relation-index)
     (== (list 'sym relation-index) relation)]))

(defn- reflected-atom-arity-byteo
  "Relate a focused call atom's argument list to the encoded arity byte."
  [args arity-byte]
  (or*
    (map (fn [arity]
           (fresh []
             (== (inc arity) arity-byte)
             (term-list-exact-lengtho arity args)))
         (range (dec sjas-code/byte-base)))))

(defn- reflected-call-header-matcho
  "Check that one encoded reflected-clause header matches the focused call."
  [atom relation-index arity-byte]
  (fresh [relation args]
    (== (lcons 'app (lcons relation args)) atom)
    (reflected-atom-relation-indexo relation relation-index)
    (reflected-atom-arity-byteo args arity-byte)))

(defn- reflected-call-header-nonmatcho
  "Prove that an encoded reflected-clause header cannot match the focused call.

   This is the object-level replacement for using `conda` as an if-then-else
   while collecting reflected alternatives. A clause is skipped only when its
   relation index differs from the focused call's index, or when the relation
   index agrees but the encoded arity byte differs from the call's arity byte."
  [atom relation-index arity-byte]
  (fresh [relation args atom-relation-index atom-arity-byte]
    (== (lcons 'app (lcons relation args)) atom)
    (reflected-atom-relation-indexo relation atom-relation-index)
    (reflected-atom-arity-byteo args atom-arity-byte)
    (conde
      [(positive-byte-neqo relation-index atom-relation-index)]
      [(== relation-index atom-relation-index)
       (positive-byte-neqo arity-byte atom-arity-byte)])))

(defn- decode-reflected-clause-callo
  "Decode one reflected-clause record as a Procedure Call Rule target.

   This is the procedure-call analogue of `decode-reflected-clause-formulao`.
   It reads `[34 relation-index arity+1 body-formula...]` directly from the
   reflected block of `system-code`, matches the decoded relation and arity
   against the focused call atom, and exposes the body/negated-body formulas
   needed by `pos-call` and `neg-call` proof constructors."
  [prog bytes rest atom env body negated-body]
  (fresh [relation args relation-index arity-byte body-bytes
          decoded-body nnf-body]
    (== (lcons 'app (lcons relation args)) atom)
    (== (lcons system-reflected-clause-tag
                (lcons relation-index
                       (lcons arity-byte body-bytes)))
        bytes)
    (sjas-object-symbol-indexo relation-index relation)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (reflected-call-env-argso 1 arity args env)
               (decode-formula-byteso prog body-bytes rest decoded-body)
               (sjas-to-nnfo decoded-body nnf-body)
               (sjas-internal-formula-asto nnf-body body)
               (sjas-negated-formula-asto nnf-body negated-body)))
           (range sjas-code/byte-base)))))

(defn- reflected-call-in-clauseso
  "Search encoded reflected clauses for a call-compatible procedure body."
  [prog remaining bytes atom env body negated-body]
  (if (zero? remaining)
    fail
    (fresh [after-current]
      (conde
        [(decode-reflected-clause-callo prog
                                        bytes
                                        after-current
                                        atom
                                        env
                                        body
                                        negated-body)]
        [(fresh [current]
           (decode-reflected-clause-syntax-formulao bytes after-current current)
           (reflected-call-in-clauseso prog
                                       (dec remaining)
                                       after-current
                                       atom
                                       env
                                       body
                                       negated-body))]))))

(defn- reflected-call-alternatives-in-clauseso
  "Collect all encoded reflected clauses matching one procedure call.

   The generic Proflog kernel compiles same-relation clauses into a finite list
   of alternatives and `neg-call-alt` closes one negated alternative from that
   list. SJAS proof-predicate checking cannot use that compiled host table, so
   this relation reconstructs the matching negated alternatives by scanning the
   reflected Group-2b records inside `system-code`."
  [prog remaining bytes atom env negated-alternatives]
  (if (zero? remaining)
    (== '() negated-alternatives)
    (fresh [relation-index arity-byte body-bytes after-current]
      (== (lcons system-reflected-clause-tag
                  (lcons relation-index
                         (lcons arity-byte body-bytes)))
          bytes)
      (conde
        [(reflected-call-header-matcho atom relation-index arity-byte)
         (fresh [body negated-body rest]
           (decode-reflected-clause-callo prog
                                          bytes
                                          after-current
                                          atom
                                          env
                                          body
                                          negated-body)
           (== (lcons negated-body rest) negated-alternatives)
           (reflected-call-alternatives-in-clauseso prog
                                                    (dec remaining)
                                                    after-current
                                                    atom
                                                    env
                                                    rest))]
        [(reflected-call-header-nonmatcho atom relation-index arity-byte)
         (fresh [current]
           (decode-reflected-clause-syntax-formulao bytes after-current current)
           (reflected-call-alternatives-in-clauseso prog
                                                    (dec remaining)
                                                    after-current
                                                    atom
                                                    env
                                                    negated-alternatives))]))))

(defn- internal-non-and-formulao
  [formula]
  (conde
    [(== (list 'true) formula)]
    [(== (list 'false) formula)]
    [(fresh [term]
       (== (list 'pos term) formula))]
    [(fresh [term]
       (== (list 'neg term) formula))]
    [(fresh [left right]
       (== (list 'eq left right) formula))]
    [(fresh [left right]
       (== (list 'neq left right) formula))]
    [(fresh [left right]
       (== (list 'or left right) formula))]
    [(fresh [body]
       (== (list 'not body) formula))]
    [(fresh [left right]
       (== (list 'implies left right) formula))]
    [(fresh [idx body]
       (== (list 'forall idx body) formula))]
    [(fresh [idx body]
       (== (list 'once-forall idx body) formula))]
    [(fresh [idx body]
       (== (list 'exists idx body) formula))]
    [(fresh [idx bound body]
       (== (list 'bounded-forall idx bound body) formula))]
    [(fresh [idx bound body]
       (== (list 'bounded-exists idx bound body) formula))]))

(defn- internal-formula-conjunctso
  "Flatten a decoded internal formula's top-level conjunctions."
  [formula conjuncts]
  (conde
    [(fresh [left right left-conjuncts right-conjuncts]
       (== (list 'and left right) formula)
       (internal-formula-conjunctso left left-conjuncts)
       (internal-formula-conjunctso right right-conjuncts)
       (formula-list-appendo left-conjuncts right-conjuncts conjuncts))]
    [(internal-non-and-formulao formula)
     (== (lcons formula '()) conjuncts)]))

(defn- internal-formula-list-negated-asto
  "Translate decoded internal formulas to their AST-level NNF negations."
  [formulas ast-formulas]
  (conde
    [(== '() formulas)
     (== '() ast-formulas)]
    [(fresh [formula rest ast-formula ast-rest]
       (== (lcons formula rest) formulas)
       (== (lcons ast-formula ast-rest) ast-formulas)
       (sjas-negated-formula-asto formula ast-formula)
       (internal-formula-list-negated-asto rest ast-rest))]))

(defn- internal-formula-list-asto
  "Translate decoded internal formulas to ordinary AST formulas."
  [formulas ast-formulas]
  (conde
    [(== '() formulas)
     (== '() ast-formulas)]
    [(fresh [formula rest ast-formula ast-rest]
       (== (lcons formula rest) formulas)
       (== (lcons ast-formula ast-rest) ast-formulas)
       (sjas-internal-formula-asto formula ast-formula)
       (internal-formula-list-asto rest ast-rest))]))

(defn- internal-guard-formulao
  [formula]
  (conde
    [(fresh [left right]
       (== (list 'eq left right) formula))]
    [(fresh [left right]
       (== (list 'neq left right) formula))]))

(defn- reflected-relation-index-in-clauseso
  "True when `relation-index` names a reflected relation in system-code bytes."
  [remaining bytes relation-index]
  (if (zero? remaining)
    fail
    (fresh [current-index arity-byte body-bytes after-current current]
      (conde
        [(== (lcons system-reflected-clause-tag
                    (lcons current-index
                           (lcons arity-byte body-bytes)))
             bytes)
         (decode-reflected-clause-syntax-formulao bytes after-current current)
         (== current-index relation-index)]
        [(decode-reflected-clause-syntax-formulao bytes after-current current)
         (reflected-relation-index-in-clauseso
           (dec remaining)
           after-current
           relation-index)]))))

(defn- internal-call-formulao
  "Recognize decoded conjuncts that call a relation reflected in system-code."
  [prog reflected-total reflected-bytes formula]
  (fresh [atom relation args relation-index]
    (conde
      [(== (list 'pos atom) formula)]
      [(== (list 'neg atom) formula)])
    (== (list 'app relation args) atom)
    (conde
      [(== (list 'sym relation-index) relation)]
      [(sjas-object-symbol-indexo relation-index relation)])
    (reflected-relation-index-in-clauseso
      reflected-total
      reflected-bytes
      relation-index)))

(defn- internal-guarded-conjunct-partitiono
  "Partition decoded conjuncts into guard, reflected call, and residual groups."
  [prog reflected-total reflected-bytes conjuncts guards calls residuals]
  (conde
    [(== '() conjuncts)
     (== '() guards)
     (== '() calls)
     (== '() residuals)]
    [(fresh [conjunct rest guard-rest]
       (== (lcons conjunct rest) conjuncts)
       (internal-guard-formulao conjunct)
       (== (lcons conjunct guard-rest) guards)
       (internal-guarded-conjunct-partitiono prog
                                             reflected-total
                                             reflected-bytes
                                             rest
                                             guard-rest
                                             calls
                                             residuals))]
    [(fresh [conjunct rest call-rest]
       (== (lcons conjunct rest) conjuncts)
       (internal-call-formulao prog reflected-total reflected-bytes conjunct)
       (== (lcons conjunct call-rest) calls)
       (internal-guarded-conjunct-partitiono prog
                                             reflected-total
                                             reflected-bytes
                                             rest
                                             guards
                                             call-rest
                                             residuals))]
    [(fresh [conjunct rest residual-rest]
       (== (lcons conjunct rest) conjuncts)
       (== (lcons conjunct residual-rest) residuals)
       (internal-guarded-conjunct-partitiono prog
                                             reflected-total
                                             reflected-bytes
                                             rest
                                             guards
                                             calls
                                             residual-rest))]))

(defn- internal-non-exists-formulao
  [formula]
  (conde
    [(== (list 'true) formula)]
    [(== (list 'false) formula)]
    [(fresh [term]
       (== (list 'pos term) formula))]
    [(fresh [term]
       (== (list 'neg term) formula))]
    [(fresh [left right]
       (== (list 'eq left right) formula))]
    [(fresh [left right]
       (== (list 'neq left right) formula))]
    [(fresh [left right]
       (== (list 'and left right) formula))]
    [(fresh [left right]
       (== (list 'or left right) formula))]
    [(fresh [body]
       (== (list 'not body) formula))]
    [(fresh [left right]
       (== (list 'implies left right) formula))]
    [(fresh [idx body]
       (== (list 'forall idx body) formula))]
    [(fresh [idx body]
       (== (list 'once-forall idx body) formula))]
    [(fresh [idx bound body]
       (== (list 'bounded-forall idx bound body) formula))]
    [(fresh [idx bound body]
       (== (list 'bounded-exists idx bound body) formula))]))

(defn- internal-leading-exists-scopeo
  "Strip leading decoded existential binders for guarded negative-call scope."
  [formula scope core]
  (conde
    [(fresh [idx body nom rest]
       (== (list 'exists idx body) formula)
       (membero [idx nom] code-nom-entries)
       (== (lcons nom rest) scope)
       (internal-leading-exists-scopeo body rest core))]
    [(internal-non-exists-formulao formula)
     (== '() scope)
     (== formula core)]))

(defn- internal-guarded-alternative-asto
  "Recover guarded-negative proof data for one reflected body.

   The generic guarded call path closes the negation of one alternative by
   either proving the fallback negation of each conjunct in source order, or by
   saturating equality guards and then proving negated residuals. For Track 1
   proof-predicate checking, reconstruct these lists from the reflected clause
   body decoded out of `system-code` rather than from the compiled
   guarded-clause host table."
  [prog reflected-total reflected-bytes formula guarded-alternative]
  (fresh [scope core conjuncts guards calls residuals ast-guards
          ast-negated-calls ast-negated-residuals ast-negated-conjuncts]
    (internal-leading-exists-scopeo formula scope core)
    (internal-formula-conjunctso core conjuncts)
    (internal-guarded-conjunct-partitiono prog
                                          reflected-total
                                          reflected-bytes
                                          conjuncts
                                          guards
                                          calls
                                          residuals)
    (internal-formula-list-asto guards ast-guards)
    (internal-formula-list-negated-asto calls ast-negated-calls)
    (internal-formula-list-negated-asto residuals ast-negated-residuals)
    (internal-formula-list-negated-asto conjuncts ast-negated-conjuncts)
    (== (list 'guarded-alternative
              scope
              ast-guards
              ast-negated-calls
              ast-negated-residuals
              ast-negated-conjuncts)
        guarded-alternative)))

(defn- decode-reflected-clause-guarded-callo
  "Decode one reflected-clause record as guarded negative-call data."
  [prog reflected-total reflected-bytes bytes rest atom env guarded-alternative]
  (fresh [relation args relation-index arity-byte body-bytes
          decoded-body nnf-body]
    (== (lcons 'app (lcons relation args)) atom)
    (== (lcons system-reflected-clause-tag
                (lcons relation-index
                       (lcons arity-byte body-bytes)))
        bytes)
    (sjas-object-symbol-indexo relation-index relation)
    (or*
      (map (fn [arity]
             (fresh []
               (== (inc arity) arity-byte)
               (reflected-call-env-argso 1 arity args env)
               (decode-proof-formula-byteso prog body-bytes rest decoded-body)
               (sjas-to-nnfo decoded-body nnf-body)
               (internal-guarded-alternative-asto
                 prog
                 reflected-total
                 reflected-bytes
                 nnf-body
                 guarded-alternative)))
           (range sjas-code/byte-base)))))

(defn- reflected-call-guarded-alternatives-in-clauseso
  "Collect guarded negative-call alternatives from encoded reflected clauses."
  [prog reflected-total reflected-bytes remaining bytes atom env guarded-alternatives]
  (if (zero? remaining)
    (== '() guarded-alternatives)
    (fresh [relation-index arity-byte body-bytes after-current]
      (== (lcons system-reflected-clause-tag
                  (lcons relation-index
                         (lcons arity-byte body-bytes)))
          bytes)
      (conde
        [(reflected-call-header-matcho atom relation-index arity-byte)
         (fresh [guarded-alternative rest]
           (decode-reflected-clause-guarded-callo prog
                                                  reflected-total
                                                  reflected-bytes
                                                  bytes
                                                  after-current
                                                  atom
                                                  env
                                                  guarded-alternative)
           (== (lcons guarded-alternative rest) guarded-alternatives)
           (reflected-call-guarded-alternatives-in-clauseso
             prog
             reflected-total
             reflected-bytes
             (dec remaining)
             after-current
             atom
             env
             rest))]
        [(reflected-call-header-nonmatcho atom relation-index arity-byte)
         (fresh [current]
           (decode-reflected-clause-syntax-formulao bytes after-current current)
           (reflected-call-guarded-alternatives-in-clauseso
             prog
             reflected-total
             reflected-bytes
             (dec remaining)
             after-current
             atom
             env
             guarded-alternatives))]))))

(defn- sjas-system-reflected-call-clauseo
  "Resolve a proof-predicate procedure call from encoded reflected clauses.

   Unlike the generic compiled-program Procedure Call Rule, this relation does
   not consult a compiled clause list. It decodes the active finite SJAS system
   code, skips the beta block, and searches the reflected Group-2b clause
   records as object-level data."
  [prog system-code atom env body negated-body]
  (fresh [system-bytes profile-tag beta-count beta-bytes
          after-betas reflected-count reflected-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (reflected-call-in-clauseso prog
                                                      reflected-total
                                                      reflected-bytes
                                                      atom
                                                      env
                                                      body
                                                      negated-body)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- sjas-system-reflected-call-alternativeso
  "Resolve all same-relation reflected call alternatives from system-code."
  [prog system-code atom env negated-alternatives]
  (fresh [system-bytes profile-tag beta-count beta-bytes
          after-betas reflected-count reflected-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (reflected-call-alternatives-in-clauseso
                            prog
                            reflected-total
                            reflected-bytes
                            atom
                            env
                            negated-alternatives)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- sjas-system-reflected-guarded-call-alternativeso
  "Resolve guarded reflected call alternatives from encoded system data."
  [prog system-code atom env guarded-alternatives]
  (fresh [system-bytes profile-tag beta-count beta-bytes
          after-betas reflected-count reflected-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (reflected-call-guarded-alternatives-in-clauseso
                            prog
                            reflected-total
                            reflected-bytes
                            reflected-total
                            reflected-bytes
                            atom
                            env
                            guarded-alternatives)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- reflected-member-in-clauseso
  "Search the encoded reflected-clause section for a formula-equivalent axiom."
  [_prog remaining bytes formula proof]
  (if (zero? remaining)
    fail
    (fresh [current after-current]
      (decode-reflected-clause-syntax-formulao bytes after-current current)
      (conde
        [(sjas-alpha-formula-equivo current formula '())
         (== '(sjas-system-reflected-axiom) proof)]
        [(reflected-member-in-clauseso _prog
                                       (dec remaining)
                                       after-current
                                       formula
                                       proof)]))))

(defn- sjas-system-reflected-formulao
  "Relate a system-code byte string to one of its reflected Group-2b axioms.

   This is the Group-2b analogue of beta membership: it reads the reflected
   block from the encoded finite system source instead of asking whether a
   generated `axiom-member/2` host fact exists. Callers now obtain
   `system-bytes` through the same object-language public-code relation used by
   syntax and theorem-code reads."
  [prog system-bytes formula proof]
  (fresh [profile-tag beta-count beta-bytes after-betas reflected-count reflected-bytes]
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (reflected-member-in-clauseso prog
                                                        reflected-total
                                                        reflected-bytes
                                                        formula
                                                        proof)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- reflected-axiom-formula-starto
  "Cheaply reject byte strings that cannot encode a reflected clause axiom.

   A reflected clause with parameters is wrapped in one or more `forall`
   formulas; a nullary reflected clause starts with the implication from body
   to head. This guard prevents negative proof-predicate tests from trying to
   parse non-formula codes, such as a whole `system-code`, as reflected axiom
   formulas."
  [formula-bytes]
  (fresh [tag rest]
    (== (lcons tag rest) formula-bytes)
    (conde
      [(== formula-forall-tag tag)]
      [(== formula-implies-tag tag)])))

(defn- sjas-reflected-axiom-membero
  "Check reflected Group-2b axiom membership from encoded system data.

   Both system and candidate formula codes are still read by the public
   object-level byte relation. Their proof payloads are summarized here because
   reflected axiom citation is a large semantic path: the relevant evidence is
   the reconstructed reflected-clause formula and alpha comparison, while
   reifying every byte-read step can dominate the proof search runtime."
  [prog system-code formula-code proof]
  (fresh [system-bytes formula-bytes system-read-proof formula-read-proof
          decoded-formula reflected-proof]
    (sjas-public-code-byteso system-code system-bytes system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (reflected-axiom-formula-starto formula-bytes)
    (decode-syntax-formula-byteso formula-bytes '() decoded-formula)
    (sjas-system-reflected-formulao prog system-bytes decoded-formula reflected-proof)
    (== (list 'sjas-system-reflected-axiom
              system-read-proof
              formula-read-proof
              reflected-proof)
        proof)))

(declare sjas-proof-antecedent-formula-asto sjas-negated-formula-asto)

(defn- sjas-proof-antecedent-quantifier-asto
  [internal-tag ast-tag formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [body body-ast]
             (== (list internal-tag idx body) formula)
             (== (list ast-tag (nominal/tie nom body-ast)) ast-formula)
             (sjas-proof-antecedent-formula-asto body body-ast)))
         code-nom-entries)))

(defn- sjas-negated-quantifier-asto
  [internal-tag ast-tag formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [body body-ast]
             (== (list internal-tag idx body) formula)
             (== (list ast-tag (nominal/tie nom body-ast)) ast-formula)
             (sjas-negated-formula-asto body body-ast)))
         code-nom-entries)))

(defn- leq-guard-asto
  [polarity nom bound-ast formula]
  (== (list polarity
            (list 'app 'leq (list 'var nom) bound-ast))
      formula))

(defn- sjas-proof-antecedent-bounded-quantifier-asto
  [internal-tag ast-tag connective guard-polarity formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [bound body bound-ast body-ast guard combined]
             (== (list internal-tag idx bound body) formula)
             (sjas-internal-term-asto bound bound-ast)
             (leq-guard-asto guard-polarity nom bound-ast guard)
             (sjas-proof-antecedent-formula-asto body body-ast)
             (== (list connective guard body-ast) combined)
             (== (list ast-tag
                       (nominal/tie nom {:bound bound-ast
                                         :body combined}))
                 ast-formula)))
         code-nom-entries)))

(defn- sjas-negated-bounded-quantifier-asto
  [internal-tag ast-tag connective guard-polarity formula ast-formula]
  (or*
    (map (fn [[idx nom]]
           (fresh [bound body bound-ast body-ast guard combined]
             (== (list internal-tag idx bound body) formula)
             (sjas-internal-term-asto bound bound-ast)
             (leq-guard-asto guard-polarity nom bound-ast guard)
             (sjas-negated-formula-asto body body-ast)
             (== (list connective guard body-ast) combined)
             (== (list ast-tag
                       (nominal/tie nom {:bound bound-ast
                                         :body combined}))
                 ast-formula)))
         code-nom-entries)))

(defn- sjas-proof-antecedent-formula-asto
  "Relate an encoded axiom formula to its double-negated proof antecedent AST.

   The source theorem query proves `(axioms -> theorem)`. The tableau branch
   checked by `tableau-proof/3` contains the double negation of the axiom basis,
   not the surface axiom basis itself. This relation mirrors
   `normalize/negate-formula` twice, but it runs as kernel relation over decoded
   formula-code structure rather than recovering the antecedent from host
   registry metadata."
  [formula ast-formula]
  (conde
    [(== (list 'true) formula)
     (== (list 'true) ast-formula)]
    [(== (list 'false) formula)
     (== (list 'false) ast-formula)]
    [(fresh [term ast-term]
       (== (list 'pos term) formula)
       (== (list 'pos ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [term ast-term]
       (== (list 'neg term) formula)
       (== (list 'neg ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [left right left-ast right-ast]
       (== (list 'eq left right) formula)
       (== (list 'eq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'neq left right) formula)
       (== (list 'neq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'and left right) formula)
       (== (list 'and left-ast right-ast) ast-formula)
       (sjas-proof-antecedent-formula-asto left left-ast)
       (sjas-proof-antecedent-formula-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'or left right) formula)
       (== (list 'or left-ast right-ast) ast-formula)
       (sjas-proof-antecedent-formula-asto left left-ast)
       (sjas-proof-antecedent-formula-asto right right-ast))]
    [(fresh [body body-ast]
       (== (list 'not body) formula)
       (sjas-negated-formula-asto body body-ast)
       (== body-ast ast-formula))]
    [(fresh [left right left-ast right-ast]
       (== (list 'implies left right) formula)
       (== (list 'or left-ast right-ast) ast-formula)
       (sjas-negated-formula-asto left left-ast)
       (sjas-proof-antecedent-formula-asto right right-ast))]
    [(sjas-proof-antecedent-quantifier-asto 'forall 'once-forall formula ast-formula)]
    [(sjas-proof-antecedent-quantifier-asto 'once-forall 'once-forall formula ast-formula)]
    [(sjas-proof-antecedent-quantifier-asto 'exists 'exists formula ast-formula)]
    [(sjas-proof-antecedent-bounded-quantifier-asto 'bounded-forall
                                                    'once-forall
                                                    'or
                                                    'neg
                                                    formula
                                                    ast-formula)]
    [(sjas-proof-antecedent-bounded-quantifier-asto 'bounded-exists
                                                    'exists
                                                    'and
                                                    'pos
                                                    formula
                                                    ast-formula)]))

(defn- sjas-negated-formula-asto
  "Relate an encoded formula to its NNF negation as an AST formula."
  [formula ast-formula]
  (conde
    [(== (list 'true) formula)
     (== (list 'false) ast-formula)]
    [(== (list 'false) formula)
     (== (list 'true) ast-formula)]
    [(fresh [term ast-term]
       (== (list 'pos term) formula)
       (== (list 'neg ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [term ast-term]
       (== (list 'neg term) formula)
       (== (list 'pos ast-term) ast-formula)
       (sjas-internal-term-asto term ast-term))]
    [(fresh [left right left-ast right-ast]
       (== (list 'eq left right) formula)
       (== (list 'neq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'neq left right) formula)
       (== (list 'eq left-ast right-ast) ast-formula)
       (sjas-internal-term-asto left left-ast)
       (sjas-internal-term-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'and left right) formula)
       (== (list 'or left-ast right-ast) ast-formula)
       (sjas-negated-formula-asto left left-ast)
       (sjas-negated-formula-asto right right-ast))]
    [(fresh [left right left-ast right-ast]
       (== (list 'or left right) formula)
       (== (list 'and left-ast right-ast) ast-formula)
       (sjas-negated-formula-asto left left-ast)
       (sjas-negated-formula-asto right right-ast))]
    [(fresh [body body-ast]
       (== (list 'not body) formula)
       (sjas-proof-antecedent-formula-asto body body-ast)
       (== body-ast ast-formula))]
    [(fresh [left right left-ast right-ast]
       (== (list 'implies left right) formula)
       (== (list 'and left-ast right-ast) ast-formula)
       (sjas-proof-antecedent-formula-asto left left-ast)
       (sjas-negated-formula-asto right right-ast))]
    [(sjas-negated-quantifier-asto 'forall 'exists formula ast-formula)]
    [(sjas-negated-quantifier-asto 'once-forall 'exists formula ast-formula)]
    [(sjas-negated-quantifier-asto 'exists 'once-forall formula ast-formula)]
    [(sjas-negated-bounded-quantifier-asto 'bounded-forall
                                           'exists
                                           'and
                                           'pos
                                           formula
                                           ast-formula)]
    [(sjas-negated-bounded-quantifier-asto 'bounded-exists
                                           'once-forall
                                           'or
                                           'neg
                                           formula
                                           ast-formula)]))

(defn- formula-list-appendo
  [left right out]
  (conde
    [(== '() left)
     (== right out)]
    [(fresh [head tail appended-tail]
       (== (lcons head tail) left)
       (== (lcons head appended-tail) out)
       (formula-list-appendo tail right appended-tail))]))

(declare formula-list-and-resto)

(defn- formula-list-ando
  [formulas formula]
  (conde
    [(== '() formulas)
     (== (list 'true) formula)]
    [(fresh [head tail]
       (== (lcons head tail) formulas)
       (formula-list-and-resto head tail formula))]))

(defn- formula-list-and-resto
  [acc formulas formula]
  (conde
    [(== '() formulas)
     (== acc formula)]
    [(fresh [head tail next]
       (== (lcons head tail) formulas)
       (== (list 'and acc head) next)
       (formula-list-and-resto next tail formula))]))

(defn- decode-proof-antecedent-formulaso
  [prog remaining bytes rest formulas]
  (if (zero? remaining)
    (fresh []
      (== bytes rest)
      (== '() formulas))
    (fresh [decoded ast-formula after-formula tail-formulas]
      (decode-proof-formula-byteso prog bytes after-formula decoded)
      (sjas-proof-antecedent-formula-asto decoded ast-formula)
      (decode-proof-antecedent-formulaso prog
                                         (dec remaining)
                                         after-formula
                                         rest
                                         tail-formulas)
      (== (lcons ast-formula tail-formulas) formulas))))

(defn- decode-reflected-proof-antecedent-formulaso
  [prog remaining bytes rest formulas]
  (if (zero? remaining)
    (fresh []
      (== bytes rest)
      (== '() formulas))
    (fresh [decoded ast-formula after-record tail-formulas]
      (decode-reflected-clause-formulao prog bytes after-record decoded)
      (sjas-proof-antecedent-formula-asto decoded ast-formula)
      (decode-reflected-proof-antecedent-formulaso prog
                                                   (dec remaining)
                                                   after-record
                                                   rest
                                                   tail-formulas)
      (== (lcons ast-formula tail-formulas) formulas))))

(defn- sjas-proof-antecedent-formula-listo
  "Convert a finite fixed internal axiom list to proof antecedent formulas.

   Group-0 and Group-1 are fixed finite schemata for every selected SJAS
   profile. The relation still exposes each formula through
   `sjas-proof-antecedent-formula-asto`; the host sequence only supplies the
   finite fixed list being internalized."
  [internal-formulas formulas]
  (if (empty? internal-formulas)
    (== '() formulas)
    (fresh [head tail]
      (sjas-proof-antecedent-formula-asto (first internal-formulas) head)
      (sjas-proof-antecedent-formula-listo (rest internal-formulas) tail)
      (== (lcons head tail) formulas))))

(defn- sjas-fixed-proof-antecedent-formulaso
  "Reconstruct the fixed Group-0 and Group-1 antecedents for `AxiomConj(s)`."
  [formulas]
  (fresh [group-zero-formulas group-one-formulas]
    (sjas-proof-antecedent-formula-listo group-zero-internal-formulas
                                         group-zero-formulas)
    (sjas-proof-antecedent-formula-listo group-one-internal-formulas
                                         group-one-formulas)
    (formula-list-appendo group-zero-formulas group-one-formulas formulas)))

(defn- sjas-system-group-three-proof-antecedento
  ([prog profile-tag system-bytes formula]
   (fresh [system-kind]
     (conde
       [(== :compact system-kind)]
       [(== :u-grounding system-kind)])
     (sjas-system-group-three-proof-antecedento prog
                                                profile-tag
                                                system-kind
                                                system-bytes
                                                formula)))
  ([prog profile-tag system-kind system-bytes formula]
   (conde
     [(fresh [decoded group-proof]
        (== system-profile-tableau0-tag profile-tag)
        (tableau0-group-three-formula-for-kindo system-kind
                                                system-bytes
                                                decoded
                                                group-proof)
        (sjas-proof-antecedent-formula-asto decoded formula))]
     [(fresh [decoded group-proof]
        (sjas-level1-family-profile-tago profile-tag)
        (level1-group-three-formula-for-kindo prog
                                              system-kind
                                              system-bytes
                                              decoded
                                              group-proof)
        (sjas-proof-antecedent-formula-asto decoded formula))]
     [(fresh [decoded group-proof]
        (== system-profile-tab2-boundary-tag profile-tag)
        (tab2-boundary-group-three-formula-for-kindo system-kind
                                                     system-bytes
                                                     decoded
                                                     group-proof)
        (sjas-proof-antecedent-formula-asto decoded formula))])))

(defn- sjas-system-proof-axiom-formulao
  ([prog system-bytes axiom-formula]
   (fresh [system-kind]
     (conde
       [(== :compact system-kind)]
       [(== :u-grounding system-kind)])
     (sjas-system-proof-axiom-formulao prog
                                       system-kind
                                       system-bytes
                                       axiom-formula)))
  ([prog system-kind system-bytes axiom-formula]
   (fresh [profile-tag beta-count beta-bytes beta-formulas after-betas
           reflected-count reflected-bytes reflected-formulas reflected-rest
           fixed-formulas fixed-and-beta beta-and-reflected all-but-group3
           group-three-formula all-formulas]
     (== (lcons system-code-tag
                 (lcons profile-tag
                        (lcons beta-count beta-bytes)))
         system-bytes)
     (sjas-system-profile-tago profile-tag)
     (or*
       (map (fn [beta-total]
              (fresh []
                (== (inc beta-total) beta-count)
                (decode-proof-antecedent-formulaso prog
                                                   beta-total
                                                   beta-bytes
                                                   after-betas
                                                   beta-formulas)
                (== (lcons reflected-count reflected-bytes) after-betas)
                (or*
                  (map (fn [reflected-total]
                         (fresh []
                           (== (inc reflected-total) reflected-count)
                           (decode-reflected-proof-antecedent-formulaso
                             prog
                             reflected-total
                             reflected-bytes
                             reflected-rest
                             reflected-formulas)
                           (== '() reflected-rest)
                           (sjas-fixed-proof-antecedent-formulaso fixed-formulas)
                           (formula-list-appendo fixed-formulas
                                                 beta-formulas
                                                 fixed-and-beta)
                           (formula-list-appendo fixed-and-beta
                                                 reflected-formulas
                                                 beta-and-reflected)
                           (sjas-system-group-three-proof-antecedento
                             prog
                             profile-tag
                             system-kind
                             system-bytes
                             group-three-formula)
                           (== (list group-three-formula) all-but-group3)
                           (formula-list-appendo beta-and-reflected
                                                 all-but-group3
                                                 all-formulas)
                           (formula-list-ando all-formulas axiom-formula)))
                       (range sjas-code/byte-base)))))
            (range sjas-code/byte-base))))))

(defn- sjas-system-axiom-formulao
  [prog system-code axiom-formula]
  (fresh [system-bytes system-kind read-proof]
    (sjas-public-code-byteso system-code system-bytes system-kind read-proof)
    (sjas-system-proof-axiom-formulao prog
                                      system-kind
                                      system-bytes
                                      axiom-formula)))

(defn- sjas-axiom-membero
  [prog system-code formula-code proof]
  (conde
    [(sjas-fixed-axiom-membero prog system-code formula-code proof)]
    [(sjas-beta-axiom-membero prog system-code formula-code proof)]
    [(sjas-reflected-axiom-membero prog system-code formula-code proof)]
    [(sjas-tableau0-group-three-axiom-membero prog system-code formula-code proof)]
    [(sjas-level1-group-three-axiom-membero prog system-code formula-code proof)]
    [(sjas-tab2-boundary-group-three-axiom-membero prog
                                                  system-code
                                                  formula-code
                                                  proof)]))

(defn- sjas-walked-axiom-membero
  "Check axiom membership after normalizing code terms through equality sigma.

   Relational `tableau-proof` and `subst-prf` calls may reach this point with
   `system-code` and `formula-code` bound in `sigma` rather than as immediately
   ground host values. The structural axiom decoders still consume code terms;
   this helper only performs the same equality walk that ordinary predicate
   dispatch uses before handing those terms to the decoders."
  [prog system-code formula-code sigma proof]
  (conde
    [(== '() sigma)
     (sjas-axiom-membero prog system-code formula-code proof)]
    [(fresh [walked-system-code walked-formula-code]
       (!= '() sigma)
       (equality/walk*o system-code sigma walked-system-code)
       (equality/walk*o formula-code sigma walked-formula-code)
       (sjas-axiom-membero prog walked-system-code walked-formula-code proof))]))

(defn- sjas-system-code-header-coreo
  "Proof-free system-code header recognizer."
  [system-bytes]
  (fresh [profile-tag rest]
    (== (lcons system-code-tag (lcons profile-tag rest)) system-bytes)
    (sjas-system-profile-tago profile-tag)))

(defn- fixed-axiom-formula-coreo
  "Proof-free fixed axiom formula recognizer."
  [formulas formula]
  (or*
    (map (fn [expected]
           (fresh []
             (sjas-alpha-formula-equivo expected formula '())))
         formulas)))

(defn- sjas-fixed-axiom-formula-coreo
  "Proof-free fixed Group-0/Group-1 axiom recognizer."
  [formula]
  (conde
    [(fixed-axiom-formula-coreo group-zero-internal-formulas formula)]
    [(fixed-axiom-formula-coreo group-one-internal-formulas formula)]))

(defn- sjas-fixed-axiom-formula-bytes-coreo
  "Proof-free fixed Group-0/Group-1 recognizer over formula-code bytes."
  [formula-bytes]
  (or*
    (map (fn [expected-bytes]
           (fresh []
             (sjas-acyclic-unifyo expected-bytes formula-bytes)))
         fixed-axiom-formula-byte-entries)))

(defn- sjas-fixed-axiom-formula-compact-code-coreo
  "Proof-free fixed Group-0/Group-1 recognizer over compact public code terms."
  [formula-code]
  (static-table-entryo formula-code fixed-axiom-formula-compact-code-entries))

(defn- sjas-fixed-axiom-member-coreo
  "Proof-free fixed axiom membership from decoded system and formula codes."
  [_prog system-code formula-code]
  (fresh [system-bytes formula-bytes]
    (conde
      [(sjas-fixed-axiom-formula-compact-code-coreo formula-code)]
      [(sjas-ug-code-bytes-coreo formula-code formula-bytes '() '())
       (sjas-fixed-axiom-formula-bytes-coreo formula-bytes)])
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-system-code-header-coreo system-bytes)))

(defn- sjas-tableau0-system-code-header-coreo
  "Proof-free Tableau-0 system header recognizer."
  [system-bytes]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-tableau0-tag rest))
        system-bytes)))

(defn- tableau0-group-three-formula-coreo
  "Proof-free Tableau-0 Group-3 axiom reconstruction."
  [system-bytes formula]
  (fresh [system-term contradiction-term]
    (tableau0-group-three-code-termso system-bytes
                                      system-term
                                      contradiction-term)
    (== (list 'forall
              1
              (list 'neg
                    (list 'app
                          'dsjas-tableau-proof
                          (list system-term
                                contradiction-term
                                (list 'var 1)))))
        formula)))

(defn- sjas-tableau0-group-three-axiom-member-coreo
  "Proof-free Tableau-0 Group-3 axiom membership."
  [prog system-code formula-code]
  (fresh [system-bytes system-kind formula-bytes formula group-three-proof]
    (sjas-public-code-bytes-coreo system-code system-bytes system-kind)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-tableau0-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tableau0-group-three-formula-for-kindo system-kind
                                            system-bytes
                                            formula
                                            group-three-proof)))

(defn- sjas-level1-system-code-header-coreo
  "Proof-free Level-1-family system header recognizer."
  [system-bytes]
  (fresh [profile-tag rest]
    (== (lcons system-code-tag
                (lcons profile-tag rest))
        system-bytes)
    (sjas-level1-family-profile-tago profile-tag)))

(defn- level1-group-three-formula-coreo
  "Proof-free Level-1 Group-3 axiom reconstruction."
  [prog system-bytes formula]
  (fresh [system-term substitution-term skeleton-bytes skeleton-formula]
    (system-code-internal-termo system-bytes system-term)
    (== (level1-selfcons-internal-formula system-term
                                          substitution-term
                                          1
                                          2
                                          3
                                          4)
        formula)
    (internal-code-term-byteso substitution-term skeleton-bytes)
    (decode-formula-byteso prog skeleton-bytes '() skeleton-formula)
    (== (level1-selfcons-internal-formula system-term
                                          (list 'var 1)
                                          2
                                          3
                                          4
                                          5)
        skeleton-formula)))

(defn- sjas-level1-group-three-axiom-member-coreo
  "Proof-free Level-1 Group-3 axiom membership."
  [prog system-code formula-code]
  (fresh [system-bytes system-kind formula-bytes formula group-three-proof]
    (sjas-public-code-bytes-coreo system-code system-bytes system-kind)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-level1-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (level1-group-three-formula-for-kindo prog
                                          system-kind
                                          system-bytes
                                          formula
                                          group-three-proof)))

(defn- sjas-tab2-boundary-system-code-header-coreo
  "Proof-free target-only Tab-2 boundary system header recognizer."
  [system-bytes]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-tab2-boundary-tag rest))
        system-bytes)))

(defn- tab2-boundary-group-three-formula-coreo
  "Proof-free target-only Tab-2 boundary Group-3 axiom reconstruction."
  [system-bytes formula]
  (fresh [system-term]
    (system-code-internal-termo system-bytes system-term)
    (== (tab2-boundary-selfcons-internal-formula system-term 1 2 3 4 5)
        formula)))

(defn- sjas-tab2-boundary-group-three-axiom-member-coreo
  "Proof-free target-only Tab-2 boundary Group-3 axiom membership."
  [prog system-code formula-code]
  (fresh [system-bytes system-kind formula-bytes formula group-three-proof]
    (sjas-public-code-bytes-coreo system-code system-bytes system-kind)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (formula-bytes-forall-rooto formula-bytes)
    (sjas-tab2-boundary-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tab2-boundary-group-three-formula-for-kindo system-kind
                                                 system-bytes
                                                 formula
                                                 group-three-proof)))

(defn- byte-list-neqo
  "Object-level disequality for finite byte lists."
  [left right]
  (conde
    [(== '() left)
     (fresh [head tail]
       (== (lcons head tail) right))]
    [(fresh [head tail]
       (== (lcons head tail) left)
       (== '() right))]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (byte-neqo left-head right-head))]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (== left-head right-head)
       (byte-list-neqo left-tail right-tail))]))

(defn- sjas-beta-member-in-formula-bytes-coreo
  "Proof-free beta block membership over encoded formula bytes."
  [_prog remaining bytes formula-bytes]
  (if (zero? remaining)
    fail
    (fresh [after-current]
      (conde
        ;; See the proof-producing companion above. The direct prefix branch is
        ;; the common ground-code path for finite beta citations.
        [(byte-prefixo formula-bytes bytes after-current)]
        [(fresh [current-bytes]
           (skip-syntax-formula-byteso bytes after-current)
           (byte-prefixo current-bytes bytes after-current)
           (byte-list-neqo formula-bytes current-bytes)
           (sjas-beta-member-in-formula-bytes-coreo _prog
                                                    (dec remaining)
                                                    after-current
                                                    formula-bytes))]))))

(defn- sjas-system-beta-formula-bytes-coreo
  "Proof-free beta axiom membership over a decoded system byte string."
  [prog system-bytes formula-bytes]
  (fresh [profile-tag beta-count beta-bytes]
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (sjas-beta-member-in-formula-bytes-coreo prog
                                                        beta-total
                                                        beta-bytes
                                                        formula-bytes)))
           (range sjas-code/byte-base)))))

(defn- sjas-beta-axiom-member-coreo
  "Proof-free beta axiom membership from public system/formula codes."
  [prog system-code formula-code]
  (fresh [system-bytes formula-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (sjas-system-beta-formula-bytes-coreo prog system-bytes formula-bytes)))

(defn- reflected-member-in-clauses-coreo
  "Proof-free reflected-clause axiom membership."
  [_prog remaining bytes formula]
  (if (zero? remaining)
    fail
    (fresh [current after-current]
      (decode-reflected-clause-syntax-formulao bytes after-current current)
      (conde
        [(sjas-alpha-formula-equivo current formula '())]
        [(reflected-member-in-clauses-coreo _prog
                                            (dec remaining)
                                            after-current
                                            formula)]))))

(defn- sjas-system-reflected-formula-coreo
  "Proof-free Group-2b reflected axiom membership over system bytes."
  [prog system-bytes formula]
  (fresh [profile-tag beta-count beta-bytes after-betas reflected-count reflected-bytes]
    (== (lcons system-code-tag
                (lcons profile-tag
                       (lcons beta-count beta-bytes)))
        system-bytes)
    (sjas-system-profile-tago profile-tag)
    (or*
      (map (fn [beta-total]
             (fresh []
               (== (inc beta-total) beta-count)
               (skip-formula-byteso prog beta-total beta-bytes after-betas)
               (== (lcons reflected-count reflected-bytes) after-betas)
               (or*
                 (map (fn [reflected-total]
                        (fresh []
                          (== (inc reflected-total) reflected-count)
                          (reflected-member-in-clauses-coreo prog
                                                             reflected-total
                                                             reflected-bytes
                                                             formula)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- sjas-reflected-axiom-member-coreo
  "Proof-free reflected Group-2b axiom membership."
  [prog system-code formula-code]
  (fresh [system-bytes formula-bytes decoded-formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (reflected-axiom-formula-starto formula-bytes)
    (decode-syntax-formula-byteso formula-bytes '() decoded-formula)
    (sjas-system-reflected-formula-coreo prog system-bytes decoded-formula)))

(defn- sjas-axiom-member-coreo
  "Proof-free finite-system axiom membership relation."
  [prog system-code formula-code]
  (conde
    [(sjas-fixed-axiom-member-coreo prog system-code formula-code)]
    [(sjas-beta-axiom-member-coreo prog system-code formula-code)]
    [(sjas-reflected-axiom-member-coreo prog system-code formula-code)]
    [(sjas-tableau0-group-three-axiom-member-coreo prog system-code formula-code)]
    [(sjas-level1-group-three-axiom-member-coreo prog system-code formula-code)]
    [(sjas-tab2-boundary-group-three-axiom-member-coreo prog
                                                       system-code
                                                       formula-code)]))

(defn- sjas-walked-axiom-member-coreo
  "Proof-free axiom membership after equality walking."
  [prog system-code formula-code sigma]
  (conde
    [(== '() sigma)
     (sjas-axiom-member-coreo prog system-code formula-code)]
    [(fresh [walked-system-code walked-formula-code]
       (!= '() sigma)
       (equality/walk*o system-code sigma walked-system-code)
       (equality/walk*o formula-code sigma walked-formula-code)
       (sjas-axiom-member-coreo prog walked-system-code walked-formula-code))]))

(defn- tableau0-group-three-proof-antecedent-coreo
  "Proof-free Tableau-0 Group-3 antecedent reconstruction.

   This is the direct proof-side AST form of the fixed Group-3 schema. It still
   obtains the embedded public code terms from `system-bytes` and rebuilds those
   public terms through the same compact-code and U-Grounding relations used by
   the generic term converter; it only avoids routing the known schema through
   the fully generic antecedent-formula walker.
   "
  [system-bytes formula]
  (let [binder-nom (sjas-code/code-nom 1)]
    (fresh [system-ast contradiction-ast]
      (conde
        [(fresh []
           (sjas-internal-code-termo system-bytes system-ast)
           (sjas-internal-code-termo tableau0-contradiction-formula-bytes
                                     contradiction-ast))]
        [(fresh [encoded-system system-bits contradiction-bits
                 system-proof contradiction-proof]
           (append-sentinel-byteo system-bytes encoded-system)
           (byte-list-bitso encoded-system system-bits)
           (bits->canonical-termo system-bits system-ast system-proof)
           (byte-list-bitso tableau0-contradiction-u-grounding-bytes
                            contradiction-bits)
           (bits->canonical-termo contradiction-bits
                                  contradiction-ast
                                  contradiction-proof))])
      (== (list 'once-forall
                (nominal/tie binder-nom
                             (list 'neg
                                   (list 'app
                                         'dsjas-tableau-proof
                                         system-ast
                                         contradiction-ast
                                         (list 'var binder-nom)))))
          formula))))

(defn- tableau0-group-three-proof-antecedent-for-code-coreo
  "Tableau-0 Group-3 antecedent reconstruction selected by public `system-code`.

   `system-bytes` are still read relationally from `system-code` by the caller.
   Keeping the public term here prevents the fixed Group-3 schema from exploring
   both compact and U-Grounding embeddings when the presented code already
   determines which object representation is correct.
   "
  [system-code system-bytes formula]
  (let [binder-nom (sjas-code/code-nom 1)]
    (fresh [system-ast contradiction-ast]
      (conde
        [(fresh []
           (sjas-internal-code-termo system-bytes system-ast)
           (== system-code system-ast)
           (sjas-internal-code-termo tableau0-contradiction-formula-bytes
                                     contradiction-ast))]
        [(fresh [encoded-system system-bits contradiction-bits
                 system-proof contradiction-proof]
           (append-sentinel-byteo system-bytes encoded-system)
           (byte-list-bitso encoded-system system-bits)
           (bits->canonical-termo system-bits system-ast system-proof)
           (== system-code system-ast)
           (byte-list-bitso tableau0-contradiction-u-grounding-bytes
                            contradiction-bits)
           (bits->canonical-termo contradiction-bits
                                  contradiction-ast
                                  contradiction-proof))])
      (== (list 'once-forall
                (nominal/tie binder-nom
                             (list 'neg
                                   (list 'app
                                         'dsjas-tableau-proof
                                         system-ast
                                         contradiction-ast
                                         (list 'var binder-nom)))))
          formula))))

(defn- sjas-system-group-three-proof-antecedent-coreo
  "Proof-free Group-3 antecedent reconstruction for `AxiomConj`."
  [prog profile-tag system-bytes formula]
  (conde
    [(== system-profile-tableau0-tag profile-tag)
     (tableau0-group-three-proof-antecedent-coreo system-bytes formula)]
    [(fresh [decoded]
       (sjas-level1-family-profile-tago profile-tag)
       (level1-group-three-formula-coreo prog system-bytes decoded)
       (sjas-proof-antecedent-formula-asto decoded formula))]
    [(fresh [decoded]
       (== system-profile-tab2-boundary-tag profile-tag)
       (tab2-boundary-group-three-formula-coreo system-bytes decoded)
       (sjas-proof-antecedent-formula-asto decoded formula))]))

(defn- sjas-system-group-three-proof-antecedent-for-code-coreo
  "Proof-free Group-3 antecedent reconstruction using the presented system code."
  [prog profile-tag system-code system-bytes formula]
  (conde
    [(== system-profile-tableau0-tag profile-tag)
     (tableau0-group-three-proof-antecedent-for-code-coreo system-code
                                                           system-bytes
                                                           formula)]
    [(fresh [decoded system-kind group-proof]
       (sjas-level1-family-profile-tago profile-tag)
       (sjas-public-code-bytes-coreo system-code system-bytes system-kind)
       (level1-group-three-formula-for-kindo prog
                                             system-kind
                                             system-bytes
                                             decoded
                                             group-proof)
       (sjas-proof-antecedent-formula-asto decoded formula))]
    [(fresh [decoded system-kind group-proof]
       (== system-profile-tab2-boundary-tag profile-tag)
       (sjas-public-code-bytes-coreo system-code system-bytes system-kind)
       (tab2-boundary-group-three-formula-for-kindo system-kind
                                                    system-bytes
                                                    decoded
                                                    group-proof)
       (sjas-proof-antecedent-formula-asto decoded formula))]))

(defn- sjas-system-proof-axiom-formula-coreo
  "Proof-free reconstruction of the finite axiom conjunction used by
   `tableau-proof/3` and `subst-prf/4`."
  ([prog system-bytes axiom-formula]
   (fresh [profile-tag beta-count beta-bytes beta-formulas after-betas
           reflected-count reflected-bytes reflected-formulas reflected-rest
           fixed-formulas fixed-and-beta beta-and-reflected all-but-group3
           group-three-formula all-formulas]
     (== (lcons system-code-tag
                 (lcons profile-tag
                        (lcons beta-count beta-bytes)))
         system-bytes)
     (sjas-system-profile-tago profile-tag)
     (or*
       (map (fn [beta-total]
              (fresh []
                (== (inc beta-total) beta-count)
                (decode-proof-antecedent-formulaso prog
                                                   beta-total
                                                   beta-bytes
                                                   after-betas
                                                   beta-formulas)
                (== (lcons reflected-count reflected-bytes) after-betas)
                (or*
                  (map (fn [reflected-total]
                         (fresh []
                           (== (inc reflected-total) reflected-count)
                           (decode-reflected-proof-antecedent-formulaso
                             prog
                             reflected-total
                             reflected-bytes
                             reflected-rest
                             reflected-formulas)
                           (== '() reflected-rest)
                           (sjas-fixed-proof-antecedent-formulaso fixed-formulas)
                           (formula-list-appendo fixed-formulas
                                                 beta-formulas
                                                 fixed-and-beta)
                           (formula-list-appendo fixed-and-beta
                                                 reflected-formulas
                                                 beta-and-reflected)
                           (sjas-system-group-three-proof-antecedent-coreo
                             prog
                             profile-tag
                             system-bytes
                             group-three-formula)
                           (== (list group-three-formula) all-but-group3)
                           (formula-list-appendo beta-and-reflected
                                                 all-but-group3
                                                 all-formulas)
                           (formula-list-ando all-formulas axiom-formula)))
                       (range sjas-code/byte-base)))))
            (range sjas-code/byte-base)))))
  ([prog system-code system-bytes axiom-formula]
   (fresh [profile-tag beta-count beta-bytes beta-formulas after-betas
           reflected-count reflected-bytes reflected-formulas reflected-rest
           fixed-formulas fixed-and-beta beta-and-reflected all-but-group3
           group-three-formula all-formulas]
     (== (lcons system-code-tag
                 (lcons profile-tag
                        (lcons beta-count beta-bytes)))
         system-bytes)
     (sjas-system-profile-tago profile-tag)
     (or*
       (map (fn [beta-total]
              (fresh []
                (== (inc beta-total) beta-count)
                (decode-proof-antecedent-formulaso prog
                                                   beta-total
                                                   beta-bytes
                                                   after-betas
                                                   beta-formulas)
                (== (lcons reflected-count reflected-bytes) after-betas)
                (or*
                  (map (fn [reflected-total]
                         (fresh []
                           (== (inc reflected-total) reflected-count)
                           (decode-reflected-proof-antecedent-formulaso
                             prog
                             reflected-total
                             reflected-bytes
                             reflected-rest
                             reflected-formulas)
                           (== '() reflected-rest)
                           (sjas-fixed-proof-antecedent-formulaso fixed-formulas)
                           (formula-list-appendo fixed-formulas
                                                 beta-formulas
                                                 fixed-and-beta)
                           (formula-list-appendo fixed-and-beta
                                                 reflected-formulas
                                                 beta-and-reflected)
                           (sjas-system-group-three-proof-antecedent-for-code-coreo
                             prog
                             profile-tag
                             system-code
                             system-bytes
                             group-three-formula)
                           (== (list group-three-formula) all-but-group3)
                           (formula-list-appendo beta-and-reflected
                                                 all-but-group3
                                                 all-formulas)
                           (formula-list-ando all-formulas axiom-formula)))
                       (range sjas-code/byte-base)))))
            (range sjas-code/byte-base))))))

(defn- sjas-system-code-valid-coreo
  "Proof-free recognizer for a complete finite SJAS system code.

   Some proof-predicate branches, such as the `Subst(g,t)` axiom of
   `subst-prf/4`, do not otherwise need the reconstructed axiom conjunction.
   They still must reject an invalid `system-code`, so this relation parses
  the complete finite system record without exposing auxiliary proof evidence."
  [prog system-code]
  (fresh [system-bytes axiom-formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog
                                          system-code
                                          system-bytes
                                          axiom-formula)))

(defn- sjas-system-axiom-formula-coreo
  "Proof-free public-code entry for axiom-conjunction reconstruction."
  [prog system-code axiom-formula]
  (fresh [system-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog
                                          system-code
                                          system-bytes
                                          axiom-formula)))

(defn- sjas-system-code-bytes-walked-coreo
  "Read `system-code` through equality state and expose both bytes and walked term."
  [system-code sigma sigma-out walked-system-code system-bytes]
  (fresh [kind]
    (sjas-formal-code-bytes-coreo system-code
                                  system-bytes
                                  sigma
                                  sigma-out
                                  kind)
    (equality/walk*o system-code sigma-out walked-system-code)))

(defn- sjas-system-code-valid-walked-coreo
  "Proof-free system-code validator for proof predicates with branch equality state."
  [prog system-code sigma sigma-out walked-system-code]
  (fresh [system-bytes axiom-formula]
    (sjas-system-code-bytes-walked-coreo system-code
                                         sigma
                                                 sigma-out
                                                 walked-system-code
                                                 system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog
                                          walked-system-code
                                          system-bytes
                                          axiom-formula)))

(defn- sjas-system-axiom-formula-walked-coreo
  "Proof-free `AxiomConj(system-code)` reconstruction through equality state."
  [prog system-code sigma sigma-out walked-system-code axiom-formula]
  (fresh [system-bytes]
    (sjas-system-code-bytes-walked-coreo system-code
                                         sigma
                                         sigma-out
                                         walked-system-code
                                         system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog
                                          walked-system-code
                                          system-bytes
                                          axiom-formula)))

(defn- sjas-substitution-formula-codeo
  "Decode a substitution-side formula code.

   Substitution is structural: it preserves application-head identity, but it
   does not need the semantic host symbol denoted by a finite codebook index.
   Using the syntax decoder keeps user relation/function heads as `(sym n)` and
   therefore lets `subst-code` run without the generated source registry."
  [_prog code sigma sigma-out formula]
  (fresh [read-proof]
    (sjas-decode-syntax-formula-code-proofo code sigma sigma-out formula read-proof)))

(defn- sjas-substitution-formula-code-coreo
  "Proof-free substitution-side formula-code decoder."
  [_prog code sigma sigma-out formula]
  (sjas-decode-syntax-formula-code-coreo code sigma sigma-out formula))

(defn- sjas-subst-code-anyo
  "Relate formula codes by structural diagonal substitution.

   The first code is decoded as a formula `F`; `F` is then used as a quoted code
   term and substituted for free canonical variable `v0` inside `F`. The second
   code must decode to a formula alpha-equivalent to the resulting formula. This
   is the object-language `Subst` operation needed by Level-1 SJAS
   self-reference; it is no longer a finite table of precomputed examples."
  [prog source-code substituted-code sigma sigma-out]
  (fresh [source-bytes source-formula substituted-formula replacement
          source-kind source-read-proof sigma-after-source]
    (sjas-formal-code-byteso source-code
                             source-bytes
                             sigma
                             sigma-after-source
                             source-kind
                             source-read-proof)
    (decode-syntax-formula-byteso source-bytes '() source-formula)
    (conde
      [(== :compact source-kind)
       (== (list 'code source-bytes) replacement)]
      [(== :u-grounding source-kind)
       (fresh [encoded-source-bytes]
         (append-sentinel-byteo source-bytes encoded-source-bytes)
         (== (list 'num encoded-source-bytes) replacement))])
    (sjas-substitution-formula-codeo prog substituted-code
                                     sigma-after-source
                                     sigma-out
                                     substituted-formula)
    (sjas-subst-alpha-formula-equivo source-formula
                                     replacement
                                     substituted-formula
                                     '())))

(defn- sjas-subst-code-any-coreo
  "Proof-free companion for structural diagonal substitution."
  [prog source-code substituted-code sigma sigma-out]
  (fresh [source-bytes source-formula substituted-formula replacement
          source-kind sigma-after-source]
    (sjas-formal-code-bytes-coreo source-code
                                  source-bytes
                                  sigma
                                  sigma-after-source
                                  source-kind)
    (decode-syntax-formula-byteso source-bytes '() source-formula)
    (conde
      [(== :compact source-kind)
       (== (list 'code source-bytes) replacement)]
      [(== :u-grounding source-kind)
       (fresh [encoded-source-bytes]
         (append-sentinel-byteo source-bytes encoded-source-bytes)
         (== (list 'num encoded-source-bytes) replacement))])
    (sjas-substitution-formula-code-coreo prog
                                          substituted-code
                                          sigma-after-source
                                          sigma-out
                                          substituted-formula)
    (sjas-subst-alpha-formula-equivo source-formula
                                     replacement
                                     substituted-formula
                                     '())))

(defn- sjas-subst-source-result-antecedento
  "Compute the substituted source sentence used by `SubstPrf`.

   Willard's `SubstPrf(g,t,p)` factors through an existential sentence `h` such
   that `Subst(g,h)` and `p` proves `t` from beta plus `h`. This relation
   decodes `g`, computes the diagonal substitution result as internal formula
   syntax, and converts that result to the proof-antecedent AST used by the
   local tableau checker. It deliberately does not synthesize a public code term
   for `h`; it still computes the object-level substituted sentence that the
   proof predicate must add to its axiom basis."
  [_prog source-code sigma sigma-out antecedent proof]
  (fresh [source-bytes source-formula substituted-formula replacement
          source-kind source-read-proof]
    (sjas-formal-code-byteso source-code
                             source-bytes
                             sigma
                             sigma-out
                             source-kind
                             source-read-proof)
    (decode-syntax-formula-byteso source-bytes '() source-formula)
    (conde
      [(== :compact source-kind)
       (== (list 'code source-bytes) replacement)]
      [(== :u-grounding source-kind)
       (fresh [encoded-source-bytes]
         (append-sentinel-byteo source-bytes encoded-source-bytes)
         (== (list 'num encoded-source-bytes) replacement))])
    (sjas-subst-formula-var-oneo source-formula replacement substituted-formula)
    (sjas-proof-antecedent-formula-asto substituted-formula antecedent)
    (== (list 'willard-sjas-subst-source-result source-read-proof) proof)))

(defn- sjas-subst-source-result-antecedent-coreo
  "Proof-free substituted-source antecedent relation for `SubstPrf`."
  [_prog source-code sigma sigma-out antecedent]
  (fresh [source-bytes source-formula substituted-formula replacement
          source-kind]
    (sjas-formal-code-bytes-coreo source-code
                                  source-bytes
                                  sigma
                                  sigma-out
                                  source-kind)
    (decode-syntax-formula-byteso source-bytes '() source-formula)
    (conde
      [(== :compact source-kind)
       (== (list 'code source-bytes) replacement)]
      [(== :u-grounding source-kind)
       (fresh [encoded-source-bytes]
         (append-sentinel-byteo source-bytes encoded-source-bytes)
         (== (list 'num encoded-source-bytes) replacement))])
    (sjas-subst-formula-var-oneo source-formula replacement substituted-formula)
    (sjas-proof-antecedent-formula-asto substituted-formula antecedent)))

(defn- sjas-class-relationo
  "Recognize the finite formula-class predicates generated for one SJAS system.

   This cheap relation guard keeps ordinary arithmetic predicates from touching
   the generated coding registry. The registry can contain very large
   arithmetized numerals, so predicate discrimination must happen before any
   metadata enumeration."
  [relation]
  (conde
    [(== 'delta-star-0-code relation)]
    [(== 'pi-star-1-code relation)]
    [(== 'sigma-star-1-code relation)]))

(def ^:private syntax-code-relations
  '#{wff delta-star-0-code pi-star-1-code sigma-star-1-code neg-pair})

(defn- syntax-code-relation?
  [relation]
  (contains? syntax-code-relations relation))

;; -----------------------------------------------------------------------------
;; Branch closing rules
;; -----------------------------------------------------------------------------

(defn- sjas-neq-close-coreo
  [fml env sigma sigma-out neqs neqs-out eq-proof]
  (fresh [lit left right]
    (sjas-subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (sjas-normal-equalo left right sigma sigma-out eq-proof)
    (== neqs neqs-out)))

(defn- sjas-neq-close-structural-coreo
  [fml env sigma sigma-out neqs neqs-out]
  (fresh [lit left right]
    (sjas-subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (sjas-normal-equal-coreo left right sigma sigma-out)
    (== neqs neqs-out)))

(defn- sjas-neq-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [eq-proof]
    (sjas-neq-close-coreo fml env sigma sigma-out neqs neqs-out eq-proof)
    (== (list 'profiled 'willard-sjas-arithmetic eq-proof) proof)))

(defn- sjas-neg-relation-close-coreo
  [fml env sigma sigma-out neqs neqs-out relation-proof]
  (fresh [lit atom walked-atom relation args]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-holdso relation args sigma sigma-out relation-proof)
    (== neqs neqs-out)))

(defn- sjas-neg-relation-close-structural-coreo
  [fml env sigma sigma-out neqs neqs-out]
  (fresh [lit atom walked-atom relation args]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-holds-coreo relation args sigma sigma-out)
    (== neqs neqs-out)))

(defn- sjas-pos-relation-close-coreo
  [fml env sigma sigma-out neqs neqs-out relation-proof]
  (fresh [lit atom walked-atom relation args]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'pos atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-failso relation args sigma sigma-out relation-proof)
    (== neqs neqs-out)))

(defn- sjas-pos-relation-close-structural-coreo
  [fml env sigma sigma-out neqs neqs-out]
  (fresh [lit atom walked-atom relation args]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'pos atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-fails-coreo relation args sigma sigma-out)
    (== neqs neqs-out)))

(defn- sjas-neg-relation-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [relation-proof]
    (sjas-neg-relation-close-coreo fml env sigma sigma-out neqs neqs-out relation-proof)
    (== (list 'profiled 'willard-sjas-arithmetic relation-proof) proof)))

(defn- sjas-pos-relation-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [relation-proof]
    (sjas-pos-relation-close-coreo fml env sigma sigma-out neqs neqs-out relation-proof)
    (== (list 'profiled 'willard-sjas-arithmetic relation-proof) proof)))

(defn- sjas-axiom-member-walked-closeo
  "General axiom-member close path for branch-local environments and sigmas."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom relation args system-code formula-code axiom-proof]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (== 'axiom-member relation)
    (== (lcons system-code (lcons formula-code '())) args)
    (sjas-walked-axiom-membero prog system-code formula-code sigma axiom-proof)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-axiom-member axiom-proof) proof)))

(defn- sjas-axiom-member-closeo
  "Close `axiom-member(system, formula)` from decoded system-code membership.

  Earlier ADR-006x stages closed this predicate by consulting generated
  `axiom-member/2` facts. ADR-0072 requires the predicate path itself to use
  the same structural axiom membership used by `sjas-axiom` proof certificates,
  so injected or stale generated facts cannot become semantic evidence."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (sjas-axiom-member-walked-closeo fml env sigma sigma-out neqs neqs-out prog proof))

(defn- sjas-axiom-member-structural-closeo
  "Close a structural tableau leaf through decoded `axiom-member/2`.

   Formula-bearing tableau proofs do not encode the ordinary Proflog answer
   marker for this closure. They only require the object relation that reads
   the system and formula codes and preserves the branch state."
  [fml env sigma sigma-out neqs neqs-out prog]
  (fresh [lit atom walked-atom relation args system-code formula-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (== 'axiom-member relation)
    (== (lcons system-code (lcons formula-code '())) args)
    (sjas-walked-axiom-member-coreo prog system-code formula-code sigma)
    (== sigma sigma-out)
    (== neqs neqs-out)))

(defn- sjas-eq-progresso
  "Consume a true arithmetic equality and continue with the pending branch.

   Without this rule, the generic free-constructor equality layer treats
   arithmetic function symbols as uninterpreted constructors. That is sound for
   ordinary Proflog programs but wrong for the SJAS U-grounding profile, where
   `sub(2,1)` and `1` denote the same number despite having different root
   constructors."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (fresh [lit left right eq-proof next rest next-fuel sigma-mid subproof]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'eq left right) lit)
    (sjas-normal-equalo left right sigma sigma-mid eq-proof)
    (== (lcons next rest) unexpanded)
    (support/step-fuelo fuel next-fuel)
    (kernel/prove-stateo next
                         rest
                         lits
                         env
                         proof-vars
                         sigma-mid
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         gamma-terms
                         next-fuel
                         subproof)
    (== (list 'profiled 'willard-sjas-arithmetic
              (list 'sjas-eq-progress eq-proof subproof))
        proof)))

(defn- sjas-wff-code-closeo
  "Close `wff(code)` by reading the object-level formula code.

   Earlier SJAS stages generated a finite host lookup table for formulas known
   when the system was compiled. ADR-0072 removes that shortcut: the predicate
   succeeds only by decoding the supplied code term through the relational byte
   reader and formula grammar used for non-generated formulas."
  [_prog code sigma sigma-out formula branch-proof]
  (fresh []
    ;; `prog` is intentionally ignored in this syntax slice. Application heads
    ;; remain numeric symbol ids, because `wff` needs shape and arity, not the
    ;; source-time host names attached to those ids.
    (sjas-decode-syntax-formula-code-proofo code sigma sigma-out formula branch-proof)))

(defn- sjas-class-code-closeo
  "Close a formula-class predicate by decoding and classifying the formula AST."
  [_prog relation code sigma sigma-out formula branch-proof]
  (fresh []
    (sjas-decode-syntax-formula-code-proofo code sigma sigma-out formula branch-proof)
    (sjas-structural-formula-classo relation formula)))

(defn- sjas-neg-pair-code-closeo
  "Close `neg-pair(left,right)` by decoding both codes and complementing ASTs."
  [_prog left right sigma sigma-out formula complement branch-proof]
  (fresh [sigma-mid left-proof right-proof]
    (sjas-decode-syntax-formula-code-proofo left sigma sigma-mid formula left-proof)
    (sjas-decode-syntax-formula-code-proofo right sigma-mid sigma-out complement right-proof)
    (sjas-formula-complemento formula complement)
    (== (list 'sjas-neg-pair-structural left-proof right-proof) branch-proof)))

(defn- sjas-syntax-code-brancho
  [prog relation args sigma sigma-out branch-proof]
  (fresh [code left right formula complement]
    (conde
      [(== 'wff relation)
       (sjas-acyclic-unifyo (lcons code '()) args)
       (sjas-wff-code-closeo prog code sigma sigma-out formula branch-proof)]
      [(sjas-class-relationo relation)
       (sjas-acyclic-unifyo (lcons code '()) args)
       (sjas-class-code-closeo prog relation code sigma sigma-out formula branch-proof)]
      [(== 'neg-pair relation)
       (sjas-acyclic-unifyo (lcons left (lcons right '())) args)
       (sjas-neg-pair-code-closeo prog left right sigma sigma-out formula complement branch-proof)])))

(defn- sjas-syntax-code-closeo
  "Close generated syntax-code predicates by decoding formula Godel-code terms.

   These predicates are no longer emitted as generated facts and no longer use
   host-generated formula lookup tables. The branch closes by reading the
  formula-code term into the kernel's internal formula syntax and then applying
  the structural recognizer for well-formedness, class membership, or
  complement pairs. When a U-Grounding code arrives through a logic binding,
  the branch proof preserves the byte-cons evidence from the relational
  decoder."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom relation args branch-proof]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
    (sjas-syntax-code-brancho prog
                              relation
                              args
                              sigma
                              sigma-out
                              branch-proof)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-code (list relation branch-proof)) proof)))

(defn- sjas-subst-code-closeo
  "Close structural `subst-code/2` goals.

   ADR-0066 separates Willard's `Subst(g,h)` relation from `SubstPrf(g,t,p)`.
   ADR-0069 computes the substitution itself by decoding formula-code bytes,
  replacing the distinguished free variable, and comparing the decoded target
  modulo bound-variable alpha-renaming."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom source-code substituted-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (list 'app 'subst-code source-code substituted-code)
                         walked-atom)
    (sjas-subst-code-any-coreo prog source-code substituted-code sigma sigma-out)
    (== neqs neqs-out)
    (== '(profiled willard-sjas-subst-code) proof)))

(declare sjas-proof-check-stateo
         sjas-proof-guided-selecto
         sjas-tableau-proof-structural-closeo
         sjas-subst-prf-structural-closeo
         sjas-dsjas-tableau-proof-structural-closeo
         sjas-dsjas-subst-prf-structural-closeo
         sjas-tab1-proof-structural-closeo
         sjas-dsjas-tab1-proof-structural-closeo
         sjas-dsjas-tab2-proof-structural-closeo
         sjas-finax4-structural-closeo)

(defn- proof-byte-prefixo
  [remaining input bytes rest]
  (if (zero? remaining)
    (fresh []
      (== '() bytes)
      (== input rest))
    (fresh [byte input-rest bytes-rest]
      (== (lcons byte input-rest) input)
      (== (lcons byte bytes-rest) bytes)
      (proof-byte-prefixo (dec remaining) input-rest bytes-rest rest))))

(defn- proof-byte-list-termo
  [proof bytes]
  (conde
    [(== '() proof)
     (== '() bytes)]
    [(fresh [byte proof-rest bytes-rest]
       (== (lcons byte proof-rest) proof)
       (== (lcons byte bytes-rest) bytes)
       (proof-byte-list-termo proof-rest bytes-rest))]))

(defn- formula-bearing-proof-node-with-byteso
  "Decode one formula-bearing node and expose its exact formula bytes."
  [prog proof formula-bytes formula children]
  (conde
    [(fresh [formula-byte-proof formula-byte-head formula-byte-tail
             decoded-formula]
       (== (lcons formula-byte-proof children) proof)
       (== (lcons formula-byte-head formula-byte-tail) formula-byte-proof)
       (== formula-byte-proof formula-bytes)
       (decode-proof-formula-byteso prog formula-bytes '() decoded-formula)
       (sjas-internal-formula-asto decoded-formula formula))]
    [(or*
       (map (fn [byte-count]
              (fresh [after-count decoded-formula]
                (== (lcons byte-count after-count) proof)
                (proof-byte-prefixo byte-count after-count formula-bytes children)
                (decode-proof-formula-byteso prog formula-bytes '() decoded-formula)
                (sjas-internal-formula-asto decoded-formula formula)))
            (range 1 sjas-code/byte-base)))]))

(defn- formula-bearing-proof-nodeo
  "Decode one formula-bearing tableau node from proof data.

   The node carries formula bytes and child nodes, not a trusted rule tag.
   Local deduction and closure rules are inferred from the decoded formula."
  [prog proof formula children]
  (fresh [formula-bytes]
    (formula-bearing-proof-node-with-byteso
      prog proof formula-bytes formula children)))

(declare sjas-ast-alpha-termo
         sjas-ast-alpha-term-listo
         sjas-ast-alpha-formulao)

(defn- ast-alpha-unmapped-nomo
  [nom env]
  (conde
    [(== '() env)]
    [(fresh [left right tail]
       (== (lcons [left right] tail) env)
       (!= nom left)
       (ast-alpha-unmapped-nomo nom tail))]))

(defn- ast-alpha-bound-nomo
  [left-nom right-nom env]
  (fresh [mapped-left mapped-right tail]
    (== (lcons [mapped-left mapped-right] tail) env)
    (conde
      [(== left-nom mapped-left)
       (== right-nom mapped-right)]
      [(!= left-nom mapped-left)
       (ast-alpha-bound-nomo left-nom right-nom tail)])))

(defn- sjas-ast-alpha-term-listo
  [left right env]
  (conde
    [(== '() left)
     (== '() right)]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (sjas-ast-alpha-termo left-head right-head env)
       (sjas-ast-alpha-term-listo left-tail right-tail env))]))

(defn- sjas-ast-alpha-termo
  "Compare AST terms modulo bound-variable alpha-renaming."
  [left right env]
  (conde
    [(fresh [head left-args right-args]
       (== (lcons 'app (lcons head left-args)) left)
       (== (lcons 'app (lcons head right-args)) right)
       (sjas-ast-alpha-term-listo left-args right-args env))]
    [(fresh [left-nom right-nom]
       (== (list 'var left-nom) left)
       (== (list 'var right-nom) right)
       (ast-alpha-bound-nomo left-nom right-nom env))]
    [(fresh [nom]
       (== (list 'var nom) left)
       (== (list 'var nom) right)
       (ast-alpha-unmapped-nomo nom env))]
    [(fresh [nom]
       (== (list 'par nom) left)
       (== (list 'par nom) right))]))

(defn- sjas-ast-alpha-formulao
  "Compare AST formulas modulo nominal binder alpha-renaming."
  [left right env]
  (conde
    [(== (list 'true) left)
     (== (list 'true) right)]
    [(== (list 'false) left)
     (== (list 'false) right)]
    [(fresh [left-term right-term]
       (== (list 'pos left-term) left)
       (== (list 'pos right-term) right)
       (sjas-ast-alpha-termo left-term right-term env))]
    [(fresh [left-term right-term]
       (== (list 'neg left-term) left)
       (== (list 'neg right-term) right)
       (sjas-ast-alpha-termo left-term right-term env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'eq left-a left-b) left)
       (== (list 'eq right-a right-b) right)
       (sjas-ast-alpha-termo left-a right-a env)
       (sjas-ast-alpha-termo left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'neq left-a left-b) left)
       (== (list 'neq right-a right-b) right)
       (sjas-ast-alpha-termo left-a right-a env)
       (sjas-ast-alpha-termo left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'and left-a left-b) left)
       (== (list 'and right-a right-b) right)
       (sjas-ast-alpha-formulao left-a right-a env)
       (sjas-ast-alpha-formulao left-b right-b env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'or left-a left-b) left)
       (== (list 'or right-a right-b) right)
       (sjas-ast-alpha-formulao left-a right-a env)
       (sjas-ast-alpha-formulao left-b right-b env))]
    [(fresh [body-left body-right]
       (== (list 'not body-left) left)
       (== (list 'not body-right) right)
       (sjas-ast-alpha-formulao body-left body-right env))]
    [(fresh [left-a left-b right-a right-b]
       (== (list 'implies left-a left-b) left)
       (== (list 'implies right-a right-b) right)
       (sjas-ast-alpha-formulao left-a right-a env)
       (sjas-ast-alpha-formulao left-b right-b env))]
    [(fresh [left-nom right-nom left-body right-body]
       (== (list 'forall (nominal/tie left-nom left-body)) left)
       (== (list 'forall (nominal/tie right-nom right-body)) right)
       (sjas-ast-alpha-formulao left-body
                                 right-body
                                 (lcons [left-nom right-nom] env)))]
    [(fresh [left-nom right-nom left-body right-body]
       (== (list 'once-forall (nominal/tie left-nom left-body)) left)
       (== (list 'once-forall (nominal/tie right-nom right-body)) right)
       (sjas-ast-alpha-formulao left-body
                                 right-body
                                 (lcons [left-nom right-nom] env)))]
    [(fresh [left-nom right-nom left-body right-body]
       (== (list 'exists (nominal/tie left-nom left-body)) left)
       (== (list 'exists (nominal/tie right-nom right-body)) right)
       (sjas-ast-alpha-formulao left-body
                                 right-body
                                 (lcons [left-nom right-nom] env)))]
    [(fresh [left-nom right-nom left-bound left-body right-bound right-body]
       (== (list 'bounded-forall
                 (nominal/tie left-nom {:bound left-bound :body left-body}))
           left)
       (== (list 'bounded-forall
                 (nominal/tie right-nom {:bound right-bound :body right-body}))
           right)
       (sjas-ast-alpha-termo left-bound right-bound env)
       (sjas-ast-alpha-formulao left-body
                                 right-body
                                 (lcons [left-nom right-nom] env)))]
    [(fresh [left-nom right-nom left-bound left-body right-bound right-body]
       (== (list 'bounded-exists
                 (nominal/tie left-nom {:bound left-bound :body left-body}))
           left)
       (== (list 'bounded-exists
                 (nominal/tie right-nom {:bound right-bound :body right-body}))
           right)
       (sjas-ast-alpha-termo left-bound right-bound env)
       (sjas-ast-alpha-formulao left-body
                                 right-body
                                 (lcons [left-nom right-nom] env)))]))

(defn- sjas-proof-node-binder-renaming-matcho
  "Match one binder layer by deterministically renaming the selected formula.
  Decoded formula-code binders use the shared `sjas-vN` noms, while source-side
  theorem and axiom formulas often retain their original host noms. A single
  substitution into the selected binder body avoids searching the full
  alpha-equivalence relation over large formula-code payloads."
  [visible-formula node-formula]
  (conde
    [(fresh [left-nom right-nom left-body right-body renamed-body]
       (== (list 'forall (nominal/tie left-nom left-body)) visible-formula)
       (== (list 'forall (nominal/tie right-nom right-body)) node-formula)
       (subst/subst-formulao left-body
                             (lcons [left-nom (ast/var-term right-nom)] '())
                             renamed-body)
       (== renamed-body right-body))]
    [(fresh [left-nom right-nom left-body right-body renamed-body]
       (== (list 'once-forall (nominal/tie left-nom left-body)) visible-formula)
       (== (list 'once-forall (nominal/tie right-nom right-body)) node-formula)
       (subst/subst-formulao left-body
                             (lcons [left-nom (ast/var-term right-nom)] '())
                             renamed-body)
       (== renamed-body right-body))]
    [(fresh [left-nom right-nom left-body right-body renamed-body]
       (== (list 'exists (nominal/tie left-nom left-body)) visible-formula)
       (== (list 'exists (nominal/tie right-nom right-body)) node-formula)
       (subst/subst-formulao left-body
                             (lcons [left-nom (ast/var-term right-nom)] '())
                             renamed-body)
       (== renamed-body right-body))]
    [(fresh [left-nom right-nom left-bound left-body right-bound right-body
             renamed-bound renamed-body]
       (== (list 'bounded-forall
                 (nominal/tie left-nom {:bound left-bound :body left-body}))
           visible-formula)
       (== (list 'bounded-forall
                 (nominal/tie right-nom {:bound right-bound :body right-body}))
           node-formula)
       (subst/subst-termo left-bound
                          (lcons [left-nom (ast/var-term right-nom)] '())
                          renamed-bound)
       (subst/subst-formulao left-body
                             (lcons [left-nom (ast/var-term right-nom)] '())
                             renamed-body)
       (== renamed-bound right-bound)
       (== renamed-body right-body))]
    [(fresh [left-nom right-nom left-bound left-body right-bound right-body
             renamed-bound renamed-body]
       (== (list 'bounded-exists
                 (nominal/tie left-nom {:bound left-bound :body left-body}))
           visible-formula)
       (== (list 'bounded-exists
                 (nominal/tie right-nom {:bound right-bound :body right-body}))
           node-formula)
       (subst/subst-termo left-bound
                          (lcons [left-nom (ast/var-term right-nom)] '())
                          renamed-bound)
       (subst/subst-formulao left-body
                             (lcons [left-nom (ast/var-term right-nom)] '())
                             renamed-body)
       (== renamed-bound right-bound)
       (== renamed-body right-body))]))

(declare sjas-proof-node-formula-matcho)

(defn- sjas-proof-node-compound-renaming-matcho
  "Match compound formulas whose subformulas may need binder renaming."
  [visible-formula node-formula]
  (conde
    [(fresh [visible-left visible-right node-left node-right]
       (== (list 'and visible-left visible-right) visible-formula)
       (== (list 'and node-left node-right) node-formula)
       (sjas-proof-node-formula-matcho visible-left node-left)
       (sjas-proof-node-formula-matcho visible-right node-right))]
    [(fresh [visible-left visible-right node-left node-right]
       (== (list 'or visible-left visible-right) visible-formula)
       (== (list 'or node-left node-right) node-formula)
       (sjas-proof-node-formula-matcho visible-left node-left)
       (sjas-proof-node-formula-matcho visible-right node-right))]
    [(fresh [visible-body node-body]
       (== (list 'not visible-body) visible-formula)
       (== (list 'not node-body) node-formula)
       (sjas-proof-node-formula-matcho visible-body node-body))]
    [(fresh [visible-left visible-right node-left node-right]
       (== (list 'implies visible-left visible-right) visible-formula)
       (== (list 'implies node-left node-right) node-formula)
       (sjas-proof-node-formula-matcho visible-left node-left)
       (sjas-proof-node-formula-matcho visible-right node-right))]))

(defn- sjas-proof-node-formula-matcho
  "Match a visible branch formula against the formula decoded from a proof node.
  Formula-bearing proof certificates usually contain the exact formula after the
  enclosing quantifier step has introduced canonical SJAS names. Match binder
  and compound roots before exact equality so sibling quantifier nodes do not
  pay for a deep failed exact scan when only the canonical binder name differs.
  Use ordinary backtracking choice because the proof-node formula is often still
  constrained by its byte decoder; committed choice can bind it to the wrong
  branch formula before the decoder has forced the actual node contents."
  [visible-formula node-formula]
  (conde
    [(sjas-proof-node-binder-renaming-matcho visible-formula node-formula)]
    [(sjas-proof-node-compound-renaming-matcho visible-formula node-formula)]
    [(sjas-acyclic-unifyo visible-formula node-formula)]
    [(sjas-ast-alpha-formulao visible-formula node-formula '())]))

(def ^:private total-multiplication-required-beta-bytes
  "Exact Type-M beta bytes shared by source generation and proof checking."
  (mapv (comp #(apply list %)
              boundary-axioms/total-multiplication-formula-code-bytes)
        (concat
          (boundary-axioms/total-multiplication-complete-axioms)
          [(boundary-axioms/total-multiplication-willard-v4-axiom)])))

(def ^:private total-multiplication-v5-beta-bytes
  "V5 differs only in the public representation of the contradiction code."
  (mapv
    (fn [contradiction-code]
      (apply list
             (boundary-axioms/total-multiplication-formula-code-bytes
               (boundary-axioms/total-multiplication-willard-v5-axiom
                 contradiction-code))))
    [(sjas-code/bytes->code-term tableau0-contradiction-formula-bytes)
     (sjas-code/bytes->u-grounding-code-term
       tableau0-contradiction-formula-bytes)]))

(def ^:private boundary-arithmetic-required-beta-bytes
  "Exact finite arithmetic basis required by Xtab and Tab-2 refutations."
  (mapv (comp #(apply list %)
              boundary-axioms/reserved-formula-code-bytes)
        (boundary-axioms/boundary-arithmetic-basis-axioms)))

(def ^:private tab2-rank2-proof-side-witness
  (boundary-axioms/proof-side-formula
    (boundary-axioms/tab2-rank2-witness-formula)))

(def ^:private xtab-lem-source-witness-bytes
  (apply list
         (boundary-axioms/xtab-formula-code-bytes
           (first (boundary-axioms/xtab-lem-witness-axioms)))))

(def ^:private tab2-rank2-source-witness-bytes
  (apply list
         (boundary-axioms/reserved-formula-code-bytes
           (boundary-axioms/tab2-rank2-witness-formula))))

(defn- sjas-all-beta-members-byteso
  "Goal: every ground formula-byte list in `formula-byte-lists` is a beta member
   of `system-bytes`. The recursion runs in Clojure over a compile-time constant
   list, conjoining one pure `sjas-system-beta-formula-byteso` goal per axiom."
  [prog system-bytes formula-byte-lists]
  (if (empty? formula-byte-lists)
    succeed
    (fresh [proof]
      (sjas-system-beta-formula-byteso prog
                                       system-bytes
                                       (first formula-byte-lists)
                                       proof)
      (sjas-all-beta-members-byteso prog
                                    system-bytes
                                    (next formula-byte-lists)))))

(defn- sjas-some-beta-member-byteso
  "Goal: at least one ground formula-byte list is a beta member of
   `system-bytes`. The disjunction is over a compile-time constant list."
  [prog system-bytes formula-byte-lists]
  (if (empty? formula-byte-lists)
    fail
    (conde
      [(fresh [proof]
         (sjas-system-beta-formula-byteso prog
                                          system-bytes
                                          (first formula-byte-lists)
                                          proof))]
      [(sjas-some-beta-member-byteso prog
                                     system-bytes
                                     (next formula-byte-lists))])))

(defn- sjas-boundary-source-hypotheses-coreo
  "Relationally check the encoded hypotheses that license a boundary system.

   Each required arithmetic/V-route axiom must occur as a beta formula of the
   presented (ground) system, decided by the pure object relation
   `sjas-system-beta-formula-byteso`. No host beta-set scan is used; the variant
   selects the exact required-axiom set by encoded `==` dispatch."
  [prog variant system-bytes witness-formula-bytes]
  (conde
    [(== 'total-multiplication-boundary variant)
     (sjas-all-beta-members-byteso prog
                                   system-bytes
                                   total-multiplication-required-beta-bytes)
     (sjas-some-beta-member-byteso prog
                                   system-bytes
                                   total-multiplication-v5-beta-bytes)
     (fresh [proof]
       (sjas-system-beta-formula-byteso prog
                                        system-bytes
                                        witness-formula-bytes
                                        proof))]
    [(== 'xtab-lem-boundary variant)
     (sjas-all-beta-members-byteso prog
                                   system-bytes
                                   boundary-arithmetic-required-beta-bytes)
     (fresh [proof]
       (sjas-system-beta-formula-byteso prog
                                        system-bytes
                                        xtab-lem-source-witness-bytes
                                        proof))]
    [(== 'tab2-boundary variant)
     (sjas-all-beta-members-byteso prog
                                   system-bytes
                                   boundary-arithmetic-required-beta-bytes)]))

(defn- sjas-total-multiplication-witnesso
  "Recognize a formula-bearing squaring-chain step `u' = mul(u,u)`."
  [formula]
  (fresh [next-symbol prior-symbol mul-symbol]
    (!= next-symbol prior-symbol)
    (== (list 'eq
              (list 'app next-symbol)
              (list 'app
                    mul-symbol
                    (list 'app prior-symbol)
                    (list 'app prior-symbol)))
        formula)))

(defn- sjas-xtab-lem-witnesso
  "Recognize a universally closed formula-independent LEM witness."
  [formula]
  (nominal/fresh [binding-nom]
    (fresh [left right]
      (== (list 'once-forall
                (nominal/tie binding-nom (list 'or left right)))
          formula)
      (conde
        [(sjas-formula-complemento left right)]
        [(sjas-formula-complemento right left)]))))

(defn- sjas-tab2-rank2-witnesso
  "Recognize the exact Rank-2 intermediate selected by ADR-0141."
  [formula]
  (sjas-proof-node-formula-matcho tab2-rank2-proof-side-witness formula))

(defn- sjas-boundary-profile-hypotheses-coreo
  "Check the exact encoded hypotheses that license one boundary metatheorem.

   A profile byte alone is insufficient. Every required arithmetic or V-route
   formula must occur in the reconstructed finite-system axiom conjunction."
  [prog variant system-code witness-formula witness-formula-bytes]
  (fresh [system-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-boundary-source-hypotheses-coreo
      prog
      variant
      system-bytes
      witness-formula-bytes)
    (conde
      [(== 'total-multiplication-boundary variant)
       (sjas-presented-system-profile-header-coreo
         system-code
         system-profile-total-multiplication-tag)
       (sjas-total-multiplication-witnesso witness-formula)]

      [(== 'xtab-lem-boundary variant)
       (sjas-presented-system-profile-header-coreo
         system-code
         system-profile-xtab-tag)
       (sjas-xtab-lem-witnesso witness-formula)]

      [(== 'tab2-boundary variant)
       (sjas-presented-system-profile-header-coreo
         system-code
         system-profile-tab2-boundary-tag)
       (sjas-tab2-rank2-witnesso witness-formula)])))

(defn- sjas-boundary-refutation-proof-bytes-coreo
  "Decode and validate a checked Workstream B metatheorem certificate.

   The constructor is deliberately outside the ordinary structural tableau
   grammar. It proves only the canonical contradiction, consumes one explicit
   formula-bearing reduced witness, and succeeds only when the exact encoded
   hypotheses for the selected unsafe profile occur in the presented system."
  [prog system-code theorem-code proof-bytes]
  (fresh [rest proof variant witness-proof witness-formula witness-children
          witness-formula-bytes theorem-bytes]
    (decode-proof-byteso proof-bytes rest proof)
    (== '() rest)
    (sjas-acyclic-unifyo
      (list 'willard-sjas-boundary-refutation
            variant
            witness-proof)
      proof)
    (sjas-public-code-bytes-coreo theorem-code theorem-bytes)
    (conde
      [(== tableau0-contradiction-formula-bytes theorem-bytes)]
      [(== reverse-contradiction-formula-bytes theorem-bytes)])
    (formula-bearing-proof-node-with-byteso
      prog
      witness-proof
      witness-formula-bytes
      witness-formula
      witness-children)
    (== '() witness-children)
    (sjas-boundary-profile-hypotheses-coreo
      prog
      variant
      system-code
      witness-formula
      witness-formula-bytes)))

(def ^:private boundary-refutation-proof-byte-prefix
  "Fixed encoded prefix shared by every `willard-sjas-boundary-refutation`
   proof list: the proof-list tag, the three-item count, and the wide-symbol
   code of the boundary constructor. The boundary refutation is the only proof
   whose encoding begins with this list-then-symbol pattern (`sjas-axiom` is a
   bare symbol and structural certificates are lists of byte nodes), so the
   prefix uniquely routes it. Computing it once as a constant keeps the routers
   a pure `==`/`!=` match against a fixed byte pattern, mirroring
   `sjas-axiom-proof-bytes`, instead of a host decode of the runtime term."
  (apply list
         (take 5 (sjas-code/proof-code-bytes
                   (list 'willard-sjas-boundary-refutation 0 '())))))

(defn- ground-byte-prefix-presento
  "Goal: `bytes` begins with the ground byte list `prefix`.

   `prefix` is a compile-time constant, so the recursion runs in Clojure to
   build a pure `lcons` chain `(p0 p1 ... . suffix)` unified against `bytes`."
  [prefix bytes]
  (if (empty? prefix)
    succeed
    (fresh [rest]
      (== (lcons (first prefix) rest) bytes)
      (ground-byte-prefix-presento (next prefix) rest))))

(defn- ground-byte-prefix-absento
  "Goal: `bytes` does not begin with the ground byte list `prefix`.

   Pure complement of `ground-byte-prefix-presento`: `bytes` is too short, or
   differs from `prefix` at some position. All comparisons are on byte values
   (numbers), where `==`/`!=` are safe."
  [prefix bytes]
  (if (empty? prefix)
    fail
    (let [p (first prefix)
          ps (next prefix)]
      (conde
        [(== '() bytes)]
        [(fresh [b rest]
           (== (lcons b rest) bytes)
           (!= p b))]
        [(fresh [rest]
           (== (lcons p rest) bytes)
           (ground-byte-prefix-absento ps rest))]))))

(defn- sjas-boundary-refutation-proof-rooto
  "Recognize the dedicated boundary constructor by its fixed encoded prefix."
  [proof-bytes]
  (ground-byte-prefix-presento boundary-refutation-proof-byte-prefix
                               proof-bytes))

(defn- sjas-non-boundary-refutation-proof-rooto
  "Exclude the dedicated boundary constructor from ordinary proof branches."
  [proof-bytes]
  (ground-byte-prefix-absento boundary-refutation-proof-byte-prefix
                              proof-bytes))

(declare sjas-atom-unify-coreo
         sjas-unify-termo-coreo
         sjas-unify-term*o-coreo
         sjas-eq-contradiction-coreo
         sjas-eq-contradiction-term*o-coreo)

(defn- sjas-complementary-atom-in-lit-listo
  [polarity atom lits sigma sigma-out]
  (fresh [head tail]
    (== (lcons head tail) lits)
    (conde
      [(fresh [opposite]
         (conde
           [(== 'pos polarity)
            (sjas-acyclic-unifyo (list 'neg opposite) head)]
           [(== 'neg polarity)
            (sjas-acyclic-unifyo (list 'pos opposite) head)])
         (sjas-atom-unify-coreo atom opposite sigma sigma-out))]
      [(sjas-complementary-atom-in-lit-listo
         polarity
         atom
         tail
         sigma
         sigma-out)])))

(defn- sjas-complementary-lit-close-coreo
  [lit lits sigma sigma-out]
  (conde
    [(fresh [atom]
       (sjas-acyclic-unifyo (list 'pos atom) lit)
       (sjas-complementary-atom-in-lit-listo
         'pos
         atom
         lits
         sigma
         sigma-out))]
    [(fresh [atom]
       (sjas-acyclic-unifyo (list 'neg atom) lit)
       (sjas-complementary-atom-in-lit-listo
         'neg
         atom
         lits
         sigma
         sigma-out))]))

(defn- sjas-unify-termo-coreo
  "Proof-free term unification for structural SJAS tableau checks.

   The ordinary equality helper returns kernel proof trace constructors such as
   `eq-bind` and `decompose`. Formula-bearing SJAS tableau nodes should not
   carry or require that trace payload, so this relation preserves the same
   branch-state effect while exposing only `sigma` and `sigma-out`."
  [left right sigma sigma-out]
  (fresh [left-root right-root]
    (sjas-walk-termo left sigma left-root)
    (sjas-walk-termo right sigma right-root)
    (conde
      [(equality/same-termo left-root right-root sigma)
       (== sigma sigma-out)]
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'var binding-nom) left-root)
         (equality/absent-termo binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'var binding-nom) right-root)
         (equality/absent-termo binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'par binding-nom) left-root)
         (equality/absent-paro binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'par binding-nom) right-root)
         (equality/absent-paro binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out))]
      [(fresh [head left-args right-args]
         (sjas-acyclic-unifyo (lcons 'app (lcons head left-args)) left-root)
         (sjas-acyclic-unifyo (lcons 'app (lcons head right-args)) right-root)
         (sjas-unify-term*o-coreo left-args right-args sigma sigma-out))])))

(defn- sjas-unify-term*o-coreo
  "Pairwise proof-free companion for `sjas-unify-termo-coreo`."
  [left right sigma sigma-out]
  (conde
    [(== '() left)
     (== '() right)
     (== sigma sigma-out)]
    [(fresh [left-head left-tail right-head right-tail sigma-mid]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (sjas-unify-termo-coreo left-head right-head sigma sigma-mid)
       (sjas-unify-term*o-coreo left-tail right-tail sigma-mid sigma-out))]))

(defn- sjas-atom-unify-coreo
  "Unify complementary atom arguments without producing proof trace evidence."
  [left right sigma sigma-out]
  (fresh [head left-args right-args]
    (sjas-acyclic-unifyo (lcons 'app (lcons head left-args)) left)
    (sjas-acyclic-unifyo (lcons 'app (lcons head right-args)) right)
    (sjas-unify-term*o-coreo left-args right-args sigma sigma-out)))

(defn- sjas-eq-contradiction-coreo
  "Succeed when an equality literal is impossible, without proof trace tags.

   This is the structural SJAS closure analogue of the kernel's
   `eq-contradictiono`: occurs failures, distinct constructor heads, argument
   arity mismatch, and recursive same-head contradictions close a branch. The
   relation intentionally returns no `free-close`, `occurs-close`, or
   `decompose` payload."
  [left right sigma]
  (fresh [left-root right-root]
    (sjas-walk-termo left sigma left-root)
    (sjas-walk-termo right sigma right-root)
    (conde
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'var binding-nom) left-root)
         (equality/occurs-termo binding-nom right-root sigma))]
      [(fresh [binding-nom]
         (sjas-acyclic-unifyo (list 'var binding-nom) right-root)
         (equality/occurs-termo binding-nom left-root sigma))]
      [(fresh [left-head left-args right-head right-args]
         (sjas-acyclic-unifyo (lcons 'app (lcons left-head left-args)) left-root)
         (sjas-acyclic-unifyo (lcons 'app (lcons right-head right-args)) right-root)
         (!= left-head right-head))]
      [(fresh [head left-args right-args]
         (sjas-acyclic-unifyo (lcons 'app (lcons head left-args)) left-root)
         (sjas-acyclic-unifyo (lcons 'app (lcons head right-args)) right-root)
         (sjas-eq-contradiction-term*o-coreo left-args right-args sigma))])))

(defn- sjas-eq-contradiction-term*o-coreo
  "Find a proof-free contradiction inside application argument lists."
  [left right sigma]
  (conde
    [(fresh [head tail]
       (== (lcons head tail) left)
       (== '() right))]
    [(fresh [head tail]
       (== '() left)
       (== (lcons head tail) right))]
    [(fresh [left-head left-tail right-head right-tail]
       (== (lcons left-head left-tail) left)
       (== (lcons right-head right-tail) right)
       (conde
         [(sjas-eq-contradiction-coreo left-head right-head sigma)]
         [(fresh [sigma-mid]
            (sjas-unify-termo-coreo left-head right-head sigma sigma-mid)
            (sjas-eq-contradiction-term*o-coreo left-tail right-tail sigma-mid))]))]))

(defn- sjas-neq-violated-coreo
  "Succeed when a stored disequality has become false under `sigma`."
  [neqs sigma]
  (fresh [left right rest]
    (conde
      [(== (lcons [left right] rest) neqs)
       (equality/same-termo left right sigma)]
      [(== (lcons [left right] rest) neqs)
       (sjas-neq-violated-coreo rest sigma)])))

(defn- sjas-contradictory-atoms-coreo
  "Succeed when saved positive and negative literals unify without proof trace."
  [lits sigma sigma-out]
  (fresh [left-atom right-atom]
    (conde
      [(membero (list 'pos left-atom) lits)
       (membero (list 'neg right-atom) lits)]
      [(membero (list 'neg left-atom) lits)
       (membero (list 'pos right-atom) lits)])
    (sjas-atom-unify-coreo left-atom right-atom sigma sigma-out)))

(defn- branch-env-lengtho
  "Succeed when the structural branch environment contains `expected` binders."
  [env expected]
  (if (zero? expected)
    (== '() env)
    (fresh [binding term tail]
      (== (lcons [binding term] tail) env)
      (branch-env-lengtho tail (dec expected)))))

(defn- sjas-next-branch-nomo
  "Select the canonical code nom for the next structural branch binder.

   Formula-bearing child nodes encode introduced variables and parameters as
   `v0`, `v1`, ... rather than as host nominal identities. The structural
   checker keys that canonical name to the branch environment depth so nested
   binders receive distinct payloads even when multiple existential parameters
   appear at the same proof-variable depth."
  [env next-nom]
  (letfn [(nexto [entries branch-env]
            (if (seq entries)
              (let [[_idx nom] (first entries)]
                (conde
                  [(== '() branch-env)
                   (== nom next-nom)]
                  [(fresh [head tail]
                     (== (lcons head tail) branch-env)
                     (nexto (rest entries) tail))]))
              fail))]
    (nexto code-nom-entries env)))

(defn- sjas-proof-tree-next-fuelo
  "Preserve runtime fuel while validating a fixed SJAS proof tree.

   The formula-bearing proof predicate is a relation over decoded system,
   theorem, and proof codes. Its recursion is driven by child proof nodes, so
   accepting a fixed certificate must not depend on an external evaluator fuel
   counter."
  [fuel next-fuel]
  (== fuel next-fuel))

(defn- sjas-agenda-entryo
  "Relate an agenda entry to its formula and saved branch environment.

   New structural proof-checker entries are `[formula env]` pairs so a sibling
   formula is later selected under the environment that was in scope when it was
   enqueued. Raw formulas are still accepted for older focused tests and for the
   initial selected formula.
   "
  [current-env entry formula entry-env]
  (conde
    [(== [formula entry-env] entry)]
    [(== formula entry)
     (== current-env entry-env)]))

(defn- sjas-agenda-cons-coreo
  "Cons `formula` onto an agenda with an explicit environment snapshot."
  [formula env rest out]
  (== (lcons [formula env] rest) out))

(defn- sjas-agenda-heado
  "Pop the next agenda formula with the environment saved for that formula."
  [current-env agenda formula formula-env rest]
  (fresh [entry]
    (== (lcons entry rest) agenda)
    (sjas-agenda-entryo current-env entry formula formula-env)))

(defn- sjas-structural-proof-check-state-decodedo
  "Validate the first formula-bearing tableau proof fragment.

   This relation is the Track 1 route away from Proflog proof-trace evidence:
   the proof object supplies formula nodes, and this checker infers the local
   tableau rule from parent formula, child formula, and branch state. The
   current fragment covers conjunction, disjunction, true-skip, false closure,
   double negation, implication, negated atomic/equality duals, negated
   conjunction/disjunction/implication, negated quantifier duals, literal
   continuation, complementary literal closure, and structural/bounded
   quantifier expansion,
   reflexive disequality closure, axiom-membership closure, recursive
   proof-predicate closure, arithmetic/profile closure, equality progression,
   and rigid disequality progression."
  [system-code fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel node-formula children]
  (fresh [visible-formula]
    (sjas-subst-formulao fml env visible-formula)
    (sjas-proof-node-formula-matcho visible-formula node-formula)
    (conde
      [(fresh [lit atom child child-formula child-children next next-env rest
               next-fuel]
         (== (lcons child '()) children)
         (sjas-acyclic-unifyo visible-formula lit)
         (conde
           [(sjas-acyclic-unifyo (list 'pos atom) lit)]
           [(sjas-acyclic-unifyo (list 'neg atom) lit)])
         (formula-bearing-proof-nodeo prog child child-formula child-children)
         (sjas-proof-guided-selecto child-formula
                                    env
                                    unexpanded
                                    next
                                    next-env
                                    rest)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-structural-proof-check-state-decodedo system-code
                                                     next
                                                     rest
                                                     (lcons lit lits)
                                                     next-env
                                                     proof-vars
                                                     sigma
                                                     sigma-out
                                                     neqs
                                                     neqs-out
                                                     prog
                                                     gamma-terms
                                                     next-fuel
                                                     child-formula
                                                     child-children))]
      [(fresh [lit atom]
         (== '() children)
         (sjas-acyclic-unifyo visible-formula lit)
         (conde
           [(sjas-acyclic-unifyo (list 'pos atom) lit)]
           [(sjas-acyclic-unifyo (list 'neg atom) lit)])
         (sjas-complementary-lit-close-coreo lit lits sigma sigma-out)
         (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
      [(fresh [left right child next-unexpanded next-fuel]
         (== (lcons child '()) children)
         (== (list 'and left right) fml)
         (sjas-agenda-cons-coreo right env unexpanded next-unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  left
                                  next-unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body child next-fuel]
           (== (lcons child '()) children)
           (== '() env)
           (conde
             [(== (list 'forall (nominal/tie binding-nom body)) fml)]
             [(== (list 'once-forall (nominal/tie binding-nom body)) fml)])
           (sjas-next-branch-nomo env free-var-nom)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body instantiated-body narrowed-env
                 child child-formula child-children next-fuel]
           (== (lcons child '()) children)
           (conde
             [(== (list 'forall (nominal/tie binding-nom body)) fml)]
             [(== (list 'once-forall (nominal/tie binding-nom body)) fml)])
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao
             body
             (lcons [binding-nom (ast/var-term free-var-nom)] narrowed-env)
             instantiated-body)
           (formula-bearing-proof-nodeo prog child child-formula child-children)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-structural-proof-check-state-decodedo system-code
                                                       instantiated-body
                                                       unexpanded
                                                       lits
                                                       env
                                                       (lcons free-var-nom proof-vars)
                                                       sigma
                                                       sigma-out
                                                       neqs
                                                       neqs-out
                                                       prog
                                                       gamma-terms
                                                       next-fuel
                                                       child-formula
                                                       child-children)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (conde
             [(== (list 'forall (nominal/tie binding-nom body)) fml)]
             [(== (list 'once-forall (nominal/tie binding-nom body)) fml)])
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body-subst
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [parameter-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body-subst
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                    proof-vars
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [parameter-nom body instantiated-body narrowed-env
                 child child-formula child-children next-fuel]
           (== (lcons child '()) children)
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao
             body
             (lcons [binding-nom (ast/par-term parameter-nom)] narrowed-env)
             instantiated-body)
           (formula-bearing-proof-nodeo prog child child-formula child-children)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-structural-proof-check-state-decodedo system-code
                                                       instantiated-body
                                                       unexpanded
                                                       lits
                                                       env
                                                       proof-vars
                                                       sigma
                                                       sigma-out
                                                       neqs
                                                       neqs-out
                                                       prog
                                                       gamma-terms
                                                       next-fuel
                                                       child-formula
                                                       child-children)))]
      [(fresh []
         (== '() children)
         (== (list 'false) fml)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh []
         (== '() children)
         (== (list 'not (list 'true)) fml)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [child next next-env rest next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'false)) fml)
         (sjas-agenda-heado env unexpanded next next-env rest)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  next-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [body child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'not body)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  body
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [atom child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'pos atom)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'neg atom)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [atom child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'neg atom)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'pos atom)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [left right child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'eq left right)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'neq left right)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [left right child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'neq left right)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'eq left right)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [left right left-child right-child next-fuel
               left-sigma-out right-sigma-out left-neqs-out right-neqs-out]
         (== (lcons left-child (lcons right-child '())) children)
         (== (list 'not (list 'and left right)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'not left)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  left-sigma-out
                                  neqs
                                  left-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  left-child)
         (sjas-proof-check-stateo system-code
                                  (list 'not right)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  right-sigma-out
                                  neqs
                                  right-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  right-child)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [left right child next-unexpanded next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'or left right)) fml)
         (sjas-agenda-cons-coreo (list 'not right)
                                 env
                                 unexpanded
                                 next-unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'not left)
                                  next-unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [left right left-child right-child next-fuel
               left-sigma-out right-sigma-out left-neqs-out right-neqs-out]
         (== (lcons left-child (lcons right-child '())) children)
         (== (list 'implies left right) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'not left)
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  left-sigma-out
                                  neqs
                                  left-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  left-child)
         (sjas-proof-check-stateo system-code
                                  right
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  right-sigma-out
                                  neqs
                                  right-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  right-child)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [left right child next-unexpanded next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'implies left right)) fml)
         (sjas-agenda-cons-coreo (list 'not right)
                                 env
                                 unexpanded
                                 next-unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  left
                                  next-unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(nominal/fresh [binding-nom]
         (fresh [quant parameter-nom body body-subst narrowed-env
                 child next-fuel]
           (== (lcons child '()) children)
           (conde
             [(== 'forall quant)]
             [(== 'once-forall quant)])
           (== (list 'not (list quant (nominal/tie binding-nom body))) fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    (list 'not body-subst)
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                    proof-vars
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (== (list 'not (list 'exists (nominal/tie binding-nom body))) fml)
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    (list 'not body-subst)
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [parameter-nom bound body bound-subst body-subst narrowed-env
                 guard guarded-body child next-fuel]
           (== (lcons child '()) children)
           (== (list 'not
                     (list 'bounded-forall
                           (nominal/tie binding-nom {:bound bound :body body})))
               fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-termo bound narrowed-env bound-subst)
           (subst/subst-formulao body narrowed-env body-subst)
           (== (list 'pos
                     (list 'app 'leq (list 'var binding-nom) bound-subst))
               guard)
           (== (list 'and guard (list 'not body-subst)) guarded-body)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    guarded-body
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                    proof-vars
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom bound body bound-subst body-subst narrowed-env
                 guard guarded-body child next-fuel]
           (== (lcons child '()) children)
           (== (list 'not
                     (list 'bounded-exists
                           (nominal/tie binding-nom {:bound bound :body body})))
               fml)
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-termo bound narrowed-env bound-subst)
           (subst/subst-formulao body narrowed-env body-subst)
           (== (list 'neg
                     (list 'app 'leq (list 'var binding-nom) bound-subst))
               guard)
           (== (list 'or guard (list 'not body-subst)) guarded-body)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    guarded-body
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(fresh [lit left right]
         (== '() children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'neq left right) lit)
         (equality/same-termo left right sigma)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [lit left right child next next-env rest next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'neq left right) lit)
         (support/rigid-different-termo left right sigma)
         (sjas-agenda-heado env unexpanded next next-env rest)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  next-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [lit left right child next next-env rest next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'neq left right) lit)
         (sjas-agenda-heado env unexpanded next next-env rest)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  next-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  (lcons [left right] neqs)
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh []
         (== '() children)
         (conde
           [(sjas-axiom-member-structural-closeo fml
                                                 env
                                                 sigma
                                                 sigma-out
                                                 neqs
                                                 neqs-out
                                                 prog)]
           [(sjas-tableau-proof-structural-closeo fml
                                                  env
                                                  sigma
                                                  sigma-out
                                                  neqs
                                                  neqs-out
                                                  prog
                                                  fuel)]
           [(sjas-subst-prf-structural-closeo fml
                                              env
                                              sigma
                                              sigma-out
                                              neqs
                                              neqs-out
                                              prog
                                              fuel)]
           [(sjas-tab1-proof-structural-closeo fml
                                               env
                                               sigma
                                               sigma-out
                                               neqs
                                               neqs-out
                                               prog
                                               fuel)]
           [(sjas-dsjas-tableau-proof-structural-closeo fml
                                                        env
                                                        sigma
                                                        sigma-out
                                                        neqs
                                                        neqs-out
                                                        prog
                                                        fuel)]
           [(sjas-dsjas-subst-prf-structural-closeo fml
                                                    env
                                                    sigma
                                                    sigma-out
                                                    neqs
                                                    neqs-out
                                                    prog
                                                    fuel)]
           [(sjas-dsjas-tab1-proof-structural-closeo fml
                                                     env
                                                     sigma
                                                     sigma-out
                                                     neqs
                                                     neqs-out
                                                     prog
                                                     fuel)]
           [(sjas-dsjas-tab2-proof-structural-closeo fml
                                                     env
                                                     sigma
                                                     sigma-out
                                                     neqs
                                                     neqs-out
                                                     prog
                                                     fuel)]
           [(sjas-finax4-structural-closeo fml
                                           env
                                           sigma
                                           sigma-out
                                           neqs
                                           neqs-out
                                           prog)]
           [(sjas-neq-close-structural-coreo fml env sigma sigma-out neqs neqs-out)]
           [(sjas-neg-relation-close-structural-coreo fml env sigma sigma-out neqs neqs-out)]
           [(sjas-pos-relation-close-structural-coreo fml env sigma sigma-out neqs neqs-out)]))]
      [(fresh [lit left right]
         (== '() children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-eq-contradiction-coreo left right sigma)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [lit left right sigma-mid]
         (== '() children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (sjas-neq-violated-coreo neqs sigma-mid)
         (== sigma-mid sigma-out)
         (support/prune-contradictory-neqso neqs sigma-mid neqs-out))]
      [(fresh [lit left right sigma-mid]
         (== '() children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (sjas-contradictory-atoms-coreo lits sigma-mid sigma-out)
         (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
      [(fresh [lit left right sigma-mid atom walked-atom relation args
               call-env body negated-body child next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (membero (list 'pos atom) lits)
         (sjas-walk-atomo atom sigma-mid walked-atom)
         (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (sjas-system-reflected-call-clauseo prog
                                             system-code
                                             walked-atom
                                             call-env
                                             body
                                             negated-body)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  body
                                  '()
                                  '()
                                  call-env
                                  proof-vars
                                  sigma-mid
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [lit left right sigma-mid atom walked-atom relation args
               call-env body negated-body child next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (membero (list 'neg atom) lits)
         (sjas-walk-atomo atom sigma-mid walked-atom)
         (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (sjas-system-reflected-call-clauseo prog
                                             system-code
                                             walked-atom
                                             call-env
                                             body
                                             negated-body)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  negated-body
                                  '()
                                  '()
                                  call-env
                                  proof-vars
                                  sigma-mid
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [lit left right sigma-mid child next next-env rest next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (sjas-agenda-heado env unexpanded next next-env rest)
         (support/stable-neqso neqs sigma-mid)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  next-env
                                  proof-vars
                                  sigma-mid
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [left right left-child right-child next-fuel
               left-sigma-out right-sigma-out left-neqs-out right-neqs-out]
         (== (lcons left-child (lcons right-child '())) children)
         (== (list 'or left right) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         ;; Structural tableau siblings share the incoming branch state. Any
         ;; equality or disequality evidence produced while closing one child
         ;; remains local to that child.
         (sjas-proof-check-stateo system-code
                                  left
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  left-sigma-out
                                  neqs
                                  left-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  left-child)
         (sjas-proof-check-stateo system-code
                                  right
                                  unexpanded
                                  lits
                                  env
                                  proof-vars
                                  sigma
                                  right-sigma-out
                                  neqs
                                  right-neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  right-child)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [lit atom walked-atom relation args call-env body negated-body
               child next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'pos atom) lit)
         (sjas-walk-atomo atom sigma walked-atom)
         (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (sjas-system-reflected-call-clauseo prog
                                             system-code
                                             walked-atom
                                             call-env
                                             body
                                             negated-body)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  body
                                  '()
                                  '()
                                  call-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(fresh [lit atom walked-atom relation args call-env body negated-body
               child next-fuel]
         (== (lcons child '()) children)
         (sjas-subst-formulao fml env lit)
         (sjas-acyclic-unifyo (list 'neg atom) lit)
         (sjas-walk-atomo atom sigma walked-atom)
         (sjas-acyclic-unifyo (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (sjas-system-reflected-call-clauseo prog
                                             system-code
                                             walked-atom
                                             call-env
                                             body
                                             negated-body)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  negated-body
                                  '()
                                  '()
                                  call-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body-subst
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (== (list 'once-forall (nominal/tie binding-nom body)) fml)
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body-subst
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [parameter-nom body body-subst narrowed-env child next-fuel]
           (== (lcons child '()) children)
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    body-subst
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                    proof-vars
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [free-var-nom bound body bound-subst body-subst narrowed-env
                 guard guarded-body child next-fuel]
           (== (lcons child '()) children)
           (== (list 'bounded-forall
                     (nominal/tie binding-nom {:bound bound :body body}))
               fml)
           (sjas-next-branch-nomo env free-var-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-termo bound narrowed-env bound-subst)
           (subst/subst-formulao body narrowed-env body-subst)
           (== (list 'neg
                     (list 'app 'leq (list 'var binding-nom) bound-subst))
               guard)
           (== (list 'or guard body-subst) guarded-body)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    guarded-body
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                    (lcons free-var-nom proof-vars)
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(nominal/fresh [binding-nom]
         (fresh [parameter-nom bound body bound-subst body-subst narrowed-env
                 guard guarded-body child next-fuel]
           (== (lcons child '()) children)
           (== (list 'bounded-exists
                     (nominal/tie binding-nom {:bound bound :body body}))
               fml)
           (sjas-next-branch-nomo env parameter-nom)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-termo bound narrowed-env bound-subst)
           (subst/subst-formulao body narrowed-env body-subst)
           (== (list 'pos
                     (list 'app 'leq (list 'var binding-nom) bound-subst))
               guard)
           (== (list 'and guard body-subst) guarded-body)
           (sjas-proof-tree-next-fuelo fuel next-fuel)
           (sjas-proof-check-stateo system-code
                                    guarded-body
                                    unexpanded
                                    lits
                                    (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                    proof-vars
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    gamma-terms
                                    next-fuel
                                    child)))]
      [(fresh [child next next-env rest next-fuel]
         (== (lcons child '()) children)
         (== (list 'true) fml)
         (sjas-agenda-heado env unexpanded next next-env rest)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  next-env
                                  proof-vars
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  gamma-terms
                                  next-fuel
                                  child))])))

(defn- sjas-structural-proof-check-stateo
  [system-code fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (fresh [node-formula children]
    (formula-bearing-proof-nodeo prog proof node-formula children)
    (sjas-structural-proof-check-state-decodedo system-code
                                                fml
                                                unexpanded
                                                lits
                                                env
                                                proof-vars
                                                sigma
                                                sigma-out
                                                neqs
                                                neqs-out
                                                prog
                                                gamma-terms
                                                fuel
                                                node-formula
                                                children)))

(defn- sjas-proof-guided-selecto
  "Select a branch formula candidate with its saved branch environment.

   The structural checker immediately validates the selected formula against the
   decoded proof-node formula. Keeping selection proof-node-blind avoids repeating
   the same large formula substitution and match before every decoded rule check,
   while remaining an ordinary relational list selection.
   "
  [_node-formula env formulas selected selected-env remaining]
  (fresh [head tail head-formula head-env]
    (== (lcons head tail) formulas)
    (sjas-agenda-entryo env head head-formula head-env)
    (conde
      [(== head-formula selected)
       (== head-env selected-env)
       (== tail remaining)]
      [(fresh [tail-selected tail-selected-env tail-remaining]
         (sjas-proof-guided-selecto _node-formula
                                    env
                                    tail
                                    tail-selected
                                    tail-selected-env
                                    tail-remaining)
         (== tail-selected selected)
         (== tail-selected-env selected-env)
         (== (lcons head tail-remaining) remaining))])))

(defn- sjas-proof-check-stateo
  [system-code fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (conde
    [(fresh [injected-proof node-formula children left right injected-agenda]
       ;; Xtab uses an explicit proof constructor so ordinary formula nodes do
       ;; not explore a cut alternative. The constructor carries a normal
       ;; formula-bearing LEM node; only the profile tag and complementary
       ;; disjunct check are additional to the existing tableau rules.
       (== (list 'xtab-lem injected-proof) proof)
       (formula-bearing-proof-nodeo prog injected-proof node-formula children)
       (sjas-presented-system-profile-header-coreo
         system-code
         system-profile-xtab-tag)
       (== (list 'or left right) node-formula)
       (conde
         [(sjas-formula-complemento left right)]
         [(sjas-formula-complemento right left)])
       (sjas-agenda-cons-coreo fml env unexpanded injected-agenda)
       (sjas-structural-proof-check-state-decodedo system-code
                                                   node-formula
                                                   injected-agenda
                                                   lits
                                                   env
                                                   proof-vars
                                                   sigma
                                                   sigma-out
                                                   neqs
                                                   neqs-out
                                                   prog
                                                   gamma-terms
                                                   fuel
                                                   node-formula
                                                   children))]
    [(fresh [node-formula children selected selected-env remaining]
       (formula-bearing-proof-nodeo prog proof node-formula children)
       (sjas-proof-guided-selecto node-formula
                                  env
                                  (lcons fml unexpanded)
                                  selected
                                  selected-env
                                  remaining)
       (sjas-structural-proof-check-state-decodedo system-code
                                                   selected
                                                   remaining
                                                   lits
                                                   selected-env
                                                   proof-vars
                                                   sigma
                                                   sigma-out
                                                   neqs
                                                   neqs-out
                                                   prog
                                                   gamma-terms
                                                   fuel
                                                   node-formula
                                                   children))]))

(defn- sjas-proof-check-programo
  "Validate `proof` for `target` through the SJAS-side proof checker.

   The target is already the proof-predicate tableau branch,
   `(and system-axioms negated-theorem)`. This wrapper preserves the ordinary
   kernel's empty initial branch state but deliberately does not call
   `kernel/prove-programo`; all accepted evidence must be consumed by
   `sjas-proof-check-stateo` above."
  [prog system-code target fuel proof]
  (fresh [sigma-out neqs-out]
    (sjas-proof-check-stateo system-code
                             target
                             '()
                             '()
                             '()
                             '()
                             '()
                             sigma-out
                             '()
                             neqs-out
                             prog
                             '()
                             fuel
                             proof)))

(defn structural-proof-valid?
  "Run the arithmeticized formula-bearing checker for an explicit target.

   This narrow diagnostic is used to test profile-specific proof rules without
   bypassing the object proof checker. It returns only whether the supplied
   finite proof tree was accepted; no host inference establishes a proof step."
  [prog system-code target proof fuel]
  (boolean
    (seq
      (run 1 [accepted]
        (sjas-proof-check-programo prog system-code target fuel proof)
        (== true accepted)))))

(defn decoded-proof-formula
  "Decode a public formula code to the exact AST used by proof checking."
  [prog formula-code]
  (first
    (run 1 [formula]
      (fresh [internal sigma-out]
        (sjas-decode-proof-formula-code-coreo prog
                                               formula-code
                                               '()
                                               sigma-out
                                               internal)
        (== '() sigma-out)
        (sjas-internal-formula-asto internal formula)))))

(defn- sjas-tableau-proof-destructureo
  "Walk a negated `tableau-proof/3` branch literal into its three code
   arguments (ADR-0091 shared preamble).

   The ADR-0095 synthesis branch reuses this so the negated-atom destructuring
   and the single proof-code position stay defined in one place rather than
   drifting between the checking and synthesizing branches."
  [fml env sigma system-code theorem-code proof-code]
  (fresh [lit atom walked-atom]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (list 'app 'tableau-proof system-code theorem-code proof-code)
                         walked-atom)))

(defn- sjas-tableau-proof-callo
  "Destructure a negated `tableau-proof/3` branch literal into its code
   arguments and decoded proof bytes (ADR-0091 shared preamble)."
  [fml env sigma system-code theorem-code proof-bytes sigma-proof proof-kind]
  (fresh [proof-code]
    (sjas-tableau-proof-destructureo fml env sigma
                                     system-code theorem-code proof-code)
    (sjas-formal-code-bytes-coreo proof-code
                                  proof-bytes
                                  sigma
                                  sigma-proof
                                  proof-kind)))

(defn- dsjas-code-bytes-match-coreo
  "Read a public code term and require it to match embedded composite bytes."
  [code embedded-bytes sigma sigma-out]
  (fresh [actual-bytes kind]
    (sjas-formal-code-bytes-coreo code actual-bytes sigma sigma-out kind)
    (sjas-byte-list-equalo embedded-bytes actual-bytes)))

(defn- decode-dsjas-tableau-proof-object-coreo
  "Decode measured `D_SJAS` object `C = (S,F,P)` from public proof code."
  [object-code system-code theorem-code proof-bytes sigma sigma-out]
  (fresh [object-bytes object-kind sigma-object rest object
          system-proof theorem-proof proof-proof
          system-bytes theorem-bytes sigma-system]
    (sjas-formal-code-bytes-coreo object-code
                                  object-bytes
                                  sigma
                                  sigma-object
                                  object-kind)
    (decode-proof-byteso object-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo
      (list 'dsjas-tableau-proof-object
            system-proof
            theorem-proof
            proof-proof)
      object)
    (proof-byte-list-termo system-proof system-bytes)
    (proof-byte-list-termo theorem-proof theorem-bytes)
    (proof-byte-list-termo proof-proof proof-bytes)
    (dsjas-code-bytes-match-coreo system-code system-bytes
                                  sigma-object sigma-system)
    (dsjas-code-bytes-match-coreo theorem-code theorem-bytes
                                  sigma-system sigma-out)))

(defn- decode-dsjas-subst-prf-object-coreo
  "Decode measured `D_SJAS` object `C = (S,G,F,P)` from public proof code."
  [object-code system-code substitution-code theorem-code proof-bytes
   sigma sigma-out]
  (fresh [object-bytes object-kind sigma-object rest object
          system-proof substitution-proof theorem-proof proof-proof
          system-bytes substitution-bytes theorem-bytes sigma-system
          sigma-substitution]
    (sjas-formal-code-bytes-coreo object-code
                                  object-bytes
                                  sigma
                                  sigma-object
                                  object-kind)
    (decode-proof-byteso object-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo
      (list 'dsjas-subst-prf-object
            system-proof
            substitution-proof
            theorem-proof
            proof-proof)
      object)
    (proof-byte-list-termo system-proof system-bytes)
    (proof-byte-list-termo substitution-proof substitution-bytes)
    (proof-byte-list-termo theorem-proof theorem-bytes)
    (proof-byte-list-termo proof-proof proof-bytes)
    (dsjas-code-bytes-match-coreo system-code system-bytes
                                  sigma-object sigma-system)
    (dsjas-code-bytes-match-coreo substitution-code substitution-bytes
                                  sigma-system sigma-substitution)
    (dsjas-code-bytes-match-coreo theorem-code theorem-bytes
                                  sigma-substitution sigma-out)))

(defn- sjas-dsjas-tableau-proof-destructureo
  "Walk a negated `dsjas-tableau-proof/3` literal into its code arguments."
  [fml env sigma system-code theorem-code proof-object-code]
  (fresh [lit atom walked-atom]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'dsjas-tableau-proof system-code theorem-code proof-object-code)
      walked-atom)))

(defn- sjas-dsjas-tableau-proof-callo
  "Destructure a measured `dsjas-tableau-proof/3` call and decode `C`."
  [fml env sigma system-code theorem-code proof-bytes sigma-proof]
  (fresh [proof-object-code]
    (sjas-dsjas-tableau-proof-destructureo fml env sigma
                                           system-code
                                           theorem-code
                                           proof-object-code)
    (decode-dsjas-tableau-proof-object-coreo proof-object-code
                                             system-code
                                             theorem-code
                                             proof-bytes
                                             sigma
                                             sigma-proof)))

(defn- sjas-dsjas-subst-prf-callo
  "Destructure a measured `dsjas-subst-prf/4` call and decode `C`."
  [fml env sigma system-code substitution-code theorem-code proof-bytes
   sigma-proof]
  (fresh [lit atom walked-atom proof-object-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'dsjas-subst-prf
            system-code substitution-code theorem-code proof-object-code)
      walked-atom)
    (decode-dsjas-subst-prf-object-coreo proof-object-code
                                         system-code
                                         substitution-code
                                         theorem-code
                                         proof-bytes
                                         sigma
                                         sigma-proof)))

(defn- sjas-tab1-proof-destructureo
  "Walk a negated `tab1-proof/3` literal into `S`, `F`, and `H` code terms."
  [fml env sigma system-code theorem-code proof-list-code]
  (fresh [lit atom walked-atom]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'tab1-proof system-code theorem-code proof-list-code)
      walked-atom)))

(defn- sjas-tab1-proof-callo
  "Destructure a public `tab1-proof/3` call and read proof-list bytes."
  [fml env sigma system-code theorem-code proof-list-bytes sigma-proof-list]
  (fresh [proof-list-code proof-list-kind]
    (sjas-tab1-proof-destructureo fml env sigma
                                  system-code
                                  theorem-code
                                  proof-list-code)
    (sjas-formal-code-bytes-coreo proof-list-code
                                  proof-list-bytes
                                  sigma
                                  sigma-proof-list
                                  proof-list-kind)))

(defn- decode-dsjas-tab1-proof-object-coreo
  "Decode measured Tab-1 object `C = (S,F,H)` from public proof code."
  [object-code system-code theorem-code proof-list-bytes sigma sigma-out]
  (fresh [object-bytes object-kind sigma-object rest object
          system-proof theorem-proof proof-list-proof
          system-bytes theorem-bytes sigma-system]
    (sjas-formal-code-bytes-coreo object-code
                                  object-bytes
                                  sigma
                                  sigma-object
                                  object-kind)
    (decode-proof-byteso object-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo
      (list 'dsjas-tab1-proof-object
            system-proof
            theorem-proof
            proof-list-proof)
      object)
    (proof-byte-list-termo system-proof system-bytes)
    (proof-byte-list-termo theorem-proof theorem-bytes)
    (proof-byte-list-termo proof-list-proof proof-list-bytes)
    (dsjas-code-bytes-match-coreo system-code system-bytes
                                  sigma-object sigma-system)
    (dsjas-code-bytes-match-coreo theorem-code theorem-bytes
                                  sigma-system sigma-out)))

(defn- sjas-dsjas-tab1-proof-callo
  "Destructure a measured `dsjas-tab1-proof/3` call and decode `C`."
  [fml env sigma system-code theorem-code proof-list-bytes sigma-proof-list]
  (fresh [lit atom walked-atom proof-object-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'dsjas-tab1-proof system-code theorem-code proof-object-code)
      walked-atom)
    (decode-dsjas-tab1-proof-object-coreo proof-object-code
                                          system-code
                                          theorem-code
                                          proof-list-bytes
                                          sigma
                                          sigma-proof-list)))

(defn- decode-dsjas-tab2-proof-object-coreo
  "Decode measured Tab-2 object `C = (S,F,H)` from public proof code."
  [object-code system-code theorem-code proof-list-bytes sigma sigma-out]
  (fresh [object-bytes object-kind sigma-object rest object
          system-proof theorem-proof proof-list-proof
          system-bytes theorem-bytes sigma-system]
    (sjas-formal-code-bytes-coreo object-code
                                  object-bytes
                                  sigma
                                  sigma-object
                                  object-kind)
    (decode-proof-byteso object-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo
      (list 'dsjas-tab2-proof-object
            system-proof
            theorem-proof
            proof-list-proof)
      object)
    (proof-byte-list-termo system-proof system-bytes)
    (proof-byte-list-termo theorem-proof theorem-bytes)
    (proof-byte-list-termo proof-list-proof proof-list-bytes)
    (dsjas-code-bytes-match-coreo system-code system-bytes
                                  sigma-object sigma-system)
    (dsjas-code-bytes-match-coreo theorem-code theorem-bytes
                                  sigma-system sigma-out)))

(defn- sjas-dsjas-tab2-proof-callo
  "Destructure measured `dsjas-tab2-proof/3` and decode its proof list."
  [fml env sigma system-code theorem-code proof-list-bytes sigma-proof-list]
  (fresh [lit atom walked-atom proof-object-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'dsjas-tab2-proof system-code theorem-code proof-object-code)
      walked-atom)
    (decode-dsjas-tab2-proof-object-coreo proof-object-code
                                          system-code
                                          theorem-code
                                          proof-list-bytes
                                          sigma
                                          sigma-proof-list)))

(defn- sjas-tableau-decoded-structural-proof-coreo
  "Validate a decoded structural tableau proof for a selected theorem code."
  [system-code theorem-code decoded-proof sigma sigma-out neqs neqs-out prog fuel]
  (fresh [neg-theorem target sigma-theorem walked-system-code axiom-formula]
    (sjas-structural-negated-theorem-coreo prog
                                           theorem-code
                                           sigma
                                           sigma-theorem
                                           neg-theorem)
    (sjas-system-axiom-formula-walked-coreo prog
                                            system-code
                                            sigma-theorem
                                            sigma-out
                                            walked-system-code
                                            axiom-formula)
    (sjas-acyclic-unifyo (list 'and axiom-formula neg-theorem) target)
    (sjas-proof-check-programo prog
                               walked-system-code
                               target
                               fuel
                               decoded-proof)
    (== neqs neqs-out)))

(defn- sjas-tableau-proof-bytes-coreo
  "Validate a `D_SJAS` tableau proof after the selected proof bytes are known."
  [system-code theorem-code proof-bytes sigma sigma-out neqs neqs-out prog fuel]
  (conde
    [(== sjas-axiom-proof-bytes proof-bytes)
     (sjas-walked-axiom-member-coreo prog
                                      system-code
                                      theorem-code
                                      sigma)
     (== sigma sigma-out)
     (== neqs neqs-out)]
    [(fresh [decoded-proof]
       (decode-structural-proof-bytes-coreo proof-bytes decoded-proof)
       (sjas-tableau-decoded-structural-proof-coreo system-code
                                                    theorem-code
                                                    decoded-proof
                                                    sigma
                                                    sigma-out
                                                    neqs
                                                    neqs-out
                                                    prog
                                                    fuel))]))

(defn- sjas-subst-prf-bytes-coreo
  "Validate a `D_SJAS` substitution proof after selected proof bytes are known."
  [system-code substitution-code theorem-code proof-bytes
   sigma sigma-out neqs neqs-out prog fuel]
  (fresh [decoded-proof axiom-formula subst-axiom-formula
          extended-axiom-formula neg-theorem target sigma-valid sigma-proof
          sigma-system walked-system-code walked-valid-system-code]
    (conde
      [(sjas-non-boundary-refutation-proof-rooto proof-bytes)
       (== sjas-axiom-proof-bytes proof-bytes)
       (fresh [system-bytes profile-tag beta-count beta-bytes]
         ;; Boundary tuples use the generated Level-1 skeleton. For a theorem
         ;; already in the finite axiom set, checking that exact skeleton is
         ;; sufficient; constructing its full diagonal result is redundant.
         (sjas-walked-axiom-member-coreo prog
                                          system-code
                                          theorem-code
                                          sigma)
         (sjas-system-code-bytes-walked-coreo system-code
                                              sigma
                                              sigma-system
                                              walked-system-code
                                              system-bytes)
         (== (lcons system-code-tag
                    (lcons profile-tag
                           (lcons beta-count beta-bytes)))
             system-bytes)
         (sjas-level1-family-profile-tago profile-tag)
         (level1-selfcons-skeleton-code-coreo
           prog
           walked-system-code
           system-bytes
           substitution-code
           sigma-system
           sigma-out))]
      [(sjas-non-boundary-refutation-proof-rooto proof-bytes)
       (== sjas-axiom-proof-bytes proof-bytes)
       (== 'sjas-axiom decoded-proof)
       (conde
         [(fresh []
            (sjas-walked-axiom-member-coreo prog
                                             system-code
                                             theorem-code
                                             sigma)
            (sjas-subst-source-result-antecedent-coreo prog
                                                        substitution-code
                                                        sigma
                                                        sigma-out
                                                        subst-axiom-formula))]
         [(fresh []
            (sjas-system-code-valid-walked-coreo prog
                                                 system-code
                                                 sigma
                                                 sigma-system
                                                 walked-valid-system-code)
            (sjas-subst-code-any-coreo prog
                                       substitution-code
                                       theorem-code
                                       sigma-system
                                       sigma-out))])]
      ;; The trusted `willard-sjas-boundary-refutation` branch was removed
      ;; (2026-06-22): it accepted the canonical contradiction on the mere
      ;; presence of the boundary hypotheses, with no checked derivation, which
      ;; made this proof predicate unsound. A genuine boundary contradiction
      ;; must be an ordinary measured structural/D_SJAS proof, checked by the
      ;; branches that remain. See
      ;; docs/interdev/2026-06-22-adr-0141-genuine-derivation-feasibility.md.
      [(sjas-non-boundary-refutation-proof-rooto proof-bytes)
       (decode-structural-proof-bytes-coreo proof-bytes decoded-proof)
       (sjas-system-axiom-formula-walked-coreo prog
                                               system-code
                                               sigma
                                               sigma-system
                                               walked-system-code
                                               axiom-formula)
       (sjas-subst-source-result-antecedent-coreo prog
                                                   substitution-code
                                                   sigma-system
                                                   sigma-valid
                                                   subst-axiom-formula)
       (sjas-structural-negated-theorem-coreo prog
                                              theorem-code
                                              sigma-valid
                                              sigma-out
                                              neg-theorem)
       (sjas-acyclic-unifyo (list 'and axiom-formula subst-axiom-formula)
                            extended-axiom-formula)
       (sjas-acyclic-unifyo (list 'and extended-axiom-formula neg-theorem) target)
       (sjas-proof-check-programo prog
                                  walked-system-code
                                  target
                                  fuel
                                  decoded-proof)])
    (== neqs neqs-out)))

(defn- decode-tab1-proof-list-object-coreo
  "Decode `H` bytes as `tab1-proof-list-object` entries.

   The host encoder stores each entry as a two-item proof list whose first
   element is theorem-code bytes and whose second element is proof-code bytes.
   Keeping this relation over decoded proof data lets Tab-1 validation stay
   inside the arithmeticized proof-code reader instead of trusting host-side
   projection."
  [proof-list-bytes entries]
  (fresh [rest object first-entry remaining-entries]
    (decode-proof-byteso proof-list-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo (lcons 'tab1-proof-list-object entries) object)
    (== (lcons first-entry remaining-entries) entries)))

(defn- decode-tab2-proof-list-object-coreo
  "Decode `H` bytes as a non-empty `tab2-proof-list-object`."
  [proof-list-bytes entries]
  (fresh [rest object first-entry remaining-entries]
    (decode-proof-byteso proof-list-bytes rest object)
    (== '() rest)
    (sjas-acyclic-unifyo (lcons 'tab2-proof-list-object entries) object)
    (== (lcons first-entry remaining-entries) entries)))

(declare sjas-tab1-tableau-proof-bytes-coreo)

(defn- sjas-tab1-proof-list-entry-coreo
  "Validate one decoded Tab-1 `(theorem, proof)` entry.

   ADR-0122 threads earlier theorem bytes into this check. The theorem bytes
   are also rebuilt as a public compact code term because the proof checker
   intentionally accepts the public code term, not raw bytes."
  [system-code prior-theorem-bytes entry sigma sigma-out neqs neqs-out prog
   fuel theorem-bytes]
  (fresh [theorem-proof proof-proof proof-bytes theorem-code]
    (== (list theorem-proof proof-proof) entry)
    (proof-byte-list-termo theorem-proof theorem-bytes)
    (proof-byte-list-termo proof-proof proof-bytes)
    (sjas-internal-code-termo theorem-bytes theorem-code)
    (sjas-tab1-tableau-proof-bytes-coreo system-code
                                         theorem-code
                                         theorem-bytes
                                         proof-bytes
                                         prior-theorem-bytes
                                         sigma
                                         sigma-out
                                         neqs
                                         neqs-out
                                         prog
                                         fuel)))

(defn- sjas-tab1-intermediate-formula-classo
  "Require an intermediate Tab-1 theorem to be `Pi*_1` or `Sigma*_1`."
  [prog theorem-bytes]
  (fresh [formula]
    (decode-proof-formula-byteso prog theorem-bytes '() formula)
    (conde
      [(sjas-pi-star-1-formulao formula)]
      [(sjas-sigma-star-1-formulao formula)])))

(defn- sjas-pi-star-2-formulao
  "Recognize prenex `forall* exists* Delta*0` formulas."
  [formula]
  (fresh [idx body]
    (== (list 'forall idx body) formula)
    (conde
      [(sjas-delta-star-0-formulao body)]
      [(sjas-pi-star-1-formulao body)]
      [(sjas-sigma-star-1-formulao body)]
      [(sjas-pi-star-2-formulao body)])))

(defn- sjas-sigma-star-2-formulao
  "Recognize prenex `exists* forall* Delta*0` formulas."
  [formula]
  (fresh [idx body]
    (== (list 'exists idx body) formula)
    (conde
      [(sjas-delta-star-0-formulao body)]
      [(sjas-sigma-star-1-formulao body)]
      [(sjas-pi-star-1-formulao body)]
      [(sjas-sigma-star-2-formulao body)])))

(defn- sjas-tab2-intermediate-formula-classo
  "Require a Tab-2 intermediate theorem to have Rank at most 2."
  [prog theorem-bytes]
  (fresh [formula]
    (decode-proof-formula-byteso prog theorem-bytes '() formula)
    (conde
      [(sjas-delta-star-0-formulao formula)]
      [(sjas-pi-star-1-formulao formula)]
      [(sjas-sigma-star-1-formulao formula)]
      [(sjas-pi-star-2-formulao formula)]
      [(sjas-sigma-star-2-formulao formula)])))

(defn- sjas-tab1-prior-theorem-member-coreo
  "Recognize a theorem byte string already validated by an earlier Tab-1 entry."
  [theorem-bytes prior-theorem-bytes]
  (fresh [head tail]
    (== (lcons head tail) prior-theorem-bytes)
    (conde
      [(sjas-byte-list-equalo theorem-bytes head)]
      [(sjas-tab1-prior-theorem-member-coreo theorem-bytes tail)])))

(defn- sjas-tab1-prior-theorem-formulas-coreo
  "Decode reusable theorem bytes into proof-side antecedent formulas."
  [prog prior-theorem-bytes formulas]
  (conde
    [(== '() prior-theorem-bytes)
     (== '() formulas)]
    [(fresh [theorem-bytes rest decoded ast-formula tail-formulas]
       (== (lcons theorem-bytes rest) prior-theorem-bytes)
       (decode-proof-formula-byteso prog theorem-bytes '() decoded)
       (sjas-proof-antecedent-formula-asto decoded ast-formula)
       (sjas-tab1-prior-theorem-formulas-coreo prog rest tail-formulas)
       (== (lcons ast-formula tail-formulas) formulas))]))

(defn- sjas-tab1-extended-axiom-formula-coreo
  "Conjoin `AxiomConj(S)` with earlier reusable Tab-1 theorem antecedents."
  [prog axiom-formula prior-theorem-bytes extended-axiom-formula]
  (fresh [prior-formulas all-formulas]
    (sjas-tab1-prior-theorem-formulas-coreo prog
                                            prior-theorem-bytes
                                            prior-formulas)
    (== (lcons axiom-formula prior-formulas) all-formulas)
    (formula-list-ando all-formulas extended-axiom-formula)))

(defn- sjas-tab1-tableau-proof-bytes-coreo
  "Validate one Tab-1 entry proof against beta plus earlier theorem entries."
  [system-code theorem-code theorem-bytes proof-bytes prior-theorem-bytes
   sigma sigma-out neqs neqs-out prog fuel]
  (conde
    [(sjas-non-boundary-refutation-proof-rooto proof-bytes)
     (== sjas-axiom-proof-bytes proof-bytes)
     (conde
       [(sjas-tab1-prior-theorem-member-coreo theorem-bytes
                                             prior-theorem-bytes)]
       [(sjas-walked-axiom-member-coreo prog
                                        system-code
                                        theorem-code
                                        sigma)])
     (== sigma sigma-out)
     (== neqs neqs-out)]
    ;; The trusted `willard-sjas-boundary-refutation` Tab-2 entry branch was
    ;; removed (2026-06-22) for the same soundness reason as the D_SJAS branch:
    ;; it accepted the contradiction without a checked derivation. A genuine
    ;; Tab-2 contradiction entry must be an ordinary measured proof.
    [(sjas-non-boundary-refutation-proof-rooto proof-bytes)
     (fresh [decoded-proof neg-theorem target sigma-theorem
             walked-system-code axiom-formula extended-axiom-formula]
       (decode-structural-proof-bytes-coreo proof-bytes decoded-proof)
       (sjas-structural-negated-theorem-coreo prog
                                              theorem-code
                                              sigma
                                              sigma-theorem
                                              neg-theorem)
       (sjas-system-axiom-formula-walked-coreo prog
                                               system-code
                                               sigma-theorem
                                               sigma-out
                                               walked-system-code
                                               axiom-formula)
       (sjas-tab1-extended-axiom-formula-coreo prog
                                               axiom-formula
                                               prior-theorem-bytes
                                               extended-axiom-formula)
       (sjas-acyclic-unifyo (list 'and extended-axiom-formula neg-theorem)
                            target)
       (sjas-proof-check-programo prog
                                  walked-system-code
                                  target
                                  fuel
                                  decoded-proof)
       (== neqs neqs-out))]))

(defn- sjas-tab1-proof-list-entries-coreo
  "Validate all entries of `H` and require the final theorem to be `F`.

   Intermediate entries are added to `prior-theorem-bytes` only after their
   proof validates and their theorem satisfies the public `Pi*_1` / `Sigma*_1`
   restriction, matching ADR-0119's beta-plus-earlier-`t_j` proof-list shape."
  [system-code target-code entries prior-theorem-bytes sigma sigma-out neqs
   neqs-out prog fuel]
  (conde
    [(fresh [entry theorem-bytes sigma-entry neqs-entry]
       (== (lcons entry '()) entries)
       (sjas-tab1-proof-list-entry-coreo system-code
                                         prior-theorem-bytes
                                         entry
                                         sigma
                                         sigma-entry
                                         neqs
                                         neqs-entry
                                         prog
                                         fuel
                                         theorem-bytes)
       (dsjas-code-bytes-match-coreo target-code theorem-bytes
                                     sigma-entry sigma-out)
       (== neqs-entry neqs-out))]
    [(fresh [entry rest next-entry remaining-rest theorem-bytes
             sigma-entry neqs-entry]
       (== (lcons entry rest) entries)
       (== (lcons next-entry remaining-rest) rest)
       (sjas-tab1-proof-list-entry-coreo system-code
                                         prior-theorem-bytes
                                         entry
                                         sigma
                                         sigma-entry
                                         neqs
                                         neqs-entry
                                         prog
                                         fuel
                                         theorem-bytes)
       (sjas-tab1-intermediate-formula-classo prog theorem-bytes)
       (sjas-tab1-proof-list-entries-coreo system-code
                                           target-code
                                           rest
                                           (lcons theorem-bytes
                                                  prior-theorem-bytes)
                                           sigma-entry
                                           sigma-out
                                           neqs-entry
                                           neqs-out
                                           prog
                                           fuel))]))

(defn- sjas-tab1-proof-list-bytes-coreo
  "Validate a decoded Tab-1 proof-list byte payload."
  [system-code theorem-code proof-list-bytes sigma sigma-out neqs neqs-out
   prog fuel]
  (fresh [entries]
    (decode-tab1-proof-list-object-coreo proof-list-bytes entries)
    (sjas-tab1-proof-list-entries-coreo system-code
                                        theorem-code
                                        entries
                                        '()
                                        sigma
                                        sigma-out
                                        neqs
                                        neqs-out
                                        prog
                                        fuel)))

(defn- sjas-tab2-proof-list-entries-coreo
  "Validate Tab-2 entries and restrict every non-final theorem to Rank 2."
  [system-code target-code entries prior-theorem-bytes sigma sigma-out neqs
   neqs-out prog fuel]
  (conde
    [(fresh [entry theorem-bytes sigma-entry neqs-entry]
       (== (lcons entry '()) entries)
       (sjas-tab1-proof-list-entry-coreo system-code
                                         prior-theorem-bytes
                                         entry
                                         sigma
                                         sigma-entry
                                         neqs
                                         neqs-entry
                                         prog
                                         fuel
                                         theorem-bytes)
       (dsjas-code-bytes-match-coreo target-code theorem-bytes
                                     sigma-entry sigma-out)
       (== neqs-entry neqs-out))]
    [(fresh [entry rest next-entry remaining-rest theorem-bytes
             sigma-entry neqs-entry]
       (== (lcons entry rest) entries)
       (== (lcons next-entry remaining-rest) rest)
       (sjas-tab1-proof-list-entry-coreo system-code
                                         prior-theorem-bytes
                                         entry
                                         sigma
                                         sigma-entry
                                         neqs
                                         neqs-entry
                                         prog
                                         fuel
                                         theorem-bytes)
       (sjas-tab2-intermediate-formula-classo prog theorem-bytes)
       (sjas-tab2-proof-list-entries-coreo system-code
                                           target-code
                                           rest
                                           (lcons theorem-bytes
                                                  prior-theorem-bytes)
                                           sigma-entry
                                           sigma-out
                                           neqs-entry
                                           neqs-out
                                           prog
                                           fuel))]))

(defn- sjas-tab2-proof-list-bytes-coreo
  "Validate a decoded Tab-2 proof-list byte payload."
  [system-code theorem-code proof-list-bytes sigma sigma-out neqs neqs-out
   prog fuel]
  (fresh [entries]
    (decode-tab2-proof-list-object-coreo proof-list-bytes entries)
    (sjas-tab2-proof-list-entries-coreo system-code
                                        theorem-code
                                        entries
                                        '()
                                        sigma
                                        sigma-out
                                        neqs
                                        neqs-out
                                        prog
                                        fuel)))

(defn tab2-proof-list-valid?
  "Return true when decoded bytes form a valid Tab-2 proof list for `theorem-code`.

   This focused diagnostic starts after the public measured-object wrapper has
   exposed the proof-list byte payload. It is useful for small tests of Tab-2
   rank classification and prior-theorem reuse; `dsjas-tab2-proof-valid?`
   remains the end-to-end measured-object check."
  [prog system-code theorem-code proof-list-bytes fuel]
  (boolean
    (seq
      (run 1 [accepted]
        (fresh [sigma-out neqs-out]
          (sjas-tab2-proof-list-bytes-coreo system-code
                                            theorem-code
                                            proof-list-bytes
                                            '()
                                            sigma-out
                                            '()
                                            neqs-out
                                            prog
                                            fuel)
          (== '() sigma-out)
          (== '() neqs-out)
          (== true accepted))))))

(defn- sjas-tab1-proof-coreo
  "Proof-free public `tab1-proof/3` predicate relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-list-bytes sigma-proof-list]
    (sjas-tab1-proof-callo fml env sigma
                           system-code
                           theorem-code
                           proof-list-bytes
                           sigma-proof-list)
    (sjas-tab1-proof-list-bytes-coreo system-code
                                      theorem-code
                                      proof-list-bytes
                                      sigma-proof-list
                                      sigma-out
                                      neqs
                                      neqs-out
                                      prog
                                      fuel)))

(defn- sjas-dsjas-tab1-proof-coreo
  "Proof-free measured `dsjas-tab1-proof/3` predicate relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-list-bytes sigma-proof-list]
    (sjas-dsjas-tab1-proof-callo fml env sigma
                                 system-code
                                 theorem-code
                                 proof-list-bytes
                                 sigma-proof-list)
    (sjas-tab1-proof-list-bytes-coreo system-code
                                      theorem-code
                                      proof-list-bytes
                                      sigma-proof-list
                                      sigma-out
                                      neqs
                                      neqs-out
                                      prog
                                      fuel)))

(defn- sjas-dsjas-tab2-proof-coreo
  "Proof-free measured `dsjas-tab2-proof/3` predicate relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-list-bytes sigma-proof-list]
    (sjas-dsjas-tab2-proof-callo fml env sigma
                                 system-code
                                 theorem-code
                                 proof-list-bytes
                                 sigma-proof-list)
    (sjas-tab2-proof-list-bytes-coreo system-code
                                      theorem-code
                                      proof-list-bytes
                                      sigma-proof-list
                                      sigma-out
                                      neqs
                                      neqs-out
                                      prog
                                      fuel)))

(defn dsjas-tab2-proof-valid?
  "Return true when the Tab-2 measured proof relation accepts a ground atom.

   This diagnostic executes the same arithmeticized `dsjas-tab2-proof/3`
   object relation used by the theory-close rule, but starts at the selected
   negative atom instead of asking the outer theorem prover to rediscover that
   branch. It is intended for focused apparatus tests and boundary validators
   that already have a concrete generated-SelfCons tuple."
  [prog system-code theorem-code proof-object-code fuel]
  (let [call (ast/neg-lit
               (ast/app-term 'dsjas-tab2-proof
                             system-code
                             theorem-code
                             proof-object-code))]
    (boolean
      (seq
        (run 1 [accepted]
          (fresh [sigma-out neqs-out]
            (sjas-dsjas-tab2-proof-coreo call
                                         '()
                                         '()
                                         sigma-out
                                         '()
                                         neqs-out
                                         prog
                                         fuel)
            (== '() sigma-out)
            (== '() neqs-out)
            (== true accepted)))))))

(declare sjas-dsjas-subst-prf-coreo)

(defn dsjas-subst-prf-valid?
  "Validate one ground measured `dsjas-subst-prf/4` atom directly.

   Boundary validators already possess a concrete generated SelfCons tuple.
   Starting at the selected theory-close relation avoids asking the outer
   tableau scheduler to rediscover that leaf while preserving the complete
   arithmeticized measured-object decoder and substitution-proof checker."
  [prog system-code substitution-code theorem-code proof-object-code fuel]
  (let [call (ast/neg-lit
               (ast/app-term 'dsjas-subst-prf
                             system-code
                             substitution-code
                             theorem-code
                             proof-object-code))]
    (boolean
      (seq
        (run 1 [accepted]
          (fresh [sigma-out neqs-out]
            (sjas-dsjas-subst-prf-coreo call
                                        '()
                                        '()
                                        sigma-out
                                        '()
                                        neqs-out
                                        prog
                                        fuel)
            (== '() sigma-out)
            (== '() neqs-out)
            (== true accepted)))))))

(defn syntax-code-valid?
  "Return true when a generated syntax-code predicate accepts ground args.

   This is a focused diagnostic for predicates such as `pi-star-1-code/1` and
   `neg-pair/2`. It executes the same arithmeticized syntax-code branch used by
   the proof profile, but starts after the branch literal has already been
   selected. Boundary SelfCons validators use it to check the positive body of
   `not(SelfCons)` without asking the generic tableau scheduler to rediscover a
   deterministic syntax predicate."
  [prog relation args]
  (boolean
    (seq
      (run 1 [accepted]
        (fresh [sigma-out branch-proof]
          (sjas-syntax-code-brancho prog
                                    relation
                                    args
                                    '()
                                    sigma-out
                                    branch-proof)
          (== '() sigma-out)
          (== true accepted))))))

(defn pi-star-1-code-valid?
  "Return true when `code` decodes as a Pi*_1 formula."
  [prog code]
  (syntax-code-valid? prog 'pi-star-1-code (list code)))

(defn neg-pair-valid?
  "Return true when `left-code` and `right-code` decode as complements."
  [prog left-code right-code]
  (syntax-code-valid? prog 'neg-pair (list left-code right-code)))

(defn- boundary-generated-code-termo
  "Build one compact public code term from relationally generated bytes.

   Workstream B synthesis uses this direction deliberately: proof and measured
   object bytes are first produced by the proof grammar, then this relation
   constructs the public object-language code supplied to SelfCons."
  [bytes code]
  (sjas-internal-code-termo bytes code))

(defn- boundary-level1-proof-callo
  "Require one generated Level-1 measured proof object to pass `D_SJAS`."
  [prog system-code substitution-code theorem-code proof-object-code fuel]
  (fresh [sigma-out neqs-out]
    (sjas-dsjas-subst-prf-coreo
      (list 'neg
            (list 'app
                  'dsjas-subst-prf
                  system-code
                  substitution-code
                  theorem-code
                  proof-object-code))
      '()
      '()
      sigma-out
      '()
      neqs-out
      prog
      fuel)
    (== '() sigma-out)
    (== '() neqs-out)))

(defn- boundary-tab2-proof-callo
  "Require one generated Tab-2 measured proof object to pass `D_SJAS`."
  [prog system-code theorem-code proof-object-code fuel]
  (fresh [sigma-out neqs-out]
    (sjas-dsjas-tab2-proof-coreo
      (list 'neg
            (list 'app
                  'dsjas-tab2-proof
                  system-code
                  theorem-code
                  proof-object-code))
      '()
      '()
      sigma-out
      '()
      neqs-out
      prog
      fuel)
    (== '() sigma-out)
    (== '() neqs-out)))

(defn- canonical-ground-proof-bytes
  "Return the canonical proof-code byte string for a fixed proof tree.

   The reverse direction of the relational `decode-proof-byteso` (ground proof
   tree, fresh bytes) is non-deterministic: its byte-literal branch yields a
   spurious first solution that the old boundary synthesis run only rejected by
   backtracking through the `dsjas-subst-prf`/`dsjas-tab2-proof` proof calls.
   That interleaved backtracking across alternative byte encodings made the
   search intractable (it did not complete in tens of minutes). The host
   encoder `sjas-code/proof-code-bytes` is the deterministic inverse and returns
   exactly the canonical byte string the old run converged to. Grounding each
   payload with it keeps the synthesis tractable while the main run still builds
   every tuple component relationally from the resulting bytes and accepts it
   only through the kernel object predicates."
  [proof-tree]
  (apply list (sjas-code/proof-code-bytes proof-tree)))

(defn- canonical-ground-public-bytes
  "Return the canonical public-code byte string for a ground code term."
  [code]
  (apply list (sjas-code/code-term-bytes code)))

(defn- deep-stack-call
  "Run `thunk` on a thread with an enlarged stack and return its value.

   The forward relational build and reification of a measured proof object
   recurse through core.logic's substitution walk to a depth proportional to the
   object size. The depth-3 total-multiplication object embeds its whole
   reflected system and overflows the default JVM thread stack. Running the
   synthesis on a thread with a large stack avoids that overflow without
   changing the search semantics: the relation, goal order, and answer are
   identical to running it inline."
  [thunk]
  (let [result (atom nil)
        error (atom nil)
        worker (Thread. nil
                        (fn []
                          (try
                            (reset! result (thunk))
                            (catch Throwable t (reset! error t))))
                        "sjas-boundary-synthesis"
                        (* 512 1024 1024))]
    (.start worker)
    (.join worker)
    (when @error (throw @error))
    @result))

(defn synthesize-level1-boundary-counterexample
  "Synthesize a Level-1 `(x,y,p,q)` SelfCons counterexample relationally.

   `x`, `y`, `p`, and `q` are fresh core.logic variables. The fixed byte
   payloads of the system, substitution skeleton, and both measured proof
   objects are first grounded in isolation (see `canonical-ground-proof-bytes`),
   because the reverse proof-byte decode is the documented non-terminating
   direction. The main run then builds every tuple component forward from those
   ground bytes through the proof-code grammar and accepts the tuple only after
   the Level-1 classifier, complement relation, and both `dsjas-subst-prf/4`
   calls succeed over the fresh tuple variables."
  [prog system-code substitution-code variant witness-formula-bytes fuel]
  (let [system-bytes (canonical-ground-public-bytes system-code)
        substitution-bytes (canonical-ground-public-bytes substitution-code)
        theorem-proof-bytes
        (canonical-ground-proof-bytes
          (list 'willard-sjas-boundary-refutation
                variant
                (list witness-formula-bytes)))
        complement-proof-bytes (canonical-ground-proof-bytes 'sjas-axiom)
        theorem-object-bytes
        (canonical-ground-proof-bytes
          (list 'dsjas-subst-prf-object
                system-bytes
                substitution-bytes
                reverse-contradiction-formula-bytes
                theorem-proof-bytes))
        complement-object-bytes
        (canonical-ground-proof-bytes
          (list 'dsjas-subst-prf-object
                system-bytes
                substitution-bytes
                reverse-contradiction-complement-formula-bytes
                complement-proof-bytes))]
    (deep-stack-call
     (fn []
      (first
       (run 1 [answer]
        (fresh [theorem-code complement-code
                theorem-proof-object complement-proof-object theorem-proof-code
                class-sigma complement-sigma class-proof complement-proof]
          (== (list theorem-code
                    complement-code
                    theorem-proof-object
                    complement-proof-object
                    theorem-proof-code)
              answer)
          (boundary-generated-code-termo
            reverse-contradiction-formula-bytes
            theorem-code)
          (boundary-generated-code-termo
            reverse-contradiction-complement-formula-bytes
            complement-code)
          (boundary-generated-code-termo theorem-proof-bytes theorem-proof-code)
          (boundary-generated-code-termo
            theorem-object-bytes
            theorem-proof-object)
          (boundary-generated-code-termo
            complement-object-bytes
            complement-proof-object)
          (sjas-syntax-code-brancho
            prog
            'pi-star-1-code
            (list theorem-code)
            '()
            class-sigma
            class-proof)
          (== '() class-sigma)
          (sjas-syntax-code-brancho
            prog
            'neg-pair
            (list theorem-code complement-code)
            '()
            complement-sigma
            complement-proof)
          (== '() complement-sigma)
          (boundary-level1-proof-callo
            prog
            system-code
            substitution-code
            theorem-code
            theorem-proof-object
            fuel)
          (boundary-level1-proof-callo
            prog
            system-code
            substitution-code
            complement-code
            complement-proof-object
            fuel))))))))

(defn synthesize-tab2-boundary-counterexample
  "Synthesize a Tab-2 `(x,y,p,q)` SelfCons counterexample relationally.

   The generated theorem proof list must first validate the supplied Rank-2
   witness, then reuse it in the dedicated boundary refutation entry. Both
   measured proof objects are constructed from fresh variables and checked by
   `dsjas-tab2-proof/3` before the tuple is returned."
  [prog system-code variant witness-code witness-proof
   witness-formula-bytes fuel]
  (let [system-bytes (canonical-ground-public-bytes system-code)
        witness-bytes (canonical-ground-public-bytes witness-code)
        witness-proof-bytes (canonical-ground-proof-bytes witness-proof)
        theorem-proof-bytes
        (canonical-ground-proof-bytes
          (list 'willard-sjas-boundary-refutation
                variant
                (list witness-formula-bytes)))
        complement-proof-bytes (canonical-ground-proof-bytes 'sjas-axiom)
        theorem-list-bytes
        (canonical-ground-proof-bytes
          (list 'tab2-proof-list-object
                (list witness-bytes witness-proof-bytes)
                (list reverse-contradiction-formula-bytes
                      theorem-proof-bytes)))
        complement-list-bytes
        (canonical-ground-proof-bytes
          (list 'tab2-proof-list-object
                (list reverse-contradiction-complement-formula-bytes
                      complement-proof-bytes)))
        theorem-object-bytes
        (canonical-ground-proof-bytes
          (list 'dsjas-tab2-proof-object
                system-bytes
                reverse-contradiction-formula-bytes
                theorem-list-bytes))
        complement-object-bytes
        (canonical-ground-proof-bytes
          (list 'dsjas-tab2-proof-object
                system-bytes
                reverse-contradiction-complement-formula-bytes
                complement-list-bytes))]
    (deep-stack-call
     (fn []
      (first
       (run 1 [answer]
        (fresh [theorem-code complement-code
                theorem-proof-object complement-proof-object theorem-proof-code
                complement-sigma complement-proof]
          (== (list theorem-code
                    complement-code
                    theorem-proof-object
                    complement-proof-object
                    theorem-proof-code)
              answer)
          (boundary-generated-code-termo
            reverse-contradiction-formula-bytes
            theorem-code)
          (boundary-generated-code-termo
            reverse-contradiction-complement-formula-bytes
            complement-code)
          (boundary-generated-code-termo theorem-proof-bytes theorem-proof-code)
          (boundary-generated-code-termo
            theorem-object-bytes
            theorem-proof-object)
          (boundary-generated-code-termo
            complement-object-bytes
            complement-proof-object)
          (sjas-syntax-code-brancho
            prog
            'neg-pair
            (list theorem-code complement-code)
            '()
            complement-sigma
            complement-proof)
          (== '() complement-sigma)
          (boundary-tab2-proof-callo
            prog
            system-code
            theorem-code
            theorem-proof-object
            fuel)
          (boundary-tab2-proof-callo
            prog
            system-code
            complement-code
            complement-proof-object
            fuel))))))))

(defn- sjas-dsjas-tableau-proof-coreo
  "Proof-free measured `dsjas-tableau-proof/3` predicate relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-bytes sigma-proof]
    (sjas-dsjas-tableau-proof-callo fml env sigma
                                    system-code theorem-code
                                    proof-bytes sigma-proof)
    (sjas-tableau-proof-bytes-coreo system-code
                                    theorem-code
                                    proof-bytes
                                    sigma-proof
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    fuel)))

(defn- sjas-dsjas-subst-prf-coreo
  "Proof-free measured `dsjas-subst-prf/4` predicate relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code substitution-code theorem-code proof-bytes sigma-proof]
    (sjas-dsjas-subst-prf-callo fml env sigma
                                system-code
                                substitution-code
                                theorem-code
                                proof-bytes
                                sigma-proof)
    (sjas-subst-prf-bytes-coreo system-code
                                substitution-code
                                theorem-code
                                proof-bytes
                                sigma-proof
                                sigma-out
                                neqs
                                neqs-out
                                prog
                                fuel)))

(defn- sjas-tableau-proof-certificate-coreo
  "Proof-free `sjas-axiom` certificate branch of `tableau-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog]
  (fresh [system-code theorem-code proof-bytes sigma-proof proof-kind]
    (sjas-tableau-proof-callo fml env sigma
                              system-code theorem-code
                              proof-bytes sigma-proof proof-kind)
    (== sjas-axiom-proof-bytes proof-bytes)
    (sjas-walked-axiom-member-coreo prog
                                     system-code
                                     theorem-code
                                     sigma-proof)
    (== sigma-proof sigma-out)
    (== neqs neqs-out)))

(defn- sjas-tableau-proof-structural-coreo
  "Proof-free structural-certificate branch of `tableau-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-bytes sigma-proof proof-kind
          decoded-proof neg-theorem target sigma-theorem
          walked-system-code axiom-formula]
    (sjas-tableau-proof-callo fml env sigma
                              system-code theorem-code
                              proof-bytes sigma-proof proof-kind)
    (decode-structural-proof-bytes-coreo proof-bytes decoded-proof)
    (sjas-structural-negated-theorem-coreo prog
                                           theorem-code
                                           sigma-proof
                                           sigma-theorem
                                           neg-theorem)
    (sjas-system-axiom-formula-walked-coreo prog
                                            system-code
                                            sigma-theorem
                                            sigma-out
                                            walked-system-code
                                            axiom-formula)
    (sjas-acyclic-unifyo (list 'and axiom-formula neg-theorem) target)
    (sjas-proof-check-programo prog
                               walked-system-code
                               target
                               fuel
                               decoded-proof)
    (== neqs neqs-out)))

(defn- sjas-tableau-proof-coreo
  "Proof-free `tableau-proof/3` predicate relation.

   This is the object relation used both by the ordinary SJAS theory wrapper
   and by formula-bearing structural tableau leaves. It decodes the supplied
   proof code, theorem code, and finite system code, then validates either an
   axiom citation or a structural tableau tree without constructing a separate
   Proflog answer-proof payload."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (conde
    [(sjas-tableau-proof-certificate-coreo fml env sigma sigma-out neqs neqs-out prog)]
    [(sjas-tableau-proof-structural-coreo fml env sigma sigma-out neqs neqs-out prog fuel)]))

(defn- sjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    ;; ADR-0091: citation certificates close through the proof-bearing
    ;; membership relation so public answers carry the axiom evidence the
    ;; e248c8b marker summary dropped; ADR-0090 pays the reification cost.
    [(fresh [system-code theorem-code proof-bytes sigma-proof proof-kind
             member-proof]
       (sjas-tableau-proof-callo fml env sigma
                                 system-code theorem-code
                                 proof-bytes sigma-proof proof-kind)
       (== sjas-axiom-proof-bytes proof-bytes)
       (sjas-walked-axiom-membero prog
                                  system-code
                                  theorem-code
                                  sigma-proof
                                  member-proof)
       (== sigma-proof sigma-out)
       (== neqs neqs-out)
       (== (list 'profiled 'willard-sjas-proof-check member-proof) proof))]
    ;; ADR-0095 synthesis: when the proof-code position walks to an unbound
    ;; object variable, construct the canonical `sjas-axiom` citation
    ;; certificate from its fixed proof bytes, bind it through the branch
    ;; equality state, then validate membership exactly as for a presented
    ;; certificate. Disjoint from the branch above: a presented code never
    ;; walks to a `var`-headed term. Construction goes through the canonical
    ;; compact builder rather than running the presented-code reader backward,
    ;; because that reader accepts non-canonical numerals and so is not a
    ;; bijection (ADR-0095 review).
    [(fresh [system-code theorem-code proof-code binding-nom
             certificate sigma-bound bind-proof member-proof]
       (sjas-tableau-proof-destructureo fml env sigma
                                        system-code theorem-code proof-code)
       (== (list 'var binding-nom) proof-code)
       (sjas-internal-code-termo sjas-axiom-proof-bytes certificate)
       (equality/unify-termo proof-code certificate sigma sigma-bound
                             bind-proof)
       (sjas-walked-axiom-membero prog
                                  system-code
                                  theorem-code
                                  sigma-bound
                                  member-proof)
       (== sigma-bound sigma-out)
       (== neqs neqs-out)
       (== (list 'profiled 'willard-sjas-proof-check
                 (list 'sjas-synthesized-citation bind-proof member-proof))
           proof))]
    ;; Structural certificates keep the plain wrapper; their inspectable
    ;; evidence is the decoded proof tree the checker validates node by node.
    [(sjas-tableau-proof-structural-coreo fml env sigma sigma-out neqs neqs-out prog fuel)
     (== '(profiled willard-sjas-proof-check) proof)]))

(defn- sjas-subst-prf-coreo
  "Proof-free `subst-prf/4` predicate relation.

   `SubstPrf` first validates the substitution-side formula relation, then
   checks the supplied theorem proof against beta plus the substituted source
   sentence. The relation intentionally returns only branch state effects; the
   ordinary public answer marker is layered on by `sjas-subst-prf-closeo`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [lit atom walked-atom system-code substitution-code theorem-code proof-code
          decoded-proof proof-bytes proof-kind axiom-formula subst-axiom-formula
          extended-axiom-formula neg-theorem target sigma-valid sigma-proof
          sigma-system walked-system-code walked-valid-system-code]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'subst-prf system-code substitution-code theorem-code proof-code)
      walked-atom)
    (sjas-formal-code-bytes-coreo proof-code
                                  proof-bytes
                                  sigma
                                  sigma-proof
                                  proof-kind)
    (conde
      [(== sjas-axiom-proof-bytes proof-bytes)
       (== 'sjas-axiom decoded-proof)
       (conde
         [(fresh []
            (sjas-walked-axiom-member-coreo prog
                                             system-code
                                             theorem-code
                                             sigma-proof)
            (sjas-subst-source-result-antecedent-coreo prog
                                                        substitution-code
                                                        sigma-proof
                                                        sigma-out
                                                        subst-axiom-formula))]
         [(fresh []
            (sjas-system-code-valid-walked-coreo prog
                                                 system-code
                                                 sigma-proof
                                                 sigma-system
                                                 walked-valid-system-code)
            (sjas-subst-code-any-coreo prog
                                       substitution-code
                                       theorem-code
                                       sigma-system
                                       sigma-out))])]
      [(decode-structural-proof-bytes-coreo proof-bytes decoded-proof)
       (sjas-system-axiom-formula-walked-coreo prog
                                               system-code
                                               sigma-proof
                                               sigma-system
                                               walked-system-code
                                               axiom-formula)
       (sjas-subst-source-result-antecedent-coreo prog
                                                   substitution-code
                                                   sigma-system
                                                   sigma-valid
                                                   subst-axiom-formula)
       (sjas-structural-negated-theorem-coreo prog
                                               theorem-code
                                               sigma-valid
                                               sigma-out
                                               neg-theorem)
       (sjas-acyclic-unifyo (list 'and axiom-formula subst-axiom-formula)
                            extended-axiom-formula)
       (sjas-acyclic-unifyo (list 'and extended-axiom-formula neg-theorem) target)
       (sjas-proof-check-programo prog
                                  walked-system-code
                                  target
                                  fuel
                                  decoded-proof)])
    (== neqs neqs-out)))

(defn- sjas-subst-prf-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-subst-prf-coreo fml
                          env
                          sigma
                          sigma-out
                          neqs
                          neqs-out
                          prog
                          fuel)
    (== '(profiled willard-sjas-subst-proof-check) proof)))

(defn- sjas-finax4-coreo
  "Executable `FinAx4(alpha)` check for Willard boundary route axioms.

   `alpha` is accepted only when it decodes as a complete generated SJAS finite
   system code. This deliberately reuses the object-level system-code
   reconstruction used by proof predicates; no host-side source registry is
   consulted."
  [fml env sigma sigma-out neqs neqs-out prog]
  (fresh [lit atom walked-atom alpha walked-system-code system-bytes]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo (list 'app 'finax4 alpha) walked-atom)
    (sjas-system-code-bytes-walked-coreo alpha
                                         sigma
                                         sigma-out
                                         walked-system-code
                                         system-bytes)
    (sjas-system-source-valid-coreo prog system-bytes)
    (== neqs neqs-out)))

(defn- sjas-finax4-closeo
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh []
    (sjas-finax4-coreo fml env sigma sigma-out neqs neqs-out prog)
    (== '(profiled willard-sjas-finax4) proof)))

(defn- sjas-finax4-structural-closeo
  [fml env sigma sigma-out neqs neqs-out prog]
  (sjas-finax4-coreo fml env sigma sigma-out neqs neqs-out prog))

(defn- sjas-semprf-alpha-destructureo
  "Destructure Willard `SemPrf_alpha(theorem, proof)` leaves.

   The relation is not an uninterpreted predicate: it delegates to the same
   public proof-code reader and tableau checker used by `tableau-proof/3`.
   This gives the V-route formulas executable proof-kernel content without
   adding a host-side proof oracle."
  [fml env sigma system-code theorem-code proof-code]
  (fresh [lit atom walked-atom]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'semprf-alpha system-code theorem-code proof-code)
      walked-atom)))

(defn- sjas-semprf-alpha-coreo
  "Proof-free executable `SemPrf_alpha` relation."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code theorem-code proof-code]
    (sjas-semprf-alpha-destructureo fml env sigma
                                    system-code theorem-code proof-code)
    (conde
      [(fresh [proof-bytes sigma-proof]
       (decode-sjas-axiom-proof-code-coreo proof-code
                                           sigma
                                           sigma-proof
                                           proof-bytes)
       (sjas-walked-axiom-member-coreo prog
                                        system-code
                                        theorem-code
                                        sigma-proof)
       (== sigma-proof sigma-out)
       (== neqs neqs-out))]
      [(fresh [proof-bytes sigma-proof decoded-proof]
         (decode-non-sjas-axiom-proof-code-coreo proof-code
                                                 sigma
                                                 sigma-proof
                                                 proof-bytes
                                                 decoded-proof)
         (sjas-tableau-decoded-structural-proof-coreo system-code
                                                      theorem-code
                                                      decoded-proof
                                                      sigma-proof
                                                      sigma-out
                                                      neqs
                                                      neqs-out
                                                      prog
                                                      fuel))])))

(defn- sjas-semprfk-alpha-destructureo
  "Destructure Willard bounded `SemPrf^k_alpha(theorem, proof, bound)` leaves."
  [fml env sigma system-code k-code theorem-code proof-code bound-code]
  (fresh [lit atom walked-atom]
    (sjas-subst-formulao fml env lit)
    (sjas-acyclic-unifyo (list 'neg atom) lit)
    (sjas-walk-atomo atom sigma walked-atom)
    (sjas-acyclic-unifyo
      (list 'app 'semprfk-alpha
            system-code k-code theorem-code proof-code bound-code)
      walked-atom)))

(defn- sjas-semprfk-alpha-coreo
  "Executable bounded semantic-proof relation used by the Willard V-route."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [system-code k-code theorem-code proof-code bound-code
          sigma-proof-valid bound-proof]
    (sjas-semprfk-alpha-destructureo fml env sigma
                                     system-code k-code theorem-code proof-code
                                     bound-code)
    (conde
      [(fresh [proof-bytes sigma-proof]
       (decode-sjas-axiom-proof-code-coreo proof-code
                                           sigma
                                           sigma-proof
                                           proof-bytes)
       (sjas-walked-axiom-member-coreo prog
                                        system-code
                                        theorem-code
                                        sigma-proof)
       (== sigma-proof sigma-proof-valid)
       (== neqs neqs-out))]
      [(fresh [proof-bytes sigma-proof decoded-proof]
         (decode-non-sjas-axiom-proof-code-coreo proof-code
                                                 sigma
                                                 sigma-proof
                                                 proof-bytes
                                                 decoded-proof)
         (sjas-tableau-decoded-structural-proof-coreo system-code
                                                      theorem-code
                                                      decoded-proof
                                                      sigma-proof
                                                      sigma-proof-valid
                                                      neqs
                                                      neqs-out
                                                      prog
                                                      fuel))])
    (sjas-semprfk-bound-holdso proof-code bound-code k-code
                               sigma-proof-valid sigma-out bound-proof)))

(defn- sjas-semprf-alpha-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-semprf-alpha-coreo fml
                             env
                             sigma
                             sigma-out
                             neqs
                             neqs-out
                             prog
                             fuel)
    (== '(profiled willard-sjas-semprf-alpha) proof)))

(defn- sjas-semprfk-alpha-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-semprfk-alpha-coreo fml
                              env
                              sigma
                              sigma-out
                              neqs
                              neqs-out
                              prog
                              fuel)
    (== '(profiled willard-sjas-semprfk-alpha) proof)))

(defn- sjas-dsjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-dsjas-tableau-proof-coreo fml
                                    env
                                    sigma
                                    sigma-out
                                    neqs
                                    neqs-out
                                    prog
                                    fuel)
    (== '(profiled willard-sjas-proof-check) proof)))

(defn- sjas-dsjas-subst-prf-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-dsjas-subst-prf-coreo fml
                                env
                                sigma
                                sigma-out
                                neqs
                                neqs-out
                                prog
                                fuel)
    (== '(profiled willard-sjas-subst-proof-check) proof)))

(defn- sjas-tab1-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-tab1-proof-coreo fml
                           env
                           sigma
                           sigma-out
                           neqs
                           neqs-out
                           prog
                           fuel)
    (== '(profiled willard-sjas-tab1-proof-check) proof)))

(defn- sjas-dsjas-tab1-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-dsjas-tab1-proof-coreo fml
                                 env
                                 sigma
                                 sigma-out
                                 neqs
                                 neqs-out
                                 prog
                                 fuel)
    (== '(profiled willard-sjas-tab1-proof-check) proof)))

(defn- sjas-dsjas-tab2-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-dsjas-tab2-proof-coreo fml
                                 env
                                 sigma
                                 sigma-out
                                 neqs
                                 neqs-out
                                 prog
                                 fuel)
    (== '(profiled willard-sjas-tab2-proof-check) proof)))

(defn- sjas-tableau-proof-structural-closeo
  "Close a structural tableau leaf through `tableau-proof/3`.

   The formula-bearing proof tree already supplies the relevant local evidence,
   so this relation keeps only the object-level predicate success and branch
   state effects. The ordinary answer proof marker is not part of the SJAS
   tableau proof code."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-tableau-proof-coreo fml
                            env
                            sigma
                            sigma-out
                            neqs
                            neqs-out
                            prog
                            fuel))

(defn- sjas-subst-prf-structural-closeo
  "Close a structural tableau leaf through `subst-prf/4`.

   This is the substitution-proof analogue of
   `sjas-tableau-proof-structural-closeo`: it invokes the arithmeticized
   substitution and proof-predicate relations but does not require or encode a
   separate Proflog proof trace in the formula-bearing tableau node."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-subst-prf-coreo fml
                        env
                        sigma
                        sigma-out
                        neqs
                        neqs-out
                        prog
                        fuel))

(defn- sjas-dsjas-tableau-proof-structural-closeo
  "Close a structural tableau leaf through measured `dsjas-tableau-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-dsjas-tableau-proof-coreo fml
                                  env
                                  sigma
                                  sigma-out
                                  neqs
                                  neqs-out
                                  prog
                                  fuel))

(defn- sjas-dsjas-subst-prf-structural-closeo
  "Close a structural tableau leaf through measured `dsjas-subst-prf/4`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-dsjas-subst-prf-coreo fml
                              env
                              sigma
                              sigma-out
                              neqs
                              neqs-out
                              prog
                              fuel))

(defn- sjas-tab1-proof-structural-closeo
  "Close a structural tableau leaf through public `tab1-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-tab1-proof-coreo fml
                         env
                         sigma
                         sigma-out
                         neqs
                         neqs-out
                         prog
                         fuel))

(defn- sjas-dsjas-tab1-proof-structural-closeo
  "Close a structural tableau leaf through measured `dsjas-tab1-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-dsjas-tab1-proof-coreo fml
                               env
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               fuel))

(defn- sjas-dsjas-tab2-proof-structural-closeo
  "Close a structural tableau leaf through measured `dsjas-tab2-proof/3`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (sjas-dsjas-tab2-proof-coreo fml
                               env
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               fuel))

(defn willard-sjas-theory-closeo
  "SJAS theory branch rule bound into the ordinary proof kernel."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (conde
    [(sjas-dsjas-tab2-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-dsjas-tab1-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-tab1-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-dsjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-dsjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-semprfk-alpha-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-semprf-alpha-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-finax4-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-pos-relation-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-eq-progresso fml unexpanded lits env proof-vars sigma sigma-out
                        neqs neqs-out prog gamma-terms fuel proof)]
    [(sjas-syntax-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-subst-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-axiom-member-closeo fml env sigma sigma-out neqs neqs-out prog proof)]))

(defn willard-sjas-answer-theory-closeo
  "SJAS theory branch rule for the answer overlay.

   The answer layer carries residual obligations in addition to equality and
   disequality state. Arithmetic closures do not create residuals, so successful
   profile steps preserve that list while exporting any numeral bindings through
   `sigma-out`."
  [fml _unexpanded _lits env _proof-vars sigma sigma-out neqs neqs-out
   residuals residuals-out prog _gamma-terms fuel _call-depth _existentials-as-vars?
   proof]
  (conde
    [(sjas-dsjas-tab2-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-dsjas-tab1-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-tab1-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-dsjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-dsjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-semprfk-alpha-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-semprf-alpha-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-finax4-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-pos-relation-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-syntax-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-subst-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-axiom-member-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]))

;; -----------------------------------------------------------------------------
;; Public proof-profile entrypoint
;; -----------------------------------------------------------------------------

(defn- profile-symbol
  "Convert a profile keyword into the symbol used in proof evidence."
  [profile]
  (symbol (name profile)))

(defn- wrap-proof
  "Attach an explicit SJAS profile marker to an ordinary kernel proof term."
  [profile proof]
  (list 'profiled (profile-symbol profile) proof))

(defn prove-program
  "Prove with SJAS arithmetic and certificate rules interleaved into the kernel."
  [profile program formula proof-limit fuel]
  (let [proofs (binding [kernel/*theory-profile-closeo* willard-sjas-theory-closeo]
                 (doall
                   (if (nil? fuel)
                     (run proof-limit [proof]
                       (kernel/prove-programo formula '() '() '() program '() nil proof))
                     (run proof-limit [proof]
                       (kernel/prove-programo formula '() '() '() program '() fuel proof)))))]
    (map #(wrap-proof profile %) proofs)))
