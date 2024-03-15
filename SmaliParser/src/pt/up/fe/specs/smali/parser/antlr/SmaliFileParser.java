package pt.up.fe.specs.smali.parser.antlr;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;

import pt.up.fe.specs.smali.ast.AccessSpec;
import pt.up.fe.specs.smali.ast.AnnotationVisibility;
import pt.up.fe.specs.smali.ast.HiddenApiRestriction;
import pt.up.fe.specs.smali.ast.LiteralType;
import pt.up.fe.specs.smali.ast.Modifier;
import pt.up.fe.specs.smali.ast.SmaliNode;
import pt.up.fe.specs.smali.ast.context.SmaliContext;
import pt.up.fe.specs.smali.ast.expr.MethodReference;
import pt.up.fe.specs.smali.ast.stmt.LineDirective;
import pt.up.fe.specs.smali.ast.type.ClassType;
import pt.up.fe.specs.smali.ast.type.MethodPrototype;
import pt.up.fe.specs.smali.ast.type.Type;
import pt.up.fe.specs.util.SpecsIo;

public class SmaliFileParser {

    private final smaliFlexLexer lex;
    private final smaliParser parser;
    private final SmaliContext context;

    private final Set<String> notImplemented;

    private final Map<Integer, Function<Tree, SmaliNode>> converters;

    private LineDirective lineDirective = null;

    public SmaliFileParser(File source, SmaliContext context, Integer targetSdkVersion) {
        this.lex = new smaliFlexLexer(new StringReader(SpecsIo.read(source)), targetSdkVersion);
        this.parser = new smaliParser(new CommonTokenStream(this.lex));
        this.context = context;
        this.converters = buildConverters();

        this.notImplemented = new HashSet<>();
    }

