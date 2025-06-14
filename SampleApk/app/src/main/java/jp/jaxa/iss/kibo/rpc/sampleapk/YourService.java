package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.nfc.Tag;
import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YourService extends KiboRpcService {
    private static final String TAG = "YourService__HAHA";
    private Detector detector;
    private Mat[] markerIds;

    private final double[][] place = {
            {10.95,-9.85,5.195},
            {10.925,-8.875,4.52},
            {10.942,-7.75,4.51},
            {10.6,-6.852,4.94}
    };

    private final float[][] angle = {
            {0,0,-0.707f,0.707f},
            {-0.0923f,0.7002f,-0.0923f,0.7002f},
            {0.1651f,0.6876f,0.1651f,0.6876f},
            {0,1f,0,0}
    };


    @Override
    protected void runPlan1() {
        Log.i(TAG, "RUN1 start");


        try {
            detector = new Detector(getApplicationContext());
        } catch (IOException e) {
            Log.i(TAG, "Failed to init Detector", e);
        }

        api.startMission();

        // Move through each area, capture and detect


        /* **************************************************** */
        /* Let's move to 1 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 1st area, capture and detect start");

        Point p = new Point(place[0][0], place[0][1], place[0][2]);
        Quaternion q = new Quaternion(
                angle[0][0], angle[0][1], angle[0][2], angle[0][3]
        );
        move(p, q, 1);
        Bitmap image=getImg(1);
        sleep(50);
        detect(image,1);
        Log.i(TAG, "================Move through 1st area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 2 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 2nd area, capture and detect start");

        Point p1 = new Point(place[1][0], place[1][1], place[1][2]);
        Quaternion q1 = new Quaternion(
                angle[1][0], angle[1][1], angle[1][2], angle[1][3]
        );
        move(p1, q1, 2);
        Bitmap image1=getImg(2);
        sleep(50);
        detect(image1,2);
        Log.i(TAG, "================Move through 2nd area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 3 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 3rd area, capture and detect start");

        Point p2 = new Point(place[2][0], place[2][1], place[2][2]);
        Quaternion q2 = new Quaternion(
                angle[2][0], angle[2][1], angle[2][2], angle[2][3]
        );
        move(p2, q2, 3);
        Bitmap image2=getImg(3);
        sleep(50);
        detect(image2,3);
        Log.i(TAG, "================Move through 3rd area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 4 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 3rd area, capture and detect start");

        Point p3 = new Point(place[3][0], place[3][1], place[3][2]);
        Quaternion q3 = new Quaternion(
                angle[3][0], angle[3][1], angle[3][2], angle[3][3]
        );
        move(p3, q3, 4);
        Bitmap image3=getImg(4);
        sleep(50);
        detect(image3,4);
        Log.i(TAG, "================Move through 3rd area, capture and detect success======");


        // Final position in front of astronaut
        Point p5 = new Point(11.143, -6.7607, 4.9654);
        Quaternion q5 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        move(p5, q5, 5);

        //=============================
        api.reportRoundingCompletion();
        //===============================
        sleep(2000);
        Bitmap image4=getImg(5);
        sleep(50);
        detect(image4,5);


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


        Log.i(TAG, "RUN1 shesh");

    }

    /**
     * Moves, captures, runs detection, and reports via setAreaInfo.
     * @param idx used both in filename and as areaId for reporting
     */
    private void move(Point point, Quaternion quat, int idx) {

        Log.i(TAG, "Moving to " + point);
        Result r = api.moveTo(point, quat, true);
        if (!r.hasSucceeded()) {
            Log.i(TAG, "Move failed idx=" + idx);
            return;
        }
        Log.i(TAG,"Move passed "+idx+" =========>>>>>>>>>>");
        sleep(500);
    }


    private void detect(Bitmap image,int idx){
        Log.i(TAG,"starting detection , idx "+idx+" =========>>>>>>>>>>");
        // 3) Convert to Bitmap and detect
        api.saveBitmapImage(image,"bitmap_"+idx+".jpg");
        //=============================
        Detector.Result det = detector.detect(image);

        // 4) Log detections
        for (Detection d : det.detections) {
            String lbl = detector.labels.get(d.classIdx);
            Log.i(TAG, String.format(
                    "→ %s @ [%.1f,%.1f→%.1f,%.1f] score=%.2f",
                    lbl, d.box.left, d.box.top, d.box.right, d.box.bottom, d.score
            ));
        }

        // 5) Report counts via setAreaInfo(areaId, itemName, number)
        for (Map.Entry<String,Integer> e : det.counts.entrySet()) {
            String itemName = e.getKey();
            int count       = e.getValue();
            Log.i(TAG,"calling setAreaInfo ()^^^^^^^^^^^^^^^^^^");
            api.setAreaInfo(idx, itemName, count);
            Log.i(TAG, "Reported area " + idx + ": " + itemName + " × " + count);
        }
    }

    public Bitmap getImg(int i){
        Log.i(TAG,"starting image processing , idx "+i+" =========>>>>>>>>>>");
        Mat image=api.getMatNavCam();


        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat> corners=new ArrayList<>();
        Mat markerId=new Mat();
        Aruco.detectMarkers(image,dictionary,corners,markerId);
//        markerIds[i]=new Mat();
//        markerIds[i] = markerId.clone();
        Log.i(TAG,i+" no Marker ID is : "+markerId);
        api.saveMatImage(image,"captured_raw_"+1+".jpg");

        //Get camera matrix
        Mat cameraMatrix=new Mat(3,3, CvType.CV_64F);
        cameraMatrix.put(0,0,api.getNavCamIntrinsics()[0]);

        //Get Lens distortion parameters
        Mat cameraCoefficient=new Mat(1,5,CvType.CV_64F);
        cameraCoefficient.put(0,0,api.getNavCamIntrinsics()[1]);
        cameraCoefficient.convertTo(cameraCoefficient,CvType.CV_64F);

        //Undistort image
        Mat undistorting=new Mat();
        Calib3d.undistort(image,undistorting,cameraMatrix,cameraCoefficient);
        api.saveMatImage(image,"undistort_"+i+".jpg");

        Mat rgb = new Mat();
        Imgproc.cvtColor(image, rgb, Imgproc.COLOR_BGR2RGB);
        Bitmap bitmap = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(rgb,bitmap);

        Log.i(TAG,"Returning bitmap image for , idx "+i+" =========>>>>>>>>>>");

        return bitmap;
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) {}
    }

    @Override
    public void onDestroy() {
        if (detector != null) detector.close();
        super.onDestroy();
    }

    @Override protected void runPlan2() { }
    @Override protected void runPlan3() { }
}