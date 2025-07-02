public class RISCVStackLocation implements RISCVLocation {
    private int offset;
    
    public RISCVStackLocation(int offset) {
        this.offset = offset;
    }
    
    @Override
    public String getCode() {
        return offset + "(sp)";
    }
    
    @Override
    public boolean isRegister() {
        return false;
    }
} 