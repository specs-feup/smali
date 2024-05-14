package pt.up.fe.specs.smali.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.smali.ast.expr.literal.MethodPrototype;
import pt.up.fe.specs.smali.ast.stmt.AnnotationDirective;
import pt.up.fe.specs.smali.ast.stmt.CatchDirective;
import pt.up.fe.specs.smali.ast.stmt.Label;
import pt.up.fe.specs.smali.ast.stmt.ParameterDirective;
import pt.up.fe.specs.smali.ast.stmt.RegistersDirective;

public class MethodNode extends SmaliNode {

    public static final DataKey<Map<String, Object>> ATTRIBUTES = KeyFactory.generic("attributes",
            HashMap::new);

    public MethodNode(DataStore data, Collection<? extends SmaliNode> children) {
        super(data, reorderMethodItems(children));
    }

    private static List<SmaliNode> reorderMethodItems(Collection<? extends SmaliNode> children) {
        List<SmaliNode> reorderedChildren = new ArrayList<>();

        children.stream()
                .filter(c -> c instanceof AnnotationDirective)
                .forEach(reorderedChildren::add);

        children.stream()
                .filter(c -> c instanceof ParameterDirective)
                .forEach(reorderedChildren::add);

        children.stream()
                .filter(c -> !(c instanceof AnnotationDirective || c instanceof ParameterDirective))
                .forEach(reorderedChildren::add);

        return reorderedChildren;
    }

    @Override
    public String getCode() {
        var attributes = get(ATTRIBUTES);
        var name = (String) attributes.get("name");
        var prototype = (MethodPrototype) attributes.get("prototype");
        var accessList = (ArrayList<Modifier>) attributes.get("accessOrRestrictionList");
        var registersDirective = (RegistersDirective) attributes.get("registersOrLocals");

        var sb = new StringBuilder();
        sb.append(".method ");
        accessList.forEach(a -> sb.append(a.getLabel()).append(" "));
        sb.append(name);
        sb.append(prototype.getCode());
        sb.append("\n");

        if (registersDirective != null) {
            sb.append(indentCode(registersDirective.getCode())).append("\n");
        }

        var childrenExceptCatch = getChildren().stream()
                .filter(c -> !(c instanceof CatchDirective))
                .toList();

        for (var child : childrenExceptCatch) {
            sb.append("\n").append(indentCode(child.getCode()));
            if (child instanceof Label label) {
                var catchDirectives = getChildren().stream()
                        .filter(c -> c instanceof CatchDirective)
                        .map(c -> (CatchDirective) c)
                        .filter(c -> c.getTryEndLabelRef().getName().equals(label.getLabel()))
                        .toList();

                for (var catchDir : catchDirectives) {
                    sb.append("\n").append(indentCode(catchDir.getCode()));
                }
                if (!catchDirectives.isEmpty()) {
                    sb.append("\n");
                }

            } else {
                sb.append("\n");
            }
        }

        sb.append(".end method\n");

        return sb.toString();
    }

    public String getMethodReferenceName() {
        var sb = new StringBuilder();
        var parentClassDescriptor = ((ClassNode) getParent()).getClassDescriptor();

        if (parentClassDescriptor != null) {
            sb.append(parentClassDescriptor.getCode()).append("->");
        }

        sb.append(get(ATTRIBUTES).get("name"));

        sb.append(((MethodPrototype) get(ATTRIBUTES).get("prototype")).getCode());

        return sb.toString();
    }

}
