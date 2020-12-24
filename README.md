# Redistricter
A tool which can be used to generate and analyze racial bias within congressional districts.

## Requirements
### Local server
* Linux (or WSL)
* Python >= 3.6
* JDK 9+
* GNU parallel
* Maven
* Machine should be VPN connected to the SeaWulf

### Python requirements for preprocessing data
* Fiona
* Shapely
* NumPY

### Remote HPC Cluster
* SLURM
* Python >= 3.6
* GNU parallel

## Filesystem Setup
* Create SSH keys and copy to remote server **Note the information located at the top of the SeaWulfHandler class regarding the requirements of the SSH keyfiles or JSch will complain and not function correctly**
* Place SSH keys in `server/.ssh/` as `sshkey`
* Copy `algorithm/` to remote server such that resulting files are stored in `~/algorithm`
* For local jobs, place algorithm files in `~/algorithm`
* Place .algo files (located under `data/`) in `~/data` on remote server and local server

# Running the server
Launch the server by executing `mvn spring-boot:run` in the `server/` directory.

## Configuration
* `server/src/main/application.properties` should be modified to change the default values of `sshUser` and `sshHost` to more appropriate values, at a minimum.
* Local and remote jobs should be enabled or disabled using `sshEnabled` and `localJobsEnabled`, as desired.
* `server/src/main/resources/persistence.xml` should have the database credentials' fields updated, as necessary.
* Any desired changes made to the slurm script should be made within the `SeaWulfHandler#createSlurmScript` method. This includes desired changes for number of nodes, number of cores, target queue names, maximum run times, etc.
* `maxServerProcessingThreads` is used for threading of results processing and should be set to a value appropriate for the machine the server is running on.

## Populating the DB
Due to the size of the data.sql file which is used to populate the DB (>150mb), it is not included by default and must be generated.
* Invoke `generate_sql.py` with a given state abbreviation and specifying an output file (eg, `generate_sql.py al al.sql` for Alabama. Note the requirements of files and locations listed at the top of the script, but these should all already exist within this repo).
* Invoke `merge_sql.py` to merge + copy the three resulting sql population files to the correct location so that the server will read + populate the db at startup
* Change database.action in persistence.xml to 'drop-and-create' and uncomment the location of the sql-load-script-source file
* Start the server using `mvn spring-boot:run` and wait

## Forcing the generation of Enacted CD information
Enacted boundary information *should* be generated upon the first request for it. In the case that it is not, you will need to perform the following:
* Create a job for each state in the DB. The job does not actually have to execute, it just needs to exist in the DB.
* The 'generated_plans' field should be a JSON list of the enacted plan id (eg, `[1]` for AL, `[2]` for FL, etc..)
* `average_plan`, `max_plan`, `min_plan`, `random_plan` should all point to that same plan number (eg, `1` for AL, `2` for FL, etc..)
* `state` should be updated to the state name (eg, 'ALABAMA', 'FLORIDA', etc)
* `slurm_job` can be set to any non-NULL integer value
* `parameters` should be a FK into the parameters DB which represents the parameters object of a job associated with that same state (it just needs to exist in the DB)
* `job_status` should be `FINISHED`

For example, in our DB the population job id for AL looks like:
```
# job_id, generated_plans, job_status, average_plan, max_plan, min_plan, random_plan, parameters, slurm_job, local_pid, state, baw_data
'54', '[1]', 'FINISHED', '1', '1', '1', '1', '48', '111', NULL, 'ALABAMA', NULL
```

where `1` is the FK into the districting_plans table for the enacted AL CD,`48` is a FK into the parameters table for a job associated with AL, and `111` is a nonsense value for the slurm_id

* After the DB has been modified, you can use Postman or CURL to visit the `/testGenerateGeoDP/jobID=X` endpoint, where X is the job_id (it would be `54` in the above given example)
* You will also need to force the population information generation using either Postman or CURL to visit the `/testForcePopGeneration?districtingPlanID=X` endpoint, where X is the districting_plan table PK to populate (in the above example, it would be `1`)

Once this is done, you can either delete the rows from the DB or change the `state` columns to `NONE` so they do not show in the job history



