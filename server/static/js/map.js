// mouseover handler for a specific state
function highlightFeature(e)
{
    let layer = e.target;
    if(e.target.feature.properties.name.toUpperCase() === selectedState)
        return;
    layer.setStyle(highlightedStyle);
    if(!L.Browser.ie && !L.Browser.opera && !L.Browser.edge)
    {
        layer.bringToFront();
    }
}

function resetHighlight(e) {
    let layer = e.target;
    layer.closePopup();
    if(e.target.feature.properties.name.toUpperCase() === selectedState)
        return;
    stateBoundaries.resetStyle(e.target);
}

// user has clicked on a specific state
// this also changes the style for the previously selected layer
// back to the original, prior to changing the style of the
// newly selected layer to have no fill opacity and a black outline
function zoomToFeature(e) {
    mainMap.fitBounds(e.getBounds());
    mainMap.invalidateSize();
    selectedLayer.setStyle(stateBorderlessStyle);
    selectStateRequest();
}

// register handlers for each state
function onEachStateFeature(feature, layer) {
    layer.on({
        mouseover: highlightFeature,
        mouseout: resetHighlight,
        click: selectState
    });
    if(stateLayers.length < 3){
        stateLayers.push(layer);
    }
}

function showStates(statesData, map)
{
    stateBoundaries = L.geoJson(statesData, {
                onEachFeature: onEachStateFeature
    });
    stateBoundaries.addTo(map);
}

function showDistricts(districtData, map)
{
    districtBoundaries = L.geoJson(districtData, {
        onEachFeature: onEachFeature
    });
    districtData.addTo(map);
}

function hideDistricts(districtData) {
    districtData.remove();
}

function showPrecincts(map) {
    statePrecinctLayers[selectedState].addTo(map);
    selectedLayer.setStyle(stateBorderlessStyle);
}

function hidePrecincts(){
    statePrecinctLayers[selectedState].remove();
    for (let filter in displayedDistricts){
        if (displayedDistricts[filter] != null){
            return;
        }
    }
    selectedLayer.setStyle(selectedStyle);
}

// Sets the properties of each precinct
function onEachPrecinctFeature(feature, layer){
    precinctLayersDict[selectedState][feature.properties.GEOID10] = feature;
    layer.setStyle(precinctStyle);
    layer.on('mouseover', function () {
        this.setStyle({weight: 3, opacity: 1});
        if (!L.Browser.ie && !L.Browser.opera && !L.Browser.edge) {
            layer.bringToFront();
        }
        selectedPrecinct = feature.properties;
        let precinctInfo = document.getElementById('precinct-info')
        precinctInfo.innerHTML +=  `<b>Name:</b> <i>${selectedPrecinct.NAME10}</i><br>`
        precinctInfo.innerHTML +=  `<b>County:</b> <i>${selectedPrecinct.COUNTY}</i><br><br>`
        precinctInfo.innerHTML +=  `<b>Demographics</b> (Total | Voting Age)</p><hr>`
        precinctInfo.innerHTML +=  `<table>
                                        <tr>
                                            <td><b>African American: &nbsp; &nbsp;</b></td>
                                            <td>${selectedPrecinct.T_AA.toLocaleString()}</td>
                                            <td>|</td>
                                            <td>${selectedPrecinct.V_AA.toLocaleString()}</td>
                                        </tr>
                                        <tr>
                                            <td><b>Asian American:  </b></td>
                                            <td>${selectedPrecinct.T_AS.toLocaleString()}</td>
                                            <td>|</td>
                                            <td>${selectedPrecinct.V_AS.toLocaleString()}</td>
                                        </tr>
                                        <tr>
                                            <td><b>Native American:  </b></td>
                                            <td>${selectedPrecinct.T_AI.toLocaleString()}</td>
                                            <td>|</td>
                                            <td>${selectedPrecinct.V_AI.toLocaleString()}</td>
                                        </tr>
                                        <tr>
                                            <td><b>Hispanic:  </b></td>
                                            <td>${selectedPrecinct.T_HISP.toLocaleString()}</td>
                                            <td>|</td>
                                            <td>${selectedPrecinct.V_HISP.toLocaleString()}</td>
                                        </tr>
                                    </table>`
    });
    layer.on('mouseout', function (){
        this.setStyle({weight: 1, opacity: 0.5});
        selectedPrecinct = null;
        let precinctInfo = document.getElementById('precinct-info')
        precinctInfo.innerHTML = ''
    });
    layer.on('click', function () {
        console.log(feature.properties)
    })
}

