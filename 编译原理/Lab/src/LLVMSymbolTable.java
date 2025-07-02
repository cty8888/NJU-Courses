import java.util.*;
import org.llvm4j.llvm4j.*;
public class LLVMSymbolTable {
    private Map<String,Value> globalSymbolTable;
    private Stack<Map<String,Value>> scopeStack;
    private Map<String,Map<String,Value>> functionParameters;
    private String currentFunction;
    private Map<String,Type> functionReturnType;
    public LLVMSymbolTable(){
        globalSymbolTable = new HashMap<>();
        scopeStack = new Stack<>();
        functionParameters = new HashMap<>();
        currentFunction = "";
        functionReturnType = new HashMap<>();
    }

    public String getCurrentFunction(){
        return currentFunction;
    }

    public void setFunctionReturnType(String name, Type type) {
        functionReturnType.put(name, type);
    }

    public Type getFunctionReturnType(String name){
        return functionReturnType.get(name);
    }

    public void resetCurrentFunction(){
        currentFunction = "";
    }
    public void addFunction(String name,Function function){
        globalSymbolTable.put(name,function);
        currentFunction = name;
        functionParameters.put(name,new HashMap<>());
        scopeStack.clear();
    }

    public void addGlobalVariable(String name,Value value){
        globalSymbolTable.put(name,value);
    }

    public void addLocalVariable(String name,Value value){
        scopeStack.peek().put(name,value);
    }

    public void addFunctionParameter(String name,Value value){
        addLocalVariable(name,value);
        functionParameters.get(currentFunction).put(name,value);
    }

    public Value findVariable(String name){
        for(int i = scopeStack.size()-1;i>=0;i--){
            Map<String,Value> scope = scopeStack.get(i);
            if(scope.containsKey(name)){
                return scope.get(name);
            }
        }
        if(globalSymbolTable.containsKey(name)){
            return globalSymbolTable.get(name);
        }
        return null;
    }

    public Function findFunction(String name){
        Value value = globalSymbolTable.get(name);
        if(value instanceof Function){
            return (Function) value;
        }
        return null;
    }

    public void enterScope(){
        scopeStack.push(new HashMap<>());
    }

    public void exitScope(){
        scopeStack.pop();
    }
}