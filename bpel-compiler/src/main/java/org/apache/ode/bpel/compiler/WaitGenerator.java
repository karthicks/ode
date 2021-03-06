/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ode.bpel.compiler;

import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.bom.WaitActivity;
import org.apache.ode.bpel.obj.OActivity;
import org.apache.ode.bpel.obj.OWait;
import org.apache.ode.utils.msg.MessageBundle;

/**
 * Generates code for the <code>&lt;wait&gt;</code> activities.
 */
class WaitGenerator extends DefaultActivityGenerator {

    private WaitGeneratorMessages _msgs = MessageBundle.getMessages(WaitGeneratorMessages.class);

    public OActivity newInstance(Activity src) {
        return new OWait(_context.getOProcess(), _context.getCurrent());
    }

    public void compile(OActivity output, Activity src) {
        WaitActivity waitDef = (WaitActivity)src;
        OWait owait = (OWait)output;
        if (waitDef.getFor() != null && waitDef.getUntil() == null) {
            owait.setForExpression(_context.compileExpr(waitDef.getFor()));
        }
        else if (waitDef.getFor() == null && waitDef.getUntil() != null) {
            owait.setUntilExpression(_context.compileExpr(waitDef.getUntil()));
        }
        else {
            throw new CompilationException(_msgs.errWaitMustDefineForOrUntilDuration().setSource(waitDef));
        }
    }
}
