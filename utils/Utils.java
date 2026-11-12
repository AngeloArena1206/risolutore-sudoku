package utils;

import sudoku.*;

/**
 * Static helper methods used all over the solver:
 * <ul>
 *   <li>small array utilities (append, remove, truncate, search, comparison);</li>
 *   <li>geometry helpers (row / column / box of a position, house objects linked to a grid);</li>
 *   <li>helpers for the XY-Wing technique (pivot and pincers);</li>
 *   <li>two hard-coded sample puzzles.</li>
 * </ul>
 *
 * Terminology used in the whole project:
 * <ul>
 *   <li><b>grid</b>: the SIZE x SIZE matrix of cells, {@code Cell[row][column]} (SIZE is 9 in a classic sudoku);</li>
 *   <li><b>position</b>: index of a cell inside the grid, from 0 to SIZE*SIZE-1, equal to row*SIZE+column;</li>
 *   <li><b>house</b>: a row, a column or a box; every house must contain each digit exactly once;</li>
 *   <li><b>peers</b>: the cells that "see" a given cell, i.e. that share at least one house with it;</li>
 *   <li><b>candidate</b>: a digit that can still be placed in an empty cell.</li>
 * </ul>
 * Many methods work on arrays that are always rebuilt (never modified in place), because the same
 * array can be shared by several objects.
 */
public class Utils{

    // ------------------------------------------------------------------
    //  PEERS
    // ------------------------------------------------------------------

    /**
     * @return the positions of the cells that are seen by ALL the cells of {@code cells[]}
     *         (if {@code cells} contains a single cell, its peers)
     */
    public static int[] commonPeerPositions(int size,Cell cells[]){
        if(cells.length==0)
            return new int[0];
        if(cells.length==1)
            return cells[0].commonPeerPositions(cells[0]);//a cell compared with itself returns all its peers
        return cells[cells.length-1].commonPeerPositions(Utils.truncate(cells,cells.length-1));
    }
    /**
     * Same as {@link #commonPeerPositions(int, Cell[])} but returns the cells of {@code grid} instead of their positions.
     */
    public static Cell[] commonPeers(int size,Cell cells[],Cell grid[][]){
        int positions[]=Utils.commonPeerPositions(size,cells);
        Cell peers[]=new Cell[positions.length];
        for(int i=0;i<peers.length;i++)
            peers[i]=grid[positions[i]/size][positions[i]%size];
        return peers;
    }

    // ------------------------------------------------------------------
    //  CHAIN COLOURS
    // ------------------------------------------------------------------

    /**
     * Swaps the two colours used to mark the cells of a chain ('A' becomes 'B' and 'B' becomes 'A').
     * The swap is based on the parity of the character code ('A'=65 is odd, 'B'=66 is even).
     */
    public static char oppositeColor(char color){
        if((int)color%2==1)
            return (char)((int)color+1);
        else
            return (char)((int)color-1);
    }

    // ------------------------------------------------------------------
    //  COMPLEMENTS
    // ------------------------------------------------------------------

    /**
     * @return the digits from 1 to size that are NOT in {@code numbers[]}
     */
    public static int[] complementNumbers(int numbers[],int size){
        int complement[]=new int[size-numbers.length],count=0;
        for(int number=1;number<=size;number++){
            boolean found=false;//true if number is in numbers[]
            for(int i=0;i<numbers.length && found==false;i++)
                if(numbers[i]==number)
                    found=true;//number is in numbers[], so it must not be in the complement
            if(found==false)
                complement[count++]=number;
        }
        return complement;
    }
    /**
     * @return the indices from 0 to size-1 that are NOT in {@code indices[]}
     *         (used both for houses and for cells inside a house)
     */
    public static int[] complementIndices(int indices[],int size){
        int complement[]=new int[size-indices.length],count=0;
        for(int i=0;i<size;i++){
            boolean found=false;
            for(int j=0;j<indices.length && found==false;j++)
                if(i==indices[j])
                    found=true;
            if(found==false)
                complement[count++]=i;
        }
        return complement;
    }

