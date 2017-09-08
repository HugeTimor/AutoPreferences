package me.zeyuan.lib.autopreferences;

import java.util.Set;

import me.zeyuan.lib.autopreferences.annotations.Preferences;

@Preferences()
interface Setting {

    boolean isFirstLaunch = false;

    /**
     * The version of api.
     */
    int version = 1;

    float score = 99.9f;

    double doubleField = 222.2222;

    long time = 1234567890;

    Set<String> langs = null;

    String name = null;
}
