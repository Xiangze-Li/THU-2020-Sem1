package minidecaf;

import java.util.Map;

public class IR
{
    public final String irCode;
    // public final int localVarCntr;
    public final Map<String, Integer> funcFrameSize;
    public final Map<String, Type> globalVar;

    public IR(final String irCode, final Map<String,Integer> funcFrameSize, final Map<String, Type> globalVar)
    {
        this.irCode = irCode;
        this.funcFrameSize = funcFrameSize;
        this.globalVar = globalVar;
    }
}
