package pt.up.fe.specs.alpakka.weaver.joinpoints;

import pt.up.fe.specs.alpakka.ast.SmaliNode;
import pt.up.fe.specs.alpakka.ast.expr.RegisterReference;
import pt.up.fe.specs.alpakka.weaver.abstracts.joinpoints.ARegisterReference;

public class RegisterReferenceJp extends ARegisterReference {

    private final RegisterReference registerReference;

    public RegisterReferenceJp(RegisterReference registerReference) {
        super(new ExpressionJp(registerReference));
        this.registerReference = registerReference;
    }

    @Override
    public SmaliNode getNode() {
        return this.registerReference;
    }
}
