package submit_a3;

import java.util.ArrayList;

public class ReturnPair {

    NodePEG beginNode; // Begin Node of the newly added thread
    ArrayList < NodePEG > listOfStartNodes; // List of all start nodes of the newly added thread

    // Default Constructor
    ReturnPair() {}

    // Parameterized Constructor
    ReturnPair(NodePEG begin, ArrayList < NodePEG > listOfStarts) {

        this();

        beginNode = begin;
        listOfStartNodes = listOfStarts;

    }

}