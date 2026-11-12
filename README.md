# Sudoku Solver

A sudoku solver written in Java that solves puzzles the way a human does: with logical techniques,
explaining every step it makes. Guessing (trial and error) is used only as the last resort.

## Techniques

`Sudoku.solve()` tries the techniques from the simplest to the hardest. As soon as one of them makes progress
(places a digit or removes a candidate), the solver starts again from the first one.

| #  | Technique                                   | Where                                                  |
|----|---------------------------------------------|--------------------------------------------------------|
| 1  | Hidden Single                               | `House.hiddenSingle`                                   |
| 2  | Locked Candidates (Pointing / Claiming)     | `House.lockedCandidates`                               |
| 3  | Naked Single                                | `Cell.nakedSingle`                                     |
| 4  | Naked & Hidden Subsets (pairs, triples, quads) | `House.subsets`                                     |
| 5  | X-Wing / Swordfish / Jellyfish              | `House.xWing`                                          |
| 6  | Remote Pairs                                | `Sudoku.remotePairs`                                   |
| 7  | Simple Colouring (color wrap / color trap)  | `Sudoku.simpleColoring`                                |
| 8  | XY-Wing                                     | `Cell.xyWing`, `House.xyWing`                          |
| 9  | Skyscraper                                  | `Sudoku.skyscraper`                                    |
| 10 | XY-Chain                                    | `Group.xyChain`                                        |
| 11 | Forcing Chains on bivalue cells             | `Sudoku.forcingChain`                                  |
| 12 | Trial and Error (last resort)               | `Sudoku.trialAndError`                                 |

After a technique removes some candidates, a *post-technique* (`Sudoku.postXxx`) re-applies the simpler
techniques only around the cells that changed.

On a test set of 171 hard puzzles with a unique solution, all are solved correctly and 164 are solved
without trial and error.

## Project structure

- `sudoku/Sudoku.java` – the grid, the solver loop, the post-techniques and the techniques that work on the whole grid
- `sudoku/Cell.java` – a single cell (number, candidates, peers, chain building)
- `sudoku/Group.java` – a generic group of cells: base class of the houses and chain used by XY-Chain / Remote Pairs
- `sudoku/House.java` – a row, column or box, with the techniques that work inside a house
- `sudoku/Row.java`, `sudoku/Column.java`, `sudoku/Box.java`, `sudoku/HouseType.java` – the three kinds of house
- `utils/Utils.java` – array and geometry helpers

## Running

```sh
javac -d out sudoku/*.java utils/*.java
java -cp out sudoku.Sudoku
```

`Sudoku.main` reads the grid from the keyboard, row by row, with `0` for an empty cell.
It currently creates a 4x4 sudoku: change `new Sudoku(4)` to `new Sudoku(9)` for a classic sudoku.