function onEachDistrictFeature(feature, layer) {
    // layer.on('mouseover', function () {
    //     this.setStyle({weight: 6});
    // });
    // layer.on('mouseout', function (){
    //     this.setStyle({weight: 3});
    // });
}

// create the nationwide map
mainMap = L.map('mapid').setView([38.0, -99.0], 5);
L.tileLayer('https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token={accessToken}', {
                attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
                maxZoom: 18,
                id: 'mapbox/light-v10',
                tileSize: 512,
                zoomOffset: -1,
                accessToken: 'pk.eyJ1Ijoic3luZGlyIiwiYSI6ImNrZmVlazMzMzAxNm0ycnB6Nno1MXg0ZG4ifQ.SX_yeaOdxwBJcY_appo1Zg'
            }).addTo(mainMap);

// Create and add Map controllers
const mapFilters = L.control({position: 'topright'});
mapFilters.onAdd = function (map) {
    let div = L.DomUtil.create('div');
    div.style.width = "190px";
    div.innerHTML = `
                    <div class="leaflet-control-layers leaflet-control" id="map_control_toggle" onmouseenter="mapControllerCollapseHandler()">
                        <a class="leaflet-control-layers-toggle" href="#" title="Layers"></a>
                    </div>
                    <div class="leaflet-control-layers leaflet-control-layers-expanded" id="map_filters_control" onmouseleave="mapControllerCollapseHandler()" style="display: none;">
                        <div class="row">
                            <h5 style="margin-left:15px; font-weight:bolder;">Filters</h5>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 45px;position: absolute;"><input type="checkbox" id="map_display_precincts" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_display_precincts">Display Precincts</label>
                            </div>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 75px;position: absolute;"><input type="checkbox" id="map_enacted_districting" disabled="true" value="FILTER_DP_ORIGINAL"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_enacted_districting">Display Enacted Districting</label>
                            </div>
                        </div>
                        <hr>
                        <div class="row">
                            <h6 style="margin-left:15px; font-weight:bolder;">Heatmap</h6>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 175px;position: absolute;"><input type="checkbox" id="map_precinct_level" onchange="heatmapOptionHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_precinct_level">Precinct Level</label>
                            </div>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 205px;position: absolute;"><input type="checkbox" id="map_state_average" onchange="heatmapOptionHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_state_average">State Average</label>
                            </div>
                        </div>
                        <hr>
                        <div class="row">
                            <h6 style="margin-left:15px; font-weight:bolder;">Minorities</h6>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 283px;position: absolute;"><input type="checkbox" id="map_african_american" onclick="demographicHeatmapHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_african_american">African-American</label>
                            </div>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 313px;position: absolute;"><input type="checkbox" id="map_asian_american" onclick="demographicHeatmapHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;;font-size: 14px;">
                                <label for="map_asian_american">Asian-American</label>
                            </div>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 341px;position: absolute;"><input type="checkbox" id="map_hispanic" onclick="demographicHeatmapHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;;font-size: 14px;">
                                <label for="map_hispanic">Hispanic</label>
                            </div>
                        </div>
                        <div id="row">
                            <div style="width:10%;top: 371px;position: absolute;"><input type="checkbox" id="map_native_american" onclick="demographicHeatmapHandler(this)" disabled="true"></div>
                            <div style="width:90%;margin-left: 30px;font-size: 14px;">
                                <label for="map_native_american">Native-American</label>
                            </div>
                        </div>
                    </div>`;
    return div;
};
mapFilters.addTo(mainMap);

