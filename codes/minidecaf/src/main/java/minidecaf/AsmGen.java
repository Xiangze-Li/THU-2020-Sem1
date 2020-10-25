package minidecaf;

import java.util.Scanner;

import minidecaf.Type.IntType;

/**
 * AsmGen
 */
public class AsmGen
{
    private AsmGen()
    {}

    public static String genAsm(final IR ir)
    {
        StringBuilder asm = new StringBuilder();
        StringBuilder data = new StringBuilder("\t.data\n");
        StringBuilder bss = new StringBuilder();
        String currentFunc = "";

        for (var kv : ir.globalVar.entrySet())
        {
            Type t = kv.getValue();
            if (!(t instanceof IntType) || ((IntType) t).initVal == null)
            {
                bss.append("\t.comm ").append(kv.getKey()).append(',').append(t.getSize()).append(",4\n");
            }
            else
            {
                var type = (IntType) t;
                data.append("\t.global ").append(kv.getKey()).append('\n');
                data.append("\t.align 4\n");
                data.append("\t.size ").append(kv.getKey()).append(',').append(type.getSize()).append('\n');
                data.append(kv.getKey()).append(":\n\t.word ").append(type.initVal).append('\n');
            }

        }

        asm.append(bss.toString()).append(data.toString());

        try (Scanner irIn = new Scanner(ir.irCode))
        {
            while (irIn.hasNextLine())
            {
                String line = irIn.nextLine();
                asm.append("# ").append(line).append('\n');
                String[] sp = line.strip().split(" ");

                switch (sp[0])
                {
                    case "func" :
                        currentFunc = sp[1];
                        doFunc(asm, sp[1], ir.funcFrameSize.get(sp[1]));
                        break;
                    case "endfunc" :
                        doEpilogue(asm, sp[1], ir.funcFrameSize.get(sp[1]));
                        currentFunc = "";
                        break;
                    case "label" :
                        doLabel(asm, sp[1]);
                        break;
                    case "call" :
                        doCall(asm, sp[1], sp[2]);
                        break;
                    case "ret" :
                        doRet(asm, currentFunc);
                        break;
                    case "push" :
                        doPush(asm, sp[1]);
                        break;
                    case "notl" :
                        doUnaryOp(asm, "seqz");
                        break;
                    case "notb" :
                        doUnaryOp(asm, "not");
                        break;
                    case "neg" :
                        doUnaryOp(asm, "neg");
                        break;
                    case "orl" :
                    case "andl" :
                    case "eq" :
                    case "neq" :
                    case "lt" :
                    case "gt" :
                    case "le" :
                    case "ge" :
                    case "add" :
                    case "addip" :
                    case "addpi" :
                    case "sub" :
                    case "subpi" :
                    case "subpp":
                    case "mul" :
                    case "div" :
                    case "rem" :
                        doBinaryOp(asm, sp[0]);
                        break;
                    case "frameaddr" :
                        doFrAddr(asm, sp[1]);
                        break;
                    case "globaladdr" :
                        doGlAddr(asm, sp[1]);
                        break;
                    case "load" :
                        doLoad(asm);
                        break;
                    case "store" :
                        doStore(asm);
                        break;
                    case "pop" :
                        doPop(asm);
                        break;
                    case "br" :
                        doJump(asm, sp[1]);
                        break;
                    case "beqz" :
                    case "bnez" :
                        doBranch(asm, sp[0], sp[1]);
                        break;
                    default :
                        assert (false);
                        break;
                }
            }
        }

        return asm.toString();
    }

    private static void doGlAddr(StringBuilder asm, String var)
    {
        asm.append("\tla t1,").append(var).append('\n');
        mPush(asm, "t1");
    }

    private static void doCall(final StringBuilder asm, final String funcName, final String paramSize)
    {
        asm.append("\tjal ").append(funcName).append('\n');
        asm.append("\taddi sp,sp,4*").append(paramSize).append('\n');
        mPush(asm, "a0");
    }

    private static void doBranch(StringBuilder asm, String whatBr, String toWhere)
    {
        mPop(asm, "t1");
        asm.append('\t').append(whatBr).append(" t1,").append(toWhere).append('\n');
    }

    private static void doJump(StringBuilder asm, String toWhere)
    {
        asm.append("\tj ").append(toWhere).append('\n');
    }

    private static void doEpilogue(final StringBuilder asm, final String label, final int localVarCntr)
    {
        doPush(asm, "0");
        doLabel(asm, label + "_epilogue");
        int framesize = 8 + 4 * localVarCntr;
        mPop(asm, "a0");
        asm.append("\tlw fp,").append(framesize).append("-8(sp)\n");
        asm.append("\tlw ra,").append(framesize).append("-4(sp)\n");
        asm.append("\taddi sp,sp,").append(framesize).append('\n');
        asm.append("\tjr ra\n");
    }

