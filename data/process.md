* import shapefile to mapshaper
* open console

For outline of state:
* select main shapefile
* `dissolve` to get border/outline of state
* `lines` to convert to polyline

For interior
* Select main shapefile
* `innerlines`
* Click simplify, DP algo, prevent shape removal, 20-5%, click repair

Join outline + interior
`merge-layers name=merged_layer target=outline_layer,inner_layer`

Join with data
`join source=original_shapefile_layer target=merged_layer point-method unjoined
unmatched`

then we have to identify the unjoined/unmatched layers and put their data in by
hand


QGIS
create virtual layer -> select "OID_", count(*) from <layer> group by "OID_";
will determine polygons with duplicate IDs
merge these so 1 precinct doesn't have multiple distinct polygons

