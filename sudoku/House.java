package sudoku;

import utils.Utils;

/**
 * A house (also called unit): a row, a column or a box.
 * Every house contains SIZE cells and must contain each digit exactly once.
 * This class contains the techniques that work inside a single house (Hidden Single, Locked Candidates,
 * Naked/Hidden Subsets) or on a set of parallel houses (X-Wing / Swordfish / Jellyfish).
 */
public class House extends Group{

    private int index;      //index of the house among the houses of the same type (0..SIZE-1)
    private HouseType type; //row, column or box

    public int getIndex(){
        return this.index;
    }
    public void setIndex(int index){
        this.index=index;
    }
    public HouseType getType(){
        return type;
    }
    public void setType(HouseType type){
        this.type=type;
    }
    /**
     * @return a short description of the house used in the log messages, e.g. "row 3"
     */
    public String label(){
        return type.label()+" "+index;
    }

    /**
     * @param houses indices of other houses of the same type
     * @return the indices of the houses of the same type that are neither this house nor one of {@code houses[]}
     */
    public int[] complementHouses(int houses[]){
        int all[]=new int[houses.length+1];
        all[0]=index;
        for(int i=1;i<=houses.length;i++)
            all[i]=houses[i-1];
        return Utils.complementIndices(all,size);
    }
    /**
     * @return the candidate masks of {@code number} for this house followed by the houses in {@code houses[]}
     *         (all of type {@code type})
     */
    public int[][] candidateMasks(int number,int houses[],HouseType type,Cell grid[][]){
        int all[]=new int[houses.length+1];
        all[0]=index;
        for(int i=1;i<=houses.length;i++)
            all[i]=houses[i-1];
        return Utils.candidateMasks(number,all,type,grid);
    }

    // ------------------------------------------------------------------
    //  SOLVING TECHNIQUES
    // ------------------------------------------------------------------

