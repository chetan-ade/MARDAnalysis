package submit_a3;

// Pair of Destination NodePEG and EdgeType
class SuccPair {

    NodePEG succNode; // Successor Node
    String edgeType; // Edge Type [NORMAL, START => BEGIN, NOTIFY => NOTIFIEDENTRY, WAITING => NOTIFIEDENTRY]

    // Default Constructor
    SuccPair() {}

    // Parameterized Constructor
    SuccPair(NodePEG n, String type) {

        succNode = n;
        edgeType = type;

    }

}