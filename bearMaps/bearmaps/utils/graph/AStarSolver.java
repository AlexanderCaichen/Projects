package bearmaps.utils.graph;

import bearmaps.utils.pq.MinHeapPQ;
import bearmaps.utils.pq.NaiveMinPQ;
import edu.princeton.cs.algs4.Stopwatch;

import java.util.*;

public class AStarSolver<Vertex> implements ShortestPathsSolver<Vertex> {

    private SolverOutcome outcome;
    private List<Vertex> solution; //start to end
    private double sweight;
    private int numStates;
    private double time;


    //Constructor which finds the solution, computing everything necessary for all other methods to
    // return their results IN CONSTANT TIME. Note that timeout passed in is in seconds.
    //NOTE: A*Graph is basically a, well, graph (neighbors, is undirected/directed, etc)

    //A* result

    //Edge relaxation:
    public AStarSolver(AStarGraph<Vertex> input, Vertex start, Vertex end, double timeout) {
        Stopwatch sw = new Stopwatch();
        numStates = 0;

        if (start.equals(end)) { //assuming timeout != 0
            outcome = SolverOutcome.SOLVED;
            nope();
            time = 0;
            return;
        }
        //key: Vertex          Value: [Vertex before, distance from start]
        HashMap<Vertex, Object[]> noted = new HashMap<>(); //basically same as visited though???
        Set<Vertex> visited = new HashSet<>();

        //!!!!!!!!!!!! replace with MinHeapPQ later !!!!!!!!!!!!!!! (before NaiveHeapPQ or something)
        MinHeapPQ<Vertex> heap = new MinHeapPQ<>();

        //Add vertexes connected to original (start)
        visited.add(start);
        for (WeightedEdge i: input.neighbors(start)) {
            Vertex to = (Vertex) i.to();
            if (visited.contains(to)) { //if for some reason start points to itself.
                continue;
            }
//            if (to.equals(end)) { //problem: straightest path is not shortest path
//                outcome = SolverOutcome.SOLVED;
//                solution = path(noted, start, end);
//                sweight = (double) noted.get(now)[1];
//                time = sw.elapsedTime();
//                return;
//            }
            double hol = holistic(input, to, end);
            heap.insert(to, hol);
            noted.put(to, new Object[]{start, i.weight()});
        }

        while (true) {
            if (sw.elapsedTime() > timeout) {
                outcome = SolverOutcome.TIMEOUT;
                nope();
                time = sw.elapsedTime();
                return;
            }
            if (noted.isEmpty()) {
                outcome = SolverOutcome.UNSOLVABLE;
                nope();
                time = sw.elapsedTime();
                return;
            }
            Vertex now = heap.poll();
            numStates++;
            if (now.equals(end)) { //in case it's next for some reason.
                // don't need to add another thing in "noted". Should already be added in line 98 or 58
                solution = path(noted, start, end);
                sweight = (double) noted.get(now)[1];
                outcome = SolverOutcome.SOLVED;
                time = sw.elapsedTime();
                return;
            }
            //p = e.from(), q = e.to(), w = e.weight()
            //if distTo[p] + w < distTo[q]: ???
                //distTo[q] = distTo[p] + w
                //if q is in the PQ: PQ.changePriority(q, distTo[q] + h(q, goal))
                //if q is not in PQ: PQ.insert(q, distTo[q] + h(q, goal))
            for (WeightedEdge i: input.neighbors(now)) {
                Vertex to = (Vertex) i.to();
                if (visited.contains(to)) { //"to" has already been visited via shorter path
                    continue;
                }
                double distSoFar = (double) noted.get(now)[1] + i.weight(); //distance to "now" + distance from now to "to"
                if (!(heap.contains(to))) {
                    heap.insert(to, distSoFar + holistic(input, to, end));
                    noted.put(to, new Object[]{now, distSoFar});
                }
                else if (heap.contains(to) && (double) noted.get(to)[1] > distSoFar) { //distSoFar + holistic(input, to, end)???
                    heap.changePriority(to, distSoFar + holistic(input, to, end));
                    noted.replace(to, new Object[]{now, distSoFar});
                }
                //else go to next vertex
            }
            visited.add(now);
        }
    }

    //Larger total distances get higher value (lower) priority
    private Double holistic(AStarGraph<Vertex> graph, Vertex too, Vertex end) { //distance + length thing
        return graph.estimatedDistanceToGoal(too, end); //need to add to distance so far, not just edge.
    }

    private void nope() {
        solution = new ArrayList<>();
        sweight = 0;
    }

    private List<Vertex> path(HashMap<Vertex, Object[]> noted, Vertex start, Vertex end) {
        List<Vertex> result = new ArrayList<>();
        result.add(end);
        Vertex now = end;
        while (!(now.equals(start))) {
            Vertex prev_Vertex = (Vertex) noted.get(now)[0];//vertex before
            result.add(prev_Vertex);
            now = prev_Vertex;
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Returns one of SolverOutcome.SOLVED, SolverOutcome.TIMEOUT, or SolverOutcome.UNSOLVABLE. The result should be:
     * SOLVED if the AStarSolver was able to complete all work in the time given.
     * UNSOLVABLE if the priority queue became empty before finding the solution.
     * TIMEOUT if the solver ran out of time. You should check to see if you have run out of time every time you dequeue.
     */
    public SolverOutcome outcome() {return outcome;}

    //A list of vertices corresponding to a solution. Should be empty if result was TIMEOUT or UNSOLVABLE.
    public List<Vertex> solution() {return solution;}

    //The total weight of the given solution, taking into account edge weights.
    // Should be 0 if result was TIMEOUT or UNSOLVABLE
    public double solutionWeight() {return sweight;}

    //The total number of priority queue poll() operations.
    // Should be the number of states explored so far if result was TIMEOUT or UNSOLVABLE.
    public int numStatesExplored() {return numStates;}

    //The total time spent in seconds by the constructor.
    public double explorationTime() {return time;}

    public static void main(String[] args) {

    }
}
