
package bearmaps.server.handler.impl;

import bearmaps.AugmentedStreetMapGraph;
import bearmaps.server.handler.APIRouteHandler;
import spark.Request;
import spark.Response;
import bearmaps.utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static bearmaps.utils.Constants.SEMANTIC_STREET_GRAPH;
import static bearmaps.utils.Constants.ROUTE_LIST;

/**
 * Handles requests from the web browser for map images. These images
 * will be rastered into one large image to be displayed to the user.
 * @author rahul, Josh Hug, _________
 */
public class RasterAPIHandler extends APIRouteHandler<Map<String, Double>, Map<String, Object>> {

    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside RasterAPIHandler.processRequest(). <br>
     * ullat : upper left corner latitude, <br> ullon : upper left corner longitude, <br>
     * lrlat : lower right corner latitude,<br> lrlon : lower right corner longitude <br>
     * w : user viewport window width in pixels,<br> h : user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
            "lrlon", "w", "h"};

    /**
     * The result of rastering must be a map containing all of the
     * fields listed in the comments for RasterAPIHandler.processRequest.
     **/
    private static final String[] REQUIRED_RASTER_RESULT_PARAMS = {"render_grid", "raster_ul_lon",
            "raster_ul_lat", "raster_lr_lon", "raster_lr_lat", "depth", "query_success"};


    @Override
    protected Map<String, Double> parseRequestParams(Request request) {
        return getRequestParams(request, REQUIRED_RASTER_REQUEST_PARAMS);
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param requestParams Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @param response : Not used by this function. You may ignore.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image;
     *                    can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    private static Double[] origin = new Double[]{-122.29980468, -122.21191406, 37.89219554, 37.82280243, (122.29980468-122.21191406)/256, (122.29980468-122.21191406), (37.89219554-37.82280243)};
    //Note: probably not exact enough but if it works, it works.
    static Double[] LonDPPs = new Double[]{origin[4], origin[4]/2, origin[4]/4, origin[4]/8, origin[4]/16, origin[4]/32,origin[4]/64,origin[4]/128};
    @Override
    public Map<String, Object> processRequest(Map<String, Double> requestParams, Response response) {
//        System.out.println("yo, wanna know the parameters given by the web browser? They are:");
        //ul = upper left , lr = lower right
        //PARAMETERS
        double lowerrightx = requestParams.get("lrlon");
        double lowerrighty = requestParams.get("lrlat");
        double upperleftx = requestParams.get("ullon");
        double upperlefty = requestParams.get("ullat");
        double width = requestParams.get("w");

        //Error cases
        if (upperlefty==lowerrighty || upperleftx==lowerrightx || origin[1] < upperleftx || origin[0] > lowerrightx || origin[2] < lowerrighty || origin[3] >upperlefty) {
            return queryFail();
        }

        Double LonDPP = (lowerrightx - upperleftx)/width;
        Integer resolution = findImageReso(LonDPP);

        if (resolution == 0) {
            System.out.println("testing"); //is resolution 0 ever used???
        }

        //FIRST FIND IMAGE RESOLUTION TO USE, then what images to use (fit in the lrlon stuff etc.)
        Map<String, Object> result = new HashMap<>();
        String[][] images = findImages(resolution, upperleftx, upperlefty, lowerrightx, lowerrighty);
        result.put("render_grid", images);
        ArrayList<Double> corners = getCorners(images, resolution);
        result.put("raster_ul_lon", corners.get(0)); //x
        result.put("raster_ul_lat", corners.get(1)); //y
        result.put("raster_lr_lon", corners.get(2));
        result.put("raster_lr_lat", corners.get(3));
        result.put("depth", resolution);
        result.put("query_success", true);

////        System.out.println(requestParams);
//        System.out.println("result: " +result);
//        System.out.println(Arrays.deepToString(images));
        return result;
    }
    private Integer findImageReso(Double LonDPP) {
        Integer resolution = 0;
        for (int i = 0; i<8; i++) {
            resolution = i;
            if (LonDPPs[i] < LonDPP) {//if it is the "greatest smallest resolution", stop
                break;
            }
        }
        return resolution;
    }

    private String[][] findImages(Integer resolution, double TLx, double TLy, double BRx, double BRy) {

        double imageLimit = Math.pow(2, resolution); //rresolution 1 has 2 images width and height, 2 has 4, etc.

        double oTLx = origin[0];//-122.29980468;
        double oTLy = origin[2]; //37.89219554;
        double oBRx = origin[1];
        double oBRy = origin[3];
        double width = origin[5];
        double height = origin[6];

        double unit1 = width/imageLimit;
        double unit2 = height/imageLimit;

        Integer iTLx; //a = TLx - oTLx. If a<0 (oTLx>TLx), iTLx = 0. else iTLx = Math.floor(a/(width/imageLimit));
        Integer iBRx;
        double diff1 = TLx - oTLx;
        if (diff1 <= 0) {
            iTLx = 0;
        }
        else {
            iTLx = (int) (diff1/unit1);
        }

        double diff2 = oBRx - BRx;
        if (diff2 <= 0) {
            iBRx = (int) imageLimit-1;
        }
        else {
            iBRx = (int) (imageLimit - (diff2/unit1));
        }

        Integer iTLy;
        Integer iBRy;
        double diff3 = oTLy - TLy;
        if (diff3 <= 0) {
            iTLy = 0;
        }
        else {
            iTLy = (int) (diff3/unit2);
        }

        double diff4 = BRy - oBRy;
        if (diff4 <= 0) {
            iBRy = (int) imageLimit-1;
        }
        else {
            iBRy = (int) (imageLimit - (diff4/unit2));
        }


        String image_start = "d" +resolution + "_x0_y";
        ArrayList<String[]> result = new ArrayList<>();
        for (int i = iTLy; i<iBRy+1; i++) {
            String name = image_start + i; //d9_x0_x7 (7)
            ArrayList<String> row = new ArrayList<>();
            for (int j = iTLx; j<iBRx+1; j++) {
                String new_name = name.substring(0, 4) + j + name.substring(5);
                row.add(new_name + ".png");
            }
            Object[] temp = row.toArray();
            result.add(Arrays.copyOf(temp, temp.length, String[].class));
        }

        /*for (int i=0; i<imageLimit; i++) {
            String name = image_start + i;
            String[] imageRow = imageRow(resolution, imageLimit, name, TLx, TLy, BRx, BRy);
            if (imageRow.length == 0) {
                continue;
            }
            if (!(result.isEmpty()) && imageRow.length==0) {
                break;
            }
            result.add(imageRow);
        }*/
        Object[] result2 = result.toArray();
        //Below is from "https://stackoverflow.com/questions/42079944/cast-java-array-of-arrays-to-java-2d-array"
        String[][] result3 = (String[][]) Arrays.stream(result2).map(Object[].class::cast).toArray(String[][]::new);
//        System.out.println(Arrays.deepToString(result3));

        return result3;
    }

    /*private String[] imageRow(Integer resolution, double imageLimit, String name, double TLx, double TLy, double BRx, double BRy) {
        ArrayList<String> row = new ArrayList<>();

        for (int i = 0; i<imageLimit; i++) {
            String new_name = name.substring(0, 4) + i + name.substring(5);

            ArrayList<Double> corners = getCorners(new String[][]{{new_name}}, resolution);
            if (corners.get(0) >= TLx && corners.get(0) < BRx) { //left side of pic in range
                if ((corners.get(3) >= BRy && corners.get(3) < TLy) || //bottom in range
                        (corners.get(1) <= TLy && corners.get(1) > BRy) || //top in range
                        (corners.get(1) >= TLy && corners.get(3) <= BRy)) {  //"envelope"
                    row.add(new_name +".png");
                    continue;
                }
            }
            if (corners.get(2) <= BRx && corners.get(2) > TLx) { //right side of pic in range
                if ((corners.get(3) >= BRy && corners.get(3) < TLy) || //bottom in range
                        (corners.get(1) <= TLy && corners.get(1) > BRy) || //top in range
                        (corners.get(1) >= TLy && corners.get(3) <= BRy)) {  //"envelope"
                    row.add(new_name +".png");
                    continue;
                }
            }
            if (corners.get(0) <= TLx && corners.get(2) >= BRx) {//"envelope" by mid of yaxis
                if (corners.get(1) >= TLy && corners.get(3) <= BRy) { //envelope entirely
                    row.add(new_name +".png");
                    break;
                }
                if ((corners.get(3) >= BRy && corners.get(3) < TLy) || //bottom in range
                        (corners.get(1) <= TLy && corners.get(1) > BRy)) {//top in range
                        row.add(new_name + ".png");
                        continue;
                }
            }
        }

        Object[] temp = row.toArray();
        //String[] result = Arrays.copyOf(temp, temp.length, String[].class);
        return Arrays.copyOf(temp, temp.length, String[].class);
    }*/

    //0th index, Topleft x, 1st index topleft y
    private ArrayList<Double> getCorners(String[][] images, Integer resolution) {
        ArrayList<Double> result = new ArrayList<>();
        Double width = origin[5];
        Double height = origin[6];
        double divider = Math.pow(2, resolution);

        Integer[] xy = middlenum(images[0][0]);
        //topleftx/long
        result.add(origin[0] + width * (xy[0]/divider));
        //toplefty/lat
        result.add(origin[2] - height * (xy[1]/divider));


        String[] bottomRow = images[images.length-1];
        String lastsquare = bottomRow[bottomRow.length-1];
        Integer[] xy2 = middlenum(lastsquare);
        //bottomrightx
        result.add(origin[0] + width * (1+(xy2[0]))/divider);
        //bottomrighty
        result.add(origin[2] - height * (1+(xy2[1]))/divider);
        return result;
    }
    private Integer[] middlenum(String name) { //input is something like "d7_x0_y0
        Integer[] finall = new Integer[2];
        String result = "";
        String result2 = "";
//        if (name.equals("d7_x127_y10")) {
//            System.out.println("debugging");
//        }
        for (int i = 4; true; i++) {
            Character a = name.charAt(i);
            if (Character.isDigit(a)) {
                result += a;
            }
            else {
                finall[0] = Integer.parseInt(result);
                break;
            }
        }
        for (int i = name.length()-1; true; i--) {
            Character a = name.charAt(i);
            if (Character.isDigit(a)) {
                result2 += a;
            }
            else if ((".png").contains(a.toString())) {//ignore if .png is added
                continue;
            }
            else {
                //from https://stackoverflow.com/questions/7569335/reverse-a-string-in-java
                result2 = new StringBuilder(result2).reverse().toString();
                finall[1] = Integer.parseInt(result2);
                break;
            }
        }
        return finall;
    }


    @Override
    protected Object buildJsonResponse(Map<String, Object> result) {
        boolean rasterSuccess = validateRasteredImgParams(result);

        if (rasterSuccess) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeImagesToOutputStream(result, os);
            String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
            result.put("b64_encoded_image_data", encodedImage);
        }
        return super.buildJsonResponse(result);
    }

    private Map<String, Object> queryFail() {
        Map<String, Object> results = new HashMap<>();
        results.put("render_grid", null);
        results.put("raster_ul_lon", 0);
        results.put("raster_ul_lat", 0);
        results.put("raster_lr_lon", 0);
        results.put("raster_lr_lat", 0);
        results.put("depth", 0);
        results.put("query_success", false);
        return results;
    }

    /**
     * Validates that Rasterer has returned a result that can be rendered.
     * @param rip : Parameters provided by the rasterer
     */
    private boolean validateRasteredImgParams(Map<String, Object> rip) {
        for (String p : REQUIRED_RASTER_RESULT_PARAMS) {
            if (!rip.containsKey(p)) {
                System.out.println("Your rastering result is missing the " + p + " field.");
                return false;
            }
        }
        if (rip.containsKey("query_success")) {
            boolean success = (boolean) rip.get("query_success");
            if (!success) {
                System.out.println("query_success was reported as a failure");
                return false;
            }
        }
        return true;
    }

    /**
     * Writes the images corresponding to rasteredImgParams to the output stream.
     * In Spring 2016, students had to do this on their own, but in 2017,
     * we made this into provided code since it was just a bit too low level.
     */
    private  void writeImagesToOutputStream(Map<String, Object> rasteredImageParams,
                                            ByteArrayOutputStream os) {
        String[][] renderGrid = (String[][]) rasteredImageParams.get("render_grid");
        int numVertTiles = renderGrid.length;
        int numHorizTiles = renderGrid[0].length;

        BufferedImage img = new BufferedImage(numHorizTiles * Constants.TILE_SIZE,
                numVertTiles * Constants.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics graphic = img.getGraphics();
        int x = 0, y = 0;

        for (int r = 0; r < numVertTiles; r += 1) {
            for (int c = 0; c < numHorizTiles; c += 1) {
                graphic.drawImage(getImage(Constants.IMG_ROOT + renderGrid[r][c]), x, y, null);
                x += Constants.TILE_SIZE;
                if (x >= img.getWidth()) {
                    x = 0;
                    y += Constants.TILE_SIZE;
                }
            }
        }

        /* If there is a route, draw it. */
        double ullon = (double) rasteredImageParams.get("raster_ul_lon"); //tiles.get(0).ulp;
        double ullat = (double) rasteredImageParams.get("raster_ul_lat"); //tiles.get(0).ulp;
        double lrlon = (double) rasteredImageParams.get("raster_lr_lon"); //tiles.get(0).ulp;
        double lrlat = (double) rasteredImageParams.get("raster_lr_lat"); //tiles.get(0).ulp;

        final double wdpp = (lrlon - ullon) / img.getWidth();
        final double hdpp = (ullat - lrlat) / img.getHeight();
        AugmentedStreetMapGraph graph = SEMANTIC_STREET_GRAPH;
        List<Long> route = ROUTE_LIST;

        if (route != null && !route.isEmpty()) {
            Graphics2D g2d = (Graphics2D) graphic;
            g2d.setColor(Constants.ROUTE_STROKE_COLOR);
            g2d.setStroke(new BasicStroke(Constants.ROUTE_STROKE_WIDTH_PX,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            route.stream().reduce((v, w) -> {
                g2d.drawLine((int) ((graph.lon(v) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(v)) * (1 / hdpp)),
                        (int) ((graph.lon(w) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(w)) * (1 / hdpp)));
                return w;
            });
        }

        rasteredImageParams.put("raster_width", img.getWidth());
        rasteredImageParams.put("raster_height", img.getHeight());

        try {
            ImageIO.write(img, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private BufferedImage getImage(String imgPath) {
        BufferedImage tileImg = null;
        if (tileImg == null) {
            try {
                File in = new File(imgPath);
                tileImg = ImageIO.read(in);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return tileImg;
    }
}