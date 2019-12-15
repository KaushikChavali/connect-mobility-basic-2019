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
    private int pauseTime;
    private WaypointTable waypointTable;

    public State getState(){
        return this.state;
    }
    private void cutTime() {
        if (this.pauseTime <= 0)
            this.pauseTime = getInterval();
        else 
            this.pauseTime--;            
    }

    //5 minutes equals 1 time unit in the simulation
    private int getInterval() {
        switch (state.getNumVal()) {
            case 0: return 2; //CAFE 10 minutes
            case 1: return 2; //TOILET 10 minutes
            case 2: return 4; //LEISURE 20 minutes
            case 3: return 18; //CLASSROOM 90 minutes UNUSED
            case 4: return 6; //LIBRARY 30 minutes
            default: return 0;
        }
    }

    @Override
    public Path getPath() {
        Path p = new Path(100*generateSpeed());
        
        // Simulation time 08:00-18:00
        // Each time unit = 5 minutes

        if (this.state == State.CLASSROOM) { 
            final double curTime = SimClock.getTime();
            // If lecture is over -0815 or 0945-1015 or 1145-1215 or 1345-1415 or 1545-1615 or 1745-
            if (curTime < 3 || curTime >= 21 && curTime < 27 || curTime >= 45 && curTime < 51
                    || curTime >= 69 && curTime < 75 || curTime >= 93 && curTime < 99 || curTime >= 117) {
                this.state = waypointTable.getNextState(this.state);
                this.pauseTime = getInterval();
                // System.out.println("Lecture is over, going to: " + this.state);

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
            // System.out.println("Time's up, going to: " + this.state);
        
            // Create the path
            p.addWaypoint(lastWaypoint.clone());
            final Coord c = waypointTable.getCoordFromState(this.state);
            p.addWaypoint(c);
            this.lastWaypoint = c;
       
        } else { // Stay at same place
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
        this.pauseTime = 0;
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

        // todo further improve the efficiency
        // CAFE(0), TOILET(1), LEISURE(2), CLASSROOM(3), LIBRARY(4), ENTRANCE(5);
        private int earlyMorningNoClass[][] = { //follows the table in note
            { 1, 10, 10, 0, 10, 5 },
            { 10, 1, 10, 0, 10, 5 },
            { 1, 5, 79, 0, 5, 5 },
            { 1, 5, 5, 0, 2,  5 },
            { 1, 1, 10, 0, 70, 5 },
            { 1, 1, 5, 0, 2, 86 }}; 
        private int earlyMorning[][] = { //follows the table in note
            { 1, 10, 10, 64, 10, 5 },
            { 10, 1, 10, 64, 10, 5 },
            { 1, 5, 79, 5, 5, 5 },
            { 1, 2, 3, 90, 2, 2 },
            { 1, 1, 10, 13, 70, 5 },
            { 1, 1, 5, 5, 2, 86 }}; 
        private int morningNoClass[][] = { //follows the table in note
            { 1, 10, 10, 0, 10, 5 },
            { 10, 1, 10, 0, 10, 5 },
            { 1, 5, 79, 0, 5, 5 }, 
            { 5, 5, 15, 0, 5,  5 },
            { 1, 1, 10, 0, 70, 5 }, 
            { 2, 2, 5, 0, 2, 80 }}; 
        private int morning[][] = { //follows the table in note
            { 1, 10, 10, 64, 10, 5 },
            { 10, 1, 10, 64, 10, 5 },
            { 1, 5, 79, 5, 5, 5 }, 
            { 5, 5, 15, 65, 5,  5 },
            { 1, 1, 10, 13, 70, 5 }, 
            { 2, 2, 5, 9, 2, 80 }}; 
        private int afternoonNoClass[][] = {//follows the table in note
            { 0, 5, 5, 0, 5, 40},
            { 6, 1, 6, 0, 10, 31},
            { 1, 3, 70, 0, 3, 20 },
            { 1, 1, 3, 0, 2, 43},
            { 1, 1, 9, 0, 30, 39 },
            { 1, 1, 1, 0, 1, 90}};
        private int afternoon[][] = {//follows the table in note
            { 0, 5, 5, 45, 5, 40},
            { 6, 1, 6, 46, 10, 31},
            { 1, 3, 70, 3, 3, 20 },
            { 1, 1, 3, 50, 2, 43},
            { 1, 1, 9, 20, 30, 39 },
            { 1, 1, 1, 6, 1, 90}};


        public WaypointTable(){ }

        private int[] getProb(int state) {
            // System.out.println(SimClock.getTime());
            // System.out.println(time + " < " + SimScenario.getInstance().getEndTime() / 2);
            int time = SimClock.getIntTime();

            // simulation time: 08:00 - 18:00
            if (time >= 6 && time < 21) // Don't enter classroom between 08:30-09:45
                return earlyMorningNoClass[state];
            else if (time < 24) // before 10:00
                return earlyMorning[state];
            else if (time >= 30 && time < 45) // Don't enter classroom between 10:30-11:45
                return morningNoClass[state];
            else if (time < 48) // before 12:00
                return morning[state];
            // Don't enter classroom between 12:30 - 13:45 or 14:30 - 15:45 or 16:30 - 17:45 
            else if (time >= 54 && time < 69 || time >= 66 && time < 81 || time >= 78 && time < 93) 
                return afternoonNoClass[state];
            else 
                return afternoon[state];
            
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

        public Coord getClassroomCoord(){
            int classroom[][] = {
                    {890, 390},
                    {890, 390},
                    {220, 220},
                    {410, 380}
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
                case LIBRARY: return new Coord(ThreadLocalRandom.current().nextInt(290, 350 + 1),ThreadLocalRandom.current().nextInt(310, 340 + 1));
                default: return new Coord(ThreadLocalRandom.current().nextInt(800, 825 + 1),ThreadLocalRandom.current().nextInt(315, 335 + 1));
            }
        }
    }
}
