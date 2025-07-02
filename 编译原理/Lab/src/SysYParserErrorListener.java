import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import java.util.ArrayList;
import java.util.List;

public class SysYParserErrorListener extends BaseErrorListener {
    private boolean hasError = false;
    private final List<String> errorMessages = new ArrayList<>();
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                           Object offendingSymbol,
                           int line, int charPositionInLine,
                           String msg, RecognitionException e) {
        hasError = true;
        String errorMessage = "Error type B at Line " + line + ": " + msg;
        errorMessages.add(errorMessage);
    }
 
    public boolean hasError() {
        return hasError;
    }
    public void printParserErrorInformation() {
        for (String errorMessage : errorMessages) {
            System.out.println(errorMessage);
        }
    }
} 