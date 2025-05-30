package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
//import java.util.Dictionary;
import java.util.List;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    private final String TAG = this.getClass().getSimpleName();
    //template file name
    private final String[] TEMPLATE_FILE_NAME={
            "coin.png",
            "compass.png",
            "coral.png",
            "crystal.png",
            "diamond.png",
            "emerald.png",
            "fossil.png",
            "key.png",
            "letter.png",
            "shell.png",
            "treasure_box.png"
    };
    //template name
    private final String[] TEMPLATE_NAME={
            "coin",
            "compass",
            "coral",
            "crystal",
            "diamond",
            "emerald",
            "fossil",
            "key",
            "letter",
            "shell",
            "treasure_box"
    };

    private final double[][] place={
            {10.95,-10,5.57},
            {10.925,-9,3},
            {10.10,-8,3},
            {10,-6.86,5}
    };

    private final float[][] angle={
            {0,0,-707f,707f},
            {0,707f,0,707f},
            {0,707f,0,707f},
            {0,1f,0,0}
    };

    @Override
    protected void runPlan1(){
        Log.i(TAG,"Start mission ! ");

        // The mission starts.
        api.startMission();

        // Move to a point.
//        Point point = new Point(10.9d, -9.92284d, 5.195d);
//        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
//        api.moveTo(point, quaternion, false);

        // Get a camera image.
//        Mat image = api.getMatNavCam();
//
//        api.saveMatImage(image,"image_1.png");

        for(int i=0;i<4;i++){
            double x=place[i][0],y=place[i][1],z=place[i][2];
            float a=angle[i][0],b=angle[i][1],c=angle[i][2],d=angle[i][3];

            Point point=new Point(x,y,z);
            Quaternion q=new Quaternion(a,b,c,d);
            move(point,q,i);
        }

        /* ******************************************************************************** */
        /* Write your code to recognize the type and number of landmark items in each area! */
        /* If there is a treasure item, remember it.                                        */
        /* ******************************************************************************** */

        //Ar tag reading
//        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
//        List<Mat>corners=new ArrayList<>();
//        Mat markerIds=new Mat();
//        Aruco.detectMarkers(image,dictionary,corners,markerIds);

        //Get camera matrix
//        Mat cameraMatrix=new Mat(3,3, CvType.CV_64F);
//        cameraMatrix.put(0,0,api.getNavCamIntrinsics()[0]);

        //Get Lens distortion parameters
//        Mat cameraCoefficient=new Mat(1,5,CvType.CV_64F);
//        cameraCoefficient.put(0,0,api.getNavCamIntrinsics()[1]);
//        cameraCoefficient.convertTo(cameraCoefficient,CvType.CV_64F);

        //Undistort image
//        Mat undistortImg=new Mat();
//        Calib3d.undistort(image,undistortImg,cameraMatrix,cameraCoefficient);

        //Pattern matching
        //Load template images
//        Mat[] templates=new Mat[TEMPLATE_FILE_NAME.length];
//
//        for(int i=0;i<TEMPLATE_FILE_NAME.length;i++){
//            try{
//                //open template image file in bitmap from file & convert to Mat
//                InputStream inputStream=getAssets().open(TEMPLATE_FILE_NAME[i]);
//                Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
//
//                Mat mat=new Mat();
//                Utils.bitmapToMat(bitmap,mat);
//
//
//                //convert to grayscale
//                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_BGR2GRAY);
//
//                //Assign to array of template
//                templates[i]=mat;
//                api.saveMatImage(templates[i],i+".png");
//                inputStream.close();
//            }catch (IOException e){
//                e.printStackTrace();
//            }
//        }




            // When you recognize landmark items, let’s set the type and number.
            api.setAreaInfo(1, "something",1);



        /* **************************************************** */
        /* Let's move to each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
         Point point = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion quaternion = new Quaternion(0f, 0f, 0.707f, 0.707f);
        api.moveTo(point, quaternion, false);
        api.reportRoundingCompletion();

        /* ********************************************************** */
        /* Write your code to recognize which target item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();
    }

    @Override
    protected void runPlan2(){
       // write your plan 2 here.
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here.
    }

    // You can add your method.
    private String yourMethod(){
        return "your method";
    }

    //Resize img
//    private Mat resizeImg(Mat img,int width){
//        int height=(int)(img.rows()*((double)width/img.cols()));
//        Mat resizedImg=new Mat();
//        Imgproc.resize(img,resizedImg,new Size(width,height));
//        return resizedImg;
//    }

    //Rotate img
//    private Mat rotImg(Mat img,int angle){
//        //api.saveMatImage(img,"Img_before_rotation.png");
//        org.opencv.core.Point center=new org.opencv.core.Point(img.cols()/2.0,img.rows()/2.0);
//        Mat rotateMat=Imgproc.getRotationMatrix2D(center,angle,1.0);
//        Mat rotateImg=new Mat();
//        Imgproc.warpAffine(img,rotateImg,rotateMat,img.size());
//        //api.saveMatImage(rotateImg,"rotated_img.png");
//        return rotateImg;
//    }

//    private Mat cropAroundMarker(Mat image, List<Mat> corners, double cmSize, Mat cameraMatrix) {
//        if (corners.isEmpty()) return image;
//
//        // Convert 15 cm to pixels
//        double fx = cameraMatrix.get(0, 0)[0]; // fx from intrinsics
//        double approxDistanceM = 1.0; // assume 1 meter from marker
//        double pixelsPerCM = fx / (approxDistanceM * 100.0);
//        int radiusPixels = (int) (pixelsPerCM * cmSize / 2.0);
//
//        // Get center point of first marker
//        Mat marker = corners.get(0); // take first marker
//        double centerX = 0;
//        double centerY = 0;
//        for (int i = 0; i < 4; i++) {
//            centerX += marker.get(0, i)[0];
//            centerY += marker.get(0, i)[1];
//        }
//        centerX /= 4.0;
//        centerY /= 4.0;
//
//        // Crop region around marker center
//        int x = (int) (centerX - radiusPixels);
//        int y = (int) (centerY - radiusPixels);
//        int width = radiusPixels * 2;
//        int height = radiusPixels * 2;
//
//        // Bounds check
//        x = Math.max(0, x);
//        y = Math.max(0, y);
//        width = (x + width > image.cols()) ? image.cols() - x : width;
//        height = (y + height > image.rows()) ? image.rows() - y : height;
//
//        Rect roi = new Rect(x, y, width, height);
//        return new Mat(image, roi);
//    }


    private boolean move(Point point, Quaternion quaternion,int i){
        Log.d("MOVE", "Attempting to move to, Point:" + point + "; Quaternion: "+quaternion);

        if(point == null || quaternion == null){
            Log.d("MOVE", "Count not move, params are null");
            return false;
        }

        int retryCount = 0;
        Result result;
        do {
            result = api.moveTo(point, quaternion, true);

            if(result.hasSucceeded()){
                wait(1500);
                Mat img=api.getMatNavCam();
                wait(10);
                api.saveMatImage(img,i+".jpg");

                return true;
            }
            retryCount++;

            Log.d("MOVE","Move attempt :" + retryCount);

            wait(1500);
        } while(!result.hasSucceeded() && retryCount < 3);

        Log.d("MOVE", "Move to target failed");
        return false;
    }

    public void wait(int milliseconds){
        Log.d("WAIT", "Pausing for: "+milliseconds+"ms");

        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            Log.d("WAIT", "Error Pausing for: "+milliseconds+"ms");
            e.printStackTrace();
        }

        return;
    }

}
