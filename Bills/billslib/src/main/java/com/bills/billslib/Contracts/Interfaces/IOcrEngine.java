package com.bills.billslib.Contracts;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;

/**
 * Created by michaelvalershtein on 02/04/2017.
 */

public interface IOcrEngine {

    /**
     * Initalizes the OCR Engine
     * @param datapath Path to data needed for initialization
     * @param language OCR language
     * @throws IllegalArgumentException In case one or more arguments are invalid
     */
    void Init(String datapath, Language language) throws IllegalArgumentException, RuntimeException;

    /**
     * Sets the image for OCR Engine to work on.
     * @param bmp The image
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws IllegalArgumentException In case argument is invalid
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    void SetImage(Bitmap bmp) throws IllegalStateException, IllegalArgumentException, RuntimeException;

    /**
     * Gets text lines recognizable by the OCR Engine
     * @return List of rectangles which contain all available lines in current image.
     * @throws IllegalStateException In case the OCR Engine was not initialized
     */
    List<Rect> GetTextlines() throws IllegalStateException, RuntimeException;

    /**
     * Gets text words recognizable by the OCR Engine
     * @return List of rectangles which contain all available words in current image.
     * @throws IllegalStateException In case the OCR Engine was not initialized
     */
    List<Rect> GetWords() throws IllegalStateException;

    /**
     * Gets parsed text from current image
     * @return Parsed text
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    public String getUTF8Text() throws IllegalStateException, RuntimeException;

    /**
     * Sets page segmentation mode
     * @param pageSegMode Page segmentation mode to set
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    public void setPageSegMode(PageSegmentation pageSegMode) throws IllegalStateException, RuntimeException;

    /**
     * Returns flag whether the OCR Engine was initialized or not
     * @return Flag whether the OCR Engine was initialized or not
     */
    boolean Initialized();

    /**
     *
     * @return Mean confidence of last parsing
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    int MeanConfidence() throws IllegalStateException, RuntimeException;

    /**
     * Closes down the OCR Engine and free up all memory. End() is equivalent to
     * destructing and reconstructing the OCR Engine.
     * <p>
     * Once End() has been used, none of the other API functions may be used
     * other than Init and anything declared above it in the class definition.
     * @throws RuntimeException
     */
    void End() throws RuntimeException;

    /**
     * Sets characters whitelist to numbers only
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    void SetNumbersOnlyFormat() throws IllegalStateException, RuntimeException;

    /**
     * Sets characters whitelist to Letters only
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    void SetTextOnlyFormat() throws IllegalStateException, RuntimeException;

    /**
     * Sets characters whitelist to all chracters
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    void SetAllCharactersWhitelist() throws IllegalStateException, RuntimeException;

    /**
     * Restricts recognition to a sub-rectangle of the image. Call after
     * SetImage. Each SetRectangle clears the recognition results so multiple
     * rectangles can be recognized with the same image.
     *
     * @param rect the bounding rectangle
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    void SetRectangle(Rect rect) throws IllegalStateException, RuntimeException;

    /**
     * Get a copy of the internal thresholded image from OCR Engine.
     * @return Bitmap containing the thresholded image
     * @throws IllegalStateException In case the OCR Engine was not initialized
     * @throws RuntimeException In case of runtime(probably native) exception accured
     */
    Bitmap GetThresholdedImage() throws IllegalStateException, RuntimeException;
}
