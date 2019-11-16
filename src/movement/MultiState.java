package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import java.util.ArrayList;
import java.util.Random;

public class MultiState extends MovementModel {

    private Coord lastWaypoint;
    private State state;
    private WaypointTable waypointTable;

    public State getState(){
        return this.state;
    }

    @Override
    public Path getPath() {
        // Update state machine every time we pick a path
        this.state = waypointTable.getNextState(this.state);
        System.out.println("going to: " + this.state);
        // Create the path
        Path p;
        p = new Path( generateSpeed() );
        p.addWaypoint( lastWaypoint.clone() );
        final Coord c = waypointTable.getCoordFromState(this.state);
        p.addWaypoint( c );

        this.lastWaypoint = c;
        return p;
    }

    public MultiState( final Settings settings ) {
        super( settings );
        this.state = State.CAFE;
    }

    public MultiState( final MultiState other ) {
        super( other );

        // Pick a random state every time we replicate rather than copying!
        // Otherwise every node would start in the same state.

        waypointTable = new WaypointTable();
        this.state = other.getState();
    }

    public MultiState(){
        this.state = State.CAFE;
        waypointTable = new WaypointTable();
    }
    @Override
    public Coord getInitialLocation() {
        this.lastWaypoint = this.waypointTable.getCoordFromState(State.ENTRANCE);
        return this.lastWaypoint;
    }

    @Override
    public MovementModel replicate() {
        return new MultiState(this);
    }

    private enum State {
        CAFE(0), TOILET(1), LEISURE(2), CLASSROOM(3), LIBRARY(4), ENTRANCE(5);
        private int numVal;

        State(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    class WaypointTable {


        public WaypointTable(){ }

        public int[][] getProb(){
           // System.out.println(SimClock.getTime());
            int time = SimClock.getIntTime();

            System.out.println(time + " < " + SimScenario.getInstance().getEndTime() / 2);
            if (time < ( SimScenario.getInstance().getEndTime() / 2))
                return new int[][]{ //follows the table in note
                        { 0, 10, 10, 70, 10, 5 },
                        { 10, 1, 9, 70, 10, 5 },
                        { 10, 5, 70, 10, 5, 5 },
                        { 1, 1, 1, 95, 2,  5 },
                        { 4, 1, 4, 7, 84, 5 },
                        { 10, 3, 10, 42, 3, 32 } };
            else {
                System.out.println("using afternoon time");
                return new int[][]{ //follows the table in note
                        { 0, 5, 5, 39, 5, 57},
                        { 6, 1, 6, 46, 10,  66},
                        { 6, 3, 46, 6, 3, 66 },
                        { 1, 1, 1, 72, 2,  76 },
                        { 1, 1, 1, 1, 75, 9 },
                        { 14, 4, 19, 39, 19, 95 } };
            }
        }

        public State getNextState(State currentState){
            ArrayList<State> temp = new ArrayList<State>();
//            for(int probability: this.probs[currentState.getNumVal()]){
//                for (int i = 0;i < probability; i++){
//                    temp.add(State.values()[probability]);
//                }
//            }
            int maxLength = currentState.equals(State.ENTRANCE)? State.values().length: State.values().length - 1;
            for(int i = 0; i < maxLength; i++){
                for (int j = 0; j < this.getProb()[currentState.getNumVal()][i]; j++){
                    temp.add(State.values()[i]);
                }
            }
            return temp.get(new Random().nextInt(temp.size()));
        }

        public Coord getCoordFromState(State state){
            switch (state){
                case CAFE: return new Coord(100,100);
                case TOILET: return new Coord(200,200);
                case LEISURE: return new Coord(300,200);
                case CLASSROOM: return new Coord(300,300);
                case LIBRARY: return new Coord(100,300);
                default: return new Coord(0,0);
            }
        }
    }
}
