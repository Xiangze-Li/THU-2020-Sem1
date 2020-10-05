package minidecaf;

/**
 * Symbol
 */
public class Symbol
{

    public final String name;
    public final int offset;
    public final Type type;

    public Symbol(final String name, final int offset, final Type type)
    {
        this.name = name;
        this.offset = offset;
        this.type = type;
    }

    @Override
    public String toString()
    {
        return name + "@" + type + ":" + offset;
    }
}