    private static void mPop(final StringBuilder asm, final String toWhere)
    {
        asm.append("\tlw ").append(toWhere).append(",0(sp)\n");
        asm.append("\taddi sp,sp,4\n");
    }

    private static void mPush(final StringBuilder asm, final String fromWhere)
    {
        asm.append("\taddi sp,sp,-4\n");
        asm.append("\tsw ").append(fromWhere).append(",0(sp)\n");
    }

    private static void doFrAddr(final StringBuilder asm, final String offset)
    {
        asm.append("\taddi t1,fp,-12-4*").append(offset).append('\n');
        mPush(asm, "t1");
    }

    private static void doFunc(final StringBuilder asm, final String label, final int localVarCntr)
    {
        asm.append('\n');
        asm.append("\t.text\n\t.global ").append(label).append('\n');
        doLabel(asm, label);

        // prologue
        int framesize = 8 + 4 * localVarCntr;
        asm.append("\taddi sp,sp,-").append(framesize).append('\n');
        asm.append("\tsw ra,").append(framesize).append("-4(sp)\n");
        asm.append("\tsw fp,").append(framesize).append("-8(sp)\n");
        asm.append("\taddi fp,sp,").append(framesize).append('\n');
    }

    private static void doLabel(final StringBuilder asm, final String label)
    {
        asm.append(label).append(":\n");
    }

    private static void doPush(final StringBuilder asm, final String pushWhat)
    {
        asm.append("\tli t1,").append(pushWhat).append('\n');
        mPush(asm, "t1");
    }

    private static void doRet(final StringBuilder asm, final String fromWhichFunc)
    {
        asm.append("\tj ").append(fromWhichFunc).append("_epilogue\n");
    }

    private static void doStore(final StringBuilder asm)
    {
        asm.append("\tlw t1,4(sp)\n").append("\tlw t2,0(sp)\n");
        asm.append("\taddi sp,sp,4\n");
        asm.append("\tsw t1,0(t2)\n");
        // asm.append("\tsw t2,0(sp)\n");
    }

    private static void doLoad(final StringBuilder asm)
    {
        asm.append("\tlw t1,0(sp)\n").append("\tlw t1,0(t1)\n").append("\tsw t1,0(sp)\n");
    }

    private static void doUnaryOp(final StringBuilder asm, final String whatOp)
    {
        mPop(asm, "t1");
        asm.append("\t").append(whatOp).append(" t1,t1\n");
        mPush(asm, "t1");
    }

    private static void doBinaryOp(final StringBuilder asm, final String whatOp)
    {
        asm.append("\tlw t1,4(sp)\n").append("\tlw t2,0(sp)\n");

        switch (whatOp)
        {
            case "orl" :
                asm.append("\tor t1,t1,t2\n").append("\tsnez t1,t1\n");
                break;
            case "andl" :
                asm.append("\tsnez t1,t1\n").append("\tsnez t2,t2\n");
                asm.append("\tand t1,t1,t2\n");
                break;
            case "eq" :
                asm.append("\tsub t1,t1,t2\n").append("\tseqz t1,t1\n");
                break;
            case "neq" :
                asm.append("\tsub t1,t1,t2\n").append("\tsnez t1,t1\n");
                break;
            case "lt" :
                asm.append("\tslt t1,t1,t2\n");
                break;
            case "gt" :
                asm.append("\tslt t1,t2,t1\n");
                break;
            case "le" :
                asm.append("\tslt t1,t2,t1\n").append("\txori t1,t1,1\n");
                break;
            case "ge" :
                asm.append("\tslt t1,t1,t2\n").append("\txori t1,t1,1\n");
                break;
            case "add" :
            case "sub" :
            case "mul" :
            case "div" :
            case "rem" :
                asm.append('\t').append(whatOp).append(" t1,t1,t2\n");
                break;
            case "addip" :
                asm.append("\tslli t1,t1,2\n").append("\tadd t1,t1,t2\n");
                break;
                case "addpi" :
                asm.append("\tslli t2,t2,2\n").append("\tadd t1,t1,t2\n");
                break;
            case "subpi" :
                asm.append("\tslli t2,t2,2\n").append("\tsub t1,t1,t2\n");
                break;
            case "subpp" :
                asm.append("\tsub t1,t1,t2\n").append("\tsrai t1,t1,2\n");
                break;
            default :
                assert (false);
                break;
        }

        asm.append("\taddi sp,sp,4\n").append("\tsw t1,0(sp)\n");
    }

    private static void doPop(final StringBuilder asm)
    {
        asm.append("\taddi sp,sp,4\n");
    }
}
