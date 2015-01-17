// Access inside `for` loop with local method

package localhost

class foreach {
  for (i <- 1 to 10) {
    def nested() = {
      m1(i)
    }
    nested()
  }
  val v1 = 4

  def m1(i: Int) {
    println(v1 + i)
  }
}

// foreach.scala:15: warning: value v1 is referenced before assignment
//         at localhost.foreach.m1(foreach.scala:8)
//         at localhost.foreach$$anonfun$1.nested$1(foreach.scala:10)
//         at localhost.foreach$$anonfun$1.apply$mcVI$sp(foreach.scala:6)
//         at localhost.foreach$$anonfun$1.apply(foreach.scala:6)
//         at localhost.foreach.<init>(foreach.scala:5)
//
//     println(v1 + i)
//             ^
