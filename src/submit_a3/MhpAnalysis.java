package submit_a3;

import java.util.HashSet;
import java.util.Map;

import dont_submit.MhpQuery;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.Node;
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

        // peg.printPEG(); // Print initial state of PEG (without notify edges)
        peg.workListIterate(); // Start the workList Iterate Algorithm
        peg.printM();

        for(int i = 0; i < A3.queryList.size(); i++) {
        	
        	MhpQuery q = A3.queryList.get(i);
        	
        	String leftVarStr = q.getLeftVar();
        	String rightVarStr = q.getRightVar();
        	
        	Local leftVar = null, rightVar = null;
        	
        	for(SootMethod method : methods) {
        		for(Local l : method.getActiveBody().getLocals()) {
        			
        			if(l.toString().equals(leftVarStr))
        				leftVar = l;
        			if(l.toString().equals(rightVarStr))
        				rightVar = l;
        		}
        	}
        	
        	HashSet<Node> leftVarObjs = peg.findPointsToList(leftVar);
        	HashSet<Node> rightVarObjs = peg.findPointsToList(rightVar);
        	
        	System.out.println(" LEFT VAR OBJS = " + leftVarObjs);
        	System.out.println(" LEFT VAR OBJS = " + rightVarObjs);
        	
//        	if(!peg.hasIntersection(leftVarObjs, rightVarObjs)) { // Empty Intersection
//        		A3.answers[i] = "NO";
//        		continue;
//        	}
        	
        	
        	HashSet < NodePEG > leftAccess = new HashSet < NodePEG > ();
        	HashSet < NodePEG > rightAccess = new HashSet < NodePEG > ();
        	
        	for(NodePEG n : peg.successors.keySet()) {
        		
        		Unit u = n.unit;
        		Stmt s = (Stmt) u;
        		
        		if(s instanceof DefinitionStmt) {
        			
        			DefinitionStmt ds = (DefinitionStmt) s;
        		
        			Value leftOp = ds.getLeftOp();
        			Value rightOp = ds.getRightOp();
        			
        			if(leftOp instanceof FieldRef) {
        				
        				
        				InstanceFieldRef leftOpFieldRef = (InstanceFieldRef) leftOp;
        				Value left = leftOpFieldRef.getBase();
        				
        				HashSet < Node > valObjs = peg.findPointsToList((Local) left);
        				
        				System.out.println(" LEFT FIELD REF = " + u + " val Objs : " + valObjs);
        				
        				
        				
        				if(peg.hasIntersection(leftVarObjs, valObjs)) 
        					leftAccess.add(n);
        				
        				if(peg.hasIntersection(rightVarObjs, valObjs))
        					rightAccess.add(n);
        				
        			}
        			
        			if(rightOp instanceof FieldRef) {
        				
        				InstanceFieldRef rightOpFieldRef = (InstanceFieldRef) rightOp;
        				Value right = rightOpFieldRef.getBase();
        				
        				HashSet < Node > valObjs = peg.findPointsToList((Local) right);
        				
        				System.out.println(" RIGHT FIELD REF = " + u + " val Objs : " + valObjs);
        				
        				if(peg.hasIntersection(leftVarObjs, valObjs)) 
        					leftAccess.add(n);
        				
        				if(peg.hasIntersection(rightVarObjs, valObjs))
        					rightAccess.add(n);
        				
        			}
        			
        		}
        	}
        	
        	System.out.println("LEFT ACCESS : ");
        	for(NodePEG l : leftAccess)
        		l.printNode();
        	
        	System.out.println("RIGHT ACCESS : ");
        	for(NodePEG r : rightAccess)
        		r.printNode();
        	
        }
    }

}