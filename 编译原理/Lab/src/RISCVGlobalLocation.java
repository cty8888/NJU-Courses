public class RISCVGlobalLocation implements RISCVLocation {
    private String label;
    
    public RISCVGlobalLocation(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    @Override
    public String getCode() {
        return label;
    }
    
    @Override
    public boolean isRegister() {
        return false;
    }
} 