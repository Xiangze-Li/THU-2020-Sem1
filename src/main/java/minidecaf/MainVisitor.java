package minidecaf;

import minidecaf.MiniDecafParser.*;
import minidecaf.Type.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public final class MainVisitor extends MiniDecafBaseVisitor<Type>
{
    /* 函数相关 */
    private String currentFunc;
    private boolean containsMain = false;

    private StringBuilder sb; // 生成的IR

    MainVisitor(StringBuilder sb)
    {
        this.sb = sb;
    }

    @Override
    public Type visitProgram(ProgramContext ctx)
    {
        visit(ctx.function());

        if (!containsMain) reportError("no main function", ctx);

        return new NoType();
    }

    @Override
    public Type visitFunction(FunctionContext ctx)
    {
        currentFunc = ctx.IDENT().getText();
        if (currentFunc.equals("main")) containsMain = true;

        // sb.append("\t.text\n") // 表示以下内容在 text 段中
        // .append("\t.global " + currentFunc + "\n") // 让该 label 对链接器可见
        // .append(currentFunc + ":\n");

        sb.append("func " + currentFunc + ":\n");

        visit(ctx.statement());

        return new NoType();
    }

    @Override
    public Type visitReturnStatement(ReturnStatementContext ctx)
    {
        visit(ctx.expr());

        sb.append("\tret\n");

        return new NoType();
    }

    @Override
    public Type visitExpr(ExprContext ctx)
    {
        TerminalNode num = ctx.INTEGER();

        // 数字字面量不能超过整型的最大值
        if (compare(Integer.toString(Integer.MAX_VALUE), num.getText()) == -1) reportError("too large number", ctx);

        // sb.append("# number " + num.getText() + "\n").append("\tli a0, " + num.getText() + "\n");

        sb.append("\tpush " + num.getText() + "\n");

        return new IntType();
    }

    /* 一些工具方法 */
    /**
     * 比较大整数 s 和 t 的大小，可能的结果为小于（-1）、等于（0）或大于（1）。 这里 s 和 t 以字符串的形式给出，要求它们仅由数字 0-9 组成。
     */
    private int compare(String s, String t)
    {
        if (s.length() != t.length())
            return s.length() < t.length() ? -1 : 1;
        else
        {
            for (int i = 0; i < s.length(); ++i)
                if (s.charAt(i) != t.charAt(i)) return s.charAt(i) < t.charAt(i) ? -1 : 1;
            return 0;
        }
    }

    /**
     * 报错，并输出错误信息和错误位置。
     * 
     * @param s
     *            错误信息
     * @param ctx
     *            发生错误的环境，用于确定错误的位置
     */
    private void reportError(String s, ParserRuleContext ctx)
    {
        throw new RuntimeException("Error(" + ctx.getStart().getLine() + ", " + ctx.getStart().getCharPositionInLine()
                + "): " + s + ".\n");
    }
}
