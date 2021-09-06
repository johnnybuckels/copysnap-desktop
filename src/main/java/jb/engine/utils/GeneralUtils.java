package jb.engine.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class GeneralUtils {

    /**
     * @return now formatted as yyyy-MM-dd-HH-mm-ss
     */
    public static String getNowAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return sdf.format(new Date());
    }

    /**
     * @return now formatted as yyyy-MM-dd-HH-mm-ss-SSSS
     */
    public static String getNowAsId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS");
        return sdf.format(new Date());

    }

}
