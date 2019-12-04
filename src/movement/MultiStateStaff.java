package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MultiStateStaff extends MovementModel {

    private Coord lastWaypoint;
    private State state;
    private int pauseTime;
    private WaypointTable waypointTable;

    private State getState(){
        return this.state;
    }
    
    private void cutTime() {
        if (this.pauseTime <= 0)
            this.pauseTime = getInterval();
        else 
            this.pauseTime--;            
    }

    private int getInterval() {
        switch (state.getNumVal()) {
            case 0: return 100; //10 minutes
            case 1: return 500; //5 minutes
            case 2: return 200;
            case 3: return 900; //90 minutes UNUSED
            default: return 0;
        }
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        
        //Variable pause time implementation
        final double curTime = SimClock.getTime();
 
        // Update state machine every time we pick a path

        if (this.state == State.OFFICE) { 
            if (curTime < 375 || curTime > 2625 && curTime < 3750 || curTime > 5625) { // If office time over
                this.state = waypointTable.getNextState(this.state);
                this.pauseTime = getInterval();
                //System.out.println("going to: " + this.state);

                // Create the path
                p.addWaypoint(lastWaypoint.clone());
                final Coord c = waypointTable.getCoordFromState(this.state);
                p.addWaypoint(c);
                this.lastWaypoint = c;
            } else
                p.addWaypoint(lastWaypoint.clone());

        } else if (this.pauseTime == 0) { // If time's up 
            this.state = waypointTable.getNextState(this.state);
            this.pauseTime = getInterval();
            //System.out.println("going to: " + this.state);
        
            // Create the path
            p.addWaypoint(lastWaypoint.clone());
            final Coord c = waypointTable.getCoordFromState(this.state);
            p.addWaypoint(c);
            this.lastWaypoint = c;
       
        } else {
            this.cutTime();
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
        return new MultiStateStaff(this);
    }

    // Constructors
    public MultiStateStaff( final Settings settings ) {
        super( settings );
        this.state = State.ENTRANCE;
    }

    public MultiStateStaff( final MultiStateStaff other ) {
        super( other );

        this.waypointTable = new WaypointTable();
        this.state = other.getState();
    }

    public MultiStateStaff() {
        this.waypointTable = new WaypointTable();
        this.state = State.ENTRANCE;
        this.pauseTime = 0;
    }

    private enum State {
        CAFE(0), TOILET(1), LEISURE(2), OFFICE(3), ENTRANCE(4);
        private int numVal;
        private int time  = 0;

        State(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    class WaypointTable {

        // TODO further improve the efficiency
        // CAFE(0), TOILET(1), LEISURE(2), OFFICE(3), ENTRANCE(4);
        private int morning[][] = { //follows the table in note
            { 0, 10, 5, 80, 5 },
            { 5, 1, 5, 80, 3 },
            { 5, 1, 10, 80, 5 },
            { 1, 1, 1, 95, 1 },
            { 3, 1, 2, 5, 82 }};
        private int afternoon[][] = {//follows the table in note
            { 0, 10, 5, 1, 80 },
            { 5, 1, 5, 3, 80 },
            { 5, 1, 10, 5, 80 },
            { 1, 1, 1, 1, 95 },
            { 1, 0, 1, 1, 90 }};


        public WaypointTable(){ }

        private int[] getProb(int state) {
            // System.out.println(SimClock.getTime());
            int time = SimClock.getIntTime();

            //System.out.println(time + " < " + SimScenario.getInstance().getEndTime() / 2);
            if (time < 5650)
                return morning[state];
            else {
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
                
        }

        public Coord getOfficeCoord(){
            // TODO modify coordinate
            int office[][] = {
                    {150, 150},
                    {150, 150},
                    {150, 150},
                    {150, 150}
            };
            int randC = ThreadLocalRandom.current().nextInt(0, 3 + 1);
            return new Coord(ThreadLocalRandom.current().nextInt(office[randC][0], office[randC][0] + 20 + 1),ThreadLocalRandom.current().nextInt(office[randC][1], office[randC][1] + 20 + 1));
        }

        public Coord getCoordFromState(State state) {
            switch (state) {
                case CAFE: return new Coord(ThreadLocalRandom.current().nextInt(590, 610 + 1),ThreadLocalRandom.current().nextInt(390, 410 + 1));
                case TOILET: return new Coord(ThreadLocalRandom.current().nextInt(740, 760 + 1),ThreadLocalRandom.current().nextInt(290, 310 + 1));
                case LEISURE: return new Coord(ThreadLocalRandom.current().nextInt(500, 700 + 1),ThreadLocalRandom.current().nextInt(300, 330 + 1));
                case OFFICE: return getOfficeCoord();
                //case LIBRARY: return new Coord(ThreadLocalRandom.current().nextInt(290, 350 + 1),ThreadLocalRandom.current().nextInt(290, 310 + 1));
                default: return new Coord(ThreadLocalRandom.current().nextInt(800, 825 + 1),ThreadLocalRandom.current().nextInt(315, 335 + 1));
            }
        }
    }
}
