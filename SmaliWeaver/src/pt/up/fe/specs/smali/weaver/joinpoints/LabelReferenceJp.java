package pt.up.fe.specs.smali.weaver.joinpoints;

import pt.up.fe.specs.smali.ast.SmaliNode;
import pt.up.fe.specs.smali.ast.expr.LabelRef;
import pt.up.fe.specs.smali.weaver.SmaliJoinpoints;
import pt.up.fe.specs.smali.weaver.abstracts.joinpoints.ALabel;
import pt.up.fe.specs.smali.weaver.abstracts.joinpoints.ALabelReference;

public class LabelReferenceJp extends ALabelReference {

    private final LabelRef labelReference;

    public LabelReferenceJp(LabelRef labelReference) {
        super(new ExpressionJp(labelReference));
        this.labelReference = labelReference;
    }

    @Override
    public SmaliNode getNode() {
        return this.labelReference;
    }

    @Override
    public ALabel getDeclImpl() {
        return SmaliJoinpoints.create(this.labelReference.getDeclaration(), ALabel.class);
    }
}
