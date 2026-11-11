package Sudoku;
import Metodi.Metodi;
public class Settore extends Gruppo{
    
    public static void main(String args[]){
        int LATO=9;
        Sudoku sudoku=new Sudoku(LATO);
        sudoku.leggo();
        if(sudoku.risolvo())
            System.out.println("Applicato x-wing");
        else    
            System.out.println("Non applicato x-wing");
        sudoku.stampo();
    }
    private int index;
    private String type;
    public int get_index(){
        return this.index;
    }
    public void set_index(int index){
        this.index=index;
    }
    public String get_type(){
        return type;
    }
    public void set_type(String type){
        this.type=type;
    }


    public int[] settori_opposti(int vet_settori[]){
        int vet[]=new int[vet_settori.length+1];
        vet[0]=index;
        for(int i=1;i<=vet_settori.length;i++)
            vet[i]=vet_settori[i-1];
        int vettore[]=Metodi.celle_opposte(vet,LATO);
        return vettore;
    }

    public int[][] conta_numero(int numero,int vet_settore[],String settore,Cella sudoku[][]){
        int vet_settore1[]=new int[vet_settore.length+1];
        vet_settore1[0]=index;
        for(int i=1;i<=vet_settore.length;i++)
            vet_settore1[i]=vet_settore[i-1];
        return Metodi.conta_numero(numero, vet_settore1, settore, sudoku);
    }


