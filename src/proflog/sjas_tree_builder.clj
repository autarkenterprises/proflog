(ns proflog.sjas-tree-builder
  "ADR-0142 Phase 3: construct-and-check cut-free tableau trees that the ordinary
   SJAS structural checker validates.

   Phase 0 established that
   `proflog.kernel.willard-sjas-profile/structural-proof-valid?` is a *validator
   of constructed cut-free tableau trees*, not a search-complete prover: each node
   is `(byte-count formula-bytes... child...)` (narrow, < 64 bytes) or
   `((formula-bytes...) child...)` (wide), and the checker INFERS the tableau rule
   from the decoded node formula and the branch state -- there is no rule tag and
   no cut tag. This namespace promotes the formula-bearing node builders (until
   now private test helpers in `proflog.willard-sjas-test`) into reusable
   construction primitives, so the Theorem 2.3 closure steps can be built
   test-first against the real generated system.

   Construction discipline (Phase 3 spec): hand-built trees are fiddly. A small
   difference in a binder's canonical index or in the generating system's coding
   context silently breaks acceptance. Child nodes that mention a parent binder's
   variable MUST use the `canonical-*` builders -- runtime AST noms are not stable
   proof-code bytes; canonical `v0`, `v1`, ... payloads are."
  (:require [proflog.kernel.willard-sjas-profile :as sjas-profile]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn ast->canonical-child
  "Lower an AST formula to canonical child syntax under a binder-name map.

   ADR-0147: gamma/delta child nodes present a parent binder's variable as the
   canonical `v0`, `v1`, ... payload. This walks an AST formula replacing
   `(var nom)` occurrences by `(var vK)` per `nom->name`, and lowering each
   remaining quantifier tie `(forall #Tie{nom body})` to the canonical
   index-form `(forall vK body)`, assigning further binder names in encounter
   order continuing from the map (the checker introduces fresh branch noms
   keyed to branch-environment depth, which matches encounter order). The
   result feeds `canonical-flex-tableau-node`."
  [formula nom->name]
  (letfn [(next-name [m] (symbol (str "v" (count m))))
          (walk-f [x m]
            (cond
              (and (seq? x)
                   (contains? '#{forall once-forall exists} (first x))
                   (instance? clojure.core.logic.nominal.Tie (second x)))
              (let [tie (second x)
                    nom (.-binding_nom ^clojure.core.logic.nominal.Tie tie)
                    body (.-body ^clojure.core.logic.nominal.Tie tie)
                    nm (next-name m)
                    m' (assoc m nom nm)]
                (list (first x) nm (walk-f body m')))
              (and (seq? x) (= 'var (first x)) (contains? m (second x)))
              (list 'var (m (second x)))
              (seq? x) (apply list (map #(walk-f % m) x))
              :else x))]
    (walk-f formula nom->name)))

(defn formula-code-bytes
  "Formula-code byte payload for `formula` under `system`'s coding context.

   Prefers the compact public code term; falls back to the U-Grounding payload
   when the system codes in U-Grounding format. Mirrors the encoding the checker
   decodes, so the bytes a node carries round-trip to the formula the checker
   reconstructs."
  [system formula]
  (let [code (sjas/formula-code system formula)]
    (or (sjas-code/code-term-bytes code)
        (sjas-code/u-grounding-code-term-bytes code))))

(defn canonical-formula-code-bytes
  "Formula-code bytes from canonical formula-code syntax (e.g. `(pos (app R (var v0)))`).

   Needed for child nodes that mention variables or parameters introduced by a
   parent quantifier: runtime AST noms are not stable proof-code bytes, but
   canonical `v0`, `v1`, ... payloads are."
  [system canonical-formula]
  (sjas-code/code-term-bytes
    (sjas-code/canonical-formula-code-term (:coding-context system)
                                           canonical-formula)))

(def ^:private narrow-byte-limit
  "The flat structural-node slice uses one proof byte for the formula length, so a
   formula whose payload is < 64 bytes is encoded narrow; wider formulas use the
   byte-list shape."
  64)

(defn- node-from-bytes
  [bytes children]
  (assert (pos? (count bytes))
          "a tableau node must carry a non-empty formula-code payload")
  (if (< (count bytes) narrow-byte-limit)
    (apply list (concat [(count bytes)] bytes children))
    (apply list (cons (apply list bytes) children))))

(defn flex-tableau-node
  "Build one formula-bearing tableau node for `formula` with `children`.

   Auto-selects the narrow `(count bytes... child...)` shape for formulas under
   the one-byte length limit and the wide `((bytes...) child...)` shape
   otherwise. The checker infers the tableau rule from the node formula and the
   branch state; the node carries no rule tag."
  [system formula & children]
  (node-from-bytes (formula-code-bytes system formula) children))

(defn canonical-flex-tableau-node
  "Like `flex-tableau-node` but from canonical formula-code syntax.

   Use for child nodes that mention a parent binder's variable/parameter."
  [system canonical-formula & children]
  (node-from-bytes (canonical-formula-code-bytes system canonical-formula) children))

(defn decode-proof-facing
  "Decode `formula` the way the proof checker sees an interior node.

   Encodes `formula` to this system's public code, then reads it back through the
   proof-facing decoder (no source codebook). Reserved/named SJAS symbols recover
   their names; every other symbol becomes a structural `(sym n)`. Use it to
   characterize which symbols a constructed node will actually present to the
   checker's `sjas-proof-node-formula-matcho` -- the match needs the decoded form
   to equal the literal branch formula, so a node mentioning a relation that
   decodes to `(sym n)` cannot match a literal branch atom that names it."
  [system formula]
  (sjas-profile/decoded-proof-formula (:program system)
                                      (sjas/formula-code system formula)))

(defn valid-tree?
  "True iff the ordinary SJAS checker accepts `proof` as a closed tableau for
   `target` over `system`, within `fuel` (default 50).

   Thin wrapper over `structural-proof-valid?`: no host inference establishes a
   proof step; this only reports whether the constructed finite proof tree was
   accepted. Construction is the obligation; this is the check."
  ([system target proof]
   (valid-tree? system target proof 50))
  ([system target proof fuel]
   (boolean
     (sjas-profile/structural-proof-valid? (:program system)
                                           (:system-code system)
                                           target
                                           proof
                                           fuel))))