    private Map<Integer, Function<Tree, SmaliNode>> buildConverters() {
        var converters = new HashMap<Integer, Function<Tree, SmaliNode>>();

        converters.put(smaliParser.I_CLASS_DEF, this::convertClass);
        converters.put(smaliParser.I_FIELD, this::convertField);
        converters.put(smaliParser.I_METHOD, this::convertMethod);
        converters.put(smaliParser.I_METHOD_PROTOTYPE, this::convertMethodPrototype);
        converters.put(smaliParser.I_CATCH, this::convertCatches);
        converters.put(smaliParser.I_CATCHALL, this::convertCatches);
        converters.put(smaliParser.I_PARAMETER, this::convertParameter);
        converters.put(smaliParser.I_ANNOTATION, this::convertAnnotation);
        converters.put(smaliParser.I_ANNOTATION_ELEMENT, this::convertAnnotationElement);
        converters.put(smaliParser.I_LINE, this::convertLineDirective);
        converters.put(smaliParser.I_LABEL, this::convertLabel);
        converters.put(smaliParser.I_STATEMENT_FORMAT10x, this::convertStatementFormat10x);
        converters.put(smaliParser.I_STATEMENT_FORMAT10t, this::convertStatementFormat10t);
        converters.put(smaliParser.I_STATEMENT_FORMAT11x, this::convertStatementFormat11x);
        converters.put(smaliParser.I_STATEMENT_FORMAT11n, this::convertStatementFormat11n);
        converters.put(smaliParser.I_STATEMENT_FORMAT12x, this::convertStatementFormat12x);
        converters.put(smaliParser.I_STATEMENT_FORMAT20t, this::convertStatementFormat20t);
        converters.put(smaliParser.I_STATEMENT_FORMAT21ih, this::convertStatementFormat21ih);
        converters.put(smaliParser.I_STATEMENT_FORMAT21lh, this::convertStatementFormat21lh);
        converters.put(smaliParser.I_STATEMENT_FORMAT21c_FIELD, this::convertStatementFormat21cField);
        converters.put(smaliParser.I_STATEMENT_FORMAT21c_STRING, this::convertStatementFormat21cString);
        converters.put(smaliParser.I_STATEMENT_FORMAT21c_TYPE, this::convertStatementFormat21cType);
        // converters.put(smaliParser.I_STATEMENT_FORMAT21c_METHOD_HANDLE, this::convertStatementFormat21cMethodHandle);
        converters.put(smaliParser.I_STATEMENT_FORMAT21c_METHOD_TYPE, this::convertStatementFormat21cMethodType);
        converters.put(smaliParser.I_STATEMENT_FORMAT21s, this::convertStatementFormat21s);
        converters.put(smaliParser.I_STATEMENT_FORMAT21t, this::convertStatementFormat21t);
        converters.put(smaliParser.I_STATEMENT_FORMAT22c_FIELD, this::convertStatementFormat22cField);
        converters.put(smaliParser.I_STATEMENT_FORMAT22c_TYPE, this::convertStatementFormat22cType);
        converters.put(smaliParser.I_STATEMENT_FORMAT22b, this::convertStatementFormat22b);
        converters.put(smaliParser.I_STATEMENT_FORMAT22s, this::convertStatementFormat22s);
        converters.put(smaliParser.I_STATEMENT_FORMAT22t, this::convertStatementFormat22t);
        converters.put(smaliParser.I_STATEMENT_FORMAT22x, this::convertStatementFormat22x);
        converters.put(smaliParser.I_STATEMENT_FORMAT23x, this::convertStatementFormat23x);
        converters.put(smaliParser.I_STATEMENT_FORMAT30t, this::convertStatementFormat30t);
        converters.put(smaliParser.I_STATEMENT_FORMAT31c, this::convertStatementFormat31c);
        converters.put(smaliParser.I_STATEMENT_FORMAT31i, this::convertStatementFormat31i);
        converters.put(smaliParser.I_STATEMENT_FORMAT31t, this::convertStatementFormat31t);
        converters.put(smaliParser.I_STATEMENT_FORMAT32x, this::convertStatementFormat32x);
        // converters.put(smaliParser.I_STATEMENT_FORMAT35c_CALL_SITE, this::convertStatementFormat35cCallSite);
        converters.put(smaliParser.I_STATEMENT_FORMAT35c_METHOD, this::convertStatementFormat35cMethod);
        converters.put(smaliParser.I_STATEMENT_FORMAT35c_TYPE, this::convertStatementFormat35cType);
        // converters.put(smaliParser.I_STATEMENT_FORMAT3rc_CALL_SITE, this::convertStatementFormat3rcCallSite);
        converters.put(smaliParser.I_STATEMENT_FORMAT3rc_METHOD, this::convertStatementFormat3rcMethod);
        converters.put(smaliParser.I_STATEMENT_FORMAT3rc_TYPE, this::convertStatementFormat3rcType);
        converters.put(smaliParser.I_STATEMENT_FORMAT45cc_METHOD, this::convertStatementFormat45ccMethod);
        converters.put(smaliParser.I_STATEMENT_FORMAT4rcc_METHOD, this::convertStatementFormat4rccMethod);
        converters.put(smaliParser.I_STATEMENT_FORMAT51l, this::convertStatementFormat51l);
        converters.put(smaliParser.STRING_LITERAL, this::convertLiteral);
        converters.put(smaliParser.INTEGER_LITERAL, this::convertLiteral);
        converters.put(smaliParser.LONG_LITERAL, this::convertLiteral);
        converters.put(smaliParser.BYTE_LITERAL, this::convertLiteral);
        converters.put(smaliParser.BOOL_LITERAL, this::convertLiteral);
        converters.put(smaliParser.NULL_LITERAL, this::convertLiteral);
        converters.put(smaliParser.REGISTER, this::convertRegisterReference);
        converters.put(smaliParser.I_REGISTER_LIST, this::convertRegisterList);
        converters.put(smaliParser.I_REGISTER_RANGE, this::convertRegisterRange);
        converters.put(smaliParser.I_STATEMENT_ARRAY_DATA, this::convertArrayDataDirective);
        converters.put(smaliParser.I_STATEMENT_PACKED_SWITCH, this::convertPackedSwitch);
        converters.put(smaliParser.I_STATEMENT_SPARSE_SWITCH, this::convertSparseSwitch);

        return converters;
    }

