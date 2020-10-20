package minidecaf;

import java.util.List;

public class Func
{
    public final Type returnType;
    public final List<Type> paramTypes;

    public Func(final Type returnType, final List<Type> paramTypes)
    {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    @Override
    public boolean equals(final Object rObject)
    {
        if (!(rObject instanceof Func)) return false;

        var r = (Func) rObject;

        if (!r.returnType.equals(this.returnType)) return false;
        if (r.paramTypes.size() != this.paramTypes.size()) return false;
        for (int i = 0; i < paramTypes.size(); i++)
        {
            if (!r.paramTypes.get(i).equals(paramTypes.get(i))) return false;
        }
        return true;
    }
}
