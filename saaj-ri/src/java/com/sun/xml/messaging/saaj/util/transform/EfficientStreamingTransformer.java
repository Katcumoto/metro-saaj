/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * EfficientStreamingTransformer.java
 *
 * Created on July 29, 2002, 3:49 PM
 */

package com.sun.xml.messaging.saaj.util.transform;

import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.sun.xml.messaging.saaj.util.XMLDeclarationParser;
import com.sun.xml.messaging.saaj.util.FastInfosetReflection;

/**
 * This class is a proxy for a Transformer object with optimizations
 * for certain cases. If source and result are of type stream, then
 * bytes are simply copied whenever possible (note that this assumes 
 * that the input is well formed). In addition, it provides support for
 * FI using native DOM parsers and serializers.
 *
 * @author Panos Kougiouris panos@acm.org
 * @author Santiago.PericasGeertsen@sun.com
 *
 */
public class EfficientStreamingTransformer
    extends javax.xml.transform.Transformer {

  //static final String version;
  //static final String vendor;

  protected static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  /** 
  removing support for Java 1.4 and 1.3 : CR6658158
  static {
        version = System.getProperty("java.vm.version");
        vendor = System.getProperty("java.vm.vendor");
        if (vendor.startsWith("Sun") && 
            (version.startsWith("1.4") || version.startsWith("1.3"))) {
            transformerFactory = 
                new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();
        }
  }*/
                                                                                                                                                  
    /**
     * TransformerFactory instance.
     */
    
    /**
     * Underlying XSLT transformer.
     */
    private Transformer m_realTransformer = null;
    
    /**
     * Undelying FI DOM parser.
     */
    private Object m_fiDOMDocumentParser = null;
    
    /**
     * Underlying FI DOM serializer.
     */
    private Object m_fiDOMDocumentSerializer = null;
    
    private EfficientStreamingTransformer() {
    }

    private void materialize() throws TransformerException {
        if (m_realTransformer == null) {
            m_realTransformer = transformerFactory.newTransformer(); 
        }
    }

    public void clearParameters() {
        if (m_realTransformer != null)
            m_realTransformer.clearParameters();
    }

    public javax.xml.transform.ErrorListener getErrorListener() {
        try {
            materialize();
            return m_realTransformer.getErrorListener();
        } catch (TransformerException e) {
            // will be caught later
        }
        return null;
    }

    public java.util.Properties getOutputProperties() {
        try {
            materialize();
            return m_realTransformer.getOutputProperties();
        } catch (TransformerException e) {
            // will be caught later
        }
        return null;
    }

    public String getOutputProperty(String str)
        throws java.lang.IllegalArgumentException {
        try {
            materialize();
            return m_realTransformer.getOutputProperty(str);
        } catch (TransformerException e) {
            // will be caught later
        }
        return null;
    }

    public Object getParameter(String str) {
        try {
            materialize();
            return m_realTransformer.getParameter(str);
        } catch (TransformerException e) {
            // will be caught later
        }
        return null;
    }

    public javax.xml.transform.URIResolver getURIResolver() {
        try {
            materialize();
            return m_realTransformer.getURIResolver();
        } catch (TransformerException e) {
            // will be caught later
        }
        return null;
    }

    public void setErrorListener(
        javax.xml.transform.ErrorListener errorListener)
        throws java.lang.IllegalArgumentException {
        try {
            materialize();
            m_realTransformer.setErrorListener(errorListener);
        } catch (TransformerException e) {
            // will be caught later
        }
    }

    public void setOutputProperties(java.util.Properties properties)
        throws java.lang.IllegalArgumentException {
        try {
            materialize();
            m_realTransformer.setOutputProperties(properties);
        } catch (TransformerException e) {
            // will be caught later
        }
    }

    public void setOutputProperty(String str, String str1)
        throws java.lang.IllegalArgumentException {
        try {
            materialize();
            m_realTransformer.setOutputProperty(str, str1);
        } catch (TransformerException e) {
            // will be caught later
        }
    }

    public void setParameter(String str, Object obj) {
        try {
            materialize();
            m_realTransformer.setParameter(str, obj);
        } catch (TransformerException e) {
            // will be caught later
        }
    }

    public void setURIResolver(javax.xml.transform.URIResolver uRIResolver) {
        try {
            materialize();
            m_realTransformer.setURIResolver(uRIResolver);
        } catch (TransformerException e) {
            // will be caught later
        }
    }

    private InputStream getInputStreamFromSource(StreamSource s)
        throws TransformerException {

        InputStream stream = s.getInputStream();
        if (stream != null)
            return stream;

        if (s.getReader() != null)
            return null;

        String systemId = s.getSystemId();
        if (systemId != null) {
            try {
                String fileURL = systemId;

                if (systemId.startsWith("file:///"))
                {
                    /*
                     systemId is:
                     file:///<drive>:/some/path/file.xml
                     or
                     file:///some/path/file.xml
                    */

                    String absolutePath = systemId.substring(7);
                    /*
                     /<drive>:/some/path/file.xml
                     or
                     /some/path/file.xml
                    */

                    boolean hasDriveDesignator = absolutePath.indexOf(":") > 0;
                    if (hasDriveDesignator) {
                      String driveDesignatedPath = absolutePath.substring(1);
                      /*
                      <drive>:/some/path/file.xml */
                      fileURL = driveDesignatedPath;
                    }
                    else {
                      /*
                      /some/path/file.xml */
                      fileURL = absolutePath;
                    }
                }
                return new FileInputStream(fileURL);
            } catch (IOException e) {
                throw new TransformerException(e.toString());
            }
        }

        throw new TransformerException("Unexpected StreamSource object");
    }

    //------------------------------------------------------------------------

    public void transform(
        javax.xml.transform.Source source,
        javax.xml.transform.Result result)
        throws javax.xml.transform.TransformerException 
    {
        // StreamSource -> StreamResult
        if ((source instanceof StreamSource)
            && (result instanceof StreamResult)) {
            try {
                StreamSource streamSource = (StreamSource) source;
                InputStream is = getInputStreamFromSource(streamSource);

                OutputStream os = ((StreamResult) result).getOutputStream();
                if (os == null)
                    // TODO: We might want to fix this if it were to be used beyond
                    // XmlDataContentHandler that we know uses only OutputStream
                    throw new TransformerException("Unexpected StreamResult object contains null OutputStream");

                if (is != null) {
                    if (is.markSupported())
                        is.mark(Integer.MAX_VALUE);
                    int num;
                    byte[] b = new byte[8192];
                    while ((num = is.read(b)) != -1) {
                        os.write(b, 0, num);
                    }
                    if (is.markSupported())
                        is.reset();
                    return;
                }

                Reader reader = streamSource.getReader();
                if (reader != null) {

                    if (reader.markSupported())
                        reader.mark(Integer.MAX_VALUE);

                    PushbackReader pushbackReader = new PushbackReader(reader, 4096); 
                    //some size to unread <?xml ....?>
                    XMLDeclarationParser ev =
                        new XMLDeclarationParser(pushbackReader);
                    try {
                        ev.parse();
                    } catch (Exception ex) {
                        throw new TransformerException(
                            "Unable to run the JAXP transformer on a stream "
                                + ex.getMessage());
                    }
                    Writer writer =
                        new OutputStreamWriter(os /*, ev.getEncoding()*/);
                    ev.writeTo(writer);		// doesn't write any, if no header

                    int num;
                    char[] ac = new char[8192];
                    while ((num = pushbackReader.read(ac)) != -1) {
                        writer.write(ac, 0, num);
                    }
                    writer.flush();

                    if (reader.markSupported())
                        reader.reset();
                    return;
                } 
            } catch (IOException e) {
                e.printStackTrace();
                throw new TransformerException(e.toString());
            }

            throw new TransformerException("Unexpected StreamSource object");
        }
        // FastInfosetSource -> DOMResult
        else if (FastInfosetReflection.isFastInfosetSource(source)
                && (result instanceof DOMResult)) 
        {
            try {
                // Use reflection to avoid a static dep with FI
                if (m_fiDOMDocumentParser == null) {
                    m_fiDOMDocumentParser = FastInfosetReflection.DOMDocumentParser_new();
                }

                // m_fiDOMDocumentParser.parse(document, source.getInputStream())
                FastInfosetReflection.DOMDocumentParser_parse(
                    m_fiDOMDocumentParser, 
                    (Document) ((DOMResult) result).getNode(),
                    FastInfosetReflection.FastInfosetSource_getInputStream(source));

                // We're done!
                return;           
            }
            catch (Exception e) {
                throw new TransformerException(e);
            }
        }
        // DOMSource -> FastInfosetResult
        else if ((source instanceof DOMSource)
                && FastInfosetReflection.isFastInfosetResult(result)) 
        {
            try {
                // Use reflection to avoid a static dep with FI
                if (m_fiDOMDocumentSerializer == null) {
                    m_fiDOMDocumentSerializer = FastInfosetReflection.DOMDocumentSerializer_new();
                }

                // m_fiDOMDocumentSerializer.setOutputStream(result.getOutputStream())
                FastInfosetReflection.DOMDocumentSerializer_setOutputStream(
                    m_fiDOMDocumentSerializer, 
                    FastInfosetReflection.FastInfosetResult_getOutputStream(result));

                // m_fiDOMDocumentSerializer.serialize(node)
                FastInfosetReflection.DOMDocumentSerializer_serialize(
                    m_fiDOMDocumentSerializer, 
                    ((DOMSource) source).getNode());

                // We're done!
                return;                  
            }
            catch (Exception e) {
                throw new TransformerException(e);
            }
        }

        // All other cases -- use transformer object
        
        materialize();
        m_realTransformer.transform(source, result);
    }

    /**
     * Threadlocal to hold a Transformer instance for this thread.
     */
    private static ThreadLocal effTransformer = new ThreadLocal(); 
    
    /**
     * Return Transformer instance for this thread, allocating a new one if 
     * necessary. Note that this method does not clear global parameters, 
     * properties or any other data set on a previously used transformer.
     */
    public static Transformer newTransformer() {
        Transformer tt = (Transformer) effTransformer.get();
        if (tt == null) {
            effTransformer.set(tt = new EfficientStreamingTransformer());
        }       
        return tt;
    }

}