## Preprocessing Scripts
* `identify_neighbors.py` : Identify precinct neighbors within a state
    ```
    identify_neighpors.py [input shapefile] [outfile]
    ```
    **NOTE: The input shapefile must NOT include any MultiPolygons. Single Polygon features with the same GEOID are fine, however. The easiest way to enforce this requirement is to use the `explode` command in MapShaper, or to `Multipart to Singleparts` in QGIS.** \
    Pregenerated output from this script is generally supplied as `output/*_neighbors.json` files.
    
* `compute_pop.py` : Compute the population of each VTD based upon the blockgroups it contains. Generally, the output file is used by `merge_neighbors_and_pop.py`, as well as injecting the population information into the simplified GeoJSON files supplied to the client.
  ```
  compute_pop.py [blockgroup file] [cvap file] [json file to join with] [output file]
  ```
  where 
  - `blockgroup file` is a blockgroup/county/VTD mapping file (generally included as `*_vtd.txt` files)
  - `CVAP file` is a CVS file containing demographic population information by blockgroup (generally included as `*_cvap.csv` files)
  - `JSON file to join with` is a GeoJSON file to join the population information against (generally included as `*_json.json` files) \
  Pregenerated output from this script is generally supplied as information injected into `output/*_pop_simplified.json`.
  
* `compute_districting.py` : Compute the enacted districting plan
  ```
  compute_districting.py [CD assignment file] [VTD assignment file] [JSON to join against] [output file]
  ```
  where 
  - `CD Assignment file` is a blockgroup/district mapping file, available from the Census Bureau (generally included as `*_CD113.txt` files)
  - `VTD Assigment file` is a blockgroup/county/VTD mapping file, available from the Census Bureau (generally included as `*_vtd.txt` files)
  - `JSON to join against` is the GeoJSON file to join the VTD/CD information against each precinct's GEOID (generally included as `*_json.json` files)\
  Pregenerated output from this script is generally supplied as `output/*.cd` files.
  
* `generate_sql.py` : Used to generate the SQL statements required to populate the DB for a given state
  ```
   generate_sql.py [state abbreviation] [output file]
   ```
   where
   - `State Abbreviation` is the abbreviation of the state, such that the directory `{state_abbreviation}_data/` exists. \
   This script requires also that the following files exist within that directory:
   - `{abbrev}.cd` (generated by `compute_districting.py` script)
   - `{abbrev}_outline.geojson` : A GeoJSON file containing the original, unmodified state outline
   - `{abbrev}_neighbors.json` (generated by the `compute_neighbors.py` script)
   - `{abbrev}_db.geojson` : A GeoJSON file containing the original, _unsimplified_ boundary and demographic information. \
   Due to the size of these files, they are not included in this repo by default.
   
* `merge_neighbors_and_pop.py` : Merges neighbor and population information into files suitable for use by the algorithm.
  ```
  merge_neighbors_and_pop.py [neighbors file] [geojson file] [output file]
  ```
  where 
  - `Neighbors File` is a file containing precinct neighbor information as generated by `identify_neighbors.py`
  - `GeoJSON file` is a file containing population information as generated by `compute_pop.py`  \
  Pregenerated output from this script is generally supplied as `output/*.algo` files.
  
* `merge_sql.sh` : Merges the SQL files for each state into a larger, single SQL file and moves it into the correct location for populating the DB on startup.

### The following are more "utility" scripts, used to verify or view the resulting outputs
* `convert_json_to_csv.py` : Converts the `.cd` files (and individual algorithm result plan files, not the summary files) to a CSV format so they may be joined against Shape/GeoJSON files in QGIS using the MMQGIS plugin and the resulting plans may be viewed.
  ```
  convert_json_to_csv.py [json file] [csv file]
  ```
  where 
  - `JSON file` is the `.cd` or _single algorithm result_ file
  - `CSV file` is the output file

* `dump.py` : Removes linebreaks from a JSON file, to reduce filesize.
  ```
  dump.py [infile] [outfile]
  ```
 
* `verify_algo.py` : Verifies the resulting .algo file from `merge_neighbors_and_pop.py`. Ensures that all records have neighbors and population information.
  ```
  verify_algo.py [algo file to verify]
  ```
  where 
  - `algo file to verify` is the file to verify
  
* `verify_neighbors.py` : Verifies the resulting .neighbors file from `identify_neighbors.py`. Ensures that all nodes have a neighbors list and that all node connections are bi-directional.
  ```
  verify_neighbors.py [neighbors file to check]
  ```
  where 
  - `neighbors file to check` is the file to verify
