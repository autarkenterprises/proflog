# Discussion: "Arithmetic" numeral reading and the deliberately non-bijective code reader

Date: 2026-06-13
Context: ADR-0095 proof synthesis. During the synthesis fix it was noted that
the compact-code byte reader `code-byte-termo` "reads numerals arithmetically,
so the reader is deliberately not a bijection." This log records the
follow-up question and answer verbatim, because the recognizer-vs-canonicalizer
distinction it draws is the reason citation synthesis must construct through
`sjas-internal-code-termo` rather than run the presented-code reader backward.

## Question

> because code-byte-termo reads numerals arithmetically, the reader is
> deliberately not a bijection — with what does "arithmetically" contrast?
> And, why would a relation be deliberately non-bijectional?

## Answer

Two good questions — they get at why the fix is "use the builder," not "make
the reader two-way."

### What "arithmetically" contrasts with

A compact code's byte argument isn't a raw integer — it's a **U-Grounding
numeral term**, an expression built from `dbl` (×2), `add`, `0`, and `1`. The
byte *value* 1 can be spelled by many different *terms*:

- canonical: `(app 1)`
- non-canonical: `(app add (app dbl (app 0)) (app 1))` — i.e. `2·0 + 1`, which
  also denotes 1

"Reading arithmetically" means `code-byte-termo` **evaluates the term to the
value it denotes** — `compact-code-byte-bits-termo` peels the dbl/add structure
into six bits, `byte-bitso` relates those bits to the byte number. It asks
*"what number does this term denote?"*

The contrast is **reading structurally** — matching the term's surface syntax
against a fixed table of the 64 canonical byte spellings (exactly what
`code-byte-build-termo` does via `byte-term-entries`). That would accept only
the one canonical spelling and reject `add(dbl(0),1)` as "not byte 1."

The existing test pins this distinction explicitly: it builds
`noncanonical-one = (add (dbl 0) 1)`, feeds it as a code byte, and asserts it
reads as byte 1 — *"read as U-Grounding numerals, not matched against generated
canonical byte terms"*
(`sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically`).

### Why a relation is deliberately non-bijective

Because `code-byte-termo` expresses a **semantic predicate** — "*term* denotes
*byte-value*" — and denotation over a numeral syntax is *intrinsically*
many-to-one: infinitely many terms denote each value. Making it injective would
mean restricting acceptance to a single normal form, i.e. reading **syntax
instead of number**. In an arithmetized-provability framework that's the wrong
specification: codes are *produced by arithmetic* (the diagonal/`subst-code`
machinery computes byte numerals through `add`/`dbl`, not by emitting canonical
literals), so the reader must accept whatever spelling the arithmetic handed it.
The non-injectivity isn't slack to be tolerated — it *is* the reader being
faithful to meaning rather than fussy about form. Forcing canonical-only would
unsoundly reject legitimate codes.

So the system correctly factors the job into two relations — the classic
**recognizer vs. canonicalizer** split:

| | relation | shape | job |
|---|---|---|---|
| read | `code-byte-termo` | many-to-one (total evaluator) | "does this term denote byte *v*?" |
| build | `code-byte-build-termo` | injective section (one canonical preimage) | "*the* canonical term for byte *v*" |

The builder is a deterministic choice function picking one normal-form
representative per value. That's why running the recognizer *backward* is the
wrong move for synthesis: inverting a many-to-one relation gives you *some*
preimage (and may enumerate or diverge), not the canonical one the rest of the
system — and `proof-certificate` — expects. The output direction needs the
section, which is precisely `sjas-internal-code-termo` (built on
`code-byte-build-termo`). The reviewer's "make the reader bidirectional" framing
collapses these two distinct jobs into one relation that can't soundly be both;
keeping them separate is the right design, and the arity tie added in ADR-0095
hardens only the *recognizer's* forward direction (reject `code-2` carrying
three args) without touching that recognizer/canonicalizer split.

(The docstring also notes a secondary, performance reason for the table-based
builder — re-deriving each digit through the recursive arithmetic reader would
dominate fixed proof checks — but the primary reason is the semantic one above.)
