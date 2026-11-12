package sudoku;

/**
 * Snapshot of the candidates of a {@link Sudoku}, stored as bit masks, used by the advanced techniques
 * ({@link Patterns} and {@link Chains}).
 *
 * <ul>
 *   <li>{@code mask[p]} has bit d set if digit d is a candidate of the EMPTY cell in position p
 *       (filled cells have mask 0, so they never take part in a pattern);</li>
 *   <li>{@code value[p]} is the digit placed in cell p (0 if the cell is empty).</li>
 * </ul>
 * The snapshot also stores the geometry of the grid (houses, peers) so that the techniques can work with
 * plain integers, which is much faster than building House and Cell objects.
 *
 * A technique does not modify the snapshot: it records the eliminations it finds with {@link #eliminate(int, int)}
 * and then calls {@link #apply(String, String, PostTechnique)}, which removes the candidates from the real cells,
 * prints the log and runs the post-technique.
 */
class CandidateGrid {

    /**
     * A post-technique: re-applies the simpler techniques around the cells from which {@code number} was removed.
     * All the static {@code Sudoku.postXxx(int, Cell[], Cell[][])} methods match this interface.
     */
    interface PostTechnique {
        void run(int number, Cell cells[], Cell grid[][]);
    }

    final Sudoku sudoku;
    final int size;         //side of the sudoku (9)
    final int boxSize;      //side of a box (3)
    final int cellCount;    //size*size (81)
    final int value[];      //digit placed in each cell, 0 if empty
    final int mask[];       //candidates of each empty cell as a bit mask (bit d = digit d)
    /** houses[h] = positions of the cells of house h: rows are 0..size-1, columns size..2*size-1, boxes 2*size..3*size-1 */
    final int houses[][];
    /** housesOf[p] = the three houses of cell p: {row, column, box} */
    final int housesOf[][];
    final boolean sees[][]; //sees[p][q] is true if p and q are different cells sharing a house
    final int peers[][];    //peers[p] = positions of the cells seen by p

    /** eliminations[p] = bit mask of the digits to remove from cell p */
    private final int eliminations[];

    CandidateGrid(Sudoku sudoku){
        this.sudoku=sudoku;
        size=sudoku.getSize();
        boxSize=sudoku.getBoxSize();
        cellCount=size*size;
        value=new int[cellCount];
        mask=new int[cellCount];
        eliminations=new int[cellCount];
        //geometry
        houses=new int[3*size][size];
        housesOf=new int[cellCount][3];
        for(int p=0;p<cellCount;p++){
            int r=row(p),c=column(p),b=box(p);
            houses[r][c]=p;
            houses[size+c][r]=p;
            houses[2*size+b][(r%boxSize)*boxSize+c%boxSize]=p;
            housesOf[p][0]=r;
            housesOf[p][1]=size+c;
            housesOf[p][2]=2*size+b;
        }
        sees=new boolean[cellCount][cellCount];
        peers=new int[cellCount][];
        for(int p=0;p<cellCount;p++){
            int count=0;
            for(int q=0;q<cellCount;q++)
                if(p!=q && (row(p)==row(q) || column(p)==column(q) || box(p)==box(q))){
                    sees[p][q]=true;
                    count++;
                }
            peers[p]=new int[count];
            count=0;
            for(int q=0;q<cellCount;q++)
                if(sees[p][q])
                    peers[p][count++]=q;
        }
        //candidates
        for(int p=0;p<cellCount;p++){
            Cell cell=sudoku.getCell(p);
            value[p]=cell.getNumber();
            if(value[p]==0)
                for(int d:cell.getCandidates())
                    mask[p]|=bit(d);
        }
        //for safety, a digit placed in a peer is never considered a candidate
        for(int p=0;p<cellCount;p++)
            if(value[p]!=0)
                for(int q:peers[p])
                    mask[q]&=~bit(value[p]);
    }

    // ------------------------------------------------------------------
    //  SMALL HELPERS
    // ------------------------------------------------------------------

