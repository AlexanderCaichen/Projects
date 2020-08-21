package bearmaps.utils.ps;


import java.util.List;

public class KDTree implements  PointSet{

    List<Point> allPoints;

    public KDTree(List<Point> allPoints) {
        this.allPoints = allPoints;
    }

    @Override
    public Point nearest(double x, double y){

        NaivePointSet a = new NaivePointSet(allPoints);
        Point toReturn = null;

        toReturn = a.nearest(x, y);

        return toReturn;
    }
}
