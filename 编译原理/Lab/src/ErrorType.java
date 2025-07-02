public enum ErrorType {
    UNDEF_VAR(1, "Undefined variable"),
    UNDEF_FUNC(2, "Undefined function"),
    REDEF_VAR(3, "Redefined variable"),
    REDEF_FUNC(4, "Redefined function"),
    MISMATCH_ASSIGN(5, "Type mismatched for assignment"),
    MISMATCH_OPRAND(6, "Type mismatched for operands"),
    MISMATCH_RETURN(7, "Type mismatched for return"),
    FUNC_ARGS_MISMATCH(8, "Function is not applicable for arguments"),
    NOT_ARRAY(9, "Not an array"),
    NOT_FUNC(10, "Not a function"),
    INVALID_LVAL(11, "The left-hand side of an assignment must be a variable");
    
    private final int code;
    private final String msg;
    
    ErrorType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public int getCode() { return code; }
    public String getMsg() { return msg; }
}