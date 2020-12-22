-- districting_plans table
--   id (int) (PK) | districts (LONGTEXT) | sorted_districts (LONGTEXT) | variance (FLOAT)
-- 
--      id                  : the unique identifier of a districting plan in the table
--      districts           : JSON containing a list of identifiers which act as FKs in the districts table
--      sorted_districts    : JSON containing a list of identifiers which act as FKs in the districts table
CREATE TABLE districting_plans ( id INT NOT NULL AUTO_INCREMENT, districts JSON NOT NULL, sorted_districts JSON, variance FLOAT, geojson JSON, PRIMARY KEY (id) );

-- states table
--   state_selection (VARCHAR max 100) (PK) | enacted_plan (int) FK | boundary (LONGTEXT) | precincts (LONGTEXT)
-- 
--      state_selection     : the String representation of the StateSelection enum (use .name() method to get a String for it)
--      enacted_plan        : FK for a state's current plan in the districting_plans table
--      boundary            : JSON containing geojson info for a state boundary outline
--      precincts           : JSON containing a list of identifiers which act as FKs in the precincts table
CREATE TABLE states ( state_selection VARCHAR(100) NOT NULL, enacted_plan INT NOT NULL, boundary JSON, precincts JSON, PRIMARY KEY (state_selection) );

-- districts table
--    id (int) (PK) | precincts (LONGTEXT) | neighboring_districts (LONGTEXT) | total_pop (INT) | compactness (FLOAT) | num_counties (INT) | total_pop_by_demo (LONGTEXT) | vap_pop_by_demo (LONGTEXT)
--
--      id                      : the unique identifier of a district in the table
--      precincts               : JSON array of identifiers which act as FKs in precincts table
--      neighboring_districts   : JSON array of identifiers which act as FKs into districts table
--      total_pop               : total population of district
--      compactness             : compactness value of district
--      num_counties            : number of counties this district spans
--      total_pop_by_demo       : JSON of total population broken down by demographic
--      vap_pop_by_demo         : JSON of VAP broken down by demographic
CREATE TABLE districts ( id INT NOT NULL AUTO_INCREMENT, precincts JSON, neighboring_districts JSON, total_pop INT, num_counties INT, total_pop_by_demo JSON, vap_pop_by_demo JSON, client_geojson JSON, real_geojson JSON, PRIMARY KEY (id) ); 

-- precincts table
--    precinct_id (int) (PK) | precinct_name (VARCHAR max 100) | county_name (VARCHAR) | county_id (int) | total_pop_by_demo (LONGTEXT) | vap_pop_by_demo (LONGTEXT) | boundary (LONGTEXT) | precinct_neighbors (LONGTEXT)
--
--      precinct_id             : the unique identifier of a precinct in the table (GEOID)
--      precinct_name           : String name of the precinct
--      county_name             : String name of the county the precinct is located in
--      county_id               : integer identifier of county (COUNTYFP10 in the geojson)
--      total_pop_by_demo       : JSON of total population broken down by demographic
--      vap_pop_by_demo         : JSON of VAP broken down by demographic
--      boundary                : JSON containing geojson info for a state boundary outline
--      precinct_neighbors      : list of identifiers which act as FKs into precincts table
CREATE TABLE precincts ( precinct_id VARCHAR(100) NOT NULL, precinct_name VARCHAR(100) NOT NULL, county_name VARCHAR(100) NOT NULL, county_id INT NOT NULL, total_pop_by_demo JSON, vap_pop_by_demo JSON, boundary JSON, precinct_neighbors JSON, PRIMARY KEY (precinct_id) );

-- jobs table
--   job_id (int) (PK) | generated_plans (JSON) | status (VARCHAR) | average_plan (int) | max_plan (int) | min_plan (int) | random_plan (int) | parameters (int) | slurm_job (INT)
--
--      job_id                  : the unique identifier of a job in the table
--      generated_plans         : JSON array of the ids of the generated plans in the districting_plans table
--      job_status              : status of the job (the .name()'d enum value)
--      average_plan            : identifier of the average plan in the districting_plans table
--      max_plan                : identifier of the max plan in the districting_plans table
--      min_plan                : identifier of the min plan in the districting_plans table
--      random_plan             : identifier of the random plan in the districting_plans table
--      parameters              : identifier of the parameters associated with this job
--      slurm_job               : number of the slurm job associated with this job
CREATE TABLE jobs ( job_id INT NOT NULL AUTO_INCREMENT, generated_plans JSON, job_status VARCHAR(100), average_plan INT, max_plan INT, min_plan INT, random_plan INT, parameters INT, slurm_job INT, local_pid INT, state VARCHAR(45), baw_data JSON, PRIMARY KEY (job_id) );

-- parameters table
--   id (int) (PK) | state VARCHAR | compactness_measure (VARCHAR) | pop_variance FLOAT(5) | selected_demos JSON | num_seeds INT | num_iters INT
--
--      id                      : the unique identifier of a set of parameters in this table
--      state                   : state associated with the job request (.name()'d enum value)
--      compactness_measure     : compactness measure requested for the job (.name()'d enum value)
--      pop_variance            : population variance
--      selected_demos          : JSON Array of selected demographics
--      num_seeds               : number of seed plans
--      num_iters               : number of iterations per seed
CREATE TABLE parameters ( id INT NOT NULL AUTO_INCREMENT, state VARCHAR(100), compactness_measure VARCHAR(100), pop_variance FLOAT(5), selected_demos JSON, num_seeds INT, num_iters INT, PRIMARY KEY (id) );