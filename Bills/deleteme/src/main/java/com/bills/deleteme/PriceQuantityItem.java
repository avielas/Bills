package com.bills.deleteme;

import android.graphics.Bitmap;

/**
 * Created by michaelvalershtein on 29/06/2017.
 */

public class PriceQuantityItem {
    public final double Price;
    public final int Quantity;
    public final Bitmap Item;

    public PriceQuantityItem(double price, int quantity, Bitmap item){
        Price = price;
        Quantity = quantity;
        Item = item;
    }
}
