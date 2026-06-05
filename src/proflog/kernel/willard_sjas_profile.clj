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
  (:require [clojure.core.logic :refer [!= == appendo conde fail fresh lcons membero or* run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel :as kernel]
            [proflog.kernel-support :as support]
            [proflog.relational-arithmetic :as arith]
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
           (contains? '#{willard-sjas-tableau0 willard-sjas-level1}
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

(defn- proof-byte-decremento
  [byte predecessor]
  (fresh [entry]
    (membero entry proof-byte-decrement-entries)
    (== [byte predecessor] entry)))

(defn- byte-bitso
  [bits byte]
  (fresh [entry]
    (membero entry byte-bit-entries)
    (== [bits byte] entry)))

(defn- byte-six-bitso
  [bits byte]
  (fresh [entry]
    (membero entry byte-six-bit-entries)
    (== [bits byte] entry)))

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

(defn- canonical-bit-termo
  "Peel one low bit from a canonical public U-Grounding numeral term."
  [term bit tail sigma sigma-out proof]
  (fresh [walked]
    (equality/walko term sigma walked)
    (conde
      [(== zero-term walked)
       (== 0 bit)
       (== zero-term tail)
       (== sigma sigma-out)
       (== '(sjas-ug-code-bit-zero) proof)]
      [(== one-term walked)
       (== 1 bit)
       (== zero-term tail)
       (== sigma sigma-out)
       (== '(sjas-ug-code-bit-one) proof)]
      [(fresh [arg]
         (== (list 'app 'dbl arg) walked)
         (== 0 bit)
         (== arg tail)
         (== sigma sigma-out)
         (== '(sjas-ug-code-bit-dbl) proof))]
      [(fresh [arg doubled]
         (== (list 'app 'add doubled one-term) walked)
         (== (list 'app 'dbl arg) doubled)
         (== 1 bit)
         (== arg tail)
         (== sigma sigma-out)
         (== '(sjas-ug-code-bit-add-one) proof))])))

(defn- canonical-byte-cons-proofo
  "Record the fixed-radix byte equation proven by six canonical bit peels."
  [tail proof]
  (conde
    [(== zero-term tail)
     (== (list 'sjas-ug-code-byte-cons
               '(sjas-ug-code-mul64-zero)
               '(sjas-ug-code-canonical-byte))
         proof)]
    [(!= zero-term tail)
     (== (list 'sjas-ug-code-byte-cons
               '(sjas-ug-code-mul64-shift)
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
    (== t6 tail)
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
        [(== sjas-code/u-grounding-sentinel-byte byte)
         (== zero-term tail)
         (== '() bytes)
         (== sigma-after sigma-out)
         (== (list 'sjas-ug-code-end byte-proof) proof)]
        [(fresh [tail-bytes tail-proof]
           (!= zero-term tail)
           (sjas-ug-code-bytes-termo (dec remaining)
                                     tail
                                     tail-bytes
                                     sigma-after
                                     sigma-out
                                     tail-proof)
           (== (lcons byte tail-bytes) bytes)
           (== (list 'sjas-ug-code-cons byte-proof) proof))]))))

(declare compact-code-byte-bits-termo)

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

   Present public terms derive their bits through the object-level numeral
   reader before the finite byte relation is consulted. Decoded-byte
   reconstruction uses `code-byte-build-termo`; this relation is optimized for
   the presented-code path. Neither mode projects a ground byte term through a
   host decoder."
  [term byte]
  (fresh [bits]
    (compact-code-byte-bits-termo term bits)
    (byte-bitso bits byte)))

(defn- code-byte-build-termo
  "Build a compact-code public byte numeral from a decoded byte value.

   This is the byte-first companion to `code-byte-termo`. It is used only when
   embedded code payload bytes have already been decoded by object-level
   syntax relations and the corresponding compact public code term must be
   reconstructed structurally."
  [term byte]
  (fresh [bits]
    (byte-bitso bits byte)
    (compact-code-byte-bits-termo term bits)))

(defn- code-constructoro
  [constructor byte-count]
  (fresh [entry]
    (membero entry code-constructor-entries)
    (== [constructor byte-count] entry)))

(defn- code-argso
  [args bytes proof]
  (conde
    [(== '() args)
     (== '() bytes)
     (== '(sjas-code-args-end) proof)]
    [(fresh [arg rest byte byte-rest rest-proof]
       (== (lcons arg rest) args)
       (code-byte-termo arg byte)
       (== (lcons byte byte-rest) bytes)
       (code-argso rest byte-rest rest-proof)
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
    (equality/walko term sigma walked)
    (== (lcons 'app (lcons constructor args)) walked)
    (code-constructoro constructor byte-count)
    (code-argso args bytes args-proof)
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
    [(== :compact kind)
     (sjas-code-byteso term bytes sigma sigma-out proof)]
    [(== :u-grounding kind)
     (sjas-ug-code-byteso term bytes sigma sigma-out proof)]))

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
    (equality/walko term sigma walked)
    (conde
      [(== zero-term walked)
       (== 0 bit)
       (== zero-term tail)
       (== sigma sigma-out)]
      [(== one-term walked)
       (== 1 bit)
       (== zero-term tail)
       (== sigma sigma-out)]
      [(fresh [arg]
         (== (list 'app 'dbl arg) walked)
         (== 0 bit)
         (== arg tail)
         (== sigma sigma-out))]
      [(fresh [arg doubled]
         (== (list 'app 'add doubled one-term) walked)
         (== (list 'app 'dbl arg) doubled)
         (== 1 bit)
         (== arg tail)
         (== sigma sigma-out))])))

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
    (== t6 tail)))

(defn- sjas-ug-code-bytes-term-coreo
  "Proof-free U-Grounding public-code byte reader."
  [remaining term bytes sigma sigma-out]
  (if (neg? remaining)
    fail
    (fresh [byte tail sigma-after]
      (canonical-byte-term-coreo term byte tail sigma sigma-after)
      (conde
        [(== sjas-code/u-grounding-sentinel-byte byte)
         (== zero-term tail)
         (== '() bytes)
         (== sigma-after sigma-out)]
        [(fresh [tail-bytes]
           (!= zero-term tail)
           (sjas-ug-code-bytes-term-coreo (dec remaining)
                                          tail
                                          tail-bytes
                                          sigma-after
                                          sigma-out)
           (== (lcons byte tail-bytes) bytes))]))))

(defn- code-args-coreo
  "Proof-free compact-code argument byte reader."
  [args bytes]
  (conde
    [(== '() args)
     (== '() bytes)]
    [(fresh [arg rest byte byte-rest]
       (== (lcons arg rest) args)
       (code-byte-termo arg byte)
       (== (lcons byte byte-rest) bytes)
       (code-args-coreo rest byte-rest))]))

(defn- sjas-code-bytes-coreo
  "Proof-free compact public-code byte reader."
  [term bytes sigma sigma-out]
  (fresh [walked constructor args byte-count]
    (equality/walko term sigma walked)
    (== (lcons 'app (lcons constructor args)) walked)
    (code-constructoro constructor byte-count)
    (code-args-coreo args bytes)
    (== sigma sigma-out)))

(defn- sjas-ug-code-bytes-coreo
  "Proof-free U-Grounding public-code byte reader."
  [term bytes sigma sigma-out]
  (sjas-ug-code-bytes-term-coreo sjas-code/max-code-bytes
                                 term
                                 bytes
                                 sigma
                                 sigma-out))

(defn- sjas-formal-code-bytes-coreo
  "Decode either supported public SJAS code representation without building
   auxiliary proof-trace evidence."
  [term bytes sigma sigma-out kind]
  (conde
    [(== :compact kind)
     (sjas-code-bytes-coreo term bytes sigma sigma-out)]
    [(== :u-grounding kind)
     (sjas-ug-code-bytes-coreo term bytes sigma sigma-out)]))

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
(def ^:private system-reflected-clause-tag 34)

(def ^:private internal-zero-num (list 'num '()))
(def ^:private internal-one-num (list 'num (list 1)))
(def ^:private internal-two-num (list 'num (list 2)))

(def ^:private group-zero-internal-formulas
  [(list 'neq internal-one-num internal-zero-num)
   (list 'neq internal-two-num internal-zero-num)])

(def ^:private group-one-internal-formulas
  [(list 'eq internal-zero-num internal-zero-num)
   (list 'eq internal-two-num internal-two-num)
   (list 'eq
         (list 'app 'sub (list internal-two-num internal-one-num))
         internal-one-num)])

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

(def ^:private reserved-symbol-index-entries
  "Fixed SJAS vocabulary entries recoverable without a generated codebook."
  (apply list
         (map-indexed (fn [idx sym]
                        [(inc idx) sym])
                      sjas-code/reserved-coding-symbols)))

(def ^:private user-symbol-index-entries
  "Formula-code symbol indexes not reserved by the fixed SJAS codebook."
  (let [reserved-indexes (set (range 1
                                      (inc (count sjas-code/reserved-coding-symbols))))]
    (apply list
           (remove reserved-indexes
                   (range 1 sjas-code/byte-base)))))

(defn- positive-byteo
  [byte]
  (membero byte positive-byte-entries))

(defn- positive-byte-except-oneo
  [byte]
  (membero byte positive-byte-except-one-entries))

(defn- positive-byte-neqo
  [left right]
  (fresh [entry]
    (membero entry positive-byte-neq-entries)
    (== [left right] entry)))

(defn- byte-neqo
  [left right]
  (fresh [entry]
    (membero entry byte-neq-entries)
    (== [left right] entry)))

(defn- sjas-reserved-symbol-indexo
  [idx sym]
  (fresh [entry]
    (membero entry reserved-symbol-index-entries)
    (== [idx sym] entry)))

(defn- sjas-user-symbol-indexo
  [idx]
  (membero idx user-symbol-index-entries))

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
      (decode-term-byteso prog bytes after-head head)
      (parse-term-list-byteso prog (dec remaining) after-head rest tail)
      (== (lcons head tail) terms))))

(defn- parse-code-payload-byteso
  [remaining bytes rest payload]
  (if (zero? remaining)
    (conde
      [(== bytes rest)
       (== '() payload)])
    (fresh [byte tail after-byte]
      (== (lcons byte after-byte) bytes)
      (parse-code-payload-byteso (dec remaining) after-byte rest tail)
      (== (lcons byte tail) payload))))

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
             (fresh [payload]
               (== expected-low low)
               (== expected-high high)
               (parse-code-payload-byteso byte-count payload-bytes rest payload)
               (== (list 'code payload) term))))
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
             (fresh [payload]
               (== expected-low low)
               (== expected-high high)
               (parse-code-payload-byteso byte-count payload-bytes rest payload)
               (== (list 'num payload) term))))
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
  [prog arity after-symbol rest args]
  (if (= arity (dec sjas-code/byte-base))
    fail
    (conde
      [(fresh [arg-bytes]
         (== (lcons (inc arity) arg-bytes) after-symbol)
         (parse-term-list-byteso prog arity arg-bytes rest args))]
      [(decode-app-arityo prog (inc arity) after-symbol rest args)])))

(defn- decode-app-termo
  [prog bytes rest term]
  (fresh [symbol-index after-symbol sym args]
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (== (list 'app sym args) term)
    (sjas-object-symbol-indexo symbol-index sym)
    (decode-app-arityo prog 0 after-symbol rest args)))

(defn- decode-term-byteso
  "Parse one canonical SJAS term from a flat formula-code byte stream.

   The decoded term is an internal syntax tree used by the SJAS code predicates,
   not the public Proflog AST. Keeping this layer separate lets syntax
   recognition avoid inventing host noms merely to decide that a code is a
   well-formed formula."
  [prog bytes rest term]
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
    [(decode-app-termo prog bytes rest term)]
    [(decode-natural-termo bytes rest term)]
    [(decode-embedded-code-termo bytes rest term)]))

(defn- decode-formula-byteso
  "Parse one canonical formula from a flat SJAS formula-code byte stream."
  [prog bytes rest formula]
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
       (decode-term-byteso prog after-tag rest term)
       (== (list 'pos term) formula))]
    [(fresh [term after-tag]
       (== (lcons formula-neg-tag after-tag) bytes)
       (decode-term-byteso prog after-tag rest term)
       (== (list 'neg term) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-eq-tag after-tag) bytes)
       (decode-term-byteso prog after-tag after-left left)
       (decode-term-byteso prog after-left rest right)
       (== (list 'eq left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-neq-tag after-tag) bytes)
       (decode-term-byteso prog after-tag after-left left)
       (decode-term-byteso prog after-left rest right)
       (== (list 'neq left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-and-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right)
       (== (list 'and left right) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-or-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right)
       (== (list 'or left right) formula))]
    [(fresh [body after-tag]
       (== (lcons formula-not-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag rest body)
       (== (list 'not body) formula))]
    [(fresh [left right after-tag after-left]
       (== (lcons formula-implies-tag after-tag) bytes)
       (decode-formula-byteso prog after-tag after-left left)
       (decode-formula-byteso prog after-left rest right)
       (== (list 'implies left right) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body)
       (== (list 'forall idx body) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-once-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body)
       (== (list 'once-forall idx body) formula))]
    [(fresh [idx body after-idx]
       (== (lcons formula-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-formula-byteso prog after-idx rest body)
       (== (list 'exists idx body) formula))]
    [(fresh [idx bound body after-idx after-bound]
       (== (lcons formula-bounded-forall-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-term-byteso prog after-idx after-bound bound)
       (decode-formula-byteso prog after-bound rest body)
       (== (list 'bounded-forall idx bound body) formula))]
    [(fresh [idx bound body after-idx after-bound]
       (== (lcons formula-bounded-exists-tag
                   (lcons idx after-idx))
           bytes)
       (positive-byteo idx)
       (decode-term-byteso prog after-idx after-bound bound)
       (decode-formula-byteso prog after-bound rest body)
       (== (list 'bounded-exists idx bound body) formula))]))

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
  [arity after-symbol rest args]
  (if (= arity (dec sjas-code/byte-base))
    fail
    (conde
      [(fresh [arg-bytes]
         (== (lcons (inc arity) arg-bytes) after-symbol)
         (parse-syntax-term-list-byteso arity arg-bytes rest args))]
      [(decode-syntax-app-arityo (inc arity) after-symbol rest args)])))

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
    (decode-syntax-app-arityo 0 after-symbol rest args)))

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
  [arity after-symbol rest]
  (if (= arity (dec sjas-code/byte-base))
    fail
    (conde
      [(fresh [arg-bytes]
         (== (lcons (inc arity) arg-bytes) after-symbol)
         (skip-syntax-term-list-byteso arity arg-bytes rest))]
      [(skip-syntax-app-arityo (inc arity) after-symbol rest)])))

(defn- skip-syntax-app-termo
  [bytes rest]
  (fresh [symbol-index after-symbol]
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (positive-byteo symbol-index)
    (skip-syntax-app-arityo 0 after-symbol rest)))

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
    [(fresh [idx bound body]
       (== (list 'bounded-forall idx bound body) formula)
       (sjas-delta-star-0-formulao body))]
    [(fresh [idx bound body]
       (== (list 'bounded-exists idx bound body) formula)
       (sjas-delta-star-0-formulao body))]))

(defn- sjas-pi-star-1-formulao
  [formula]
  (fresh [idx body]
    (== (list 'forall idx body) formula)
    (conde
      [(sjas-delta-star-0-formulao body)]
      [(sjas-pi-star-1-formulao body)])))

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

(defn- sjas-internal-code-termo
  "Convert a decoded embedded code payload back to its public AST term."
  [bytes term]
  (or*
    (map (fn [byte-count]
           (fresh [constructor args args-proof]
             (byte-list-counto byte-count bytes)
             (code-constructoro constructor byte-count)
             (code-args-buildo args bytes args-proof)
             (== (lcons 'app (lcons constructor args)) term)))
         (range (inc sjas-code/max-code-bytes)))))

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
  (fresh [entry]
    (membero entry proof-symbol-index-entries)
    (== [idx sym] entry)))

(defn- proof-symbol-wide-indexo
  [high low sym]
  (fresh [entry]
    (membero entry proof-symbol-wide-index-entries)
    (== [high low sym] entry)))

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
    [(membero high positive-byte-entries)
     (membero low proof-byte-entries)]
    [(== 0 high)
     (membero low positive-byte-entries)]))

(defn- decrement-wide-proof-counto
  [high low high-out low-out]
  (conde
    [(membero low positive-byte-entries)
     (== high high-out)
     (proof-byte-decremento low low-out)]
    [(membero high positive-byte-entries)
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
  (fresh [kind rest]
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
  (fresh [kind rest]
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

(defn- sjas-system-profile-tago
  [profile-tag]
  (conde
    [(== system-profile-tableau0-tag profile-tag)]
    [(== system-profile-level1-tag profile-tag)]))

(defn- sjas-public-code-byteso
  "Expose public code bytes through the SJAS object-language code relation.

   Both compact `code-N` terms and U-Grounding numeral terms are read by
   `sjas-formal-code-byteso`. This keeps system, formula, proof, and
   substitution code reads inspectable in proof evidence instead of projecting
   already-ground Clojure terms to byte vectors outside the object relation."
  [code bytes proof]
  (fresh [kind read-proof sigma-out]
    (sjas-formal-code-byteso code bytes '() sigma-out kind read-proof)
    (== '() sigma-out)
    (== (list 'sjas-system-code-bytes read-proof) proof)))

(defn- sjas-public-code-bytes-coreo
  "Expose public code bytes without constructing auxiliary proof evidence."
  [code bytes]
  (fresh [kind sigma-out]
    (sjas-formal-code-bytes-coreo code bytes '() sigma-out kind)
    (== '() sigma-out)))

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
     (== (list 'code (list formula-false-tag)) contradiction-term)]
    [(fresh [encoded-system]
       (append-sentinel-byteo system-bytes encoded-system)
       (== (list 'num encoded-system) system-term)
       (== (list 'num
                 (list formula-false-tag sjas-code/u-grounding-sentinel-byte))
           contradiction-term))]))

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
                          'tableau-proof
                          (list system-term
                                contradiction-term
                                (list 'var 1)))))
        formula)
    (== '(sjas-system-tableau0-group-three-axiom) proof)))

(defn- sjas-tableau0-group-three-axiom-membero
  "Cite the Tableau-0 Group-3 axiom from system-code, not generated facts."
  [prog system-code formula-code proof]
  (fresh [system-bytes formula-bytes system-read-proof formula-read-proof
          header-proof formula group-three-proof]
    (sjas-public-code-byteso system-code system-bytes system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (sjas-tableau0-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tableau0-group-three-formulao system-bytes formula group-three-proof)
    (== (list 'sjas-system-group-three-axiom
              system-read-proof
              formula-read-proof
              header-proof
              group-three-proof)
        proof)))

(defn- sjas-level1-system-code-headero
  "Recognize the header of a Level-1 SJAS system code."
  [system-bytes proof]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-level1-tag rest))
        system-bytes)
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
        neg-pair (list 'neg
                       (list 'app 'neg-pair (list x-term y-term)))
        left-subst (list 'neg
                         (list 'app
                               'subst-prf
                               (list system-term
                                     substitution-term
                                     x-term
                                     p-term)))
        right-subst (list 'neg
                          (list 'app
                                'subst-prf
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
                                  neg-pair
                                  (list 'or left-subst right-subst))))))))

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

(defn- sjas-level1-group-three-axiom-membero
  "Cite the Level-1 Group-3 axiom by checking its fixed-point skeleton."
  [prog system-code formula-code proof]
  (fresh [system-bytes formula-bytes system-read-proof formula-read-proof
          header-proof formula group-three-proof]
    (sjas-public-code-byteso system-code system-bytes system-read-proof)
    (sjas-public-code-byteso formula-code formula-bytes formula-read-proof)
    (sjas-level1-system-code-headero system-bytes header-proof)
    (decode-formula-byteso prog formula-bytes '() formula)
    (level1-group-three-formulao prog system-bytes formula group-three-proof)
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
      (skip-syntax-formula-byteso bytes after-current)
      (conde
        [(byte-prefixo formula-bytes bytes after-current)
         (== '(sjas-system-beta-axiom) proof)]
        [(fresh [current-bytes]
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
  [prog profile-tag system-bytes formula]
  (conde
    [(fresh [decoded group-proof]
       (== system-profile-tableau0-tag profile-tag)
       (tableau0-group-three-formulao system-bytes decoded group-proof)
       (sjas-proof-antecedent-formula-asto decoded formula))]
    [(fresh [decoded group-proof]
       (== system-profile-level1-tag profile-tag)
       (level1-group-three-formulao prog system-bytes decoded group-proof)
       (sjas-proof-antecedent-formula-asto decoded formula))]))

(defn- sjas-system-proof-axiom-formulao
  [prog system-bytes axiom-formula]
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
                            system-bytes
                            group-three-formula)
                          (== (list group-three-formula) all-but-group3)
                          (formula-list-appendo beta-and-reflected
                                                all-but-group3
                                                all-formulas)
                          (formula-list-ando all-formulas axiom-formula)))
                      (range sjas-code/byte-base)))))
           (range sjas-code/byte-base)))))

(defn- sjas-system-axiom-formulao
  [prog system-code axiom-formula]
  (fresh [system-bytes read-proof]
    (sjas-public-code-byteso system-code system-bytes read-proof)
    (sjas-system-proof-axiom-formulao prog system-bytes axiom-formula)))

(defn- sjas-axiom-membero
  [prog system-code formula-code proof]
  (conde
    [(sjas-beta-axiom-membero prog system-code formula-code proof)]
    [(sjas-reflected-axiom-membero prog system-code formula-code proof)]
    [(sjas-fixed-axiom-membero prog system-code formula-code proof)]
    [(sjas-tableau0-group-three-axiom-membero prog system-code formula-code proof)]
    [(sjas-level1-group-three-axiom-membero prog system-code formula-code proof)]))

(defn- sjas-walked-axiom-membero
  "Check axiom membership after normalizing code terms through equality sigma.

   Relational `tableau-proof` and `subst-prf` calls may reach this point with
   `system-code` and `formula-code` bound in `sigma` rather than as immediately
   ground host values. The structural axiom decoders still consume code terms;
   this helper only performs the same equality walk that ordinary predicate
   dispatch uses before handing those terms to the decoders."
  [prog system-code formula-code sigma proof]
  (fresh [walked-system-code walked-formula-code]
    (equality/walk*o system-code sigma walked-system-code)
    (equality/walk*o formula-code sigma walked-formula-code)
    (sjas-axiom-membero prog walked-system-code walked-formula-code proof)))

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

(defn- sjas-fixed-axiom-member-coreo
  "Proof-free fixed axiom membership from decoded system and formula codes."
  [prog system-code formula-code]
  (fresh [system-bytes formula-bytes formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (sjas-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (sjas-fixed-axiom-formula-coreo formula)))

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
                          'tableau-proof
                          (list system-term
                                contradiction-term
                                (list 'var 1)))))
        formula)))

(defn- sjas-tableau0-group-three-axiom-member-coreo
  "Proof-free Tableau-0 Group-3 axiom membership."
  [prog system-code formula-code]
  (fresh [system-bytes formula-bytes formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (sjas-tableau0-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (tableau0-group-three-formula-coreo system-bytes formula)))

(defn- sjas-level1-system-code-header-coreo
  "Proof-free Level-1 system header recognizer."
  [system-bytes]
  (fresh [rest]
    (== (lcons system-code-tag
                (lcons system-profile-level1-tag rest))
        system-bytes)))

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
  (fresh [system-bytes formula-bytes formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-public-code-bytes-coreo formula-code formula-bytes)
    (sjas-level1-system-code-header-coreo system-bytes)
    (decode-formula-byteso prog formula-bytes '() formula)
    (level1-group-three-formula-coreo prog system-bytes formula)))

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
      (skip-syntax-formula-byteso bytes after-current)
      (conde
        [(byte-prefixo formula-bytes bytes after-current)]
        [(fresh [current-bytes]
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
    [(sjas-beta-axiom-member-coreo prog system-code formula-code)]
    [(sjas-reflected-axiom-member-coreo prog system-code formula-code)]
    [(sjas-fixed-axiom-member-coreo prog system-code formula-code)]
    [(sjas-tableau0-group-three-axiom-member-coreo prog system-code formula-code)]
    [(sjas-level1-group-three-axiom-member-coreo prog system-code formula-code)]))

(defn- sjas-walked-axiom-member-coreo
  "Proof-free axiom membership after equality walking."
  [prog system-code formula-code sigma]
  (fresh [walked-system-code walked-formula-code]
    (equality/walk*o system-code sigma walked-system-code)
    (equality/walk*o formula-code sigma walked-formula-code)
    (sjas-axiom-member-coreo prog walked-system-code walked-formula-code)))

(defn- sjas-system-group-three-proof-antecedent-coreo
  "Proof-free Group-3 antecedent reconstruction for `AxiomConj`."
  [prog profile-tag system-bytes formula]
  (conde
    [(fresh [decoded]
       (== system-profile-tableau0-tag profile-tag)
       (tableau0-group-three-formula-coreo system-bytes decoded)
       (sjas-proof-antecedent-formula-asto decoded formula))]
    [(fresh [decoded]
       (== system-profile-level1-tag profile-tag)
       (level1-group-three-formula-coreo prog system-bytes decoded)
       (sjas-proof-antecedent-formula-asto decoded formula))]))

(defn- sjas-system-proof-axiom-formula-coreo
  "Proof-free reconstruction of the finite axiom conjunction used by
   `tableau-proof/3` and `subst-prf/4`."
  [prog system-bytes axiom-formula]
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

(defn- sjas-system-code-valid-coreo
  "Proof-free recognizer for a complete finite SJAS system code.

   Some proof-predicate branches, such as the `Subst(g,t)` axiom of
   `subst-prf/4`, do not otherwise need the reconstructed axiom conjunction.
   They still must reject an invalid `system-code`, so this relation parses
   the complete finite system record without exposing auxiliary proof evidence."
  [prog system-code]
  (fresh [system-bytes axiom-formula]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog system-bytes axiom-formula)))

(defn- sjas-system-axiom-formula-coreo
  "Proof-free public-code entry for axiom-conjunction reconstruction."
  [prog system-code axiom-formula]
  (fresh [system-bytes]
    (sjas-public-code-bytes-coreo system-code system-bytes)
    (sjas-system-proof-axiom-formula-coreo prog system-bytes axiom-formula)))

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
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (sjas-normal-equalo left right sigma sigma-out eq-proof)
    (== neqs neqs-out)))

(defn- sjas-neq-close-structural-coreo
  [fml env sigma sigma-out neqs neqs-out]
  (fresh [lit left right]
    (subst/subst-formulao fml env lit)
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
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-holdso relation args sigma sigma-out relation-proof)
    (== neqs neqs-out)))

(defn- sjas-neg-relation-close-structural-coreo
  [fml env sigma sigma-out neqs neqs-out]
  (fresh [lit atom walked-atom relation args]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-holds-coreo relation args sigma sigma-out)
    (== neqs neqs-out)))

(defn- sjas-neg-relation-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [relation-proof]
    (sjas-neg-relation-close-coreo fml env sigma sigma-out neqs neqs-out relation-proof)
    (== (list 'profiled 'willard-sjas-arithmetic relation-proof) proof)))

(defn- sjas-axiom-member-walked-closeo
  "General axiom-member close path for branch-local environments and sigmas."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom relation args system-code formula-code axiom-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
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
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
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
    (subst/subst-formulao fml env lit)
    (== (list 'eq left right) lit)
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
       (== (lcons code '()) args)
       (sjas-wff-code-closeo prog code sigma sigma-out formula branch-proof)]
      [(== (lcons code '()) args)
       (sjas-class-relationo relation)
       (sjas-class-code-closeo prog relation code sigma sigma-out formula branch-proof)]
      [(== 'neg-pair relation)
       (== (lcons left (lcons right '())) args)
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
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
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
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'subst-code source-code substituted-code) walked-atom)
    (sjas-subst-code-any-coreo prog source-code substituted-code sigma sigma-out)
    (== neqs neqs-out)
    (== '(profiled willard-sjas-subst-code) proof)))

(declare sjas-proof-check-stateo
         sjas-tableau-proof-structural-closeo
         sjas-subst-prf-structural-closeo)

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

(defn- formula-bearing-proof-nodeo
  "Decode one formula-bearing tableau node from proof data.

   The node shape is `(byte-count byte... child...)`. It deliberately carries
   the formula bytes and child proof nodes, not a proof-rule tag. Local
   `Deduction` and `Closure` rules are inferred by the structural checker from
   the decoded formula and child list."
  [prog proof formula children]
  (conde
    [(fresh [formula-byte-proof formula-bytes decoded-formula]
       (== (lcons formula-byte-proof children) proof)
       (proof-byte-list-termo formula-byte-proof formula-bytes)
       (decode-proof-formula-byteso prog formula-bytes '() decoded-formula)
       (sjas-internal-formula-asto decoded-formula formula))]
    [(or*
       (map (fn [byte-count]
              (fresh [after-count formula-bytes decoded-formula]
                (== (lcons byte-count after-count) proof)
                (proof-byte-prefixo byte-count after-count formula-bytes children)
                (decode-proof-formula-byteso prog formula-bytes '() decoded-formula)
                (sjas-internal-formula-asto decoded-formula formula)))
            (range 1 sjas-code/byte-base)))]))

(declare sjas-atom-unify-coreo
         sjas-unify-termo-coreo
         sjas-unify-term*o-coreo
         sjas-eq-contradiction-coreo
         sjas-eq-contradiction-term*o-coreo)

(defn- sjas-complementary-lit-close-coreo
  [lit lits sigma sigma-out]
  (conde
    [(fresh [atom opposite]
       (== (list 'pos atom) lit)
       (membero (list 'neg opposite) lits)
       (sjas-atom-unify-coreo atom opposite sigma sigma-out))]
    [(fresh [atom opposite]
       (== (list 'neg atom) lit)
       (membero (list 'pos opposite) lits)
       (sjas-atom-unify-coreo atom opposite sigma sigma-out))]))

(defn- sjas-unify-termo-coreo
  "Proof-free term unification for structural SJAS tableau checks.

   The ordinary equality helper returns kernel proof trace constructors such as
   `eq-bind` and `decompose`. Formula-bearing SJAS tableau nodes should not
   carry or require that trace payload, so this relation preserves the same
   branch-state effect while exposing only `sigma` and `sigma-out`."
  [left right sigma sigma-out]
  (fresh [left-root right-root]
    (equality/walko left sigma left-root)
    (equality/walko right sigma right-root)
    (conde
      [(equality/same-termo left-root right-root sigma)
       (== sigma sigma-out)]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) left-root)
         (equality/absent-termo binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) right-root)
         (equality/absent-termo binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) left-root)
         (equality/absent-paro binding-nom right-root sigma)
         (== (lcons [binding-nom right-root] sigma) sigma-out))]
      [(fresh [binding-nom]
         (== (list 'par binding-nom) right-root)
         (equality/absent-paro binding-nom left-root sigma)
         (== (lcons [binding-nom left-root] sigma) sigma-out))]
      [(fresh [head left-args right-args]
         (== (lcons 'app (lcons head left-args)) left-root)
         (== (lcons 'app (lcons head right-args)) right-root)
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
    (== (lcons 'app (lcons head left-args)) left)
    (== (lcons 'app (lcons head right-args)) right)
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
    (equality/walko left sigma left-root)
    (equality/walko right sigma right-root)
    (conde
      [(fresh [binding-nom]
         (== (list 'var binding-nom) left-root)
         (equality/occurs-termo binding-nom right-root sigma))]
      [(fresh [binding-nom]
         (== (list 'var binding-nom) right-root)
         (equality/occurs-termo binding-nom left-root sigma))]
      [(fresh [left-head left-args right-head right-args]
         (== (lcons 'app (lcons left-head left-args)) left-root)
         (== (lcons 'app (lcons right-head right-args)) right-root)
         (!= left-head right-head))]
      [(fresh [head left-args right-args]
         (== (lcons 'app (lcons head left-args)) left-root)
         (== (lcons 'app (lcons head right-args)) right-root)
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
  (or*
    (map (fn [[idx nom]]
           (fresh []
             (branch-env-lengtho env (dec idx))
             (== nom next-nom)))
         code-nom-entries)))

(defn- sjas-proof-tree-next-fuelo
  "Preserve runtime fuel while validating a fixed SJAS proof tree.

   The formula-bearing proof predicate is a relation over decoded system,
   theorem, and proof codes. Its recursion is driven by child proof nodes, so
   accepting a fixed certificate must not depend on an external evaluator fuel
   counter."
  [fuel next-fuel]
  (== fuel next-fuel))

(defn- sjas-structural-proof-check-stateo
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
   prog gamma-terms fuel proof]
  (fresh [node-formula visible-formula children]
    (formula-bearing-proof-nodeo prog proof node-formula children)
    (subst/subst-formulao fml env visible-formula)
    (== visible-formula node-formula)
    (conde
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
      [(fresh [child next rest next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'false)) fml)
         (== (lcons next rest) unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
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
      [(fresh [left right child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'or left right)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  (list 'not left)
                                  (lcons (list 'not right) unexpanded)
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
      [(fresh [left right child next-fuel]
         (== (lcons child '()) children)
         (== (list 'not (list 'implies left right)) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  left
                                  (lcons (list 'not right) unexpanded)
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
      [(fresh [lit atom]
         (== '() children)
         (subst/subst-formulao fml env lit)
         (conde
           [(== (list 'pos atom) lit)]
           [(== (list 'neg atom) lit)])
         (sjas-complementary-lit-close-coreo lit lits sigma sigma-out)
         (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
      [(fresh [lit left right]
         (== '() children)
         (subst/subst-formulao fml env lit)
         (== (list 'neq left right) lit)
         (equality/same-termo left right sigma)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [lit left right child next rest next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (== (list 'neq left right) lit)
         (support/rigid-different-termo left right sigma)
         (== (lcons next rest) unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
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
      [(fresh [lit left right child next rest next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (== (list 'neq left right) lit)
         (== (lcons next rest) unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  lits
                                  env
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
           [(sjas-neq-close-structural-coreo fml env sigma sigma-out neqs neqs-out)]
           [(sjas-neg-relation-close-structural-coreo fml env sigma sigma-out neqs neqs-out)]))]
      [(fresh [lit left right]
         (== '() children)
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-eq-contradiction-coreo left right sigma)
         (== sigma sigma-out)
         (== neqs neqs-out))]
      [(fresh [lit left right sigma-mid]
         (== '() children)
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (sjas-neq-violated-coreo neqs sigma-mid)
         (== sigma-mid sigma-out)
         (support/prune-contradictory-neqso neqs sigma-mid neqs-out))]
      [(fresh [lit left right sigma-mid]
         (== '() children)
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (sjas-contradictory-atoms-coreo lits sigma-mid sigma-out)
         (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
      [(fresh [lit left right sigma-mid atom walked-atom relation args
               call-env body negated-body child next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (membero (list 'pos atom) lits)
         (equality/walk-atomo atom sigma-mid walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
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
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (membero (list 'neg atom) lits)
         (equality/walk-atomo atom sigma-mid walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
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
      [(fresh [lit left right sigma-mid child next rest next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (== (list 'eq left right) lit)
         (sjas-unify-termo-coreo left right sigma sigma-mid)
         (== (lcons next rest) unexpanded)
         (support/stable-neqso neqs sigma-mid)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
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
                                  child))]
      [(fresh [left right child next-fuel]
         (== (lcons child '()) children)
         (== (list 'and left right) fml)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  left
                                  (lcons right unexpanded)
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
      [(fresh [lit atom child next rest next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (conde
           [(== (list 'pos atom) lit)]
           [(== (list 'neg atom) lit)])
         (== (lcons next rest) unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
                                  (lcons lit lits)
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
      [(fresh [lit atom walked-atom relation args call-env body negated-body
               child next-fuel]
         (== (lcons child '()) children)
         (subst/subst-formulao fml env lit)
         (== (list 'pos atom) lit)
         (equality/walk-atomo atom sigma walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
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
         (subst/subst-formulao fml env lit)
         (== (list 'neg atom) lit)
         (equality/walk-atomo atom sigma walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
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
      [(fresh [child next rest next-fuel]
         (== (lcons child '()) children)
         (== (list 'true) fml)
         (== (lcons next rest) unexpanded)
         (sjas-proof-tree-next-fuelo fuel next-fuel)
         (sjas-proof-check-stateo system-code
                                  next
                                  rest
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
                                  child))])))

(defn- sjas-proof-check-stateo
  [system-code fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (fresh [selected remaining]
    (support/selecto selected (lcons fml unexpanded) remaining)
    (sjas-structural-proof-check-stateo system-code
                                        selected
                                        remaining
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
                                        proof)))

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

(defn- sjas-tableau-proof-coreo
  "Proof-free `tableau-proof/3` predicate relation.

   This is the object relation used both by the ordinary SJAS theory wrapper
   and by formula-bearing structural tableau leaves. It decodes the supplied
   proof code, theorem code, and finite system code, then validates either an
   axiom citation or a structural tableau tree without constructing a separate
   Proflog answer-proof payload."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [lit atom walked-atom system-code theorem-code proof-code
          decoded-proof proof-bytes axiom-formula neg-theorem
          target sigma-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'tableau-proof system-code theorem-code proof-code) walked-atom)
    (conde
      [(decode-sjas-axiom-proof-code-coreo proof-code
                                           sigma
                                           sigma-proof
                                           proof-bytes)
       (== 'sjas-axiom decoded-proof)
       (fresh []
         (sjas-walked-axiom-member-coreo prog
                                          system-code
                                          theorem-code
                                          sigma-proof)
         (== sigma-proof sigma-out))]
      [(decode-non-sjas-axiom-proof-code-coreo proof-code
                                               sigma
                                               sigma-proof
                                               proof-bytes
                                               decoded-proof)
       (sjas-structural-negated-theorem-coreo prog
                                              theorem-code
                                              sigma-proof
                                              sigma-out
                                              neg-theorem)
       (sjas-system-axiom-formula-coreo prog system-code axiom-formula)
       (== (list 'and axiom-formula neg-theorem) target)
       (sjas-proof-check-programo prog
                                  system-code
                                  target
                                  fuel
                                  decoded-proof)])
    (== neqs neqs-out)))

(defn- sjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh []
    (sjas-tableau-proof-coreo fml
                              env
                              sigma
                              sigma-out
                              neqs
                              neqs-out
                              prog
                              fuel)
    (== '(profiled willard-sjas-proof-check) proof)))

(defn- sjas-subst-prf-coreo
  "Proof-free `subst-prf/4` predicate relation.

   `SubstPrf` first validates the substitution-side formula relation, then
   checks the supplied theorem proof against beta plus the substituted source
   sentence. The relation intentionally returns only branch state effects; the
   ordinary public answer marker is layered on by `sjas-subst-prf-closeo`."
  [fml env sigma sigma-out neqs neqs-out prog fuel]
  (fresh [lit atom walked-atom system-code substitution-code theorem-code proof-code
          decoded-proof proof-bytes axiom-formula subst-axiom-formula
          extended-axiom-formula neg-theorem target sigma-valid sigma-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'subst-prf system-code substitution-code theorem-code proof-code)
        walked-atom)
    (conde
      [(decode-sjas-axiom-proof-code-coreo proof-code
                                           sigma
                                           sigma-proof
                                           proof-bytes)
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
            (sjas-system-code-valid-coreo prog system-code)
            (sjas-subst-code-any-coreo prog substitution-code theorem-code sigma-proof sigma-out))])]
      [(decode-non-sjas-axiom-proof-code-coreo proof-code
                                               sigma
                                               sigma-proof
                                               proof-bytes
                                               decoded-proof)
       (sjas-system-axiom-formula-coreo prog system-code axiom-formula)
       (sjas-subst-source-result-antecedent-coreo prog
                                                   substitution-code
                                                   sigma-proof
                                                   sigma-valid
                                                   subst-axiom-formula)
       (sjas-structural-negated-theorem-coreo prog
                                               theorem-code
                                               sigma-valid
                                               sigma-out
                                               neg-theorem)
       (== (list 'and axiom-formula subst-axiom-formula)
           extended-axiom-formula)
       (== (list 'and extended-axiom-formula neg-theorem) target)
       (sjas-proof-check-programo prog
                                  system-code
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

(defn willard-sjas-theory-closeo
  "SJAS theory branch rule bound into the ordinary proof kernel."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (conde
    [(sjas-eq-progresso fml unexpanded lits env proof-vars sigma sigma-out
                        neqs neqs-out prog gamma-terms fuel proof)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
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
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
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
