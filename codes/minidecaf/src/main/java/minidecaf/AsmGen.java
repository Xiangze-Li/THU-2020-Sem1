package minidecaf;

import java.util.Scanner;

/**
 * AsmGen
 */
public class AsmGen {
    private AsmGen() {
    }

    public static String genAsm(final String ir) {
        StringBuilder sb = new StringBuilder();

        try (Scanner irIn = new Scanner(ir)) {
            while (irIn.hasNextLine()) {
                String[] sp = irIn.nextLine().strip().split(" ");

                switch (sp[0]) {
                    case "func":
                        doFunc(sb, sp[1]);
                        break;
                    case "push":
                        doPush(sb, sp[1]);
                        break;
                    case "ret":
                        doRet(sb);
                        break;
                    case "notl":
                        doUnaryOp(sb, "seqz");
                        break;
                    case "notb":
                        doUnaryOp(sb, "not");
                        break;
                    case "neg":
                        doUnaryOp(sb, "neg");
                        break;
                    case "orl":
                    case "andl":
                    case "eq":
                    case "neq":
                    case "lt":
                    case "gt":
                    case "le":
                    case "ge":
                    case "add":
                    case "sub":
                    case "mul":
                    case "div":
                    case "rem":
                        doBinaryOp(sb, sp[0]);
                        break;

                    default:
                        assert (false);
                        break;
                }
            }
        }

        return sb.toString();
    }

    private static void mPop(final StringBuilder sb, final String toWhere) {
        sb.append("\tlw ").append(toWhere).append(",0(sp)\n");
        sb.append("\taddi sp,sp,4\n");
    }

    private static void mPush(final StringBuilder sb, final String fromWhere) {
        sb.append("\taddi sp,sp,-4\n");
        sb.append("\tsw ").append(fromWhere).append(",0(sp)\n");
    }

    private static void doFunc(final StringBuilder sb, final String label) {
        sb.append("\t.text\n\t.global ").append(label).append("\n");
        doLabel(sb, label);
    }

    private static void doLabel(final StringBuilder sb, final String label) {
        sb.append(label).append(":\n");
    }

    private static void doPush(final StringBuilder sb, final String pushWhat) {
        sb.append("\tli t1,").append(pushWhat).append("\n");
        mPush(sb, "t1");
    }

    private static void doRet(final StringBuilder sb) {
        mPop(sb, "a0");
        sb.append("\tjr ra\n");
    }

    private static void doUnaryOp(final StringBuilder sb, final String whatOp) {
        mPop(sb, "t1");
        sb.append("\t").append(whatOp).append(" t1,t1\n");
        mPush(sb, "t1");
    }

    private static void doBinaryOp(final StringBuilder sb, final String whatOp) {
        sb.append("\tlw t1,4(sp)\n").append("\tlw t2,0(sp)\n");

        switch (whatOp) {
            case "orl":
                sb.append("\tor t1,t1,t2\n").append("\tsnez t1,t1\n");
                break;
            case "andl":
                sb.append("\tsnez t1,t1\n").append("\tsnez t2,t2\n");
                sb.append("\tand t1,t1,t2\n");
                break;
            case "eq":
                sb.append("\tsub t1,t1,t2\n").append("\tseqz t1,t1\n");
                break;
            case "neq":
                sb.append("\tsub t1,t1,t2\n").append("\tsnez t1,t1\n");
                break;
            case "lt":
                sb.append("\tslt t1,t1,t2\n");
                break;
            case "gt":
                sb.append("\tslt t1,t2,t1\n");
                break;
            case "le":
                sb.append("\tslt t1,t2,t1\n").append("\txori t1,t1,1\n");
                break;
            case "ge":
                sb.append("\tslt t1,t1,t2\n").append("\txori t1,t1,1\n");
                break;
            case "add":
            case "sub":
            case "mul":
            case "div":
            case "rem":
                sb.append("\t").append(whatOp).append(" t1,t1,t2\n");
                break;
            default:
                assert (false);
                break;
        }

        sb.append("\taddi sp,sp,4\n").append("\tsw t1,0(sp)\n");
    }

}
