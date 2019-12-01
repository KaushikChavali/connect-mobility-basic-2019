package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MultiState extends MovementModel {

    private Coord lastWaypoint;
    private State state;
    private WaypointTable waypointTable;

    public State getState(){
        return this.state;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        // Update state machine every time we pick a path
        if (this.state.getTime() == 0) {
            this.state = waypointTable.getNextState(this.state);
            System.out.println("going to: " + this.state);
            // Create the path
            p.addWaypoint(lastWaypoint.clone());
            final Coord c = waypointTable.getCoordFromState(this.state);
            p.addWaypoint(c);
            this.lastWaypoint = c;
        } else {
            this.state.cutTime();

            p.addWaypoint(lastWaypoint.clone());

        }
        return p;
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

    // Constructors
    public MultiState( final Settings settings ) {
        super( settings );
        this.state = State.ENTRANCE;
    }

    public MultiState( final MultiState other ) {
        super( other );

        this.waypointTable = new WaypointTable();
        this.state = other.getState();
    }

    public MultiState(){
        this.waypointTable = new WaypointTable();
        this.state = State.ENTRANCE;
    }

    private enum State {
        CAFE(0), TOILET(1), LEISURE(2), CLASSROOM(3), LIBRARY(4), ENTRANCE(5);
        private int numVal;
        private int time  = getInterval();

        State(int numVal) {
            this.numVal = numVal;
        }

        public int getTime(){
            return this.time;
        }
        public void cutTime(){
            if (this.time < 0){
                this.time = getInterval();
            } else {
                this.time = this.time - 1;
            }
        }

        public int getInterval() {
            switch (numVal){
                case 0: return 10000; //10 minutes
                case 1: return 10000; //10 minutes
                case 2: return 20000;
                case 3: return 90000; //90 minutes
                case 4: return 60000; //60 minutes
                default: return 10000;
            }
        }

        public int getNumVal() {
            return numVal;
        }
    }

    class WaypointTable {

        // TODO further improve the efficiency
        private int morning[][] = { //follows the table in note
            { 0, 10, 10, 70, 10, 5 },
            { 10, 1, 9, 70, 10, 5 },
            { 10, 5, 70, 10, 5, 5 },
            { 1, 1, 1, 95, 2,  5 },
            { 4, 1, 4, 7, 84, 5 },
            { 10, 3, 10, 42, 3, 32 } };
        private int afternoon[][] = {//follows the table in note
            { 0, 5, 5, 39, 5, 57},
            { 6, 1, 6, 46, 10,  66},
            { 6, 3, 46, 6, 3, 66 },
            { 1, 1, 1, 72, 2,  76 },
            { 1, 1, 1, 1, 75, 9 },
            { 14, 4, 19, 39, 19, 95 } };


        public WaypointTable(){ }

        private int[] getProb(int state) {
            // System.out.println(SimClock.getTime());
            int time = SimClock.getIntTime();

            System.out.println(time + " < " + SimScenario.getInstance().getEndTime() / 2);
            if (time < ( SimScenario.getInstance().getEndTime() / 2))
                return morning[state];
            else {
                System.out.println("using afternoon time");
                return afternoon[state];
            }
        }

        public State getNextState(State currentState) {
            // TODO further improve the efficiency z.B. precalculate total
            int total = 0;
            int probs[] = getProb(currentState.getNumVal());
            
            for (int i = 0; i < State.values().length; i++)
                total += probs[i];

            int randomNumber = new Random().nextInt(total);
            for (int i = 0; i < State.values().length; i++) {
                randomNumber -= probs[i];
                if (randomNumber < 0)
                    return State.values()[i];
            }
            
            return State.values()[State.values().length-1];
                

            /*ArrayList<State> temp = new ArrayList<State>();
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
            return temp.get(new Random().nextInt(temp.size()));*/
        }

        public Coord getClassroomCoord(){
            int classroom[][] = {
                    {890, 390},
                    {220, 220},
                    {410, 380},
                    {780, 470}
            };
            int randC = ThreadLocalRandom.current().nextInt(0, 3 + 1);
            return new Coord(ThreadLocalRandom.current().nextInt(classroom[randC][0], classroom[randC][0] + 20 + 1),ThreadLocalRandom.current().nextInt(classroom[randC][1], classroom[randC][1] + 20 + 1));
        }

        public Coord getCoordFromState(State state) {
            switch (state) {
                case CAFE: return new Coord(ThreadLocalRandom.current().nextInt(590, 610 + 1),ThreadLocalRandom.current().nextInt(390, 410 + 1));
                case TOILET: return new Coord(ThreadLocalRandom.current().nextInt(740, 760 + 1),ThreadLocalRandom.current().nextInt(290, 310 + 1));
                case LEISURE: return new Coord(ThreadLocalRandom.current().nextInt(500, 700 + 1),ThreadLocalRandom.current().nextInt(300, 330 + 1));
                case CLASSROOM: return getClassroomCoord();
                case LIBRARY: return new Coord(ThreadLocalRandom.current().nextInt(290, 350 + 1),ThreadLocalRandom.current().nextInt(290, 310 + 1));
                default: return new Coord(ThreadLocalRandom.current().nextInt(800, 825 + 1),ThreadLocalRandom.current().nextInt(315, 335 + 1));
            }
        }
    }
}