    // ------------------------------------------------------------------
    //  OPERATIONS ON A BARE GRID
    //  (the static techniques receive only Cell[][]: these methods wrap it in a temporary Sudoku)
    // ------------------------------------------------------------------

    /**
     * Places {@code number} in cell (i,j) of {@code grid} and removes it from the candidates of its peers.
     * @return the cells from which {@code number} has been removed
     */
    public static Cell[] setNumber(int i,int j,int number,Cell grid[][]){
        Sudoku sudoku=new Sudoku(grid.length);
        sudoku.setCells(grid);//the temporary sudoku works directly on the cells of grid
        return sudoku.setNumber(i,j,number);
    }
    public static Cell[] setNumber(int position,int number,Cell grid[][]){
        return setNumber(position/grid.length,position%grid.length,number,grid);
    }
    public static Cell[] setNumber(Cell cell,int number,Cell grid[][]){
        return setNumber(cell.getRow(),cell.getColumn(),number,grid);
    }
    /**
     * @return for each house of {@code houses[]} (of type {@code type}) the candidate mask of {@code number}
     *         (see {@link Group#candidateMask(int)})
     */
    public static int[][] candidateMasks(int number,int houses[],HouseType type,Cell grid[][]){
        Sudoku sudoku=new Sudoku(grid.length);
        sudoku.setCells(grid);
        return sudoku.candidateMasks(number,houses,type);
    }

    // ------------------------------------------------------------------
    //  ARRAY UTILITIES
    // ------------------------------------------------------------------

