initialization
==============

Scala compiler plugin checking if compiled code may run into
any of the several Scala's initialization order
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

status
------

At concept stage.

concept
-------

The plugin provides two additional compilation phases: `init-order`
and `init-check`. The `init-order` phase leaves metadata about
initialization order, while the `init-check` phase verifies
correctness of that order.

* `init-order`

This phase leaves certain annotations in resulting classfiles. Those
annotations allow to tell how initialization sequence looks like.

    @Reference(method m1, file1.scala, line-4)
    @Definition(val v1, file1.scala, line-5)
    class C {
      m1()
      val v1 = 4

      @Reference(val v1, file1.scala, line-9)
      def m1() {
        println(v1)
      }
    }

This part is done right after typechecking, when AST has most of the
necessary type information but has not become too messy yet.
It could probably be done even later with analysis of generated
bytecode, but then the messages issued after compilation would be
rather incomprehensible.

* `init-check`

This phase reads the reference graph from annotations and tries to
find any errors.

There is more to it, like early definitions or class linearization,
but this is the general idea.

license
-------

[BSD 2-Clause](http://opensource.org/licenses/BSD-2-Clause)
