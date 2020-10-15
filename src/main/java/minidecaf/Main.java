package minidecaf;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileWriter;
import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 2 || args[0].equals("-h") || args[0].equals("--help"))
        {
            System.out.printf("Usage:%n\tminidecaf <input minidecaf file> <output riscv assembly file>%n");
            return;
        }

        CharStream input = CharStreams.fromFileName(args[0]);
        MiniDecafLexer lexer = new MiniDecafLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniDecafParser parser = new MiniDecafParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        ParseTree tree = parser.program();
        MainVisitor visitor = new MainVisitor();

        visitor.visit(tree);

        IR ir = visitor.getIR();

        System.err.println(ir.irCode);

        String asm = AsmGen.genAsm(ir);

        try (FileWriter writer = new FileWriter(args[1]))
        {
            writer.write(asm);
            System.out.println(asm);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw e;
        }
    }
}
