package com.openshine.escova.functional

import java.security.{AccessController, PrivilegedAction}

import scala.reflect.ClassTag

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object implicits {

  implicit class PrivateMethodAccessor[A](val instance: A)(
      implicit c: ClassTag[A]) {
    def getPrivateFieldValue[B](fieldName: String): B = {
      AccessController.doPrivileged(new PrivilegedAction[B] {
        override def run(): B = {
          val f: java.lang.reflect.Field =
            c.runtimeClass.getDeclaredField(fieldName)

          f.setAccessible(true)
          f.get(instance).asInstanceOf[B]
        }
      })
    }
  }

}