     //TECNICHE RISOLUTIVE
    public boolean slittamento(Cella sudoku[][]){//ritorna il vettore di celle dove ha messo numeri
        for(int numero=1;numero<=get_LATO();numero++)
            if(slittamento(numero,sudoku)){
                slittamento(sudoku);
                return true;
            }
        return false;
    }
    public boolean slittamento(int numero,Cella sudoku[][]){//NON PUO ESISTERE slittamento()perche non si riesce ad inserire il numero e ce bisogno della classe sudoku
        System.out.println("Provo a fare slittamento del numero "+numero+" nella "+get_type()+" "+get_index());
        if(conta_numero(numero)==1 && ce_numero(numero)==false){
            System.out.println("Applico slittamento il numero "+numero+" nella cella "+dove_numero(numero,0)[0].get_posiz());
            Cella celle[]=Metodi.set_numero(dove_numero(numero,0)[0],numero,sudoku);//contiene le celle da cui set_numero ha rimosso un candidato
            Sudoku.post_slittamento(numero,celle,sudoku);//se celle.length==0 non ce problema
            return true;
        }
        return false;
    }
    public boolean naked_pairs(int numero,Cella sudoku[][]){
        System.out.println("Provo a fare naked-pairs del numero "+numero+" nella "+get_type()+" "+get_index());
        int cont=0;
        Cella vettore[]=dove_numero(numero,0),vettore_di_celle[]=new Cella[3*get_LATO()];//celle del settore dove puo andare numero
        if(vettore.length>=2 && vettore.length<=get_lato()){
            Cella celle_comune[]=Metodi.celle_in_comune(get_LATO(),vettore,sudoku);
            for(int i=0;i<celle_comune.length;i++)
                if(celle_comune[i].rimuovi_candidato(numero)){
                    System.out.println("Applico naked-pairs il "+numero+" non puo andare nella cella "+celle_comune[i].get_posiz()+" perche' la "+get_type()+" "+get_index()+" forma naked-pairs");
                    vettore_di_celle[cont++]=celle_comune[i];//celle accumula le celle da cui si e rimosso un candidato
                }
            if(cont>0){
                Sudoku.post_naked_pairs(Metodi.accorcio_lunghezza(vettore_di_celle,cont),sudoku);
                return true;
            }
        }
        return false;
    }
    public boolean coppie(Cella sudoku[][]){
        //il metodo toglie i candidati dalle celle una volta applicata la tecnica e ritorna il vettore dii celle da dove a tolto un candidato
        System.out.println("Provo a fare coppie nella "+get_type()+" "+get_index());
        int vet_num[]=quali_numeri();//contiene i numeri che possono andare nel settore
        if(vet_num.length==1)
            return slittamento(vet_num[0],sudoku);
        //si provano tutti i gruppi di 2,3 e 4 numeri (coppie/terne/quaterne nascoste) e di 2,3 e 4 celle (coppie/terne/quaterne nude)
        //in un settore di 9 celle basta arrivare a 4: un gruppo nudo di 5 celle corrisponde ad un gruppo nascosto di 4 numeri e viceversa
        for(int dim=2;dim<=get_lato()+1 && dim<vet_num.length;dim++){
            if(coppie_nascoste(vet_num,new int[0],0,dim,sudoku))
                return true;
            if(coppie_nude(dove_numeri(vet_num),new int[0],0,dim,sudoku))
                return true;
        }
        return false;
    }
    private boolean coppie_nascoste(int vet_num[],int scelti[],int inizio,int dim,Cella sudoku[][]){
        //sceglie in tutti i modi possibili dim numeri fra quelli di vet_num e per ognuno prova coppie(scelti,sudoku)
        if(scelti.length==dim)
            return dove_numeri(scelti).length==dim && coppie(scelti,sudoku);
        for(int i=inizio;i<vet_num.length;i++)
            if(coppie_nascoste(vet_num,Metodi.aggiungo_int_al_vettore(scelti,vet_num[i]),i+1,dim,sudoku))
                return true;
        return false;
    }
    private boolean coppie_nude(int indici_vuote[],int scelte[],int inizio,int dim,Cella sudoku[][]){
        //sceglie in tutti i modi possibili dim celle vuote del settore: se insieme hanno solo dim candidati
        //quei numeri vanno in quelle celle e si tolgono dalle altre celle del settore
        if(scelte.length==dim){
            int numeri[]=new int[0];
            for(int i=0;i<scelte.length;i++)
                for(int k=0;k<get_cella(scelte[i]).get_candidati().length;k++)
                    if(Metodi.ce_elemento(numeri,get_cella(scelte[i]).get_candidati()[k])==false)
                        numeri=Metodi.aggiungo_int_al_vettore(numeri,get_cella(scelte[i]).get_candidati()[k]);
            if(numeri.length!=dim)
                return false;
            boolean applicato=false;
            for(int i=0;i<get_LATO();i++)
                if(Metodi.ce_elemento(scelte,i)==false && get_cella(i).get_numero()==0 && get_cella(i).rimuovi_candidato(numeri).length>0){
                    System.out.println("Applicato coppie nude nella "+get_type()+" "+get_index()+": tolgo candidati dalla cella "+get_cella(i).get_posiz());
                    applicato=true;
                }
            return applicato;
        }
        for(int i=inizio;i<indici_vuote.length;i++)
            if(get_cella(indici_vuote[i]).get_numero()==0 && coppie_nude(indici_vuote,Metodi.aggiungo_int_al_vettore(scelte,indici_vuote[i]),i+1,dim,sudoku))
                return true;
        return false;
    }
    public boolean coppie(int vet_num[],Cella sudoku[][]){//Vet_num.length non è 1
        //ritorna un vettore formato dalle celle dalle quali è stato rimosso un candidato
        Cella vet_celle[]=dove_numeri(vet_num,0),vettore_di_celle[]=new Cella[2*get_LATO()];
        int cont=0;
        System.out.print("Invoco coppie ai numeri: ");
        for(int i=0;i<vet_num.length;i++)
            System.out.print(vet_num[i]+" ");
        System.out.print(".che possono andare nelle celle: ");
        for(int i=0;i<vet_celle.length;i++)
            System.out.print(vet_celle[i].get_posiz()+" ");
        System.out.println("");
        boolean applicato=false;
        if(vet_num.length==vet_celle.length){//i numeri di vet_num formano coppie nelle celle di vet_celle e vet_num.length non è 1
            Cella celle_comune[]=Metodi.celle_in_comune(get_LATO(), vet_celle,sudoku);//celle_comune contiene le celle viste da tutti gli elementi di vet_celle
            for(int i=0;i<celle_comune.length;i++){//tolgo dalle celle viste da vet_celle i numeri di vet_num
                int vet[]=celle_comune[i].rimuovi_candidato(vet_num);//contiene i numeri di vet_num che sono stati rimossi da rimuovi_candidato()
                if(vet.length>0){
                    System.out.print("Applicato coppie: tolgo candidati dalla cella "+celle_comune[i].get_posiz()+". candidati rimossi: ");
                    for(int j=0;j<vet.length;j++)
                        System.out.print(vet[j]+" ");
                    System.out.println("");
                    vettore_di_celle[cont++]=celle_comune[i];
                    applicato=true;
                }
            }
            if(cont>0)
                Sudoku.post_coppie(vet_num,Metodi.accorcio_lunghezza(vettore_di_celle,cont),sudoku);
            int vet_num1[]=Metodi.numeri_opposti(vet_num,get_LATO());
            for(int i=0;i<vet_celle.length;i++){//rimuovo, dalle celle dove vanno i numeri di vet_num, i numeri che non vanno in vet_num
                int vet[]=vet_celle[i].rimuovi_candidato(vet_num1);
                if(vet.length>0){
                    System.out.print("Applicato coppie: tolgo candidato dalla cella "+vet_celle[i].get_posiz()+". Candidati: ");
                    for(int j=0;j<vet.length;j++)
                        System.out.print(vet[j]+" ");
                    System.out.println("");
                    applicato=true;
                }
            }
        }//se i numeri non formano un gruppo non si fa nulla: tutti i gruppi possibili vengono provati da coppie(sudoku)
        return applicato;
    }
    public boolean x_wing(int numero,Cella sudoku[][]){
        //il settore invocante e' il primo; si provano tutti i gruppi di altri 1,2 o 3 settori paralleli successivi (x-wing, swordfish, jellyfish)
        if(get_type()!="Riga" && get_type()!="Colonna"){
            System.out.println("Errore: invocato x-wing alle tabelle");
            return false;
        }
        if(ce_numero(numero) || conta_numero(numero)<2)
            return false;
        for(int dim=1;dim<=get_lato();dim++)
            if(conta_numero(numero)<=dim+1 && x_wing(numero,new int[0],get_index()+1,dim,sudoku))
                return true;
        return false;
    }
    private boolean x_wing(int numero,int scelti[],int inizio,int dim,Cella sudoku[][]){
        if(scelti.length==dim)
            return x_wing(numero,scelti,sudoku);
        for(int i=inizio;i<get_LATO();i++){
            Settore settore=get_type()=="Riga" ? Metodi.get_riga(i,sudoku) : Metodi.get_colonna(i,sudoku);
            if(settore.ce_numero(numero)==false && settore.conta_numero(numero)>=2 && settore.conta_numero(numero)<=dim+1)
                if(x_wing(numero,Metodi.aggiungo_int_al_vettore(scelti,i),i+1,dim,sudoku))
                    return true;
        }
        return false;
    }
    public boolean x_wing(int numero,int vet_settore[],Cella sudoku[][]){
        int vet_celle[]=Sudoku.x_wing_generale(conta_numero(numero,vet_settore,get_type(),sudoku));
        System.out.print("Cerco di fare x-wing alle "+get_type()+" numero: "+get_index()+" ");
        for(int i=0;i<vet_settore.length;i++)
            System.out.print(vet_settore[i]+" ");
        System.out.print(".Nelle celle: ");
        for(int i=0;i<vet_celle.length;i++)
            System.out.print(vet_celle[i]+" ");
        System.out.println("");
        boolean applicato=false;
        if(vet_settore.length+1==vet_celle.length){//i settori coinvolti sono il settore invocante e i settori di vet_settore per un totale di vet_settore.length+1
            int vet_settore1[]=settori_opposti(vet_settore),cont=0;
            Cella celle[]=new Cella[get_LATO()*get_LATO()];//vettore che contiene le celle di sudoku da cui e stato rimosso un candidato
            for(int i=0;i<vet_celle.length;i++){
                Settore settore=new Riga(get_LATO(),0);
                if(get_type()=="Riga")
                    settore=Metodi.get_colonna(vet_celle[i],sudoku);
                else
                    settore=Metodi.get_riga(vet_celle[i],sudoku);
                System.out.print("Cerco di rimuovere il numero "+numero+" dalle celle: ");
                for(int j=0;j<vet_settore1.length;j++)
                    System.out.print(vet_settore1[j]);
                System.out.println(" della "+settore.get_type()+" "+settore.get_index());
                int vettore[]=settore.rimuovi_candidato(numero,vet_settore1);//contiene gli indici delle celle del settore dalle quali si è eliminato un candidato
                
                if(vettore.length>0){
                    for(int j=0;j<vettore.length;j++)
                        celle[cont++]=sudoku[settore.get_cella(vettore[j]).get_riga()][settore.get_cella(vettore[j]).get_colonna()];
                    System.out.print("Applicato x-wing rimosso il numero "+numero+" dalle celle ");
                    for(int j=0;j<vettore.length;j++)
                        System.out.print(vettore[j]+" ");
                    System.out.println(" della "+settore.get_type()+" "+settore.get_index());
                    applicato=true;
                }
            }
            if(cont>0)
                Sudoku.post_x_wing(numero,Metodi.accorcio_lunghezza(celle,cont),sudoku);
        }//se i settori non formano x-wing non si fa nulla: tutti i gruppi di settori vengono provati da x_wing(numero,sudoku)
        return applicato;
    }
    public boolean xy_wing(int i,int j,Cella sudoku[][]){//vedere se si fa xy_wing fra la cella i e la cella y del settore
        //la cella i-esima è il perno e la cella j l'ala il metodo trova una erza cella vista dal perno
        boolean applicato=false;
        if(get_cella(i).get_numero()!=0 || get_cella(j).get_numero()!=0 || get_cella(i).get_candidati().length!=2 || get_cella(j).get_candidati().length!=2)
            return false;//le celle potrebbero essere cambiate a causa delle post-tecniche
        if(get_cella(i).get_candidati()[0]!=get_cella(j).get_candidati()[0] && get_cella(i).get_candidati()[0]!=get_cella(j).get_candidati()[1] && get_cella(i).get_candidati()[1]!=get_cella(j).get_candidati()[0] && get_cella(i).get_candidati()[1]!=get_cella(j).get_candidati()[1])
            return false;
        if(get_cella(i).get_candidati()[0]==get_cella(j).get_candidati()[0] && get_cella(i).get_candidati()[1]==get_cella(j).get_candidati()[1])
            if(coppie(sudoku))
                return true;
            else
                return false;
        System.out.println("La cella " + j + " della "+get_type()+" " + get_index() + " e' una possibile ala della cella con i candidati: "
            + get_cella(j).get_candidati()[0] + "," + get_cella(j).get_candidati()[1]);
        if(get_type()=="Riga"){
            if(Metodi.get_tabella(get_cella(i).get_tabella(),sudoku).xy_wing(get_cella(i),get_cella(j),sudoku))
                return true;//dopo l'applicazione le celle possono essere cambiate: non si continua con dati vecchi
            if(Metodi.get_colonna(get_cella(i).get_colonna(),sudoku).xy_wing(get_cella(i),get_cella(j),sudoku))
                return true;
        }else if(get_type()=="Tabella"){
            if(Metodi.get_colonna(get_cella(i).get_colonna(),sudoku).xy_wing(get_cella(i),get_cella(j),sudoku))
                return true;
        }else
            System.out.println("Errore invocato xy-wing alla colonna");
        return applicato;
    }
    public boolean xy_wing(Cella cella1,Cella cella2,Cella sudoku[][]){//cella1 e cella 2 hanno due candidati di cui uno in comune(uno e uno solo)
        //Il metodo trova una terza cella con i candidati giusti nel settore invocante e rimuove i candidati
        //ritorna true se ha rimosso qualche candidato. cella1 è il perno e cella2 è l'ala
        
        boolean applicato=false;
        if(cella1.get_numero()!=0 || cella2.get_numero()!=0 || cella1.get_candidati().length!=2 || cella2.get_candidati().length!=2)
            return false;//le celle potrebbero essere cambiate a causa delle post-tecniche
        int perno=Metodi.get_perno(cella1,cella2);
        if(perno<=0)//get_perno ritorna -1 se le celle non hanno candidati in comune o se hanno gli stessi candidati
            return false;
        int vet[]=Metodi.get_ali(cella1,cella2);//vet contiene i candidati da cercare nella terza cella per fare xy-wing
        int cost = 0;//cost deve essere tolto dai candidati
        if (cella2.get_candidati()[0] == perno)//il numero che va rimosso è il candidato di cella2 che non è perno
            cost =cella2.get_candidati()[1];
        else
            cost = cella2.get_candidati()[0];
        System.out.println("Cerco nella "+get_type()+" "+get_index()+" la terza cella chd forma xy-wing(Cioe' con possibili candidati: "+vet[0]+" "+vet[1]);
        Cella vet_celle[]=get_celle_se_candidati(vet,0);

        for(int i=0;i<vet_celle.length && applicato==false;i++){//dopo un'applicazione si esce: le post-tecniche cambiano le celle
            System.out.println("La cella "+vet_celle[i].get_posiz()+" forma xy-wing con le due celle precedenti");
            Cella celle_comune[] = vet_celle[i].celle_in_comune(cella2,sudoku);
            System.out.print("cerco di togliere il numero " + cost + " dalle celle: ");
            for (int j = 0; j < celle_comune.length; j++)
                System.out.print(celle_comune[j].get_posiz() + " ");
            System.out.println("");
            Cella vettore_di_celle[]=new Cella[celle_comune.length];
            int cont=0;
            for(int j=0;j<celle_comune.length;j++)
                if(celle_comune[j].rimuovi_candidato(cost)){
                    vettore_di_celle[cont++]=celle_comune[j];
                    System.out.println("Applicato xy-wing il numero "+cost+ " non puo' andare nella cella "+celle_comune[j].get_posiz());
                    applicato=true;
                }
            if(applicato)
                Sudoku.post_x_wing(cost,Metodi.accorcio_lunghezza(vettore_di_celle,cont),sudoku);
        }
        return applicato;
    }
    public boolean colore(int numero){//Nel settore numero puo andare in due celle almeno una delle qualoi è A o B
        boolean applicato=false;
        if(dove_numero(numero,0)[0].get_carattere()=='-'){
            System.out.println("Inserisco A o B nella cella "+dove_numero(numero)[0]);
            applicato=true;
            dove_numero(numero,0)[0].set_carattere(Metodi.inverto_A(dove_numero(numero,0)[1].get_carattere()));
        }else if(dove_numero(numero,0)[1].get_carattere()=='-'){
            applicato=true;
            System.out.println("Inserisco A o B nella cella "+dove_numero(numero)[1]);
            dove_numero(numero,0)[1].set_carattere(Metodi.inverto_A(dove_numero(numero,0)[0].get_carattere()));   
        }else
            System.out.println("Invocato Settore.colore(numero),ma il settore ha gia sia A che B");
        return applicato;
    }
    
}