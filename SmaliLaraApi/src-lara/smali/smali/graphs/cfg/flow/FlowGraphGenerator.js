import { MethodNode, Program, Statement, } from "../../../../Joinpoints.js";
import Query from "lara-js/api/weaver/Query.js";
import UnknownInstructionNode from "./node/instruction/UnknownInstructionNode.js";
import InstructionNode from "./node/instruction/InstructionNode.js";
import StatementNode from "./node/instruction/StatementNode.js";
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
            for (const $function of Query.searchFrom(this.#$jp, "methodNode")) {
                this.#processFunction($function);
            }
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
        // if ($jp instanceof MethodNode) {
        //     return this.#processFunction($jp);
        // } else if ($jp instanceof Scope) {
        //     return this.#processScope($jp, context);
        // } else if ($jp instanceof WrapperStmt) {
        //     if ($jp.kind === "comment") {
        //         return this.#addInstruction(
        //             new CommentNode.Builder($jp.content as Comment),
        //         );
        //     } else if ($jp.kind === "pragma") {
        //         return this.#addInstruction(
        //             new PragmaNode.Builder($jp.content as Pragma),
        //         );
        //     } else {
        //         throw new Error(
        //             `Cannot build graph for "${$jp.joinPointType}:${$jp.kind}"`,
        //         );
        //     }
        if ($jp instanceof Statement) {
            return this.#addInstruction(new StatementNode.Builder($jp));
        }
        else {
            console.log($jp.joinPointType);
            throw new Error(`Cannot build graph for joinpoint "${$jp.joinPointType}"`);
        }
        // if ($jp instanceof DeclStmt) {
        //     return this.#processVarDecl($jp);
        // } else if ($jp instanceof EmptyStmt) {
        //     return this.#addInstruction(new EmptyStatementNode.Builder($jp));
        // } else if ($jp instanceof ExprStmt) {
        //     return this.#addInstruction(new ExpressionNode.Builder($jp.expr));
        // } else if ($jp instanceof If) {
        //     return this.#processIf($jp, context);
        // // } else if ($jp instanceof Loop) {
        // //     return this.#processLoop($jp, context);
        // } else if ($jp instanceof Switch) {
        //     return this.#processSwitch($jp, context);
        // } else if ($jp instanceof Case) {
        //     // Case nodes will be processed by the Switch
        //     // Marking them as a temporary is enough
        //     const node = this.#createTemporaryNode($jp);
        //     if ($jp.isDefault) {
        //         context.defaultCase = node;
        //     } else {
        //         context.caseNodes?.push(node);
        //     }
        //     return [node, node];
        // } else if ($jp instanceof ReturnStmt) {
        //     return this.#addOutwardsJump(
        //         new ReturnNode.Builder($jp),
        //         context.returnNode!,
        //     );
        // // } else if ($jp instanceof Break) {
        // //     return this.#addOutwardsJump(new BreakNode.Builder($jp), context.breakNode!);
        // // } else if ($jp instanceof Continue) {
        // //     return this.#addOutwardsJump(
        // //         new ContinueNode.Builder($jp),
        // //         context.continueNode!,
        // //     );
        // } else if ($jp instanceof Label) {
        //     return this.#processLabelStmt($jp, context);
        // } else if ($jp instanceof Goto) {
        //     return this.#processGoto($jp, context);
        // } else {
        //     // TODO maybe be silent when inside recursive calls?
        //     throw new Error(`Cannot build graph for joinpoint "${$jp.joinPointType}"`);
        // }
    }
    #processFunction($jp) {
        const returnNode = this.#createTemporaryNode($jp);
        // const params = $jp.params.map(($p) =>
        //     this.#graph
        //         .addNode()
        //         .init(new VarDeclarationNode.Builder($p))
        //         .as(VarDeclarationNode.Class),
        // );
        const body = $jp.children.map((child) => {
            const [head, tail] = this.#processJp(child, {
                labels: new Map(),
                gotos: new Map(),
                returnNode,
            });
            return [head, tail ? [tail] : []];
        });
        for (let i = 0; i < body.length - 2; i++) {
            const [head, tail] = body[i];
            const [nextHead, nextTail] = body[i + 1];
            for (const tailNode of tail) {
                tailNode.nextNode = nextHead;
            }
        }
        // const body = this.#processJp($jp.body, {
        //     labels: new Map(),
        //     gotos: new Map(),
        //     returnNode,
        // });
        const bodyTail = body[body.length - 1][1];
        if (bodyTail !== undefined) {
            for (const tailNode of bodyTail) {
                tailNode.nextNode = returnNode;
            }
        }
        const bodyHead = body[0][0];
        return this.#graph.addMethod($jp, bodyHead);
        // const params: VarDeclarationNode.Class[] = [];
        // return this.#graph.addMethod($jp, bodyHead, params);
    }
    // // #processScope(
    // //     $jp: Scope,
    // //     context: ProcessJpContext,
    // // ): [ScopeStartNode.Class, ScopeEndNode.Class?] {
    // //     const subGraphs = $jp.children.map((child) => {
    // //         const [head, tail] = this.#processJp(child, context);
    // //         return [head, tail ? [tail] : []] as [
    // //             FlowNode.Class,
    // //             InstructionNode.Class[],
    // //         ];
    // //     });
    // //     return this.#graph.addScope($jp, subGraphs);
    // // }
    // #processVarDecl($jp: DeclStmt): [VarDeclarationNode.Class, VarDeclarationNode.Class] {
    //     if ($jp.decls.length === 0) {
    //         throw new Error("Empty declaration statement");
    //     }
    //     let head: VarDeclarationNode.Class | undefined;
    //     let tail: VarDeclarationNode.Class | undefined;
    //     for (const $decl of $jp.decls) {
    //         if ($decl instanceof Vardecl) {
    //             const node = this.#graph
    //                 .addNode()
    //                 .init(new VarDeclarationNode.Builder($decl))
    //                 .as(VarDeclarationNode.Class);
    //             if (head === undefined) {
    //                 head = node;
    //             }
    //             if (tail !== undefined) {
    //                 tail.nextNode = node;
    //             }
    //             tail = node;
    //         } else {
    //             throw new Error("Unsupported declaration type");
    //         }
    //     }
    //     return [head!, tail!];
    // }
    // #processIf(
    //     $jp: If,
    //     context: ProcessJpContext,
    // ): [ConditionNode.Class, InstructionNode.Class?] {
    //     // const $iftrue = $jp.then as Scope;
    //     // // Type conversion necessary because the return type of clava is incorrect
    //     // const $iffalse = $jp.else as Scope | undefined;
    //     // const [ifTrueHead, iftrueTail] = this.#processScope($iftrue, context);
    //     // let ifFalseHead: FlowNode.Class;
    //     // let ifFalseTail: InstructionNode.Class | undefined;
    //     // if ($iffalse !== undefined) {
    //     //     [ifFalseHead, ifFalseTail] = this.#processScope($iffalse, context);
    //     // } else {
    //     //     const falseNode = this.#createTemporaryNode();
    //     //     [ifFalseHead, ifFalseTail] = [falseNode, falseNode];
    //     // }
    //     // const node = this.#graph.addCondition($jp, ifTrueHead, ifFalseHead);
    //     const $ifLabel = $jp.label as LabelReference;
    //     const $ifTrue = $ifLabel.decl.nextStatement;
    //     //const $ifFalse = $jp.nextStatement as Statement;
    //     const $ifFalse = this.#createTemporaryNode();
    //     const $true = this.#addInstruction(new IfTrueJump.Builder($jp));
    //     const node = this.#graph.addCondition($jp, $true[0], $ifFalse);
    //     // if (iftrueTail === undefined && ifFalseTail === undefined) {
    //     //     return [node];
    //     // }
    //     // const endIf = this.#createTemporaryNode($jp);
    //     // if (iftrueTail !== undefined) {
    //     //     iftrueTail.nextNode = endIf;
    //     // }
    //     // if (ifFalseTail !== undefined) {
    //     //     ifFalseTail.nextNode = endIf;
    //     // }
    //     return [node, endIf];
    // }
    // // #processLoop(
    // //     $jp: Loop,
    // //     context: ProcessJpContext,
    // // ): [FlowNode.Class, InstructionNode.Class] {
    // //     const continueNode = this.#createTemporaryNode($jp);
    // //     const breakNode = this.#createTemporaryNode($jp);
    // //     const [bodyHead, bodyTail] = this.#processScope($jp.body, {
    // //         ...context,
    // //         breakNode,
    // //         continueNode,
    // //     });
    // //     const node = this.#graph.addLoop(
    // //         $jp,
    // //         bodyHead,
    // //         bodyTail ? [bodyTail] : [],
    // //         breakNode,
    // //     );
    // //     let head: FlowNode.Class;
    // //     if ($jp.kind === "for") {
    // //         const [, init] = this.#processJp($jp.init, context);
    // //         const [, step] = this.#processJp($jp.step, context);
    // //         if (init === undefined) {
    // //             throw new Error("Init must be an instruction node");
    // //         }
    // //         if (step === undefined) {
    // //             throw new Error("Step must be an instruction node");
    // //         }
    // //         continueNode.insertBefore(init);
    // //         node.insertBefore(step);
    // //         head = init;
    // //     } else if ($jp.kind == "dowhile") {
    // //         head = bodyHead;
    // //     } else if ($jp.kind == "while") {
    // //         head = continueNode;
    // //     } else {
    // //         throw new Error(`Unsupported loop kind "${$jp.kind}"`);
    // //     }
    // //     node.insertBefore(continueNode);
    // //     return [head, breakNode];
    // // }
    // #processSwitch(
    //     $jp: Switch,
    //     context: ProcessJpContext,
    // ): [SwitchNode.Class, ScopeEndNode.Class?] {
    //     // We know child 1 is a Label to a switch but we don't know the type sparse or packed
    //     // We also know the default case is the next statement
    //     const $labelRef = $jp.getChild(1);
    //     if (!($labelRef instanceof LabelReference)) {
    //         throw new Error("Switch statement must include a label reference");
    //     }
    //     const $switchDecl = ($labelRef as LabelReference).decl.nextStatement;
    //     const $defaultCase = $jp.nextStatement;
    //     if ($switchDecl instanceof PackedSwitch) {
    //     }
    //     else if ($switchDecl instanceof SparseSwitch) {
    //     }
    //     const $body = $jp.getChild(1);
    //     if (!($body instanceof Scope)) {
    //         throw new Error("Switch body must be a scope");
    //     }
    //     const breakNode = this.#createTemporaryNode($body);
    //     const caseNodes: UnknownInstructionNode.Class[] = [];
    //     const innerContext = { ...context, breakNode, caseNodes };
    //     const [bodyHead, bodyTail] = this.#processScope($body, innerContext);
    //     const defaultCase = innerContext.defaultCase;
    //     const node = this.#graph
    //         .addNode()
    //         .init(new SwitchNode.Builder($jp))
    //         .as(SwitchNode.Class);
    //     bodyHead.insertBefore(node);
    //     let previousCase: ConditionNode.Class | undefined = undefined;
    //     for (const tempCaseNode of caseNodes) {
    //         const currentCase = this.#graph.addCondition(
    //             tempCaseNode.jp as Case,
    //             tempCaseNode.nextNode!,
    //             tempCaseNode, // False node doesn't matter for now, since it will change
    //         );
    //         if (previousCase === undefined) {
    //             bodyHead.nextNode = currentCase;
    //         } else {
    //             previousCase.falseNode = currentCase;
    //         }
    //         for (const incomer of tempCaseNode.incomers) {
    //             incomer.target = tempCaseNode.nextNode!;
    //         }
    //         previousCase = currentCase;
    //     }
    //     if (defaultCase !== undefined) {
    //         const currentCase = this.#graph.addCondition(
    //             defaultCase.jp as Case,
    //             defaultCase.nextNode!,
    //             defaultCase, // False node doesn't matter for now, since it will change
    //         );
    //         if (previousCase === undefined) {
    //             bodyHead.nextNode = currentCase;
    //         } else {
    //             previousCase.falseNode = currentCase;
    //         }
    //         for (const incomer of defaultCase.incomers) {
    //             incomer.target = defaultCase.nextNode!;
    //         }
    //         previousCase = currentCase;
    //     }
    //     let scopeEnd = bodyTail;
    //     if (scopeEnd === undefined) {
    //         if (breakNode.incomers.length === 0) {
    //             return [node];
    //         }
    //         scopeEnd = this.#graph
    //             .addNode()
    //             .init(new ScopeEndNode.Builder($body))
    //             .as(ScopeEndNode.Class);
    //         breakNode.nextNode = scopeEnd;
    //     }
    //     scopeEnd.insertBefore(breakNode);
    //     if (previousCase === undefined) {
    //         bodyHead.nextNode = scopeEnd;
    //     } else {
    //         previousCase.falseNode = scopeEnd;
    //     }
    //     return [node, scopeEnd];
    // }
    // #processLabelStmt(
    //     $jp: Label,
    //     context: ProcessJpContext,
    // ): [GotoLabelNode.Class, GotoLabelNode.Class] {
    //     const node = this.#graph
    //         .addNode()
    //         .init(new GotoLabelNode.Builder($jp))
    //         .as(GotoLabelNode.Class);
    //     context.labels.set($jp.name, node);
    //     const gotos = context.gotos.get($jp.name);
    //     if (gotos !== undefined) {
    //         for (const goto of gotos) {
    //             this.#connectArbitraryJump(goto, node);
    //         }
    //     }
    //     return [node, node];
    // }
    // #processGoto($jp: Goto, context: ProcessJpContext): [GotoNode.Class] {
    //     const node = this.#graph
    //         .addNode()
    //         .init(new GotoNode.Builder($jp))
    //         .as(GotoNode.Class);
    //     if (context.gotos.has($jp.label.name)) {
    //         context.gotos.get($jp.label.name)!.push(node);
    //     } else {
    //         context.gotos.set($jp.label.name, [node]);
    //     }
    //     const label = context.labels.get($jp.label.name);
    //     if (label !== undefined) {
    //         this.#connectArbitraryJump(node, label);
    //     }
    //     return [node];
    // }
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
}
//# sourceMappingURL=FlowGraphGenerator.js.map