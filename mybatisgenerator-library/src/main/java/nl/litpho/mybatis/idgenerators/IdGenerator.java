package nl.litpho.mybatis.idgenerators;

public interface IdGenerator<T> {

  boolean supports(Class<?> keyType);

  T nextId();
}
