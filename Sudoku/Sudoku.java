package Sudoku;
import Metodi.Metodi;

import java.util.Scanner;
public class Sudoku {
    public static void main(String args[]) {
        /*Sudoku sudoku=Metodi.creo_sudoku(2);
        sudoku.stampo();
        Scanner tastiera=new Scanner(System.in);
        tastiera.nextInt();
        sudoku.risolvo();
        sudoku.stampo();*/

        Sudoku sudoku1=new Sudoku(4);
        sudoku1.leggo();
        sudoku1.risolvo();
        sudoku1.stampo();
    }
    private int LATO;
    private int lato;
    private Cella celle[][];
    private Riga righe[];
    private Colonna colonne[];
    private Tabella tabelle[];
    public void set_LATO(int LATO) {
        this.LATO = LATO;
        lato = (int) Math.sqrt(LATO);
    }
    public int get_LATO() {
        return LATO;
    }
    public int get_lato() {
        return lato;
    }

    public void set_celle() {
        for (int i = 0; i < LATO; i++) {
            for (int j = 0; j < LATO; j++)
                celle[i][j] = new Cella(i * LATO + j, LATO);
        }
    }
    public void set_celle(Cella celle[][]){
        this.celle=celle;
        set_settori();
    }
    public Cella get_cella(int posiz) {
        return celle[(int) posiz / LATO][posiz % LATO];
    }

    public void set_settori() {
        for(int i=0;i<LATO;i++){
            righe[i].set_celle(celle[i]);
            Cella vet_colonna[]=new Cella[LATO];
            for(int j=0;j<LATO;j++)
                vet_colonna[j]=celle[j][i];
            colonne[i].set_celle(vet_colonna);
            Tabella tabella=new Tabella(LATO,i);
            Cella vet_tabella[]=new Cella[LATO];
            for(int j=0;j<LATO;j++)
                vet_tabella[j]=celle[tabella.get_cella(j).get_riga()][tabella.get_cella(j).get_colonna()];
            tabelle[i].set_celle(vet_tabella);
        }
    }
    public Riga[] get_righe() {
        return righe;
    }
    public Riga get_riga(int index) {
        return righe[index];
    }
    public Colonna[] get_colonne() {
        return colonne;
    }
    public Colonna get_colonna(int index) {
        return colonne[index];
    }
    public Tabella[] get_tabelle() {
        return tabelle;
    }
    public Tabella get_tabella(int index) {
        return tabelle[index];
    }
    public int get_tabella(int i, int j) {
        return (int) (i / lato) * lato + (int) (j / lato);
    }
    public Sudoku(int LATO) {
        set_LATO(LATO);
        celle = new Cella[LATO][LATO];
        righe = new Riga[LATO];
        colonne = new Colonna[LATO];
        tabelle = new Tabella[LATO];
        for (int i = 0; i < LATO; i++) {
            righe[i] = new Riga(LATO, i);
            colonne[i] = new Colonna(LATO, i);
            tabelle[i] = new Tabella(LATO, i);
        }
        set_celle();
        set_settori();//collega righe, colonne e tabelle alle celle del sudoku (altrimenti i settori hanno celle proprie scollegate)
    }
    //METODI DELLA CLASSE
    public void stampo() {
        for (int i = 0; i < LATO; i++) {
            for (int j = 0; j < LATO; j++) {
                System.out.print(celle[i][j].get_carattere());
                if (j % lato == lato - 1)
                    System.out.print(" ");
            }
            System.out.print("\n");
            if (i % lato == lato - 1)
                System.out.print("\n");
        }
    }
    public void leggo() {
        System.out.println("Inserisci sudoku:\n");
        Scanner tastiera = new Scanner(System.in);
        for (int i = 0; i < LATO; i++)
            for (int j = 0; j < LATO; j++) {
                int numero = tastiera.nextInt();
                if (numero >= 0 && numero <= LATO)// se numero inserito è 0 la cella è vuota
                    set_numero(i, j, numero);
                else {
                    System.out.println("il numero " + numero + " non e' compresp fra 1 e " + LATO + ".\nrienserisci: ");
                    j--;// funziona anche quando j=0
                }
            }
        tastiera.close();

    }
    public void stampo_candidati() {
        for (int i = 0; i < lato * LATO; i++) {
            for (int j = 0; j < LATO * lato; j++) {
                if ((i % lato) * lato + j % lato < celle[(int) (i / lato)][(int) (j / lato)].get_candidati().length)
                    System.out.print(celle[(int) (i / lato)][(int) (j / lato)].get_candidati()[(i % lato) * lato + j % lato]);
                else
                    System.out.print(" ");
                if (j % lato == lato - 1)
                    System.out.print(" ");
                if (j % LATO == LATO - 1)
                    System.out.print(" ");
            }
            System.out.println("");
            if (i % lato == lato - 1)
                System.out.println("");
            if (i % LATO == LATO - 1)
                System.out.println("");

        }
    }
    /**
     * 
     * @param i
     * @param j
     * @param numero
     * @return vettore contenente le celle dalle quali è stato rimosso numero dai candidati
     */
    public Cella[] set_numero(int i, int j, int numero) {//ritorna un vettore che contiene le celle dalle quali è stato rimosso un numero dai candidati
        //se numero==0 ritorna null
        Cella vet_celle[]=new Cella[3*LATO];//vettore che verra ritornato contiene le celle dalle quali e stato rimosso un numero
        int cont=0;//conta il numero di celle da dove è stato rimosso un numero
        if (numero != 0) {
            int celle_comune[] = celle[i][j].celle_in_comune(celle[i][j]);
            for (int k = 0; k < celle_comune.length; k++)
                if(celle[(int) celle_comune[k] / LATO][celle_comune[k] % LATO].rimuovi_candidato(numero))
                    vet_celle[cont++]=celle[(int) celle_comune[k] / LATO][celle_comune[k] % LATO];
        }
        celle[i][j].set_numero(numero);
        if (numero != 0)
            stampo();
        if(cont==0)
            return new Cella[0];
        Cella temp[]=new Cella[cont];
        for(int k=0;k<cont;k++)
            temp[k]=vet_celle[k];
        return temp;       
    }

