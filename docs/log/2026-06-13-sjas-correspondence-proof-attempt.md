# SJAS Correspondence Proof Attempt Audit

Date: 2026-06-13

ADR: [ADR-0101](../adr/ADR-0101-sjas-correspondence-proof-attempt.md)

Reviewed claim: [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md)

## Verdict

I cannot demonstrate the ADR-0100 theorem wholly as written.

The propositional, quantifier, and ordinary formula-bearing structural branches
can be matched to Willard's semantic-tableau method `D` with plausible
bookkeeping lemmas. The first-fragment proof-code grammar also gives a strong
anti-compression argument for genuine formula-bearing structural proof trees.

The whole theorem fails to close because ADR-0100 quantified over more than
those cases:

1. `sjas-structural-proof-check-state-decodedo` accepts branches that are not
   literal Willard `D` steps. They are arithmetic/profile/equality/reflected-call
   or recursive proof-predicate steps that need a selected extended apparatus or
   macro-expansion theorem.
2. The first fragment includes the bare `sjas-axiom` citation. That proof code
   is a fixed certificate for axiom membership, while the cited axiom formula is
   supplied by `S` and `F`. Therefore the lower bound cannot be stated as
   "the encoding of `P` has >= 5J bits" over the ADR-0100 covered domain.

## Target Attempted

The attempted theorem was the ADR-0100 target:

```text
For every covered S, F, and first-fragment proof certificate P:

  ProflogAccepts(P,S,F) iff SemPrf_D(decode(P),S,F)

and the encoding of P satisfies the Conventional Tableaux Encoding
Requirement, size(P) >= 5J(decode(P)).
```

Here `D` is the Willard semantic-tableau apparatus recorded in ADR-0100: root
`not F`, non-root nodes are axioms or one of the eight deduction rules, and each
branch closes by containing a sentence and its negation.

## Clause Audit

The checked relation is
`src/proflog/kernel/willard_sjas_profile.clj:sjas-structural-proof-check-state-decodedo`
at lines 6160-7130. Every branch below is tag-free and can consume a
formula-bearing proof node.

| Lines | Branch family | Proof attempt status |
|---|---|---|
| 6179-6209 | Literal save plus agenda continuation | Not a separate `D` rule. Plausible bookkeeping lemma: the literal is a branch node, and the child selected from `unexpanded` remains a deduction from an earlier alpha/de Morgan/agenda ancestor. Needs an explicit ancestor-preservation lemma. |
| 6210-6217 | Complementary literal closure | Direct Willard branch closure. |
| 6218-6236 | Conjunction | Direct alpha rule, with right conjunct carried by agenda. |
| 6237-6313 | Universal / once-universal expansions | Gamma-like, but the implementation uses branch variables in `env` and proof-vars. Needs a parameter-term/admissible-variable lemma; plausible but not explicit in ADR-0100. |
| 6314-6363 | Existential expansions | Direct delta rule if `sjas-next-branch-nomo` freshness is recorded as the fresh-parameter lemma. |
| 6364-6373 | `false` and `not true` leaves | Direct closure only if the selected tableau language treats `false` as contradictory and `not true` as contradictory. This is standard, but it was not in ADR-0100's Willard-rule table. |
| 6374-6392 | `not false` agenda continuation | Bookkeeping / truth simplification, not a literal `D` rule. Needs a truth-normalization irrelevance lemma. |
| 6393-6482 | Double negation and atomic/equality negation duals | Direct negation-normalization steps, provided the proof allows NNF rules as ADR-0100 claimed. |
| 6483-6596 | Negated conjunction/disjunction/implication and implication | Direct alpha/beta/implication/de Morgan correspondence, with same agenda bookkeeping caveat for same-branch decompositions. |
| 6597-6706 | Negated and bounded quantifier duals | Direct if bounded gamma/delta and negated quantifier duals are admitted exactly as in Willard's list; requires the same environment/freshness lemmas. |
| 6707-6754 | Reflexive/rigid/stored disequality | Not literal Willard branch closure unless the selected `D` includes a free-constructor/equality theory or a macro expansion into equality axioms plus ordinary closure. |
| 6755-6783 | Axiom-member, recursive tableau-proof, subst-prf, arithmetic/profile closes | Not covered by literal `D` as written. These are selected object-predicate/theory closures. They require explicit primitive status or macro-expansion theorems. |
| 6784-6805 | Positive equality contradiction, stored disequality violation, contradictory atoms after unification | Equality-theory closure, not a literal `D` step without an equality apparatus. |
| 6806-6867 | Equality-triggered reflected positive/negative calls | Combination of equality substitution and reflected procedure-call expansion. This needs an explicit reflected-call rule or macro expansion. |
| 6868-6889 | Equality progression with agenda continuation | Equality-theory state update plus bookkeeping. Needs a substitution-preservation lemma and selected equality apparatus. |
| 6890-6927 | Disjunction | Direct beta rule. |
| 6928-6985 | Direct positive/negative reflected calls | Not a literal Willard `D` rule. It can be a selected Group-2b reflected-clause deduction only after the reflected-call apparatus is defined and size-accounted. |
| 6986-7111 | Additional universal, once-universal, existential, bounded-universal, bounded-existential expansions | Direct gamma/delta/bounded gamma/bounded delta if the environment/freshness and bounded-guard lemmas are supplied. |
| 7112-7130 | `true` agenda continuation | Bookkeeping / truth simplification, not a literal `D` rule. Needs a truth-normalization irrelevance lemma. |

This audit is enough to refute ADR-0100's "every checker clause matched to a
`D` rule" sentence. Some branches are direct `D`; several are not.

