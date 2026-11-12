package sudoku;

import utils.Utils;

import java.util.Scanner;

/**
 * A sudoku of side SIZE (9 for a classic sudoku, in general a perfect square) and its solver.
 *
 * <h2>How the solver works</h2>
 * {@link #solve()} tries the techniques in order of difficulty. As soon as a technique makes progress
 * (places a digit or removes a candidate) the solver starts again from the first, simplest technique.
 * The order is:
 * <ol>
 *   <li>Hidden Single</li>
 *   <li>Naked Single</li>
 *   <li>Locked Candidates (Pointing / Claiming)</li>
 *   <li>Naked and Hidden Subsets (pairs, triples, quads)</li>
 *   <li>X-Wing / Swordfish / Jellyfish</li>
 *   <li>Unique Rectangle (types 1, 2, 4)</li>
 *   <li>Remote Pairs</li>
 *   <li>Skyscraper</li>
 *   <li>Two-String Kite</li>
 *   <li>Finned / Sashimi X-Wing and Swordfish</li>
 *   <li>Simple Colouring</li>
 *   <li>W-Wing</li>
 *   <li>XY-Wing</li>
 *   <li>XYZ-Wing</li>
 *   <li>Multi-Coloring</li>
 *   <li>X-Cycles</li>
 *   <li>XY-Chain</li>
 *   <li>AIC (Alternating Inference Chains)</li>
 *   <li>ALS-XZ</li>
 *   <li>Forcing Chains (assumptions propagated with singles only)</li>
 *   <li>Forcing Net on bivalue cells (assumptions propagated with every technique, second-to-last resort)</li>
 *   <li>Trial and Error (last resort)</li>
 * </ol>
 * The order follows the usual difficulty ratings of the techniques (e.g. the scores used by HoDoKu).
 * The advanced pattern techniques are in {@link Patterns}, the chain techniques in {@link Chains}.
 *
 * <h2>Post-techniques</h2>
 * When a technique removes some candidates, the corresponding static {@code postXxx} method re-applies the
 * simpler techniques only around the cells that changed (e.g. after an X-Wing removes digit 5 from some cells,
 * the houses of those cells are checked again for hidden singles, locked candidates and subsets of 5).
 * Each post-technique calls the post-technique of the previous technique, so they form a cascade:
 * postHiddenSingle &lt; postLockedCandidates &lt; postSubsets &lt; postXWing &lt; postXyWing &lt; postXyChain &lt; postForcingNet.
 * The post-techniques of the advanced techniques plug into this cascade.
 */
public class Sudoku {
    /**
     * Reads a sudoku from the keyboard (0 for an empty cell), solves it and prints it.
     */
    public static void main(String args[]) {
        /*Sudoku sudoku=Utils.createSample(2);
        sudoku.print();
        Scanner keyboard=new Scanner(System.in);
        keyboard.nextInt();
        sudoku.solve();
        sudoku.print();*/

        Sudoku sudoku1=new Sudoku(4);
        sudoku1.read();
        sudoku1.solve();
        sudoku1.print();
    }
    private int size;       //side of the sudoku (9 in a classic sudoku)
    private int boxSize;    //side of a box (3 in a classic sudoku)
    private Cell cells[][]; //the grid: cells[row][column]
    private Row rows[];
    private Column columns[];
    private Box boxes[];
    private boolean logicOnly=false;//true in the copies created by forcingNet(): solve() then uses only the logical techniques
    //true once the uniqueness of the solution has been checked. The copies made by copy() start with true: they contain
    //an assumption that may be wrong, and must be solved logically even when they have no solution
    private boolean uniquenessChecked=false;

