/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epimorphics.registry.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.impl.ScriptAction;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitor;
import com.epimorphics.tasks.SimpleProgressMonitor;

/**
 * Utility to run a shell script pass simple command line arguments.
 * Returned monitor indicates success/failure and has a log of message from the execution.
 */
public class RunShell {
    String scriptFile;
    
    public RunShell(String scriptFile) {
        this.scriptFile = scriptFile;
    }
    
    public ProgressMonitor run(String...args) {
        ScriptAction action = new ScriptAction();
        
        JsonObject config = JsonUtil.makeJson(ScriptAction.SCRIPT_PARAM, scriptFile); 
        List<String> arglist = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String key = "arg" + i;
            config.put(key, args[i]);
            arglist.add( key );
        }
        action.setArgMap(arglist);
        
        SimpleProgressMonitor monitor = new SimpleProgressMonitor();
        action.run(config, monitor);
        return monitor;
    }

}
