package minidecaf;


public abstract class Type
{
    private final String name;

    private Type(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public abstract boolean equals(Type type); // 判断两个 Type 是否相等

    public abstract int getSize();

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
            throw new UnsupportedOperationException("Error: trying getting the size of NoType.");
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
    }
}
