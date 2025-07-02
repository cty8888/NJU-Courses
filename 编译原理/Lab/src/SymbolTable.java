import java.util.Stack;

public class SymbolTable {
    private Scope globalScope;
    private Scope currentScope;
    private Stack<Scope> scopeStack;

    public SymbolTable(){
        globalScope = new Scope(null);
        currentScope = globalScope;
        scopeStack = new Stack<>();
        scopeStack.push(globalScope);
    }

    public Scope enterScope(){
        Scope newScope = new Scope(currentScope);
        scopeStack.push(newScope);
        currentScope = newScope;
        return newScope;
    }
    public Scope exitScope(){
        if(scopeStack.size()<=1){
            throw new IllegalStateException("can not exit global scope");
        }
        scopeStack.pop();
        currentScope = scopeStack.peek();
        return currentScope;
    }

    public void addSymbol(String name, Types type){
        if(currentScope.find(name)!=null){
            throw new IllegalStateException("symbol "+name+" already defined in current scope");
        }
        currentScope.put(name, type);
    }
    public void addGlobalSymbol(String name, Types type){
        if(globalScope.find(name)!=null){
            throw new IllegalStateException("symbol "+name+" already defined in global scope");
        }
        globalScope.put(name, type);
    }
    public Types findSymbol(String name){
        return currentScope.find(name);
    }
    public Types findGlobalSymbol(String name){
        return globalScope.find(name);
    }
    public Types lookupSymbol(String name){
        return currentScope.lookup(name);
    }
    public int getScopeDepth(){
        return scopeStack.size();
    }
    public boolean isGlobalScope(){
        return scopeStack.size()==1;
    }



    
}