    /**
     * HIDDEN SINGLE on every digit of the house.
     * After a digit is placed the search starts again from the first digit.
     * @return true if a digit has been placed
     */
    public boolean hiddenSingle(Cell grid[][]){
        for(int number=1;number<=getSize();number++)
            if(hiddenSingle(number,grid)){
                hiddenSingle(grid);
                return true;
            }
        return false;
    }
    /**
     * HIDDEN SINGLE: if {@code number} is not yet placed in the house and can go in only one cell of the house,
     * it goes in that cell. Then the post-technique looks for new hidden singles around the changed cells.
     * @return true if the digit has been placed
     */
    public boolean hiddenSingle(int number,Cell grid[][]){
        System.out.println("Trying hidden single for digit "+number+" in "+label());
        if(countCandidates(number)==1 && hasNumber(number)==false){
            System.out.println("Applied hidden single: digit "+number+" in cell "+cellsFor(number)[0].getPosition());
            Cell changed[]=Utils.setNumber(cellsFor(number)[0],number,grid);//cells from which setNumber removed a candidate
            Sudoku.postHiddenSingle(number,changed,grid);//nothing happens if changed is empty
            return true;
        }
        return false;
    }
    /**
     * LOCKED CANDIDATES (Pointing and Claiming / Box-Line Reduction).
     * If inside this house {@code number} can go only in 2..boxSize cells that all lie in another common house
     * (e.g. all in the same row of a box, or all in the same box of a row), then {@code number} must go in one of
     * those cells and can be removed from all the other cells that see all of them.
     * @return true if at least one candidate has been removed
     */
    public boolean lockedCandidates(int number,Cell grid[][]){
        System.out.println("Trying locked candidates for digit "+number+" in "+label());
        int count=0;
        Cell where[]=cellsFor(number),changed[]=new Cell[3*getSize()];//where: cells of the house where number can go
        if(where.length>=2 && where.length<=getBoxSize()){
            Cell peers[]=Utils.commonPeers(getSize(),where,grid);//cells seen by all the cells of where[]
            for(int i=0;i<peers.length;i++)
                if(peers[i].removeCandidate(number)){
                    System.out.println("Applied locked candidates: digit "+number+" cannot go in cell "+peers[i].getPosition()+" because of "+label());
                    changed[count++]=peers[i];//changed collects the cells from which a candidate has been removed
                }
            if(count>0){
                Sudoku.postLockedCandidates(Utils.truncate(changed,count),grid);
                return true;
            }
        }
        return false;
    }
    /**
     * NAKED and HIDDEN SUBSETS (pairs, triples and quads) in the house.
     * Every group of 2, 3, 4 (in general up to boxSize+1) digits and of 2, 3, 4 empty cells is tried.
     * In a house of 9 cells it is enough to stop at 4: a naked subset of 5 cells is the same as a hidden subset of
     * 4 digits and vice versa.
     * @return true if at least one candidate has been removed (or a digit placed)
     */
    public boolean subsets(Cell grid[][]){
        System.out.println("Trying subsets in "+label());
        int missing[]=missingNumbers();//digits that are not yet placed in the house
        if(missing.length==1)
            return hiddenSingle(missing[0],grid);
        for(int dim=2;dim<=getBoxSize()+1 && dim<missing.length;dim++){
            if(hiddenSubsets(missing,new int[0],0,dim,grid))
                return true;
            if(nakedSubsets(indicesForAny(missing),new int[0],0,dim,grid))
                return true;
        }
        return false;
    }
    /**
     * Chooses in every possible way {@code dim} digits among {@code numbers[]}: if together they can go in exactly
     * {@code dim} cells, they form a hidden subset and {@link #applySubset(int[], Cell[][])} is called.
     * @param chosen digits chosen so far
     * @param start  index in numbers[] from which the next digit is chosen (to avoid repeated combinations)
     */
    private boolean hiddenSubsets(int numbers[],int chosen[],int start,int dim,Cell grid[][]){
        if(chosen.length==dim)
            return indicesForAny(chosen).length==dim && applySubset(chosen,grid);
        for(int i=start;i<numbers.length;i++)
            if(hiddenSubsets(numbers,Utils.append(chosen,numbers[i]),i+1,dim,grid))
                return true;
        return false;
    }
    /**
     * Chooses in every possible way {@code dim} empty cells of the house: if together they have only {@code dim}
     * candidates (a naked subset), those digits must go in those cells and are removed from the other cells of the house.
     * @param emptyIndices indices of the cells that can be chosen
     * @param chosen       indices of the cells chosen so far
     */
    private boolean nakedSubsets(int emptyIndices[],int chosen[],int start,int dim,Cell grid[][]){
        if(chosen.length==dim){
            int numbers[]=new int[0];//union of the candidates of the chosen cells
            for(int i=0;i<chosen.length;i++)
                for(int k=0;k<getCell(chosen[i]).getCandidates().length;k++)
                    if(Utils.contains(numbers,getCell(chosen[i]).getCandidates()[k])==false)
                        numbers=Utils.append(numbers,getCell(chosen[i]).getCandidates()[k]);
            if(numbers.length!=dim)
                return false;
            boolean applied=false;
            for(int i=0;i<getSize();i++)
                if(Utils.contains(chosen,i)==false && getCell(i).getNumber()==0 && getCell(i).removeCandidates(numbers).length>0){
                    System.out.println("Applied naked subset in "+label()+": removing candidates from cell "+getCell(i).getPosition());
                    applied=true;
                }
            return applied;
        }
        for(int i=start;i<emptyIndices.length;i++)
            if(getCell(emptyIndices[i]).getNumber()==0 && nakedSubsets(emptyIndices,Utils.append(chosen,emptyIndices[i]),i+1,dim,grid))
                return true;
        return false;
    }
    /**
     * Applies a subset made of the digits {@code numbers[]} (more than one): if they can go in exactly
     * numbers.length cells of the house, then
     * <ul>
     *   <li>they are removed from every cell that sees all those cells (e.g. outside the house);</li>
     *   <li>every other digit is removed from those cells (hidden subset).</li>
     * </ul>
     * If the digits do not form a subset nothing happens: all the possible subsets are tried by {@link #subsets(Cell[][])}.
     * @return true if at least one candidate has been removed
     */
    public boolean applySubset(int numbers[],Cell grid[][]){
        Cell subsetCells[]=cellsForAny(numbers),changed[]=new Cell[2*getSize()];
        int count=0;
        System.out.print("Trying subset with digits: ");
        for(int i=0;i<numbers.length;i++)
            System.out.print(numbers[i]+" ");
        System.out.print(". They can go in cells: ");
        for(int i=0;i<subsetCells.length;i++)
            System.out.print(subsetCells[i].getPosition()+" ");
        System.out.println("");
        boolean applied=false;
        if(numbers.length==subsetCells.length){//the digits of numbers[] form a subset in the cells of subsetCells[]
            Cell peers[]=Utils.commonPeers(getSize(),subsetCells,grid);//cells seen by all the cells of the subset
            for(int i=0;i<peers.length;i++){//remove the digits of the subset from those cells
                int removed[]=peers[i].removeCandidates(numbers);
                if(removed.length>0){
                    System.out.print("Applied subset: removing candidates from cell "+peers[i].getPosition()+". Removed candidates: ");
                    for(int j=0;j<removed.length;j++)
                        System.out.print(removed[j]+" ");
                    System.out.println("");
                    changed[count++]=peers[i];
                    applied=true;
                }
            }
            if(count>0)
                Sudoku.postSubsets(numbers,Utils.truncate(changed,count),grid);
            int others[]=Utils.complementNumbers(numbers,getSize());
            for(int i=0;i<subsetCells.length;i++){//remove every other digit from the cells of the subset
                int removed[]=subsetCells[i].removeCandidates(others);
                if(removed.length>0){
                    System.out.print("Applied subset: removing candidates from cell "+subsetCells[i].getPosition()+". Candidates: ");
                    for(int j=0;j<removed.length;j++)
                        System.out.print(removed[j]+" ");
                    System.out.println("");
                    applied=true;
                }
            }
        }
        return applied;
    }
    /**
     * X-WING and its generalisations SWORDFISH (3 houses) and JELLYFISH (4 houses), i.e. basic fish.
     * This house (a row or a column) is the first one; every group of 1, 2 or 3 further parallel houses is tried.
     * If in n parallel houses {@code number} can go only in cells lying in n crossing houses, then {@code number}
     * can be removed from the other cells of those crossing houses.
     * @return true if at least one candidate has been removed
     */
    public boolean xWing(int number,Cell grid[][]){
        if(getType()!=HouseType.ROW && getType()!=HouseType.COLUMN){
            System.out.println("Error: x-wing called on a box");
            return false;
        }
        if(hasNumber(number) || countCandidates(number)<2)
            return false;
        for(int dim=1;dim<=getBoxSize();dim++)
            if(countCandidates(number)<=dim+1 && xWing(number,new int[0],getIndex()+1,dim,grid))
                return true;
        return false;
    }
    /**
     * Chooses in every possible way {@code dim} parallel houses after this one, in which {@code number} can go in
     * 2..dim+1 cells, and tries the fish on them.
     */
    private boolean xWing(int number,int chosen[],int start,int dim,Cell grid[][]){
        if(chosen.length==dim)
            return xWing(number,chosen,grid);
        for(int i=start;i<getSize();i++){
            House house=getType()==HouseType.ROW ? Utils.getRow(i,grid) : Utils.getColumn(i,grid);
            if(house.hasNumber(number)==false && house.countCandidates(number)>=2 && house.countCandidates(number)<=dim+1)
                if(xWing(number,Utils.append(chosen,i),i+1,dim,grid))
                    return true;
        }
        return false;
    }
    /**
     * Tries the fish made of this house and of the parallel houses {@code houses[]}.
     * @return true if at least one candidate has been removed
     */
    public boolean xWing(int number,int houses[],Cell grid[][]){
        //crossing houses (columns for rows, rows for columns) in which number can go inside the chosen houses
        int crossing[]=Sudoku.unionOfMasks(candidateMasks(number,houses,getType(),grid));
        System.out.print("Trying x-wing on "+getType().label()+"s: "+getIndex()+" ");
        for(int i=0;i<houses.length;i++)
            System.out.print(houses[i]+" ");
        System.out.print(". In cells: ");
        for(int i=0;i<crossing.length;i++)
            System.out.print(crossing[i]+" ");
        System.out.println("");
        boolean applied=false;
        if(houses.length+1==crossing.length){//the fish uses this house and the houses of houses[], i.e. houses.length+1 houses
            int otherHouses[]=complementHouses(houses),count=0;
            Cell changed[]=new Cell[getSize()*getSize()];//cells of the grid from which a candidate has been removed
            for(int i=0;i<crossing.length;i++){
                House house;
                if(getType()==HouseType.ROW)
                    house=Utils.getColumn(crossing[i],grid);
                else
                    house=Utils.getRow(crossing[i],grid);
                System.out.print("Trying to remove digit "+number+" from cells: ");
                for(int j=0;j<otherHouses.length;j++)
                    System.out.print(otherHouses[j]);
                System.out.println(" of "+house.label());
                //indices of the cells of the crossing house from which number has been removed
                int removed[]=house.removeCandidate(number,otherHouses);
                if(removed.length>0){
                    for(int j=0;j<removed.length;j++)
                        changed[count++]=grid[house.getCell(removed[j]).getRow()][house.getCell(removed[j]).getColumn()];
                    System.out.print("Applied x-wing: removed digit "+number+" from cells ");
                    for(int j=0;j<removed.length;j++)
                        System.out.print(removed[j]+" ");
                    System.out.println(" of "+house.label());
                    applied=true;
                }
            }
            if(count>0)
                Sudoku.postXWing(number,Utils.truncate(changed,count),grid);
        }//if the houses do not form a fish nothing happens: every group of houses is tried by xWing(number,grid)
        return applied;
    }
    /**
     * XY-WING: the i-th cell of the house is the pivot and the j-th cell is the first pincer;
     * the second pincer is searched by {@link #xyWing(Cell, Cell, Cell[][])} in another house seen by the pivot.
     * If the two cells have the same two candidates they form a naked pair, so subsets is applied instead.
     * @return true if at least one candidate has been removed
     */
    public boolean xyWing(int i,int j,Cell grid[][]){
        boolean applied=false;
        if(getCell(i).getNumber()!=0 || getCell(j).getNumber()!=0 || getCell(i).getCandidates().length!=2 || getCell(j).getCandidates().length!=2)
            return false;//the cells could have been changed by the post-techniques
        if(getCell(i).getCandidates()[0]!=getCell(j).getCandidates()[0] && getCell(i).getCandidates()[0]!=getCell(j).getCandidates()[1] && getCell(i).getCandidates()[1]!=getCell(j).getCandidates()[0] && getCell(i).getCandidates()[1]!=getCell(j).getCandidates()[1])
            return false;//no candidate in common
        if(getCell(i).getCandidates()[0]==getCell(j).getCandidates()[0] && getCell(i).getCandidates()[1]==getCell(j).getCandidates()[1])
            return subsets(grid);//same candidates: naked pair
        System.out.println("Cell " + j + " of "+label()+" is a possible pincer of the pivot, with candidates: "
            + getCell(j).getCandidates()[0] + "," + getCell(j).getCandidates()[1]);
        if(getType()==HouseType.ROW){
            if(Utils.getBox(getCell(i).getBox(),grid).xyWing(getCell(i),getCell(j),grid))
                return true;//after an elimination the cells may have changed: do not go on with old data
            if(Utils.getColumn(getCell(i).getColumn(),grid).xyWing(getCell(i),getCell(j),grid))
                return true;
        }else if(getType()==HouseType.BOX){
            if(Utils.getColumn(getCell(i).getColumn(),grid).xyWing(getCell(i),getCell(j),grid))
                return true;
        }else
            System.out.println("Error: xy-wing called on a column");
        return applied;
    }
    /**
     * Looks in this house for the second pincer of an XY-Wing and applies the elimination.
     * {@code pivot} and {@code pincer} are bivalue cells sharing exactly one candidate.
     * The second pincer must have as candidates the two non shared candidates of pivot and pincer;
     * the digit z of the first pincer that is not in the pivot is then removed from the cells seen by both pincers.
     * @return true if at least one candidate has been removed
     */
    public boolean xyWing(Cell pivot,Cell pincer,Cell grid[][]){
        boolean applied=false;
        if(pivot.getNumber()!=0 || pincer.getNumber()!=0 || pivot.getCandidates().length!=2 || pincer.getCandidates().length!=2)
            return false;//the cells could have been changed by the post-techniques
        int shared=Utils.sharedCandidate(pivot,pincer);
        if(shared<=0)//-1 if the cells have no candidate in common or the same candidates
            return false;
        int wanted[]=Utils.pincerCandidates(pivot,pincer);//candidates of the second pincer
        int z=0;//digit to remove: the candidate of the first pincer that is not shared with the pivot
        if(pincer.getCandidates()[0]==shared)
            z=pincer.getCandidates()[1];
        else
            z=pincer.getCandidates()[0];
        System.out.println("Looking in "+label()+" for the third cell of the xy-wing (i.e. with candidates: "+wanted[0]+" "+wanted[1]+")");
        Cell secondPincers[]=cellsWithCandidates(wanted);

        for(int i=0;i<secondPincers.length && applied==false;i++){//stop after an elimination: the post-techniques change the cells
            System.out.println("Cell "+secondPincers[i].getPosition()+" forms an xy-wing with the two previous cells");
            Cell peers[]=secondPincers[i].commonPeers(pincer,grid);
            System.out.print("Trying to remove digit " + z + " from cells: ");
            for(int j=0;j<peers.length;j++)
                System.out.print(peers[j].getPosition() + " ");
            System.out.println("");
            Cell changed[]=new Cell[peers.length];
            int count=0;
            for(int j=0;j<peers.length;j++)
                if(peers[j].removeCandidate(z)){
                    changed[count++]=peers[j];
                    System.out.println("Applied xy-wing: digit "+z+ " cannot go in cell "+peers[j].getPosition());
                    applied=true;
                }
            if(applied)
                Sudoku.postXWing(z,Utils.truncate(changed,count),grid);
        }
        return applied;
    }
}
