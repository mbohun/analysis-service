<?xml version="1.0" encoding="UTF-8"?><sld:UserStyle xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml">
  <sld:Name>raster</sld:Name>
  <sld:Title>A very simple color map</sld:Title>
  <sld:Abstract>A very basic color map</sld:Abstract>
  <sld:FeatureTypeStyle>
    <sld:Name>name</sld:Name>
    <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
    <sld:Rule>
      <sld:RasterSymbolizer>
        <sld:Geometry>
          <ogc:PropertyName>geom</ogc:PropertyName>
        </sld:Geometry>
        <sld:ChannelSelection>
          <sld:GrayChannel>
            <sld:SourceChannelName>1</sld:SourceChannelName>
          </sld:GrayChannel>
        </sld:ChannelSelection>
        <sld:ColorMap>
          <sld:ColorMapEntry color="#ffffff" opacity="0" quantity="-9999"/>
          <sld:ColorMapEntry color="#002DD0" quantity="0.0" label="0.0 "/>
          <sld:ColorMapEntry color="#008C73" quantity="4.0606384"/>
          <sld:ColorMapEntry color="#00B944" quantity="7.2949066"/>
          <sld:ColorMapEntry color="#00E716" quantity="10.494217"/>
          <sld:ColorMapEntry color="#A0FF00" quantity="13.946724"/>
          <sld:ColorMapEntry color="#FFFF00" quantity="17.647339"/>
          <sld:ColorMapEntry color="#FFC814" quantity="22.172928"/>
          <sld:ColorMapEntry color="#FFA000" quantity="28.624817"/>
          <sld:ColorMapEntry color="#FF5B00" quantity="37.75006"/>
          <sld:ColorMapEntry color="#FF0000" quantity="75.77716" label="75.77716 "/>
        </sld:ColorMap>
      </sld:RasterSymbolizer>
    </sld:Rule>
  </sld:FeatureTypeStyle>
</sld:UserStyle>