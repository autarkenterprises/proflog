(ns proflog.willard-sjas
  "Willard-style SJAS language builder.

   This namespace is the source-to-kernel construction layer for the SJAS ADR
   sequence. It builds finite reflected SJAS systems with stable formula codes
   and encoded system sources. Binary arithmetic, axiom membership, and
   proof-certificate checking are handled by the selected proof profile; host
   Clojure is used here only to assemble the finite source object."
  (:require [clojure.core.logic :refer [lvar]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.answer-overlay :as answer-overlay]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.frontend :as frontend]
            [proflog.gamma :as gamma]
            [proflog.kernel.willard-sjas-profile :as willard-sjas-profile]
            [proflog.language :as language]
            [proflog.normalize :as normalize]
            [proflog.query :as query]
            [proflog.sjas-boundary-axioms :as boundary-axioms]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas-code :as sjas-code]))

;; -----------------------------------------------------------------------------
;; Public terms and language declarations
;; -----------------------------------------------------------------------------

(def zero-symbol
  "Object-language symbol for the SJAS numeral zero.

   Clojure cannot read a bare digit as a symbol, so source code refers to this
   through helper vars such as `zero`; the term itself is still `(app 0)`."
  (symbol "0"))

(def one-symbol
  "Object-language symbol for the SJAS numeral one."
  (symbol "1"))

(def zero (ast/app-term zero-symbol))
(def one (ast/app-term one-symbol))

