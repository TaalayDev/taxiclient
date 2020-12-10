package kg.dos2.taxi_client;

import android.os.Bundle;

public interface FragmentsListener {
    void curFrag(int f);
    void cancOrd();
    void complOrd(int w);
    void newOrder(Bundle bundle);
    void drvSearch();
    void ordInfo();
    void drvInfo(int drv);
    void footerDetailsShowOrHide(boolean show);
    void ordFromFav(int id);
    void openDriverChat();
    void dialogDismiss();
}
