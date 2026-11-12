package sudoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Chain based techniques that work on a {@link CandidateGrid} snapshot:
 * X-Cycles, Alternating Inference Chains (AIC), ALS-XZ and Forcing Chains.
 *
 * <h2>Links between candidates</h2>
 * A candidate is a pair (cell, digit), written in Eureka notation, e.g. (5)r1c3.
 * <ul>
 *   <li>STRONG link: at least one of the two candidates is true. It exists between the two candidates of a bivalue
 *       cell and between the two cells of a house where a digit can go only twice (conjugate pair).
 *       So "A is false" implies "B is true".</li>
 *   <li>WEAK link: at most one of the two candidates is true. It exists between two candidates of the same cell and
 *       between the same digit in two cells that see each other. So "A is true" implies "B is false".</li>
 * </ul>
 * An Alternating Inference Chain starts with a strong link, alternates strong and weak links and ends with a strong
 * link: if the first candidate is false the last one is true, so at least one of the two ends is true.
 *
 * A new Chains object must be created for every call, because the snapshot is taken in the constructor.
 */
class Chains {
    private final CandidateGrid g;
    private final int size,cellCount,nodeCount;

    Chains(Sudoku sudoku){
        g=new CandidateGrid(sudoku);
        size=g.size;
        cellCount=g.cellCount;
        nodeCount=cellCount*size;
    }

    // ------------------------------------------------------------------
    //  AIC ENGINE (used by X-Cycles and AIC)
    // ------------------------------------------------------------------

    private int node(int p,int digit){
        return p*size+digit-1;
    }
    private int cellOf(int node){
        return node/size;
    }
    private int digitOf(int node){
        return node%size+1;
    }

    private int strong[][],weak[][];//strong[n] / weak[n]: candidates linked to candidate n

    /**
     * Builds the strong and weak links between the candidates.
     * @param singleDigit true for X-Cycles: only links between cells with the same digit are used
     */
    private void buildLinks(boolean singleDigit){
        strong=new int[nodeCount][];
        weak=new int[nodeCount][];
        for(int p=0;p<cellCount;p++)
            for(int d=1;d<=size;d++){
                if(g.has(p,d)==false)
                    continue;
                List<Integer> s=new ArrayList<>(),w=new ArrayList<>();
                if(singleDigit==false){
                    if(CandidateGrid.count(g.mask[p])==2)//bivalue cell: strong link between its two candidates
                        s.add(node(p,CandidateGrid.firstDigit(g.mask[p]&~CandidateGrid.bit(d))));
                    for(int e:CandidateGrid.digits(g.mask[p]))//candidates of the same cell: weak links
                        if(e!=d)
                            w.add(node(p,e));
                }
                for(int h:g.housesOf[p]){//conjugate pairs: strong links
                    int where[]=g.positions(h,d);
                    if(where.length==2){
                        int other=node(where[0]==p ? where[1] : where[0],d);
                        if(s.contains(other)==false)
                            s.add(other);
                    }
                }
                for(int q:g.peers[p])//the same digit in a peer: weak link
                    if(g.has(q,d))
                        w.add(node(q,d));
                strong[node(p,d)]=s.stream().mapToInt(Integer::intValue).toArray();
                weak[node(p,d)]=w.stream().mapToInt(Integer::intValue).toArray();
            }
    }
    /**
     * Searches for an AIC whose eliminations remove at least one candidate, and applies the first one found.
     * For every starting candidate S a breadth first search follows the implications "S is false": strong links lead
     * from false candidates to true ones, weak links from true candidates to false ones. Every candidate E that is
     * reached as TRUE (with a chain of at least 3 links) satisfies "S or E is true", which gives:
     * <ul>
     *   <li>same digit in two cells: the digit is removed from the cells that see both;</li>
     *   <li>two digits of the same cell: every other candidate is removed from the cell;</li>
     *   <li>different digits in two cells that see each other: each cell loses the digit of the other end;</li>
     *   <li>E is S itself (discontinuous loop): S is true, every other candidate of its cell is removed.</li>
     * </ul>
     */
    private boolean searchAic(boolean singleDigit,String technique,String name,CandidateGrid.PostTechnique post){
        buildLinks(singleDigit);
        int parent[]=new int[2*nodeCount],depth[]=new int[2*nodeCount],queue[]=new int[2*nodeCount];
        int stamp[]=new int[2*nodeCount],currentStamp=0;//stamp[state]==currentStamp means visited in this search
        for(int start=0;start<nodeCount;start++){
            if(strong[start]==null || strong[start].length==0)
                continue;
            currentStamp++;
            int head=0,tail=0;
            int s0=2*start;//state = 2*node + truth (0 false, 1 true)
            queue[tail++]=s0;
            stamp[s0]=currentStamp;
            parent[s0]=-1;
            depth[s0]=0;
            while(head<tail){
                int state=queue[head++];
                int n=state/2,truth=state%2;
                int next[]=truth==0 ? strong[n] : weak[n];
                for(int m:next){
                    int ns=2*m+(1-truth);
                    if(stamp[ns]==currentStamp)
                        continue;
                    stamp[ns]=currentStamp;
                    parent[ns]=state;
                    depth[ns]=depth[state]+1;
                    queue[tail++]=ns;
                    if(ns%2==1 && depth[ns]>=3 && aicEliminations(start,m)){
                        String chain=describeChain(ns,parent);
                        if(g.apply(technique,name+": "+chain,post))
                            return true;
                    }
                }
            }
        }
        return false;
    }
    /** Records the eliminations implied by "start or end is true"; returns true if there is at least one. */
    private boolean aicEliminations(int start,int end){
        int ps=cellOf(start),ds=digitOf(start),pe=cellOf(end),de=digitOf(end);
        if(start==end){//discontinuous loop: start is true
            for(int e:CandidateGrid.digits(g.mask[ps]))
                if(e!=ds)
                    g.eliminate(ps,e);
        }else if(ds==de)
            g.eliminateFromCommonPeers(ds,ps,pe);
        else if(ps==pe){
            for(int e:CandidateGrid.digits(g.mask[ps]))
                if(e!=ds && e!=de)
                    g.eliminate(ps,e);
        }else if(g.sees[ps][pe]){
            g.eliminate(ps,de);
            g.eliminate(pe,ds);
        }
        return g.hasEliminations();
    }
    /** @return the chain in Eureka notation, e.g. (5)r1c2=(5)r1c7-(5)r3c8=(5)r3c1 ("=" strong, "-" weak link) */
    private String describeChain(int endState,int parent[]){
        List<String> parts=new ArrayList<>();
        for(int s=endState;s!=-1;s=parent[s]){
            int n=s/2;
            String text=g.node(cellOf(n),digitOf(n));
            if(parent[s]!=-1)
                text=(s%2==1 ? "=" : "-")+text;//arriving at a true candidate means a strong link
            parts.add(0,text);
        }
        return String.join("",parts);
    }

