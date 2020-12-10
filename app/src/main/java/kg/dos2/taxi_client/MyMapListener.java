package kg.dos2.taxi_client;

import android.app.Activity;

interface MyMapListener {
    int getStep();
    Activity getAct();
    void btmTotals(boolean b, float d, int i);
    void afterLoadGeoJSON();
    void getStreet(String street);
    void getDistance(double distance);
}
