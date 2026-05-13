(ns proflog.kernel.willard-sjas-profile
  "Kernel-interleaved Willard SJAS profile.

   ADR-0061 promotes the ADR-0060 scaffold in two ways:

   - U-grounding arithmetic is interpreted as relations over binary numerals
     whose object-language constants are `0` and `1`;
   - `tableau-proof/3` checks a structural proof certificate by running the
     existing Proflog kernel with the decoded proof term already supplied.

   The profile therefore remains a tableau extension, not a host-side evaluator:
   arithmetic constraints and proof checking are both miniKanren goals
   interleaved at the branch rule boundary."
  (:refer-clojure :exclude [== < <=])
  (:require [clojure.core.logic :refer [== conde fail fresh lcons membero run]]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel :as kernel]
            [proflog.kernel-support :as support]
            [proflog.relational-arithmetic :as arith]
            [proflog.subst :as subst]))

(def ^:private zero-symbol (symbol "0"))
(def ^:private one-symbol (symbol "1"))
(def ^:private zero-term (ast/app-term zero-symbol))
(def ^:private one-term (ast/app-term one-symbol))
(def ^:private one-bits (arith/build-num 1))
(def ^:private two-bits (arith/build-num 2))

;; -----------------------------------------------------------------------------
;; Structural proof-code encoding
;; -----------------------------------------------------------------------------

(def ^:private proof-nil-symbol 'proof-nil)

(def ^:private proof-symbols
  "Proof atoms that can appear as leaves in encoded kernel proof terms.

   The checker is structural for lists and for these proof atoms. Terms or
   other non-symbol payloads can be added here as new kernel proof constructors
   demand them; the certificate relation itself does not prove by looking up a
   preapproved proof result."
  '[conj
    split
    univ
    once-univ
    witness
    eq-step
    eq-triggered-call
    eq-triggered-neg-call
    neq-close
    neq-rigid
    neq-store
    refl-close
    savefml
    false-close
    close
    pos-call
    neg-call
    neg-call-alt
    neg-call-guarded-alt
    guarded-alt
    guarded-neg-alt
    guarded-neg-alt-saturated
    guarded-seq-step
    guarded-seq-last
    guarded-call-seq-step
    guarded-call-seq-defer
    guarded-residual-seq-step
    guarded-residual-seq-last
    guarded-scope-exists
    query-pos-call
    query-neg-call
    query-neg-call-guarded-alt
    profiled
    willard-sjas-tableau0
    willard-sjas-level1
    willard-sjas-arithmetic
    willard-sjas-fact
    willard-sjas-proof-check
    sjas-bind-done
    sjas-bind-num
    sjas-equal
    sjas-eq-progress
    sjas-leq
    sjas-lt
    sjas-mult
    sjas-num-add-one
    sjas-num-dbl
    sjas-num-one
    sjas-num-zero
    sjas-read-add
    sjas-read-count
    sjas-read-dbl
    sjas-read-div
    sjas-read-log
    sjas-read-max
    sjas-read-one
    sjas-read-pred
    sjas-read-root
    sjas-read-sub
    sjas-read-var
    sjas-read-zero])

(defn- proof-symbol-code-symbol
  [sym]
  (symbol (str "proofsym_" (name sym))))

(def ^:private proof-symbol->code-symbol
  (into {} (map (fn [sym]
                  [sym (proof-symbol-code-symbol sym)])
                proof-symbols)))

(def proof-code-constants
  "Object-language constants needed for structural proof certificates."
  (vec (cons proof-nil-symbol (vals proof-symbol->code-symbol))))

(def ^:private proof-symbol-code-entries
  (apply list
         (map (fn [[sym code-symbol]]
                [(ast/app-term code-symbol) sym])
              proof-symbol->code-symbol)))

(defn- proof-symbol-codeo
  [code sym]
  (fresh [entry]
    (membero entry proof-symbol-code-entries)
    (== [code sym] entry)))

(declare decode-proof-codeo)

