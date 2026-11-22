package com.modularmclib.mixinweaver.gametest.target;

public class TransformationTarget {

    public int optionalValue() {
        return 1;
    }

    public int handlerValue() {
        return 2;
    }

    public int adjustedValue() {
        return 5;
    }

    public int originalValue() {
        return 6;
    }

    public int cancelledValue() {
        return 7;
    }

    public int unavailableValue() {
        return 8;
    }

    public static class Other {

        public int adjustedValue() {
            return 5;
        }

        public int originalValue() {
            return 6;
        }

        public int cancelledValue() {
            return 7;
        }
    }
}
