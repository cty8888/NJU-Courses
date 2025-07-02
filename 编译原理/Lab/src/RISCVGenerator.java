import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import java.io.*;
import java.util.*;

public class RISCVGenerator {
    private LLVMModuleRef moduleRef;
    private File outputFile;
    private RISCVAsmBuilder asmBuilder;
    private Map<String, RISCVLocation> GlobalvariableLocations; // 变量位置表
    private int currentStackOffset; // 当前栈偏移
    private List<RISCVInterval> intervals; // 变量生命周期区间列表
    private List<String> availableRegs;
    private Set<String> usedRegs;
    private Map<String, RISCVLocationInfo> locationInfoMap;
    private int currentInstructionCount;
    public RISCVGenerator(LLVMModuleRef module, File outputFile) {
        this.moduleRef = module;
        this.outputFile = outputFile;
        this.asmBuilder = new RISCVAsmBuilder();
        this.GlobalvariableLocations = new HashMap<>();
        this.currentStackOffset = 0;
        this.intervals = new ArrayList<>();
        this.availableRegs = new ArrayList<>(Arrays.asList(
            "s0","s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
            "a0","a1","a2","a3","a4","a5","a6","a7",
            "t2","t3", "t4", "t5", "t6"
        ));
        this.usedRegs = new HashSet<>();
        this.locationInfoMap = new HashMap<>();
        this.currentInstructionCount = 0;
    }
    
    public void generate() throws FileNotFoundException {
        processGlobals();        
        processMainFunction();
        asmBuilder.dumpToFile(outputFile);
    }
    
    private void processGlobals() {
        for (LLVMValueRef global = LLVMGetFirstGlobal(moduleRef); global != null; global = LLVMGetNextGlobal(global)) {
            
            String name = LLVMGetValueName(global).getString();
            LLVMValueRef initializer = LLVMGetInitializer(global);
            int initialValue = 0;
            if (initializer != null && LLVMIsAConstantInt(initializer) != null) {
                initialValue = (int) LLVMConstIntGetSExtValue(initializer);
            }
            asmBuilder.addGlobal(name, initialValue);
            GlobalvariableLocations.put(name, new RISCVGlobalLocation(name));
        }
    }
    
