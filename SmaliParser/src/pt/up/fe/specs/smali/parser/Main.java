package pt.up.fe.specs.smali.parser;

import java.io.File;
import java.util.ArrayList;

import pt.up.fe.specs.smali.parser.antlr.SmaliParser;
import pt.up.fe.specs.util.SpecsIo;

public class Main {

	public static void main(String[] args) {
		var filesList = new ArrayList<File>();
		filesList.add(SpecsIo.resourceCopy("pt/up/fe/specs/smali/HelloWorld.smali"));

		var antlrParser = new SmaliParser(filesList);
		var smaliRoot = antlrParser.parse().orElseThrow();

		System.out.println(smaliRoot.toTree());

		filesList.forEach(File::delete);
	}

}