    public Optional<SmaliNode> parse() {

        try {
            var root = parser.smali_file().getTree();

            if (parser.getNumberOfSyntaxErrors() > 0) {
                throw new RuntimeException("Syntax errors");
            }

            return Optional.of(convert(root));
        } catch (RecognitionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private SmaliNode convert(Tree node) {

        var type = node.getType();

        var converter = converters.get(type);

        if (converter != null) {
            return converter.apply(node);
        }

        // System.out.println("Not implemented: " + parser.getTokenNames()[type]);

        var kind = parser.getTokenNames()[type];

        var children = new ArrayList<SmaliNode>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        var factory = context.get(SmaliContext.FACTORY);

        return factory.placeholder(kind, children);
    }

    private SmaliNode convertClass(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var accessList = new ArrayList<AccessSpec>();
        var implementsDescriptors = new ArrayList<ClassType>();

        var attributes = new HashMap<String, Object>();
        var children = new ArrayList<SmaliNode>();

        for (int i = 0; i < node.getChildCount(); i++) {
            switch (node.getChild(i).getType()) {
            case smaliParser.CLASS_DESCRIPTOR -> {
                attributes.put("classDescriptor", factory.classType(node.getChild(i).getText()));
            }
            case smaliParser.I_ACCESS_LIST -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    accessList.add(AccessSpec.getFromLabel(node.getChild(i).getChild(j).getText()));
                }
            }
            case smaliParser.I_SUPER -> {
                attributes.put("superClassDescriptor", factory.classType(node.getChild(i).getChild(0).getText()));
            }
            case smaliParser.I_IMPLEMENTS -> {
                implementsDescriptors.add(factory.classType(node.getChild(i).getChild(0).getText()));
            }
            case smaliParser.I_SOURCE -> {
                attributes.put("source", convert(node.getChild(i).getChild(0)));
            }
            case smaliParser.I_METHODS, smaliParser.I_FIELDS -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    children.add(convert(node.getChild(i).getChild(j)));
                }
            }
            case smaliParser.I_ANNOTATIONS -> {
                if (node.getChild(i).getChildCount() > 0) {
                    todo(parser.getTokenNames()[node.getChild(i).getType()]);
                }
            }
            }
        }

        attributes.put("accessList", accessList);
        attributes.put("implementsDescriptors", implementsDescriptors);

        return factory.classNode(attributes, children);
    }

    private void todo(String todo) {
        if (!notImplemented.contains(todo)) {
            notImplemented.add(todo);
            System.out.println("TODO: " + todo);
        }
    }

    private SmaliNode convertLineDirective(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var attributes = new HashMap<String, Object>();

        attributes.put("line", convert(node.getChild(0)));

        return factory.lineDirective(attributes);
    }

    private SmaliNode convertLabel(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var attributes = getStatementAttributes(null);

        attributes.put("label", node.getChild(0).getText());

        return factory.label(attributes);
    }

    private MethodPrototype convertMethodPrototype(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var prototypeAttributes = new HashMap<String, Object>();
        var parameters = new ArrayList<Type>();
        for (int j = 0; j < node.getChildCount(); j++) {
            if (node.getChild(j).getType() == smaliParser.I_METHOD_RETURN_TYPE) {
                // Type descriptor
                if (node.getChild(j).getChild(0).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
                    prototypeAttributes.put("returnType", factory.arrayType(node.getChild(j).getChild(1).getText()));
                } else {
                    prototypeAttributes.put("returnType", factory.type(node.getChild(j).getChild(0).getText()));
                }
            } else if (node.getChild(j).getType() == smaliParser.PARAM_LIST_OR_ID_PRIMITIVE_TYPE) {
                todo(parser.getTokenNames()[node.getChild(j).getType()]);
            } else {
                // Non void type descriptor
                if (node.getChild(j).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
                    j++;
                    parameters.add(factory.arrayType(node.getChild(j).getText()));
                } else {
                    parameters.add(factory.nonVoidType(node.getChild(j).getText()));
                }
            }
        }

        prototypeAttributes.put("parameters", parameters);

        return factory.methodPrototype(prototypeAttributes);
    }

    private Modifier getAccessSpecOrHiddenApiRestriction(Tree node) {
        if (node.getType() == smaliParser.ACCESS_SPEC) {
            return AccessSpec.getFromLabel(node.getText());
        } else if (node.getType() == smaliParser.HIDDENAPI_RESTRICTION) {
            HiddenApiRestriction.getFromLabel(node.getText());
        }

        return null;
    }

    private SmaliNode convertMethod(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var methodAttributes = new HashMap<String, Object>();
        var accessOrRestrictionList = new ArrayList<Modifier>();

        var children = new ArrayList<SmaliNode>();

        for (int i = 0; i < node.getChildCount(); i++) {
            switch (node.getChild(i).getType()) {
            case smaliParser.SIMPLE_NAME -> {
                methodAttributes.put("name", node.getChild(i).getText());
            }
            case smaliParser.I_METHOD_PROTOTYPE -> {
                methodAttributes.put("prototype", convert(node.getChild(i)));
            }
            case smaliParser.I_ACCESS_OR_RESTRICTION_LIST -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    accessOrRestrictionList.add(getAccessSpecOrHiddenApiRestriction(node.getChild(i).getChild(j)));
                }
            }
            case smaliParser.I_REGISTERS, smaliParser.I_LOCALS -> {
                var directiveAttributes = new HashMap<String, Object>();
                directiveAttributes.put("type", parser.getTokenNames()[node.getChild(i).getType()]);
                directiveAttributes.put("value", convert(node.getChild(i).getChild(0)));

                methodAttributes.put("registersOrLocals", factory.registersDirective(directiveAttributes));
            }
            case smaliParser.I_ORDERED_METHOD_ITEMS -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    if (node.getChild(i).getChild(j).getType() == smaliParser.I_LINE) {
                        lineDirective = (LineDirective) convert(node.getChild(i).getChild(j));
                    } else {
                        children.add(convert(node.getChild(i).getChild(j)));
                        lineDirective = null;
                    }
                }
            }
            case smaliParser.I_CATCHES, smaliParser.I_PARAMETERS, smaliParser.I_ANNOTATIONS -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    children.add(convert(node.getChild(i).getChild(j)));
                }
            }
            }
        }

        methodAttributes.put("accessOrRestrictionList", accessOrRestrictionList);

        return factory.methodNode(methodAttributes, children);
    }

    private SmaliNode convertCatches(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);

        var i = 0;

        if (node.getChild(i).getType() != smaliParser.SIMPLE_NAME) {
            // Non void type descriptor
            if (node.getChild(i).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
                i++;
                attributes.put("nonVoidTypeDescriptor", factory.arrayType(node.getChild(i).getText()));
            } else {
                attributes.put("nonVoidTypeDescriptor", factory.nonVoidType(node.getChild(i).getText()));
            }
            i++;
        }

        var labelRefAttributes = new HashMap<String, Object>();
        labelRefAttributes.put("label", node.getChild(i).getText());
        attributes.put("from", factory.labelRef(labelRefAttributes));
        i++;

        labelRefAttributes = new HashMap<String, Object>();
        labelRefAttributes.put("label", node.getChild(i).getText());
        attributes.put("to", factory.labelRef(labelRefAttributes));
        i++;

        labelRefAttributes = new HashMap<String, Object>();
        labelRefAttributes.put("label", node.getChild(i).getText());
        attributes.put("label", factory.labelRef(labelRefAttributes));

        return factory.catchDirective(attributes);
    }

    private SmaliNode convertParameter(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);
        var children = new ArrayList<SmaliNode>();

        var i = 0;

        attributes.put("register", convert(node.getChild(i)));
        i++;

        if (node.getChild(i).getType() == smaliParser.STRING_LITERAL) {
            attributes.put("string", convert(node.getChild(i)));
            i++;
        }

        for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
            children.add(convert(node.getChild(i).getChild(j)));
        }

        return factory.parameterDirective(attributes, children);
    }

    private SmaliNode convertAnnotation(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);
        var children = new ArrayList<SmaliNode>();

        attributes.put("visibility", AnnotationVisibility.getFromString(node.getChild(0).getText()));

        var subannotation = node.getChild(1);
        attributes.put("classDescriptor", factory.classType(subannotation.getChild(0).getText()));

        for (int i = 1; i < subannotation.getChildCount(); i++) {
            children.add(convert(subannotation.getChild(i)));
        }

        return factory.annotationDirective(attributes, children);
    }

    private SmaliNode convertAnnotationElement(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = new HashMap<String, Object>();

        attributes.put("name", node.getChild(0).getText());
        attributes.put("value", convert(node.getChild(1)));

        return factory.annotationElement(attributes);
    }

    private SmaliNode convertField(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var fieldAttributes = new HashMap<String, Object>();
        var accessOrRestrictionList = new ArrayList<Modifier>();

        var children = new ArrayList<SmaliNode>();

        for (int i = 0; i < node.getChildCount(); i++) {
            switch (node.getChild(i).getType()) {
            case smaliParser.I_ACCESS_OR_RESTRICTION_LIST -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    accessOrRestrictionList.add(getAccessSpecOrHiddenApiRestriction(node.getChild(i).getChild(j)));
                }
            }
            case smaliParser.SIMPLE_NAME -> {
                fieldAttributes.put("memberName", node.getChild(i).getText());
            }
            case smaliParser.I_FIELD_TYPE -> {
                // Non void type descriptor
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    if (node.getChild(i).getChild(j).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
                        j++;
                        fieldAttributes.put("fieldType", factory.arrayType(node.getChild(i).getChild(j).getText()));
                    } else {
                        fieldAttributes.put("fieldType", factory.nonVoidType(node.getChild(i).getChild(j).getText()));
                    }
                }
            }
            case smaliParser.I_FIELD_INITIAL_VALUE -> {
                // Literal
                children.add(convert(node.getChild(i).getChild(0)));
            }
            case smaliParser.I_ANNOTATIONS -> {
                for (int j = 0; j < node.getChild(i).getChildCount(); j++) {
                    children.add(convert(node.getChild(i).getChild(j)));
                }
            }
            }
        }

        fieldAttributes.put("accessOrRestrictionList", accessOrRestrictionList);

        return factory.fieldNode(fieldAttributes, children);
    }

    private SmaliNode convertLiteral(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var literalAttributes = new HashMap<String, Object>();
        literalAttributes.put("value", node.getText());

        var literalExpr = factory.literalRef(literalAttributes);
        
        return literalExpr;
    }

    private SmaliNode convertRegisterReference(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        return factory.register(node.getText());
    }

    private SmaliNode convertRegisterList(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var children = new ArrayList<SmaliNode>();

        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.registerList(children);
    }

    private SmaliNode convertRegisterRange(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var children = new ArrayList<SmaliNode>();

        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.registerRange(children);
    }

    private List<SmaliNode> convertFieldReferenceStatement(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var children = new ArrayList<SmaliNode>();

        var fieldReferenceAttributes = new HashMap<String, Object>();

        var i = 1;

        while (i < node.getChildCount() && node.getChild(i).getType() == smaliParser.REGISTER) {
            children.add(convert(node.getChild(i)));
            i++;
        }

        if (node.getChild(i).getType() != smaliParser.SIMPLE_NAME) {
            // Reference type descriptor
            if (node.getChild(i).getType() == smaliParser.CLASS_DESCRIPTOR) {
                fieldReferenceAttributes.put("referenceTypeDescriptor", factory.classType(node.getChild(i).getText()));
            } else {
                i++;
                fieldReferenceAttributes.put("referenceTypeDescriptor", factory.arrayType(node.getChild(i).getText()));
            }
            i++;
        }

        fieldReferenceAttributes.put("memberName", node.getChild(i).getText());
        i++;

        // Non void type descriptor
        if (node.getChild(i).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
            i++;
            fieldReferenceAttributes.put("nonVoidTypeDescriptor", factory.arrayType(node.getChild(i).getText()));
        } else {
            fieldReferenceAttributes.put("nonVoidTypeDescriptor", factory.nonVoidType(node.getChild(i).getText()));
        }

        children.add(factory.fieldReference(fieldReferenceAttributes));

        return children;
    }

    private List<SmaliNode> convertTypeReferenceStatement(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var children = new ArrayList<SmaliNode>();

        var i = 1;

        while (i < node.getChildCount() && (node.getChild(i).getType() == smaliParser.REGISTER
                || node.getChild(i).getType() == smaliParser.I_REGISTER_LIST
                || node.getChild(i).getType() == smaliParser.I_REGISTER_RANGE)) {
            children.add(convert(node.getChild(i)));
            i++;
        }

        // Non void type descriptor
        if (node.getChild(i).getType() == smaliParser.ARRAY_TYPE_PREFIX) {
            i++;
            children.add(factory.arrayType(node.getChild(i).getText()));
        } else {
            children.add(factory.nonVoidType(node.getChild(i).getText()));
        }

        return children;
    }

    private List<SmaliNode> convertLabelReferenceStatement(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var children = new ArrayList<SmaliNode>();

        var i = 1;

        while (i < node.getChildCount() && node.getChild(i).getType() != smaliParser.SIMPLE_NAME) {
            children.add(convert(node.getChild(i)));
            i++;
        }

        var labelRefAttributes = new HashMap<String, Object>();
        labelRefAttributes.put("label", node.getChild(i).getText());

        children.add(factory.labelRef(labelRefAttributes));

        return children;
    }

    private HashMap<String, Object> getStatementAttributes(String instruction) {
        var attributes = new HashMap<String, Object>();
        attributes.put("instruction", instruction);
        attributes.put("lineDirective", lineDirective);
        return attributes;
    }

    private SmaliNode convertStatementFormat10x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var attributes = getStatementAttributes(node.getChild(0).getText());

        return factory.instructionFormat10x(attributes);
    }

    private SmaliNode convertStatementFormat10t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat10t(attributes, children);
    }

    private SmaliNode convertStatementFormat11x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat11x(attributes, children);
    }

    private SmaliNode convertStatementFormat11n(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat11n(attributes, children);
    }

    private SmaliNode convertStatementFormat12x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat12x(attributes, children);
    }

    private SmaliNode convertStatementFormat20t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat20t(attributes, children);
    }

    private SmaliNode convertStatementFormat21ih(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat21ih(attributes, children);
    }

    private SmaliNode convertStatementFormat21lh(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat21lh(attributes, children);
    }

    private SmaliNode convertStatementFormat21cField(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());

        var children = convertFieldReferenceStatement(node);

        return factory.instructionFormat21cField(attributes, children);
    }

    private SmaliNode convertStatementFormat21cString(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat21cString(attributes, children);
    }

    private SmaliNode convertStatementFormat21cType(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertTypeReferenceStatement(node);

        return factory.instructionFormat21cType(attributes, children);
    }

    private SmaliNode convertStatementFormat21cMethodType(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat21cMethodType(attributes, children);
    }

    private SmaliNode convertStatementFormat21s(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat21s(attributes, children);
    }

    private SmaliNode convertStatementFormat21t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat21t(attributes, children);
    }

    private SmaliNode convertStatementFormat22cField(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());

        var children = convertFieldReferenceStatement(node);

        return factory.instructionFormat22cField(attributes, children);
    }

    private SmaliNode convertStatementFormat22cType(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertTypeReferenceStatement(node);

        return factory.instructionFormat22cType(attributes, children);
    }

    private SmaliNode convertStatementFormat22b(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat22b(attributes, children);
    }

    private SmaliNode convertStatementFormat22s(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat22s(attributes, children);
    }

    private SmaliNode convertStatementFormat22t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat22t(attributes, children);
    }

    private SmaliNode convertStatementFormat22x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat22x(attributes, children);
    }

    private SmaliNode convertStatementFormat23x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat23x(attributes, children);
    }

    private SmaliNode convertStatementFormat30t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat30t(attributes, children);
    }

    private SmaliNode convertStatementFormat31c(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat31c(attributes, children);
    }

    private SmaliNode convertStatementFormat31i(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat31i(attributes, children);
    }

    private SmaliNode convertStatementFormat31t(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertLabelReferenceStatement(node);

        return factory.instructionFormat31t(attributes, children);
    }

    private SmaliNode convertStatementFormat32x(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat32x(attributes, children);
    }

    private MethodReference convertMethodReferenceInStatement(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);
        var methodReferenceAttributes = new HashMap<String, Object>();

        int i = 2;
        if (node.getChild(i).getType() != smaliParser.SIMPLE_NAME) {
            // Reference type descriptor
            if (node.getChild(i).getType() == smaliParser.CLASS_DESCRIPTOR) {
                methodReferenceAttributes.put("referenceTypeDescriptor", factory.classType(node.getChild(i).getText()));
            } else {
                i++;
                methodReferenceAttributes.put("referenceTypeDescriptor", factory.arrayType(node.getChild(i).getText()));
            }
            i++;
        }

        methodReferenceAttributes.put("memberName", node.getChild(i).getText());
        i++;

        methodReferenceAttributes.put("prototype", convert(node.getChild(i)));

        return factory.methodReference(methodReferenceAttributes);
    }

    private SmaliNode convertStatementFormat35cType(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertTypeReferenceStatement(node);

        return factory.instructionFormat35cType(attributes, children);
    }

    private SmaliNode convertStatementFormat35cMethod(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        children.add(convert(node.getChild(1)));

        children.add(convertMethodReferenceInStatement(node));

        return factory.instructionFormat35cMethod(attributes, children);
    }

    private SmaliNode convertStatementFormat3rcMethod(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        children.add(convert(node.getChild(1)));

        children.add(convertMethodReferenceInStatement(node));

        return factory.instructionFormat3rcMethod(attributes, children);
    }

    private SmaliNode convertStatementFormat3rcType(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = convertTypeReferenceStatement(node);

        return factory.instructionFormat3rcType(attributes, children);
    }

    private SmaliNode convertStatementFormat45ccMethod(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        children.add(convert(node.getChild(1)));

        children.add(convertMethodReferenceInStatement(node));

        children.add(convert(node.getChild(node.getChildCount() - 1)));

        return factory.instructionFormat45ccMethod(attributes, children);
    }

    private SmaliNode convertStatementFormat4rccMethod(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        children.add(convert(node.getChild(1)));

        children.add(convertMethodReferenceInStatement(node));

        children.add(convert(node.getChild(node.getChildCount() - 1)));

        return factory.instructionFormat4rccMethod(attributes, children);
    }

    private SmaliNode convertStatementFormat51l(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(node.getChild(0).getText());
        var children = new ArrayList<SmaliNode>();

        for (int i = 1; i < node.getChildCount(); i++) {
            children.add(convert(node.getChild(i)));
        }

        return factory.instructionFormat51l(attributes, children);
    }

    private SmaliNode convertArrayDataDirective(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);
        var children = new ArrayList<SmaliNode>();

        attributes.put("elementWidth", convert(node.getChild(0).getChild(0)));

        var arrayElements = node.getChild(1);

        for (int i = 0; i < arrayElements.getChildCount(); i++) {
            children.add(convert(arrayElements.getChild(i)));
        }

        return factory.arrayDataDirective(attributes, children);
    }

    private SmaliNode convertPackedSwitch(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);
        var children = new ArrayList<SmaliNode>();

        attributes.put("key", convert(node.getChild(0).getChild(0)));

        var packedSwitchElements = node.getChild(1);

        for (int i = 0; i < packedSwitchElements.getChildCount(); i++) {
            var labelRefAttributes = new HashMap<String, Object>();
            labelRefAttributes.put("label", packedSwitchElements.getChild(i).getText());

            children.add(factory.labelRef(labelRefAttributes));
        }

        return factory.packedSwitchDirective(attributes, children);
    }

    private SmaliNode convertSparseSwitch(Tree node) {
        var factory = context.get(SmaliContext.FACTORY);

        var attributes = getStatementAttributes(null);
        var children = new ArrayList<SmaliNode>();
        var sparseSwitchElements = node.getChild(0);

        for (int i = 0; i < sparseSwitchElements.getChildCount(); i += 2) {
            var elementAttributes = new HashMap<String, Object>();
            elementAttributes.put("key", convert(sparseSwitchElements.getChild(i)));

            var labelRefAttributes = new HashMap<String, Object>();
            labelRefAttributes.put("label", sparseSwitchElements.getChild(i + 1).getText());

            elementAttributes.put("label", factory.labelRef(labelRefAttributes));

            children.add(factory.sparseSwitchElement(elementAttributes));
        }

        return factory.sparseSwitchDirective(attributes, children);
    }

}
