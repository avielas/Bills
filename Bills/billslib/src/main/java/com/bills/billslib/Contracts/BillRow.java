package com.bills.billslib.Contracts;

import android.graphics.Bitmap;

/**
 * Created by michaelvalershtein on 01/08/2017.
 */

public class BillRow {
    private double mPrice;
    private int mQuantity;
    private int mRowIndex;
    private Bitmap mItem;

    public BillRow(double price, int quantity, int rowIndex, Bitmap item){
        mPrice = price;
        mQuantity = quantity;
        mRowIndex = rowIndex;
        mItem = item;
    }

    public double GetPrice(){return mPrice;}

    public int GetQuantity(){return mQuantity;}

    public int GetRowIndex(){return mRowIndex;}

    public Bitmap GetItem(){return mItem;}
}
