package me.zeyuan.lib.autopreferences;

import me.zeyuan.lib.autopreferences.annotations.Commit;
import me.zeyuan.lib.autopreferences.annotations.Name;
import me.zeyuan.lib.autopreferences.annotations.Preferences;

@Preferences("test_auth")
interface Auth {
    @Name("auth_name")
    String userName = "";

    @Commit()
    @Name("auth_token")
    String token = "";

    @Name("auth_time")
    long time = 1234567890;
}
