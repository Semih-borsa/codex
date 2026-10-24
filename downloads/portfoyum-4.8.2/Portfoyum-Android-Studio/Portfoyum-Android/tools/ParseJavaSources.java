import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.io.File;
import java.util.*;

/** Syntax-only check. Does not substitute for compilation against the Android SDK. */
class ParseJavaSources {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<File> files = new ArrayList<>();
            for (String arg : args) files.add(new File(arg));
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    Arrays.asList("-proc:none", "--release", "17"), null, manager.getJavaFileObjectsFromFiles(files));
            task.parse();
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) throw new IllegalStateException(d.toString());
            }
            System.out.println("Java syntax valid; Android SDK/type checking was NOT performed.");
        }
    }
}
