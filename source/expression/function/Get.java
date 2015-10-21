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
import main.IMatch;
import main.ITarget;

import java.util.Map;

public class Get extends Function {

    static {
        register(Get.class, "get");
    }

    public Get(IMatch match, ITarget target, Map<String, IExpression> parameters) {
        super(match, target, parameters);
    }

    public String resolve() {
        return mTarget.getProperty(getParameter(ANONYMOUS).resolve());
    }
}
