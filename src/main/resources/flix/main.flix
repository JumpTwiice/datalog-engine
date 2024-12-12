import java.lang.System;

def createEdges(n: Int32): List[(Int32, Int32)] = 
  let range = List.range(0,n);
  List.map(x -> (x, x+1), range)

def reachable(): Unit \ IO =
  let p = #{
      Edge(x, y) :- Edge(y, x).
      Reachable(x, y) :- Edge(x, y).
      Reachable(x, y) :- Reachable(x, z), Edge(z, y).
    };
  let range = List.range(0,10);
  println("Warming up...");
  List.forEach(_ -> {
    let edges = inject createEdges(1000) into Edge;
    let q = query edges, p select (x, y) from Reachable(x, y);
    let start = System.currentTimeMillis();
    println(Vector.find(_x -> true, q));
    let end = System.currentTimeMillis();
    println(end-start)
  }, range);
  println("Benchmarking");
  List.forEach(_ -> {
    let edges = inject createEdges(1000) into Edge;
    let q = query edges, p select (x, y) from Reachable(x, y);
    let start = System.currentTimeMillis();
    println(Vector.find(_x -> true, q));
    let end = System.currentTimeMillis();
    println(end-start)
  }, range)


def reachable_flipped(): Unit \ IO =
  let range = List.range(0,3);
  let p = #{
      Edge(x, y) :- Edge(y, x).
      Reachable(x, y) :- Edge(x, y).
      Reachable(x, y) :- Edge(z, y), Reachable(x, z).
    };
  List.forEach(_ -> {
    let edges = inject createEdges(500) into Edge;
    let q = query edges, p select (x, y) from Reachable(x, y);
    let start = System.currentTimeMillis();
    println(Vector.find(_x -> true, q));
    let end = System.currentTimeMillis();
    println(end-start)
  }, range)


def cartesian(): Unit \ IO = 
  let range = List.range(0,10);
  let prog = #{
      R(x,y,z,w) :- P(x,y), Q(z,w).
    };
  List.forEach(_ -> {
    let p = inject createEdges(500) into P;
    let q = inject createEdges(500) into Q;
    let res = query p, q, prog select (x, y, z, w) from R(x, y, z, w);
    let start = System.currentTimeMillis();
    println(Vector.find(_x -> true, res));
    let end = System.currentTimeMillis();
    println(end-start)
  }, range)

def main(): Unit \ IO = 
  cartesian()