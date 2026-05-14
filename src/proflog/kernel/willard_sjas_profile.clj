(ns proflog.kernel.willard-sjas-profile
  "Kernel-interleaved Willard SJAS profile.

   ADR-0061 promotes the ADR-0060 scaffold in two ways:

   - U-grounding arithmetic is interpreted as relations over binary numerals
     whose object-language constants are `0` and `1`;
   - `tableau-proof/3` checks a structural proof certificate by running the
     existing Proflog kernel with the decoded proof term already supplied;
   - `subst-prf/4` exposes the Level-1 substitution-proof vocabulary while
     currently consulting the finite substitution boundary generated for the
     active `IS#_D(beta)` system.

   The profile therefore remains a tableau extension, not a host-side evaluator:
   arithmetic constraints and proof checking are both miniKanren goals
   interleaved at the branch rule boundary."
  (:refer-clojure :exclude [== < <=])
  (:require [clojure.core.logic :refer [== conde fail fresh lcons membero or* run]]
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
       (bits->canonical-termo tail tail-term tail-proof)
       (== (list 'app 'dbl tail-term) term)
       (== (list 'sjas-num-dbl tail-proof) proof))]
    [(fresh [tail tail-term tail-proof]
       (== (lcons 1 tail) bits)
       (arith/poso tail)
       (bits->canonical-termo tail tail-term tail-proof)
       (== (list 'app 'add (list 'app 'dbl tail-term) one-term) term)
       (== (list 'sjas-num-add-one tail-proof) proof))]))

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

;; -----------------------------------------------------------------------------
;; Arithmetic code decoding
;; -----------------------------------------------------------------------------

(def ^:private byte-base-bits (arith/build-num sjas-code/byte-base))

(def ^:private byte-bit-entries
  (apply list
         (map (fn [byte]
                [(arith/build-num byte) byte])
              (range sjas-code/byte-base))))

(def ^:private code-byte-term-entries
  (apply list
         (map (fn [byte]
                [(nth sjas-code/byte-terms byte) byte])
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

(defn- byte-bitso
  [bits byte]
  (fresh [entry]
    (membero entry byte-bit-entries)
    (== [bits byte] entry)))

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

(defn- code-byte-termo
  [term byte]
  (fresh [entry]
    (membero entry code-byte-term-entries)
    (== [term byte] entry)))

(defn- code-constructoro
  [constructor byte-count]
  (fresh [entry]
    (membero entry code-constructor-entries)
    (== [constructor byte-count] entry)))

(defn- code-argso
  [args bytes]
  (conde
    [(== '() args)
     (== '() bytes)]
    [(fresh [arg rest byte byte-rest]
       (== (lcons arg rest) args)
       (code-byte-termo arg byte)
       (== (lcons byte byte-rest) bytes)
       (code-argso rest byte-rest))]))

(defn- sjas-code-byteso
  "Decode an object-language SJAS code term into base-64 bytes.

   Codes are first-order terms of the shape `(code-N b0 ... bN-1)`, where each
   byte is itself a small public binary numeral. This keeps Godel codes visible
   to the object language without forcing proof search to walk a huge nested
   binary numeral for every sentence and proof certificate."
  [term bytes sigma sigma-out proof]
  (fresh [walked constructor args byte-count]
    (equality/walko term sigma walked)
    (== (lcons 'app (lcons constructor args)) walked)
    (code-constructoro constructor byte-count)
    (code-argso args bytes)
    (== sigma sigma-out)
    (== '(sjas-code-bytes) proof)))

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

(declare decode-proof-byteso)

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

(defn- decode-proof-list-with-counto
  [bytes rest proof]
  (or*
    (map (fn [count]
           (fresh [after-count]
             (== (lcons sjas-code/proof-list-tag
                         (lcons (inc count) after-count))
                 bytes)
             (parse-proof-items count after-count rest proof)))
         (range 1 63))))

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
    [(decode-proof-list-with-counto bytes rest proof)]))

(defn- decode-proof-codeo
  [code sigma sigma-out proof-bytes proof proof-read-proof]
  (fresh [rest]
    (sjas-code-byteso code proof-bytes sigma sigma-out proof-read-proof)
    (decode-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- sjas-formula-codeo
  [prog code formula]
  (fresh [entry]
    (membero entry (or (:sjas/formula-entries (or (some-> prog :sjas/registry deref)
                                                   prog))
                       '()))
    (== [code formula] entry)))

(defn- sjas-formula-negationo
  [prog formula negated]
  (fresh [entry]
    (membero entry (or (:sjas/formula-negation-entries (or (some-> prog :sjas/registry deref)
                                                            prog))
                       '()))
    (== [formula negated] entry)))

(defn- sjas-formula-classo
  [prog relation code]
  (fresh [entry]
    (membero entry (or (:sjas/formula-class-entries (or (some-> prog :sjas/registry deref)
                                                         prog))
                       '()))
    (== [relation code] entry)))

(defn- sjas-neg-pairo
  [prog left right]
  (fresh [entry]
    (membero entry (or (:sjas/neg-pair-entries (or (some-> prog :sjas/registry deref)
                                                    prog))
                       '()))
    (== [left right] entry)))

(defn- sjas-system-axiom-formulao
  [prog system-code axiom-formula]
  (fresh [entry]
    (membero entry (or (:sjas/system-entries (or (some-> prog :sjas/registry deref)
                                                  prog))
                       '()))
    (== [system-code axiom-formula] entry)))

(defn- sjas-axiom-membero
  "Relate a reflected system code to one of its generated axiom formula codes.

   The proof predicate uses this for formal axiom-citation certificates. This
   is still an object-language check: it consumes the same generated
   `axiom-member/2` facts that ordinary SJAS queries can inspect."
  [prog system-code formula-code]
  (fresh [fact]
    (membero fact (or (:sjas/fact-atoms (or (some-> prog :sjas/registry deref)
                                             prog))
                      '()))
    (== (list 'app 'axiom-member system-code formula-code) fact)))

(defn- sjas-active-systemo
  [prog system-code]
  (== system-code (:sjas/system-code (or (some-> prog :sjas/registry deref)
                                         prog))))

(defn- sjas-subst-codeo
  [prog source-code substituted-code]
  (fresh [entry]
    (membero entry (or (:sjas/subst-code-entries (or (some-> prog :sjas/registry deref)
                                                      prog))
                       '()))
    (== [source-code substituted-code] entry)))

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

;; -----------------------------------------------------------------------------
;; Branch closing rules
;; -----------------------------------------------------------------------------

(defn- sjas-neq-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [lit left right eq-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neq left right) lit)
    (sjas-normal-equalo left right sigma sigma-out eq-proof)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-arithmetic eq-proof) proof)))

(defn- sjas-neg-relation-closeo
  [fml env sigma sigma-out neqs neqs-out proof]
  (fresh [lit atom walked-atom relation args relation-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
    (sjas-relation-holdso relation args sigma sigma-out relation-proof)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-arithmetic relation-proof) proof)))

(defn- sjas-generated-fact-closeo
  "Close negated generated coding facts directly from reflected system metadata."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom relation args fact]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
    (== 'axiom-member relation)
    (membero fact (or (:sjas/fact-atoms (or (some-> prog :sjas/registry deref)
                                             prog))
                      '()))
    (== walked-atom fact)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-fact fact) proof)))

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

(defn- sjas-syntax-code-closeo
  "Close generated syntax-code predicates by decoding formula Godel-code terms.

   These predicates are no longer emitted as generated facts. The finite entries
   here are the system's decode relation: they connect an arithmetic code term
   to the formula or class information obtained from that code."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom relation args code left right formula]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (lcons 'app (lcons relation args)) walked-atom)
    (conde
      [(== 'wff relation)
       (== (lcons code '()) args)
       (sjas-formula-codeo prog code formula)]
      [(== (lcons code '()) args)
       (sjas-class-relationo relation)
       (sjas-formula-classo prog relation code)]
      [(== 'neg-pair relation)
       (== (lcons left (lcons right '())) args)
       (sjas-neg-pairo prog left right)])
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-code relation) proof)))

(defn- sjas-subst-code-closeo
  "Close finite generated `subst-code/2` facts.

   ADR-0066 separates Willard's `Subst(g,h)` relation from `SubstPrf(g,t,p)`.
   This branch rule exposes the finite generated substitution table at the
   object-language predicate boundary."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (fresh [lit atom walked-atom source-code substituted-code]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'subst-code source-code substituted-code) walked-atom)
    (sjas-subst-codeo prog source-code substituted-code)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== '(profiled willard-sjas-subst-code) proof)))

(defn- sjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh [lit atom walked-atom system-code theorem-code proof-code
          decoded-proof proof-bytes axiom-formula theorem-formula neg-theorem
          target sigma-proof proof-read-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'tableau-proof system-code theorem-code proof-code) walked-atom)
    (decode-proof-codeo proof-code sigma sigma-proof proof-bytes decoded-proof proof-read-proof)
    (conde
      [(== 'sjas-axiom decoded-proof)
       (sjas-axiom-membero prog system-code theorem-code)]
      [(sjas-system-axiom-formulao prog system-code axiom-formula)
       (sjas-formula-codeo prog theorem-code theorem-formula)
       (sjas-formula-negationo prog theorem-formula neg-theorem)
       (== (list 'and axiom-formula neg-theorem) target)
       (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)])
    (== sigma-proof sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-proof-check proof-read-proof decoded-proof) proof)))

(defn- sjas-subst-prf-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh [lit atom walked-atom system-code substitution-code theorem-code proof-code
          substituted-code decoded-proof proof-bytes axiom-formula theorem-formula
          substituted-formula neg-theorem target sigma-proof proof-read-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'subst-prf system-code substitution-code theorem-code proof-code)
        walked-atom)
    (sjas-active-systemo prog system-code)
    (sjas-subst-codeo prog substitution-code substituted-code)
    (decode-proof-codeo proof-code sigma sigma-proof proof-bytes decoded-proof proof-read-proof)
    (conde
      [(== 'sjas-axiom decoded-proof)
       (conde
         [(sjas-axiom-membero prog system-code theorem-code)]
         [(== theorem-code substituted-code)])]
      [(sjas-system-axiom-formulao prog system-code axiom-formula)
       (sjas-formula-codeo prog theorem-code theorem-formula)
       (sjas-formula-negationo prog theorem-formula neg-theorem)
       (== (list 'and axiom-formula neg-theorem) target)
       (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)]
      [(sjas-system-axiom-formulao prog system-code axiom-formula)
       (sjas-formula-codeo prog theorem-code theorem-formula)
       (sjas-formula-codeo prog substituted-code substituted-formula)
       (sjas-formula-negationo prog theorem-formula neg-theorem)
       (== (list 'and (list 'and axiom-formula substituted-formula) neg-theorem)
           target)
       (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)])
    (== sigma-proof sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-subst-proof-check
              proof-read-proof
              decoded-proof)
        proof)))

(defn willard-sjas-theory-closeo
  "SJAS theory branch rule bound into the ordinary proof kernel."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (conde
    [(sjas-eq-progresso fml unexpanded lits env proof-vars sigma sigma-out
                        neqs neqs-out prog gamma-terms fuel proof)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-syntax-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-subst-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-generated-fact-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]))

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
    [(sjas-syntax-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-subst-code-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-generated-fact-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
     (== residuals residuals-out)]
    [(sjas-subst-prf-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
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
