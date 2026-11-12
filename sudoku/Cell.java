package sudoku;

import utils.Utils;

/**
 * A single cell of the sudoku.
 *
 * A cell knows:
 * <ul>
 *   <li>its position in the grid (row*SIZE+column) and the size of the sudoku;</li>
 *   <li>the number placed in it (0 if the cell is empty);</li>
 *   <li>its candidates, i.e. the digits that can still go in it (a filled cell has its number as only candidate);</li>
 *   <li>a symbol used for printing and for chain techniques: the digit as a character, '-' for an empty cell,
 *       'A' or 'B' for the two colours of a chain;</li>
 *   <li>the cells of the {@link Group} (chain) it currently belongs to, and the cells of that group that it sees.</li>
 * </ul>
 * The cell also contains the parts of the techniques that are naturally expressed from the point of view of a
 * single cell (Naked Single, XY-Wing) and the methods that build the chains used by Remote Pairs and XY-Chain.
 */
public class Cell{
    private int position;           //row*size+column
    private int number;             //number placed in the cell, 0 if the cell is empty
    private char symbol;            //printed character: the digit, '-' if empty, 'A'/'B' while colouring a chain
    private int size;               //side of the sudoku the cell belongs to (9 in a classic sudoku)
    private int boxSize;            //side of a box (3 in a classic sudoku)
    private int groupIndex;         //index of the group the cell belongs to
    private Cell groupCells[];      //all the cells of the group (chain) the cell belongs to
    private Cell visibleGroupCells[];//the cells of the group that are seen by this cell
                                    //(in a killer sudoku every cell sees every other cell of the same cage)
    private int candidates[];       //digits that can still go in the cell

    // ------------------------------------------------------------------
    //  GETTERS AND SETTERS
    // ------------------------------------------------------------------

    public void setPosition(int position){
        this.position=position;
    }
    public int getPosition(){
        return position;
    }
    public int getNumber(){
        return number;
    }
    public char getSymbol(){
        return symbol;
    }
    public void setSymbol(char symbol){
        this.symbol=symbol;
    }
    public int[] getCandidates(){
        return candidates;
    }
    public void setCandidates(int candidates[]){
        this.candidates=candidates.clone();//copy: the array must never be shared between cells
    }
    /**
     * Writes {@code number} in the cell (0 empties it). A filled cell keeps its number as its only candidate.
     * It does NOT update the peers: use {@link Sudoku#setNumber(int, int, int)} for that.
     */
    public void setNumber(int number){
        this.number=number;
        if(number==0)
            this.symbol='-';
        else{
            this.symbol=(char)(number+'0');
            candidates=new int[1];
            candidates[0]=number;
        }
    }
    public int getSize(){
        return this.size;
    }
    public int getBoxSize(){
        return this.boxSize;
    }
    public void setSize(int size){
        this.size=size;
        boxSize=(int)Math.sqrt(size);
    }
    public int getGroupIndex(){
        return groupIndex;
    }
    public void setGroupIndex(int index){
        groupIndex=index;
    }
    public void setGroupCells(Cell cells[]){
        groupCells=cells;
    }
    public void setVisibleGroupCells(Cell cells[]){
        visibleGroupCells=cells;
    }
    public Cell[] getGroupCells(){
        return groupCells;
    }
    public Cell getGroupCell(int index){
        return groupCells[index];
    }
    public Cell[] getVisibleGroupCells(){
        return visibleGroupCells;
    }
    public Cell getVisibleGroupCell(int index){
        return visibleGroupCells[index];
    }

    /**
     * Creates an empty cell: every digit from 1 to size is a candidate.
     */
    public Cell(int position,int size){
        candidates=new int[size];
        number=0;
        symbol='-';
        setSize(size);
        for(int i=0;i<size;i++)
            candidates[i]=i+1;
        this.position=position;
    }
    public int getRow(){
        return position/size;
    }
    public int getColumn(){
        return position%size;
    }
    public int getBox(){
        return (getRow()/boxSize)*boxSize+getColumn()/boxSize;
    }

