package sudoku;

/**
 * A row of the sudoku: the cells with positions index*SIZE .. index*SIZE+SIZE-1.
 */
public class Row extends House{
    /**
     * @param size  side of the sudoku (9 for a classic sudoku)
     * @param index index of the row, from 0 (top) to size-1 (bottom)
     */
    public Row(int size,int index){
        setSize(size);
        setType();
        setIndex(index);
        initCells();
    }
    /**
     * Creates the (still unlinked) cells of the row.
     * The cell in column i of the row has position index*SIZE+i.
     * Sudoku.linkHouses() later replaces them with the real cells of the grid.
     */
    @Override
    public void initCells(){
        super.initCells();
        for(int i=0;i<getSize();i++)
            super.setCell(getIndex()*getSize()+i,i);
    }
    public void setType(){
        super.setType(HouseType.ROW);
    }
}
