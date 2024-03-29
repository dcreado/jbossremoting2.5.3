<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://nwalsh.com/xsl/documentation/1.0"
                exclude-result-prefixes="doc"
                version='1.0'>

<!-- ********************************************************************
     $Id: subtitles.xsl 1341 2006-08-02 16:35:13Z telrod $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<!-- subtitle markup -->

<doc:mode mode="subtitle.markup" xmlns="">
<refpurpose>Provides access to element subtitles</refpurpose>
<refdescription>
<para>Processing an element in the
<literal role="mode">subtitle.markup</literal> mode produces the
subtitle of the element.
</para>
</refdescription>
</doc:mode>

<xsl:template match="*" mode="subtitle.markup">
  <xsl:message>
    <xsl:text>Request for subtitle of unexpected element: </xsl:text>
    <xsl:value-of select="name(.)"/>
  </xsl:message>
  <xsl:text>???SUBTITLE???</xsl:text>
</xsl:template>

<xsl:template match="subtitle" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="set" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(setinfo/subtitle|info/subtitle|subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="book" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(bookinfo/subtitle|info/subtitle|subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="part" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(partinfo/subtitle
                                |docinfo/subtitle
                                |info/subtitle
                                |subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="preface|chapter|appendix" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(docinfo/subtitle
                                |info/subtitle
                                |prefaceinfo/subtitle
                                |chapterinfo/subtitle
                                |appendixinfo/subtitle
                                |subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="article" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(artheader/subtitle
                                |articleinfo/subtitle
                                |info/subtitle
                                |subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="dedication|colophon" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="subtitle"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="reference" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(referenceinfo/subtitle
                                |docinfo/subtitle
                                |info/subtitle
                                |subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="refentry" mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(refentryinfo/subtitle
                                |info/subtitle
                                |docinfo/subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="section
                     |sect1|sect2|sect3|sect4|sect5
                     |refsect1|refsect2|refsect3
                     |simplesect"
              mode="subtitle.markup">
  <xsl:param name="allow-anchors" select="'0'"/>
  <xsl:apply-templates select="(info/subtitle
                                |sectioninfo/subtitle
                                |sect1info/subtitle
                                |sect2info/subtitle
                                |sect3info/subtitle
                                |sect4info/subtitle
                                |sect5info/subtitle
                                |refsect1info/subtitle
                                |refsect2info/subtitle
                                |refsect3info/subtitle
                                |subtitle)[1]"
                       mode="subtitle.markup">
    <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
  </xsl:apply-templates>
</xsl:template>

</xsl:stylesheet>

