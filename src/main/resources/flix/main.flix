import java.lang.System;

def createEdges(n: Int32): List[(Int32, Int32)] = 
  let range = List.range(0,n);
  List.map(x -> (x, x+1), range)


def main(): Unit \ IO =
  let p = #{
    Edge(x, y) :- Edge(y, x).
    Reachable(x, y) :- Edge(x, y).
    Reachable(x, y) :- Reachable(x, z), Edge(z, y).
  };
  
  let edges = inject createEdges(1000) into Edge;
  let q = query edges, p select (x, y) from Reachable(x, y);
  let start = System.currentTimeMillis();
  println(Vector.find(_x -> true, q));
  let end = System.currentTimeMillis();
  println(end-start)

  