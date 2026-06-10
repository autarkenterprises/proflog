# SJAS SelfCons Godel Code Output

Date: 2026-06-06

## Context

ADR-0073 Track 1 requires the project to print the numerical Godel code of the
self-consistency statement for the concrete ordinary-tableau `IS#_D(beta)`
instance after the proof machinery is internalized. The existing Track 1 work
already generated Group-3 formula codes and used them in the public
`tableau-proof/3` checks, but there was no reviewer-facing command or public API
that printed the concrete decimal code.

## Change

Added `proflog.willard-sjas/selfcons-godel-code-report`,
`selfcons-godel-code`, and `print-selfcons-godel-code`. The report decodes the
generated Group-3 formal formula-code term back to its exact base-64 byte
sequence and then computes the ordinary natural-number view with
`bytes->natural`. It also retains the exact byte string because compact public
code terms are byte strings, and byte strings with trailing zeroes must not be
reconstructed through a lossy natural-number round trip.

Added the Leiningen alias:

```sh
lein print-sjas-selfcons-godel-code
```

For the default ordinary-tableau `IS#_D(beta)` instance, this printed:

```text
431159687003828162118819841327179
```

## Evidence

Red:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau0-selfcons-godel-code-is-publicly-printable
Syntax error compiling at (proflog/willard_sjas_test.clj:712:18).
No such var: sjas/selfcons-godel-code-report
Tests failed.
```

Green:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau0-selfcons-godel-code-is-publicly-printable
Ran 1 tests containing 10 assertions.
0 failures, 0 errors.
```

Print command:

```text
lein print-sjas-selfcons-godel-code
431159687003828162118819841327179
```

The regression checks that changing the reflected beta basis changes the
reported Group-3 self-consistency code, while changing runtime-only external
clauses does not. That keeps this output tied to the encoded
`IS#_D(beta)` system/formula coding path rather than to unrelated Proflog
runtime data.

## Source Audit Follow-Up

While rerunning the broader proof-profile source audit, one stale assertion
failed because it still expected the older `code-args-buildo` helper name in
the embedded-code reconstruction path. The implementation uses
`code-args-build-counto`, which is the counted byte-first builder introduced to
avoid rereading large embedded code payloads. The audit now checks for that
helper and explicitly rejects the public `code-argso` reader in the same slice.

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
```
