package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class Detector {
    private static final String TAG = "Detector_HAHAHA";
    private static final String MODEL_FILENAME = "pbmodel.tflite";
    private static final String LABELS_FILENAME = "labels.txt";

    private static final float CONFIDENCE_THRESHOLD = 0.75f;
    private static final float NMS_IOU_THRESHOLD = 0.45f;
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 640;

    private final Interpreter interpreter;
    public final List<String> labels;

    private final int outputHeadCount;
    private final int outputInnerSize;
    private final int predsPerHead;
    private final int floatsPerPrediction = 6;

    public Detector(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(getFileFromAsset(context, MODEL_FILENAME));
        interpreter = new Interpreter(modelBuffer);
        labels = FileUtil.loadLabels(context, LABELS_FILENAME);

        int[] shape = interpreter.getOutputTensor(0).shape();
        outputHeadCount = shape[1];
        outputInnerSize = shape[2];
        if (outputInnerSize % floatsPerPrediction != 0) {
            throw new IllegalStateException("Output inner size not divisible by " + floatsPerPrediction);
        }
        predsPerHead = outputInnerSize / floatsPerPrediction;
    }

    public Result detect(Bitmap bitmap) {
        long inferenceTime = SystemClock.uptimeMillis();

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);
        ByteBuffer input = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 3 * 4)
                .order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        scaled.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        for (int p : pixels) {
            input.putFloat(((p >> 16) & 0xFF) / 255f);
            input.putFloat(((p >> 8) & 0xFF) / 255f);
            input.putFloat((p & 0xFF) / 255f);
        }
        input.rewind();

        float[][][] raw = new float[1][outputHeadCount][outputInnerSize];
        interpreter.run(input, raw);

        List<Detection> detections = new ArrayList<Detection>();
        Map<String, Integer> counts = new HashMap<>();

        for (int h = 0; h < outputHeadCount; h++) {
            float[] head = raw[0][h];
            for (int i = 0; i < predsPerHead; i++) {
                int b = i * floatsPerPrediction;
                float score = head[b + 4];
                if (score < CONFIDENCE_THRESHOLD) continue;

                int cls = Math.round(head[b + 5]);
                if (cls < 0 || cls >= labels.size()) continue;

                float cx = head[b];
                float cy = head[b + 1];
                float w = head[b + 2];
                float hi = head[b + 3];

                float x1 = cx - (w / 2f);
                float y1 = cy - (h / 2f);
                float x2 = cx + (w / 2f);
                float y2 = cy + (hi / 2f);

                if (x1 < 0 || y1 < 0 || x2 > 1 || y2 > 1) continue;

                String label = labels.get(cls);
                detections.add(new Detection(new RectF(x1, y1, x2, y2), cls, score, label));
                counts.put(label, counts.getOrDefault(label, 0) + 1);
            }
        }

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

        List<Detection> finalDetections = applyNMS(detections);

        return new Result(finalDetections, counts, inferenceTime);
    }

    private List<Detection> applyNMS(List<Detection> boxes) {
        List<Detection> sorted = new ArrayList<>(boxes);
        Collections.sort(sorted, new Comparator<Detection>() {
            public int compare(Detection a, Detection b) {
                return Float.compare(b.score, a.score);
            }
        });

        List<Detection> keep = new ArrayList<>();
        for (Detection d : sorted) {
            boolean shouldKeep = true;
            for (Detection k : keep) {
                if (iou(d.box, k.box) > NMS_IOU_THRESHOLD) {
                    shouldKeep = false;
                    break;
                }
            }
            if (shouldKeep) keep.add(d);
        }
        return keep;
    }

    private float iou(RectF a, RectF b) {
        float x1 = Math.max(a.left, b.left);
        float y1 = Math.max(a.top, b.top);
        float x2 = Math.min(a.right, b.right);
        float y2 = Math.min(a.bottom, b.bottom);
        float interArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float unionArea = a.width() * a.height() + b.width() * b.height() - interArea;
        return unionArea <= 0 ? 0 : (interArea / unionArea);
    }

    private static MappedByteBuffer loadModelFile(File modelFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(modelFile, "r");
        FileChannel channel = raf.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    }

    private static File getFileFromAsset(Context context, String assetFileName) throws IOException {
        File fileDir = context.getFilesDir();
        File file = new File(fileDir, assetFileName);
        if (!file.exists()) {
            InputStream in = context.getAssets().open(assetFileName);
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            in.close();
            out.flush();
            out.close();
        }
        return file;
    }

    public static class Detection {
        public final RectF box;
        public final int classIdx;
        public final float score;
        public final String className;

        public Detection(RectF box, int classIdx, float score, String className) {
            this.box = box;
            this.classIdx = classIdx;
            this.score = score;
            this.className = className;
        }
    }

    public static class Result {
        public final List<Detection> detections;
        public final Map<String, Integer> counts;
        public final long inferenceTime;

        public Result(List<Detection> detections, Map<String, Integer> counts, long inferenceTime) {
            this.detections = detections;
            this.counts = counts;
            this.inferenceTime = inferenceTime;
        }
    }

    public void close() {
        interpreter.close();
    }
}
