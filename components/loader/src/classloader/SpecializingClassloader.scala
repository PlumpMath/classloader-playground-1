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

  def needsModifying(name: String): Boolean = {
    val result = name.endsWith("Target_1")
    System.err.println("[classloader] needsModifying: " + name + " : "  + result)
    result
  }

  def modifyClass(in: InputStream, oldname: String, newname: String): Array[Byte] = {
    val cr = new ClassReader(in)
    val classNode = new ClassNode()
    cr.accept(classNode, 0)

    classNode.name = newname

//    val fieldNodes = classNode.fields.asInstanceOf[JList[FieldNode]].asScala
//    for (fieldNode <- fieldNodes if fieldNode.name.endsWith("$Type")) {
//      fieldNode.access |= Opcodes.ACC_STATIC;
//      fieldNode.value = new Integer(1)
//    }

    // Patch all the methods
    val methodNodes = classNode.methods.asInstanceOf[JList[MethodNode]].asScala
    for (methodNode <- methodNodes) {
      val insnNodes = methodNode.instructions.iterator().asInstanceOf[ListIterator[AbstractInsnNode]]
      while (insnNodes.hasNext) {
        insnNodes.next match {
//          case finsn: FieldInsnNode if (finsn.name.endsWith("_TypeTag")) =>
//            finsn.getOpcode match {
//              case Opcodes.GETFIELD =>
//                insnNodes.set(new InsnNode(Opcodes.POP));
//                insnNodes.add(new FieldInsnNode(Opcodes.GETSTATIC, finsn.owner, finsn.name, finsn.desc))
//              case Opcodes.PUTFIELD =>
//                insnNodes.set(new InsnNode(Opcodes.POP2));
//            }
          case finst: FieldInsnNode =>
            finst.owner = finst.owner.replace(oldname, newname) // update names everywhere
          case minst: MethodInsnNode =>
            minst.owner = minst.owner.replace(oldname, newname) // update names everywhere
          case _ =>
        }
      }
    }

//    val printWriter = new PrintWriter(System.err);
//    val traceClassVisitor = new TraceClassVisitor(printWriter);
//    classNode.accept(traceClassVisitor);

//    val analyzer = new Analyzer(new BasicVerifier)
//    for (methodNode <- methodNodes) {
//      analyzer.analyze(name, methodNode)
//    }

    val cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    classNode.accept(cw);
    var classBytes = cw.toByteArray

    classBytes
  }

  override def findClass(name: String): Class[_] = {
    if (needsModifying(name)) {
      try {
        val className = name.replace('.', '/')
        val classTpl = className.replaceAll("1", "J")
        val classData = super.getResourceAsStream(classTpl + ".class");
        if (classData == null) {
          throw new ClassNotFoundException("class " + classTpl + " is not findable");
        }
        val array = modifyClass(classData, classTpl, className);
        return defineClass(name, array, 0, array.length);
      } catch {
        case io: IOException =>
          throw new ClassNotFoundException(io.toString);
      }
    } else {
      return super.findClass(name);
    }
  }
}
