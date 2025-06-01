package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;


import org.opencv.core.Mat;

//import java.util.Dictionary;
import java.util.List;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    private final String TAG = this.getClass().getSimpleName();

    private final double[][] place={
            {10.7,-9.79,4.95},
            {10.91,-8.875,4.55},
            {10.535,-7.88,4.55},
            {10.52,-6.7,4.95}
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


        for(int i=0;i<4;i++){
            double x=place[i][0],y=place[i][1],z=place[i][2];
            float a=angle[i][0],b=angle[i][1],c=angle[i][2],d=angle[i][3];

            Point point=new Point(x,y,z);
            Quaternion q=new Quaternion(a,b,c,d);
            boolean ans=move(point,q,i);
            Log.i(TAG,"ans: "+i+" moving to is : "+ans);
        }


            api.setAreaInfo(1, "something",1);



        /* **************************************************** */
        /* Let's move to each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
         Point point1 = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion quaternion1 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        api.moveTo(point1, quaternion1, false);
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


    public boolean move(Point point, Quaternion quaternion,int i){
        Log.i(TAG, "Attempting to move to, Point:" + point + "; Quaternion: "+quaternion);

        if(point == null || quaternion == null){
            Log.i(TAG, "Count not move, params are null");
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

            Log.i(TAG,"Move attempt :" + retryCount);

            wait(1500);
        } while(!result.hasSucceeded() && retryCount < 3);

        Log.i(TAG, "Move to target failed");
        return false;
    }

    public void wait(int milliseconds){
        Log.i(TAG, "Pausing for: "+milliseconds+"ms");

        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            Log.i(TAG, "Error Pausing for: "+milliseconds+"ms");
            e.printStackTrace();
        }

        return;
    }

}
