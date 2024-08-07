/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.alpakka.weaver.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import pt.up.fe.specs.alpakka.weaver.SmaliWeaverTester;
import pt.up.fe.specs.util.SpecsSystem;

public class SmaliWeaverTest {

    @BeforeClass
    public static void setupOnce() {
        SpecsSystem.programStandardInit();
        SmaliWeaverTester.clean();
    }

    @After
    public void tearDown() {
        SmaliWeaverTester.clean();
    }

    private static SmaliWeaverTester newTester() {
        return new SmaliWeaverTester("alpakka/test/weaver/")
                .setSrcPackage("src/")
                .setResultPackage("results/");
    }

    @Test
    public void testBasic() {
        newTester().test("HelloWorld.js", "HelloWorld.smali");
    }

    @Test
    public void testManifest() {
        newTester().test("Manifest.js", "Bankdroid-rev-2b0345b5c2.apk");
    }

}
