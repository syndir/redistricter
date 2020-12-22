from collections import defaultdict

class Node:
    # edges: {nodeId1: 1, nodeId2: 1, nodeId3: 1}
    def __init__(self, node_id, population):
        self.id = node_id
        self.population = population
        self.edges = defaultdict(int)
        self.cluster = None

    def add_edge(self, node_id):
        self.edges[node_id] = 1

    def set_cluster(self, cluster):
        self.cluster = cluster

    def print(self):
        print(f"Node {self.id}: -----------------------------------")
        print(f"edges: {self.edges.keys()}")
        print(f"cluster: {self.cluster}")
        print(f"population: {self.population}")
        print()
