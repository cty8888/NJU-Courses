import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.*;

public class DeadCodeEliminationPass implements OptimizationPass {
    
    private Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> incomingEdges; // 到达该块的边
    private Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> outgoingEdges; // 从该块出发的边
    private Set<LLVMBasicBlockRef> reachableBlocks; // 可达基本块集合
    
    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            if (LLVMCountBasicBlocks(function) > 0) {
                optimizeFunction(function);
            }
            function = LLVMGetNextFunction(function);
        }
        
        return module;
    }
    
    @Override
    public String getName() {
        return "DeadCodeElimination";
    }
    

    private void optimizeFunction(LLVMValueRef function) {
        // 初始化控制流图数据结构
        incomingEdges = new HashMap<>();
        outgoingEdges = new HashMap<>();
        reachableBlocks = new HashSet<>();
        

        final int MAX_ITERATIONS = 20;
        int iterations = 0;
        boolean madeChanges;
        
        do {
          
            analyzeControlFlow(function);
            
         
            madeChanges = false;
            
            
            madeChanges |= optimizeConstantConditions(function);
            
            
            if (madeChanges) {
                analyzeControlFlow(function);
            }
            
            // 尝试删除不可达代码
            madeChanges |= removeDeadBlocks(function);
            
           
            if (madeChanges) {
                analyzeControlFlow(function);
            }
            
           
            madeChanges |= mergeSequentialBlocks(function);
            
            iterations++;
        } while (madeChanges && iterations < MAX_ITERATIONS);
    }
    

    private void analyzeControlFlow(LLVMValueRef function) {

        incomingEdges.clear();
        outgoingEdges.clear();
        reachableBlocks.clear();

        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            incomingEdges.put(block, new ArrayList<>());
            outgoingEdges.put(block, new ArrayList<>());
            block = LLVMGetNextBasicBlock(block);
        }
        

        block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator != null) {
                int opcode = LLVMGetInstructionOpcode(terminator);
                if (opcode == LLVMBr) {
  
                    int numOperands = LLVMGetNumOperands(terminator);
                    if (numOperands == 1) {
        
                        LLVMBasicBlockRef target = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 0));
                        if (target != null) {
                            outgoingEdges.get(block).add(target);
                            incomingEdges.get(target).add(block);
                        }
                    } else if (numOperands == 3) {
                        LLVMBasicBlockRef trueTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 2));
                        LLVMBasicBlockRef falseTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 1));
                        
                        if (falseTarget != null) {
                            outgoingEdges.get(block).add(falseTarget);
                            incomingEdges.get(falseTarget).add(block);
                        }
                        
                        if (trueTarget != null) {
                            outgoingEdges.get(block).add(trueTarget);
                            incomingEdges.get(trueTarget).add(block);
                        }
                    }
                }

            }
            block = LLVMGetNextBasicBlock(block);
        }
        
        findReachableBlocks(function);
    }
    

    private void findReachableBlocks(LLVMValueRef function) {
        LLVMBasicBlockRef entryBlock = LLVMGetFirstBasicBlock(function);
        if (entryBlock == null) return;
        

        Deque<LLVMBasicBlockRef> stack = new ArrayDeque<>();
        stack.push(entryBlock);
        
        while (!stack.isEmpty()) {
            LLVMBasicBlockRef current = stack.pop();
            

            if (reachableBlocks.contains(current)) continue;
            

            reachableBlocks.add(current);
            

            for (LLVMBasicBlockRef successor : outgoingEdges.get(current)) {
                if (!reachableBlocks.contains(successor)) {
                    stack.push(successor);
                }
            }
        }
    }
    

    private boolean optimizeConstantConditions(LLVMValueRef function) {
        boolean changed = false;
        
        // 遍历所有可达基本块
        for (LLVMBasicBlockRef block : new ArrayList<>(incomingEdges.keySet())) {
            // 跳过不可达块
            if (!reachableBlocks.contains(block)) continue;
            
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator == null) continue;
            
            int opcode = LLVMGetInstructionOpcode(terminator);
            if (opcode == LLVMBr && LLVMGetNumOperands(terminator) == 3) {
                // 处理条件分支
                LLVMValueRef condition = LLVMGetOperand(terminator, 0);
                if (LLVMIsConstant(condition) != 0) {
                    // 常量条件，确定跳转方向
                    boolean condValue = LLVMConstIntGetSExtValue(condition) != 0;
                    
                    // 获取跳转目标
                    LLVMBasicBlockRef falseTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 1));
                    LLVMBasicBlockRef trueTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 2));
                    
                    // 根据常量条件选择保留的分支
                    LLVMBasicBlockRef targetToKeep = condValue ? trueTarget : falseTarget;
                    LLVMBasicBlockRef targetToRemove = condValue ? falseTarget : trueTarget;
                    
                    // 创建无条件跳转
                    LLVMBuilderRef builder = LLVMCreateBuilder();
                    try {
                        LLVMPositionBuilderBefore(builder, terminator);
                        LLVMBuildBr(builder, targetToKeep);
                        
                        // 删除原有的条件跳转
                        LLVMInstructionEraseFromParent(terminator);
                        
                        // 如果要移除的目标块只有当前块是其前驱，且不是保留的目标块
                        // 则可以安全地删除该块
                        if (incomingEdges.containsKey(targetToRemove) && 
                            incomingEdges.get(targetToRemove).size() == 1 && 
                            incomingEdges.get(targetToRemove).contains(block) &&
                            !targetToRemove.equals(targetToKeep)) {
                            
                            // 清空要删除的块中的所有指令
                            while (LLVMGetFirstInstruction(targetToRemove) != null) {
                                LLVMInstructionEraseFromParent(LLVMGetFirstInstruction(targetToRemove));
                            }
                            
                            // 从函数中删除该块
                            LLVMRemoveBasicBlockFromParent(targetToRemove);
                        }
                        
                        changed = true;
                    } finally {
                        LLVMDisposeBuilder(builder);
                    }
                    
                    // 当前变化之后，立即跳出循环，重新分析控制流
                    if (changed) break;
                }
            }
        }
        
        return changed;
    }
    

    private boolean removeDeadBlocks(LLVMValueRef function) {
        boolean changed = false;
        

        LLVMBasicBlockRef firstBlock = LLVMGetFirstBasicBlock(function);
        if (firstBlock == null) return false;
        

        List<LLVMBasicBlockRef> blocksToRemove = new ArrayList<>();
        

        LLVMBasicBlockRef block = LLVMGetNextBasicBlock(firstBlock);
        while (block != null) {
            if (!reachableBlocks.contains(block)) {
                blocksToRemove.add(block);
            }
            block = LLVMGetNextBasicBlock(block);
        }
        

        for (LLVMBasicBlockRef deadBlock : blocksToRemove) {

            while (LLVMGetFirstInstruction(deadBlock) != null) {
                LLVMInstructionEraseFromParent(LLVMGetFirstInstruction(deadBlock));
            }

            LLVMRemoveBasicBlockFromParent(deadBlock);
            changed = true;
        }
        
        return changed;
    }
    

    private boolean mergeSequentialBlocks(LLVMValueRef function) {
        boolean changed = false;
        
        // 遍历所有可达基本块
        for (LLVMBasicBlockRef block : new ArrayList<>(incomingEdges.keySet())) {
            // 跳过不可达块
            if (!reachableBlocks.contains(block)) continue;
            
            // 获取块的终结指令
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator == null) continue;
            
            // 检查是否为无条件跳转
            if (LLVMGetInstructionOpcode(terminator) == LLVMBr && 
                LLVMGetNumOperands(terminator) == 1) {
                
                // 获取跳转目标
                LLVMBasicBlockRef targetBlock = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 0));
                if (targetBlock == null || targetBlock.equals(block)) continue;
                
                // 检查目标块是否只有当前块作为前驱
                if (incomingEdges.containsKey(targetBlock) && 
                    incomingEdges.get(targetBlock).size() == 1 && 
                    incomingEdges.get(targetBlock).contains(block)) {
                    
                    // 收集目标块中的所有指令
                    List<LLVMValueRef> instructionsToMove = new ArrayList<>();
                    for (LLVMValueRef inst = LLVMGetFirstInstruction(targetBlock); 
                         inst != null; 
                         inst = LLVMGetNextInstruction(inst)) {
                        instructionsToMove.add(inst);
                    }
                    
                    // 删除当前块的无条件跳转
                    LLVMInstructionEraseFromParent(terminator);
                    
                    // 将目标块的指令移动到当前块
                    LLVMBuilderRef builder = LLVMCreateBuilder();
                    try {
                        LLVMPositionBuilderAtEnd(builder, block);
                        
                        // 逐个移动指令
                        for (LLVMValueRef inst : instructionsToMove) {
                            // 从原块中移除指令
                            LLVMInstructionRemoveFromParent(inst);
                            // 插入到当前块的末尾
                            LLVMInsertIntoBuilderWithName(builder, inst, LLVMGetValueName(inst));
                        }
                    } finally {
                        LLVMDisposeBuilder(builder);
                    }
                    
                    // 删除目标块（现已为空）
                    LLVMRemoveBasicBlockFromParent(targetBlock);
                    
                    changed = true;
                    break;  // 重新分析控制流后继续
                }
            }
        }
        
        return changed;
    }
} 