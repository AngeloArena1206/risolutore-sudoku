package webapp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.teavm.jso.JSExport;
import sudoku.SolutionCounter;
import sudoku.Sudoku;

/**
 * Entry point of the web app.
 *
 * This class is compiled to JavaScript by TeaVM (see pom.xml) together with the whole solver, so the browser runs
 * exactly the same Java code as the desktop version. The page calls {@link #solve(String)} from a Web Worker.
 */
public class WebSolver {

    /** TeaVM needs a main method; the web app only uses the exported methods. */
    public static void main(String args[]){
    }

    /**
     * Solves a 9x9 sudoku.
     * @param puzzle 81 characters, row by row: the digits 1-9 for the givens, '0' or '.' for the empty cells
     * @return a JSON object:
     * <pre>
     * {
     *   "status":   "solved" | "unsolved" | "multiple" | "none" | "invalid",
     *   "solution": 81 characters ('0' for the cells that could not be solved),
     *   "steps":    [ the lines of the log that describe the reasoning ],
     *   "techniques": { "technique name": number of applications, ... }
     * }
     * </pre>
     */
    @JSExport
    public static String solve(String puzzle){
        if(puzzle==null || puzzle.length()!=81)
            return "{\"status\":\"invalid\",\"message\":\"The sudoku must have 81 cells\"}";
        Sudoku sudoku=new Sudoku(9);
        //the log of the solver is captured: it contains the explanation of every step
        PrintStream screen=System.out;
        ByteArrayOutputStream log=new ByteArrayOutputStream();
        String status;
        try{
            System.setOut(new PrintStream(log,true));
            for(int k=0;k<81;k++){
                char c=puzzle.charAt(k);
                if(c>='1' && c<='9')
                    sudoku.setNumber(k/9,k%9,c-'0');
            }
            int solutions=new SolutionCounter(sudoku).countSolutions(2);
            if(solutions==0)
                status="none";
            else if(solutions>1)
                status="multiple";
            else
                status=sudoku.solve() ? "solved" : "unsolved";
        }catch(RuntimeException e){
            status="invalid";
        }finally{
            System.setOut(screen);
        }
        StringBuilder json=new StringBuilder("{\"status\":\"").append(status).append("\",\"solution\":\"");
        for(int k=0;k<81;k++)
            json.append(sudoku.getCell(k).getNumber());
        json.append("\",\"steps\":[");
        boolean first=true;
        java.util.Map<String,Integer> techniques=new java.util.TreeMap<>();
        for(String line:log.toString().split("\n")){
            line=line.trim();
            if(isStep(line)==false)
                continue;
            if(line.startsWith("Applied ")){
                String name=line.substring(8);
                int end=name.length();
                int colon=name.indexOf(':'),paren=name.indexOf(" (");
                if(colon>=0)
                    end=Math.min(end,colon);
                if(paren>=0)
                    end=Math.min(end,paren);
                name=name.substring(0,end);
                if(name.startsWith("naked subset in "))
                    name="naked subset";
                techniques.merge(name,1,Integer::sum);
            }
            json.append(first ? "" : ",").append('"').append(escape(line)).append('"');
            first=false;
        }
        json.append("],\"techniques\":{");
        first=true;
        for(java.util.Map.Entry<String,Integer> entry:techniques.entrySet()){
            json.append(first ? "" : ",").append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
            first=false;
        }
        return json.append("}}").toString();
    }

    /**
     * Counts the solutions of a sudoku (stops at 2).
     * @param puzzle 81 characters as in {@link #solve(String)}
     * @return 0, 1 or 2 (2 means "more than one")
     */
    @JSExport
    public static int countSolutions(String puzzle){
        Sudoku sudoku=new Sudoku(9);
        PrintStream screen=System.out;
        try{
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            for(int k=0;k<81 && k<puzzle.length();k++){
                char c=puzzle.charAt(k);
                if(c>='1' && c<='9')
                    sudoku.setNumber(k/9,k%9,c-'0');
            }
        }finally{
            System.setOut(screen);
        }
        return new SolutionCounter(sudoku).countSolutions(2);
    }

    /**
     * @return true if a line of the log describes a step of the solution (the other lines are the "Trying ..."
     *         messages and the printed grids)
     */
    private static boolean isStep(String line){
        if(line.isEmpty())
            return false;
        return line.startsWith("Applied ")
            || line.startsWith("Subset in ") || line.startsWith("Naked subset in ")
            || line.startsWith("Unique rectangle") || line.startsWith("Two-string kite") || line.startsWith("Finned ")
            || line.startsWith("Sashimi ") || line.startsWith("W-Wing") || line.startsWith("XYZ-Wing")
            || line.startsWith("Multi-coloring") || line.startsWith("X-Cycle") || line.startsWith("AIC")
            || line.startsWith("ALS-XZ") || line.startsWith("Forcing chains") || line.startsWith("Forcing net")
            || line.startsWith("The sudoku has") || line.startsWith("Sudoku solved")
            || line.startsWith("The available techniques");
    }

    private static String escape(String text){
        StringBuilder result=new StringBuilder();
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c=='"' || c=='\\')
                result.append('\\').append(c);
            else if(c<32)
                result.append(' ');
            else
                result.append(c);
        }
        return result.toString();
    }
}