(defn- decode-proof-listo
  [code proof]
  (conde
    [(== (ast/app-term proof-nil-symbol) code)
     (== '() proof)]
    [(fresh [head-code tail-code head tail]
       (== (list 'app 'proof-cons head-code tail-code) code)
       (decode-proof-codeo head-code head)
       (decode-proof-listo tail-code tail)
       (== (lcons head tail) proof))]))

(defn- decode-proof-codeo
  "Relate an object-language proof-code term to a kernel proof datum."
  [code proof]
  (conde
    [(proof-symbol-codeo code proof)]
    [(decode-proof-listo code proof)]))

(declare ground-proof-codeo)

(defn- ground-proof-code-listo
  [terms]
  (conde
    [(== '() terms)]
    [(fresh [head tail]
       (== (lcons head tail) terms)
       (ground-proof-codeo head)
       (ground-proof-code-listo tail))]))

(defn- ground-proof-codeo
  "Recognize already-constructed proof-code terms.

   Open object-language variables must not cause the certificate checker to
   enumerate possible proof codes. They are meaningful in SJAS formulas, but a
   concrete `tableau-proof/3` check requires a concrete certificate."
  [term]
  (fresh [head args]
    (== (lcons 'app (lcons head args)) term)
    (ground-proof-code-listo args)))

(defn- strip-profile-wrapper
  [proof]
  (if (and (seq? proof)
           (= 'profiled (first proof))
           (= 3 (count proof)))
    (nth proof 2)
    proof))

(declare encode-proof-code)

(defn- encode-proof-list
  [items]
  (if (empty? items)
    (ast/app-term proof-nil-symbol)
    (ast/app-term 'proof-cons
                  (encode-proof-code (first items))
                  (encode-proof-list (rest items)))))

(defn- encode-proof-code
  [value]
  (cond
    (symbol? value)
    (if-let [code-symbol (get proof-symbol->code-symbol value)]
      (ast/app-term code-symbol)
      (throw (ex-info "Unsupported proof symbol in SJAS certificate"
                      {:symbol value})))

    (sequential? value)
    (encode-proof-list value)

    :else
    (throw (ex-info "Unsupported proof payload in SJAS certificate"
                    {:value value
                     :class (some-> value class .getName)}))))

(defn proof-certificate-term
  "Encode a kernel proof term as an object-language SJAS certificate term."
  [proof]
  (encode-proof-code (strip-profile-wrapper proof)))

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
  (fresh [lit atom walked-atom fact]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (membero fact (or (:sjas/fact-atoms prog) '()))
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

(defn- sjas-proof-targeto
  [prog system-code theorem-code target]
  (fresh [entry]
    (membero entry (or (:sjas/proof-targets prog) '()))
    (== [system-code theorem-code target] entry)))

(defn- sjas-tableau-proof-closeo
  [fml env sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh [lit atom walked-atom system-code theorem-code proof-code decoded-proof target]
    (subst/subst-formulao fml env lit)
    (== (list 'neg atom) lit)
    (equality/walk-atomo atom sigma walked-atom)
    (== (list 'app 'tableau-proof system-code theorem-code proof-code) walked-atom)
    (ground-proof-codeo proof-code)
    (decode-proof-codeo proof-code decoded-proof)
    (sjas-proof-targeto prog system-code theorem-code target)
    (kernel/prove-programo target '() '() '() prog '() fuel decoded-proof)
    (== sigma sigma-out)
    (== neqs neqs-out)
    (== (list 'profiled 'willard-sjas-proof-check decoded-proof) proof)))

(defn willard-sjas-theory-closeo
  "SJAS theory branch rule bound into the ordinary proof kernel."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out
   prog gamma-terms fuel proof]
  (conde
    [(sjas-eq-progresso fml unexpanded lits env proof-vars sigma sigma-out
                        neqs neqs-out prog gamma-terms fuel proof)]
    [(sjas-generated-fact-closeo fml env sigma sigma-out neqs neqs-out prog proof)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)]))

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
    [(sjas-generated-fact-closeo fml env sigma sigma-out neqs neqs-out prog proof)
     (== residuals residuals-out)]
    [(sjas-neq-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-neg-relation-closeo fml env sigma sigma-out neqs neqs-out proof)
     (== residuals residuals-out)]
    [(sjas-tableau-proof-closeo fml env sigma sigma-out neqs neqs-out prog fuel proof)
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
