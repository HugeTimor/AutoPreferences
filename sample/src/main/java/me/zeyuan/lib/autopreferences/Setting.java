package me.zeyuan.lib.autopreferences;

import me.zeyuan.lib.autopreferences.annotations.Ignore;
import me.zeyuan.lib.autopreferences.annotations.Preferences;

@Preferences()
interface Setting {

    boolean isFirstLaunch = false;

    String token = "";

    int version = 1;

    float score = 0f;

    long time = 1234567890;

    @Ignore()
    String id = "";

    @Ignore()
    enum Type {
        FIELD,
        CLASS
    }

}
