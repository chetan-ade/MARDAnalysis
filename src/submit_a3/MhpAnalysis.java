package submit_a3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import dont_submit.MhpQuery;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
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
    
    ArrayList < NodePEG > getVarAccesses(HashSet<Node> queryVarObjs, PEG peg, String field) {
    	
    	HashSet < NodePEG > varAccess = new HashSet < NodePEG > ();

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
    				SootField f = leftOpFieldRef.getField();
    				
    				HashSet < Node > valObjs = peg.findPointsToList((Local) left);
    				
    				if(peg.hasIntersection(queryVarObjs, valObjs) && f.getName().equals(field)) 
    					varAccess.add(n);
    				
    			}
    			
    			if(rightOp instanceof FieldRef) {
    				
    				InstanceFieldRef rightOpFieldRef = (InstanceFieldRef) rightOp;
    				Value right = rightOpFieldRef.getBase();
    				SootField f = rightOpFieldRef.getField();
    				
    				HashSet < Node > valObjs = peg.findPointsToList((Local) right);
    				
    				if(peg.hasIntersection(queryVarObjs, valObjs) && f.getName().equals(field)) 
    					varAccess.add(n);
    				
    			}
    			
    		}
    	}
    	
    	return new ArrayList<NodePEG> (varAccess);
    }
    
    Local strToLocal(String varName, HashSet < SootMethod > methods) {
    	
    	Local local = null;
    	
    	for(SootMethod method : methods) {
    		for(Local l : method.getActiveBody().getLocals()) {
    			
    			if(l.toString().equals(varName))
    				local = l;
    		}
    	}
    	
    	return local;
    }
    
    Boolean runInParallel(NodePEG a, NodePEG b, PEG peg) {
    	
    	return (peg.M.get(a).contains(b) || peg.M.get(b).contains(a));
    
    }
    
    Boolean isWrite(NodePEG n) {
    	
    	Unit u = n.unit;
		Stmt s = (Stmt) u;
		
		if(s instanceof DefinitionStmt) {
			
			DefinitionStmt ds = (DefinitionStmt) s;
		
			Value leftOp = ds.getLeftOp();
			
			if(leftOp instanceof FieldRef) 
				return true;
			
		}
		
		return false;
    }
    
    String analyseQuery(MhpQuery q, PEG peg, HashSet < SootMethod > methods) {
    	
    	String varStr = q.getVar();
    	Local var = strToLocal(varStr, methods);
    	String field = q.getField();
    	
    	HashSet<Node> queryVarObjs = peg.findPointsToList(var);
    	ArrayList < NodePEG > varAccesses = getVarAccesses(queryVarObjs, peg, field);
    	
    	for(int i = 0; i < varAccesses.size(); i++) {
    		for(int j = i + 1; j < varAccesses.size(); j++) {
    			
    			NodePEG a = varAccesses.get(i);
    			NodePEG b = varAccesses.get(j);
    			
    			if(runInParallel(a, b, peg)) {
    				if(isWrite(a) || isWrite(b)) {
    					return "Yes";
    				}
    			}
    		}
    	}
    	
    	return "No";
    }
     
    // Soot's Internal Transform Method
    @Override
    protected void internalTransform(String phaseName, Map < String, String > options) {

        HashSet < SootClass > classes = findAllClasses(); // Get the List of All Classes
        HashSet < SootMethod > methods = findAllFunctions(classes); // Get the List of All Functions 

        PEG peg = new PEG(methods); // Create a PEG for the entire program
        peg.workListIterate(); // Start the workList Iterate Algorithm

        for(int i = 0; i < A3.queryList.size(); i++) {
        	MhpQuery q = A3.queryList.get(i);
        	A3.answers[i] = analyseQuery(q, peg, methods);
        }
    }

}