window.onload = () => {
    getHistory();
    setGlobalTimer();
    // let chart = new CanvasJS.Chart("boxPlotContainer", {
    //     animationEnabled: true,
    //     data: [{
    //         type: "boxAndWhisker",
    //         dataPoints: planData
    //     }]
    // });
    // chart.render();

    // Change state selection
    let stateSelection = document.getElementById("select_state");
    stateSelection.onchange = () =>{
        for(let i = 0; i < 3; i++){
            if(stateLayers[i].feature.properties.name.toUpperCase() == stateSelection.value){
                selectState(stateLayers[i]);
                break;
            }
        }
    }
    let stateSelectionResults = document.getElementById("select_state_results");
    stateSelectionResults.onchange = () =>{
        for(let i = 0; i < 3; i++){
            if(stateLayers[i].feature.properties.name.toUpperCase() == stateSelectionResults.value){
                selectState(stateLayers[i]);
                break;
            }
        }
    }

    // Reset state selection when page is reload
    stateSelection.value = "";
    disableMapOptTab();
    disableGenPlanTab();

    // Linked submit job button
    let submitJobButton = document.getElementById("algo_btn_submit")
    submitJobButton.onclick = createNewJob

    // create the state bounding polygons
    showStates(statesData, mainMap);
}


// called when a state is selected
function selectState(e){
    resetOnStateChange();
    if(statePrecinctLayers[selectedState]){
        hidePrecincts();
    }
    // target is state layer
    let target;
    if(!e.target){
        // if function call is from dropdown
        target = e;
    }else{
        // if function call is from clicking map
        target = e.target
    }
    
    if(selectedLayer){
        stateBoundaries.resetStyle(selectedLayer);
    }

    selectedState = target.feature.properties.name.toUpperCase();
    selectedLayer = target;
    console.log("selectedState = " + selectedState);
    document.getElementById("select_state").value = selectedState.toUpperCase();
    document.getElementById("select_state_results").value = selectedState.toUpperCase();
    // document.getElementById("algo_state_label").innerHTML = selectedState;
    enableGenPlanTab();
    enableMapOptTab();
    if(!statePrecinctsDatas[selectedState]){
        document.getElementById("map_display_precincts").disabled = true;
    }
    loadGeoJson();
    zoomToFeature(target);

    document.getElementById('map_enacted_districting').checked = true;
}

function resetOnStateChange(){
    document.getElementById('map_display_precincts').checked = false;
    document.getElementById('map_african_american').checked = false;
    document.getElementById('map_asian_american').checked = false;
    document.getElementById('map_hispanic').checked = false;
    document.getElementById('map_native_american').checked = false;
    for (let demo in selectedHeatMapDemographics){
        selectedHeatMapDemographics[demo] = false;
    }
    if (selectedState != null){
        statePrecinctLayers[selectedState].eachLayer(function (layer){
            layer.setStyle({fillColor: '#b2b0ae'})
        });
    }
    for (let district in displayedDistricts){
        if (displayedDistricts[district] != null){
          displayedDistricts[district].remove();
          displayedDistricts[district] = null;
        }
    }
    previousJob = null;
    openedJob = null;
}

// used to load geojson to statePrecinctLayers and statePrecinctData
function loadGeoJson(){
    let st = selectedState == "ALABAMA" ? "al" : selectedState == "FLORIDA" ? "fl" : selectedState == "PENNSYLVANIA" ? "pa" : null;
    let file = `${st}_pop_simplified_geo.geojson`
    if(statePrecinctsDatas[selectedState] == null){
        let xhr = new XMLHttpRequest();
        xhr.open("GET", file);
        xhr.onload = function(){
            statePrecinctsDatas[selectedState] = JSON.parse(xhr.response);
            statePrecinctLayers[selectedState] = L.geoJson(statePrecinctsDatas[selectedState.toUpperCase()], {
                onEachFeature: onEachPrecinctFeature
            });
            document.getElementById("map_display_precincts").disabled = false;
        }
        xhr.send();
    }else{
        document.getElementById("map_display_precincts").disabled = false;
    }
}

function setGlobalTimer() {
    let count = 60; 
    timer = setInterval(function() {
        // document.getElementById("global-timer").innerHTML = `Timer: ${count--}`
        if(--count == -1){
            count = 60;
            getHistory();
        }
    }, 1000);
}

function updateHistory() {
    for (job in jobHistory){
        if (!currentJobHistory.includes(job)){
            let currentJob = jobHistory[job];
            let jobStatus = Object.keys(currentJob)[0];
            addJobCard(job, jobStatus, currentJob[jobStatus]['state'], Object.keys(currentJob[jobStatus]['selectedDemographics']), 
                currentJob[jobStatus]['numSeedPlans'],
                currentJob[jobStatus]['populationVariance'], currentJob[jobStatus]['compactnessMeasure']
            );
            currentJobHistory.push(job)
        }
    }
    currentJobHistory.forEach(job => {
        if (!(job in jobHistory)){ 
            document.getElementById(job).parentNode.removeChild(document.getElementById(job))
        }
    })
    currentJobHistory = currentJobHistory.filter(job => {return job in jobHistory})

    
    if(openedJob != null){
        $(`#collapse_content_${openedJob}`).collapse('show');
    }
    
}