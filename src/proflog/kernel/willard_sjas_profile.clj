(ns proflog.kernel.willard-sjas-profile
  "Kernel-interleaved Willard SJAS profile.

   ADR-0061 promotes the ADR-0060 scaffold in two ways:

   - U-grounding arithmetic is interpreted as relations over binary numerals
     whose object-language constants are `0` and `1`;
   - `tableau-proof/3` checks a structural proof certificate by running the
     existing Proflog kernel with the decoded proof term already supplied;
   - `subst-prf/4` exposes the Level-1 substitution-proof vocabulary by
     decoding formula codes and checking diagonal substitution structurally.

   The profile therefore remains a tableau extension, not a host-side evaluator:
   arithmetic constraints and proof checking are both miniKanren goals
   interleaved at the branch rule boundary."
  (:refer-clojure :exclude [== < <=])
  (:require [clojure.core.logic :refer [!= == conda conde fail fresh lcons membero or* run]]
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

(defn- ground-pop-bit
  "Return `[bit tail-term]` for one ground canonical U-Grounding numeral step.

   This helper inspects only the root constructor and its immediate children.
   It deliberately does not compute the numeral's value; the recursive decoder
   still consumes the object-language `dbl`/`add` structure one bit at a time."
  [term]
  (cond
    (= zero-term term)
    [0 zero-term]

    (= one-term term)
    [1 zero-term]

    (and (seq? term)
         (= 'app (first term))
         (= 'dbl (second term)))
    [0 (nth term 2)]

    (and (seq? term)
         (= 'app (first term))
         (= 'add (second term))
         (= one-term (nth term 3 nil)))
    (let [doubled (nth term 2 nil)]
      (when (and (seq? doubled)
                 (= 'app (first doubled))
                 (= 'dbl (second doubled)))
        [1 (nth doubled 2)]))

    :else
    nil))

(defn- ground-pop-byte
  "Host-stack-safe byte popper for already-ground public code terms.

   This is an operational projector for the already-ground public code term. It
   follows the same six root-constructor pops as the relation above; it does
   not use generated code constructors, hashes, or a total object-language
   multiplication function."
  [term]
  (loop [remaining 6
         current term
         byte 0
         bit-index 0]
    (if (zero? remaining)
      [byte current]
      (when-let [[bit tail] (ground-pop-bit current)]
        (recur (dec remaining)
               tail
               (if (zero? bit)
                 byte
                 (bit-set byte bit-index))
               (inc bit-index))))))

(defn- ground-u-grounding-code-term-bytes
  "Decode a ground public U-Grounding code term by repeated byte pops.

   The sentinel byte bounds the loop. An ill-formed term returns nil rather than
   synthesizing bytes."
  [term]
  (loop [current term
         out []
         steps 0]
    (when (clojure.core/<= steps sjas-code/max-code-bytes)
      (when-let [[byte tail] (ground-pop-byte current)]
        (if (and (= sjas-code/u-grounding-sentinel-byte byte)
                 (= zero-term tail))
          out
          (recur tail
                 (conj out byte)
                 (inc steps)))))))

(defn- ground-formal-code-term
  "Return `[bytes kind read-proof]` for an already-ground public code term.

   This is a deterministic entry shortcut, not a theorem decision procedure. It
   keeps large U-Grounding numerals from first becoming a deep logic binding
   that core.logic then re-walks during every formula-parser unification. The
   formula, proof, and substitution predicates still consume the resulting byte
   stream through their ordinary structural relations."
  [term]
  (if-let [bytes (ground-u-grounding-code-term-bytes term)]
    [bytes :u-grounding '(sjas-ug-code-bytes)]
    (when-let [bytes (sjas-code/code-term-bytes term)]
      [bytes :compact '(sjas-code-bytes)])))

(defn- byte-list-literal
  "Return a proper Clojure list for byte literals passed to core.logic goals."
  [bytes]
  (apply list bytes))

(defn- u-grounding-byte-list-literal
  "Return the sentinel-terminated byte list for a U-Grounding numeral code."
  [bytes]
  (byte-list-literal (conj (vec bytes) sjas-code/u-grounding-sentinel-byte)))

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

(defn- sjas-ug-code-byteso
  "Decode an object-language U-Grounding numeral code into base-64 bytes.

   Ground public codes use a deterministic constructor-pop shortcut to avoid
   overflowing core.logic on very large numeral towers. Bound or otherwise
   relational entries read the canonical numeral to bits and then strip the
   sentinel through `sjas-ug-code-bytes-bitso`, which reconstructs each byte
   with the relation-backed `byte + 64 * tail` equation."
  [term bytes sigma sigma-out proof]
  (if-let [ground-bytes (ground-u-grounding-code-term-bytes term)]
    (fresh []
      (== '() sigma)
      (== '() sigma-out)
      (== (apply list ground-bytes) bytes)
      (== '(sjas-ug-code-bytes) proof))
    (conde
      [(fresh [bits read-proof decode-proof]
         (sjas-canonical-num-termo term bits sigma sigma-out read-proof)
         (sjas-ug-code-bytes-bitso bits bytes decode-proof)
         (== (list 'sjas-ug-code-bytes read-proof decode-proof) proof))])))

(defn- sjas-formal-code-byteso
  "Decode either supported public SJAS code representation.

   `kind` is `:compact` for `code-N` terms and `:u-grounding` for the ADR-0071
   binary numeral representation. Callers that need to know how a code should
   be quoted during diagonal substitution inspect this value."
  [term bytes sigma sigma-out kind proof]
  (conde
    [(== :u-grounding kind)
     (sjas-ug-code-byteso term bytes sigma sigma-out proof)]
    [(== :compact kind)
     (sjas-code-byteso term bytes sigma sigma-out proof)]))

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

(def ^:private positive-byte-entries
  (apply list (range 1 sjas-code/byte-base)))

(def ^:private positive-byte-except-one-entries
  (apply list (range 2 sjas-code/byte-base)))

(def ^:private code-nom-entries
  "Shared code-level noms used when decoded formula-code variables become ASTs."
  sjas-code/code-nom-entries)

(defn- positive-byteo
  [byte]
  (membero byte positive-byte-entries))

(defn- positive-byte-except-oneo
  [byte]
  (membero byte positive-byte-except-one-entries))

(defn- sjas-symbol-indexo
  "Relate a formula-code symbol index to a declared object-language symbol."
  [prog idx sym]
  (fresh [entry]
    (membero entry (or (:sjas/symbol-index-entries
                         (or (some-> prog :sjas/registry deref) prog))
                       '()))
    (== [idx sym] entry)))

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

(defn- decode-app-termo
  [prog bytes rest term]
  (fresh [symbol-index after-symbol sym]
    (== (lcons term-app-tag
                (lcons symbol-index after-symbol))
        bytes)
    (sjas-symbol-indexo prog symbol-index sym)
    (or*
      (map (fn [arity]
             (fresh [arg-bytes args]
               (== (lcons (inc arity) arg-bytes) after-symbol)
               (parse-term-list-byteso prog arity arg-bytes rest args)
               (== (list 'app sym args) term)))
           (range 63)))))

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

(defn- sjas-decode-formula-code-proofo
  [prog code sigma sigma-out formula read-proof]
  (if-let [[bytes _kind ground-read-proof] (ground-formal-code-term code)]
    (fresh [rest]
      (== '() sigma)
      (== '() sigma-out)
      (== ground-read-proof read-proof)
      (decode-formula-byteso prog (byte-list-literal bytes) rest formula)
      (== '() rest))
    (fresh [bytes rest kind]
      (sjas-formal-code-byteso code bytes sigma sigma-out kind read-proof)
      (decode-formula-byteso prog bytes rest formula)
      (== '() rest))))

(defn- sjas-decode-formula-codeo
  [prog code sigma sigma-out formula]
  (fresh [read-proof]
    (sjas-decode-formula-code-proofo prog code sigma sigma-out formula read-proof)))

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
         sjas-subst-formula-var-oneo)

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
    [(fresh [bytes]
       (== (list 'num bytes) term)
       (== term substituted))]
    [(fresh [bytes]
       (== (list 'code bytes) term)
       (== term substituted))]))

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
    [(fresh [bytes]
       (== (list 'num bytes) left)
       (== (list 'num bytes) right))]
    [(fresh [bytes]
       (== (list 'code bytes) left)
       (== (list 'code bytes) right))]))

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
           (fresh [constructor args]
             (byte-list-counto byte-count bytes)
             (code-constructoro constructor byte-count)
             (code-argso args bytes)
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
   merely because a code was generated by the current system builder."
  [prog theorem-code sigma sigma-out neg-theorem theorem-read-proof]
  (fresh [formula complement read-proof]
    (sjas-decode-formula-code-proofo prog theorem-code sigma sigma-out formula read-proof)
    (sjas-formula-complemento formula complement)
    (sjas-internal-formula-asto complement neg-theorem)
    (== (list 'willard-sjas-theorem-code read-proof) theorem-read-proof)))

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
  (fresh [rest kind]
    (sjas-formal-code-byteso code proof-bytes sigma sigma-out kind proof-read-proof)
    (decode-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- decode-proof-code-kindo
  [code sigma sigma-out proof-bytes proof kind]
  (fresh [rest read-proof]
    (sjas-formal-code-byteso code proof-bytes sigma sigma-out kind read-proof)
    (decode-proof-byteso proof-bytes rest proof)
    (== '() rest)))

(defn- code-read-marker-o
  [kind proof]
  (conde
    [(== :u-grounding kind)
     (== '(sjas-ug-code-bytes) proof)]
    [(== :compact kind)
     (== '(sjas-code-bytes) proof)]))

(defn- sjas-system-profile-tago
  [profile-tag]
  (conde
    [(== system-profile-tableau0-tag profile-tag)]
    [(== system-profile-level1-tag profile-tag)]))

(defn- sjas-ground-code-byteso
  "Expose the already-ground byte string for a public code term.

   This remains a host-side operational boundary and is not the final ADR-0072
   object-level reader. The value of this helper in this slice is narrower: it
   lets axiom membership stop consulting generated `axiom-member/2` facts for
   user beta axioms and instead consume the beta formula bytes encoded in the
   system code."
  [code bytes proof]
  (if-let [[ground-bytes _kind ground-read-proof] (ground-formal-code-term code)]
    (fresh []
      (== (byte-list-literal ground-bytes) bytes)
      (== (list 'sjas-system-code-bytes ground-read-proof) proof))
    fail))

(defn- byte-prefixo
  [prefix bytes rest]
  (conde
    [(== '() prefix)
     (== bytes rest)]
    [(fresh [head prefix-tail bytes-tail]
       (== (lcons head prefix-tail) prefix)
       (== (lcons head bytes-tail) bytes)
       (byte-prefixo prefix-tail bytes-tail rest))]))

(defn- sjas-beta-member-in-formula-byteso
  [prog remaining bytes formula-bytes proof]
  (if (zero? remaining)
    fail
    (fresh [current after-current after-prefix]
      (decode-formula-byteso prog bytes after-current current)
      (conde
        [(byte-prefixo formula-bytes bytes after-prefix)
         (== after-prefix after-current)
         (== '(sjas-system-beta-axiom) proof)]
        [(sjas-beta-member-in-formula-byteso prog
                                             (dec remaining)
                                             after-current
                                             formula-bytes
                                             proof)]))))

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
    (sjas-ground-code-byteso system-code system-bytes system-read-proof)
    (sjas-ground-code-byteso formula-code formula-bytes formula-read-proof)
    (sjas-system-beta-formula-byteso prog system-bytes formula-bytes beta-proof)
    (== (list 'sjas-system-beta-axiom
              system-read-proof
              formula-read-proof
              beta-proof)
        proof)))

(defn- sjas-system-axiom-formulao
  [prog system-code axiom-formula]
  (fresh [entry]
    (membero entry (or (:sjas/system-entries (or (some-> prog :sjas/registry deref)
                                                  prog))
                       '()))
    (== [system-code axiom-formula] entry)))

(defn- sjas-generated-axiom-membero
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

(defn- sjas-axiom-membero
  [prog system-code formula-code proof]
  (conda
    [(sjas-beta-axiom-membero prog system-code formula-code proof)]
    [(sjas-generated-axiom-membero prog system-code formula-code)
     (== '(sjas-generated-axiom-member) proof)]))

(defn- sjas-active-systemo
  [prog system-code]
  (== system-code (:sjas/system-code (or (some-> prog :sjas/registry deref)
                                         prog))))

(defn- sjas-code-format
  "Return the public code representation selected by the active SJAS system.

   This is profile metadata, not an object-language fact. It is used only to
   choose a search order. U-Grounding code terms are deep binary numerals, so
   trying generated-code table lookup first can force core.logic to compare
   large numeral towers against every registry entry before the structural
   decoder has a chance to run."
  [prog]
  (or (:sjas/code-format (or (some-> prog :sjas/registry deref)
                             prog))
      :compact))

(defn- sjas-subst-code-anyo
  "Relate formula codes by structural diagonal substitution.

   The first code is decoded as a formula `F`; `F` is then used as a quoted code
   term and substituted for free canonical variable `v0` inside `F`. The second
   code must decode to a formula alpha-equivalent to the resulting formula. This
   is the object-language `Subst` operation needed by Level-1 SJAS
   self-reference; it is no longer a finite table of precomputed examples."
  [prog source-code substituted-code sigma sigma-out]
  (if-let [[source-bytes source-kind _source-read-proof]
           (ground-formal-code-term source-code)]
    (let [source-byte-list (byte-list-literal source-bytes)
          replacement (case source-kind
                        :compact (list 'code source-byte-list)
                        :u-grounding (list 'num (u-grounding-byte-list-literal
                                                  source-bytes)))]
      (fresh [source-formula substituted-formula expected]
        (== '() sigma)
        (decode-formula-byteso prog source-byte-list '() source-formula)
        (sjas-subst-formula-var-oneo source-formula
                                     replacement
                                     expected)
        (sjas-decode-formula-codeo prog substituted-code
                                   '()
                                   sigma-out
                                   substituted-formula)
        (sjas-alpha-formula-equivo expected substituted-formula '())))
    (fresh [source-bytes source-formula substituted-formula expected replacement
            source-kind source-read-proof sigma-after-source]
      (sjas-formal-code-byteso source-code
                               source-bytes
                               sigma
                               sigma-after-source
                               source-kind
                               source-read-proof)
      (decode-formula-byteso prog source-bytes '() source-formula)
      (conde
        [(== :compact source-kind)
         (== (list 'code source-bytes) replacement)]
        [(== :u-grounding source-kind)
         (fresh [encoded-source-bytes]
           (append-sentinel-byteo source-bytes encoded-source-bytes)
           (== (list 'num encoded-source-bytes) replacement))])
      (sjas-subst-formula-var-oneo source-formula
                                   replacement
                                   expected)
      (sjas-decode-formula-codeo prog substituted-code
                                 sigma-after-source
                                 sigma-out
                                 substituted-formula)
      (sjas-alpha-formula-equivo expected substituted-formula '()))))

(defn- sjas-subst-source-codeo
  "Check that a substitution source code is a well-formed formula code.

   `subst-prf/4` sometimes needs only the existence of a substituted formula,
   not the public code for that formula. Generating a fresh public code term is
   an expensive synthesis problem. Since structural substitution is total on
   decoded formula syntax, proof checking uses this source-only relation unless
   it must compare against a concrete theorem code."
  [prog source-code sigma sigma-out]
  (if-let [[source-bytes _source-kind _source-read-proof]
           (ground-formal-code-term source-code)]
    (fresh [source-formula]
      (== '() sigma)
      (== '() sigma-out)
      (decode-formula-byteso prog
                             (byte-list-literal source-bytes)
                             '()
                             source-formula))
    (fresh [source-bytes source-formula source-read-proof source-kind]
      (sjas-formal-code-byteso source-code
                               source-bytes
                               sigma
                               sigma-out
                               source-kind
                               source-read-proof)
      (decode-formula-byteso prog source-bytes '() source-formula))))

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

(defn- ground-negated-subst-code-args
  "Extract ground `subst-code/2` arguments before relation-level walking.

   Querying `subst-code` over ADR-0071 U-Grounding codes places large binary
   numerals directly inside the branch literal. When no binder environment or
   equality substitution is active, destructuring that host-ground atom is
   equivalent to the generic `subst-formulao`/`walk-atomo` prefix but avoids
   forcing core.logic to occurs-check the large public code terms."
  [fml]
  (when (and (seq? fml)
             (= 'neg (first fml)))
    (let [atom (second fml)]
      (when (and (seq? atom)
                 (= 'app (first atom))
                 (= 'subst-code (second atom))
                 (= 4 (count atom)))
        [(nth atom 2) (nth atom 3)]))))

(defn- ground-negated-app-args
  "Extract arguments from a host-ground negated relation atom."
  [fml relation arity]
  (when (and (seq? fml)
             (= 'neg (first fml)))
    (let [atom (second fml)]
      (when (and (seq? atom)
                 (= 'app (first atom))
                 (= relation (second atom))
                 (= (+ 2 arity) (count atom)))
        (vec (drop 2 atom))))))

(defn- ground-negated-relation
  "Return the relation symbol of a host-ground negated atomic formula."
  [fml]
  (when (and (seq? fml)
             (= 'neg (first fml)))
    (let [atom (second fml)]
      (when (and (seq? atom)
                 (= 'app (first atom))
                 (symbol? (second atom)))
        (second atom)))))

(def ^:private direct-negated-profile-relations
  "Top-level SJAS atom relations that must bypass generic agenda selection.

   Keep this set narrow. Syntax predicates over moderate formula codes already
   work through the ordinary kernel path, while large `subst-code` and
   `tableau-proof` atoms need direct focus to avoid occurs-checking their code
   numerals during agenda selection."
  '#{wff
     delta-star-0-code
     pi-star-1-code
     sigma-star-1-code
     neg-pair
     subst-code
     tableau-proof})

(defn- direct-negated-profile-relation
  [fml]
  (let [relation (ground-negated-relation fml)]
    (when (contains? direct-negated-profile-relations relation)
      relation)))

(def ^:private syntax-code-relations
  '#{wff delta-star-0-code pi-star-1-code sigma-star-1-code neg-pair})

(defn- syntax-code-relation?
  [relation]
  (contains? syntax-code-relations relation))

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

(defn- sjas-wff-code-closeo
  "Close `wff(code)` by reading the object-level formula code.

   Earlier SJAS stages generated a finite host lookup table for formulas known
   when the system was compiled. ADR-0072 removes that shortcut: the predicate
   succeeds only by decoding the supplied code term through the relational byte
   reader and formula grammar used for non-generated formulas."
  [prog code sigma sigma-out _structural-first? formula branch-proof]
  (sjas-decode-formula-code-proofo prog code sigma sigma-out formula branch-proof))

(defn- sjas-class-code-closeo
  "Close a formula-class predicate by decoding and classifying the formula AST."
  [prog relation code sigma sigma-out _structural-first? formula branch-proof]
  (fresh []
    (sjas-decode-formula-code-proofo prog code sigma sigma-out formula branch-proof)
    (sjas-structural-formula-classo relation formula)))

(defn- sjas-neg-pair-code-closeo
  "Close `neg-pair(left,right)` by decoding both codes and complementing ASTs."
  [prog left right sigma sigma-out _structural-first? formula complement branch-proof]
  (fresh [sigma-mid left-proof right-proof]
    (sjas-decode-formula-code-proofo prog left sigma sigma-mid formula left-proof)
    (sjas-decode-formula-code-proofo prog right sigma-mid sigma-out complement right-proof)
    (sjas-formula-complemento formula complement)
    (== (list 'sjas-neg-pair-structural left-proof right-proof) branch-proof)))

(defn- sjas-syntax-code-brancho
  [prog relation args sigma sigma-out structural-first? branch-proof]
  (fresh [code left right formula complement]
    (conde
      [(== 'wff relation)
       (== (lcons code '()) args)
       (sjas-wff-code-closeo prog code sigma sigma-out structural-first? formula branch-proof)]
      [(== (lcons code '()) args)
       (sjas-class-relationo relation)
       (sjas-class-code-closeo prog relation code sigma sigma-out structural-first? formula branch-proof)]
      [(== 'neg-pair relation)
       (== (lcons left (lcons right '())) args)
       (sjas-neg-pair-code-closeo prog left right sigma sigma-out structural-first? formula complement branch-proof)])))

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
  (let [structural-first? (= :u-grounding (sjas-code-format prog))
        ground-relation (ground-negated-relation fml)]
    (if (syntax-code-relation? ground-relation)
      (conde
        [(fresh [branch-proof]
           (== '() env)
           (== '() sigma)
           (sjas-syntax-code-brancho prog
                                     ground-relation
                                     (apply list
                                            (ground-negated-app-args
                                              fml
                                              ground-relation
                                              (if (= 'neg-pair ground-relation) 2 1)))
                                     '()
                                     sigma-out
                                     structural-first?
                                     branch-proof)
           (== neqs neqs-out)
           (== (list 'profiled 'willard-sjas-code
                     (list ground-relation branch-proof))
               proof))]
        [(fresh [lit atom walked-atom relation args branch-proof]
           (subst/subst-formulao fml env lit)
           (== (list 'neg atom) lit)
           (equality/walk-atomo atom sigma walked-atom)
           (== (lcons 'app (lcons relation args)) walked-atom)
           (sjas-syntax-code-brancho prog
                                     relation
                                     args
                                     sigma
                                     sigma-out
                                     structural-first?
                                     branch-proof)
           (== neqs neqs-out)
           (== (list 'profiled 'willard-sjas-code (list relation branch-proof)) proof))])
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
                                  structural-first?
                                  branch-proof)
        (== neqs neqs-out)
        (== (list 'profiled 'willard-sjas-code (list relation branch-proof)) proof)))))

(defn- sjas-subst-code-closeo
  "Close structural `subst-code/2` goals.

   ADR-0066 separates Willard's `Subst(g,h)` relation from `SubstPrf(g,t,p)`.
   ADR-0069 computes the substitution itself by decoding formula-code bytes,
   replacing the distinguished free variable, and comparing the decoded target
   modulo bound-variable alpha-renaming."
  [fml env sigma sigma-out neqs neqs-out prog proof]
  (if-let [[source-code substituted-code] (ground-negated-subst-code-args fml)]
    (fresh []
      (== '() env)
      (== '() sigma)
      (sjas-subst-code-anyo prog source-code substituted-code '() sigma-out)
      (== neqs neqs-out)
      (== '(profiled willard-sjas-subst-code) proof))
    (fresh [lit atom walked-atom source-code substituted-code]
      (subst/subst-formulao fml env lit)
      (== (list 'neg atom) lit)
      (equality/walk-atomo atom sigma walked-atom)
      (== (list 'app 'subst-code source-code substituted-code) walked-atom)
      (sjas-subst-code-anyo prog source-code substituted-code sigma sigma-out)
      (== neqs neqs-out)
      (== '(profiled willard-sjas-subst-code) proof))))

(defn- sjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (let [ground-args (ground-negated-app-args fml 'tableau-proof 3)]
    (if ground-args
      (let [[system-code theorem-code proof-code] ground-args]
        (fresh [decoded-proof proof-bytes axiom-formula neg-theorem
                target sigma-proof proof-kind proof-read-proof theorem-read-proof]
          (== '() env)
          (== '() sigma)
          (decode-proof-code-kindo proof-code '() sigma-proof proof-bytes decoded-proof proof-kind)
          (code-read-marker-o proof-kind proof-read-proof)
          (conde
            [(fresh [axiom-proof]
               (== 'sjas-axiom decoded-proof)
               (sjas-axiom-membero prog system-code theorem-code axiom-proof)
               (== (list 'willard-sjas-axiom-member axiom-proof) theorem-read-proof))
             (== sigma-proof sigma-out)]
            [(sjas-system-axiom-formulao prog system-code axiom-formula)
             (sjas-structural-negated-theorem-proofo prog
                                                      theorem-code
                                                      sigma-proof
                                                      sigma-out
                                                      neg-theorem
                                                      theorem-read-proof)
             (== (list 'and axiom-formula neg-theorem) target)
             (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)])
          (== neqs neqs-out)
          (== (list 'profiled
                    'willard-sjas-proof-check
                    proof-read-proof
                    theorem-read-proof
                    decoded-proof)
              proof)))
      (fresh [lit atom walked-atom system-code theorem-code proof-code
              decoded-proof proof-bytes axiom-formula neg-theorem
              target sigma-proof proof-kind proof-read-proof theorem-read-proof]
        (subst/subst-formulao fml env lit)
        (== (list 'neg atom) lit)
        (equality/walk-atomo atom sigma walked-atom)
        (== (list 'app 'tableau-proof system-code theorem-code proof-code) walked-atom)
        (decode-proof-code-kindo proof-code sigma sigma-proof proof-bytes decoded-proof proof-kind)
        (code-read-marker-o proof-kind proof-read-proof)
        (conde
          [(fresh [axiom-proof]
             (== 'sjas-axiom decoded-proof)
             (sjas-axiom-membero prog system-code theorem-code axiom-proof)
             (== (list 'willard-sjas-axiom-member axiom-proof) theorem-read-proof))
           (== sigma-proof sigma-out)]
          [(sjas-system-axiom-formulao prog system-code axiom-formula)
           (sjas-structural-negated-theorem-proofo prog
                                                    theorem-code
                                                    sigma-proof
                                                    sigma-out
                                                    neg-theorem
                                                    theorem-read-proof)
           (== (list 'and axiom-formula neg-theorem) target)
           (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)])
        (== neqs neqs-out)
        (== (list 'profiled
                  'willard-sjas-proof-check
                  proof-read-proof
                  theorem-read-proof
                  decoded-proof)
            proof)))))

(defn- sjas-subst-prf-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh [lit atom walked-atom system-code substitution-code theorem-code proof-code
          decoded-proof proof-bytes axiom-formula neg-theorem
          target sigma-valid sigma-proof proof-kind proof-read-proof theorem-read-proof]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'subst-prf system-code substitution-code theorem-code proof-code)
        walked-atom)
    (sjas-active-systemo prog system-code)
    (decode-proof-code-kindo proof-code sigma sigma-proof proof-bytes decoded-proof proof-kind)
    (code-read-marker-o proof-kind proof-read-proof)
    (conde
      [(== 'sjas-axiom decoded-proof)
       (conde
         [(fresh [axiom-proof]
            (sjas-axiom-membero prog system-code theorem-code axiom-proof)
            (sjas-subst-source-codeo prog substitution-code sigma-proof sigma-out)
            (== (list 'willard-sjas-axiom-member axiom-proof) theorem-read-proof))]
         [(sjas-subst-code-anyo prog substitution-code theorem-code sigma-proof sigma-out)
          (== '(willard-sjas-subst-code) theorem-read-proof)])]
      [(sjas-system-axiom-formulao prog system-code axiom-formula)
       (sjas-subst-source-codeo prog substitution-code sigma-proof sigma-valid)
       (sjas-structural-negated-theorem-proofo prog
                                                theorem-code
                                                sigma-valid
                                                sigma-out
                                                neg-theorem
                                                theorem-read-proof)
       (== (list 'and axiom-formula neg-theorem) target)
       (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)])
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-subst-proof-check
              proof-read-proof
              theorem-read-proof
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

(defn- direct-negated-profile-closeo
  "Close a single already-focused negated SJAS atom.

   The ordinary kernel reaches these same relations through `close-agendao`.
   ADR-0071 U-Grounding code terms can be large enough that merely selecting
   the focused formula through a fresh logic variable overflows core.logic's
   occurs check. For top-level profile predicate queries, this shortcut keeps
   the focus as a host-ground value and delegates immediately to the same SJAS
   close relation."
  [fml prog fuel proof]
  (if-let [relation (ground-negated-relation fml)]
    (fresh [sigma-out neqs-out]
      (case relation
        subst-code
        (sjas-subst-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        wff
        (sjas-syntax-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        delta-star-0-code
        (sjas-syntax-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        pi-star-1-code
        (sjas-syntax-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        sigma-star-1-code
        (sjas-syntax-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        neg-pair
        (sjas-syntax-code-closeo fml '() '() sigma-out '() neqs-out prog proof)

        axiom-member
        (sjas-generated-fact-closeo fml '() '() sigma-out '() neqs-out prog proof)

        tableau-proof
        (sjas-tableau-proof-closeo fml '() '() sigma-out '() neqs-out prog fuel proof)

        subst-prf
        (sjas-subst-prf-closeo fml '() '() sigma-out '() neqs-out prog fuel proof)

        mult
        (sjas-neg-relation-closeo fml '() '() sigma-out '() neqs-out proof)

        leq
        (sjas-neg-relation-closeo fml '() '() sigma-out '() neqs-out proof)

        lt
        (sjas-neg-relation-closeo fml '() '() sigma-out '() neqs-out proof)

        fail))
    fail))

;; -----------------------------------------------------------------------------
;; Public proof-profile entrypoint
;; -----------------------------------------------------------------------------

(defn- profile-symbol
  "Convert a profile keyword into the symbol used in proof evidence."
  [profile]
  (symbol (name profile)))

(defn- hide-sjas-clauses-from-generic-sidecars
  "Disable generic sidecar classification for one SJAS proof run.

   `proflog.kernel/active-program-relations` inspects the host `:clauses` map
   to decide whether propositional/first-order sidecars may treat an atom as
   inert. SJAS profile predicates carry very large arithmetic code numerals, so
   those generic classifiers can exhaust core.logic's structural walk before
   the SJAS rule runs. The relational clause lists remain in the program, so
   ordinary procedure calls still work; only the host-side sidecar eligibility
   shortcut is hidden from the kernel during SJAS proof search."
  [program]
  (assoc program :clauses nil))

(defn- wrap-proof
  "Attach an explicit SJAS profile marker to an ordinary kernel proof term."
  [profile proof]
  (list 'profiled (profile-symbol profile) proof))

(defn prove-program
  "Prove with SJAS arithmetic and certificate rules interleaved into the kernel."
  [profile program formula proof-limit fuel]
  (let [program (hide-sjas-clauses-from-generic-sidecars program)
        proofs (binding [kernel/*theory-profile-closeo* willard-sjas-theory-closeo]
                 (doall
                   (if (direct-negated-profile-relation formula)
                     (run proof-limit [proof]
                       (direct-negated-profile-closeo formula program fuel proof))
                     (if (nil? fuel)
                       (run proof-limit [proof]
                         (kernel/prove-programo formula '() '() '() program '() nil proof))
                       (run proof-limit [proof]
                         (kernel/prove-programo formula '() '() '() program '() fuel proof))))))]
    (map #(wrap-proof profile %) proofs)))
