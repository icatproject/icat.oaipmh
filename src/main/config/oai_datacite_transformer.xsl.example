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
      <oai_datacite xmlns="http://schema.datacite.org/oai/oai-1.1/" 
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schema.datacite.org/oai/oai-1.1/ http://schema.datacite.org/oai/oai-1.1/oai.xsd">

        <schemaVersion>4.3</schemaVersion>
        <datacentreSymbol>EI</datacentreSymbol>

        <payload>
          <resource xmlns="http://datacite.org/schema/kernel-4" 
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.3/metadata.xsd">

            <identifier identifierType="DOI">
              <xsl:if test="doi">
                <xsl:value-of select="doi"/>
              </xsl:if>
              <xsl:if test="not(doi)">
                <xsl:text>:unav</xsl:text>
              </xsl:if>
            </identifier>

            <creators>
              <xsl:if test="investigationUsers/instance">
                <xsl:for-each select="investigationUsers/instance/user">
                  <creator>
                    <creatorName nameType="Personal">
                      <xsl:if test="fullName">
                        <xsl:value-of select="fullName"/>
                      </xsl:if>
                      <xsl:if test="not(fullName)">
                        <xsl:text>:null</xsl:text>
                      </xsl:if>
                    </creatorName>
                    <xsl:if test="givenName">
                      <givenName>
                        <xsl:value-of select="givenName"/>
                      </givenName>
                    </xsl:if>
                    <xsl:if test="familyName">
                      <familyName>
                        <xsl:value-of select="familyName"/>
                      </familyName>
                    </xsl:if>
                    <xsl:if test="orcidId">
                      <nameIdentifier schemeURI="https://orcid.org/" nameIdentifierScheme="ORCID">
                        <xsl:value-of select="orcidId"/>
                      </nameIdentifier>
                    </xsl:if>
                    <xsl:if test="affiliation">
                      <affiliation>
                        <xsl:value-of select="affiliation"/>
                      </affiliation>
                    </xsl:if>
                  </creator>
                </xsl:for-each>
              </xsl:if>
              <xsl:if test="not(investigationUsers/instance)">
                <creator>
                  <creatorName nameType="Personal">:null</creatorName>
                </creator>
              </xsl:if>
            </creators>

            <titles>
              <title>
                <xsl:if test="title">
                  <xsl:value-of select="title"/>
                </xsl:if>
                <xsl:if test="not(title)">
                  <xsl:text>:null</xsl:text>
                </xsl:if>
              </title>
            </titles>

            <publisher>Example Institute</publisher>

            <xsl:if test="releaseDate">
              <publicationYear>
                <xsl:value-of select="substring-before(releaseDate, '-')"/>
              </publicationYear>
            </xsl:if>
            <xsl:if test="not(releaseDate)">
              <publicationYear>
                <xsl:text>:unav</xsl:text>
              </publicationYear>
            </xsl:if>

            <xsl:if test="keywords/instance">
              <subjects>
                <xsl:for-each select="keywords/instance">
                  <subject>
                    <xsl:value-of select="name"/>
                  </subject>
                </xsl:for-each>
              </subjects>
            </xsl:if>

            <xsl:if test="startDate and endDate">
              <dates>
                <date dateType="Collected">
                  <xsl:value-of select="startDate"/>
                  <xsl:text>/</xsl:text>
                  <xsl:value-of select="endDate"/>
                </date>
                <date dateType="Accepted">
                  <xsl:value-of select="endDate"/>
                </date>
              </dates>
            </xsl:if>
            <xsl:if test="startDate and not(endDate)">
              <dates>
                <date dateType="Collected">
                  <xsl:value-of select="startDate"/>
                </date>
              </dates>
            </xsl:if>
            <xsl:if test="not(startDate) and endDate">
              <dates>
                <date dateType="Accepted">
                  <xsl:value-of select="endDate"/>
                </date>
              </dates>
            </xsl:if>

            <resourceType resourceTypeGeneral="Dataset">
              <xsl:text>Dataset</xsl:text>
            </resourceType>

            <rightsList>
              <rights rightsURI="https://creativecommons.org/publicdomain/zero/1.0/" rightsIdentifier="CC0-1.0">
                <xsl:text>CC0 Public Domain Dedication</xsl:text>
              </rights>
            </rightsList>

            <xsl:if test="summary">
              <descriptions>
                <description descriptionType="Abstract">
                  <xsl:value-of select="summary"/>
                </description>
              </descriptions>
            </xsl:if>

          </resource>
        </payload>

      </oai_datacite>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space()"/>
  </xsl:template>

</xsl:stylesheet>
