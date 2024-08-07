/**
 * Copyright 2024 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.alpakka.ast.stmt;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.alpakka.ast.SmaliNode;
import pt.up.fe.specs.alpakka.ast.AnnotationVisibility;
import pt.up.fe.specs.alpakka.ast.expr.literal.typeDescriptor.ClassType;

public class AnnotationDirective extends Statement {

    public AnnotationDirective(DataStore data, Collection<? extends SmaliNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var sb = new StringBuilder();
        var visibility = (AnnotationVisibility) get(ATTRIBUTES).get("visibility");
        var classDescriptor = (ClassType) get(ATTRIBUTES).get("classDescriptor");

        sb.append(getLine());

        sb.append(".annotation " + visibility.getVisibility() + " " + classDescriptor.getCode() + "\n");

        for (var child : getChildren()) {
            sb.append(indentCode(child.getCode()) + "\n");
        }

        sb.append(".end annotation");

        return sb.toString();
    }

}
