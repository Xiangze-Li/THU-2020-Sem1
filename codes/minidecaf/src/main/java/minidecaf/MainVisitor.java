package minidecaf;

import minidecaf.MiniDecafParser.*;
import minidecaf.Type.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public final class MainVisitor extends MiniDecafBaseVisitor<Type>
{
    /* 函数相关 */
    private String currentFunc;
    private boolean containsMain = false;
    private Map<String, Func> funcDeclared;
    private Map<String, Func> funcDefined;
    private Map<String, Integer> funcFrameSize;

    private StringBuilder ir; // 生成的IR

    // 变量相关
    private int localCntr;
    private int frameCntr;
    private Deque<Map<String, Symbol>> symbolTable;
    private Map<String, Type> globDeclared;
    // private Map<String, Type> globDefined;
    // private Map<String, Integer> globDefVal;

    private int labelCntr;
    private int loopCntr;
    private LinkedList<Integer> loopStack;

    public IR getIR()
    {
        return new IR(ir.toString(), funcFrameSize, globDeclared);
    }

    @Override
    public Type visitProgram(ProgramContext ctx)
    {
        visitChildren(ctx);

        if (!containsMain) reportError("no main function", ctx);

        // for ()

        return new NoType();
    }

    @Override
    public Type visitDeclGlNaive(DeclGlNaiveContext ctx)
    {
        String name = ctx.IDENT().getText();
        if (funcDeclared.containsKey(name)) reportError("A global variable and a function have the same name", ctx);

        Type type = visit(ctx.type());
        Type stored = globDeclared.get(name);

        if (stored == null)
        {
            var num = ctx.INTEGER();
            if (num != null)
            {
                IntType intType = (IntType) type;
                intType.initVal = Integer.valueOf(num.getText());
            }
        }
        else
        {
            if (!stored.equals(type)) reportError("Different global variables with same name are declared", ctx);
            var num = ctx.INTEGER();
            if (num != null)
            {
                IntType intStored = (IntType) stored;
                if (intStored.initVal != null) reportError("Initializing a global variable twice", ctx);
                IntType intType = (IntType) type;
                intType.initVal = Integer.valueOf(num.getText());
            }
        }
        globDeclared.put(name, type.valueCast(ValueCat.LVALUE));

        return new NoType();
    }

    @Override
    public Type visitDeclGlArray(DeclGlArrayContext ctx)
    {
        // TODO + visitDeclGlArray
        String name = ctx.IDENT().getText();
        if (funcDeclared.containsKey(name)) reportError("A global variable and a function have the same name", ctx);

        Deque<Type> types = new ArrayDeque<>();
        types.add(visit(ctx.type()).valueCast(ValueCat.LVALUE));
        for (int i = ctx.INTEGER().size() - 1; i >= 0; i--)
        {
            int x = Integer.parseInt(ctx.INTEGER(i).getText());
            if (x == 0) reportError("the dimension of array cannot be 0", ctx);
            types.addFirst(new ArrayType(types.getFirst(), x));
        }

        Type type = types.getFirst();

        if (globDeclared.containsKey(name) && !globDeclared.get(name).equals(type))
            reportError("different global variables with same name are declared", ctx);
        globDeclared.put(name, type);

        return new NoType();
    }

    @Override
    public Type visitFuncDecl(FuncDeclContext ctx)
    {
        String name = ctx.IDENT(0).getText();
        if (globDeclared.containsKey(name)) reportError("Function name duplicated with global var", ctx);

        Type returnType = visit(ctx.type(0));
        ArrayList<Type> paramTypes = new ArrayList<>();
        for (int i = 1; i < ctx.type().size(); i++)
            paramTypes.add(visit(ctx.type(i)));
        Func func = new Func(returnType, paramTypes);

        if (funcDeclared.containsKey(name) && !funcDeclared.get(name).equals(func))
            reportError("Redeclaring a function with diffrent params", ctx);

        funcDeclared.put(name, func);

        return new NoType();
    }

    /**
     * 参数传递规范: 所有参数逆序压栈. 那么, 第 k (>=0) 个参数相对子函数 fp 的偏移量是 4*k. 考虑函数访问第 l (>=0) 个局部变量的方式是 -12-4*l, 可以得知对应的偏移量是 l = -3-k
     *
     */
    @Override
    public Type visitFuncDef(FuncDefContext ctx)
    {
        currentFunc = ctx.IDENT(0).getText();
        if (globDeclared.containsKey(currentFunc)) reportError("Function name duplicated with global var", ctx);
        if (currentFunc.equals("main")) containsMain = true;

        if (funcDefined.containsKey(currentFunc)) reportError("Re-Defining function", ctx);

        Type returnType = visit(ctx.type(0));
        ArrayList<Type> paramTypes = new ArrayList<>();
        for (int i = 1; i < ctx.type().size(); i++)
            paramTypes.add(visit(ctx.type(i)));
        Func func = new Func(returnType, paramTypes);

        if (funcDeclared.containsKey(currentFunc) && !funcDeclared.get(currentFunc).equals(func))
            reportError("Function definition doesn't match declaration", ctx);

        funcDefined.put(currentFunc, func);
        funcDeclared.put(currentFunc, func);

        ir.append("func " + currentFunc + '\n');

        localCntr = 0;
        frameCntr = 0;
        startScope();

        for (int i = 1; i < ctx.IDENT().size(); i++)
        {
            String name = ctx.IDENT(i).getText();
            if (symbolTable.peek().containsKey(name)) reportError("Function parameter name duplicated", ctx);
            symbolTable.peek().put(name, new Symbol(name, -2 - i, paramTypes.get(i - 1).valueCast(ValueCat.LVALUE)));
        }

        // visit(ctx.compoundStatement());
        for (var line : ctx.blockItem())
            visit(line);

        endScope();
        this.funcFrameSize.put(currentFunc, frameCntr);

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
        Type returnType = castToRValue(visit(ctx.expression()), ctx);
        Type expectedType = funcDefined.get(currentFunc).returnType;
        if (!expectedType.equals(returnType))
            reportError("Return type " + returnType + " is inconsitent with expected return type " + expectedType, ctx);

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

        typeCheck(visit(ctx.expression()), IntType.class, ctx);

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

        if (ctx.declearation() != null) visit(ctx.declearation());
        if (pre != null)
        {
            visit(pre);
            ir.append("\tpop\n");
        }

        ir.append("\tlabel ").append(beginLabel).append('\n');

        if (cond != null)
        {
            typeCheck(visit(cond), IntType.class, ctx);
            ir.append("\tbeqz ").append(breakLabel).append('\n');
        }

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

        typeCheck(visit(ctx.expression()), IntType.class, ctx);

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

        typeCheck(visit(ctx.expression()), IntType.class, ctx);
        ir.append("\tbnez ").append(beginLabel).append('\n');

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
    public Type visitStmtConti(StmtContiContext ctx)
    {
        if (loopStack.isEmpty()) reportError("Using continue outside a loop.", ctx);
        ir.append("\tbr .continue_loop").append(loopStack.peek()).append('\n');
        return new NoType();
    }

    @Override
    public Type visitDeclLoNaive(DeclLoNaiveContext ctx)
    {
        String name = ctx.IDENT().getText();
        if (symbolTable.peek().get(name) != null) reportError("Re-Declearing an existing variable", ctx);

        Type type = visit(ctx.type());

        symbolTable.peek().put(name, new Symbol(name, localCntr, type.valueCast(ValueCat.LVALUE)));

        declLocalVar();

        if (ctx.expression() != null)
        {
            Type exprType = castToRValue(visit(ctx.expression()), ctx);
            if (!exprType.equals(type))
                reportError("Initialize value of type " + exprType + " to some variable of type " + type, ctx);
            ir.append("\tframeaddr ").append(localCntr - 1).append('\n');
            ir.append("\tstore\n");
            ir.append("\tpop\n");
        }

        return new NoType();
    }

    @Override
    public Type visitDeclLoArray(DeclLoArrayContext ctx)
    {
        // TODO + visitDeclLoArray
        String name = ctx.IDENT().getText();
        if (symbolTable.peek().containsKey(name)) reportError("Re-Declearing an existing variable", ctx);

        Deque<Type> types = new ArrayDeque<>();
        types.add(visit(ctx.type()).valueCast(ValueCat.LVALUE));
        for (int i = ctx.INTEGER().size() - 1; i >= 0; i--)
        {
            int x = Integer.parseInt(ctx.INTEGER(i).getText());
            if (x == 0) reportError("the dimension of array cannot be 0", ctx);
            types.addFirst(new ArrayType(types.getFirst(), x));
        }

        ArrayType type = (ArrayType) types.getFirst();

        declLocalVar(type.getSize() / 4);
        symbolTable.peek().put(name, new Symbol(name, localCntr - 1, type));

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
            // NOTE: 以下两行顺序不可交换. 关乎于地址和表达式的值在 IR 计算栈中的顺序.
            Type exprType = castToRValue(visit(ctx.expression()), ctx);
            Type unaryType = typeCheck(visit(ctx.unary()), Type.class, ValueCat.LVALUE, ctx);
            if (!exprType.equals(unaryType.valueCast(ValueCat.RVALUE)))
                reportError("Assign value of type " + exprType + " to some variable of type " + unaryType, ctx);

            ir.append("\tstore\n");

            return unaryType;
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

            typeCheck(visit(ctx.exprOr()), IntType.class, ctx);
            ir.append("\tbeqz ").append(elseLable).append('\n');
            Type thenType = castToRValue(visit(ctx.expression()), ctx);
            ir.append("\tbr ").append(endLabel).append('\n');
            ir.append("\tlabel ").append(elseLable).append('\n');
            Type elseType = castToRValue(visit(ctx.exprTernary()), ctx);
            ir.append("\tlabel ").append(endLabel).append('\n');

            if (!thenType.equals(elseType)) reportError("Different types of branches of a ternary", ctx);
            return thenType;
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
            typeCheck(visit(ctx.getChild(0)), IntType.class, ctx);
            typeCheck(visit(ctx.getChild(2)), IntType.class, ctx);
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
            typeCheck(visit(ctx.getChild(0)), IntType.class, ctx);
            typeCheck(visit(ctx.getChild(2)), IntType.class, ctx);
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
            Type leftType = castToRValue(visit(ctx.getChild(0)), ctx);
            Type rightType = castToRValue(visit(ctx.getChild(2)), ctx);
            if (!leftType.equals(rightType))
                reportError("Diffrent types on two sides of " + ctx.getChild(1).getText(), ctx);
            if (leftType instanceof ArrayType) reportError("ArrayType cannot be part of equational opr", ctx);

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
            typeCheck(visit(ctx.getChild(0)), IntType.class, ctx);
            typeCheck(visit(ctx.getChild(2)), IntType.class, ctx);
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
            Type left = castToRValue(visit(ctx.getChild(0)), ctx);
            Type right = castToRValue(visit(ctx.getChild(2)), ctx);
            switch (ctx.getChild(1).getText())
            {
                case "+" :
                {
                    if (left instanceof IntType && right instanceof IntType)
                    {
                        ir.append("\tadd\n");
                        return new IntType();
                    }
                    else if (left instanceof IntType && right instanceof PointerType)
                    {
                        ir.append("\taddip\n");
                        return right;
                    }
                    else if (left instanceof PointerType && right instanceof IntType)
                    {
                        ir.append("\taddpi\n");
                        return left;
                    }
                    else
                    {
                        reportError("Adding a pointer to a pointer", ctx);
                    }
                    break;
                }
                case "-" :
                {
                    if (left instanceof IntType && right instanceof IntType)
                    {
                        ir.append("\tsub\n");
                        return new IntType();
                    }
                    else if (left instanceof PointerType && right instanceof IntType)
                    {
                        ir.append("\tsubpi\n");
                        return left;
                    }
                    else if (left instanceof PointerType && right.equals(left))
                    {
                        ir.append("\tsubpp\n");
                        return new IntType();
                    }
                    else
                    {
                        reportError("Subing a pointer to a pointer", ctx);
                    }
                    break;
                }
                default :
                    assert (false);
                    break;
            }
            return new NoType();
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
            typeCheck(visit(ctx.getChild(0)), IntType.class, ctx);
            typeCheck(visit(ctx.getChild(2)), IntType.class, ctx);
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
    public Type visitUnaryPostfix(UnaryPostfixContext ctx)
    {
        return visit(ctx.postfix());
    }

    @Override
    public Type visitUnaryOp(UnaryOpContext ctx)
    {
        typeCheck(visit(ctx.unary()), IntType.class, ctx);

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
    public Type visitUnaryAddr(UnaryAddrContext ctx)
    {
        Type type = visit(ctx.unary());

        switch (ctx.getChild(0).getText())
        {
            case "*" :
                return castToRValue(type, ctx).deref();
            case "&" :
                return type.ref();
            default :
                assert (false);
                return new NoType();
        }
    }

    @Override
    public Type visitUnaryCast(UnaryCastContext ctx)
    {
        Type srcType = visit(ctx.unary());
        Type dstType = visit(ctx.type());
        return dstType.valueCast(srcType.valueCat);
    }

    @Override
    public Type visitPfixPrim(PfixPrimContext ctx)
    {
        return visit(ctx.primary());
    }

    @Override
    public Type visitPfixFCall(PfixFCallContext ctx)
    {
        String name = ctx.IDENT().getText();
        Func func = funcDeclared.get(name);
        if (func == null) reportError("Calling un-declared function", ctx);
        if (func.paramTypes.size() != ctx.expression().size())
            reportError("Number of arguments doesn't match with param number", ctx);

        for (int i = ctx.expression().size() - 1; i >= 0; i--)
        {
            Type t = castToRValue(visit(ctx.expression(i)), ctx);
            if (!t.equals(func.paramTypes.get(i)))
                reportError("The type of argument " + i + " is different from the type of parameter " + i
                        + " of function " + name, ctx);
        }

        ir.append("\tcall ").append(name).append(' ').append(func.paramTypes.size()).append('\n');

        // NOTE: The caller shouldn't pop the arguments.
        // this work is handled by IR-ASM generater.

        return func.returnType;
    }

    @Override
    public Type visitPfixSubscri(PfixSubscriContext ctx)
    {
        // TODO + visitPfixSubscri
        Type postfixType = castToRValue(visit(ctx.postfix()), ctx);
        typeCheck(visit(ctx.expression()), IntType.class, ValueCat.RVALUE, ctx);
        if (postfixType instanceof PointerType)
        {
            // sb.append("# subscript applied to a pointer\n").append("\tslli t1, t1, 2\n").append("\tadd t0, t0,
            // t1\n");
            // push("t0");
            ir.append("\taddpi\n");
            return postfixType.deref();
        }
        else if (postfixType instanceof ArrayType)
        {
            Type baseType = ((ArrayType) postfixType).baseType;
            // sb.append("# subscript applied to an array\n").append("\tli t2, " + baseType.getSize() + "\n")
            // .append("\tmul t1, t1, t2\n").append("\tadd t0, t0, t1\n");
            // push("t0");
            ir.append("\tpush ").append(baseType.getSize() / 4).append('\n');
            ir.append("\tmul\n");
            ir.append("\taddpi\n");
            return baseType;
        }
        else
        {
            reportError("the subscript operator could only be applied to a pointer or an array", ctx);
            return new NoType();
        }
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

        if (symbol != null)
        {
            ir.append("\tframeaddr ").append(symbol.offset).append('\n');
            return symbol.type;
        }
        else if (globDeclared.containsKey(name))
        {
            ir.append("\tglobaladdr ").append(name).append('\n');
            return globDeclared.get(name);
        }
        else
        {
            reportError("Using an undefined variable", ctx);
            return new NoType();
        }
    }

    @Override
    public Type visitType(TypeContext ctx)
    {
        int refDepth = ctx.getChildCount() - 1;
        if (refDepth == 0)
            return new IntType();
        else
            return new PointerType(refDepth);
    }

    // NOTE: Visitors Ends Here

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

    private void declLocalVar()
    {
        // localCntr++;
        // if (localCntr > frameCntr) frameCntr = localCntr;
        declLocalVar(1);
    }

    private void declLocalVar(int num)
    {
        localCntr += num;
        if (localCntr > frameCntr) frameCntr = localCntr;
    }

    private Type typeCheck(final Type actual, Class<? extends Type> expected, ValueCat needed, ParserRuleContext ctx)
    {
        if (!expected.isAssignableFrom(actual.getClass()))
            reportError("Type " + actual + " appears, but " + expected.getName() + " is expected", ctx);
        if (needed == ValueCat.LVALUE && actual.valueCat == ValueCat.RVALUE)
            reportError("An L-value is needed here", ctx);
        if (needed == ValueCat.RVALUE && actual.valueCat == ValueCat.LVALUE)
        {
            ir.append("\tload\n");
            return actual.valueCast(ValueCat.RVALUE);
        }
        return actual.valueCast(needed);
    }

    private Type typeCheck(final Type actual, Class<? extends Type> expected, ParserRuleContext ctx)
    {
        return typeCheck(actual, expected, ValueCat.RVALUE, ctx);
    }

    private Type castToRValue(final Type actual, ParserRuleContext ctx)
    {
        return typeCheck(actual, Type.class, ValueCat.RVALUE, ctx);
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
        this.funcDeclared = new HashMap<>();
        this.funcDefined = new HashMap<>();
        this.funcFrameSize = new HashMap<>();
        this.globDeclared = new HashMap<>();
        // this.globDefined = new HashMap<>();
        // this.globDefVal = new HashMap<>();
    }
}
