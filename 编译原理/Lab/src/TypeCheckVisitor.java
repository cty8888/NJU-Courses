import org.antlr.v4.runtime.Token;
import java.util.List;
import java.util.ArrayList;
public class TypeCheckVisitor extends SysYParserBaseVisitor<Types> {
    private SymbolTable symbolTable;
    private boolean hasError;
    private IntType intType = IntType.getInstance();
    private Void_Type voidType = Void_Type.getInstance();
    private WrongType wrongType = WrongType.getInstance();
    private Types currentFuncReturnType;
    private boolean inFunctionBody;

    public TypeCheckVisitor(){  
        this.symbolTable = new SymbolTable();
        this.hasError = false;
        this.currentFuncReturnType = null;
        this.inFunctionBody = false;
    }
    
    public static void printSemanticError(ErrorType errorType, int line) {
        System.err.println("Error type " + errorType.getCode() + " at Line " + line + ": " + errorType.getMsg() + ".");
    }
    
    private void reportError(ErrorType type, Token token) {
        hasError = true;
        printSemanticError(type, token.getLine());
    }
    
    private boolean TypeCheck(Types leftType,Types rightType){
        if(leftType==null||rightType==null){
            return false;
        }
        if(leftType==wrongType||rightType==wrongType){
            return false;
        }
        if(leftType.isInt()&&rightType.isInt()){
            return true;
        }
        if(leftType.isArray()&&rightType.isArray()){
            ArrayType leftArrayType = (ArrayType) leftType;
            ArrayType rightArrayType = (ArrayType) rightType;
            int left_dim = leftArrayType.getDimension();
            int right_dim = rightArrayType.getDimension();
            if(left_dim!=right_dim){
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public Types visitProgram(SysYParser.ProgramContext ctx) {
        visit(ctx.compUnit());
        if(!hasError){
            System.err.println("No semantic errors in the program!");
        }
        return wrongType;
    }

    @Override
    public Types visitCompUnit(SysYParser.CompUnitContext ctx) {
        int childCount = ctx.getChildCount() - 1;
        for(int i = 0; i < childCount; i++){
            visit(ctx.getChild(i));
        }
        return wrongType;
    }

    @Override
    public Types visitDecl(SysYParser.DeclContext ctx) {
        if (ctx.getChild(0) instanceof SysYParser.ConstDeclContext) 
            visit(ctx.constDecl());
        else if (ctx.getChild(0) instanceof SysYParser.VarDeclContext)
            visit(ctx.varDecl());
        else{
            System.err.println("error: unknown declaration");
        }
        return wrongType;
    }

    @Override
    public Types visitConstDecl(SysYParser.ConstDeclContext ctx) {
        List<SysYParser.ConstDefContext> constDefs = ctx.constDef();
        for (SysYParser.ConstDefContext constDef : constDefs) {
            visit(constDef);
        }
        return wrongType;
    }

    @Override
    public Types visitConstDef(SysYParser.ConstDefContext ctx) {
        String name = ctx.IDENT().getText();
        if(symbolTable.findSymbol(name)!=null){
            reportError(ErrorType.REDEF_VAR, ctx.IDENT().getSymbol());
            return wrongType;
        }
        Types varType;
        if(ctx.constExp().size()>0){
            Types baseType = intType;
            for(int i=ctx.constExp().size()-1;i>=0;i--){
                Types expType = visit(ctx.constExp(i));
                if(expType==wrongType){
                    return wrongType;
                }
                int num_elements = 0;
                baseType = new ArrayType(baseType, num_elements);
            }
            varType = baseType;
        }else{
            varType = intType;
        }
        symbolTable.addSymbol(name, varType);
        if(ctx.constInitVal()!=null){
            Types initType = visit(ctx.constInitVal());
            // 数组自定义初始化暂时忽略
            if(initType == wrongType){
                return wrongType;
            }
            if(!varType.equals(initType)){
                reportError(ErrorType.MISMATCH_ASSIGN, ctx.IDENT().getSymbol());
            }
        }
        return wrongType;
    }

    @Override
    public Types visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        if(ctx.L_BRACE()!=null && ctx.R_BRACE()!=null){
            // 数组自定义初始化暂时忽略
            return wrongType;
        }
        if(ctx.constExp() != null) {
            return visit(ctx.constExp());
        }
        return wrongType;
    }

    @Override
    public Types visitVarDecl(SysYParser.VarDeclContext ctx) {
        List<SysYParser.VarDefContext> varDefs = ctx.varDef();
        for(SysYParser.VarDefContext varDef:varDefs){
            visit(varDef);
        }
        return wrongType;
    }

    @Override
    public Types visitVarDef(SysYParser.VarDefContext ctx) {
        String name = ctx.IDENT().getText();
        if(symbolTable.findSymbol(name)!=null){
            reportError(ErrorType.REDEF_VAR, ctx.IDENT().getSymbol());
            return wrongType;
        }
        Types varType;
        if(ctx.constExp().size()>0){
            Types baseType = intType;
            for(int i=ctx.constExp().size()-1;i>=0;i--){
                Types expType = visit(ctx.constExp(i));
                if(expType==wrongType){
                    return wrongType;
                }
                int num_elements = 0;
                baseType = new ArrayType(baseType, num_elements);
            }
            varType = baseType;
        }else{
            varType = intType;
        }
        symbolTable.addSymbol(name, varType);
        if(ctx.initVal()!=null){
            Types initType = visit(ctx.initVal());
            // 数组自定义初始化暂时忽略
            if(initType == wrongType){
                return wrongType;
            }
            if(!varType.equals(initType)){
                reportError(ErrorType.MISMATCH_ASSIGN, ctx.IDENT().getSymbol());
            }
        }
        return wrongType;
    }

    @Override
    public Types visitInitVal(SysYParser.InitValContext ctx) {
        if(ctx.L_BRACE()!=null && ctx.R_BRACE()!=null){
            // 数组自定义初始化暂时忽略，按照要求不检查
            return wrongType;
        }
        if(ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return wrongType;
    }

    @Override
    public Types visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if(symbolTable.findSymbol(funcName)!=null){
            reportError(ErrorType.REDEF_FUNC, ctx.IDENT().getSymbol());
            return wrongType;
        }
        Types returnType = visit(ctx.funcType());
        currentFuncReturnType = returnType;
        symbolTable.enterScope();
        ArrayList<Types> paramTypes = new ArrayList<>();
        if(ctx.funcFParams()!=null){
            for(SysYParser.FuncFParamContext param:ctx.funcFParams().funcFParam()){
                String paramName = param.IDENT().getText();
                if(symbolTable.findSymbol(paramName)!=null){
                    reportError(ErrorType.REDEF_VAR, param.IDENT().getSymbol());
                    continue;
                }
                Types paramType;
                if(param.L_BRACKT().size()>0){
                    Types baseType = intType;
                    for(int i=param.L_BRACKT().size()-1;i>=0;i--){
                        baseType = new ArrayType(baseType, 0);
                    }
                    paramType = baseType;
                }else{
                    paramType = intType;
                }
                symbolTable.addSymbol(paramName, paramType);
                paramTypes.add(paramType);
            }
        }
        FuncType funcType = new FuncType(returnType, paramTypes);
        symbolTable.addGlobalSymbol(funcName, funcType);
        inFunctionBody = true;
        visit(ctx.block());
        inFunctionBody = false;
        symbolTable.exitScope();
        currentFuncReturnType = null;
        return wrongType;
    }

    @Override
    public Types visitFuncType(SysYParser.FuncTypeContext ctx) {
        if(ctx.VOID()!=null){
            return voidType;
        }
        else return intType;
    }

    @Override
    public Types visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        return wrongType;
    }

    @Override
    public Types visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        return wrongType;
    }

