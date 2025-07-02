import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Module;
import org.llvm4j.llvm4j.PhiInstruction;
import org.llvm4j.optional.Option;
import java.io.File;
import java.util.*;
import kotlin.Pair; 
import org.bytedeco.llvm.LLVM.LLVMModuleRef;

@SuppressWarnings("unchecked")
public class LLVMVisitor extends SysYParserBaseVisitor<Value> {
    private LLVMSymbolTable symbolTable;
    private static final Context context = new Context();
    private static final IRBuilder builder = context.newIRBuilder();
    private static final Module module = context.newModule("module");
    private static final IntegerType i32 = context.getInt32Type();
    private static final VoidType voidType = context.getVoidType();
    private static final ConstantInt zero = i32.getConstant(0,false);
    private static final ConstantInt one = i32.getConstant(1,false);
    private boolean inFunctionBody;
    private boolean hasReturn;
    private Stack<LoopInfo> loopStack;

    /**
     * 循环信息类，保存循环的条件块和出口块
     */
    private class LoopInfo{
        BasicBlock condBlock;
        BasicBlock afterBlock;
        boolean hasBreak;
        boolean hasContinue;
        
        /**
         * 创建循环信息
         * @param condBlock 条件块
         * @param afterBlock 循环结束后的块
         */
        public LoopInfo(BasicBlock condBlock,BasicBlock afterBlock){
            this.condBlock = condBlock;
            this.afterBlock = afterBlock;
            this.hasBreak = false;
            this.hasContinue = false;
        }
    }
    

    public LLVMVisitor(){
        this.symbolTable = new LLVMSymbolTable();
        this.inFunctionBody = false;
        this.hasReturn = false;
        this.loopStack = new Stack<>();

    }
    
    public void dispose() {
        module.close();
        builder.close();
        context.close();
    }

    private int evalExp(SysYParser.ExpContext ctx) {
        try {
            // 括号表达式
            if (ctx.L_PAREN() != null) {
                return evalExp(ctx.exp(0));
            }
            // 字面量
            if (ctx.number() != null) {
                String s = ctx.number().getText();
                // 十六进制数
                if (s.startsWith("0x") || s.startsWith("0X")) {
                    return (int)Long.parseLong(s.substring(2), 16);
                }
                // 八进制数
                else if (s.startsWith("0") && s.length() > 1) {
                    return (int)Long.parseLong(s.substring(1), 8);
                }
                // 十进制数
                else {
                    return (int)Long.parseLong(s);
                }
            }
            // 一元运算
            if (ctx.unaryOp() != null) {
                int v = evalExp(ctx.exp(0));
                // +v
                if (ctx.unaryOp().PLUS() != null) {
                    return +v;
                }
                // -v
                else if (ctx.unaryOp().MINUS() != null) {
                    return -v;
                }
                // !v (非0变0，0变1)
                else if (ctx.unaryOp().NOT() != null) {
                    return (v == 0 ? 1 : 0);
                }
            }
            // 二元运算
            if (ctx.exp().size() == 2) {
                int l = evalExp(ctx.exp(0));
                int r = evalExp(ctx.exp(1));
                // 乘法
                if (ctx.MUL() != null) {
                    return l * r;
                }
                // 除法 (检查除零)
                else if (ctx.DIV() != null) {
                    if (r == 0) {
                        throw new ArithmeticException("常量表达式中除零错误: " + ctx.getText());
                    }
                    return l / r;
                }
                // 求余 (检查除零)
                else if (ctx.MOD() != null) {
                    if (r == 0) {
                        throw new ArithmeticException("常量表达式中求余除零错误: " + ctx.getText());
                    }
                    return l % r;
                }
                // 加法
                else if (ctx.PLUS() != null) {
                    return l + r;
                }
                // 减法
                else if (ctx.MINUS() != null) {
                    return l - r;
                }
            }
            throw new RuntimeException("不支持的常量表达式: " + ctx.getText());
        } catch (NumberFormatException e) {
            throw new RuntimeException("常量表达式数值超出范围: " + ctx.getText(), e);
        } catch (ArithmeticException e) {
            throw new RuntimeException("常量表达式计算错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("常量表达式评估错误: " + ctx.getText(), e);
        }
    }
	
    public LLVMModuleRef getModule(){
        return module.getRef();
    }

    @Override 
    public Value visitProgram(SysYParser.ProgramContext ctx) {
        visit(ctx.compUnit());
        File file = new File("outputOld.ll");
        module.dump(Option.of(file));
        return null;
    }


	@Override 
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) { 
        for(int i = 0; i<ctx.getChildCount()-1;i++){
            visit(ctx.getChild(i));
        }
        return null;
    }

