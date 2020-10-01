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
                    default:
                        assert (false);
                        break;
                }
            }
        }

        return sb.toString();
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

    private static void mPop(final StringBuilder sb, final String toWhere) {
        sb.append("\tlw ").append(toWhere).append(",0(sp)\n").append("\taddi sp,sp,4\n");
    }

    private static void mPush(final StringBuilder sb, final String fromWhere) {
        sb.append("\taddi sp,sp,-4\n").append("sw ").append(fromWhere).append(",0(sp)\n");
    }

}
