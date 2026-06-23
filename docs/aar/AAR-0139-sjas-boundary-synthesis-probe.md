# AAR-0139: SJAS Boundary Synthesis Probe

- Date: 2026-06-19
- ADR: [ADR-0139](../adr/ADR-0139-sjas-boundary-synthesis-probe.md)
- Branch: `adr-0139-sjas-boundary-synthesis-probe`

## Outcome

ADR-0139 is complete as the first proof-search synthesis probe surface for
ADR-0119 Workstream B.

The implementation adds `proflog.sjas-boundary-synthesis-probe`, with public
helpers to list Workstream B synthesis variants, describe each synthesis plan,
and produce a synthesis report for a selected generated SelfCons target. The
report can either run a live bounded `tableau-proof/3` proof-code search or
consume a proof code returned by a durable probe run. It then builds a
`:proof-search-synthesis` evidence candidate and screens it through
`screen-boundary-evidence`.

The fast tested path records the current ordinary `sjas-axiom` proof code as a
durable synthesis result for the total-multiplication target. The screen rejects
that candidate as `:ordinary-selfcons-citation`, leaves zero completed
obligations, and keeps both final Workstream B evidence obligations open.

The Workstream B roadmap now advertises the synthesis probe for total
multiplication, Xtab/LEM, and Tab-2-or-stronger. This is not final evidence:
actual constructed certificates and proof-search synthesis evidence remain open
for every variant.

## Evidence

Initial red selectors failed as intended:

```text
boundary-proof-search-synthesis-probe-exposes-all-workstream-b-variants
Could not locate proflog/sjas_boundary_synthesis_probe.clj on classpath.

boundary-proof-search-synthesis-probe-recorded-for-final-evidence
expected :implemented, actual nil
```

An initial live total-multiplication synthesis assertion was stopped after it
proved too slow for the ordinary not-slow gate. The implementation was narrowed
so the normal test covers durable result intake, candidate formation, and cheap
screening; live synthesis remains available through the probe helper and CLI
for detachable `test-runs/` executions.

Focused green selectors:

```text
lein test-vars :not-slow proflog.sjas-boundary-synthesis-probe-test
:SUMMARY pass=36 fail=0 error=0

lein test-vars proflog.sjas-correspondence-test/boundary-proof-search-synthesis-probe-recorded-for-final-evidence
:SUMMARY pass=19 fail=0 error=0
```

Final gates:

```text
lein test-proflog-fast
Ran 231 tests containing 1484 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1310 fail=0 error=0
```

## Follow-up

- Run long-lived live synthesis probes through the durable `test-runs/` pattern.
- Add nontrivial screened proof-search candidates before claiming the
  `:proof-search-synthesis` obligation.
- Add actual constructed-certificate candidates before claiming the
  `:constructed-certificate` obligation for any variant.

## 2026-06-19 Reassessment

ADR-0140 found that ADR-0139's live query synthesizes proofs of the positive
Group-3 SelfCons theorem. Rejecting the immediate axiom citation was necessary
but not sufficient: every result from that query targets ordinary
self-justification rather than the counterexample tuple inside
`not(SelfCons(S))`. The probe is therefore retained as diagnostic-only and is
no longer described as an executable path to final proof-search evidence. A
future synthesis ADR must search jointly for the complementary theorem codes
and measured proof objects required by the generated SelfCons body.
