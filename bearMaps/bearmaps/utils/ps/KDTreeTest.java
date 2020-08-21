package bearmaps.utils.ps;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KDTreeTest {

    @Test
    public void findNearestTest1() throws InterruptedException {
        //borrowed from https://stackoverflow.com/questions/3680637/generate-a-random-double-in-a-range
        Double min = 0.0;  // Set To Your Desired Min Value
        Double max = 10.0; // Set To Your Desired Max Value
        List<Point> allPoints =  new ArrayList();
        int results = 0;
        for (int k = 0; k < 100; k++) {
            for (int i = 0; i < 3; i ++) {
                double x = (Math.random() * ((max - min) + 1)) + min;   // This Will Create A Random Number Inbetween Your Min And Max.
                double y = (Math.random() * ((max - min) + 1)) + min;
                double xrounded = Math.round(x * 100.0) / 100.0;
                double yrounded = Math.round(y * 100.0) / 100.0;
                Point point = new Point(xrounded, yrounded);
                allPoints.add(point);
            }

            double x = (Math.random() * ((max - min) + 1)) + min;   // This Will Create A Random Number Inbetween Your Min And Max.
            double y = (Math.random() * ((max - min) + 1)) + min;
            double xrounded = Math.round(x * 100.0) / 100.0;
            double yrounded = Math.round(y * 100.0) / 100.0;

            NaivePointSet naive = new NaivePointSet(allPoints);
            KDTree kd = new KDTree(allPoints);
            System.out.println(naive.nearest(xrounded, yrounded));
            System.out.println(kd.nearest(xrounded, yrounded));
            System.out.println("naive, correct: " + naive.nearest(xrounded, yrounded));
            System.out.println("KD tree implementation : " + kd.nearest(xrounded, yrounded));
            if (naive.nearest(xrounded, yrounded).equals(kd.nearest(xrounded, yrounded) )){
                results +=1;
            }
//            break;


        }
        System.out.println(results);


    }
}