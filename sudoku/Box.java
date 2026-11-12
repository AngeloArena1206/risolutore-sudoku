package sudoku;

/**
 * A box of the sudoku: one of the boxSize x boxSize squares (3x3 in a classic sudoku).
 * Boxes are numbered from left to right and from top to bottom:
 * <pre>
 *  0 1 2
 *  3 4 5
 *  6 7 8
 * </pre>
 * and the cells inside a box are numbered in the same way.
 */
public class Box extends House{
    /**
     * @param size  side of the sudoku (9 for a classic sudoku)
     * @param index index of the box, from 0 (top left) to size-1 (bottom right)
     */
    public Box(int size,int index){
        setSize(size);
        setType();
        setIndex(index);
        initCells();
    }
    /**
     * Creates the (still unlinked) cells of the box.
     * Sudoku.linkHouses() later replaces them with the real cells of the grid.
     */
    @Override
    public void initCells(){
        super.initCells();
        //position of the top left cell of the box
        int topLeft=(getIndex()-getIndex()%getBoxSize())*getSize()+(getIndex()%getBoxSize())*getBoxSize();
        for(int i=0;i<getBoxSize();i++)//row inside the box
            for(int j=0;j<getBoxSize();j++)//column inside the box
                super.setCell(topLeft+i*getSize()+j,i*getBoxSize()+j);
    }
    public void setType(){
        super.setType(HouseType.BOX);
    }
}
