package submit_a3;

import java.util.HashSet;
import soot.Unit;
import soot.jimple.spark.pag.Node;

class NodePEG {

    Unit unit; // The actual unit -> The same unit can belong to multiple nodes. (threadID, Unit) uniquely identify a NodePEG.

    HashSet < Node > object; // Object(s) of focus in unit

    String threadID; // Thread ID the unit belongs to

    String specialProperty; // Special Property of NodePEG if present (BEGIN, END, WAIT, WAITING, NOTIFIEDENTRY, NOTIFY, NOTIFYALL)

    // Default Constructor
    NodePEG() {

        specialProperty = "";

    }

    // Parameterized Constructors
    NodePEG(HashSet < Node > obj, Unit u, String id) {

        this(); // Call the Default Constructor

        unit = u;
        object = obj;
        threadID = id;

    }

    // Parameterized Constructor with specialProperty
    NodePEG(HashSet < Node > obj, Unit u, String id, String property) {

        this(obj, u, id); // Set object, unit and thread-id
        specialProperty = property; // Set property

    }

    // Utility Function to Print PEG Node
    String printNode() {

        // List of valid special properties
        HashSet < String > specialProperties = new HashSet < String > ();

        specialProperties.add("BEGIN");
        specialProperties.add("END");
        specialProperties.add("WAIT");
        specialProperties.add("WAITING");
        specialProperties.add("NOTIFIEDENTRY");
        specialProperties.add("NOTIFY");
        specialProperties.add("NOTIFYALL");

        // Compute String Representation of Object
        String objString = "this";

        if (!(object == null)) {

            objString = "[";

            for (Node obj: object)
                objString = objString + PEG.objects.get(obj) + ","; // For cleaner print
            
            objString = objString.substring(0, objString.length() - 1); // Remove last ','
            objString += "]";

        }

        // Return String with Special Property instead of unit (if present in list)
        if (specialProperties.contains(specialProperty))
            return "(" + objString + " , " + specialProperty.toString() + " , " + threadID.toString() + ")";

        // Otherwise, Return unit
        else
            return "(" + objString + " , " + unit.toString() + " , " + threadID.toString() + ")";

    }

}