    // ------------------------------------------------------------------
    //  BASIC OPERATIONS
    // ------------------------------------------------------------------

    /**
     * @return the index (0..size-1) of this cell inside its box
     */
    public int getIndexInBox(){
        //build a throw-away grid only to obtain the layout of the box
        Cell grid[][]=new Cell[size][size];
        for(int i=0;i<size;i++)
            for(int j=0;j<size;j++)
                grid[i][j]=new Cell(i*size+j,size);
        for(int i=0;i<size;i++)
            if(Utils.getBox(getBox(),grid).getCell(i).getPosition()==getPosition())
                return i;
        System.out.println("Error: check the method getIndexInBox");
        return 0;
    }
    /**
     * Removes {@code number} from the candidates of the cell.
     * @return true if the candidate was actually removed
     */
    public boolean removeCandidate(int number){
        int numbers[]=new int[1];
        numbers[0]=number;
        return removeCandidates(numbers).length>0;
    }
    /**
     * Removes all the digits of {@code numbers[]} from the candidates of the cell.
     * A filled cell never loses its candidate.
     * NB: the candidates array is never modified in place (it could be shared): a new one is created.
     * @return the digits that have actually been removed (an empty array if none)
     */
    public int[] removeCandidates(int numbers[]){
        if(number!=0)//a filled cell keeps its only candidate
            return new int[0];
        int removed[]=new int[numbers.length],count=0;//digits of numbers[] that have been removed
        int kept[]=new int[candidates.length],n=0;   //candidates that stay
        for(int i=0;i<candidates.length;i++){
            if(Utils.contains(numbers,candidates[i]))
                removed[count++]=candidates[i];
            else
                kept[n++]=candidates[i];
        }
        if(count>0)
            candidates=Utils.truncate(kept,n);
        return Utils.truncate(removed,count);
    }
    /**
     * @return true if {@code cell} is a different cell sharing a row, a column or a box with this one (i.e. a peer)
     */
    public boolean sees(Cell cell){
        return cell!=this && (getRow()==cell.getRow() || getColumn()==cell.getColumn() || getBox()==cell.getBox());
    }
    /**
     * @return true if {@code number} is a candidate of the cell
     */
    public boolean canHold(int number){
        for(int i=0;i<candidates.length;i++)
            if(candidates[i]==number)
                return true;
        return false;
    }

    // ------------------------------------------------------------------
    //  COMMON PEERS
    //  Almost every elimination technique ends with "the digit cannot go in the cells that see
    //  both A and B": these methods compute those cells.
    // ------------------------------------------------------------------