    private void processMainFunction() {
        LLVMValueRef mainFunction = findMainFunction();
        analyzeVariableLifetimes(mainFunction);
        //System.out.println(intervals.size());
        /*for(RISCVInterval interval : intervals){
            System.out.println(interval.name + " " + interval.start + " " + interval.end);
        }*/
        allocateRegisters();
        int stackSize = calculateStackSize();
        assignStackLocations(stackSize);

        /*System.out.println("局部变量数量:"+locationInfoMap.size());
        
        // 按照变量生命周期的开始顺序打印信息
        for (RISCVInterval interval : intervals) {
            String varName = interval.name;
            RISCVLocationInfo info = locationInfoMap.get(varName);
            
            System.out.print(info.Varname + " ");
            if(info.regLocation != null){
                System.out.print(info.regLocation.getCode() + " ");
            }else{
                System.out.print("没有进行寄存器分配过");
            }
            if(info.isSpilled){
                System.out.print("溢出过"+info.spillPoint+" "+info.stackLocation.getCode());
            
            }else{
                System.out.print("没有溢出过");
            }
            if(info.locationChanged){
                System.out.print("位置改变过");
            }else{
                System.out.print("位置没有改变过");
            }
            System.out.println();
        }*/
        
        // 识别循环关键变量
        Set<String> loopVars = new HashSet<>();
        for (RISCVLocationInfo info : locationInfoMap.values()) {
            if (info.Varname.equals("i") || info.Varname.contains("lt_") || 
                info.Varname.contains("cond") || info.Varname.contains("while")) {
                loopVars.add(info.Varname);
            }
        }
        
        asmBuilder.startFunction("main");
        asmBuilder.addPrologue(stackSize);
        
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(mainFunction); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
            String bbLabel = LLVMGetBasicBlockName(bb).getString();
            asmBuilder.addBlockEntry(bbLabel);
            
            // 在每个基本块开始处恢复关键循环变量
            if (bbLabel.contains("while_cond") || bbLabel.contains("for_cond")) {
                for (String varName : loopVars) {
                    RISCVLocationInfo info = locationInfoMap.get(varName);
                    if (info != null && info.isSpilled && info.stackLocation != null && info.regLocation != null) {
                        // 在循环条件块开始处恢复循环变量的值
                        asmBuilder.addComment("恢复循环变量 " + varName);
                        asmBuilder.addLoad(info.regLocation.getCode(), info.stackLocation.getCode());
                    }
                }
            }
            
            for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                currentInstructionCount++;
                handleSpill();
                processInstruction(inst);
            }
        }

        // 默认的程序结束处理（仅当没有ret指令时才会执行）
        asmBuilder.addLoadImmediate("a0", 0);  // 默认返回值为0
        asmBuilder.addEpilogue(stackSize);
        asmBuilder.addExitCall();
    }
    
    private void processInstruction(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst); 
        switch (opcode) {
            case LLVMRet:
                processRetInstruction(inst);
                break;
            case LLVMAlloca:
                processAllocaInstruction(inst);
                break;
            case LLVMLoad:
                processLoadInstruction(inst);
                break;
            case LLVMStore:
                processStoreInstruction(inst);
                break;
            case LLVMAdd:
                processBinaryInstruction(inst, "add");
                break;
            case LLVMSub:
                processBinaryInstruction(inst, "sub");
                break;
            case LLVMMul:
                processBinaryInstruction(inst, "mul");
                break;
            case LLVMSDiv:
            case LLVMUDiv:
                processBinaryInstruction(inst, "div");
                break;
            case LLVMSRem:
            case LLVMURem:
                processBinaryInstruction(inst, "rem");
                break;
            case LLVMZExt:
                processZExtInstruction(inst);
                break;
            case LLVMICmp:
                processICmpInstruction(inst);
                break;
            case LLVMBr:
                processBrInstruction(inst);
                break;
            default:
                break;
        }
    }
    
    private void processZExtInstruction(LLVMValueRef inst) {
        LLVMValueRef sourceValue = LLVMGetOperand(inst, 0);
        String sourceName = LLVMGetValueName(sourceValue).getString();
        String destName = LLVMGetValueName(inst).getString();
        
        RISCVLocationInfo destLocationInfo = locationInfoMap.get(destName);
    
        RISCVLocationInfo sourceLocationInfo = locationInfoMap.get(sourceName);
        if (sourceLocationInfo.isSpilled && sourceLocationInfo.spillPoint <= currentInstructionCount) {
            asmBuilder.addLoad("t0", sourceLocationInfo.stackLocation.getCode());
        } else {
            asmBuilder.addMove("t0", sourceLocationInfo.regLocation.getCode());
        }
        // 确保只有最低位有效（执行零扩展）
        //asmBuilder.addBinaryOp("andi", "t0", "t0", "1");

        if (destLocationInfo.isSpilled && destLocationInfo.spillPoint <= currentInstructionCount) {
            asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
        } else {
            asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
        }
    }

    private void processICmpInstruction(LLVMValueRef inst) {
        int predicate = LLVMGetICmpPredicate(inst);
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);
        String destName = LLVMGetValueName(inst).getString();
        RISCVLocationInfo destLocationInfo = locationInfoMap.get(destName);
        if(LLVMIsAConstantInt(lhs) != null){
            int lhsValue = (int) LLVMConstIntGetSExtValue(lhs);
            asmBuilder.addLoadImmediate("t0", lhsValue);
        }else if(LLVMIsAGlobalVariable(lhs) != null){
            //应该不会出现这种情况
        }else{
            String lhsName = LLVMGetValueName(lhs).getString();
            RISCVLocationInfo lhsLocationInfo = locationInfoMap.get(lhsName);
            if(lhsLocationInfo.isSpilled&&lhsLocationInfo.spillPoint<=currentInstructionCount){
                asmBuilder.addLoad("t0", lhsLocationInfo.stackLocation.getCode());
            }else{
                asmBuilder.addMove("t0", lhsLocationInfo.regLocation.getCode());
            }
        }
        if(LLVMIsAConstantInt(rhs) != null){
            int rhsValue = (int) LLVMConstIntGetSExtValue(rhs);
            asmBuilder.addLoadImmediate("t1", rhsValue);
        }else if(LLVMIsAGlobalVariable(rhs) != null){
            //应该不会出现这种情况
        }else{
            String rhsName = LLVMGetValueName(rhs).getString();
            RISCVLocationInfo rhsLocationInfo = locationInfoMap.get(rhsName);
            if(rhsLocationInfo.isSpilled&&rhsLocationInfo.spillPoint<=currentInstructionCount){
                asmBuilder.addLoad("t1", rhsLocationInfo.stackLocation.getCode());
            }else{
                asmBuilder.addMove("t1", rhsLocationInfo.regLocation.getCode());
            }
        }
        switch(predicate){
            case LLVMIntEQ:
                asmBuilder.addBinaryOp("xor", "t0", "t0", "t1");
                asmBuilder.addOneOp("seqz", "t0", "t0");  
                break;
            case LLVMIntNE:
                asmBuilder.addBinaryOp("xor", "t0", "t0", "t1");
                asmBuilder.addOneOp("snez", "t0", "t0");  
                break;
            case LLVMIntSLT:
                asmBuilder.addBinaryOp("slt", "t0", "t0", "t1");
                break;
            case LLVMIntSLE:
                asmBuilder.addBinaryOp("slt", "t0", "t1", "t0");  
                asmBuilder.addBinaryOp("xori", "t0", "t0", "1");  
                break;
            case LLVMIntSGT:
                asmBuilder.addBinaryOp("slt", "t0", "t1", "t0");  
                break;
            case LLVMIntSGE:
                asmBuilder.addBinaryOp("slt", "t0", "t0", "t1");
                asmBuilder.addBinaryOp("xori", "t0", "t0", "1"); 
                break;
        }
        if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
            asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
        }else{
            asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
        }
    }

    private void processBrInstruction(LLVMValueRef inst) {
        int numOperands = LLVMGetNumOperands(inst);
        if (numOperands == 1) {
            LLVMBasicBlockRef targetBB = LLVMValueAsBasicBlock(LLVMGetOperand(inst, 0));
            String targetLabel = LLVMGetBasicBlockName(targetBB).getString();
            asmBuilder.addJump(targetLabel);
        } 
        else if (numOperands == 3) {
            LLVMValueRef condition = LLVMGetOperand(inst, 0);
            LLVMBasicBlockRef trueBB = LLVMValueAsBasicBlock(LLVMGetOperand(inst, 2));
            LLVMBasicBlockRef falseBB = LLVMValueAsBasicBlock(LLVMGetOperand(inst, 1));
            System.out.println(LLVMGetBasicBlockName(trueBB).getString());
            System.out.println(LLVMGetBasicBlockName(falseBB).getString());
            String trueLabel = LLVMGetBasicBlockName(trueBB).getString();
            String falseLabel = LLVMGetBasicBlockName(falseBB).getString();
            
            // 加载条件值到t0
            if (LLVMIsAConstantInt(condition) != null) {
                int condValue = (int) LLVMConstIntGetSExtValue(condition);
                asmBuilder.addLoadImmediate("t0", condValue);
            } else {
                String condName = LLVMGetValueName(condition).getString();          
                RISCVLocationInfo condLocationInfo = locationInfoMap.get(condName);
                
                if (condLocationInfo.isSpilled && condLocationInfo.spillPoint <= currentInstructionCount) {
                    asmBuilder.addLoad("t0", condLocationInfo.stackLocation.getCode());
                } else {
                    asmBuilder.addMove("t0", condLocationInfo.regLocation.getCode());
                }
            }
            
            asmBuilder.addBranchZero("bne", "t0", trueLabel);
            asmBuilder.addJump(falseLabel);
        }
    }
    
    private void processRetInstruction(LLVMValueRef inst) {
        if (LLVMGetNumOperands(inst) > 0) {
            LLVMValueRef retValue = LLVMGetOperand(inst, 0);
            
            if (LLVMIsAConstantInt(retValue) != null) {
                int value = (int) LLVMConstIntGetSExtValue(retValue);
                asmBuilder.addLoadImmediate("a0", value);
            } else {
                if (LLVMIsAGlobalVariable(retValue) != null) {
                    String valueName = LLVMGetValueName(retValue).getString();
                    asmBuilder.addLoadAddress("t0", valueName);
                    asmBuilder.addLoad("a0", "0(t0)");
                } else {
                    String valueName = LLVMGetValueName(retValue).getString();
                    RISCVLocationInfo valueLocationInfo = locationInfoMap.get(valueName);
                    if (valueLocationInfo != null) {
                        if (valueLocationInfo.isSpilled && valueLocationInfo.stackLocation != null) {
                            asmBuilder.addLoad("a0", valueLocationInfo.stackLocation.getCode());
                        } else if (!valueLocationInfo.isSpilled && valueLocationInfo.regLocation != null) {
                            asmBuilder.addMove("a0", valueLocationInfo.regLocation.getCode());
                        }
                    }
                }
            }
        }
        
        asmBuilder.addExitCall();
    }
    
    private void processAllocaInstruction(LLVMValueRef inst) {

    }
    
    private void processLoadInstruction(LLVMValueRef inst) {
        String destName = LLVMGetValueName(inst).getString();
        LLVMValueRef source = LLVMGetOperand(inst, 0);
        String sourceName = LLVMGetValueName(source).getString();
        
        boolean isGlobalVarPtr = LLVMIsAGlobalVariable(source) != null;
        RISCVLocationInfo destLocationInfo = locationInfoMap.get(destName);

        if(isGlobalVarPtr){
            asmBuilder.addLoadAddress("t0", sourceName);
            asmBuilder.addLoad("t0","0(t0)");
            if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
            }else{
                asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
            }
        }else{
            RISCVLocationInfo sourceLocationInfo = locationInfoMap.get(sourceName);
            if(sourceLocationInfo.isSpilled&&sourceLocationInfo.spillPoint<=currentInstructionCount){
                if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addLoad("t0", sourceLocationInfo.stackLocation.getCode());
                    asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addLoad("t0", sourceLocationInfo.stackLocation.getCode());
                    asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
                }
            }else{
                if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addStore(sourceLocationInfo.regLocation.getCode(), destLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove(destLocationInfo.regLocation.getCode(), sourceLocationInfo.regLocation.getCode());
                }
            }
        }
    }
    
    private void processStoreInstruction(LLVMValueRef inst) {
        LLVMValueRef value = LLVMGetOperand(inst, 0);
        LLVMValueRef pointer = LLVMGetOperand(inst, 1);
        String pointerName = LLVMGetValueName(pointer).getString();
        
        
        boolean isGlobalVarPtr = LLVMIsAGlobalVariable(pointer) != null;
        if (isGlobalVarPtr) {
            if (LLVMIsAConstantInt(value) != null) { 
                //常量 --> 全局
                int constValue = (int) LLVMConstIntGetSExtValue(value);
                asmBuilder.addLoadImmediate("t0", constValue);
                asmBuilder.addLoadAddress("t1", pointerName);
                asmBuilder.addStore("t0", "0(t1)");
            } else if(LLVMIsAGlobalVariable(value) != null){
                //全局变量 --> 全局
                String valueName = LLVMGetValueName(value).getString();
                asmBuilder.addLoadAddress("t0", valueName);
                asmBuilder.addLoad("t0", "0(t0)");
                asmBuilder.addLoadAddress("t1", pointerName);
                asmBuilder.addStore("t0", "0(t1)");
            }else{
                //局部变量 --> 全局
                String valueName = LLVMGetValueName(value).getString();
                RISCVLocationInfo valueLocationInfo = locationInfoMap.get(valueName);
                if(valueLocationInfo.isSpilled&&valueLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addLoad("t0", valueLocationInfo.stackLocation.getCode());
                    asmBuilder.addLoadAddress("t1", pointerName);
                    asmBuilder.addStore("t0", "0(t1)");
                }else{
                    asmBuilder.addLoadAddress("t0", pointerName);
                    asmBuilder.addStore(valueLocationInfo.regLocation.getCode(), "0(t0)");
                }
            }
        }else{
            RISCVLocationInfo destLocationInfo = locationInfoMap.get(pointerName);
            if (LLVMIsAConstantInt(value) != null) {
                //常量 --> 局部
                int constValue = (int) LLVMConstIntGetSExtValue(value);
                asmBuilder.addLoadImmediate("t0", constValue);
                if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
                }
            } else if(LLVMIsAGlobalVariable(value) != null){
                //全局变量 --> 局部
                String valueName = LLVMGetValueName(value).getString();
                asmBuilder.addLoadAddress("t0", valueName);
                asmBuilder.addLoad("t0", "0(t0)");
                if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
                }
            }else{
                //局部变量 --> 局部
                String valueName = LLVMGetValueName(value).getString();
                RISCVLocationInfo valueLocationInfo = locationInfoMap.get(valueName);
                if(valueLocationInfo.isSpilled&&valueLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addLoad("t0", valueLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove("t0", valueLocationInfo.regLocation.getCode());  
                }
                if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
                }
            }
        }
    }
    
    private void processBinaryInstruction(LLVMValueRef inst, String operation) {
        String DestName = LLVMGetValueName(inst).getString();
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);

        if(LLVMIsAConstantInt(lhs) != null){
            int lhsValue = (int) LLVMConstIntGetSExtValue(lhs);
            asmBuilder.addLoadImmediate("t0", lhsValue);
        }else{
            String lhsName = LLVMGetValueName(lhs).getString();
            if(LLVMIsAGlobalVariable(lhs) != null){
                asmBuilder.addLoadAddress("t0", lhsName);
                asmBuilder.addLoad("t0", "0(t0)");
            }else{
                RISCVLocationInfo lhsLocationInfo = locationInfoMap.get(lhsName);
                if(lhsLocationInfo.isSpilled&&lhsLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addLoad("t0", lhsLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove("t0", lhsLocationInfo.regLocation.getCode());
                }
            }
        }
        if(LLVMIsAConstantInt(rhs) != null){
            int rhsValue = (int) LLVMConstIntGetSExtValue(rhs);
            asmBuilder.addLoadImmediate("t1", rhsValue);
        }else{
            String rhsName = LLVMGetValueName(rhs).getString();
            if(LLVMIsAGlobalVariable(rhs) != null){
                asmBuilder.addLoadAddress("t1", rhsName);
                asmBuilder.addLoad("t1", "0(t1)");
            }else{
                RISCVLocationInfo rhsLocationInfo = locationInfoMap.get(rhsName);
                if(rhsLocationInfo.isSpilled&&rhsLocationInfo.spillPoint<=currentInstructionCount){
                    asmBuilder.addLoad("t1", rhsLocationInfo.stackLocation.getCode());
                }else{
                    asmBuilder.addMove("t1", rhsLocationInfo.regLocation.getCode());
                }
            }
        }
        asmBuilder.addBinaryOp(operation, "t0", "t0", "t1");
        RISCVLocationInfo destLocationInfo = locationInfoMap.get(DestName);
        if(destLocationInfo.isSpilled&&destLocationInfo.spillPoint<=currentInstructionCount){
            asmBuilder.addStore("t0", destLocationInfo.stackLocation.getCode());
        }else{
            asmBuilder.addMove(destLocationInfo.regLocation.getCode(), "t0");
        }
        return;
    }

    private LLVMValueRef findMainFunction() {
        for (LLVMValueRef func = LLVMGetFirstFunction(moduleRef); func != null; func = LLVMGetNextFunction(func)) {
            String name = LLVMGetValueName(func).getString();
            if (name.equals("main")) {
                return func;
            }
        }
        return null;
    }

    private void analyzeVariableLifetimes(LLVMValueRef mainFunction) {
        Map<String, Integer> startPoints = new HashMap<>();
        Map<String, Integer> endPoints = new HashMap<>();
        int instPosition = 1;
        
        // 第一遍：记录所有指令位置
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(mainFunction); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
            for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                String definedVar = LLVMGetValueName(inst).getString();
                if (!definedVar.isEmpty()) {
                    startPoints.put(definedVar, instPosition);
                    endPoints.put(definedVar, instPosition);
                }
                int numOperands = LLVMGetNumOperands(inst);
                for (int i = 0; i < numOperands; i++) {
                    LLVMValueRef operand = LLVMGetOperand(inst, i);
                    if (LLVMIsAConstant(operand) == null&&LLVMIsAGlobalVariable(operand) == null) {
                        String operandName = LLVMGetValueName(operand).getString();
                        endPoints.put(operandName, instPosition);
                    }
                }
                instPosition++;
            }
        }
        
        // 第二遍：特别处理循环变量
        instPosition = 1;
        Set<String> loopVariables = new HashSet<>(); // 记录所有循环变量
        
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(mainFunction); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
            String bbName = LLVMGetBasicBlockName(bb).getString();
            // 检测是否是循环条件块
            if (bbName.contains("while_cond") || bbName.contains("for_cond")) {
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                    // 找出条件分支指令
                    if (LLVMGetInstructionOpcode(inst) == LLVMBr && LLVMGetNumOperands(inst) > 1) {
                        // 扩展所有在条件块中使用的变量的生命周期
                        for (LLVMValueRef innerInst = LLVMGetFirstInstruction(bb); innerInst != null; innerInst = LLVMGetNextInstruction(innerInst)) {
                            int numOps = LLVMGetNumOperands(innerInst);
                            for (int i = 0; i < numOps; i++) {
                                LLVMValueRef op = LLVMGetOperand(innerInst, i);
                                if (LLVMIsAConstant(op) == null && LLVMIsAGlobalVariable(op) == null) {
                                    String opName = LLVMGetValueName(op).getString();
                                    if (!opName.isEmpty() && startPoints.containsKey(opName)) {
                                        // 将循环变量的结束点延长到整个循环结束
                                        loopVariables.add(opName);
                                        
                                        // 找到最大指令位置来延长生命周期
                                        int maxPos = 0;
                                        for (Integer pos : endPoints.values()) {
                                            if (pos > maxPos) maxPos = pos;
                                        }
                                        
                                        // 延长此变量的生命周期
                                        endPoints.put(opName, maxPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for (String var : startPoints.keySet()) {
            intervals.add(new RISCVInterval(var, startPoints.get(var), endPoints.get(var)+1));
        }
        
        intervals.sort(Comparator.comparingInt(i -> i.start));
    }

    private void allocateRegisters(){
        List<RISCVInterval> active = new ArrayList<>();
        for(RISCVInterval current : intervals){
            expireOldIntervals(current, active);
            if(availableRegs.size() > 0){
                String reg = availableRegs.remove(0);
                usedRegs.add(reg);
                RISCVLocation regLocation = new RISCVRegister(reg);
                RISCVLocationInfo locationInfo = new RISCVLocationInfo(current.name, regLocation);
                locationInfoMap.put(current.name, locationInfo);
                active.add(current);
                active.sort(Comparator.comparingInt(i -> i.end));
            }else{
                spillIntervalWithPosition(current, active);
            }
        }
    }

    private void expireOldIntervals(RISCVInterval current, List<RISCVInterval> active){
        int expiredCount = 0;
        for(int i=0;i<active.size();i++){
            if(active.get(i).end < current.start){
                expiredCount++;
                RISCVInterval expired = active.get(i);
                RISCVLocationInfo expiredInfo = locationInfoMap.get(expired.name);
                String reg = expiredInfo.regLocation.getCode();
                usedRegs.remove(reg);
                availableRegs.add(reg);
            }else break;
        }
        if(expiredCount > 0){
            for(int i=0;i<expiredCount;i++){
                active.remove(0);
            }
        }
    }

    private void spillIntervalWithPosition(RISCVInterval current, List<RISCVInterval> active) {
        // 检测是否是循环变量或条件比较变量
        boolean isLoopVar = current.name.equals("i") || current.name.contains("lt_") || 
                             current.name.contains("cond") || current.name.contains("while");
        
        if (!active.isEmpty() && active.get(active.size() - 1).end > current.end) {
            RISCVInterval lastToEnd = active.get(active.size() - 1);
            
            // 检查最长生命周期的变量是否是循环变量
            boolean lastIsLoopVar = lastToEnd.name.equals("i") || lastToEnd.name.contains("lt_") || 
                                    lastToEnd.name.contains("cond") || lastToEnd.name.contains("while");
            
            // 如果当前是循环变量但最长生命周期的不是，优先保留当前变量
            if (isLoopVar && !lastIsLoopVar) {
                // 溢出lastToEnd而不是当前循环变量
                RISCVLocationInfo lastInfo = locationInfoMap.get(lastToEnd.name);
                String reg = lastInfo.regLocation.getCode();
                lastInfo.isSpilled = true;
                lastInfo.spillPoint = current.start;
                lastInfo.locationChanged = true;
                
                RISCVLocation regLocation = new RISCVRegister(reg);
                RISCVLocationInfo currentInfo = new RISCVLocationInfo(current.name, regLocation);
                locationInfoMap.put(current.name, currentInfo);
                
                active.remove(lastToEnd);
                active.add(current);
                active.sort(Comparator.comparingInt(i -> i.end));
            }
            // 如果最长生命周期的是循环变量但当前不是，保留最长生命周期的
            else if (!isLoopVar && lastIsLoopVar) {
                RISCVLocationInfo info = new RISCVLocationInfo(current.name, null);
                info.isSpilled = true;
                info.spillPoint = current.start;
                locationInfoMap.put(current.name, info);
            }
            // 其他情况，按照原策略处理
            else {
                RISCVLocationInfo lastInfo = locationInfoMap.get(lastToEnd.name);
                String reg = lastInfo.regLocation.getCode();
                lastInfo.isSpilled = true;
                lastInfo.spillPoint = current.start;
                lastInfo.locationChanged = true;
                
                RISCVLocation regLocation = new RISCVRegister(reg);
                RISCVLocationInfo currentInfo = new RISCVLocationInfo(current.name, regLocation);
                locationInfoMap.put(current.name, currentInfo);
                
                active.remove(lastToEnd);
                active.add(current);
                active.sort(Comparator.comparingInt(i -> i.end));
            }
        } else {
            RISCVLocationInfo info = new RISCVLocationInfo(current.name, null);
            info.isSpilled = true;
            info.spillPoint = current.start;
            locationInfoMap.put(current.name, info);
        }
    }
    private int calculateStackSize() {
        int numStackVars = 0;
        
        for (RISCVLocationInfo info : locationInfoMap.values()) {
            if (info.isSpilled) {
                numStackVars++;
            }
        }
        int stackBytes = numStackVars * 4;  
        if (stackBytes % 16 != 0) {
            stackBytes = ((stackBytes / 16) + 1) * 16;
        }
        
        return stackBytes;
    }
    
    private void assignStackLocations(int stackSize) {
        currentStackOffset = 0;
        
        List<RISCVLocationInfo> spilledVars = new ArrayList<>();
        for (RISCVLocationInfo info : locationInfoMap.values()) {
            if (info.isSpilled) spilledVars.add(info);
        }
        
        spilledVars.sort(Comparator.comparingInt(info -> info.spillPoint));
        
        for (RISCVLocationInfo info : spilledVars) {
            currentStackOffset += 4;
            RISCVStackLocation stackLoc = new RISCVStackLocation(stackSize - currentStackOffset);
            info.stackLocation = stackLoc;
        }
    }

    private void handleSpill() {
        for (RISCVLocationInfo info : locationInfoMap.values()) {
            if (info.isSpilled && info.spillPoint == currentInstructionCount) {
                if(info.locationChanged){
                    // 检查避免空指针异常
                    if (info.regLocation != null && info.stackLocation != null) {
                        // 为循环变量添加额外注释
                        boolean isLoopVar = info.Varname.equals("i") || info.Varname.contains("lt_") || 
                                           info.Varname.contains("cond") || info.Varname.contains("while");
                        
                        if (isLoopVar) {
                            asmBuilder.addComment("保存循环变量 " + info.Varname);
                        }
                        
                        asmBuilder.addStore(info.regLocation.getCode(), info.stackLocation.getCode());
                    }
                }
            }
        }
    }


}

