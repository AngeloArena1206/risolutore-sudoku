package sudoku;

/**
 * Fast solution counter, used to check that a sudoku has exactly one solution before solving it "like a human".
 *
 * Algorithm: backtracking with bit masks and MRV (minimum remaining values).
 * <ul>
 *   <li>For every row, column and box a bit mask records the digits already used, so the candidates of a cell are
 *       computed with a few bitwise operations: {@code ~(rowUsed | columnUsed | boxUsed)}.</li>
 *   <li>At every step the empty cell with the FEWEST candidates is chosen (a cell with no candidates means a dead
 *       end, a cell with one candidate is a forced move): this keeps the search tree very small.</li>
 *   <li>The search stops as soon as {@code limit} solutions are found: to know whether the solution is unique it is
 *       enough to look for two.</li>
 * </ul>
 * Only the placed numbers (the givens) are used, not the candidates, so the result does not depend on the
 * techniques of the solver. A classic 9x9 sudoku is checked in a few milliseconds even when it is very hard.
 */
public class SolutionCounter {
    private final int size,boxSize,cellCount;
    private final int value[];      //digit of each cell, 0 if empty
    private final int rowUsed[],columnUsed[],boxUsed[];//bit d set if digit d is already used in that house
    private final int all;          //bit mask with the bits of all the digits 1..size
    private int solutions;
    private int limit;

    /**
     * @param sudoku the sudoku to check (it is not modified)
     */
    public SolutionCounter(Sudoku sudoku){
        size=sudoku.getSize();
        boxSize=sudoku.getBoxSize();
        cellCount=size*size;
        value=new int[cellCount];
        rowUsed=new int[size];
        columnUsed=new int[size];
        boxUsed=new int[size];
        all=((1<<size)-1)<<1;
        for(int p=0;p<cellCount;p++)
            value[p]=sudoku.getCell(p).getNumber();
    }

    /**
     * Counts the solutions of the sudoku, stopping at {@code limit}.
     * @param limit maximum number of solutions to look for (2 is enough to test uniqueness)
     * @return the number of solutions found: 0 if the sudoku is impossible, at most {@code limit}
     */
    public int countSolutions(int limit){
        this.limit=limit;
        solutions=0;
        //register the givens; a digit repeated in a house means that there is no solution
        for(int p=0;p<cellCount;p++){
            int d=value[p];
            if(d==0)
                continue;
            int b=1<<d,r=row(p),c=column(p),x=box(p);
            if((rowUsed[r]&b)!=0 || (columnUsed[c]&b)!=0 || (boxUsed[x]&b)!=0)
                return 0;
            rowUsed[r]|=b;
            columnUsed[c]|=b;
            boxUsed[x]|=b;
        }
        search();
        return solutions;
    }
    /**
     * @return true if the sudoku has exactly one solution
     */
    public boolean hasUniqueSolution(){
        return countSolutions(2)==1;
    }

    private int row(int p){
        return p/size;
    }
    private int column(int p){
        return p%size;
    }
    private int box(int p){
        return (row(p)/boxSize)*boxSize+column(p)/boxSize;
    }
    /** @return the candidates of the empty cell p as a bit mask */
    private int candidates(int p){
        return all&~(rowUsed[row(p)]|columnUsed[column(p)]|boxUsed[box(p)]);
    }

    /**
     * Recursive backtracking: fills the empty cell with fewest candidates with each of its candidates in turn.
     */
    private void search(){
        //MRV: look for the empty cell with the fewest candidates
        int best=-1,bestMask=0,bestCount=Integer.MAX_VALUE;
        for(int p=0;p<cellCount;p++){
            if(value[p]!=0)
                continue;
            int m=candidates(p),n=Integer.bitCount(m);
            if(n<bestCount){
                best=p;
                bestMask=m;
                bestCount=n;
                if(n<=1)
                    break;//dead end (0) or forced move (1): no better cell can exist
            }
        }
        if(best==-1){//no empty cell left: a solution has been found
            solutions++;
            return;
        }
        int r=row(best),c=column(best),x=box(best);
        while(bestMask!=0 && solutions<limit){
            int b=bestMask&-bestMask;//lowest candidate bit
            bestMask&=~b;
            //place the digit
            value[best]=Integer.numberOfTrailingZeros(b);
            rowUsed[r]|=b;
            columnUsed[c]|=b;
            boxUsed[x]|=b;
            search();
            //undo it
            rowUsed[r]&=~b;
            columnUsed[c]&=~b;
            boxUsed[x]&=~b;
            value[best]=0;
        }
    }
}
