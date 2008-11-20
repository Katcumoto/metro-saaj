/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
/*
 * 
 * 
 * 
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.xml.messaging.saaj.soap;

import com.sun.xml.messaging.saaj.SOAPExceptionImpl;

import com.sun.xml.messaging.saaj.packaging.mime.util.ASCIIUtility;

import com.sun.xml.messaging.saaj.packaging.mime.Header;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimePartDataSource;
import com.sun.xml.messaging.saaj.packaging.mime.internet.InternetHeaders;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeBodyPart;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeUtility;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.messaging.saaj.util.LogDomainConstants;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.MailcapCommandMap;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.*;
import javax.xml.soap.*;

/**
 * Implementation of attachments.
 *
 * @author Anil Vijendran (akv@eng.sun.com)
 */
public class AttachmentPartImpl extends AttachmentPart {

    protected static final Logger log =
        Logger.getLogger(LogDomainConstants.SOAP_DOMAIN,
                         "com.sun.xml.messaging.saaj.soap.LocalStrings");

    static {
        try {
            CommandMap map = CommandMap.getDefaultCommandMap();
            if (map instanceof MailcapCommandMap) {
                MailcapCommandMap mailMap = (MailcapCommandMap) map;
                String hndlrStr = ";;x-java-content-handler=";
                mailMap.addMailcap(
                    "text/xml"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.XmlDataContentHandler");
                mailMap.addMailcap(
                    "application/xml"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.XmlDataContentHandler");
                mailMap.addMailcap(
                    "application/fastinfoset"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.FastInfosetDataContentHandler");
                /* Image DataContentHandler handles all image types
                mailMap.addMailcap(
                    "image/jpeg"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.JpegDataContentHandler");
                mailMap.addMailcap(
                    "image/gif"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.GifDataContentHandler"); */
                /*mailMap.addMailcap(
                    "multipart/*"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.MultipartDataContentHandler");*/
                mailMap.addMailcap(
                    "image/*"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.ImageDataContentHandler");
                mailMap.addMailcap(
                    "text/plain"
                        + hndlrStr
                        + "com.sun.xml.messaging.saaj.soap.StringDataContentHandler");
            } else {
                throw new SOAPExceptionImpl("Default CommandMap is not a MailcapCommandMap");
            }
        } catch (Throwable t) {
            log.log(
                Level.SEVERE,
                "SAAJ0508.soap.cannot.register.handlers",
                t);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t.getLocalizedMessage());
            }
        }
    };

    private final MimeHeaders headers;
    private MimeBodyPart rawContent = null;
    private DataHandler dataHandler = null;

    public AttachmentPartImpl() {
        headers = new MimeHeaders();
    }

    public int getSize() throws SOAPException {
        byte[] bytes;

        if ((rawContent == null) && (dataHandler == null))
            return 0;
 
        if (rawContent != null) {
            try {
                return rawContent.getSize();
            } catch (Exception ex) {
                log.log(
                    Level.SEVERE,
                    "SAAJ0573.soap.attachment.getrawbytes.ioexception",
                    new String[] { ex.getLocalizedMessage()});
                throw new SOAPExceptionImpl("Raw InputStream Error: " + ex);
            }
        } else {
            ByteOutputStream bout = new ByteOutputStream();
            try {
                dataHandler.writeTo(bout);
            } catch (IOException ex) {
                log.log(
                    Level.SEVERE,
                    "SAAJ0501.soap.data.handler.err",
                    new String[] { ex.getLocalizedMessage()});
                throw new SOAPExceptionImpl("Data handler error: " + ex);
            }
            bytes = bout.getBytes();
            if (bytes != null)
                return bytes.length;
        } 
        return -1;
    }

    public void clearContent() {
        dataHandler = null;
        rawContent = null;
    }

    public Object getContent() throws SOAPException {
        try {
            if (dataHandler != null)  {
                return getDataHandler().getContent();
            } else if (rawContent != null) {
                return rawContent.getContent();
            } else {
                log.severe("SAAJ0572.soap.no.content.for.attachment");
                throw new SOAPExceptionImpl("No data handler/content associated with this attachment");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "SAAJ0575.soap.attachment.getcontent.exception", ex);
            throw new SOAPExceptionImpl(ex.getLocalizedMessage());
        }
    }

    public void setContent(Object object, String contentType)
        throws IllegalArgumentException {
        DataHandler dh = new DataHandler(object, contentType);

        setDataHandler(dh);
    }


    public DataHandler getDataHandler() throws SOAPException {
        if (dataHandler == null) {
            if (rawContent != null) {
                return new DataHandler(new MimePartDataSource(rawContent));
            }
            log.severe("SAAJ0502.soap.no.handler.for.attachment");
            throw new SOAPExceptionImpl("No data handler associated with this attachment");
        }
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler)
        throws IllegalArgumentException {
        if (dataHandler == null) {
            log.severe("SAAJ0503.soap.no.null.to.dataHandler");
            throw new IllegalArgumentException("Null dataHandler argument to setDataHandler");
        }
        this.dataHandler = dataHandler;
        rawContent = null;

        log.log( 
            Level.FINE,
            "SAAJ0580.soap.set.Content-Type",
            new String[] { dataHandler.getContentType()});
        setMimeHeader("Content-Type", dataHandler.getContentType());
    }

    public void removeAllMimeHeaders() {
        headers.removeAllHeaders();
    }

    public void removeMimeHeader(String header) {
        headers.removeHeader(header);
    }

    public String[] getMimeHeader(String name) {
        return headers.getHeader(name);
    }

    public void setMimeHeader(String name, String value) {
        headers.setHeader(name, value);
    }

    public void addMimeHeader(String name, String value) {
        headers.addHeader(name, value);
    }

    public Iterator getAllMimeHeaders() {
        return headers.getAllHeaders();
    }

    public Iterator getMatchingMimeHeaders(String[] names) {
        return headers.getMatchingHeaders(names);
    }

    public Iterator getNonMatchingMimeHeaders(String[] names) {
        return headers.getNonMatchingHeaders(names);
    }

    boolean hasAllHeaders(MimeHeaders hdrs) {
        if (hdrs != null) {
            Iterator i = hdrs.getAllHeaders();
            while (i.hasNext()) {
                MimeHeader hdr = (MimeHeader) i.next();
                String[] values = headers.getHeader(hdr.getName());
                boolean found = false;

                if (values != null) {
                    for (int j = 0; j < values.length; j++)
                        if (hdr.getValue().equalsIgnoreCase(values[j])) {
                            found = true;
                            break;
                        }
                }

                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }

    MimeBodyPart getMimePart() throws SOAPException {
        try {
            if (rawContent != null) {
                copyMimeHeaders(headers, rawContent);
                return rawContent;
            } 

            MimeBodyPart envelope = new MimeBodyPart();

            envelope.setDataHandler(dataHandler);
            copyMimeHeaders(headers, envelope);

            return envelope;
        } catch (Exception ex) {
            log.severe("SAAJ0504.soap.cannot.externalize.attachment");
            throw new SOAPExceptionImpl("Unable to externalize attachment", ex);
        }
    }

    public static void copyMimeHeaders(MimeHeaders headers, MimeBodyPart mbp)
        throws SOAPException {

        Iterator i = headers.getAllHeaders();

        while (i.hasNext())
            try {
                MimeHeader mh = (MimeHeader) i.next();

                mbp.setHeader(mh.getName(), mh.getValue());
            } catch (Exception ex) {
                log.severe("SAAJ0505.soap.cannot.copy.mime.hdr");
                throw new SOAPExceptionImpl("Unable to copy MIME header", ex);
            }
    }

    public static void copyMimeHeaders(MimeBodyPart mbp, AttachmentPartImpl ap)
        throws SOAPException {
        try {
            List hdr = mbp.getAllHeaders();
            int sz = hdr.size();
            for( int i=0; i<sz; i++ ) {
                Header h = (Header)hdr.get(i);
                if(h.getName().equalsIgnoreCase("Content-Type"))
                    continue;   // skip
                ap.addMimeHeader(h.getName(), h.getValue());
            }
        } catch (Exception ex) {
            log.severe("SAAJ0506.soap.cannot.copy.mime.hdrs.into.attachment");
            throw new SOAPExceptionImpl(
                "Unable to copy MIME headers into attachment",
                ex);
        }
    }
 
    public  void setBase64Content(InputStream content, String contentType) 
        throws SOAPException {
        dataHandler = null;
        try {
            InputStream decoded = MimeUtility.decode(content, "base64");
            InternetHeaders hdrs = new InternetHeaders();
            hdrs.setHeader("Content-Type", contentType);
            //TODO: reading the entire attachment here is ineffcient. Somehow the MimeBodyPart
            // Ctor with inputStream causes problems based on the InputStream 
            // has markSupported()==true
            ByteOutputStream bos = new ByteOutputStream();
            bos.write(decoded);
            rawContent = new MimeBodyPart(hdrs, bos.getBytes(), bos.getCount());
            setMimeHeader("Content-Type", contentType);
        } catch (Exception e) {
            log.log(Level.SEVERE, "SAAJ0578.soap.attachment.setbase64content.exception", e);
            throw new SOAPExceptionImpl(e.getLocalizedMessage());
        } 
    }

    public  InputStream getBase64Content() throws SOAPException {
        InputStream stream;
        if (rawContent != null) {
            try {
                 stream = rawContent.getInputStream();
            } catch (Exception e) {
                log.log(Level.SEVERE,"SAAJ0579.soap.attachment.getbase64content.exception", e);
                throw new SOAPExceptionImpl(e.getLocalizedMessage());
            }
        } else if (dataHandler != null) {
            try {
                stream = dataHandler.getInputStream();
            } catch (IOException e) {
                log.severe("SAAJ0574.soap.attachment.datahandler.ioexception");
                throw new SOAPExceptionImpl("DataHandler error" + e);
            }
        } else {
            log.severe("SAAJ0572.soap.no.content.for.attachment");
            throw new SOAPExceptionImpl("No data handler/content associated with this attachment");
        }

        //TODO: Write a BASE64EncoderInputStream instead, 
        // this code below is inefficient
        // where we are trying to read the whole attachment first
        int len;
        int size = 1024;
        byte [] buf;
        if (stream != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(size);
                //TODO: try and optimize this on the same lines as 
                // ByteOutputStream : to eliminate the temp buffer here
                OutputStream ret = MimeUtility.encode(bos, "base64");
                buf = new byte[size];
                while ((len = stream.read(buf, 0, size)) != -1) {
                    ret.write(buf, 0, len);
                }
                ret.flush();
                buf = bos.toByteArray(); 
                return new ByteArrayInputStream(buf);
            } catch (Exception e) {
                // throw new SOAPException
                log.log(Level.SEVERE,"SAAJ0579.soap.attachment.getbase64content.exception", e);
                throw new SOAPExceptionImpl(e.getLocalizedMessage());
            }
        } else {
          //throw  new SOAPException
          log.log(Level.SEVERE,"SAAJ0572.soap.no.content.for.attachment");
          throw new SOAPExceptionImpl("No data handler/content associated with this attachment");
        }
    }

    public void setRawContent(InputStream content, String contentType) 
        throws SOAPException {
        dataHandler = null;
        try {
            InternetHeaders hdrs = new InternetHeaders();
            hdrs.setHeader("Content-Type", contentType);
            //TODO: reading the entire attachment here is ineffcient. Somehow the MimeBodyPart
            // Ctor with inputStream causes problems based on whether the InputStream has 
            // markSupported()==true or false
            ByteOutputStream bos = new ByteOutputStream();
            bos.write(content);
            rawContent = new MimeBodyPart(hdrs, bos.getBytes(), bos.getCount());
            setMimeHeader("Content-Type", contentType);
        } catch (Exception e) {
            log.log(Level.SEVERE, "SAAJ0576.soap.attachment.setrawcontent.exception", e);
            throw new SOAPExceptionImpl(e.getLocalizedMessage());
        }
    }

   /*
    public void setRawContentBytes(byte[] content, String contentType) 
        throws SOAPException {
        if (content == null) {
            throw new SOAPExceptionImpl("Null content passed to setRawContentBytes");
        }
        dataHandler = null;
        try {
            InternetHeaders hdrs = new InternetHeaders();
            hdrs.setHeader("Content-Type", contentType);
            rawContent = new MimeBodyPart(hdrs, content, content.length);
            setMimeHeader("Content-Type", contentType);
        } catch (Exception e) {
            log.log(Level.SEVERE, "SAAJ0576.soap.attachment.setrawcontent.exception", e);
            throw new SOAPExceptionImpl(e.getLocalizedMessage());
        }
    } */

    public void setRawContentBytes(
        byte[] content, int off, int len, String contentType) 
        throws SOAPException {
        if (content == null) {
            throw new SOAPExceptionImpl("Null content passed to setRawContentBytes");
        }
        dataHandler = null;
        try {
            InternetHeaders hdrs = new InternetHeaders();
            hdrs.setHeader("Content-Type", contentType);
            rawContent = new MimeBodyPart(hdrs, content, off, len);
            setMimeHeader("Content-Type", contentType);
        } catch (Exception e) {
            log.log(Level.SEVERE, 
                "SAAJ0576.soap.attachment.setrawcontent.exception", e);
            throw new SOAPExceptionImpl(e.getLocalizedMessage());
        }
    }

    public  InputStream getRawContent() throws SOAPException {
        if (rawContent != null) {
            try {
                return rawContent.getInputStream();
            } catch (Exception e) {
                log.log(Level.SEVERE,"SAAJ0577.soap.attachment.getrawcontent.exception", e);
                throw new SOAPExceptionImpl(e.getLocalizedMessage());
            }
        } else if (dataHandler != null) {
            try {
                return dataHandler.getInputStream();
            } catch (IOException e) {
                log.severe("SAAJ0574.soap.attachment.datahandler.ioexception"); 
                throw new SOAPExceptionImpl("DataHandler error" + e);
            }
        } else {
            log.severe("SAAJ0572.soap.no.content.for.attachment");
            throw new SOAPExceptionImpl("No data handler/content associated with this attachment");
        }
    }

    //TODO: investigate size differences in mime.AttachImageTest
    public  byte[] getRawContentBytes() throws SOAPException {
        InputStream ret;
        if (rawContent != null) {
            try {
                ret = rawContent.getInputStream();
                return ASCIIUtility.getBytes(ret);
            } catch (Exception e) {
                log.log(Level.SEVERE,"SAAJ0577.soap.attachment.getrawcontent.exception", e);
                throw new SOAPExceptionImpl(e.getLocalizedMessage());
            }
        } else if (dataHandler != null) {
            try {
                ret = dataHandler.getInputStream();
                return ASCIIUtility.getBytes(ret);
            } catch (IOException e) {
                log.severe("SAAJ0574.soap.attachment.datahandler.ioexception"); 
                throw new SOAPExceptionImpl("DataHandler error" + e);
            }
        } else {
            log.severe("SAAJ0572.soap.no.content.for.attachment");
            throw new SOAPExceptionImpl("No data handler/content associated with this attachment");
        }
    }

    // attachments are equal if they are the same reference
    public boolean equals(Object o) {
        return (this == o);
    }

    public MimeHeaders getMimeHeaders() {
        return headers;
    }

}

