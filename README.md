# AutoPreferences
Auto generate Android SharedPreferences read and write methods by annotation.

## 下载 

```groovy
dependencies {
  compile 'me.zeyuan:auto-preferences-annotations:0.4.0'
  annotationProcessor 'me.zeyuan:auto-preferences-compiler:0.4.0'
}
```

## 如何使用

### Define SharedPreferences entity
使用 `@Preferences() `  注解一个接口，在接口中定义所需的存储字段 ，field 的 value 为 SharedPreferences 的默认值。

```java
@Preferences()
interface Setting {

    boolean isFirstLaunch = false;

    /**
     * The version of api.
     */
    int version = 1;

    float score = 99.9f;

    long time = 1234567890;

}
```

### Rebuild project

重新构建工程, 注解编译处理器会根据定义的字段生成setter、getter方法。

```java
public class SettingPreferences {
  public static SharedPreferences getPreferences(Context context) {
    return context.getSharedPreferences("Setting", Context.MODE_PRIVATE);
  }

  public static boolean isFirstLaunch(Context context) {
    return getPreferences(context).getBoolean("isFirstLaunch",false);
  }

  public static void setFirstLaunch(Context context, boolean value) {
    SharedPreferences.Editor editor = getPreferences(context).edit();
    editor.putBoolean("isFirstLaunch",value);
    editor.apply();
  }

  /**
   *  The version of api.
   */
  public static int getVersion(Context context) {
    return getPreferences(context).getInt("version",1);
  }

  /**
   *  The version of api.
   */
  public static void setVersion(Context context, int value) {
    SharedPreferences.Editor editor = getPreferences(context).edit();
    editor.putInt("version",value);
    editor.apply();
  }

  public static float getScore(Context context) {
    return getPreferences(context).getFloat("score",99.9f);
  }

  public static void setScore(Context context, float value) {
    SharedPreferences.Editor editor = getPreferences(context).edit();
    editor.putFloat("score",value);
    editor.apply();
  }

  public static long getTime(Context context) {
    return getPreferences(context).getLong("time",1234567890);
  }

  public static void setTime(Context context, long value) {
    SharedPreferences.Editor editor = getPreferences(context).edit();
    editor.putLong("time",value);
    editor.apply();
  }
}

```

### Doing get || apply value
Rebuild project 之后，编译器会根据接口文件名生成：文件名 + Preferences 的 class file。
例如，上面的示例中文件名为Setting，生成的文件名为：SettingPreferences. 直接引用其中的setter，getter方法即可完成数据的get或者apply。

```
SettingPreferences.setVersion(MainActivity.this, 100);
SettingPreferences.getVersion(MainActivity.this)
```

## More 

### Skip some field 
默认情况下，接口文件中所有的字段都会生成setter、getter方法，如果你希望某些字段不生成这些方法，可以对字段使用 `@Skip` 注解。

### Set value by commit

默认情况下，所有的setter 方法都是使用 apply 来提交数据，如果希望使用 commit来提交数据，可以对字段使用 `@commit` 注解。
使用该注解之后，编译器会生成 setter 方法的同时生成 setterSync 方法，该方法内部是使用 commit 来提交数据。