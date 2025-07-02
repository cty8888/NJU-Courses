import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.*;

public class ConstantPropagationPass implements OptimizationPass {
   
    private Map<LLVMValueRef, Value> globalVariables = new HashMap<>();
    private Set<LLVMValueRef> modifiedGlobals = new HashSet<>();
    
    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        performConstantPropagation(module);
        return module;
    }
    
    @Override
    public String getName() {
        return "ConstantPropagation";
    }
    
    private static class Value {
        enum Type { UNDEF, CONST, NAC }
        
        private Type type;
        private long constValue; 
        
        // 创建UNDEF值
        public static Value getUndef() {
            Value val = new Value();
            val.type = Type.UNDEF;
            return val;
        }
        
        // 创建常量值
        public static Value getConst(long value) {
            Value val = new Value();
            val.type = Type.CONST;
            val.constValue = value;
            return val;
        }
        
        // 创建NAC值
        public static Value getNAC() {
            Value val = new Value();
            val.type = Type.NAC;
            return val;
        }
        
        public boolean isUndef() {
            return type == Type.UNDEF;
        }
        
        public boolean isConst() {
            return type == Type.CONST;
        }
        
        public boolean isNAC() {
            return type == Type.NAC;
        }
        
        public long getConstValue() {
            if (!isConst()) {
                throw new IllegalStateException("Value is not a constant");
            }
            return constValue;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Value)) {
                return false;
            }
            Value other = (Value) obj;
            if (type != other.type) {
                return false;
            }
            if (type == Type.CONST) {
                return constValue == other.constValue;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, constValue);
        }
        
        @Override
        public String toString() {
            switch (type) {
                case UNDEF: return "UNDEF";
                case CONST: return "CONST(" + constValue + ")";
                case NAC: return "NAC";
                default: return "UNKNOWN";
            }
        }
    }
    

    private static class Instruction {
        LLVMValueRef instruction;
        Set<Instruction> predecessors = new HashSet<>();
        Set<Instruction> successors = new HashSet<>();
        Map<LLVMValueRef, Value> in = new HashMap<>();  
        Map<LLVMValueRef, Value> out = new HashMap<>(); 
        
        public Instruction(LLVMValueRef instruction) {
            this.instruction = instruction;
        }
    }
    
    private void performConstantPropagation(LLVMModuleRef module) {
   
        findGlobalStores(module);
        
    
        collectGlobalVariables(module);
        
        List<Instruction> allInstructions = buildCFG(module);
        
 
        for (Instruction inst : allInstructions) {
            if (LLVMGetTypeKind(LLVMTypeOf(inst.instruction)) != LLVMVoidTypeKind) {
                inst.out.put(inst.instruction, Value.getUndef());
            }
        }
        
        List<Instruction> worklist = new ArrayList<>(allInstructions);
        while (!worklist.isEmpty()) {    
            Instruction inst = worklist.remove(0);
            Map<LLVMValueRef, Value> oldOut = new HashMap<>(inst.out);
            
            if (!inst.predecessors.isEmpty()) {
                for (Instruction pred : inst.predecessors) {
                    for (Map.Entry<LLVMValueRef, Value> entry : pred.out.entrySet()) {
                        LLVMValueRef var = entry.getKey();
                        Value predVal = entry.getValue();
                        Value currentVal = inst.in.getOrDefault(var, Value.getUndef());
                        inst.in.put(var, meet(currentVal, predVal));
                    }
                }
            }
            
            transfer(inst);
            
            if (!mapsEqual(oldOut, inst.out)) {
                for (Instruction succ : inst.successors) {
                    if (!worklist.contains(succ)) {
                        worklist.add(succ);
                    }
                }
            }
        }
        applyOptimization(allInstructions);
    }
    
    private void findGlobalStores(LLVMModuleRef module) {
        for (LLVMValueRef function = LLVMGetFirstFunction(module); function != null; function = LLVMGetNextFunction(function)) {
            for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); block != null; block = LLVMGetNextBasicBlock(block)) {
                for (LLVMValueRef instruction = LLVMGetFirstInstruction(block); instruction != null; instruction = LLVMGetNextInstruction(instruction)) {
                    int opcode = LLVMGetInstructionOpcode(instruction);
                    if (opcode == LLVMStore) {
                        LLVMValueRef pointerOperand = LLVMGetOperand(instruction, 1);
                        if (LLVMIsAGlobalVariable(pointerOperand) != null) {
                            modifiedGlobals.add(pointerOperand);
                        }
                    }
                }
            }
        }
    }

    private void collectGlobalVariables(LLVMModuleRef module) {
        for (LLVMValueRef global = LLVMGetFirstGlobal(module); global != null; global = LLVMGetNextGlobal(global)) {
            // 只有非常量且有初始值且未被修改的全局变量才被视为常量
            if ((LLVMIsGlobalConstant(global) != 0) || 
                (LLVMGetInitializer(global) != null && !modifiedGlobals.contains(global))) {
                LLVMValueRef initializer = LLVMGetInitializer(global);
                if (initializer != null && LLVMIsConstant(initializer) != 0) {
                    long constValue = LLVMConstIntGetSExtValue(initializer);
                    globalVariables.put(global, Value.getConst(constValue));
                } else if (initializer != null && LLVMIsUndef(initializer) == 0) {
                    // 如果初始值不是常量，标记为NAC
                    globalVariables.put(global, Value.getNAC());
                }
            } else if (modifiedGlobals.contains(global)) {
                // 被修改过的全局变量标记为NAC
                globalVariables.put(global, Value.getNAC());
            }
        }
    }
    
    private Value meet(Value x, Value y) {
        if (x.equals(y)) {
            return x;
        }
        if (x.isUndef()) {
            return y;
        }
        if (y.isUndef()) {
            return x;
        }
        return Value.getNAC();
    }
    

    private void transfer(Instruction inst) {
        for (Map.Entry<LLVMValueRef, Value> entry : inst.in.entrySet()) {
            inst.out.put(entry.getKey(), entry.getValue());
        }
        
        int opcode = LLVMGetInstructionOpcode(inst.instruction);
        
        if (opcode == LLVMLoad) {
            LLVMValueRef pointerOperand = LLVMGetOperand(inst.instruction, 0);
            
            if (LLVMIsAGlobalVariable(pointerOperand) != null) {
                Value globalValue = globalVariables.get(pointerOperand);
                if (globalValue != null && globalValue.isConst()) {
                    inst.out.put(inst.instruction, globalValue);
                    return;
                }
            }
            
            // Handle normal load as before
            Value value = inst.in.getOrDefault(pointerOperand, Value.getUndef());
            inst.out.put(inst.instruction, value);
        } 
        else if (opcode == LLVMStore) {
            LLVMValueRef valueOperand = LLVMGetOperand(inst.instruction, 0);
            LLVMValueRef pointerOperand = LLVMGetOperand(inst.instruction, 1);
            
            // Special handling for global variables - never update the globalVariables map
            // during analysis, as that was pre-computed
            
            if (LLVMIsConstant(valueOperand) != 0) {
                long constValue = LLVMConstIntGetSExtValue(valueOperand);
                inst.out.put(pointerOperand, Value.getConst(constValue));
            } else {
                Value value = inst.in.getOrDefault(valueOperand, Value.getUndef());
                if (value.isConst()) {
                    inst.out.put(pointerOperand, value);
                } else if (value.isNAC()) {
                    inst.out.put(pointerOperand, Value.getNAC());
                } else {
                    inst.out.put(pointerOperand, Value.getUndef());
                }
            }
        } 
        else if (opcode == LLVMAdd || opcode == LLVMSub || opcode == LLVMMul || 
                 opcode == LLVMSDiv || opcode == LLVMUDiv || opcode == LLVMSRem || 
                 opcode == LLVMURem) {
            
            LLVMValueRef lhs = LLVMGetOperand(inst.instruction, 0);
            LLVMValueRef rhs = LLVMGetOperand(inst.instruction, 1);
            
            Value lhsValue, rhsValue;
            
            if (LLVMIsConstant(lhs) != 0) {
                lhsValue = Value.getConst(LLVMConstIntGetSExtValue(lhs));
            } else {
                lhsValue = inst.in.getOrDefault(lhs, Value.getUndef());
            }
            
            if (LLVMIsConstant(rhs) != 0) {
                rhsValue = Value.getConst(LLVMConstIntGetSExtValue(rhs));
            } else {
                rhsValue = inst.in.getOrDefault(rhs, Value.getUndef());
            }
            
            if (lhsValue.isConst() && rhsValue.isConst()) {
                long result;
                try {
                    switch (opcode) {
                        case LLVMAdd:
                            result = lhsValue.getConstValue() + rhsValue.getConstValue();
                            break;
                        case LLVMSub:
                            result = lhsValue.getConstValue() - rhsValue.getConstValue();
                            break;
                        case LLVMMul:
                            result = lhsValue.getConstValue() * rhsValue.getConstValue();
                            break;
                        case LLVMSDiv:
                        case LLVMUDiv:
                            if (rhsValue.getConstValue() == 0) {
                                // 除以0，结果是NAC
                                inst.out.put(inst.instruction, Value.getNAC());
                                return;
                            }
                            result = lhsValue.getConstValue() / rhsValue.getConstValue();
                            break;
                        case LLVMSRem:
                        case LLVMURem:
                            if (rhsValue.getConstValue() == 0) {
                                // 除以0，结果是NAC
                                inst.out.put(inst.instruction, Value.getNAC());
                                return;
                            }
                            result = lhsValue.getConstValue() % rhsValue.getConstValue();
                            break;
                        default:
                            inst.out.put(inst.instruction, Value.getNAC());
                            return;
                    }
                    inst.out.put(inst.instruction, Value.getConst(result));
                } catch (Exception e) {
                    inst.out.put(inst.instruction, Value.getNAC());
                }
            } else if (lhsValue.isNAC() || rhsValue.isNAC()) {
                inst.out.put(inst.instruction, Value.getNAC());
            } else {

                inst.out.put(inst.instruction, Value.getUndef());
            }
        }
        // 处理比较指令
        else if (opcode == LLVMICmp) {
            LLVMValueRef lhs = LLVMGetOperand(inst.instruction, 0);
            LLVMValueRef rhs = LLVMGetOperand(inst.instruction, 1);
            int predicate = LLVMGetICmpPredicate(inst.instruction);
            
            Value lhsValue, rhsValue;
            
            // 获取左操作数的值
            if (LLVMIsConstant(lhs) != 0) {
                lhsValue = Value.getConst(LLVMConstIntGetSExtValue(lhs));
            } else {
                lhsValue = inst.in.getOrDefault(lhs, Value.getUndef());
            }
            
            // 获取右操作数的值
            if (LLVMIsConstant(rhs) != 0) {
                rhsValue = Value.getConst(LLVMConstIntGetSExtValue(rhs));
            } else {
                rhsValue = inst.in.getOrDefault(rhs, Value.getUndef());
            }
            
            // 如果两个操作数都是常量，则计算结果
            if (lhsValue.isConst() && rhsValue.isConst()) {
                boolean result;
                long lval = lhsValue.getConstValue();
                long rval = rhsValue.getConstValue();
                
                switch (predicate) {
                    case LLVMIntEQ:
                        result = lval == rval;
                        break;
                    case LLVMIntNE:
                        result = lval != rval;
                        break;
                    case LLVMIntSGT:
                        result = lval > rval;
                        break;
                    case LLVMIntSGE:
                        result = lval >= rval;
                        break;
                    case LLVMIntSLT:
                        result = lval < rval;
                        break;
                    case LLVMIntSLE:
                        result = lval <= rval;
                        break;
                    case LLVMIntUGT:
                        result = Long.compareUnsigned(lval, rval) > 0;
                        break;
                    case LLVMIntUGE:
                        result = Long.compareUnsigned(lval, rval) >= 0;
                        break;
                    case LLVMIntULT:
                        result = Long.compareUnsigned(lval, rval) < 0;
                        break;
                    case LLVMIntULE:
                        result = Long.compareUnsigned(lval, rval) <= 0;
                        break;
                    default:
                        // 其他未处理的比较，结果是NAC
                        inst.out.put(inst.instruction, Value.getNAC());
                        return;
                }
                
                inst.out.put(inst.instruction, Value.getConst(result ? 1 : 0));
            } else if (lhsValue.isNAC() || rhsValue.isNAC()) {
                // 如果任一操作数是NAC，则结果也是NAC
                inst.out.put(inst.instruction, Value.getNAC());
            } else {
                // 否则结果是UNDEF
                inst.out.put(inst.instruction, Value.getUndef());
            }
        }
        // 处理ZExt和SExt指令（零扩展和符号扩展）
        else if (opcode == LLVMZExt || opcode == LLVMSExt) {
            LLVMValueRef operand = LLVMGetOperand(inst.instruction, 0);
            
            if (LLVMIsConstant(operand) != 0) {
                long constValue = LLVMConstIntGetSExtValue(operand);
                inst.out.put(inst.instruction, Value.getConst(constValue));
            } else {
                Value value = inst.in.getOrDefault(operand, Value.getUndef());
                inst.out.put(inst.instruction, value);
            }
        }
        else if (opcode == LLVMAlloca) {
            inst.out.put(inst.instruction, Value.getUndef());
        }
    }
    

    private List<Instruction> buildCFG(LLVMModuleRef module) {
        List<Instruction> allInstructions = new ArrayList<>();
        Map<LLVMValueRef, Instruction> instructionMap = new HashMap<>();
        Map<LLVMBasicBlockRef, List<Instruction>> blockInstructions = new HashMap<>();
        for (LLVMValueRef function = LLVMGetFirstFunction(module); function != null; function = LLVMGetNextFunction(function)) {
            for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); block != null; block = LLVMGetNextBasicBlock(block)) {
                List<Instruction> instList = new ArrayList<>();
                blockInstructions.put(block, instList);
                for (LLVMValueRef instruction = LLVMGetFirstInstruction(block); instruction != null; instruction = LLVMGetNextInstruction(instruction)) {
                    Instruction inst = new Instruction(instruction);
                    instructionMap.put(instruction, inst);
                    allInstructions.add(inst);
                    instList.add(inst);
                }
            }
        }
        for (List<Instruction> instList : blockInstructions.values()) {
            if (instList.size() <= 1) continue;
            for (int i = 0; i < instList.size() - 1; i++) {
                Instruction current = instList.get(i);
                Instruction next = instList.get(i + 1);
                int opcode = LLVMGetInstructionOpcode(current.instruction);
                if (opcode != LLVMRet && opcode != LLVMBr ) {
                    current.successors.add(next);
                    next.predecessors.add(current);
                }
            }
        }
        
        for (LLVMValueRef function = LLVMGetFirstFunction(module); function != null; function = LLVMGetNextFunction(function)) {
            for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); block != null; block = LLVMGetNextBasicBlock(block)) {
                LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
                if (terminator == null) continue;  
                int opcode = LLVMGetInstructionOpcode(terminator);
                Instruction terminatorInst = instructionMap.get(terminator);
                if (opcode == LLVMBr) {
                    int numOperands = LLVMGetNumOperands(terminator);
                    if (numOperands == 1) {
                        LLVMBasicBlockRef destBlock = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 0));
                        List<Instruction> destInstructions = blockInstructions.get(destBlock);
                        if (destInstructions != null && !destInstructions.isEmpty()) {
                            Instruction destFirstInst = destInstructions.get(0);
                            terminatorInst.successors.add(destFirstInst);
                            destFirstInst.predecessors.add(terminatorInst);
                        }
                    }
                    else if (numOperands == 3) {
                        LLVMBasicBlockRef trueBlock = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 1));
                        List<Instruction> trueInstructions = blockInstructions.get(trueBlock);
                        if (trueInstructions != null && !trueInstructions.isEmpty()) {
                            Instruction trueFirstInst = trueInstructions.get(0);
                            terminatorInst.successors.add(trueFirstInst);
                            trueFirstInst.predecessors.add(terminatorInst);
                        }

                        LLVMBasicBlockRef falseBlock = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 2));
                        List<Instruction> falseInstructions = blockInstructions.get(falseBlock);
                        if (falseInstructions != null && !falseInstructions.isEmpty()) {
                            Instruction falseFirstInst = falseInstructions.get(0);
                            terminatorInst.successors.add(falseFirstInst);
                            falseFirstInst.predecessors.add(terminatorInst);
                        }
                    }
                }
            }
        }
        return allInstructions;
    }
    
 
    private void applyOptimization(List<Instruction> allInstructions) {
        for (Instruction inst : allInstructions) {
            Value value = inst.out.get(inst.instruction);
            if (value != null && value.isConst()) {
 
                LLVMTypeRef type = LLVMTypeOf(inst.instruction);
 
                if (LLVMGetTypeKind(type) == LLVMVoidTypeKind) {
                    continue;
                }
                
                try {
                    LLVMValueRef constValue = LLVMConstInt(type, value.getConstValue(), 0);
                    
  
                    LLVMReplaceAllUsesWith(inst.instruction, constValue);
   
                    LLVMInstructionEraseFromParent(inst.instruction);
                } catch (Exception e) {
                    System.err.println("Error replacing instruction with constant: " + e.getMessage());
                }
            }
        }
    }

    private boolean mapsEqual(Map<LLVMValueRef, Value> map1, Map<LLVMValueRef, Value> map2) {
        if (map1.size() != map2.size()) {
            return false;
        }
        
        for (Map.Entry<LLVMValueRef, Value> entry : map1.entrySet()) {
            Value value1 = entry.getValue();
            Value value2 = map2.get(entry.getKey());
            if (value2 == null || !value1.equals(value2)) {
                return false;
            }
        }
        
        return true;
    }
} 