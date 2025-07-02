import java.util.List;


public class FormattingVisitor extends SysYParserBaseVisitor<Void> {
    private int tab_depth = 0;
    private StringBuilder result = new StringBuilder();
    private final String INDENT="    ";
    private final String NEWLINE="\n";
    private final String SPACE=" ";

    boolean Special_if_else = false;

    private String indent() {
        return INDENT.repeat(tab_depth);
    }

    public String getResult() {
        String res = result.toString();
        return res.endsWith("\n") ? res.substring(0, res.length() - 1) : res;
    }

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        visit(ctx.compUnit());
        return null;
    }

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
        int childCount = ctx.getChildCount() - 1;  // 跳过最后一个子节点，因为最后一个字节点是EOF
        
        for (int i = 0; i < childCount; i++) {
            if (i > 0 && ctx.getChild(i) instanceof SysYParser.FuncDefContext) {//当函数为第一行第一个时，不需要空行，否则需要空行
                result.append(NEWLINE);
            }
            visit(ctx.getChild(i));
        }
        return null;
    }

    @Override
    public Void visitDecl(SysYParser.DeclContext ctx) {//Decl仅仅由ConstDecl和VarDecl组成，且只有一个子节点
        if (ctx.getChild(0) instanceof SysYParser.ConstDeclContext) 
            visit(ctx.constDecl());
        else if (ctx.getChild(0) instanceof SysYParser.VarDeclContext)
            visit(ctx.varDecl());
        else{
            System.out.println("error: unknown declaration");
        }
        return null;
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        result.append(indent());
        if(ctx.CONST()!=null){
            result.append("const");
        }
        result.append(SPACE);
        visit(ctx.bType());
        result.append(SPACE);
        
        
        // 使用isFirst标志控制逗号的添加，避免在第一个元素前添加逗号
        List<SysYParser.ConstDefContext> constDefs = ctx.constDef();
        boolean isFirst = true;
        for (SysYParser.ConstDefContext constDef : constDefs) {
            if (!isFirst) {
                result.append(",");
                result.append(SPACE);
            }
            isFirst = false;
            visit(constDef);
        }
        result.append(";").append(NEWLINE);
        return null;
    }
    @Override
    public Void visitBType(SysYParser.BTypeContext ctx){
        result.append(ctx.getText());
        return null;
    }

    @Override
    public Void visitConstDef(SysYParser.ConstDefContext ctx) {
        result.append(ctx.IDENT().getText());
   
        List<SysYParser.ConstExpContext> constExps = ctx.constExp();
        for (SysYParser.ConstExpContext constExp : constExps) {
            result.append("[");
            visit(constExp);
            result.append("]");
        }
        result.append(SPACE);
        result.append("=");
        result.append(SPACE);
        visit(ctx.constInitVal());
        return null;
    }

    @Override
    public Void visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        if (ctx.constExp() != null) {
            visit(ctx.constExp());
            return null;
        }
        
        result.append("{");
        

        int size = ctx.constInitVal().size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                result.append(",");
                result.append(SPACE);
            }
            visit(ctx.constInitVal(i));
        }
        
        result.append("}");
        return null;
    }


    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        result.append(indent());
        result.append(ctx.bType().getText());
        result.append(SPACE);
        
  
        List<SysYParser.VarDefContext> varDefs = ctx.varDef();
        boolean isFirst = true;
        for (SysYParser.VarDefContext varDef : varDefs) {
            if (!isFirst) {
                result.append(",");
                result.append(SPACE);
            }
            isFirst = false;
            visit(varDef);
        }
        
        result.append(";").append(NEWLINE);
        return null;
    }

    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        result.append(ctx.IDENT().getText());


        List<SysYParser.ConstExpContext> constExps = ctx.constExp();
        for (SysYParser.ConstExpContext constExp : constExps) {
            result.append("[");
            visit(constExp);
            result.append("]");
        }

        // 处理初始化赋值
        if (ctx.ASSIGN() != null) {
            result.append(SPACE);
            result.append("=");
            result.append(SPACE);
            visit(ctx.initVal());
        }
        
        return null;
    }

    @Override
    public Void visitInitVal(SysYParser.InitValContext ctx) {
        if (ctx.exp() != null) {
            visit(ctx.exp());
            return null;
        }
        
        result.append("{");
        

        List<SysYParser.InitValContext> initVals = ctx.initVal();
        boolean isFirst = true;
        for (SysYParser.InitValContext initVal : initVals) {
            if (!isFirst) {
                result.append(",");
                result.append(SPACE);
            }
            isFirst = false;
            visit(initVal);
        }
        
        result.append("}");
        return null;
    }


    // 函数定义
    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 函数头
        result.append(indent());
        visit(ctx.funcType());
        result.append(SPACE);
        result.append(ctx.IDENT().getText()).append("(");
        
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
        }
        
        // 函数体
        result.append(") {");
        result.append(NEWLINE);
        tab_depth++;
        visit(ctx.block());
        tab_depth--;
        result.append(indent()).append("}").append(NEWLINE);
        
        return null;
    }

    @Override
    public Void visitFuncType(SysYParser.FuncTypeContext ctx) {
        if(ctx.VOID()!=null){
            result.append("void");
        }else if(ctx.INT()!=null){
            result.append("int");
        }else{
            System.out.println("error: unknown function type");
        }
        return null;
    }

    @Override
    public Void visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
    
        List<SysYParser.FuncFParamContext> funcParams = ctx.funcFParam();
        boolean isFirst = true;
        for (SysYParser.FuncFParamContext funcParam : funcParams) {
            if (!isFirst) {
                result.append(",");
                result.append(SPACE);
            }
            isFirst = false;
            visit(funcParam);
        }
        return null;
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // 参数类型和名称
        result.append(ctx.bType().getText());
        result.append(SPACE);
        result.append(ctx.IDENT().getText());

        if (!ctx.L_BRACKT().isEmpty()&&ctx.R_BRACKT()!=null) {
            result.append("[]");
            
            List<SysYParser.ExpContext> exps = ctx.exp();
            for (SysYParser.ExpContext exp : exps) {
                result.append("[");
                visit(exp);
                result.append("]");
            }
        }
        return null;
    }

    // 语句块
    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        List<SysYParser.BlockItemContext> blockItems = ctx.blockItem();
        for (SysYParser.BlockItemContext blockItem : blockItems) {
            visit(blockItem);
        }
        return null;
    }

    // 块项
    @Override
    public Void visitBlockItem(SysYParser.BlockItemContext ctx) {
        if(ctx.getChild(0) instanceof SysYParser.DeclContext){
            visit(ctx.decl());
        }else if(ctx.getChild(0) instanceof SysYParser.StmtContext){
            visit(ctx.stmt());
        }else{
            System.out.println("error: unknown block item");
        }
        return null;
    }


    private void formatStatementBody(SysYParser.StmtContext stmtCtx) {
        if (stmtCtx.block() != null) {
            result.append(" {").append(NEWLINE);
            tab_depth++;
            visit(stmtCtx.block());
            tab_depth--;
            result.append(indent()).append("}").append(NEWLINE);
        } 
        else if (stmtCtx.IF() != null || stmtCtx.WHILE() != null
                || stmtCtx.lVal() != null || stmtCtx.exp() != null 
                || stmtCtx.RETURN() != null || stmtCtx.BREAK() != null
                || stmtCtx.CONTINUE() != null) {
            result.append(NEWLINE);
            tab_depth++;
            visit(stmtCtx);
            tab_depth--;
        } 
        else {
            // 处理空语句或不支持的语句类型
            result.append(NEWLINE);
            tab_depth++;
            result.append(indent()).append(";").append(NEWLINE);
            tab_depth--;
        }
    }

    // 语句
    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.lVal() != null && ctx.ASSIGN() != null && ctx.exp() != null) {
            result.append(indent());
            visit(ctx.lVal());
            result.append(SPACE);
            result.append("=");
            result.append(SPACE);
            visit(ctx.exp());
            result.append(";");
            result.append(NEWLINE);
            return null;
        } 
        
        if (ctx.block() != null) {
            result.append(indent());
            result.append("{");
            result.append(NEWLINE);
            tab_depth++;
            visit(ctx.block());
            tab_depth--;
            result.append(indent());
            result.append("}");
            result.append(NEWLINE);
            return null;
        } 
        
        if (ctx.RETURN() != null) {
            result.append(indent());
            result.append("return");
            if (ctx.exp() != null) {
                result.append(SPACE);
                visit(ctx.exp());
            }
            result.append(";");
            result.append(NEWLINE);
            return null;
        } 
        
        // if语句
        if (ctx.IF() != null) {
            // 处理缩进
            if (!Special_if_else) result.append(indent());
            else Special_if_else = false;

            result.append("if (");
            visit(ctx.cond());
            result.append(")");
            
            // if主体
            if (ctx.stmt(0) != null) {
                formatStatementBody(ctx.stmt(0));
            }
            
            // else部分
            if (ctx.ELSE() != null && ctx.stmt(1) != null) {
                result.append(indent());
                result.append("else");
                
                // else if
                if (ctx.stmt(1).IF() != null) {
                    Special_if_else = true;
                    result.append(SPACE);
                    visit(ctx.stmt(1));
                } 
                // else块或普通else
                else {
                    formatStatementBody(ctx.stmt(1));
                }
            }
            
            return null;
        } 
        
        // while语句
        if (ctx.WHILE() != null && ctx.cond() != null) {
            result.append(indent());
            result.append("while (");
            visit(ctx.cond());
            result.append(")");
            
            // while主体
            if (ctx.stmt(0) != null) {
                formatStatementBody(ctx.stmt(0));
            }
            
            return null;
        } 
        
        if (ctx.BREAK() != null) {
            result.append(indent());
            result.append("break;");
            result.append(NEWLINE);
            return null;
        } 
        
        if (ctx.CONTINUE() != null) {
            result.append(indent());
            result.append("continue;");
            result.append(NEWLINE);
            return null;
        } 
        
        if (ctx.exp() != null) {
            result.append(indent());
            visit(ctx.exp());
            result.append(";");
            result.append(NEWLINE);
            return null;
        } 
        
        result.append(indent());
        result.append(";");
        result.append(NEWLINE);
        return null;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        // 括号表达式
        if (ctx.L_PAREN() != null && ctx.exp(0) != null && ctx.R_PAREN() != null) {
            result.append("(");
            visit(ctx.exp(0));
            result.append(")");
            return null;
        } 
        
        // 左值
        if (ctx.lVal() != null) {
            visit(ctx.lVal());
            return null;
        } 
        
        // 数字
        if (ctx.number() != null) {
            visit(ctx.number());
            return null;
        } 
        
        // 函数调用
        if (ctx.IDENT() != null) {
            result.append(ctx.IDENT().getText());
            if (ctx.L_PAREN() != null) {
                result.append("(");
                if (ctx.funcRParams() != null) {
                    visit(ctx.funcRParams());
                }
                result.append(")");
            }
            return null;
        } 
        
        // 一元操作
        if (ctx.unaryOp() != null && ctx.exp(0) != null) {
            visit(ctx.unaryOp());
            visit(ctx.exp(0));
            return null;
        } 
        
        // 二元操作
        if (ctx.exp().size() == 2) {
            visit(ctx.exp(0));
            result.append(SPACE);
            if(ctx.MUL()!=null){
                result.append("*");
            }else if (ctx.DIV()!=null) {
                result.append("/");
            }else if (ctx.MOD()!=null) {
                result.append("%");
            }else if (ctx.PLUS()!=null) {
                result.append("+");
            }else if (ctx.MINUS()!=null) {
                result.append("-");
            }else{
                System.out.println("error: unknown binary operator");
            }
            result.append(SPACE);
            visit(ctx.exp(1));
            return null;
        } 
        return null;
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            visit(ctx.exp());
        } else {
            visit(ctx.cond(0));
            result.append(SPACE);
            if(ctx.AND()!=null){
                result.append("&&");
            }else if (ctx.OR()!=null) {
                result.append("||");
            }else if(ctx.LT()!=null){
                result.append("<");
            }else if(ctx.GT()!=null){
                result.append(">");
            }else if(ctx.LE()!=null){
                result.append("<=");
            }else if(ctx.GE()!=null){
                result.append(">=");
            }else if(ctx.NEQ()!=null){
                result.append("!=");
            }else if(ctx.EQ()!=null){
                result.append("==");
            }else{
                System.out.println("error: unknown conditional operator");
            }
            result.append(SPACE);
            visit(ctx.cond(1));
        }

        return null;
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        result.append(ctx.IDENT().getText());
        
        for (SysYParser.ExpContext expCtx : ctx.exp()) {
            result.append("[");
            visit(expCtx);
            result.append("]");
        }

        return null;
    }

    @Override
    public Void visitNumber(SysYParser.NumberContext ctx) {
        result.append(ctx.INTEGER_CONST().getText());
        return null;
    }

    @Override
    public Void visitUnaryOp(SysYParser.UnaryOpContext ctx) {
        //result.append(ctx.getText());
        if(ctx.PLUS()!=null){
            result.append("+");
        }else if(ctx.MINUS()!=null){
            result.append("-");
        }else if(ctx.NOT()!=null){
            result.append("!");
        }else{
            System.out.println("error: unknown unary operator");
        }
        return null;
    }

    @Override
    public Void visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
        boolean isFirst = true;
        for (SysYParser.ParamContext param : ctx.param()) {
            if (!isFirst) {
                result.append(",");
                result.append(SPACE);
            }
            isFirst = false;
            visit(param);
        }
        return null;
    }

    @Override
    public Void visitParam(SysYParser.ParamContext ctx) {
        visit(ctx.exp());
        return null;
    }

    @Override
    public Void visitConstExp(SysYParser.ConstExpContext ctx) {
        visit(ctx.exp());
        return null;
    }
}
