package com.example.demo.preference;

public class PreferenceSummary {

    private Integer detailId;
    private int count;

    public PreferenceSummary() {
    }

    public PreferenceSummary(Integer detailId, int count) {
        this.detailId = detailId;
        this.count = count;
    }

    public Integer getDetailId() {
        return detailId;
    }

    public void setDetailId(Integer detailId) {
        this.detailId = detailId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}