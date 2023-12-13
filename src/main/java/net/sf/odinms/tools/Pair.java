package net.sf.odinms.tools;

import java.io.Serializable;

public record Pair<E, F>(E left, F right) implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;

    @Override
    public String toString() {
        return left.toString() + ":" + right.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<E, F> other = (Pair<E, F>) obj;
        if (left == null) {
            if (other.left != null) {
                return false;
            }
        } else if (!left.equals(other.left)) {
            return false;
        }
        if (right == null) {
            if (other.right != null) {
                return false;
            }
        } else if (!right.equals(other.right)) {
            return false;
        }
        return true;
    }
}
