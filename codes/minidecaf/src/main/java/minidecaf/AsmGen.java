package minidecaf;

import java.util.Scanner;

/**
 * AsmGen
 */
public class AsmGen
{
    private AsmGen()
    {}

    public static String genAsm(final String ir)
    {
        StringBuilder sb = new StringBuilder();

        try (Scanner irIn = new Scanner(ir))
        {
            while (irIn.hasNextLine())
            {
                String[] sp = irIn.nextLine().strip().split(" ");

                switch (sp[0])
                {
                    case "func" :
                        doFunc(sb, sp[1]);
                        break;
                    case "push" :
                        doPush(sb, sp[1]);
                        break;
                    case "ret" :
                        doRet(sb);
                        break;
                    default :
                        break;
                }
            }
        }

        return sb.toString();
    }

    private static void doFunc(final StringBuilder sb, final String label) {
        sb.append("\t.text\n\t.global " + label.substring(0, label.length() - 2) + "\n");
        doLabel(sb, label);
    }

    private static void doLabel(final StringBuilder sb, final String label)
    {
        sb.append(label + "\n");
    }

    private static void doPush(final StringBuilder sb, final String pushWhat)
    {
        sb.append("\taddi sp,sp,-4\n").append("\tli t1," + pushWhat + "\n").append("\tsw t1,0(sp)\n");
    }

    private static void doRet(final StringBuilder sb)
    {
        sb.append("\tlw a0,0(sp)\n\taddi sp,sp,4\n\tjr ra\n");
    }
}