    /** @return a new array made of {@code cells[]} followed by {@code cell} */
    public static Cell[] append(Cell cells[],Cell cell){
        Cell array[]=new Cell[1];
        array[0]=cell;
        return appendAll(cells,array);
    }
    /**
     * @return a new array made of {@code cells1[]} followed by {@code cells2[]};
     *         a null argument is treated as an empty array
     */
    public static Cell[] appendAll(Cell cells1[],Cell cells2[]){
        if(cells1==null && cells2==null)
            return new Cell[0];
        if(cells1==null)
            return cells2;
        if(cells2==null)
            return cells1;
        Cell result[]=new Cell[cells1.length+cells2.length];
        for(int i=0;i<cells1.length;i++)
            result[i]=cells1[i];
        for(int i=0;i<cells2.length;i++)
            result[cells1.length+i]=cells2[i];
        return result;
    }
    /** @return a new array made of {@code array[]} followed by {@code number} */
    public static int[] append(int array[],int number){
        int array1[]=new int[1];
        array1[0]=number;
        return appendAll(array,array1);
    }
    /** @return a new array made of {@code array1[]} followed by {@code array2[]} */
    public static int[] appendAll(int array1[],int array2[]){
        int result[]=new int[array1.length+array2.length];
        for(int i=0;i<array1.length;i++)
            result[i]=array1[i];
        for(int i=0;i<array2.length;i++)
            result[i+array1.length]=array2[i];
        return result;
    }
    /** @return a new array equal to {@code cells[]} without (every occurrence of) {@code cell} */
    public static Cell[] remove(Cell cells[],Cell cell){
        Cell array[]=new Cell[1];
        array[0]=cell;
        return removeAll(cells,array);
    }
    /** @return a new array equal to {@code cells1[]} without the elements of {@code cells2[]} */
    public static Cell[] removeAll(Cell cells1[],Cell cells2[]){
        for(int i=0;i<cells2.length;i++)
            cells1=removeAt(cells1,indicesOf(cells1,cells2[i]));
        return cells1;
    }
    /** @return a new array equal to {@code array[]} without (every occurrence of) {@code number} */
    public static int[] remove(int array[],int number){
        int array1[]=new int[1];
        array1[0]=number;
        return removeAll(array,array1);
    }
    /** @return a new array equal to {@code array1[]} without the elements of {@code array2[]} */
    public static int[] removeAll(int array1[],int array2[]){
        for(int i=0;i<array2.length;i++)
            array1=removeAt(array1,indicesOf(array1,array2[i]));
        return array1;
    }
    /** @return the first {@code length} elements of {@code array[]} (an empty array if length is 0) */
    public static int[] truncate(int array[],int length){
        int result[]=new int[length];
        for(int i=0;i<length;i++)
            result[i]=array[i];
        return result;
    }
    /** @return the first {@code length} elements of {@code array[]} (an empty array if length is 0) */
    public static Cell[] truncate(Cell array[],int length){
        Cell result[]=new Cell[length];
        for(int i=0;i<length;i++)
            result[i]=array[i];
        return result;
    }
    /** @return true if {@code element} is in {@code array[]} */
    public static boolean contains(int array[],int element){
        for(int i=0;i<array.length;i++)
            if(array[i]==element)
                return true;
        return false;
    }
    /** @return true if {@code cell} is in {@code array[]} (false if the array is null) */
    public static boolean contains(Cell array[],Cell cell){
        if(array==null)
            return false;
        for(int i=0;i<array.length;i++)
            if(array[i]==cell)
                return true;
        return false;
    }
    /** @return the index of the first occurrence of {@code element} in {@code array[]}, or -1 */
    public static int indexOf(int array[],int element){
        for(int i=0;i<array.length;i++)
            if(array[i]==element)
                return i;
        return -1;
    }
    /** @return the index of the first occurrence of {@code element} in {@code array[]}, or -1 */
    public static int indexOf(Cell array[],Cell element){
        for(int i=0;i<array.length;i++)
            if(array[i]==element)
                return i;
        return -1;
    }
    /** @return the indices of ALL the occurrences of {@code element} in {@code array[]} (an empty array if none) */
    public static int[] indicesOf(int array[],int element){
        int indices[]=new int[array.length],count=0;
        for(int i=0;i<array.length;i++)
            if(array[i]==element)
                indices[count++]=i;
        return truncate(indices,count);
    }
    /** @return the indices of ALL the occurrences of {@code element} in {@code array[]} (an empty array if none) */
    public static int[] indicesOf(Cell array[],Cell element){
        int indices[]=new int[array.length];
        int count=0;
        for(int i=0;i<array.length;i++)
            if(array[i]==element)
                indices[count++]=i;
        return truncate(indices,count);
    }
    /** @return a new array equal to {@code array[]} without the element at position {@code index} */
    public static int[] removeAt(int array[],int index){
        int indices[]=new int[1];
        indices[0]=index;
        return removeAt(array,indices);
    }
    /** @return a new array equal to {@code array[]} without the elements at the positions in {@code indices[]} */
    public static int[] removeAt(int array[],int indices[]){
        int result[]=new int[array.length-indices.length],count=0;
        for(int i=0;i<array.length;i++)
            if(contains(indices,i)==false)//array[i] is kept only if i is not one of the indices to remove
                result[count++]=array[i];
        return result;
    }
    /** @return a new array equal to {@code array[]} without the element at position {@code index} */
    public static Cell[] removeAt(Cell array[],int index){
        int indices[]=new int[1];
        indices[0]=index;
        return removeAt(array,indices);
    }
    /** @return a new array equal to {@code array[]} without the elements at the positions in {@code indices[]} */
    public static Cell[] removeAt(Cell array[],int indices[]){
        Cell result[]=new Cell[array.length-indices.length];
        int count=0;
        for(int i=0;i<array.length;i++)
            if(contains(indices,i)==false)
                result[count++]=array[i];
        return result;
    }
    /**
     * @return true if the two arrays contain the same elements, in any order
     *         ({@code Arrays.equals} would also compare the order)
     */
    public static boolean sameElements(int array1[],int array2[]){
        if(array1.length!=array2.length)
            return false;
        //look for the last element of array1 in array2: if it is there, remove it from both and compare the rest
        for(int i=0;i<array1.length;i++)
            if(array1[array1.length-1]==array2[i]){
                if(array1.length==1)
                    return true;
                else
                    return sameElements(truncate(array1,array1.length-1),removeAt(array2,i));
            }
        return false;
    }
    /** @return true if the two arrays contain the same cells, in any order */
    public static boolean sameElements(Cell array1[],Cell array2[]){
        if(array1.length!=array2.length)
            return false;
        for(int i=0;i<array1.length;i++)
            if(array1[array1.length-1]==array2[i]){
                if(array1.length==1)
                    return true;
                else
                    return sameElements(truncate(array1,array1.length-1),removeAt(array2,i));
            }
        return false;
    }

