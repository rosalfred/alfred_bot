package com.rosalfred.core.ia.rivescript.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.google.common.base.Strings;
import com.rosalfred.core.ia.rivescript.RiveScript;
import com.rosalfred.core.ia.rivescript.lang.java.CharSequenceJavaFileObject;
import com.rosalfred.core.ia.rivescript.lang.java.ClassFileManager;
import com.rosalfred.core.ia.rivescript.lang.java.JavaHandler;
import com.rosalfred.core.ia.rivescript.lang.java.RunRiver;

public class Java extends JavaHandler {

    private JavaCompiler compiler;
    private JavaFileManager fileManager;

    private String classpath;
    private String classname;

    public Java(RiveScript rs) {
        this(rs, null);
    }

    public Java(RiveScript rs, String classpath) {
        super(rs);

        this.classpath = classpath;

        // We get an instance of JavaCompiler.
        this.compiler = ToolProvider.getSystemJavaCompiler();

        // We create a file manager
        // (our custom implementation of it)
        this.fileManager = new
                ClassFileManager(
                        this.compiler.getStandardFileManager(null, null, null));

    }

    @Override
    public boolean onLoad(String name, String[] code) {
        StringBuilder src = new StringBuilder();

        this.loadFromRive(name, code, src);

        // Dynamic compiling requires specifying
        // a list of "files" to compile. In our case
        // this is a list containing one "file" which is in our case
        // our own implementation (see details below)
        CharSequenceJavaFileObject javafile =
        		new CharSequenceJavaFileObject(name, src);
        List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
        jfiles.add(javafile);

        List<String> optionList = null;

        if (this.classpath != null) {
            optionList = new ArrayList<String>();
            // set compiler's classpath to be same as the runtime's
            optionList.addAll(Arrays.asList("-classpath",
                     System.getProperty("java.class.path") + ":" + classpath));
        }

        // We specify a task to the compiler. Compiler should use our file
        // manager and our list of "files".
        // Then we run the compilation with call()
        this.compiler.getTask(null, this.fileManager, null, optionList,
                null, jfiles).call();

        this.classname = javafile.getQualifiedName();

        return false;
    }

    public String getLastCompiledClassName() {
    	return classname;
    }

    /**
     * @param name method name
     * @param code Rive code of method
     * @param src  runtime code
     */
    private void loadFromRive(String name, String[] code, StringBuilder src) {
        StringBuilder srcInner = new StringBuilder();
        StringBuilder srcImport = new StringBuilder();
        StringBuilder srcfinal = new StringBuilder();
        String cdLast = "";
        boolean isInner = true;

        for (String cd : code) {
            cdLast = cd;

            if (cd.startsWith("public")) {
                isInner = false;
            }

            if (cd.startsWith("import")) {
                srcImport.append(cd);
            } else {
                srcInner.append(cd);
            }

            srcfinal.append(cd + "\n");
        }

        if (isInner) {
            // Append import package
            if (srcImport.length() > 0) {
                src.append("\n" + srcImport + "\n\n");
            }

            src.append("\nimport " + RiveScript.class.getName() + ";\n" +
                    "import " + RunRiver.class.getName() + ";\n");

            // Append inner code
            src.append("public class " + name +
                    " implements " + RunRiver.class.getSimpleName() + " {\n\n");
            src.append("    @Override\n");
            src.append("    public String invoke(" +
                    RiveScript.class.getSimpleName() +
                    " rs, String user, String[] args) {\n\n");

            // Append inner code
            src.append("        " + srcInner + "\n");

            // Check if return exist
            if (!Strings.isNullOrEmpty(cdLast) && !cdLast.contains("return"))
                src.append("\n    return \"\";\n");

            src.append("    }\n");
            src.append("}\n");
        } else {
            src.append(srcfinal);
        }
    }

    @Override
    protected ClassLoader getClassLoader() {
        return this.fileManager.getClassLoader(null);
    }
}
