// Enable all elements in the map options tab
// Used when a state is selected
function enableMapOptTab(){
    // document.getElementById("map_display_enacted_plan").disabled = false;
    // document.getElementById("map_display_districts").disabled = false;
    // document.getElementById("map_display_precincts").disabled = false;
    document.getElementById("map_enacted_districting").disabled = false;
    document.getElementById("map_african_american").disabled = false;
    document.getElementById("map_asian_american").disabled = false;
    document.getElementById("map_hispanic").disabled = false;
    document.getElementById("map_native_american").disabled = false;
    document.getElementById("map_precinct_level").disabled = false;
    document.getElementById("map_state_average").disabled = false;
    
}

function disableMapOptTab(){
    // document.getElementById("map_display_enacted_plan").disabled = true;
    // document.getElementById("map_display_districts").disabled = true;
    // document.getElementById("map_display_precincts").disabled = true;
    document.getElementById("map_enacted_districting").disabled = true;
    document.getElementById("map_african_american").disabled = true;
    document.getElementById("map_asian_american").disabled = true;
    document.getElementById("map_hispanic").disabled = true;
    document.getElementById("map_native_american").disabled = true;
    document.getElementById("map_precinct_level").disabled = true;
    document.getElementById("map_state_average").disabled = true;
}

// Enable all elements in the generate plan tab
// Used when a state is selected
function enableGenPlanTab(){
    document.getElementById("algo_african_american").disabled = false;
    document.getElementById("algo_asian_american").disabled = false;
    document.getElementById("algo_hispanic").disabled = false;
    document.getElementById("algo_native_american").disabled = false;
    document.getElementById("algo_num_seeds").disabled = false;
    document.getElementById("algo_compactness_constraint").disabled = false;
    document.getElementById("algo_pop_variance_constraint").disabled = false;
    document.getElementById("algo_btn_submit").disabled = false;
}

function disableGenPlanTab(){
    document.getElementById("algo_african_american").disabled = true;
    document.getElementById("algo_asian_american").disabled = true;
    document.getElementById("algo_hispanic").disabled = true;
    document.getElementById("algo_native_american").disabled = true;
    document.getElementById("algo_num_seeds").disabled = true;
    document.getElementById("algo_compactness_constraint").disabled = true;
    document.getElementById("algo_pop_variance_constraint").disabled = true;
    document.getElementById("algo_btn_submit").disabled = true;
}

// Results tab job handlers
function addJobCard(jobID, status, state, targetDemographic, seeds, populationVarience, compactness){
    let cardContainer = document.getElementById("cardViewContainer");
    let statusColor = {
        "ABORTED": "gray",
        "ERROR": "red",
        "FINISHED": "black",
        "PENDING": "blue",
        "RUNNING": "green"
    }
    //<h5 class="card-title">State: ${state[0] + state.slice(1).toLowerCase()}</h5>
    let statusText = `<span style="color:${statusColor[status]}"><b>${status}</b></span>`
    let cardElement = `<div class="card" id="${jobID}" style="margin: 10px" onmouseover="mouseOverJob(${jobID})" onmouseout="mouseOutJob(${jobID})">
                            <span class="card-header" style="font-size: 14px;">Job ID: ${jobID}&emsp;${statusText}
                                <img src="https://img.icons8.com/ios/50/000000/expand-arrow--v2.png" id="collapse-arrow-${jobID}" style="position: absolute;right: 55px; top: 15px;" width="20" height="20" onclick="collapseClicked(${jobID})"/>
                                <button class="btn" id="abort_btn" style="position: absolute;right: 10px;top: 5px;" onclick="abortClicked(${jobID}, '${status}')"><i class="fa fa-trash"></i></button>
                            </span> 
                            <div class="card-body" style="padding-bottom: 0px; padding-left: 1.5rem;"> 
                                <p class="card-text" id="job_demographic_${jobID}" style="font-size:14px;">Target Demographic: ${targetDemographic.map(element => {return element.split('_').map(s => {return s[0] + s.slice(1).toLowerCase()}).join('-')}).join(', ')}</p>
                                <button class="btn btn-link" style="float:right; display:none" id="collapse_btn_${jobID}" onclick="collapseClicked(${jobID})">More</button>
                            </div>
                            <div id="collapse_content_${jobID}" class="collapse" aria-labelledby="headingOne" data-parent="#cardViewContainer">
                                <div class="card-body" id="job_content_${jobID}" style="margin-left:20px; margin-right:20px; padding-top:0px">
                                    <div class="row">
                                        <div style="text-align: left; width: 75%; font-size:14px;"><label>Number of plans to generate</label></div>
                                        <div style="text-align: right; width: 25%; font-size:14px;">${seeds}</div>
                                    </div>
                                    <div class="row">
                                        <div style="text-align: left; width: 75%; font-size:14px;"><label>Population Variance</label></div>
                                        <div style="text-align: right; width: 25%; font-size:14px;">${populationVarience}</div>
                                    </div>
                                    <div class="row">
                                        <div style="text-align: left; width: 50%; font-size:14px;"><label>Compactness Measure</label></div>
                                        <div style="text-align: right; width: 50%; font-size:14px;">${compactness.split('_').map(element => {return element[0] + element.slice(1).toLowerCase()}).join(' ')}</div>
                                    </div>
                                    <hr>
                                    ${status=="FINISHED" ? 
                                    `<div id="job_filter_${jobID}">
                                        <div class="row">
                                            <div><h5>Map Filters</h5></div>
                                        </div>
                                        <div class="row">
                                            <div style="text-align: left; width: 70%; font-size:14px;"><label for="avg_districting_${jobID}">Average Districting:</label></div>
                                            <div style="text-align: right; width: 30%;"><input type="checkbox" id="avg_districting_${jobID}" value="FILTER_DP_AVERAGE" onchange="jobDistrictHandler(this)"></div>
                                        </div>
                                        <div class="row">
                                            <div style="text-align: left; width: 70%; font-size:14px;"><label for="best_districting_${jobID}">Extreme Districting 1:</label></div>
                                            <div style="text-align: right; width: 30%;"><input type="checkbox" id="best_districting_${jobID}" value="FILTER_DP_BEST" onchange="jobDistrictHandler(this)"></div>
                                        </div>
                                        <div class="row">
                                            <div style="text-align: left; width: 70%; font-size:14px;"><label for="worst_districting_${jobID}">Extreme Districting 2:</label></div>
                                            <div style="text-align: right; width: 30%;"><input type="checkbox" id="worst_districting_${jobID}" value="FILTER_DP_WORST" onchange="jobDistrictHandler(this)"></div>
                                        </div>
                                        <div class="row">
                                            <div style="text-align: left; width: 70%; font-size:14px;"><label for="random_districting_${jobID}">Random Districting:</label></div>
                                            <div style="text-align: right; width: 30%;"><input type="checkbox" id="random_districting_${jobID}" value="FILTER_DP_RANDOM" onchange="jobDistrictHandler(this)""></div>
                                        </div>
                                    </div>
                                    <hr>
                                    <button type="button" class="btn btn-primary" id="job_plot_${jobID}" style="width: 100%;" data-toggle="modal" data-target="#summaryModal" onclick="getBoxAndWhiskerData(${jobID})">Box and Whisker Plot</button>`
                                    :""}
                                </div>
                            </div>
                        </div>`
    cardContainer.innerHTML += cardElement;
}

