package classloader.test

trait ClassloaderSupport extends Base {

  def withClassloader[T](classloader: ClassLoader)(f: () => T): T = {

    val crtLoader = Thread.currentThread().getContextClassLoader()
    Thread.currentThread().setContextClassLoader(classloader)

    debug("Replaced current classloader " + crtLoader + " by " + classloader)
    val fclass = f.getClass

    debug("Loading " + fclass + " with classloader: " + classloader)
    val fclassloaded = classloader.loadClass(fclass.getName()).asInstanceOf[Class[() => T]]

    debug("Loaded. Now instantiating.")
    val floaded: (() => T) = fclassloaded.newInstance()

    debug("Instantiated. Now running.")
    val result = floaded()

    debug("Ran. Restoring classloader: " + crtLoader)
    Thread.currentThread().setContextClassLoader(crtLoader)

    debug("Out.")
    result
  }

}
