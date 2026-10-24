import javax.tools.ToolProvider;

class JavaCheck {
    public static void main(String[] args) {
        int version = Runtime.version().feature();
        if (version < 17 || version > 23 || ToolProvider.getSystemJavaCompiler() == null) {
            System.err.println("Bu proje icin JDK 17 veya 21 secin. Bulunan Java: " + version);
            System.exit(1);
        }
        System.out.println("Java denetimi tamam: JDK " + version);
    }
}
