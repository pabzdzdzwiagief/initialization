// *not implemented* Access inside `for` loop

package localhost

class foreach {
  for (m ← Stream.continually(m1 _)) {
    m()
  }
  val v1 = 4

  def m1() {
    println(v1)
  }
}

// foreach.scala:10: warning: value v1 is referenced before assignment
//         at localhost.foreach.m1(foreach.scala:6)
//         at localhost.foreach.<init>(foreach.scala:5)
//
//     println(v1)
//             ^
