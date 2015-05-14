/******************************************************************
 * File:        Status.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
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

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * The set of status values which a RegisterItem can have.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Status {
    public static final String PRES_DEFAULT = "default";
    public static final String PRES_SUCCESS = "success";
    public static final String PRES_WARNING = "warning";
    public static final String PRES_DANGER  = "danger";
    
    public static Status Any          = new Status(RegistryVocab.Status,             "any");
    
    public static Status NotAccepted  = new Status(RegistryVocab.statusNotAccepted,  "notaccepted", PRES_DEFAULT, Any);
    public static Status Submitted    = new Status(RegistryVocab.statusSubmitted,    "submitted",   PRES_DEFAULT, NotAccepted);
    public static Status Reserved     = new Status(RegistryVocab.statusReserved,     "reserved",    PRES_DEFAULT, NotAccepted, Submitted);
    public static Status Invalid      = new Status(RegistryVocab.statusInvalid,      "invalid",     PRES_DANGER,  NotAccepted);
    
    public static Status Accepted     = new Status(RegistryVocab.statusAccepted,     "accepted",    PRES_DEFAULT, Any);
    public static Status Valid        = new Status(RegistryVocab.statusValid,        "valid",       PRES_DEFAULT, Accepted);
    public static Status Deprecated   = new Status(RegistryVocab.statusDeprecated,   "deprecated",  PRES_DANGER,  Valid);
    public static Status Superseded   = new Status(RegistryVocab.statusSuperseded,   "superseded",  PRES_DANGER,  Deprecated);
    public static Status Retired      = new Status(RegistryVocab.statusRetired,      "retired",     PRES_DANGER,  Deprecated);
    
    public static Status Stable       = new Status(RegistryVocab.statusStable,       "stable",       PRES_SUCCESS, Valid, Retired, Superseded);
    public static Status Experimental = new Status(RegistryVocab.statusExperimental, "experimental", PRES_WARNING, Valid, Stable, Retired, Superseded);

    public static final String LIFECYCLE_REGISTER = "/system/lifecycle";

    protected static Map<String, Status> statusIndex;
    protected static boolean needsReload = true;
    
    protected Resource resource;
    protected String label;
    protected Set<Status> successors = new HashSet<>();
    protected Status parent;
    protected String presentation = "";
    
    protected Status(Resource resource, String label) {
        this(resource, label, PRES_DEFAULT, null);
    }
    
    protected Status(Resource resource, String label, String presentation, Status parent, Status...successors) {
        this.resource = resource;
        this.label = label.toLowerCase();
        this.parent = parent;
        this.presentation = presentation;
        for (Status successor : successors) {
            this.successors.add( successor );
        }
    }
    
    public void addSuccessor(Status successor) {
        successors.add(successor);
    }
    
    public String getPresentation() {
        return presentation;
    }

    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    public String getLabel() {
        return label;
    }

    public Collection<Status> nextStates() {
        return successors;
    }

    public void setParent(Status parent) {
        this.parent = parent;
    }

    public Resource getResource() {
        return resource;
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof Status && this.resource.equals( ((Status)other).resource );
    }
    
    @Override
    final public int hashCode() {
        return resource.hashCode();
    }

    public static Status forResource(Resource r) {
        reload();
        for (Status s : statusIndex.values()) {
            if (s.getResource().equals(r)) {
                return s;
            }
        }
        return NotAccepted;
    }

    public static Status forString(String param, Status deflt) {
        reload();
        Status s = statusIndex.get(param);
        return s == null ? deflt : s;
    }

    public boolean isNotAccepted() {
        return isA( NotAccepted );
    }

    public boolean isAccepted() {
        return ! isNotAccepted();
    }

    public boolean isDeprecated() {
        return this == Superseded || this == Retired;
    }

    /**
     * Return true if this status is an instance of the target status category
     */
    public boolean isA(Status target) {
        if (target == null) return true;
        if (this.equals(target)) {
            return true;
        } else {
            if (parent != null) {
                return parent.isA(target);
            } else {
                return false;
            }
        }
    }

    /**
     * Return true of the target status is a legal next state after this status.
     * The target should be a concrete (leaf) status, otherwise will return false.
     */
    public boolean legalNextState(Status target) {
        if (this.equals(target)) return true;
        for (Status n : successors) {
            if ( n.equals(target) ) {
                return true;
            }
        }
        return false;
    }

    public synchronized static void reset() {
        needsReload = true;
    }
    
    /**
     * Update the status set based on the status register
     */
    public synchronized static void reload() {
        if (needsReload) {
            // Reset back to just builtins
            statusIndex = new HashMap<String, Status>();
            for (Status s : new Status[]{Any, NotAccepted, Submitted, Reserved, Invalid, Accepted, Valid, Deprecated, Superseded, Retired}) {
                addStatus(s);
                s.addSuccessor(Invalid);
            }
    
            String registerURI = Registry.get().getBaseURI() + LIFECYCLE_REGISTER;
            Description d = Registry.get().getStore().getDescription(registerURI);
            if (d instanceof Register) {
                Register register = (Register) d;
            
                Model view = ModelFactory.createDefaultModel();
                List<Resource> members = new ArrayList<>();
                register.constructView(view, false, null, 0, -1, -1, members);
            
                for (Resource member : members) {
                    Status s = new Status(member, RDFUtil.getLabel(member));
                    addStatus(s);
                    Resource parent = RDFUtil.getResourceValue(member, SKOS.broader);
                    if (parent != null) {
                        s.setParent( forResource(parent) );
                    }
                    s.setPresentation( RDFUtil.getStringValue(member, RegistryVocab.presentation, PRES_DEFAULT) );
                    for (Status suc : getStatusValues(member, RegistryVocab.nextState)) {
                        s.addSuccessor(suc);
                    }
                    for (Status p : getStatusValues(member, RegistryVocab.priorState)) {
                        p.addSuccessor(s);
                    }
               }
            } else {
                // No custom lifecycle 
                for (Status s : new Status[]{Stable, Experimental}) {
                    addStatus(s);
                    s.addSuccessor(Invalid);
                }
                Submitted.addSuccessor(Stable);
                Submitted.addSuccessor(Experimental);
            }
            needsReload = false;
        }
    }
    
    protected static List<Status> getStatusValues(Resource root, Property p) {
        List<Status> results = new ArrayList<>();
        for (StmtIterator i = root.listProperties(p); i.hasNext();) {
            RDFNode value = i.next().getObject();
            Status s = null;
            if (value.isLiteral()) {
                s = forString(value.asLiteral().getLexicalForm(), null);
            } else if (value.isURIResource()) {
                s = forResource(value.asResource());
            }
            if (s != null) {
                results.add( s );
            }
        }
        return results;
    }
    
    protected static void addStatus(Status s) {
        statusIndex.put(s.getLabel(), s);
    }
}
