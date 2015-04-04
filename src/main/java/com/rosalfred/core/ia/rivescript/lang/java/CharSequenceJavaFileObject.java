package com.rosalfred.core.ia.rivescript.lang.java;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class CharSequenceJavaFileObject extends SimpleJavaFileObject {
	/** Class name with namespace. **/
	private String qualifiedName;

    /**
     * CharSequence representing the source code to be compiled
     */
    private CharSequence content;

    /**
     * This constructor will store the source code in the
     * internal "content" variable and register it as a
     * source code, using a URI containing the class full name
     *
     * @param className
     *            name of the public class in the source code
     * @param content
     *            source code to compile
     */
    public CharSequenceJavaFileObject(String className,
            CharSequence content) {
        super(URI.create("string:///" + className.replace('.', '/')
                + Kind.SOURCE.extension), Kind.SOURCE);
        this.content = content;
    }

    /**
     * Answers the CharSequence to be compiled. It will give
     * the source code stored in variable "content"
     */
    @Override
    public CharSequence getCharContent(
            boolean ignoreEncodingErrors) {
        return content;
    }

    /**
     * Set the qualifiedname of the JavaFileObject.
     * @param name The classname with namespace
     */
    public void setQualifiedName(String name) {
    	this.qualifiedName = name;
    }

    /**
     * @return Get the QualifiedName of the JavaFileObject
     */
    public String getQualifiedName() {
    	return this.qualifiedName;
    }
}