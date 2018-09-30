/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.bills.billslib.Core;

import android.app.Activity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This classifier works with the Inception-v3 slim model.
 * It applies floating point inference rather than using a quantized model.
 */
public class ImageClassifierFloatHED extends ImageClassifier {

  /**
   * The inception net requires additional normalization of the used input.
   */
  private static final float IMAGE_MEAN_B = 103.939f;
  private static final float IMAGE_MEAN_G = 116.779f;
  private static final float IMAGE_MEAN_R = 123.68f;
  private static final float IMAGE_STD_B = 103.939f;
  private static final float IMAGE_STD_G = 116.779f;
  private static final float IMAGE_STD_R = 123.68f;

  Map<Integer, Object> outputs = new HashMap<>();

  /**
   * An array to hold inference results, to be feed into Tensorflow Lite as outputs.
   * This isn't part of the super class, because we need a primitive array here.
   */
  private float[][][][] labelProbArray = null;
  private float[][][][] labelProbArray1 = null;
  private float[][][][] labelProbArray2 = null;
  private float[][][][] labelProbArray3 = null;
  private float[][][][] labelProbArray4 = null;

  /**
   * Initializes an {@code ImageClassifier}.
   *
   * @param activity
   */
  public ImageClassifierFloatHED(Activity activity) throws IOException {
    super(activity);
    labelProbArray = new float[1][480][480][1];
    labelProbArray1 = new float[1][480][480][1];
    labelProbArray2 = new float[1][480][480][1];
    labelProbArray3 = new float[1][480][480][1];
    labelProbArray4 = new float[1][480][480][1];
  }

  @Override
  protected String getModelPath() {
    // you can download this file from
    // https://storage.googleapis.com/download.tensorflow.org/models/tflite/inception_v3_slim_2016_android_2017_11_10.zip
    return "hed-model-9900.tflite";
  }

  @Override
  protected String getLabelPath() {
    return "dog_cat.txt";
  }

  @Override
  protected int getImageSizeX() {
    return 480;
  }

  @Override
  protected int getImageSizeY() {
    return 480;
  }

  @Override
  protected int getNumBytesPerChannel() {
    // a 32bit float value requires 4 bytes
    return 4;
  }

  @Override
  protected void addPixelValue(int pixelValue) {
    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN_B) / IMAGE_STD_B);
    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN_G) / IMAGE_STD_G);
    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN_R) / IMAGE_STD_R);
  }

  @Override
  protected float getProbability(int labelIndex) {
    return labelProbArray[0][0][0][0];
  }

  @Override
  protected void setProbability(int labelIndex, Number value) {
    //labelProbArray[0][0][0][0] = value.floatValue();
  }

  @Override
  protected float getNormalizedProbability(int labelIndex) {
    // TODO the following value isn't in [0,1] yet, but may be greater. Why?
    return getProbability(labelIndex);
  }

  @Override
  protected void runInference() {
//    tflite.run(imgData, labelProbArray);

    outputs.put(0, labelProbArray);
    outputs.put(1, labelProbArray1);
    outputs.put(2, labelProbArray2);
    outputs.put(3, labelProbArray3);
    outputs.put(4, labelProbArray4);

    tflite.runForMultipleInputsOutputs(new Object[]{imgData}, outputs);
  }

  @Override
  public float[][][][] getArray(int i) {
    return (float[][][][]) outputs.get(i);
  }
}
