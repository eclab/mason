package sim.util;

import java.util.*;

public class Segment implements Comparable<Segment> {

    public int pid;
    public double st, ed, max, min;
    public Segment left, right, parent;

    public Segment(double st, double ed) {
        if (st > ed)
            throw new IllegalArgumentException(String.format("st (%g) should be less than or equal to ed (%g)", st, ed));
        
        this.st = st;
        this.ed = ed;
        this.max = ed;
        this.min = st;
    }

    public Segment(double st, double ed, int pid) {
        this(st, ed);
        this.pid = pid;
    }

    public Segment(int st, int ed) {
        this((double)st, (double)ed);
    }

    public Segment(int st, int ed, int pid) {
        this(st, ed);
        this.pid = pid;
    }

    public String toString() {
        String ppid = "null";
        if (parent != null)
            ppid = Integer.toString(parent.pid);

        return "[" + this.st + "-" + this.ed + " , " + this.min + " , " + this.max + " , " + ppid + ", " +this.all() + " )";
    }

    @Override
    public int compareTo(Segment other) {
        if (this.st < other.st)
            return -1;
        else if (this.st > other.st)
            return 1;

        return this.ed <= other.ed ? -1 : 1;
    }

    /**
     * overlapWith() and contains() assume the interval to be half-close half-open [st, ed)
     **/

    public boolean overlapWith(Segment other) {
        return !((this.st >= other.ed) || (this.ed <= other.st));
    }

    public boolean contains(double target) {
        return (this.st <= target) && (this.ed > target);
    }

    // return if the range [this.min, this.max] is covered by this node and its children
    public boolean all() {
        double curr_max = this.ed, curr_min = this.st;

        if (this.left != null) {
            if (!this.left.all() || this.left.max < curr_min)
                return false;
            curr_max = Math.max(this.left.max, curr_max);
            curr_min = Math.min(this.left.min, curr_min);
        }
        if (this.right != null) {
            if (!this.right.all() || this.right.min > curr_max)
                return false;
            curr_max = Math.max(this.right.max, curr_max);
            curr_min = Math.min(this.right.min, curr_min);
        }

        return curr_min == this.min && curr_max == this.max;
    }

}
