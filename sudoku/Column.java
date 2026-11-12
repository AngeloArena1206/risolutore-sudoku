package sudoku;

/**
 * A column of the sudoku: the cells with positions index, index+SIZE, index+2*SIZE, ...
 */
public class Column extends House{
    /**
     * @param size  side of the sudoku (9 for a classic sudoku)
     * @param index index of the column, from 0 (left) to size-1 (right)
     */
    public Column(int size,int index){
        setSize(size);
        setType();
        setIndex(index);
        initCells();
    }
    /**
     * Creates the (still unlinked) cells of the column.
     * The cell in row i of the column has position index+i*SIZE.
     * Sudoku.linkHouses() later replaces them with the real cells of the grid.
     */
    @Override
    public void initCells(){
        super.initCells();
        for(int i=0;i<getSize();i++)
            super.setCell(getIndex()+i*getSize(),i);
    }
    public void setType(){
        super.setType(HouseType.COLUMN);
    }
}
