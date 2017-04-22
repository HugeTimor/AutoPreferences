package me.zeyuan.lib.autopreferences.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import me.zeyuan.lib.autopreferences.annotations.Ignore;
import me.zeyuan.lib.autopreferences.annotations.Name;
import me.zeyuan.lib.autopreferences.annotations.Preferences;


@AutoService(Processor.class)
public class PreferencesProcessor extends AbstractProcessor {

    private enum Act {
        PUT,
        GET
    }

    private static final String CLASS_NAME_SUFFIX = "Preferences";
    private static final int CONTEXT_MODE_PRIVATE = 0x0000;

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    private TypeName SharedPreferences = ClassName.get("android.content", "SharedPreferences");
    private TypeName Context = ClassName.get("android.content", "Context");


    /**
     * @param env 处理环境信息
     */
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        for (Element preferences : env.getElementsAnnotatedWith(Preferences.class)) {
            if (preferences.getKind() == ElementKind.INTERFACE) {

                TypeSpec.Builder classBuilder = buildBaseClass(preferences);

                addOperateMethod(preferences, classBuilder);

                writeToJavaFile(getPackageName(preferences), classBuilder.build());
            }
        }
        return false;
    }

    private TypeSpec.Builder buildBaseClass(Element preferences) {
        String fileName = getFileName(preferences);
        MethodSpec getPreferences = genGetPreferences(fileName);

        String className = preferences.getSimpleName().toString() + CLASS_NAME_SUFFIX;
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getPreferences);
    }

    private void addOperateMethod(Element preferences, TypeSpec.Builder classBuilder) {
        for (Element field : preferences.getEnclosedElements()) {
            if (!isIgnored(field)) {
                String fieldName = field.getSimpleName().toString();
                String keyName = getKeyName(field);
                TypeMirror type = field.asType();
                Object defaultValue = ((VariableElement) field).getConstantValue();

                MethodSpec getterMethod = genGetterMethod(fieldName, keyName, type, defaultValue);
                classBuilder.addMethod(getterMethod);

                MethodSpec setterMethod = genSetterMethod(fieldName, keyName, type);
                classBuilder.addMethod(setterMethod);
            }
        }
    }

    private String getKeyName(Element field) {
        Name name = field.getAnnotation(Name.class);
        if (name == null || name.value().isEmpty()) {
            return field.getSimpleName().toString();
        } else {
            return name.value();
        }
    }

    private String getFileName(Element preferences) {
        Preferences pref = preferences.getAnnotation(Preferences.class);
        if (pref.value().isEmpty()) {
            return preferences.getSimpleName().toString();
        } else {
            return pref.value();
        }
    }

    private MethodSpec genGetterMethod(String fieldName, String keyName, TypeMirror type,
                                       Object defaultValue) {
        defaultValue = formatValue(defaultValue);
        String action = getAction(type, Act.GET);
        String methodName = getterNameFormat(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(type))
                .addParameter(Context, "context")
                .addStatement("return getPreferences(context).$L($S,$L)", action, keyName, defaultValue)
                .build();
    }

    private MethodSpec genSetterMethod(String fieldName, String keyName, TypeMirror type) {
        String methodName = setterNameFormat(fieldName);
        String action = getAction(type, Act.PUT);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(Context, "context")
                .addParameter(ClassName.get(type), "value")
                .addStatement("SharedPreferences.Editor editor = getPreferences(context).edit()")
                .addStatement("editor.$L($S,value)", action, keyName)
                .addStatement("editor.apply()")
                .build();
    }

    private Object formatValue(Object defaultValue) {
        Object result = defaultValue;
        if (defaultValue instanceof String && ((String) defaultValue).isEmpty()) {
            result = "null";
        }
        // 避免替换 $L 时被认为 double
        if (defaultValue instanceof Float) {
            result = defaultValue + "f";
        }
        return result;
    }

    private String getAction(TypeMirror type, Act act) {
        String prefix;
        if (act == Act.PUT) {
            prefix = "put";
        } else {
            prefix = "get";
        }
        switch (type.toString()) {
            case "boolean":
                return prefix + "Boolean";
            case "int":
                return prefix + "Int";
            case "float":
                return prefix + "Float";
            case "long":
                return prefix + "Long";
            case "java.lang.String":
                return prefix + "String";
            case "java.util.Set":
                return prefix + "StringSet";
        }
        return null;
    }

    private String setterNameFormat(String key) {
        String result = key;
        if (key.startsWith("is")) {
            result = key.replace("is", "");
        }
        result = capitalize(result);
        return "set" + result;
    }

    private String getterNameFormat(String name) {
        if (!name.startsWith("is")) {
            name = capitalize(name);
            return "get" + name;
        }
        return name;
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private boolean isIgnored(Element preference) {
        return preference.getAnnotation(Ignore.class) != null;
    }

    private String getPackageName(Element element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private void writeToJavaFile(String packageName, TypeSpec typeSpec) {
        JavaFile javaFile = JavaFile
                .builder(packageName, typeSpec)
                .addFileComment("This codes are generated automatically. Do not modify!")
                .build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec genGetPreferences(String name) {
        return genGetPreferences(name, CONTEXT_MODE_PRIVATE);
    }

    private MethodSpec genGetPreferences(String name, int mode) {
        String modeName = getModeName(mode);
        return MethodSpec.methodBuilder("getPreferences")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(SharedPreferences)
                .addParameter(Context, "context")
                .addStatement("return context.getSharedPreferences($S, $L)", name, modeName)
                .build();
    }

    private String getModeName(int mode) {
        switch (mode) {
            case 0x0000:
                return "Context.MODE_PRIVATE";
            case 0x0001:
                return "Context.MODE_WORLD_READABLE";
            case 0x0002:
                return "Context.MODE_WORLD_WRITEABLE";
            case 0x8000:
                return "Context.MODE_APPEND";
            default:
                return "Context.MODE_PRIVATE";

        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotations = new LinkedHashSet<>();

        supportedAnnotations.add(Preferences.class.getCanonicalName());
        supportedAnnotations.add(Ignore.class.getCanonicalName());

        return supportedAnnotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
