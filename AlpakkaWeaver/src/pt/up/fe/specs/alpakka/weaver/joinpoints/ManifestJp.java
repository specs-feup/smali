package pt.up.fe.specs.alpakka.weaver.joinpoints;

import pt.up.fe.specs.alpakka.ast.Manifest;
import pt.up.fe.specs.alpakka.ast.SmaliNode;
import pt.up.fe.specs.alpakka.weaver.abstracts.joinpoints.AManifest;

public class ManifestJp extends AManifest {

	private final Manifest manifest;

	public ManifestJp(Manifest manifest) {
		this.manifest = manifest;
	}

	@Override
	public SmaliNode getNode() {
		return manifest;
	}

	@Override
	public String getPackageNameImpl() {
		return manifest.getPackageName();
	}

	@Override
	public String[] getActivitiesArrayImpl() {
		return manifest.getActivities().toArray(new String[0]);
	}

	@Override
	public String[] getServicesArrayImpl() {
		return manifest.getServices().toArray(new String[0]);
	}
}
