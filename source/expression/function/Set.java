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
import main.Match;

import java.util.Map;

public class Set extends Function {

    static {
        register(Set.class, "set");
    }

    private static final String VALUE = "value";

    private String mKey;
    private String mValue;

    public Set(IMatch match, ITarget target, Map<String, IExpression> parameters) {
        super(match, target);
        IExpression key = getParameter(parameters, NAME);
        IExpression value = getParameter(parameters, VALUE);
        if (!(key instanceof Literal)) {
            Match.error("Set function expects a String key");
        }
        if (!(value instanceof Literal)) {
            Match.error("Set function expects a String value");
        }
        mKey = key.resolve();
        mValue = value.resolve();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() {
        mMatch.setProperty(mKey, mValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve() {
        return mValue;
    }
}
