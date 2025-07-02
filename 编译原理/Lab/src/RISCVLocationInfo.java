public class RISCVLocationInfo {
    String Varname;
    RISCVLocation regLocation;    // 当前位置(寄存器)
    boolean isSpilled;         // 是否已溢出
    boolean locationChanged;   // 是否位置改变
    int spillPoint;            // 溢出发生的指令位置
    RISCVLocation stackLocation; // 溢出后的栈位置

        public RISCVLocationInfo(String varName, RISCVLocation location) {
            this.Varname = varName;
            this.regLocation = location;
            this.isSpilled = false;
            this.locationChanged = false;
            this.spillPoint = -1;
            this.stackLocation = null;
        }
        
        
}
