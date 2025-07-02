public class RISCVRegister implements RISCVLocation {
    private String name;
    
    public RISCVRegister(String name) {
        this.name = name;
    }
    
    @Override
    public String getCode() {
        return name; 
    }

    @Override
    public boolean isRegister() {
        return true;
    }
    
    // 获取寄存器名称
    public String getName() {
        return name;
    }
}
