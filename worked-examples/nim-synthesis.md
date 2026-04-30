# Nim Synthesis

This file covers `test/proflog/nim_synthesis_test.clj`.

The current winning-move witness formula is:

```clojure
(x = s(y) or x = s(s(y))) and not win(y)
```

So each example asks for a concrete witness `y` showing that position `x` has a
move to a losing position.

## Winning Witnesses

Current committed witnesses:

```clojure
win(1) via y = 0
win(2) via y = 0
win(4) via y = 3
win(5) via y = 3
```

These witness formulas all succeed directly.

## Wrong Witnesses

The suite also checks that bad witnesses are rejected:

```clojure
win(1) via y = 1 => fails
win(4) via y = 2 => fails
```

So the current kernel is not merely finding some proof. It is distinguishing
the intended losing successor from an arbitrary candidate.

## Deeper Ground Positions

The extended namespace also checks:

```clojure
win(4) => succeeds
win(5) => succeeds
```

These are the next concrete winning positions beyond the fast baseline.
