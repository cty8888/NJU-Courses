import org.bytedeco.llvm.LLVM.LLVMModuleRef;


public interface OptimizationPass {
    LLVMModuleRef run(LLVMModuleRef module);
    String getName();
}