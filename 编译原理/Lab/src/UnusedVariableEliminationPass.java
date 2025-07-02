import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.*;


public class UnusedVariableEliminationPass implements OptimizationPass {
    
    private Set<LLVMValueRef> usedVariables = new HashSet<>();//记录被使用的变量
    private Set<LLVMValueRef> allVariables = new HashSet<>();//记录被使用的变量
    private Set<LLVMValueRef> globalVariables = new HashSet<>();//记录所有全局变量（不会被消除）
    private Map<LLVMValueRef, List<LLVMValueRef>> variableDefInstructions = new HashMap<>();//记录每个变量的定义指令
    private Set<LLVMValueRef> toRemove = new HashSet<>();//标记需要被移除的指令和变量
    
    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        usedVariables.clear();
        allVariables.clear();
        globalVariables.clear();
        variableDefInstructions.clear();
        toRemove.clear();
        
        collectGlobalVariables(module);
        
        for (LLVMValueRef function = LLVMGetFirstFunction(module); function != null; function = LLVMGetNextFunction(function)) {
            identifyVariablesAndUsages(function);
            markUnusedVariables();
            removeUnusedVariables(function);
        }
        return module;
    }
    
    @Override
    public String getName() {
        return "UnusedVariableElimination";
    }
    

    private void collectGlobalVariables(LLVMModuleRef module) {
        for (LLVMValueRef global = LLVMGetFirstGlobal(module); global != null; global = LLVMGetNextGlobal(global)) {
            globalVariables.add(global);
        }
    }
    

    private void identifyVariablesAndUsages(LLVMValueRef function) {
        for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); block != null; block = LLVMGetNextBasicBlock(block)) {
            for (LLVMValueRef instruction = LLVMGetFirstInstruction(block); instruction != null; instruction = LLVMGetNextInstruction(instruction)) {
                int opcode = LLVMGetInstructionOpcode(instruction);
                if (opcode == LLVMAlloca) {
                    allVariables.add(instruction);
                    variableDefInstructions.computeIfAbsent(instruction, k -> new ArrayList<>()).add(instruction);
                } 
                else if (opcode == LLVMStore) {
                    LLVMValueRef valueOperand = LLVMGetOperand(instruction, 0);
                    LLVMValueRef pointerOperand = LLVMGetOperand(instruction, 1);

                    if(LLVMIsConstant(valueOperand) == 0 && allVariables.contains(valueOperand)){
                        usedVariables.add(valueOperand);
                    }
                    if(LLVMIsAGlobalVariable(pointerOperand) == null){
                        variableDefInstructions.computeIfAbsent(pointerOperand, k -> new ArrayList<>()).add(instruction);
                    }
                } 
                else if (opcode == LLVMLoad) {
                    LLVMValueRef pointerOperand = LLVMGetOperand(instruction, 0);
                    
                    if(LLVMIsAGlobalVariable(pointerOperand) == null && allVariables.contains(pointerOperand)){
                        usedVariables.add(pointerOperand);
                    }
                    
                    allVariables.add(instruction);
                    variableDefInstructions.computeIfAbsent(instruction, k -> new ArrayList<>()).add(instruction);
                } 
                else if (opcode == LLVMBr) {
                    int numOperands = LLVMGetNumOperands(instruction);
                    if (numOperands == 3) {
                        LLVMValueRef condition = LLVMGetOperand(instruction, 0);
                        if (allVariables.contains(condition)) {
                            usedVariables.add(condition);
                        }
                    }
                }
                else if (opcode == LLVMRet) {
                    if (LLVMGetNumOperands(instruction) > 0) {
                        LLVMValueRef returnValue = LLVMGetOperand(instruction, 0);
                        if (allVariables.contains(returnValue)) {
                            usedVariables.add(returnValue);
                        }
                    }
                }
                else {
                    int numOperands = LLVMGetNumOperands(instruction);
                    for (int i = 0; i < numOperands; i++) {
                        LLVMValueRef operand = LLVMGetOperand(instruction, i);
                        if (allVariables.contains(operand)) {
                            usedVariables.add(operand);
                        }
                    }
                }
            }
        }
    }
    

    private void markUnusedVariables() {
        for (LLVMValueRef variable : allVariables) {
            if (!usedVariables.contains(variable) && !globalVariables.contains(variable)) {
                toRemove.add(variable);
            }
        }
    }
    

    private void removeUnusedVariables(LLVMValueRef function) {
        boolean changed;
        int safetyCounter = 0; 
        final int MAX_ITERATIONS = 10;
        
        do {
            changed = false;
            safetyCounter++;
            

            Set<LLVMValueRef> instructionsToRemove = new HashSet<>();
            
            for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
                 block != null; 
                 block = LLVMGetNextBasicBlock(block)) {
                
                for (LLVMValueRef instruction = LLVMGetFirstInstruction(block); 
                     instruction != null; 
                     instruction = LLVMGetNextInstruction(instruction)) {
                    
                    int opcode = LLVMGetInstructionOpcode(instruction);
                    
                    if (opcode == LLVMAlloca && toRemove.contains(instruction)) {
                        instructionsToRemove.add(instruction);
                    }
                    else if (opcode == LLVMStore) {
                        LLVMValueRef pointerOperand = LLVMGetOperand(instruction, 1);
                        
                        if (allVariables.contains(pointerOperand) && toRemove.contains(pointerOperand)) {
                            instructionsToRemove.add(instruction);
                        }
                    }
                    else if (opcode == LLVMLoad) {
                        LLVMValueRef pointerOperand = LLVMGetOperand(instruction, 0);
                        
                        if (allVariables.contains(pointerOperand) && toRemove.contains(pointerOperand)) {
                            toRemove.add(instruction);
                            instructionsToRemove.add(instruction);
                        }
                    }
                }
            }
            

            boolean dependencyFound;
            do {
                dependencyFound = false;
                
                for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
                     block != null; 
                     block = LLVMGetNextBasicBlock(block)) {
                    
                    for (LLVMValueRef instruction = LLVMGetFirstInstruction(block); 
                         instruction != null; 
                         instruction = LLVMGetNextInstruction(instruction)) {
                        
                        // Skip instructions already marked for removal
                        if (instructionsToRemove.contains(instruction)) {
                            continue;
                        }
                        
                        int numOperands = LLVMGetNumOperands(instruction);
                        boolean hasRemovedOperand = false;
                        
                        for (int i = 0; i < numOperands; i++) {
                            LLVMValueRef operand = LLVMGetOperand(instruction, i);
                            if (toRemove.contains(operand) || instructionsToRemove.contains(operand)) {
                                hasRemovedOperand = true;
                                break;
                            }
                        }
                        
                        if (hasRemovedOperand) {
                            instructionsToRemove.add(instruction);
                            toRemove.add(instruction);
                            dependencyFound = true;
                        }
                    }
                }
            } while (dependencyFound);
            

            if (!instructionsToRemove.isEmpty()) {
                for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
                     block != null; 
                     block = LLVMGetNextBasicBlock(block)) {
                    
                    LLVMValueRef instruction = LLVMGetFirstInstruction(block);
                    while (instruction != null) {
                        LLVMValueRef nextInstruction = LLVMGetNextInstruction(instruction);
                        
                        if (instructionsToRemove.contains(instruction)) {
                            try {
                                LLVMInstructionEraseFromParent(instruction);
                                changed = true;
                            } catch (Exception e) {

                                instructionsToRemove.remove(instruction);
                                toRemove.remove(instruction);
                            }
                        }
                        
                        instruction = nextInstruction;
                    }
                }
            }
            

            if (changed) {
                usedVariables.clear();
                allVariables.clear();
                variableDefInstructions.clear();
                toRemove.clear();
                
                identifyVariablesAndUsages(function);
                markUnusedVariables();
            }
            
        } while (changed && safetyCounter < MAX_ITERATIONS);
    }
} 