    /**
     * @return the positions of the cells seen both by this cell and by {@code cell}.
     *         If {@code cell} is this same cell, all its peers (row + column + box) are returned.
     */
    public int[] commonPeerPositions(Cell cell){
        int result[],count=0;
        if(getRow()==cell.getRow() && getColumn()==cell.getColumn()){//same cell: all the peers
            result=new int[3*size-1-2*boxSize];
            for(int i=0;i<size;i++)//walk along the row
                if(i!=getColumn())
                    result[count++]=getRow()*size+i;
            for(int i=0;i<size;i++)//walk along the column
                if(i!=getRow())
                    result[count++]=i*size+getColumn();
            Box box=new Box(size,getBox());
            for(int i=0;i<size;i++)//the cells of the box that are not in the same row or column
                if(box.getCell(i).getRow()!=getRow() && box.getCell(i).getColumn()!=getColumn())
                    result[count++]=box.getCell(i).getPosition();
            return result;
        }
        if(getRow()==cell.getRow() && getBox()==cell.getBox()){//same row and same box
            result=new int[2*size-2-boxSize];
            for(int i=0;i<size;i++)//rest of the row
                if(i!=getColumn() && i!=cell.getColumn())
                    result[count++]=getRow()*size+i;
            Box box=new Box(size,getBox());
            for(int i=0;i<size;i++)//rest of the box
                if(box.getCell(i).getRow()!=getRow())
                    result[count++]=box.getCell(i).getPosition();
            return result;
        }
        if(getBox()==cell.getBox() && getColumn()==cell.getColumn()){//same column and same box
            result=new int[2*size-2-boxSize];
            for(int i=0;i<size;i++)//rest of the column
                if(i!=getRow() && i!=cell.getRow())
                    result[count++]=i*size+getColumn();
            Box box=new Box(size,getBox());
            for(int i=0;i<size;i++)//rest of the box
                if(box.getCell(i).getColumn()!=getColumn())
                    result[count++]=box.getCell(i).getPosition();
            return result;
        }
        if(getRow()==cell.getRow()){//only the same row
            result=new int[size-2];
            for(int i=0;i<size;i++)
                if(i!=getColumn() && i!=cell.getColumn())
                    result[count++]=getRow()*size+i;
            return result;
        }
        if(getBox()==cell.getBox()){//only the same box
            result=new int[size-2];
            Box box=new Box(size,getBox());
            for(int i=0;i<size;i++)
                if(box.getCell(i).getPosition()!=position && box.getCell(i).getPosition()!=cell.getPosition())
                    result[count++]=box.getCell(i).getPosition();
            return result;
        }
        if(getColumn()==cell.getColumn()){//only the same column
            result=new int[size-2];
            for(int i=0;i<size;i++)
                if(i!=getRow() && i!=cell.getRow())
                    result[count++]=i*size+getColumn();
            return result;
        }
        //from here on the two cells share no house
        if(getRow()/boxSize==cell.getRow()/boxSize){//same band (horizontal stripe of boxes)
            //the common peers are the cells of each box that lie on the row of the other cell
            result=new int[2*boxSize];
            count=0;
            Box box1=new Box(size,getBox()),box2=new Box(size,cell.getBox());
            for(int i=0;i<size;i++)
                if(box1.getCell(i).getRow()==cell.getRow())
                    result[count++]=box1.getCell(i).getPosition();
            for(int i=0;i<size;i++)
                if(box2.getCell(i).getRow()==getRow())
                    result[count++]=box2.getCell(i).getPosition();
            return result;
        }
        if(getColumn()/boxSize==cell.getColumn()/boxSize){//same stack (vertical stripe of boxes)
            result=new int[2*boxSize];
            count=0;
            Box box1=new Box(size,getBox()),box2=new Box(size,cell.getBox());
            for(int i=0;i<size;i++)
                if(box1.getCell(i).getColumn()==cell.getColumn())
                    result[count++]=box1.getCell(i).getPosition();
            for(int i=0;i<size;i++)
                if(box2.getCell(i).getColumn()==getColumn())
                    result[count++]=box2.getCell(i).getPosition();
            return result;
        }
        //different band and stack: only the two "corners" of the rectangle
        result=new int[2];
        result[0]=getRow()*size+cell.getColumn();
        result[1]=cell.getRow()*size+getColumn();
        return result;
    }
    /**
     * @return the positions of the cells seen by this cell and by ALL the cells of {@code cells[]}
     */
    public int[] commonPeerPositions(Cell cells[]){
        //peers[k] holds the common peers of a pair of cells: a position is returned if it is in every peers[k]
        int peers[][]=new int[cells.length][3*size],result[]=new int[size*size],count=0;
        peers[0]=commonPeerPositions(cells[0]);
        for(int i=0;i<cells.length-1;i++)
            peers[i+1]=cells[i].commonPeerPositions(cells[i+1]);
        boolean found[]=new boolean[peers.length],inAll;
        for(int i=0;i<size;i++){
            for(int j=0;j<size;j++){
                inAll=true;
                for(int k=0;k<peers.length && inAll;k++){
                    found[k]=false;
                    for(int h=0;h<peers[k].length && found[k]==false;h++)
                        if(peers[k][h]==(i*size+j))
                            found[k]=true;
                    if(found[k]==false)
                        inAll=false;
                }
                if(inAll)
                    result[count++]=i*size+j;
            }
        }
        return Utils.truncate(result,count);
    }
    /**
     * @return the cells of {@code grid} seen both by this cell and by the cell in {@code position}
     */
    public Cell[] commonPeers(int position,Cell grid[][]){
        Cell cell=new Cell(position,size);
        return commonPeers(cell,grid);
    }
    /**
     * @return the cells of {@code grid} seen both by this cell and by {@code cell}
     *         (all the peers of this cell if {@code cell} is this cell)
     */
    public Cell[] commonPeers(Cell cell,Cell grid[][]){
        Cell cells[]=new Cell[1];
        cells[0]=cell;
        return commonPeers(cells,grid);
    }
    /**
     * @return the cells of {@code grid} seen by this cell and by all the cells of {@code cells[]}
     */
    public Cell[] commonPeers(Cell cells[],Cell grid[][]){
        int positions[]=commonPeerPositions(cells);
        Cell result[]=new Cell[positions.length];
        for(int i=0;i<positions.length;i++)
            result[i]=grid[positions[i]/grid.length][positions[i]%grid.length];
        return result;
    }
    /**
     * @return true if the cell is empty and its candidates are exactly the digits of {@code numbers[]} (in any order).
     *         ({@code getCandidates().equals(numbers)} would compare the references, not the contents)
     */
    public boolean hasExactlyCandidates(int numbers[]){
        return number==0 && Utils.sameElements(candidates,numbers);
    }

