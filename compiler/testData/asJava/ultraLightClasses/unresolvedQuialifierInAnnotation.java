@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract KClass<?> kClass();//  kClass()

}

@Ann(kClass = some.name.Unresolved.class)
public final class A /* A*/ {
  public  A();//  .ctor()

}
