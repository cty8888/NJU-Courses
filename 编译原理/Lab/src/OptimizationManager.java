import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import org.bytedeco.javacpp.BytePointer;

public class OptimizationManager {
    private LLVMModuleRef module;
    
    public OptimizationManager(LLVMModuleRef module) {
        this.module = module;
    }
    
    public LLVMModuleRef runConstantPropagation() {
        module = new ConstantPropagationPass().run(module);
        return module;
    }

    public LLVMModuleRef runUnusedVarElimination() {
        module = new UnusedVariableEliminationPass().run(module);
        return module;
    }
    
    public LLVMModuleRef runDeadCodeElimination() {
        module = new DeadCodeEliminationPass().run(module);
        return module;
    }

    public LLVMModuleRef runJumpThreading() {
        module = new JumpThreadingPass().run(module);
        return module;
    }
    
    public LLVMModuleRef runConditionSimplification() {
    
        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
            while (block != null) {
                simplifyConditionsInBlock(block);
                block = LLVMGetNextBasicBlock(block);
            }
            function = LLVMGetNextFunction(function);
        }
        return module;
    }
    
    private void simplifyConditionsInBlock(LLVMBasicBlockRef block) {

        LLVMValueRef instruction = LLVMGetFirstInstruction(block);
        
        while (instruction != null) {
        
            if (LLVMGetInstructionOpcode(instruction) == LLVMICmp) {
              
                LLVMValueRef nextInst = LLVMGetNextInstruction(instruction);
                if (nextInst != null && LLVMGetInstructionOpcode(nextInst) == LLVMZExt) {
                    
                    LLVMValueRef thirdInst = LLVMGetNextInstruction(nextInst);
                    if (thirdInst != null && LLVMGetInstructionOpcode(thirdInst) == LLVMICmp) {
                       
                        LLVMValueRef fourthInst = LLVMGetNextInstruction(thirdInst);
                        if (fourthInst != null && LLVMGetInstructionOpcode(fourthInst) == LLVMBr) {
                            LLVMValueRef brCond = LLVMGetOperand(fourthInst, 0);
                            if (brCond.equals(thirdInst)) {
                        
                                LLVMSetOperand(fourthInst, 0, instruction);
                                LLVMInstructionEraseFromParent(nextInst);
                                LLVMInstructionEraseFromParent(thirdInst);
                            }
                            
                        }
                    }
                }
            }
            instruction = LLVMGetNextInstruction(instruction);
        }
    }
    
    public LLVMModuleRef runAllPasses() {
 
        module = runConstantPropagation();
        module = runDeadCodeElimination();
        module = runJumpThreading();
        

        module = runUnusedVarElimination();

        module = runConstantPropagation();
        module = runDeadCodeElimination();
        module = runJumpThreading();
        

        module = runUnusedVarElimination();
        
        return module;
    }
    
    public LLVMModuleRef runAllOptimizations() {
        boolean changed;
        do {
            String beforeIR = moduleToString(module);
            
            runAllPasses();
            
            String afterIR = moduleToString(module);
            changed = !beforeIR.equals(afterIR);
            
        } while (changed);
        
        
        runConditionSimplification();
        runJumpThreading();
        
        return module;
    }
    private String moduleToString(LLVMModuleRef mod) {
        BytePointer buffer = LLVMPrintModuleToString(mod);
        String irStr = buffer.getString();
        LLVMDisposeMessage(buffer);
        return irStr;
    }
} 