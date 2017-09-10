package com.bills.billslib.Core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
import com.bills.billslib.Utilities.FilesHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;

/**
 * Created by mvalersh on 12/2/2016.
 */


public class TemplateMatcher  {
    private IOcrEngine mOCREngine;
    private int itemColumn;
    public final ArrayList<Double[]> priceAndQuantity = new ArrayList<>();
    public ArrayList<Rect> itemLocationsRect = new ArrayList<>();
    public ArrayList<Bitmap> itemLocationsByteArray = new ArrayList<>();
    public Bitmap mFullBillProcessedImage;
//    Boolean secondColumnIsConnected;
//    Boolean oneBeforeLastColumnConnected;

    /******* Global vars for parsing ********/
    public ArrayList<ArrayList<Rect>> locationsItemsArea;
    public LinkedHashMap<Rect, Rect>[] connectionsItemsArea;
    /****************************************/

    /**
     * @param ocrEngine     initialized ocr engine
     * @param fullBillPreprocessedImage full, processed and warped bill image
     */
    public TemplateMatcher(IOcrEngine ocrEngine, Bitmap fullBillPreprocessedImage) {
        if (!ocrEngine.Initialized()) {
            throw new IllegalArgumentException("OCREngine must be initialized.");
        }
        mOCREngine = ocrEngine;
        mFullBillProcessedImage = fullBillPreprocessedImage;
//        secondColumnIsConnected = false;
//        oneBeforeLastColumnConnected = false;
    }

    public void InitializeBeforeSecondUse(Bitmap fullBillPreprocessedImage){
        if (!mOCREngine.Initialized()) {
            throw new IllegalArgumentException("OCREngine must be initialized.");
        }
        mFullBillProcessedImage = fullBillPreprocessedImage;
//        secondColumnIsConnected = false;
//        oneBeforeLastColumnConnected = false;
        priceAndQuantity.clear();
    }

