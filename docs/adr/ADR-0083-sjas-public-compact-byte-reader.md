# ADR-0083: SJAS Public Compact Byte Reader

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0083](../aar/AAR-0083-sjas-public-compact-byte-reader.md)

## Context

ADR-0073 Track 1 requires public formula, system, substitution, and proof codes
to be inspectable object terms. For compact `code-N` terms, each byte argument
is itself a public U-Grounding numeral. The public reader must therefore parse
the presented byte numeral structure. It must not merely rebuild the canonical
byte term from a decoded byte value and compare against that finite expansion.

The audit found that `code-byte-termo` has the intended parser shape, but
`code-argso` and its proof-free companion `code-args-coreo` currently call
`code-byte-build-termo`. That builder is appropriate when reconstructing
embedded code payloads from already decoded bytes, but it is the wrong relation
for public-code input.

## Decision

Use `code-byte-termo` in public compact-code readers:

- `code-argso`;
- `code-args-coreo`.

Keep `code-byte-build-termo` in byte-first reconstruction paths such as
`code-args-buildo` and embedded code payload construction.

## Consequences

- Public compact code terms are accepted by interpreting each byte argument as
  a numeral relation.
- Byte-first reconstruction remains available for decoded embedded payloads.
- This is a correctness/internalization change, not a SelfCons optimization.

## Test Obligations

- Add a red source audit proving public compact-code readers call
  `code-byte-termo` and do not call `code-byte-build-termo`.
- Keep the existing noncanonical compact-byte semantic regression green.
- Keep the broad SJAS source audit green.

## After Action

Completed on 2026-06-09. The public compact-code reader and proof-free
companion now parse presented byte numerals through `code-byte-termo`; the
byte-first builder remains isolated to embedded payload reconstruction. Focused
selectors and the fast/extended gates passed. See the AAR for details.
