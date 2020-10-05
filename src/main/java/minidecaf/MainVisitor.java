package minidecaf;

import minidecaf.MiniDecafParser.*;
import minidecaf.Type.*;

import java.util.HashMap;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public final class MainVisitor extends MiniDecafBaseVisitor<Type>
{
    /* 函数相关 */
    private String currentFunc;
    private boolean containsMain = false;

    private StringBuilder sb; // 生成的IR

    private HashMap<String, Symbol> symbolTable;
    private int localCntr;

    public IR getIR()
    {
        return new IR(sb.toString(), localCntr);
    }

    MainVisitor()
    {
        this.sb = new StringBuilder();
        this.symbolTable = new HashMap<>();
        this.localCntr = 0;
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

        sb.append("func " + currentFunc + "\n");

        for (var stmt : ctx.statement())
            visit(stmt);

        sb.append("endfunc ").append(currentFunc).append("\n");

        return new NoType();
    }

    @Override
    public Type visitStmtRet(StmtRetContext ctx)
    {
        visit(ctx.expression());

        sb.append("\tret\n");

        return new NoType();
    }

    @Override
    public Type visitStmtExpr(StmtExprContext ctx)
    {
        var expr = ctx.expression();
        if (expr != null)
        {
            visit(expr);
            sb.append("\tpop\n");
        }
        return new NoType();
    }

    @Override
    public Type visitStmtDecl(StmtDeclContext ctx)
    {
        return visit(ctx.declearation());
    }

    @Override
    public Type visitDeclearation(DeclearationContext ctx)
    {
        String name = ctx.IDENT().getText();
        if (symbolTable.get(name) != null) reportError("Re-Declearing an existing variable", ctx);

        symbolTable.put(name, new Symbol(name, localCntr, new IntType()));
        localCntr++;

        if (ctx.expression() != null)
        {
            visit(ctx.expression());
            sb.append("\tframeaddr ").append(localCntr - 1).append("\n");
            sb.append("\tstore\n");
            sb.append("\tpop\n");
        }

        return new NoType();
    }

    @Override
    public Type visitExpression(ExpressionContext ctx)
    {
        return visit(ctx.expr_assign());
    }

    @Override
    public Type visitExpr_assign(Expr_assignContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            String name = ctx.IDENT().getText();
            Symbol symbol = symbolTable.get(name);
            if (symbol == null) reportError("Using an undefined variable", ctx);

            visit(ctx.expression());

            sb.append("\tframeaddr ").append(symbol.offset).append("\n");
            sb.append("\tstore\n");
            // sb.append("\tpop\n");

            return new IntType();
        }
    }

    @Override
    public Type visitExpr_or(Expr_orContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            sb.append("\torl\n");
            return new IntType();
        }
    }

    @Override
    public Type visitExpr_and(Expr_andContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            sb.append("\tandl\n");
            return new IntType();
        }
    }

    @Override
    public Type visitExpr_equal(Expr_equalContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            switch (ctx.getChild(1).getText())
            {
                case "==" :
                    sb.append("\teq\n");
                    break;
                case "!=" :
                    sb.append("\tneq\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExpr_relation(Expr_relationContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            switch (ctx.getChild(1).getText())
            {
                case "<" :
                    sb.append("\tlt\n");
                    break;
                case ">" :
                    sb.append("\tgt\n");
                    break;
                case "<=" :
                    sb.append("\tle\n");
                    break;
                case ">=" :
                    sb.append("\tge\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExpr_add(Expr_addContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            switch (ctx.getChild(1).getText())
            {
                case "+" :
                    sb.append("\tadd\n");
                    break;
                case "-" :
                    sb.append("\tsub\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExpr_multiply(Expr_multiplyContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            visit(ctx.getChild(0));
            visit(ctx.getChild(2));
            switch (ctx.getChild(1).getText())
            {
                case "*" :
                    sb.append("\tmul\n");
                    break;
                case "/" :
                    sb.append("\tdiv\n");
                    break;
                case "%" :
                    sb.append("\trem\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitUnaryPrimary(UnaryPrimaryContext ctx)
    {
        return visit(ctx.primary());
    }

    @Override
    public Type visitUnaryOp(UnaryOpContext ctx)
    {
        visit(ctx.unary());

        switch (ctx.children.get(0).getText())
        {
            case "!" :
                sb.append("\tnotl\n");
                break;
            case "~" :
                sb.append("\tnotb\n");
                break;
            case "-" :
                sb.append("\tneg\n");
                break;
            default :
                assert (false);
                break;
        }

        return new IntType();
    }

    @Override
    public Type visitPrimIntLit(PrimIntLitContext ctx)
    {
        TerminalNode num = ctx.INTEGER();

        // 数字字面量不能超过整型的最大值
        if (compare(Integer.toString(Integer.MAX_VALUE), num.getText()) == -1) reportError("too large number", ctx);

        sb.append("\tpush ").append(num.getText()).append("\n");

        return new IntType();
    }

    @Override
    public Type visitPrimParen(PrimParenContext ctx)
    {
        return visit(ctx.expression());
    }

    @Override
    public Type visitPrimIdent(PrimIdentContext ctx) {
        String name = ctx.IDENT().getText();
        Symbol symbol = symbolTable.get(name);

        if (symbol == null) reportError("Using an undefined variable", ctx);

        sb.append("\tframeaddr ").append(symbol.offset).append("\n");
        sb.append("\tload\n");

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
