package submit_a3;

import java.util.HashSet;
import java.util.Map;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

public class MhpAnalysis extends SceneTransformer {

    // Utility Function that computes list of all classes present in the program
    HashSet < SootClass > findAllClasses() {

        // Convert Chain of classes to HashSet of classes
        Chain < SootClass > classes = Scene.v().getApplicationClasses();
        HashSet < SootClass > listClasses = new HashSet < SootClass > (classes);

        return listClasses;

    }

    // Utility Function that computes a list of all functions present in the program
    HashSet < SootMethod > findAllFunctions(HashSet < SootClass > classes) {

        HashSet < SootMethod > methods = new HashSet < SootMethod > (); // Get the List of All Functions

        // Iterate over all methods of all classes and compute list of methods in the program (ignoring constructors)
        for (SootClass _class: classes)
            for (SootMethod method: _class.getMethods())
                if (!method.isConstructor())
                    methods.add(method);

        return methods;

    }

    // Soot's Internal Transform Method
    @Override
    protected void internalTransform(String phaseName, Map < String, String > options) {

        HashSet < SootClass > classes = findAllClasses(); // Get the List of All Classes
        HashSet < SootMethod > methods = findAllFunctions(classes); // Get the List of All Functions 

        PEG peg = new PEG(methods); // Create a PEG for the entire program

        peg.printPEG(); // Print initial state of PEG (without notify edges)
        peg.workListIterate(); // Start the workList Iterate Algorithm

    }

}