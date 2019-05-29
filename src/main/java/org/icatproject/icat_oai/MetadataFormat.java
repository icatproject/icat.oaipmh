package org.icatproject.icat_oai;

public class MetadataFormat {

    private String metadataPrefix;
    private String metadataXslt;
    private String metadataNamespace;
    private String metadataSchema;

    public MetadataFormat(String metadataPrefix, String metadataXslt, String metadataNamespace, String metadataSchema) {
        this.metadataPrefix = metadataPrefix;
        this.metadataXslt = metadataXslt;
        this.metadataNamespace = metadataNamespace;
        this.metadataSchema = metadataSchema;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
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
}