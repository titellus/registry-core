/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.appbase.webapi.WebApiException;


public class CommandValidate extends Command {

    @Override
    public ValidationResponse validate() {
        Description d = store.getCurrentVersion(target);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, String.format("No such register for target '%s'", target));
        }
        return ValidationResponse.OK;
    }

    @Override
    public Response doExecute() {
        StringBuffer msg = new StringBuffer();
        boolean valid = true;
        List<String> testURIs = parameters.get(Parameters.VALIDATE);
        int count = 0;
        for (String uri : testURIs) {
            uri = uri.trim();
            if (uri.isEmpty()) continue;
            count++;
            boolean thisValid = false;
            List<EntityInfo> infos = store.listEntityOccurences(uri);
            for (EntityInfo info : infos) {
                if (info.getRegisterURI().startsWith(target) && info.getStatus().isA(Status.Valid)) {
                    thisValid = true;
                    msg.append(uri + " is " + info.getItemURI() + "\n");
                }
            }
            // TODO validate in delegated registers as well
            if (!thisValid) {
                if (infos.isEmpty()) {
                    msg.append("URI not found anywhere: ");
                } else {
                    msg.append("URI known but not marked as valid within this register subtree: ");
                }
                msg.append(uri);  msg.append("\n");
                valid = false;
            }
        }
        if (valid) {
            if (count > 0) {
                return Response.ok().type(MediaType.TEXT_PLAIN).entity(msg.toString()).build();
            } else {
                throw new WebApiException(BAD_REQUEST, "Empty validation list");
            }
        } else {
            throw new WebApiException(BAD_REQUEST, msg.toString());
        }
    }

}