    @Override
    public Types visitBlock(SysYParser.BlockContext ctx) {
        boolean createNewScope = !inFunctionBody; // 只有非函数体直接块才创建新作用域
        if (createNewScope) {
            symbolTable.enterScope();
        }
        // 如果是函数体直接块，重置标志，后续的块将创建自己的作用域
        if (inFunctionBody) {
            inFunctionBody = false;
        }
        for(SysYParser.BlockItemContext item : ctx.blockItem()) {
            visit(item);
        }
        if (createNewScope) {
            symbolTable.exitScope();
        }
        return wrongType;
    }

    @Override
    public Types visitBlockItem(SysYParser.BlockItemContext ctx) {
        if(ctx.decl() != null) {
            visit(ctx.decl());
        } else if(ctx.stmt() != null) {
            visit(ctx.stmt());
        }
        return wrongType;                        
    }

    @Override
    public Types visitStmt(SysYParser.StmtContext ctx) {
        if(ctx.lVal()!=null && ctx.ASSIGN()!=null && ctx.exp()!=null){
            Types lvalType = visit(ctx.lVal());
            
            // 如果左值是函数类型，则报告错误类型11
            if(lvalType != wrongType && lvalType.isFunction()){
                reportError(ErrorType.INVALID_LVAL, ctx.lVal().getStart());
                return wrongType;
            }
            
            Types expType = visit(ctx.exp());
            if(lvalType==wrongType || expType==wrongType){
                return wrongType;
            }         
            if(!TypeCheck(lvalType, expType)){
                reportError(ErrorType.MISMATCH_ASSIGN, ctx.ASSIGN().getSymbol());
                return wrongType;
            }
        }
        else if(ctx.RETURN()!=null){
            if(ctx.exp()!=null){
                Types returnType = visit(ctx.exp());
                
                // 检查返回值中是否包含函数标识符
                // 如果返回值是函数类型，应该报告错误类型7
                if(returnType != wrongType && returnType.isFunction()){
                    reportError(ErrorType.MISMATCH_RETURN, ctx.RETURN().getSymbol());
                    return wrongType;
                }
                
                if(returnType==wrongType){
                    return wrongType;
                }
                
                // 当前没有函数上下文
                if(currentFuncReturnType==null){
                    reportError(ErrorType.MISMATCH_RETURN, ctx.RETURN().getSymbol());
                    return wrongType;
                }
                // 返回类型不匹配，应报告类型7错误
                if(!TypeCheck(returnType, currentFuncReturnType)){
                    reportError(ErrorType.MISMATCH_RETURN, ctx.RETURN().getSymbol());
                    return wrongType;
                }
            }else{
                if(currentFuncReturnType!=null && !currentFuncReturnType.isVoid()){
                    reportError(ErrorType.MISMATCH_RETURN, ctx.RETURN().getSymbol());
                    return wrongType;
                }
            }
        }else if(ctx.block()!=null){
            visit(ctx.block());
        }else if(ctx.IF()!=null){
            visit(ctx.cond());
            visit(ctx.stmt(0));
            if(ctx.ELSE()!=null){
                visit(ctx.stmt(1));
            }
        }else if(ctx.WHILE()!=null){
            visit(ctx.cond());
            visit(ctx.stmt(0));
        }else if(ctx.BREAK()!=null){
            return wrongType;
        }else if(ctx.CONTINUE()!=null){
            return wrongType;
        }else if(ctx.exp()!=null){
            visit(ctx.exp());
        }
        return wrongType;
    }

