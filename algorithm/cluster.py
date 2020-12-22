class Cluster:
    # nodes: {nodeID : Node, ...}
    # edges: [[nodeId1,nodeId2],[nodeId3, nodeId4], ...]
    # neighbor_cluster: {clusterId : Cluster, ...}
    # neighborNodes: {nodeID : 1, ...}
    def __init__(self, clusterID):
        self.id = clusterID
        self.nodes = dict()
        self.edges = list()
        self.neighbor_clusters = dict()
        self.neighbor_nodes = dict()
        self.neighbor_nodes_in_cluster = dict()
        self.total_population = 0
        self.total_nodes = 0

    # add new node
    def add_node(self, n, perm):
        self.nodes[n.id] = n
        self.total_nodes += 1
        if perm:
            n.set_cluster(self.id)
        self.total_population += n.population
        if n.id in self.neighbor_nodes:
            self.neighbor_nodes.pop(n.id)
        for adj in n.edges:
            if adj not in self.nodes:
                self.neighbor_nodes[adj] = 1
            if adj in self.nodes:
                self.edges.append([n.id, self.nodes[adj].id])

    def update_nodes_cluster(self):
        for node in self.nodes:
            self.nodes[node].set_cluster(self.id)

    def add_neighbor(self, cluster):
        self.neighbor_clusters[cluster.id] = cluster

    # get edges betweeen two clusters
    def get_neighbor_edges(self, cluster):
        l = []
        for u in self.nodes:
            for v in cluster.nodes:
                if v in self.nodes[u].edges:
                    l.append([u, v])
        return l

    # remove itself from all its neighboring clusters
    def remove_neighbor_clusters(self):
        for c in self.neighbor_clusters:
            self.neighbor_clusters[c].neighbor_clusters.pop(self.id)

    def set_in_cluster(self, node):
        self.neighbor_nodes.pop(node)
        self.neighbor_nodes_in_cluster[node] = 1

    def compute_compactness(self):
        total = self.total_nodes
        exterior = 0
        for node in self.nodes:
            for adj in self.nodes[node].edges:
                if adj in self.neighbor_nodes or adj in self.neighbor_nodes_in_cluster:
                    exterior += 1
                    break
        return exterior/total

    def print(self):
        print(f"cluster: {self.id} --------------------------------------------")
        # print(f"edges: {self.edges}")
        print(f"nodes: {self.nodes.keys()}")
        print(f"neighbor nodes: %s" % self.neighbor_nodes.keys())
        print(f"neighbor nodes in a cluster: {self.neighbor_nodes_in_cluster.keys()}")
        # print(f"edges: {len(self.edges)}")
        # print(f"nodes: {len(self.nodes.keys())}")
        # print(f"neighbor nodes: {len(self.neighbor_nodes.keys())}")
        # print(f"neighbor nodes in a cluster: {len(self.neighbor_nodes_in_cluster.keys())}")
        print(f"neighbor clusters: {self.neighbor_clusters.keys()}")
        print(f"population: {self.total_population}")
        print(f"compactness: {self.compute_compactness()}")
        print()

