package com.bills.deleteme;

import android.graphics.Bitmap;

/**
 * Created by michaelvalershtein on 24/06/2017.
 */

public class Tuple {
    public Double Price;
    public Double Quantity;
    public String Item;

    public Tuple(){}

    public Tuple(Double price, Double quantity, String item){
        Price = price;
        Quantity = quantity;
        Item = item;
    }
}
