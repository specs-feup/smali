package pt.up.fe.specs.alpakka.ast.expr.literal;

import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.alpakka.ast.SmaliNode;
import pt.up.fe.specs.alpakka.ast.expr.literal.typeDescriptor.TypeDescriptor;

public class MethodPrototype extends Literal {

    public MethodPrototype(DataStore data, Collection<? extends SmaliNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var returnType = getReturnType();
        var parameters = getParameters();

        var builder = new StringBuilder();
        builder.append("(");
        parameters.forEach(p -> builder.append(p.getCode()));
        builder.append(")");

        builder.append(returnType.getCode());

        return builder.toString();
    }

    public List<TypeDescriptor> getParameters() {
        return (List<TypeDescriptor>) get(SmaliNode.ATTRIBUTES).get("parameters");
    }

    public TypeDescriptor getReturnType() {
        return (TypeDescriptor) get(SmaliNode.ATTRIBUTES).get("returnType");
    }

}
