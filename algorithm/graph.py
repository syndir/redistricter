import random
import json
from cluster import Cluster
from node import Node
from MST import MST
import statistics

class Graph:
    # nodes: {nodeId : Node, ...}
    # clusters: {clusterID: Cluster, ...}
    def __init__(self):
        self.nodes = dict()
        self.clusters = dict()
        self.variance = 0
        self.compactness = 1
        self.total_population = 0
        self.avg_population = 0
        self.range = [0, 0]
        self.edge_tracker = {
            "total": 0,
            "improve": 0,
            "accept": 0,
            "bad": 0
        }

    def add_node(self, n):
        self.nodes[n.id] = n

    # set variance and calculate the range to be expects
    def set_variance(self, variance):
        self.variance = variance
        n = len(self.clusters)
        self.avg_population = self.total_population / n
        change = self.avg_population * variance / 2
        self.range[0] = self.avg_population - change
        self.range[1] = self.avg_population + change

    def set_compactness(self, compactness):
        self.compactness = compactness

    # put nodes into x clusters
    def init_seeding(self, x):
        init_nodes = random.sample(list(self.nodes.keys()), x)
        cluster_can_add = list()
        for i in range(x):
            temp_cluster = Cluster(str(i+1))
            temp_cluster.add_node(self.nodes[init_nodes[i]], True)
            self.clusters[str(i+1)] = temp_cluster
            cluster_can_add.append(temp_cluster)
        n = len(cluster_can_add)
        while n > 0:
            j = int(random.random() * n)
            temp_cluster = cluster_can_add[j]
            node = self.find_to_add(temp_cluster)
            if node is None:
                cluster_can_add.pop(j)
                n -= 1
                self.total_population += temp_cluster.total_population
                continue
            temp_cluster.add_node(self.nodes[node], True)

    # find node can be added to cluster
    def find_to_add(self, cluster):
        n = len(cluster.neighbor_nodes)
        while n > 0:
            node = random.choice(list(cluster.neighbor_nodes.keys()))
            if self.nodes[node].cluster is None:
                return node
            else:
                cluster.set_in_cluster(node)
                n -= 1
                if self.nodes[node].cluster not in cluster.neighbor_clusters:
                    cluster.add_neighbor(self.clusters[self.nodes[node].cluster])
                    self.clusters[self.nodes[node].cluster].add_neighbor(cluster)
        return None

    def merge_cluster(self):
        while True:
            mst = self.get_mst()
            c1, c2 = mst.cut_edge(self.nodes)
            if c1 is not None:
                self.clusters[c1.id].remove_neighbor_clusters()
                self.clusters[c2.id].remove_neighbor_clusters()
                self.clusters[c1.id] = c1
                self.clusters[c2.id] = c2
                self.update_neighbor_clusters(c1, c2)
                break

    def merge_cluster_skip_version(self):
        mst = self.get_mst()
        c1, c2 = mst.cut_edge(self.nodes)
        if c1 is not None:
            self.clusters[c1.id].remove_neighbor_clusters()
            self.clusters[c2.id].remove_neighbor_clusters()
            self.clusters[c1.id] = c1
            self.clusters[c2.id] = c2
            self.update_neighbor_clusters(c1, c2)

    # Get MST of two clusters using Kruskal's algorithm
    def get_mst(self):
        c1 = random.choice(list(self.clusters.keys()))
        c2 = random.choice(list(self.clusters[c1].neighbor_clusters.keys()))
        cluster1 = self.clusters[c1]
        cluster2 = self.clusters[c2]
        common_edges, pop_diff, pop_total, avg_comp = self.compute_necessary_data(cluster1, cluster2)
        edges = common_edges + cluster1.edges + cluster2.edges
        mst = MST(c1, c2, pop_diff, pop_total, self.range, self.compactness, avg_comp, self.edge_tracker)
        if len(common_edges) == 0 or len(edges) == 0:
            return mst
        random.shuffle(edges)
        vertices = len(cluster1.nodes) + len(cluster2.nodes)
        i, e = 0, 0
        parent = dict()
        rank = dict()
        for id in cluster1.nodes:
            parent[id] = id
            rank[id] = 0
        for id in cluster2.nodes:
            parent[id] = id
            rank[id] = 0
        while e < vertices-1:
            u, v = edges[i]
            i = i + 1
            x = self.find(parent, u)
            y = self.find(parent, v)
            if x != y:
                e = e + 1
                mst.add_edge([u, v])
                self.apply_union(parent, rank, x, y)
        return mst

    def compute_necessary_data(self, c1, c2):
        commonEdges = c1.get_neighbor_edges(c2)
        pop_diff = abs(c1.total_population - c2.total_population)
        pop_total = c1.total_population + c2.total_population
        compactness1 = c1.compute_compactness()
        compactness2 = c2.compute_compactness()
        avg_compactness = (compactness1 + compactness2) / 2
        return commonEdges, pop_diff, pop_total, avg_compactness

    # find root of the node, used in Kruskal's algorithm
    def find(self, parent, id):
        if parent[id] == id:
            return id
        return self.find(parent, parent[id])

    # update roots/rank after adding new edge, used in Kruskal's algorithm
    def apply_union(self, parent, rank, x, y):
        xroot = self.find(parent, x)
        yroot = self.find(parent, y)
        if rank[xroot] < rank[yroot]:
            parent[xroot] = yroot
        elif rank[xroot] > rank[yroot]:
            parent[yroot] = xroot
        else:
            parent[yroot] = xroot
            rank[xroot] += 1

    def update_neighbor_clusters(self, c1, c2):
        keys = list(c1.neighbor_nodes.keys())
        for key in keys:
            temp = self.nodes[key].cluster
            if temp is None or temp == c1.id:
                continue
            c1.add_neighbor(self.clusters[temp])
            self.clusters[temp].add_neighbor(c1)
            c1.set_in_cluster(key)
        keys = list(c2.neighbor_nodes.keys())
        for key in keys:
            temp = self.nodes[key].cluster
            if temp is None or temp == c2.id:
                continue
            c2.add_neighbor(self.clusters[temp])
            self.clusters[temp].add_neighbor(c2)
            c2.set_in_cluster(key)

    # used to load fake data
    def load(self, data):
        for id in data:
            pop = int(random.random() * 1000 + 1000)
            n = Node(id, pop)
            for edge in data[id]:
                n.add_edge(edge)
            self.add_node(n)

    # load data from file
    def load_data(self, file):
        with open(file) as f:
            data = json.load(f)
        for id in data:
            pop = data[id]['population']
            n = Node(id, pop)
            for adj in data[id]['neighbors']:
                n.add_edge(adj)
            self.add_node(n)

    def print_clusters(self):
        print(f"mean population: {self.avg_population}")
        print(f"variance: {self.variance}")
        print(f"Compactness: {self.compactness}")
        print(f"range: {self.range}")
        print("   id | population | compactness")
        print("----------------------------------------")
        pop = 0
        l = list()
        l2 = list()
        for c in self.clusters:
            # self.clusters[c].print()
            cluster = self.clusters[c]
            temp = cluster.total_population
            temp_comp = cluster.compute_compactness()
            l.append(temp)
            l2.append(temp_comp)
            print(f"    {c} | {temp}      {temp_comp}")
            pop += temp
        print(f"total | {pop}")
        print(f"std: {statistics.stdev(l)}")
        print(f"avg compactness {sum(l2) / len(l2)}")
        print()

    def print_clusters_final(self):
        for c in self.clusters:
            self.clusters[c].print()

    def print_nodes(self):
        for n in self.nodes:
            self.nodes[n].print()