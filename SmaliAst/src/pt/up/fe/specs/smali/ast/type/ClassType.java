package pt.up.fe.specs.smali.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.smali.ast.SmaliNode;

public class ClassType extends Type {

	public static final DataKey<String> CLASS_NAME = KeyFactory.string("className");
	public static final DataKey<String> PACKAGE_NAME = KeyFactory.string("packageName");

	public ClassType(DataStore data, Collection<? extends SmaliNode> children) {
		super(data, children);
	}

	public String getPackageName() {
		return get(PACKAGE_NAME);
	}

	public String getClassName() {
		return get(CLASS_NAME);
	}

	@Override
	public String getCode() {
		return "L" + get(PACKAGE_NAME) + "/" + get(CLASS_NAME) + ";";
	}
}