    public Cella[] set_numero(Cella cella,int numero){
        return set_numero(cella.get_riga(),cella.get_colonna(),numero);
    }
    public int[][] conta_numero(int numero,int vet_settore[],String settore){//restituisce vet nella cui i-esima cella ce conta_numero(numero,0) del settore vet_settore[i]
        int vet[][]=new int[vet_settore.length][LATO];
        if(settore=="Riga" || settore=="Righe")
            for(int i=0;i<vet_settore.length;i++)
                vet[i]=righe[vet_settore[i]].conta_numero(numero,0);
        if(settore=="Colonna" || settore=="Colonne")
            for(int i=0;i<vet_settore.length;i++)
                vet[i]=colonne[vet_settore[i]].conta_numero(numero,0);
        if(settore=="Tabella" || settore=="Tabelle")
            for(int i=0;i<vet_settore.length;i++)
                vet[i]=tabelle[vet_settore[i]].conta_numero(numero,0);
        return vet;
    }
    public int[][] dove_numero(int numero) {// restituisce un vettore vet che in vet[0] ha gli indici delle righe dove si
        // puo inserire numero in vet[1] delle colonne e in vet[2] delle tabelle
        int vet[][] = new int[3][0];
        vet[0]=dove_numero_riga(numero);
        vet[1]=dove_numero_colonna(numero);
        vet[2]=dove_numero_tabella(numero);
        return vet;
    }
    public int[][] dove_numero(int numero,int quante_volte){//restituisce un vettore che in vet[0] ha gli indici delle righe dove numero puo andare un numero di vole <= a quante_volte in vet[1] delle colonne e vet[2] delle tabelle
        int vet[][]=new int[3][0];
        vet[0]=dove_numero_riga(numero,quante_volte);
        vet[1]=dove_numero_colonna(numero,quante_volte);
        vet[2]=dove_numero_tabella(numero,quante_volte);
        return vet;
    }
    public int[][] dove_numero_esatto(int numero,int quante_volte){
        int vet[][]=new int[3][0];
        vet[0]=dove_numero_riga_esatto(numero, quante_volte);
        vet[1]=dove_numero_colonna_esatto(numero,quante_volte);
        vet[2]=dove_numero_tabella_esatto(numero,quante_volte);
        return vet;
    }
    public int[] dove_numero_riga(int numero){//restituisce un vettore formato dagli indici delle righe dove puo andare numero
        int vet_riga[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(righe[i].ce_numero(numero)==false)
                vet_riga[cont++]=i;
        return Metodi.accorcio_lunghezza(vet_riga, cont);
    }
    public int[] dove_numero_riga(int numero,int quante_volte){//restituisce un vettore che contiene gli indici delle righe dove numero può andare un numero di volte <= quante_volte
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(righe[i].ce_numero(numero)==false && righe[i].conta_numero(numero)<=quante_volte)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet,cont);
    }
    public int[] dove_numero_riga_esatto(int numero,int quante_volte){//restituisce gli indici delle righe dove numero puo andare esattamente in quante_volte celle
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(righe[i].conta_numero(numero)==quante_volte)
                vet[cont++]=i;
        System.out.println("Ci sono "+cont+"righe dove il "+numero+" puo andare in due celle");
        return Metodi.accorcio_lunghezza(vet, cont);
    }
    public int[] dove_numero_colonna(int numero){//restituisce vettore formato dagli indici delle colonne dove puo andare numero
        int vet_colonna[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(colonne[i].ce_numero(numero)==false)
                vet_colonna[cont++]=i;
        return Metodi.accorcio_lunghezza(vet_colonna, cont);
    }
    public int[] dove_numero_colonna(int numero,int quante_volte){
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(colonne[i].ce_numero(numero)==false && colonne[i].conta_numero(numero)<=quante_volte)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet,cont);
    }
    public int[] dove_numero_colonna_esatto(int numero,int quante_volte){//restituisce gli indici delle righe dove numero puo andare esattamente in quante_volte celle
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(colonne[i].conta_numero(numero)==quante_volte)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet, cont);
    }
    public int[] dove_numero_tabella(int numero){//restituisce vettore formato dagli indici delle colonne dove puo andare numero
        int vet_tabella[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(tabelle[i].ce_numero(numero)==false)
                vet_tabella[cont++]=i;
        return Metodi.accorcio_lunghezza(vet_tabella, cont);
    } 
    public int[] dove_numero_tabella(int numero,int quante_volte){
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(tabelle[i].ce_numero(numero)==false && tabelle[i].conta_numero(numero)<=quante_volte)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet,cont);
    }
    public int[] dove_numero_tabella_esatto(int numero,int quante_volte){//restituisce gli indici delle righe dove numero puo andare esattamente in quante_volte celle
        int vet[]=new int[LATO],cont=0;
        for(int i=0;i<LATO;i++)
            if(tabelle[i].conta_numero(numero)==quante_volte)
                vet[cont++]=i;
        return Metodi.accorcio_lunghezza(vet, cont);
    }
    public Cella[] celle_in_comune(Cella celle[]){//ritorna vet in cui mette le celle viste da tutti gli elementi di celle(diverso da this.celle)
        int vet_int[]=Metodi.celle_in_comune(LATO, celle);//vet_int contiene le posizioni delle celle viste in comune
        Cella vet[]=new Cella[vet_int.length];
        for(int i=0;i<vet.length;i++)
            vet[i]=this.celle[(int)vet_int[i]/LATO][vet_int[i]%LATO];
        return vet;
    }
    public boolean finito(){
        boolean finito=true;
        for(int i=0;i<LATO && finito;i++)
            for(int j=0;j<LATO && finito;j++)
                if(celle[i][j].get_carattere()=='-')
                    finito=false;
        return finito;
    }
    private boolean risolvo(int inutile){
        if(finito() && assurdo()==false){
            System.out.println("Ho risolto il sudoku");
            return true;
        }
        return risolvo();
    }
    public boolean risolvo(){
        if(assurdo()){//succede solo nelle copie create da tentativi() con un numero sbagliato (o se il sudoku inserito e' impossibile)
            System.out.println("Il sudoku e' impossibile");
            return false;
        }
        if(slittamento())
            return risolvo(0);
        if(naked_pairs())
            return risolvo(0);
        if(esclusione())
            return risolvo(0);
        if(coppie())
            return risolvo(0);
        if(x_wing())
            return risolvo(0);
        if(colore())
            return risolvo(0);
        if(xy_wing())
            return risolvo(0);
        if(sky_screaper())
            return risolvo(0);
        if(finito()==false && tentativi())//ultima risorsa
            return risolvo(0);
        if(finito()==false)
            System.out.println("Con le tecniche a disposizione non sono in grado di risolvere il sudoku");
        return false;
    }
                        //!!!POST-TECNICHE RISOLUTIVE!!!
    public static void post_slittamento(int numero,Cella celle[],Cella sudoku[][]){
        for(int i=0;i<celle.length;i++)//se celle.length=0 non entra nel for
            post_slittamento(numero,celle[i],sudoku);
    }
    public static void post_slittamento(int numero,Cella cella,Cella sudoku[][]){
        if(cella==null){
            System.out.println("Errore NullPointerExeption a Sudoku.post_slittamento()");
            return;
        }
        cella.esclusione(sudoku);
        Metodi.get_tabella(cella.get_tabella(),sudoku).slittamento(numero,sudoku);
        Metodi.get_riga(cella.get_riga(),sudoku).slittamento(numero,sudoku);
        Metodi.get_colonna(cella.get_colonna(),sudoku).slittamento(numero,sudoku);

    }
    public static void post_slittamento(Cella celle[],Cella sudoku[][]){//Dalle celle di celle è stato rimosso qualche candidato
        for(int numero=1;numero<=sudoku.length;numero++)
            post_slittamento(numero,celle,sudoku);
    }
    public static void post_naked_pairs(Cella celle[],Cella sudoku[][]){
        post_slittamento(celle,sudoku);
        for(int numero=1;numero<=sudoku.length;numero++)
            post_naked_pairs(numero,celle,sudoku);
    }
    public static void post_naked_pairs(int numero,Cella celle[],Cella sudoku[][]){
        post_slittamento(numero,celle,sudoku);//fa esclusione della cella e slittamento dei settori della cella
        for(int i=0;i<celle.length;i++)
            post_naked_pairs(numero,celle[i],sudoku);
    }
    public static void post_naked_pairs(int numero,Cella cella,Cella sudoku[][]){
        if(cella==null){
            System.out.println("Errore NullPointerExeption a post_naked_pairs()");
            return;
        }
        post_slittamento(numero,cella,sudoku);
        Metodi.get_tabella(cella.get_tabella(),sudoku).naked_pairs(numero,sudoku);
        Metodi.get_riga(cella.get_riga(),sudoku).naked_pairs(numero,sudoku);
        Metodi.get_colonna(cella.get_colonna(),sudoku).naked_pairs(numero,sudoku);

    }
    public static void post_remote_pairs(int vet_num[],Cella celle[],Cella sudoku[][]){
        for(int i=0;i<vet_num.length;i++)
            post_x_wing(vet_num[i],celle,sudoku);
    }
    public static void post_coppie(Cella celle[],Cella sudoku[][]){
        System.out.println("Entro in post_coppie alle celle: ");
        for(int i=0;i<celle.length;i++)
            System.out.print(celle[i].get_posiz()+" ");
        System.out.println("");
        post_naked_pairs(celle,sudoku);
        for(int numero=1;numero<=sudoku.length;numero++)
            post_coppie(numero,celle,sudoku);
    }
    public static void post_coppie(int numero,Cella celle[],Cella sudoku[][]){
        post_naked_pairs(numero,celle,sudoku);
        for(int i=0;i<celle.length;i++)
            post_coppie(numero,celle[i],sudoku);
    }
    public static void post_coppie(int vet_num[],Cella cella,Cella sudoku[][]){
        for(int i=0;i<vet_num.length;i++)
            post_coppie(vet_num[i],cella,sudoku);
    }
    public static void post_coppie(int vet_num[],Cella celle[],Cella sudoku[][]){
        for(int i=0;i<vet_num.length;i++)
            post_coppie(vet_num[i],celle,sudoku);
    }
    public static void post_coppie(int numero,Cella cella,Cella sudoku[][]){
        if(cella==null){
            System.out.println("Errore null_pointerException in Sudoku.post_coppie");
            return;
        }
        post_naked_pairs(numero,cella,sudoku);
        Metodi.get_tabella(cella.get_tabella(),sudoku).coppie(sudoku);
        Metodi.get_riga(cella.get_riga(),sudoku).coppie(sudoku);
        Metodi.get_colonna(cella.get_colonna(),sudoku).coppie(sudoku);
    }
    public static void post_x_wing(Cella celle[],Cella sudoku[][]){
        for(int numero=1;numero<=sudoku.length;numero++)
            post_x_wing(numero,celle,sudoku);
    }
    public static void post_x_wing(int numero,Cella celle[],Cella sudoku[][]){
        for(int i=0;i<celle.length;i++)
            post_x_wing(numero,celle[i],sudoku);
    }
    public static void post_x_wing(int numero,Cella cella,Cella sudoku[][]){
        post_coppie(numero,cella,sudoku);
        if(cella==null){
            System.out.println("Errore NullPointerExeption in post_x_wing()");
            return;
        }
        Metodi.get_riga(cella.get_riga(),sudoku).x_wing(numero,sudoku);
        Metodi.get_colonna(cella.get_colonna(),sudoku).x_wing(numero,sudoku);
    }
    public static void post_xy_wing(int numero,Cella celle[],Cella sudoku[][]){
        return;
    }
    public static void post_ipotesi(Cella celle[],Cella sudoku[][]){
        post_x_wing(celle,sudoku);
    }
    //TECNICHE RISOLUTIVE
    public boolean esclusione() {//VERIFICATA
        for (int i = 0; i < LATO; i++)
            for (int j = 0; j < LATO; j++)
                if(celle[i][j].esclusione(this.celle)!=0){
                    esclusione();
                    return true;
                }
        return false;
    }
    public boolean slittamento() {//VERIFICATA
        for (int numero = 1; numero <= LATO; numero++)
            if (slittamento(numero)){
                slittamento();//avendo inserito un numero ri-incomincia da capo
                return true;
            }
        return false;
    }
    public boolean slittamento(int inutile1,int inutile2){
        slittamento();
        return true;
    }
    public boolean slittamento(int numero){
        System.out.println("Entro in slittamento numero: "+numero);
        int vet[]=dove_numero_tabella(numero);//in vet ci sono gli indici delle tabelle dove puo andare numero
        for(int i=0;i<vet.length;i++)//scorro sulle tabelle
            if(tabelle[vet[i]].slittamento(numero,this.celle))
                return slittamento(0,0);
        vet=dove_numero_riga(numero);
        for(int i=0;i<vet.length;i++)   
            if(righe[vet[i]].slittamento(numero,this.celle))
                return slittamento(0,0);
        vet=dove_numero_colonna(numero);
        for(int i=0;i<vet.length;i++)//scorro sulle tabelle
            if(colonne[vet[i]].slittamento(numero,this.celle))
                return slittamento(0,0);
        return false;
    }
    public boolean naked_pairs(){
        for(int numero=1;numero<=LATO;numero++)
            if(naked_pairs(numero)){
                naked_pairs();
                return true;
            }
        return false;
    }
    public boolean naked_pairs(int inutile1,int inutile2){
        naked_pairs();
        return true;
    }
    public boolean naked_pairs(int numero){
        System.out.println("Entro in naked pairs al numero "+numero);
        int vet[]=dove_numero_tabella(numero);//in vet ci sono gli indici delle tabelle dove puo andare numero
        for(int i=0;i<vet.length;i++)//scorro sulle tabelle dove non puo andare numero
            if(tabelle[vet[i]].naked_pairs(numero,celle))
                return naked_pairs(0,0);
        vet=dove_numero_riga(numero);//in vet ci sono gli indici delle righe dove va inserito numero
        for(int i=0;i<vet.length;i++)//scorro sulle righe dove va inserito numero
            if(righe[vet[i]].naked_pairs(numero,celle))
                return naked_pairs(0,0);
        vet=dove_numero_colonna(numero);//in vet ci sono gli indici delle colonne doe non puo andare numero
        for(int i=0;i<vet.length;i++)//scorro sulle colonne dove non puo andare numero
            if(colonne[vet[i]].naked_pairs(numero,celle))
                return naked_pairs(0,0);
        return false;
    }
    public boolean coppie() {
        boolean applicato = false;
        for (int i = 0; i < LATO; i++)// TABELLE
            if (tabelle[i].coppie(this.celle))
                applicato = true;//meglio non invocare qui coppie
        for (int i = 0; i < LATO; i++)// RIGHE
            if (righe[i].coppie(this.celle))
                applicato = true;
        for (int i = 0; i < LATO; i++)// COLONNE
            if (colonne[i].coppie(this.celle))
                applicato = true;
        if(applicato)
            coppie();
        return applicato;
    }
    public boolean x_wing() {
        boolean applicato = false;
        for (int numero = 1; numero <= LATO; numero++)
            if (x_wing(numero))//x_wing numero fa tutto per numero non ce bisogno di mettere numero--
                applicato = true;
        return applicato;
    }
    public boolean x_wing(int numero,int inutile){
        x_wing(numero);
        return true;
    }
    public boolean x_wing(int numero) {
        System.out.println("Entro in x-wing al numero: "+numero);
        int vet[]=dove_numero_riga(numero);
        for(int i=0;i<vet.length-1;i++)//scorro sulle righe
            if(righe[vet[i]].x_wing(numero,this.celle))
                return x_wing(numero,0);
        vet=dove_numero_colonna(numero);
        for(int i=0;i<vet.length-1;i++)
            if(colonne[vet[i]].x_wing(numero,this.celle))
                return x_wing(numero,0);
        return false;
    }
    public static int[] x_wing_generale(int vet[][]) {// gli elementi di vet sono 0 o 1 il metodo torna un vettore che contiene qualle j per le quali esiste i tale che vet[i][j]=1
        // vet[0],vet[1],... devono tutti essere lunghi LATO
        int somma[] = new int[vet[0].length];
        for (int i = 0; i < vet[0].length; i++) {
            somma[i] = 0;
            for (int j = 0; j < vet.length; j++)
                somma[i] = somma[i] + vet[j][i];
        }
        int cont = 0, vettore[] = new int[100];

        for (int i = 0; i < somma.length; i++)
            if (somma[i] > 0)
                vettore[cont++] = i;
        int temp_vet[] = vettore;
        vettore=new int[cont];
        for (int i = 0; i < cont; i++)
            vettore[i] = temp_vet[i];
        return vettore;
    }
    public boolean xy_wing(){
        boolean applicato=false;
        for(int i=0;i<LATO;i++)//scorre sulle righe
            for(int j=0;j<LATO;j++)
                if(celle[i][j].xy_wing(this.celle))//Vede se celle[i][j forma xy-wing con altre due celle e fa quelo che deve fare
                    applicato=true;
        return applicato;
    }
    public boolean colore() {
        boolean applicato = false;
        for (int numero = 1; numero <= LATO; numero++)
            if (colore(numero))
                applicato = true;
        return applicato;
    }
    /**
     * Colorazione semplice per il numero numero.
     * Due celle sono "coppia coniugata" se in un settore (riga, colonna o tabella) numero puo' andare solo in quelle due:
     * una delle due contiene sicuramente numero. Partendo da una cella si colorano A e B alternati lungo le coppie coniugate
     * (una catena): tutte le A sono vere oppure tutte le B sono vere.
     * Regola 2: se due celle con la stessa lettera si vedono, quella lettera e' falsa e numero si toglie da tutte le sue celle.
     * Regola 4: una cella fuori dalla catena che vede sia una A che una B non puo' contenere numero.
     * @return true se ha rimosso almeno un candidato
     */
    public boolean colore(int numero) {
        Settore settori[][] = {righe, colonne, tabelle};
        Cella coppie[][] = new Cella[3 * LATO][];//coppie coniugate di numero
        int n_coppie = 0;
        for (int s = 0; s < settori.length; s++)
            for (int i = 0; i < LATO; i++)
                if (settori[s][i].ce_numero(numero) == false && settori[s][i].dove_numero(numero, 0).length == 2)
                    coppie[n_coppie++] = settori[s][i].dove_numero(numero, 0);
        boolean applicato = false;
        boolean visitata[][] = new boolean[LATO][LATO];
        for (int c = 0; c < n_coppie && applicato == false; c++) {
            Cella inizio = coppie[c][0];
            if (visitata[inizio.get_riga()][inizio.get_colonna()])
                continue;
            //coloro la catena che parte da inizio
            Cella catena[] = new Cella[LATO * LATO];
            int lunghezza = 0;
            inizio.set_carattere('A');
            visitata[inizio.get_riga()][inizio.get_colonna()] = true;
            catena[lunghezza++] = inizio;
            for (int k = 0; k < lunghezza; k++)//catena[] fa da coda: si aggiungono in fondo le celle appena colorate
                for (int h = 0; h < n_coppie; h++) {
                    Cella altra = null;
                    if (coppie[h][0] == catena[k])
                        altra = coppie[h][1];
                    else if (coppie[h][1] == catena[k])
                        altra = coppie[h][0];
                    if (altra != null && visitata[altra.get_riga()][altra.get_colonna()] == false) {
                        altra.set_carattere(Metodi.inverto_A(catena[k].get_carattere()));
                        visitata[altra.get_riga()][altra.get_colonna()] = true;
                        catena[lunghezza++] = altra;
                    }
                }
            if (lunghezza >= 3) {
                //regola 2: due celle con la stessa lettera che si vedono
                char falsa = '-';
                for (int a = 0; a < lunghezza && falsa == '-'; a++)
                    for (int b = a + 1; b < lunghezza && falsa == '-'; b++)
                        if (catena[a].get_carattere() == catena[b].get_carattere() && catena[a].vede(catena[b]))
                            falsa = catena[a].get_carattere();
                if (falsa != '-') {
                    for (int a = 0; a < lunghezza; a++)
                        if (catena[a].get_carattere() == falsa && catena[a].rimuovi_candidato(numero)) {
                            System.out.println("Applicato colore (regola 2): il " + numero + " non puo' andare nella cella " + catena[a].get_posiz());
                            applicato = true;
                        }
                } else {
                    //regola 4: celle fuori dalla catena che vedono sia una A che una B
                    for (int i = 0; i < LATO; i++)
                        for (int j = 0; j < LATO; j++) {
                            Cella cella = celle[i][j];
                            if (cella.get_numero() != 0 || visitata[i][j] || cella.ci_puo_andare(numero) == false)
                                continue;
                            boolean vede_A = false, vede_B = false;
                            for (int a = 0; a < lunghezza; a++)
                                if (cella.vede(catena[a])) {
                                    if (catena[a].get_carattere() == 'A')
                                        vede_A = true;
                                    else
                                        vede_B = true;
                                }
                            if (vede_A && vede_B && cella.rimuovi_candidato(numero)) {
                                System.out.println("Applicato colore (regola 4): il " + numero + " non puo' andare nella cella " + cella.get_posiz());
                                applicato = true;
                            }
                        }
                }
            }
            for (int a = 0; a < lunghezza; a++)//cancello le lettere della catena
                catena[a].set_carattere('-');
        }
        return applicato;
    }
    public boolean sky_screaper(){
        boolean applicato=false;
        for(int numero=1;numero<=LATO;numero++)
            if(sky_screaper(numero,righe) || sky_screaper(numero,colonne))
                applicato=true;
        return applicato;
    }
    /**
     * Sky-screaper per il numero numero sui settori paralleli settori[] (tutte righe o tutte colonne).
     * Si cercano due settori in cui numero puo' andare in esattamente due celle e che hanno una delle due celle
     * sulla stessa linea perpendicolare (la "base"): al massimo una delle due celle della base contiene numero,
     * quindi almeno una delle altre due celle (le "punte") contiene numero e numero si toglie dalle celle che vedono entrambe le punte.
     */
    public boolean sky_screaper(int numero,Settore settori[]){
        boolean applicato=false;
        for(int i=0;i<LATO;i++)
            for(int j=i+1;j<LATO;j++){
                if(settori[i].ce_numero(numero) || settori[j].ce_numero(numero))
                    continue;
                Cella celle1[]=settori[i].dove_numero(numero,0),celle2[]=settori[j].dove_numero(numero,0);
                if(celle1.length!=2 || celle2.length!=2)
                    continue;
                for(int a=0;a<2;a++)
                    for(int b=0;b<2;b++){
                        Cella base1=celle1[a],base2=celle2[b],punta1=celle1[1-a],punta2=celle2[1-b];
                        boolean stessa_linea_base=settori[i].get_type()=="Riga" ? base1.get_colonna()==base2.get_colonna() : base1.get_riga()==base2.get_riga();
                        boolean stessa_linea_punte=settori[i].get_type()=="Riga" ? punta1.get_colonna()==punta2.get_colonna() : punta1.get_riga()==punta2.get_riga();
                        if(stessa_linea_base==false || stessa_linea_punte)//se anche le punte sono allineate e' un x-wing
                            continue;
                        for(int r=0;r<LATO;r++)
                            for(int c=0;c<LATO;c++){
                                Cella cella=celle[r][c];
                                if(cella.vede(punta1) && cella.vede(punta2) && cella.rimuovi_candidato(numero)){
                                    System.out.println("Applicato sky-screaper: il "+numero+" non puo' andare nella cella "+cella.get_posiz());
                                    applicato=true;
                                }
                            }
                    }
            }
        return applicato;
    }
    /*
    public boolean ipotesi(){
        boolean applicato=false;
        for(int i=0;i<LATO;i++)
            for(int j=0;j<LATO;j++){
                Gruppo gruppo=celle[i][j].creo_catena(this.celle);//crea un gruppo con prima cella celle[i][j] di tutte celle comunicanti e con due candidati
                if(gruppo!=null){
                    System.out.println("Faccio ipotesi partendo dalla cella: "+(i*LATO+j));
                    stampo();
                    Gruppo gruppo1=gruppo.creo_identico(),gruppo2=new gruppo.creo_identico();
                    gruppo1.set_numero(celle[i][j].get_candidati()[0],0);
                    gruppo2.set_numero(celle[i][j].get_candidati()[1],0);
                    gruppo1.risolvo(celle);
                    gruppo2.risolvo(celle);

                        if(gruppo1.assurdo() && gruppo2.assurdo()){
                            System.out.println("Facendo ipotesi mi sono accorto che il sudoku è impossibile perche nella cella non ci possono andare entrambi i candidati")
                        }else if(gruppo1.assurdo()){
                            System.out.println("Applicato ipotesi alla cella il numero "+celle[i][j].get_candidati()[0]+" inserisco numero "+celle[i][j].get_candidati()[1]);
                            set_numero(celle[i][j].get_candidati()[1]);
                        }else if(gruppo1.assurdo()){
                            System.out.println("Applicato ipotesi alla cella il numero "+celle[i][j].get_candidati()[1]+" inserisco numero "+celle[i][j].get_candidati()[0]);
                            set_numero(celle[i][j].get_candidati()[0]);
                        }else{//se entrambi i grupi non sono assurdi
                            Cella vettore_di_celle[]=new Cella[3*LATO];
                            for(int k=0;k<gruppo1.get_celle().length;k++)
                                if(gruppo1.get_cella(k).get_numero()!=0)
                                    for(int h=0;h<gruppo2.get_celle().length;h++)
                                        if(gruppo2.get_cella(h).get_numero()==gruppo1.get_cella(k).get_numero()){
                                            Cella celle_comune[]=gruppo1.get_cella(k).celle_in_comune(gruppo2.get_cella(h),this.celle);
                                            int cont=0;
                                            for(int q=0;q<celle_comune.length;q++)
                                                if(celle_comune[q].rimuovi_candidato(gruppo1.get_cella(k).get_numero())){
                                                    System.out.println("Applicato ipotesi il numero "+gruppo1.get_cella(k).get_numero+" non puo andare nella cella "+celle_comune[q].get_posiz());
                                                    vettore_di_celle[cont++]=celle_comune[q];//si segna tutte le celle da cui è stato rimosso un candidato
                                                }
                                        }
                            Sudoku.post_ipotesi(vettore_di_celle,sudoku);
                        }
                    
                    for(int k=0;k<LATO;k++)
                        for(int h=0;h<LATO;h++)
                            if(celle[k][h].get_carattere()=='A')
                                celle[k][h].set_carattere('-');
                }
            }
        return applicato;
    }*/
    /**
     * Crea una copia indipendente del sudoku (celle nuove con gli stessi numeri e candidati)
     */
    public Sudoku copia(){
        Sudoku copia=new Sudoku(LATO);
        for(int i=0;i<LATO;i++)
            for(int j=0;j<LATO;j++)
                if(celle[i][j].get_numero()!=0)
                    copia.celle[i][j].set_numero(celle[i][j].get_numero());
                else
                    copia.celle[i][j].set_candidati(celle[i][j].get_candidati());
        return copia;
    }
    /**
     * @return true se il sudoku e' impossibile: una cella vuota senza candidati, un numero ripetuto in un settore
     * oppure un numero che in un settore non puo' andare in nessuna cella
     */
    public boolean assurdo(){
        Settore settori[][]={righe,colonne,tabelle};
        for(int s=0;s<settori.length;s++)
            for(int i=0;i<LATO;i++){
                if(settori[s][i].assurdo())
                    return true;
                for(int numero=1;numero<=LATO;numero++)
                    if(settori[s][i].ce_numero(numero)==false && settori[s][i].conta_numero(numero)==0)
                        return true;
            }
        return false;
    }
    /**
     * Ultima risorsa quando le tecniche logiche non bastano.
     * Si sceglie la cella vuota con meno candidati e si prova ogni candidato su una copia del sudoku:
     * - se la copia diventa assurda il candidato e' impossibile e viene tolto dalla cella;
     * - se la copia viene risolta il candidato e' quello giusto e viene inserito.
     * Le prove sulle copie non stampano nulla.
     * @return true se ha tolto un candidato o inserito un numero
     */
    public boolean tentativi(){
        Cella scelta=null;
        for(int i=0;i<LATO;i++)
            for(int j=0;j<LATO;j++)
                if(celle[i][j].get_numero()==0 && (scelta==null || celle[i][j].get_candidati().length<scelta.get_candidati().length))
                    scelta=celle[i][j];
        if(scelta==null)
            return false;
        int candidati[]=scelta.get_candidati().clone();
        for(int k=0;k<candidati.length;k++){
            Sudoku prova=copia();
            java.io.PrintStream schermo=System.out;
            try{
                System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
                prova.set_numero(scelta.get_riga(),scelta.get_colonna(),candidati[k]);
                prova.risolvo();
            }finally{
                System.setOut(schermo);
            }
            if(prova.assurdo()){
                System.out.println("Applicato tentativi: mettendo il "+candidati[k]+" nella cella "+scelta.get_posiz()+" il sudoku diventa impossibile, quindi lo tolgo dai candidati");
                scelta.rimuovi_candidato(candidati[k]);
                return true;
            }
            if(prova.finito()){
                System.out.println("Applicato tentativi: mettendo il "+candidati[k]+" nella cella "+scelta.get_posiz()+" il sudoku si risolve, quindi lo inserisco");
                set_numero(scelta.get_riga(),scelta.get_colonna(),candidati[k]);
                return true;
            }
        }
        return false;
    }
    public boolean remote_pairs(){// non ha senso metterlo dopo colore perchè fare prima colore rende inefficace remote_pairs()
        boolean applicato=false;
        for(int i=0;i<LATO;i++)
            for(int j=0;j<LATO;j++){
                Gruppo gruppo=celle[i][j].creo_catena_remote_pairs(this.celle);//Gruppo è una catena di A e B che 
                if(gruppo!=null){//con una catena di tre celle non si fa nulla
                    stampo();
                    if(gruppo.get_celle().length>=4){
                    //controllo se ci sono due A o due B che si vedono
                        for(int k=0;k<gruppo.get_celle().length;k++)
                            for(int h=0;h<gruppo.get_cella(k).get_celle_gruppo_viste().length;h++)
                                if(gruppo.get_cella(k).get_carattere()==gruppo.get_cella(k).get_cella_gruppo_vista(h).get_carattere()){
                                    System.out.println("Il sudoku e' impossibile perche' la lettera "+gruppo.get_cella(k).get_carattere()+" si autovede");
                                    System.exit(0);
                                }
                        Cella vettore_di_celle[]=new Cella[LATO*LATO];
                        int cont=0;
                        for(int k=0;k<gruppo.get_celle().length-1;k++){
                            for(int h=k+1;h<gruppo.get_celle().length;h++){//se in gruppo.get_cella(k) ce la A nelle altrre celle viste c'è la B
                                if(gruppo.get_cella(k).get_carattere()==Metodi.inverto_A(gruppo.get_cella(h).get_carattere())){
                                    Cella celle_comune[]=gruppo.get_cella(k).celle_in_comune(gruppo.get_cella(h),this.celle);
                                    for(int q=0;q<celle_comune.length;q++)
                                        if(celle_comune[q].rimuovi_candidato(celle[i][j].get_candidati()).length>0){
                                            System.out.println("Applicato remote pairs i numeri non possono andare nella cella "+celle_comune[q].get_posiz());
                                            vettore_di_celle[cont++]=celle_comune[q];//è inutile dire che celle_comune[q] puo essere inserito in vettore_di_celle solo se non è gia stato inserito
                                            applicato=true;
                                        }
                                }
                            }
                            gruppo.get_cella(k).set_carattere('-');
                        }
                        gruppo.get_cella(gruppo.get_celle().length-1).set_carattere('-');
                        if(cont>0)
                            Sudoku.post_remote_pairs(celle[i][j].get_candidati(),vettore_di_celle,this.celle);
                    }
                    gruppo.distruggo_gruppo();//resetta gli attributi celle_gruppo e celle_gruppo_viste delle celle del gruppo
                }   
            }
        return applicato;
    }
    public boolean xy_wing_generalizzato(){
        Cella vet[]=new Cella[LATO*LATO];
        boolean applicato=false;
        for(int i=0;i<LATO*LATO;i++)
            vet[i]=celle[(int)i/LATO][i%LATO];
        for(int i=0;i<vet.length;i++){
            Gruppo gruppo=vet[i].creo_catena_xy_wing(this.celle);
            if(gruppo!=null){
                gruppo.stampo();
                if(gruppo.xy_wing_generalizzato(celle)){
                    i=-1;
                    vet=Metodi.rimuovo_celle_al_vettore(vet,gruppo.get_celle());
                    applicato=true;
                }
                gruppo.distruggo_gruppo();//sistema le celle_gruppo delle varie celle
            }
        }
        return applicato;
    }
}