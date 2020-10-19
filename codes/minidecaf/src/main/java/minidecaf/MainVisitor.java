package minidecaf;

import minidecaf.MiniDecafParser.*;
import minidecaf.Type.*;

import java.util.HashMap;
import java.util.LinkedList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public final class MainVisitor extends MiniDecafBaseVisitor<Type>
{
    /* 函数相关 */
    private String currentFunc;
    private boolean containsMain = false;

    private StringBuilder ir; // 生成的IR

    private LinkedList<HashMap<String, Symbol>> symbolTable;
    private int localCntr;
    private int frameCntr;

    private int labelCntr;
    private int loopCntr;
    private LinkedList<Integer> loopStack;

    public IR getIR()
    {
        return new IR(ir.toString(), frameCntr);
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

        ir.append("func " + currentFunc + '\n');

        visit(ctx.compoundStatement());

        ir.append("endfunc ").append(currentFunc).append('\n');

        return new NoType();
    }

    @Override
    public Type visitCompoundStatement(CompoundStatementContext ctx)
    {
        startScope();

        for (var blockItem : ctx.blockItem())
            visit(blockItem);

        localCntr -= endScope();
        return new NoType();
    }

    @Override
    public Type visitBlockStmt(BlockStmtContext ctx)
    {
        return visit(ctx.statement());
    }

    @Override
    public Type visitBlockDecl(BlockDeclContext ctx)
    {
        return visit(ctx.declearation());
    }

    @Override
    public Type visitStmtRet(StmtRetContext ctx)
    {
        visit(ctx.expression());

        ir.append("\tret\n");

        return new NoType();
    }

    @Override
    public Type visitStmtExpr(StmtExprContext ctx)
    {
        var expr = ctx.expression();
        if (expr != null)
        {
            visit(expr);
            ir.append("\tpop\n");
        }
        return new NoType();
    }

    @Override
    public Type visitStmtIf(StmtIfContext ctx)
    {
        this.labelCntr++;

        visit(ctx.expression());

        String endLabel = ".endif" + this.labelCntr;
        String elseLable = ".elseif" + this.labelCntr;

        if (ctx.statement().size() == 2)
            ir.append("\tbeqz ").append(elseLable).append('\n');
        else
            ir.append("\tbeqz ").append(endLabel).append('\n');

        visit(ctx.statement(0)); // from 'then'

        if (ctx.statement().size() == 2)
        {
            ir.append("\tbr ").append(endLabel).append('\n');
            ir.append("\tlabel ").append(elseLable).append('\n');
            visit(ctx.statement(1));
        }

        ir.append("\tlabel ").append(endLabel).append('\n');

        return new NoType();
    }

    @Override
    public Type visitStmtCompound(StmtCompoundContext ctx)
    {
        return visit(ctx.compoundStatement());
    }

    @Override
    public Type visitStmtFor(StmtForContext ctx)
    {
        int curLoopNo = this.loopCntr++;

        String beginLabel = ".begin_loop" + curLoopNo;
        String contiLabel = ".continue_loop" + curLoopNo;
        String breakLabel = ".break_loop" + curLoopNo;

        ExpressionContext pre = null;
        ExpressionContext cond = null;
        ExpressionContext post = null;

        for (int i = 0; i < ctx.getChildCount(); i++)
        {
            if (ctx.getChild(i) instanceof ExpressionContext)
            {
                var expr = (ExpressionContext) ctx.getChild(i);
                if (ctx.getChild(i - 1).getText().equals("("))
                    pre = expr;
                else if (ctx.getChild(i + 1).getText().equals(")"))
                    post = expr;
                else
                    cond = expr;
            }
        }

        startScope();

        if (ctx.declearation() != null)
            visit(ctx.declearation());
        else
        {
            assert (pre != null);
            visit(pre);
            ir.append("\tpop\n");
        }

        ir.append("\tlabel ").append(beginLabel).append('\n');

        if (cond != null) visit(cond);

        ir.append("\tbeqz ").append(breakLabel).append('\n');

        loopStack.push(curLoopNo);
        visit(ctx.statement());
        loopStack.pop();

        ir.append("\tlabel ").append(contiLabel).append('\n');

        if (post != null)
        {
            visit(post);
            ir.append("\tpop\n");
        }

        ir.append("\tbr ").append(beginLabel).append('\n');
        ir.append("\tlabel ").append(breakLabel).append('\n');

        localCntr -= endScope();

        return new NoType();
    }

    @Override
    public Type visitStmtWhile(StmtWhileContext ctx)
    {
        int curLoopNo = loopCntr++;
        String beginLabel = ".begin_loop" + curLoopNo;
        String contiLabel = ".continue_loop" + curLoopNo;
        String breakLabel = ".break_loop" + curLoopNo;

        ir.append("\tlabel ").append(beginLabel).append('\n');

        visit(ctx.expression());

        ir.append("\tbeqz ").append(breakLabel).append('\n');

        loopStack.push(curLoopNo);
        visit(ctx.statement());
        loopStack.pop();

        ir.append("\tlabel ").append(contiLabel).append('\n');
        ir.append("\tbr ").append(beginLabel).append('\n');
        ir.append("\tlabel ").append(breakLabel).append('\n');

        return new NoType();
    }

    @Override
    public Type visitStmtDo(StmtDoContext ctx)
    {
        int curLoopNo = loopCntr++;
        String beginLabel = ".begin_loop" + curLoopNo;
        String contiLabel = ".continue_loop" + curLoopNo;
        String breakLabel = ".break_loop" + curLoopNo;

        ir.append("\tlabel ").append(beginLabel).append('\n');

        loopStack.push(curLoopNo);
        visit(ctx.statement());
        loopStack.pop();

        ir.append("\tlabel ").append(contiLabel).append('\n');

        visit(ctx.expression());
        ir.append("\tbnqz ").append(beginLabel).append('\n');

        ir.append("\tlabel ").append(breakLabel).append('\n');

        return new NoType();
    }

    @Override
    public Type visitStmtBreak(StmtBreakContext ctx)
    {
        if (loopStack.isEmpty()) reportError("Using break outside a loop.", ctx);
        ir.append("\tbr .break_loop").append(loopStack.peek()).append('\n');
        return new NoType();
    }

    @Override
    public Type visitStmtConti(StmtContiContext ctx) {
        if (loopStack.isEmpty()) reportError("Using continue outside a loop.", ctx);
        ir.append("\tbr .continue_loop").append(loopStack.peek()).append('\n');
        return new NoType();
    }

    @Override
    public Type visitDeclearation(DeclearationContext ctx)
    {
        String name = ctx.IDENT().getText();
        if (symbolTable.peek().get(name) != null) reportError("Re-Declearing an existing variable", ctx);

        symbolTable.peek().put(name, new Symbol(name, localCntr, new IntType()));

        localCntr++;
        if (localCntr > frameCntr) frameCntr = localCntr;

        if (ctx.expression() != null)
        {
            visit(ctx.expression());
            ir.append("\tframeaddr ").append(localCntr - 1).append('\n');
            ir.append("\tstore\n");
            ir.append("\tpop\n");
        }

        return new NoType();
    }

    @Override
    public Type visitExpression(ExpressionContext ctx)
    {
        return visit(ctx.exprAssign());
    }

    @Override
    public Type visitExprAssign(ExprAssignContext ctx)
    {
        if (ctx.getChildCount() == 1)
        {
            return visit(ctx.getChild(0));
        }
        else
        {
            assert (ctx.getChildCount() == 3);
            String name = ctx.IDENT().getText();
            Symbol symbol = this.lookup(name);
            if (symbol == null) reportError("Using an undefined variable", ctx);

            visit(ctx.expression());

            ir.append("\tframeaddr ").append(symbol.offset).append('\n');
            ir.append("\tstore\n");
            // sb.append("\tpop\n");

            return new IntType();
        }
    }

    @Override
    public Type visitExprTernary(ExprTernaryContext ctx)
    {
        if (ctx.getChildCount() == 1)
            return visit(ctx.getChild(0));
        else
        {
            this.labelCntr++;
            String endLabel = ".endter" + this.labelCntr;
            String elseLable = ".elseter" + this.labelCntr;

            visit(ctx.exprOr());
            ir.append("\tbeqz ").append(elseLable).append('\n');
            visit(ctx.expression());
            ir.append("\tbr ").append(endLabel).append('\n');
            ir.append("\tlabel ").append(elseLable).append('\n');
            visit(ctx.exprTernary()); // from 'then'
            ir.append("\tlabel ").append(endLabel).append('\n');

            return new IntType();

        }

    }

    @Override
    public Type visitExprOr(ExprOrContext ctx)
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
            ir.append("\torl\n");
            return new IntType();
        }
    }

    @Override
    public Type visitExprAnd(ExprAndContext ctx)
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
            ir.append("\tandl\n");
            return new IntType();
        }
    }

    @Override
    public Type visitExprEqual(ExprEqualContext ctx)
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
                    ir.append("\teq\n");
                    break;
                case "!=" :
                    ir.append("\tneq\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExprRelation(ExprRelationContext ctx)
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
                    ir.append("\tlt\n");
                    break;
                case ">" :
                    ir.append("\tgt\n");
                    break;
                case "<=" :
                    ir.append("\tle\n");
                    break;
                case ">=" :
                    ir.append("\tge\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExprAdd(ExprAddContext ctx)
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
                    ir.append("\tadd\n");
                    break;
                case "-" :
                    ir.append("\tsub\n");
                    break;
                default :
                    assert (false);
                    break;
            }
            return new IntType();
        }
    }

    @Override
    public Type visitExprMultiply(ExprMultiplyContext ctx)
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
                    ir.append("\tmul\n");
                    break;
                case "/" :
                    ir.append("\tdiv\n");
                    break;
                case "%" :
                    ir.append("\trem\n");
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
                ir.append("\tnotl\n");
                break;
            case "~" :
                ir.append("\tnotb\n");
                break;
            case "-" :
                ir.append("\tneg\n");
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

        ir.append("\tpush ").append(num.getText()).append('\n');

        return new IntType();
    }

    @Override
    public Type visitPrimParen(PrimParenContext ctx)
    {
        return visit(ctx.expression());
    }

    @Override
    public Type visitPrimIdent(PrimIdentContext ctx)
    {
        String name = ctx.IDENT().getText();
        Symbol symbol = this.lookup(name);

        if (symbol == null) reportError("Using an undefined variable", ctx);

        ir.append("\tframeaddr ").append(symbol.offset).append('\n');
        ir.append("\tload\n");

        return new IntType();
    }

    private void startScope()
    {
        symbolTable.push(new HashMap<>());
    }

    private Symbol lookup(String name)
    {
        for (var local : symbolTable)
        {
            if (local.containsKey(name)) return local.get(name);
        }
        return null;
    }

    private int endScope()
    {
        return symbolTable.pop().size();
    }

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

    MainVisitor()
    {
        this.ir = new StringBuilder();
        this.symbolTable = new LinkedList<>();
        this.localCntr = 0;
        this.frameCntr = 0;
        this.labelCntr = 0;
        this.loopCntr = 0;
        this.loopStack = new LinkedList<>();
    }
}
