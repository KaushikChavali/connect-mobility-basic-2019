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

    //Variable pause time implementation
    private int getInterval() {
        switch (state.getNumVal()) {
            case 0: return 2; //10 minutes
            case 1: return 1; //5 minutes
            case 2: return 6;
            case 3: return 18; //90 minutes UNUSED
            default: return 0;
        }
    }

    @Override
    public Path getPath() {
        Path p = new Path(100*generateSpeed());

        if (this.state == State.OFFICE) { 
            final double curTime = SimClock.getTime();
            // If office time is over -0830 or 1145-1245 or 1645-
            if (curTime < 6 || curTime >= 45 && curTime < 57 || curTime >= 105) {
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

        // todo further improve the efficiency
        // CAFE(0), TOILET(1), LEISURE(2), OFFICE(3), ENTRANCE(4);
        private int morning[][] = { //follows the table in note
            { 0, 10, 5, 80, 5 },
            { 5, 1, 5, 80, 3 },
            { 5, 1, 10, 80, 5 },
            { 1, 1, 1, 95, 1 },
            { 3, 1, 2, 5, 80 }};
        private int afternoon[][] = {//follows the table in note
            { 0, 10, 5, 1, 80 },
            { 5, 1, 5, 3, 80 },
            { 5, 1, 10, 5, 80 },
            { 1, 1, 1, 1, 95 },
            { 1, 0, 1, 1, 90 }};


        public WaypointTable(){ }

        private int[] getProb(int state) {
            int time = SimClock.getIntTime();

            //if (time < 5650)
            if (time < 60) // before 13:00
                return morning[state];
            else {
                return afternoon[state];
            }
        }

        public State getNextState(State currentState) {
            // todo further improve the efficiency z.B. precalculate total
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
                    {780, 470},
                    {780, 470},
                    {780, 470},
                    {780, 470}
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
                default: return new Coord(ThreadLocalRandom.current().nextInt(800, 825 + 1),ThreadLocalRandom.current().nextInt(315, 335 + 1));
            }
        }
    }
}
