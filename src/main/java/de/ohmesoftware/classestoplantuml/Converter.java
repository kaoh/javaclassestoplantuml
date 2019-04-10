/*
 * Copyright(c) 2019 Simless, Inc.
 *
 * All rights reserved.
 */

package de.ohmesoftware.classestoplantuml;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Doc.
 *
 * @author <a href="mailto:karsten@simless.com">Karsten Ohme
 * (karsten@simless.com)</a>
 */
public class Converter {

    private String packageName;

    public Converter(String packageName) {
        this.packageName = packageName;
    }

    public String convert() {

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))));

        Set<String> allClasses = reflections.getAllTypes();
        allClasses.addAll(reflections.getSubTypesOf(Enum.class).stream().map(e -> e.getName()).collect(Collectors.toSet()));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("@startuml");

        for (String className : allClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                if (!clazz.isEnum() && !clazz.isInterface() && Modifier.isAbstract(clazz.getModifiers())) {
                    printWriter.print("abstract ");
                }
                if (clazz.isEnum()) {
                    printWriter.print("enum ");
                } else if (clazz.isInterface()) {
                    printWriter.print("interface ");
                } else {
                    printWriter.print("class ");
                }
                printWriter.print(escapeClassName(className));
                printWriter.println(" {");
                List<String> associations = new ArrayList<>();
                if (clazz.isEnum()) {
                    Arrays.asList(clazz.getEnumConstants()).forEach(printWriter::println);
                }
                else if (!clazz.isInterface()) {
                    for (Field field : clazz.getDeclaredFields()) {
                        printWriter.print("{field} ");
                        if (Modifier.isPrivate(field.getModifiers())) {
                            printWriter.print("-");
                        } else if (Modifier.isProtected(field.getModifiers())) {
                            printWriter.print("#");
                        } else if (Modifier.isPublic(field.getModifiers())) {
                            printWriter.print("+");
                        } else {
                            printWriter.print("~");
                        }
                        printWriter.print(field.getName());
                        printWriter.print(" : ");
                        printWriter.print(escapeClassName(field.getType().getName()));
                        if (allClasses.contains(field.getType().getName())) {
                            associations.add(escapeClassName(className) + "-->"
                                    + escapeClassName(field.getType().getName()));
                        }
                        if (Collection.class.isAssignableFrom(field.getType())) {
                            Type collectionType = field.getGenericType();
                            if (collectionType instanceof ParameterizedType) {
                                ParameterizedType paramType = (ParameterizedType) collectionType;
                                Type[] argTypes = paramType.getActualTypeArguments();
                                if (argTypes.length > 0) {
                                    if (allClasses.contains(argTypes[0].getTypeName())) {
                                        associations.add(escapeClassName(className) + "-->"
                                                + escapeClassName(argTypes[0].getTypeName()));
                                    }
                                    printWriter.print("<" + escapeClassName(argTypes[0].getTypeName()) + ">");
                                }
                            }
                        }
                        printWriter.println();
                    }
                }
                printWriter.println("}");
                associations.forEach(printWriter::println);
                // add inheritance
                if (allClasses.contains(clazz.getSuperclass().getName())) {
                    printWriter.println(escapeClassName(clazz.getSuperclass().getName()) + "<|--"
                            + escapeClassName(className));
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Detected class not found.", e);
            }
        }
        printWriter.println("@enduml\n");
        return stringWriter.getBuffer().toString();
    }

    private static String escapeClassName(String className) {
        return "\"" + className + "\"";
    }
}
