import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.IOException;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            return;
        }
        String source = args[0];
        String output = args[1];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer Lexer = new SysYLexer(input);
        SysYLexerErrorListener myLexerErrorListener = new SysYLexerErrorListener();
        Lexer.removeErrorListeners();
        Lexer.addErrorListener(myLexerErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(Lexer);
        SysYParser parser = new SysYParser(tokens);
        SysYParserErrorListener myParserErrorListener = new SysYParserErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(myParserErrorListener);
        ParseTree tree = parser.program();
        
        LLVMVisitor llvmVisitor = new LLVMVisitor();
        llvmVisitor.visit(tree);
        
        LLVMModuleRef module = llvmVisitor.getModule();
        
        OptimizationManager optimizer = new OptimizationManager(module);
        
        // module = optimizer.runConstantPropagation();
        // module = optimizer.runUnusedVarElimination();
        // module = optimizer.runDeadCodeElimination();
        module = optimizer.runAllOptimizations();
        ByteBuffer errorMsgBuf = ByteBuffer.allocate(1024);
        LLVMPrintModuleToFile(module, output, errorMsgBuf);
    }
}