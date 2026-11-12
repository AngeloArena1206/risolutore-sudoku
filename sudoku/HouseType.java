package sudoku;

/**
 * The three kinds of "house" (also called "unit") of a sudoku.
 * Every house contains SIZE cells and must contain every digit from 1 to SIZE exactly once.
 */
public enum HouseType {
    ROW,     // horizontal line of cells
    COLUMN,  // vertical line of cells
    BOX;     // square block of boxSize x boxSize cells (3x3 in a classic sudoku)

    /**
     * @return the lower case name of the house type, used in the log messages (e.g. "row")
     */
    public String label() {
        return name().toLowerCase();
    }
}
