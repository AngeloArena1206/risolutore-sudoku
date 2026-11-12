package sudoku;

/**
 * Pattern based techniques that work on a {@link CandidateGrid} snapshot:
 * Unique Rectangle, Two-String Kite, Finned / Sashimi Fish, W-Wing, XYZ-Wing and Multi-Coloring.
 *
 * Every method looks for the first instance of its pattern that removes at least one candidate, applies it
 * (through {@link CandidateGrid#apply(String, String, CandidateGrid.PostTechnique)}) and returns true.
 * A new Patterns object must be created for every call, because the snapshot is taken in the constructor.
 */
class Patterns {
    private final CandidateGrid g;
    private final int size,boxSize,cellCount;

    Patterns(Sudoku sudoku){
        g=new CandidateGrid(sudoku);
        size=g.size;
        boxSize=g.boxSize;
        cellCount=g.cellCount;
    }

    // ------------------------------------------------------------------
    //  UNIQUE RECTANGLE
    // ------------------------------------------------------------------

    /**
     * UNIQUE RECTANGLE (types 1, 2 and 4).
     * Four empty cells at the corners of a rectangle, spread over exactly two boxes, that all contain the two
     * candidates {a,b}. If all four cells ended up with a or b, a and b could be swapped and the sudoku would have
     * two solutions (a "deadly pattern"). Since a proper sudoku has a unique solution, this must not happen:
     * <ul>
     *   <li>type 1: three corners are exactly {a,b} -> a and b are removed from the fourth corner;</li>
     *   <li>type 2: two corners are exactly {a,b} and the other two are exactly {a,b,x} -> one of them is x, so x is
     *       removed from the cells that see both;</li>
     *   <li>type 4: two corners are exactly {a,b}, the other two lie in the same row/column and in a house they share
     *       digit u (a or b) can go only in those two cells -> one of them is u, so neither can be the other digit,
     *       which is removed from both.</li>
     * </ul>
     * NB: this technique assumes that the sudoku has a unique solution.
     */
    boolean uniqueRectangle(){
        for(int r1=0;r1<size;r1++)
            for(int r2=r1+1;r2<size;r2++)
                for(int c1=0;c1<size;c1++)
                    for(int c2=c1+1;c2<size;c2++){
                        int corners[]={r1*size+c1,r1*size+c2,r2*size+c1,r2*size+c2};
                        boolean sameBand=r1/boxSize==r2/boxSize,sameStack=c1/boxSize==c2/boxSize;
                        if(sameBand==sameStack)//the rectangle must cover exactly two boxes
                            continue;
                        int common=~0;
                        for(int p:corners)
                            common&=g.mask[p];//0 if a corner is filled
                        if(CandidateGrid.count(common)<2)
                            continue;
                        int ds[]=CandidateGrid.digits(common);
                        for(int i=0;i<ds.length;i++)
                            for(int j=i+1;j<ds.length;j++)
                                if(uniqueRectangle(corners,ds[i],ds[j]))
                                    return true;
                    }
        return false;
    }
    private boolean uniqueRectangle(int corners[],int a,int b){
        int ab=CandidateGrid.bit(a)|CandidateGrid.bit(b);
        int floor[]=new int[4],roof[]=new int[4],nFloor=0,nRoof=0;//floor: exactly {a,b}; roof: {a,b} plus something else
        for(int p:corners)
            if(g.mask[p]==ab)
                floor[nFloor++]=p;
            else
                roof[nRoof++]=p;
        String cells=corners[0]+", "+corners[1]+", "+corners[2]+", "+corners[3];
        if(nFloor==3){//type 1
            g.eliminate(roof[0],a);
            g.eliminate(roof[0],b);
            return g.apply("unique rectangle","Unique rectangle type 1 on cells "+cells+" with digits "+a+"/"+b+": cell "+roof[0]+" cannot be "+a+" or "+b,Sudoku::postUniqueRectangle);
        }
        if(nFloor!=2)
            return false;
        int r0=roof[0],r1=roof[1];
        int extra0=g.mask[r0]&~ab,extra1=g.mask[r1]&~ab;
        //type 2: both roof cells have the same single extra candidate x
        if(extra0==extra1 && CandidateGrid.count(extra0)==1){
            int x=CandidateGrid.firstDigit(extra0);
            g.eliminateFromCommonPeers(x,r0,r1);
            if(g.apply("unique rectangle","Unique rectangle type 2 on cells "+cells+" with digits "+a+"/"+b+": cell "+r0+" or cell "+r1+" is "+x,Sudoku::postUniqueRectangle))
                return true;
        }
        //type 4: the roof cells share a house in which a (or b) can go only in the roof cells
        if(g.row(r0)==g.row(r1) || g.column(r0)==g.column(r1)){
            for(int k=0;k<3;k++){
                int h=g.housesOf[r0][k];
                if(h!=g.housesOf[r1][k])
                    continue;//not a shared house
                int pair[][]={{a,b},{b,a}};
                for(int u[]:pair){
                    int where[]=g.positions(h,u[0]);
                    if(where.length==2 && ((where[0]==r0 && where[1]==r1) || (where[0]==r1 && where[1]==r0))){
                        g.eliminate(r0,u[1]);
                        g.eliminate(r1,u[1]);
                        if(g.apply("unique rectangle","Unique rectangle type 4 on cells "+cells+" with digits "+a+"/"+b+": in "+g.houseName(h)+" digit "+u[0]+" must go in cell "+r0+" or "+r1+", so neither can be "+u[1],Sudoku::postUniqueRectangle))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  TWO-STRING KITE
    // ------------------------------------------------------------------

    /**
     * TWO-STRING KITE, a relative of the Skyscraper.
     * Digit d can go in exactly two cells of a row (a1, a2) and in exactly two cells of a column (b1, b2), and one end
     * of the row (a1) is in the same box as one end of the column (b1). a1 and b1 cannot both be d, so at least one
     * of the other two ends (a2, b2) is d: d is removed from the cells that see both a2 and b2.
     */
    boolean twoStringKite(){
        for(int d=1;d<=size;d++)
            for(int r=0;r<size;r++){
                int rowCells[]=g.positions(r,d);
                if(rowCells.length!=2 || g.placedIn(r,d))
                    continue;
                for(int c=0;c<size;c++){
                    int colCells[]=g.positions(size+c,d);
                    if(colCells.length!=2 || g.placedIn(size+c,d))
                        continue;
                    for(int i=0;i<2;i++)
                        for(int j=0;j<2;j++){
                            int a1=rowCells[i],a2=rowCells[1-i],b1=colCells[j],b2=colCells[1-j];
                            if(a1==b1 || a1==b2 || a2==b1 || a2==b2 || g.box(a1)!=g.box(b1))
                                continue;
                            g.eliminateFromCommonPeers(d,a2,b2);
                            if(g.apply("two-string kite","Two-string kite on digit "+d+": row "+r+" (cells "+a1+", "+a2+") and column "+c+" (cells "+b1+", "+b2+") are linked in box "+g.box(a1)+", so cell "+a2+" or cell "+b2+" is "+d,Sudoku::postTwoStringKite))
                                return true;
                        }
                }
            }
        return false;
    }

    // ------------------------------------------------------------------
    //  FINNED / SASHIMI FISH
    // ------------------------------------------------------------------

    /**
     * FINNED and SASHIMI X-WING / SWORDFISH, an extension of the basic fish (X-Wing, Swordfish).
     * Take n base lines (rows, or columns) where digit d is not placed and n cover lines in the other direction.
     * The candidates of d in the base lines that are outside the cover lines are the "fins"; they must all be in the
     * same box. Either one fin is d, or (if no fin is d) the base lines form a normal fish and d is removed from the
     * cover lines. Therefore d can be removed from the cells that are in a cover line, outside the base lines and in
     * the box of the fins (they see every fin). The fish is called "sashimi" when a base line has at most one
     * candidate inside the cover lines.
     */
    boolean finnedFish(){
        for(int n=2;n<=3;n++)
            for(int d=1;d<=size;d++)
                for(int orientation=0;orientation<2;orientation++)//0: base lines are rows, 1: base lines are columns
                    if(finnedFish(d,n,orientation))
                        return true;
        return false;
    }
    private boolean finnedFish(int d,int n,int orientation){
        //lineMask[l] = bit mask of the cross indices where d can go in base line l
        int lineMask[]=new int[size];
        int lines[]=new int[size],nLines=0;
        for(int l=0;l<size;l++){
            int h=orientation==0 ? l : size+l;
            if(g.placedIn(h,d))
                continue;
            for(int p:g.houses[h])
                if(g.has(p,d))
                    lineMask[l]|=1<<(orientation==0 ? g.column(p) : g.row(p));
            if(lineMask[l]!=0)
                lines[nLines++]=l;
        }
        int base[]=new int[n];
        return chooseBase(d,n,orientation,lineMask,lines,nLines,base,0,0);
    }
    private boolean chooseBase(int d,int n,int orientation,int lineMask[],int lines[],int nLines,int base[],int chosen,int start){
        if(chosen==n){
            int union=0;
            for(int l:base)
                union|=lineMask[l];
            //every set of n cover lines that leaves at least one fin outside
            for(int cover=0;cover<(1<<size);cover++){
                if(Integer.bitCount(cover)!=n || (union&~cover)==0)
                    continue;
                if(tryFish(d,n,orientation,lineMask,base,cover))
                    return true;
            }
            return false;
        }
        for(int i=start;i<nLines;i++){
            base[chosen]=lines[i];
            if(chooseBase(d,n,orientation,lineMask,lines,nLines,base,chosen+1,i+1))
                return true;
        }
        return false;
    }
    private int cellAt(int orientation,int line,int cross){
        return orientation==0 ? line*size+cross : cross*size+line;
    }
    private boolean tryFish(int d,int n,int orientation,int lineMask[],int base[],int cover){
        int finBox=-1;
        boolean sashimi=false;
        for(int l:base){
            if(Integer.bitCount(lineMask[l]&cover)<=1)
                sashimi=true;
            int fins=lineMask[l]&~cover;
            for(int x=0;x<size;x++)
                if((fins&(1<<x))!=0){
                    int b=g.box(cellAt(orientation,l,x));
                    if(finBox==-1)
                        finBox=b;
                    else if(finBox!=b)
                        return false;//fins in more than one box
                }
        }
        //targets: cells of d in the cover lines, outside the base lines, inside the box of the fins
        for(int x=0;x<size;x++){
            if((cover&(1<<x))==0)
                continue;
            for(int l=0;l<size;l++){
                boolean isBase=false;
                for(int bl:base)
                    if(bl==l)
                        isBase=true;
                int p=cellAt(orientation,l,x);
                if(isBase==false && g.box(p)==finBox)
                    g.eliminate(p,d);
            }
        }
        if(g.hasEliminations()==false)
            return false;
        String name=(sashimi ? "Sashimi " : "Finned ")+(n==2 ? "X-Wing" : "Swordfish");
        StringBuilder text=new StringBuilder(name+" on digit "+d+": base "+(orientation==0 ? "rows" : "columns"));
        for(int l:base)
            text.append(" ").append(l);
        text.append(", cover ").append(orientation==0 ? "columns" : "rows");
        for(int x=0;x<size;x++)
            if((cover&(1<<x))!=0)
                text.append(" ").append(x);
        text.append(", fins in box ").append(finBox);
        return g.apply("finned fish",text.toString(),Sudoku::postFinnedFish);
    }

    // ------------------------------------------------------------------
    //  W-WING
    // ------------------------------------------------------------------

    /**
     * W-WING.
     * Two bivalue cells P and Q with the same candidates {x,y} that do not see each other, and a strong link on x
     * (a house where x can go only in two cells L1 and L2) with L1 seeing P and L2 seeing Q.
     * If P were not y it would be x, so L1 would not be x, L2 would be x and Q would be y: P or Q is y,
     * and y is removed from the cells that see both P and Q.
     */
    boolean wWing(){
        for(int p=0;p<cellCount;p++){
            if(CandidateGrid.count(g.mask[p])!=2)
                continue;
            for(int q=p+1;q<cellCount;q++){
                if(g.mask[q]!=g.mask[p] || g.sees[p][q])
                    continue;
                int ds[]=CandidateGrid.digits(g.mask[p]);
                for(int k=0;k<2;k++){
                    int x=ds[k],y=ds[1-k];//x: digit of the strong link, y: digit to remove
                    for(int h=0;h<3*size;h++){
                        int link[]=g.positions(h,x);
                        if(link.length!=2 || link[0]==p || link[0]==q || link[1]==p || link[1]==q)
                            continue;
                        boolean connected=(g.sees[link[0]][p] && g.sees[link[1]][q]) || (g.sees[link[0]][q] && g.sees[link[1]][p]);
                        if(connected==false)
                            continue;
                        g.eliminateFromCommonPeers(y,p,q);
                        if(g.apply("w-wing","W-Wing: cells "+p+" and "+q+" with candidates "+x+"/"+y+" linked by the strong link on "+x+" in "+g.houseName(h)+" (cells "+link[0]+", "+link[1]+"), so cell "+p+" or cell "+q+" is "+y,Sudoku::postWWing))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  XYZ-WING
    // ------------------------------------------------------------------

    /**
     * XYZ-WING, the natural extension of the XY-Wing.
     * A pivot with three candidates {x,y,z} sees two bivalue pincers {x,z} and {y,z}. Whatever the pivot is,
     * one of the three cells is z, so z is removed from the cells that see the pivot and both pincers.
     */
    boolean xyzWing(){
        for(int pivot=0;pivot<cellCount;pivot++){
            int pm=g.mask[pivot];
            if(CandidateGrid.count(pm)!=3)
                continue;
            for(int a:g.peers[pivot]){
                int am=g.mask[a];
                if(CandidateGrid.count(am)!=2 || (am&~pm)!=0)
                    continue;
                for(int b:g.peers[pivot]){
                    int bm=g.mask[b];
                    if(b<=a || CandidateGrid.count(bm)!=2 || (bm&~pm)!=0 || bm==am)
                        continue;
                    int z=CandidateGrid.firstDigit(am&bm);
                    g.eliminateFromCommonPeers(z,pivot,a,b);
                    if(g.apply("xyz-wing","XYZ-Wing: pivot "+pivot+" with pincers "+a+" and "+b+": one of them is "+z,Sudoku::postXyzWing))
                        return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  MULTI-COLORING
    // ------------------------------------------------------------------

    /**
     * MULTI-COLORING, an extension of Simple Coloring that uses two or more clusters of conjugate pairs.
     * Each cluster (cells of digit d connected by conjugate pairs) is coloured with two colours, exactly one of which
     * is true. Given two clusters with colours (A1,B1) and (A2,B2):
     * <ul>
     *   <li>if a cell of colour A1 sees a cell of colour A2, A1 and A2 cannot both be true, so B1 or B2 is true and
     *       d is removed from the cells that see both a B1 and a B2 cell;</li>
     *   <li>if colour A1 sees both colours of the other cluster, A1 is false and d is removed from all its cells.</li>
     * </ul>
     */
    boolean multiColoring(){
        for(int d=1;d<=size;d++)
            if(multiColoring(d))
                return true;
        return false;
    }
    private boolean multiColoring(int d){
        //conjugate pairs of d
        boolean linked[][]=new boolean[cellCount][cellCount];
        for(int h=0;h<3*size;h++){
            int where[]=g.positions(h,d);
            if(where.length==2 && g.placedIn(h,d)==false){
                linked[where[0]][where[1]]=true;
                linked[where[1]][where[0]]=true;
            }
        }
        //clusters: cluster[p] = index of the cluster of p (-1 if none), color[p] = 0 or 1
        int cluster[]=new int[cellCount],color[]=new int[cellCount],nClusters=0;
        java.util.Arrays.fill(cluster,-1);
        for(int s=0;s<cellCount;s++){
            boolean hasLink=false;
            for(int q=0;q<cellCount;q++)
                if(linked[s][q])
                    hasLink=true;
            if(cluster[s]!=-1 || hasLink==false)
                continue;
            int queue[]=new int[cellCount],n=0;
            queue[n++]=s;
            cluster[s]=nClusters;
            color[s]=0;
            for(int k=0;k<n;k++)
                for(int q=0;q<cellCount;q++)
                    if(linked[queue[k]][q] && cluster[q]==-1){
                        cluster[q]=nClusters;
                        color[q]=1-color[queue[k]];
                        queue[n++]=q;
                    }
            nClusters++;
        }
        for(int c1=0;c1<nClusters;c1++)
            for(int c2=0;c2<nClusters;c2++){
                if(c1==c2)
                    continue;
                for(int i=0;i<2;i++){
                    boolean seesColor[]=new boolean[2];//which colours of c2 are seen by colour i of c1
                    for(int p=0;p<cellCount;p++)
                        if(cluster[p]==c1 && color[p]==i)
                            for(int q:g.peers[p])
                                if(cluster[q]==c2)
                                    seesColor[color[q]]=true;
                    //colour i of c1 sees both colours of c2: it is false
                    if(seesColor[0] && seesColor[1]){
                        for(int p=0;p<cellCount;p++)
                            if(cluster[p]==c1 && color[p]==i)
                                g.eliminate(p,d);
                        if(g.apply("multi-coloring","Multi-coloring on digit "+d+": a colour of cluster "+c1+" sees both colours of cluster "+c2+", so it is false",Sudoku::postMultiColoring))
                            return true;
                    }
                    for(int j=0;j<2;j++){
                        if(seesColor[j]==false || c2<c1)
                            continue;
                        //colours i of c1 and j of c2 exclude each other: the opposite colours (1-i of c1, 1-j of c2) are not both false
                        for(int q=0;q<cellCount;q++){
                            if(g.has(q,d)==false || cluster[q]==c1 || cluster[q]==c2)
                                continue;
                            boolean seesFirst=false,seesSecond=false;
                            for(int r:g.peers[q]){
                                if(cluster[r]==c1 && color[r]==1-i)
                                    seesFirst=true;
                                if(cluster[r]==c2 && color[r]==1-j)
                                    seesSecond=true;
                            }
                            if(seesFirst && seesSecond)
                                g.eliminate(q,d);
                        }
                        if(g.apply("multi-coloring","Multi-coloring on digit "+d+": clusters "+c1+" and "+c2+" are linked, so one of their opposite colours is true",Sudoku::postMultiColoring))
                            return true;
                    }
                }
            }
        return false;
    }
}
