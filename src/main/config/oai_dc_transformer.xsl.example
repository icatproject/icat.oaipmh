<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="metadata">
    <xsl:copy>
      <oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" 
        xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">

        <xsl:for-each select="title">
          <dc:title>
            <xsl:value-of select="."/>
          </dc:title>
        </xsl:for-each>

        <xsl:for-each select="investigationUsers/instance/user">
          <dc:creator>
            <xsl:value-of select="fullName"/>
          </dc:creator>
        </xsl:for-each>

        <xsl:for-each select="summary">
          <dc:description>
            <xsl:value-of select="."/>
          </dc:description>
        </xsl:for-each>

        <dc:publisher>Example Institute</dc:publisher>

        <xsl:for-each select="releaseDate">
          <dc:date>
            <xsl:value-of select="."/>
          </dc:date>
        </xsl:for-each>

        <dc:type>Experiment Measurement Data</dc:type>

        <xsl:for-each select="doi">
          <dc:identifier>
            <xsl:text>doi:</xsl:text>
            <xsl:value-of select="."/>
          </dc:identifier>
        </xsl:for-each>

        <dc:rights>CC0 Public Domain Dedication (CC0 1.0)</dc:rights>

      </oai_dc:dc>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space()"/>
  </xsl:template>

</xsl:stylesheet>