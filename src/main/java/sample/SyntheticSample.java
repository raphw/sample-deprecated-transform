package sample;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class SyntheticSample {

    public static void main(String[] args) throws Exception {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_4, Opcodes.ACC_ABSTRACT, "foo/Bar", null, "java/lang/Object", null);
        classWriter.visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC, "qux", "()V", null, null).visitEnd();
        final byte[] bytes = classWriter.toByteArray();

        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals("foo.Bar")) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
                return super.findClass(name);
            }
        };

        final Class<?> type = Class.forName("foo.Bar", false, classLoader);

        Instrumentation inst = ByteBuddyAgent.install();

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (classBeingRedefined == type) {
                    ClassReader classReader = new ClassReader(classfileBuffer);
                    ClassWriter transform = new ClassWriter(classReader, 0);
                    classReader.accept(transform, 0);
                    return transform.toByteArray();
                }
                return null;
            }
        }, true);

        inst.retransformClasses(type);
    }
}