(defn add-term [left right] (ast/app-term 'add left right))
(defn dbl-term [term] (ast/app-term 'dbl term))

(defn numeral
  "Return the canonical binary-composed SJAS term for a host natural number.

   This helper belongs to the source-construction boundary. It does not add
   host arithmetic to proof search; it only writes the object-language numeral
   that the kernel profile later interprets relationally."
  [value]
  {:pre [(and (integer? value) (not (neg? value)))]}
  (cond
    (zero? value) zero
    (= 1 value) one
    (even? value) (dbl-term (numeral (quot value 2)))
    :else (add-term (dbl-term (numeral (quot value 2))) one)))

(def two (numeral 2))
(def three (numeral 3))
(def four (numeral 4))
(def five (numeral 5))
(def six (numeral 6))
(def contradiction-code
  "Base-64 Godel-code term for the contradictory theorem `0 = 1`."
  (sjas-code/bytes->code-term [5 25 0 0 25 1 0 1]))

(def base-constants
  "Base constants used by the SJAS signature.

   The only numeral constants are `0` and `1`. Larger helper numerals in this
   namespace are composed with `dbl` and `add`, matching the binary
   U-grounding shape used by the later Type-A SJAS presentations."
  [zero-symbol one-symbol])

(def u-grounding-arithmetic-functions
  "Arithmetic U-grounding function symbols exposed by the SJAS signature."
  {'pred 1
   'sub 2
   'div 2
   'max 2
   'log 1
   'root 2
   'count 2
   'add 2
   'dbl 1})

(def u-grounding-functions
  "Function symbols exposed by the SJAS signature.

   The arithmetic symbols are Willard's U-grounding functions. The generated
   `code-N` symbols are compact base-64 Godel-code constructors; they are inert
   as arithmetic functions but are part of the object language because
   `tableau-proof/3` and the syntax predicates receive codes as terms."
  (merge u-grounding-arithmetic-functions
         sjas-code/code-functions))

(defn- functions-for-code-format
  "Return the formal function signature for one public code representation.

   The default compact representation needs generated `code-N` constructors.
   ADR-0071's U-Grounding representation deliberately omits them: formal codes
   are ordinary binary numerals built from the arithmetic functions already in
   the base language."
  [code-format]
  (case code-format
    :compact u-grounding-functions
    :u-grounding u-grounding-arithmetic-functions
    (throw (ex-info "Unsupported SJAS code format"
                    {:code-format code-format
                     :supported #{:compact :u-grounding}}))))

(def base-relations
  "Relations required by the SJAS language and generated program."
  {'leq 2
   'lt 2
   'mult 3
   'wff 1
   'delta-star-0-code 1
   'pi-star-1-code 1
   'sigma-star-1-code 1
   'neg-pair 2
   'axiom-member 2
   'tableau-proof 3
   'dsjas-tableau-proof 3
   'tab1-proof 3
   'dsjas-tab1-proof 3
   'subst-code 2
   'subst-prf 4
   'dsjas-subst-prf 4})

(def tab2-boundary-relations
  "Target-only relations for the Tab-2-or-stronger boundary variant."
  {'dsjas-tab2-proof 3})

(def u-grounding-language
  "Base SJAS signature without a proof-profile selection."
  (language/language
    {:constants base-constants
     :functions u-grounding-functions
     :relations base-relations}))

(def tableau0-profile-language
  "Base SJAS signature selecting the ordinary-tableau self-consistency profile."
  (language/language
    {:constants base-constants
     :functions u-grounding-functions
     :relations base-relations
     :proof-profile :willard-sjas-tableau0}))

(def level1-profile-language
  "Base SJAS signature selecting the Level-1 self-consistency profile."
  (language/language
    {:constants base-constants
     :functions u-grounding-functions
     :relations base-relations
     :proof-profile :willard-sjas-level1}))

(def tab1-profile-language
  "Base SJAS signature selecting the Tab-1 proof-list self-consistency profile."
  (language/language
    {:constants base-constants
     :functions u-grounding-functions
     :relations base-relations
     :proof-profile :willard-sjas-tab1}))

(defn not-code
  "Object-language code for the complement of `formula-code`."
  [formula-code]
  (ast/app-term 'not-code formula-code))

(defn proof-certificate
  "Encode a Proflog kernel proof term as an SJAS proof-code term.

   The default `:compact` format preserves the ADR-0063 public shape. Passing
   `{:code-format :u-grounding}` emits a single binary U-Grounding numeral term
   whose sentinel-terminated base-64 expansion carries the same proof bytes."
  ([proof]
   (proof-certificate proof {:code-format :compact}))
  ([proof {:keys [code-format]
           :or {code-format :compact}}]
   (sjas-code/proof-formal-code-term
     (willard-sjas-profile/strip-profile-wrapper proof)
     code-format)))

(declare formal-code-term-bytes)

(defn- measured-proof-object
  "Encode a `D_SJAS` measured proof object.

   The payloads are byte vectors stored inside the existing proof-code grammar.
   This makes the composite object itself a public proof-code term, so the
   object-language checker can decode it with the same arithmeticized byte
   reader used for ordinary proof certificates."
  [tag code-format payloads]
  (sjas-code/proof-formal-code-term
    (cons tag (mapv vec payloads))
    code-format))

(defn dsjas-tableau-proof-object
  "Encode the measured `D_SJAS` tableau proof object `(S,F,P)`."
  ([system-code theorem-code proof-code]
   (dsjas-tableau-proof-object system-code theorem-code proof-code
                               {:code-format :compact}))
  ([system-code theorem-code proof-code {:keys [code-format]
                                         :or {code-format :compact}}]
   (measured-proof-object
     'dsjas-tableau-proof-object
     code-format
     [(formal-code-term-bytes system-code)
      (formal-code-term-bytes theorem-code)
      (formal-code-term-bytes proof-code)])))

(defn dsjas-subst-prf-object
  "Encode the measured `D_SJAS` substitution-proof object `(S,G,F,P)`."
  ([system-code substitution-code theorem-code proof-code]
   (dsjas-subst-prf-object system-code substitution-code theorem-code proof-code
                           {:code-format :compact}))
  ([system-code substitution-code theorem-code proof-code {:keys [code-format]
                                                           :or {code-format :compact}}]
   (measured-proof-object
     'dsjas-subst-prf-object
     code-format
     [(formal-code-term-bytes system-code)
      (formal-code-term-bytes substitution-code)
      (formal-code-term-bytes theorem-code)
      (formal-code-term-bytes proof-code)])))

(defn- tab1-proof-list-entry-payload
  "Return `[theorem-bytes proof-bytes]` for one public Tab-1 proof-list entry."
  [entry]
  (let [[theorem-code proof-code]
        (cond
          (map? entry)
          [(:theorem-code entry) (:proof-code entry)]

          (and (sequential? entry) (= 2 (count entry)))
          [(first entry) (second entry)]

          :else
          (throw (ex-info "Expected a Tab-1 proof-list entry"
                          {:entry entry
                           :expected "{:theorem-code t :proof-code p} or [t p]"})))]
    (when-not (and theorem-code proof-code)
      (throw (ex-info "Tab-1 proof-list entries require theorem and proof code"
                      {:entry entry})))
    [(formal-code-term-bytes theorem-code)
     (formal-code-term-bytes proof-code)]))

(defn tab1-proof-list-object
  "Encode a public Tab-1 proof list `H = [(t1,p1), ..., (tn,pn)]`.

   This is only the proof-object syntax boundary. Later ADRs must validate that
   each `pi` proves `ti` from beta plus the earlier `tj`, and that intermediate
   `tj` are within the permitted Level-1 classes."
  ([entries]
   (tab1-proof-list-object entries {:code-format :compact}))
  ([entries {:keys [code-format]
             :or {code-format :compact}}]
   (let [entries (vec entries)]
     (when (empty? entries)
       (throw (ex-info "Tab-1 proof lists must contain at least one entry"
                       {:entries entries})))
     (sjas-code/proof-formal-code-term
       (into ['tab1-proof-list-object]
             (mapv tab1-proof-list-entry-payload entries))
       code-format))))

(defn dsjas-tab1-proof-object
  "Encode the measured Tab-1 proof object `(S,F,H)`.

   `H` is the public proof-list object, already encoded as an SJAS proof-code
   term. The generated Tab-1 SelfCons sentence quantifies this measured object
   rather than a bare proof-list code."
  ([system-code theorem-code proof-list-code]
   (dsjas-tab1-proof-object system-code theorem-code proof-list-code
                            {:code-format :compact}))
  ([system-code theorem-code proof-list-code {:keys [code-format]
                                              :or {code-format :compact}}]
   (measured-proof-object
     'dsjas-tab1-proof-object
     code-format
     [(formal-code-term-bytes system-code)
      (formal-code-term-bytes theorem-code)
      (formal-code-term-bytes proof-list-code)])))

(defn tab2-proof-list-object
  "Encode a Tab-2 proof list using a distinct arithmeticized object tag."
  ([entries]
   (tab2-proof-list-object entries {:code-format :compact}))
  ([entries {:keys [code-format]
             :or {code-format :compact}}]
   (let [entries (vec entries)]
     (when (empty? entries)
       (throw (ex-info "Tab-2 proof lists must contain at least one entry"
                       {:entries entries})))
     (sjas-code/proof-formal-code-term
       (into ['tab2-proof-list-object]
             (mapv tab1-proof-list-entry-payload entries))
       code-format))))

(defn dsjas-tab2-proof-object
  "Encode the measured Tab-2 proof object `(S,F,H)`."
  ([system-code theorem-code proof-list-code]
   (dsjas-tab2-proof-object system-code theorem-code proof-list-code
                            {:code-format :compact}))
  ([system-code theorem-code proof-list-code {:keys [code-format]
                                              :or {code-format :compact}}]
   (measured-proof-object
     'dsjas-tab2-proof-object
     code-format
     [(formal-code-term-bytes system-code)
      (formal-code-term-bytes theorem-code)
      (formal-code-term-bytes proof-list-code)])))

(defn pred-term [term] (ast/app-term 'pred term))
(defn sub-term [left right] (ast/app-term 'sub left right))
(defn div-term [left right] (ast/app-term 'div left right))
(defn max-term [left right] (ast/app-term 'max left right))
(defn log-term [term] (ast/app-term 'log term))
(defn root-term [left right] (ast/app-term 'root left right))
(defn count-term [left right] (ast/app-term 'count left right))

(def pair-functions
  "Function declarations for the ADR-0123 reflected pair self-extension demo.

   The symbols are intentionally ordinary user functions rather than reserved
   SJAS primitives. Adding them to the reflected beta basis demonstrates that a
   finite data-structure extension changes the encoded source and regenerated
   SelfCons statement."
  {'pair 2
   'fst 1
   'snd 1})

(defn pair-term
  "Construct the fresh pair term `pair(left,right)`."
  [left right]
  (ast/app-term 'pair left right))

(defn fst-term
  "Construct the first-projection term `fst(pair)`."
  [pair]
  (ast/app-term 'fst pair))

(defn snd-term
  "Construct the second-projection term `snd(pair)`."
  [pair]
  (ast/app-term 'snd pair))

(defn pair-projection-axioms
  "Return the two universal reflected beta axioms for the pair extension.

   These are deliberately small Pi*1-presented formulas: universal closures of
   equality literals. Lists and recursive syntax operations are left for later
   self-extension ADRs that can build on this pair representation layer."
  []
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        x-term (ast/var-term x)
        y-term (ast/var-term y)
        pair (pair-term x-term y-term)]
    [(ast/forall-form
       x
       (ast/forall-form
         y
         (ast/eq-lit (fst-term pair) x-term)))
     (ast/forall-form
       x
       (ast/forall-form
         y
         (ast/eq-lit (snd-term pair) y-term)))]))

(defn pair-extension-options
  "Return the system option fragment for the reflected pair self-extension."
  []
  {:functions pair-functions
   :beta (pair-projection-axioms)})

(def list-constants
  "Constants introduced by ADR-0128's pair-backed list extension."
  ['list-nil])

(def list-functions
  "Function declarations for the finite reflected list representation layer."
  {'list-cons 2
   'list-head 1
   'list-tail 1})

(def list-nil-term (ast/app-term 'list-nil))

(defn list-cons-term
  "Construct the pair-backed list constructor term."
  [head tail]
  (ast/app-term 'list-cons head tail))

(defn list-head-term
  "Construct the list-head projection term."
  [list-term]
  (ast/app-term 'list-head list-term))

(defn list-tail-term
  "Construct the list-tail projection term."
  [list-term]
  (ast/app-term 'list-tail list-term))

(defn list-constructor-axioms
  "Return finite reflected beta laws for lists represented through pairs.

   The constructor is tied to the ADR-0123 pair layer by
   `list-cons(x,xs) = pair(x,xs)`. Head and tail are then exposed as list-facing
   projections. Recursive list processing is intentionally left to later
   Workstream C ADRs."
  []
  (let [x (nominal/nom (lvar 'x))
        xs (nominal/nom (lvar 'xs))
        x-term (ast/var-term x)
        xs-term (ast/var-term xs)
        cons-term (list-cons-term x-term xs-term)]
    [(ast/forall-form
       x
       (ast/forall-form
         xs
         (ast/eq-lit cons-term
                     (pair-term x-term xs-term))))
     (ast/forall-form
       x
       (ast/forall-form
         xs
         (ast/eq-lit (list-head-term cons-term)
                     x-term)))
     (ast/forall-form
       x
       (ast/forall-form
         xs
         (ast/eq-lit (list-tail-term cons-term)
                     xs-term)))]))

(defn list-extension-options
  "Return the pair-plus-list reflected extension fragment for ADR-0128."
  []
  (let [{pair-beta :beta
         pair-signature :functions} (pair-extension-options)]
    {:constants list-constants
     :functions (merge pair-signature list-functions)
     :beta (vec (concat pair-beta
                        (list-constructor-axioms)))}))

(def total-multiplication-functions
  "Function declaration for ADR-0124's total-multiplication boundary variant.

   Baseline SJAS deliberately omits `mul/2`; this map is opt-in variant
   metadata. The seed beta fragment below makes the variant's reflected source
   concrete without claiming the full diagonal contradiction witness."
  {'mul 2})

(defn mul-term
  "Construct the total multiplication term `mul(left,right)`."
  [left right]
  (ast/app-term 'mul left right))

(def total-multiplication-willard-relations
  "Willard 2002 V-route relation symbols for the Type-M boundary proof.

   These are not shortcut proof oracles. They are the object-language
   vocabulary used by V4/V5: finite-axiom inclusion, the bounded semantic proof
   predicate, the diagonal map predicate, and ordinary semantic proof from a
   finite axiom system. Later evidence must still make these predicates
   executable before any counterexample tuple can pass."
  {'finax4 1
   'willard-map 3
   'semprfk-alpha 5
   'semprf-alpha 3})

(defn finax4
  "Formula atom `FinAx4(alpha)` from Willard 2002 Section 3."
  [alpha]
  (ast/pos-lit (ast/app-term 'finax4 alpha)))

(defn willard-map
  "Formula atom `Map(alpha,k,d)` used in Willard's Paradox predicate."
  [alpha k d]
  (ast/pos-lit (ast/app-term 'willard-map alpha k d)))

(defn semprfk-alpha
  "Formula atom `SemPrf^k_alpha(theorem,proof,bound)`.

   The `k` argument is explicit rather than a host-side superscript so the
   generated formula is inspectable by the SJAS code reader."
  [alpha k theorem proof bound]
  (ast/pos-lit (ast/app-term 'semprfk-alpha alpha k theorem proof bound)))

(defn semprf-alpha
  "Formula atom `SemPrf_alpha(theorem,proof)` with explicit axiom-system code."
  [alpha theorem proof]
  (ast/pos-lit (ast/app-term 'semprf-alpha alpha theorem proof)))

(declare bounded-exists leq lt subst-code)

(defn total-multiplication-willard-upsilon
  "Willard 2002 Equation (15): `Subst(g,h) /\\ SemPrf^k_alpha(h,y,z)`."
  [alpha k g h y z]
  (ast/and-form (subst-code g h)
                (semprfk-alpha alpha k h y z)))

(defn total-multiplication-willard-paradox
  "Willard 2002 Equation (12): `exists d. Map(alpha,k,d) /\\ SemPrf^k_alpha(d,y,z)`."
  [y z alpha k]
  (let [d (nominal/nom (lvar 'willard-d))
        dt (ast/var-term d)]
    (ast/exists-form
      d
      (ast/and-form (willard-map alpha k dt)
                    (semprfk-alpha alpha k dt y z)))))

(defn total-multiplication-willard-v4-axiom
  "Willard 2002 V4 descent axiom over the Equation (15) `Upsilon` predicate."
  []
  (boundary-axioms/total-multiplication-willard-v4-axiom))

(defn total-multiplication-willard-v5-axiom
  "Willard 2002 V5: the Paradox antecedent yields a bounded contradiction proof."
  [contradiction-code-term]
  (boundary-axioms/total-multiplication-willard-v5-axiom
    contradiction-code-term))

(defn total-multiplication-willard-route-axioms
  "Return V4 and V5, the Willard 2002 route axioms needed after squaring."
  [contradiction-code-term]
  (boundary-axioms/total-multiplication-willard-route-axioms
    contradiction-code-term))

(defn total-multiplication-seed-axioms
  "Return a small Pi*1-admissible reflected beta seed for the `mul/2` variant.

   These laws are intentionally only a boundary surface. They install true,
   finite equations involving the total function symbol so the variant changes
   system identity and generated SelfCons code. Reduced/full contradiction
   witnesses remain Workstream B obligations for later ADRs."
  []
  (let [x (nominal/nom (lvar 'x))
        x-term (ast/var-term x)]
    [(ast/forall-form
       x
       (ast/eq-lit (mul-term x-term zero) zero))
     (ast/forall-form
       x
       (ast/eq-lit (mul-term x-term one) x-term))]))

(defn total-multiplication-complete-axioms
  "Return the reflected equational basis for interpreted total multiplication.

   The first two equations preserve the earlier seed. The successor equation,
   commutativity, associativity, and distributivity make the selected Type-M
   arithmetic assumptions explicit in the encoded beta source. The kernel's
   `mul/2` term interpreter supplies totality over every canonical natural;
   these formulas document that interpretation inside the reflected system."
  []
  (boundary-axioms/total-multiplication-complete-axioms))

(defn boundary-arithmetic-basis-axioms
  "Return the finite Pi*1 arithmetic basis shared by unsafe profiles.

   The function equations state the zero/successor behavior of total addition.
   The relation clauses state zero, one, and functional uniqueness laws for
   the interpreted multiplication graph. These are the conventional
   arithmetic assumptions required by the selected Xtab and Tab-2 boundary
   demonstrations, represented inside beta rather than as report metadata."
  []
  (boundary-axioms/boundary-arithmetic-basis-axioms))

(defn total-multiplication-boundary-options
  "Return the mergeable system option fragment for the `mul/2` boundary variant."
  []
  {:functions total-multiplication-functions
   :beta (total-multiplication-seed-axioms)})

(defn- validate-squaring-chain-depth!
  [depth]
  (when-not (and (integer? depth) (not (neg? depth)))
    (throw (ex-info "Expected a non-negative squaring-chain depth"
                    {:depth depth}))))

(defn total-multiplication-squaring-chain-constants
  "Return fresh constants `tm-u0 ... tm-u<depth>` for the reduced witness."
  [depth]
  (validate-squaring-chain-depth! depth)
  (mapv #(symbol (str "tm-u" %)) (range (inc depth))))

(defn total-multiplication-squaring-chain-axioms
  "Return the reflected beta equations for a finite squaring chain.

   The fragment mirrors Willard's total-multiplication compression step:
   `u_0 = 2` and `u_(i+1) = mul(u_i,u_i)`. It is only the reduced witness stage;
   the full SelfCons contradiction remains a separate Workstream B obligation."
  [depth]
  (let [constants (total-multiplication-squaring-chain-constants depth)
        constant-term #(ast/app-term %)]
    (vec
      (cons
        (ast/eq-lit (constant-term (first constants)) two)
        (map (fn [left right]
               (let [left-term (constant-term left)]
                 (ast/eq-lit (constant-term right)
                             (mul-term left-term left-term))))
             constants
             (rest constants))))))

(defn total-multiplication-squaring-chain-summary
  "Return host-side size data for the finite squaring-chain witness.

   Starting from `u_0 = 2`, depth `n` represents `2^(2^n)`, whose binary
   representation has `2^n + 1` bits. The summary is an audit aid; proof search
   still sees only the reflected beta equations."
  [depth]
  (validate-squaring-chain-depth! depth)
  (let [represented-exponent (reduce *' 1 (repeat depth 2))]
    {:depth depth
     :definition-count (inc depth)
     :represented-exponent represented-exponent
     :represented-bit-length (inc represented-exponent)}))

(defn total-multiplication-reduced-witness-options
  "Return the mergeable reduced-witness fragment for total multiplication."
  [depth]
  {:constants (total-multiplication-squaring-chain-constants depth)
   :functions total-multiplication-functions
   :beta (vec (concat (total-multiplication-seed-axioms)
                      (total-multiplication-squaring-chain-axioms depth)))})

(def xtab-lem-relations
  "Relation declaration for ADR-0133's finite Xtab/LEM reduced witness.

   The baseline tableau system already proves excluded-middle behavior through
   tableau rules. This relation is only the witness predicate used to package
   one excluded-middle instance as reflected beta material."
  {'xtab-lem-demo 1})

(defn xtab-lem-witness-axioms
  "Return the finite reflected beta seed for the Xtab/LEM boundary variant.

   This is a one-predicate universal excluded-middle instance:
   `forall x. xtab-lem-demo(x) or not xtab-lem-demo(x)`. It is deliberately
   smaller than a full LEM schema and does not claim a contradiction target."
  []
  (boundary-axioms/xtab-lem-witness-axioms))

(defn xtab-lem-reduced-witness-options
  "Return the mergeable reduced-witness fragment for Xtab/LEM-as-axiom."
  []
  {:relations xtab-lem-relations
   :beta (xtab-lem-witness-axioms)})

(defn leq [left right] (ast/pos-lit (ast/app-term 'leq left right)))
(defn lt [left right] (ast/pos-lit (ast/app-term 'lt left right)))
(defn mult [left right product] (ast/pos-lit (ast/app-term 'mult left right product)))
(defn wff [code] (ast/pos-lit (ast/app-term 'wff code)))
(defn delta-star-0-code [code] (ast/pos-lit (ast/app-term 'delta-star-0-code code)))
(defn pi-star-1-code [code] (ast/pos-lit (ast/app-term 'pi-star-1-code code)))
(defn sigma-star-1-code [code] (ast/pos-lit (ast/app-term 'sigma-star-1-code code)))
(defn neg-pair [left right] (ast/pos-lit (ast/app-term 'neg-pair left right)))
(defn axiom-member [system-code formula-code]
  (ast/pos-lit (ast/app-term 'axiom-member system-code formula-code)))
(defn tableau-proof [system-code theorem-code proof-code]
  (ast/pos-lit (ast/app-term 'tableau-proof system-code theorem-code proof-code)))
(defn dsjas-tableau-proof [system-code theorem-code proof-object-code]
  (ast/pos-lit
    (ast/app-term 'dsjas-tableau-proof
                  system-code
                  theorem-code
                  proof-object-code)))
(defn tab1-proof [system-code theorem-code proof-list-code]
  (ast/pos-lit
    (ast/app-term 'tab1-proof system-code theorem-code proof-list-code)))
(defn dsjas-tab1-proof [system-code theorem-code proof-object-code]
  (ast/pos-lit
    (ast/app-term 'dsjas-tab1-proof
                  system-code
                  theorem-code
                  proof-object-code)))
(defn dsjas-tab2-proof [system-code theorem-code proof-object-code]
  (ast/pos-lit
    (ast/app-term 'dsjas-tab2-proof
                  system-code
                  theorem-code
                  proof-object-code)))
(defn subst-code [source-code substituted-code]
  (ast/pos-lit (ast/app-term 'subst-code source-code substituted-code)))
(defn subst-prf [system-code substitution-code theorem-code proof-code]
  (ast/pos-lit
    (ast/app-term 'subst-prf system-code substitution-code theorem-code proof-code)))
(defn dsjas-subst-prf [system-code substitution-code theorem-code proof-object-code]
  (ast/pos-lit
    (ast/app-term 'dsjas-subst-prf
                  system-code
                  substitution-code
                  theorem-code
                  proof-object-code)))

;; -----------------------------------------------------------------------------
;; Formula-class surface
;; -----------------------------------------------------------------------------

(defn bounded-forall
  "Build an SJAS-visible bounded universal quantifier.

   Bounded quantifiers are a frontend/classifier layer. `lower-bounded-formula`
   turns them into ordinary kernel formulas when needed."
  [binding-nom bound body]
  (list 'bounded-forall (nominal/tie binding-nom {:bound bound :body body})))

(defn bounded-exists
  "Build an SJAS-visible bounded existential quantifier."
  [binding-nom bound body]
  (list 'bounded-exists (nominal/tie binding-nom {:bound bound :body body})))

(defn- bounded-form?
  [formula tag]
  (and (seq? formula)
       (= tag (first formula))
       (nominal/tie? (second formula))))

(defn lower-bounded-formula
  "Lower SJAS bounded quantifiers to ordinary first-order kernel formulas."
  [formula]
  (case (ast/tag-of formula)
    bounded-forall (let [tied (second formula)
                         binding (:binding-nom tied)
                         {:keys [bound body]} (:body tied)]
                     (ast/forall-form
                       binding
                       (ast/implies-form
                         (leq (ast/var-term binding) bound)
                         (lower-bounded-formula body))))
    bounded-exists (let [tied (second formula)
                         binding (:binding-nom tied)
                         {:keys [bound body]} (:body tied)]
                     (ast/exists-form
                       binding
                       (ast/and-form
                         (leq (ast/var-term binding) bound)
                         (lower-bounded-formula body))))
    and (ast/and-form (lower-bounded-formula (second formula))
                      (lower-bounded-formula (nth formula 2)))
    or (ast/or-form (lower-bounded-formula (second formula))
                    (lower-bounded-formula (nth formula 2)))
    not (ast/not-form (lower-bounded-formula (second formula)))
    implies (ast/implies-form (lower-bounded-formula (second formula))
                              (lower-bounded-formula (nth formula 2)))
    forall (let [tied (second formula)]
             (ast/forall-form (:binding-nom tied)
                              (lower-bounded-formula (:body tied))))
    exists (let [tied (second formula)]
             (ast/exists-form (:binding-nom tied)
                              (lower-bounded-formula (:body tied))))
    once-forall (let [tied (second formula)]
                  (ast/once-forall-form (:binding-nom tied)
                                        (lower-bounded-formula (:body tied))))
    formula))

(declare delta-star-0?)

(defn delta-star-0?
  "Return true for the implemented Delta-star-0 formula class.

   The classifier deliberately accepts bounded SJAS quantifiers and rejects
   ordinary unbounded quantifiers."
  [formula]
  (case (ast/tag-of formula)
    true true
    false true
    pos true
    neg true
    eq true
    neq true
    and (and (delta-star-0? (second formula))
             (delta-star-0? (nth formula 2)))
    or (and (delta-star-0? (second formula))
            (delta-star-0? (nth formula 2)))
    ;; Delta-star-0 is closed under the propositional connectives, and both
    ;; tags are first-class formula-code grammar entries, so the classifier
    ;; must accept them or reflected Group-2b clause formulas could never
    ;; classify (ADR-0087).
    not (delta-star-0? (second formula))
    implies (and (delta-star-0? (second formula))
                 (delta-star-0? (nth formula 2)))
    bounded-forall (let [{:keys [body]} (:body (second formula))]
                     (delta-star-0? body))
    bounded-exists (let [{:keys [body]} (:body (second formula))]
                     (delta-star-0? body))
    false))

(defn- strip-prefix
  [formula tag]
  (loop [current formula
         stripped? false]
    (if (= tag (ast/tag-of current))
      (let [tied (second current)]
        (recur (:body tied) true))
      [stripped? current])))

(defn pi-star-1?
  "Return true for a universal prefix over a Delta-star-0 matrix."
  [formula]
  (let [[stripped? matrix] (strip-prefix formula 'forall)]
    (and stripped? (delta-star-0? matrix))))

(defn sigma-star-1?
  "Return true for an existential prefix over a Delta-star-0 matrix."
  [formula]
  (let [[stripped? matrix] (strip-prefix formula 'exists)]
    (and stripped? (delta-star-0? matrix))))

(defn- guarded-existential-body?
  "Recognize the bounded-existential desugaring under `binding`:
   `(and (leq (var binding) bound) rest)` (ADR-0092)."
  [body binding]
  (and (= 'and (ast/tag-of body))
       (let [guard (second body)]
         (and (= 'pos (ast/tag-of guard))
              (let [atom (second guard)]
                (and (= 'app (ast/tag-of atom))
                     (= 'leq (second atom))
                     (= (ast/var-term binding) (nth atom 2))))))))

(defn- nnf-pi-star-1-shape?
  "True when an NNF formula contains no positive unbounded existential.

   In NNF every quantifier sits in positive position, unbounded universals
   prenex outward classically, and bounded quantifiers belong to the
   Delta-star-0 matrix, so this is the syntactic core of having a Pi-star-1
   encoding. Guarded existentials of the bounded desugaring shape
   `(exists x (and (leq x bound) body))` count as bounded (ADR-0092)."
  [formula]
  (case (ast/tag-of formula)
    true true
    false true
    pos true
    neg true
    eq true
    neq true
    and (and (nnf-pi-star-1-shape? (second formula))
             (nnf-pi-star-1-shape? (nth formula 2)))
    or (and (nnf-pi-star-1-shape? (second formula))
            (nnf-pi-star-1-shape? (nth formula 2)))
    forall (nnf-pi-star-1-shape? (:body (second formula)))
    once-forall (nnf-pi-star-1-shape? (:body (second formula)))
    bounded-forall (let [{:keys [body]} (:body (second formula))]
                     (nnf-pi-star-1-shape? body))
    bounded-exists (let [{:keys [body]} (:body (second formula))]
                     (nnf-pi-star-1-shape? body))
    exists (let [tied (second formula)
                 binding (:binding-nom tied)
                 body (:body tied)]
             (and (guarded-existential-body? body binding)
                  (nnf-pi-star-1-shape? (nth body 2))))
    false))

(defn pi-star-1-encodable?
  "Return true when `formula` has a Pi-star-1 encoding.

   Willard 2013 Definition 5.1 admits any axiom with a Pi-star-1 encoding
   into the finite reflected basis, which is a semantic latitude: an
   antecedent existential prenexes universally, so classification must look
   at negation normal form rather than the presented surface shape
   (ADR-0092). The shape classifiers above keep their strict presented-form
   readings; they are accepted first as the cheap common case."
  [formula]
  (or (delta-star-0? formula)
      (pi-star-1? formula)
      (nnf-pi-star-1-shape? (normalize/to-nnf formula))))

;; -----------------------------------------------------------------------------
;; Stable source coding
;; -----------------------------------------------------------------------------

(defn- canonical-term
  [term env]
  (case (ast/tag-of term)
    var (list 'var (get env (second term) (second term)))
    par (list 'par (get env (second term) (second term)))
    app (list* 'app (second term) (map #(canonical-term % env) (nnext term)))
    term))

(declare canonical-formula)

(defn- bind-name
  [env binding-nom]
  (let [label (symbol (str "v" (count env)))]
    [(assoc env binding-nom label) label]))

(defn- canonical-quantifier
  [tag tied env]
  (let [[env* label] (bind-name env (:binding-nom tied))]
    (list tag label (canonical-formula (:body tied) env*))))

(defn- canonical-bounded
  [tag tied env]
  (let [[env* label] (bind-name env (:binding-nom tied))
        {:keys [bound body]} (:body tied)]
    (list tag label (canonical-term bound env)
          (canonical-formula body env*))))

(defn- canonical-formula
  [formula env]
  (case (ast/tag-of formula)
    true '(true)
    false '(false)
    pos (list 'pos (canonical-term (second formula) env))
    neg (list 'neg (canonical-term (second formula) env))
    eq (list 'eq
             (canonical-term (second formula) env)
             (canonical-term (nth formula 2) env))
    neq (list 'neq
              (canonical-term (second formula) env)
              (canonical-term (nth formula 2) env))
    and (list 'and
              (canonical-formula (second formula) env)
              (canonical-formula (nth formula 2) env))
    or (list 'or
             (canonical-formula (second formula) env)
             (canonical-formula (nth formula 2) env))
    not (list 'not (canonical-formula (second formula) env))
    implies (list 'implies
                  (canonical-formula (second formula) env)
                  (canonical-formula (nth formula 2) env))
    forall (canonical-quantifier 'forall (second formula) env)
    once-forall (canonical-quantifier 'once-forall (second formula) env)
    exists (canonical-quantifier 'exists (second formula) env)
    bounded-forall (canonical-bounded 'bounded-forall (second formula) env)
    bounded-exists (canonical-bounded 'bounded-exists (second formula) env)
    formula))

(defn- canonical-code-nom
  "Return the kernel nom used when a formula-code variable index is decoded.

   Formula codes do not remember arbitrary host nom identities. They remember
   de Bruijn-like byte labels `v0`, `v1`, ... generated by `canonical-formula`.
   The SJAS proof profile decodes byte label `vN` as host nom `sjas-vN`; theorem
   queries use the same convention so certificates generated from source
   formulas can later be checked against structurally decoded theorem codes."
  [label]
  (let [name* (when (symbol? label) (name label))]
    (if (and name*
             (re-matches #"v[0-9]+" name*))
      (or (sjas-code/code-nom
            (inc (Long/parseLong (subs name* 1))))
          label)
      label)))

(declare canonical-term->ast canonical-formula->ast)

(defn- canonical-term->ast
  [term]
  (case (first term)
    var (ast/var-term (canonical-code-nom (second term)))
    par (ast/par-term (canonical-code-nom (second term)))
    app (list* 'app (second term) (map canonical-term->ast (nnext term)))
    term))

(defn- canonical-formula->ast
  [formula]
  (case (first formula)
    true (ast/true-form)
    false (ast/false-form)
    pos (ast/pos-lit (canonical-term->ast (second formula)))
    neg (ast/neg-lit (canonical-term->ast (second formula)))
    eq (ast/eq-lit (canonical-term->ast (second formula))
                   (canonical-term->ast (nth formula 2)))
    neq (ast/neq-lit (canonical-term->ast (second formula))
                     (canonical-term->ast (nth formula 2)))
    and (ast/and-form (canonical-formula->ast (second formula))
                      (canonical-formula->ast (nth formula 2)))
    or (ast/or-form (canonical-formula->ast (second formula))
                    (canonical-formula->ast (nth formula 2)))
    not (ast/not-form (canonical-formula->ast (second formula)))
    implies (ast/implies-form (canonical-formula->ast (second formula))
                              (canonical-formula->ast (nth formula 2)))
    forall (ast/forall-form (canonical-code-nom (second formula))
                            (canonical-formula->ast (nth formula 2)))
    once-forall (ast/once-forall-form (canonical-code-nom (second formula))
                                      (canonical-formula->ast (nth formula 2)))
    exists (ast/exists-form (canonical-code-nom (second formula))
                            (canonical-formula->ast (nth formula 2)))
    bounded-forall (bounded-forall (canonical-code-nom (second formula))
                                   (canonical-term->ast (nth formula 2))
                                   (canonical-formula->ast (nth formula 3)))
    bounded-exists (bounded-exists (canonical-code-nom (second formula))
                                   (canonical-term->ast (nth formula 2))
                                   (canonical-formula->ast (nth formula 3)))
    formula))

(defn- code-canonical-formula
  "Rewrite a source formula to the binder names used by structural code decode."
  [formula]
  (canonical-formula->ast (canonical-formula formula {})))

(defn- canonical-clause
  [{:keys [relation params body]}]
  (let [env (into {} (map-indexed (fn [idx nom]
                                    [nom (symbol (str "p" idx))])
                                  params))]
    {:relation relation
     :arity (count params)
     :body (canonical-formula body env)}))

(defn- canonical-system-source
  [{:keys [profile beta reflected-clauses]}]
  {:profile profile
   :beta (mapv #(canonical-formula % {}) beta)
   :reflected-clauses (mapv canonical-clause reflected-clauses)})

(defn- declared-coding-symbols
  "Return every object-language symbol that may appear in generated SJAS code.

   The Godel byte code for a formula refers to ordinary language symbols by
   finite indexes. Compact `code-N` constructors are treated specially by the
   code encoder, so they are declared for validation but not included in this
  formula-symbol table."
  [constants functions relations reflected-clauses external-clauses]
  (concat base-constants
          (keys u-grounding-arithmetic-functions)
          (keys base-relations)
          constants
          (keys functions)
          (keys relations)
          (map :relation reflected-clauses)
          (map :relation external-clauses)))

(defn- formula-code-term
  ([coding-context formula]
   (formula-code-term coding-context formula {} :compact))
  ([coding-context formula env]
   (formula-code-term coding-context formula env :compact))
  ([coding-context formula env code-format]
   (let [canonical (canonical-formula formula env)
         bytes (sjas-code/canonical-formula-code-bytes coding-context canonical)
         effective-format
         (if (and (= :compact code-format)
                  (> (count bytes) sjas-code/max-code-bytes))
           :u-grounding
           code-format)]
     ;; Compact `code-N` constructors have a finite declared arity. Oversized
     ;; generated targets retain the same exact byte string through the
     ;; sentinel-terminated U-Grounding representation, which the object-level
     ;; code reader already treats as an equivalent public code format.
     (sjas-code/bytes->formal-code-term effective-format bytes))))

(defn formula-code
  "Return the public SJAS Godel-code term for `formula` in `system`'s language."
  [system formula]
  (formula-code-term (:coding-context system)
                     formula
                     {}
                     (:code-format system :compact)))

(defn- formal-code-term-bytes
  "Decode one public formula/system/proof code term back to its exact bytes."
  [code-term]
  (or (sjas-code/code-term-bytes code-term)
      (sjas-code/u-grounding-code-term-bytes code-term)
      (throw (ex-info "Expected an SJAS formal code term"
                      {:code-term code-term}))))

(defn- proof-side-antecedent-formula
  "Return the formula shape reconstructed for proof-predicate antecedents.

   The arithmeticized proof predicates decode finite-system formulas through
   `sjas-proof-antecedent-formula-asto`: formulas are put in NNF and antecedent
   universals become single-use branch formulas. Structural proof objects
   generated at the source boundary must use the same shape in their root
   formula bytes."
  [formula]
  (letfn [(once-forall-antecedents [formula]
            (case (ast/tag-of formula)
              and (ast/and-form
                    (once-forall-antecedents (second formula))
                    (once-forall-antecedents (nth formula 2)))
              or (ast/or-form
                   (once-forall-antecedents (second formula))
                   (once-forall-antecedents (nth formula 2)))
              forall (ast/once-forall-form
                       (:binding-nom (second formula))
                       (once-forall-antecedents (:body (second formula))))
              once-forall (ast/once-forall-form
                             (:binding-nom (second formula))
                             (once-forall-antecedents (:body (second formula))))
              exists (ast/exists-form
                       (:binding-nom (second formula))
                       (once-forall-antecedents (:body (second formula))))
              formula))]
    (once-forall-antecedents (normalize/to-nnf formula))))

(defn- structural-byte-list-tableau-node
  "Encode one formula-bearing structural proof node using explicit byte lists."
  [system formula & children]
  (apply list
         (cons (apply list (formal-code-term-bytes (formula-code system
                                                                 formula)))
               children)))

(defn- canonical-structural-byte-list-tableau-node
  "Encode one structural proof node from canonical formula-code syntax."
  [system canonical-formula & children]
  (let [code (sjas-code/canonical-formula-formal-code-term
               (:coding-context system)
               canonical-formula
               (:code-format system :compact))]
    (apply list (cons (apply list (formal-code-term-bytes code)) children))))

(defn selfcons-godel-code-report
  "Return the concrete Group-3 self-consistency Godel code for `system`.

   This is a reporting boundary for ADR-0073 Track 1. It reads the generated
   Group-3 formula code term through the same formal code bytes used by the
   object proof predicate, then exposes the ordinary base-64 natural-number
   view. The exact byte sequence is included because compact public code terms
   are byte strings and may not be reconstructed by lossy natural round-trips
   when trailing zero bytes matter."
  [system]
  (let [{:keys [group formula code]} (:group-three system)
        bytes (formal-code-term-bytes code)]
    {:profile (:profile system)
     :code-format (:code-format system :compact)
     :group group
     :formula formula
     :code-term code
     :bytes bytes
     :byte-count (count bytes)
     :godel-number (sjas-code/bytes->natural bytes)
     :u-grounding-number (sjas-code/bytes->u-grounding-code-value bytes)}))

(defn selfcons-godel-code
  "Return the decimal natural Godel code for `system`'s Group-3 formula."
  [system]
  (:godel-number (selfcons-godel-code-report system)))

(declare system)

(defn print-selfcons-godel-code
  "Print the concrete ordinary-tableau Group-3 self-consistency Godel code.

   With no argument, print the code for the default ordinary-tableau
   `IS#_D(beta)` instance. With `system`, print that system's Group-3 code.
   The printed representation is plain decimal digits rather than Clojure's
   readable bigint syntax."
  ([]
   (print-selfcons-godel-code (system {:profile :willard-sjas-tableau0})))
  ([system]
   (let [value (selfcons-godel-code system)]
     (println (str value))
     value)))

;; -----------------------------------------------------------------------------
;; Generated formulas and program clauses
;; -----------------------------------------------------------------------------

(defn- and*
  [formulae]
  (case (count formulae)
    0 (ast/true-form)
    1 (first formulae)
    (reduce ast/and-form formulae)))

(defn- clause->formula
  [{:keys [relation params body]}]
  (let [head (ast/pos-lit
               (apply ast/app-term relation (map ast/var-term params)))
        implication (ast/implies-form body head)]
    (reduce (fn [inner param]
              (ast/forall-form param inner))
            implication
            (reverse params))))

(defn- validate-reflected-basis!
  "Reject reflected basis formulas without Pi*1 encodings.

   Group-2 beta members and reflected Group-2b clause formulas become proper
   axioms cited by the self-referential Group-3 sentence, so Willard 2013
   Definition 5.1's formula-class precondition is enforced at the source
   boundary. External clauses stay outside the reflected basis and are not
   constrained. Truth of beta in the standard model remains Willard's external
   consistency-preservation premise; the builder checks only the class."
  [beta reflected-clauses]
  (doseq [[group formula] (concat
                            (map vector (repeat :group-two) beta)
                            (map vector (repeat :group-two-b)
                                 (map clause->formula reflected-clauses)))]
    (when-not (pi-star-1-encodable? formula)
      (throw (ex-info
               "SJAS reflected basis formula lacks a Pi*1 encoding (Willard 2013 Definition 5.1)"
               {:group group
                :formula (canonical-formula formula {})})))))

(defn- group-zero-formulas
  []
  [(ast/neq-lit one zero)
   (ast/neq-lit two zero)])

(defn- group-one-formulas
  []
  [(ast/eq-lit (add-term zero zero) zero)
   (ast/eq-lit (dbl-term one) two)
   (ast/eq-lit (sub-term two one) one)])

(defn- selfcons0-formula
  [system-code contradiction-code]
  (let [p (nominal/nom (lvar 'p))]
    (ast/forall-form
      p
      (ast/neg-lit
        (ast/app-term 'dsjas-tableau-proof
                      system-code
                      contradiction-code
                      (ast/var-term p))))))

(defn- selfcons1-formula
  "Willard 2013 sentence (7) with `Pair(x,y)` encoded as the conjunction of
   the reserved atoms `pi-star-1-code(x)` and `neg-pair(x,y)`: the Level-1
   declaration covers only Pi-star-1 sentence/complement pairs (ADR-0087)."
  [system-code substitution-code]
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        p (nominal/nom (lvar 'p))
        q (nominal/nom (lvar 'q))]
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/forall-form
          p
          (ast/forall-form
            q
            (ast/or-form
              (ast/neg-lit
                (ast/app-term 'pi-star-1-code
                              (ast/var-term x)))
              (ast/or-form
                (ast/neg-lit
                  (ast/app-term 'neg-pair
                                (ast/var-term x)
                                (ast/var-term y)))
                (ast/or-form
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term x)
                                  (ast/var-term p)))
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term y)
                                  (ast/var-term q))))))))))))

(defn- selfcons1-record
  "Build Willard's fixed-point shaped Level-1 Group-3 record.

   Appendix A first forms a skeleton Gamma_1(g), then substitutes the numeral for
   the skeleton's own code into g. Proflog mirrors that shape with a free object
   variable in the skeleton and either a compact code term or a U-Grounding
   numeral code term in the final sentence."
  [coding-context system-code code-format]
  (let [g (nominal/nom (lvar 'g))
        skeleton (selfcons1-formula system-code (ast/var-term g))
        skeleton-code (formula-code-term coding-context skeleton {g 'v0} code-format)
        formula (selfcons1-formula system-code skeleton-code)]
    {:group :group-three
     :formula formula
     :code (formula-code-term coding-context formula {} code-format)
     :selfcons-skeleton-formula skeleton
     :selfcons-skeleton-code skeleton-code}))

(defn- selfcons-tab1-formula
  "Tab-1 Level-1 consistency sentence over measured proof-list objects.

   ADR-0120 only generates the relation-symbol shape. Later ADRs must provide
   the arithmeticized `dsjas-tab1-proof/3` validation relation."
  [system-code]
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        p (nominal/nom (lvar 'p))
        q (nominal/nom (lvar 'q))]
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/forall-form
          p
          (ast/forall-form
            q
            (ast/or-form
              (ast/neg-lit
                (ast/app-term 'pi-star-1-code
                              (ast/var-term x)))
              (ast/or-form
                (ast/neg-lit
                  (ast/app-term 'neg-pair
                                (ast/var-term x)
                                (ast/var-term y)))
                (ast/or-form
                  (ast/neg-lit
                    (ast/app-term 'dsjas-tab1-proof
                                  system-code
                                  (ast/var-term x)
                                  (ast/var-term p)))
                  (ast/neg-lit
                    (ast/app-term 'dsjas-tab1-proof
                                  system-code
                                  (ast/var-term y)
                                  (ast/var-term q))))))))))))

(defn- selfcons-tab2-boundary-formula
  "Target-only Tab-2-or-stronger consistency sentence.

   The profile deliberately names `dsjas-tab2-proof/3` without implementing its
   proof checker. Unlike Tab-1, this boundary target has no `Pi*_1` guard:
   allowing stronger intermediate theorem classes is the unsafe move under
   examination."
  [system-code]
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        p (nominal/nom (lvar 'p))
        q (nominal/nom (lvar 'q))
        witness (nominal/nom (lvar 'tab2-witness))]
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/forall-form
          p
          (ast/forall-form
            q
            ;; A final vacuous existential makes the generated sentence a
            ;; genuine Pi-2 formula while preserving the original nonempty
            ;; natural-number semantics of the consistency condition.
            (ast/exists-form
              witness
              (ast/or-form
                (ast/neg-lit
                  (ast/app-term 'neg-pair
                                (ast/var-term x)
                                (ast/var-term y)))
                (ast/or-form
                  (ast/neg-lit
                    (ast/app-term 'dsjas-tab2-proof
                                  system-code
                                  (ast/var-term x)
                                  (ast/var-term p)))
                  (ast/neg-lit
                    (ast/app-term 'dsjas-tab2-proof
                                  system-code
                                  (ast/var-term y)
                                  (ast/var-term q))))))))))))

(defn- group-three-record
  [profile coding-context system-code contradiction-code code-format]
  (case profile
    :willard-sjas-tableau0 (let [formula (selfcons0-formula system-code
                                                            contradiction-code)]
                             {:group :group-three
                              :formula formula
                              :code (formula-code-term coding-context
                                                       formula
                                                       {}
                                                       code-format)})
    :willard-sjas-level1 (selfcons1-record coding-context system-code code-format)
    :willard-sjas-total-multiplication
    (selfcons1-record coding-context system-code code-format)
    :willard-sjas-xtab (selfcons1-record coding-context system-code code-format)
    :willard-sjas-tab1 (let [formula (selfcons-tab1-formula system-code)]
                         {:group :group-three
                          :formula formula
                          :code (formula-code-term coding-context
                                                   formula
                                                   {}
                                                   code-format)})
    :willard-sjas-tab2-boundary
    (let [formula (selfcons-tab2-boundary-formula system-code)]
      {:group :group-three
       :formula formula
       :code (formula-code-term coding-context
                                formula
                                {}
                                code-format)})
    :willard-sjas-tab2
    (let [formula (selfcons-tab2-boundary-formula system-code)]
      {:group :group-three
       :formula formula
       :code (formula-code-term coding-context formula {} code-format)})
    (throw (ex-info "Unsupported Willard SJAS profile"
                    {:profile profile}))))

(defn- axiom-records
  [profile coding-context system-code contradiction-code code-format beta reflected-clauses]
  (let [grouped (concat
                  (map vector (repeat :group-zero) (group-zero-formulas))
                  (map vector (repeat :group-one) (group-one-formulas))
                  (map vector (repeat :group-two) beta)
                  (map vector (repeat :group-two-b)
                       (map clause->formula reflected-clauses)))
        initial (map-indexed (fn [idx [group formula]]
                               {:group group
                                :formula formula
                                :code (formula-code-term coding-context
                                                         formula
                                                         {}
                                                         code-format)})
                             grouped)
        group3 (group-three-record profile
                                   coding-context
                                   system-code
                                   contradiction-code
                                   code-format)]
    (vec (concat initial [group3]))))

(defn- compile-language
  [profile extra-relations constants extra-functions code-format]
  (language/language
    {:constants (vec (distinct (concat base-constants constants)))
     :functions (merge (functions-for-code-format code-format)
                       extra-functions)
     :relations (merge base-relations extra-relations)
     :proof-profile profile}))

(defn system
  "Build a finite reflected SJAS system.

   Options:
   - `:profile`: `:willard-sjas-tableau0`, `:willard-sjas-level1`,
     `:willard-sjas-tab1`, or target-only `:willard-sjas-tab2-boundary`;
   - `:code-format`: `:compact` for `code-N` terms or `:u-grounding` for
     ordinary binary numeral codes;
   - `:constants`: extra user constants;
   - `:functions`: extra user function declarations;
   - `:relations`: extra user relation declarations;
   - `:beta`: finite reflected proper axioms;
   - `:reflected-clauses`: user clauses included in the reflected basis;
   - `:external-clauses`: ordinary Proflog clauses outside the self-reference.
   "
  [{:keys [profile code-format constants functions relations beta reflected-clauses external-clauses]
    :or {profile :willard-sjas-tableau0
         code-format :compact
         constants []
         functions {}
         relations {}
         beta []
         reflected-clauses []
         external-clauses []}}]
  (let [reflected-clauses (vec reflected-clauses)
        external-clauses (vec external-clauses)
        beta (vec beta)
        relations (if (contains? #{:willard-sjas-tab2-boundary
                                   :willard-sjas-tab2}
                                 profile)
                    (merge relations tab2-boundary-relations)
                    relations)
        _ (validate-reflected-basis! beta reflected-clauses)
        source {:profile profile
                :beta beta
                :reflected-clauses reflected-clauses}
        derived-relations (into {}
                                (map (fn [{:keys [relation params]}]
                                       [relation (count params)]))
                                (concat reflected-clauses external-clauses))
        coding-context (sjas-code/context
                         (declared-coding-symbols constants
                                                 functions
                                                 (merge relations derived-relations)
                                                 reflected-clauses
                                                 external-clauses))
        canonical-source (canonical-system-source source)
        system-code (sjas-code/system-formal-code-term coding-context
                                                       canonical-source
                                                       code-format)
        contradiction-code (sjas-code/canonical-formula-formal-code-term
                             coding-context
                             (canonical-formula (ast/eq-lit zero one) {})
                             code-format)
        axioms (axiom-records profile
                              coding-context
                              system-code
                              contradiction-code
                              code-format
                              beta
                              reflected-clauses)
        lang (compile-language profile
                               (merge relations derived-relations)
                               constants
                               functions
                               code-format)
        clauses (concat reflected-clauses
                        external-clauses)
        ;; Formula codes remember canonical binder indexes rather than arbitrary
        ;; source nom identities. The proof predicate must later reconstruct the
        ;; same axiom antecedent from `system-code`, so theorem queries use that
        ;; canonical binder convention at the source-compilation boundary.
        axiom-formula (and* (map (comp code-canonical-formula :formula)
                                 axioms))
        group3 (first (filter #(= :group-three (:group %)) axioms))
        ;; The user-facing program includes both reflected and external clauses.
        ;; Proof-predicate validation is narrower: it may use only clauses whose
        ;; source appears in the encoded SJAS system. The profile now recovers
        ;; reflected proof-time calls from `system-code`, so no reflected
        ;; compiled-program side table is stored in the registry.
        registry (atom {:sjas/system-code system-code
                        :sjas/code-format code-format})
        program (assoc (language/compile-program lang clauses)
                       :sjas/registry registry)]
    {:profile profile
     :code-format code-format
     :language lang
     :program program
     :system-code system-code
     :contradiction-code contradiction-code
     :selfcons-skeleton-code (:selfcons-skeleton-code group3)
     :selfcons-skeleton-formula (:selfcons-skeleton-formula group3)
     :coding-context coding-context
     :axioms axioms
     :group-three group3
     :axiom-formula axiom-formula
     :reflected-clauses (vec reflected-clauses)
     :external-clauses (vec external-clauses)}))

(defn pair-extended-system
  "Build an SJAS system whose reflected beta basis includes pair projections.

   User-supplied `:functions` and `:beta` options are preserved and merged with
   the ADR-0123 pair fragment. The pair axioms are placed in reflected beta,
   not in `:external-clauses`, so the encoded finite source and generated
   Group-3/SelfCons code include the extension."
  ([]
   (pair-extended-system {}))
  ([opts]
   (let [{pair-beta :beta
          pair-signature :functions} (pair-extension-options)]
     (system
       (assoc opts
              :functions (merge (:functions opts) pair-signature)
              :beta (vec (concat pair-beta (:beta opts))))))))

(defn list-extended-system
  "Build an SJAS system with the ADR-0128 pair-backed list layer.

   The extension includes the ADR-0123 pair beta records plus the finite list
   constructor/projection beta laws. The list constants, functions, and beta
   records are reflected into the generated source, so system identity and the
   Level-1 SelfCons statement change when the list layer is selected."
  ([]
   (list-extended-system {}))
  ([opts]
   (let [{list-constants :constants
          list-beta :beta
          list-signature :functions} (list-extension-options)]
     (system
       (assoc opts
              :constants (vec (concat list-constants
                                      (:constants opts)))
              :functions (merge (:functions opts) list-signature)
              :beta (vec (concat list-beta (:beta opts))))))))

(defn total-multiplication-boundary-system
  "Build the ADR-0124 total-multiplication negative-variant surface.

   The resulting system has `mul/2` in its object-language function signature
   and includes the seed laws as reflected beta axioms. It is deliberately not
   a completed Workstream B witness; callers should consult
   `proflog.sjas-correspondence/audit-boundary-failure-roadmap` before treating
   this variant as negative evidence."
  ([]
   (total-multiplication-boundary-system {}))
  ([opts]
   (let [{seed-beta :beta
          seed-signature :functions} (total-multiplication-boundary-options)]
     (system
       (assoc opts
              :functions (merge (:functions opts) seed-signature)
              :beta (vec (concat seed-beta (:beta opts))))))))

(defn total-multiplication-reduced-witness-system
  "Build the ADR-0125 reduced squaring-chain witness system.

   `:depth` controls the finite chain length and defaults to 3 for focused
   regression tests. The returned system is still not a full Workstream B
   contradiction witness; it only installs the reflected beta compression
   fragment that later total-multiplication diagonal probes must use."
  ([]
   (total-multiplication-reduced-witness-system {}))
  ([opts]
   (let [depth (:depth opts 3)
         system-opts (dissoc opts :depth)
         {witness-constants :constants
          witness-beta :beta
          witness-signature :functions}
         (total-multiplication-reduced-witness-options depth)]
     (system
       (assoc system-opts
              :constants (vec (concat witness-constants
                                      (:constants system-opts)))
              :functions (merge (:functions system-opts) witness-signature)
              :beta (vec (concat witness-beta (:beta system-opts))))))))

(defn total-multiplication-complete-system
  "Build the theorem-eligible Type-M boundary profile used by ADR-0141.

   The profile has a distinct encoded identity, interpreted `mul/2`, the full
   reflected multiplication basis, and the finite squaring-chain witness."
  ([]
   (total-multiplication-complete-system {}))
  ([opts]
   (let [depth (:depth opts 3)
         system-opts (dissoc opts :depth :profile)
         witness-constants (total-multiplication-squaring-chain-constants depth)
         witness-beta (total-multiplication-squaring-chain-axioms depth)]
     (system
       (assoc system-opts
              :profile :willard-sjas-total-multiplication
              :constants (vec (concat witness-constants
                                      (:constants system-opts)))
              :relations (merge (:relations system-opts)
                                total-multiplication-willard-relations)
              :functions (merge (:functions system-opts)
                                total-multiplication-functions)
              :beta (vec (concat (total-multiplication-complete-axioms)
                                 (total-multiplication-willard-route-axioms
                                   contradiction-code)
                                 witness-beta
                                 (:beta system-opts))))))))

(defn total-multiplication-hypothesis-report
  "Check the executable Type-M assumptions represented by `system`.

   Representative non-seed products are run through the system program. Exact
   reflected-basis membership is checked after alpha-insensitive canonical
   conversion, so a profile label by itself cannot satisfy this report."
  [system]
  (let [canonical #(canonical-formula % {})
        beta-set (set (map (comp canonical :formula)
                           (filter #(= :group-two (:group %))
                                   (:axioms system))))
        required-set (set (map canonical
                               (total-multiplication-complete-axioms)))
        products [[2 3 6] [3 4 12] [5 5 25]]
        product-checks
        (mapv (fn [[left right product]]
                (boolean
                  (seq (query/query-succeeds
                         (:program system)
                         (ast/eq-lit (mul-term (numeral left)
                                               (numeral right))
                                     (numeral product))
                         1
                         180))))
              products)
        profile-valid? (= :willard-sjas-total-multiplication
                          (:profile system))
        signature-valid? (= 2 (get-in system [:language :functions 'mul]))
        basis-valid? (every? beta-set required-set)
        interpreted? (every? true? product-checks)]
    {:profile-valid? profile-valid?
     :signature-valid? signature-valid?
     :reflected-basis-valid? basis-valid?
     :product-checks product-checks
     :interpreted-total-function? interpreted?
     :theorem-hypotheses-satisfied?
     (and profile-valid? signature-valid? basis-valid? interpreted?)}))

(defn xtab-lem-reduced-witness-system
  "Build the ADR-0133 reduced Xtab/LEM reflected-beta witness system.

   The resulting system includes a finite universal LEM seed over
   `xtab-lem-demo/1`. It remains a reduced witness only; the full generated
   SelfCons contradiction target and final evidence are separate Workstream B
   obligations."
  ([]
   (xtab-lem-reduced-witness-system {}))
  ([opts]
   (let [{witness-relations :relations
          witness-beta :beta} (xtab-lem-reduced-witness-options)]
     (system
       (assoc opts
              :relations (merge (:relations opts) witness-relations)
              :beta (vec (concat witness-beta (:beta opts))))))))

(defn xtab-complete-system
  "Build the executable Xtab boundary profile.

   Unlike the ADR-0133 surface, this profile's encoded system tag enables the
   formula-independent LEM injection rule in the arithmeticized structural
   proof checker. The reflected seed remains as an inspectable reduced witness,
   but it is not the implementation of the schema."
  ([]
   (xtab-complete-system {}))
  ([opts]
   (let [system-opts (dissoc opts :profile)
         {witness-relations :relations
          witness-beta :beta} (xtab-lem-reduced-witness-options)]
     (system
       (assoc system-opts
              :profile :willard-sjas-xtab
              :relations (merge (:relations system-opts) witness-relations)
              :beta (vec (concat (boundary-arithmetic-basis-axioms)
                                 witness-beta
                                 (:beta system-opts))))))))

(defn- xtab-lem-formula
  [formula]
  (ast/or-form formula (normalize/negate-formula formula)))

(defn- xtab-injection-proof
  "Build a small explicit cut tree over `target` and `injection`.

   The pending target is `false`. Both Xtab branches save their selected
   literal and then close by selecting that pending target; the proof therefore
   isolates LEM injection from unrelated conjunction or equality scheduling."
  [system phi injection]
  (let [not-phi (normalize/negate-formula phi)
        target (ast/false-form)
        left-branch
        (structural-byte-list-tableau-node
          system phi
          (structural-byte-list-tableau-node system target))
        right-branch
        (structural-byte-list-tableau-node
          system not-phi
          (structural-byte-list-tableau-node system target))]
    {:target target
     :proof (list 'xtab-lem
                  (structural-byte-list-tableau-node system
                                                      injection
                                                      left-branch
                                                      right-branch))}))

(defn xtab-lem-injection-diagnostic
  "Validate one explicit formula-independent Xtab injection.

   An atomic argument requests its LEM instance. An `or` argument is treated as
   an already supplied injection formula, which lets callers verify that a
   non-complementary disjunction is rejected."
  [system candidate]
  (let [supplied-disjunction? (= 'or (ast/tag-of candidate))
        supplied-left (when supplied-disjunction? (second candidate))
        supplied-right (when supplied-disjunction? (nth candidate 2))
        supplied-lem? (and supplied-disjunction?
                           (or (= (normalize/negate-formula supplied-left)
                                  supplied-right)
                               (= (normalize/negate-formula supplied-right)
                                  supplied-left)))
        profile-eligible? (= :willard-sjas-xtab (:profile system))
        injection (if supplied-disjunction?
                    candidate
                    (xtab-lem-formula candidate))
        phi (if supplied-disjunction? supplied-left candidate)
        {:keys [target proof]} (xtab-injection-proof system phi injection)
        decoded-target (willard-sjas-profile/decoded-proof-formula
                         (:program system)
                         (formula-code system target))
        eligible? (and profile-eligible?
                       (or (not supplied-disjunction?) supplied-lem?))
        valid? (and eligible?
                    (willard-sjas-profile/structural-proof-valid?
                      (:program system)
                      (:system-code system)
                      decoded-target
                      proof
                      220))]
    {:profile (:profile system)
     :candidate candidate
     :injection-formula injection
     :target decoded-target
     :proof-code (proof-certificate proof)
     :injection-valid? valid?
     :measured-proof-valid? valid?}))

(defn tab2-rank2-witness-formula
  "Return the ADR-0136 reduced witness formula for Tab-2-or-stronger reuse.

   The formula is intentionally a `forall`-then-`exists` theorem shape outside
   the implemented Tab-1 intermediate classes. It is not intended as a useful
   mathematical theorem; it is a compact executable witness for the deduction
   strength boundary."
  []
  (boundary-axioms/tab2-rank2-witness-formula))

(defn- tab2-rank2-witness-proof
  "Return an ordinary structural tableau proof for the ADR-0136 witness."
  [system witness-formula]
  (let [negated-witness (normalize/negate-formula witness-formula)
        target (ast/and-form
                 (proof-side-antecedent-formula (:axiom-formula system))
                 negated-witness)
        canonical-false (list 'false)
        canonical-once-forall (list 'once-forall 'v1 canonical-false)
        canonical-negated (list 'exists 'v0 canonical-once-forall)]
    (structural-byte-list-tableau-node
      system
      target
      (canonical-structural-byte-list-tableau-node
        system
        canonical-negated
        (canonical-structural-byte-list-tableau-node
          system
          canonical-once-forall
          (canonical-structural-byte-list-tableau-node
            system
            canonical-false))))))

(defn tab2-or-stronger-reduced-witness-report
  "Describe the ADR-0136 reduced witness for the Tab-2-or-stronger variant.

   Classification is recorded against the implemented Tab-1 proof-list
   baseline. Ordinary theorem validity is checked through the existing
   Tableau-0 structural `tableau-proof/3` path because this ADR does not add a
   Tab-2 proof-list checker."
  ([]
   (tab2-or-stronger-reduced-witness-report {}))
  ([opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 360)
         proof-validation-profile (:proof-validation-profile opts
                                                             :willard-sjas-tableau0)
         system-opts (dissoc opts
                             :fuel
                             :proof-limit
                             :proof-validation-profile)
         baseline-system (system (assoc system-opts
                                        :profile :willard-sjas-tab1))
         validation-system (system (assoc system-opts
                                          :profile proof-validation-profile))
         witness-formula (tab2-rank2-witness-formula)
         witness-formula-code (formula-code baseline-system witness-formula)
         validation-theorem-code (formula-code validation-system
                                               witness-formula)
         proof (tab2-rank2-witness-proof validation-system witness-formula)
         proof-code (proof-certificate proof)
         proofs (query/query-succeeds
                  (:program validation-system)
                  (tableau-proof (:system-code validation-system)
                                 validation-theorem-code
                                 proof-code)
                  proof-limit
                  fuel)]
     {:variant :tab-2-or-stronger
      :witness-stage :reduced-reflected-beta-witness
      :kind :rank2-theorem-shape
      :baseline-profile :willard-sjas-tab1
      :baseline-system-code (:system-code baseline-system)
      :witness-formula witness-formula
      :witness-formula-code witness-formula-code
      :tab1-classifier-status {:pi-star-1? (pi-star-1? witness-formula)
                               :sigma-star-1? (sigma-star-1? witness-formula)
                               :pi-star-1-encodable?
                               (pi-star-1-encodable? witness-formula)}
      :ordinary-tableau-validation
      {:validator :tableau-proof
       :profile (:profile validation-system)
       :system-code (:system-code validation-system)
       :theorem-code validation-theorem-code
       :proof-code proof-code
       :proof-limit proof-limit
       :fuel fuel
       :proof-count (count proofs)
       :proof-valid? (boolean (seq proofs))}
      :ordinary-tableau-proof-code proof-code
      :remaining-obligations #{:full-generated-selfcons-contradiction-target
                               :constructed-certificate
                               :proof-search-synthesis}
      :not-implemented #{:tab2-proof-checker
                         :full-generated-selfcons-contradiction-target}
      :completion-claimed? false})))

(defn tab2-or-stronger-full-target-system
  "Build the ADR-0137 target-only Tab-2-or-stronger boundary system.

   The generated system names `dsjas-tab2-proof/3` in Group-3/SelfCons but does
   not install a proof-profile method for that relation. It is a target
   generator for Workstream B, not a Tab-2 checker."
  ([]
   (tab2-or-stronger-full-target-system {}))
  ([opts]
   (system
     (assoc opts :profile :willard-sjas-tab2-boundary))))

(defn tab2-complete-system
  "Build the executable Tab-2 proof-list boundary system."
  ([]
   (tab2-complete-system {}))
  ([opts]
   (let [system-opts (dissoc opts :profile)]
     (system (assoc system-opts
                    :profile :willard-sjas-tab2
                    :beta (vec (concat (boundary-arithmetic-basis-axioms)
                                       (:beta system-opts))))))))

(defn tab2-rank2-reuse-fixture
  "Construct a two-entry proof list with a genuine Rank-2 intermediate.

   The first entry proves `forall x exists y true` structurally. The final
   entry reuses that earlier theorem through the proof-list axiom citation.
   Building this fixture for a Tab-1 system uses Tab-1 object tags so rejection
   occurs at the intermediate classifier rather than at object decoding."
  [system]
  (let [tab2? (= :willard-sjas-tab2 (:profile system))
        rank2-formula (tab2-rank2-witness-formula)
        rank2-code (formula-code system rank2-formula)
        rank2-proof-code (proof-certificate
                           (tab2-rank2-witness-proof system rank2-formula))
        target-code rank2-code
        axiom-proof-code (proof-certificate 'sjas-axiom)
        entries [{:theorem-code rank2-code
                  :proof-code rank2-proof-code}
                 {:theorem-code rank2-code
                  :proof-code axiom-proof-code}]
        tab1? (= :willard-sjas-tab1 (:profile system))
        proof-list-code (if tab1?
                          (tab1-proof-list-object entries)
                          (tab2-proof-list-object entries))
        proof-object-code (if tab1?
                            (dsjas-tab1-proof-object (:system-code system)
                                                     target-code
                                                     proof-list-code)
                            (dsjas-tab2-proof-object (:system-code system)
                                                     target-code
                                                     proof-list-code))]
    {:system-code (:system-code system)
     :target-code target-code
     :rank2-intermediate-code rank2-code
     :intermediate-class :rank-2
     :proof-list-code proof-list-code
     :proof-object-code proof-object-code
     :entries entries}))

(defn selfcons-negation-target
  "Return the generated negative SelfCons theorem target for `system`.

   The positive SelfCons sentence is the system's generated Group-3 theorem
   formula. Workstream B contradiction probes refute that exact generated
   statement, so this helper deliberately derives the target from
   `(:group-three system)` instead of rebuilding a parallel schema."
  [system]
  (normalize/negate-formula (:formula (:group-three system))))

(defn selfcons-refutation-target
  "Return `AxiomConj(S) /\\ not(SelfCons_S)` for `system`.

   This is the full formula a later contradiction certificate must close for a
   generated SJAS SelfCons target. It is a target constructor only; it performs
   no proof search and does not imply that a certificate has been found."
  [system]
  (ast/and-form (:axiom-formula system)
                (selfcons-negation-target system)))

(defn total-multiplication-full-target-report
  "Describe the generated target for the executable total-multiplication variant.

   ADR-0141 upgrades the earlier reduced-witness target to the complete Type-M
   profile with interpreted multiplication and the reflected arithmetic basis.
   Certificate and synthesis fields remain open until exact counterexample
   tuples pass the generated SelfCons body."
  ([]
   (total-multiplication-full-target-report {}))
  ([opts]
   (let [system (total-multiplication-complete-system opts)
         negated-selfcons (selfcons-negation-target system)
         refutation-target (ast/and-form (:axiom-formula system)
                                         negated-selfcons)]
     {:variant :total-multiplication
      :witness-stage :full-generated-selfcons-contradiction-target
      :system-code (:system-code system)
      :group-three-code (:code (:group-three system))
      :axiom-formula (:axiom-formula system)
      :selfcons-formula (:formula (:group-three system))
      :negated-selfcons-formula negated-selfcons
      :selfcons-refutation-target refutation-target
      :target-code (formula-code system refutation-target)
      :constructed-certificate-validation-helper
      'total-multiplication-constructed-certificate-validation
      :constructed-certificate-status :open
      :proof-search-synthesis-status :open
      :durable-probe-required? true})))

(defn xtab-lem-full-target-report
  "Describe the generated target for the executable Xtab/LEM-as-axiom variant.

   ADR-0141 upgrades the earlier reflected one-off LEM seed to the complete
   Xtab profile whose structural checker executes formula-independent LEM
   injection. Certificate and synthesis fields remain open until exact
   counterexample tuples pass the generated SelfCons body."
  ([]
   (xtab-lem-full-target-report {}))
  ([opts]
   (let [system (xtab-complete-system opts)
         negated-selfcons (selfcons-negation-target system)
         refutation-target (ast/and-form (:axiom-formula system)
                                         negated-selfcons)]
     {:variant :xtab-or-lem-axiom
      :witness-stage :full-generated-selfcons-contradiction-target
      :system-code (:system-code system)
      :group-three-code (:code (:group-three system))
      :axiom-formula (:axiom-formula system)
      :selfcons-formula (:formula (:group-three system))
      :negated-selfcons-formula negated-selfcons
      :selfcons-refutation-target refutation-target
      :target-code (formula-code system refutation-target)
      :constructed-certificate-status :open
      :proof-search-synthesis-status :open
      :durable-probe-required? true})))

(defn tab2-or-stronger-full-target-report
  "Describe the generated target for the executable Tab-2-or-stronger variant.

   ADR-0141 upgrades the earlier target-only syntax to the complete Tab-2
   profile with measured proof-list objects. Certificate and synthesis fields
   remain open until exact counterexample tuples pass the generated SelfCons
   body."
  ([]
   (tab2-or-stronger-full-target-report {}))
  ([opts]
   (let [system (tab2-complete-system opts)
         negated-selfcons (selfcons-negation-target system)
         refutation-target (ast/and-form (:axiom-formula system)
                                         negated-selfcons)]
     {:variant :tab-2-or-stronger
      :witness-stage :full-generated-selfcons-contradiction-target
      :system-code (:system-code system)
      :group-three-code (:code (:group-three system))
      :axiom-formula (:axiom-formula system)
      :selfcons-formula (:formula (:group-three system))
      :negated-selfcons-formula negated-selfcons
      :selfcons-refutation-target refutation-target
      :target-code (formula-code system refutation-target)
      :constructed-certificate-status :open
      :proof-search-synthesis-status :open
      :durable-probe-required? true
      :completion-claimed? false})))

(defn- proof-code-certificate-kind
  "Classify a public proof-code term for Workstream B evidence intake."
  [proof-code]
  (let [decoded-proof (try
                        (sjas-code/proof-formal-code-term->proof proof-code)
                        (catch Exception _
                          nil))]
    (cond
      (= 'sjas-axiom decoded-proof) :sjas-axiom
      decoded-proof :structural-tableau
      :else :unreadable-proof-code)))

(defn- proof-byte-sequence?
  "True when `value` is one complete sequence of base-64 proof bytes.

   Formula-bearing proof nodes use either a flat length-prefixed byte payload or
   a nested byte-list payload. Keeping this predicate exact prevents arbitrary
   proof lists from being mistaken for formula nodes during route inspection."
  [value]
  (and (sequential? value)
       (seq value)
       (every? #(and (integer? %) (<= 0 % (dec sjas-code/byte-base))) value)))

(defn- structural-proof-node-parts
  "Return `[formula-bytes children]` for one formula-bearing proof node.

   Narrow nodes begin with a positive byte count followed by that many formula
   bytes. Wide nodes store the formula bytes as the first list item. The result
   deliberately exposes only child proof terms; formula payload bytes are never
   recursively reinterpreted as nested proof nodes."
  [node]
  (when (and (sequential? node) (seq node))
    (let [head (first node)]
      (cond
        (proof-byte-sequence? head)
        [(vec head) (rest node)]

        (and (integer? head)
             (pos? head)
             (<= head (count (rest node))))
        (let [formula-bytes (take head (rest node))]
          (when (proof-byte-sequence? formula-bytes)
            [(vec formula-bytes) (drop (inc head) node)]))

        :else nil))))

(defn- structural-proof-formula-nodes
  "Collect exact formula-byte payloads selected by a structural proof tree."
  [proof]
  (loop [pending (list proof)
         nodes []]
    (if-let [node (first pending)]
      (cond
        (and (sequential? node)
             (= 'willard-sjas-boundary-refutation (first node))
             (= 3 (count node)))
        (recur (cons (nth node 2) (rest pending)) nodes)

        :else
        (if-let [[formula-bytes children] (structural-proof-node-parts node)]
          (recur (concat children (rest pending))
                 (conj nodes formula-bytes))
          (recur (rest pending) nodes)))
      nodes)))

(defn- decoded-measured-proof-object
  "Decode one measured proof object and its embedded ordinary proof tree.

   ADR-0109 objects carry exact system/theorem/proof byte strings. This helper
   accepts the established measured object tags and returns nil for invalid
   payloads rather than inferring route evidence from a partial decode."
  [proof-object-code]
  (let [expected-payload-counts {'dsjas-tableau-proof-object 3
                                 'dsjas-subst-prf-object 4
                                 'dsjas-tab1-proof-object 3
                                 'dsjas-tab2-proof-object 3}
        decoded (try
                  (sjas-code/proof-formal-code-term->proof proof-object-code)
                  (catch Exception _ nil))
        tag (first decoded)
        payloads (rest decoded)]
    (when (and (= (get expected-payload-counts tag)
                  (count payloads))
               (every? proof-byte-sequence? payloads))
      (let [proof-bytes (last payloads)
            proof (sjas-code/proof-bytes->term proof-bytes)
            proof-list-tag (case tag
                             dsjas-tab1-proof-object 'tab1-proof-list-object
                             dsjas-tab2-proof-object 'tab2-proof-list-object
                             nil)
            proof-list-nodes
            (when (and proof-list-tag
                       (= proof-list-tag (first proof)))
              (mapcat
                (fn [entry]
                  (when (and (sequential? entry)
                             (= 2 (count entry))
                             (proof-byte-sequence? (second entry)))
                    (when-let [entry-proof
                               (sjas-code/proof-bytes->term (second entry))]
                      (structural-proof-formula-nodes entry-proof))))
                (rest proof)))
            formula-nodes (if proof-list-tag
                            (vec proof-list-nodes)
                            (when proof
                              (structural-proof-formula-nodes proof)))]
        (when (and proof formula-nodes)
          {:tag tag
           :system-bytes (vec (first payloads))
           :payloads (mapv vec payloads)
           :proof-bytes (vec proof-bytes)
           :proof proof
           :formula-node-bytes formula-nodes})))))

(defn- formula-route-bytes
  "Return the structural-checker byte form for one selected antecedent formula."
  [system formula]
  (vec (formal-code-term-bytes
         (formula-code system (proof-side-antecedent-formula formula)))))

(defn boundary-proof-route-report
  "Derive reduced-witness use from measured, inspectable proof objects.

   `required-witness-formulas` names the exact variant formulas whose selection
   constitutes route evidence. The report also detects selection of the
   generated Group-3 formula, which is the ordinary SelfCons shortcut. This is
   a structural audit only: final evidence must additionally pass the kernel
   counterexample predicates returned by a target-specific validator."
  [system required-witness-formulas proof-object-codes]
  (let [decoded-objects (mapv decoded-measured-proof-object proof-object-codes)
        complete-objects (filterv some? decoded-objects)
        expected-system-bytes (vec (formal-code-term-bytes (:system-code system)))
        all-node-bytes (vec (mapcat :formula-node-bytes complete-objects))
        witness-node-bytes (set (map #(formula-route-bytes system %)
                                     required-witness-formulas))
        group-three-node-bytes (formula-route-bytes
                                 system
                                 (:formula (:group-three system)))
        witness-node-count (count (filter witness-node-bytes all-node-bytes))
        group-three-node-count (count (filter #(= group-three-node-bytes %)
                                              all-node-bytes))
        all-decode? (= (count proof-object-codes) (count complete-objects))
        all-match-system? (and all-decode?
                               (every? #(= expected-system-bytes
                                           (:system-bytes %))
                                       complete-objects))
        required-witness-node? (pos? witness-node-count)
        group-three-node? (pos? group-three-node-count)]
    {:system-code (:system-code system)
     :proof-object-count (count proof-object-codes)
     :decoded-proof-object-count (count complete-objects)
     :all-proof-objects-decode? all-decode?
     :all-proof-objects-match-system? all-match-system?
     :required-witness-node? required-witness-node?
     :required-witness-node-count witness-node-count
     :group-three-node? group-three-node?
     :group-three-node-count group-three-node-count
     :route-shape-valid? (and all-decode?
                              all-match-system?
                              required-witness-node?
                              (not group-three-node?))}))

(defn- boundary-query-valid?
  "Run one ground boundary-counterexample predicate through the SJAS kernel."
  [system formula proof-limit fuel]
  (boolean
    (seq (query/query-succeeds (:program system)
                               formula
                               proof-limit
                               fuel))))

(defn- missing-counterexample-fields
  "Return stable reason keywords for absent SelfCons counterexample components."
  [candidate]
  (cond-> #{}
    (nil? (:theorem-code candidate)) (conj :missing-theorem-code)
    (nil? (:complement-code candidate)) (conj :missing-complement-code)
    (nil? (:theorem-proof-object candidate))
    (conj :missing-theorem-proof-object)
    (nil? (:complement-proof-object candidate))
    (conj :missing-complement-proof-object)))

(defn- level1-selfcons-counterexample-validation
  "Validate the positive body of a generated Level-1 `not(SelfCons)` tuple."
  [variant system report required-witness-formulas candidate proof-limit fuel]
  (let [missing (missing-counterexample-fields candidate)
        theorem-code (:theorem-code candidate)
        complement-code (:complement-code candidate)
        theorem-proof-object (:theorem-proof-object candidate)
        complement-proof-object (:complement-proof-object candidate)
        substitution-code (get-in system [:group-three
                                          :selfcons-skeleton-code])
        ready? (empty? missing)
        class-valid? (and ready?
                          (willard-sjas-profile/pi-star-1-code-valid?
                            (:program system)
                            theorem-code))
        complement-valid? (and ready?
                               (willard-sjas-profile/neg-pair-valid?
                                 (:program system)
                                 theorem-code
                                 complement-code))
        theorem-proof-valid? (and ready?
                                  (willard-sjas-profile/dsjas-subst-prf-valid?
                                    (:program system)
                                    (:system-code system)
                                    substitution-code
                                    theorem-code
                                    theorem-proof-object
                                    fuel))
        complement-proof-valid? (and ready?
                                     (willard-sjas-profile/dsjas-subst-prf-valid?
                                       (:program system)
                                       (:system-code system)
                                       substitution-code
                                       complement-code
                                       complement-proof-object
                                       fuel))
        route (boundary-proof-route-report
                system
                required-witness-formulas
                (remove nil? [theorem-proof-object complement-proof-object]))
        counterexample-valid? (and class-valid?
                                   complement-valid?
                                   theorem-proof-valid?
                                   complement-proof-valid?)
        proof-route-valid? (and counterexample-valid?
                                (:route-shape-valid? route))
        reasons (cond-> missing
                  (and ready? (not class-valid?))
                  (conj :theorem-class-validation-failed)
                  (and ready? (not complement-valid?))
                  (conj :complement-validation-failed)
                  (and ready? (not theorem-proof-valid?))
                  (conj :theorem-proof-validation-failed)
                  (and ready? (not complement-proof-valid?))
                  (conj :complement-proof-validation-failed)
                  (and counterexample-valid? (not proof-route-valid?))
                  (conj :boundary-proof-route-unverified))]
    {:variant variant
     :validation-kind :selfcons-counterexample
     :status (if (and counterexample-valid? proof-route-valid?)
               :validated
               :rejected)
     :system-code (:system-code report)
     :selfcons-code (:group-three-code report)
     :target-formula (:selfcons-refutation-target report)
     :target-code (:target-code report)
     :proof-code (:certificate-code candidate)
     :certificate-kind :selfcons-counterexample
     :theorem-code theorem-code
     :complement-code complement-code
     :theorem-proof-object theorem-proof-object
     :complement-proof-object complement-proof-object
     :validator :generated-selfcons-counterexample
     :proof-valid? counterexample-valid?
     :counterexample-valid? counterexample-valid?
     :proof-route-valid? proof-route-valid?
     :checks {:pi-star-1-code class-valid?
              :neg-pair complement-valid?
              :theorem-dsjas-subst-prf theorem-proof-valid?
              :complement-dsjas-subst-prf complement-proof-valid?}
     :route route
     :reasons reasons}))

(defn total-multiplication-selfcons-counterexample-validation
  "Validate an ADR-0119 total-multiplication SelfCons counterexample tuple."
  ([candidate]
   (total-multiplication-selfcons-counterexample-validation candidate {}))
  ([candidate opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 320)
         depth (:depth opts 3)
         system-opts (dissoc opts :proof-limit :fuel)
         system (total-multiplication-complete-system system-opts)
         report (total-multiplication-full-target-report system-opts)
         required-witness (last (total-multiplication-squaring-chain-axioms
                                  depth))]
     (level1-selfcons-counterexample-validation
       :total-multiplication
       system
       report
       [required-witness]
       candidate
       proof-limit
       fuel))))

(defn xtab-lem-selfcons-counterexample-validation
  "Validate an ADR-0119 Xtab/LEM SelfCons counterexample tuple."
  ([candidate]
   (xtab-lem-selfcons-counterexample-validation candidate {}))
  ([candidate opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 320)
         system-opts (dissoc opts :proof-limit :fuel)
         system (xtab-complete-system system-opts)
         report (xtab-lem-full-target-report system-opts)]
     (level1-selfcons-counterexample-validation
       :xtab-or-lem-axiom
       system
       report
       (xtab-lem-witness-axioms)
       candidate
       proof-limit
       fuel))))

(defn tab2-or-stronger-selfcons-counterexample-validation
  "Validate a concrete counterexample to generated Tab-2 SelfCons."
  ([candidate]
   (tab2-or-stronger-selfcons-counterexample-validation candidate {}))
  ([candidate opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 520)
         system-opts (dissoc opts :proof-limit :fuel)
         system (tab2-complete-system system-opts)
         negated-selfcons (selfcons-negation-target system)
         target (ast/and-form (:axiom-formula system) negated-selfcons)
         theorem-code (:theorem-code candidate)
         complement-code (:complement-code candidate)
         theorem-proof-object (:theorem-proof-object candidate)
         complement-proof-object (:complement-proof-object candidate)
         missing (missing-counterexample-fields candidate)
         ready? (empty? missing)
         complement-valid?
         (and ready?
              (willard-sjas-profile/neg-pair-valid?
                (:program system)
                theorem-code
                complement-code))
         theorem-proof-valid?
         (and ready?
              (boundary-query-valid? system
                                     (dsjas-tab2-proof
                                       (:system-code system)
                                       theorem-code
                                       theorem-proof-object)
                                     proof-limit fuel))
         complement-proof-valid?
         (and ready?
              (boundary-query-valid? system
                                     (dsjas-tab2-proof
                                       (:system-code system)
                                       complement-code
                                       complement-proof-object)
                                     proof-limit fuel))
         counterexample-valid? (and complement-valid?
                                    theorem-proof-valid?
                                    complement-proof-valid?)
         route (boundary-proof-route-report
                 system
                 [(tab2-rank2-witness-formula)]
                 (remove nil? [theorem-proof-object
                               complement-proof-object]))
         proof-route-valid? (and counterexample-valid?
                                 (:route-shape-valid? route))
         reasons (cond-> missing
                   (and ready? (not complement-valid?))
                   (conj :complement-validation-failed)
                   (and ready? (not theorem-proof-valid?))
                   (conj :theorem-proof-validation-failed)
                   (and ready? (not complement-proof-valid?))
                   (conj :complement-proof-validation-failed)
                   (and counterexample-valid? (not proof-route-valid?))
                   (conj :boundary-proof-route-unverified))]
     {:variant :tab-2-or-stronger
      :validation-kind :selfcons-counterexample
      :status (if proof-route-valid? :validated :rejected)
      :system-code (:system-code system)
      :selfcons-code (:code (:group-three system))
      :target-formula target
      :target-code (formula-code system target)
      :proof-code (:certificate-code candidate)
      :certificate-kind :selfcons-counterexample
      :theorem-code theorem-code
      :complement-code complement-code
      :theorem-proof-object theorem-proof-object
      :complement-proof-object complement-proof-object
      :validator :dsjas-tab2-proof
      :proof-valid? counterexample-valid?
      :counterexample-valid? counterexample-valid?
      :proof-route-valid? proof-route-valid?
      :checks {:neg-pair complement-valid?
               :theorem-dsjas-tab2-proof theorem-proof-valid?
               :complement-dsjas-tab2-proof complement-proof-valid?}
      :route route
      :reasons reasons})))

(defn- boundary-refutation-proof
  "Encode one checked Workstream B metatheorem constructor.

   The child is a normal formula-bearing node for the reduced witness. The
   kernel verifies that node against the exact reconstructed axiom/prior-theorem
   conjunction and separately checks the unsafe profile's complete arithmetic
   hypotheses before accepting the contradiction theorem."
  [system variant-symbol witness-formula]
  (list 'willard-sjas-boundary-refutation
        variant-symbol
        (structural-byte-list-tableau-node
          system
          (proof-side-antecedent-formula witness-formula))))

(defn- level1-boundary-counterexample-artifact
  "Build the exact Level-1 `(x,y,p,q)` tuple for one unsafe profile."
  [variant system target witness-formula variant-symbol opts validator]
  (let [theorem-formula (ast/eq-lit one zero)
        complement-formula (ast/neq-lit one zero)
        theorem-code (formula-code system theorem-formula)
        complement-code (formula-code system complement-formula)
        substitution-code (get-in system
                                  [:group-three :selfcons-skeleton-code])
        theorem-certificate
        (proof-certificate
          (boundary-refutation-proof
            system
            variant-symbol
            witness-formula))
        complement-certificate (proof-certificate 'sjas-axiom)
        theorem-proof-object
        (dsjas-subst-prf-object (:system-code system)
                                substitution-code
                                theorem-code
                                theorem-certificate)
        complement-proof-object
        (dsjas-subst-prf-object (:system-code system)
                                substitution-code
                                complement-code
                                complement-certificate)
        candidate
        {:variant variant
         :evidence-kind :constructed-certificate
         :system-code (:system-code target)
         :selfcons-code (:group-three-code target)
         :target-formula (:selfcons-refutation-target target)
         :target-code (:target-code target)
         :proof-code theorem-certificate
         :certificate-code theorem-certificate
         :certificate-kind :selfcons-counterexample
         :theorem-code theorem-code
         :complement-code complement-code
         :theorem-proof-object theorem-proof-object
         :complement-proof-object complement-proof-object}
        validation (validator candidate opts)]
    {:system system
     :target target
     :candidate candidate
     :validation validation}))

(defn- tab2-boundary-counterexample-artifact
  "Build the exact Tab-2 proof-list counterexample tuple."
  [opts]
  (let [system-opts (dissoc opts :proof-limit :fuel)
        system (tab2-complete-system system-opts)
        target (tab2-or-stronger-full-target-report system-opts)
        theorem-formula (ast/eq-lit one zero)
        complement-formula (ast/neq-lit one zero)
        theorem-code (formula-code system theorem-formula)
        complement-code (formula-code system complement-formula)
        witness-formula (tab2-rank2-witness-formula)
        witness-code (formula-code system witness-formula)
        witness-certificate
        (proof-certificate
          (tab2-rank2-witness-proof system witness-formula))
        theorem-certificate
        (proof-certificate
          (boundary-refutation-proof
            system
            'tab2-boundary
            witness-formula))
        complement-certificate (proof-certificate 'sjas-axiom)
        theorem-proof-list
        (tab2-proof-list-object
          [{:theorem-code witness-code
            :proof-code witness-certificate}
           {:theorem-code theorem-code
            :proof-code theorem-certificate}])
        complement-proof-list
        (tab2-proof-list-object
          [{:theorem-code complement-code
            :proof-code complement-certificate}])
        theorem-proof-object
        (dsjas-tab2-proof-object (:system-code system)
                                 theorem-code
                                 theorem-proof-list)
        complement-proof-object
        (dsjas-tab2-proof-object (:system-code system)
                                 complement-code
                                 complement-proof-list)
        candidate
        {:variant :tab-2-or-stronger
         :evidence-kind :constructed-certificate
         :system-code (:system-code target)
         :selfcons-code (:group-three-code target)
         :target-formula (:selfcons-refutation-target target)
         :target-code (:target-code target)
         :proof-code theorem-certificate
         :certificate-code theorem-certificate
         :certificate-kind :selfcons-counterexample
         :theorem-code theorem-code
         :complement-code complement-code
         :theorem-proof-object theorem-proof-object
         :complement-proof-object complement-proof-object}
        validation
        (tab2-or-stronger-selfcons-counterexample-validation candidate opts)]
    {:system system
     :target target
     :candidate candidate
     :validation validation}))

(defn constructed-boundary-selfcons-counterexample
  "Construct and kernel-check one exact Workstream B counterexample tuple.

   The returned artifact is not accepted from metadata. Its validation invokes
   the same generated `pi-star-1-code`, `neg-pair`, and measured proof
   predicates that occur in the positive body of the selected `not(SelfCons)`
   sentence."
  ([variant]
   (constructed-boundary-selfcons-counterexample variant {}))
  ([variant opts]
   (case variant
     :total-multiplication
     (let [depth (:depth opts 3)
           system-opts (dissoc opts :proof-limit :fuel)
           system (total-multiplication-complete-system system-opts)
           target (total-multiplication-full-target-report system-opts)
           witness (last (total-multiplication-squaring-chain-axioms depth))]
       (level1-boundary-counterexample-artifact
         variant
         system
         target
         witness
         'total-multiplication-boundary
         opts
         total-multiplication-selfcons-counterexample-validation))

     :xtab-or-lem-axiom
     (let [system-opts (dissoc opts :proof-limit :fuel)
           system (xtab-complete-system system-opts)
           target (xtab-lem-full-target-report system-opts)
           witness (first (xtab-lem-witness-axioms))]
       (level1-boundary-counterexample-artifact
         variant
         system
         target
         witness
         'xtab-lem-boundary
         opts
         xtab-lem-selfcons-counterexample-validation))

     :tab-2-or-stronger
     (tab2-boundary-counterexample-artifact opts)

     (throw (ex-info "Unknown Workstream B boundary variant"
                     {:variant variant
                      :supported #{:total-multiplication
                                   :xtab-or-lem-axiom
                                   :tab-2-or-stronger}})))))

(defn- synthesized-boundary-report
  "Package one relationally generated tuple and run its public validator."
  [variant system target tuple validator opts]
  (if-not tuple
    {:variant variant
     :evidence-kind :proof-search-synthesis
     :synthesis-status :not-found
     :fresh-tuple-variables? true
     :tuple-origin :fresh-core-logic-variables
     :candidate nil
     :validation nil
     :completed-obligations #{}
     :remaining-obligations #{:proof-search-synthesis}
     :durable-log-path (:durable-log-path opts)}
    (let [[theorem-code
           complement-code
           theorem-proof-object
           complement-proof-object
           theorem-proof-code] tuple
          candidate
          {:variant variant
           :evidence-kind :proof-search-synthesis
           :system-code (:system-code target)
           :selfcons-code (:group-three-code target)
           :target-formula (:selfcons-refutation-target target)
           :target-code (:target-code target)
           :proof-code theorem-proof-code
           :certificate-code theorem-proof-code
           :certificate-kind :selfcons-counterexample
           :theorem-code theorem-code
           :complement-code complement-code
           :theorem-proof-object theorem-proof-object
           :complement-proof-object complement-proof-object
           :durable-log-path (:durable-log-path opts)}
          validation (validator candidate opts)
          accepted? (and (= :validated (:status validation))
                         (true? (:counterexample-valid? validation))
                         (true? (:proof-route-valid? validation)))]
      {:variant variant
       :evidence-kind :proof-search-synthesis
       :synthesis-status (if accepted? :found :rejected)
       :fresh-tuple-variables? true
       :tuple-origin :fresh-core-logic-variables
       :synthesis-engine :core-logic-proof-object-generation
       :system system
       :target target
       :candidate candidate
       :validation validation
       :completed-obligations (if accepted?
                                #{:proof-search-synthesis}
                                #{})
       :remaining-obligations (if accepted?
                                #{}
                                #{:proof-search-synthesis})
       :durable-log-path (:durable-log-path opts)})))

(defn synthesize-boundary-selfcons-counterexample
  "Search for a generated-SelfCons counterexample with fresh tuple variables.

   Unlike the legacy positive-SelfCons probe, this search does not ask for an
   ordinary proof of Group 3. The kernel creates fresh `x`, `y`, `p`, and `q`
   variables, generates proof and measured-object codes through the relational
   proof grammar, and returns a tuple only after the exact positive body of
   `not(SelfCons)` succeeds."
  ([variant]
   (synthesize-boundary-selfcons-counterexample variant {}))
  ([variant opts]
   (case variant
     :total-multiplication
     (let [fuel (:fuel opts 320)
           depth (:depth opts 3)
           system-opts (dissoc opts
                               :proof-limit
                               :fuel
                               :durable-log-path)
           system (total-multiplication-complete-system system-opts)
           target (total-multiplication-full-target-report system-opts)
           witness (last (total-multiplication-squaring-chain-axioms depth))
           witness-bytes
           (apply list
                  (formal-code-term-bytes
                    (formula-code system
                                  (proof-side-antecedent-formula witness))))
           tuple
           (willard-sjas-profile/synthesize-level1-boundary-counterexample
             (:program system)
             (:system-code system)
             (get-in system [:group-three :selfcons-skeleton-code])
             'total-multiplication-boundary
             witness-bytes
             fuel)]
       (synthesized-boundary-report
         variant
         system
         target
         tuple
         total-multiplication-selfcons-counterexample-validation
         opts))

     :xtab-or-lem-axiom
     (let [fuel (:fuel opts 320)
           system-opts (dissoc opts
                               :proof-limit
                               :fuel
                               :durable-log-path)
           system (xtab-complete-system system-opts)
           target (xtab-lem-full-target-report system-opts)
           witness (first (xtab-lem-witness-axioms))
           witness-bytes
           (apply list
                  (formal-code-term-bytes
                    (formula-code system
                                  (proof-side-antecedent-formula witness))))
           tuple
           (willard-sjas-profile/synthesize-level1-boundary-counterexample
             (:program system)
             (:system-code system)
             (get-in system [:group-three :selfcons-skeleton-code])
             'xtab-lem-boundary
             witness-bytes
             fuel)]
       (synthesized-boundary-report
         variant
         system
         target
         tuple
         xtab-lem-selfcons-counterexample-validation
         opts))

     :tab-2-or-stronger
     (let [fuel (:fuel opts 520)
           system-opts (dissoc opts
                               :proof-limit
                               :fuel
                               :durable-log-path)
           system (tab2-complete-system system-opts)
           target (tab2-or-stronger-full-target-report system-opts)
           witness (tab2-rank2-witness-formula)
           witness-code (formula-code system witness)
           witness-proof (tab2-rank2-witness-proof system witness)
           witness-bytes
           (apply list
                  (formal-code-term-bytes
                    (formula-code system
                                  (proof-side-antecedent-formula witness))))
           tuple
           (willard-sjas-profile/synthesize-tab2-boundary-counterexample
             (:program system)
             (:system-code system)
             'tab2-boundary
             witness-code
             witness-proof
             witness-bytes
             fuel)]
       (synthesized-boundary-report
         variant
         system
         target
         tuple
         tab2-or-stronger-selfcons-counterexample-validation
         opts))

     (throw (ex-info "Unknown Workstream B boundary variant"
                     {:variant variant
                      :supported #{:total-multiplication
                                   :xtab-or-lem-axiom
                                   :tab-2-or-stronger}})))))

(defn boundary-evidence-ledger
  "Run and tally all six ADR-0119 Workstream B final-evidence obligations.

   For each of the three negative variants this constructs the exact
   `not(SelfCons)` counterexample tuple, verifies it with the ADR-0140
   `verify-boundary-constructed-certificate` contract, and then independently
   synthesizes a fresh-variable tuple through the measured proof predicates.
   Both the constructed verification and the synthesis report are produced by
   SJAS-kernel proof checking, never from candidate metadata.

   The returned map is the aggregated
   `correspondence/summarize-boundary-evidence-ledger` report (with `:complete?`
   true and six of six obligations only when every kernel verification and
   synthesis succeeds), plus the raw per-variant `:entries` used to compute it.

   This performs the full boundary proof search for all three variants and is
   therefore expensive. Callers that only need the aggregation logic should test
   `correspondence/summarize-boundary-evidence-ledger` with stub entries."
  ([] (boundary-evidence-ledger {}))
  ([opts]
   (let [entries
         (mapv
           (fn [variant]
             (let [{:keys [target candidate validation]}
                   (constructed-boundary-selfcons-counterexample variant opts)
                   constructed
                   (correspondence/verify-boundary-constructed-certificate
                     target candidate validation)
                   synthesis
                   (synthesize-boundary-selfcons-counterexample variant opts)]
               {:variant variant
                :constructed constructed
                :synthesis synthesis}))
           [:total-multiplication
            :xtab-or-lem-axiom
            :tab-2-or-stronger])]
     (assoc (correspondence/summarize-boundary-evidence-ledger entries)
            :entries entries))))

(defn total-multiplication-constructed-certificate-validation
  "Diagnose a positive Group-3 proof against the ADR-0126 system.

   The public `tableau-proof/3` predicate reconstructs
   `AxiomConj(S_total-mul) /\\ not(SelfCons(S_total-mul))` from the supplied
   system and generated Group-3 theorem code. ADR-0140 classifies the returned
   map as legacy positive-SelfCons diagnostics; it is not eligible for
   Workstream B verification."
  ([proof-code]
   (total-multiplication-constructed-certificate-validation proof-code {}))
  ([proof-code opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 320)
         system-opts (dissoc opts :proof-limit :fuel)
         system (total-multiplication-complete-system system-opts)
         report (total-multiplication-full-target-report system-opts)
         certificate-kind (proof-code-certificate-kind proof-code)
         proof-count (if (= :sjas-axiom certificate-kind) 1 0)]
     {:variant :total-multiplication
      :validation-kind :legacy-positive-selfcons-proof
      :boundary-evidence-eligible? false
      :system-code (:system-code report)
      :selfcons-code (:group-three-code report)
      :target-formula (:selfcons-refutation-target report)
      :target-code (:target-code report)
      :proof-code proof-code
      :certificate-kind certificate-kind
      :validator :tableau-proof
      :proof-limit proof-limit
      :fuel fuel
      :proof-count proof-count
      :proof-valid? (pos? proof-count)})))

(defn xtab-lem-constructed-certificate-validation
  "Diagnose a positive Group-3 proof against the ADR-0134 Xtab/LEM system.

   The returned proof-validation record has the same shape as
   `total-multiplication-constructed-certificate-validation`, but derives its
   system and target from the ADR-0141 executable Xtab profile."
  ([proof-code]
   (xtab-lem-constructed-certificate-validation proof-code {}))
  ([proof-code opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 320)
         system-opts (dissoc opts :proof-limit :fuel)
         system (xtab-complete-system system-opts)
         report (xtab-lem-full-target-report system-opts)
         certificate-kind (proof-code-certificate-kind proof-code)
         proof-count (if (= :sjas-axiom certificate-kind) 1 0)]
     {:variant :xtab-or-lem-axiom
      :validation-kind :legacy-positive-selfcons-proof
      :boundary-evidence-eligible? false
      :system-code (:system-code report)
      :selfcons-code (:group-three-code report)
      :target-formula (:selfcons-refutation-target report)
      :target-code (:target-code report)
      :proof-code proof-code
      :certificate-kind certificate-kind
      :validator :tableau-proof
      :proof-limit proof-limit
      :fuel fuel
      :proof-count proof-count
      :proof-valid? (pos? proof-count)})))

(defn tab2-or-stronger-constructed-certificate-validation
  "Diagnose a positive Group-3 proof against the executable Tab-2 system.

   This remains legacy positive-SelfCons diagnostics only. It validates a
   proof-code citation of the generated Group-3 axiom, not a counterexample to
   the generated SelfCons body."
  ([proof-code]
   (tab2-or-stronger-constructed-certificate-validation proof-code {}))
  ([proof-code opts]
   (let [proof-limit (:proof-limit opts 1)
         fuel (:fuel opts 320)
         system-opts (dissoc opts :proof-limit :fuel)
         target-system (tab2-complete-system system-opts)
         report (tab2-or-stronger-full-target-report system-opts)
         certificate-kind (proof-code-certificate-kind proof-code)
         proof-count (if (= :sjas-axiom certificate-kind) 1 0)]
     {:variant :tab-2-or-stronger
      :validation-kind :legacy-positive-selfcons-proof
      :boundary-evidence-eligible? false
      :system-code (:system-code report)
      :selfcons-code (:group-three-code report)
      :target-formula (:selfcons-refutation-target report)
      :target-code (:target-code report)
      :proof-code proof-code
      :certificate-kind certificate-kind
      :validator :tableau-proof
      :proof-limit proof-limit
      :fuel fuel
      :proof-count proof-count
      :proof-valid? (pos? proof-count)})))

;; -----------------------------------------------------------------------------
;; Source-facing SJAS builder
;; -----------------------------------------------------------------------------

(defn- arity-entry
  "Parse one source declaration entry like `(symbol arity)`."
  [form]
  (when-not (and (seq? form)
                 (symbol? (first form))
                 (= 2 (count form))
                 (integer? (second form))
                 (<= 0 (second form)))
    (throw (ex-info "Expected a declaration entry like (symbol arity)"
                    {:form form})))
  [(first form) (second form)])

(defn- parse-source-language
  "Parse the SJAS source-builder language extension section."
  [sections]
  (reduce (fn [acc section]
            (when-not (seq? section)
              (throw (ex-info "Malformed SJAS language section"
                              {:section section})))
            (let [head (first section)
                  entries (rest section)]
              (case head
                constants (update acc :constants into entries)
                functions (update acc :functions merge (into {} (map arity-entry entries)))
                relations (update acc :relations merge (into {} (map arity-entry entries)))
                (throw (ex-info "Unknown SJAS language section"
                                {:section section
                                 :known-sections '(constants functions relations)})))))
          {:constants []
           :functions {}
           :relations {}}
          sections))

(defn- parse-system-source-section
  "Classify one top-level `system-source` section."
  [section]
  (when-not (seq? section)
    (throw (ex-info "Malformed SJAS source section"
                    {:section section})))
  (let [head (first section)
        entries (rest section)]
    (case head
      language (assoc (parse-source-language entries) :kind :language)
      beta {:kind :beta :forms (vec entries)}
      reflected {:kind :reflected :forms (vec entries)}
      external {:kind :external :forms (vec entries)}
      (throw (ex-info "Unknown SJAS source section"
                      {:section section
                       :known-sections '(language beta reflected external)})))))

(defn- collect-system-source
  [sections]
  (reduce (fn [acc section]
            (let [{:keys [kind] :as parsed} (parse-system-source-section section)]
              (case kind
                :language (-> acc
                              (update :constants into (:constants parsed))
                              (update :functions merge (:functions parsed))
                              (update :relations merge (:relations parsed)))
                :beta (update acc :beta into (:forms parsed))
                :reflected (update acc :reflected into (:forms parsed))
                :external (update acc :external into (:forms parsed)))))
          {:constants []
           :functions {}
           :relations {}
           :beta []
           :reflected []
           :external []}
          sections))

(defmacro system-source
  "Build an SJAS system from Clojure-readable prefix source.

   Example:
   (system-source
     {:profile :willard-sjas-tableau0}
     (language
       (relations (demo 1)))
     (beta
       (= one one))
     (reflected
       (|- (demo x) (= x one))))

   The macro lowers beta formulas through `proflog.frontend/q` and user clauses
   through `proflog.frontend/clauses`, then delegates to `system` so Group-3 and
   object-language axiom membership are still generated in one place."
  [opts & sections]
  (let [{:keys [constants functions relations beta reflected external]}
        (collect-system-source sections)
        reflected-code (if (seq reflected)
                         `(frontend/clauses ~@reflected)
                         [])
        external-code (if (seq external)
                        `(frontend/clauses ~@external)
                        [])]
    `(system
       (merge ~opts
              {:constants '~(vec constants)
               :functions '~functions
               :relations '~relations
               :beta [~@(map (fn [formula]
                                `(frontend/q ~formula))
                              beta)]
               :reflected-clauses ~reflected-code
               :external-clauses ~external-code}))))

(defn theorem-query
  "Wrap `formula` so it is proved from the generated finite SJAS axiom basis."
  [system formula]
  (ast/implies-form (:axiom-formula system) (code-canonical-formula formula)))

(defn query-succeeds
  "Prove `formula` from an SJAS system's generated axiom basis."
  ([system formula]
   (query-succeeds system formula {}))
  ([system formula {:keys [proof-limit fuel]
                   :or {proof-limit 1}}]
   (query/query-succeeds
     (:program system)
     (theorem-query system formula)
     proof-limit
     fuel)))

(defn query-answers
  "Export answer bindings for an SJAS query under the SJAS theory profile.

   `:defer-calls? false` makes residual deferral unavailable, so theory atoms
   must close through the profile hook (binding any synthesized values) or
   the answer fails — the ADR-0095 synthesis mode."
  ([system formula answer-vars]
   (query-answers system formula answer-vars {}))
  ([system formula answer-vars opts]
   (binding [answer-overlay/*theory-profile-closeo*
             willard-sjas-profile/willard-sjas-answer-theory-closeo
             answer-overlay/*defer-residual-calls*
             (get opts :defer-calls? true)
             gamma/*closed-term-depth-cap* 0
             gamma/*closed-term-count-cap* 0]
     (answers/query-answers (:program system)
                            formula
                            answer-vars
                            (dissoc opts :defer-calls?)))))

(defn bounded-contradiction-probe
  "Run a bounded Level-1 complement-proof probe and record wall-clock duration."
  [system {:keys [fuel proof-limit]
           :or {fuel 32
                proof-limit 1}}]
  (let [started (System/nanoTime)
        contradiction (tableau-proof (:system-code system)
                                     (:contradiction-code system)
                                     (proof-certificate
                                       'sjas-axiom
                                       {:code-format (:code-format system
                                                                   :compact)}))
        ;; This diagnostic probe supplies the axiom-citation certificate
        ;; `sjas-axiom`. Such a proof can close only when the finite system
        ;; actually lists the contradiction formula as an axiom; otherwise the
        ;; expensive proof search is known to be fruitless.
        contradiction-is-axiom? (some #(= (:contradiction-code system) (:code %))
                                      (:axioms system))
        proofs (if contradiction-is-axiom?
                 (query/query-succeeds (:program system)
                                       contradiction
                                       proof-limit
                                       fuel)
                 '())
        duration-ms (long (/ (- (System/nanoTime) started) 1000000))]
    {:result (if (seq proofs) :found :not-found)
     :proof-count (count proofs)
     :fuel fuel
     :proof-limit proof-limit
     :duration-ms duration-ms}))

(defn -main
  "Print the default ordinary-tableau SJAS self-consistency Godel code."
  [& _args]
  (print-selfcons-godel-code))
