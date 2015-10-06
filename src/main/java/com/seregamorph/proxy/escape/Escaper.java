package com.seregamorph.proxy.escape;

abstract class Escaper {
    protected Escaper() {}

    public abstract String escape(String string);
}
