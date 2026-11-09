(ns proflog.fitting.programs
  "The programs of M. Fitting, \"Tableaus for Logic Programming\" (1993),
  written as the paper writes them: each language as its symbols, and each
  program as its clauses (see `proflog.fitting.syntax` for the notation).
  They are plain data, in the form `proflog.fitting/ask` takes:

      (ask L2 P2 '(win (s (s (s 0)))))")

(def L1
  "§2: \"L has 0 as a constant symbol, s as a one-place function symbol, and
  even and odd, and of course = as relation symbols.\""
  '{:constants [0]
    :functions {s 1}
    :relations {even 1 odd 1}})

(def P1
  "Program P1 (§2), \"intended to recognize the even and the odd numbers,
  where the number n is identified with s^n(0)\":

      even(x) ← x = 0 ∨ (∃y)[x = s(y) ∧ odd(y)]
      odd(x)  ← (∀y)[even(y) ⊃ ¬(x = y)]"
  '[(<- (even x) (or (= x 0) (exists y (and (= x (s y)) (odd y)))))
    (<- (odd x) (forall y (implies (even y) (not (= x y)))))])

(def L2
  "§2: \"L has 0 as a constant symbol, s as a one-place function symbol, and
  win as a relation symbol (and equality ...).\""
  '{:constants [0]
    :functions {s 1}
    :relations {win 1}})

(def P2
  "Program P2 (§2), \"for a simple version of the game nim, in which there
  are two players, one starts with a positive integer and, taking turns, a
  player lowers the number by either 1 or 2. The first player unable to move
  looses.\"

      win(x) ← (∃y)[(x = s(y) ∨ x = s(s(y))) ∧ ¬win(y)]"
  '[(<- (win x) (exists y (and (or (= x (s y)) (= x (s (s y)))) (not (win y)))))])

(def L-move
  "The language of §8's factored version of P2."
  '{:constants [0]
    :functions {s 1}
    :relations {win 1 move 2}})

(def move-program
  "§8: \"A more usual way of presenting this might be:

      win(x) ← (∃y)[move(x, y) ∧ ¬win(y)]
      move(x, y) ← x = s(y) ∨ x = s(s(y))

  It is not hard to check that this does not work as expected. The reason is
  that, while the interpretation of equality is fixed, the interpretation of
  move in a weak Herbrand model is not — there could be non-standard
  moves.\""
  '[(<- (win x) (exists y (and (move x y) (not (win y)))))
    (<- (move x y) (or (= x (s y)) (= x (s (s y)))))])
