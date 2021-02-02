import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


import java.util.Random;

import static java.lang.Math.sqrt;
import static java.lang.System.exit;
import static java.lang.System.out;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then method start() far below.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {

    class Actor {
        final Color color;        // Color an existing JavaFX class


        Actor(Color color) {      // Constructor to initialize
            this.color = color;
        }
    }


    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
     void updateWorld() {
        // % of surrounding neighbours that are like me
        double threshold = 0.7;
        //for(int i = 0; i<world.length;i++) {


        int[] freeSpace = fischerYates(empty(world)); //Find the indices for all empty spots, then shuffle them.
        int[] disIndex = listDisGruntled(world, threshold); //Find out where the disgruntled actors are
        world = moveDisgruntled(disIndex,freeSpace,world); //Use those arrays to update tho world array.
    }





    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime
    // That's why we must have "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!
        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.25, 0.25, 0.50};
        // Number of locations (places) in world (must be a square)
        int nLocations = 65536;   // Should also try 90 000
        int size = (int) Math.round(Math.sqrt(nLocations));
        world = new Actor[size][size];

        // TODO initialize the world
        // We first initialize the world in order, and then shuffle the world to get a random distribution
        for(int i = 0;i<nLocations;i++) {
            //Fill the world with red actors until threshold is reached
            if(i < dist[0]*nLocations) {
                setByIndex(world,i, new Actor(Color.DARKSLATEBLUE));
            } else if (i < (1-dist[2])*nLocations) {
                // We assume that the values of dist adds upp to 1
                // That is, if there are k red actors and n colorless actors
                // There must be (1-n-k) blue actors.
                setByIndex(world,i, new Actor(Color.HOTPINK));
            } else {
                // Finally, fill the rest of the array with null
                setByIndex(world,i,null);
            }
        }
        world = fischerYates(world);

        // Should be last
        fixScreenSize(nLocations);
    }

    // ---------------  Methods ------------------------------
        /* The getByIndex and setByIndex methods allow us to index a matrix as if it were a rank 1 array.
    Among other things, this makes shuffling the arrays easy, without forcing us to actually convert
    the world to a rank 1 array and back.

     */

    <T> T getByIndex(T[][] arr, int index) {
        return arr[index/arr.length][index%arr.length];
    }

    <T> void setByIndex(T[][] arr, int index, T value) {
        arr[index/arr.length][index%arr.length] = value;
    }
    //We need two implementations of the fischer-yates shuffling algorithm.
    //One of them works on unidimensional int array, the other utilises our
    //getBy/setByIndex methods to work on 2dimensional generic object arrays.
    int[] fischerYates(int[] arr) {
        Random rand = new Random();
        int length = arr.length;
        for(int i = 0;i< length;i++) {
            int n = rand.nextInt(length-i);
            int tmp = arr[n];
            arr[n] = arr[length-1-i];
            arr[length-1-i] = tmp;
        }
        return arr;
    }

    <T> T[][] fischerYates(T[][] arr) {
        Random rand = new Random();
        int length = arr.length*arr[0].length;
        for(int i = 0;i< length;i++) {
            int n = rand.nextInt(length-i);
            T tmp = getByIndex(arr,n);
            setByIndex(arr,n,getByIndex(arr,length-1-i));
            setByIndex(arr,length-1-i,tmp);
        }
        return arr;
    }



    /* Takes arrays of (Unidimensional) indices to where the disgruntled Actors are and where there are free
    spots, then swaps each  disgruntled actor with an empty spot in the world. This method assumes that there
    are no more disgruntled actors than there are empty spots.
     */
    Actor[][] moveDisgruntled(int[] disgruntled, int[] empty, Actor[][] world) {
        for(int i=0;i<disgruntled.length; i++) {
            Actor tmp = getByIndex(world,disgruntled[i]);
            setByIndex(world,disgruntled[i],null);
            setByIndex(world,empty[i],tmp);
        }
        return world;
    }
    /*
        Iterates over the world matrix and creates an array of indices to where the empty spots
        are, which is then returned. The indices found are unidimensional and can be used with our
        getByindex/setByIndex methods.
     */
    int[] empty(Actor[][] world) {
        int numEmpty = 0;
        //Because we don't have access to the dist array in this method, we do not
        //know how many empty spots there are. Furthermore, arrays are fixed length and we are not
        //allowed to use Lists. This means we need to iterate twice over the array, once to count
        //the number of empty spots, then we need to create the array and iterate a second time over
        //the world
        for(int i =0; i<world.length*world[0].length;i++) {
            if (getByIndex(world, i) == null) {
                numEmpty++;
            }
        }
        int[] allEmpty = new int[numEmpty];
        for(int i =0; i<world.length*world[0].length;i++) {
            if (getByIndex(world, i) == null) {
                allEmpty[--numEmpty] = i;
            }
        }
        return(allEmpty);
    }

    /*  iterates over the world of actors to list how many of them are disgruntled,
    so an array of that length can be constructed and then a second time to populate that array with the
    indices of those actors. Hopefully, this can be done with a single loop later
     */

    int[] listDisGruntled(Actor[][] world, double threshold) {
        int numDisgruntled = 0;
        //Here we have the same problem that we had in the other method,
        //We don't know how many disgruntled actors there are and we cannot grow an array
        for(int i = 0;i<world.length*world[0].length;i++) {
            if(disgruntled(world,i,threshold)) {
                numDisgruntled++;
            }
        }
        int[] allDisGruntled = new int[numDisgruntled];

        for(int i = 0; i<world.length*world[0].length;i++) {
            if (disgruntled(world,i,threshold)) {
                allDisGruntled[--numDisgruntled] = i;
            }
        }
        return allDisGruntled;
    }



    // Check if inside world
    boolean isValidLocation(int size, int row, int col) {
        return 0 <= row && row < size && 0 <= col && col < size;
    }

    /*
    Determines whether actor at index i in the world is disgruntled according to threshold.
     */
    boolean disgruntled(Actor[][] world,int index, double threshold) {
        if (getByIndex(world,index)==null) {
            return false;
        }
        int numSame = -1; //A point should not be counted as its own neighbor
        int numNeighbors = -1;
        int row = index / world.length;
        int col = index % world.length;
        Color color = getByIndex(world,index).color;
        for(int i = -1; i <= 1;i++) {
            for(int j = -1; j <= 1;j++) {
                try {
                    //Two exceptions can occur here, either we try to index outside the world matrix and
                    //Get an ArrayIndexOutOfBoundsException, or we index properly, but hit an empty spot and
                    //try to take the color of that spot, in which case we get a NullPointerException. In Both cases
                    //we take that to mean that there is no neighbor at the position we tried to compare to, which
                    //means we don't want to do anything
                    if(world[row+i][col+j].color == color) {
                        numSame++;
                    }
                    //If we manage to get here without raising an exception we should increment numNeighbors
                    numNeighbors++;
                } catch (Exception e) {
                    //Pass
                }
            }
        }
        return(double) numSame/numNeighbors < threshold;
    }

    // ----------- Utility methods -----------------

    // TODO (general method possible reusable elsewhere)

    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work. Important!!!!
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {new Actor(Color.RED), new Actor(Color.RED), null},
                {null, new Actor(Color.BLUE), null},
                {new Actor(Color.RED), null, new Actor(Color.BLUE)}
        };
        double th = 0.5;   // Simple threshold used for testing

        int size = testWorld.length;
        out.println(isValidLocation(size, 0, 0));
        out.println(!isValidLocation(size, -1, 0));
        out.println(!isValidLocation(size, 0, 3));

        // TODO

        exit(0);
    }

    // ******************** NOTHING to do below this row, it's JavaFX stuff  **************

    double width = 500;   // Size for window
    double height = 500;
    final double margin = 50;
    double dotSize;

    void fixScreenSize(int nLocations) {
        // Adjust screen window
        dotSize = 300000 / nLocations;
        if (dotSize < 1) {
            dotSize = 2;
        }
        width = sqrt(nLocations) * dotSize + 2 * margin;
        height = width;
    }

    long lastUpdateTime;
    final long INTERVAL = 450_000_00;


    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long now) {
                long elapsedNanos = now - lastUpdateTime;
                if (elapsedNanos > INTERVAL) {
                    updateWorld();
                    renderWorld(gc);
                    lastUpdateTime = now;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = (int) (dotSize * col + margin);
                int y = (int) (dotSize * row + margin);
                if (world[row][col] != null) {
                    g.setFill(world[row][col].color);
                    g.fillOval(x, y, dotSize, dotSize);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
