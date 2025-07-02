
import java.util.HashMap;
public class Scope {
    public HashMap<String, Types> symbols;
    public Scope parent;
    public String name;
    public Scope(Scope parent) {
        this.parent = parent;
        this.symbols = new HashMap<>();
    }   
    
    public Types find(String name){
        if(symbols.containsKey(name)){
            return symbols.get(name);
        }
        return null;
    }

    public void put(String name, Types type){
        symbols.put(name, type);
    }
    
    public Types lookup(String name){
        Types type = find(name);
        if(type!=null){
            return type;
        }
        if(parent!=null){
            return parent.lookup(name);
        }
        return null;
    }
}