    public void setSize(int size) {
        this.size = size;
        boxSize = (int) Math.sqrt(size);
    }
    public int getSize() {
        return size;
    }
    public int getBoxSize() {
        return boxSize;
    }
    /**
     * Fills the grid with new empty cells.
     */
    public void initCells() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++)
                cells[i][j] = new Cell(i * size + j, size);
        }
    }
    /**
     * Makes this sudoku work directly on the cells of {@code cells} (no copy) and links the houses to them.
     */
    public void setCells(Cell cells[][]){
        this.cells=cells;
        linkHouses();
    }
    /** @return the grid of cells (used by the advanced techniques to call the post-techniques) */
    Cell[][] getGrid() {
        return cells;
    }
    public Cell getCell(int position) {
        return cells[position / size][position % size];
    }
    /**
     * Makes rows, columns and boxes point to the cells of the grid
     * (otherwise each house would have its own unlinked copies of the cells).
     */
    public void linkHouses() {
        for(int i=0;i<size;i++){
            rows[i].setCells(cells[i]);
            Cell columnCells[]=new Cell[size];
            for(int j=0;j<size;j++)
                columnCells[j]=cells[j][i];
            columns[i].setCells(columnCells);
            Box box=new Box(size,i);//its cells are placeholders that tell where the real cells are
            Cell boxCells[]=new Cell[size];
            for(int j=0;j<size;j++)
                boxCells[j]=cells[box.getCell(j).getRow()][box.getCell(j).getColumn()];
            boxes[i].setCells(boxCells);
        }
    }
    public Row[] getRows() {
        return rows;
    }
    public Row getRow(int index) {
        return rows[index];
    }
    public Column[] getColumns() {
        return columns;
    }
    public Column getColumn(int index) {
        return columns[index];
    }
    public Box[] getBoxes() {
        return boxes;
    }
    public Box getBox(int index) {
        return boxes[index];
    }
    /**
     * @return the index of the box containing cell (i,j)
     */
    public int boxIndex(int i, int j) {
        return (i / boxSize) * boxSize + j / boxSize;
    }
    /**
     * Creates an empty sudoku of side {@code size}.
     */
    public Sudoku(int size) {
        setSize(size);
        cells = new Cell[size][size];
        rows = new Row[size];
        columns = new Column[size];
        boxes = new Box[size];
        for (int i = 0; i < size; i++) {
            rows[i] = new Row(size, i);
            columns[i] = new Column(size, i);
            boxes[i] = new Box(size, i);
        }
        initCells();
        linkHouses();
    }

    // ------------------------------------------------------------------
    //  INPUT / OUTPUT
    // ------------------------------------------------------------------

    /**
     * Prints the grid ('-' for an empty cell), with a blank between boxes.
     */
    public void print() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(cells[i][j].getSymbol());
                if (j % boxSize == boxSize - 1)
                    System.out.print(" ");
            }
            System.out.print("\n");
            if (i % boxSize == boxSize - 1)
                System.out.print("\n");
        }
    }
    /**
     * Reads the sudoku from the keyboard: SIZE*SIZE integers, row by row, 0 for an empty cell.
     */
    public void read() {
        System.out.println("Enter the sudoku:\n");
        Scanner keyboard = new Scanner(System.in);
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                int number = keyboard.nextInt();
                if (number >= 0 && number <= size)//0 means empty cell
                    setNumber(i, j, number);
                else {
                    System.out.println("the number " + number + " is not between 1 and " + size + ".\nenter it again: ");
                    j--;//also works when j=0
                }
            }
        keyboard.close();
    }
    /**
     * Prints the candidates of every cell: each cell is shown as a boxSize x boxSize square of digits.
     */
    public void printCandidates() {
        for (int i = 0; i < boxSize * size; i++) {
            for (int j = 0; j < size * boxSize; j++) {
                //(i/boxSize, j/boxSize) is the cell, (i%boxSize)*boxSize+j%boxSize is the index of the candidate to print
                if ((i % boxSize) * boxSize + j % boxSize < cells[i / boxSize][j / boxSize].getCandidates().length)
                    System.out.print(cells[i / boxSize][j / boxSize].getCandidates()[(i % boxSize) * boxSize + j % boxSize]);
                else
                    System.out.print(" ");
                if (j % boxSize == boxSize - 1)
                    System.out.print(" ");
                if (j % size == size - 1)
                    System.out.print(" ");
            }
            System.out.println("");
            if (i % boxSize == boxSize - 1)
                System.out.println("");
            if (i % size == size - 1)
                System.out.println("");
        }
    }

    // ------------------------------------------------------------------
    //  BASIC OPERATIONS AND QUERIES
    // ------------------------------------------------------------------

    /**
     * Places {@code number} in cell (i,j) and removes it from the candidates of all the peers of the cell.
     * With number==0 the cell is emptied.
     * @return the cells from which {@code number} has been removed (an empty array if none)
     */
    public Cell[] setNumber(int i, int j, int number) {
        Cell changed[]=new Cell[3*size];
        int count=0;
        if (number != 0) {
            int peers[] = cells[i][j].commonPeerPositions(cells[i][j]);//all the peers of the cell
            for (int k = 0; k < peers.length; k++)
                if(cells[peers[k] / size][peers[k] % size].removeCandidate(number))
                    changed[count++]=cells[peers[k] / size][peers[k] % size];
        }
        cells[i][j].setNumber(number);
        if (number != 0)
            print();
        return Utils.truncate(changed,count);
    }
    public Cell[] setNumber(Cell cell,int number){
        return setNumber(cell.getRow(),cell.getColumn(),number);
    }
    /**
     * @return for each house {@code houses[k]} of type {@code type}, the candidate mask of {@code number}
     *         (see {@link Group#candidateMask(int)})
     */
    public int[][] candidateMasks(int number,int houses[],HouseType type){
        int masks[][]=new int[houses.length][size];
        Group all[];
        switch(type){
            case ROW:    all=rows;    break;
            case COLUMN: all=columns; break;
            default:     all=boxes;   break;
        }
        for(int i=0;i<houses.length;i++)
            masks[i]=all[houses[i]].candidateMask(number);
        return masks;
    }
    /**
     * @return an array with, in [0], the indices of the rows where {@code number} is not yet placed,
     *         in [1] the same for the columns and in [2] for the boxes
     */
    public int[][] housesMissing(int number) {
        int result[][] = new int[3][0];
        result[0]=rowsMissing(number);
        result[1]=columnsMissing(number);
        result[2]=boxesMissing(number);
        return result;
    }
    /**
     * Same as {@link #housesMissing(int)}, but only the houses where {@code number} can go in at most
     * {@code maxCount} cells are returned.
     */
    public int[][] housesMissing(int number,int maxCount){
        int result[][]=new int[3][0];
        result[0]=rowsMissing(number,maxCount);
        result[1]=columnsMissing(number,maxCount);
        result[2]=boxesMissing(number,maxCount);
        return result;
    }
    /**
     * Same as {@link #housesMissing(int)}, but only the houses where {@code number} can go in exactly
     * {@code count} cells are returned.
     */
    public int[][] housesWithExactly(int number,int count){
        int result[][]=new int[3][0];
        result[0]=rowsWithExactly(number, count);
        result[1]=columnsWithExactly(number,count);
        result[2]=boxesWithExactly(number,count);
        return result;
    }
    /** @return the indices of the rows where {@code number} is not yet placed */
    public int[] rowsMissing(int number){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(rows[i].hasNumber(number)==false)
                result[count++]=i;
        return Utils.truncate(result, count);
    }
    /** @return the indices of the rows where {@code number} is not yet placed and can go in at most maxCount cells */
    public int[] rowsMissing(int number,int maxCount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(rows[i].hasNumber(number)==false && rows[i].countCandidates(number)<=maxCount)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /** @return the indices of the rows where {@code number} can go in exactly {@code amount} cells */
    public int[] rowsWithExactly(int number,int amount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(rows[i].countCandidates(number)==amount)
                result[count++]=i;
        System.out.println("There are "+count+" rows where digit "+number+" can go in "+amount+" cells");
        return Utils.truncate(result, count);
    }
    /** @return the indices of the columns where {@code number} is not yet placed */
    public int[] columnsMissing(int number){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(columns[i].hasNumber(number)==false)
                result[count++]=i;
        return Utils.truncate(result, count);
    }
    /** @return the indices of the columns where {@code number} is not yet placed and can go in at most maxCount cells */
    public int[] columnsMissing(int number,int maxCount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(columns[i].hasNumber(number)==false && columns[i].countCandidates(number)<=maxCount)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /** @return the indices of the columns where {@code number} can go in exactly {@code amount} cells */
    public int[] columnsWithExactly(int number,int amount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(columns[i].countCandidates(number)==amount)
                result[count++]=i;
        return Utils.truncate(result, count);
    }
    /** @return the indices of the boxes where {@code number} is not yet placed */
    public int[] boxesMissing(int number){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(boxes[i].hasNumber(number)==false)
                result[count++]=i;
        return Utils.truncate(result, count);
    }
    /** @return the indices of the boxes where {@code number} is not yet placed and can go in at most maxCount cells */
    public int[] boxesMissing(int number,int maxCount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(boxes[i].hasNumber(number)==false && boxes[i].countCandidates(number)<=maxCount)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /** @return the indices of the boxes where {@code number} can go in exactly {@code amount} cells */
    public int[] boxesWithExactly(int number,int amount){
        int result[]=new int[size],count=0;
        for(int i=0;i<size;i++)
            if(boxes[i].countCandidates(number)==amount)
                result[count++]=i;
        return Utils.truncate(result, count);
    }
    /**
     * @return the cells of this sudoku seen by all the cells of {@code cells[]}
     */
    public Cell[] commonPeers(Cell cells[]){
        int positions[]=Utils.commonPeerPositions(size, cells);
        Cell result[]=new Cell[positions.length];
        for(int i=0;i<result.length;i++)
            result[i]=this.cells[positions[i]/size][positions[i]%size];
        return result;
    }
    /**
     * @return true if every cell is filled
     */
    public boolean isSolved(){
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++)
                if(cells[i][j].getSymbol()=='-')
                    return false;
        return true;
    }

    // ------------------------------------------------------------------
    //  SOLVER
    // ------------------------------------------------------------------

    /**
     * Solves the sudoku: the techniques are tried from the simplest to the hardest and, as soon as one of them
     * makes progress, the search starts again from the simplest one (see {@link #applyNextTechnique()}).
     * Before the first technique, the sudoku is checked with a fast backtracking ({@link SolutionCounter}):
     * if it has no solution or more than one solution the user is told and nothing else is done.
     * The steps are repeated in a loop (not with recursion), so even a very long solution does not grow the stack:
     * this matters in the web version, where the stack of the browser is small.
     * @return true if the sudoku has been solved
     */
    public boolean solve(){
        if(uniquenessChecked==false){
            uniquenessChecked=true;//the check is done only once
            int solutions=new SolutionCounter(this).countSolutions(2);
            if(solutions==0){
                System.out.println("The sudoku has no solution: it cannot be solved");
                return false;
            }
            if(solutions>1){
                System.out.println("The sudoku has more than one solution: a proper sudoku must have exactly one, so it is not solved");
                return false;
            }
            System.out.println("The sudoku has exactly one solution: starting the logical solution");
        }
        while(true){
            if(hasContradiction()){//happens only in the copies made by trialAndError()/forcingNet() with a wrong digit (or if the input is invalid)
                System.out.println("The sudoku has no solution");
                return false;
            }
            if(applyNextTechnique()==false){//no technique makes progress any more
                if(isSolved()==false)
                    System.out.println("The available techniques are not enough to solve the sudoku");
                return false;
            }
            if(isSolved() && hasContradiction()==false){
                System.out.println("Sudoku solved");
                return true;
            }
        }
    }
    /**
     * Tries the techniques in increasing order of difficulty and stops at the first one that makes progress.
     * @return true if a technique has placed a digit or removed a candidate
     */
    private boolean applyNextTechnique(){
        if(hiddenSingle())
            return true;
        if(nakedSingle())
            return true;
        if(lockedCandidates())
            return true;
        if(subsets())
            return true;
        if(xWing())//X-Wing, Swordfish, Jellyfish
            return true;
        if(new Patterns(this).uniqueRectangle())
            return true;
        if(remotePairs())//must come before simple colouring, which would make it useless
            return true;
        if(skyscraper())
            return true;
        if(new Patterns(this).twoStringKite())
            return true;
        if(new Patterns(this).finnedFish())
            return true;
        if(simpleColoring())
            return true;
        if(new Patterns(this).wWing())
            return true;
        if(xyWing())
            return true;
        if(new Patterns(this).xyzWing())
            return true;
        if(new Patterns(this).multiColoring())
            return true;
        if(new Chains(this).xCycles())
            return true;
        if(xyChain())//generalisation of the XY-Wing
            return true;
        if(new Chains(this).aic())
            return true;
        if(new Chains(this).alsXz())
            return true;
        if(new Chains(this).forcingChains())
            return true;
        if(logicOnly==false && forcingNet())//second-to-last resort; the copies created by forcingNet() use only logic
            return true;
        if(logicOnly==false && isSolved()==false && trialAndError())//last resort
            return true;
        return false;
    }

    // ------------------------------------------------------------------
    //  POST-TECHNIQUES
    //  After a technique has removed candidates from some cells, these methods re-apply the simpler
    //  techniques only around those cells. They are static because they are called from Cell and House,
    //  which know only the grid.
    // ------------------------------------------------------------------

    /**
     * After a Hidden Single (or any change) on {@code cells[]}: for each changed cell try Naked Single on the cell
     * and Hidden Single of {@code number} in its box, row and column.
     */
    public static void postHiddenSingle(int number,Cell cells[],Cell grid[][]){
        for(int i=0;i<cells.length;i++)//nothing happens if cells is empty
            postHiddenSingle(number,cells[i],grid);
    }
    public static void postHiddenSingle(int number,Cell cell,Cell grid[][]){
        if(cell==null){
            System.out.println("Error: NullPointerException in Sudoku.postHiddenSingle()");
            return;
        }
        cell.nakedSingle(grid);
        Utils.getBox(cell.getBox(),grid).hiddenSingle(number,grid);
        Utils.getRow(cell.getRow(),grid).hiddenSingle(number,grid);
        Utils.getColumn(cell.getColumn(),grid).hiddenSingle(number,grid);
    }
    /**
     * Same as above for every digit: some candidates have been removed from the cells of {@code cells[]}.
     */
    public static void postHiddenSingle(Cell cells[],Cell grid[][]){
        for(int number=1;number<=grid.length;number++)
            postHiddenSingle(number,cells,grid);
    }
    /**
     * After Locked Candidates: Hidden Single, then Locked Candidates again around the changed cells, for every digit.
     */
    public static void postLockedCandidates(Cell cells[],Cell grid[][]){
        postHiddenSingle(cells,grid);
        for(int number=1;number<=grid.length;number++)
            postLockedCandidates(number,cells,grid);
    }
    public static void postLockedCandidates(int number,Cell cells[],Cell grid[][]){
        postHiddenSingle(number,cells,grid);//naked single on the cell and hidden single in its houses
        for(int i=0;i<cells.length;i++)
            postLockedCandidates(number,cells[i],grid);
    }
    public static void postLockedCandidates(int number,Cell cell,Cell grid[][]){
        if(cell==null){
            System.out.println("Error: NullPointerException in postLockedCandidates()");
            return;
        }
        postHiddenSingle(number,cell,grid);
        Utils.getBox(cell.getBox(),grid).lockedCandidates(number,grid);
        Utils.getRow(cell.getRow(),grid).lockedCandidates(number,grid);
        Utils.getColumn(cell.getColumn(),grid).lockedCandidates(number,grid);
    }
    /**
     * After Remote Pairs: the X-Wing post-technique for both digits of the pair.
     */
    public static void postRemotePairs(int numbers[],Cell cells[],Cell grid[][]){
        for(int i=0;i<numbers.length;i++)
            postXWing(numbers[i],cells,grid);
    }
    /**
     * After Subsets: the previous post-techniques, then Subsets again in the houses of the changed cells.
     */
    public static void postSubsets(Cell cells[],Cell grid[][]){
        System.out.println("Entering postSubsets on cells: ");
        for(int i=0;i<cells.length;i++)
            System.out.print(cells[i].getPosition()+" ");
        System.out.println("");
        postLockedCandidates(cells,grid);
        for(int number=1;number<=grid.length;number++)
            postSubsets(number,cells,grid);
    }
    public static void postSubsets(int number,Cell cells[],Cell grid[][]){
        postLockedCandidates(number,cells,grid);
        for(int i=0;i<cells.length;i++)
            postSubsets(number,cells[i],grid);
    }
    public static void postSubsets(int numbers[],Cell cell,Cell grid[][]){
        for(int i=0;i<numbers.length;i++)
            postSubsets(numbers[i],cell,grid);
    }
    public static void postSubsets(int numbers[],Cell cells[],Cell grid[][]){
        for(int i=0;i<numbers.length;i++)
            postSubsets(numbers[i],cells,grid);
    }
    public static void postSubsets(int number,Cell cell,Cell grid[][]){
        if(cell==null){
            System.out.println("Error: NullPointerException in Sudoku.postSubsets");
            return;
        }
        postLockedCandidates(number,cell,grid);
        Utils.getBox(cell.getBox(),grid).subsets(grid);
        Utils.getRow(cell.getRow(),grid).subsets(grid);
        Utils.getColumn(cell.getColumn(),grid).subsets(grid);
    }
    /**
     * After X-Wing: the previous post-techniques, then X-Wing of the same digit starting from the row and the
     * column of every changed cell.
     */
    public static void postXWing(Cell cells[],Cell grid[][]){
        for(int number=1;number<=grid.length;number++)
            postXWing(number,cells,grid);
    }
    public static void postXWing(int number,Cell cells[],Cell grid[][]){
        for(int i=0;i<cells.length;i++)
            postXWing(number,cells[i],grid);
    }
    public static void postXWing(int number,Cell cell,Cell grid[][]){
        postSubsets(number,cell,grid);
        if(cell==null){
            System.out.println("Error: NullPointerException in postXWing()");
            return;
        }
        Utils.getRow(cell.getRow(),grid).xWing(number,grid);
        Utils.getColumn(cell.getColumn(),grid).xWing(number,grid);
    }
    /**
     * After XY-Wing: the X-Wing post-technique, then XY-Wing again using as pivot every changed cell
     * that is left with two candidates.
     */
    public static void postXyWing(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
        for(int i=0;i<cells.length;i++)//a cell left with two candidates can be the pivot of a new XY-Wing
            if(cells[i]!=null && cells[i].getNumber()==0 && cells[i].getCandidates().length==2)
                cells[i].xyWing(grid);
    }
    /**
     * After XY-Chain: the XY-Wing post-technique, then XY-Chain again starting from every changed cell
     * that is left with two candidates (recursively, as long as something changes).
     */
    public static void postXyChain(int number,Cell cells[],Cell grid[][]){
        postXyWing(number,cells,grid);
        for(int i=0;i<cells.length;i++)
            if(cells[i]!=null && cells[i].getNumber()==0 && cells[i].getCandidates().length==2){
                Group chain=cells[i].buildXyChain(grid);
                boolean applied=chain.xyChain(grid);
                Cell modifiedCells[]=chain.getModifiedCells();
                int removedNumber=chain.getRemovedNumber();
                chain.dissolve();//the chain must be dissolved before calling the post-technique (which builds other chains)
                if(applied)
                    postXyChain(removedNumber,modifiedCells,grid);
            }
    }
    /**
     * After the Forcing Net: the XY-Chain post-technique (which includes all the previous ones).
     */
    public static void postForcingNet(int number,Cell cells[],Cell grid[][]){
        postXyChain(number,cells,grid);
    }
    /**
     * After Forcing Chains: the XY-Chain post-technique (which includes all the previous ones).
     */
    public static void postForcingChains(int number,Cell cells[],Cell grid[][]){
        postXyChain(number,cells,grid);
    }
    /**
     * After a Unique Rectangle: the X-Wing post-technique (singles, locked candidates, subsets and fish around the cells).
     */
    public static void postUniqueRectangle(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
    }
    /**
     * After a Two-String Kite: the X-Wing post-technique (the kite is a single digit pattern, like the fish).
     */
    public static void postTwoStringKite(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
    }
    /**
     * After a Finned / Sashimi Fish: the X-Wing post-technique, which also tries the basic fish again.
     */
    public static void postFinnedFish(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
    }
    /**
     * After a Multi-Coloring: the X-Wing post-technique (single digit technique).
     */
    public static void postMultiColoring(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
    }
    /**
     * After an X-Cycle: the X-Wing post-technique (single digit technique).
     */
    public static void postXCycles(int number,Cell cells[],Cell grid[][]){
        postXWing(number,cells,grid);
    }
    /**
     * After a W-Wing: the XY-Wing post-technique (the cells left with two candidates are tried as pivots).
     */
    public static void postWWing(int number,Cell cells[],Cell grid[][]){
        postXyWing(number,cells,grid);
    }
    /**
     * After an XYZ-Wing: the XY-Wing post-technique.
     */
    public static void postXyzWing(int number,Cell cells[],Cell grid[][]){
        postXyWing(number,cells,grid);
    }
    /**
     * After an AIC: the XY-Wing post-technique.
     */
    public static void postAic(int number,Cell cells[],Cell grid[][]){
        postXyWing(number,cells,grid);
    }
    /**
     * After an ALS-XZ: the XY-Wing post-technique.
     */
    public static void postAlsXz(int number,Cell cells[],Cell grid[][]){
        postXyWing(number,cells,grid);
    }

    // ------------------------------------------------------------------
    //  SOLVING TECHNIQUES
    // ------------------------------------------------------------------

    /**
     * NAKED SINGLE on every cell (see {@link Cell#nakedSingle(Cell[][])}).
     * After a digit is placed the scan starts again.
     */
    public boolean nakedSingle() {
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if(cells[i][j].nakedSingle(this.cells)!=0){
                    nakedSingle();
                    return true;
                }
        return false;
    }
    /**
     * HIDDEN SINGLE for every digit (see {@link House#hiddenSingle(int, Cell[][])}).
     * After a digit is placed the scan starts again from the first digit.
     */
    public boolean hiddenSingle() {
        for (int number = 1; number <= size; number++)
            if (hiddenSingle(number)){
                hiddenSingle();
                return true;
            }
        return false;
    }
    /**
     * Repeats hiddenSingle() on the whole grid and returns true (a digit has just been placed).
     */
    private boolean hiddenSingleAgain(){
        hiddenSingle();
        return true;
    }
    /**
     * HIDDEN SINGLE for {@code number} in every box, row and column where it is not yet placed.
     */
    public boolean hiddenSingle(int number){
        System.out.println("Trying hidden single for digit: "+number);
        int houses[]=boxesMissing(number);//boxes where number is not yet placed
        for(int i=0;i<houses.length;i++)
            if(boxes[houses[i]].hiddenSingle(number,this.cells))
                return hiddenSingleAgain();
        houses=rowsMissing(number);
        for(int i=0;i<houses.length;i++)
            if(rows[houses[i]].hiddenSingle(number,this.cells))
                return hiddenSingleAgain();
        houses=columnsMissing(number);
        for(int i=0;i<houses.length;i++)
            if(columns[houses[i]].hiddenSingle(number,this.cells))
                return hiddenSingleAgain();
        return false;
    }
    /**
     * LOCKED CANDIDATES (Pointing / Claiming) for every digit (see {@link House#lockedCandidates(int, Cell[][])}).
     */
    public boolean lockedCandidates(){
        for(int number=1;number<=size;number++)
            if(lockedCandidates(number)){
                lockedCandidates();
                return true;
            }
        return false;
    }
    private boolean lockedCandidatesAgain(){
        lockedCandidates();
        return true;
    }
    public boolean lockedCandidates(int number){
        System.out.println("Trying locked candidates for digit "+number);
        int houses[]=boxesMissing(number);//pointing: boxes where number is not yet placed
        for(int i=0;i<houses.length;i++)
            if(boxes[houses[i]].lockedCandidates(number,cells))
                return lockedCandidatesAgain();
        houses=rowsMissing(number);//claiming: rows where number is not yet placed
        for(int i=0;i<houses.length;i++)
            if(rows[houses[i]].lockedCandidates(number,cells))
                return lockedCandidatesAgain();
        houses=columnsMissing(number);//claiming: columns where number is not yet placed
        for(int i=0;i<houses.length;i++)
            if(columns[houses[i]].lockedCandidates(number,cells))
                return lockedCandidatesAgain();
        return false;
    }
    /**
     * NAKED and HIDDEN SUBSETS in every box, row and column (see {@link House#subsets(Cell[][])}).
     */
    public boolean subsets() {
        boolean applied = false;
        for (int i = 0; i < size; i++)//boxes
            if (boxes[i].subsets(this.cells))
                applied = true;
        for (int i = 0; i < size; i++)//rows
            if (rows[i].subsets(this.cells))
                applied = true;
        for (int i = 0; i < size; i++)//columns
            if (columns[i].subsets(this.cells))
                applied = true;
        if(applied)
            subsets();
        return applied;
    }
    /**
     * X-WING / SWORDFISH / JELLYFISH for every digit (see {@link House#xWing(int, Cell[][])}).
     */
    public boolean xWing() {
        boolean applied = false;
        for (int number = 1; number <= size; number++)
            if (xWing(number))//xWing(number) already repeats itself for number as long as it makes progress
                applied = true;
        return applied;
    }
    private boolean xWingAgain(int number){
        xWing(number);
        return true;
    }
    public boolean xWing(int number) {
        System.out.println("Trying x-wing for digit: "+number);
        int houses[]=rowsMissing(number);
        for(int i=0;i<houses.length-1;i++)//the first house of the fish cannot be the last one
            if(rows[houses[i]].xWing(number,this.cells))
                return xWingAgain(number);
        houses=columnsMissing(number);
        for(int i=0;i<houses.length-1;i++)
            if(columns[houses[i]].xWing(number,this.cells))
                return xWingAgain(number);
        return false;
    }
    /**
     * The elements of {@code masks} are 0 or 1 (candidate masks of some houses, all of the same length).
     * @return the indices j for which at least one mask has masks[i][j]==1
     *         (e.g. the columns covered by a digit in a set of rows)
     */
    public static int[] unionOfMasks(int masks[][]) {
        int sum[] = new int[masks[0].length];
        for (int i = 0; i < masks[0].length; i++) {
            sum[i] = 0;
            for (int j = 0; j < masks.length; j++)
                sum[i] = sum[i] + masks[j][i];
        }
        int count = 0, result[] = new int[sum.length];
        for (int i = 0; i < sum.length; i++)
            if (sum[i] > 0)
                result[count++] = i;
        return Utils.truncate(result, count);
    }
    /**
     * XY-WING using every cell as pivot (see {@link Cell#xyWing(Cell[][])}).
     */
    public boolean xyWing(){
        boolean applied=false;
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++)
                if(cells[i][j].xyWing(this.cells))//checks if cells[i][j] is the pivot of an XY-Wing and applies it
                    applied=true;
        return applied;
    }
    /**
     * SIMPLE COLOURING for every digit.
     */
    public boolean simpleColoring() {
        boolean applied = false;
        for (int number = 1; number <= size; number++)
            if (simpleColoring(number))
                applied = true;
        return applied;
    }
    /**
     * SIMPLE COLOURING (single digit colouring) for {@code number}.
     * Two cells form a "conjugate pair" (strong link) if in some house {@code number} can go only in those two cells:
     * exactly one of them contains {@code number}. Starting from a cell, the cells linked by conjugate pairs are coloured
     * alternately A and B (a chain): either all the A cells or all the B cells contain {@code number}.
     * <ul>
     *   <li>Color wrap (rule 2): if two cells with the same colour see each other, that colour is false and
     *       {@code number} is removed from all its cells.</li>
     *   <li>Color trap (rule 4): a cell outside the chain that sees both an A and a B cell cannot contain {@code number}.</li>
     * </ul>
     * @return true if at least one candidate has been removed
     */
    public boolean simpleColoring(int number) {
        House houses[][] = {rows, columns, boxes};
        Cell pairs[][] = new Cell[3 * size][];//conjugate pairs of number
        int pairCount = 0;
        for (int s = 0; s < houses.length; s++)
            for (int i = 0; i < size; i++)
                if (houses[s][i].hasNumber(number) == false && houses[s][i].cellsFor(number).length == 2)
                    pairs[pairCount++] = houses[s][i].cellsFor(number);
        boolean applied = false;
        boolean visited[][] = new boolean[size][size];
        for (int c = 0; c < pairCount && applied == false; c++) {
            Cell start = pairs[c][0];
            if (visited[start.getRow()][start.getColumn()])
                continue;
            //colour the chain that starts from start
            Cell chain[] = new Cell[size * size];
            int length = 0;
            start.setSymbol('A');
            visited[start.getRow()][start.getColumn()] = true;
            chain[length++] = start;
            for (int k = 0; k < length; k++)//chain[] is used as a queue: the newly coloured cells are appended
                for (int h = 0; h < pairCount; h++) {
                    Cell other = null;
                    if (pairs[h][0] == chain[k])
                        other = pairs[h][1];
                    else if (pairs[h][1] == chain[k])
                        other = pairs[h][0];
                    if (other != null && visited[other.getRow()][other.getColumn()] == false) {
                        other.setSymbol(Utils.oppositeColor(chain[k].getSymbol()));
                        visited[other.getRow()][other.getColumn()] = true;
                        chain[length++] = other;
                    }
                }
            if (length >= 3) {
                //rule 2 (color wrap): two cells with the same colour that see each other
                char falseColor = '-';
                for (int a = 0; a < length && falseColor == '-'; a++)
                    for (int b = a + 1; b < length && falseColor == '-'; b++)
                        if (chain[a].getSymbol() == chain[b].getSymbol() && chain[a].sees(chain[b]))
                            falseColor = chain[a].getSymbol();
                if (falseColor != '-') {
                    for (int a = 0; a < length; a++)
                        if (chain[a].getSymbol() == falseColor && chain[a].removeCandidate(number)) {
                            System.out.println("Applied simple coloring (color wrap): digit " + number + " cannot go in cell " + chain[a].getPosition());
                            applied = true;
                        }
                } else {
                    //rule 4 (color trap): cells outside the chain that see both an A and a B cell
                    for (int i = 0; i < size; i++)
                        for (int j = 0; j < size; j++) {
                            Cell cell = cells[i][j];
                            if (cell.getNumber() != 0 || visited[i][j] || cell.canHold(number) == false)
                                continue;
                            boolean seesA = false, seesB = false;
                            for (int a = 0; a < length; a++)
                                if (cell.sees(chain[a])) {
                                    if (chain[a].getSymbol() == 'A')
                                        seesA = true;
                                    else
                                        seesB = true;
                                }
                            if (seesA && seesB && cell.removeCandidate(number)) {
                                System.out.println("Applied simple coloring (color trap): digit " + number + " cannot go in cell " + cell.getPosition());
                                applied = true;
                            }
                        }
                }
            }
            for (int a = 0; a < length; a++)//clear the colours of the chain
                chain[a].setSymbol('-');
        }
        return applied;
    }
    /**
     * SKYSCRAPER for every digit, on rows and on columns.
     */
    public boolean skyscraper(){
        boolean applied=false;
        for(int number=1;number<=size;number++)
            if(skyscraper(number,rows) || skyscraper(number,columns))
                applied=true;
        return applied;
    }
    /**
     * SKYSCRAPER for {@code number} on the parallel houses {@code houses[]} (all rows or all columns).
     * Look for two houses where {@code number} can go in exactly two cells, with one of the two cells of each house
     * on the same perpendicular line (the "base"). At most one of the two base cells contains {@code number}, so at
     * least one of the other two cells (the "tops") contains it: {@code number} is removed from the cells that see
     * both tops.
     */
    public boolean skyscraper(int number,House houses[]){
        boolean applied=false;
        for(int i=0;i<size;i++)
            for(int j=i+1;j<size;j++){
                if(houses[i].hasNumber(number) || houses[j].hasNumber(number))
                    continue;
                Cell cells1[]=houses[i].cellsFor(number),cells2[]=houses[j].cellsFor(number);
                if(cells1.length!=2 || cells2.length!=2)
                    continue;
                for(int a=0;a<2;a++)
                    for(int b=0;b<2;b++){
                        Cell base1=cells1[a],base2=cells2[b],top1=cells1[1-a],top2=cells2[1-b];
                        boolean alignedBase=houses[i].getType()==HouseType.ROW ? base1.getColumn()==base2.getColumn() : base1.getRow()==base2.getRow();
                        boolean alignedTops=houses[i].getType()==HouseType.ROW ? top1.getColumn()==top2.getColumn() : top1.getRow()==top2.getRow();
                        if(alignedBase==false || alignedTops)//if the tops are aligned too it is an X-Wing
                            continue;
                        for(int r=0;r<size;r++)
                            for(int c=0;c<size;c++){
                                Cell cell=cells[r][c];
                                if(cell.sees(top1) && cell.sees(top2) && cell.removeCandidate(number)){
                                    System.out.println("Applied skyscraper: digit "+number+" cannot go in cell "+cell.getPosition());
                                    applied=true;
                                }
                            }
                    }
            }
        return applied;
    }
    /**
     * FORCING NET on bivalue cells (the old "ipotesi"): unlike {@link Chains#forcingChains()}, every branch is
     * propagated with ALL the logical techniques. For each empty cell with two candidates, each candidate is tried on a
     * copy of the sudoku solved with the logical techniques only:
     * <ul>
     *   <li>if both copies end in a contradiction the sudoku has no solution;</li>
     *   <li>if only one copy ends in a contradiction, the other candidate goes in the cell (contradiction forcing chain);</li>
     *   <li>otherwise, if a digit is placed in cell k by the first copy and in cell h by the second copy, the digit
     *       cannot go in the cells seen by both k and h (in the peers of k if k==h), because one of the two
     *       candidates is the right one (verity).</li>
     * </ul>
     * @return true if a digit has been placed or a candidate removed
     */
    public boolean forcingNet(){
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++){
                if(cells[i][j].getNumber()!=0 || cells[i][j].getCandidates().length!=2)
                    continue;
                int candidates[]=cells[i][j].getCandidates().clone();
                Sudoku branch1=tryAssumption(i,j,candidates[0]),branch2=tryAssumption(i,j,candidates[1]);
                boolean contradiction1=branch1.hasContradictionSilently(),contradiction2=branch2.hasContradictionSilently();
                if(contradiction1 && contradiction2){
                    System.out.println("Forcing net: the sudoku has no solution because neither candidate fits in cell "+cells[i][j].getPosition());
                    cells[i][j].removeCandidates(candidates);//the cell is left without candidates: solve() notices it with hasContradiction()
                    return true;
                }
                if(contradiction1 || contradiction2){
                    int right=contradiction1 ? candidates[1] : candidates[0],wrong=contradiction1 ? candidates[0] : candidates[1];
                    System.out.println("Applied forcing net: placing "+wrong+" in cell "+cells[i][j].getPosition()+" leads to a contradiction, so "+right+" goes there");
                    Cell changed[]=setNumber(i,j,right);
                    Sudoku.postForcingNet(right,changed,this.cells);
                    return true;
                }
                //both assumptions are possible: look for digits placed by both branches
                Cell changed[][]=new Cell[size+1][size*size];//changed[number] holds the cells from which number has been removed
                int count[]=new int[size+1];
                boolean applied=false;
                for(int k=0;k<size*size;k++){
                    int number=branch1.getCell(k).getNumber();
                    if(number==0 || getCell(k).getNumber()!=0)
                        continue;
                    for(int h=0;h<size*size;h++)
                        if(branch2.getCell(h).getNumber()==number && getCell(h).getNumber()==0){
                            Cell peers[]=getCell(k).commonPeers(getCell(h),this.cells);
                            for(int q=0;q<peers.length;q++)
                                if(peers[q].removeCandidate(number)){
                                    System.out.println("Applied forcing net (cell "+cells[i][j].getPosition()+"): digit "+number+" cannot go in cell "+peers[q].getPosition());
                                    if(count[number]<size*size)
                                        changed[number][count[number]++]=peers[q];
                                    applied=true;
                                }
                        }
                }
                if(applied){
                    for(int number=1;number<=size;number++)
                        if(count[number]>0)
                            Sudoku.postForcingNet(number,Utils.truncate(changed[number],count[number]),this.cells);
                    return true;
                }
            }
        return false;
    }
    /**
     * Creates a copy of the sudoku, places {@code number} in cell (i,j) and solves the copy with the logical
     * techniques only, without printing anything.
     */
    private Sudoku tryAssumption(int i,int j,int number){
        Sudoku copy=copy();
        copy.logicOnly=true;
        java.io.PrintStream screen=System.out;
        try{
            System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
            copy.setNumber(i,j,number);
            copy.solve();
        }finally{
            System.setOut(screen);
        }
        return copy;
    }
    /**
     * Same as {@link #hasContradiction()} but without printing anything.
     */
    private boolean hasContradictionSilently(){
        java.io.PrintStream screen=System.out;
        try{
            System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
            return hasContradiction();
        }finally{
            System.setOut(screen);
        }
    }
    /**
     * @return an independent copy of the sudoku (new cells with the same numbers and candidates)
     */
    public Sudoku copy(){
        Sudoku copy=new Sudoku(size);
        copy.uniquenessChecked=true;//the copies are never checked for uniqueness
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++)
                if(cells[i][j].getNumber()!=0)
                    copy.cells[i][j].setNumber(cells[i][j].getNumber());
                else
                    copy.cells[i][j].setCandidates(cells[i][j].getCandidates());
        return copy;
    }
    /**
     * @return true if the sudoku has no solution: an empty cell without candidates, a digit repeated in a house,
     *         or a digit that cannot go in any cell of a house
     */
    public boolean hasContradiction(){
        House houses[][]={rows,columns,boxes};
        for(int s=0;s<houses.length;s++)
            for(int i=0;i<size;i++){
                if(houses[s][i].hasContradiction())
                    return true;
                for(int number=1;number<=size;number++)
                    if(houses[s][i].hasNumber(number)==false && houses[s][i].countCandidates(number)==0)
                        return true;
            }
        return false;
    }
    /**
     * TRIAL AND ERROR (guessing): the last resort when the logical techniques are not enough.
     * The empty cell with the fewest candidates is chosen and each candidate is tried on a copy of the sudoku:
     * <ul>
     *   <li>if the copy ends in a contradiction the candidate is wrong and is removed from the cell;</li>
     *   <li>if the copy gets solved the candidate is the right one and is placed.</li>
     * </ul>
     * The copies print nothing; they can themselves use forcing nets and trial and error.
     * @return true if a candidate has been removed or a digit placed
     */
    public boolean trialAndError(){
        Cell chosen=null;
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++)
                if(cells[i][j].getNumber()==0 && (chosen==null || cells[i][j].getCandidates().length<chosen.getCandidates().length))
                    chosen=cells[i][j];
        if(chosen==null)
            return false;
        int candidates[]=chosen.getCandidates().clone();
        for(int k=0;k<candidates.length;k++){
            Sudoku attempt=copy();
            java.io.PrintStream screen=System.out;
            try{
                System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
                attempt.setNumber(chosen.getRow(),chosen.getColumn(),candidates[k]);
                attempt.solve();
            }finally{
                System.setOut(screen);
            }
            if(attempt.hasContradiction()){
                System.out.println("Applied trial and error: placing "+candidates[k]+" in cell "+chosen.getPosition()+" leads to a contradiction, so it is removed from the candidates");
                chosen.removeCandidate(candidates[k]);
                return true;
            }
            if(attempt.isSolved()){
                System.out.println("Applied trial and error: placing "+candidates[k]+" in cell "+chosen.getPosition()+" solves the sudoku, so it is placed");
                setNumber(chosen.getRow(),chosen.getColumn(),candidates[k]);
                return true;
            }
        }
        return false;
    }
    /**
     * REMOTE PAIRS.
     * A chain of cells with the same two candidates {x,y}, each one seeing the next: consecutive cells must hold
     * different digits, so the cells are coloured alternately A and B (all A cells hold the same digit, all B cells
     * the other one). Any cell that sees both an A and a B cell cannot contain x or y.
     * If two cells with the same colour see each other the sudoku has no solution.
     * Must run before simple colouring: once colouring has been applied remote pairs finds nothing new.
     * @return true if at least one candidate has been removed (or a contradiction found)
     */
    public boolean remotePairs(){
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++){
                if(cells[i][j].getNumber()!=0)
                    continue;
                Group chain=cells[i][j].buildRemotePairsChain(this.cells);//cells with the same colour hold the same digit
                if(chain==null)
                    continue;
                int pair[]=cells[i][j].getCandidates().clone();
                Cell changed[]=new Cell[size*size];
                int count=0;
                boolean impossible=false;
                if(chain.getCells().length>=4){//with a chain of three cells nothing is done
                    //check if two cells with the same colour see each other
                    for(int k=0;k<chain.getCells().length && impossible==false;k++)
                        for(int h=0;h<chain.getCell(k).getVisibleGroupCells().length && impossible==false;h++)
                            if(chain.getCell(k).getSymbol()==chain.getCell(k).getVisibleGroupCell(h).getSymbol()){
                                System.out.println("The sudoku has no solution because two cells with colour "+chain.getCell(k).getSymbol()+" see each other");
                                impossible=true;
                            }
                    for(int k=0;k<chain.getCells().length-1 && impossible==false;k++)
                        for(int h=k+1;h<chain.getCells().length;h++)//one of the two cells is A and the other is B
                            if(chain.getCell(k).getSymbol()==Utils.oppositeColor(chain.getCell(h).getSymbol())){
                                Cell peers[]=chain.getCell(k).commonPeers(chain.getCell(h),this.cells);
                                for(int q=0;q<peers.length;q++)
                                    if(peers[q].removeCandidates(pair).length>0){
                                        System.out.println("Applied remote pairs: digits "+pair[0]+" "+pair[1]+" cannot go in cell "+peers[q].getPosition());
                                        if(count<changed.length)
                                            changed[count++]=peers[q];
                                    }
                            }
                }
                for(int k=0;k<chain.getCells().length;k++)//always clear the colours of the chain
                    chain.getCell(k).setSymbol('-');
                Cell chainCells[]=chain.getCells();
                chain.dissolve();//clears groupCells and visibleGroupCells of the chain cells
                if(impossible){
                    chainCells[0].removeCandidates(pair);//the cell is left without candidates: solve() notices it with hasContradiction()
                    return true;
                }
                if(count>0){
                    Sudoku.postRemotePairs(pair,Utils.truncate(changed,count),this.cells);
                    return true;
                }
            }
        return false;
    }
    /**
     * XY-CHAIN: for each bivalue cell not yet explored, builds the network of bivalue cells reachable from it and
     * searches it for an XY-Chain (see {@link Group#xyChain(Cell[][])}).
     * @return true at the first elimination
     */
    public boolean xyChain(){
        boolean visited[]=new boolean[size*size];//the cells of an already explored network do not start it again
        for(int k=0;k<size*size;k++){
            Cell cell=getCell(k);
            if(visited[k] || cell.getNumber()!=0 || cell.getCandidates().length!=2)
                continue;
            Group chain=cell.buildXyChain(this.cells);
            for(int q=0;q<chain.getCells().length;q++)
                visited[chain.getCell(q).getPosition()]=true;
            boolean applied=chain.xyChain(this.cells);
            Cell modifiedCells[]=chain.getModifiedCells();
            int number=chain.getRemovedNumber();
            chain.dissolve();//resets the links of the chain cells
            if(applied){
                Sudoku.postXyChain(number,modifiedCells,this.cells);
                return true;
            }
        }
        return false;
    }
}