function selectJobHandler(jobID){
    if (selectedJob == null){
        selectedJob = document.getElementById(jobID);
    } else if (selectedJob != document.getElementById(jobID)) {
        selectedJob = document.getElementById(jobID);
    } else {
        selectedJob = null;
    }
}

function mouseOverJob(jobID){
    hoverJob = document.getElementById(jobID)
    hoverJob.classList.add("border-secondary")
}

function mouseOutJob(jobID){
    hoverJob = document.getElementById(jobID)
    hoverJob.classList.remove("border-secondary")
}

function abortClicked(jobID, status){
    let modal = $('#abortModal');
    $('#confirm_abort_btn').replaceWith($('#confirm_abort_btn').clone()); // gets rid of old event listeners

    if(status == "PENDING" || status == "RUNNING"){
        document.getElementById('abortModalLabel').innerHTML = `Canceling Job ${jobID}`;
        document.getElementById('abort_text').innerHTML = `Are you sure you want to cancel Job ${jobID} ?`;
        document.getElementById('confirm_abort_btn').addEventListener('click', () => { cancelJob(jobID) } )
    }else{
        document.getElementById('abortModalLabel').innerHTML = `Deleting Job ${jobID}`;
        document.getElementById('abort_text').innerHTML = `Are you sure you want to delete Job ${jobID} ?`;
        document.getElementById('confirm_abort_btn').addEventListener('click', () => { deleteJob(jobID) } )
    }
    modal.modal();
}

function collapseClicked(jobID){
    let mode = document.getElementById(`collapse_btn_${jobID}`).innerHTML;
    let collapseArrowOri = document.getElementById(`collapse-arrow-${jobID}`)
    if(mode == "More"){
        $(`#collapse_content_${jobID}`).collapse('show');
        //document.getElementById(`${jobID}`).scrollIntoView(true);
        document.getElementById(`collapse_btn_${jobID}`).innerHTML = "Less";
        let collapseArrow = collapseArrowOri.cloneNode(true);
        collapseArrow.src = 'https://img.icons8.com/ios/24/000000/collapse-arrow--v1.png';
        collapseArrowOri.parentElement.replaceChild(collapseArrow, collapseArrowOri);
        openedJob = jobID;
    }else{
        $(`#collapse_content_${jobID}`).collapse('hide');
        document.getElementById(`collapse_btn_${jobID}`).innerHTML = "More";
        let collapseArrow = collapseArrowOri.cloneNode(true);
        collapseArrow.src = 'https://img.icons8.com/ios/50/000000/expand-arrow--v2.png';
        openedJob = null;
    }
    $(`#collapse_content_${jobID}`).on('hide.bs.collapse', function(){
        document.getElementById(`collapse_btn_${jobID}`).innerHTML = "More";
        collapseArrowOri = document.getElementById(`collapse-arrow-${jobID}`);
        let collapseArrow = collapseArrowOri.cloneNode(true);
        collapseArrow.src = 'https://img.icons8.com/ios/50/000000/expand-arrow--v2.png';
        collapseArrowOri.parentElement.replaceChild(collapseArrow, collapseArrowOri);
    });
    console.log(openedJob);
}