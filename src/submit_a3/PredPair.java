package submit_a3;

// Pair of Predecessor node and the type of edge between them
class PredPair {

    NodePEG predNode; // Predecessor Node
    String edgeType; // Edge Type [NORMAL, START => BEGIN, NOTIFY => NOTIFIEDENTRY, WAITING => NOTIFIEDENTRY]

    // Default Constructor
    PredPair() {}

    // Parameterized Constructor
    PredPair(NodePEG n, String type) {

        predNode = n;
        edgeType = type;

    }

}