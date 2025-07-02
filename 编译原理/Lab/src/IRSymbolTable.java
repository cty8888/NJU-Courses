import org.llvm4j.llvm4j.*;
import java.util.*;

public class IRSymbolTable {
    // 全局符号表：存储全局变量和函数
    private Map<String, Value> globalSymbolTable = new HashMap<>();
    // 当前正在处理的函数名
    private String currentFunction = "";
    // 作用域栈：第一层为函数作用域，后续为嵌套块中的局部变量
    private Stack<Map<String, Value>> scopeStack = new Stack<>();
    // 函数参数集合：记录每个函数的参数名称
    private Map<String, Set<String>> functionParameters = new HashMap<>();

    private Map<String,Type> functionReturnType = new HashMap<>();

    public void setCurrentFunction(String functionName) {
        this.currentFunction = functionName;
        scopeStack.clear();
        scopeStack.push(new HashMap<>());
        functionParameters.put(functionName, new HashSet<>());
    }
    

    public String getCurrentFunction() {
        return currentFunction;
    }
    public void addFunction(String name, Function function) {
        globalSymbolTable.put(name, function);
        setCurrentFunction(name);
    }
    public void setFunctionReturnType(String name, Type type) {
        functionReturnType.put(name, type);
    }

    public void addGlobalVariable(String name, Value value) {
        globalSymbolTable.put(name, value);
    }

    public void addLocalVariable(String name, Value value) {
        scopeStack.peek().put(name, value);
    }
    
    public void addFunctionParameter(String name, Value value) {
        addLocalVariable(name, value);
        functionParameters.get(currentFunction).add(name);// 标记为函数参数
    }

    public Value findVariable(String name) {
        // 从作用域栈顶到底查找（从内层到外层）
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Value> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        // 最后查找全局变量
        if (globalSymbolTable.containsKey(name)) {
            return globalSymbolTable.get(name);
        }
        return null; 
    }

    public Function findFunction(String name) {
        Value value = globalSymbolTable.get(name);
        if (value instanceof Function) {
            return (Function) value;
        }
        return null;
    }

    public void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    public void exitScope() {
        scopeStack.pop();
    }
}