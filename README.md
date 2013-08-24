initialization
==============

Scala compiler plugin checking if compiled code may run into
any of several Scala's initialization order
[pitfalls](https://github.com/paulp/scala-faq/wiki/Initialization-Order).
This plugin does not pretend to prevent every
reference-before-initialization. Still, at least some classes of such
errors can be covered.

The difference between this and `-Xcheckinit` compiler switch is that
plugin's initialization checks are performed purely during compilation
time. There are no costs incurred at runtime, although a small library
dependency is brought for necessary metadata.

compilation
-----------

     sbt package

example
-------

    // simple.scala

    pacakge localhost

    class simple {
      m1()
      val v1 = 4

      def m1() {
        println(v1)
      }
    }

The code above will generate following warning:

    simple.scala:10: warning: value v1 is referenced before assignment
            at localhost.simple.m1(simple.scala:6)
            at localhost.simple.<init>(simple.scala:5)

        println(v1)
                ^

For more examples see `plugin/src/test/resources/positives/`.

license
-------

[BSD 2-Clause](http://opensource.org/licenses/BSD-2-Clause)
