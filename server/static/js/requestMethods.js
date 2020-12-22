//TODO: 
// Selected Demographics Param
// HTTP Headers?
// Callback Functions
function createNewJob() {
    let xhr = new XMLHttpRequest()
    let demographics = new Map()
    let aa_checkbox = document.getElementById('algo_african_american')
    let as_checkbox = document.getElementById('algo_asian_american')
    let hisp_checkbox = document.getElementById('algo_hispanic')
    let na_checkbox = document.getElementById('algo_native_american')
    let errorMessage = document.getElementById("errorText")
    let hasDemo = false;

    if(aa_checkbox.checked === true) {
        demographics.set(aa_checkbox.value, true)
        hasDemo = true;
    }
    if(as_checkbox.checked === true) {
        demographics.set(as_checkbox.value, true)
        hasDemo = true;
    }
    if(hisp_checkbox.checked === true) {
        demographics.set(hisp_checkbox.value, true)
        hasDemo = true;
    }
    if(na_checkbox.checked === true) {
        demographics.set(na_checkbox.value, true)
        hasDemo = true;
    }
    params = {
        "selectedDemographics": Object.fromEntries(demographics),
        "numSeedPlans": parseInt(document.getElementById("algo_num_seeds").value),
        "compactnessMeasure": document.getElementById("algo_compactness_constraint").value,
        "populationVariance": parseFloat(document.getElementById("algo_pop_variance_constraint").value),
        "state" : selectedState
    }
    if(hasDemo === false) {
        errorMessage.innerHTML = "Please select at least one demographic for the job."
        document.getElementById("errorMessage").hidden = false;
        return;
    }
    if(!Number.isInteger(params["numSeedPlans"])) {
        errorMessage.innerHTML = "Number of plans to generate must be a number."
        document.getElementById("errorMessage").hidden = false;
        return;
    }
    if(params["numSeedPlans"] <= 0 || params["numSeedPlans"] > 1000) {
        errorMessage.innerHTML = "Number of plans to generate must be between 1 and 1000."
        document.getElementById("errorMessage").hidden = false;
        return;
    }
    if(isNaN(params["populationVariance"]) || params["populationVariance"] < 0.01 || params["populationVariance"] > 100.0) {
        errorMessage.innerHTML = "Population variance must be a number between 0.01 and 100.00."
        document.getElementById("errorMessage").hidden = false;
        return;
    }

    document.getElementById("errorMessage").hidden = true;
    console.log(JSON.stringify(params))
    xhr.open("POST", "/createJob")
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = () => {
        console.log(`createNewJob: ${xhr.status} ${xhr.response}`);
        getHistory();
        let modal = $('#submitJobModal');
        let jobModalBody = document.getElementById('jobModalBody');
        jobModalBody.innerHTML = `Job ${xhr.response} has been created.`
        modal.modal(); 
    }
    xhr.onerror = () => {
        console.log(`createNewJob: Request Failed`)
    }
    xhr.send(JSON.stringify(params))
}

function selectStateRequest() {
    let xhr = new XMLHttpRequest();

    xhr.open("PUT", "/selectState")
    xhr.send(selectedState)
    xhr.onload = () => {
        getHistory();
        applyFilter("FILTER_DP_ORIGINAL", null);
    }
}

function cancelJob(jobID) {
    let xhr = new XMLHttpRequest()

    xhr.open("POST", "/abortJob")
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = () => {
        console.log(`cancelJob: ${xhr.status} ${xhr.response}`);
        getHistory();
    }
    xhr.onerror = () => {
        console.log(`cancelJob: Request Failed`)
    }
    xhr.send(jobID)
}

function deleteJob(jobID) {
    let xhr = new XMLHttpRequest()

    xhr.open("DELETE", "/deleteJob?jobID=" + jobID)
    xhr.onload = () => {
        console.log(`deletePreviousJob: ${xhr.status} ${xhr.response}`);
        getHistory();
    }
    xhr.onerror = () => {
        console.log(`deletePreviousJob: Request Failed`)
    }
    xhr.send()
}

function updateServerState(){
    let xhr = new XMLHttpRequest()

    xhr.open("PUT", "/selectState")
    xhr.onload = () => {
        console.log(`updateServerState: ${xhr.status} ${xhr.response}`);
        getHistory();
    }
    xhr.onerror = () => {
        console.log(`updateServerState: Request Failed`)
    }
    xhr.send(selectedState)
}

