package Sudoku;
import Metodi.Metodi;
import Enum.Enum;
public class Gruppo{//PUO ESSERE UNA SETTORE O ALTRO

    protected int LATO;
    protected int lato;
    private Cella celle[];

    public int get_LATO(){
        return LATO;
    }
    public int get_lato(){
        return lato;
    }
    public void set_LATO(int LATO){
        this.LATO=LATO;
        lato=(int)Math.sqrt(LATO);
    }
    public void set_celle(){
        celle=new Cella[LATO];
    }
    public void set_celle(Cella vet_celle[]){
        celle=new Cella[vet_celle.length];
        for(int i=0;i<vet_celle.length;i++)
            set_cella(vet_celle[i],i);
    }
    public void set_cella(Cella cella,int index){
        celle[index]=cella;
    }
    public void set_cella(int posiz,int index){
        celle[index]=new Cella(posiz,get_LATO());
    }
    public Cella get_cella(int i){
        return celle[i];
    }
    /**
     * Il metodo restituisce un vettore formato dalle celle del gruppo i cui candidati sono esattamente quelli di vet_num
     * @param vet_num
     * @param inutile
     * @return
     */
    public Cella[] get_celle_se_candidati(int vet_num[],int inutile){
        Cella celle[]=new Cella[LATO];
        int cont=0;
        for(int i=0;i<celle.length;i++)
            if(get_cella(i).se_candidati(vet_num))
                celle[cont++]=get_cella(i);
        return Metodi.accorcio_lunghezza(celle, cont);
    }
    /**
     * restituisce un vettore formato dagli indici delle celle i cui candidati sono esattamente quelli di vet_num
     * @param vet_num
     * @return
     */
    public int[] get_celle_se_candidati(int vet_num[]){
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<celle.length;i++)
            if(get_cella(i).se_candidati(vet_num))
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet, cont);
    }
    public Cella[] get_cella_se_candidati(int quantita,boolean inutile){//ritorna le celle del gruppo il cui numero di candidati è quantità
        Cella vet[]=new Cella[celle.length];
        int cont=0;
        for(int i=0;i<celle.length;i++)
            if(celle[i].get_candidati().length==quantita)
                vet[cont++]=celle[i];
        return Metodi.accorcio_lunghezza(vet,cont);
                
    }
    public int[] get_celle_se_candidati(int quantita){
        int vet[]=new int[celle.length],cont=0;
        for(int i=0;i<celle.length;i++)
            if(celle[i].get_candidati().length==quantita)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet, cont);
    }

    public Cella[] get_celle(){
        return celle;
    }
    public void set_numero(int numero,int i){//mette nell'i-esima cella del gruppo la q.tità numero
        rimuovi_candidato(numero);
        get_cella(i).set_numero(numero);
    }
    public int[] rimuovi_candidato(int numero){//ritorna true se effettivamente ha rimosso qualcosa
        //toglie per ogni cella del settore il numero numero dai candidati della cella
        int vet[]=new int[get_LATO()];
        for(int i=0;i<celle.length;i++)
            vet[i]=i;
        return rimuovi_candidato(numero,vet);
    }
    public int[] rimuovi_candidato(int numero,int vet[]){//ritorna true se effettivamente ha rimosso qualcosa, vet contiene gli indici delle celle del gruppo dalle quali si vuole rimuovere numero
        int vettore[]=new int[get_LATO()],cont=0;//le celle di vettore contengono gli indici delle celle dalle quali si è tolto numero dai candidati
        for(int i=0;i<vet.length;i++)
            if(get_cella(vet[i]).rimuovi_candidato(numero))
                vettore[cont++]=vet[i];
        return Metodi.accorcio_lunghezza(vettore,cont);
    }
    public Gruppo(){
        celle=new Cella[0];
    }
    public Gruppo(Cella cella){//Il lato è sottointeso
        set_LATO(cella.get_LATO());
        aggiungi_cella(cella);
    }
    public Gruppo(Cella celle[]){
        set_LATO(celle[0].get_LATO());
        aggiungi_celle(celle);
    }

    //METODI DI CLASSE
    public int conta_numero(int numero){//VERIFICATA//conta quante volte numero può andare nel settore
        int cont=0;
        for(int i=0;i<LATO;i++){
            if(celle[i].ci_puo_andare(numero))
                cont++;
            if(celle[i].get_numero()==numero){
                cont=1;
                i=LATO-1;
            }
        }
        return cont;
    }

    public int[] conta_numero(int numero,int inutile){//mette 1 in vet nelle celle in cui puo andare numero altrimenti mette 0
        //inutile serve solo a distinguerlo dal metodo precedente
        int cont=0,vet[]=new int[LATO];
        for(int i=0;i<LATO;i++){
            if(celle[i].ci_puo_andare(numero)){
                cont++;
                vet[i]=1;
            }
            else
                vet[i]=0;
            if(celle[i].get_numero()==numero){
                vet[i]=1;
                cont=1;
                i=LATO-1;
            }
        }
        return vet;
    }
    public int[] quali_numeri(){//ritorna unn vettore formato dai numeri che possono andare nel settore
        int vet[]=new int[LATO],cont=0;
        for(int i=1;i<=LATO;i++)
            if(ce_numero(i)==false)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet, cont);
    }
    public int[] dove_numero(int numero){// ritorna un vettore vet che contiene gli indici (non la posizione) delle celle del settore dove puo andare numero
        int vet_num[]=new int[1];
        vet_num[0]=numero;
        return dove_numeri(vet_num);
    }
    public Cella[] dove_numero(int numero,int inutile){//inutile serve a distinguere questo metodo dal precedente
        int vet_num[]=new int[1];
        vet_num[0]=numero;
        return dove_numeri(vet_num,inutile);
    }
    public int[] dove_numeri(int vet_num[]){//ritorna un vettore con gli indici(e non le posizioni) delle celle del settore dove puo andare almeno un numero di vet_num
        int cont=0,vet[][]=new int[vet_num.length][0],somma[]=new int[LATO];//la riga i-esima di vet contiene conta_numero(vet_num[i])
        for(int i=0;i<vet.length;i++)
            vet[i]=conta_numero(vet_num[i],0);
        for(int i=0;i<LATO;i++){
            somma[i]=0;
            for(int j=0;j<vet_num.length;j++)
                somma[i]=somma[i]+vet[j][i];
        }
        int vet_ritornato[]=new int[LATO];
        for(int i=0;i<LATO;i++)
            if(somma[i]>0)
                vet_ritornato[cont++]=i;
        int temp[]=new int [cont];
        for(int i=0;i<cont;i++)
            temp[i]=vet_ritornato[i];
        return temp;
    }
    public Cella[] dove_numeri(int vet_num[],int inutile){//Ritorna le celle del settore dove puo andare almeno un numero di vet_num
        int vet_celle[]=dove_numeri(vet_num);
        Cella celle[]=new Cella[vet_celle.length];
        for(int i=0;i<celle.length;i++)
            celle[i]=get_cella(vet_celle[i]);
        return celle;
    }
    public boolean ce_numero(int numero){
        for(int i=0;i<LATO;i++)
            if(celle[i].get_numero()==numero)
                return true;
        return false;
    }
    public boolean assurdo(){
        for(int i=0;i<celle.length;i++)
            if(celle[i].get_candidati().length==0){
                System.out.println("Il sudoku è assurdo perche' alla cella "+celle[i].get_posiz()+" non ci possono andare numeri");
                return true;
            }
        for(int i=0;i<celle.length-1;i++)
            if(celle[i].get_numero()!=0)
                for(int j=i+1;j<celle.length;j++)
                    if(celle[i].get_numero()==celle[j].get_numero()){
                        System.out.println("Il sudoku è assurdo perche' nel gruppo il numero "+celle[i].get_numero()+" ci puo andare due volte");
                        return true;
                    }
        return false;
    }
    
    public void aggiungi_cella(Cella cella){
        if(cella==null || Metodi.ce_elemento(celle,cella))
            return;
        celle=Metodi.aggiungo_cella_al_vettore(celle,cella);
        for(int i=0;i<celle.length;i++)
            celle[i].set_celle_gruppo(celle);
        for(int i=0;i<celle.length-1;i++)
            if(celle[i].get_riga()==cella.get_riga() || celle[i].get_colonna()==cella.get_colonna() || celle[i].get_tabella()==cella.get_tabella()){
                if(Metodi.ce_elemento(celle[i].get_celle_gruppo_viste(),cella)==false)
                    celle[i].set_celle_gruppo_viste(Metodi.aggiungo_cella_al_vettore(celle[i].get_celle_gruppo_viste(),cella));
                if(Metodi.ce_elemento(cella.get_celle_gruppo_viste(),celle[i])==false)
                    cella.set_celle_gruppo_viste(Metodi.aggiungo_cella_al_vettore(cella.get_celle_gruppo_viste(),celle[i]));
            }
    }
    public void aggiungi_celle(Cella vet_celle[]){//aggiunge le celle di vet_celle in this.celle e aggiorna celle_gruppo, celle_gruppo_comune
        //il metodo dovrebbe controllare se la cella che si vuole inserire non si trova già nel gruppo
        if(celle==null){
            celle=vet_celle;
            return;
        }
        for(int i=0;i<celle.length;i++)
            aggiungi_cella(vet_celle[i]);
    }
    public void risolvo(Cella sudoku[][]){
        for(int i=0;i<celle.length;i++)
            if(celle[i].get_candidati().length==1)
                return;
    }
    public boolean ce_cella(Cella cella){
        for(int i=0;i<celle.length;i++)
            if(celle[i]==cella)
                return true;
        return false;
    }
    public void stampo(){
        for(int i=0;i<celle.length;i++){
            System.out.print("Cella "+i+" "+celle[i].get_posiz()+".Celle viste: ");
            for(int j=0;j<celle[i].get_celle_gruppo_viste().length;j++)
                System.out.print(celle[i].get_cella_gruppo_vista(j).get_posiz()+" ");
            System.out.println("");
        }
    }
    public void distruggo_gruppo(){//quando un gruppo non si deve più usare bisogna inizializzare le celle_gruppo_viste di ogni sua cella
        for(int i=0;i<celle.length;i++){
            celle[i].set_celle_gruppo(new Cella[0]);
            celle[i].set_celle_gruppo_viste(new Cella[0]);
        }
    }
    //TECNICHE RISOLUTIVE
    
    private Cella celle_modificate[]=new Cella[0];//celle da cui xy_wing_generalizzato() ha rimosso numero_rimosso
    private int numero_rimosso=0;
    public Cella[] get_celle_modificate(){
        return celle_modificate;
    }
    public int get_numero_rimosso(){
        return numero_rimosso;
    }
    /**
     * Xy-wing generalizzato (catena xy) sul gruppo creato da Cella.creo_catena_xy_wing().
     * Si parte da una cella inizio con candidati {x,a} e si suppone che inizio non sia x: allora inizio vale a,
     * una cella vista {a,b} vale b, una cella vista {b,c} vale c, ecc. Se si arriva ad una cella fine che vale x
     * allora o inizio o fine contiene x, quindi x non puo' andare nelle celle viste sia da inizio che da fine.
     * L'xy-wing e' la catena di tre celle (ala-perno-ala).
     * Le celle della catena devono vedersi (celle_gruppo_viste) e avere due candidati.
     * @return true se ha rimosso un candidato (si ferma alla prima applicazione)
     */
    public boolean xy_wing_generalizzato(Cella sudoku[][]){
        celle_modificate=new Cella[0];
        numero_rimosso=0;
        for(int i=0;i<celle.length;i++){
            Cella inizio=celle[i];
            if(inizio.get_numero()!=0 || inizio.get_candidati().length!=2)
                continue;
            for(int c=0;c<2;c++){
                int x=inizio.get_candidati()[c];
                //visita in ampiezza: coda[k] vale forzato[k] se inizio non vale x; lunghezza[k] e' il numero di celle della catena
                Cella coda[]=new Cella[2*celle.length];
                int forzato[]=new int[2*celle.length],lunghezza[]=new int[2*celle.length];
                boolean visitato[][]=new boolean[LATO*LATO][LATO+1];
                int n=0;
                coda[n]=inizio;
                forzato[n]=inizio.get_candidati()[1-c];
                lunghezza[n++]=1;
                visitato[inizio.get_posiz()][forzato[0]]=true;
                for(int k=0;k<n;k++){
                    Cella viste[]=coda[k].get_celle_gruppo_viste();
                    if(viste==null)
                        continue;
                    for(int h=0;h<viste.length;h++){
                        Cella cella=viste[h];
                        if(cella==inizio || cella.get_numero()!=0 || cella.get_candidati().length!=2 || cella.ci_puo_andare(forzato[k])==false)
                            continue;
                        int altro=cella.get_candidati()[0]==forzato[k] ? cella.get_candidati()[1] : cella.get_candidati()[0];
                        if(visitato[cella.get_posiz()][altro])
                            continue;
                        visitato[cella.get_posiz()][altro]=true;
                        if(altro==x && lunghezza[k]+1>=3 && rimuovo_xy_wing_generalizzato(x,inizio,cella,lunghezza[k]+1,sudoku))
                            return true;
                        if(n<coda.length){
                            coda[n]=cella;
                            forzato[n]=altro;
                            lunghezza[n++]=lunghezza[k]+1;
                        }
                    }
                }
            }
        }
        return false;
    }
    private boolean rimuovo_xy_wing_generalizzato(int x,Cella inizio,Cella fine,int lunghezza,Cella sudoku[][]){
        Cella celle_comune[]=inizio.celle_in_comune(fine,sudoku);
        Cella vet[]=new Cella[celle_comune.length];
        int cont=0;
        for(int q=0;q<celle_comune.length;q++)
            if(celle_comune[q].rimuovi_candidato(x)){
                System.out.println("Applicato xy-wing generalizzato (catena di "+lunghezza+" celle da "+inizio.get_posiz()+" a "+fine.get_posiz()+"): il "+x+" non puo' andare nella cella "+celle_comune[q].get_posiz());
                vet[cont++]=celle_comune[q];
            }
        if(cont==0)
            return false;
        celle_modificate=Metodi.accorcio_lunghezza(vet,cont);
        numero_rimosso=x;
        return true;
    }
}