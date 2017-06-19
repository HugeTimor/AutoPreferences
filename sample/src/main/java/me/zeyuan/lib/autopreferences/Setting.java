package me.zeyuan.lib.autopreferences;

import me.zeyuan.lib.autopreferences.annotations.Preferences;
import me.zeyuan.lib.autopreferences.annotations.Skip;

@Preferences()
interface Setting {

    boolean isFirstLaunch = false;

    /**
     * The version of api.
     */
    int version = 1;

    float score = 99.9f;

    long time = 1234567890;

    @Skip()
    enum Type {
        FIELD,
        CLASS
    }
}
