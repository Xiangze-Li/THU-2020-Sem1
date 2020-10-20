package minidecaf;

import java.util.Map;

public class IR
{
    public final String irCode;
    // public final int localVarCntr;
    public final Map<String, Integer> funcFrameSize;

    public IR(final String irCode, final Map<String,Integer> funcFrameSize)
    {
        this.irCode = irCode;
        this.funcFrameSize = funcFrameSize;
    }
}
