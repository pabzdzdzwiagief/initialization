// Simple access inside `for` loop from README

package localhost

class simpleForeach {
  for (i <- 1 to 10) {
    println(notInitialized)
  }

  val notInitialized = 4
}

// simple-foreach.scala:7: warning: value notInitialized is referenced before assignment
//         at localhost.simpleForeach$$anonfun$1.apply$mcVI$sp(simple-foreach.scala:6)
//         at localhost.simpleForeach$$anonfun$1.apply(simple-foreach.scala:6)
//         at localhost.simpleForeach.<init>(simple-foreach.scala:5)
//
//     println(notInitialized)
//             ^
