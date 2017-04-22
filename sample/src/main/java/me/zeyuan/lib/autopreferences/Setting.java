package me.zeyuan.lib.autopreferences;

import me.zeyuan.lib.autopreferences.annotations.Ignore;
import me.zeyuan.lib.autopreferences.annotations.Preferences;

@Preferences()
interface Setting {

    boolean isFirstLaunch = false;

    /**
     * The version of api.
     */
    int version = 1;

    float score = 99.9f;

    long time = 1234567890;

    @Ignore()
    enum Type {
        FIELD,
        CLASS
    }

}
