package net.quazar.mg.mapper;

public interface ObjectMapper<S, T> {

    T mapTo(S source);

    S mapFrom(T t);

}