    static int bit(int digit){
        return 1<<digit;
    }
    static int count(int mask){
        return Integer.bitCount(mask);
    }
    /** @return the digits of a bit mask, in increasing order */
    static int[] digits(int mask){
        int result[]=new int[Integer.bitCount(mask)],n=0;
        for(int d=1;d<32;d++)
            if((mask&bit(d))!=0)
                result[n++]=d;
        return result;
    }
    /** @return the lowest digit of a bit mask */
    static int firstDigit(int mask){
        return Integer.numberOfTrailingZeros(mask);
    }
    int row(int p){
        return p/size;
    }
    int column(int p){
        return p%size;
    }
    int box(int p){
        return (row(p)/boxSize)*boxSize+column(p)/boxSize;
    }
    boolean has(int p,int digit){
        return (mask[p]&bit(digit))!=0;
    }
    /** @return true if {@code digit} is already placed in house h */
    boolean placedIn(int h,int digit){
        for(int p:houses[h])
            if(value[p]==digit)
                return true;
        return false;
    }
    /** @return the positions of the cells of house h where {@code digit} is a candidate */
    int[] positions(int h,int digit){
        int n=0;
        for(int p:houses[h])
            if(has(p,digit))
                n++;
        int result[]=new int[n];
        n=0;
        for(int p:houses[h])
            if(has(p,digit))
                result[n++]=p;
        return result;
    }
    /** @return a human readable name of a house, e.g. "row 3" */
    String houseName(int h){
        if(h<size)
            return "row "+h;
        if(h<2*size)
            return "column "+(h-size);
        return "box "+(h-2*size);
    }
    /** @return the candidate in Eureka notation, e.g. "(5)r1c3" (rows and columns counted from 1) */
    String node(int p,int digit){
        return "("+digit+")r"+(row(p)+1)+"c"+(column(p)+1);
    }

    // ------------------------------------------------------------------
    //  ELIMINATIONS
    // ------------------------------------------------------------------

    /** Records that {@code digit} must be removed from cell p (ignored if it is not a candidate). */
    void eliminate(int p,int digit){
        if(has(p,digit))
            eliminations[p]|=bit(digit);
    }
    /**
     * Records that {@code digit} must be removed from every cell that sees ALL the cells of {@code cells[]},
     * except those cells themselves.
     */
    void eliminateFromCommonPeers(int digit,int... cells){
        for(int q=0;q<cellCount;q++){
            if(has(q,digit)==false)
                continue;
            boolean seesAll=true;
            for(int c:cells)
                if(c==q || sees[q][c]==false){
                    seesAll=false;
                    break;
                }
            if(seesAll)
                eliminate(q,digit);
        }
    }
    boolean hasEliminations(){
        for(int p=0;p<cellCount;p++)
            if(eliminations[p]!=0)
                return true;
        return false;
    }
    /** Forgets the eliminations recorded so far (when a candidate pattern turns out to be useless). */
    void clearEliminations(){
        for(int p=0;p<cellCount;p++)
            eliminations[p]=0;
    }
    /**
     * Applies the recorded eliminations to the real cells of the sudoku: prints {@code description} and a line
     * "Applied technique: digit d cannot go in cell p" for each elimination, then runs {@code post} once for every
     * digit with the cells that lost it. All the candidates are removed before running the post-techniques.
     * @return true if at least one candidate has been removed
     */
    boolean apply(String technique,String description,PostTechnique post){
        if(hasEliminations()==false)
            return false;
        System.out.println(description);
        Cell grid[][]=sudoku.getGrid();
        Cell changed[][]=new Cell[size+1][cellCount];
        int count[]=new int[size+1];
        boolean applied=false;
        for(int p=0;p<cellCount;p++)
            for(int d=1;d<=size;d++)
                if((eliminations[p]&bit(d))!=0 && sudoku.getCell(p).removeCandidate(d)){
                    System.out.println("Applied "+technique+": digit "+d+" cannot go in cell "+p);
                    changed[d][count[d]++]=sudoku.getCell(p);
                    applied=true;
                }
        clearEliminations();
        for(int d=1;d<=size;d++)
            if(count[d]>0)
                post.run(d,utils.Utils.truncate(changed[d],count[d]),grid);
        return applied;
    }
}
