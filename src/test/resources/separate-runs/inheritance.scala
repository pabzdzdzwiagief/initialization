package localhost

class inheritance extends base {
  m1()
  val v1 = 4
}

// warning: value v1 is referenced before assignment
//         at localhost.base.m1(inheritance.scala:4)
//         at localhost.inheritance.<init>(inheritance.scala:3)
//
