public abstract interface A /* A*/ {
}

public abstract interface B /* B*/<T, R>  {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract java.lang.Class<?>[] kClass();//  kClass()

}

@Ann(kClass = {A.class, A.class, A.class, B.class, B.class})
public abstract interface Test /* Test*/ {
}
