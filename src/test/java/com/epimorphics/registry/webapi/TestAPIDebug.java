/******************************************************************
 * File:        TestAPIDebug.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Place where new webapi tests can be developed to investigate
 * reported problems. Once running they get merged
 * in the main tests in TestAPI.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestAPIDebug extends TomcatTestBase {

    static final String EXT_BLACK = "http://example.com/colours/black";
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";

    String getWebappRoot() {
        return "src/test/webapp";
    }

    @Test
    public void testDebug() throws IOException {
        // Set up some base data
        assertEquals(201, postFileStatus("test/reg1.ttl", BASE_URL));
        assertEquals(201, postFileStatus("test/red.ttl", REG1));
        
        String annotationGraph = BASE_URL + "reg1/_red?annotation=test";
        assertEquals(404, getResponse(annotationGraph).getStatus());
        
        ClientResponse response = invoke("PUT", "test/ont1.ttl", annotationGraph);
        assertEquals(201, response.getStatus());
        
        Model m = getModelResponse(annotationGraph);
        assertNotNull(m);
        assertTrue( hasTerm(m, "A") );
        assertTrue( hasTerm(m, "a") );
        assertTrue( hasTerm(m, "p") );
        
        m = getModelResponse(BASE_URL + "reg1/_red");
        Resource item = m.getResource(ROOT_REGISTER + "reg1/_red");
        assertTrue( item.hasProperty(RegistryVocab.annotation) );
        assertTrue( item.hasProperty(RegistryVocab.annotation, m.getResource(ROOT_REGISTER + "reg1/_red?annotation=test")) );

    }

    private boolean hasTerm(Model m, String term) {
        Resource r = m.getResource(ROOT_REGISTER + "reg1/ont#" + term);
        return r.hasProperty(RDF.type);
    }

    // Debugging utility only, should not be used while transactions are live
    public void printResourceState(String...uris) {
        Store storesvc = ServiceConfig.get().getServiceAs("basestore", Store.class);
        storesvc.lock();
        try {
            Dataset ds =  storesvc.asDataset();
            Model store = ds.getDefaultModel();
            Model description = ModelFactory.createDefaultModel();
            for (String uri: uris) {
                Resource r = store.getResource(uri);
                Closure.closure(r, false, description);
                if (r.hasProperty(Version.currentVersion)) {
                    r = r.getPropertyResourceValue(Version.currentVersion);
                    Closure.closure(r, false, description);
                }
            }
            description.setNsPrefixes( Prefixes.get() );
            description.write(System.out, "Turtle");
            for (NodeIterator ni = description.listObjectsOfProperty(RegistryVocab.sourceGraph); ni.hasNext(); ) {
                String graphname = ni.next().asResource().getURI();
                Model graph = ModelFactory.createDefaultModel().add( ds.getNamedModel(graphname) );
                System.out.println("Graph " + graphname);
                graph.setNsPrefixes(Prefixes.get());
                graph.write(System.out, "Turtle");
            }
        } finally {
            storesvc.unlock();
        }
    }

    public static void debugStatus(ClientResponse response) {
        if (response.getStatus() >= 400) {
            System.out.println("Response was: " + response.getEntity(String.class) + " (" + response.getStatus() + ")");
            assertTrue(false);
        }
    }
}
