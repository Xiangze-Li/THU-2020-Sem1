package minidecaf;

public abstract class Type
{
    enum ValueCat
    {
        LVALUE, RVALUE
    }

    protected final String name;

    public final ValueCat valueCat;

    private Type(final String name)
    {
        this.name = name;
        this.valueCat = ValueCat.RVALUE;
    }

    private Type(final String name, final ValueCat valueCat)
    {
        this.name = name;
        this.valueCat = valueCat;
    }

    @Override
    public String toString()
    {
        return name + '(' + valueCat + ')';
    }

    public abstract boolean equals(Type type); // 判断两个 Type 是否相等

    public abstract int getSize();

    public abstract Type valueCast(ValueCat target);

    public abstract Type ref();

    public abstract Type deref();

    /**
     * 用于语句、声明等没有类型的分析树节点
     */
    public static class NoType extends Type
    {
        public NoType()
        {
            super("NoType");
        }

        @Override
        public boolean equals(Type type)
        {
            return type instanceof NoType;
        }

        @Override
        public int getSize()
        {
            throw new UnsupportedOperationException("Error: trying getting the size of " + this.name);
        }

        @Override
        public Type valueCast(ValueCat target)
        {
            return this;
        }

        @Override
        public Type ref()
        {
            throw new UnsupportedOperationException("Error: trying refrencing a " + this.name);
        }
        @Override
        public Type deref()
        {
            throw new UnsupportedOperationException("Error: trying de-refrencing a " + this.name);
        }
    }

    /**
     * 整型
     */
    public static class IntType extends Type
    {
        public Integer initVal;

        public IntType()
        {
            super("IntType");
            this.initVal = null;
        }

        public IntType(final ValueCat valueCat)
        {
            super("IntType", valueCat);
        }

        public IntType(final int initVal)
        {
            super("IntType");
            this.initVal = initVal;
        }

        @Override
        public boolean equals(Type type)
        {
            return type instanceof IntType;
        }

        @Override
        public int getSize()
        {
            return 4;
        }

        @Override
        public Type valueCast(ValueCat target)
        {
            var t = new IntType(target);
            t.initVal = this.initVal;
            return t;
        }

        @Override
        public Type ref()
        {
            if (this.valueCat == ValueCat.LVALUE)
                return new PointerType(1);
            else
                throw new UnsupportedOperationException("Error: trying refrencing a R-value of " + this.name);
        }
        @Override
        public Type deref()
        {
            throw new UnsupportedOperationException("Error: trying de-refrencing a " + this.name);
        }
    }

    public static class PointerType extends Type
    {
        public final int refDepth;

        public PointerType(int refDepth)
        {
            super("PointerType<" + refDepth + '>');
            this.refDepth = refDepth;
        }

        public PointerType(int refDepth, ValueCat valueCat)
        {
            super("PointerType", valueCat);
            this.refDepth = refDepth;
        }

        @Override
        public boolean equals(Type type)
        {
            return type instanceof PointerType && this.refDepth == ((PointerType) type).refDepth;
        }

        @Override
        public int getSize()
        {
            return 4;
        }

        @Override
        public Type valueCast(ValueCat target)
        {
            return new PointerType(refDepth, target);
        }

        @Override
        public Type ref()
        {
            if (this.valueCat == ValueCat.LVALUE)
                return new PointerType(refDepth + 1);
            else
                throw new UnsupportedOperationException("Error: trying refrencing a R-value of " + this.name);
        }
        @Override
        public Type deref()
        {
            if (refDepth>1)
                return new PointerType(refDepth - 1, ValueCat.LVALUE);
            else
                return new IntType(ValueCat.LVALUE);
        }

    }
}
