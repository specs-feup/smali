package pt.up.fe.specs.smali.ast.expr.literal;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.smali.ast.SmaliNode;
import pt.up.fe.specs.smali.ast.expr.Expression;

public abstract class Literal extends Expression {

    public Literal(DataStore data, Collection<? extends SmaliNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var attributes = get(ATTRIBUTES);

        return (String) attributes.get("value");
    }

}
