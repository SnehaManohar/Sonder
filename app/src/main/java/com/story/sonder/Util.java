package com.story.sonder;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.util.Pair;
import android.view.Window;

import com.story.sonder.model.ModelUtils;
import com.story.sonder.model.Tensor;
import com.story.sonder.model.layer.ILayer;
import com.story.sonder.model.layer.Reshape;
import com.story.sonder.model.layer.Sequence;
import com.story.sonder.model.loss.ILoss;
import com.story.sonder.model.optimizer.IOptimizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class Util {
    private static final double[] mean = new double[]{0.498420, 0.458495, 0.397866};
    private static final double[] std = new double[]{0.313047, 0.269076, 0.231632};

    static Dialog createDialog(Context context, int layoutId) {
        final Dialog dialog = new Dialog(context);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawable(new ColorDrawable(Color.rgb(30, 30, 30)));
        dialog.setContentView(layoutId);
        return dialog;
    }

    static String readFromFile(Context context, String file) throws IOException {
        FileInputStream input = context.openFileInput(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        StringBuilder stringConfig = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringConfig.append(line);
        }
        reader.close();
        return stringConfig.toString();
    }

    static void writeToFile(File file, Object object) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(object.toString());
        }
    }

    static Pair<ILayer, Pair<ILoss, IOptimizer>> createModelFromJSON(JSONObject conf) throws JSONException {
        Sequence features = (Sequence) ModelUtils.getLayer("Sequence", conf.getJSONObject("features"));
        Reshape reshape = ModelUtils.reshape(conf.getJSONArray("reshape"));
        Sequence classifier = (Sequence) ModelUtils.getLayer("Sequence", conf.getJSONObject("classifier"));
        ILayer model = new Sequence(features, reshape, classifier);

        JSONObject lossObject = conf.getJSONObject("loss");
        ILoss loss = ModelUtils.getLoss(lossObject.getString("name"));

        JSONObject optimizerObject = conf.getJSONObject("optimizer");
        IOptimizer optimizer = ModelUtils.getOptimizer(
                optimizerObject.getString("name"),
                model,
                optimizerObject.getJSONObject("parameters"));
        return Pair.create(model, Pair.create(loss, optimizer));
    }

    static Pair<ILayer, Pair<ILoss, IOptimizer>> createModelFromJSONString(String json) throws JSONException {
        return createModelFromJSON(new JSONObject(json));
    }

    static Tensor bitmapToTensor(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w * (long) height > width * (long) h) {
            w = w * height / h;
            h = height;
        } else {
            h = h * width / w;
            w = width;
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        int x = w - width >> 1;
        int y = h - height >> 1;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, x, y, width, height);
        double[] image = new double[pixels.length * 3];
        int greenShift = pixels.length;
        int blueShift = 2 * greenShift;
        for (int i = -1; ++i < pixels.length; ) {
            image[i] = (Color.red(pixels[i]) / 255. - mean[0]) / std[0];
            image[i + greenShift] = (Color.green(pixels[i]) / 255. - mean[1]) / std[1];
            image[i + blueShift] = (Color.blue(pixels[i]) / 255. - mean[2]) / std[2];
        }
        return new Tensor(image, 3, width, height);
    }

    static Tensor bitmapToTensor(Bitmap bitmap) {
        return bitmapToTensor(bitmap, Constants.inputWidth, Constants.inputHeight);
    }

    static int[] topKIndices(Tensor probabilities, int k) {
        List<Pair<Double, Integer>> list = new ArrayList<>();
        probabilities.forEach((i, v) -> {
            list.add(Pair.create(v, i));
            return 0;
        });
        Collections.sort(list, (o1, o2) -> o2.first.compareTo(o1.first));
        int[] index = new int[k];
        for (int i = -1; ++i < k; )
            index[i] = list.get(i).second;
        return index;
    }

    static int[] topKIndices(Tensor probabilities) {
        return topKIndices(probabilities, Constants.topK);
    }

    static Pair<String[], Object> getTagCategories(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        Pair<Tensor, Object> modelForward = Constants.model.first.forward(Util.bitmapToTensor(bitmap));
        int[] indices = Util.topKIndices(modelForward.first);

        String[] tags = new String[indices.length + 1];
        for (int i = -1; ++i < indices.length; ) {
            tags[i] = Constants.categories[indices[i]];
        }
        tags[indices.length] = "None";
        return Pair.create(tags, modelForward.second);
    }
}
