import java.util.ArrayList;
import java.util.List;
public abstract class Types {
 
    @Override
    public abstract boolean equals(Object obj);
    public boolean isInt() {
        return this instanceof IntType;
    }
    public boolean isArray() {
        return this instanceof ArrayType;
    }
    
    public boolean isFunction() {
        return this instanceof FuncType;
    }

    public boolean isWrong() {
        return this instanceof WrongType;
    }
    public boolean isVoid() {
        return this instanceof Void_Type;
    }
}

class WrongType extends Types {
    private static final WrongType instance = new WrongType();
    private WrongType() {}
    public static WrongType getInstance() {
        return instance;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof WrongType;
    }
}
class IntType extends Types {
    private static final IntType instance = new IntType();
    
    // 私有构造函数
    private IntType() {}
    
    public static IntType getInstance() {
        return instance;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntType;
    }
    
}

class Void_Type extends Types {
    // 使用单例模式
    private static final Void_Type instance = new Void_Type();
    
    // 私有构造函数
    private Void_Type() {}
    
    // 获取VoidType实例的方法
    public static Void_Type getInstance() {
        return instance;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Void_Type;
    }
    
}


class ArrayType extends Types {
    Types containedType;
    int num_elements;
    public ArrayType(Types containedType, int num_elements) {
        this.containedType = containedType;
        this.num_elements = num_elements;
    }
    public Types getElementType() {
        return containedType;
    }
    
    public int getElementCount() {
        return num_elements;
    }
    
    public int getDimension() {
        if (containedType instanceof ArrayType) {
            return 1 + ((ArrayType) containedType).getDimension();
        }
        return 1; // 一维数组
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ArrayType)) {
            return false;
        }
        
        ArrayType other = (ArrayType) obj;
        // 数组类型相等只要求元素类型相等，不要求数量相等
        return containedType.equals(other.containedType);
    }
    
}


class FuncType extends Types {
    Types returnType;

    ArrayList<Types> paramTypes;
    
    public FuncType(Types returnType, ArrayList<Types> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = new ArrayList<>(paramTypes); 
    }

    public Types getReturnType() {
        return returnType;
    }
    
    public ArrayList<Types> getParamTypes() {
        return new ArrayList<>(paramTypes); 
    }
    
    public int getParamCount() {
        return paramTypes.size();
    }
    
    public boolean checkParams(List<Types> argTypes) {
        if (paramTypes.size() != argTypes.size()) {
            return false;
        }
        
        for (int i = 0; i < paramTypes.size(); i++) {
            Types paramType = paramTypes.get(i);
            Types argType = argTypes.get(i);
            
            // 如果参数类型不匹配
            if (!paramType.equals(argType)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FuncType)) {
            return false;
        }
        
        FuncType other = (FuncType) obj;
        
        if (!returnType.equals(other.returnType)) {
            return false;
        }
        
        if (paramTypes.size() != other.paramTypes.size()) {
            return false;
        }
        
        for (int i = 0; i < paramTypes.size(); i++) {
            if (!paramTypes.get(i).equals(other.paramTypes.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
}