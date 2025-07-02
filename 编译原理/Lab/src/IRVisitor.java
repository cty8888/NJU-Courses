import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import org.llvm4j.optional.Option;
import java.util.*;
import java.io.File;
//需要考虑的问题，函数参数和顶层局部变量 const问题 i
public class IRVisitor extends SysYParserBaseVisitor<Value> {
    private IRSymbolTable symbolTable;
    private static final Context context = new Context();
    private static final IRBuilder builder = context.newIRBuilder();
    private static final Module module = context.newModule("module");
    private static final IntegerType i32 =context.getInt32Type();
    private static final ConstantInt zero = i32.getConstant(0,false);
    private boolean inFunctionBody;
    private Stack<LoopInfo> loopStack;
    private File outputFile;

    private static class LoopInfo {
        BasicBlock condBlock;   
        BasicBlock bodyBlock;    
        BasicBlock afterBlock; 
        
        boolean needsJumpToCond;
        
        public LoopInfo(BasicBlock condBlock, BasicBlock bodyBlock, BasicBlock afterBlock) {
            this.condBlock = condBlock;
            this.bodyBlock = bodyBlock;
            this.afterBlock = afterBlock;
            this.needsJumpToCond = true;
        }
        
        public void setNoJumpNeeded() {
            this.needsJumpToCond = false;
        }
    }
    
    public IRVisitor(File outputFile) {
        this.symbolTable = new IRSymbolTable();
        this.inFunctionBody = false;
        this.loopStack = new Stack<>();
        this.outputFile = outputFile;
    }

	@Override 
    public Value visitProgram(SysYParser.ProgramContext ctx) { 
        visit(ctx.compUnit());
        module.dump(Option.of(outputFile));
        return null; 
    }

