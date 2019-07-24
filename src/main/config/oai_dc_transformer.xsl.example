<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:key name="unique_user" match="studyInvestigations/instance/investigation/investigationUsers/instance/user" use="fullName"/>
  <xsl:key name="unique_keyword" match="studyInvestigations/instance/investigation/keywords/instance" use="name"/>

  <xsl:template match="metadata">
    <xsl:copy>
      <oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" 
        xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">

        <xsl:if test="dataConfigurationIdentifier = 'inv'">

          <xsl:for-each select="investigationUsers/instance/user">
            <dc:creator>
              <xsl:value-of select="fullName"/>
            </dc:creator>
          </xsl:for-each>

          <xsl:if test="startDate and endDate">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="startDate"/>
              <xsl:text>/</xsl:text>
              <xsl:value-of select="endDate"/>
            </dc:date>
          </xsl:if>
          <xsl:if test="startDate and not(endDate)">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="startDate"/>
            </dc:date>
          </xsl:if>
          <xsl:if test="not(startDate) and endDate">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="endDate"/>
            </dc:date>
          </xsl:if>

          <xsl:for-each select="releaseDate">
            <dc:date>
              <xsl:text>Available: </xsl:text>
              <xsl:value-of select="."/>
            </dc:date>
          </xsl:for-each>

          <xsl:for-each select="summary">
            <dc:description>
              <xsl:value-of select="."/>
            </dc:description>
          </xsl:for-each>

          <dc:format>Data from large facility measurement</dc:format>

          <xsl:for-each select="doi">
            <dc:identifier>
              <xsl:text>doi:</xsl:text>
              <xsl:value-of select="."/>
            </dc:identifier>
          </xsl:for-each>

          <dc:publisher>Example Institute</dc:publisher>

          <dc:rights>CC0 Public Domain Dedication (CC0-1.0)</dc:rights>

          <xsl:for-each select="keywords/instance">
            <dc:subject>
              <xsl:value-of select="name"/>
            </dc:subject>
          </xsl:for-each>

          <xsl:for-each select="title">
            <dc:title>
              <xsl:value-of select="."/>
            </dc:title>
          </xsl:for-each>

          <dc:type>Collection</dc:type>

        </xsl:if>

        <xsl:if test="dataConfigurationIdentifier = 'stud'">

          <xsl:for-each select="studyInvestigations/instance/investigation/investigationUsers/instance/user[generate-id() = generate-id(key('unique_user', fullName)[1])]">
            <dc:creator>
              <xsl:value-of select="fullName"/>
            </dc:creator>
          </xsl:for-each>

          <xsl:if test="startDate and endDate">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="startDate"/>
              <xsl:text>/</xsl:text>
              <xsl:value-of select="endDate"/>
            </dc:date>
          </xsl:if>
          <xsl:if test="startDate and not(endDate)">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="startDate"/>
            </dc:date>
          </xsl:if>
          <xsl:if test="not(startDate) and endDate">
            <dc:date>
              <xsl:text>Created: </xsl:text>
              <xsl:value-of select="endDate"/>
            </dc:date>
          </xsl:if>

          <xsl:for-each select="description">
            <dc:description>
              <xsl:value-of select="."/>
            </dc:description>
          </xsl:for-each>

          <dc:format>Data from large facility measurement</dc:format>

          <xsl:for-each select="pid">
            <dc:identifier>
              <xsl:text>doi:</xsl:text>
              <xsl:value-of select="."/>
            </dc:identifier>
          </xsl:for-each>

          <dc:publisher>Example Institute</dc:publisher>

          <dc:rights>CC0 Public Domain Dedication (CC0-1.0)</dc:rights>

          <xsl:for-each select="studyInvestigations/instance/investigation/keywords/instance[generate-id() = generate-id(key('unique_keyword', name)[1])]">
            <dc:subject>
              <xsl:value-of select="name"/>
            </dc:subject>
          </xsl:for-each>

          <xsl:for-each select="title">
            <dc:title>
              <xsl:value-of select="."/>
            </dc:title>
          </xsl:for-each>

          <dc:type>Collection</dc:type>

        </xsl:if>

      </oai_dc:dc>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space()"/>
  </xsl:template>

</xsl:stylesheet>
