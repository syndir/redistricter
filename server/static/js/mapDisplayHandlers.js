// Event Handler for Display Districts Checkbox. If Checkbox is checked, display districts for selected state.
// var displayDistrictsCheckBox = document.getElementById('map_display_districts')
// displayDistrictsCheckBox.addEventListener('change', function() {
//     if(this.checked){
//         mainMap.addControl(selectedDistricts)
//     } else {
//         // remove popup for each feature
//         mainMap.removeControl(selectedDistricts)
//     }
// })

// Dummy handler to display precinct data (which is really dummy district data)
let displayPrecinctsCheckBox = document.getElementById('map_display_precincts')
displayPrecinctsCheckBox.addEventListener('change', function() {
    if(this.checked){
        showPrecincts(mainMap)
    } else {
        hidePrecincts();
    }
})

function mapControllerCollapseHandler() {
    var x = document.getElementById("map_filters_control");
    var y = document.getElementById("map_control_toggle");
    if (x.style.display === "none") {
      x.style.display = "block";
      y.style.display = "none"
    } else {
      x.style.display = "none";
      y.style.display = "block"
    }
}

function demographicHeatmapHandler(e){
  let precinctCheckbox = document.getElementById('map_display_precincts')
  let stateAvgCheckbox = document.getElementById('map_state_average')
  let precinctLevelCheckbox = document.getElementById('map_precinct_level')
  if(stateAvgCheckbox.checked == false && precinctLevelCheckbox.checked == false){
    precinctLevelCheckbox.checked = true;
    heatmapLegend2.remove();
    heatmapLegend.addTo(mainMap);
  }
  if (precinctCheckbox.checked == false){
    precinctCheckbox.checked = true;
    showPrecincts(mainMap);
  }
  if (e.checked) {
    switch(e.id){
      case "map_african_american":
        selectedHeatMapDemographics['AA'] = true;
        break;
      case "map_asian_american":
        selectedHeatMapDemographics['AS'] = true;
        break;
      case "map_hispanic":
        selectedHeatMapDemographics['HISP'] = true;
        break;
      case "map_native_american":
        selectedHeatMapDemographics['AI'] = true;
        break;
    }
  } else {
    switch(e.id){
      case "map_african_american":
        selectedHeatMapDemographics['AA'] = false;
        break;
      case "map_asian_american":
        selectedHeatMapDemographics['AS'] = false;
        break;
      case "map_hispanic":
        selectedHeatMapDemographics['HISP'] = false;
        break;
      case "map_native_american":
        selectedHeatMapDemographics['AI'] = false;
        break;
    }
  }
  changeHeatMapColor()
  let isHeatMapSelected = false;
  for (let demo in selectedHeatMapDemographics){
    if (selectedHeatMapDemographics[demo]){
      isHeatMapSelected = true;
      break;
    }
  }
  if(!isHeatMapSelected){
    statePrecinctLayers[selectedState].eachLayer(function (layer){
      layer.setStyle({fillColor: '#b2b0ae'})
    });
  }
}

function changeHeatMapColor(){
  let totalPop = 0;
  if (document.getElementById('map_state_average').checked){
    for (let demo in selectedHeatMapDemographics){
      if(selectedHeatMapDemographics[demo]){
        totalPop += maximumPopulation[selectedState][demo];
      }
    }
  }
  statePrecinctLayers[selectedState].eachLayer(function (layer) {
    let demographicsPop = 0;
    for (let demo in selectedHeatMapDemographics){
      switch(demo){
        case 'AA':
          if(selectedHeatMapDemographics[demo]){
            demographicsPop += layer.feature.properties.T_AA
          }
          break;
        case 'AS':
          if(selectedHeatMapDemographics[demo]){
            demographicsPop += layer.feature.properties.T_AS
          }
          break;
        case 'AI':
          if(selectedHeatMapDemographics[demo]){
            demographicsPop += layer.feature.properties.T_AI
          }
          break;
        case 'HISP':
          if(selectedHeatMapDemographics[demo]){
            demographicsPop += layer.feature.properties.T_HISP
          }
          break;
      }   
    }
    if(document.getElementById('map_state_average').checked){
      layer.setStyle({fillColor: getColor2(demographicsPop, totalPop/numberOfPrecincts[selectedState])});
    } else {
      layer.setStyle({fillColor: getColor(demographicsPop, layer.feature.properties.T_POP)});
    }
  })
}

function heatmapOptionHandler(e){
  let stateAvgCheckbox = document.getElementById('map_state_average')
  let precinctLevelCheckbox = document.getElementById('map_precinct_level')
  if (e.id == 'map_state_average' && e.checked){
    precinctLevelCheckbox.checked = false;
    heatmapLegend2.addTo(mainMap);
    heatmapLegend.remove();
  }
  if (e.id == 'map_precinct_level' && e.checked){
    stateAvgCheckbox.checked = false;
    heatmapLegend2.remove();
    heatmapLegend.addTo(mainMap);
  }

  let isHeatMapSelected = false;
  for (let demo in selectedHeatMapDemographics){
    if (selectedHeatMapDemographics[demo]){
      isHeatMapSelected = true;
      break;
    }
  }

  if (isHeatMapSelected){
    changeHeatMapColor();
  }
}

// Dummy handler to display currently enacted plan (which is really dummy district data)
let displayEnactedPlan = document.getElementById('map_enacted_districting')
function displayDistrictHandler() {
  if(this.checked === false)
  {
    // TODO: remove from display
    displayedDistricts[this.value].remove();
    displayedDistricts[this.value] = null;
    // check if all the filters are off
    for (let filter in displayedDistricts){
      if (displayedDistricts[filter] != null){
        return;
      }
    }
    let precinctCheckbox = document.getElementById('map_display_precincts')
    // show state border if precinct is not showing
    if (!precinctCheckbox.checked){
      selectedLayer.setStyle(selectedStyle);
    }
    return;
  }
  if(this.value === "FILTER_DP_ORIGINAL") {
    applyFilter(this.value, null);
    selectedLayer.setStyle(stateBorderlessStyle);
  }
}

displayEnactedPlan.onchange = displayDistrictHandler;
function jobDistrictHandler(e) {
  if (e.checked === false){
    displayedDistricts[e.value].remove();
    displayedDistricts[e.value] = null;
    // check if all the filters are off
    for (let filter in displayedDistricts){
      if (displayedDistricts[filter] != null){
        return;
      }
    }
    let precinctCheckbox = document.getElementById('map_display_precincts')
    if (!precinctCheckbox.checked){
      selectedLayer.setStyle(selectedStyle);
    }
    return;
  } else {
    if (previousJob != null && openedJob != previousJob){
      document.getElementById(`best_districting_${previousJob}`).checked = false;
      document.getElementById(`worst_districting_${previousJob}`).checked = false;
      document.getElementById(`avg_districting_${previousJob}`).checked = false;
      document.getElementById(`random_districting_${previousJob}`).checked = false;
      previousJob = openedJob;
      for (let district in displayedDistricts){
        if (district != 'FILTER_DP_ORIGINAL' && displayedDistricts[district] != null){
          displayedDistricts[district].remove();
          displayedDistricts[district] = null;
        }
      }
    }
    if (previousJob == null){
      previousJob = openedJob;
    }
    applyFilter(e.value, openedJob);
    selectedLayer.setStyle(stateBorderlessStyle);
  }
}