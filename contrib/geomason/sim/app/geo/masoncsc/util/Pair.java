package masoncsc.util;

/**
 *
 * @author Eric 'Siggy' Scott
 */
public class Pair<L, R>
{

    protected final L left;
    protected final R right;

    public Pair(L left, R right)
    {
        assert(left != null);
        assert(right != null);
        
        this.left = left;
        this.right = right;
    }

    public L getLeft()
    {
        return left;
    }

    public R getRight()
    {
        return right;
    }

    @Override
    public int hashCode()
    {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (!(o instanceof Pair))
        {
            return false;
        }
        Pair pairo = (Pair) o;
        return this.left.equals(pairo.getLeft())
                && this.right.equals(pairo.getRight());
    }
}
