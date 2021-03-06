/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Tests split packages
 * @library ../lib
 * @build CompilerUtils
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng SplitPackage
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.JdepsConfiguration;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class SplitPackage {
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path CLASSES_DIR = Paths.get("classes");

    private static final String SPLIT_PKG_NAME = "javax.annotation";
    private static final String JAVA_ANNOTATIONS_COMMON = "java.annotations.common";
    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(CLASSES_DIR);
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "patches"), CLASSES_DIR));
    }

    @Test
    public void runTest() throws Exception {
        // split package detected if java.annotation.common is in the root set
        runTest(JAVA_ANNOTATIONS_COMMON, SPLIT_PKG_NAME);
        runTest("ALL-SYSTEM", SPLIT_PKG_NAME);
        // default
        runTest(null, SPLIT_PKG_NAME);

        // Test jdeps classes
        runTest("ALL-DEFAULT");

    }

    private void runTest(String root, String... splitPackages) throws Exception {
        String cmd = String.format("jdeps -verbose:class --add-modules %s %s%n",
            root, CLASSES_DIR);

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.verbose("-verbose:class")
                .addRoot(CLASSES_DIR);
            if (root != null)
                jdeps.addmods(Set.of(root));

            JdepsConfiguration config = jdeps.configuration();
            Map<String, Set<String>> pkgs = config.splitPackages();

            final Set<String> expected;
            if (splitPackages != null) {
                expected = Arrays.stream(splitPackages).collect(Collectors.toSet());
            } else {
                expected = Collections.emptySet();
            }

            if (!pkgs.keySet().equals(expected)) {
                throw new RuntimeException(splitPackages.toString());
            }

            // java.annotations.common is not observable
            DepsAnalyzer analyzer = jdeps.getDepsAnalyzer();

            assertTrue(analyzer.run());

            jdeps.dumpOutput(System.err);
        }
    }
}
