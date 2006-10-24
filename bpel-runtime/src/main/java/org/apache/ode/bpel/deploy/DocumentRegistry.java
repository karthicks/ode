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
package org.apache.ode.bpel.deploy;

import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.bom.PartnerLinkType;
import org.apache.ode.bpel.compiler.bom.Property;
import org.apache.ode.bpel.compiler.bom.PropertyAlias;
import org.apache.ode.bpel.compiler.wsdl.Definition4BPEL;
import org.apache.ode.bpel.compiler.wsdl.XMLSchemaType;
import org.apache.ode.bpel.iapi.BpelEngineException;
import org.apache.ode.utils.xsd.SchemaModel;
import org.apache.ode.utils.xsd.SchemaModelImpl;
import org.apache.ode.utils.xsd.XSUtils;
import org.apache.ode.utils.xsd.XsdException;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
 * A parsed collection of WSDL definitions, including BPEL-specific extensions.
 */
public class DocumentRegistry {
    private static final Log __log = LogFactory.getLog(DocumentRegistry.class);
    private static final Messages __msgs = Messages.getMessages(Messages.class);

    private final ArrayList<Definition4BPEL> _definitions = new ArrayList<Definition4BPEL>();
    private final Map<URI, byte[]> _schemas = new HashMap<URI,byte[]>();

    private SchemaModel _model;
    private XMLEntityResolver _resolver;

    public DocumentRegistry(XMLEntityResolver resolver) {
        // bogus schema to force schema creation
        _schemas.put(URI.create("http://www.apache.org/ode/bogus/namespace"),
                ("<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                        + " targetNamespace=\"http://www.apache.org/ode/bogus/namespace\">"
                        + "<xsd:simpleType name=\"__bogusType__\">"
                        + "<xsd:restriction base=\"xsd:normalizedString\"/>"
                        + "</xsd:simpleType>" + "</xsd:schema>").getBytes());
        _resolver = resolver;
    }


    /**
     * Obtains an WSDL definition based on its target namespace.
     *
     * @param serviceName
     *
     * @return WSDL definition or <code>null</code> if unavailable.
     */
    public Definition4BPEL getDefinition(QName serviceName) {
        for (Definition4BPEL definition4BPEL : _definitions) {
            if (definition4BPEL.getTargetNamespace().equals(serviceName.getNamespaceURI())) {
                if (definition4BPEL.getService(serviceName) != null)
                    return definition4BPEL;
            }
        }
        return null;
    }

    public Definition4BPEL[] getDefinitions(){
        return _definitions.toArray(new Definition4BPEL[_definitions.size()]);
    }

    /**
     * Adds a WSDL definition for use in resolving MessageType, PortType,
     * Operation and BPEL properties and property aliases
     * @param def WSDL definition
     */
    @SuppressWarnings("unchecked")
    public void addDefinition(Definition4BPEL def) throws CompilationException {
        if (def == null)
            throw new NullPointerException("def=null");

        if (__log.isDebugEnabled()) {
            __log.debug("addDefinition(" + def.getTargetNamespace() + " from " + def.getDocumentBaseURI() + ")");
        }

        _definitions.add(def);

        // For now the schemas are never used at runtime. Check the compiler if this needs to be
        // put back in.
//        captureSchemas(def);
    }

}
