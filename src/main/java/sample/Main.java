package sample;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Main {

    public static void main(String[] args) throws Exception {
        Instrumentation inst = ByteBuddyAgent.install(); // Attaches an agent to current VM process

        // Includes deprecated attribute for "getCurrentCategories"
        ClassReader classReader = new ClassReader("org.apache.log4j.Category");
        classReader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);

        Class<?> type = Class.forName("org.apache.log4j.Category");

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                if ("org/apache/log4j/Category".equals(className)) {
                    // Does not include deprecated attribute for "getCurrentCategories"
                    ClassReader classReader = new ClassReader(classfileBuffer);
                    classReader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
                }
                return null;
            }
        }, true);

        inst.retransformClasses(type);
    }
}