function getColor(d, totalPop) {
    // Get color based on demographic pop / total pop
    // return d/totalPop > .95 ? '#3f0000' :
    //     d/totalPop > .9    ? '#580000' :
    //     d/totalPop > .85   ? '#720000' :
    //     d/totalPop > .8    ? '#8b0000' :
    //     d/totalPop > .75   ? '#a50000' :
    //     d/totalPop > .7    ? '#be0000' :
    //     d/totalPop > .65   ? '#d80000' :
    //     d/totalPop > .6    ? '#e50000' :
    //     d/totalPop > .55   ? '#f80000' :
    //     d/totalPop > .5    ? '#ff3333' :
    //     d/totalPop > .45   ? '#fff7fb' :
    //     d/totalPop > .4    ? '#ece7f2' :
    //     d/totalPop > .35   ? '#d0d1e6' :
    //     d/totalPop > .3    ? '#a6bddb' :
    //     d/totalPop > .25   ? '#74a9cf' :
    //     d/totalPop > .2    ? '#3690c0' :
    //     d/totalPop > .15   ? '#0570b0' :
    //     d/totalPop > .1    ? '#045a8d' :
    //     d/totalPop > .5    ? '#023858' :
    //                 '#011858';
    return d/totalPop > .95 ? '#FFFF00' :
        d/totalPop > .9    ? '#FFFF0D' :
        d/totalPop > .85   ? '#FFFF19' :
        d/totalPop > .8    ? '#FFFF26' :
        d/totalPop > .75   ? '#FFFF33' :
        d/totalPop > .7    ? '#FFFF40' :
        d/totalPop > .65   ? '#FFFF4D' :
        d/totalPop > .6    ? '#FFFF59' :
        d/totalPop > .55   ? '#FFFF66' :
        d/totalPop > .5    ? '#FFFF73' :
        d/totalPop > .45   ? '#FFFF80' :
        d/totalPop > .4    ? '#FFFF8C' :
        d/totalPop > .35   ? '#FFFF99' :
        d/totalPop > .3    ? '#FFFFA6' :
        d/totalPop > .25   ? '#FFFFB3' :
        d/totalPop > .2    ? '#FFFFBF' :
        d/totalPop > .15   ? '#FFFFCC' :
        d/totalPop > .1    ? '#FFFFD9' :
        d/totalPop > .5    ? '#FFFFE6' :
                '#FFFFF2';
}

function getColor2(d, stateAveragePop) {
    // Get color based on demographic pop / total pop
    return d > stateAveragePop + (stateAveragePop * 0.9) ? '#3f0000' :
        d > stateAveragePop + (stateAveragePop * 0.8)    ? '#580000' :
        d > stateAveragePop + (stateAveragePop * 0.7)   ? '#720000' :
        d > stateAveragePop + (stateAveragePop * 0.6)    ? '#8b0000' :
        d > stateAveragePop + (stateAveragePop * 0.5)   ? '#a50000' :
        d > stateAveragePop + (stateAveragePop * 0.4)    ? '#be0000' :
        d > stateAveragePop + (stateAveragePop * 0.3)   ? '#d80000' :
        d > stateAveragePop + (stateAveragePop * 0.2)    ? '#e50000' :
        d > stateAveragePop + (stateAveragePop * 0.1)   ? '#f80000' :
        d > stateAveragePop    ? '#ff3333' :
        d > stateAveragePop - (stateAveragePop * 0.1)   ? '#fff7fb' :
        d > stateAveragePop - (stateAveragePop * 0.2)    ? '#ece7f2' :
        d > stateAveragePop - (stateAveragePop * 0.3)   ? '#d0d1e6' :
        d > stateAveragePop - (stateAveragePop * 0.4)    ? '#a6bddb' :
        d > stateAveragePop - (stateAveragePop * 0.5)   ? '#74a9cf' :
        d > stateAveragePop - (stateAveragePop * 0.6)    ? '#3690c0' :
        d > stateAveragePop - (stateAveragePop * 0.7)   ? '#0570b0' :
        d > stateAveragePop - (stateAveragePop * 0.8)    ? '#045a8d' :
        d > stateAveragePop - (stateAveragePop * 0.9)    ? '#023858' :
                    '#011858';
    // return d > stateAveragePop + (stateAveragePop * 0.9) ? '#FFFF00' :
    //     d > stateAveragePop + (stateAveragePop * 0.8)    ? '#FFFF0D' :
    //     d > stateAveragePop + (stateAveragePop * 0.7)   ? '#FFFF19' :
    //     d > stateAveragePop + (stateAveragePop * 0.6)    ? '#FFFF26' :
    //     d > stateAveragePop + (stateAveragePop * 0.5)   ? '#FFFF33' :
    //     d > stateAveragePop + (stateAveragePop * 0.4)    ? '#FFFF40' :
    //     d > stateAveragePop + (stateAveragePop * 0.3)   ? '#FFFF4D' :
    //     d > stateAveragePop + (stateAveragePop * 0.2)    ? '#FFFF59' :
    //     d > stateAveragePop + (stateAveragePop * 0.1)   ? '#FFFF66' :
    //     d > stateAveragePop    ? '#FFFF73' :
    //     d > stateAveragePop - (stateAveragePop * 0.1)   ? '#FFFF73' :
    //     d > stateAveragePop - (stateAveragePop * 0.2)    ? '#FFFF80' :
    //     d > stateAveragePop - (stateAveragePop * 0.3)   ? '#FFFF8C' :
    //     d > stateAveragePop - (stateAveragePop * 0.4)    ? '#FFFF99' :
    //     d > stateAveragePop - (stateAveragePop * 0.5)   ? '#FFFFA6' :
    //     d > stateAveragePop - (stateAveragePop * 0.6)    ? '#FFFFB3' :
    //     d > stateAveragePop - (stateAveragePop * 0.7)   ? '#FFFFCC' :
    //     d > stateAveragePop - (stateAveragePop * 0.8)    ? '#FFFFD9' :
    //     d > stateAveragePop - (stateAveragePop * 0.9)    ? '#FFFFE6' :
    //             '#FFFFF2';
}