    public void Match() throws Exception {
        boolean success;
        ArrayList<ArrayList<Rect>> locations = GetWordLocations(mFullBillProcessedImage);
        int lineIndex = 0;
        //print all word locations to Log
        for (ArrayList<Rect> line : locations) {
            String str = "";

            for (Rect word : line) {
                if(word == null){
                    continue;
                }
                str += word.right + "--> ";
            }
            Log.d(this.getClass().getSimpleName(), "Line " + lineIndex++ + ": " + str);
        }

        LinkedHashMap<Rect, Rect>[] connections = new LinkedHashMap[locations.size() - 1];

        SetConnections(locations, connections);

        List<Map.Entry<Integer, Integer>> startEndOfAreasList = new ArrayList<>();
        //find largest "connected" area. Two lines are connected if there are at least two words in "similar" location which are connected
        IdentifyOptionalConnectedAreas(locations, connections, startEndOfAreasList);

        int maxSizeIndex = Integer.MIN_VALUE;
        for(int i = 0, maxSize = Integer.MIN_VALUE; i < startEndOfAreasList.size(); i++){
            if(maxSize < Math.abs(startEndOfAreasList.get(i).getValue() - startEndOfAreasList.get(i).getKey())){
                maxSize = Math.abs(startEndOfAreasList.get(i).getValue() - startEndOfAreasList.get(i).getKey());
                maxSizeIndex = i;
            }
        }

        int itemsAreaStart = startEndOfAreasList.get(maxSizeIndex).getKey();
        int itemsAreaEnd = startEndOfAreasList.get(maxSizeIndex).getValue();

        try {
            GetPriceAndQuantity(itemsAreaStart, itemsAreaEnd, connections, locations, false);
            SetItemsLocations(itemsAreaStart, itemsAreaEnd, connections, locations);
            success = true;
            CreatingRects(startEndOfAreasList, connections, locations);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void Parsing(int numOfItems) {
        ArrayList<ArrayList<Rect>> locations = locationsItemsArea;

        LinkedHashMap<Rect, Rect>[] connections = connectionsItemsArea;

        int itemsAreaStart = 0;
        int itemsAreaEnd = numOfItems - 1;

        try {
            GetPriceAndQuantity(itemsAreaStart, itemsAreaEnd, connections, locations, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SetConnections(ArrayList<ArrayList<Rect>> locations, LinkedHashMap<Rect, Rect>[] connections) {
        for (int i = 0; i < locations.size()-1; i++){
            connections[i] = new LinkedHashMap<>();
            for(int j = 0; j < locations.get(i).size(); j++){
                Rect word = locations.get(i).get(j);
                for(int k = 0; k < locations.get(i+1).size(); k++){  //compare current line word to all next line words until match
                    Rect nextLineWord = locations.get(i+1).get(k);
                    if(InRange(word, nextLineWord)){
                        connections[i].put(word, nextLineWord);
                        break;
                    }
                }
            }
        }
    }

    private void SetItemsLocations(int itemsAreaStart, int itemsAreaEnd, LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations) {
        Rect[][] lineConnectionRects = new Rect[itemsAreaEnd - itemsAreaStart + 1][connections[itemsAreaStart].size()];
        for(int i = itemsAreaStart; i < itemsAreaEnd; i++){
            try {
                if(i == itemsAreaEnd){
                    //in case of last items area lines, we making a special calculation
//                    connection = (Rect) (connections[i-1].values().toArray()[j]);
                    connections[i-1].values().toArray(lineConnectionRects[i - itemsAreaStart -1]);
                }
                else {
                    connections[i].keySet().toArray(lineConnectionRects[i - itemsAreaStart]);
                }
            }catch (Exception ex){
                //TODO: handle exception
            }
        }

        try{
            int i = 0;
            for(Map.Entry<Rect, Rect> entry : connections[itemsAreaEnd - 1].entrySet()){
                lineConnectionRects[itemsAreaEnd - itemsAreaStart][i++] = entry.getValue();
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        for (int i = 0; i < lineConnectionRects.length; i++){
            int prevConnectedRect = 0;
            for(int j = 0; j < locations.get(i + itemsAreaStart).size(); j++){
                if(lineConnectionRects[i][itemColumn] == locations.get(i + itemsAreaStart).get(j)){
                    Rect itemLocation = new Rect(
                            locations.get(i + itemsAreaStart).get(prevConnectedRect + 1).left,
                            locations.get(i + itemsAreaStart).get(prevConnectedRect + 1).top,
                            locations.get(i + itemsAreaStart).get(j).right,
                            locations.get(i + itemsAreaStart).get(prevConnectedRect + 1).bottom);
                    itemLocationsRect.add(itemLocation);

                    /** the following code save product name as ByteArray for later serialize to BillSummarizer **/
//                    mOCREngine.SetRectangle(itemLocation);
//                    Rect itemsRect = null;
//                    try {
//                        //TODO crashing here sometimes
//                        List<Rect> textLines =  mOCREngine.GetTextlines();
//                        itemsRect = textLines.get(0);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        continue;
//                    }
                    int xBegin = itemLocation.left;
                    int xEnd = itemLocation.right;
                    int yBegin = itemLocation.top;
                    int yEnd = itemLocation.bottom;
                    Bitmap bitmap = Bitmap.createBitmap(mFullBillProcessedImage, xBegin, yBegin, xEnd-xBegin, yEnd-yBegin);
//                    FilesHandler.SaveToJPGFile(bitmap, Constants.IMAGES_PATH + "/" + i + "_" + j +".jpg");
                    itemLocationsByteArray.add(bitmap);
                    /****** end ******/
                    break;
                }
                //TODO I added 'i + itemsAreaStart == connections.length ||'
                //TODO just due to parsing items area bug (second call of TM). It should be refactored ASAP !!!
                if(i + itemsAreaStart == connections.length ||
                        connections[i + itemsAreaStart].containsKey(locations.get(i + itemsAreaStart).get(j))){
                    prevConnectedRect = j;
                }
                else
                if(connections[i + itemsAreaStart - 1].containsValue(locations.get(i + itemsAreaStart).get(j))){
                    prevConnectedRect = j;
                }
            }
        }
    }

    private void IdentifyOptionalConnectedAreas(ArrayList<ArrayList<Rect>> locations, LinkedHashMap<Rect, Rect>[] connections, List<Map.Entry<Integer, Integer>> startEndOfAreasList) {
        LinkedHashMap<Rect, Rect> connection = new LinkedHashMap();
        int connctedRects = 0;
        int start = -1;
        for(int i = 0; i < connections.length - 1; i++){
            if(connections[i].size() >= 3){
                if(start == -1 ){
                    start = i;
                    connection.putAll(connections[i]);
                }
                else{
                    for (Map.Entry<Rect, Rect> rectConnection : connection.entrySet()) {
                        if(connections[i].keySet().contains(rectConnection.getValue()) ){
                            connctedRects++;
                        }
                    }
                    connection.clear();
                    if(connctedRects >= 3)
                    {
                        connection.putAll(connections[i]);

                    }
                    else{
//                        start = ValidatingTitleLineLocation(start, connections, locations);
                        //TODO - assuming title always in item area
                        start = start +1;
                        startEndOfAreasList.add(new AbstractMap.SimpleEntry<>(start, i));
                        Log.d(this.getClass().getSimpleName(), "Found area: " + start + "-->" + i);
                        start = -1;
//                        secondColumnIsConnected = false;
//                        oneBeforeLastColumnConnected = false;
                    }
                    connctedRects = 0;
                }
            }
            else{
                if(start >= 0){
//                    start = ValidatingTitleLineLocation(start, connections, locations);
                    //TODO - assuming title always in item area
                    start = start +1;
                    startEndOfAreasList.add(new AbstractMap.SimpleEntry<>(start, i));
                    Log.d(this.getClass().getSimpleName(), "Found area: " + start + "-->" + i);
                    start = -1;
//                    secondColumnIsConnected = false;
//                    oneBeforeLastColumnConnected = false;
                }
            }
        }
//        for(int i = 0; i < locations.size() - 1; i++){
//            if(IsLinesConnected(locations.get(i), locations.get(i+1), connections[i])){
//                if(start == -1 ){
//                    start = i;
//                }
//            }
//            else{
//                if(start >= 0){
//                    start = ValidatingTitleLineLocation(start, connections, locations);
//                    /**** start+1 because title always included at items area ****/
//                    startEndOfAreasList.add(new AbstractMap.SimpleEntry<>(start, i));
//                    Log.d(this.getClass().getSimpleName(), "Found area: " + start + "-->" + i);
//                    start = -1;
//                    secondColumnIsConnected = false;
//                    oneBeforeLastColumnConnected = false;
//                }
//            }
//        }
    }
//
//    Boolean IsLinesConnected(ArrayList<Rect> line, ArrayList<Rect> nextLine, LinkedHashMap<Rect, Rect> connections) {
//        Boolean isFirstConnectedCofigurationExist;
//        Boolean isSecondConnectedCofigurationExist;
//
//        if(!secondColumnIsConnected && !oneBeforeLastColumnConnected) {
//            secondColumnIsConnected = line.size() >= 3 &&
//                    connections.get(line.get(0)) == nextLine.get(0) &&
//                    connections.get(line.get(line.size() - 1)) == nextLine.get(nextLine.size() - 1) &&
//                    connections.get(line.get(1)) == nextLine.get(1);
//            oneBeforeLastColumnConnected = line.size() >= 3 &&
//                    connections.get(line.get(0)) == nextLine.get(0) &&
//                    connections.get(line.get(line.size() - 1)) == nextLine.get(nextLine.size() - 1) &&
//                    connections.get(line.get(line.size() - 2)) == nextLine.get(nextLine.size() - 2);
//
//            if(secondColumnIsConnected && oneBeforeLastColumnConnected)
//            {
//                oneBeforeLastColumnConnected = false;
//            }
//
//            return (secondColumnIsConnected && !oneBeforeLastColumnConnected) ||
//                    (!secondColumnIsConnected && oneBeforeLastColumnConnected);
//        }
//        else
//        {
//            isFirstConnectedCofigurationExist = line.size() >= 3 &&
//                    connections.get(line.get(0)) == nextLine.get(0) &&
//                    connections.get(line.get(line.size() - 1)) == nextLine.get(nextLine.size() - 1) &&
//                    connections.get(line.get(1)) == nextLine.get(1);
//            isSecondConnectedCofigurationExist = line.size() >= 3 &&
//                    connections.get(line.get(0)) == nextLine.get(0) &&
//                    connections.get(line.get(line.size() - 1)) == nextLine.get(nextLine.size() - 1) &&
//                    connections.get(line.get(line.size() - 2)) == nextLine.get(nextLine.size() - 2);
//            return (isFirstConnectedCofigurationExist && secondColumnIsConnected) ||
//                    (isSecondConnectedCofigurationExist && oneBeforeLastColumnConnected);
//        }
//    }
//
//
//    private int ValidatingTitleLineLocation(int start, LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations) {
//        Boolean isTitleAtItemsArea;
//
//        isTitleAtItemsArea = IsTitleAtItemsArea(start, connections, locations);
//
//        return isTitleAtItemsArea ? start + 1 : start;
//    }
//
//    private Boolean IsTitleAtItemsArea(int start, LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations) {
//        if(start - 1 < 0)
//        {
//            return true;
//        }
//
//        int numberOfConnections = connections[start].size();
//        if(3 <= numberOfConnections || 7 <= numberOfConnections)
//        {
//            if(IsTitleConnected(locations.get(start), connections[start-1]))
//            {
//                return false;
//            }
//            return true;
//        }
//        return true;
//    }
//
//    private Boolean IsTitleConnected(ArrayList<Rect> firstLineItemsArea, LinkedHashMap<Rect, Rect> titleConnections) {
//
//        if(firstLineItemsArea.size() < 3 || titleConnections.size() < 3)
//        {
//            return false;
//        }
//
//        Boolean isFirstColumnConnected = IsConnected(titleConnections, firstLineItemsArea.get(0));
//        Boolean isLastColumnConnected = IsConnected(titleConnections, firstLineItemsArea.get(firstLineItemsArea.size() - 1));
//        Boolean isSecondOrOneBeforeLastColumnConnected = false;
//
//        if(secondColumnIsConnected)
//        {
//            isSecondOrOneBeforeLastColumnConnected = IsConnected(titleConnections, firstLineItemsArea.get(1));
//        }
//        else if(oneBeforeLastColumnConnected)
//        {
//            isSecondOrOneBeforeLastColumnConnected = IsConnected(titleConnections, firstLineItemsArea.get(firstLineItemsArea.size() - 2));
//        }
//
//        return isFirstColumnConnected && isLastColumnConnected && isSecondOrOneBeforeLastColumnConnected;
//    }

    private Boolean IsConnected(LinkedHashMap<Rect, Rect> connections, Rect rect) {
        for (Map.Entry<Rect, Rect> connection : connections.entrySet()) {
            if(connection.getValue() == rect)
            {
                return true;
            }
        }
        return false;
    }

    private void GetPriceAndQuantity(int itemsAreaStart, int itemsAreaEnd,
                                     LinkedHashMap<Rect, Rect>[] connections,
                                     ArrayList<ArrayList<Rect>> locations, Boolean isParsing) throws Exception {
        mOCREngine.SetImage(mFullBillProcessedImage);

        mOCREngine.SetNumbersOnlyFormat();

        double[][] parsedNumbersArray = new double[itemsAreaEnd - itemsAreaStart + 1][connections[itemsAreaStart].size()];

        GetParsedNumbers(itemsAreaStart, itemsAreaEnd, connections, parsedNumbersArray, locations, isParsing);

        int[] parsedNumbersArraySizes = new int[connections[itemsAreaStart].size()];

        GetParsedNumbersSizes(parsedNumbersArray, parsedNumbersArraySizes);

        int[] sortedParsedNumbersArraySizes = new int[parsedNumbersArraySizes.length];
        System.arraycopy(parsedNumbersArraySizes, 0, sortedParsedNumbersArraySizes, 0, parsedNumbersArraySizes.length);
        Arrays.sort(sortedParsedNumbersArraySizes);


        int priceColumn = Integer.MIN_VALUE;
        int quantityColumn = Integer.MIN_VALUE;

        //find quantity column
        for(int i = 0; i < parsedNumbersArraySizes.length; i++){
            if(parsedNumbersArraySizes[i] == sortedParsedNumbersArraySizes[1]){
                quantityColumn = i;
                break;
            }
        }

        //find price column
        for(int i = 0; i < parsedNumbersArraySizes.length; i++){
            if(parsedNumbersArraySizes[i] == sortedParsedNumbersArraySizes[2]){
                priceColumn = i;
                break;
            }
        }

        for(int i = 0; i < parsedNumbersArray.length; i++){
            priceAndQuantity.add(i, new Double[]{parsedNumbersArray[i][priceColumn], parsedNumbersArray[i][quantityColumn]});
        }
    }

    private void GetParsedNumbersSizes(double[][] parsedNumbersArray, int[] parsedNumbersArraySizes) {
        for(int i = 0; i < parsedNumbersArray.length; i++){
            double[] sortedLine = new double[parsedNumbersArray[0].length];
            System.arraycopy(parsedNumbersArray[i], 0, sortedLine, 0, sortedLine.length);
            Arrays.sort(sortedLine);

            for(int j = 0; j < parsedNumbersArray[0].length; j++){
                for(int k = 0; k < sortedLine.length; k++){
                    if(sortedLine[k] == parsedNumbersArray[i][j]){
                        parsedNumbersArraySizes[j] += k;
                    }
                }
            }
        }
    }

    private void GetParsedNumbers(int itemsAreaStart, int itemsAreaEnd,
                                  LinkedHashMap<Rect, Rect>[] connections,
                                  double[][] parsedNumbersArray,
                                  ArrayList<ArrayList<Rect>> locations, Boolean isParsing) throws Exception {
        int i;
        itemColumn = CalculateIndexOfItemsColumn(itemsAreaStart, itemsAreaEnd, connections, locations);
        for(i = itemsAreaStart; i < itemsAreaEnd; i++) {
            int j = -1;
            for (Rect entry : connections[i].keySet()) {
                j++;
                if(j == itemColumn){
                    parsedNumbersArray[i - itemsAreaStart][j] = -1;
                    continue;
                }
                Double parsedNumber = 0.0;
                if(isParsing) {
                    entry.left -= 3;
                    entry.right += 3;
                    entry.top -= 3;
                    entry.bottom += 3;
                }
                mOCREngine.SetRectangle(entry);
                try{
                    String parsedNumberString = mOCREngine.GetUTF8Text();
                    parsedNumberString = CleaningParsedNumber(parsedNumberString);
                    parsedNumber = Double.parseDouble(parsedNumberString);
                }
                catch(Exception ex){
                    parsedNumbersArray[i - itemsAreaStart][j] = -2;
                    continue;
                }
                parsedNumbersArray[i - itemsAreaStart][j] = parsedNumber;
            }
        }

        int j = -1;
        for(Map.Entry<Rect, Rect> entry : connections[itemsAreaEnd - 1].entrySet()) {
            j++;
            if(j == itemColumn){
                parsedNumbersArray[i - itemsAreaStart][j] = -1;
                continue;
            }
            Double parsedNumber = 0.0;
            if(isParsing) {
                entry.setValue(new Rect(entry.getValue().left   - Constants.ENLARGE_RECT_VALUE,
                                        entry.getValue().top    - Constants.ENLARGE_RECT_VALUE,
                                        entry.getValue().right  + Constants.ENLARGE_RECT_VALUE,
                                        entry.getValue().bottom + Constants.ENLARGE_RECT_VALUE));
                /**** the following code is for debugging  ****/
//                int xBegin   = entry.getValue().left;
//                int xEnd  = entry.getValue().right;
//                int yBegin    = entry.getValue().top ;
//                int yEnd = entry.getValue().bottom;
//                Bitmap bitmap = Bitmap.createBitmap(mFullBillProcessedImage, xBegin, yBegin, xEnd-xBegin, yEnd-yBegin);
//                FilesHandler.SaveToJPGFile(bitmap, Constants.IMAGES_PATH + "/rect_test_" + i + "_" + j + ".jpg");
//                bitmap.recycle();
                /**********************************************/
            }
            mOCREngine.SetRectangle(entry.getValue());
            try {
                String parsedNumberString = mOCREngine.GetUTF8Text();
                parsedNumberString = CleaningParsedNumber(parsedNumberString);
                parsedNumber = Double.parseDouble(parsedNumberString);
            } catch (Exception ex) {
                parsedNumbersArray[i - itemsAreaStart][j] = -1;
                continue;
            }
            parsedNumbersArray[i - itemsAreaStart][j] = parsedNumber;
        }
    }

    private String CleaningParsedNumber(String parsedNumberString) {
        parsedNumberString = parsedNumberString.replace(" ", "");
        while (parsedNumberString.startsWith("."))
        {
            parsedNumberString = parsedNumberString.substring(1);
        }

        while (parsedNumberString.endsWith("."))
        {
            parsedNumberString = parsedNumberString.substring(0,parsedNumberString.length()-2);
        }
        return  parsedNumberString;
    }

    private boolean InRange(Rect word, Rect nextLineWord) {
        double range = 0.4*Math.abs(word.bottom - word.top);
        boolean isInRange = ((word.right >= nextLineWord.right && word.right - range <= nextLineWord.right) ||
                (word.right <= nextLineWord.right && word.right + range >= nextLineWord.right));
        //boolean isHeightSame = Math.abs(Math.abs(word.bottom - word.top) - Math.abs(nextLineWord.bottom - nextLineWord.top)) <= range;
        return isInRange;// && isHeightSame;
    }

    private ArrayList<ArrayList<Rect>> GetWordLocations(Bitmap processedBillImage) {
        List<Rect> textlines = null;
        Rect textLine = null;
        List<Rect> textWords = null;
        ArrayList<ArrayList<Rect>> locations = new ArrayList<>();
        int lineCount = 0;
        int validLineCount = 0;
        int wordCount = 0;
        
        mOCREngine.SetImage(processedBillImage);
        mOCREngine.SetNumbersOnlyFormat();
        textlines = mOCREngine.GetTextlines();
        // go over each line and find all numbers with their locations.
        while (lineCount < textlines.size()) {
            textLine = textlines.get(lineCount++);
            mOCREngine.SetRectangle(textLine);
            try {
                textWords = mOCREngine.GetWords();
            } catch (Exception ex) {
                Log.d(this.getClass().getSimpleName(), "Failed to get words of line. Error: " + ex.getMessage());
                continue;
            }
            locations.add(new ArrayList<Rect>());
            validLineCount++;
            while (wordCount < textWords.size()) {
                Rect wordRect = textWords.get(wordCount++);
                locations.get(validLineCount - 1).add(wordRect);
            }
            wordCount = 0;
        }

        return locations;
    }

    private int CalculateIndexOfItemsColumn(int itemsAreaStart, int itemsAreaEnd, LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations) {
        int i;
        int[] sumPerColumns = new int[connections[itemsAreaStart].size()];
        for(i = itemsAreaStart; i <= itemsAreaEnd; i++) {
            int start=0,current=0;
            int[] calculatePerColumns = new int[connections[itemsAreaStart].size()];
            for (int j=0; j<connections[itemsAreaStart].size(); j++) {
                Rect connection;
                if(i == itemsAreaEnd)
                {
                    //in case of last items area lines, we making a special calculation
                    connection = (Rect) (connections[i-1].values().toArray()[j]);
                }
                else
                {
                    // ??????
                    //TODO - to remove???
                    if(j >= connections[i].keySet().size())
                        continue;
                    connection = (Rect) (connections[i].keySet().toArray()[j]);
                }

                while(true) {
                    ArrayList<Rect> locationsLine = locations.get(i);
                    if(locationsLine.get(current) == connection)
                    {
                        calculatePerColumns[j] = locationsLine.get(current).right - locationsLine.get(start).left;
//                        Log.d(this.getClass().getSimpleName(), "right: "+locationsLine.get(current).right+", left: "+locationsLine.get(start).left);
                        current++;
                        start = current;
                        break;
                    }
                    else
                    {
                        current++;
                    }
                }
            }
            UpdateSumPerColumn(sumPerColumns, calculatePerColumns);
        }
        return FindMaxValueIndex(sumPerColumns);
    }

    private int FindMaxValueIndex(int[] array){
        int maxIndex = 0;
        for (int i = 0; i < array.length; i++) {
            int currNumber = array[i];
            if ((currNumber > array[maxIndex])) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private void UpdateSumPerColumn(int[] sumPerColumns, int[] calculatePerColumns) {
        int[] sortedCalculatePerColumns = calculatePerColumns.clone();
        Arrays.sort(sortedCalculatePerColumns);
        for (int j=0; j < sumPerColumns.length; j++)
        {
            for(int i=0; i < sortedCalculatePerColumns.length ; i++)
            {
                if(calculatePerColumns[j] == sortedCalculatePerColumns[i])
                {
                    if(i == (sortedCalculatePerColumns.length - 1)) {
                        sumPerColumns[j] += i + 1;
                    }else {
                        sumPerColumns[j] += i;
                    }
                }
            }
        }
    }

    private void CreatingRects(List<Map.Entry<Integer, Integer>> startEndOfAreasList, LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations) {
        Log.d(this.getClass().getSimpleName(), "");
        int max = 0, beginIndex = 0, endIndex = 0;
        for (Map.Entry<Integer, Integer> entry : startEndOfAreasList) {
            int startLine = entry.getKey();
            int endLine = entry.getValue();
            int numberOfLines = endLine - startLine;
            if(numberOfLines > max)
            {
                max = numberOfLines;
                beginIndex = startLine;
                endIndex = endLine;
            }
        }
        CreateRects(connections, locations, beginIndex, endIndex);
    }

    private void CreateRects(LinkedHashMap<Rect, Rect>[] connections, ArrayList<ArrayList<Rect>> locations, int beginIndex, int endIndex) {
        ArrayList<Rect> keyListCurrentIndex = null;
        connectionsItemsArea = new LinkedHashMap[endIndex + 1 - beginIndex];
        locationsItemsArea = new ArrayList<>();

        for (int i = beginIndex; i < endIndex + 1; i++)
        {

            if(i == endIndex)
            {
                keyListCurrentIndex = new ArrayList<>(connections[i-1].values());
                connectionsItemsArea[i-beginIndex] = new LinkedHashMap<>();
                connectionsItemsArea[i-beginIndex] = (LinkedHashMap<Rect, Rect>) connections[i];
                locationsItemsArea.add(locations.get(i));
            }
//            else if(i == beginIndex)
            else
            {
                keyListCurrentIndex = new ArrayList<>(connections[i].keySet());
                connectionsItemsArea[i-beginIndex] = new LinkedHashMap<>();
                connectionsItemsArea[i-beginIndex] = (LinkedHashMap<Rect, Rect>) connections[i];
                locationsItemsArea.add(locations.get(i));
            }

        }
    }
}