    /**
     * X-CYCLES (X-Chains): AICs that use a single digit, i.e. only conjugate pairs as strong links and
     * peers as weak links. They generalise the Skyscraper, the Two-String Kite and Simple Coloring.
     */
    boolean xCycles(){
        return searchAic(true,"x-cycle","X-Cycle",Sudoku::postXCycles);
    }
    /**
     * ALTERNATING INFERENCE CHAINS with any digits: they generalise X-Cycles, XY-Chains and Remote Pairs.
     */
    boolean aic(){
        return searchAic(false,"aic","AIC",Sudoku::postAic);
    }

    // ------------------------------------------------------------------
    //  ALS-XZ
    // ------------------------------------------------------------------

    /** An Almost Locked Set: N cells of one house with N+1 candidates in total. */
    private static class Als{
        int cells[];
        long lo,hi;         //the cells as a 81 bit set (lo: positions 0..63, hi: 64..80)
        int mask;           //union of the candidates
        long dlo[],dhi[];   //dlo[d]/dhi[d]: cells of the ALS that contain digit d
        String house;
    }
    private long peersLo[],peersHi[];

    /**
     * ALS-XZ.
     * Two non overlapping Almost Locked Sets A and B (N cells with N+1 candidates each) share a restricted common
     * candidate x: every cell of A containing x sees every cell of B containing x, so x can be true in at most one of
     * them. Then at least one of A and B becomes a locked set without x: for any other common candidate z, z must be
     * in A or in B, so z is removed from the cells (outside A and B) that see all the z cells of both sets.
     * ALS of 1 to 4 cells are considered (a bivalue cell is an ALS of one cell).
     */
    boolean alsXz(){
        peersLo=new long[cellCount];
        peersHi=new long[cellCount];
        for(int p=0;p<cellCount;p++)
            for(int q:g.peers[p])
                if(q<64)
                    peersLo[p]|=1L<<q;
                else
                    peersHi[p]|=1L<<(q-64);
        List<Als> sets=findAls(4);
        for(int i=0;i<sets.size();i++)
            for(int j=i+1;j<sets.size();j++){
                Als a=sets.get(i),b=sets.get(j);
                if((a.lo&b.lo)!=0 || (a.hi&b.hi)!=0)
                    continue;//overlapping sets
                int common=a.mask&b.mask;
                if(CandidateGrid.count(common)<2)
                    continue;
                for(int x:CandidateGrid.digits(common)){
                    if(restricted(a,b,x)==false)
                        continue;
                    for(int z:CandidateGrid.digits(common)){
                        if(z==x)
                            continue;
                        long tlo=~0L,thi=~0L;//cells that see every z cell of A and B
                        for(int p=0;p<cellCount;p++)
                            if(inSet(a.dlo[z],a.dhi[z],p) || inSet(b.dlo[z],b.dhi[z],p)){
                                tlo&=peersLo[p];
                                thi&=peersHi[p];
                            }
                        for(int q=0;q<cellCount;q++)
                            if(inSet(tlo,thi,q) && inSet(a.lo,a.hi,q)==false && inSet(b.lo,b.hi,q)==false)
                                g.eliminate(q,z);
                    }
                    if(g.apply("als-xz","ALS-XZ: A = cells "+cellList(a)+" in "+a.house+", B = cells "+cellList(b)+" in "+b.house+", restricted common candidate "+x,Sudoku::postAlsXz))
                        return true;
                }
            }
        return false;
    }
    private static boolean inSet(long lo,long hi,int p){
        return p<64 ? (lo&(1L<<p))!=0 : (hi&(1L<<(p-64)))!=0;
    }
    private static String cellList(Als als){
        StringBuilder text=new StringBuilder();
        for(int p:als.cells)
            text.append(text.length()==0 ? "" : ",").append(p);
        return text.toString();
    }
    /** @return true if every x cell of a sees every x cell of b */
    private boolean restricted(Als a,Als b,int x){
        for(int p:a.cells)
            if(g.has(p,x) && ((b.dlo[x]&~peersLo[p])!=0 || (b.dhi[x]&~peersHi[p])!=0))
                return false;
        return true;
    }
    /** @return all the Almost Locked Sets with at most maxCells cells (without duplicates) */
    private List<Als> findAls(int maxCells){
        List<Als> result=new ArrayList<>();
        HashSet<String> seen=new HashSet<>();
        for(int h=0;h<3*size;h++){
            int empty[]=new int[size],n=0;
            for(int p:g.houses[h])
                if(g.mask[p]!=0)
                    empty[n++]=p;
            //every subset of the empty cells of the house, coded as a bit mask on empty[]
            for(int subset=1;subset<(1<<n);subset++){
                int k=Integer.bitCount(subset);
                if(k>maxCells || k>=n)
                    continue;
                int mask=0;
                long lo=0,hi=0;
                int cells[]=new int[k],c=0;
                for(int i=0;i<n;i++)
                    if((subset&(1<<i))!=0){
                        int p=empty[i];
                        cells[c++]=p;
                        mask|=g.mask[p];
                        if(p<64)
                            lo|=1L<<p;
                        else
                            hi|=1L<<(p-64);
                    }
                if(CandidateGrid.count(mask)!=k+1 || seen.add(lo+":"+hi)==false)
                    continue;
                Als als=new Als();
                als.cells=cells;
                als.lo=lo;
                als.hi=hi;
                als.mask=mask;
                als.house=g.houseName(h);
                als.dlo=new long[size+1];
                als.dhi=new long[size+1];
                for(int p:cells)
                    for(int d:CandidateGrid.digits(g.mask[p]))
                        if(p<64)
                            als.dlo[d]|=1L<<p;
                        else
                            als.dhi[d]|=1L<<(p-64);
                result.add(als);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    //  FORCING CHAINS
    // ------------------------------------------------------------------

    /**
     * Propagation of an assumption with the simplest logic only (Naked Single and Hidden Single):
     * placing a digit removes it from the peers, a cell left with one candidate gets it, a digit left with one place
     * in a house goes there. This is equivalent to following all the chains of implications that start from the
     * assumption.
     */
    private class Propagation{
        int m[];            //candidates of every cell (filled cells have their digit as only candidate)
        boolean done[];     //true if the digit of the cell is known
        int queue[];        //cells whose digit has been fixed and not yet removed from the peers
        int head,tail;
        boolean ok=true;    //false as soon as a contradiction is found

        Propagation(){
            m=new int[cellCount];
            done=new boolean[cellCount];
            queue=new int[cellCount];
            for(int p=0;p<cellCount;p++){
                done[p]=g.value[p]!=0;
                m[p]=done[p] ? CandidateGrid.bit(g.value[p]) : g.mask[p];
            }
        }
        void assign(int p,int d){
            if((m[p]&CandidateGrid.bit(d))==0){
                ok=false;
                return;
            }
            if(done[p])
                return;
            m[p]=CandidateGrid.bit(d);
            done[p]=true;
            queue[tail++]=p;
        }
        /** @return this, or null if the assumption leads to a contradiction */
        Propagation run(){
            while(ok){
                while(head<tail && ok){
                    int p=queue[head++],d=CandidateGrid.firstDigit(m[p]);
                    for(int q:g.peers[p]){
                        if((m[q]&CandidateGrid.bit(d))==0)
                            continue;
                        if(done[q]){//two peers with the same digit
                            ok=false;
                            break;
                        }
                        m[q]&=~CandidateGrid.bit(d);
                        if(m[q]==0)
                            ok=false;
                        else if(CandidateGrid.count(m[q])==1)
                            assign(q,CandidateGrid.firstDigit(m[q]));//naked single
                    }
                }
                if(ok==false)
                    break;
                boolean progress=false;
                for(int h=0;h<3*size && ok;h++)
                    for(int d=1;d<=size && ok;d++){
                        int where=-1,count=0;
                        for(int p:g.houses[h])
                            if((m[p]&CandidateGrid.bit(d))!=0){
                                where=p;
                                count++;
                            }
                        if(count==0)
                            ok=false;//the digit cannot go anywhere in the house
                        else if(count==1 && done[where]==false){
                            assign(where,d);//hidden single
                            progress=true;
                        }
                    }
                if(progress==false)
                    break;
            }
            return ok ? this : null;
        }
    }
    private Propagation assume(int p,int d){
        Propagation propagation=new Propagation();
        propagation.assign(p,d);
        return propagation.run();
    }

    /**
     * FORCING CHAINS, the logical version of the Forcing Net ("ipotesi"): instead of solving a copy of the sudoku with
     * every technique, each assumption is propagated only with Naked and Hidden Singles.
     * <ul>
     *   <li>CELL forcing chains: every candidate of a cell is assumed in turn (one of them is true);</li>
     *   <li>DIGIT (unit) forcing chains: every position of a digit in a house is assumed in turn (one of them is true).</li>
     * </ul>
     * If an assumption leads to a contradiction, that candidate is removed. Otherwise every candidate that is
     * removed by ALL the assumptions is false and is removed (this includes digits placed by all the assumptions).
     * Cells with fewer candidates and houses with fewer positions are tried first.
     */
    boolean forcingChains(){
        for(int n=2;n<=size;n++){
            for(int p=0;p<cellCount;p++)
                if(g.mask[p]!=0 && CandidateGrid.count(g.mask[p])==n){
                    int ds[]=CandidateGrid.digits(g.mask[p]);
                    int cells[]=new int[n];
                    java.util.Arrays.fill(cells,p);
                    if(forcing(cells,ds,"cell "+p))
                        return true;
                }
            for(int h=0;h<3*size;h++)
                for(int d=1;d<=size;d++){
                    if(g.placedIn(h,d))
                        continue;
                    int where[]=g.positions(h,d);
                    if(where.length!=n)
                        continue;
                    int ds[]=new int[n];
                    java.util.Arrays.fill(ds,d);
                    if(forcing(where,ds,"digit "+d+" in "+g.houseName(h)))
                        return true;
                }
        }
        return false;
    }
    /**
     * Tries the assumptions "cells[k] is digits[k]" (exactly one of them is true).
     */
    private boolean forcing(int cells[],int digits[],String source){
        int n=cells.length;
        Propagation results[]=new Propagation[n];
        boolean contradiction=false,allContradictions=true;
        for(int k=0;k<n;k++){
            results[k]=assume(cells[k],digits[k]);
            if(results[k]==null)
                contradiction=true;
            else
                allContradictions=false;
        }
        if(contradiction){
            StringBuilder text=new StringBuilder("Forcing chains from "+source+":");
            for(int k=0;k<n;k++)
                if(results[k]==null){
                    g.eliminate(cells[k],digits[k]);
                    text.append(" placing ").append(digits[k]).append(" in cell ").append(cells[k]).append(" leads to a contradiction;");
                }
            if(allContradictions)//the sudoku has no solution: the cell(s) lose all the candidates, solve() will notice it
                return g.apply("forcing chains",text+" the sudoku has no solution",(number,changed,grid)->{});
            return g.apply("forcing chains",text.toString(),Sudoku::postForcingChains);
        }
        for(int q=0;q<cellCount;q++)
            for(int e:CandidateGrid.digits(g.mask[q])){
                boolean removedByAll=true;
                for(int k=0;k<n && removedByAll;k++)
                    if((results[k].m[q]&CandidateGrid.bit(e))!=0)
                        removedByAll=false;
                if(removedByAll)
                    g.eliminate(q,e);
            }
        return g.apply("forcing chains","Forcing chains from "+source+": every possibility leads to the same eliminations",Sudoku::postForcingChains);
    }
}
