package org.icatproject.icat_oaipmh;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

public class MetadataFormat {

    private String metadataXslt;
    private String metadataNamespace;
    private String metadataSchema;

    private Templates template;

    public MetadataFormat(String metadataXslt, String metadataNamespace, String metadataSchema, boolean responseDebug)
            throws FileNotFoundException, SecurityException, TransformerConfigurationException {
        this.metadataXslt = metadataXslt;
        this.metadataNamespace = metadataNamespace;
        this.metadataSchema = metadataSchema;

        if (responseDebug) {
            this.template = null;
        } else {
            TransformerFactory factory = TransformerFactory.newInstance();
            StreamSource xsl = new StreamSource(new FileInputStream(metadataXslt));
            this.template = factory.newTemplates(xsl);
        }
    }

    public String getMetadataXslt() {
        return metadataXslt;
    }

    public String getMetadataNamespace() {
        return metadataNamespace;
    }

    public String getMetadataSchema() {
        return metadataSchema;
    }

    public Templates getTemplate() {
        return template;
    }
}