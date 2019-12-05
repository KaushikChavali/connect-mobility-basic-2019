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
        //System.out.println("cuttime:" + this.pauseTime);//
    }

    // TODO Align the time!
    //DONE. VERIFY!
    //10 minutes equals 250 time units in the simulation
    private int getInterval() {
        switch (state.getNumVal()) {
            case 0: return 250; //CAFE 10 minutes
            case 1: return 250; //TOILET 10 minutes
            case 2: return 500; //LEISURE 20 minutes
            case 3: return 4500; //CLASSROOM 90 minutes UNUSED
            case 4: return 750; //LIBRARY 30 minutes
            default: return 0;
        }
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        
        //Variable pause time implementation
        final double curTime = SimClock.getTime();
        //final double endTime = SimScenario.getInstance().getEndTime();
 
        //Timetable for pause time (Repeats every hour)
        //Scenario Time: 6000
        //Classroom: 10:15-11:45
        //Cafe: 10:10-10:20 and 10:40-10:50
        //Toilet: None
        //Leisure: 10:10-10:30 and 10:40-11:00
        //Library: 10:00-10:20 and 10:30-10:50
 
        //blockTime of 1 hour, i.e., 1500
        //final double blockTime = curTime % 1500;        
        
        // Update state machine every time we pick a path
        
        // TODO Align the time!
        //DONE. VERIFY!

        if (this.state == State.CLASSROOM) { 
            if (curTime < 375 || curTime > 2625 && curTime < 3375 || curTime > 5625) { // If lecture over
                this.state = waypointTable.getNextState(this.state);
                this.pauseTime = getInterval();
                //System.out.println("Lecture is over, going to: " + this.state);

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
            //System.out.println("Time's up, going to: " + this.state);
        
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

        // TODO further improve the efficiency
        // CAFE(0), TOILET(1), LEISURE(2), CLASSROOM(3), LIBRARY(4), ENTRANCE(5);
        private int earlyMorningNoClass[][] = { //follows the table in note
            { 0, 10, 10, 0, 10, 5 },
            { 1, 1, 10, 0, 10, 5 },
            { 1, 5, 90, 0, 5, 5 },
            { 1, 1, 1, 0, 2,  5 },
            { 1, 1, 4, 0, 184, 5 },
            { 1, 1, 5, 0, 2, 595 }};
        private int earlyMorning[][] = { //follows the table in note
            { 0, 10, 10, 70, 10, 5 },
            { 1, 1, 10, 70, 10, 5 },
            { 1, 5, 90, 5, 5, 5 },
            { 1, 1, 1, 95, 2,  5 },
            { 1, 1, 4, 7, 184, 5 },
            { 1, 1, 5, 5, 2, 595 }};
        private int morningNoClass[][] = { //follows the table in note
            { 0, 10, 10, 0, 10, 5 },
            { 1, 1, 9, 0, 10, 5 },
            { 1, 1, 190, 0, 5, 5 },
            { 1, 1, 1, 0, 2,  5 },
            { 1, 1, 4, 0, 150, 5 },
            { 5, 2, 7, 0, 2, 380 }};
        private int morning[][] = { //follows the table in note
            { 0, 10, 10, 70, 10, 5 },
            { 1, 1, 9, 70, 10, 5 },
            { 1, 1, 190, 5, 5, 5 },
            { 1, 1, 1, 95, 2,  5 },
            { 1, 1, 4, 7, 150, 5 },
            { 5, 2, 7, 10, 2, 380 }};
        private int afternoonNoClass[][] = {//follows the table in note
            { 0, 5, 5, 0, 5, 57},
            { 6, 1, 6, 0, 10,  66},
            { 6, 3, 146, 0, 3, 20 },
            { 1, 1, 1, 0, 2,  76 },
            { 1, 1, 1, 0, 75, 9 },
            { 1, 1, 19, 0, 19, 95 } };
        private int afternoon[][] = {//follows the table in note
            { 0, 5, 5, 39, 5, 57},
            { 6, 1, 6, 46, 10,  66},
            { 6, 3, 146, 6, 3, 20 },
            { 1, 1, 1, 72, 2,  76 },
            { 1, 1, 1, 1, 75, 9 },
            { 1, 1, 19, 39, 19, 95 } };


        public WaypointTable(){ }

        private int[] getProb(int state) {
            // System.out.println(SimClock.getTime());
            int time = SimClock.getIntTime();

            //System.out.println(time + " < " + SimScenario.getInstance().getEndTime() / 2);
            // TODO Align the time!
            // TODO Don't enter classroom between 10:30-11:45
            //DONE. VERIFY!
            if (time > 750 && time < 2625)
                return earlyMorningNoClass[state];
            else if (time < 1500)
                return earlyMorning[state];
            else if (time < ( SimScenario.getInstance().getEndTime() / 2))
                return morning[state];
            else {
                //System.out.println("using afternoon time");
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
