#!/usr/bin/env bash

#SBATCH --job-name=job3
#SBATCH --output=jobs/3/out.log
#SBATCH --ntasks-per-node=28
#SBATCH --nodes=1
#SBATCH --time=24:00:00
#SBATCH -p extended-28core

module load python/3.8.6
module load gnu-parallel/6.0

## HERE YOU MAKE A LIST OF THE COMMANDS TO RUN AND WRITE IT ALL TO A FILE
job_num=1
cur_job=1
num_seeds=28

mkdir -p jobs/$job_num/output
echo > jobs/$job_num/commands.txt
while [ $cur_job -le $num_seeds ]
do
    echo python3 algorithm/main.py jobs/$job_num/config.json $cur_job >> jobs/$job_num/commands.txt
    cur_job=$(expr $cur_job + 1)
done

echo RUNNING > jobs/$job_num/status.txt
date >> jobs/$job_num/out.log
parallel --verbose --jobs 28 < jobs/$job_num/commands.txt
python3 algorithm/merge_dps.py jobs/$job_num/output
date >> jobs/$job_num/out.log
echo FINISHED > jobs/$job_num/status.txt
