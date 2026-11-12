# Sudoku Solver

A sudoku solver written in Java that solves puzzles the way a human does: with logical techniques,
explaining every step it makes. Guessing (trial and error) is used only as the last resort.

It is also a web app that can be installed on a phone (PWA): the Java solver is compiled to JavaScript with
[TeaVM](https://teavm.org) and runs entirely on the device, offline too.

## Web app

A sudoku can be entered in three ways:

- **Take a photo** with the camera of the phone;
- **Upload a file**: a photo/screenshot of a sudoku, or a text file (`.txt`, `.sdk`, `.ss`, `.csv`...) with the
  81 digits, using `0` or `.` for the empty cells;
- **Type it in** on the grid (on a computer the keyboard, arrow keys and paste also work).

From a photo the app finds the grid (its corners can be moved by hand), reads the digits and highlights in yellow
the ones it is not sure about, so that they can be checked before solving. Photo reading is done in the browser:
no image leaves the device.

The solution is shown step by step: for every step the technique, its explanation and the cells involved are
highlighted on the grid. Puzzles with no solution or with more than one solution are reported.

To install it: open the page on the phone and choose *Install app* (Android/Chrome) or
*Share → Add to Home Screen* (iPhone/Safari).

### Building and running locally

Requirements: JDK 21 and Maven.

```sh
mvn package                       # compiles the solver to web/solver/solver.js
cd web && python -m http.server 8000
```

then open <http://localhost:8000>. The page must be served over HTTP (or HTTPS), not opened as a file,
because it uses JavaScript modules, a Web Worker and a service worker.

The workflow `.github/workflows/pages.yml` builds the app and publishes it on GitHub Pages at every push on `main`
(enable it once in *Settings → Pages → Source: GitHub Actions*).

Web app files:

- `webapp/WebSolver.java` – entry point compiled by TeaVM: solves a sudoku and returns the result and the steps as JSON
- `pom.xml` – Maven/TeaVM build
- `web/index.html`, `web/css/app.css`, `web/js/app.js` – the user interface
- `web/js/ocr.js` – grid detection, perspective correction and digit recognition for photos
- `web/js/solver-worker.js` – runs the solver in a Web Worker so that the page stays responsive
- `web/sw.js`, `web/manifest.webmanifest`, `web/icons/` – offline support and installation

## Techniques

Before solving, `Sudoku.solve()` checks with a fast backtracking search (`SolutionCounter`: bit masks and
minimum-remaining-values heuristic, stopping at two solutions) that the sudoku has exactly one solution.
If it has no solution or more than one, the user is told and nothing else is done.

Then `Sudoku.solve()` tries the techniques from the simplest to the hardest. As soon as one of them makes progress
(places a digit or removes a candidate), the solver starts again from the first one.

| #  | Technique                                        | Where                            |
|----|--------------------------------------------------|----------------------------------|
| 1  | Hidden Single                                    | `House.hiddenSingle`             |
| 2  | Naked Single                                     | `Cell.nakedSingle`               |
| 3  | Locked Candidates (Pointing / Claiming)          | `House.lockedCandidates`         |
| 4  | Naked & Hidden Subsets (pairs, triples, quads)   | `House.subsets`                  |
| 5  | X-Wing / Swordfish / Jellyfish                   | `House.xWing`                    |
| 6  | Unique Rectangle (types 1, 2, 4)                 | `Patterns.uniqueRectangle`       |
| 7  | Remote Pairs                                     | `Sudoku.remotePairs`             |
| 8  | Skyscraper                                       | `Sudoku.skyscraper`              |
| 9  | Two-String Kite                                  | `Patterns.twoStringKite`         |
| 10 | Finned / Sashimi X-Wing and Swordfish            | `Patterns.finnedFish`            |
| 11 | Simple Coloring (color wrap / color trap)        | `Sudoku.simpleColoring`          |
| 12 | W-Wing                                           | `Patterns.wWing`                 |
| 13 | XY-Wing                                          | `Cell.xyWing`, `House.xyWing`    |
| 14 | XYZ-Wing                                         | `Patterns.xyzWing`               |
| 15 | Multi-Coloring                                   | `Patterns.multiColoring`         |
| 16 | X-Cycles                                         | `Chains.xCycles`                 |
| 17 | XY-Chain                                         | `Group.xyChain`                  |
| 18 | AIC (Alternating Inference Chains)               | `Chains.aic`                     |
| 19 | ALS-XZ                                           | `Chains.alsXz`                   |
| 20 | Forcing Chains (cell and digit, singles only)    | `Chains.forcingChains`           |
| 21 | Forcing Net on bivalue cells                     | `Sudoku.forcingNet`              |
| 22 | Trial and Error (last resort)                    | `Sudoku.trialAndError`           |

The order follows the usual difficulty ratings of the techniques. Unique Rectangle assumes that the puzzle
has a unique solution.

After a technique removes some candidates, a *post-technique* (`Sudoku.postXxx`) re-applies the simpler
techniques only around the cells that changed.

On a test set of 171 hard puzzles with a unique solution, all are solved correctly: 169 with logical
techniques only (no forcing net and no trial and error), the other 2 need the forcing net.

## Project structure

- `sudoku/Sudoku.java` – the grid, the solver loop, the post-techniques and the techniques that work on the whole grid
- `sudoku/Cell.java` – a single cell (number, candidates, peers, chain building)
- `sudoku/Group.java` – a generic group of cells: base class of the houses and chain used by XY-Chain / Remote Pairs
- `sudoku/House.java` – a row, column or box, with the techniques that work inside a house
- `sudoku/Patterns.java` – Unique Rectangle, Two-String Kite, Finned Fish, W-Wing, XYZ-Wing, Multi-Coloring
- `sudoku/Chains.java` – X-Cycles, AIC, ALS-XZ, Forcing Chains
- `sudoku/SolutionCounter.java` – fast backtracking solution counter used for the uniqueness check
- `sudoku/CandidateGrid.java` – bit mask snapshot of the candidates used by `Patterns` and `Chains`
- `sudoku/Row.java`, `sudoku/Column.java`, `sudoku/Box.java`, `sudoku/HouseType.java` – the three kinds of house
- `utils/Utils.java` – array and geometry helpers

## Running

```sh
javac -d out sudoku/*.java utils/*.java
java -cp out sudoku.Sudoku
```

`Sudoku.main` reads the grid from the keyboard, row by row, with `0` for an empty cell.
It currently creates a 4x4 sudoku: change `new Sudoku(4)` to `new Sudoku(9)` for a classic sudoku.
