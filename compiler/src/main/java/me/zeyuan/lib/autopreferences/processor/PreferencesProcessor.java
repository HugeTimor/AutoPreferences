package me.zeyuan.lib.autopreferences.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
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
import javax.tools.Diagnostic.Kind;

import me.zeyuan.lib.autopreferences.annotations.Commit;
import me.zeyuan.lib.autopreferences.annotations.Key;
import me.zeyuan.lib.autopreferences.annotations.Preferences;


@AutoService(Processor.class)
public class PreferencesProcessor extends AbstractProcessor {

    private enum Operation {
        PUT,
        GET
    }

    private static final String CLASS_NAME_SUFFIX = "Preferences";

    private TypeName SharedPreferences = ClassName.get("android.content", "SharedPreferences");
    private TypeName Context = ClassName.get("android.content", "Context");

    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    /**
     * @param env 处理环境信息
     */
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        for (Element annotatedFile : env.getElementsAnnotatedWith(Preferences.class)) {
            if (annotatedFile.getKind() != ElementKind.INTERFACE) {
                error("@Preferences can only be applied attach to interface", annotatedFile);
            }

            JavaFile javaFile = brewJava(annotatedFile);
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotations = new LinkedHashSet<>();
        supportedAnnotations.add(Preferences.class.getCanonicalName());
        return supportedAnnotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private JavaFile brewJava(Element annotatedFile) {
        String className = annotatedFile.getSimpleName().toString() + CLASS_NAME_SUFFIX;
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
        classBuilder.addModifiers(Modifier.PUBLIC);

        classBuilder.addMethod(genGetPreferencesMethod(annotatedFile));
        classBuilder.addMethods(genOperationMethods(annotatedFile));

        return JavaFile
                .builder(getPackageName(annotatedFile), classBuilder.build())
                .addFileComment("This codes are generated automatically. Do not modify!")
                .build();
    }

    private void error(String message, Element preferences) {
        messager.printMessage(Kind.ERROR, message, preferences);
    }

    private void error(String message) {
        messager.printMessage(Kind.ERROR, message);
    }

    private ArrayList<MethodSpec> genOperationMethods(Element annotatedFile) {
        ArrayList<MethodSpec> methods = new ArrayList<>();
        for (Element field : annotatedFile.getEnclosedElements()) {
            if (!isSupportType(field)) {
                error("The " + field.asType().toString() + " is no supported");
            }
            MethodSpec getterMethod = genGetterMethod(field);
            methods.add(getterMethod);
            MethodSpec setterMethod = genSetterMethod(field);
            methods.add(setterMethod);
            if (haveCommitAnnotation(field)) {
                MethodSpec setterSyncMethod = genSetterSyncMethod(field);
                methods.add(setterSyncMethod);
            }
        }
        return methods;
    }

    private boolean isSupportType(Element field) {
        TypeMirror type = field.asType();
        switch (type.getKind()) {
            case BOOLEAN:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return true;
        }
        if (type.toString().equals("java.lang.String")) {
            return true;
        }
        if (type.toString().equals("java.util.Set<java.lang.String>")) {
            return true;
        }
        return false;
    }

    private Object getDefaultValue(Element field) {
        Object defValue = ((VariableElement) field).getConstantValue();
        if (defValue == null) {
            return "null";
        }
        if (defValue instanceof String) {
            String stringValue = (String) defValue;
            if (stringValue.isEmpty()) {
                return "\"\"";
            } else {
                return "\"" + defValue + "\"";
            }
        } else if (defValue instanceof Float) {
            return defValue + "f";
        } else if (defValue instanceof Double) {
            return "\"" + defValue + "\"";
        }
        return defValue;
    }

    private boolean haveCommitAnnotation(Element field) {
        return field.getAnnotation(Commit.class) != null;
    }

    private String getNameOfKey(Element field) {
        Key key = field.getAnnotation(Key.class);
        if (key == null || key.value().isEmpty()) {
            return field.getSimpleName().toString();
        } else {
            return key.value();
        }
    }

    private String getFileName(Element preferences) {
        Preferences annotation = preferences.getAnnotation(Preferences.class);
        if (annotation.value().isEmpty()) {
            return preferences.getSimpleName().toString();
        } else {
            return annotation.value();
        }
    }

    private MethodSpec genGetterMethod(Element field) {
        String fieldName = field.getSimpleName().toString();
        String keyName = getNameOfKey(field);
        Object defValue = getDefaultValue(field);
        String comment = elementUtils.getDocComment(field);
        TypeMirror type = field.asType();

        String action = getAction(type, Operation.GET);
        String methodName = getterNameFormat(fieldName);
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(type))
                .addParameter(Context, "context");
        if (((VariableElement) field).getConstantValue() instanceof Double) {
            method.addStatement("return Double.valueOf(getPreferences(context).$L($S,$L))", action, keyName, defValue);
        } else {
            method.addStatement("return getPreferences(context).$L($S,$L)", action, keyName, defValue);
        }
        if (comment != null && !comment.isEmpty()) {
            method.addJavadoc(comment);
        }
        return method.build();
    }

    private MethodSpec genSetterMethod(Element field) {
        String fieldName = field.getSimpleName().toString();
        String keyName = getNameOfKey(field);
        String comment = elementUtils.getDocComment(field);
        TypeMirror type = field.asType();

        String methodName = setterNameFormat(fieldName);
        String action = getAction(type, Operation.PUT);
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(Context, "context")
                .addParameter(ClassName.get(type), "value")
                .addStatement("SharedPreferences.Editor editor = getPreferences(context).edit()");
        if (ClassName.get(type).toString().equals("double")) {
            method.addStatement("editor.$L($S,String.valueOf(value))", action, keyName);
        } else {
            method.addStatement("editor.$L($S,value)", action, keyName);
        }
        method.addStatement("editor.apply()");
        if (comment != null && !comment.isEmpty()) {
            method.addJavadoc(comment);
        }
        return method.build();
    }

    private MethodSpec genSetterSyncMethod(Element field) {
        String fieldName = field.getSimpleName().toString();
        String keyName = getNameOfKey(field);
        String comment = elementUtils.getDocComment(field);
        TypeMirror type = field.asType();

        String methodName = setterNameFormat(fieldName) + "Sync";
        String action = getAction(type, Operation.PUT);
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(Context, "context")
                .addParameter(ClassName.get(type), "value")
                .addStatement("SharedPreferences.Editor editor = getPreferences(context).edit()");
        if (ClassName.get(type).toString().equals("double")) {
            method.addStatement("editor.$L($S,String.valueOf(value))", action, keyName);
        } else {
            method.addStatement("editor.$L($S,value)", action, keyName);
        }
        method.addStatement("return editor.commit()");
        if (comment != null && !comment.isEmpty()) {
            method.addJavadoc(comment);
        }
        return method.build();
    }

    private String getAction(TypeMirror type, Operation operation) {
        String prefix;
        if (operation == Operation.PUT) {
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
            case "double":
                return prefix + "String";
            case "long":
                return prefix + "Long";
            case "java.lang.String":
                return prefix + "String";
            case "java.util.Set<java.lang.String>":
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

    private String getPackageName(Element element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private MethodSpec genGetPreferencesMethod(Element annotatedFile) {
        String fileName = getFileName(annotatedFile);
        return MethodSpec.methodBuilder("getPreferences")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(SharedPreferences)
                .addParameter(Context, "context")
                .addStatement("return context.getSharedPreferences($S, $L)", fileName, "Context.MODE_PRIVATE")
                .build();
    }
}
