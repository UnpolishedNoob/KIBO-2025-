package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

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

    @Override
    protected void runPlan1(){
        Log.i(TAG,"Start mission ! ");

        // The mission starts.
        api.startMission();

        // Move to a point.
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        api.moveTo(point, quaternion, false);

        // Get a camera image.
        Mat image = api.getMatNavCam();

        api.saveMatImage(image,"image_1.png");

        /* ******************************************************************************** */
        /* Write your code to recognize the type and number of landmark items in each area! */
        /* If there is a treasure item, remember it.                                        */
        /* ******************************************************************************** */

        //Ar tag reading
        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat>corners=new ArrayList<>();
        Mat markerIds=new Mat();
        Aruco.detectMarkers(image,dictionary,corners,markerIds);

        //Get camera matrix
        Mat cameraMatrix=new Mat(3,3, CvType.CV_64F);
        cameraMatrix.put(0,0,api.getNavCamIntrinsics()[0]);

        //Get Lens distortion parameters
        Mat cameraCoefficient=new Mat(1,5,CvType.CV_64F);
        cameraCoefficient.put(0,0,api.getNavCamIntrinsics()[1]);
        cameraCoefficient.convertTo(cameraCoefficient,CvType.CV_64F);

        //Undistort image
        Mat undistortImg=new Mat();
        Calib3d.undistort(image,undistortImg,cameraMatrix,cameraCoefficient);

        //Pattern matching
        //Load template images
        Mat[] templates=new Mat[TEMPLATE_FILE_NAME.length];

        for(int i=0;i<TEMPLATE_FILE_NAME.length;i++){
            try{
                //open template image file in bitmap from file & convert to Mat
                InputStream inputStream=getAssets().open(TEMPLATE_FILE_NAME[i]);
                Bitmap bitmap= BitmapFactory.decodeStream(inputStream);

                Mat mat=new Mat();
                Utils.bitmapToMat(bitmap,mat);


                //convert to grayscale
                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_BGR2GRAY);

                //Assign to array of template
                templates[i]=mat;
                api.saveMatImage(templates[i],i+".png");
                inputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        //Number of matches for each templates
        int templateMatchCount[]=new int[templates.length];

        //Get the number of template matches
        for(int tempNum=0;tempNum<templates.length;tempNum++){
            //number of matches
            int matchcnt=0;

            //coordinates of the matched location
            List<org.opencv.core.Point>matches=new ArrayList<>();

            //Loading template image and target image
            Mat template=templates[tempNum].clone();
            Mat targetImg=undistortImg.clone();

            //Pattern matching
            int widthMin=20;//px
            int widthMax=100;
            int changeWidth=100;
            int changeAngle=45;

            for(int size=widthMin;size<=widthMax;size+=changeWidth){
                for(int angle=0;angle<360;angle+=changeAngle){
                    Mat resizedTemp=resizeImg(template,size);
                    Mat rotresizedTemp=rotImg(resizedTemp,angle);

                    Mat result = new Mat();
                    Imgproc.matchTemplate(targetImg,rotresizedTemp,result,Imgproc.TM_CCOEFF_NORMED);

                    //Get coordinates with similarity greater than or equal to the threshhold

                    double threshhold=0.7;
                    Core.MinMaxLocResult mmlr=Core.minMaxLoc(result);
                    double maxVal=mmlr.maxVal;

                    if(maxVal>=threshhold){
                        //Extract only results greater than or equal to the threshold
                        Mat thresholdResult=new Mat();
                        Imgproc.threshold(result,thresholdResult,threshhold,1.0,Imgproc.THRESH_TOZERO);
                        api.saveMatImage(targetImg,angle+"degree_"+size+"size_target.png");
                        api.saveMatImage(rotresizedTemp,angle+"degree_"+size+"size_target.png");

                        //Get coordinates of matched location
                        for(int y=0;y<thresholdResult.rows();y++){
                            for(int x=0;x<thresholdResult.cols();x++){
                                if(thresholdResult.get(y,x)[0]>0){
                                    //matchcnt++;
                                    matches.add(new org.opencv.core.Point(x,y));
                                }
                            }
                        }
                    }
                }
            }
            //Avoid detecting the same location multiple times
            List<org.opencv.core.Point>filterMatches=removeDuplicate(matches);
            matchcnt+=filterMatches.size();

            //number of matches for each template
            templateMatchCount[tempNum]=matchcnt;
        }

        int mostMatchTemplateNum=getMaxIndex(templateMatchCount);

        // When you recognize landmark items, let’s set the type and number.
        api.setAreaInfo(1, TEMPLATE_NAME[mostMatchTemplateNum], templateMatchCount[mostMatchTemplateNum]);
        Log.i(TAG,"found"+TEMPLATE_NAME[mostMatchTemplateNum]);

        /* **************************************************** */
        /* Let's move to each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
        point = new Point(11.143d, -6.7607d, 4.9654d);
        quaternion = new Quaternion(0f, 0f, 0.707f, 0.707f);
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
    private Mat resizeImg(Mat img,int width){
        int height=(int)(img.rows()*((double)width/img.cols()));
        Mat resizedImg=new Mat();
        Imgproc.resize(img,resizedImg,new Size(width,height));
        return resizedImg;
    }

    //Rotate img
    private Mat rotImg(Mat img,int angle){
        //api.saveMatImage(img,"Img_before_rotation.png");
        org.opencv.core.Point center=new org.opencv.core.Point(img.cols()/2.0,img.rows()/2.0);
        Mat rotateMat=Imgproc.getRotationMatrix2D(center,angle,1.0);
        Mat rotateImg=new Mat();
        Imgproc.warpAffine(img,rotateImg,rotateMat,img.size());
        //api.saveMatImage(rotateImg,"rotated_img.png");
        return rotateImg;
    }

    //find the distance between two points
    private double calculateDistance(org.opencv.core.Point p1,org.opencv.core.Point p2){
        double dx=p1.x-p2.x;
        double dy=p1.y-p2.y;
        return Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2));
    }

    //Remove multiple detections
    private List<org.opencv.core.Point>removeDuplicate(List<org.opencv.core.Point>points){
        double length=10;
        List<org.opencv.core.Point>filteredList=new ArrayList<>();

        for(org.opencv.core.Point point:points){
            boolean include=false;
            for(org.opencv.core.Point checkpoint:filteredList){
                double distance=calculateDistance(point,checkpoint);
                if(distance<=length){
                    include=true;
                    break;
                }
            }
            if(!include){
                filteredList.add(point);
            }
        }
        return filteredList;
    }

    //get maximum value of an array
    private int getMaxIndex(int[] array){
        int max=0;
        int maxid=0;
        for(int i=0;i< array.length;i++){
            if(array[i]>max){
                max=array[i];
                maxid=i;
            }
        }
        return maxid;
    }
}
