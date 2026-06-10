# ADR-0079: SJAS Embedded Payload Header-First Dispatch

- Status: completed
- Date: 2026-06-08
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0079](../aar/AAR-0079-sjas-embedded-payload-header-first-dispatch.md)

## Context

After ADR-0077 and ADR-0078, the latest-source SelfCons core probe still
exceeded 15 minutes under load. A non-invasive JVM thread sample no longer
showed static `membero` table scans; instead it was in
`decode-embedded-code-bodyo`, repeatedly allocating logic variables while
enumerating possible embedded payload lengths.

The embedded code and natural decoders already know the two length-header bytes
for the payload. The current branch shape allocates `payload` before checking
whether the candidate length's low/high bytes match those headers, so every
wrong candidate still pays fresh-variable allocation and scheduling overhead.

## Decision

Check the low/high length header constraints before allocating the payload
logic variable in embedded code and natural payload decoders. This keeps the
same bounded finite enumeration and the same relational byte semantics, but
wrong length candidates fail before payload parsing allocates fresh state.

## Consequences

- Large formula-code payload decoding avoids avoidable logic-variable
  allocation for wrong length candidates.
- The change is local to payload length dispatch and does not introduce
  committed choice or host-side parsing of object formulas.
- The SelfCons core probe may still require deeper optimization if formula
  decoding remains the dominant cost after this cleanup.

## Test Obligations

- Add a red source-audit regression requiring header checks before
  `fresh [payload]`.
- Keep wide formula-bearing proof-code decoding and public U-Grounding proof
  selector green.
- Rerun fast and extended gates before closing the ADR.