// Map legend..
const heatmapLegend = L.control({position: 'bottomleft'});
heatmapLegend.onAdd = function (map) {
    let div = L.DomUtil.create('div', 'leaflet-control-layers');
    div.id = 'heatmap_legend';
    L.DomUtil.addClass(div, 'leaflet-control-layers-expanded')
    div.style.opacity = 0.7
    let grades = [
        0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95
    ]
    for (let i = 0; i < grades.length; i++){
        div.innerHTML += '<div class="color-box" style="background-color:' + getColor(grades[i] + 1, 100) + '"></div>'
        div.innerHTML += `<span> ${grades[i]}% ${(grades[i + 1] ? '&ndash;' + grades[i + 1] + '%' : '+')}</span><br>`
    }
    return div;
}

const heatmapLegend2 = L.control({position: 'bottomleft'});
heatmapLegend2.onAdd = function (map) {
    let div = L.DomUtil.create('div', 'leaflet-control-layers');
    div.id = 'heatmap_legend';
    L.DomUtil.addClass(div, 'leaflet-control-layers-expanded')
    div.style.opacity = 0.7
    let grades = [
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190
    ]
    for (let i = 0; i < grades.length; i++){
        div.innerHTML += '<div class="color-box" style="background-color:' + getColor2(grades[i] + 1, 100) + '"></div>'
        div.innerHTML += `<span> ${(i+1) < 10 ? 'Avg - ' + (100 - (i+1)%10 * 10) + '%': 'Avg + ' + (i+1)%10 * 10 + '%'}</span><br>`
    }
    return div;
}

const districtingLegend = L.control({position: 'bottomleft'});
districtingLegend.onAdd = function (map) {
    let div = L.DomUtil.create('div', 'leaflet-control-layers');
    div.id = 'districting_legend';
    L.DomUtil.addClass(div, 'leaflet-control-layers-expanded')
    div.style.opacity = 0.7
    let colors = {
        "Enacted Districing": "green",
        "Average Districting": "blue",
        "Extreme Districting 1": "deeppink",
        "Extreme Districting 2": "darkred",
        "Random Districting": "purple"
    }
    for (let x in colors){
        div.innerHTML += `<div class="color-box" style="background-color: ${colors[x]}"></div>`
        div.innerHTML += `<span> ${x} </span><br>`
    }
    return div;
}
districtingLegend.addTo(mainMap);
heatmapLegend.addTo(mainMap);
// Precinct Details Controller
const precinctDetails = L.control({position: 'bottomright'});
precinctDetails.onAdd = function (map){
    let div = L.DomUtil.create('div', 'precinct-details');
    L.DomUtil.addClass(div, 'leaflet-control-layers')
    L.DomUtil.addClass(div, 'leaflet-control-layers-expanded')
    div.innerHTML = `<div id='precinct-info'></div>`;
    return div;
}
precinctDetails.addTo(mainMap)