import { MethodNode, Program, Statement, ReturnStatement, Label, Goto, IfComparison, IfComparisonWithZero, Switch, LabelReference, PackedSwitch, SparseSwitch, SparseSwitchElement, ThrowStatement, Catch, Instruction, MethodReference, } from "../../../../Joinpoints.js";
import UnknownInstructionNode from "./node/instruction/UnknownInstructionNode.js";
import InstructionNode from "./node/instruction/InstructionNode.js";
import StatementNode from "./node/instruction/StatementNode.js";
import ReturnNode from "./node/instruction/ReturnNode.js";
import LabelNode from "./node/instruction/LabelNode.js";
import GotoNode from "./node/instruction/GotoNode.js";
import SwitchNode from "./node/instruction/SwitchNode.js";
import ThrowNode from "./node/instruction/ThrowNode.js";
import IfComparisonNode from "./node/condition/IfComparisonNode.js";
export default class FlowGraphGenerator {
    #$jp;
    #graph;
    #temporaryNodes;
    constructor($jp, graph) {
        this.#$jp = $jp;
        this.#graph = graph;
        this.#temporaryNodes = [];
    }
    build() {
        if (this.#$jp instanceof Program) {
            this.#$jp.classes.forEach((child) => {
                child.methods.forEach((method) => {
                    this.#processFunction(method);
                });
            });
        }
        else if (this.#$jp instanceof MethodNode) {
            this.#processFunction(this.#$jp);
        }
        for (const node of this.#temporaryNodes) {
            node.removeFromFlow();
            node.remove();
        }
        return this.#graph;
    }
    #processJp($jp, context) {
        if (context.preprocessedStatementStack.length > 0) {
            const [head, tail] = context.preprocessedStatementStack.pop();
            return [head, tail];
        }
        if ($jp instanceof Label) {
            return this.#processLabelStmt($jp, context);
        }
        else if ($jp instanceof Switch) {
            return this.#processSwitch($jp, context);
        }
        else if ($jp instanceof IfComparison ||
            $jp instanceof IfComparisonWithZero) {
            return this.#processIf($jp, context);
        }
        else if ($jp instanceof Goto) {
            return this.#processGoto($jp, context);
        }
        else if ($jp instanceof Catch) {
            return this.#processCatchDirective($jp, context);
        }
        else if ($jp instanceof ReturnStatement) {
            return this.#addReturnStmt(new ReturnNode.Builder($jp));
        }
        else if ($jp instanceof ThrowStatement) {
            return this.#addThrowStmt(new ThrowNode.Builder($jp));
        }
        else if ($jp instanceof Statement) {
            return this.#addInstruction(new StatementNode.Builder($jp));
        }
        else {
            throw new Error(`Cannot build graph for joinpoint "${$jp.joinPointType}"`);
        }
    }
    #processFunction($jp) {
        const processedFunction = this.#graph.getFunction($jp.referenceName);
        if (processedFunction !== undefined) {
            return [processedFunction, undefined];
        }
        const context = {
            labels: new Map(),
            preprocessedStatementStack: new Array(),
            tryCatchDirectives: new Array(),
        };
        const body = $jp.children.map((child) => {
            const [head, tail] = this.#processJp(child, context);
            return [head, tail ? [tail] : []];
        });
        const functionTail = [];
        if (body.length === 0) {
            return this.#graph.addFunction($jp, undefined, functionTail);
        }
        for (let i = 0; i < body.length - 1; i++) {
            const [head, tail] = body[i];
            const [nextHead] = body[i + 1];
            if (head instanceof ReturnNode.Class || head instanceof ThrowNode.Class) {
                functionTail.push(head);
            }
            for (const tailNode of tail) {
                tailNode.nextNode = nextHead;
            }
        }
        const [lastHead, lastTail] = body[body.length - 1];
        if (lastTail.length === 0 &&
            (lastHead instanceof ReturnNode.Class ||
                lastHead instanceof ThrowNode.Class)) {
            functionTail.push(lastHead);
        }
        for (const directive of context.tryCatchDirectives) {
            const tryStartLabel = context.labels.get(directive.tryStart.decl.name);
            if (tryStartLabel === undefined) {
                throw new Error("Could not find try start label node");
            }
            const tryEndLabel = context.labels.get(directive.tryEnd.decl.name);
            if (tryEndLabel === undefined) {
                throw new Error("Could not find try end label node");
            }
            const catchLabel = context.labels.get(directive.catch.decl.name);
            let exception = "Ljava/lang/Exception;";
            if (!(directive.code.startsWith(".catchall"))) {
                exception = directive.exception.code;
            }
            const tryEndIndex = body.findIndex(([head]) => head === tryEndLabel);
            const tryStartIndex = body.findIndex(([head]) => head === tryStartLabel);
            for (let i = tryEndIndex - 1; i > tryStartIndex; i--) {
                const [head, tail] = body[i];
                // TODO: Check if exception being thrown is caught by this catch directive
                if (head instanceof ThrowNode.Class) {
                    if (functionTail.includes(head)) {
                        functionTail.splice(functionTail.indexOf(head), 1);
                    }
                    this.#connectArbitraryJump(head, catchLabel);
                }
                else if (head.jp instanceof Instruction &&
                    this.canThrow(head.jp, exception)) {
                    if (tail.length === 0) {
                        continue;
                    }
                    const nextNode = tail[0].nextNode;
                    if (nextNode === undefined) {
                        continue;
                    }
                    body[i] = [
                        this.#graph.addTryCatch(head.jp, nextNode, catchLabel),
                        [],
                    ];
                    const [prevHead, previousTail] = body[i - 1];
                    for (const previousTailNode of previousTail) {
                        previousTailNode.nextNode = body[i][0];
                    }
                    if (prevHead instanceof IfComparisonNode.Class) {
                        // The true node in an if condition will always be a label
                        prevHead.falseNode = body[i][0];
                    }
                    head.removeFromFlow();
                    head.remove();
                }
            }
        }
        const bodyHead = body[0][0];
        // for (const [head] of body) {
        //   if (head.incomers.length === 0 && bodyHead !== head) {
        //     head.removeFromFlow();
        //     head.remove();
        //   }
        // }
        return this.#graph.addFunction($jp, bodyHead, functionTail);
    }
    canThrow($jp, $exception) {
        // return $jp.canThrow;
        if ($jp.opCodeName === "check-cast" &&
            ($exception === "Ljava/lang/ClassCastException;" ||
                $exception === "Ljava/lang/RuntimeException;" ||
                $exception === "Ljava/lang/Exception;")) {
            return true;
        }
        if (($jp.opCodeName.startsWith("rem") || $jp.opCodeName.startsWith("div")) &&
            ($exception === "Ljava/lang/ArithmeticException;" ||
                $exception === "Ljava/lang/RuntimeException;" ||
                $exception === "Ljava/lang/Exception;")) {
            return true;
        }
        // These can use dynamic dispatch, I think
        // Would need further analysis to accurately determine if they can throw
        if ($jp.opCodeName.startsWith("invoke-virtual") ||
            $jp.opCodeName.startsWith("invoke-interface")) {
            return $jp.canThrow;
        }
        if ($jp.opCodeName.startsWith("invoke")) {
            const methodRef = $jp.children[1];
            if (methodRef === undefined || !(methodRef instanceof MethodReference)) {
                return false;
            }
            let classDescriptor = methodRef.parentClassDescriptor;
            let processedFunction;
            while (classDescriptor.decl !== undefined &&
                processedFunction === undefined) {
                classDescriptor.decl.children.forEach((child) => {
                    if (child instanceof MethodNode &&
                        child.referenceName === methodRef.code) {
                        // Calling this might lead to maximum call stack size exceeded, needs to be reworked
                        processedFunction = this.#processFunction(child)[0];
                    }
                });
                if (classDescriptor.decl.superClassDescriptor === undefined) {
                    break;
                }
                classDescriptor = classDescriptor.decl.superClassDescriptor;
            }
            if (processedFunction === undefined) {
                // returning $jp.canThrow since we don't have access to system apis
                return $jp.canThrow;
            }
            let instruction = undefined;
            if (processedFunction.nextNodes[0] !== undefined) {
                instruction = processedFunction.nextNodes[0].jp;
            }
            while (instruction !== undefined) {
                if (instruction instanceof ThrowStatement) {
                    // TODO: Needs to move backwards and check exception type
                    return true;
                }
                instruction = instruction.nextStatement;
            }
        }
        // Not sure if there are more exceptions
        return false;
    }
    #processIf($jp, context) {
        const $iftrue = $jp.label.decl;
        const $iffalse = $jp.nextStatement;
        if (!context.labels.has($iftrue.name)) {
            this.#processLabelStmt($iftrue, context);
        }
        let ifTrueHead;
        const label = context.labels.get($iftrue.name);
        if (label !== undefined) {
            ifTrueHead = label;
        }
        else {
            const trueNode = this.#createTemporaryNode();
            ifTrueHead = trueNode;
        }
        let ifFalseHead;
        let ifFalseTail;
        [ifFalseHead, ifFalseTail] = this.#processJp($iffalse, context);
        context.preprocessedStatementStack.push([ifFalseHead, ifFalseTail]);
        return [this.#graph.addCondition($jp, ifTrueHead, ifFalseHead)];
    }
    #processSwitch($jp, context) {
        const $labelRef = $jp.getChild(1);
        if (!($labelRef instanceof LabelReference)) {
            throw new Error("Switch statement must include a label reference");
        }
        const $switchDecl = $labelRef.decl.nextStatement;
        const defaultCase = $jp.nextStatement;
        const node = this.#graph
            .addNode()
            .init(new SwitchNode.Builder($jp))
            .as(SwitchNode.Class);
        const $children = $switchDecl.children;
        let previousCase = undefined;
        const childrenRefs = [];
        if ($switchDecl instanceof PackedSwitch) {
            for (const childRef of $children) {
                if (!(childRef instanceof LabelReference)) {
                    throw new Error("Packed switch directive children must be label references");
                }
                if (!context.labels.has(childRef.decl.name)) {
                    this.#processLabelStmt(childRef.decl, context);
                }
                childrenRefs.push(childRef);
            }
        }
        else if ($switchDecl instanceof SparseSwitch) {
            for (const element of $children) {
                if (!(element instanceof SparseSwitchElement)) {
                    throw new Error("Sparse switch directive children must be sparse switch elements");
                }
                const childRef = element.label;
                if (!(childRef instanceof LabelReference)) {
                    throw new Error("Sparse switch element must contain a label reference");
                }
                if (!context.labels.has(childRef.decl.name)) {
                    this.#processLabelStmt(childRef.decl, context);
                }
                childrenRefs.push(childRef);
            }
        }
        for (const child of childrenRefs) {
            const label = context.labels.get(child.decl.name);
            if (label !== undefined) {
                const currentCase = this.#graph.addSwitchCase(child, label, label);
                if (previousCase === undefined) {
                    node.nextNode = currentCase;
                }
                else {
                    previousCase.falseNode = currentCase;
                }
                previousCase = currentCase;
            }
        }
        const preProcessedStatement = this.#processJp(defaultCase, context);
        context.preprocessedStatementStack.push(preProcessedStatement);
        if (previousCase === undefined) {
            node.nextNode = preProcessedStatement[0];
        }
        else {
            previousCase.falseNode = preProcessedStatement[0];
        }
        return [node];
    }
    #processLabelStmt($jp, context) {
        if (context.labels.has($jp.name)) {
            const label = context.labels.get($jp.name);
            if (label !== undefined) {
                return [label, label];
            }
        }
        const node = this.#graph
            .addNode()
            .init(new LabelNode.Builder($jp))
            .as(LabelNode.Class);
        context.labels.set($jp.name, node);
        return [node, node];
    }
    #processGoto($jp, context) {
        const node = this.#graph
            .addNode()
            .init(new GotoNode.Builder($jp))
            .as(GotoNode.Class);
        if (!context.labels.has($jp.label.decl.name)) {
            this.#processLabelStmt($jp.label.decl, context);
        }
        const label = context.labels.get($jp.label.decl.name);
        if (label !== undefined) {
            this.#connectArbitraryJump(node, label);
        }
        return [node];
    }
    #processCatchDirective($jp, context) {
        context.tryCatchDirectives.push($jp);
        return this.#addInstruction(new StatementNode.Builder($jp));
    }
    #createTemporaryNode($jp) {
        const node = this.#graph
            .addNode()
            .init(new UnknownInstructionNode.Builder($jp))
            .as(UnknownInstructionNode.Class);
        this.#temporaryNodes.push(node);
        return node;
    }
    #addInstruction(builder) {
        const node = this.#graph.addNode().init(builder).as(InstructionNode.Class);
        return [node, node];
    }
    #addReturnStmt(builder) {
        const node = this.#graph.addNode().init(builder).as(ReturnNode.Class);
        return [node];
    }
    #addThrowStmt(builder) {
        const node = this.#graph.addNode().init(builder).as(ThrowNode.Class);
        return [node];
    }
    #connectArbitraryJump(from, to) {
        from.nextNode = to;
    }
}
//# sourceMappingURL=FlowGraphGenerator.js.map