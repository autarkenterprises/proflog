# SJAS Subst-Code Symbol-ID Decoder

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 substitution slice removes source symbol-registry dependence from
`subst-code/2` over user-symbol formulas.

Substitution is structural. It must preserve application-head identity and
replace free variable index `1` with the source formula's own public code term,
but it does not need to know the host spelling or semantic interpretation of a
user relation such as `demo`. The previous `subst-code` path decoded source
and target formulas through the proof-facing symbol resolver, so it failed when
`:sjas/registry` was absent and the formula contained a non-reserved user
application head.

The substitution decoder now uses the syntax/numeric-symbol formula decoder for
source and target codes. User heads decode as structural `(sym n)` terms, and
the fused substitution-plus-alpha-equivalence relation compares those ids
directly. Fixed SJAS semantic primitives are still recovered by the
proof-facing decoder where branch closure needs their meaning; `subst-code`
does not need that semantic dispatch.

## Red/Green Evidence

The focused regression
`sjas-subst-code-decodes-user-symbols-without-symbol-registry` builds the
source formula `demo(v0)`, the target formula `demo(code(demo(v0)))`, removes
`:sjas/registry`, and asks `subst-code(source, target)`.

Before the change, the positive check failed:

```text
FAIL in (sjas-subst-code-decodes-user-symbols-without-symbol-registry)
Subst is structural and must not need the source symbol registry for user application heads
  actual: (not (successful? ()))
```

After the change, the same selector passes and the negative identity check
still rejects `subst-code(source, source)`.

## Remaining Boundary

This is not full signature internalization. It removes the source codebook only
where structural symbol identity is sufficient. User-symbol semantic
interpretation remains unavailable without either reflected system-code
records or a future structural signature block in `system-code`.

The result is nevertheless a strict Track 1 improvement: diagonal substitution
over formula codes no longer relies on a host registry for user application
heads.

## Verification

```text
timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-subst-code-decodes-user-symbols-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-subst-code-computes-general-formula-code-substitution
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-subst-code-relates-structural-substitution-codes
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 260s lein test :only proflog.willard-sjas-test/sjas-subst-prf-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 260s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-decode-built-in-relations-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
```
