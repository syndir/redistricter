var jobsData = {
    "2": {
        "PENDING": {
            "id": 6,
            "state": "ALABAMA",
            "compactnessMeasure": "HIGH_COMPACTNESS",
            "populationVariance": 10.0,
            "selectedDemographics": {
                "AFRICAN_AMERICAN": true
            },
            "numSeedPlans": 5,
            "numIterations": 5000
        }
    },
    "3": {
        "PENDING": {
            "id": 7,
            "state": "ALABAMA",
            "compactnessMeasure": "HIGH_COMPACTNESS",
            "populationVariance": 10.0,
            "selectedDemographics": {
                "AFRICAN_AMERICAN": true
            },
            "numSeedPlans": 5,
            "numIterations": 5000
        }
    },
    "4": {
        "PENDING": {
            "id": 8,
            "state": "ALABAMA",
            "compactnessMeasure": "HIGH_COMPACTNESS",
            "populationVariance": 10.0,
            "selectedDemographics": {
                "AFRICAN_AMERICAN": true
            },
            "numSeedPlans": 5,
            "numIterations": 5000
        }
    },
    "5": {
        "PENDING": {
            "id": 9,
            "state": "FLORIDA",
            "compactnessMeasure": "HIGH_COMPACTNESS",
            "populationVariance": 10.0,
            "selectedDemographics": {
                "AFRICAN_AMERICAN": true
            },
            "numSeedPlans": 5,
            "numIterations": 5000
        }
    }
}

var planData = [
    {label: "1", y:[0.09, 0.11, 0.14, 0.17, 0.13]},
    {label: "2", y:[0.13, 0.15, 0.17, 0.19, 0.16]},
    {label: "3", y:[0.14, 0.15, 0.18, 0.21, 0.16]},
    {label: "4", y:[0.15, 0.16, 0.19, 0.25, 0.18]},
    {label: "5", y:[0.17, 0.18, 0.23, 0.26, 0.20]},
    {label: "6", y:[0.17, 0.20, 0.25, 0.27, 0.22]},
    {label: "7", y:[0.19, 0.21, 0.29, 0.32, 0.25]}
]
var planData2 = [
    [0.09, 0.11, 0.14, 0.17, 0.13],
    [0.13, 0.15, 0.17, 0.19, 0.16],
    [0.14, 0.15, 0.18, 0.21, 0.16],
    [0.15, 0.16, 0.19, 0.25, 0.18],
    [0.17, 0.18, 0.23, 0.26, 0.20],
    [0.17, 0.20, 0.25, 0.27, 0.22],
    [0.19, 0.21, 0.29, 0.32, 0.25]
]

var popupData = {
    "Alabama":{
        "density": 94.65,
        "population": "4,903,185",
        "land area": "52,419 sq mi",
        "# of congressional districts": "7",
        "African-American": "26.8%",
        "Asian-American": "1.5%",
        "Hispanic": "4.6%",
        "Native-American": "0.7%",
        "Registered Voters - Democrats" : "XX",
        "Registered Voters - Republicans" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Florida":{
        "density": 353.4,
        "population": "21,477,737",
        "land area": "65,758 sq mi",
        "# of congressional districts": "27",
        "African-American": "16.9%",
        "Asian-American": "3.0%",
        "Hispanic": "26.4%",
        "Native-American": "0.5%",
        "Registered Voters - Democrats" : "XX",
        "Registered Voters - Republicans" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Pennsylvania":{
        "density": 284.3,
        "population": "12,801,989",
        "land area": "44,817 sq mi",
        "# of congressional districts": "19",
        "African-American": "12.0%",
        "Asian-American": "3.8%",
        "Hispanic": "7.8%",
        "Native-American": "0.4%",
        "Registered Voters - Democrats" : "XX",
        "Registered Voters - Republicans" : "XX",
        "Registered Voters - Other" : "XX"
    }
}

var districtPopupData = {
    "Alabama" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Florida" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Pennsylvania" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    }
}

var precinctPopupData = {
    "Alabama" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Florida" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    },
    "Pennsylvania" : {
        "population" : "XX",
        "land area" : "XX sq mi",
        "African-American" : "XX%",
        "Asian-American" : "XX%",
        "Hispanic" : "XX%",
        "Native-American" : "XX%",
        "Registered Voters - Democrat" : "XX",
        "Registered Voters - Republican" : "XX",
        "Registered Voters - Other" : "XX"
    }   
}