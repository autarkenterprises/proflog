# ADR-0080: SJAS Application Arity Byte Dispatch

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0080](../aar/AAR-0080-sjas-app-arity-byte-dispatch.md)

## Context

After ADR-0079 moved embedded payload length-header checks before payload
allocation, the post-ADR-0079 SelfCons probe advanced to a new hot stack:
`decode-app-arityo` repeatedly tried candidate application arities by unifying
each candidate against the same byte stream head.

The formula-code grammar stores application arity as the next byte after the
symbol index. Recursively trying every arity candidate is therefore unnecessary
for decoding a presented byte stream.

## Decision

Destructure the encoded arity byte once, then dispatch through explicit finite
arity alternatives that parse the argument list for the matching host arity.
Apply the same scheduling shape to syntax-only app decoding and syntax-skip app
decoding.

This keeps the decoder relational and object-level: arity remains an encoded
byte in the SJAS stream, and argument parsing still uses the existing structural
relations. The change is only the scheduling of the finite arity choice.

## Consequences

- Wrong arity candidates fail against a small byte equality instead of
  repeatedly unifying the whole `(arity . args)` byte tail.
- Application term decoding remains bounded by the same maximum arity.
- If SelfCons remains slow, the next sample should point past arity decoding to
  the following formula-code or proof-checker bottleneck.

## Test Obligations

- Add a red source-audit regression rejecting recursive arity candidate
  schedulers.
- Keep wide formula-bearing proof-code decoding and public U-Grounding proof
  selector green.
- Rerun fast and extended gates before closing the ADR.
