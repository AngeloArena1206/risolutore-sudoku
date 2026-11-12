/*
 * Web Worker that runs the Java solver (compiled to JavaScript by TeaVM, see pom.xml) in the background,
 * so that the page stays responsive while a hard sudoku is being solved.
 *
 * Messages in:   { id, solve: "81 characters" }  -> solves the sudoku
 *                { id, count: "81 characters" }  -> counts its solutions (0, 1 or 2 = "more than one")
 * Messages out:  { id, result: {status, solution, steps, techniques}, ms } | { id, count } | { id, error }
 */
import { solve, countSolutions } from '../solver/solver.js';

self.onmessage = (event) => {
  const { id } = event.data;
  const start = performance.now();
  try {
    if (event.data.count !== undefined) {
      self.postMessage({ id, count: countSolutions(event.data.count) });
      return;
    }
    const result = JSON.parse(solve(event.data.solve));
    self.postMessage({ id, result, ms: Math.round(performance.now() - start) });
  } catch (e) {
    self.postMessage({ id, error: String((e && e.message) || e) });
  }
};
