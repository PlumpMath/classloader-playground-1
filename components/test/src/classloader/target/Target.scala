package classloader.target

import scala.collection.mutable.WeakHashMap

trait Target[T] {
  def print: Unit
  def t: T
  def t_J: Long
}

class Target_J[`T$sp`](val t_J: Long, val `T$Type`: Byte) extends Target[`T$sp`] {
  def t = ???
  def print: Unit = System.out.println("print(" + t_J + ", " + `T$Type` + ") by " + this.getClass.getName())
}

class Target_L[`T$sp`](val t: `T$sp`) extends Target[T$sp] {
  def t_J = ???
  def print: Unit = System.out.println("print( " + t.toString + ") by " + this.getClass.getName())
}

object TargetFactory {

  def newTarget_L[T$inst](t: T$inst): Target[T$inst] =
    newTarget(t)

  def newTarget_J[T$inst](t_J: Long, T$Type: Byte): Target[T$inst] = {
    val name = "Target_" + T$Type
    try {
      System.err.println(s"[factory] Attempting to locate $name. with classloader: " + Thread.currentThread().getContextClassLoader())
      val clazz = Class.forName("classloader.target.Target_" + T$Type, true, Thread.currentThread().getContextClassLoader())
      System.err.println(s"[factory] Located $name.")
      // this is not realistic for miniboxing, but will have to do
      val const = clazz.getConstructor(classOf[Long], classOf[Byte])
      val inst  = const.newInstance(t_J: java.lang.Long, T$Type: java.lang.Byte)
      inst.asInstanceOf[Target[T$inst]]
    } catch {
      case cnf: ClassNotFoundException =>
        System.err.println(s"[factory] Could not locate $name: " + cnf.toString)
        new Target_J(t_J, T$Type)
    }
  }

  def newTarget[T$inst](t: T$inst): Target[T$inst] =
    new Target_L[T$inst](t)
}
