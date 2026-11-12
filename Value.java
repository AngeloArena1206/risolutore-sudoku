/**
 * Simple mutable holder for an integer value.
 * It is not used by the solver: it is a leftover of the first experiments of the project.
 */
public class Value{
    private int value;
    public void setValue(int value){
        this.value=value;
    }
    public int getValue(){
        return value;
    }
}
