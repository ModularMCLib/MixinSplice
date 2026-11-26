package com.modularmc.mixinsplice.gametest.target;

public class PluginTarget {

    public int amount() {
        return 1;
    }

    public int unavailable() {
        return 3;
    }

    public int cancelled() {
        return 4;
    }

    public int original() {
        return 5;
    }

    public static final class Child extends PluginTarget {

        @Override
        public int amount() {
            return 2;
        }
    }
}
