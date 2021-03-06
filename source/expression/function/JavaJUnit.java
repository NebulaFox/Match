/*
 * Copyright 2015 Stuart Scott
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package expression.function;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.IMatch;
import main.ITarget;
import main.Utilities;
import expression.IExpression;
import expression.Literal;

public class JavaJUnit extends Function {

    private static final String RESULT_OUTPUT = "./out/results";
    private static final String MKDIR_COMMAND = "mkdir -p %s";
    private static final String RUN_COMMAND = "java %s org.junit.runner.JUnitCore %s | tee %s";

    private String mName;
    private String mMainClass;
    private String mOutput;

    public JavaJUnit(IMatch match, ITarget target, Map<String, IExpression> parameters) {
        super(match, target, parameters);
        IExpression name = getParameter(NAME);
        IExpression mainClass = getParameter(MAIN_CLASS);
        if (!(name instanceof Literal)) {
            mMatch.error("JavaJar function expects a String name");
        }
        if (!(mainClass instanceof Literal)) {
            mMatch.error("JavaJar function expects a String main_class");
        }
        mName = name.resolve();
        mMainClass = mainClass.resolve();
        mOutput = String.format("%s/%s", RESULT_OUTPUT, mName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() {
        mMatch.addFile(mOutput);
        mMatch.setProperty(mName, mOutput);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve() {
        List<String> libraries = new ArrayList<>();
        Set<String> libs = new HashSet<>();
        libs.add("junit");
        libs.add("hamcrest");
        libs.add("mockito");
        if (hasParameter(LIBRARY)) {
            for (String library : getParameter(LIBRARY).resolveList()) {
                libs.add(library);
            }
        }
        for (String library : libs) {
            libraries.add(mMatch.getProperty(library));
        }
        String classpath = String.format("-cp %s", Utilities.join(":", libraries));
        mMatch.runCommand(String.format(MKDIR_COMMAND, RESULT_OUTPUT));
        mMatch.runCommand(String.format(RUN_COMMAND, classpath, mMainClass, mOutput));
        mMatch.provideFile(mOutput);
        return mOutput;
    }
}
