# NOTE: to enable the version of the algo that will skip iterations when no valid edge is found in the MST,
# uncomment line 53: g.merge_cluster_skip_version() (this is the faster version), to enable the version of the algo that
# will ensure each iteration alters the graph is some way, uncomment line 52: g.merge_cluster() (much slower, but the
# results will be better representative of a longer random walk from the initial clustering)

# During testing it was found that forcing all 10k iterations to alter the graph vs allowing iterations to be
# skipped had a negligible effect on the resulting graph
from graph import Graph
import json
import time
import sys

def gen_plan(config_file, parallel_id):
    g = Graph()
    config = load_config(config_file)
    job_id = config["job"]
    data_file = "./data/" + config["state"] + ".algo"
    variance = config["populationVariance"]
    compactness = config["compactnessMeasure"]
    num_districts = config["numDistricts"]
    iterations = config["numIterations"]

    t = time.time()
    try:
        algorithm(g, data_file, variance, compactness, num_districts, iterations, parallel_id)
        generate_output(g, job_id, parallel_id)
    except Exception as e:
        generate_output(g, job_id, f"dp{parallel_id}_error")
        print(parallel_id, e)
    tt = g.edge_tracker["total"]
    i = g.edge_tracker["improve"]
    a = g.edge_tracker["accept"]
    b = g.edge_tracker["bad"]
    print(f"dp{parallel_id} edges: total-{tt}   improve-{i}({round(i/tt*100,4)}%)   "
          f"acceptable-{a}({round(a/tt*100,4)}%)   bad-{b}({round(b/tt*100,4)}%)")
    print(f"dp{parallel_id} time: {(time.time() - t)}")

def load_config(file):
    with open(file) as f:
        config = json.load(f)
    return config

def algorithm(g, data_file, variance, compactness, num_districts, iterations, id):
    g.load_data(data_file)
    g.init_seeding(num_districts)
    g.set_variance(variance)
    g.set_compactness(compactness)

    print(f"dp{id} initial clusters")
    g.print_clusters()
    for i in range(iterations):
        # g.merge_cluster()   # uncomment for the full iterations version
        g.merge_cluster_skip_version()  # uncomment for the skip iterations version
    print(f"dp{id} final clusters")
    g.print_clusters()

def generate_output(g, job_id, parallel_id):
    data = {}
    file = f"jobs/{job_id}/output/dp{parallel_id}.json"
    for district in g.clusters:
        data[district] = list()
        for precinct in g.clusters[district].nodes:
            data[district].append(precinct)
    with open(file, "w") as outfile:
        json.dump(data, outfile, indent=2)

if __name__ == "__main__":
    import cProfile, pstats
    profiler = cProfile.Profile()
    if len(sys.argv) > 1:
        profiler.enable()
        gen_plan(sys.argv[1], sys.argv[2])
        profiler.disable()
        stats = pstats.Stats(profiler).sort_stats('tottime')
        print(f"dp{sys.argv[2]} profiler --------------------------")
        stats.print_stats()
    else:
        profiler.enable()
        gen_plan("config.json", 1)
        profiler.disable()
        stats = pstats.Stats(profiler).sort_stats('tottime')
        stats.print_stats()
        # stats.dump_stats('profile.out')