    // ------------------------------------------------------------------
    //  CHAIN BUILDING (colouring, Remote Pairs, XY-Chain)
    //  These methods return a Group of linked cells; the first cell of the group is this cell.
    //  After use, the group MUST be dissolved with Group.dissolve(), otherwise the cells keep
    //  references to it (groupCells / visibleGroupCells).
    // ------------------------------------------------------------------

    /**
     * Builds the group of all the bivalue cells reachable from this cell moving between peers
     * (old helper, currently not used by the solver). The cells are marked with 'A'.
     * @return the group, or null if this cell is not bivalue
     */
    public Group buildBivalueChain(Cell grid[][]){
        Group group=new Group(grid[getPosition()/size][getPosition()%size]);//this cell is the first of the group
        if(getCandidates().length==2){
            setSymbol('A');
            Cell peers[]=commonPeers(getPosition(),grid);
            for(int i=0;i<peers.length;i++)
                if(peers[i].getSymbol()!='A')
                    group=peers[i].buildBivalueChain(group,grid);
        }
        else
            group=null;
        return group;
    }
    public Group buildBivalueChain(Group group,Cell grid[][]){
        if(getCandidates().length==2){
            group.addCell(grid[getPosition()/size][getPosition()%size]);
            setSymbol('A');
            Cell peers[]=commonPeers(getPosition(),grid);
            for(int i=0;i<peers.length;i++)
                if(peers[i].getSymbol()!='A')
                    group=peers[i].buildBivalueChain(group,grid);
        }
        return group;
    }
    /**
     * Remote Pairs chain: starting from this bivalue cell {x,y}, collects every cell with the same two
     * candidates {x,y} that can be reached moving between peers. Consecutive cells of the chain see each other,
     * so they must contain different digits: the cells are coloured alternately 'A' and 'B'
     * (all the 'A' cells contain the same digit, all the 'B' cells the other one).
     * @return the chain, or null if this cell is not bivalue
     */
    public Group buildRemotePairsChain(Cell grid[][]){
        if(getCandidates().length==2){
            System.out.println("Building a remote pairs chain starting from cell "+getPosition()+" with candidates "+getCandidates()[0]+" "+getCandidates()[1]);
            Group group=new Group(grid[getPosition()/size][getPosition()%size]);
            setSymbol('A');
            Cell peers[]=commonPeers(grid[getPosition()/size][getPosition()%size],grid);
            for(int i=0;i<peers.length;i++)
                if(Utils.sameElements(peers[i].getCandidates(),getCandidates()) && group.containsCell(peers[i])==false)//peers[i] has the right candidates to extend the chain
                    group=peers[i].buildRemotePairsChain(group,grid);//appends peers[i] to the chain and goes on from it
            return group;
        }
        return null;
    }
    public Group buildRemotePairsChain(Group group,Cell grid[][]){
        group.addCell(grid[getPosition()/size][getPosition()%size]);
        //the cell takes the opposite colour of the first chain cell it sees (the one it was reached from)
        setSymbol(Utils.oppositeColor(visibleGroupCells[0].getSymbol()));
        Cell peers[]=commonPeers(grid[getPosition()/size][getPosition()%size],grid);
        for(int i=0;i<peers.length;i++)
            if(Utils.sameElements(peers[i].getCandidates(),getCandidates()) && group.containsCell(peers[i])==false)
                group=peers[i].buildRemotePairsChain(group,grid);
        return group;
    }
    /**
     * XY chain network: starting from this bivalue cell, collects every bivalue cell that can be reached moving
     * between peers that share at least one candidate. The group is then searched by {@link Group#xyChain(Cell[][])}.
     * @return the group, or null if this cell is not bivalue
     */
    public Group buildXyChain(Cell grid[][]){
        if(getCandidates().length==2){
            System.out.println("Building an XY-Chain network starting from cell "+getPosition()+" with candidates "+getCandidates()[0]+" "+getCandidates()[1]);
            Group group=new Group(grid[getPosition()/size][getPosition()%size]);
            Cell peers[]=commonPeers(grid[getPosition()/size][getPosition()%size],grid);
            for(int i=0;i<peers.length;i++)//a peer with the same candidates as this cell is also added to the chain
                if(peers[i].getCandidates().length==2 && (peers[i].getCandidates()[0]==getCandidates()[0] || peers[i].getCandidates()[0]==getCandidates()[1] || peers[i].getCandidates()[1]==getCandidates()[0] || peers[i].getCandidates()[1]==getCandidates()[1]))
                    group=peers[i].buildXyChain(group,grid);
            return group;
        }
        return null;
    }
    public Group buildXyChain(Group group,Cell grid[][]){
        group.addCell(grid[getPosition()/size][getPosition()%size]);
        Cell peers[]=commonPeers(grid[getPosition()/size][getPosition()%size],grid);
        for(int i=0;i<peers.length;i++)
            if(peers[i].getCandidates().length==2 && (peers[i].getCandidates()[0]==getCandidates()[0] || peers[i].getCandidates()[0]==getCandidates()[1] || peers[i].getCandidates()[1]==getCandidates()[0] || peers[i].getCandidates()[1]==getCandidates()[1]) && Utils.contains(group.getCells(),peers[i])==false)
                group=peers[i].buildXyChain(group,grid);
        return group;
    }

