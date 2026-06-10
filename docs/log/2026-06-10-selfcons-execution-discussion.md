# SelfCons Execution Discussion (Verbatim)

Date: 2026-06-10

Verbatim transcript of the user/agent exchange on whether the SJAS
implementation has executed self-verification, the concrete shape of the
fixed-point SelfCons sentences, and the proof-synthesis question, logged at
the user's request. Code line references are to the post-ADR-0094 tree.
The synthesis discussion led to
[ADR-0095](../adr/ADR-0095-sjas-proof-synthesis.md).

---

**User:** Has the SJAS implementation successfully executed a program
equivalent to the proving of its own consistency?

**Agent:** Yes — in the precise sense the literature licenses, and with two
qualifications that are themselves part of the answer. Let me state exactly
what has executed, because the claim is only as good as its precision.

*What has executed.* The structural form: the public proof predicate
`tableau-proof(s, t, p)` — running as a Proflog query against the compiled
SJAS program — has accepted a formula-bearing closed-tableau proof code of
the system's own consistency sentence: `s` decodes to the system's own
Godel-coded descriptor, `t` to the Group-3 SelfCons sentence ("for all p,
p is not a tableau proof of `0 = 1` from this very system"), and `p` to a
genuine structural proof tree (471 proof-code bytes against a 77-byte
target, per the ADR-0085 record). Validation proceeds entirely over
object-level relations: the public byte readers decode `s`, `AxiomConj(s)`
is reconstructed from the descriptor — including the Group-3 sentence
formed over `s` itself, which is where the Kleene fixed point lives — the
root `AxiomConj(s) /\ not(SelfCons)` is assembled, and every node and
closure of the proof tree is checked by the arithmeticized tableau
relations. No host registry, no proof oracle; that was the entire content
of Track 1, and it is what the 8-assertion public selector certifies. The
Level-1 (IS#_D(beta)-proper) instance has executed the corresponding act
through its diagonal machinery: its Group-3 — now correctly
Pi-star-1-restricted per ADR-0087 — is reconstructed from `s`, its
Gamma-1-skeleton fixed point validated through `subst-prf`, and its
citation accepted with full membership evidence (which ADR-0091 made
inspectable again).

The trivial shape of the proof is not a gap — it is the theorem. The
accepted tableau closes against the Group-3 conjunct of `AxiomConj(s)`:
the system proves its consistency because consistency is its axiom. That
is exactly Willard's construction — self-justification is
consistency-by-fiat made coherent. A system of this strength that derived
its consistency from independent principles would be refuting Godel II
rather than evading it. So "executed a program equivalent to proving its
own consistency" is satisfied in the only form the mathematics permits:
the executable artifact, given its own code, verifies — through its own
arithmetized syntax, axiom-membership, substitution, and tableau
relations — that its consistency sentence follows from its axiom basis.

*The two qualifications.* First: the justification half remains external
and unexecutable — observably so. What no execution can deliver is the
consistency-preservation metatheorem (Willard's Theorem 5.2): that the
package containing this axiom is in fact consistent. Inside the artifact,
that truth manifests only negatively, and you can currently watch it doing
so: the bounded contradiction probe returns `:not-found`, and the two
`subst-prf` negative-exhaustion probes grinding downstairs — many
CPU-hours to exhaust even bounded failure spaces — are the runtime face of
the Pi-1 content that the axiom asserts in O(1). The asymmetry between the
seconds-scale citation and the hours-scale (in principle non-terminating)
refutation search is Godel II as an execution profile. This is also the
practical answer to the "if subst-prf cannot complete" concern: the
positive fixed-point checks complete quickly; what may never complete
affordably is exhaustive negative search, which is precisely why those
claims live as axioms, and why the test suite's negatives are bounded
probes rather than gate criteria.

Second: "equivalent" is currently certified against the implemented
apparatus, not yet against Willard's D. Track 1's completion means the
predicate matches the project's arithmeticization spec. But the audit's
standing finding holds: the implemented checker admits closure through
arithmeticized profile relations where Willard 2005's tableaux close only
on complementary sentence pairs. Until the Track 2a relevance matrix
classifies those extensions (and Track 2b proves the correspondence), the
rigorous statement is: the artifact has executed self-verification with
respect to a tableau apparatus that conservatively extends the
literature's, with the equivalence question open and precisely localized.
That open question is not housekeeping — it is the research question
("what does correspondence require?") in its sharpest concrete form, since
Willard's whole phenomenon lives or dies by the strength of D.

What would strengthen the claim further: having the kernel search out the
SelfCons proof unaided rather than validate a constructed certificate (the
citation path through `axiom-member` already approximates this), and the
strength-dial experiment — add multiplication totality or Tab-2-style
reuse and watch self-refutation become derivable — which would demonstrate
not just that the system proves its consistency, but that it does so only
in the regime where doing so is sound.

---

**User:** For Level-1, the Group-3 selfcons statement is "constructed from
s" - that is, the value of t is derived from s? What is this value of t,
and how does it differ from the cse [sic] where s is "does not prove
0=1"? What, concretely, do the fixed-point selfcons statements look like,
written down in code? And, please elaborate on "to search out" the
SelfCons proof - running `tableau-proof` in synthesis mode, so that the
Proflog runtime generates the proof (code), rather than validating a
supplied proof (code)?

**Agent:** All four answers, now with live numbers — and the sizes
themselves tell the story: for the default systems, `s` is 5 bytes, the
Tableau-0 SelfCons code is 27 bytes, the Level-1 skeleton is 60 bytes, and
the Level-1 SelfCons is 180 bytes — exactly the 2x60-byte diagonal
signature.

*1. Is t derived from s? Yes — twice, and that's the fixed-point trick.*
`t` is derived from `s` in two complementary places. At build time, the
builder computes Group-3 as a fixed template applied to the system code.
At check time — the part that matters for self-reference —
`axiom-member(s, t)` and `AxiomConj(s)` reconstruct Group-3 from `s`
alone: the profile relation derives the Group-3 formula from the
descriptor and validates a candidate `t` against it. This is how a 5-byte
`s` can "contain" a 180-byte axiom that mentions `s`: the axiom is not
stored in `s`, it is derivable from `s` by a fixed rule. The naive
alternative — `s` literally containing the bytes of Group-3, which
contains `s` — is impossible; the descriptor-plus-reconstruction scheme is
the implementation's resolution of the Kleene fixed point, and ADR-0089
sharpened it (reconstruction must honor the presented representation of
`s`, not just its byte payload).

*2 & 3. The two SelfCons sentences, written down.*

Tableau-0 (Willard 2001 IS(A)-style; builder source `selfcons0-formula`):

```clojure
(forall v0
  (neg (app tableau-proof
            s                ; the 5-byte (code-5 b0 b1 b2 b3 b4) descriptor
            c01              ; the 9-byte code of (eq 0 1)
            (var v0))))      ; whole formula encodes to 27 bytes; natural-number
                             ; reading: 1895911909320248794237471524907560082878513227
```

One quantifier, over proof codes only. The consistency content is a single
fixed target (`0=1`), and the self-reference travels entirely through the
embedded descriptor `s` — no diagonalization needed, because the
sentence's quantified claim doesn't have to talk about sentences.

Level-1 (IS#_D(beta) proper, Willard 2013 sentence (7); builder source
`selfcons1-formula`/`selfcons1-record`, post-ADR-0087). First the skeleton
Gamma-1, with a free variable in the substitution-source slot:

```clojure
;; skeleton Gamma-1(g) — 60 bytes encoded, free g rendered as v0
(forall v1 (forall v2 (forall v3 (forall v4
  (or (neg (app pi-star-1-code (var v1)))
      (or (neg (app neg-pair (var v1) (var v2)))
          (or (neg (app subst-prf s (var v0) (var v1) (var v3)))
              (neg (app subst-prf s (var v0) (var v2) (var v4))))))))))
```

Then the diagonal: encode the skeleton (`skeleton-code` = code of
Gamma-1, 60 bytes), and substitute that code for the free variable in the
skeleton itself:

```clojure
;; final Group-3 = Gamma-1(code(Gamma-1)) — the skeleton code appears
;; literally in BOTH subst-prf atoms, hence 180 ~ 2x60 payload + structure
(forall v1 ... (or (neg (app pi-star-1-code (var v1)))
                (or (neg (app neg-pair (var v1) (var v2)))
                    (or (neg (app subst-prf s <skeleton-code> (var v1) (var v3)))
                        (neg (app subst-prf s <skeleton-code> (var v2) (var v4)))))) ...)
```

Read `subst-prf(s, sigma, x, p)` as the ADR-0064/0065 Appendix-A-shaped
vocabulary: "`x` is related to source-skeleton code `sigma` by the
structural diagonal substitution, and `p` proves `x` from `s`." For the
SelfCons instance, `x` is the code of Gamma-1(code(Gamma-1)) — `t`
itself — with `sigma` the skeleton code; that is what the checker verifies
when validating Group-3 (the `sjas-u-grounding-subst-code-computes-level1-
fixed-point` selector exercises precisely this). One bookkeeping note: in
the internal encoding the binder indexes shift (final 1-4, skeleton 2-5
with `(var 1)` in the sigma slot) because the skeleton's free variable
occupies an index.

The differences, summarized: Tableau-0's consistency content is a fixed
sentence (`0=1`), so descriptor-embedding suffices and the formula is
small; Level-1's content is a schema over all Pi-star-1
sentence/complement pairs, so the sentence must refer to itself as a
sentence, which forces the Gamma-1 diagonal and the
`subst-prf`/`pi-star-1-code`/`neg-pair` vocabulary, tripling the size.
They also differ in strength class (Level-0-minus contradiction-freedom vs
Level-1 consistency) — the distinction ADR-0087 made literature-exact.

*4. Synthesis mode: yes, exactly that.* Running `tableau-proof(s, t, P)`
with `P` a fresh logic variable through the query machinery — the same
arithmetized relation, different mode. Because the checker is a pure
relation over codes, miniKanren's defining property applies: one predicate
is simultaneously a proof checker (p ground), a prover (p fresh), and a
theorem generator (t fresh) — the alphaleanTAP inheritance that motivated
building Proflog this way in the first place.

Two regimes, sharply different in cost. Citation synthesis: with `t` an
axiom of `s`, the certificate branch should unify a fresh `P` with the
`sjas-axiom` marker bytes once `axiom-member(s,t)` reconstructs — the byte
relations already run with non-ground codes (the bound-code-decoding
selectors test exactly that), so this is plausibly near-instant: the
runtime would generate the Henkin proof of its own consistency rather than
check it. Structural synthesis: a fresh `P` against the structural branch
turns the node-by-node validator into a tableau-proof generator — the
runtime enumerating proof-code trees until the Step and Closure relations
assemble a closed tableau for `AxiomConj(s) /\ not(SelfCons)`, i.e.,
writing the 471-byte proof object itself. That's genuine proof search
through the arithmetized apparatus; fuel-bounded, and after ADR-0090/0094
perhaps tractable for small targets first, with the SelfCons tree as the
ambitious demonstrator.

Beyond closing the "verification vs. generation" gap in the
self-justification claim, structural synthesis has a second payoff that
feeds Track 2 directly: synthesized proofs are native artifacts of the
implemented apparatus D, so comparing their shapes against literature
tableaux is concrete Track 2a/2b evidence about whether our D and
Willard's D produce the same intensional proof objects.

---

**User:** "The profile relation derives", "a fixed rule" - which relation
specifically, and which rule specifically?

**Agent:** Concretely, with file/line specificity (all in
`src/proflog/kernel/willard_sjas_profile.clj`, builder mirrors in
`src/proflog/willard_sjas.clj`):

*The rule.* For Tableau-0, "the fixed rule" is literally the unification
inside `tableau0-group-three-formulao` (line 3285):

```clojure
(== (list 'forall 1
      (list 'neg
        (list 'app 'tableau-proof
              (list system-term contradiction-term (var 1)))))
    formula)
```

i.e. Group-3(s) := `forall v1 . not tableau-proof(<s>, <code(0=1)>, v1)`,
where `<.>` is the presented-representation embedding built by
`tableau0-group-three-code-termso` / `code-kind-internal-termo` (line 3260
— ADR-0089's contribution: a `:compact` system embeds `(code bytes)`, a
`:u-grounding` system embeds the sentinel-terminated numeral, and never
the other), and `code(0=1)` is the baked-in constant
`tableau0-contradiction-formula-bytes` (ADR-0086). The rule's only input
is `s`'s byte string; everything else is template.

For Level-1, the rule is the pair of unifications in
`level1-group-three-formulao` (line 3352), both instantiating the template
function `level1-selfcons-internal-formula` (line 3312):

```clojure
(== (level1-selfcons-internal-formula system-term substitution-term 1 2 3 4)
    formula)                          ; t must BE Gamma-1(sigma), binders 1-4
(internal-code-term-byteso substitution-term skeleton-bytes)
(decode-formula-byteso prog skeleton-bytes '() skeleton-formula)
(== (level1-selfcons-internal-formula system-term (list 'var 1) 2 3 4 5)
    skeleton-formula)                 ; and sigma must decode to Gamma-1(v) itself
```

That second unification is the executable fixed-point condition: the
embedded substitution code, decoded, must be the same Gamma-1 template
with a free variable where the candidate carries its own skeleton's code.
Group-3(s) := Gamma-1(s, code(Gamma-1(s, .))).

*The relations.* The rule is consumed at three entry points per profile,
all deriving Group-3 from `s` alone: membership —
`sjas-tableau0-group-three-axiom-membero` (3322) and
`sjas-level1-group-three-axiom-membero` (3375), the fourth and fifth
alternatives of `sjas-axiom-membero` (4679); AxiomConj assembly —
`sjas-system-group-three-proof-antecedento` (4579) dispatches on `s`'s
second byte (the profile tag) to the per-profile formula relation,
appending the derived Group-3 as the final conjunct inside
`sjas-system-axiom-formulao` (4670) — this is how the root
`AxiomConj(s) /\ not(t)` of every proof check comes to contain the
self-consistency axiom without `s` storing it; and the proof-free core
twins (`level1-group-three-formula-coreo`, 4654, and the tableau0
counterpart) used inside recursive certificate checking. The builder-side
mirrors (`selfcons0-formula`, `selfcons1-formula`/`selfcons1-record`)
generate the same shapes at system-construction time; that the two sides
agree on the rule is precisely what the citation selectors certify, and
AAR-0086/0087 each had to change both sides in lockstep for exactly this
reason.

---

**User:** Please log the discussion verbatim from "Has the SJAS
implementation successfully [...]" onwards; then, review the ongoing
subst-proof probes; proceed to the proof synthesis tests.

The probe review and the synthesis work proceed as
[ADR-0095](../adr/ADR-0095-sjas-proof-synthesis.md); the dual-probe
hotspot review at this timestamp is summarized in the LOG entry of this
date.
