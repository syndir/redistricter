from collections import defaultdict
from cluster import Cluster
import random

class MST:
    # edges_list = {1: {nodeID1: nodeID2}, 2: {nodeID1, nodeID3}, ...}
    # edges = {nodeID1: [nodeID2, nodeId3], ...}
    def __init__(self, id1, id2, pop_diff, pop_total, range, compactness, original_compactness, edge_tracker):
        self.edges_to_cut = dict()
        self.edges = defaultdict(list)
        self.pop_diff = pop_diff
        self.pop_total = pop_total
        self.original_compactness = original_compactness
        self.range = range
        self.compactness_constraint = compactness
        self.c1 = id1
        self.c2 = id2
        self.edge_count = 0
        self.edge_tracker = edge_tracker

    def add_edge(self, edge):
        self.edges[edge[0]].append(edge[1])
        self.edges[edge[1]].append(edge[0])
        self.edges_to_cut[self.edge_count] = edge
        self.edge_count += 1

    def cut_edge(self, nodes):
        running = True
        clusters = [None, None]
        while running:
            if self.edge_count == 0:
                break
            edge, key = self.get_edge()
            self.edges_to_cut.pop(key)
            self.edge_count -= 1
            temp_tracker = [0]
            if self.population_check(edge, nodes, temp_tracker):
                temp1 = Cluster(self.c1)
                temp2 = Cluster(self.c2)
                self.cluster_add_nodes(edge[0], temp1, nodes, {edge[1]: 1})
                self.cluster_add_nodes(edge[1], temp2, nodes, {edge[0]: 1})
                if self.compactness_check(temp1, temp2, temp_tracker):
                    temp1.update_nodes_cluster()
                    temp2.update_nodes_cluster()
                    clusters[0] = temp1
                    clusters[1] = temp2
                    running = False
            elif self.edge_count == 0:
                running = False
            if temp_tracker[0] == 2:
                self.edge_tracker["improve"] += 1
            elif temp_tracker[0] == 1:
                self.edge_tracker["accept"] += 1
            else:
                self.edge_tracker["bad"] += 1
            self.edge_tracker["total"] += 1
        return clusters[0], clusters[1]

    def get_edge(self):
        key = random.choice(list(self.edges_to_cut.keys()))
        return self.edges_to_cut[key], key

    def cluster_add_nodes(self, root, cluster, nodes, seen):
        cluster.add_node(nodes[root], False)
        seen[root] = 1
        n = len(self.edges[root])
        if n == 1:
            return
        for i in range(n):
            temp = self.edges[root][i]
            if temp in seen:
                continue
            else:
                self.cluster_add_nodes(temp, cluster, nodes, seen)

    def population_check(self, edge, nodes, tracker):
        p1 = self.calculate_population(edge[0], nodes, {edge[1]: 1})
        p2 = self.pop_total - p1
        pop_diff = abs(p1 - p2)
        if pop_diff <= self.pop_diff:
            tracker[0] = 2
            return True
        elif self.range[0] <= p1 <= self.range[1] and self.range[0] <= p2 <= self.range[1]:
            tracker[0] = 1
            return True
        return False

    def calculate_population(self, root, nodes, seen):
        pop = nodes[root].population
        seen[root] = 1
        n = len(self.edges[root])
        if n == 1:
            return pop
        for i in range(n):
            temp = self.edges[root][i]
            if temp in seen:
                continue
            pop += self.calculate_population(temp, nodes, seen)
        return pop

    # check if two clusters for compactness constraint
    def compactness_check(self, c1, c2, tracker):
        compactness1 = c1.compute_compactness()
        compactness2 = c2.compute_compactness()
        avg_compactness = (compactness1 + compactness2) / 2
        if avg_compactness <= self.original_compactness:
            return True
        elif avg_compactness <= self.compactness_constraint:
            if tracker[0] == 2:
                tracker[0] = 1
            return True
        tracker[0] = 0
        return False

    def print(self):
        print(f"merging clustesr: {self.c1}, {self.c2}")
        print(f"population difference {self.pop_diff}")
        print(f"original compactness {self.original_compactness}")
        print()