    // ------------------------------------------------------------------
    //  SOLVING TECHNIQUES
    // ------------------------------------------------------------------

    /**
     * NAKED SINGLE: if the empty cell has a single candidate, that digit goes in the cell.
     * After placing it, Hidden Single is tried on the box, row and column of the cell.
     * @return the digit placed, or 0 if the technique does not apply
     */
    public int nakedSingle(Cell grid[][]){
        System.out.println("Trying naked single on cell: "+getPosition());
        if(getCandidates().length==1 && getNumber()==0){
            System.out.println("Applied naked single: place " + getCandidates()[0]+" in cell: "+getPosition());
            Utils.setNumber(getPosition(),getCandidates()[0],grid);
            //a digit has been placed in these houses: look for new hidden singles there
            Utils.getBox(getBox(),grid).hiddenSingle(grid);
            Utils.getRow(getRow(),grid).hiddenSingle(grid);
            Utils.getColumn(getColumn(),grid).hiddenSingle(grid);
            return getNumber();
        }
        return 0;
    }
    /**
     * XY-WING with this cell as pivot. The second cell (a pincer) is searched in the row and in the box of the pivot,
     * the third cell in the houses seen by the pivot (see {@link House#xyWing(int, int, Cell[][])}).
     * This method exists on the cell because a cell that is left with two candidates becomes a possible pivot,
     * so the post-techniques can call it directly.
     * @return true if at least one candidate has been removed
     */
    public boolean xyWing(Cell grid[][]){
        boolean applied=false;
        if(getCandidates().length==2){
            System.out.println("Trying xy-wing with pivot cell "+getPosition()+" with candidates: "+getCandidates()[0]+" "+getCandidates()[1]);
            for(int i=0;i<size;i++)//walk along the row of the pivot
                if(grid[getRow()][i].getCandidates().length==2 && i!=getColumn())//the i-th cell of the row is bivalue and is not the pivot
                    if(Utils.getRow(getRow(),grid).xyWing(getColumn(),i,grid))//also returns true if it applied subsets with the two cells
                        applied=true;
            for(int i=0;i<size;i++)//walk along the box of the pivot: the second cell is searched outside the row of the pivot
                if(grid[Utils.getBox(getBox(),grid).getCell(i).getRow()][Utils.getBox(getBox(),grid).getCell(i).getColumn()].getCandidates().length==2 && Utils.getBox(getBox(),grid).getCell(i).getRow()!=getRow())
                    if(Utils.getBox(getBox(),grid).xyWing(getIndexInBox(),i,grid))
                        applied=true;
        }
        return applied;
    }
    /**
     * XY-Wing where this cell is the pivot and {@code pincer} is the first pincer: the second pincer is searched
     * among the cells of the current group seen by this cell.
     */
    public boolean xyWing(Cell pincer,Cell grid[][]){
        if(getCandidates()[0]==pincer.getCandidates()[0] && getCandidates()[1]==pincer.getCandidates()[1])
            return false;//same candidates: subsets has already been applied if we got here
        if(getCandidates()[0]!=pincer.getCandidates()[0] && getCandidates()[0]!=pincer.getCandidates()[1] && getCandidates()[1]!=pincer.getCandidates()[0] && getCandidates()[1]!=pincer.getCandidates()[1])
            return false;//the pivot and the pincer have no candidate in common
        int shared=Utils.sharedCandidate(grid[getPosition()/size][getPosition()%size],pincer);//surely different from 0
        int pincerCandidates[]=Utils.pincerCandidates(grid[getPosition()/size][getPosition()%size],pincer);
        boolean applied=false;
        for(int i=0;i<visibleGroupCells.length;i++)//look among the visible cells for the third cell of the XY-Wing
            if(getVisibleGroupCell(i)!=pincer)
                if(xyWing(pincerCandidates,shared,pincer,getVisibleGroupCell(i),grid))
                    applied=true;
        return applied;
    }
    /**
     * Checks if pivot (this cell), {@code pincer1} and {@code pincer2} form an XY-Wing and applies it:
     * the digit z shared by the two pincers is removed from the cells seen by both pincers.
     * @param pincerCandidates the candidates the second pincer must have
     * @param shared           the candidate shared by the pivot and pincer1
     */
    public boolean xyWing(int pincerCandidates[],int shared,Cell pincer1,Cell pincer2,Cell grid[][]){
        if(Utils.sameElements(pincer2.getCandidates(),pincerCandidates)){
            int z,count=0;
            if(pincer1.getCandidates()[0]==shared)
                z=pincer1.getCandidates()[1];
            else
                z=pincer1.getCandidates()[0];
            Cell peers[]=pincer1.commonPeers(pincer2,grid);
            Cell changed[]=new Cell[peers.length];
            for(int i=0;i<peers.length;i++)
                if(peers[i].removeCandidate(z)){//the pivot is among the peers but z is not one of its candidates
                    System.out.println("Applied xy-wing: digit "+z+" cannot go in cell "+peers[i].getPosition());
                    changed[count++]=peers[i];
                }
            Sudoku.postXyWing(z,Utils.truncate(changed,count),grid);
            return count>0;
        }
        return false;
    }
}
