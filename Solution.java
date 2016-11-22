import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

class Database{
    /* Store all (name,value) pairs */
    private final HashMap<String,String> data; 
    
    /* Store the number of certain values */
    private final HashMap<String,Integer> valNum;
    
    /* Store the reverse command of transaction in a string to rollback(only record set and unset)
      "/" represent transaction start, ";" used to delimit data commands, get will not be stored 
      This log string is like a Stack<Stack<String>> data structure */
    private final StringBuilder log;               
                                       
    public Database(){
        this.data=new HashMap<String,String>();
        this.valNum=new HashMap<String,Integer>();
        this.log=new StringBuilder();
    }
    
    /* Every set should be logged to be rollback, directly record the rollback command
      Only regular command should be logged, rollback command should not be logged */
    public void set(String name, String value, boolean rollback){ 
        if(data.containsKey(name)){
            // Update name, oldVal minus 1, reverse command is set to old value 
            String oldValue=data.get(name);
            if(valNum.get(oldValue)==1){
                valNum.remove(oldValue);
            }
            else{
                valNum.put(oldValue,valNum.get(oldValue)-1);
            }
            if(log.length()!=0&&!rollback){
                log.append("S "+name+" "+oldValue+";");
            }
        }
        else{
            // Add new name, reverse command is unset(delete)
            if(log.length()!=0&&!rollback){
                log.append("U "+name+";");            
            }
        }
        data.put(name,value);
        if(!valNum.containsKey(value)){
            valNum.put(value,1);
        }
        else{     
            valNum.put(value,valNum.get(value)+1);
        }
    }
        
    /* If get a not existed name, should return NULL */
    public String get(String name){
        if(!data.containsKey(name)){
            return "NULL";
        }
        else return data.get(name);
    }
    
    /* For unset, we can also directly set it as null, but big input may cause too many resize of hashmap, so i choose to delete */
    public void unset(String name, boolean rollback){
        if(data.containsKey(name)){
            String value=data.get(name);
            data.remove(name);
            if(valNum.get(value)==1){
                valNum.remove(value);
            }
            else{
                valNum.put(value,valNum.get(value)-1);
            }
            if(log.length()!=0&&!rollback){
                log.append("S "+name+" "+value+";");
            }
        }
        else {}
    }
    
    /* Return 0 if no such value, otherwise, get the number and return */ 
    public String numEqualTo(String value){
        if(!valNum.containsKey(value)) return ""+0;
        else return ""+valNum.get(value);
    }
    
    /* Create a new transaction starting with "/" */
    public void begin(){
        log.append("/");
    }
    
    /* Rollback if no transaction, return "NO TRANSACTION", if recent transaction is empty, just delte it, output nothing
      if recent transaction contain commands, get them and rollback all of them from last to first, set roolback boolean as true to avoid log again */
    public String rollback(){
        int length=log.length();
        if(length==0){
            return "NO TRANSACTION";
        }
        else if(log.charAt(length-1)=='/'){
            log.deleteCharAt(length-1);
            return "";
        }
        else{
            int i=length-1;
            // Check the start position of recent transaction
            while(i>0 && log.charAt(i)!='/'){           
                i--;
            }
            String commands=log.substring(i+1,length);
            log.delete(i,length);
            // Get and rollback all the commands in the recent transaction
            String[] command=commands.split(";");         
            for(int j=command.length-1;j>=0;j--){         
                String[] splitArray=command[j].split(" ");
                if(splitArray[0].equals("S")){
                    set(splitArray[1],splitArray[2],true);
                }
                else{
                    unset(splitArray[1],true);
                }
            }
            return "";
        }
    }
    
    /* According to my implementation, get command before commit will return new value, but if rollback, will return old value
      As i consider about it, commit only means permanent close the transaction and no rollback should be used again */
    public String commit(){
        if(log.length()==0){
            return "NO TRANSACTION";
        }
        else{
            log.setLength(0);
            return "";
        }
    }
}

public class Solution {
    private static final Database database=new Database();
    
    /* Scan all the commands line by line from stdin */
    private static void scanInput(Scanner in) throws Exception{
        String line;
        while(in.hasNext()){
            line=in.next();
            System.out.println(line);
            executeInput(line);  
        }
    }
    
    /* Only used for system output */
    private static void printResult(String result){
        if(result.length()!=0){
            System.out.println("> "+result);
        }
    }
    
    /* Used for check input commands. I don't know whether the command with extra whitespace should work or not, so I just consider them all as illegal
      No extra whitespace permitted, case sensitive, "SET a 3 ", " SET a 3", "Set a 3", "Begin", "begin" are all illegal input, only "SET a 3" is legal
      I output "ERROR INPUT" when meet illegal input */
    private static void executeInput(String line){
        /* Pattern can be easily changed if future changes needed, to make code simple, i directly use equals() to compare commands with only one 
          string, in real world, we should also consider use patterns to make code more flexible for future changes */
        Pattern setPattern=Pattern.compile("^SET\\s[^\\s]+\\s[^\\s]+$");
        Pattern getPattern=Pattern.compile("^GET\\s[^\\s]+$");
        Pattern unsetPattern=Pattern.compile("^UNSET\\s[^\\s]+$");
        Pattern numPattern=Pattern.compile("^NUMEQUALTO\\s[^\\s]+$");
        if(line.equals("BEGIN")){         
            database.begin();          
        }
        else if(line.equals("ROLLBACK")){
            String result=database.rollback();
            printResult(result);
        }
        else if(line.equals("COMMIT")){
            String result=database.commit();
            printResult(result);
        }
        else if(setPattern.matcher(line).matches()){
            String[] set=line.split(" ");
            database.set(set[1],set[2],false);
        }
        else if(getPattern.matcher(line).matches()){
            String[] get=line.split(" ");
            String result=database.get(get[1]);
            printResult(result);
        }
        else if(unsetPattern.matcher(line).matches()){
            String[] unset=line.split(" ");
            database.unset(unset[1],false);
        }
        else if(numPattern.matcher(line).matches()){
            String[] numEqualTo=line.split(" ");
            String result=database.numEqualTo(numEqualTo[1]);
            printResult(result);
        }
        else if(line.equals("END")){
            return;
        }
        else{
            printResult("ERROR INPUT");       //You can delete this if your test cases need me to ignore all illegal input
        }
    }
    
    public static void main(String args[] ) throws Exception {
        /* Enter your code here. Read input from STDIN. Print output to STDOUT */
        Scanner in=new Scanner(System.in);
        in.useDelimiter("\n");
        scanInput(in);
    }
}
