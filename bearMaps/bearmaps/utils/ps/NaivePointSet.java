package bearmaps.utils.ps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class NaivePointSet implements PointSet {

    private List<Point> points;

    @Override
    public Point nearest(double x, double y) {
        Point a = new Point(x,y);
        HashMap<Double, Point> distanceAway = new HashMap<Double, Point>();
        for (Point point: points) {
            distanceAway.put(calculateDistance(a, point), point);
        }

        Double min = Collections.min(distanceAway.keySet());
        return distanceAway.get(min);
    }

    public NaivePointSet(List<Point> points) {
        this.points = points;

    }

    public double calculateDistance(Point a, Point b) {
        return Math.sqrt(Point.distance(a, b));
    }

    public static void main(String[] args) {
        Point a = new Point(-1,2);
        Point b = new Point(-1,-2);
        Point c = new Point(2,1);
        List<Point> allPoints = List.of(a,b,c);
        NaivePointSet naivePointSet = new NaivePointSet(allPoints);
        System.out.println(naivePointSet.nearest(1,1));
        System.out.println(naivePointSet.nearest(-1,0));
        System.out.println(naivePointSet.nearest(-1,-2));



        Point p1 = new Point(1.1, 2.2); // constructs a Point with x = 1.1, y = 2.2
        Point p2 = new Point(3.3, 4.4);
        Point p3 = new Point(-2.9, 4.2);
        NaivePointSet nn = new NaivePointSet(List.of(p1, p2, p3));
        Point ret = nn.nearest(3.0, 4.0); // returns p2
        System.out.println(ret);
        ret.getX(); // evaluates to 3.3
        ret.getY(); // evaluates to 4.4

    }
}
