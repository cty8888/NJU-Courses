import org.antlr.v4.runtime.Token;

public class SysYException extends RuntimeException {
    private final ErrorType errorType;
    private final int line;
    private final int column;
    
    public SysYException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.line = -1;
        this.column = -1;
    }
    
    public SysYException(ErrorType errorType, Token token) {
        super(String.format("%s at line %d, column %d", errorType.getMsg(), token.getLine(), token.getCharPositionInLine()));
        this.errorType = errorType;
        this.line = token.getLine();
        this.column = token.getCharPositionInLine();
    }
    
    public SysYException(ErrorType errorType, int line, int column) {
        super(String.format("%s at line %d, column %d", errorType.getMsg(), line, column));
        this.errorType = errorType;
        this.line = line;
        this.column = column;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
} 