function getHeatmapData(){
    params = {

    }
    let xhr = new XMLHttpRequest()

    xhr.open("GET", "/getHeatmap")
    xhr.onload = () => {
        console.log(`getHeatmapData: ${xhr.status} ${xhr.response}`);
    }
    xhr.onerror = () => {
        console.log(`getHeatmapData: Request Failed`)
    }
    xhr.send(params)
}

function applyFilter(filter, job) {
    let params = {
        "jobID" : job,
        "filter" : filter
    }
    let xhr = new XMLHttpRequest()

    console.log(params);
    xhr.open("POST", "/applyFilter")
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = () => {
        // console.log(`applyFilter: ${xhr.status} ${xhr.response}`);
        enactedDistricts = JSON.parse(xhr.response);
        displayedDistricts[filter] = L.geoJSON(enactedDistricts, {
            style: {weight: 4,
            fillColor: '#b2b0ae',
            color: `${filter == 'FILTER_DP_ORIGINAL' ? 'green' : 
            filter == 'FILTER_DP_BEST' ? 'deeppink': 
            filter == 'FILTER_DP_WORST' ? 'darkred' : 
            filter == 'FILTER_DP_AVERAGE' ? 'blue': 'purple'}`,
            fillOpacity: 0.0}, 
            onEachFeature : onEachDistrictFeature})
        displayedDistricts[filter].addTo(mainMap);
        let precinctCheckbox = document.getElementById('map_display_precincts')
        if (precinctCheckbox.checked){
            statePrecinctLayers[selectedState].bringToFront();
        }
    }   
    xhr.onerror = () => {
        console.log(`applyFilter: Request Failed`)
    }
    xhr.send(JSON.stringify(params))
}

function getBoxAndWhiskerData(job) {
    let xhr = new XMLHttpRequest()
    xhr.open("GET", "/getBoxAndWhiskerData?jobID=" + job)
	xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = () => {
        console.log(`getBoxAndWhiskerData: ${xhr.status} ${xhr.response}`);
        let bwPlot = JSON.parse(xhr.response);
        let dp = [];
        let dp2 = [];
        bwPlot['whiskers'].forEach((element, index)=>{
            dp.push({x:index, label: index + 1, y: [element['min'], element['q1'], element['q3'], element['max'], element['median']]})
        })
        bwPlot['currentPlanDataPoints'].forEach((element, index) => {
            dp2.push({x:index, label:index + 1, y:element});
        })
        let chart = new CanvasJS.Chart("boxPlotContainer", {
            animationEnabled: true,
            axisX: {
                title: "Indexed Districts",
                titleFontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
                titleFontSize: 18
            },
            axisY: {
                title: "VAP %",
                titleFontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
                titleFontSize: 18
            },
            legend: {
                cursor: "pointer",
                fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
                fontSize: 14
            },
            data: [{
                type: "boxAndWhisker",
                // toolTipContent: "<b>Minimum:</b> {y[0]}<br><b>Q1:</b> {y[1]}<br> <b>Median:</b> {y[4]}<br><b>Q3:</b> {y[2]}<br><b>Maximum:</b> {y[3]}",
                toolTipContent: "<table><tr><td align='left'><b>Minimum:</b></td><td>{y[0]}</td></tr><tr><td align='left'><b>Q1:</b></td><td>{y[1]}</td></tr><tr><td align='left'><b>Median:</b></td><td>{y[4]}</td></tr><tr><td align='left'><b>Q3:</b></td><td>{y[2]}</td></tr><tr><td align='left'><b>Maximum:</b></td><td>{y[3]}</td></tr></table>",
                dataPoints: dp,
                name: "Aggregate Data for Generated Plans",
                showInLegend: true
            },
            {
                type: "scatter",
                name: "Currently Enacted Plan",
                showInLegend: true,
                toolTipContent: "<span style=\"color:#C0504E\">{name}</span>: {y}",
                dataPoints: dp2
            }]
        });
        chart.render();
    }
    xhr.onerror = () => {
        console.log(`getBoxAndWhiskerData: Request Failed`)
    }
    xhr.send()
}

function getHistory() {
    params = {}
    let xhr = new XMLHttpRequest()
    xhr.open("GET", "/getHistory")
    xhr.onload = () => {
        console.log(`getHistory: ${xhr.status} ${xhr.response}`);
        jobHistory = JSON.parse(xhr.response);
        updateHistory();
    }
    xhr.onerror = () => {
        console.log(`getHistory: Request Failed`)
    }
    xhr.send(params)
}
