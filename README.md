# Redistricter
A tool which can be used to generate and analyze racial bias within
congressional districts.

## Requirements
### Local server
* Python >= 3.6
* JDK 9+
* GNU parallel
* Maven

### Python requirements for preprocessing data
* Fiona
* Shapely
* NumPY

### Remote HPC Cluster
* SLURM
* Python >= 3.6
* GNU parallel

## Setup
* Create SSH keys and copy to remote server
* Place SSH keys in `server/.ssh/` as `sshkey`
* Copy `algorithm/` to remote server such that resulting files are stored in
  `~/algorithm`
* For local jobs, place algorithm files in `~/algorithm`
* Place .algo files (located under `data/`) in `~/data` on remote server and local server

## Configuration
* `server/src/main/application.properties` should be modified to change the
default values of `sshUser` and `sshHost` to more appropriate values, at
a minimum.
* Local and remote jobs should be enabled or disabled, as desired.
* `server/src/main/resources/persistence.xml` should have the database
  credentials' fields updated, as necessary.

# Running
Launch the server by executing `mvn spring-boot:run` in the `server/` directory.
