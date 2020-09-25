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
                String line = irIn.nextLine().strip();

                if (line.endsWith(":"))
                {
                    doLabel(sb, line);
                    continue;
                }

                String[] sp = line.split(" ");

                switch (sp[0])
                {
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
