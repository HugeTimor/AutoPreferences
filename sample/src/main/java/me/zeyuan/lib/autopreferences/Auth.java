package me.zeyuan.lib.autopreferences;

import me.zeyuan.lib.autopreferences.annotations.Commit;
import me.zeyuan.lib.autopreferences.annotations.Key;
import me.zeyuan.lib.autopreferences.annotations.Preferences;

@Preferences("test_auth")
interface Auth {
    @Key("auth_name")
    String userName = "perter";

    @Commit()
    @Key("auth_token")
    String token = "fasdfad";

    @Commit()
    double validTime = 22.3333;

    @Key("auth_time")
    long time = 1234567890;
}
