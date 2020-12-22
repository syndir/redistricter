let mainMap;
let stateBoundaries;
let selectedState = null;
let selectedLayer = null;
let selectedDistricts = null;
let selectedJob = null;
let jobHistory;
let selectedPrecinct = null;
let hoverJob;
let enactedDistrict;
let openedJob = null;
let previousJob = null;
let currentJobHistory = [];
let precinctLayersDict = {
    "ALABAMA": {},
    "FLORIDA": {},
    "PENNSYLVANIA": {}
}
const stateLayers = [];
const statePrecinctLayers = {
    "ALABAMA": null,
    "FLORIDA": null,
    "PENNSYLVANIA": null
}
const statePrecinctsDatas = {
    "ALABAMA": null,
    "FLORIDA": null,
    "PENNSYLVANIA": null
}
const selectedHeatMapDemographics = {
    'AA': false,
    'AS': false,
    'AI': false,
    'HISP': false
}
const selectedStyle = {
    weight: 5,
    color: '#000000',
    fillColor: '#b2b0ae',
    fillOpacity: 0.5
}
const stateBorderlessStyle = {
    weight: 0,
    color: '#000000',
    fillColor: '#b2b0ae',
    fillOpacity: 0.5
}
const highlightedStyle = {
    weight: 5,
    color: '#999794',
    fillColor: '#b2b0ae',
    fillOpacity: 0.7
}
const precinctStyle = {
    weight: 1,
    opacity: 0.5,
    color: '#666666',
    fillColor: '#b2b0ae',
    fillOpacity: 0.5
}

const displayedDistricts = {
    'FILTER_DP_ORIGINAL': null,
    'FILTER_DP_BEST': null,
    'FILTER_DP_WORST': null,
    'FILTER_DP_AVERAGE': null,
    'FILTER_DP_RANDOM': null
}

const maximumPopulation = {
    'ALABAMA': {
        'AA': 1220723,
        'AS': 30393,
        'AI': 3861,
        'HISP': 90377
    },
    'FLORIDA' : {
        'AA': 2532737,
        'AS': 305053,
        'AI': 9985,
        'HISP': 2828443
    },
    'PENNSYLVANIA': {
        'AA': 1262954,
        'AS': 218334,
        'AI': 9695,
        'HISP': 553700
    }
}

const numberOfPrecincts = {
    "ALABAMA": 1993,
    "FLORIDA": 9373,
    "PENNSYLVANIA": 9256
}