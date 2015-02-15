[![Build Status](https://travis-ci.org/pabzdzdzwiagief/initialization.svg?branch=master)](https://travis-ci.org/pabzdzdzwiagief/initialization)
[![Coverage Status](https://coveralls.io/repos/pabzdzdzwiagief/initialization/badge.svg?branch=master)](https://coveralls.io/r/pabzdzdzwiagief/initialization?branch=master)

initialization
==============

A Scala compiler plugin. Detects code that may run into any of several Scala's
initialization order
[pitfalls](http://docs.scala-lang.org/tutorials/FAQ/initialization-order.html).
It does not attempt to prevent every possible reference-before-initialization.
Still, at least some classes of such errors are covered.

The difference between this and `-Xcheckinit` compiler switch is that checks
are done purely during compilation time. There are no costs incurred
at runtime, only a library dependency is brought for necessary metadata.

usage
-----

Add this to your `build.sbt`:

     autoCompilerPlugins := true

     libraryDependencies += compilerPlugin("com.github.pabzdzdzwiagief" %% "initialization" % "0.11.0-rc.2")

     libraryDependencies += "com.github.pabzdzdzwiagief.initialization" %% "annotation" % "0.11.0-rc.2"

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

For more examples see `src/test/resources/examples/positives/`.

limitations
-----------

#### code compiled without the plugin

Initialization problems can be spotted only in the code compiled with
the plugin. When e.g. extending a class that comes from some external
library, the analysis will loose its track every time a call graph descends
to methods defined in that class.

#### false positives

The plugin may happen to be overreactive. In such cases use standard
`@unchecked` annotation.

    pacakge localhost

    import util.Random.{nextBoolean => iFeelLucky}

    class uncheck {
      (m1() : @unchecked) // no warnings
      val v1 = 4

      def m1() {
        println(if (iFeelLucky) v1 else 4)
      }
    }

#### functions

All functions are always treated as if they were called immediately after
being created. The primary reason is handling a quite typical case:

    class usageOfForLoop {
      for (i <- 1 to 10) {
        println(notInitialized)
      }

      val notInitialized = 4
    }

which uses a compiler-generated function underneath. Tracking down where do
functions passed as parameters eventually end up being called requires
far broader and more detailed analysis than one the plugin performs.

In some cases changing a function definition to a local method may help:

    // change this:
    val inc = {(x: Int) => x + 1}

    // to this:
    def inc(x: Int) = x + 1

and while any later partial application/currying will trigger reference check
anyway, it will at least happen in a closer proximity to the actual function
call.

test cases
----------

To add a test case, put a file in `src/test/resources/positives/` directory
(or in `negatives/` for tests with no compilation warnings expected) with
the following format:

    // [one-line title of the test case]

    package localhost

    [scala code to compile]

    // [expected compilation messages, with one-space margin]
    //
    //

to have it automatically picked up when tests are executed.

license
-------

[BSD 2-Clause](http://opensource.org/licenses/BSD-2-Clause)
