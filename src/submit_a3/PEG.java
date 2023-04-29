package submit_a3;

import java.util.ArrayList;
import java.util.Map;

import soot.SootMethod;
import soot.Type;

import java.util.HashMap;
import java.util.HashSet;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class PEG {

    int threadIDCounter; // Counter to create threadID

    PointsToAnalysis pta; // Points To Analysis Object

    static HashMap < Node, String > objects; // AllocNode - String Unique Mapping

    // <------------------------------------------------------------------------------ DATA STRUCTURES FOR MAPPINGS -------------------------------------------------------------------------------> //

    HashMap < NodePEG, HashSet < SuccPair >> successors; // Program Execution Graph stored as a adjacency list

    HashMap < NodePEG, HashSet < PredPair >> predecessors; // Mapping of Node to its predecessors

    HashMap < NodePEG, HashSet < NodePEG > > M; // M(n) Map

    HashMap < NodePEG, HashSet < NodePEG > > OUT; // OUT(n) Map

    // Default Constructor
    PEG() {

        threadIDCounter = 0; // Initialize counter to 0

        pta = Scene.v().getPointsToAnalysis(); // Initialize PointsTo Analysis Object

        // Initialize all maps 
        objects = new HashMap < Node, String > ();
        successors = new HashMap < NodePEG, HashSet < SuccPair >> ();
        predecessors = new HashMap < NodePEG, HashSet < PredPair >> ();
        M = new HashMap < NodePEG, HashSet < NodePEG > > ();
        OUT = new HashMap < NodePEG, HashSet < NodePEG > > ();

    }

    // Parameterized Constructor
    PEG(HashSet < SootMethod > methods) {

        this(); // Call Default Constructor

        computeObjNames(methods); // Give each allocNode a unique object ID

        constructInitialPEG(methods); // Construct Initial PEG (without notify edges)

        populatePredecessors(); // Use Successors to populate predecessors map // TODO -> Whenever a new edge is added to PEG after initial construction, predecessors must also be filled  

        initializeM_OUT(); // Initialize M and OUT map with empty sets for all nodes

    }

    // Give each allocNode a unique object name
    void computeObjNames(HashSet < SootMethod > methods) {

        // Get list of all allocNodes in program
        HashSet < Node > allObjects = new HashSet < Node > ();

        for (SootMethod method: methods)
            for (Local l: method.getActiveBody().getLocals())
                allObjects.addAll(this.findPointsToList(l));

        // Give each allocNode object a unique ID
        int objID = 0;

        for (Node object: allObjects)
            objects.put(object, "Obj" + Integer.toString(objID++));

    }

    // Initialize M and Out for all nodes with empty set
    void initializeM_OUT() {

        for (NodePEG n: this.successors.keySet()) {

            M.put(n, new HashSet < NodePEG > ());
            OUT.put(n, new HashSet < NodePEG > ());

        }

    }

    // Print M(n) for all nodes(n) in PEG
    void printM() {

        System.out.println("M MAP\n");

        for (NodePEG n: M.keySet()) {

            System.out.println(n.printNode() + " -----> ");

            for (NodePEG m: M.get(n))
                System.out.println("\t" + m.printNode());

        }
    }

    // Print OUT(n) for all nodes(n) in PEG
    void printOUT() {

        System.out.println("OUT MAP\n");

        for (NodePEG n: OUT.keySet()) {

            System.out.println(n.printNode() + " -----> ");

            for (NodePEG m: OUT.get(n))
                System.out.println("\t" + m.printNode());

        }
    }

    // Construct the initial PEG by using CFG graphs, Start Information and modifying wait nodes
    void constructInitialPEG(HashSet < SootMethod > methods) {

        SootMethod mainFunction = findMain(methods); // Get the main function

        // List of threads started in Main Function (The initial list of start nodes)
        ReturnPair retPair = populateGraph(mainFunction);
        ArrayList < NodePEG > listOfStartNodes = retPair.listOfStartNodes;

        // Iterate till there are start nodes // TODO -> What if two threads call each other infinitely??
        while (!listOfStartNodes.isEmpty()) {

            NodePEG startNode = listOfStartNodes.get(0); // Store and pop a start node
            listOfStartNodes.remove(0);

            Type classOfVar = getClassOfStartNode(startNode); // Obtain class of variable

            // get run method of the class. Iterate over all methods and check if class of method is same as class of variable. Since only run method is present in a class, we need to check only if the classes are matching.
            SootMethod runMethod = null;
            for (SootMethod m: methods)
                if (m.getDeclaringClass().toString().equals(classOfVar.toString()))
                    runMethod = m;

            retPair = populateGraph(runMethod); // Populate Graph for the thread (an instance of the run method)

            listOfStartNodes.addAll(retPair.listOfStartNodes); // Add newly found start nodes to listOfStartNodes

            successors.get(startNode).add(new SuccPair(retPair.beginNode, "START => BEGIN")); // Add START edge from startNode to beginNode of the new thread

        }
    }

    // Once the initial successors has been created, populate the reverse mapping predecessors for all nodes
    void populatePredecessors() {

        for (NodePEG n: successors.keySet())
            this.predecessors.put(n, new HashSet < PredPair > ()); // Add empty list of Predecessors for all nodes

        for (NodePEG node: successors.keySet()) {
            for (SuccPair successor: successors.get(node)) {

                // Add node to the list of predecessors for successorNode
                NodePEG successorNode = successor.succNode;
                predecessors.get(successorNode).add(new PredPair(node, successor.edgeType));

            }
        }
    }

    // Returns the class of the variable in start unit. Eg, x.start() -> A (if x is an object of class A)
    Type getClassOfStartNode(NodePEG startNode) {

        return getVariableOfStartNode(startNode).getType();

    }
    
    // Returns the variable in start unit. Eg, x.start() -> x (if x is an object of class A)
    Value getVariableOfStartNode(NodePEG startNode) {

        Stmt s = (Stmt) startNode.unit; // Get unit from NodePEG and type case it into statement

        VirtualInvokeExpr vie = (VirtualInvokeExpr) s.getInvokeExpr(); // Get invoke expression from statement and type case it into virtual invoke expression
        
        return vie.getBase();

    }
    
    // Returns the variable in start unit. Eg, x.start() -> x (if x is an object of class A)
    Value getVariableOfJoinNode(NodePEG joinNode) {
    	
    	Stmt s = (Stmt) joinNode.unit; // Get unit from NodePEG and type case it into statement

        VirtualInvokeExpr vie = (VirtualInvokeExpr) s.getInvokeExpr(); // Get invoke expression from statement and type case it into virtual invoke expression
        
        return vie.getBase();
        
    }
    
    // Returns the variable in entry unit.
    Value getVariableOfEntryNode(NodePEG entryNode) { 
    	
    	Stmt s = (Stmt) entryNode.unit;    	
    	
    	return ((EnterMonitorStmt) s).getOp();
    }
    
    // Returns the variable in exit unit.
    Value getVariableOfExitNode(NodePEG exitNode) { 
    	
    	Stmt s = (Stmt) exitNode.unit;    	
    	
    	return ((ExitMonitorStmt) s).getOp();
    }
    
    Value getVariableOfNotifyNode(NodePEG notifyNode) {
    	
    	Stmt s = (Stmt) notifyNode.unit; // Get unit from NodePEG and type case it into statement

        VirtualInvokeExpr vie = (VirtualInvokeExpr) s.getInvokeExpr(); // Get invoke expression from statement and type case it into virtual invoke expression
        
        return vie.getBase();
        
    }
    
    Value getVariableOfNotifyAllNode(NodePEG notifyAllNode) {
    	
    	Stmt s = (Stmt) notifyAllNode.unit; // Get unit from NodePEG and type case it into statement

        VirtualInvokeExpr vie = (VirtualInvokeExpr) s.getInvokeExpr(); // Get invoke expression from statement and type case it into virtual invoke expression
        
        return vie.getBase();
        
    }
    
    Value getVariableOfWaitNode(NodePEG waitNode) {
    	
    	Stmt s = (Stmt) waitNode.unit; // Get unit from NodePEG and type case it into statement

        VirtualInvokeExpr vie = (VirtualInvokeExpr) s.getInvokeExpr(); // Get invoke expression from statement and type case it into virtual invoke expression
        
        return vie.getBase();
        
    }

    // Returns the main function
    SootMethod findMain(HashSet < SootMethod > methods) {

        // Return main function
        for (SootMethod method: methods)
            if (method.isMain())
                return method;

        return null; // Return null if main function is not present

    }

    // Returns a unique thread ID for each thread
    String findThreadID(SootMethod method) {

        if (method.isMain())
            return "main"; // Return "main" for main function

        return "Thread-" + threadIDCounter++; // Return a new thread ID

    }

    // Populates the graph with nodes and edges for a particular method
    ReturnPair populateGraph(SootMethod method) {

        String threadID = findThreadID(method); // get the threadID

        UnitGraph g = new BriefUnitGraph(method.getActiveBody()); // Create CFG for method

        HashMap < Unit, NodePEG > unitNodeMap = new HashMap < Unit, NodePEG > (); // Create a mapping from unit to its NodePEG // Local Scope

        // Find thisLocal // thisLocal will be null for main function, not null for other threads
        Local thisLocal = null;

        for (Local local: method.getActiveBody().getLocals())
            if (local.toString().equals("this"))
                thisLocal = local;

        // Find objects of interest for node 
        HashSet < Node > objectsOfInterest = null;
        if (!(thisLocal == null))
            objectsOfInterest = findPointsToList(thisLocal);

        for (Unit u: g) {

            NodePEG newNode = new NodePEG(objectsOfInterest, u, threadID);; // Create a new nodePEG for Unit u
            unitNodeMap.put(u, newNode); // Add the unit and NodePEG to map
            successors.put(newNode, new HashSet < SuccPair > ()); // Create an empty array list for each NodePEG
        }

        // <--------------------------------------------------------------------------- ADD NORMAL EDGES TO PEG --------------------------------------------------------------------------->

        for (Unit u: g) { // Iterate over all Units to add Normal edges in PEG

            for (Unit succ: g.getSuccsOf(u)) { // Add an edge from Node (corresponding to u) to succ Node with type as "normal"

                NodePEG node = unitNodeMap.get(u);
                NodePEG succNode = unitNodeMap.get(succ);
                String edgeType = "NORMAL";

                successors.get(node).add(new SuccPair(succNode, edgeType));

            }

        }

        // <---------------------------------------------------------------------------- ADD BEGIN NodePEG TO PEG ---------------------------------------------------------------------------->

        NodePEG beginNode = new NodePEG(objectsOfInterest, null, threadID, "BEGIN"); // Compute Begin Node        
        successors.put(beginNode, new HashSet < SuccPair > ()); // Create an empty array list for Begin NodePEG

        // Add edge from Begin Node to first Node of Method // TODO : Only the first head?
        NodePEG firstNode = unitNodeMap.get(g.getHeads().get(0));
        successors.get(beginNode).add(new SuccPair(firstNode, "NORMAL"));

        // <----------------------------------------------------------------------------- ADD END NodePEG TO PEG ----------------------------------------------------------------------------->

        NodePEG endNode = new NodePEG(objectsOfInterest, null, threadID, "END"); // Compute End Node
        successors.put(endNode, new HashSet < SuccPair > ()); // Create an empty array list for End NodePEG

        // Get last unit(s) of Method
        ArrayList < Unit > lastUnits = new ArrayList < Unit > ();
        for (Unit u: g)
            if (g.getSuccsOf(u).size() == 0)
                lastUnits.add(u);

        // Add edges from Last NodePEG(s) to END
        for (Unit u: lastUnits) {

            NodePEG lastNode = unitNodeMap.get(u);
            successors.get(lastNode).add(new SuccPair(endNode, "NORMAL"));
        }

        // <------------------------------------------------------------------------------ MODIFY WAIT NODES ----------------------------------------------------------------------------->

        // get list of all wait nodes
        ArrayList < NodePEG > waitNodes = new ArrayList < NodePEG > ();

        for (Map.Entry < NodePEG, HashSet < SuccPair > > entries: this.successors.entrySet()) {

            if (entries.getKey().threadID.equals(threadID)) { // Iterate over nodes of current thread

                NodePEG n = entries.getKey();
                Unit u = n.unit;
                Stmt s = (Stmt) u;

                if (s instanceof InvokeStmt) {
                    InvokeExpr ie = s.getInvokeExpr();

                    if (ie instanceof VirtualInvokeExpr) {
                        VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;

                        if (vie.getMethod().toString().equals("<java.lang.Object: void wait()>")) { // Wait Nodes

                            waitNodes.add(n); // Add wait node to the list
                            n.specialProperty = "WAIT"; // Modify Original NodePEG to  Wait NodePEG

                        }
                    }
                }
            }
        }

        for (NodePEG n: waitNodes) { // Iterate over all wait nodes

            NodePEG waitingNode = new NodePEG(n.object, null, n.threadID, "WAITING"); // Create Waiting NodePEG
            NodePEG notifiedEntryNode = new NodePEG(n.object, null, n.threadID, "NOTIFIEDENTRY"); // Create Notified Entry NodePEG

            // Store the list of successors for wait node
            ArrayList < SuccPair > succsOfWait = new ArrayList < SuccPair > ();
            succsOfWait.addAll(successors.get(n));

            successors.get(n).clear(); // Empty the list of successors for wait node
            successors.get(n).add(new SuccPair(waitingNode, "NORMAL")); // Add edge from WAIT to WAITING

            // Add edge from WAITING to NOTIFIED ENTRY
            successors.put(waitingNode, new HashSet < SuccPair > ());
            successors.get(waitingNode).add(new SuccPair(notifiedEntryNode, "WAITING => NOTIFIEDENTRY"));

            // Add edge from NOTIFIED ENTRY to stored successors of wait node
            successors.put(notifiedEntryNode, new HashSet < SuccPair > ());
            successors.get(notifiedEntryNode).addAll(succsOfWait);
        }
        
        // <------------------------------------------------------------------------ UPDATE OBJECTS OF INTEREST ------------------------------------------------------------------------->
        
        for(Unit u : g) {
        	if (((Stmt) u) instanceof InvokeStmt)
                if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr) 	
                	System.out.println("Unit : " + u + " Method : " + ((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString());
                
        }
        
        for(Unit u : g) {
        		
        	// Start Unit
        	if(isStartUnit(u)) {
        		Value variable = getVariableOfStartNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for start node to "this" of thread it is starting 
        	}
        	
        	// Join Unit
        	else if(isJoinUnit(u)) {
        		Value variable = getVariableOfJoinNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for start node to "this" of thread it is joining
        	}
        	
        	// Entry Unit
        	else if(isEntryUnit(u)) {
        		Value variable = getVariableOfEntryNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for entry node to object on which lock is taken
        	}
        	
        	// Exit Unit
        	else if(isExitUnit(u)) {
        		Value variable = getVariableOfExitNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for exit node to object on which lock is taken
        	}
        	
        	// Notify Unit
        	else if(isNotifyUnit(u)) {
        		Value variable = getVariableOfNotifyNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for notify node to object on which lock is taken
        	}
        	
        	// NotifyAll Unit
        	else if(isNotifyAllUnit(u)) {
        		Value variable = getVariableOfNotifyAllNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for notifyAll node to object on which lock is taken
        	}
        	
        	// Wait Unit
        	else if(isWaitUnit(u)) {
        		Value variable = getVariableOfWaitNode(unitNodeMap.get(u));
        		unitNodeMap.get(u).object = findPointsToList((Local) variable); // Update objects of interest for notifyAll node to object on which lock is taken
        	}
        }

        // <------------------------------------------------------------------------------ GET START NODES ------------------------------------------------------------------------------>

        return new ReturnPair(beginNode, findStartNodes(g, unitNodeMap)); // Return begin node and list of start nodes of newly created thread
    }

    // Returns list of start nodes in graph g
    ArrayList < NodePEG > findStartNodes(UnitGraph g, HashMap < Unit, NodePEG > unitNodeMap) {

        ArrayList < NodePEG > listOfStartNodes = new ArrayList < NodePEG > (); // get List of nodes with start function

        for (Unit u: g)
            if (this.isStartUnit(u))
                listOfStartNodes.add(unitNodeMap.get(u)); // Add NodePEG to list if it is the start() function

        return listOfStartNodes;

    }

    // Returns true if unit is a start unit
    Boolean isStartUnit(Unit u) {

        if (((Stmt) u) instanceof InvokeStmt)
            if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr)
                if (((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString().equals("<java.lang.Thread: void start()>"))
                    return true;

        return false;
    }
    
    // Returns true is join unit
    Boolean isJoinUnit(Unit u) {
    	
    	if (((Stmt) u) instanceof InvokeStmt)
            if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr)
                if (((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString().equals("<java.lang.Thread: void join()>"))
                    return true;

        return false;
        
    }
    
    // Returns true if enterMonitor statement
    Boolean isEntryUnit(Unit u) {
    	
    	if(((Stmt) u) instanceof EnterMonitorStmt) 
    		return true;
    	
    	return false;
    	
    }
    
    // Returns true if exitMonitor statement
    Boolean isExitUnit(Unit u) {
    	
    	if(((Stmt) u) instanceof ExitMonitorStmt) 
    		return true;
    	
    	return false;
    	
    }

    Boolean isNotifyUnit(Unit u) {
    	
    	if (((Stmt) u) instanceof InvokeStmt)
            if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr)
                if (((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString().equals("<java.lang.Object: void notify()>"))
                    return true;

        return false;
        
    }
    
    Boolean isNotifyAllUnit(Unit u) {
    	
    	if (((Stmt) u) instanceof InvokeStmt)
            if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr)
                if (((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString().equals("<java.lang.Object: void notifyAll()>"))
                    return true;

        return false;
        
    }
    
    Boolean isWaitUnit(Unit u) {
    	
    	if (((Stmt) u) instanceof InvokeStmt)
            if (((Stmt) u).getInvokeExpr() instanceof VirtualInvokeExpr)
                if (((VirtualInvokeExpr)((Stmt) u).getInvokeExpr()).getMethod().toString().equals("<java.lang.Object: void wait()>"))
                    return true;

        return false;
        
    }
    
    
    // Returns the list of objects a local variable points to
    HashSet < Node > findPointsToList(Local l) {

        // Initialize an empty array list
        HashSet < Node > listOfObjs = new HashSet < Node > ();

        // get the reaching objects and type cast to PointsToSetInternal
        PointsToSet pts = this.pta.reachingObjects(l);
        PointsToSetInternal pti = (PointsToSetInternal) pts;

        // We need to use P2SetVisitor for traversing all nodes in Points To Set
        P2SetVisitor vis = new P2SetVisitor() {

            @Override
            public void visit(Node n) {
                listOfObjs.add(n);
            }

        };
        pti.forall(vis);

        // Return list of points to objects
        return listOfObjs;
    }

    // Utility Function for Printing the PEG Nodes and their outgoing edges for a particular method
    void printPEGPerMethod(String threadID) {

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ START ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

        for (Map.Entry < NodePEG, HashSet < SuccPair >> entry: successors.entrySet()) {

            if (!entry.getKey().threadID.equals(threadID)) // Skip nodes of other threads
                continue;

            NodePEG node = entry.getKey();
            HashSet < SuccPair > adjNodes = entry.getValue();

            for (SuccPair pair: adjNodes) {

                NodePEG adjNode = pair.succNode;
                String edgeType = pair.edgeType;

                System.out.println(node.printNode() + "   --- " + edgeType + " --->   " + adjNode.printNode() + "\n");
            }
        }

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ END ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

    }

    // Print Entire PEG
    void printPEG() {

        printPEGPerMethod("main"); // Print Main function PEG

        for (int i = 0; i < threadIDCounter; i++)
            printPEGPerMethod("Thread-" + i); // Print All Threads PEG

    }

    // Initialize WorkList for PEG information without notify edges
    HashSet < NodePEG > initializeWorkList() {

        HashSet < NodePEG > workList = new HashSet < NodePEG > (); // Empty WorkList

        // <--------------------------------------------------------------------------- Find reachable starts from begin of main --------------------------------------------------------------------------->

        NodePEG beginMain = getBeginOfMain(); // Get the begin node of main function

        ArrayList < NodePEG > queue = new ArrayList < NodePEG > (); // Queue for BFS // TODO -> Implemented Visited
        queue.add(beginMain); // Initialize Queue with begin node of main

        while (!queue.isEmpty()) {

            NodePEG topNode = queue.get(0); // Store and pop a node from queue
            queue.remove(0);

            for (SuccPair p: successors.get(topNode)) {
                if (p.succNode.threadID.equals("main")) {

                    queue.add(p.succNode); // Add to queue if successors belong to main thread

                    if (isStartUnit(p.succNode.unit))
                        workList.add(p.succNode); // Add to workList if start node

                }
            }
        }

        return workList;
    }

    // Returns the begin node of main function
    NodePEG getBeginOfMain() {

        for (NodePEG n: successors.keySet())
            if (n.threadID.equals("main") & n.specialProperty.equals("BEGIN"))
                return n;

        return null;

    }

    // The Work List Iterate Algorithm
    void workListIterate() {

        HashSet < NodePEG > workList = initializeWorkList(); // Initialize WorkList 
        System.out.println(workList);
    }

    // Returns the waiting predecessor of a node
    NodePEG waitingPred(NodePEG m) {

        for (PredPair p: predecessors.get(m))
            if (p.predNode.specialProperty.equals("WAITING"))
                return p.predNode;

        return null;

    }

    // Returns nodes of a thread
    HashSet < NodePEG > N(String threadID) {

        HashSet < NodePEG > threadNodes = new HashSet < NodePEG > ();

        for (NodePEG n: successors.keySet())
            if (n.threadID.equals(threadID))
                threadNodes.add(n); // Add node n to list is thread id matches

        return threadNodes;
    }

    // Returns the threadID of a node
    String getThread(NodePEG n) {

        return n.threadID;

    }

    // Returns the list of start predecessors of a node
    HashSet < NodePEG > startPred(NodePEG n) {

        HashSet < NodePEG > startPreds = new HashSet < NodePEG > ();

        for (PredPair p: predecessors.get(n))
            if (isStartUnit(p.predNode.unit))
                startPreds.add(p.predNode); // Add to list if start pred

        return startPreds;
    }

    // Returns the list of notify predecessors of a node
    HashSet < NodePEG > notifyPred(NodePEG n) {

        HashSet < NodePEG > notifyPreds = new HashSet < NodePEG > ();

        for (PredPair p: predecessors.get(n))
            if (p.predNode.specialProperty.equals("NOTIFY") || p.predNode.specialProperty.equals("NOTIFYALL"))
                notifyPreds.add(p.predNode); // Add to list if notify pred

        return notifyPreds;
    }

    // Returns the list of local predecessors of a node
    HashSet < NodePEG > localPred(NodePEG n) {

        HashSet < NodePEG > localPreds = new HashSet < NodePEG > ();

        for (PredPair p: predecessors.get(n))
            if (p.edgeType.equals("NORMAL"))
                localPreds.add(p.predNode); // Add to list if local pred

        return localPreds;
    }

    // Returns list of nodes than MHP with node n due to a notify all
    HashSet < NodePEG > genNotifyAll(NodePEG n) {

        HashSet < NodePEG > parallelNodes = new HashSet < NodePEG > ();

        if (!(n.specialProperty.equals("NOTIFIEDENTRY")))
            return parallelNodes; // Empty Set for Non notifiedEntry Nodes

        for (NodePEG m: successors.keySet()) {

            if (m.specialProperty.equals("NOTIFIEDENTRY") && hasIntersection(n.object, m.object)) { // m is a notified entry and both m and n have a common object of interest

                if (M.get(waitingPred(m)).contains(waitingPred(n))) { // waiting pred of m and n run in parallel

                    for (NodePEG r: successors.keySet()) {

                        if (r.specialProperty.equals("NOTIFYALL")) { // there exists a notify all node r

                            HashSet < NodePEG > intersectionOfMs = getIntersectionPEG(M.get(waitingPred(m)), M.get(waitingPred(n))); // r runs in parallel with waiting pred of m and n

                            if (intersectionOfMs.contains(r))
                                parallelNodes.add(m);

                        }
                    }

                }
            }
        }

        return parallelNodes;

    }

    // Returns the list of notify successors of a node
    HashSet < NodePEG > notifySucc(NodePEG n) {

        HashSet < NodePEG > notifySuccs = new HashSet < NodePEG > (); // Empty List

        for (NodePEG w: M.get(n)) { // Iterate over M(n)

            if (w.specialProperty.equals("WAITING")) { // Search for Waiting Nodes

                // Find m node
                NodePEG m = null;
                for (SuccPair successor: successors.get(w))
                    m = successor.succNode; // w is a waiting node, then m is the successor of w

                if (successors.get(w).size() > 1)
                    System.out.println("LOGICAL ERROR : Waiting Node has more than one predecessors" + w.printNode());

                if (m.specialProperty.equals("NOTIFIEDENTRY")) { // m should be a notified entry node

                    if (n.object == null)
                        System.out.println("LOGICAL ERROR : notifySucc(n) n.object is null");

                    if (this.hasIntersection(n.object, m.object))
                        notifySuccs.add(m); // If n and m have a common object of interest, add m as notify successor of n

                }
            }

        }

        return notifySuccs;
    }

    // Returns the list of begin successors of a start node
    HashSet < NodePEG > beginSucc(NodePEG n) {

        HashSet < NodePEG > beginSuccs = new HashSet < NodePEG > (); // Empty List

        for (SuccPair succ: successors.get(n))

            if (succ.succNode.specialProperty.equals("BEGIN"))
                beginSuccs.add(succ.succNode);

        return beginSuccs;

    }

    // Check if two sets have intersection
    Boolean hasIntersection(HashSet < Node > setA, HashSet < Node > setB) {

        for (Node node: setA)
            if (setB.contains(node))
                return true;

        return false;

    }

    // Compute intersection of two sets
    HashSet < Node > getIntersection(HashSet < Node > setA, HashSet < Node > setB) {

        HashSet < Node > intersection = new HashSet < Node > ();

        for (Node node: setA)
            if (setB.contains(node))
                intersection.add(node); // add node to intersection set if present in both sets

        return intersection;

    }

    // Compute intersection of two sets
    HashSet < NodePEG > getIntersectionPEG(HashSet < NodePEG > setA, HashSet < NodePEG > setB) {

        HashSet < NodePEG > intersection = new HashSet < NodePEG > ();

        for (NodePEG node: setA)
            if (setB.contains(node))
                intersection.add(node); // add node to intersection set if present in both sets

        return intersection;

    }

    // Compute Union of two sets
    HashSet < NodePEG > getUnion(HashSet < NodePEG > setA, HashSet < NodePEG > setB) {

        HashSet < NodePEG > union = new HashSet < NodePEG > ();

        union.addAll(setA);
        union.addAll(setB);

        return union;
    }

    // Compute difference of two sets (setA - setB)
    HashSet < NodePEG > getDifference(HashSet < NodePEG > setA, HashSet < NodePEG > setB) {

        HashSet < NodePEG > difference = new HashSet < NodePEG > ();

        for (NodePEG node: setA)
            if (!setB.contains(node))
                difference.add(node);

        return difference;
    }

    // Computes the OUT of node
    HashSet < NodePEG > getOut(NodePEG n) {

        return getDifference(getUnion(M.get(n), gen(n)), kill(n));

    }

    // Computes the GEN of node // TODO
    HashSet < NodePEG > gen(NodePEG n) {

        HashSet < NodePEG > genSet = new HashSet < NodePEG > ();

        if (isStartUnit(n.unit))
            return beginSucc(n); // n is start node -> add begin successors of n

        // TODO notify nodes

        // TODO kill(n dash)

        return genSet;
    }

    // Computes of KILL of node // TODO
    HashSet < NodePEG > kill(NodePEG n) {

        HashSet < NodePEG > killSet = new HashSet < NodePEG > ();

        return killSet;
    }
}