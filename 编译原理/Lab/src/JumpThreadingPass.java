import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.*;

public class JumpThreadingPass implements OptimizationPass {
    
    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            if (LLVMCountBasicBlocks(function) > 0) {
                try {
                    optimizeFunction(function);
                } catch (Exception e) {
                    System.err.println("Error in JumpThreadingPass: " + e.getMessage());
                }
            }
            function = LLVMGetNextFunction(function);
        }
        return module;
    }
    
    @Override
    public String getName() {
        return "JumpThreading";
    }
    
    private void optimizeFunction(LLVMValueRef function) {
        boolean changed;
        do {
            changed = false;
            
            // 构建控制流图
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> successors = buildCFG(function);
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> predecessors = buildPredecessors(successors);
            
            // 找到所有包含无条件跳转的基本块
            List<LLVMBasicBlockRef> jumpBlocks = findSingleJumpBlocks(function);
            if (jumpBlocks.isEmpty()) {
                break;  // 没有可优化的块，退出循环
            }
            
            // 处理跳转链压缩 - 找到最终目标
            Map<LLVMBasicBlockRef, LLVMBasicBlockRef> jumpTargets = new HashMap<>();
            for (LLVMBasicBlockRef block : jumpBlocks) {
                if (block == null || isBlockInvalid(block)) continue;
                
                LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
                if (terminator == null) continue;
                
                // 安全检查：确保跳转指令有效
                if (LLVMGetNumOperands(terminator) < 1) continue;
                
                LLVMValueRef targetValue = LLVMGetOperand(terminator, 0);
                if (targetValue == null) continue;
                
                try {
                    LLVMBasicBlockRef immediateTarget = LLVMValueAsBasicBlock(targetValue);
                    if (immediateTarget == null || isBlockInvalid(immediateTarget)) continue;
                    
                    // 追踪跳转链到最终目标，但避免无限循环
                    LLVMBasicBlockRef finalTarget = immediateTarget;
                    Set<LLVMBasicBlockRef> visited = new HashSet<>();
                    visited.add(block);
                    
                    int maxDepth = 10;  // 限制追踪深度以避免潜在问题
                    int depth = 0;
                    
                    while (jumpBlocks.contains(finalTarget) && !visited.contains(finalTarget) && depth < maxDepth) {
                        visited.add(finalTarget);
                        depth++;
                        
                        LLVMValueRef nextTerminator = LLVMGetBasicBlockTerminator(finalTarget);
                        if (nextTerminator == null || 
                            LLVMGetInstructionOpcode(nextTerminator) != LLVMBr ||
                            LLVMGetNumOperands(nextTerminator) != 1) {
                            break;
                        }
                        
                        LLVMValueRef nextTargetValue = LLVMGetOperand(nextTerminator, 0);
                        if (nextTargetValue == null) break;
                        
                        LLVMBasicBlockRef nextTarget = LLVMValueAsBasicBlock(nextTargetValue);
                        if (nextTarget == null || isBlockInvalid(nextTarget)) break;
                        
                        finalTarget = nextTarget;
                    }
                    
                    if (!finalTarget.equals(immediateTarget)) {
                        jumpTargets.put(block, finalTarget);
                    }
                } catch (Exception e) {
                    // 忽略处理此块时的错误，继续处理下一个块
                    System.err.println("Error processing jump target: " + e.getMessage());
                }
            }
            
            // 重定向所有跳转到最终目标
            for (Map.Entry<LLVMBasicBlockRef, LLVMBasicBlockRef> entry : jumpTargets.entrySet()) {
                LLVMBasicBlockRef block = entry.getKey();
                LLVMBasicBlockRef target = entry.getValue();
                
                if (block == null || target == null || isBlockInvalid(block) || isBlockInvalid(target)) {
                    continue;
                }
                
                // 直接更新块的终结指令
                try {
                    LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
                    if (terminator == null) continue;
                    
                    if (LLVMGetInstructionOpcode(terminator) == LLVMBr && 
                        LLVMGetNumOperands(terminator) == 1) {
                        LLVMValueRef targetValue = LLVMBasicBlockAsValue(target);
                        if (targetValue != null) {
                            LLVMSetOperand(terminator, 0, targetValue);
                            changed = true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error updating terminator: " + e.getMessage());
                }
            }
            
            // 重定向所有指向跳转块的前驱
            List<LLVMBasicBlockRef> simpleJumpBlocks = new ArrayList<>();
            for (LLVMBasicBlockRef block : jumpBlocks) {
                if (block == null || isBlockInvalid(block)) continue;
                
                // 检查此块是否为仅包含单个跳转指令的块
                if (isSimpleJumpBlock(block)) {
                    simpleJumpBlocks.add(block);
                }
            }
            
            for (LLVMBasicBlockRef block : simpleJumpBlocks) {
                try {
                    LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
                    if (terminator == null) continue;
                    
                    LLVMValueRef targetValue = LLVMGetOperand(terminator, 0);
                    if (targetValue == null) continue;
                    
                    LLVMBasicBlockRef target = LLVMValueAsBasicBlock(targetValue);
                    if (target == null || isBlockInvalid(target)) continue;
                    
                    if (canRedirectPredecessors(block, target, predecessors)) {
                        redirectPredecessors(block, target, predecessors);
                        changed = true;
                    }
                } catch (Exception e) {
                    System.err.println("Error redirecting predecessors: " + e.getMessage());
                }
            }
            
        } while (changed);
        
        // 清理空块和只包含单个跳转指令的块
        try {
            cleanupEmptyBlocks(function);
        } catch (Exception e) {
            System.err.println("Error cleaning up blocks: " + e.getMessage());
        }
    }
    
    // 检查基本块是否已无效（被删除或损坏）
    private boolean isBlockInvalid(LLVMBasicBlockRef block) {
        if (block == null) return true;
        try {
            // 尝试获取第一条指令，如果块无效会抛出异常
            LLVMGetFirstInstruction(block);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
    
    private boolean isSimpleJumpBlock(LLVMBasicBlockRef block) {
        if (block == null) return false;
        
        try {
            LLVMValueRef firstInst = LLVMGetFirstInstruction(block);
            if (firstInst == null) return false;
            
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator == null) return false;
            
            // 检查是否只有终结指令
            return firstInst.equals(terminator) && 
                   LLVMGetInstructionOpcode(terminator) == LLVMBr && 
                   LLVMGetNumOperands(terminator) == 1;
        } catch (Exception e) {
            return false;
        }
    }
    
    private List<LLVMBasicBlockRef> findSingleJumpBlocks(LLVMValueRef function) {
        List<LLVMBasicBlockRef> result = new ArrayList<>();
        
        for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
             block != null; 
             block = LLVMGetNextBasicBlock(block)) {
            
            // 获取块的终结指令
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator == null) continue;
            
            // 检查是否是无条件跳转
            if (LLVMGetInstructionOpcode(terminator) == LLVMBr && 
                LLVMGetNumOperands(terminator) == 1) {
                
                // 添加所有包含无条件跳转的块
                result.add(block);
            }
        }
        
        return result;
    }
    
    private Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> buildCFG(LLVMValueRef function) {
        Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> successors = new HashMap<>();
        
        for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
             block != null; 
             block = LLVMGetNextBasicBlock(block)) {
            
            successors.put(block, new ArrayList<>());
            
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(block);
            if (terminator == null) continue;
            
            int opcode = LLVMGetInstructionOpcode(terminator);
            if (opcode == LLVMBr) {
                int numOperands = LLVMGetNumOperands(terminator);
                
                if (numOperands == 1) {
                    // 无条件跳转
                    LLVMBasicBlockRef target = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 0));
                    successors.get(block).add(target);
                } else if (numOperands == 3) {
                    // 条件跳转
                    LLVMBasicBlockRef trueTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 2));
                    LLVMBasicBlockRef falseTarget = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 1));
                    
                    successors.get(block).add(falseTarget);
                    successors.get(block).add(trueTarget);
                }
            }
        }
        
        return successors;
    }
    
    private Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> buildPredecessors(
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> successors) {
        
        Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> predecessors = new HashMap<>();
        
        // 初始化空列表
        for (LLVMBasicBlockRef block : successors.keySet()) {
            predecessors.put(block, new ArrayList<>());
        }
        
        // 填充前驱信息
        for (Map.Entry<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> entry : successors.entrySet()) {
            LLVMBasicBlockRef block = entry.getKey();
            List<LLVMBasicBlockRef> blockSuccessors = entry.getValue();
            
            for (LLVMBasicBlockRef succ : blockSuccessors) {
                predecessors.get(succ).add(block);
            }
        }
        
        return predecessors;
    }
    
    private boolean canRedirectPredecessors(
            LLVMBasicBlockRef block, 
            LLVMBasicBlockRef target,
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> predecessors) {
        
        // 如果块没有前驱或目标是自身，则不能重定向
        if (!predecessors.containsKey(block) || 
            predecessors.get(block).isEmpty() || 
            block.equals(target)) {
            return false;
        }
        
        return true;
    }
    
    private void redirectPredecessors(
            LLVMBasicBlockRef block, 
            LLVMBasicBlockRef target,
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> predecessors) {
        
        if (block == null || target == null || !predecessors.containsKey(block)) {
            return;
        }
        
        // 获取所有前驱
        List<LLVMBasicBlockRef> preds = new ArrayList<>(predecessors.get(block));
        
        for (LLVMBasicBlockRef pred : preds) {
            if (pred == null || isBlockInvalid(pred)) continue;
            
            try {
                LLVMValueRef terminator = LLVMGetBasicBlockTerminator(pred);
                if (terminator == null) continue;
                
                int opcode = LLVMGetInstructionOpcode(terminator);
                if (opcode == LLVMBr) {
                    int numOperands = LLVMGetNumOperands(terminator);
                    
                    if (numOperands == 1) {
                        // 无条件跳转，直接替换目标
                        LLVMValueRef targetValue = LLVMBasicBlockAsValue(target);
                        if (targetValue != null) {
                            LLVMSetOperand(terminator, 0, targetValue);
                        }
                    } else if (numOperands == 3) {
                        // 条件跳转，需要检查哪个分支指向了block
                        try {
                            LLVMValueRef trueOperand = LLVMGetOperand(terminator, 2);
                            LLVMValueRef falseOperand = LLVMGetOperand(terminator, 1);
                            
                            if (trueOperand == null || falseOperand == null) continue;
                            
                            LLVMBasicBlockRef trueTarget = LLVMValueAsBasicBlock(trueOperand);
                            LLVMBasicBlockRef falseTarget = LLVMValueAsBasicBlock(falseOperand);
                            
                            if (trueTarget == null || falseTarget == null) continue;
                            
                            LLVMValueRef targetValue = LLVMBasicBlockAsValue(target);
                            if (targetValue == null) continue;
                            
                            if (trueTarget.equals(block)) {
                                LLVMSetOperand(terminator, 2, targetValue);
                            }
                            if (falseTarget.equals(block)) {
                                LLVMSetOperand(terminator, 1, targetValue);
                            }
                        } catch (Exception e) {
                            System.err.println("Error handling conditional branch: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error redirecting predecessor: " + e.getMessage());
            }
        }
    }
    
    private void cleanupEmptyBlocks(LLVMValueRef function) {
        boolean changed;
        do {
            changed = false;
            
            try {
                // 重新构建控制流信息
                Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> successors = buildCFG(function);
                Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> predecessors = buildPredecessors(successors);
                
                // 找到不可达的块
                Set<LLVMBasicBlockRef> reachable = findReachableBlocks(function, successors);
                
                for (LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function); 
                     block != null;) {
                    
                    if (isBlockInvalid(block)) {
                        block = LLVMGetNextBasicBlock(block);
                        continue;
                    }
                    
                    LLVMBasicBlockRef nextBlock = LLVMGetNextBasicBlock(block);
                    
                    // 跳过入口块
                    if (block.equals(LLVMGetFirstBasicBlock(function))) {
                        block = nextBlock;
                        continue;
                    }
                    
                    // 如果块不可达或没有前驱，可以删除
                    if (!reachable.contains(block) || 
                        !predecessors.containsKey(block) || 
                        predecessors.get(block).isEmpty()) {
                        
                        try {
                            // 清空块中的所有指令
                            LLVMValueRef instr = LLVMGetFirstInstruction(block);
                            while (instr != null) {
                                LLVMValueRef nextInstr = LLVMGetNextInstruction(instr);
                                LLVMInstructionEraseFromParent(instr);
                                instr = nextInstr;
                            }
                            
                            // 从函数中删除块
                            LLVMRemoveBasicBlockFromParent(block);
                            changed = true;
                        } catch (Exception e) {
                            System.err.println("Error removing block: " + e.getMessage());
                        }
                    }
                    
                    block = nextBlock;
                }
            } catch (Exception e) {
                System.err.println("Error in cleanup loop: " + e.getMessage());
                break;  // 发生错误时退出循环
            }
            
        } while (changed);
    }
    
    private Set<LLVMBasicBlockRef> findReachableBlocks(
            LLVMValueRef function,
            Map<LLVMBasicBlockRef, List<LLVMBasicBlockRef>> successors) {
        
        Set<LLVMBasicBlockRef> reachable = new HashSet<>();
        Deque<LLVMBasicBlockRef> worklist = new ArrayDeque<>();
        
        // 从入口块开始
        LLVMBasicBlockRef entry = LLVMGetFirstBasicBlock(function);
        if (entry == null) return reachable;
        
        worklist.add(entry);
        reachable.add(entry);
        
        while (!worklist.isEmpty()) {
            LLVMBasicBlockRef block = worklist.poll();
            
            if (block == null || isBlockInvalid(block)) continue;
            
            if (successors.containsKey(block)) {
                for (LLVMBasicBlockRef succ : successors.get(block)) {
                    if (succ != null && !isBlockInvalid(succ) && !reachable.contains(succ)) {
                        reachable.add(succ);
                        worklist.add(succ);
                    }
                }
            }
        }
        
        return reachable;
    }
}