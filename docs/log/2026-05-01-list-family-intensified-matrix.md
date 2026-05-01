# Intensified List-Family Matrix

Date: 2026-05-01
Branch: `adr-0033-structural-answer-variable-recursion`

## Purpose

After ADR-33 closed the inherited ADR-31 list-family rows, the matrix was
expanded to interrogate the same structural residual-completion path with
deeper nesting, longer lists, and multiple answer variables.

## Added Rows

Default matrix regression additions:

- `append-inverse-flat`: `append(x, y, [a,b,c])`, two answer variables and all
  four split points;
- `reverse-output-deep-nested-longer`:
  `reverse([[[a]],[[b]],[[c]]], r)`;
- `reverse-partial-output-longer-tail`:
  `reverse([a,b,c,a], cons(a, r))`.

Probe catalog additions outside the default regression:

- `append-inverse-flat-longer`: `append(x, y, [a,b,c,a])`, two answer variables
  and five split points, configured with `raw-limit 32`;
- `reverse-input-flat-longer`: `reverse(r, [c,b,a])`, a longer version of the
  carried input-synthesis row.

## Results

The default intensified matrix passed:

```text
timeout -k 10s 360s lein test proflog.list-kernel-matrix-test
```

Result:

```text
Ran 2 tests containing 22 assertions.
0 failures, 0 errors.
```

Focused probe results:

- `reverse-output-deep-nested-longer` found its target through the ordinary raw
  matrix path.
- `reverse-partial-output-longer-tail` found its target through the ordinary
  raw matrix path and is now covered by the default matrix regression.
- `append-inverse-flat-longer` found all five split targets when run with
  `raw-limit 32`, but it is deliberately not part of the default matrix test
  because it is a heavier stress row.
- `reverse-input-flat-longer` did not return a probe result within a 240 second
  timeout in the first stress attempt. It remains in the catalog as an
  explicit longer-list input-synthesis frontier for future work, but it is not
  promoted to the passing gate.

## Interpretation

ADR-33's structural completion generalizes beyond the exact inherited rows:
it handles a deeper nested reverse output and a longer partial reverse output
without list-symbol dispatch.

The remaining stress signal is asymmetric. Lengthening output/partial reverse
works under the current bounded completion path, while lengthening reverse
input synthesis still stresses raw search enough to exceed the probe timeout.
That is useful pressure for future search-control work, but it should not
invalidate the ADR-33 closure unless its exit criteria are deliberately raised.