    // ------------------------------------------------------------------
    //  GEOMETRY
    // ------------------------------------------------------------------

    /** @return the row {@code index} made of the cells of {@code grid} */
    public static Row getRow(int index,Cell grid[][]){
        Row row=new Row(grid.length,index);
        for(int i=0;i<grid.length;i++)
            row.setCell(grid[index][i],i);
        return row;
    }
    /** @return the column {@code index} made of the cells of {@code grid} */
    public static Column getColumn(int index,Cell grid[][]){
        Column column=new Column(grid.length,index);
        for(int i=0;i<grid.length;i++)
            column.setCell(grid[i][index],i);
        return column;
    }
    /** @return the box {@code index} made of the cells of {@code grid} */
    public static Box getBox(int index,Cell grid[][]){
        Box box=new Box(grid.length,index);//its cells are placeholders that tell where the real cells are
        for(int i=0;i<grid.length;i++)
            box.setCell(grid[box.getCell(i).getRow()][box.getCell(i).getColumn()],i);
        return box;
    }
    /** @return the row of the cell in {@code position} */
    public static int rowOf(int position,int size){
        return position/size;
    }
    /** @return the column of the cell in {@code position} */
    public static int columnOf(int position,int size){
        return position%size;
    }
    /** @return the box of the cell in {@code position} */
    public static int boxOf(int position,int size){
        int boxSize=(int)Math.sqrt(size);
        return (rowOf(position,size)/boxSize)*boxSize+columnOf(position,size)/boxSize;
    }
    public static int rowOf(Cell cell,int size){
        return rowOf(cell.getPosition(),size);
    }
    public static int columnOf(Cell cell,int size){
        return columnOf(cell.getPosition(),size);
    }
    public static int boxOf(Cell cell,int size){
        return boxOf(cell.getPosition(),size);
    }

    // ------------------------------------------------------------------
    //  XY-WING HELPERS
    //  An XY-Wing is made of a pivot {x,y} and two pincers {x,z} and {y,z} that see the pivot.
    // ------------------------------------------------------------------

    /**
     * {@code cell1} and {@code cell2} are two bivalue cells sharing exactly one candidate.
     * @return the two candidates to look for in a third cell to build an XY-Wing with cell1 and cell2
     *         (the candidate of cell1 and the candidate of cell2 that are not shared);
     *         an empty array if the two cells share no candidate or have the same candidates
     */
    public static int[] pincerCandidates(Cell cell1,Cell cell2){
        int shared=sharedCandidate(cell1,cell2);
        if(shared==-1)
            return new int[0];
        int result[]=new int[2];
        if(cell1.getCandidates()[0]==shared)
            result[0]=cell1.getCandidates()[1];
        else
            result[0]=cell1.getCandidates()[0];
        if(cell2.getCandidates()[0]==shared)
            result[1]=cell2.getCandidates()[1];
        else
            result[1]=cell2.getCandidates()[0];
        return result;
    }
    /**
     * @return the candidate shared by the two bivalue cells, or -1 if they share no candidate
     *         or if they have exactly the same candidates
     */
    public static int sharedCandidate(Cell cell1,Cell cell2){
        if(Utils.sameElements(cell1.getCandidates(),cell2.getCandidates()))
            return -1;
        if(cell1.getCandidates()[0]!=cell2.getCandidates()[0] && cell1.getCandidates()[0]!=cell2.getCandidates()[1] && cell1.getCandidates()[1]!=cell2.getCandidates()[0] && cell1.getCandidates()[1]!=cell2.getCandidates()[1])
            return -1;
        if(cell1.getCandidates()[0]==cell2.getCandidates()[0] || cell1.getCandidates()[0]==cell2.getCandidates()[1])
            return cell1.getCandidates()[0];
        if(cell1.getCandidates()[1]==cell2.getCandidates()[0] || cell1.getCandidates()[1]==cell2.getCandidates()[1])
            return cell1.getCandidates()[1];
        return 0;//never reached
    }

