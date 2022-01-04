<?xml version="1.0" ?>
<xsl:stylesheet version="2.0"
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
				xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" 
				xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
				xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xmlns:str="http://exslt.org/strings"
				>
  <xsl:output indent="yes" />

  <!--xsl:param name="border">solid</xsl:param-->
  <xsl:param name="border">none</xsl:param>
  <xsl:param name="myaddr-ja">〒639-1015
  奈良県大和郡山市箕山町6-27
  河野 恭也・エッコ・拓海・美琴(19)
  </xsl:param>
  <xsl:param name="myaddr-en">Kyoya, Eck, Takumi &amp; Mikoto Kawano
  6-27 Minoyama-cho
  Yamatokoriyama-shi
  Nara 639-1015, Japan</xsl:param>

  <xsl:template match="/" >
	<fo:root>
	  <fo:layout-master-set>
		<fo:simple-page-master master-name="a4" >
		  <fo:region-body/>
		</fo:simple-page-master>
		<fo:simple-page-master master-name="a4t" >
		  <fo:region-body writing-mode="tb-rl"/>
		</fo:simple-page-master>
		<fo:simple-page-master master-name="postcard"
							   page-width="100mm" page-height="148mm"
							   margin-top="0mm" margin-bottom="0mm" margin-left="0mm" margin-right="0mm"
							   >
		  <fo:region-body writing-mode="tb-rl"/>
		</fo:simple-page-master>
	  </fo:layout-master-set>

	  <!--海外向け-->
	  <xsl:for-each select="office:document-content/office:body/office:spreadsheet/table:table[2]/table:table-row[starts-with(table:table-cell[4],'1現未')]">
		<xsl:call-template name="addr-layout">
		  <xsl:with-param name="addr" select="table:table-cell[6]"/>
		  <xsl:with-param name="addr-top">25mm</xsl:with-param>
		  <xsl:with-param name="names" select="concat(',',table:table-cell[1])"/>
		  <xsl:with-param name="names-top">10mm</xsl:with-param>
		  <xsl:with-param name="title" select="''" />
		  <xsl:with-param name="myaddr" select="$myaddr-en" />
		</xsl:call-template>
	  </xsl:for-each>

	  <!--国内向け-->
	  <xsl:for-each select="office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[starts-with(table:table-cell[2],'1現未')]">
		<xsl:call-template name="addr-layout">
		  <xsl:with-param name="zipcode" select="table:table-cell[7]"/>
		  <xsl:with-param name="addr" select="table:table-cell[8]"/>
		  <xsl:with-param name="addr-top">10mm</xsl:with-param>
		  <xsl:with-param name="names" select="table:table-cell[1]" />
		  <xsl:with-param name="names-top">35mm</xsl:with-param>
		  <xsl:with-param name="title" select="' 様'"/>
		  <xsl:with-param name="myaddr" select="$myaddr-ja" />
		</xsl:call-template>
	  </xsl:for-each>

	</fo:root>
  </xsl:template>

  <xsl:template name="addr-layout">

	<xsl:param name="zipcode" />
	<xsl:param name="addr-top" />
	<xsl:param name="addr" />
	<xsl:param name="names" />
	<xsl:param name="names-top" />
	<xsl:param name="title" />
	<xsl:param name="myaddr" />

	<fo:page-sequence master-reference="postcard">
	  <fo:flow flow-name="xsl-region-body">

		<!--はがき全体-->
		<fo:block-container absolute-position="absolute" top="0mm" left="0mm" width="147mm" height="100mm" border-style="{$border}"
							border-color="yellow" >
		  <fo:block text-align="left" font-family="メイリオ">

			<!--ZIP-->
			<xsl:if test="$zipcode!=''"> 
			  <fo:block-container absolute-position="absolute" top="8mm" left="13mm" 
								  reference-orientation="90" font-size="20pt"
								  width="48mm"
								  >
				<fo:block border-style="{$border}" border-color="yellow"
						  start-indent=
"0mm" end-indent="0mm"
						  text-align-last="justify"
						  >
				  <xsl:for-each select="str:tokenize($zipcode,'')">
					<xsl:value-of select="."/> <xsl:text> </xsl:text>
				  </xsl:for-each>
				</fo:block>
			  </fo:block-container>
			</xsl:if>

			<!--住所-->
			<fo:block-container absolute-position="absolute" top="{$addr-top}" left="30mm" width="130mm" border-style="{$border}"
								border-color="yellow"> <fo:block font-size="16pt">
			  <!--「\\」で分割-->
			  <xsl:for-each select="str:split($addr, '\\')">
				<fo:block white-space-treatment="preserve" padding-start="5em">
				  <xsl:value-of select="."/>
				</fo:block>
			  </xsl:for-each>
			</fo:block> </fo:block-container>

			<!--宛名-->
			<fo:block-container absolute-position="absolute" top="{$names-top}" left="30mm" width="100mm" border-style="{$border}"
								border-color="yellow"> <fo:block font-size="20pt">
			  <!--「,」で分割した最初が「姓」、残りが「名」-->
			  <!--分割されない場合「名」扱い-->
			  <xsl:variable name="fnn" select="str:split($names, ',')"/>
			  <xsl:if test="count(str:split($names, ','))=1">
				<xsl:variable name="fnn" select="concat(',',str:split($names, ','))"/>
			  </xsl:if>
			  <xsl:variable name="fn" select="$fnn[1]"/>
			  <xsl:variable name="n" select="$fnn[position()>1]"/>

			  <fo:table table-layout="fixed" width="100%">

				<fo:table-body>
				  <xsl:for-each select="$n">
					<xsl:variable name="fnl" select="string-length($fn)"/>
					<fo:table-row>
					  <xsl:element name="fo:table-cell">
						<xsl:attribute name="width"><xsl:value-of select="$fnl"/>em</xsl:attribute>
						<fo:block border-style="{$border}" border-color="yellow">
						  <xsl:if test="position()=1">
							<xsl:value-of select="$fn"/>
						  </xsl:if>
						</fo:block>
					  </xsl:element>

					  <fo:table-cell>
						<fo:block linefeed-treatment="preserve" border-style="{$border}" border-color="yellow">
						  <xsl:value-of select="."/>
						  <xsl:value-of select="$title"/>
						</fo:block>
					  </fo:table-cell>
					</fo:table-row>
				  </xsl:for-each>
				</fo:table-body>
			  </fo:table>
			  
			</fo:block> </fo:block-container>

			<!--差出-->
			<fo:block-container absolute-position="absolute" top="75mm" left="60mm" width="80mm" border-style="{$border}"
								border-color="yellow">
			  <fo:block font-size="11pt" linefeed-treatment="preserve">
				<xsl:value-of select="$myaddr"/>
			  </fo:block>
			</fo:block-container>

		</fo:block> </fo:block-container>
	  </fo:flow>

	</fo:page-sequence>
  </xsl:template>

  <xsl:template name="tate" >
	<xsl:param name="value"/>
	<fo:block-container absolute-position="absolute" width="0mm" height="140mm" border-style="{$border}"
						border-color="yellow">
	  <fo:block text-align="left" font-family="メイリオ">
		<xsl:value-of select="$value"/>
	  </fo:block>
	</fo:block-container>
  </xsl:template>

</xsl:stylesheet>
