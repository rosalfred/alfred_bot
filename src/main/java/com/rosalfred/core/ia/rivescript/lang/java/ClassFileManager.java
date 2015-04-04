package com.rosalfred.core.ia.rivescript.lang.java;

import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.HashMap;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileObject.Kind;

public class ClassFileManager
        extends ForwardingJavaFileManager<JavaFileManager> {

    /**
     * Instance of JavaClassObject that will store the
     * compiled bytecode of our class
     */
    private HashMap<String, JavaClassObject> jclassObjects =
            new HashMap<String, JavaClassObject>();

    /**
     * Will initialize the manager with the specified
     * standard java file manager
     *
     * @param standardManger
     */
    public ClassFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
    }

    /**
     * Will be used by us to get the class loader for our
     * compiled class. It creates an anonymous class
     * extending the SecureClassLoader which uses the
     * byte code created by the compiler and stored in
     * the JavaClassObject, and returns the Class for it
     */
    @Override
    public ClassLoader getClassLoader(Location location) {
        return new SecureClassLoader() {

            @Override
            protected Class<?> findClass(String name)
                    throws ClassNotFoundException {

                if (jclassObjects.containsKey(name)) {
                    JavaClassObject jclassObject = jclassObjects.get(name);
                    byte[] b = jclassObject.getBytes();

                    return super.defineClass(
                            name,
                            jclassObject.getBytes(),
                            0,
                            b.length);
                }

                return null;
            }
        };
    }

    /**
     * Gives the compiler an instance of the JavaClassObject
     * so that the compiler can write the byte code into it.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
            String className, Kind kind, FileObject sibling)
                    throws IOException {
        JavaClassObject jclassObject;

        if (sibling instanceof CharSequenceJavaFileObject) {
        	((CharSequenceJavaFileObject) sibling).setQualifiedName(className);
        }

        //if (!this.jclassObjects.containsKey(className)) {
            jclassObject = new JavaClassObject(className, kind);
            if (jclassObjects.containsKey(className));
                this.jclassObjects.put(className, jclassObject);
        //} else {
        //    jclassObject = this.jclassObjects.get(className);
        //}

        return jclassObject;
    }
}