	@Override 
    public Value visitDecl(SysYParser.DeclContext ctx) {
        if(ctx.constDecl()!=null){
            return visit(ctx.constDecl());
        }else{
            return visit(ctx.varDecl());
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
        
        // 全局常量
        if(symbolTable.getCurrentFunction().isEmpty()){
            ConstantInt ci = zero;
            if (ctx.constInitVal() != null) {
                // 计算常量表达式的值
                int v = evalExp(ctx.constInitVal().constExp().exp());
                ci = i32.getConstant(v, false);
            }
            GlobalVariable globalVar = module.addGlobalVariable(constName, i32, Option.empty()).unwrap();
            globalVar.setInitializer(ci);

            symbolTable.addGlobalVariable(constName, globalVar);
            return globalVar;
        } 
        // 局部常量
        else {
            Value allocatePtr = builder.buildAlloca(i32, Option.of(constName));
            Value constInitVal = visit(ctx.constInitVal());
            builder.buildStore(allocatePtr, constInitVal);
            symbolTable.addLocalVariable(constName, allocatePtr);
            return allocatePtr;
        }
    }

	@Override 
    public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        return visit(ctx.constExp());
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
        
        // 全局变量
        if(symbolTable.getCurrentFunction().isEmpty()){
            ConstantInt ci = zero;
            if (ctx.initVal() != null) {
                // 如果有初始值，计算常量表达式的值
                int v = evalExp(ctx.initVal().exp());
                ci = i32.getConstant(v, false);
            }
            GlobalVariable globalVar = module.addGlobalVariable(varName, i32, Option.empty()).unwrap();
            globalVar.setInitializer(ci);
            symbolTable.addGlobalVariable(varName, globalVar);
            return globalVar;
        } 
        // 局部变量
        else {
            Value allocatePtr = builder.buildAlloca(i32, Option.of(varName));
            if(ctx.initVal() != null){
                // 如果有初始值，计算并存储
                Value initVal = visit(ctx.initVal());
                builder.buildStore(allocatePtr, initVal);
            } else {
                // 无初始值默认为0
                builder.buildStore(allocatePtr, zero);
            }
            symbolTable.addLocalVariable(varName, allocatePtr);
            return allocatePtr;
        }
    }
	@Override 
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        return visit(ctx.exp());

    }

    /**
     * 处理main函数定义
     * 这是程序的入口点
     * @param ctx 函数定义上下文
     * @return 函数值
     */
    private Value handleMainFunction(SysYParser.FuncDefContext ctx) {
        String funcName = "main";
        Type returnType = i32; // main函数返回值必须是int
        
        // 设置函数返回类型
        symbolTable.setFunctionReturnType(funcName, returnType);
        
        // 创建main函数
        FunctionType funcType = context.getFunctionType(returnType, new Type[0], false);
        Function function = module.addFunction(funcName, funcType);
        
        // 创建函数入口块
        BasicBlock entryBlock = context.newBasicBlock("main_entry");
        function.addBasicBlock(entryBlock);
        builder.positionAfter(entryBlock);
        
        // 将函数添加到符号表
        symbolTable.addFunction(funcName, function);
        symbolTable.enterScope();
        
        // 处理函数体
        inFunctionBody = true;
        hasReturn = false;
        visit(ctx.block());
        
        // 如果没有返回语句，添加默认的return 0
        if (!hasReturn) {
            builder.buildReturn(Option.of(zero));
        }
        
        // 离开函数作用域
        symbolTable.exitScope();
        symbolTable.resetCurrentFunction();
        return null;
    }

    /**
     * 访问函数定义，创建LLVM函数
     * @param ctx 函数定义上下文
     * @return null
     */
    @Override 
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        
        // 特殊处理main函数
        if (funcName.equals("main")) {
            return handleMainFunction(ctx);
        }
        
        // 确定函数返回类型
        Type returnType;
        if (ctx.funcType().INT() != null) {
            returnType = i32;
        } else {
            returnType = voidType;
        }

        symbolTable.setFunctionReturnType(funcName, returnType);
        
        // 处理函数参数
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        
        if (ctx.funcFParams() != null) {
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                paramTypes.add(i32); // 目前只支持i32类型参数
                paramNames.add(param.IDENT().getText());
            }
        }
        
        // 创建函数类型和函数
        Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
        FunctionType funcType = context.getFunctionType(returnType, paramTypesArray, false);
        Function function = module.addFunction(funcName, funcType);
        
        // 创建函数入口基本块
        BasicBlock entryBlock = context.newBasicBlock(funcName + "_entry");
        function.addBasicBlock(entryBlock);
        builder.positionAfter(entryBlock);
        
        // 将函数添加到符号表
        symbolTable.addFunction(funcName, function);
        symbolTable.enterScope();
        
        // 为函数参数分配局部变量空间
        if (!paramNames.isEmpty()) {
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                Value paramAlloc = builder.buildAlloca(i32, Option.of(paramName));
                builder.buildStore(paramAlloc, function.getParameters()[i]);
                symbolTable.addFunctionParameter(paramName, paramAlloc);
            }
        }
        
        // 处理函数体
        inFunctionBody = true;
        hasReturn = false;
        visit(ctx.block());
        
        // 如果没有返回语句，添加默认返回
        if (!hasReturn) {
            if (returnType.equals(voidType)) {
                builder.buildReturn(Option.empty());   
            } else {
                builder.buildReturn(Option.of(zero));   
            }
        }
        
        // 离开函数作用域
        symbolTable.exitScope();
        symbolTable.resetCurrentFunction();
        return null;
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
        boolean createNewScope = !inFunctionBody;
        if(createNewScope){
            symbolTable.enterScope();
        }
        if(inFunctionBody){
            inFunctionBody = false;
        }
        for(SysYParser.BlockItemContext blockItem : ctx.blockItem()){
            visit(blockItem);
        }
        if(createNewScope){
            symbolTable.exitScope();
        }
        return null;
    }
	
	@Override 
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        return visit(ctx.getChild(0));//返回给block，所以null没有空指针的错误
    }

    /**
     * 处理if语句
     * @param ctx 语句上下文
     * @param condValue 条件值
     * @return 是否生成了终止指令
     */
    private boolean handleIfStatement(SysYParser.StmtContext ctx, Value condValue) {
        Function currentFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
        // 使用唯一标识符创建基本块，避免名称冲突
        BasicBlock trueBlock = context.newBasicBlock("if_true_" + ctx.hashCode());
        BasicBlock falseBlock = context.newBasicBlock("if_false_" + ctx.hashCode());
        BasicBlock nextBlock = context.newBasicBlock("if_next_" + ctx.hashCode());

        currentFunction.addBasicBlock(trueBlock);
        currentFunction.addBasicBlock(falseBlock);
        currentFunction.addBasicBlock(nextBlock);

        // 根据条件决定跳转到哪个分支
        Value condBool = builder.buildIntCompare(IntPredicate.NotEqual, condValue, zero, Option.of("if_cond_" + ctx.hashCode()));
        builder.buildConditionalBranch(condBool, trueBlock, falseBlock);

        // Then 分支处理
        builder.positionAfter(trueBlock);
        boolean oldHasReturn = hasReturn;
        boolean oldHasBreak = false;
        boolean oldHasContinue = false;
        
        // 保存嵌套循环中的break/continue状态
        if (!loopStack.isEmpty()) {
            oldHasBreak = loopStack.peek().hasBreak;
            oldHasContinue = loopStack.peek().hasContinue;
            loopStack.peek().hasBreak = false;
            loopStack.peek().hasContinue = false;
        }
        
        hasReturn = false;
        visit(ctx.stmt(0));
        
        // 判断then分支是否已经生成了终止指令(return/break/continue)
        boolean thenTerminated = hasReturn || 
                                (!loopStack.isEmpty() && 
                                 (loopStack.peek().hasBreak || loopStack.peek().hasContinue));
                                 
        if (!thenTerminated) {
            // 如果没有终止，则跳转到下一个块
            builder.buildBranch(nextBlock);
        }

        // 恢复原有状态
        if (!loopStack.isEmpty()) {
            loopStack.peek().hasBreak = oldHasBreak;
            loopStack.peek().hasContinue = oldHasContinue;
        }
        hasReturn = oldHasReturn;

        // Else 分支处理
        builder.positionAfter(falseBlock);
        boolean hasElse = ctx.ELSE() != null;
        
        if (hasElse) {
            oldHasReturn = hasReturn;
            oldHasBreak = false;
            oldHasContinue = false;
            
            // 保存嵌套循环中的break/continue状态
            if (!loopStack.isEmpty()) {
                oldHasBreak = loopStack.peek().hasBreak;
                oldHasContinue = loopStack.peek().hasContinue;
                loopStack.peek().hasBreak = false;
                loopStack.peek().hasContinue = false;
            }
            
            hasReturn = false;
            visit(ctx.stmt(1));
            
            // 判断else分支是否已经生成了终止指令
            boolean elseTerminated = hasReturn || 
                                    (!loopStack.isEmpty() && 
                                     (loopStack.peek().hasBreak || loopStack.peek().hasContinue));
                                     
            if (!elseTerminated) {
                // 如果没有终止，则跳转到下一个块
                builder.buildBranch(nextBlock);
            }

            // 恢复原有状态
            if (!loopStack.isEmpty()) {
                loopStack.peek().hasBreak = oldHasBreak;
                loopStack.peek().hasContinue = oldHasContinue;
            }
            hasReturn = oldHasReturn;
        } else {
            // 无else分支，直接跳转到next块
            builder.buildBranch(nextBlock);
        }

        // 设置下一个指令位置
        builder.positionAfter(nextBlock);
        return false;
    }
    
    /**
     * 处理while语句
     * @param ctx 语句上下文
     * @return 是否生成了终止指令
     */
    private boolean handleWhileStatement(SysYParser.StmtContext ctx) {
        Function currentFunction = symbolTable.findFunction(symbolTable.getCurrentFunction());
        
        // 创建带有唯一标识符的基本块
        String uniqueId = "while_" + System.identityHashCode(ctx);
        BasicBlock condBlock = context.newBasicBlock("while_cond_" + uniqueId);
        BasicBlock bodyBlock = context.newBasicBlock("while_body_" + uniqueId);
        BasicBlock afterBlock = context.newBasicBlock("while_next_" + uniqueId);

        currentFunction.addBasicBlock(condBlock);
        currentFunction.addBasicBlock(bodyBlock);
        currentFunction.addBasicBlock(afterBlock);

        // 首先无条件跳转到条件判断块
        builder.buildBranch(condBlock);
        
        // 设置条件判断块
        builder.positionAfter(condBlock);
        Value condValue = visit(ctx.cond());
        Value condBool = builder.buildIntCompare(
            IntPredicate.NotEqual, 
            condValue, 
            zero, 
            Option.of("while_cond_" + uniqueId)
        );
        builder.buildConditionalBranch(condBool, bodyBlock, afterBlock);

        // 创建循环控制信息并入栈
        LoopInfo loopInfo = new LoopInfo(condBlock, afterBlock);
        loopStack.push(loopInfo);

        // 生成循环体代码
        builder.positionAfter(bodyBlock);
        boolean oldHasReturn = hasReturn;
        hasReturn = false;
        visit(ctx.stmt(0));

        // 如果循环体内没有break, continue或return，就跳回条件判断块
        if (!loopInfo.hasBreak && !loopInfo.hasContinue && !hasReturn) {
            builder.buildBranch(condBlock);
        }

        // 完成循环处理，恢复状态
        builder.positionAfter(afterBlock);
        loopStack.pop();
        hasReturn = oldHasReturn;
        return false;
    }

    /**
     * 处理函数调用表达式
     * @param ctx 表达式上下文
     * @param funcName 函数名
     * @return 函数调用的返回值
     */
    private Value handleFunctionCall(SysYParser.ExpContext ctx, String funcName) {
        // 查找函数
        Function function = symbolTable.findFunction(funcName);
        if(function == null) {
            throw new RuntimeException("找不到函数: " + funcName);
        }
        
        // 收集参数
        List<Value> params = new ArrayList<>();
        if(ctx.funcRParams() != null) {
            for(int i = 0; i < ctx.funcRParams().param().size(); i++) {
                SysYParser.ParamContext param = ctx.funcRParams().param(i);
                Value paramValue = visit(param.exp());
                if(paramValue == null) {
                    throw new RuntimeException("无法解析函数参数: " + param.getText());
                }
                params.add(paramValue);
            }
        }
        
        // 检查参数数量
        int expectedParamCount = function.getParameters().length;
        int actualParamCount = params.size();
        if(expectedParamCount != actualParamCount) {
            throw new RuntimeException(
                String.format("函数 %s 需要 %d 个参数，但提供了 %d 个", 
                    funcName, expectedParamCount, actualParamCount)
            );
        }
        
        // 调用函数
        Value[] paramsArray = params.toArray(new Value[0]);
        Type returnType = symbolTable.getFunctionReturnType(funcName);
        
        if(returnType.equals(voidType)) {
            // void 函数调用
            builder.buildCall(function, paramsArray, Option.empty());
            return zero; // 在表达式中返回0
        } else {
            // 返回值函数调用
            return builder.buildCall(
                function, 
                paramsArray, 
                Option.of("call_" + funcName + "_" + System.identityHashCode(ctx))
            );
        }
    }

    /**
     * 访问语句
     * @param ctx 语句上下文
     * @return null
     */
    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        // 赋值语句
        if (ctx.lVal() != null && ctx.ASSIGN() != null && ctx.exp() != null) {
            String varName = ctx.lVal().IDENT().getText();
            Value varPtr = symbolTable.findVariable(varName);
            /*if (varPtr == null) {
                throw new SysYException(ErrorType.UNDEF_VAR, ctx.lVal().IDENT().getSymbol());
            }*/
            Value assignValue = visit(ctx.exp());
            builder.buildStore(varPtr, assignValue);
            return null;
        } 
        // return语句
        else if (ctx.RETURN() != null) {
            hasReturn = true;
            if (ctx.exp() != null) {
                Value returnValue = visit(ctx.exp());
                builder.buildReturn(Option.of(returnValue));
            } else {
                builder.buildReturn(Option.empty());
            }
            return null;
        } 
        // 表达式语句
        else if (ctx.exp() != null && ctx.SEMICOLON() != null) {
            return visit(ctx.exp());
        } 
        // 块语句
        else if (ctx.block() != null) {
            return visit(ctx.block());
        } 
        // if语句
        else if (ctx.IF() != null) {
            Value condValue = visit(ctx.cond());
            return handleIfStatement(ctx, condValue) ? null : null;
        } 
        // while语句
        else if (ctx.WHILE() != null) {
            return handleWhileStatement(ctx) ? null : null;
        } 
        // break语句
        else if (ctx.BREAK() != null) {
            /*if (loopStack.isEmpty()) {
                throw new SysYException(
                    ErrorType.MISMATCH_OPRAND, 
                    "break statement outside of loop"
                );
            }*/
            LoopInfo loopInfo = loopStack.peek();
            loopInfo.hasBreak = true;
            builder.buildBranch(loopInfo.afterBlock);
            return null;
        } 
        // continue语句
        else if (ctx.CONTINUE() != null) {
            /*if (loopStack.isEmpty()) {
                throw new SysYException(
                    ErrorType.MISMATCH_OPRAND, 
                    "continue statement outside of loop"
                );
            }*/
            LoopInfo loopInfo = loopStack.peek();
            loopInfo.hasContinue = true;
            builder.buildBranch(loopInfo.condBlock);
            return null;
        }
        return null;
    }

	/**
     * 处理表达式
     */
    @Override 
    public Value visitExp(SysYParser.ExpContext ctx) {
        // 括号表达式
        if(ctx.L_PAREN() != null && ctx.exp(0) != null && ctx.R_PAREN() != null) {
            return visit(ctx.exp(0));
        }
        // 变量引用
        else if(ctx.lVal() != null) {
            return visit(ctx.lVal());
        }
        // 数字字面量
        else if(ctx.number() != null) {
            return visit(ctx.number());
        }
        // 函数调用
        else if(ctx.IDENT() != null && ctx.L_PAREN() != null && ctx.R_PAREN() != null) {
            return handleFunctionCall(ctx, ctx.IDENT().getText());
        }
        // 一元运算
        else if(ctx.unaryOp() != null) {
            Value operand = visit(ctx.exp(0));
            
            // 根据运算符类型处理
            if(ctx.unaryOp().PLUS() != null) {
                // +x 即 x
                return operand;
            }
            else if(ctx.unaryOp().MINUS() != null) {
                // -x 即 0-x
                return builder.buildIntSub(
                    zero, 
                    operand, 
                    WrapSemantics.Unspecified, 
                    Option.of("neg_" + System.identityHashCode(ctx))
                );
            }
            else if(ctx.unaryOp().NOT() != null) {
                // !x 即 x==0
                Value isZero = builder.buildIntCompare(
                    IntPredicate.Equal, 
                    operand, 
                    zero, 
                    Option.of("is_zero_" + System.identityHashCode(ctx))
                );
                return builder.buildZeroExt(
                    isZero, 
                    i32, 
                    Option.of("not_" + System.identityHashCode(ctx))
                );
            }
        }
        // 二元运算
        else if(ctx.exp().size() == 2) {
            Value leftOperand = visit(ctx.exp(0));
            Value rightOperand = visit(ctx.exp(1));
            String opId = "op_" + System.identityHashCode(ctx);
            
            // 根据运算符类型处理
            if(ctx.MUL() != null) {
                return builder.buildIntMul(
                    leftOperand, 
                    rightOperand, 
                    WrapSemantics.Unspecified, 
                    Option.of("mul_" + opId)
                );
            }
            else if(ctx.DIV() != null) {
                return builder.buildSignedDiv(
                    leftOperand, 
                    rightOperand, 
                    false, 
                    Option.of("div_" + opId)
                );
            }
            else if(ctx.MOD() != null) {
                return builder.buildSignedRem(
                    leftOperand, 
                    rightOperand, 
                    Option.of("mod_" + opId)
                );
            }
            else if(ctx.PLUS() != null) {
                return builder.buildIntAdd(
                    leftOperand, 
                    rightOperand, 
                    WrapSemantics.Unspecified, 
                    Option.of("add_" + opId)
                );
            }
            else if(ctx.MINUS() != null) {
                return builder.buildIntSub(
                    leftOperand, 
                    rightOperand, 
                    WrapSemantics.Unspecified, 
                    Option.of("sub_" + opId)
                );
            }
        }
        
        // 无法处理的表达式
        throw new RuntimeException("不支持的表达式: " + ctx.getText());
    }
	
	@Override 
    public Value visitCond(SysYParser.CondContext ctx) {
        // 基本条件：表达式
        if(ctx.exp() != null) {
            return visit(ctx.exp());
        }
        
        // 逻辑与操作 (AND): 短路求值
        else if(ctx.AND() != null) {
            // 为每个AND操作创建唯一的基本块，防止嵌套时冲突
            Function currentFunc = symbolTable.findFunction(symbolTable.getCurrentFunction());
            String uniqueId = "and_" + System.identityHashCode(ctx);
            
            // 为短路逻辑创建基本块
            BasicBlock evalRightBlock = context.newBasicBlock("and_right_" + uniqueId);
            BasicBlock endBlock = context.newBasicBlock("and_end_" + uniqueId);
            
            currentFunc.addBasicBlock(evalRightBlock);
            currentFunc.addBasicBlock(endBlock);
            
            // 计算左操作数
            Value leftValue = visit(ctx.cond(0));
            // 当前基本块，用于Phi指令
            BasicBlock leftBlock = builder.getInsertionBlock().unwrap();
            
            // 左操作数为真时继续计算右操作数，否则短路
            Value isLeftTrue = builder.buildIntCompare(
                IntPredicate.NotEqual, 
                leftValue, 
                zero, 
                Option.of("and_left_cond_" + uniqueId)
            );
            builder.buildConditionalBranch(isLeftTrue, evalRightBlock, endBlock);
            
            // 计算右操作数
            builder.positionAfter(evalRightBlock);
            Value rightValue = visit(ctx.cond(1));
            Value isRightTrue = builder.buildIntCompare(
                IntPredicate.NotEqual, 
                rightValue, 
                zero, 
                Option.of("and_right_cond_" + uniqueId)
            );
            Value rightResult = builder.buildZeroExt(isRightTrue, i32, Option.of("and_right_result_" + uniqueId));
            BasicBlock rightBlock = builder.getInsertionBlock().unwrap();
            builder.buildBranch(endBlock);
            
            // 合并结果：使用Phi指令
            builder.positionAfter(endBlock);
            PhiInstruction resultPhi = builder.buildPhi(i32, Option.of("and_result_" + uniqueId));
            Pair<BasicBlock, Value>[] incomingPairs = new Pair[] {
                new Pair<>(leftBlock, zero),
                new Pair<>(rightBlock, rightResult)
            };
            resultPhi.addIncoming(incomingPairs);
            
            return resultPhi;
        }
        
        // 逻辑或操作 (OR): 短路求值
        else if(ctx.OR() != null) {
            // 为每个OR操作创建唯一的基本块，防止嵌套时冲突
            Function currentFunc = symbolTable.findFunction(symbolTable.getCurrentFunction());
            String uniqueId = "or_" + System.identityHashCode(ctx);
            
            // 为短路逻辑创建基本块
            BasicBlock evalRightBlock = context.newBasicBlock("or_right_" + uniqueId);
            BasicBlock endBlock = context.newBasicBlock("or_end_" + uniqueId);
            
            currentFunc.addBasicBlock(evalRightBlock);
            currentFunc.addBasicBlock(endBlock);
            
            // 计算左操作数
            Value leftValue = visit(ctx.cond(0));
            // 当前基本块，用于Phi指令
            BasicBlock leftBlock = builder.getInsertionBlock().unwrap();
            
            // 左操作数为假时继续计算右操作数，否则短路
            Value isLeftTrue = builder.buildIntCompare(
                IntPredicate.NotEqual, 
                leftValue, 
                zero, 
                Option.of("or_left_cond_" + uniqueId)
            );
            builder.buildConditionalBranch(isLeftTrue, endBlock, evalRightBlock);
            
            // 计算右操作数
            builder.positionAfter(evalRightBlock);
            Value rightValue = visit(ctx.cond(1));
            Value isRightTrue = builder.buildIntCompare(
                IntPredicate.NotEqual, 
                rightValue, 
                zero, 
                Option.of("or_right_cond_" + uniqueId)
            );
            Value rightResult = builder.buildZeroExt(isRightTrue, i32, Option.of("or_right_result_" + uniqueId));
            BasicBlock rightBlock = builder.getInsertionBlock().unwrap();
            builder.buildBranch(endBlock);
            
            // 合并结果：使用Phi指令
            builder.positionAfter(endBlock);
            PhiInstruction resultPhi = builder.buildPhi(i32, Option.of("or_result_" + uniqueId));
            Pair<BasicBlock, Value>[] incomingPairs = new Pair[] {
                new Pair<>(leftBlock, one),
                new Pair<>(rightBlock, rightResult)
            };
            resultPhi.addIncoming(incomingPairs);
            
            return resultPhi;
        }
        
        // 比较运算: 小于
        else if(ctx.LT() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.SignedLessThan, "lt_" + System.identityHashCode(ctx));
        }
        
        // 比较运算: 大于
        else if(ctx.GT() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.SignedGreaterThan, "gt_" + System.identityHashCode(ctx));
        }
        
        // 比较运算: 小于等于
        else if(ctx.LE() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.SignedLessEqual, "le_" + System.identityHashCode(ctx));
        }
        
        // 比较运算: 大于等于
        else if(ctx.GE() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.SignedGreaterEqual, "ge_" + System.identityHashCode(ctx));
        }
        
        // 比较运算: 等于
        else if(ctx.EQ() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.Equal, "eq_" + System.identityHashCode(ctx));
        }
        
        // 比较运算: 不等于
        else if(ctx.NEQ() != null) {
            Value left = visit(ctx.cond(0));
            Value right = visit(ctx.cond(1));
            return createComparisonExpr(left, right, IntPredicate.NotEqual, "neq_" + System.identityHashCode(ctx));
        }
        
        // 无法识别的条件表达式
        throw new RuntimeException("不支持的条件表达式: " + ctx.getText());
    }

	/**
     * 创建比较表达式并返回结果
     * @param left 左操作数
     * @param right 右操作数
     * @param predicate 比较谓词
     * @param tmpName 临时变量名
     * @return 比较结果（i32类型）
     */
    private Value createComparisonExpr(Value left, Value right, IntPredicate predicate, String tmpName) {
        Value compareResult = builder.buildIntCompare(predicate, left, right, Option.of(tmpName + "tmp"));
        return builder.buildZeroExt(compareResult, i32, Option.of(tmpName + "_ext"));
    }
    



	@Override 
    public Value visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Value varPtr = symbolTable.findVariable(varName);
        /*if(varPtr == null){
            throw new SysYException(ErrorType.UNDEF_VAR, ctx.IDENT().getSymbol());
        }*/
        return builder.buildLoad(varPtr, Option.of(varName));
    }

	@Override 
    public Value visitNumber(SysYParser.NumberContext ctx) {
        String number = ctx.getText();
        long value;
        if(number.startsWith("0x")||number.startsWith("0X")){
            value = Long.parseLong(number.substring(2), 16);
        }else if(number.startsWith("0")&&number.length()>1){
            value = Long.parseLong(number.substring(1), 8);
        }else{
            value = Long.parseLong(number);
        }
        // 将结果截断为32位整数
        return i32.getConstant((int)value, false); 
    }

	@Override 
    public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) { 
        return null; 
    }

	@Override 
    public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) { 
        return null;
    }

	@Override 
    public Value visitParam(SysYParser.ParamContext ctx) { 
        return visit(ctx.exp());
    }

	@Override 
    public Value visitConstExp(SysYParser.ConstExpContext ctx) {
        return visit(ctx.exp());
    }

 
}