	@Override 
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) { 
        for(int i = 0; i<ctx.getChildCount()-1; i++){
            visit(ctx.getChild(i));
        }
        return null; 
    }

	@Override 
    public Value visitDecl(SysYParser.DeclContext ctx) {
        if(ctx.getChild(0) instanceof SysYParser.ConstDeclContext)
            return visit(ctx.constDecl());
        else if (ctx.getChild(0) instanceof SysYParser.VarDeclContext)
            return visit(ctx.varDecl());
        else{
            System.err.println("error: unknown declaration");
            return null;
        }
    }

	@Override
    public Value visitConstDecl(SysYParser.ConstDeclContext ctx) {
        for(SysYParser.ConstDefContext constDef : ctx.constDef()){
            visit(constDef);
        }
        return null; 
    }

	@Override 
    public Value visitBType(SysYParser.BTypeContext ctx) { 
        return null; 
    }

	@Override 
    public Value visitConstDef(SysYParser.ConstDefContext ctx) {
        String constName = ctx.IDENT().getText();
        //全局常量
        if(symbolTable.getCurrentFunction().isEmpty()){
            Value constValue = visit(ctx.constInitVal());
            GlobalVariable globalVar = module.addGlobalVariable(constName,i32,Option.empty()).unwrap();
            globalVar.setInitializer((Constant)constValue);
            symbolTable.addGlobalVariable(constName, globalVar);
            return globalVar;
        }else{//局部常量
            Value allocatePtr = builder.buildAlloca(i32, Option.of(constName));
            Value constInitVal = visit(ctx.constInitVal());
            builder.buildStore(allocatePtr,constInitVal);
            symbolTable.addLocalVariable(constName, allocatePtr);
            return allocatePtr;
        }
    }

	@Override 
    public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        if(ctx.constExp()!=null){
            return visit(ctx.constExp());
        }
        return null; 
    }
	
	@Override 
    public Value visitVarDecl(SysYParser.VarDeclContext ctx) {
        for(SysYParser.VarDefContext varDef : ctx.varDef()){
            visit(varDef);
        }
        return null; 
    }

	@Override 
    public Value visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        if(symbolTable.getCurrentFunction().isEmpty()){
            ConstantInt InitialValue = zero;
            if(ctx.initVal()!=null){
                Value initVal = visit(ctx.initVal());
                InitialValue = (ConstantInt) initVal;
            }
            GlobalVariable globalVar = module.addGlobalVariable(varName,i32,Option.empty()).unwrap();
            globalVar.setInitializer(InitialValue);
            symbolTable.addGlobalVariable(varName, globalVar);
            return globalVar;
        }else{
            Value allocatePtr = builder.buildAlloca(i32, Option.of(varName));
            if(ctx.initVal()!=null){
                Value initVal = visit(ctx.initVal());
                builder.buildStore(allocatePtr,initVal);
            }
            symbolTable.addLocalVariable(varName, allocatePtr);
            return allocatePtr;
        }
    }

	@Override 
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        return visit(ctx.exp());
    }

	@Override 
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        Type returnType;
        if(ctx.funcType().VOID()!=null){
            returnType = context.getVoidType();
        }else{
            returnType = i32;
        }
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        if(ctx.funcFParams()!=null){
            for(SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()){
                paramTypes.add(i32);
                paramNames.add(param.IDENT().getText());
            }
        }
        Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
        FunctionType funcType = context.getFunctionType(returnType, paramTypesArray, false);
        Function function = module.addFunction(funcName, funcType);
        symbolTable.addFunction(funcName, function);
        BasicBlock entryBlock = context.newBasicBlock(funcName+"Entry");
        function.addBasicBlock(entryBlock);
        builder.positionAfter(entryBlock);
        if(ctx.funcFParams()!=null){
            for(int i = 0; i<paramNames.size(); i++){
                Value paramAlloc =builder.buildAlloca(i32,Option.of(paramNames.get(i)));
                builder.buildStore(paramAlloc, function.getParameters()[i]);
                symbolTable.addLocalVariable(paramNames.get(i), paramAlloc);
            }
        }
        inFunctionBody = true;
        visit(ctx.block());
        inFunctionBody = false;
        symbolTable.setCurrentFunction(""); 
        return function; 
    }

	@Override 
    public Value visitFuncType(SysYParser.FuncTypeContext ctx) {
        return null;
    }

	@Override 
    public Value visitFuncFParams(SysYParser.FuncFParamsContext ctx) { 
        return null; 
    }

	@Override 
    public Value visitFuncFParam(SysYParser.FuncFParamContext ctx) { 
        return null; 
    }

	@Override 
    public Value visitBlock(SysYParser.BlockContext ctx) {
        boolean createNewScope = !inFunctionBody; // 只有非函数体直接块才创建新作用域
        if(createNewScope){
            symbolTable.enterScope();
        }
        if(inFunctionBody){
            inFunctionBody = false;
        }
        
        // 遍历块中的每个语句，检查是否有终止语句
        for(SysYParser.BlockItemContext blockItem : ctx.blockItem()){
            //System.out.println("visitBlockItem被调用 "+blockItem.getText());
            Value result = visit(blockItem);
            // 如果语句返回了终止标记，提前返回并传递终止信息
            if(result != null && result.equals(i32.getConstant(1, false))) {
                if(createNewScope){
                    symbolTable.exitScope();
                }
                return result;
            }
        }
        
        if(createNewScope){
            symbolTable.exitScope();
        }
        return null; 
    }
	
	@Override 
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        if(ctx.decl()!=null){
            return visit(ctx.decl());
        }else if(ctx.stmt()!=null){
            return visit(ctx.stmt());
        }
        return null; 
    }

	@Override 
    public Value visitStmt(SysYParser.StmtContext ctx) {
        //赋值语句
        if(ctx.lVal()!=null&&ctx.ASSIGN()!=null&&ctx.exp()!=null){
            //System.out.println("赋值语句"+ctx.getText());
            String varName = ctx.lVal().IDENT().getText();
            Value varPtr = symbolTable.findVariable(varName);
            if(varPtr==null){
                System.err.println("error: undefined variable "+varName);
                return null;
            }
            Value assignValue = visit(ctx.exp());
            builder.buildStore(varPtr,assignValue);
            return null;
        }
        else if(ctx.RETURN()!=null){
            if(ctx.exp()!=null){
                Value returnValue = visit(ctx.exp());
                builder.buildReturn(Option.of(returnValue));
            }else{
                builder.buildReturn(Option.empty());
            }
            // 标记当前块已终止，不需要额外的跳转
            markCurrentBlockAsTerminated();
            // 返回特殊标记，表示这个块已经终止了
            return i32.getConstant(1, false);
        }
        else if(ctx.exp()!=null&&ctx.SEMICOLON()!=null){
            visit(ctx.exp());
            return null;
        }

        else if(ctx.block()!=null){
            Value blockResult = visit(ctx.block());
            return blockResult; // 传递内部块的终止状态
        }
        else if(ctx.IF()!=null){
            Function currentFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
            BasicBlock thenBlock = context.newBasicBlock("if_true_");
            BasicBlock elseBlock = context.newBasicBlock("if_false_");
            BasicBlock nextBlock = context.newBasicBlock("if_next_");

            currentFunction.addBasicBlock(thenBlock);
            currentFunction.addBasicBlock(elseBlock);
            currentFunction.addBasicBlock(nextBlock);

            Value condValue = visit(ctx.cond());

            builder.buildConditionalBranch(condValue, thenBlock, elseBlock);

            // 处理then分支
            builder.positionAfter(thenBlock);
            Value thenResult = visit(ctx.stmt(0));
            // 如果then分支没有终止(return/break/continue)，则需要跳转到nextBlock
            if (thenResult == null || !thenResult.equals(i32.getConstant(1, false))) {
                builder.buildBranch(nextBlock);
            }

            // 处理else分支
            builder.positionAfter(elseBlock);
            if(ctx.ELSE()!=null){
                Value elseResult = visit(ctx.stmt(1));
                // 如果else分支没有终止(return/break/continue)，则需要跳转到nextBlock
                if (elseResult == null || !elseResult.equals(i32.getConstant(1, false))) {
                    builder.buildBranch(nextBlock);
                }
            }else{
                builder.buildBranch(nextBlock);
            }
            
            // 定位到下一个基本块
            builder.positionAfter(nextBlock);
            
            // 如果在循环中，需要考虑是否跳转回循环条件块
            if(!loopStack.isEmpty()){
                LoopInfo currentLoop = loopStack.peek();
                if (currentLoop.needsJumpToCond) {
                    builder.buildBranch(currentLoop.condBlock);
                }
            }
            
            return null;
        }
        // while语句
        else if(ctx.WHILE() != null){
            //System.out.println("visitWhile被调用 "+ctx.WHILE().getText());
            Function currentFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
            
            BasicBlock condBlock = context.newBasicBlock("whileCond");
            BasicBlock bodyBlock = context.newBasicBlock("whileBody");
            BasicBlock afterBlock = context.newBasicBlock("whileNext");
            
            currentFunction.addBasicBlock(condBlock);
            currentFunction.addBasicBlock(bodyBlock);
            currentFunction.addBasicBlock(afterBlock);
            
            // 从当前块跳到条件块
            builder.buildBranch(condBlock);
            
            // 条件计算
            builder.positionAfter(condBlock);
            Value condValue = visit(ctx.cond());
            builder.buildConditionalBranch(condValue, bodyBlock, afterBlock);
            
            // 循环体
            builder.positionAfter(bodyBlock);
            LoopInfo loopInfo = new LoopInfo(condBlock, bodyBlock, afterBlock);
            loopStack.push(loopInfo);
            
            Value bodyResult = visit(ctx.stmt(0));
            
            // 如果循环体没有终止语句(return/break/continue)，则需要跳回条件块
            if (loopInfo.needsJumpToCond && (bodyResult == null || !bodyResult.equals(i32.getConstant(1, false)))) {
                builder.buildBranch(condBlock);
            }
            
            loopStack.pop();
            
            // 定位到循环后的块
            builder.positionAfter(afterBlock);
            return null;
        }
        // break语句
        else if(ctx.BREAK() != null){
            //System.out.println("visitBreak被调用 "+ctx.BREAK().getText());
            if(loopStack.isEmpty()){
                System.err.println("error: break statement not in loop");
                return null;
            }
            
            LoopInfo currentLoop = loopStack.peek();
            builder.buildBranch(currentLoop.afterBlock);
            markCurrentBlockAsTerminated();
            return i32.getConstant(1, false);
        }
        
        // continue语句
        else if(ctx.CONTINUE() != null){
            //System.out.println("visitContinue被调用 "+ctx.CONTINUE().getText());
            if(loopStack.isEmpty()){
                System.err.println("error: continue statement not in loop");
                return null;
            }
            // 获取最内层循环的信息
            LoopInfo currentLoop = loopStack.peek();
            builder.buildBranch(currentLoop.condBlock);
            markCurrentBlockAsTerminated();
            return i32.getConstant(1, false);
        }
        //System.out.println("visitStmt被调用 "+ctx.getText()+"不应该到达这里");
        return null; 
    }

	@Override 
    public Value visitExp(SysYParser.ExpContext ctx) {
        //System.out.println("该表达式为"+ctx.getText());
        //括号表达式
        if(ctx.L_PAREN()!=null&&ctx.exp(0)!=null&&ctx.R_PAREN()!=null){
            return visit(ctx.exp(0));
        }
        //左值表达式
        else if(ctx.lVal()!=null){
            return visit(ctx.lVal());
        }
        //数字表达式
        else if(ctx.number()!=null){
            return visit(ctx.number());
        }
        //函数调用表达式
        else if(ctx.IDENT()!=null&&ctx.L_PAREN()!=null&&ctx.R_PAREN()!=null){
            //System.out.println("函数被调用"+ctx.IDENT().getText());
            String funcName = ctx.IDENT().getText();
            Function function = symbolTable.findFunction(funcName);
            if(function==null){
                //System.out.println("error: undefined function "+funcName);
                return null;
            }
            List<Value> args = new ArrayList<>();
            if(ctx.funcRParams()!=null){
                for(SysYParser.ParamContext param : ctx.funcRParams().param()){
                    Value arg = visit(param.exp());
                    if(arg ==null){
                        //System.out.println("error: undefined argument "+param.getText());
                        return null;
                    }
                    args.add(arg);
                }
            }
            int expectedParamCount = function.getParameters().length;
            if(args.size() != expectedParamCount) {
                //System.out.println("参数个数不匹配");
                return null;
            }
            Value[] argsArray = args.toArray(new Value[0]);
            return builder.buildCall(function, argsArray, Option.of("calltmp"));

        }
        //一元表达式
        else if(ctx.unaryOp()!=null){
            Value operand = visit(ctx.exp(0));
            if(ctx.unaryOp().PLUS()!=null){
                return operand;
            }else if(ctx.unaryOp().MINUS()!=null){
                return builder.buildIntSub(zero, operand, WrapSemantics.Unspecified, Option.of("negtmp"));
            }else if(ctx.unaryOp().NOT()!=null){
                Value isZero = builder.buildIntCompare(IntPredicate.NotEqual, operand, zero, Option.of("iszerotmp"));
                return builder.buildZeroExt(isZero, i32, Option.of("nottmp"));
            }
        }
        //二元表达式
        else if(ctx.exp().size()==2){
            Value left = visit(ctx.exp(0));
            Value right = visit(ctx.exp(1));
            if(ctx.MUL()!=null){
                return builder.buildIntMul(left, right, WrapSemantics.Unspecified, Option.of("multmp"));
            }else if(ctx.DIV()!=null){
                return builder.buildSignedDiv(left, right, false, Option.of("divtmp"));
            }else if(ctx.MOD()!=null){
                return builder.buildSignedRem(left, right, Option.of("modtmp"));
            }else if(ctx.PLUS()!=null){
                return builder.buildIntAdd(left, right, WrapSemantics.Unspecified, Option.of("addtmp"));
            }else if(ctx.MINUS()!=null){
                return builder.buildIntSub(left, right, WrapSemantics.Unspecified, Option.of("subtmp"));
            }
        }
        return null; 
    }
	
	@Override 
    public Value visitCond(SysYParser.CondContext ctx) {
        if(ctx.exp() != null){
            Value expValue = visit(ctx.exp());
            return expValue;
        }
        
        if(ctx.AND() != null){
            Function curFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
            BasicBlock rhsBlock = context.newBasicBlock("and_rhs");
            BasicBlock endBlock = context.newBasicBlock("and_end");
  
            curFunction.addBasicBlock(rhsBlock);
            curFunction.addBasicBlock(endBlock);
            
            
            Value lhsValue = visit(ctx.cond(0));
            
            builder.buildConditionalBranch(lhsValue, rhsBlock, endBlock);
            
            builder.positionAfter(rhsBlock);
            Value rhsValue = visit(ctx.cond(1));
            builder.buildBranch(endBlock);
            
            builder.positionAfter(endBlock);
            Value falseConst = context.getInt1Type().getConstant(0, false);
            Value result = builder.buildSelect(lhsValue, rhsValue, falseConst, Option.of("and_result"));
            
            return result;
        }
        
        if(ctx.OR() != null){
            Function curFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
            
            BasicBlock rhsBlock = context.newBasicBlock("or_rhs");
            BasicBlock endBlock = context.newBasicBlock("or_end");
            
            curFunction.addBasicBlock(rhsBlock);
            curFunction.addBasicBlock(endBlock);
            
            Value lhsValue = visit(ctx.cond(0));
            
            builder.buildConditionalBranch(lhsValue, endBlock, rhsBlock);
            
            builder.positionAfter(rhsBlock);
            Value rhsValue = visit(ctx.cond(1));
            builder.buildBranch(endBlock);
            
            builder.positionAfter(endBlock);
            
            Value trueConst = context.getInt1Type().getConstant(1, false);
            Value result = builder.buildSelect(lhsValue, trueConst, rhsValue, Option.of("or_result"));
            
            return result;
        }
        
        // 条件比较操作 (>, <, >=, <=, ==, !=)
        if(ctx.LT() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.SignedLessThan, left, right, Option.of("lttmp"));
        }
        if(ctx.GT() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.SignedGreaterThan, left, right, Option.of("gttmp"));
        }
        if(ctx.LE() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.SignedLessEqual, left, right, Option.of("letmp"));
        }
        if(ctx.GE() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.SignedGreaterEqual, left, right, Option.of("getmp"));
        }
        if(ctx.EQ() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.Equal, left, right, Option.of("eqtmp"));
        }
        if(ctx.NEQ() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return builder.buildIntCompare(IntPredicate.NotEqual, left, right, Option.of("neqtmp"));
        }

        return null; 
    }

	@Override 
    public Value visitLVal(SysYParser.LValContext ctx) {
        //System.out.println("visitLVal被调用 "+ctx.getText());
        String varName = ctx.IDENT().getText();
        Value varPtr = symbolTable.findVariable(varName);
        if(varPtr==null){
            System.err.println("error: undefined variable "+varName);
            return null;
        }
        return builder.buildLoad(varPtr, Option.of(varName));
    }

	@Override 
    public Value visitNumber(SysYParser.NumberContext ctx) {
        String number = ctx.getText();
        int value;
        if(number.startsWith("0x")||number.startsWith("0X")){
            value = Integer.parseInt(number.substring(2), 16);
        }else if(number.startsWith("0")&&number.length()>1){
            value = Integer.parseInt(number.substring(1), 8);
        }else{
            value = Integer.parseInt(number);
        }
        return i32.getConstant(value, false); 
    }

	@Override 
    public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) { 
        return null; 
    }

	@Override 
    public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) { 
        for (SysYParser.ParamContext param : ctx.param()) {
            visit(param);
        }
        return null;
    }

	@Override 
    public Value visitParam(SysYParser.ParamContext ctx) { 
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return null;
    }

	@Override 
    public Value visitConstExp(SysYParser.ConstExpContext ctx) {
        if(ctx.exp()!=null){
            return visit(ctx.exp());
        }
        return null; 
    }

    private void markCurrentBlockAsTerminated() {
        if (!loopStack.isEmpty()) {
            LoopInfo current = loopStack.peek();
            current.setNoJumpNeeded();
        }
    }

}