    @Override
    public Types visitExp(SysYParser.ExpContext ctx) {
        if(ctx.L_PAREN()!=null && ctx.exp(0)!=null && ctx.R_PAREN()!=null){
            return visit(ctx.exp(0));
        }else if(ctx.lVal()!=null){
            Types lvalType = visit(ctx.lVal());
            // 函数类型在表达式中的处理应该放到上层，在返回语句中应该是类型7错误
            // 而不是在这里统一报告类型6错误
            // 我们在这里直接返回lvalType，让调用方根据上下文决定报告什么错误
            return lvalType;
        }else if(ctx.number()!=null){
            return visit(ctx.number());
        }else if(ctx.IDENT()!=null && ctx.L_PAREN()!=null && ctx.R_PAREN()!=null){
            String funcName = ctx.IDENT().getText();
            Types funcType = symbolTable.lookupSymbol(funcName);
            if(funcType==null){
                reportError(ErrorType.UNDEF_FUNC, ctx.IDENT().getSymbol());
                return wrongType;
            }
            if(!funcType.isFunction()){
                reportError(ErrorType.NOT_FUNC, ctx.IDENT().getSymbol());
                return wrongType;
            }
            FuncType func = (FuncType)funcType;
            ArrayList<Types> paramTypes = func.getParamTypes();
            int paramCount = paramTypes.size();
            
            // 检查 funcRParams 是否为 null
            int realParamCount = 0;
            if(ctx.funcRParams() != null) {
                realParamCount = ctx.funcRParams().param().size();
            }
            
            if(paramCount!=realParamCount){
                reportError(ErrorType.FUNC_ARGS_MISMATCH, ctx.IDENT().getSymbol());
                return wrongType;
            }
            
            if(ctx.funcRParams() != null && realParamCount > 0) {
                for(int i=0; i<paramCount; i++){
                    Types paramType = paramTypes.get(i);
                    Types realParamType = visit(ctx.funcRParams().param(i));
                    if(realParamType==wrongType){
                        return wrongType;
                    }
                    if(!TypeCheck(paramType, realParamType)){
                        reportError(ErrorType.FUNC_ARGS_MISMATCH, ctx.IDENT().getSymbol());
                        return wrongType;
                    }
                }
            }
            return func.getReturnType();
        }else if(ctx.unaryOp()!=null && ctx.exp(0)!=null){
            Types expType = visit(ctx.exp(0));
            if(expType==wrongType){
                return wrongType;
            }
            
            // 如果操作数是函数类型，报错类型6
            if(expType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp(0).getStart());
                return wrongType;
            }
            
            if(!expType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.unaryOp().getStart());
                return wrongType;
            }
            return expType;
        }else if(ctx.exp().size()==2){
            // 处理双操作数表达式，如 a + b
            // 先检查左操作数
            Types leftType = visit(ctx.exp(0));
            if(leftType==wrongType){
                return wrongType;
            }
            
            // 如果左操作数是函数类型，直接报错类型6，不再访问右操作数
            if(leftType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp(0).getStart());
                return wrongType;
            }
            
            // 如果左操作数不是int，直接报错类型6，不再访问右操作数
            if(!leftType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp(0).getStart());
                return wrongType;
            }
            
            // 左操作数OK，继续检查右操作数
            Types rightType = visit(ctx.exp(1));
            if(rightType==wrongType){
                return wrongType;
            }
            
            // 如果右操作数是函数类型，报错类型6
            if(rightType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp(1).getStart());
                return wrongType;
            }
            
            // 右操作数如果不是int，也报错类型6
            if(!rightType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp(1).getStart());
                return wrongType;
            }
            return intType;
        }
        return wrongType;
    }

    @Override
    public Types visitCond(SysYParser.CondContext ctx) {
        if(ctx.exp()!=null){
            Types expType = visit(ctx.exp());
            if(expType==wrongType){
                return wrongType;
            }
            
            // 如果条件表达式是函数类型，报错类型6
            if(expType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp().getStart());
                return wrongType;
            }
            
            if(!expType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.exp().getStart());
                return wrongType;
            }
            return intType;
        }else if(ctx.cond().size()==2){
            // 处理逻辑运算，如 a && b
            // 先检查左条件
            Types leftType = visit(ctx.cond(0));
            if(leftType==wrongType){
                return wrongType;
            }
            
            // 如果左条件是函数类型，直接报错，不再访问右条件
            if(leftType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.cond(0).getStart());
                return wrongType;
            }
            
            // 左条件如果不是int，直接报错，不再访问右条件
            if(!leftType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.cond(0).getStart());
                return wrongType;
            }
            
            // 继续检查右条件
            Types rightType = visit(ctx.cond(1));
            if(rightType==wrongType){
                return wrongType;
            }
            
            // 如果右条件是函数类型，报错
            if(rightType.isFunction()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.cond(1).getStart());
                return wrongType;
            }
            
            if(!rightType.isInt()){
                reportError(ErrorType.MISMATCH_OPRAND, ctx.cond(1).getStart());
                return wrongType;
            }
            return intType;
        }
        return wrongType;
    }

    @Override
    public Types visitLVal(SysYParser.LValContext ctx) {
        String name = ctx.IDENT().getText();
        Types varType = symbolTable.lookupSymbol(name);

        if(varType==null){
            reportError(ErrorType.UNDEF_VAR, ctx.IDENT().getSymbol());
            return wrongType;
        }
        
        // 在赋值左边使用的左值才报告类型11，否则不在此处报错
        // 例如 a = 1; 中左边的a是函数时才报告类型11
        // 当是表达式中的左值时，让上层的visitExp或visitStmt决定报告什么错误类型
        // 注意：这里无法区分是否是赋值左边，所以我们都不报错，让调用方决定
        
        if(varType.isVoid()){
            reportError(ErrorType.INVALID_LVAL, ctx.IDENT().getSymbol());
            return wrongType;
        }

        if(ctx.exp().size()>0){
            if(!varType.isArray()){
                reportError(ErrorType.NOT_ARRAY, ctx.IDENT().getSymbol());
                return wrongType;
            }
            for(SysYParser.ExpContext exp:ctx.exp()){
                Types expType = visit(exp);
                if(expType==wrongType || !expType.isInt()){
                    reportError(ErrorType.MISMATCH_OPRAND, exp.getStart());
                    return wrongType;
                }
            }
            for(int i=0; i<ctx.exp().size(); i++){
                if(varType instanceof ArrayType){
                    varType = ((ArrayType) varType).getElementType();
                }else{
                    reportError(ErrorType.NOT_ARRAY, ctx.IDENT().getSymbol());
                    return wrongType;
                }
            }
        }

        return varType;
    }

    @Override
    public Types visitNumber(SysYParser.NumberContext ctx) {
        return intType;
    }

    @Override
    public Types visitUnaryOp(SysYParser.UnaryOpContext ctx) {
        return wrongType;
    }

    @Override
    public Types visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
        return wrongType;
    }

    @Override
    public Types visitParam(SysYParser.ParamContext ctx) {
        if(ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return wrongType;
    }

    @Override
    public Types visitConstExp(SysYParser.ConstExpContext ctx) {
        if(ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return wrongType;
    }
}
