import java.io.*;

public class RISCVAsmBuilder {
    private StringBuilder dataSection;
    private StringBuilder textSection;
    
    public RISCVAsmBuilder() {
        dataSection = new StringBuilder();
        textSection = new StringBuilder();
    }
    
    public void addGlobal(String name, int initialValue) {
        dataSection.append("  .data\n")
                  .append(name).append(":\n")
                  .append("  .word ").append(initialValue).append("\n\n");
    }
    
    public void startFunction(String FuncName) {
        textSection.append("  .text\n")
                  .append("  .globl ").append(FuncName).append("\n")
                  .append(FuncName).append(":\n");
    }
    
    public void addPrologue(int stackSize) {
        textSection.append("  addi sp, sp, -").append(stackSize).append("      # prologue\n");
    }
    public void addBlockEntry(String blockName){
        textSection.append(blockName).append(":\n");
    }
    
    public void addEpilogue(int stackSize) {
        textSection.append("  addi sp, sp, ").append(stackSize).append("       # epilogue\n");
    }
    

    
    public void addExitCall() {
        textSection.append("  li a7, 93\n")
                  .append("  ecall\n");
    }
    
    public void addLabel(String label) {
        textSection.append(label).append(":\n");
    }
    
    public void addLoad(String dest, String src) {
        textSection.append("  lw ").append(dest).append(", ").append(src).append("\n");
    }
    
    public void addStore(String src, String dest) {
        textSection.append("  sw ").append(src).append(", ").append(dest).append("\n");
    }
    
    public void addLoadImmediate(String dest, int value) {
        textSection.append("  li ").append(dest).append(", ").append(value).append("\n");
    }
    
    public void addLoadAddress(String dest, String label) {
        textSection.append("  la ").append(dest).append(", ").append(label).append("\n");
    }
    
    public void addMove(String dest, String src) {
        textSection.append("  mv ").append(dest).append(", ").append(src).append("\n");
    }
    
    public void addBinaryOp(String op, String dest, String src1, String src2) {
        textSection.append("  ").append(op).append(" ")
                  .append(dest).append(", ")
                  .append(src1).append(", ")
                  .append(src2).append("\n");
    }
    public void addOneOp(String op, String dest, String src) {
        textSection.append("  ").append(op).append(" ")
                  .append(dest).append(", ")
                  .append(src).append("\n");
    }

    public void addJump(String label) {
        textSection.append("  j ").append(label).append("\n");
    }

    public void addBranchZero(String op, String src,String label) {
        textSection.append("  ").append(op).append(" ")
                  .append(src).append(", ")
                  .append("x0").append(", ")
                  .append(label).append("\n");
    }
    
    public void addComment(String comment) {
        textSection.append("  # ").append(comment).append("\n");
    }
    
    public void dumpToFile(File file) throws FileNotFoundException {

    
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(dataSection.toString());
            writer.print(textSection.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileNotFoundException("Could not write to file: " + file.getPath() + " - " + e.getMessage());
        }
    }
}
