package org.fog.utils;

public class FractionalSelectivity {
    private double selectivity;

    public FractionalSelectivity(double selectivity) {
        this.selectivity = selectivity;
    }

    public double getSelectivity() {
        return selectivity;
    }

    public void setSelectivity(double selectivity) {
        this.selectivity = selectivity;
    }
}