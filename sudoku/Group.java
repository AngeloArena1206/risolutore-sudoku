package sudoku;

import utils.Utils;

/**
 * A generic group of cells.
 * It is used in two ways:
 * <ul>
 *   <li>as base class of {@link House} (rows, columns and boxes always have SIZE cells);</li>
 *   <li>as a chain (or network) of linked cells built by the chain techniques (Remote Pairs, XY-Chain):
 *       in this case every cell of the group keeps the list of the group cells it sees
 *       ({@link Cell#getVisibleGroupCells()}), and the group must be dissolved with {@link #dissolve()} after use.</li>
 * </ul>
 */
public class Group{

    protected int size;     //side of the sudoku (9 in a classic sudoku)
    protected int boxSize;  //side of a box (3 in a classic sudoku)
    private Cell cells[];

    public int getSize(){
        return size;
    }
    public int getBoxSize(){
        return boxSize;
    }
    public void setSize(int size){
        this.size=size;
        boxSize=(int)Math.sqrt(size);
    }
    /**
     * Creates an empty array of SIZE cells. Overridden by Row, Column and Box to fill it with their cells.
     */
    public void initCells(){
        cells=new Cell[size];
    }
    public void setCells(Cell cells[]){
        this.cells=new Cell[cells.length];
        for(int i=0;i<cells.length;i++)
            setCell(cells[i],i);
    }
    public void setCell(Cell cell,int index){
        cells[index]=cell;
    }
    /**
     * Puts in place {@code index} a NEW empty cell with the given position (a placeholder that only tells
     * where the real cell is).
     */
    public void setCell(int position,int index){
        cells[index]=new Cell(position,getSize());
    }
    public Cell getCell(int i){
        return cells[i];
    }
    public Cell[] getCells(){
        return cells;
    }
    /**
     * @return the cells of the group whose candidates are exactly the digits of {@code numbers[]}
     */
    public Cell[] cellsWithCandidates(int numbers[]){
        Cell result[]=new Cell[size];
        int count=0;
        for(int i=0;i<cells.length;i++)
            if(getCell(i).hasExactlyCandidates(numbers))
                result[count++]=getCell(i);
        return Utils.truncate(result,count);
    }
    /**
     * @return the indices (inside the group) of the cells whose candidates are exactly the digits of {@code numbers[]}
     */
    public int[] indicesWithCandidates(int numbers[]){
        int result[]=new int[size],count=0;
        for(int i=0;i<cells.length;i++)
            if(getCell(i).hasExactlyCandidates(numbers))
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /**
     * @return the cells of the group that have exactly {@code amount} candidates
     */
    public Cell[] cellsWithCandidateCount(int amount){
        Cell result[]=new Cell[cells.length];
        int count=0;
        for(int i=0;i<cells.length;i++)
            if(cells[i].getCandidates().length==amount)
                result[count++]=cells[i];
        return Utils.truncate(result,count);
    }
    /**
     * @return the indices (inside the group) of the cells that have exactly {@code amount} candidates
     */
    public int[] indicesWithCandidateCount(int amount){
        int result[]=new int[cells.length],count=0;
        for(int i=0;i<cells.length;i++)
            if(cells[i].getCandidates().length==amount)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /**
     * Places {@code number} in the i-th cell of the group and removes it from the other cells of the group.
     */
    public void setNumber(int number,int i){
        removeCandidate(number);
        getCell(i).setNumber(number);
    }
    /**
     * Removes {@code number} from the candidates of every cell of the group.
     * @return the indices of the cells from which it has actually been removed
     */
    public int[] removeCandidate(int number){
        int indices[]=new int[getSize()];
        for(int i=0;i<cells.length;i++)
            indices[i]=i;
        return removeCandidate(number,indices);
    }
    /**
     * Removes {@code number} from the candidates of the cells of the group whose indices are in {@code indices[]}.
     * @return the indices of the cells from which it has actually been removed
     */
    public int[] removeCandidate(int number,int indices[]){
        int changed[]=new int[getSize()],count=0;
        for(int i=0;i<indices.length;i++)
            if(getCell(indices[i]).removeCandidate(number))
                changed[count++]=indices[i];
        return Utils.truncate(changed,count);
    }
    public Group(){
        cells=new Cell[0];
    }
    /**
     * Creates a group (chain) made of a single cell; the size is taken from the cell.
     */
    public Group(Cell cell){
        setSize(cell.getSize());
        addCell(cell);
    }
    public Group(Cell cells[]){
        setSize(cells[0].getSize());
        addCells(cells);
    }

    // ------------------------------------------------------------------
    //  QUERIES ON A HOUSE
    // ------------------------------------------------------------------

    /**
     * @return in how many cells of the house {@code number} can go.
     *         If {@code number} is already placed in the house, 1 is returned.
     */
    public int countCandidates(int number){
        int count=0;
        for(int i=0;i<size;i++){
            if(cells[i].canHold(number))
                count++;
            if(cells[i].getNumber()==number){
                count=1;
                i=size-1;//stop the loop
            }
        }
        return count;
    }
    /**
     * @return the "candidate mask" of {@code number}: an array of SIZE elements with 1 for the cells of the house
     *         where {@code number} can go and 0 for the others. If {@code number} is already placed, the mask has a 1
     *         in the cell where it is placed (and possibly in the cells checked before it).
     */
    public int[] candidateMask(int number){
        int mask[]=new int[size];
        for(int i=0;i<size;i++){
            if(cells[i].canHold(number))
                mask[i]=1;
            else
                mask[i]=0;
            if(cells[i].getNumber()==number){
                mask[i]=1;
                i=size-1;//stop the loop
            }
        }
        return mask;
    }
    /**
     * @return the digits that are not yet placed in the house
     */
    public int[] missingNumbers(){
        int result[]=new int[size],count=0;
        for(int i=1;i<=size;i++)
            if(hasNumber(i)==false)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /**
     * @return the indices (inside the house, NOT the positions in the grid) of the cells where {@code number} can go
     */
    public int[] indicesFor(int number){
        int numbers[]=new int[1];
        numbers[0]=number;
        return indicesForAny(numbers);
    }
    /**
     * @return the cells of the house where {@code number} can go
     */
    public Cell[] cellsFor(int number){
        int numbers[]=new int[1];
        numbers[0]=number;
        return cellsForAny(numbers);
    }
    /**
     * @return the indices (inside the house) of the cells where at least one digit of {@code numbers[]} can go
     */
    public int[] indicesForAny(int numbers[]){
        int count=0,masks[][]=new int[numbers.length][0],sum[]=new int[size];//masks[i] is the candidate mask of numbers[i]
        for(int i=0;i<masks.length;i++)
            masks[i]=candidateMask(numbers[i]);
        for(int i=0;i<size;i++){
            sum[i]=0;
            for(int j=0;j<numbers.length;j++)
                sum[i]=sum[i]+masks[j][i];
        }
        int result[]=new int[size];
        for(int i=0;i<size;i++)
            if(sum[i]>0)
                result[count++]=i;
        return Utils.truncate(result,count);
    }
    /**
     * @return the cells of the house where at least one digit of {@code numbers[]} can go
     */
    public Cell[] cellsForAny(int numbers[]){
        int indices[]=indicesForAny(numbers);
        Cell result[]=new Cell[indices.length];
        for(int i=0;i<result.length;i++)
            result[i]=getCell(indices[i]);
        return result;
    }
    /**
     * @return true if {@code number} is already placed in the house
     */
    public boolean hasNumber(int number){
        for(int i=0;i<size;i++)
            if(cells[i].getNumber()==number)
                return true;
        return false;
    }
    /**
     * @return true if the group contains a cell without candidates or the same digit placed twice
     */
    public boolean hasContradiction(){
        for(int i=0;i<cells.length;i++)
            if(cells[i].getCandidates().length==0){
                System.out.println("The sudoku is invalid because no digit can go in cell "+cells[i].getPosition());
                return true;
            }
        for(int i=0;i<cells.length-1;i++)
            if(cells[i].getNumber()!=0)
                for(int j=i+1;j<cells.length;j++)
                    if(cells[i].getNumber()==cells[j].getNumber()){
                        System.out.println("The sudoku is invalid because digit "+cells[i].getNumber()+" appears twice in the same house");
                        return true;
                    }
        return false;
    }

    // ------------------------------------------------------------------
    //  CHAIN MANAGEMENT
    // ------------------------------------------------------------------

    /**
     * Adds {@code cell} to the group (if not already there) and updates, for every cell of the group,
     * the list of the group cells and the list of the group cells it sees.
     */
    public void addCell(Cell cell){
        if(cell==null || Utils.contains(cells,cell))
            return;
        cells=Utils.append(cells,cell);
        for(int i=0;i<cells.length;i++)
            cells[i].setGroupCells(cells);
        for(int i=0;i<cells.length-1;i++)
            if(cells[i].getRow()==cell.getRow() || cells[i].getColumn()==cell.getColumn() || cells[i].getBox()==cell.getBox()){
                //the new cell and cells[i] see each other
                if(Utils.contains(cells[i].getVisibleGroupCells(),cell)==false)
                    cells[i].setVisibleGroupCells(Utils.append(cells[i].getVisibleGroupCells(),cell));
                if(Utils.contains(cell.getVisibleGroupCells(),cells[i])==false)
                    cell.setVisibleGroupCells(Utils.append(cell.getVisibleGroupCells(),cells[i]));
            }
    }
    /**
     * Adds the cells of {@code newCells[]} to the group (see {@link #addCell(Cell)}).
     */
    public void addCells(Cell newCells[]){
        if(cells==null){
            cells=newCells;
            return;
        }
        for(int i=0;i<newCells.length;i++)
            addCell(newCells[i]);
    }
    /**
     * Placeholder: does nothing yet.
     */
    public void solve(Cell grid[][]){
        for(int i=0;i<cells.length;i++)
            if(cells[i].getCandidates().length==1)
                return;
    }
    /**
     * @return true if {@code cell} belongs to the group
     */
    public boolean containsCell(Cell cell){
        for(int i=0;i<cells.length;i++)
            if(cells[i]==cell)
                return true;
        return false;
    }
    /**
     * Prints every cell of the group with the group cells it sees (for debugging).
     */
    public void print(){
        for(int i=0;i<cells.length;i++){
            System.out.print("Cell "+i+" "+cells[i].getPosition()+". Visible cells: ");
            for(int j=0;j<cells[i].getVisibleGroupCells().length;j++)
                System.out.print(cells[i].getVisibleGroupCell(j).getPosition()+" ");
            System.out.println("");
        }
    }
    /**
     * Must be called when the group is no longer needed: it clears groupCells and visibleGroupCells of every
     * cell, otherwise the next chain built on the same cells would inherit the old links.
     */
    public void dissolve(){
        for(int i=0;i<cells.length;i++){
            cells[i].setGroupCells(new Cell[0]);
            cells[i].setVisibleGroupCells(new Cell[0]);
        }
    }

    // ------------------------------------------------------------------
    //  SOLVING TECHNIQUES
    // ------------------------------------------------------------------

    private Cell modifiedCells[]=new Cell[0];//cells from which xyChain() removed removedNumber
    private int removedNumber=0;
    public Cell[] getModifiedCells(){
        return modifiedCells;
    }
    public int getRemovedNumber(){
        return removedNumber;
    }
    /**
     * XY-CHAIN on the group built by {@link Cell#buildXyChain(Cell[][])}.
     *
     * Start from a bivalue cell {x,a} and suppose that it is NOT x: then it is a; a bivalue peer {a,b} is then b,
     * a bivalue peer {b,c} of that cell is then c, and so on. If the chain reaches an end cell that would then be x,
     * either the start cell or the end cell is x, so x cannot go in the cells that see both the start and the end.
     * An XY-Wing is the special case of an XY-Chain of three cells (pincer - pivot - pincer).
     *
     * The search is a breadth first search on the states (cell, digit forced in that cell); every cell of the chain
     * must be bivalue and must see the previous one (visibleGroupCells).
     * @return true if a candidate has been removed (the search stops at the first elimination;
     *         the changed cells and the removed digit are available through getModifiedCells()/getRemovedNumber())
     */
    public boolean xyChain(Cell grid[][]){
        modifiedCells=new Cell[0];
        removedNumber=0;
        for(int i=0;i<cells.length;i++){
            Cell start=cells[i];
            if(start.getNumber()!=0 || start.getCandidates().length!=2)
                continue;
            for(int c=0;c<2;c++){
                int x=start.getCandidates()[c];
                //BFS queue: queue[k] is forced to forced[k] if start is not x; length[k] is the number of cells of the chain
                Cell queue[]=new Cell[2*cells.length];
                int forced[]=new int[2*cells.length],length[]=new int[2*cells.length];
                boolean visited[][]=new boolean[size*size][size+1];//visited[position][digit]
                int n=0;
                queue[n]=start;
                forced[n]=start.getCandidates()[1-c];
                length[n++]=1;
                visited[start.getPosition()][forced[0]]=true;
                for(int k=0;k<n;k++){
                    Cell visible[]=queue[k].getVisibleGroupCells();
                    if(visible==null)
                        continue;
                    for(int h=0;h<visible.length;h++){
                        Cell cell=visible[h];
                        //the next link must be an empty bivalue cell containing the digit forced in the current cell
                        if(cell==start || cell.getNumber()!=0 || cell.getCandidates().length!=2 || cell.canHold(forced[k])==false)
                            continue;
                        //the current cell holds forced[k], so the next cell must hold its other candidate
                        int other=cell.getCandidates()[0]==forced[k] ? cell.getCandidates()[1] : cell.getCandidates()[0];
                        if(visited[cell.getPosition()][other])
                            continue;
                        visited[cell.getPosition()][other]=true;
                        if(other==x && length[k]+1>=3 && applyXyChain(x,start,cell,length[k]+1,grid))
                            return true;
                        if(n<queue.length){
                            queue[n]=cell;
                            forced[n]=other;
                            length[n++]=length[k]+1;
                        }
                    }
                }
            }
        }
        return false;
    }
    /**
     * Removes x from the cells seen both by the start and by the end of an XY-Chain.
     * @return true if at least one candidate has been removed
     */
    private boolean applyXyChain(int x,Cell start,Cell end,int length,Cell grid[][]){
        Cell peers[]=start.commonPeers(end,grid);
        Cell changed[]=new Cell[peers.length];
        int count=0;
        for(int q=0;q<peers.length;q++)
            if(peers[q].removeCandidate(x)){
                System.out.println("Applied XY-Chain (chain of "+length+" cells from "+start.getPosition()+" to "+end.getPosition()+"): digit "+x+" cannot go in cell "+peers[q].getPosition());
                changed[count++]=peers[q];
            }
        if(count==0)
            return false;
        modifiedCells=Utils.truncate(changed,count);
        removedNumber=x;
        return true;
    }
}
