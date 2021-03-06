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

import expression.IExpression;
import expression.Literal;
import main.IMatch;
import main.ITarget;
import main.MatchTest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FindTest {

    private static final String BAR = ".*/bar";
    private final Set<String> filesA = new HashSet<String>();
    private final Set<String> filesB = new HashSet<String>();

    private File mRoot;
    private String mRootPath;

    @Before
    public void setUp() throws IOException {
        mRoot = MatchTest.createFileStructure();
        mRootPath = mRoot.toString();
        filesA.add(String.format("%s/a/b", mRootPath));
        filesA.add(String.format("%s/c/d/e", mRootPath));
        filesA.add(String.format("%s/c/d/f", mRootPath));
        filesA.add(String.format("%s/bar", mRootPath));
        filesB.add(String.format("%s/bar", mRootPath));
    }

    @After
    public void tearDown() throws IOException {
        MatchTest.deleteFileStructure(mRoot);
    }

    @Test
    public void resolveAnonymous() {
        resolve(filesA, Function.ANONYMOUS, "");
    }

    @Test
    public void resolveNamed() {
        resolve(filesB, Find.DIRECTORY, "", Find.PATTERN, BAR);
    }

    private void resolve(Set<String> expected, String... values) {
        IMatch match = Mockito.mock(IMatch.class);
        ITarget target = Mockito.mock(ITarget.class);
        Mockito.when(target.getFile()).thenReturn(new File(mRoot, "match"));
        Map<String, IExpression> parameters = new HashMap<String, IExpression>();
        for (int i = 0; i < values.length; i++) {
            parameters.put(values[i], new Literal(match, target, values[++i]));
        }
        IFunction function = new Find(match, target, parameters);
        function.configure();
        List<String> actual = function.resolveList();
        Assert.assertEquals("Wrong number of files", expected.size(), actual.size());
        for (String file : actual) {
            Assert.assertTrue(String.format("%s not found", file), expected.contains(file));
        }
    }

}
