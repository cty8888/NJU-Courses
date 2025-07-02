public class RISCVInterval {
    String name;          
    int start;            
    int end;              
    
    public RISCVInterval(String name, int start, int end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public String toString() {
        return name + " [" + start + ", " + end + "]";
    }
}
