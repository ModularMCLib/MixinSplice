package com.modularmclib.mixinsplice.gametest.target;

public class ExtrasScopeTarget {

    public int value() {
        return 2;
    }

    public int onlyParent() {
        return 3;
    }

    public int guard() {
        return 4;
    }

    public static class Child extends ExtrasScopeTarget {

        @Override
        public int value() {
            return 5;
        }
    }
}
