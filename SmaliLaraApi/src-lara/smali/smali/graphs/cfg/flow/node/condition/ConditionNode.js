import ControlFlowEdge from "../../../flow/edge/ControlFlowEdge.js";
import FlowNode from "../../../flow/node/FlowNode.js";
var ConditionNode;
(function (ConditionNode) {
    class Class extends FlowNode.Class {
        get trueEdge() {
            // Data and scratchdata should be ControlFlowEdge
            const edge = this.graph.getEdgeById(this.data.trueEdgeId);
            return edge.as(ControlFlowEdge.Class);
        }
        get trueNode() {
            const node = this.trueEdge.target;
            return node.as(FlowNode.Class);
        }
        set trueNode(node) {
            this.trueEdge.target = node;
        }
        get falseEdge() {
            // Data and scratchdata should be ControlFlowEdge
            const edge = this.graph.getEdgeById(this.data.falseEdgeId);
            return edge.as(ControlFlowEdge.Class);
        }
        get falseNode() {
            const node = this.falseEdge.target;
            return node.as(FlowNode.Class);
        }
        set falseNode(node) {
            this.falseEdge.target = node;
        }
        get jp() {
            return this.scratchData.$jp;
        }
    }
    ConditionNode.Class = Class;
    class Builder extends FlowNode.Builder {
        #truePath;
        #falsePath;
        constructor(truePath, falsePath, $jp) {
            super(FlowNode.Type.CONDITION, $jp);
            this.#truePath = truePath;
            this.#falsePath = falsePath;
        }
        buildData(data) {
            return {
                ...super.buildData(data),
                trueEdgeId: this.#truePath.id,
                falseEdgeId: this.#falsePath.id,
            };
        }
        buildScratchData(scratchData) {
            return {
                ...super.buildScratchData(scratchData),
            };
        }
    }
    ConditionNode.Builder = Builder;
    ConditionNode.TypeGuard = {
        isDataCompatible(data) {
            if (!FlowNode.TypeGuard.isDataCompatible(data))
                return false;
            const d = data;
            if (d.flowNodeType !== FlowNode.Type.CONDITION)
                return false;
            if (typeof d.trueEdgeId !== "string")
                return false;
            if (typeof d.falseEdgeId !== "string")
                return false;
            return true;
        },
        isScratchDataCompatible(scratchData) {
            if (!FlowNode.TypeGuard.isScratchDataCompatible(scratchData))
                return false;
            return true;
        },
    };
})(ConditionNode || (ConditionNode = {}));
export default ConditionNode;
//# sourceMappingURL=ConditionNode.js.map