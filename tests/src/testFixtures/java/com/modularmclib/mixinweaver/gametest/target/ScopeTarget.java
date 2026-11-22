package com.modularmclib.mixinweaver.gametest.target;

public class ScopeTarget {

    protected int offset = 4;

    protected int additionalOffset() {
        return 6;
    }

    public int value(boolean skipParent) {
        return 1;
    }

    public int extra() {
        return 5;
    }

    public static class Inherited extends ScopeTarget {}

    public static class Delegating extends ScopeTarget {

        @Override
        public int value(boolean skipParent) {
            return super.value(skipParent);
        }
    }

    public static class Bypassing extends ScopeTarget {

        @Override
        public int value(boolean skipParent) {
            return 2;
        }

        public int alternate() {
            return 6;
        }
    }

    public static class Cancelled extends ScopeTarget {

        @Override
        public int value(boolean skipParent) {
            return 5;
        }
    }

    public static class Partial extends ScopeTarget {

        @Override
        public int value(boolean skipParent) {
            if (skipParent) {
                return 3;
            }
            return super.value(false);
        }
    }

    public interface Contract {

        default int score() {
            return 2;
        }
    }

    public static class Implementation implements Contract {

        @Override
        public int score() {
            return 4;
        }
    }

    public static class DefaultDelegating implements Contract {

        @Override
        public int score() {
            return Contract.super.score();
        }
    }

    public static class Constructed {

        private int count;

        public int count() {
            return count;
        }

        public void increment() {
            count++;
        }
    }

    public static class ConstructedChild extends Constructed {}
}
