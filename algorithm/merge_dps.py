import json
import glob
import sys

def merge(output_path):
    summary = {}
    # path = f"jobs/{job_id}/output"
    for f in glob.glob(f"{output_path}/*.json"):
        with open(f, "r") as dp_file:
            dp = json.load(dp_file)
            dp_id = f.split("/")[-1].split(".")[0]
            summary[dp_id] = dp

    with open(f"{output_path}/results.json", "w") as summary_file:
        json.dump(summary, summary_file, indent=2)

if __name__ == "__main__":
    if len(sys.argv) == 2:
        output_path = sys.argv[1]
        merge(output_path)
    else:
        print(f"{sys.argv[0]} [directory containing output files to merge]")