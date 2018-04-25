package project3;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author wen-zhe.liu@asml.com
 *
 */
public class TestClass {
  List<Codeable> cs;
  private List<String> strings;
  private Set<B> bs;
  private Map<A, C> acMap;
  private A[] as;
  private int[] is;
  public Optional<C> ss;
  TestClass(B[] bs, List<A> as) {
    
  }
  
  static class SubCls {
    C[] ccs;
    interface SubInterface {
      A getA();
    }
  }
}

interface Codeable extends Cloneable {
  A create();
}

abstract class A extends Object implements Serializable {
  private int i = 0;
  private String str = "";
  private long[] ls;
  public A(int i) {
    this.i = i;
  }
  public String getStr() {
    return str;
  }
  public void setStr(String str) {
    this.str = str;
  }
  protected void hello() {
    
  }
}

class B extends A implements Codeable, Comparable<Integer> {
  public B() {
    super(5);
  }

  @Override
  public A create() {
    return this;
  }
  
  public B(int i) {
    super(i);
  }

  @Override
  public int compareTo(Integer o) {
    // TODO Auto-generated method stub
    return 0;
  }
}

final class C {
  private Codeable cc;
  private A a;
  private final Person[] persons = new Person[] {Person.MAN, Person.WOMAN};
  public void setCC(B b) {
    cc = b;
  }
  public void setCC(Codeable c) {
    cc = c;
  }
  public Optional<TestClass> hi() {
    return Optional.empty();
  }
}

enum Person implements Runnable {
  MAN("man"), WOMAN("woman");
  private final String name;
  Person(String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
  
  @Override
  public void run() {
    // TODO Auto-generated method stub
    
  }
}
