package classloader

import java.net._
import java.io._
import org.objectweb.asm._
import org.objectweb.asm.tree._
import java.lang.{ClassLoader => JClassLoader}
import java.util.{List => JList}
import scala.collection.JavaConverters.asScalaBufferConverter
import java.util.ListIterator
import org.objectweb.asm.util._
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.BasicVerifier

/** Taken from http://stackoverflow.com/questions/6366288/how-to-change-default-class-loader-in-java */
class SpecializingClassloader(parent: ClassLoader) extends ClassLoader(parent) {

  def needsModifying(name: String): Boolean =
    // TODO: Extend to more parameters
    name.matches(".*_[0-9]$")

  def modifyClass(in: InputStream, oldname: String, newname: String): Array[Byte] = {
    val cr = new ClassReader(in)
    val classNode = new ClassNode()
    cr.accept(classNode, 0)

    classNode.name = newname

    // Make the type tags static final
    val fieldNodes = classNode.fields.asInstanceOf[JList[FieldNode]].asScala
    for (fieldNode <- fieldNodes if fieldNode.name.endsWith("$Type")) {
      fieldNode.access |= Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
      // TODO: Extend to more parameters, this only supports one
      fieldNode.value = new Integer(newname.last.toInt - '0'.toInt)
    }

    // Patch all the methods
    val methodNodes = classNode.methods.asInstanceOf[JList[MethodNode]].asScala
    for (methodNode <- methodNodes) {
      val insnNodes = methodNode.instructions.iterator().asInstanceOf[ListIterator[AbstractInsnNode]]
      while (insnNodes.hasNext) {
        insnNodes.next match {
          case finst: FieldInsnNode =>
            // update owner to the new class
            finst.owner = finst.owner.replace(oldname, newname) // update names everywhere
            // and patch the code for the static final value
            if (finst.name.endsWith("$Type")) {
              finst.getOpcode match {
                case Opcodes.GETFIELD =>
                  insnNodes.set(new InsnNode(Opcodes.POP));
                  insnNodes.add(new FieldInsnNode(Opcodes.GETSTATIC, finst.owner, finst.name, finst.desc))
                case Opcodes.PUTFIELD =>
                  insnNodes.set(new InsnNode(Opcodes.POP2));
              }
            }
          case minst: MethodInsnNode =>
            // update owner to the new class
            minst.owner = minst.owner.replace(oldname, newname) // update names everywhere
          case _ =>
        }
      }
    }

    // Debugging:
//    val printWriter = new PrintWriter(System.err);
//    val traceClassVisitor = new TraceClassVisitor(printWriter);
//    classNode.accept(traceClassVisitor);
//
//    val analyzer = new Analyzer(new BasicVerifier)
//    for (methodNode <- methodNodes) {
//      analyzer.analyze(name, methodNode)
//    }

    val cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    classNode.accept(cw);
    var classBytes = cw.toByteArray

    classBytes
  }

  override def findClass(decodedName: String): Class[_] = {
    if (needsModifying(decodedName)) {
      try {
        val encodedName = decodedName.replace('.', '/')
        // TODO: Extend to more parameters
        val encodedTplName = encodedName.replaceAll("_[0-9]$", "_J")
        val templateBytes = super.getResourceAsStream(encodedTplName + ".class");
        if (templateBytes == null) {
          throw new ClassNotFoundException("Class " + encodedTplName + " not found. Sorry.");
        }
        val array = modifyClass(templateBytes, encodedTplName, encodedName);
        return defineClass(decodedName, array, 0, array.length);
      } catch {
        case io: IOException =>
          throw new ClassNotFoundException(io.toString);
      }
    } else {
      return super.findClass(decodedName);
    }
  }
}