## Partial Proof That Does Work

The following subtheorem is defensible from the current source:

```text
For non-axiom formula-bearing structural proof trees whose checker path uses
only propositional, first-order quantifier, bounded-quantifier, literal closure,
truth-normalization, and agenda-continuation branches, Proflog acceptance maps
to a Willard semantic-tableau proof up to explicit agenda and NNF
irrelevance lemmas.
```

The proof is by induction on the supplied proof tree:

- each proof node carries formula bytes, decoded through the structural proof
  decoder before the checker runs;
- alpha, beta, implication, negation, gamma, delta, bounded gamma, and bounded
  delta branches construct exactly the expected child formulas;
- agenda continuation linearizes same-branch descendants but does not change
  the ancestor relation;
- closure branches close only against an explicit contradictory branch formula
  or a standard truth constant.

This does not cover equality, arithmetic/profile closure, reflected calls,
recursive `tableau-proof/3`, `subst-prf/4`, or the bare `sjas-axiom` citation.

## Anti-Compression Attempt

For genuine formula-bearing structural proof trees, the lower-bound proof can
be strengthened beyond ADR-0100's informal paragraph.

Source facts:

- structural proof-tree audit records that a node is either
  `(byte-count byte... child...)` or `((byte...) child...)`, with exact byte
  payload accounting;
- `src/proflog/willard_sjas_code.clj` encodes every application term as at
  least three formula bytes: the app tag, symbol index, and arity byte;
- `proof-code-bytes` serializes every integer proof byte as at least two
  base-64 proof bytes and every list with additional list overhead;
- public compact `code-N` terms tie `N` to the actual byte argument count.

Therefore, for a structural proof term `P` with decoded tableau tree `T`:

```text
proof-byte-count(P) >= 2 * formula-byte-count(T)
formula-byte-count(T) >= 3 * app-occurrences(T)
bit-size(P) >= 6 * proof-byte-count(P)
```

So `bit-size(P) >= 36 * app-occurrences(T)`, which is stronger than the
`5J` requirement when `J` is the number of function-symbol/application
occurrences explicitly present in the formula-bearing tree.

This still has to be paired with the selected SJAS syntax measure for compact
`num` terms and embedded `code` terms. The point of the subargument is narrower:
the formula-bearing structural proof grammar is not itself compressing explicit
application-symbol occurrences out of the proof object.

This repaired argument is only for genuine formula-bearing structural proof
trees. It does not prove ADR-0100's covered domain because of `sjas-axiom`.

## Hard Blocker: Bare `sjas-axiom`

ADR-0100 explicitly includes the bare `sjas-axiom` citation in its covered
domain. In the implementation, `sjas-axiom-proof-bytes` is the fixed byte
encoding of the symbol `sjas-axiom`, and `tableau-proof/3` accepts that
certificate when decoded axiom membership holds for `S` and `F`.

That means the proof certificate `P` can be constant size while the cited axiom
formula, and therefore the semantic tableau axiom node it denotes, grows with
the theorem/system code arguments. This is not compatible with the ADR-0100
theorem's lower-bound sentence:

```text
the encoding of P satisfies >= 5J bits
```

unless one of the following changes is made:

- exclude bare `sjas-axiom` from the proof-size theorem;
- replace it with a formula-bearing axiom leaf whose proof code carries the
  axiom formula bytes;
- or explicitly define the SJAS proof object as including theorem/system code
  payload, then prove that this combined object is the literature-compliant
  encoded semantic-tableau proof.

The third option may be viable, but it is not ADR-0100's stated theorem.

## Recursive Predicate Blocker

The recursive `tableau-proof/3` and `subst-prf/4` structural closes are closer
to the intended arithmeticized proof predicate, but they still require a
separate theorem:

```text
negative object predicate leaf closes
iff
the encoded object proof predicate relation succeeds
iff
there exists a corresponding SJAS tableau proof object with preserved size.
```

The current implementation decodes proof bytes and calls the structural checker
recursively. This is much better than an opaque host proof trace, but it is not
itself a proof that the recursive object relation is Willard's `SemPrf_D`.
Closing this would require a well-founded induction on nested proof-code
payloads or a stratified definition of the selected proof predicate, plus the
same size argument for nested proof bytes.

## What Would Make the Proof Whole

One of these revised targets is needed:

1. **Narrow literal-Willard target.** Remove bare `sjas-axiom`,
   equality/profile/arithmetic/reflected/proof-predicate shortcut branches, or
   require their formula-bearing macro expansions to appear explicitly in the
   proof tree. Then prove the direct `D` correspondence for the remaining
   structural tree.

2. **Extended selected apparatus.** Define a stable `D_SJAS` that includes
   equality, arithmetic, code predicates, reflected calls, `tableau-proof/3`,
   and `subst-prf/4` as selected primitives or macros. Prove that Willard's
   self-verification results apply to `IS#_{D_SJAS}(beta)`. This is closer to
   Track 2c unless the literature already permits exactly these primitives.

3. **Combined proof-object encoding.** Define the proof object as the tuple
   `(S,F,P)` or an encoded semantic-tableau expansion that includes the
   theorem/system payload. This may repair `sjas-axiom`, but it changes the
   proof-size statement and must be checked against the literature's proof-code
   quantification.

## Conclusion

ADR-0100 should not be treated as a completed Track 2b proof over its stated
covered domain. It remains a useful scaffold and partial proof for the ordinary
formula-bearing structural fragment, but a whole demonstration requires a
revised theorem and additional lemmas before the project can honestly claim
literature-compliant correspondence.
