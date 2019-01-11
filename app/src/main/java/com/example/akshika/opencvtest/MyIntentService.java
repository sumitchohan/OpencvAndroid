package com.example.akshika.opencvtest;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService {
    private static final String TAG = "OCVSample::Activity";
    private static final int REQUEST_PERMISSION = 100;
    private int w, h;
    private CameraBridgeViewBase mOpenCvCameraView;
    TextView tvName;
    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);
    FeatureDetector detector;
    DescriptorExtractor descriptor;
    DescriptorMatcher matcher;
    Mat descriptors2,descriptors1;
    Mat img1, img2;
    MatOfKeyPoint keypoints1,keypoints2;
    KeyPoint[] keyPoints1Arr;
    String screenImgPath="";
    String itemImgPaths="";
    String directoryPath="";
    String resultsFilePath="";
    String input_offset_x="";
    String input_offset_y="";
    String input_width="";
    String input_height="";
    public MyIntentService() {
        super("MyIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            screenImgPath= intent.getExtras().getString("screenImgPath");;
            itemImgPaths= intent.getExtras().getString("itemImgPaths");;
            directoryPath= intent.getExtras().getString("directoryPath");;
            resultsFilePath= intent.getExtras().getString("resultsFilePath");;
            input_offset_x= intent.getExtras().getString("input_offset_x");;
            input_offset_y= intent.getExtras().getString("input_offset_y");;
            input_width= intent.getExtras().getString("input_width");;
            input_height= intent.getExtras().getString("input_height");;
            Process();
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                    try {
                        //processRequest("/sdcard/icon_bar.png"));
                        processRequest(screenImgPath,itemImgPaths,directoryPath,resultsFilePath, input_offset_x,input_offset_y,
                                input_width,input_height);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    public void Process()
    {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    private void processRequest(String imagePath, String imgItemsPaths, String directoryPath, String resultsFilePath,
                                String input_offset_x,
                                String input_offset_y,
                                String input_width,
                                String input_height) throws IOException {

        detector = FeatureDetector.create(FeatureDetector.ORB);
        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        img1 = new Mat();
        Bitmap fullBitMap=BitmapFactory.decodeFile(directoryPath+"/"+imagePath);
        Bitmap bitmap = Bitmap.createBitmap(fullBitMap, Integer.parseInt(input_offset_x),
                Integer.parseInt(input_offset_y),
                Integer.parseInt(input_width),
                Integer.parseInt(input_height));
        Utils.bitmapToMat(bitmap, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGB2GRAY);
        img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image
        descriptors1 = new Mat();
        keypoints1 = new MatOfKeyPoint();
        detector.detect(img1, keypoints1);
        descriptor.compute(img1, keypoints1, descriptors1);
        keyPoints1Arr= keypoints1.toArray();
        detector = FeatureDetector.create(FeatureDetector.ORB);
        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        String[] itemImgPaths=imgItemsPaths.split(",");
        String results="isTroopPresent=\"n\"\n#Choose $1 item x y";
        for (int i=0;i<itemImgPaths.length;i++)
        {
            if(itemImgPaths[i].trim()!="")
            {
                String location= locate(directoryPath+"/"+itemImgPaths[i].trim()+".png",itemImgPaths[i].trim());
                results=results+"\n"+location;
            }
        }
        writeUsingOutputStream(results,directoryPath+"/"+resultsFilePath);
        writeUsingOutputStream("y",directoryPath+"/intent_completed");
    }

    public String locate (String imagePath, String itemName) throws IOException {
        img2 = new Mat();
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        Utils.bitmapToMat(bitmap, img2);
        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGB2GRAY);
        img2.convertTo(img2, 0); //converting the image to match with the type of the cameras image
        descriptors2 = new Mat();
        keypoints2 = new MatOfKeyPoint();
        detector.detect(img2, keypoints2);
        descriptor.compute(img2, keypoints2, descriptors2);

        // Matching
        MatOfDMatch matches = new MatOfDMatch();
        if(descriptors2.cols()==0)
        {
            return "#no_Cols found for " + itemName;
        }

        matcher.match(descriptors1, descriptors2, matches);
        List<DMatch> matchesList = matches.toList();

        Double max_dist = 0.0;
        Double min_dist = 100.0;

        for (int i = 0; i < matchesList.size(); i++) {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if (dist > max_dist)
                max_dist = dist;
        }
        double sum_x=0;
        int count=0;
        double sum_y=0;

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            if (matchesList.get(i).distance <= (1.5 * min_dist)) {
                good_matches.addLast(matchesList.get(i));
                count++;
                sum_x=sum_x+keyPoints1Arr[matchesList.get(i).queryIdx].pt.x;
                sum_y=sum_y+keyPoints1Arr[matchesList.get(i).queryIdx].pt.y;

            }
        }
        if(count==0)
        {
            count++;
        }
        int avg_x=Integer.parseInt(input_offset_x) + (int)  sum_x/count;
        int avg_y=Integer.parseInt(input_offset_y) +  (int) sum_y/count;
        String result="Choose $1 \""+itemName+"\" "+String.valueOf(avg_x)+" "+String.valueOf(avg_y);
        return result;
    }


    private static void writeUsingOutputStream(String data, String filepath) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(new File(filepath));
            os.write(data.getBytes(), 0, data.length());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