    // ------------------------------------------------------------------
    //  SAMPLE PUZZLES
    // ------------------------------------------------------------------

    /**
     * @param number 1 or 2
     * @return one of the two hard-coded sample sudokus
     */
    public static Sudoku createSample(int number){
        Sudoku sudoku;
        switch(number){
            case 1:
                sudoku=new Sudoku(9);
                sudoku.setNumber(0,0,2);
                sudoku.setNumber(4,0,5);
                sudoku.setNumber(5,0,1);
                sudoku.setNumber(7,0,7);

                sudoku.setNumber(1,1,3);
                sudoku.setNumber(1,4,2);
                sudoku.setNumber(1,8,1);
                sudoku.setNumber(2,2,5);
                sudoku.setNumber(2,3,3);
                sudoku.setNumber(3,2,6);
                sudoku.setNumber(3,5,3);
                sudoku.setNumber(3,8,4);

                sudoku.setNumber(4,0,8);
                sudoku.setNumber(4,1,2);
                sudoku.setNumber(4,7,1);
                sudoku.setNumber(4,8,6);
                sudoku.setNumber(5,0,4);
                sudoku.setNumber(5,3,6);
                sudoku.setNumber(5,6,3);
                sudoku.setNumber(6,5,2);
                sudoku.setNumber(6,6,4);

                sudoku.setNumber(7,0,6);
                sudoku.setNumber(7,4,4);
                sudoku.setNumber(7,7,3);

                sudoku.setNumber(8,1,1);
                sudoku.setNumber(8,3,9);
                sudoku.setNumber(8,4,6);
                sudoku.setNumber(8,8,2);
                return sudoku;
            case 2:
                sudoku=new Sudoku(9);

                sudoku.setNumber(0,4,1);
                sudoku.setNumber(0,6,7);
                sudoku.setNumber(0,7,5);

                sudoku.setNumber(1,0,7);
                sudoku.setNumber(1,1,4);
                sudoku.setNumber(1,2,8);
                sudoku.setNumber(1,3,6);
                sudoku.setNumber(1,6,3);
                sudoku.setNumber(1,8,1);

                sudoku.setNumber(2,0,3);
                sudoku.setNumber(2,4,9);
                sudoku.setNumber(2,5,4);

                sudoku.setNumber(3,0,1);
                sudoku.setNumber(3,2,4);
                sudoku.setNumber(3,5,6);
                sudoku.setNumber(3,6,5);
                sudoku.setNumber(3,8,7);

                sudoku.setNumber(4,0,5);
                sudoku.setNumber(4,1,9);
                sudoku.setNumber(4,4,7);
                sudoku.setNumber(4,7,3);

                sudoku.setNumber(5,4,4);
                sudoku.setNumber(5,5,8);

                sudoku.setNumber(6,0,4);
                sudoku.setNumber(6,1,3);
                sudoku.setNumber(6,3,1);
                sudoku.setNumber(6,8,2);

                sudoku.setNumber(7,0,9);
                sudoku.setNumber(7,2,2);
                sudoku.setNumber(7,6,6);
                sudoku.setNumber(7,7,4);

                sudoku.setNumber(8,2,7);
                sudoku.setNumber(8,4,5);
                sudoku.setNumber(8,6,9);
                sudoku.setNumber(8,7,1);
                return sudoku;
            default:
                throw new RuntimeException("Unknown sample sudoku\nnumber: "+number);
        }
    }
}
