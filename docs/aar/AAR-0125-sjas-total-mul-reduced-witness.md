# AAR-0125: SJAS Total Multiplication Reduced Witness

- Date: 2026-06-18
- ADR: [ADR-0125](../adr/ADR-0125-sjas-total-mul-reduced-witness.md)
- Branch: `adr-0125-sjas-total-mul-reduced-witness`

## Outcome

ADR-0125 is complete as the reduced reflected-beta witness stage for the
total-multiplication Workstream B variant.

The implementation adds:

- `total-multiplication-squaring-chain-constants`, generating
  `tm-u0 ... tm-u<depth>`;
- `total-multiplication-squaring-chain-axioms`, generating the finite beta
  equations `tm-u0 = 2` and `tm-u(i+1) = mul(tm-ui, tm-ui)`;
- `total-multiplication-squaring-chain-summary`, recording the exponential
  bit-length growth visible from the finite fragment;
- `total-multiplication-reduced-witness-options` and
  `total-multiplication-reduced-witness-system`;
- an updated Workstream B audit that marks
  `:reduced-reflected-beta-witness` complete for total multiplication while
  keeping the full SelfCons contradiction target, constructed certificate, and
  proof-search synthesis evidence open.

The depth-3 reduced witness installs four chain equations, includes `mul/2` and
the chain constants in the language, changes encoded system identity and
regenerated Group-3/SelfCons code, and makes the final chain beta record
citeable through decoded `axiom-member/2` and `tableau-proof/3`.

This is not a final Goedel-boundary contradiction witness. It is the finite
squaring-chain compression fragment that later diagonal witness ADRs must use.

## Evidence

Initial red selectors failed as intended:

```text
boundary-failure-roadmap-keeps-witness-contract-open
expected :reduced-witness-implemented, actual :surface-implemented
expected completed reduced witness stage, actual nil

sjas-total-multiplication-reduced-witness-builds-squaring-chain
No such var: sjas/total-multiplication-squaring-chain-constants
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 9 assertions.
0 failures, 0 errors.

sjas-total-multiplication-reduced-witness-builds-squaring-chain
Ran 1 tests containing 12 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 221 tests containing 1377 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1142 fail=0 error=0
```

## Follow-up

- The total-multiplication variant still needs the full generated SelfCons
  contradiction target.
- Completion still requires both an explicit constructed certificate and
  proof-search synthesis evidence for that target.
- Tab-2-or-stronger and Xtab/LEM-as-axiom variants remain open.
